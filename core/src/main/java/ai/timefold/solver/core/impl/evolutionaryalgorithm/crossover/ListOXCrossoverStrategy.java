package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.context.IndividualGenerator;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Default implementation of OX crossover strategy for list variables.
 */
public class ListOXCrossoverStrategy<Solution_, Score_ extends Score<Score_>> implements CrossoverStrategy<Solution_, Score_> {

    private final IndividualGenerator<Solution_, Score_> individualGenerator;

    public ListOXCrossoverStrategy(IndividualGenerator<Solution_, Score_> individualGenerator) {
        this.individualGenerator = individualGenerator;
    }

    @Override
    public Individual<Solution_, Score_> apply(SolverScope<Solution_> solverScope,
            Individual<Solution_, Score_> firstIndividual, Individual<Solution_, Score_> secondIndividual) {
        var innerScoreDirector = solverScope.<Score_> getScoreDirector();
        var offspringSolution = innerScoreDirector.cloneSolution(firstIndividual.getSolution());
        var offspringIndividual =
                individualGenerator.generateIndividual(offspringSolution, firstIndividual.getScore(), innerScoreDirector);

        return offspringIndividual;
    }
}
