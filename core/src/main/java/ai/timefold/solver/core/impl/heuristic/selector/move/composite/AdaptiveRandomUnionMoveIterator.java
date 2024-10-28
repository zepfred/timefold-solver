package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.ConsumerMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

final class AdaptiveRandomUnionMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private final int iterationToRefreshStatistic;
    private final double reactionFactor;
    private final NavigableMap<Double, ProbabilityItem<Solution_>> probabilityItemMap;
    private final Random workingRandom;
    private int iterations = 0;
    private double probabilityWeightTotal = 0;

    public AdaptiveRandomUnionMoveIterator(List<MoveSelector<Solution_>> childMoveSelectorList,
            Random workingRandom, int iterationToRefreshStatistic, double reactionFactor) {
        this.probabilityItemMap = new TreeMap<>();
        double defaultProbabilityPerSelector = 1d / childMoveSelectorList.size();
        probabilityWeightTotal = defaultProbabilityPerSelector;
        for (var moveSelector : childMoveSelectorList) {
            var moveIterator = moveSelector.iterator();
            var probabilityItem = new ProbabilityItem<Solution_>();
            probabilityItem.moveSelector = moveSelector;
            probabilityItem.moveIterator = moveIterator;
            probabilityItem.probabilityWeight = defaultProbabilityPerSelector;
            probabilityItemMap.put(probabilityWeightTotal, probabilityItem);
            probabilityWeightTotal += defaultProbabilityPerSelector;
        }
        this.iterationToRefreshStatistic = iterationToRefreshStatistic;
        this.reactionFactor = reactionFactor;
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !probabilityItemMap.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        iterations++;
        if (iterations == iterationToRefreshStatistic) {
            refreshMoveIteratorMap();
            iterations = 0;
        }
        var randomOffset = RandomUtils.nextDouble(workingRandom, 1.0);
        var item = probabilityItemMap.floorEntry(randomOffset).getValue();
        var moveIterator = item.moveIterator;
        var move = moveIterator.next();
        return new ConsumerMoveAdapter<>(move, item::updateScore);
    }

    private void refreshMoveIteratorMap() {
        var items = new ArrayList<>(probabilityItemMap.values());
        probabilityItemMap.clear();
        // Adjust all probabilities before updating the selector map
        for (var probabilityItem : items) {
            var updatedProbabilityWeight = probabilityItem.probabilityWeight * (1 - reactionFactor) + reactionFactor;
        }
        probabilityWeightTotal = 0;
        for (var probabilityItem : items) {
            probabilityWeightTotal += probabilityItem.probabilityWeight;
            probabilityItemMap.put(probabilityWeightTotal, probabilityItem);
        }
    }

    private static final class ProbabilityItem<Solution_> {

        MoveSelector<Solution_> moveSelector;
        Iterator<Move<Solution_>> moveIterator;
        int count = 0;
        Score<?> bestScore = null;
        double probabilityWeight = 0.0;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void updateScore(Score score) {
            count++;
            if (bestScore == null || score.compareTo(bestScore) > 0) {
                bestScore = score;
            }
        }
    }

}
