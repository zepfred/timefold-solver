package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.ConsumerMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

/**
 * A {@link CompositeMoveSelector} that extends {@link UnionMoveSelector} to apply an adaptive neighborhood selection
 * strategy.
 * 
 * 
 *
 * @see CompositeMoveSelector
 */
public class AdaptiveMoveSelector<Solution_> extends UnionMoveSelector<Solution_> {

    public AdaptiveMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList,
            SelectionProbabilityWeightFactory<Solution_, MoveSelector<Solution_>> selectorProbabilityWeightFactory) {
        super(childMoveSelectorList, true, Objects.requireNonNull(selectorProbabilityWeightFactory,
                "The factory selectorProbabilityWeightFactory cannot be null."));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateStatistics(Move<Solution_> move, Score<?> score) {
        if (move instanceof ConsumerMoveAdapter consumerMoveAdapter) {
            consumerMoveAdapter.consume(score);
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new BiasedRandomUnionMoveIterator<>(childMoveSelectorList,
                moveSelector -> {
                    double weight = selectorProbabilityWeightFactory.createProbabilityWeight(scoreDirector, moveSelector);
                    if (weight < 0.0) {
                        throw new IllegalStateException(
                                "The selectorProbabilityWeightFactory (" + selectorProbabilityWeightFactory
                                        + ") returned a negative probabilityWeight (" + weight + ").");
                    }
                    return weight;
                }, workingRandom);
    }

    @Override
    public String toString() {
        return "Adaptive(" + childMoveSelectorList + ")";
    }

}
