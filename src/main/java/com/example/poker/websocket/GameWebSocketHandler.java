package com.example.poker.websocket;

import com.example.poker.model.*;
import com.example.poker.service.GameService;
import com.example.poker.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final RoomService roomService;
    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public GameWebSocketHandler(RoomService roomService, GameService gameService, ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String code = getCodeFromSession(session);
        if (code != null) {
            roomSessions.computeIfAbsent(code, k -> ConcurrentHashMap.newKeySet()).add(session);
            broadcastToRoom(code, new WebSocketMessage("PLAYER_JOINED", null));
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String code = getCodeFromSession(session);
        if (code == null) return;

        Room room = roomService.getRoom(code);
        if (room == null) return;

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            handleMessage(code, session, wsMessage);
        } catch (Exception e) {
            sendErrorToSession(session, "Invalid message format: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String code = getCodeFromSession(session);
        if (code != null) {
            Set<WebSocketSession> sessions = roomSessions.get(code);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(code);
                }
            }
        }
    }

    private void handleMessage(String code, WebSocketSession session, WebSocketMessage message) {
        Room room = roomService.getRoom(code);
        if (room == null) return;

        GameState gameState = room.getGameState();
        
        // Ignore actions if it's not the player's turn, except for game admin actions like START_GAME if applicable
        if (!message.getAction().equals("NEXT_ROUND")) {
             PlayerState currentPlayer = gameState.getCurrentPlayer();
             if (currentPlayer == null || !currentPlayer.getPlayerId().equals(message.getPlayerId())) {
                  sendErrorToSession(session, "Not your turn");
                  return;
             }
        }

        switch (message.getAction()) {
            case "BET":
                handleBet(code, message, gameState);
                break;
            case "FOLD":
                handleFold(code, message, gameState);
                break;
            case "CHECK":
                handleCheck(code, message, gameState);
                break;
            case "CALL":
                handleCall(code, message, gameState);
                break;
            case "RAISE":
                handleRaise(code, message, gameState);
                break;
            case "ALL_IN":
                handleAllIn(code, message, gameState);
                break;
            case "NEXT_ROUND":
                // NEXT_ROUND could be a manual override, or we can just ignore it since it's automatic now.
                break;
            default:
                sendErrorToRoom(code, "Unknown action: " + message.getAction());
        }

        // Broadcast updated game state
        broadcastGameState(code);

        // Automatically start next hand if game is over
        System.out.println("[DEBUG] After action, stage=" + gameState.getStage());
        if (gameState.getStage() == GameStage.GAME_OVER) {
            System.out.println("[DEBUG] GAME_OVER detected, scheduling next hand in 6s");
            scheduleNextHand(code, gameState);
        }
    }

    private void scheduleNextHand(String code, GameState gameState) {
        scheduler.schedule(() -> {
            try {
                Room room = roomService.getRoom(code);
                System.out.println("[DEBUG] Scheduler fired. room=" + room + " gameStarted="
                        + (room != null ? room.isGameStarted() : "N/A")
                        + " stage=" + (room != null ? room.getGameState().getStage() : "N/A"));
                if (room != null && room.isGameStarted() && room.getGameState().getStage() == GameStage.GAME_OVER) {
                    System.out.println("[DEBUG] Starting next hand...");
                    gameService.startGame(room.getGameState());
                    System.out.println("[DEBUG] Next hand started, broadcasting...");
                    broadcastGameState(code);
                    System.out.println("[DEBUG] Broadcast done.");
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Scheduler exception: " + e.getMessage());
                e.printStackTrace();
            }
        }, 6, TimeUnit.SECONDS);
    }

    private void handleBet(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        int amount = message.getAmount();
        gameService.handlePlayerBet(gameState, playerId, amount);
    }

    private void handleFold(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        gameService.handlePlayerFold(gameState, playerId);
    }

    private void handleCheck(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        gameService.handlePlayerCheck(gameState, playerId);
    }

    private void handleCall(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        PlayerState player = gameState.getPlayer(playerId);
        if (player != null) {
            int maxBet = gameState.getActivePlayers().stream()
                    .mapToInt(PlayerState::getCurrentBet)
                    .max()
                    .orElse(0);
            int amountToCall = maxBet - player.getCurrentBet();
            if (amountToCall >= 0) { // Call could be 0, which is technically a check
                if (amountToCall == 0) {
                     gameService.handlePlayerCheck(gameState, playerId);
                } else {
                     gameService.handlePlayerBet(gameState, playerId, amountToCall);
                }
            }
        }
    }

    private void handleRaise(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        int amount = message.getAmount();
        
        PlayerState player = gameState.getPlayer(playerId);
        if (player != null && amount >= gameState.getMinRaise()) {
            gameService.handlePlayerBet(gameState, playerId, amount);
        } else {
            // Amount too small, could reject or default to minimum
            if (player != null) {
                gameService.handlePlayerBet(gameState, playerId, Math.max(amount, gameState.getMinRaise()));
            }
        }
    }

    private void handleAllIn(String code, WebSocketMessage message, GameState gameState) {
        String playerId = message.getPlayerId();
        PlayerState player = gameState.getPlayer(playerId);
        if (player != null && player.getTotalChips() > 0) {
            gameService.handlePlayerBet(gameState, playerId, player.getTotalChips());
        }
    }

    private void broadcastGameState(String code) {
        Room room = roomService.getRoom(code);
        if (room != null) {
            broadcastToRoom(code, new WebSocketMessage("GAME_STATE_UPDATE", room.getGameState()));
        }
    }

    private void broadcastToRoom(String code, WebSocketMessage message) {
        Set<WebSocketSession> sessions = roomSessions.get(code);
        if (sessions == null || sessions.isEmpty()) return;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }

        for (WebSocketSession session : sessions) {
            synchronized (session) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                } catch (IOException e) {
                    // Session error, will be handled in afterConnectionClosed
                }
            }
        }
    }

    private void sendErrorToRoom(String code, String error) {
        broadcastToRoom(code, new WebSocketMessage("ERROR", error));
    }

    private void sendErrorToSession(WebSocketSession session, String error) {
        try {
            session.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(new WebSocketMessage("ERROR", error))
            ));
        } catch (IOException e) {
            // Ignore
        }
    }

    private String getCodeFromSession(WebSocketSession session) {
        String uri = session.getUri().toString();
        // Strip query string if present
        if (uri.contains("?")) uri = uri.substring(0, uri.indexOf("?"));
        // Both /ws/game/{code} and /sockjs/game/{code}/... are supported
        for (String prefix : new String[]{"/ws/game/", "/sockjs/game/"}) {
            int idx = uri.indexOf(prefix);
            if (idx >= 0) {
                String rest = uri.substring(idx + prefix.length());
                // The code is everything up to the next '/' (SockJS appends more segments)
                int slash = rest.indexOf('/');
                return slash >= 0 ? rest.substring(0, slash) : rest;
            }
        }
        return null;
    }

    public void broadcastPlayerLeft(String code) {
        broadcastToRoom(code, new WebSocketMessage("PLAYER_LEFT", null));
        broadcastGameState(code);
        
        Room room = roomService.getRoom(code);
        if (room != null && room.isGameStarted() && room.getGameState().getStage() == GameStage.GAME_OVER) {
            scheduleNextHand(code, room.getGameState());
        }
    }

    // DTO for WebSocket messages
    public static class WebSocketMessage {
        public String action;
        public String playerId;
        public Object payload;
        public int amount;

        public WebSocketMessage() {}

        public WebSocketMessage(String action, Object payload) {
            this.action = action;
            this.payload = payload;
        }

        // Getters and setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }

        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }

        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }
}
