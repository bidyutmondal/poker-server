package com.example.poker.dto;

public class PlayerActionDto {
    private String action; // FOLD, CHECK, CALL, RAISE, ALL_IN
    private String playerId;
    private int amount; // for RAISE and ALL_IN

    public PlayerActionDto() {}

    public PlayerActionDto(String action, String playerId, int amount) {
        this.action = action;
        this.playerId = playerId;
        this.amount = amount;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
