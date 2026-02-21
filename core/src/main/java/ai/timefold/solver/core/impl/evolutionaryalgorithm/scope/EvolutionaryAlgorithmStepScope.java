package ai.timefold.solver.core.impl.evolutionaryalgorithm.scope;

import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public final class EvolutionaryAlgorithmStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope;
    private Individual<Solution_, ?> bestIndividual;

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope,
            Individual<Solution_, ?> bestIndividual) {
        this(phaseScope, phaseScope.getNextStepIndex(), bestIndividual);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, int stepIndex,
            Individual<Solution_, ?> bestIndividual) {
        super(stepIndex);
        this.phaseScope = phaseScope;
        this.bestIndividual = bestIndividual;
    }

    public Individual<Solution_, ?> getBestIndividual() {
        return bestIndividual;
    }

    public void setBestIndividual(Individual<Solution_, ?> bestIndividual) {
        this.bestIndividual = bestIndividual;
    }

    @Override
    public AbstractPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    @Override
    public Solution_ cloneWorkingSolution() {
        return getScoreDirector().cloneSolution(bestIndividual.getSolution());
    }
}
