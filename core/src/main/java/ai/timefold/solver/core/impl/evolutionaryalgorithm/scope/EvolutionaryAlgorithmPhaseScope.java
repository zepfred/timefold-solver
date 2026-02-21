package ai.timefold.solver.core.impl.evolutionaryalgorithm.scope;

import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class EvolutionaryAlgorithmPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private EvolutionaryAlgorithmStepScope<Solution_> lastCompletedStepScope;
    private Population<Solution_, ?> population;

    public EvolutionaryAlgorithmPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex);
        this.lastCompletedStepScope = new EvolutionaryAlgorithmStepScope<>(this, 0, null);
    }

    @Override
    public AbstractStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    public Population<Solution_, ?> getPopulation() {
        return population;
    }

    public void setPopulation(Population<Solution_, ?> population) {
        this.population = population;
    }

}
