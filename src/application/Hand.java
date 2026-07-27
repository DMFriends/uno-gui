package application;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a player's hand of cards in Uno.
 * Manages card collection including adding, removing, and searching for cards.
 */
public class Hand {
	private final ArrayList<Card> playerCards = new ArrayList<>();

	/**
	 * Creates an empty hand.
	 */
	public Hand() {

	}

	/**
	 * Adds a card to the hand.
	 *
	 * @param c The card to add
	 */
	public void add(Card c) {
		playerCards.add(c);
	}

	/**
	 * Removes a card from the hand.
	 *
	 * @param c The card to remove
	 */
	public void remove(Card c) {
		playerCards.remove(c);
	}

	/**
	 * Gets the number of cards in the hand.
	 *
	 * @return The size of the hand
	 */
	public int size() {
		return playerCards.size();
	}

	/**
	 * Gets the list of cards in the hand.
	 *
	 * @return The list of cards
	 */
	public ArrayList<Card> getCards() {
		return playerCards;
	}

	/**
	 * Returns a sorted list of card strings in the hand.
	 * The original hand order is not modified.
	 *
	 * @return A new sorted list of card strings
	 */
	public ArrayList<String> sort() {
		ArrayList<String> sorted = new ArrayList<>();

		for (int i = 0; i < size(); i++) {
			sorted.add(playerCards.get(i).toString());
		}

		Collections.sort(sorted);

		return sorted;
	}
}