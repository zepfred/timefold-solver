package ai.timefold.solver.core.impl.evolutionaryalgorithm.scope;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public final class EvolutionaryAlgorithmStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope;
    private Individual<Solution_, ?> bestIndividual;

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex(), null);
    }

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

    public <Score_ extends Score<Score_>> void setBestIndividual(Individual<Solution_, Score_> bestIndividual) {
        this.bestIndividual = bestIndividual;
    }

    @Override
    public EvolutionaryAlgorithmPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    @Override
    public Solution_ cloneWorkingSolution() {
        return getScoreDirector().cloneSolution(bestIndividual.getSolution());
    }
}
