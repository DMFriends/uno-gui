package application;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Manages the game state and rules for a game of Uno.
 * Tracks players, whose turn it is, the current top card, and game progression.
 * Enforces Uno rules including valid moves, action card effects, and turn
 * order.
 */
public class Game {

    private ArrayList<Player> players;
    private Integer currentPlayerIndex;
    private boolean isReversed;
    private Card topCard;
    private final Deck deck;

    /**
     * Creates a new game instance with the given players.
     *
     * @param players The list of players participating in the game
     */
    public Game(ArrayList<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.currentPlayerIndex = 0;
        this.isReversed = false;
    }

    /**
     * Initializes the game by dealing 7 cards to each player and drawing the
     * initial top card.
     * Ensures the initial top card is not an action card.
     *
     * @param players The list of players in the game
     */
    public void startGame(ArrayList<Player> players) {
        this.players = players;
        for (Player player : players) {
            deck.dealCards(player);
        }
        Card c;
        do {
            c = drawFromDeck();
            if (c.isActionCard())
                deck.add(c);
            else {
                this.topCard = c;
                break;
            }
        } while (true);
    }

    /**
     * Draws a single card from the deck and adds it to the player's hand.
     *
     * @param player The player drawing a card
     * @return The drawn card, or null if the deck is empty
     */
    public Card drawCard(Player player) {
        Card drawn = drawFromDeck();
        if (drawn != null) {
            player.getCards().add(drawn);
        }
        return drawn;
    }

    /**
     * Draws from the deck and applies fallback rebuild logic when empty.
     * Rebuild avoids adding the current top card as the immediate replacement.
     *
     * @return A drawn card, or null if no fallback card can be created
     */
    private Card drawFromDeck() {
        Card drawn = deck.draw();
        if (drawn != null) {
            return drawn;
        }

        for (String card : Card.cards) {
            if (topCard == null || !card.equals(topCard.toString())) {
                deck.add(new Card(card));
                break;
            }
        }

        return deck.draw();
    }

    /**
     * Draws two cards from the deck and adds them to the player's hand.
     *
     * @param player The player drawing cards
     */
    public void drawTwoCards(Player player) {
        drawCard(player);
        drawCard(player);
    }

    /**
     * Draws four cards from the deck and adds them to the player's hand.
     *
     * @param player The player drawing cards
     */
    public void drawFourCards(Player player) {
        drawCard(player);
        drawCard(player);
        drawCard(player);
        drawCard(player);
    }

    /**
     * Advances to the next player in turn order.
     * If the game is reversed, moves backward; otherwise moves forward.
     * Wraps around at the start and end of the player list.
     */
    public void nextPlayer() {
        if (this.isReversed) {
            this.currentPlayerIndex--;
        } else {
            this.currentPlayerIndex++;
        }
        if (this.currentPlayerIndex >= this.players.size()) {
            this.currentPlayerIndex = 0;
        } else if (this.currentPlayerIndex < 0) {
            this.currentPlayerIndex = this.players.size() - 1;
        }
    }

    /**
     * Gets the index of the current player.
     *
     * @return The index of the current player in the players list
     */
    public int getCurrentPlayerIndex() {
        return this.currentPlayerIndex;
    }

    /**
     * Determines if a card can be legally played on the current top card.
     * A move is valid if: the card is wild, or it matches the top card's color or
     * value.
     *
     * @param card The card to validate
     * @return true if the move is valid, false otherwise
     */
    public boolean isValidMove(Card card) {
        if (card == null || topCard == null) {
            return false;
        }
        if (card.isWild()) {
            return true;
        }
        return Objects.equals(card.getColor(), topCard.getColor()) || card.getValue() == topCard.getValue();
    }

    /**
     * Sets the top card of the discard pile.
     *
     * @param card The card to set as the top card
     */
    public void setTopCard(Card card) {
        this.topCard = card;
    }

    /**
     * Toggles the game direction (forward or backward).
     * In 2-player games, reverse acts like a skip (advances to next player).
     */
    public void reverse() {
        this.isReversed = !this.isReversed;
        if (this.players.size() == 2) {
            this.nextPlayer();
        }
    }

    /**
     * Skips the next player's turn.
     */
    public void skipTurn() {
        this.nextPlayer();
    }

    /**
     * Processes the effects of a card, applying the color already chosen for a
     * wild card (for example, by the color picker).
     *
     * @param c           The card to process
     * @param chosenColor The color to apply to wild cards (R, G, B, or Y); ignored
     *                    for non-wild cards
     */
    public void applyCard(Card c, String chosenColor) {
        String cardStr = c.toString();
        if (c.isWild()) {
            if (c.isPlusFour()) {
                this.nextPlayer();
                this.drawFourCards(this.players.get(this.currentPlayerIndex));
            }
            c.setColor(chosenColor);
        } else if (cardStr.endsWith("R")) {
            this.reverse();
        } else if (cardStr.endsWith("S")) {
            this.skipTurn();
        } else if (cardStr.endsWith("P")) {
            this.nextPlayer();
            this.drawTwoCards(this.players.get(this.currentPlayerIndex));
        }
        this.setTopCard(c);
    }

    /**
     * Gets the list of players in the game.
     *
     * @return The list of players
     */
    public ArrayList<Player> getPlayers() {
        return players;
    }

    /**
     * Checks whether play is currently running in reverse order.
     *
     * @return true if the direction has been reversed, false otherwise
     */
    public boolean isReversed() {
        return isReversed;
    }

    /**
     * Gets the current top card of the discard pile.
     *
     * @return The top card
     */
    public Card getTopCard() {
        return topCard;
    }
}
