package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveMoveIterator.class);
    private final AdaptiveMoveSelectorStats<Solution_> stats;
    private final Random random;

    // Geometric restart
    private final Clock clock;
    private static final int GEOMETRIC_FACTOR = 2;
    private static final int MAX_GROW_GEOMETRIC_FACTOR_SECONDS = 600; // 10 minutes
    private static final int SCALING_FACTOR = 1;

    public AdaptiveMoveIterator(AdaptiveMoveSelectorStats<Solution_> stats, Random random, Clock clock) {
        this.stats = stats;
        this.random = random;
        this.clock = clock;
    }

    private void ensureInitialized() {
        if (stats.getNextRestartMillis() == 0 || stats.getGeometricGrowFactor() == 0) {
            // Delay of 10 seconds
            stats.addNextRestartMillis(clock.millis() + 10_000L);
            stats.setGeometricGrowFactor(1);
        }
    }

    protected void checkRestart() {
        if (clock.millis() >= stats.getNextRestartMillis()) {
            var nextRestart = SCALING_FACTOR * stats.getGeometricGrowFactor();
            stats.addNextRestartMillis(nextRestart * 1_000L);
            var newGrowFactor = stats.getGeometricGrowFactor() * GEOMETRIC_FACTOR;
            log.info("Adaptive move selector restarted, current grow factor {}, next grow factor {}",
                    stats.getGeometricGrowFactor(), newGrowFactor);
            stats.setGeometricGrowFactor(Math.min(newGrowFactor, MAX_GROW_GEOMETRIC_FACTOR_SECONDS));
            stats.reset();
        }
    }

    @Override
    public boolean hasNext() {
        return stats.hasMoveIterator();
    }

    @Override
    public Move<Solution_> next() {
        ensureInitialized();
        checkRestart();
        var weight = random.nextInt(stats.getTotalWeight()) + 1;
        var itemStatsList = stats.getIteratorList();
        var iteratorList = new ArrayList<Integer>(itemStatsList.size());
        var multiplier = 1;
        while (iteratorList.isEmpty()) {
            for (var i = 0; i < itemStatsList.size(); i++) {
                var item = itemStatsList.get(i);
                if (weight - item.getWeight() * multiplier <= 0) {
                    iteratorList.add(i);
                }
            }
            multiplier++;
        }
        var pos = 0;
        if (iteratorList.size() > 1) {
            pos = random.nextInt(iteratorList.size());
        }
        var iterator = itemStatsList.get(pos).getMoveIterator();
        var move = iterator.next();
        if (!iterator.hasNext()) {
            stats.removeIterator(pos);
            pos = -1;
        }
        return new AdaptiveMoveAdapter<>(move, stats, pos);
    }

    protected record AdaptiveMoveAdapter<Solution_>(Move<Solution_> move,
            AdaptiveMoveSelectorStats<Solution_> stats,
            int iteratorIndex) implements Move<Solution_> {

        @Override
        public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
            return move.isMoveDoable(scoreDirector);
        }

        @Override
        public Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
            return move.doMove(scoreDirector);
        }

        @Override
        public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
            move.doMoveOnly(scoreDirector);
        }

        @Override
        public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
            return move.rebase(destinationScoreDirector);
        }

        @Override
        public String getSimpleMoveTypeDescription() {
            return move.getSimpleMoveTypeDescription();
        }

        @Override
        public Collection<?> getPlanningEntities() {
            return move.getPlanningEntities();
        }

        @Override
        public Collection<?> getPlanningValues() {
            return move.getPlanningValues();
        }

        public void incrementWeight() {
            if (iteratorIndex > -1) {
                stats.incrementWeight(iteratorIndex);
            }
        }

        @Override
        public String toString() {
            return move.toString();
        }
    }
}
