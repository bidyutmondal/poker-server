package com.example.poker.service;

import com.example.poker.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {

    public void startGame(GameState game) {
        long playersWithChips = game.getPlayers().values().stream()
                .filter(p -> p.getTotalChips() > 0)
                .count();
        if (playersWithChips < 2) return;
        
        // Reset everything for a new hand
        game.resetHand();

        // Increment dealer index
        game.setDealerIndex((game.getDealerIndex() + 1) % game.getPlayers().size());
        
        // Set all players who have chips to active
        for (PlayerState p : game.getPlayers().values()) {
            if (p.getTotalChips() > 0) {
                p.setFolded(false);
                p.setAllIn(false);
            } else {
                p.setFolded(true); // Can't play if 0 chips
            }
        }

        // Initialize deck and shuffle
        List<Card> deck = PokerGameLogic.initializeDeck();
        PokerGameLogic.shuffleDeck(deck);
        game.setDeck(deck);

        // Deal hole cards
        PokerGameLogic.dealHoleCards(game);

        // Post blinds
        List<PlayerState> activePlayers = game.getActivePlayers();
        if (activePlayers.size() >= 2) {
            int sbIndex = (game.getDealerIndex() + 1) % activePlayers.size();
            int bbIndex = (game.getDealerIndex() + 2) % activePlayers.size();
            
            PlayerState sbPlayer = activePlayers.get(sbIndex);
            PlayerState bbPlayer = activePlayers.get(bbIndex);

            sbPlayer.bet(Math.min(game.getSmallBlind(), sbPlayer.getTotalChips()));
            bbPlayer.bet(Math.min(game.getBigBlind(), bbPlayer.getTotalChips()));
            
            game.incrementPot(sbPlayer.getCurrentBet() + bbPlayer.getCurrentBet());

            // UTG is next
            if (activePlayers.size() == 2) {
                // Heads up: dealer posts SB and acts first pre-flop
                game.setCurrentPlayerIndex(sbIndex);
            } else {
                game.setCurrentPlayerIndex((bbIndex + 1) % activePlayers.size());
            }
        }

        game.setStage(GameStage.PRE_FLOP);
        game.setMinRaise(game.getBigBlind());
    }

    public void advanceStage(GameState game) {
        switch (game.getStage()) {
            case PRE_FLOP:
                dealFlop(game);
                break;
            case FLOP:
                dealTurn(game);
                break;
            case TURN:
                dealRiver(game);
                break;
            case RIVER:
                gotoShowdown(game);
                determineWinner(game);
                break;
            default:
                break;
        }
    }

    private void prepareNextRound(GameState game) {
        for (PlayerState p : game.getActivePlayers()) {
            p.setCurrentBet(0);
            p.setActedThisRound(false);
        }
        game.setMinRaise(game.getBigBlind());

        // First to act is small blind (first active player after dealer)
        List<PlayerState> activePlayers = game.getActivePlayers();
        if (!activePlayers.isEmpty()) {
            int nextIdx = (game.getDealerIndex() + 1) % activePlayers.size();
            game.setCurrentPlayerIndex(nextIdx);
            
            // If the first player is all in, find next
            while(activePlayers.get(game.getCurrentPlayerIndex()).isAllIn()) {
                game.setCurrentPlayerIndex((game.getCurrentPlayerIndex() + 1) % activePlayers.size());
                // Break if everyone is all in
                if (game.getCurrentPlayerIndex() == nextIdx) break;
            }
        }
    }

    public void dealFlop(GameState game) {
        // Burn a card
        if (!game.getDeck().isEmpty()) game.getDeck().remove(0);
        PokerGameLogic.dealFlop(game);
        prepareNextRound(game);
    }

    public void dealTurn(GameState game) {
        PokerGameLogic.dealTurn(game);
        prepareNextRound(game);
    }

    public void dealRiver(GameState game) {
        PokerGameLogic.dealRiver(game);
        prepareNextRound(game);
    }

    public void gotoShowdown(GameState game) {
        game.setStage(GameStage.SHOWDOWN);
        
        // Evaluate all hands
        for (PlayerState player : game.getNonFoldedPlayers()) {
            HandRank rank = PokerGameLogic.evaluateHand(player.getHoleCards(), game.getCommunityCards());
            int strength = PokerGameLogic.getHandStrength(player.getHoleCards(), game.getCommunityCards());
            player.setBestHand(rank);
            player.setHandStrength(strength);
        }
    }

    public List<String> determineWinner(GameState game) {
        List<String> winners = PokerGameLogic.determineWinner(game);
        game.setWinners(winners);
        
        if (!winners.isEmpty()) {
            int potSplit = game.getPot() / winners.size();
            for (String winnerId : winners) {
                PlayerState p = game.getPlayer(winnerId);
                if (p != null) {
                    p.setTotalChips(p.getTotalChips() + potSplit);
                }
            }
        }
        
        game.setPot(0);
        game.setStage(GameStage.GAME_OVER);
        return winners;
    }

    public void nextTurn(GameState game) {
        if (game.getPlayersInHand() <= 1) {
            determineWinner(game);
            return;
        }

        if (canMoveToNextBettingRound(game)) {
            advanceStage(game);
        } else {
            // Find next active player
            List<PlayerState> activePlayers = game.getActivePlayers();
            if (activePlayers.isEmpty()) return;

            int attempts = 0;
            do {
                game.setCurrentPlayerIndex((game.getCurrentPlayerIndex() + 1) % activePlayers.size());
                attempts++;
            } while (activePlayers.get(game.getCurrentPlayerIndex()).isAllIn() && attempts < activePlayers.size());
            
            // If all are all-in, advance
            if (attempts >= activePlayers.size()) {
                 advanceStage(game);
            }
        }
    }

    public void handlePlayerFold(GameState game, String playerId) {
        PlayerState player = game.getPlayer(playerId);
        if (player != null) {
            player.foldHand();
            player.setActedThisRound(true);
        }

        if (game.getPlayersInHand() == 1) {
            determineWinner(game);
        } else {
            nextTurn(game);
        }
    }

    /**
     * Handles a player leaving mid-game. Auto-folds if it's their turn, then removes them.
     * If only one player remains after removal, end the hand.
     */
    public void handlePlayerLeave(GameState game, String playerId) {
        PlayerState player = game.getPlayer(playerId);
        if (player == null) return;

        boolean wasCurrentPlayer = false;
        PlayerState currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.getPlayerId().equals(playerId)) {
            wasCurrentPlayer = true;
        }

        // Fold the player first so turn tracking works correctly
        if (!player.isFolded()) {
            player.foldHand();
            player.setActedThisRound(true);
        }

        // Remove from game state
        game.getPlayers().remove(playerId);

        // If only one player remains, end the hand
        if (game.getPlayersInHand() <= 1) {
            if (!game.getPlayers().isEmpty()) {
                determineWinner(game);
            }
            return;
        }

        // If the leaving player held the turn, advance it
        if (wasCurrentPlayer) {
            nextTurn(game);
        }
    }

    /**
     * Fully stops the game — resets state to WAITING_FOR_PLAYERS.
     */
    public void stopGame(GameState game) {
        game.resetHand();
        game.setStage(GameStage.WAITING_FOR_PLAYERS);
    }

    public void handlePlayerCheck(GameState game, String playerId) {
        PlayerState player = game.getPlayer(playerId);
        if (player != null && player.canBet()) {
             // In order to check, the player's current bet must match the max bet (or max bet is 0)
             int maxBet = game.getActivePlayers().stream().mapToInt(PlayerState::getCurrentBet).max().orElse(0);
             if (player.getCurrentBet() == maxBet) {
                 player.setActedThisRound(true);
                 nextTurn(game);
             }
        }
    }

    public void handlePlayerBet(GameState game, String playerId, int amount) {
        PlayerState player = game.getPlayer(playerId);
        if (player != null && player.canBet()) {
            int newBet = Math.max(0, Math.min(amount, player.getTotalChips()));
            int previousBet = player.getCurrentBet();
            
            player.bet(newBet);
            game.incrementPot(newBet);
            player.setActedThisRound(true);
            
            if (newBet > game.getMinRaise()) {
                game.setMinRaise(newBet - previousBet);
                // When someone raises, others must act again
                for (PlayerState p : game.getActivePlayers()) {
                    if (!p.getPlayerId().equals(playerId) && !p.isAllIn()) {
                        p.setActedThisRound(false);
                    }
                }
            }
            nextTurn(game);
        }
    }

    public boolean canMoveToNextBettingRound(GameState game) {
        List<PlayerState> activePlayers = game.getActivePlayers();
        
        // If 1 or 0 players can act, move to next round
        long playersWhoCanAct = activePlayers.stream().filter(p -> !p.isAllIn()).count();
        if (playersWhoCanAct <= 1) {
             // ensure the last acting player matched the bet
             int maxBet = activePlayers.stream().mapToInt(PlayerState::getCurrentBet).max().orElse(0);
             boolean allMatched = activePlayers.stream().allMatch(p -> p.isAllIn() || p.getCurrentBet() == maxBet);
             return allMatched;
        }

        int maxBet = activePlayers.stream().mapToInt(PlayerState::getCurrentBet).max().orElse(0);
        
        for (PlayerState p : activePlayers) {
            if (!p.isAllIn()) {
                if (!p.isActedThisRound() || p.getCurrentBet() < maxBet) {
                    return false;
                }
            }
        }
        return true;
    }
}
