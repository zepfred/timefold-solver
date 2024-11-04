package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * A {@link CompositeMoveSelector} that combines two or more {@link MoveSelector}s and applies a strategy based on the
 * Variable Neighborhood Descent.
 * <p>
 * The objective is to generate random moves within a neighborhood until no further improvement is observed for a defined
 * maximum number of iterations. When there is no improvement, the next available neighborhood is chosen. The process continues
 * until no improvements are found in all available neighborhoods. The counter resets when an improvement is found, and it
 * finishes when all neighborhoods are evaluated without an improvement.
 * <p>
 * Warning: there is no duplicated {@link Move} check, so union of {A, B, C} and {B, D} will result in {A, B, C, B, D}.
 *
 * @see CompositeMoveSelector
 */
public class VariableMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private final int maxIterationsMultiplier;
    private int maxIterations;
    protected SolverScope<Solution_> solverScope;
    private Iterator<Move<Solution_>> currentMoveIterator;

    public VariableMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, int maxIterationsMultiplier) {
        super(childMoveSelectorList, true);
        this.maxIterationsMultiplier = maxIterationsMultiplier;
        if (childMoveSelectorList.size() < 2) {
            throw new IllegalArgumentException("The selector (%s) must have at least two moves.".formatted(this));
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.solverScope = phaseScope.getSolverScope();
        this.maxIterations = (int) (maxIterationsMultiplier
                * solverScope.getSolutionDescriptor().getApproximateValueCount(solverScope.getBestSolution()));
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        currentMoveIterator = new VariableMoveIterator<>(solverScope, childMoveSelectorList, maxIterations);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.solverScope = null;
        this.currentMoveIterator = null;
        this.maxIterations = -1;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public long getSize() {
        // At least maxIterations
        return maxIterations;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return currentMoveIterator;
    }

    @Override
    public String toString() {
        return "Variable(" + childMoveSelectorList + ")";
    }

}
