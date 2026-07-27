package application;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the game deck containing all Uno cards.
 * Manages card drawing and deck replenishment when cards run low.
 */
public class Deck {
    private final ArrayList<Card> deck = new ArrayList<>();

    /**
     * Creates and initializes a new deck with 3 copies of each card.
     * The deck is shuffled after initialization.
     *
     */
    public Deck() {
        for (int i = 0; i < 3; i++) {
            for (String card : Card.cards) {
                deck.add(new Card(card));
            }
        }

        Collections.shuffle(deck);
    }

    /**
     * Draws a card from the top of the deck.
     *
     * @return The drawn card, or null if the deck is empty
     */
    public Card draw() {
        if (!deck.isEmpty()) {
            return deck.remove(0);
        }
        return null;
    }

    /**
     * Deals 7 random cards to a player at the start of the game.
     *
     * @param p The player to receive cards
     */
    public void dealCards(Player p) {
        for (int i = 0; i < 7; i++) {
            int randIndex = (int) (Math.random() * deck.size());
            String randomCard = deck.get(randIndex).toString();
            deck.remove(deck.get(randIndex));

            Card c = new Card(randomCard);

            p.getCards().add(c);
        }
    }

    /**
     * Gets the number of cards left in the deck.
     *
     * @return The deck size
     */
    public int size() {
        return deck.size();
    }

    /**
     * Adds a card back to the deck.
     * Clears any color choice on wild cards before adding.
     *
     * @param c The card to add to the deck
     */
    public void add(Card c) {
        if (c != null) {
            c.clearChosenColor();
        }
        deck.add(c);
    }
}