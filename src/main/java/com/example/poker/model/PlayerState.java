package com.example.poker.model;

import java.util.ArrayList;
import java.util.List;

public class PlayerState {
    private String playerId;
    private String name;
    private int totalChips;
    private int chipsInPot;
    private int currentBet; // bet in current round
    private boolean folded;
    private boolean allIn;
    private List<Card> holeCards = new ArrayList<>();
    private HandRank bestHand;
    private int handStrength; // for comparison
    private boolean actedThisRound;

    public PlayerState() {}

    public PlayerState(String playerId, String name, int chips) {
        this.playerId = playerId;
        this.name = name;
        this.totalChips = chips;
        this.chipsInPot = 0;
        this.currentBet = 0;
        this.folded = false;
        this.allIn = false;
        this.actedThisRound = false;
    }

    public boolean canBet() {
        return !folded && !allIn && totalChips > 0;
    }

    public void bet(int amount) {
        int toBet = Math.min(amount, totalChips);
        totalChips -= toBet;
        currentBet += toBet;
        chipsInPot += toBet;
        if (totalChips == 0) {
            allIn = true;
        }
    }

    public void foldHand() {
        folded = true;
    }

    public void reset() {
        currentBet = 0;
        folded = false;
        allIn = totalChips == 0;
        actedThisRound = false;
        holeCards.clear();
        bestHand = null;
    }

    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalChips() { return totalChips; }
    public void setTotalChips(int totalChips) { this.totalChips = totalChips; }

    public int getChipsInPot() { return chipsInPot; }
    public void setChipsInPot(int chipsInPot) { this.chipsInPot = chipsInPot; }

    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean folded) { this.folded = folded; }

    public boolean isAllIn() { return allIn; }
    public void setAllIn(boolean allIn) { this.allIn = allIn; }

    public List<Card> getHoleCards() { return holeCards; }
    public void setHoleCards(List<Card> holeCards) { this.holeCards = holeCards; }

    public HandRank getBestHand() { return bestHand; }
    public void setBestHand(HandRank bestHand) { this.bestHand = bestHand; }

    public int getHandStrength() { return handStrength; }
    public void setHandStrength(int handStrength) { this.handStrength = handStrength; }

    public boolean isActedThisRound() { return actedThisRound; }
    public void setActedThisRound(boolean actedThisRound) { this.actedThisRound = actedThisRound; }
}
