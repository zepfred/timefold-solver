package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private static final double GEOMETRIC_FACTOR = 1.3;
    private static final int MAX_GROW_GEOMETRIC_FACTOR_SECONDS = 600; // 10 minutes
    private static final double SCALING_FACTOR = 1.0;

    public AdaptiveMoveIterator(AdaptiveMoveSelectorStats<Solution_> stats, Random random, Clock clock) {
        this.stats = stats;
        this.random = random;
        this.clock = clock;
    }

    private void ensureInitialized() {
        if (stats.getNextRestart() == 0 || stats.getGeometricGrowFactor() == 0) {
            stats.incrementNextRestart(clock.millis() + 1_000L);
            stats.setGeometricGrowFactor(1);
        }
    }

    protected void checkTimeRestart() {
        if (clock.millis() >= stats.getNextRestart()) {
            var nextRestart = Math.ceil(SCALING_FACTOR * stats.getGeometricGrowFactor());
            stats.setNextRestart((long) (clock.millis() + nextRestart * 1_000L));
            var newGrowFactor = Math.ceil(stats.getGeometricGrowFactor() * GEOMETRIC_FACTOR);
            //            log.info("Adaptive move selector restarted, current grow factor {}, next grow factor {}, {}",
            //                    stats.getGeometricGrowFactor(), newGrowFactor, stats);
            stats.setGeometricGrowFactor(Math.min(newGrowFactor, MAX_GROW_GEOMETRIC_FACTOR_SECONDS));
            stats.reset();
        }
    }

    protected void checkBestSolutionRestart() {
        if (stats.getBestSolutionCount() >= stats.getNextRestart()) {
            var nextRestart = SCALING_FACTOR * stats.getGeometricGrowFactor();
            stats.setNextRestart((long) nextRestart);
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
        checkTimeRestart();
        var itemStatsList = stats.getItemStatsList();
        List<AdaptiveMoveIteratorData<Solution_>> iteratorList = new ArrayList<>(itemStatsList.size());
        var weight = random.nextDouble(stats.getTotalWeight()) + 1;
        if (stats.getBestSolutionCount() > 0 && weight > itemStatsList.size()) {
            var multiplier = 1;
            while (iteratorList.isEmpty()) {
                for (var item : itemStatsList) {
                    if (weight - item.getWeight() * multiplier <= 0) {
                        iteratorList.add(item);
                    }
                }
                multiplier++;
            }
        } else {
            iteratorList = itemStatsList;
        }
        var pos = 0;
        if (iteratorList.size() > 1) {
            pos = random.nextInt(iteratorList.size());
        }
        var item = iteratorList.get(pos);
        var iterator = item.getMoveIterator();
        var move = iterator.next();
        if (!iterator.hasNext()) {
            stats.reset();
            item = null;
        }
        return new AdaptiveMoveAdapter<>(move, item);
    }

    protected record AdaptiveMoveAdapter<Solution_>(Move<Solution_> move,
            AdaptiveMoveIteratorData<Solution_> item) implements Move<Solution_> {

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
            if (item != null) {
                item.incrementWeight();
            }
        }

        @Override
        public String toString() {
            return move.toString();
        }
    }
}
