package com.example.poker.service;

import com.example.poker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokerGameLogicTest {

    private List<Card> testDeck;

    @BeforeEach
    void setUp() {
        testDeck = PokerGameLogic.initializeDeck();
    }

    @Test
    void testDeckInitialization() {
        assertEquals(52, testDeck.size());
    }

    @Test
    void testDeckShuffle() {
        List<Card> deck1 = PokerGameLogic.initializeDeck();
        List<Card> deck2 = PokerGameLogic.initializeDeck();
        PokerGameLogic.shuffleDeck(deck1);
        PokerGameLogic.shuffleDeck(deck2);
        
        // While theoretically possible, it's extremely unlikely shuffled decks are identical
        assertNotEquals(deck1, deck2);
    }

    @Test
    void testPairDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.EIGHT));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.FOUR));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.TWO));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.ONE_PAIR, rank);
    }

    @Test
    void testStraightDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.FOUR));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.THREE));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.TWO));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));

        // Should detect A-2-3-4-5 straight (wheel)
        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.STRAIGHT, rank);
    }

    @Test
    void testFlushDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.TWO));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.FOUR));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.SIX));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.FLUSH, rank);
    }

    @Test
    void testFullHouseDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.KING));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.JACK));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.FULL_HOUSE, rank);
    }

    @Test
    void testThreeOfAKindDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.EIGHT));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.FOUR));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.TWO));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.THREE_OF_A_KIND, rank);
    }

    @Test
    void testFourOfAKindDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.QUEEN));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.QUEEN));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.JACK));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.FOUR_OF_A_KIND, rank);
    }

    @Test
    void testHighCardDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.TWO));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.THREE));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.FIVE));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.SEVEN));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.JACK));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.KING));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.HIGH_CARD, rank);
    }

    @Test
    void testTwoPairDetection() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.ACE));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.KING));
        cards.add(new Card(Card.Suit.SPADES, Card.Rank.KING));
        cards.add(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        cards.add(new Card(Card.Suit.DIAMONDS, Card.Rank.SEVEN));
        cards.add(new Card(Card.Suit.CLUBS, Card.Rank.FOUR));

        HandRank rank = PokerGameLogic.evaluateHand(cards.subList(0, 2), cards.subList(2, 7));
        assertEquals(HandRank.TWO_PAIR, rank);
    }

    @Test
    void testWinnerDetermination() {
        GameState game = new GameState("test-game");
        
        PlayerState p1 = new PlayerState("player1", "Alice", 1000);
        p1.getHoleCards().add(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        p1.getHoleCards().add(new Card(Card.Suit.DIAMONDS, Card.Rank.ACE));

        PlayerState p2 = new PlayerState("player2", "Bob", 1000);
        p2.getHoleCards().add(new Card(Card.Suit.CLUBS, Card.Rank.KING));
        p2.getHoleCards().add(new Card(Card.Suit.SPADES, Card.Rank.KING));

        game.addPlayer(p1);
        game.addPlayer(p2);

        List<Card> community = new ArrayList<>();
        community.add(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        community.add(new Card(Card.Suit.DIAMONDS, Card.Rank.JACK));
        community.add(new Card(Card.Suit.CLUBS, Card.Rank.TEN));
        community.add(new Card(Card.Suit.SPADES, Card.Rank.NINE));
        community.add(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        game.setCommunityCards(community);

        List<String> winners = PokerGameLogic.determineWinner(game);
        assertEquals(1, winners.size());
        assertEquals("player1", winners.get(0)); // Pair of Aces beats Pair of Kings
    }
}
