package com.example.poker.service;

import com.example.poker.model.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final GameService gameService;

    public RoomService(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Create a room with optional blind and buy-in settings.
     */
    public Room createRoom(int smallBlind, int bigBlind, int defaultBuyIn) {
        Room room = new Room();
        if (smallBlind > 0) room.setSmallBlind(smallBlind);
        if (bigBlind > 0)   room.setBigBlind(bigBlind);
        if (defaultBuyIn > 0) room.setDefaultBuyIn(defaultBuyIn);
        rooms.put(room.getCode(), room);
        return room;
    }

    public Room getRoom(String code) {
        return rooms.get(code);
    }

    /**
     * Adds a player to a room.
     * - First player becomes the owner.
     * - Players are automatically assigned `defaultBuyIn` chips.
     * - Mid-game joiners are added to GameState as folded (sit out current hand).
     */
    public Player addPlayer(String code, String name) {
        Room room = rooms.get(code);
        if (room == null) return null;

        Player p = new Player(name, room.getDefaultBuyIn());
        room.getPlayers().add(p);

        // First player becomes the room owner
        if (room.getOwnerId() == null) {
            room.setOwnerId(p.getId());
        }

        // If game is already running, add as a folded participant in GameState
        if (room.isGameStarted()) {
            PlayerState ps = new PlayerState(p.getId(), p.getName(), p.getChips());
            ps.setFolded(true); // Sit out until next hand
            room.getGameState().addPlayer(ps);
        }

        return p;
    }

    /**
     * Update room blind settings. Only the owner can do this, and only before the game starts.
     */
    public void updateSettings(String code, String requestingPlayerId,
                               Integer smallBlind, Integer bigBlind, Integer defaultBuyIn) throws Exception {
        Room room = rooms.get(code);
        if (room == null) throw new Exception("Room not found");
        if (!room.isOwner(requestingPlayerId)) throw new Exception("Only the room owner can change settings");
        if (room.isGameStarted()) throw new Exception("Cannot change settings while a game is in progress");

        if (smallBlind != null && smallBlind > 0) room.setSmallBlind(smallBlind);
        if (bigBlind   != null && bigBlind > 0)   room.setBigBlind(bigBlind);
        if (defaultBuyIn != null && defaultBuyIn > 0) room.setDefaultBuyIn(defaultBuyIn);

        // Validate blind relationship
        if (room.getBigBlind() < room.getSmallBlind()) {
            throw new Exception("Big blind must be >= small blind");
        }
    }

    /**
     * Distribute chips (buy-in) to a player. Can be used before or during a game.
     * Only the room owner can distribute chips.
     */
    public void distributeChips(String code, String requestingPlayerId,
                                String targetPlayerId, int chips) throws Exception {
        Room room = rooms.get(code);
        if (room == null) throw new Exception("Room not found");
        if (!room.isOwner(requestingPlayerId)) throw new Exception("Only the room owner can give chips");

        Player player = room.findPlayer(targetPlayerId);
        if (player == null) throw new Exception("Player not found");
        if (chips <= 0) throw new Exception("Chips must be positive");

        player.setChips(player.getChips() + chips); // Additive buy-in

        // Sync with live GameState
        if (room.isGameStarted()) {
            PlayerState ps = room.getGameState().getPlayer(targetPlayerId);
            if (ps != null) {
                ps.setTotalChips(ps.getTotalChips() + chips);
                // Unfold if they now have chips (so they can play next hand)
                if (ps.isFolded() && ps.getTotalChips() > 0) {
                    // Leave folded for current hand; they'll be unfolded at start of next hand
                }
            }
        }
    }

    /**
     * Starts the game. Only the room owner can call this.
     * Blind settings from the room are pushed into GameState.
     */
    public void startGame(String code, String requestingPlayerId) throws Exception {
        Room room = rooms.get(code);
        if (room == null) throw new Exception("Room not found");
        if (!room.isOwner(requestingPlayerId)) throw new Exception("Only the room owner can start the game");
        if (room.getPlayers().size() < 2) throw new Exception("At least 2 players required");

        // Validate at least 2 players have chips
        long playersWithChips = room.getPlayers().stream().filter(p -> p.getChips() > 0).count();
        if (playersWithChips < 2) throw new Exception("At least 2 players must have chips to start");

        room.setGameStarted(true);
        GameState gameState = room.getGameState();

        // Apply room blind settings to game state
        gameState.setSmallBlind(room.getSmallBlind());
        gameState.setBigBlind(room.getBigBlind());
        gameState.setMinRaise(room.getBigBlind());

        // Sync all room players into GameState (avoid duplicates)
        for (Player p : room.getPlayers()) {
            if (gameState.getPlayer(p.getId()) == null) {
                PlayerState ps = new PlayerState(p.getId(), p.getName(), p.getChips());
                gameState.addPlayer(ps);
            } else {
                // Keep chips in sync
                gameState.getPlayer(p.getId()).setTotalChips(p.getChips());
            }
        }

        room.setDealerId(room.getPlayers().get(0).getId());
        gameState.setDealerIndex(0);

        gameService.startGame(gameState);
    }

    /**
     * Stops the game. Only the room owner can call this.
     */
    public void stopGame(String code, String requestingPlayerId) throws Exception {
        Room room = rooms.get(code);
        if (room == null) throw new Exception("Room not found");
        if (!room.isOwner(requestingPlayerId)) throw new Exception("Only the room owner can stop the game");

        gameService.stopGame(room.getGameState());
        room.setGameStarted(false);
    }

    /**
     * Removes a player from the room. Auto-folds their hand if game is running.
     * Transfers ownership if owner leaves.
     */
    public void removePlayer(String code, String playerId) throws Exception {
        Room room = rooms.get(code);
        if (room == null) throw new Exception("Room not found");

        Player player = room.findPlayer(playerId);
        if (player == null) throw new Exception("Player not found");

        if (room.isGameStarted()) {
            gameService.handlePlayerLeave(room.getGameState(), playerId);
        }

        room.getPlayers().removeIf(p -> p.getId().equals(playerId));

        // Transfer ownership if owner left
        if (room.isOwner(playerId) && !room.getPlayers().isEmpty()) {
            room.setOwnerId(room.getPlayers().get(0).getId());
        } else if (room.getPlayers().isEmpty()) {
            room.setOwnerId(null);
        }
    }

    public boolean isValidRoomCode(String code) {
        return rooms.containsKey(code);
    }

    public int getPlayerCount(String code) {
        Room room = rooms.get(code);
        return room != null ? room.getPlayers().size() : 0;
    }
}
