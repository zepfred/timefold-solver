package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicRecordingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicReplayingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.SubListMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public final class SubListSelectorFactory<Solution_> extends AbstractFromConfigFactory<Solution_, SubListSelectorConfig> {

    private static final int DEFAULT_MINIMUM_SUB_LIST_SIZE = 1; // TODO Bump this to 2 in Timefold Solver 2.0
    private static final int DEFAULT_MAXIMUM_SUB_LIST_SIZE = Integer.MAX_VALUE;

    private SubListSelectorFactory(SubListSelectorConfig config) {
        super(config);
    }

    public static <Solution_> SubListSelectorFactory<Solution_> create(SubListSelectorConfig subListSelectorConfig) {
        return new SubListSelectorFactory<>(subListSelectorConfig);
    }

    public SubListSelector<Solution_> buildSubListSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> entitySelector, SelectionCacheType minimumCacheType,
            SelectionOrder inheritedSelectionOrder) {
        if (config.getMimicSelectorRef() != null) {
            return buildMimicReplaying(configPolicy);
        }
        if (inheritedSelectionOrder != SelectionOrder.RANDOM) {
            throw new IllegalArgumentException(
                    "The subListSelector (%s) has an inheritedSelectionOrder(%s) which is not supported. SubListSelector only supports random selection order."
                            .formatted(config, inheritedSelectionOrder));
        }

        var valueSelector = buildIterableValueSelector(configPolicy, entitySelector.getEntityDescriptor(),
                minimumCacheType, inheritedSelectionOrder);

        var minimumSubListSize = Objects.requireNonNullElse(config.getMinimumSubListSize(), DEFAULT_MINIMUM_SUB_LIST_SIZE);
        var maximumSubListSize = Objects.requireNonNullElse(config.getMaximumSubListSize(), DEFAULT_MAXIMUM_SUB_LIST_SIZE);
        var baseSubListSelector =
                new RandomSubListSelector<>(entitySelector, valueSelector, minimumSubListSize, maximumSubListSize);

        var subListSelector =
                applyNearbySelection(configPolicy, minimumCacheType, inheritedSelectionOrder, baseSubListSelector);

        subListSelector = applyMimicRecording(configPolicy, subListSelector);

        return subListSelector;
    }

    SubListSelector<Solution_> buildMimicReplaying(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (config.getId() != null
                || config.getMinimumSubListSize() != null
                || config.getMaximumSubListSize() != null
                || config.getValueSelectorConfig() != null
                || config.getNearbySelectionConfig() != null) {
            throw new IllegalArgumentException(
                    "The subListSelectorConfig (%s) with mimicSelectorRef (%s) has another property that is not null."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        SubListMimicRecorder<Solution_> subListMimicRecorder =
                configPolicy.getSubListMimicRecorder(config.getMimicSelectorRef());
        if (subListMimicRecorder == null) {
            throw new IllegalArgumentException(
                    "The subListSelectorConfig (%s) has a mimicSelectorRef (%s) for which no subListSelector with that id exists (in its solver phase)."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        return new MimicReplayingSubListSelector<>(subListMimicRecorder);
    }

    private SubListSelector<Solution_> applyMimicRecording(HeuristicConfigPolicy<Solution_> configPolicy,
            SubListSelector<Solution_> subListSelector) {
        var id = config.getId();
        if (id != null) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException(
                        "The subListSelectorConfig (%s) has an empty id (%s).".formatted(config, id));
            }
            var mimicRecordingSubListSelector = new MimicRecordingSubListSelector<>(subListSelector);
            configPolicy.addSubListMimicRecorder(id, mimicRecordingSubListSelector);
            subListSelector = mimicRecordingSubListSelector;
        }
        return subListSelector;
    }

    private SubListSelector<Solution_> applyNearbySelection(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            RandomSubListSelector<Solution_> subListSelector) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        if (nearbySelectionConfig == null) {
            return subListSelector;
        }
        return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.NEARBY_SELECTION)
                .applyNearbySelection(config, configPolicy, minimumCacheType, resolvedSelectionOrder, subListSelector);
    }

    private IterableValueSelector<Solution_> buildIterableValueSelector(
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder) {
        ValueSelectorConfig valueSelectorConfig = config != null ? config.getValueSelectorConfig() : null;
        if (valueSelectorConfig == null) {
            valueSelectorConfig = new ValueSelectorConfig();
        }
        // Mixed models require that the variable name be set
        if (configPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()
                && valueSelectorConfig.getVariableName() == null) {
            var variableName = entityDescriptor.getGenuineListVariableDescriptor().getVariableName();
            valueSelectorConfig.setVariableName(variableName);
        }
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory
                .<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder);
        return (IterableValueSelector<Solution_>) valueSelector;
    }
}
