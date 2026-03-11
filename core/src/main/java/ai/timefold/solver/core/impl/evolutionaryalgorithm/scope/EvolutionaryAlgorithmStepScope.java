package ai.timefold.solver.core.impl.evolutionaryalgorithm.scope;

import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public final class EvolutionaryAlgorithmStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope;
    private Solution_ bestSolution;

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex(), null);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, Solution_ bestSolution) {
        this(phaseScope, phaseScope.getNextStepIndex(), bestSolution);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, int stepIndex,
            Solution_ bestSolution) {
        super(stepIndex);
        this.phaseScope = phaseScope;
        this.bestSolution = bestSolution;
    }

    public void setBestSolution(Solution_ bestSolution) {
        this.bestSolution = bestSolution;
    }

    @Override
    public EvolutionaryAlgorithmPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    @Override
    public Solution_ cloneWorkingSolution() {
        return getScoreDirector().cloneSolution(bestSolution);
    }
}
