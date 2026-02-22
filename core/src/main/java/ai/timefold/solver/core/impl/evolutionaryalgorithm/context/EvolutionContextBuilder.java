package ai.timefold.solver.core.impl.evolutionaryalgorithm.context;

import java.util.Random;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.ListOXCrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.DefaultPopulation;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.ListVariableIndividual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract sealed class EvolutionContextBuilder<Solution_, Score_ extends Score<Score_>> {

    final boolean hasListVariable;

    private EvolutionContextBuilder(boolean hasListVariable) {
        this.hasListVariable = hasListVariable;
    }

    public abstract EvolutionaryContext<Solution_, Score_> build();

    public static <Solution_, Score_ extends Score<Score_>> EvolutionContextBuilder<Solution_, Score_>
            builderHGS(boolean hasListVariable, Phase<Solution_> constructionHeuristicPhase, Phase<Solution_> localSearchPhase,
                    @Nullable Phase<Solution_> swapStarPhase) {
        return new HGSEvolutionaryContextBuilder<>(hasListVariable, constructionHeuristicPhase, localSearchPhase,
                swapStarPhase);
    }

    private static final class HGSEvolutionaryContextBuilder<Solution_, Score_ extends Score<Score_>>
            extends EvolutionContextBuilder<Solution_, Score_> {

        private final Phase<Solution_> constructionHeuristicPhase;
        private final Phase<Solution_> localSearchPhase;
        private final @Nullable Phase<Solution_> swapStarPhase;

        private HGSEvolutionaryContextBuilder(boolean hasListVariable, Phase<Solution_> constructionHeuristicPhase,
                Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> swapStarPhase) {
            super(hasListVariable);
            this.constructionHeuristicPhase = constructionHeuristicPhase;
            this.localSearchPhase = localSearchPhase;
            this.swapStarPhase = swapStarPhase;
        }

        public EvolutionaryContext<Solution_, Score_> build() {
            if (hasListVariable) {
                return new HGSEvolutionaryContext<>() {

                    private @Nullable CrossoverStrategy<Solution_, Score_> crossoverStrategy;

                    @Override
                    public Phase<Solution_> getConstructionHeuristicPhase() {
                        return constructionHeuristicPhase;
                    }

                    @Override
                    public Phase<Solution_> getLocalSearchPhase() {
                        return localSearchPhase;
                    }

                    @Override
                    public @Nullable Phase<Solution_> getSwapStarPhase() {
                        return swapStarPhase;
                    }

                    @Override
                    public CrossoverStrategy<Solution_, Score_> getCrossoverStrategy() {
                        if (crossoverStrategy != null) {
                            return crossoverStrategy;
                        }
                        crossoverStrategy = new ListOXCrossoverStrategy<>(this);
                        return crossoverStrategy;
                    }

                    @Override
                    public Individual<Solution_, Score_> generateIndividual(Solution_ solution, InnerScore<Score_> score,
                            InnerScoreDirector<Solution_, Score_> scoreDirector) {
                        return new ListVariableIndividual<>(solution, score, scoreDirector);
                    }

                    @Override
                    public Population<Solution_, Score_> generatePopulation(Random workingRandom, int populationSize,
                            int generationSize, int eliteSolutionSize,
                            IndividualGenerator<Solution_, Score_> individualGenerator) {
                        return new DefaultPopulation<>(workingRandom, populationSize, generationSize, eliteSolutionSize,
                                individualGenerator);
                    }
                };
            } else {
                throw new UnsupportedOperationException("Basic variables are not supported yet.");
            }
        }
    }
}
