package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;

public class AdaptiveUnionMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private final AdaptiveMoveProbabilityManager<Solution_> adaptiveMoveProbabilityManager;

    public AdaptiveUnionMoveIterator(AdaptiveMoveProbabilityManager<Solution_> adaptiveMoveProbabilityManager) {
        this.adaptiveMoveProbabilityManager = adaptiveMoveProbabilityManager;
    }

    @Override
    public boolean hasNext() {
        return adaptiveMoveProbabilityManager.hasIteratorsAvailable();
    }

    @Override
    public Move<Solution_> next() {
        var iterator = adaptiveMoveProbabilityManager.getSelectedMoveIterator();
        var move = iterator.next();
        if (!iterator.hasNext()) {
            adaptiveMoveProbabilityManager.removeSelectedMoveIterator();
        }
        return move;
    }
}
