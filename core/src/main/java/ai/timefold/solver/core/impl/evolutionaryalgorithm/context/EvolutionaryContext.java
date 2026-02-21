package ai.timefold.solver.core.impl.evolutionaryalgorithm.context;

import ai.timefold.solver.core.api.score.Score;

public interface EvolutionaryContext<Solution_, Score_ extends Score<Score_>>
        extends IndividualGenerator<Solution_, Score_>, PopulationGenerator<Solution_, Score_> {

}
