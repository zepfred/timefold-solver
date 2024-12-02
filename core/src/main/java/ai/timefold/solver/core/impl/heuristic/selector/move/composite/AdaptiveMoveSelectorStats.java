package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.AdaptiveMoveIterator.AdaptiveMoveAdapter;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveMoveSelectorStats<Solution_> implements PhaseLifecycleListener<Solution_> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveMoveSelectorStats.class);
    private final List<MoveSelector<Solution_>> childMoveSelectorList;
    private final List<AdaptiveMoveIteratorData<Solution_>> itemStatsList;
    private Score<?> currentBest;
    private int weightTotal;
    // Geometric restart
    private int geometricGrowFactor;
    private long nextRestartMillis;

    public AdaptiveMoveSelectorStats(List<MoveSelector<Solution_>> childMoveSelectorList) {
        this.childMoveSelectorList = childMoveSelectorList;
        this.itemStatsList = new ArrayList<>(childMoveSelectorList.size());
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    protected void reset() {
        itemStatsList.clear();
        for (var moveSelector : childMoveSelectorList) {
            var moveIterator = moveSelector.iterator();
            if (moveIterator.hasNext()) {
                var itemStats = new AdaptiveMoveIteratorData<>(moveSelector, moveIterator, 1);
                itemStatsList.add(itemStats);
                weightTotal++;
            }
        }
    }

    protected int getTotalWeight() {
        return weightTotal;
    }

    protected void incrementWeight(int iteratorIndex) {
        itemStatsList.get(iteratorIndex).incrementWeight();
        this.weightTotal++;
    }

    protected void addNextRestartMillis(long increment) {
        this.nextRestartMillis += increment;
    }

    protected long getNextRestartMillis() {
        return nextRestartMillis;
    }

    protected void setGeometricGrowFactor(int geometricGrowFactor) {
        this.geometricGrowFactor = geometricGrowFactor;
    }

    protected int getGeometricGrowFactor() {
        return geometricGrowFactor;
    }

    public List<AdaptiveMoveIteratorData<Solution_>> getIteratorList() {
        return itemStatsList;
    }

    public void removeIterator(int pos) {
        var item = itemStatsList.remove(pos);
        this.weightTotal -= item.getWeight();
    }

    protected boolean hasMoveIterator() {
        return weightTotal > 0;
    }

    // ************************************************************************
    // lifecycle methods
    // ************************************************************************
    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        this.weightTotal = 0;
        this.geometricGrowFactor = 0;
        this.nextRestartMillis = 0;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        this.currentBest = stepScope.getPhaseScope().getBestScore();
        if (stepScope.getPhaseScope().isStuck() || itemStatsList.isEmpty()) {
            reset();
        } else {
            itemStatsList.forEach(AdaptiveMoveIteratorData::refreshMoveIterator);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        var improved = ((Score) currentBest).compareTo(stepScope.getScore()) < 0;
        if (improved && stepScope instanceof LocalSearchStepScope<Solution_> localSearchStepScope
                && localSearchStepScope.getStep() instanceof LegacyMoveAdapter<Solution_> legacyMoveAdapter
                && legacyMoveAdapter.legacyMove() instanceof AdaptiveMoveAdapter<Solution_> adaptiveMoveAdapter) {
            log.info("Best solution improved {}, {}", stepScope.getScore(), legacyMoveAdapter.legacyMove());
            adaptiveMoveAdapter.incrementWeight();
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
        log.info("Move weight ({}): {}", weightTotal, itemStatsList.stream()
                .map(i -> "%s=%d".formatted(
                        i.getMoveSelector().toString().substring(0, i.getMoveSelector().toString().indexOf("(")),
                        i.getWeight()))
                .collect(joining(";")));
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }
}
