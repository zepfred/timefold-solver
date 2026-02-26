package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public record CrossoverResult<Solution_, Score_ extends Score<Score_>>(Solution_ offspringSolution,
        InnerScore<Score_> offspringScore) {
}
