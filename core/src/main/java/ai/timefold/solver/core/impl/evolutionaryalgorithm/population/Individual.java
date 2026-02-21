package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Basic representation of an individual.
 */
public interface Individual<Solution_, Score_ extends Score<Score_>> extends Comparable<Individual<Solution_, Score_>> {

    /**
     * The solution representation of the individual.
     */
    Solution_ getSolution();

    /**
     * Calculates the difference between two individuals according to some strategy.
     *
     * @param otherIndividual the other individual
     * @return a double number where a higher value reflects a greater difference between the two individuals.
     */
    double diff(Individual<Solution_, Score_> otherIndividual);

    /**
     * The method analyzes the feasibility based on the hard score of the solution.
     *
     * @return true if the individual is feasible.
     */
    boolean isFeasiable();

    /**
     * The solution score without any adjustment.
     */
    InnerScore<Score_> getScore();
}
