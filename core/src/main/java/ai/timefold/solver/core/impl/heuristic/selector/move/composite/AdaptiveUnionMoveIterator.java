package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;

public class AdaptiveUnionMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private final AdaptiveMoveProbabilityManager<Solution_> adaptiveMoveProbabilityManager;
    private final Random random;

    public AdaptiveUnionMoveIterator(AdaptiveMoveProbabilityManager<Solution_> adaptiveMoveProbabilityManager, Random random) {
        this.adaptiveMoveProbabilityManager = adaptiveMoveProbabilityManager;
        this.random = random;
    }

    @Override
    public boolean hasNext() {
        return adaptiveMoveProbabilityManager.getProbabilityWeightTotal() > 0;
    }

    @Override
    public Move<Solution_> next() {
        var weight = random.nextInt(adaptiveMoveProbabilityManager.getProbabilityWeightTotal()) + 1;
        var movesSelected = new ArrayList<Iterator<Move<Solution_>>>(adaptiveMoveProbabilityManager.getMoveListSize());
        var multiplier = 1;
        while (movesSelected.isEmpty()) {
            for (var i = 0; i < adaptiveMoveProbabilityManager.getMoveListSize(); i++) {
                if (weight - adaptiveMoveProbabilityManager.getMoveWeight(i) * multiplier <= 0) {
                    movesSelected.add(adaptiveMoveProbabilityManager.getMoveIterator(i));
                }
            }
            multiplier++;
        }
        var pos = 0;
        if (movesSelected.size() > 1) {
            pos = random.nextInt(movesSelected.size());
        }
        var move = movesSelected.get(pos).next();
        if (!movesSelected.get(pos).hasNext()) {
            adaptiveMoveProbabilityManager.reset();
        }
        return move;
    }
}
