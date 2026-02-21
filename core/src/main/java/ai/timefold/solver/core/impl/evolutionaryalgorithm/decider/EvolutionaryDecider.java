package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

/**
 * Basic contract for implementing evolutionary strategies.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
@NullMarked
public abstract sealed class EvolutionaryDecider<Solution_, Score_ extends Score<Score_>>
        implements PhaseLifecycleListener<Solution_> permits HybridGeneticSearchDecider {

    /**
     * Creates the initial population, serving as a foundation for the subsequent generations.
     *
     * @return the list of individuals to compose the initial generation
     */
    public abstract Population<Solution_, Score_>
            generateInitialPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    /**
     * The population is updated using this method, and the logic used will vary according to the evolutionary strategy.
     * This method will manage all operations related to the evolutionary strategy
     * (genetic search, hybrid genetic search, genetic programming, etc.),
     * such as parent selection, recombination, mutation, and survival selection.
     *
     * @param stepScope the step scope.
     */
    public abstract void evolvePopulation(EvolutionaryAlgorithmStepScope<Solution_> stepScope);

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> abstractPhaseScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> abstractPhaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> abstractStepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> abstractStepScope) {
        // Do nothing
    }
}
