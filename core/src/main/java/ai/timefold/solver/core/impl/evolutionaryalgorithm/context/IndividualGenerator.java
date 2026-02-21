package ai.timefold.solver.core.impl.evolutionaryalgorithm.context;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

@FunctionalInterface
public interface IndividualGenerator<Solution_, Score_ extends Score<Score_>> {

    Individual<Solution_, Score_> generateIndividual(Solution_ solution, InnerScore<Score_> score,
            InnerScoreDirector<Solution_, Score_> scoreDirector);
}
