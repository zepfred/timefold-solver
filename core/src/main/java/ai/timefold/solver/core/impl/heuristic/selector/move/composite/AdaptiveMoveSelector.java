package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.time.Clock;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public class AdaptiveMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private final AdaptiveMoveSelectorStats<Solution_> stats;
    private final Clock clock;

    public AdaptiveMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, Clock clock) {
        super(childMoveSelectorList, true);
        this.stats = new AdaptiveMoveSelectorStats<>(childMoveSelectorList);
        this.clock = clock;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        stats.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        stats.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        stats.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        stats.phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        return childMoveSelectorList.stream().anyMatch(MoveSelector::isNeverEnding);
    }

    @Override
    public long getSize() {
        return childMoveSelectorList.stream().mapToLong(MoveSelector::getSize).sum();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new AdaptiveMoveIterator<>(stats, this.workingRandom, clock);
    }

    @Override
    public String toString() {
        return "Adaptive(" + childMoveSelectorList + ")";
    }

}
