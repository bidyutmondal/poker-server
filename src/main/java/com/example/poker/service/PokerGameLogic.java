package com.example.poker.service;

import com.example.poker.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PokerGameLogic {
    
    /**
     * Initialize deck with all 52 cards
     */
    public static List<Card> initializeDeck() {
        List<Card> deck = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
        return deck;
    }

    /**
     * Shuffle the deck
     */
    public static void shuffleDeck(List<Card> deck) {
        Collections.shuffle(deck);
    }

    /**
     * Deal hole cards to all players
     */
    public static void dealHoleCards(GameState game) {
        List<PlayerState> activePlayers = game.getActivePlayers();
        List<Card> deck = game.getDeck();

        // Deal 2 cards to each player
        for (PlayerState player : activePlayers) {
            if (deck.size() >= 2) {
                player.getHoleCards().add(deck.remove(0));
                player.getHoleCards().add(deck.remove(0));
            }
        }
    }

    /**
     * Deal flop (3 cards)
     */
    public static void dealFlop(GameState game) {
        List<Card> deck = game.getDeck();
        if (deck.size() >= 4) {
            deck.remove(0); // burn card
            for (int i = 0; i < 3; i++) {
                game.getCommunityCards().add(deck.remove(0));
            }
        }
        game.setStage(GameStage.FLOP);
    }

    /**
     * Deal turn card
     */
    public static void dealTurn(GameState game) {
        List<Card> deck = game.getDeck();
        if (deck.size() >= 2) {
            deck.remove(0); // burn card
            game.getCommunityCards().add(deck.remove(0));
        }
        game.setStage(GameStage.TURN);
    }

    /**
     * Deal river card
     */
    public static void dealRiver(GameState game) {
        List<Card> deck = game.getDeck();
        if (deck.size() >= 2) {
            deck.remove(0); // burn card
            game.getCommunityCards().add(deck.remove(0));
        }
        game.setStage(GameStage.RIVER);
    }

    /**
     * Evaluate best 5-card hand from 7 available cards
     */
    public static HandRank evaluateHand(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(holeCards);
        allCards.addAll(communityCards);

        // Check for royal flush first
        if (isRoyalFlush(allCards)) return HandRank.ROYAL_FLUSH;
        if (isStraightFlush(allCards)) return HandRank.STRAIGHT_FLUSH;
        if (isFourOfAKind(allCards)) return HandRank.FOUR_OF_A_KIND;
        if (isFullHouse(allCards)) return HandRank.FULL_HOUSE;
        if (isFlush(allCards)) return HandRank.FLUSH;
        if (isStraight(allCards)) return HandRank.STRAIGHT;
        if (isThreeOfAKind(allCards)) return HandRank.THREE_OF_A_KIND;
        if (isTwoPair(allCards)) return HandRank.TWO_PAIR;
        if (isOnePair(allCards)) return HandRank.ONE_PAIR;
        return HandRank.HIGH_CARD;
    }

    /**
     * Get hand strength for comparison (used for tiebreakers)
     */
    public static int getHandStrength(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(holeCards);
        allCards.addAll(communityCards);
        allCards.sort((a, b) -> Integer.compare(b.getRank().value, a.getRank().value));

        int strength = 0;
        for (int i = 0; i < allCards.size() && i < 5; i++) {
            strength = strength * 100 + allCards.get(i).getRank().value;
        }
        return strength;
    }

    private static boolean isRoyalFlush(List<Card> cards) {
        if (!isFlush(cards)) return false;
        if (!isStraight(cards)) return false;
        // Check if highest card is Ace
        return cards.stream().anyMatch(c -> c.getRank() == Card.Rank.ACE) &&
               cards.stream().anyMatch(c -> c.getRank() == Card.Rank.KING);
    }

    private static boolean isStraightFlush(List<Card> cards) {
        return isFlush(cards) && isStraight(cards);
    }

    private static boolean isFourOfAKind(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = countRanks(cards);
        return rankCount.values().stream().anyMatch(count -> count == 4);
    }

    private static boolean isFullHouse(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = countRanks(cards);
        boolean hasThree = rankCount.values().stream().anyMatch(count -> count == 3);
        boolean hasTwo = rankCount.values().stream().anyMatch(count -> count == 2);
        return hasThree && hasTwo;
    }

    private static boolean isFlush(List<Card> cards) {
        Map<Card.Suit, Integer> suitCount = cards.stream()
                .collect(Collectors.groupingBy(Card::getSuit, Collectors.summingInt(e -> 1)));
        return suitCount.values().stream().anyMatch(count -> count >= 5);
    }

    private static boolean isStraight(List<Card> cards) {
        Set<Integer> ranks = cards.stream()
                .map(c -> c.getRank().value)
                .collect(Collectors.toSet());

        // Check for regular straight
        for (int i = 14; i >= 5; i--) {
            boolean straight = true;
            for (int j = 0; j < 5; j++) {
                if (!ranks.contains(i - j)) {
                    straight = false;
                    break;
                }
            }
            if (straight) return true;
        }

        // Check for A-2-3-4-5 (wheel)
        return ranks.contains(14) && ranks.contains(2) && ranks.contains(3) && 
               ranks.contains(4) && ranks.contains(5);
    }

    private static boolean isThreeOfAKind(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = countRanks(cards);
        return rankCount.values().stream().anyMatch(count -> count == 3);
    }

    private static boolean isTwoPair(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = countRanks(cards);
        long pairCount = rankCount.values().stream().filter(count -> count == 2).count();
        return pairCount >= 2;
    }

    private static boolean isOnePair(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = countRanks(cards);
        return rankCount.values().stream().anyMatch(count -> count == 2);
    }

    private static Map<Card.Rank, Integer> countRanks(List<Card> cards) {
        return cards.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.summingInt(e -> 1)));
    }

    /**
     * Determine winner between multiple players
     */
    public static List<String> determineWinner(GameState game) {
        List<PlayerState> nonFolded = game.getNonFoldedPlayers();
        if (nonFolded.isEmpty()) return new ArrayList<>();

        // If only one player left, they win
        if (nonFolded.size() == 1) {
            return List.of(nonFolded.get(0).getPlayerId());
        }

        // Evaluate all players' hands
        for (PlayerState player : nonFolded) {
            HandRank rank = evaluateHand(player.getHoleCards(), game.getCommunityCards());
            int strength = getHandStrength(player.getHoleCards(), game.getCommunityCards());
            player.setBestHand(rank);
            player.setHandStrength(strength);
        }

        // Sort by hand rank, then by strength
        nonFolded.sort((a, b) -> {
            int handCompare = Integer.compare(b.getBestHand().rank, a.getBestHand().rank);
            if (handCompare != 0) return handCompare;
            return Integer.compare(b.getHandStrength(), a.getHandStrength());
        });

        // Check for splits (same hand)
        List<String> winners = new ArrayList<>();
        PlayerState bestPlayer = nonFolded.get(0);
        winners.add(bestPlayer.getPlayerId());

        for (int i = 1; i < nonFolded.size(); i++) {
            PlayerState player = nonFolded.get(i);
            if (player.getBestHand().rank == bestPlayer.getBestHand().rank &&
                player.getHandStrength() == bestPlayer.getHandStrength()) {
                winners.add(player.getPlayerId());
            } else {
                break;
            }
        }

        return winners;
    }
}
