package com.example.poker.dto;

public class GameEventDto {
    private String type; // JOIN, START_GAME, ACTION, GAME_STATE_UPDATE, PLAYER_ACTION, SHOWDOWN, GAME_END
    private String playerId;
    private String playerName;
    private Object payload;
    private long timestamp;

    public GameEventDto() {}

    public GameEventDto(String type, String playerId, String playerName, Object payload, long timestamp) {
        this.type = type;
        this.playerId = playerId;
        this.playerName = playerName;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public GameEventDto(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
