package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.AdaptiveMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class AdaptiveMoveSelectorFactory<Solution_>
        extends AbstractCompositeMoveSelectorFactory<Solution_, AdaptiveMoveSelectorConfig> {

    public AdaptiveMoveSelectorFactory(AdaptiveMoveSelectorConfig moveSelectorConfig) {
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
        List<MoveSelector<Solution_>> moveSelectorList =
                buildInnerMoveSelectors(moveSelectorConfigList, configPolicy, minimumCacheType, randomSelection);

        return new AdaptiveMoveSelector<>(moveSelectorList);
    }
}
