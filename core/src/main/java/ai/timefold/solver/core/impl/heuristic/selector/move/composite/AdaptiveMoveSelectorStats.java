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
    private List<AdaptiveMoveIteratorData<Solution_>> itemStatsList;
    private Score<?> currentBest;
    private double weightTotal;
    private int bestSolutionCount;
    // Geometric restart
    private double geometricGrowFactor;
    private long nextRestart;

    public AdaptiveMoveSelectorStats(List<MoveSelector<Solution_>> childMoveSelectorList) {
        this.childMoveSelectorList = childMoveSelectorList;
        this.itemStatsList = new ArrayList<>(childMoveSelectorList.size());
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    protected void reset() {
        this.weightTotal = 0;
        this.bestSolutionCount = 0;
        itemStatsList.clear();
        for (var moveSelector : childMoveSelectorList) {
            var moveIterator = moveSelector.iterator();
            if (moveIterator.hasNext()) {
                var itemStats = new AdaptiveMoveIteratorData<>(this, moveSelector, moveIterator, 1);
                itemStatsList.add(itemStats);
                weightTotal++;
            }
        }
    }

    protected void resetWithHistory() {
        this.weightTotal = 0;
        this.bestSolutionCount = 0;
        List<AdaptiveMoveIteratorData<Solution_>> newItemStatsList = new ArrayList<>(itemStatsList.size());
        for (var item : itemStatsList) {
            item.setMoveIterator(item.getMoveSelector().iterator());
            if (item.getMoveIterator().hasNext()) {
                item.setWeight(Math.max(1.0, 1.0 + item.getWeight() * 0.25));
                newItemStatsList.add(item);
                weightTotal += item.getWeight();
            }
        }
        itemStatsList = newItemStatsList;
    }

    protected double getTotalWeight() {
        return weightTotal;
    }

    protected void incrementNextRestart(long increment) {
        this.nextRestart += increment;
    }

    protected void setNextRestart(long nextRestart) {
        this.nextRestart = nextRestart;
    }

    protected long getNextRestart() {
        return nextRestart;
    }

    protected void setGeometricGrowFactor(double geometricGrowFactor) {
        this.geometricGrowFactor = geometricGrowFactor;
    }

    protected double getGeometricGrowFactor() {
        return geometricGrowFactor;
    }

    public int getBestSolutionCount() {
        return bestSolutionCount;
    }

    protected boolean hasMoveIterator() {
        return weightTotal > 0;
    }

    protected void incrementWeightTotal() {
        this.weightTotal++;
    }

    public List<AdaptiveMoveIteratorData<Solution_>> getItemStatsList() {
        return itemStatsList;
    }

    // ************************************************************************
    // lifecycle methods
    // ************************************************************************
    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        this.weightTotal = 0;
        this.geometricGrowFactor = 0;
        this.nextRestart = 0;
        this.bestSolutionCount = 0;
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
            //            log.info("Best solution improved {}, {}", stepScope.getScore(), legacyMoveAdapter.legacyMove());
            adaptiveMoveAdapter.incrementWeight();
            bestSolutionCount++;
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
        log.info("Move weight ({}): {}", weightTotal, this);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public String toString() {
        return itemStatsList.stream()
                .map(i -> "%s=%.2f".formatted(
                        i.getMoveSelector().toString().substring(0, i.getMoveSelector().toString().indexOf("(")),
                        i.getWeight()))
                .collect(joining(";"));
    }
}
