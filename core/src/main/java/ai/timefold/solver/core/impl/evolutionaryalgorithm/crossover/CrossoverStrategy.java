package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Base contract for defining crossover operations.
 */
public interface CrossoverStrategy<Solution_, Score_ extends Score<Score_>> {

    Individual<Solution_, Score_> apply(SolverScope<Solution_> solverScope, Individual<Solution_, Score_> firstIndividual,
            Individual<Solution_, Score_> secondIndividual);
}
