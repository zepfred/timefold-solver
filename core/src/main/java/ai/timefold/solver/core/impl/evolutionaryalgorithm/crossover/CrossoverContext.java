package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import java.util.Random;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public record CrossoverContext<Solution_, Score_ extends Score<Score_>>(InnerScoreDirector<Solution_, Score_> scoreDirector,
        Random workingRandom, Individual<Solution_, Score_> firstIndividual, Individual<Solution_, Score_> secondIndividual) {

}
