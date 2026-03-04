package com.example.poker.model;

public class Card {
    public enum Suit {
        HEARTS(4), DIAMONDS(3), CLUBS(2), SPADES(1);
        
        public final int value;
        Suit(int value) { this.value = value; }
    }

    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6),
        SEVEN(7), EIGHT(8), NINE(9), TEN(10),
        JACK(11), QUEEN(12), KING(13), ACE(14);

        public final int value;
        Rank(int value) { this.value = value; }
    }

    private Suit suit;
    private Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() { return suit; }
    public void setSuit(Suit suit) { this.suit = suit; }

    public Rank getRank() { return rank; }
    public void setRank(Rank rank) { this.rank = rank; }

    @Override
    public String toString() {
        return rank.name() + " of " + suit.name();
    }
}
