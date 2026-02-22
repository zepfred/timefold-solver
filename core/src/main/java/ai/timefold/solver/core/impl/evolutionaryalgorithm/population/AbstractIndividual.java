package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

abstract sealed class AbstractIndividual<Solution_, Score_ extends Score<Score_>> implements Individual<Solution_, Score_>
        permits ListVariableIndividual {

    protected final Solution_ solution;
    protected final InnerScore<Score_> score;

    protected AbstractIndividual(Solution_ solution, InnerScore<Score_> score) {
        this.solution = solution;
        this.score = score;
    }

    @Override
    public Solution_ getSolution() {
        return solution;
    }

    @Override
    public boolean isFeasible() {
        return score.raw().isFeasible();
    }

    @Override
    public InnerScore<Score_> getScore() {
        return score;
    }

    @Override
    public int compareTo(Individual<Solution_, Score_> otherIndividual) {
        return score.compareTo(otherIndividual.getScore());
    }
}
