package com.bananarepublic.model.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevelopmentDeck {
    private final List<DevelopmentCard> drawPile;
    private final List<DevelopmentCard> discardPile = new ArrayList<>();

    public DevelopmentDeck(List<DevelopmentCard> cards) {
        this.drawPile = new ArrayList<>(cards);
    }

    public static DevelopmentDeck createDefaultDeck() {
        List<DevelopmentCard> cards = new ArrayList<>();
        int idCounter = 1;

        for (int i = 0; i < 14; i++) {
            cards.add(new KnightCard("KNIGHT-" + String.format("%02d", idCounter++)));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new RoadBuildingCard("ROAD-" + String.format("%02d", idCounter++)));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MonopolyCard("MONOPOLY-" + String.format("%02d", idCounter++)));
        }
        for (int i = 0; i < 5; i++) {
            cards.add(new VictoryPointCard("VP-" + String.format("%02d", idCounter++)));
        }

        Collections.shuffle(cards);
        return new DevelopmentDeck(cards);
    }

    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    public DevelopmentCard draw() {
        if (drawPile.isEmpty()) {
            return null;
        }
        return drawPile.removeLast();
    }

    public void discard(DevelopmentCard card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public boolean isEmpty() {
        return drawPile.isEmpty();
    }

    public int size() {
        return drawPile.size();
    }

    public int discardSize() {
        return discardPile.size();
    }

    public void addCards(List<DevelopmentCard> cards) {
        drawPile.addAll(cards);
        shuffle();
    }

    public List<DevelopmentCard> getDrawPile() {
        return List.copyOf(drawPile);
    }

    public List<DevelopmentCard> getDiscardPile() {
        return List.copyOf(discardPile);
    }
}
