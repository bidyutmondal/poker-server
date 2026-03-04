package com.example.poker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    private String code;
    private List<Player> players = new ArrayList<>();
    private GameState gameState;
    private boolean gameStarted;
    private String dealerId;
    private String ownerId;   // First player to join; has admin privileges

    // Configurable room settings (set by owner before game starts)
    private int smallBlind = 10;
    private int bigBlind   = 20;
    private int defaultBuyIn = 1000; // Chips auto-assigned when a player joins

    public Room() {
        this.code = generateCode();
        this.gameState = new GameState(UUID.randomUUID().toString());
        this.gameStarted = false;
    }

    private String generateCode() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(letters.charAt((int)(Math.random() * letters.length())));
        }
        return sb.toString();
    }

    public Player findPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isOwner(String playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }

    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }

    public String getDealerId() { return dealerId; }
    public void setDealerId(String dealerId) { this.dealerId = dealerId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getSmallBlind() { return smallBlind; }
    public void setSmallBlind(int smallBlind) { this.smallBlind = smallBlind; }

    public int getBigBlind() { return bigBlind; }
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }

    public int getDefaultBuyIn() { return defaultBuyIn; }
    public void setDefaultBuyIn(int defaultBuyIn) { this.defaultBuyIn = defaultBuyIn; }
}
