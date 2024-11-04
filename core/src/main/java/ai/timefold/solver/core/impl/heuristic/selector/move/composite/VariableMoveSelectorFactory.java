package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.VariableMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.decider.refinement.LocalSearchRefinementConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class VariableMoveSelectorFactory<Solution_>
        extends AbstractCompositeMoveSelectorFactory<Solution_, VariableMoveSelectorConfig> {

    public VariableMoveSelectorFactory(VariableMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var moveSelectorConfigList = new LinkedList<>(config.getMoveSelectorList());
        if (configPolicy.getNearbyDistanceMeterClass() != null) {
            for (var selectorConfig : config.getMoveSelectorList()) {
                if (selectorConfig instanceof NearbyAutoConfigurationEnabled nearbySelectorConfig) {
                    if (selectorConfig.hasNearbySelectionConfig()) {
                        throw new IllegalArgumentException(
                                """
                                        The selector configuration (%s) already includes the Nearby Selection setting, making it incompatible with the top-level property nearbyDistanceMeterClass (%s).
                                        Remove the Nearby setting from the selector configuration or remove the top-level nearbyDistanceMeterClass."""
                                        .formatted(nearbySelectorConfig, configPolicy.getNearbyDistanceMeterClass()));
                    }
                    // We delay the autoconfiguration to the deepest UnionMoveSelectorConfig node in the tree
                    // to avoid duplicating configuration
                    // when there are nested unionMoveSelector configurations
                    if (selectorConfig instanceof UnionMoveSelectorConfig) {
                        continue;
                    }
                    // Add a new configuration with Nearby Selection enabled
                    moveSelectorConfigList
                            .add(nearbySelectorConfig.enableNearbySelection(configPolicy.getNearbyDistanceMeterClass(),
                                    configPolicy.getRandom()));

                }
            }
        }
        var maxIterationMultiplier = Optional.ofNullable(config.getRefinementConfig())
                .map(LocalSearchRefinementConfig::getMaxIterationsMultiplier)
                .orElse(2);
        List<MoveSelector<Solution_>> moveSelectorList =
                buildInnerMoveSelectors(moveSelectorConfigList, configPolicy, minimumCacheType, randomSelection);
        return new VariableMoveSelector<>(moveSelectorList, maxIterationMultiplier);
    }
}
