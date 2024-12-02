package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class AdaptiveMoveIteratorData<Solution_> {

    private final MoveSelector<Solution_> moveSelector;
    private Iterator<Move<Solution_>> moveIterator;
    private int weight;

    public AdaptiveMoveIteratorData(MoveSelector<Solution_> moveSelector, Iterator<Move<Solution_>> moveIterator, int weight) {
        this.moveSelector = moveSelector;

        this.moveIterator = moveIterator;
        this.weight = weight;
    }

    public MoveSelector<Solution_> getMoveSelector() {
        return moveSelector;
    }

    public Iterator<Move<Solution_>> getMoveIterator() {
        return moveIterator;
    }

    public void setMoveIterator(Iterator<Move<Solution_>> moveIterator) {
        this.moveIterator = moveIterator;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void incrementWeight() {
        this.weight++;
    }

    public void refreshMoveIterator() {
        this.moveIterator = moveSelector.iterator();
    }
}
