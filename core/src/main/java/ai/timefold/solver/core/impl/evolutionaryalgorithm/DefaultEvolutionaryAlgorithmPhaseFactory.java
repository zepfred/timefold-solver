package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import static ai.timefold.solver.core.impl.AbstractFromConfigFactory.deduceEntityDescriptor;

import java.util.ArrayList;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicForagerConfig;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicPickEarlyType;
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryAlgorithmPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacerFactory;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy.EvolutionaryStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy.HybridGeneticSearch;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy.HybridSearchConfiguration;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.swapstar.ListSwapStarPhase;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public class DefaultEvolutionaryAlgorithmPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, EvolutionaryAlgorithmPhaseConfig> {

    public DefaultEvolutionaryAlgorithmPhaseFactory(EvolutionaryAlgorithmPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public EvolutionaryAlgorithmPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        if (solverConfigPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()) {
            throw new UnsupportedOperationException("The evolutionary algorithm cannot be applied to mixed models.");
        }
        var constructionHeuristicPhase = buildConstructionHeuristicPhase(solverConfigPolicy, solverTermination);
        var hasListVariable = solverConfigPolicy.getSolutionDescriptor().hasListVariable();
        var localSearchPhase =
                buildLocalSearchPhase(solverConfigPolicy, solverTermination, bestSolutionRecaller, hasListVariable);
        var swapStarPhase = buildSwapStarPhase(solverConfigPolicy, solverTermination);
        var evolutionaryStrategy = buildEvolutionaryAlgorithmStrategy(phaseConfig, constructionHeuristicPhase, localSearchPhase,
                swapStarPhase, bestSolutionRecaller);
        return new DefaultEvolutionaryAlgorithmPhase.Builder<>(phaseIndex, "",
                buildPhaseTermination(solverConfigPolicy, solverTermination), evolutionaryStrategy).build();
    }

    /**
     * The method creates a construction heuristic phase using a first-fit and random value selector
     * to allow for faster generation of different solutions
     */
    private static <Solution_> Phase<Solution_> buildConstructionHeuristicPhase(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, SolverTermination<Solution_> solverTermination) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        var entityPlacerConfig = DefaultConstructionHeuristicPhaseFactory.buildDefaultEntityPlacerConfig(solverConfigPolicy,
                constructionHeuristicPhaseConfig, ConstructionHeuristicType.FIRST_FIT);
        shuffleEntityPlacerConfig(solverConfigPolicy, entityPlacerConfig);
        constructionHeuristicPhaseConfig.setEntityPlacerConfig(entityPlacerConfig);
        constructionHeuristicPhaseConfig.setForagerConfig(new ConstructionHeuristicForagerConfig()
                .withPickEarlyType(ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD));
        return PhaseFactory.<Solution_> create(constructionHeuristicPhaseConfig).buildPhase(0, false,
                solverConfigPolicy, null, solverTermination);
    }

    /**
     * The method ensures that the source entity or source value selectors from the entity placer selector is shuffled,
     * allowing for the generation of different solutions whenever the phase is restarted.
     * <p>
     * The proposed approach avoids shuffling the move selector,
     * eliminating the need to generate the entire move list upfront and then randomize it.
     */
    private static <Solution_> void shuffleEntityPlacerConfig(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            EntityPlacerConfig<?> entityPlacerConfig) {
        if (entityPlacerConfig instanceof QueuedEntityPlacerConfig queuedEntityPlacerConfig) {
            // Basic variable, then we randomize the entity selector
            var entitySelectorConfig = Objects.requireNonNullElseGet(queuedEntityPlacerConfig.getEntitySelectorConfig(),
                    () -> QueuedEntityPlacerFactory.buildEntitySelectorConfig(solverConfigPolicy, queuedEntityPlacerConfig));
            var entityDescriptor =
                    deduceEntityDescriptor(solverConfigPolicy, entitySelectorConfig, entitySelectorConfig.getEntityClass());
            queuedEntityPlacerConfig.setEntitySelectorConfig(entitySelectorConfig);
            shuffleEntitySelectorConfig(entitySelectorConfig);
            var moveSelectorConfigList = Objects.requireNonNullElseGet(queuedEntityPlacerConfig.getMoveSelectorConfigList(),
                    () -> QueuedEntityPlacerFactory.buildMoveSelectorConfig(solverConfigPolicy, queuedEntityPlacerConfig,
                            entityDescriptor, entitySelectorConfig));
            if (moveSelectorConfigList.size() != 1) {
                throw new IllegalStateException(
                        "Impossible state: the move configuration list %s cannot be empty or contain multiple items."
                                .formatted(moveSelectorConfigList));
            }
            queuedEntityPlacerConfig.setMoveSelectorConfigList(moveSelectorConfigList);
            var moveSelectorConfig = Objects.requireNonNull(moveSelectorConfigList.get(0));
            if (moveSelectorConfig instanceof ChangeMoveSelectorConfig changeMoveSelectorConfig) {
                shuffleValueSelectorConfig(changeMoveSelectorConfig.getValueSelectorConfig());
            } else if (moveSelectorConfig instanceof CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig) {
                for (var innerMoveSelectorConfig : Objects
                        .requireNonNull(cartesianProductMoveSelectorConfig.getMoveSelectorList())) {
                    if (!(innerMoveSelectorConfig instanceof ChangeMoveSelectorConfig changeMoveSelectorConfig)) {
                        throw new IllegalStateException(
                                "Impossible state: the inner move configration (%s) must match the type (%s)"
                                        .formatted(innerMoveSelectorConfig, ChangeMoveSelectorConfig.class.getSimpleName()));
                    }
                    shuffleValueSelectorConfig(changeMoveSelectorConfig.getValueSelectorConfig());
                }
            } else {
                throw new IllegalStateException("Impossible state: the move configration (%s) must match the types (%s, %s)"
                        .formatted(moveSelectorConfig, ChangeMoveSelectorConfig.class.getSimpleName(),
                                CartesianProductMoveSelectorConfig.class.getSimpleName()));
            }
        } else if (entityPlacerConfig instanceof QueuedValuePlacerConfig queuedValuePlacerConfig) {
            // List variable, then we shuffle the source value selector first
            var valueSelectorConfig = Objects.requireNonNull(queuedValuePlacerConfig.getValueSelectorConfig());
            shuffleValueSelectorConfig(valueSelectorConfig);
            // The move list has only one list change move
            var moveSelectorConfig = Objects.requireNonNull(queuedValuePlacerConfig.getMoveSelectorConfig());
            if (!(moveSelectorConfig instanceof ListChangeMoveSelectorConfig listChangeMoveSelectorConfig)) {
                throw new IllegalStateException("Impossible state: the move configration (%s) must match the type (%s)"
                        .formatted(moveSelectorConfig, ListChangeMoveSelectorConfig.class.getSimpleName()));
            }
            var destinationSelectorConfig = Objects.requireNonNullElseGet(
                    listChangeMoveSelectorConfig.getDestinationSelectorConfig(), DestinationSelectorConfig::new);
            listChangeMoveSelectorConfig.setDestinationSelectorConfig(destinationSelectorConfig);
            var entitySelectorConfig = Objects.requireNonNullElseGet(destinationSelectorConfig.getEntitySelectorConfig(),
                    EntitySelectorConfig::new);
            destinationSelectorConfig.setEntitySelectorConfig(entitySelectorConfig);
            // we shuffle the entity selector used by the destination selector
            shuffleEntitySelectorConfig(destinationSelectorConfig.getEntitySelectorConfig());
        }
    }

    private static void shuffleEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        Objects.requireNonNull(entitySelectorConfig).setSelectionOrder(SelectionOrder.SHUFFLED);
        Objects.requireNonNull(entitySelectorConfig).setCacheType(SelectionCacheType.PHASE);
    }

    private static void shuffleValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        Objects.requireNonNull(valueSelectorConfig).setSelectionOrder(SelectionOrder.SHUFFLED);
        Objects.requireNonNull(valueSelectorConfig).setCacheType(SelectionCacheType.PHASE);
    }

    /**
     * The method creates a local search phase based on Variable Neighborhood Descent (VND).
     */
    private static <Solution_> Phase<Solution_> buildLocalSearchPhase(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            SolverTermination<Solution_> solverTermination, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            boolean isListVariable) {
        var localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig.withLocalSearchType(LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT);
        loadMoveSelectorConfig(solverConfigPolicy, localSearchPhaseConfig, isListVariable);
        return PhaseFactory.<Solution_> create(localSearchPhaseConfig).buildPhase(0, false,
                solverConfigPolicy.copyConfigPolicyWithoutNearbySetting(), bestSolutionRecaller, solverTermination);
    }

    /**
     * The method adds all moves according to the original article:
     * 2.1 - Reallocate planning value U after a planning value V (regular change and list change moves)
     * 2.2 - Swap planning value U with a planning value V (regular swap and list swap moves)
     * 2.3 - Reallocate planning value U and its successor X after a planning value V: (U, X) -> Entity[position]
     * 2.4 - Reallocate planning value U and its successor X after a planning value V, and invert the values: (X, U) ->
     * Entity[position]
     * 2.5 - Swap two planning values U and X with a planning value: (U, X) <-> V
     * 2.6 - Swap two planning values U and X with a planning value V, and invert the values: (X, U) <-> V
     * 2.7 - Intra route 2-opt move: (U, X) (V, Y) -> (U, V) (X, Y)
     * 2.8 - Inter route 2-opt move: (U, X) (V, Y) -> (U, V) (X, Y)
     * 2.9 - Inter route 2-opt move: (U, X) (V, Y) -> (U, Y) (V, X)
     */
    private static <Solution_> void loadMoveSelectorConfig(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            LocalSearchPhaseConfig localSearchPhaseConfig, boolean isListVariable) {
        var unionMoveSelectorConfig = new UnionMoveSelectorConfig();
        var moveList = new ArrayList<MoveSelectorConfig>();
        if (isListVariable) {
            // Move 2.1
            moveList.add(new ListChangeMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL));
            // Move 2.2
            moveList.add(new ListSwapMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL));
            // Move 2.3
            moveList.add(new SubListChangeMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL)
                    .withSelectReversingMoveToo(false).withSubListSelectorConfig(
                            new SubListSelectorConfig().withOnlyConsecutive(true).withMinimumSubListSize(2)
                                    .withMaximumSubListSize(2)));
            // Move 2.4
            moveList.add(new SubListChangeMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL)
                    .withSelectReversingMoveToo(true).withSubListSelectorConfig(
                            new SubListSelectorConfig().withOnlyConsecutive(true).withMinimumSubListSize(2)
                                    .withMaximumSubListSize(2)));
            // Move 2.5
            moveList.add(new SubListSwapMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL)
                    .withSelectReversingMoveToo(false)
                    .withSubListSelectorConfig(new SubListSelectorConfig().withOnlyConsecutive(true).withMinimumSubListSize(2)
                            .withMaximumSubListSize(2))
                    .withSecondarySubListSelectorConfig(new SubListSelectorConfig().withOnlyConsecutive(true)
                            .withMinimumSubListSize(1).withMaximumSubListSize(1)));
            // Move 2.6
            moveList.add(new SubListSwapMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL)
                    .withSelectReversingMoveToo(true)
                    .withSubListSelectorConfig(new SubListSelectorConfig().withOnlyConsecutive(true).withMinimumSubListSize(2)
                            .withMaximumSubListSize(2))
                    .withSecondarySubListSelectorConfig(new SubListSelectorConfig().withOnlyConsecutive(true)
                            .withMinimumSubListSize(1).withMaximumSubListSize(1)));
            // Moves 2.7, 2.8 and 2.9
            moveList.add(new KOptListMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL).withMinimumK(2)
                    .withMaximumK(2));
        } else {
            // Move 2.1
            moveList.add(new ChangeMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL));
            // Move 2.2
            moveList.add(new SwapMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL));
        }
        unionMoveSelectorConfig.setMoveSelectorList(moveList);
        // Enable the granular neighborhood
        if (solverConfigPolicy.getNearbyDistanceMeterClass() != null) {
            var nearbyDistanceMeterClass =
                    (Class<? extends NearbyDistanceMeter<?, ?>>) solverConfigPolicy.getNearbyDistanceMeterClass();
            unionMoveSelectorConfig =
                    unionMoveSelectorConfig.enableNearbySelection(nearbyDistanceMeterClass, solverConfigPolicy.getRandom());
            var updatedList = unionMoveSelectorConfig.getMoveSelectorList().stream()
                    .filter(SelectorConfig::hasNearbySelectionConfig).toList();
            // We only use the moves with Nearby enabled as proposed by FILO
            // A Fast and Scalable Heuristic for the Solution of Large-Scale Capacitated Vehicle Routing Problems
            unionMoveSelectorConfig.setMoveSelectorList(updatedList);
        }
        localSearchPhaseConfig.setMoveSelectorConfig(unionMoveSelectorConfig);
    }

    /**
     * The method creates an optimization phase to implement the SWAP* approach as outlined in the HGS article.
     */
    private <Solution_> Phase<Solution_> buildSwapStarPhase(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            SolverTermination<Solution_> solverTermination) {
        if (solverConfigPolicy.getSolutionDescriptor().hasListVariable()
                && solverConfigPolicy.getNearbyDistanceMeterClass() != null) {
            var entityClass = solverConfigPolicy.getSolutionDescriptor().getListVariableDescriptor().getEntityDescriptor()
                    .getEntityClass();
            var originalEntitySelectorConfig = new EntitySelectorConfig()
                    .withId(ConfigUtils.addRandomSuffix(entityClass.getName(), solverConfigPolicy.getRandom()))
                    .withEntityClass(entityClass);
            var originalEntitySelector = EntitySelectorFactory.<Solution_> create(originalEntitySelectorConfig)
                    .buildEntitySelector(solverConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL);
            var innerEntitySelectorConfig = new EntitySelectorConfig()
                    .withNearbySelectionConfig(new NearbySelectionConfig()
                            .withOriginEntitySelectorConfig(
                                    EntitySelectorConfig.newMimicSelectorConfig(originalEntitySelectorConfig.getId()))
                            .withNearbyDistanceMeterClass(solverConfigPolicy.getNearbyDistanceMeterClass()));
            var innerEntitySelector = EntitySelectorFactory.<Solution_> create(innerEntitySelectorConfig)
                    .buildEntitySelector(solverConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL);

            return new ListSwapStarPhase.Builder<>(0, "", PhaseTermination.bridge(solverTermination), originalEntitySelector,
                    innerEntitySelector).build();
        }
        return null;
    }

    /**
     * The method creates the evolutionary strategy used by the algorithm.
     * By default, the Hybrid Genetic Search approach is used.
     */
    private static <Solution_, Score_ extends Score<Score_>> EvolutionaryStrategy<Solution_, Score_>
            buildEvolutionaryAlgorithmStrategy(
                    EvolutionaryAlgorithmPhaseConfig phaseConfig, Phase<Solution_> constructionHeuristicPhase,
                    Phase<Solution_> localSearchPhase, Phase<Solution_> swapStarPhase,
                    BestSolutionRecaller<Solution_> bestSolutionRecaller) {
        // Same as the original paper
        var populationSize = Objects.requireNonNullElse(phaseConfig.getPopulationSize(), 25);
        var generationSize = Objects.requireNonNullElse(phaseConfig.getGenerationSize(), 40);
        var eliteGroupSize = Objects.requireNonNullElse(phaseConfig.getEliteSolutionSize(), 5);
        var configuration = new HybridSearchConfiguration(populationSize, generationSize, eliteGroupSize);
        return TimefoldSolverEnterpriseService.loadOrDefault(
                service -> service.buildEvolutionaryStrategy(configuration, constructionHeuristicPhase, localSearchPhase,
                        swapStarPhase, bestSolutionRecaller),
                () -> new HybridGeneticSearch<>(configuration, constructionHeuristicPhase, localSearchPhase, swapStarPhase,
                        bestSolutionRecaller));
    }

}
