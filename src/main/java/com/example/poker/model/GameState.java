package com.example.poker.model;

import java.util.*;

import java.util.stream.Collectors;

public class GameState {
    private String gameId;
    private GameStage stage;
    private Map<String, PlayerState> players; // playerId -> PlayerState
    private List<Card> communityCards;
    private List<Card> deck;
    private int pot;
    private int currentPlayerIndex;
    private int dealerIndex;
    private int smallBlind = 10;
    private int bigBlind = 20;
    private int minRaise;
    private String winnerId;
    private List<String> winners; // for splits
    private long lastActionTime;

    public GameState() {
        this(UUID.randomUUID().toString());
    }

    public GameState(String gameId) {
        this.gameId = gameId;
        this.stage = GameStage.WAITING_FOR_PLAYERS;
        this.players = new HashMap<>();
        this.communityCards = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.pot = 0;
        this.currentPlayerIndex = 0;
        this.dealerIndex = 0;
        this.minRaise = bigBlind;
        this.winners = new ArrayList<>();
        this.lastActionTime = System.currentTimeMillis();
    }

    public PlayerState getPlayer(String playerId) {
        return players.get(playerId);
    }

    public void addPlayer(PlayerState player) {
        players.put(player.getPlayerId(), player);
    }

    public List<PlayerState> getActivePlayers() {
        return players.values().stream()
                .filter(p -> !p.isFolded() || p.isAllIn())
                .collect(Collectors.toList());
    }

    public List<PlayerState> getNonFoldedPlayers() {
        return players.values().stream()
                .filter(p -> !p.isFolded())
                .collect(Collectors.toList());
    }

    public int getPlayersInHand() {
        return (int) players.values().stream()
                .filter(p -> !p.isFolded())
                .count();
    }

    public boolean isGameOver() {
        return getPlayersInHand() <= 1;
    }

    public PlayerState getCurrentPlayer() {
        List<PlayerState> active = getActivePlayers();
        if (active.isEmpty()) return null;
        return active.get(currentPlayerIndex % active.size());
    }

    public void incrementPot(int amount) {
        pot += amount;
    }

    public void resetHand() {
        for (PlayerState p : players.values()) {
            p.reset();
        }
        communityCards = new ArrayList<>();
        deck = new ArrayList<>();
        pot = 0;
        winnerId = null;
        winners = new ArrayList<>();
        stage = GameStage.PRE_FLOP;
    }

    // Getters and Setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public GameStage getStage() { return stage; }
    public void setStage(GameStage stage) { this.stage = stage; }

    public Map<String, PlayerState> getPlayers() { return players; }
    public void setPlayers(Map<String, PlayerState> players) { this.players = players; }

    public List<Card> getCommunityCards() { return communityCards; }
    public void setCommunityCards(List<Card> communityCards) { this.communityCards = communityCards; }

    public List<Card> getDeck() { return deck; }
    public void setDeck(List<Card> deck) { this.deck = deck; }

    public int getPot() { return pot; }
    public void setPot(int pot) { this.pot = pot; }

    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }

    public int getDealerIndex() { return dealerIndex; }
    public void setDealerIndex(int dealerIndex) { this.dealerIndex = dealerIndex; }

    public int getSmallBlind() { return smallBlind; }
    public void setSmallBlind(int smallBlind) { this.smallBlind = smallBlind; }

    public int getBigBlind() { return bigBlind; }
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }

    public int getMinRaise() { return minRaise; }
    public void setMinRaise(int minRaise) { this.minRaise = minRaise; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public List<String> getWinners() { return winners; }
    public void setWinners(List<String> winners) { this.winners = winners; }

    public long getLastActionTime() { return lastActionTime; }
    public void setLastActionTime(long lastActionTime) { this.lastActionTime = lastActionTime; }
}
