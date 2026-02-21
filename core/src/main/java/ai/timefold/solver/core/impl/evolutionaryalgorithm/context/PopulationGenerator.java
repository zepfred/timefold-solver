package ai.timefold.solver.core.impl.evolutionaryalgorithm.context;

import java.util.Random;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;

@FunctionalInterface
public interface PopulationGenerator<Solution_, Score_ extends Score<Score_>> {

    Population<Solution_, Score_> generatePopulation(Random workingRandom, int populationSize, int generationSize,
            int eliteSolutionSize, IndividualGenerator<Solution_, Score_> individualGenerator);
}
