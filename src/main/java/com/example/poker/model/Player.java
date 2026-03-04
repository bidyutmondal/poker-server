package com.example.poker.model;

import java.util.UUID;

public class Player {
    private String id; // UUID
    private String name;
    private int chips;
    private boolean active;

    public Player() {
        this.id = UUID.randomUUID().toString();
        this.chips = 0;
        this.active = true;
    }

    public Player(String name, int chips) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.chips = chips;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getChips() { return chips; }
    public void setChips(int chips) { this.chips = chips; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
