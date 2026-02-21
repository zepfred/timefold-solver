package ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

import org.jspecify.annotations.NullMarked;

/**
 * Basic contract for implementing evolutionary strategies.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
@NullMarked
public sealed interface EvolutionaryStrategy<Solution_, Score_ extends Score<Score_>> extends PhaseLifecycleListener<Solution_>
        permits HybridGeneticSearch {

    /**
     * Creates the initial population, serving as a foundation for the subsequent generations.
     *
     * @return the list of individuals to compose the initial generation
     */
    Population<Solution_, Score_> generateInitialPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    /**
     * The new generation is created using this method, and the logic used will vary according to the strategy.
     * This method will manage all operations related to the evolutionary strategy
     * (genetic search, hybrid genetic search, genetic programming, etc.),
     * such as parent selection, recombination, mutation, and survival selection.
     *
     * @param stepScope the step scope.
     */
    void execute(EvolutionaryAlgorithmPhaseScope<Solution_> stepScope);

}
