package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public class AdaptiveMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private final AdaptiveMoveProbabilityManager<Solution_> adaptiveMoveProbabilityManager;

    public AdaptiveMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList) {
        super(childMoveSelectorList, true);
        this.adaptiveMoveProbabilityManager = new AdaptiveMoveProbabilityManager<>(childMoveSelectorList);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        adaptiveMoveProbabilityManager.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        adaptiveMoveProbabilityManager.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        adaptiveMoveProbabilityManager.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        adaptiveMoveProbabilityManager.phaseEnded(phaseScope);
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
        return new AdaptiveUnionMoveIterator<>(adaptiveMoveProbabilityManager, workingRandom);
    }

    @Override
    public String toString() {
        return "Adaptive(" + childMoveSelectorList + ")";
    }

}
