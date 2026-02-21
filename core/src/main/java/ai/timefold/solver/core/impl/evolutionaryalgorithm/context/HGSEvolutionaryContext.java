package ai.timefold.solver.core.impl.evolutionaryalgorithm.context;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.phase.Phase;

public interface HGSEvolutionaryContext<Solution_, Score_ extends Score<Score_>>
        extends EvolutionaryContext<Solution_, Score_> {

    Phase<Solution_> getConstructionHeuristicPhase();

    Phase<Solution_> getLocalSearchPhase();

    Phase<Solution_> getSwapStarPhase();

    CrossoverStrategy<Solution_, Score_> getCrossoverStrategy();
}
