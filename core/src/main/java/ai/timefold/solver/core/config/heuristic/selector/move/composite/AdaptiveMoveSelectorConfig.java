package ai.timefold.solver.core.config.heuristic.selector.move.composite;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "moveSelectorConfigList"
})
public class AdaptiveMoveSelectorConfig
        extends MoveSelectorConfig<AdaptiveMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<AdaptiveMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "adaptiveMoveSelector";

    @XmlElements({
            @XmlElement(name = CartesianProductMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = CartesianProductMoveSelectorConfig.class),
            @XmlElement(name = ChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ChangeMoveSelectorConfig.class),
            @XmlElement(name = KOptListMoveSelectorConfig.XML_ELEMENT_NAME, type = KOptListMoveSelectorConfig.class),
            @XmlElement(name = ListChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ListChangeMoveSelectorConfig.class),
            @XmlElement(name = ListSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = ListSwapMoveSelectorConfig.class),
            @XmlElement(name = MoveIteratorFactoryConfig.XML_ELEMENT_NAME, type = MoveIteratorFactoryConfig.class),
            @XmlElement(name = MoveListFactoryConfig.XML_ELEMENT_NAME, type = MoveListFactoryConfig.class),
            @XmlElement(name = PillarChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = PillarChangeMoveSelectorConfig.class),
            @XmlElement(name = PillarSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = PillarSwapMoveSelectorConfig.class),
            @XmlElement(name = RuinRecreateMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = RuinRecreateMoveSelectorConfig.class),
            @XmlElement(name = ListRuinRecreateMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = ListRuinRecreateMoveSelectorConfig.class),
            @XmlElement(name = SubChainChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainChangeMoveSelectorConfig.class),
            @XmlElement(name = SubChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainSwapMoveSelectorConfig.class),
            @XmlElement(name = SubListChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = SubListChangeMoveSelectorConfig.class),
            @XmlElement(name = SubListSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SubListSwapMoveSelectorConfig.class),
            @XmlElement(name = SwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SwapMoveSelectorConfig.class),
            @XmlElement(name = TailChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = TailChainSwapMoveSelectorConfig.class),
            @XmlElement(name = AdaptiveMoveSelectorConfig.XML_ELEMENT_NAME, type = AdaptiveMoveSelectorConfig.class)
    })
    private List<MoveSelectorConfig> moveSelectorConfigList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public AdaptiveMoveSelectorConfig() {
    }

    public AdaptiveMoveSelectorConfig(@NonNull List<@NonNull MoveSelectorConfig> moveSelectorConfigList) {
        this.moveSelectorConfigList = moveSelectorConfigList;
    }

    /**
     * @deprecated Prefer {@link #getMoveSelectorList()}.
     * @return sometimes null
     */
    @Deprecated
    public List<MoveSelectorConfig> getMoveSelectorConfigList() {
        return getMoveSelectorList();
    }

    /**
     * @deprecated Prefer {@link #setMoveSelectorList(List)}.
     * @param moveSelectorConfigList sometimes null
     */
    @Deprecated
    public void setMoveSelectorConfigList(List<MoveSelectorConfig> moveSelectorConfigList) {
        setMoveSelectorList(moveSelectorConfigList);
    }

    public @Nullable List<@NonNull MoveSelectorConfig> getMoveSelectorList() {
        return moveSelectorConfigList;
    }

    public void setMoveSelectorList(@Nullable List<@NonNull MoveSelectorConfig> moveSelectorConfigList) {
        this.moveSelectorConfigList = moveSelectorConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull AdaptiveMoveSelectorConfig
            withMoveSelectorList(@NonNull List<@NonNull MoveSelectorConfig> moveSelectorConfigList) {
        this.moveSelectorConfigList = moveSelectorConfigList;
        return this;
    }

    public @NonNull AdaptiveMoveSelectorConfig withMoveSelectors(@NonNull MoveSelectorConfig @NonNull... moveSelectorConfigs) {
        this.moveSelectorConfigList = Arrays.asList(moveSelectorConfigs);
        return this;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void extractLeafMoveSelectorConfigsIntoList(@NonNull List<@NonNull MoveSelectorConfig> leafMoveSelectorConfigList) {
        for (MoveSelectorConfig moveSelectorConfig : moveSelectorConfigList) {
            moveSelectorConfig.extractLeafMoveSelectorConfigsIntoList(leafMoveSelectorConfigList);
        }
    }

    @Override
    public @NonNull AdaptiveMoveSelectorConfig inherit(@NonNull AdaptiveMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        moveSelectorConfigList =
                ConfigUtils.inheritMergeableListConfig(moveSelectorConfigList, inheritedConfig.getMoveSelectorList());
        return this;
    }

    @Override
    public @NonNull AdaptiveMoveSelectorConfig copyConfig() {
        return new AdaptiveMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (moveSelectorConfigList != null) {
            moveSelectorConfigList.forEach(ms -> ms.visitReferencedClasses(classVisitor));
        }
    }

    @Override
    public @NonNull AdaptiveMoveSelectorConfig enableNearbySelection(
            @NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            @NonNull Random random) {
        AdaptiveMoveSelectorConfig nearbyConfig = copyConfig();
        var updatedMoveSelectorList = new LinkedList<MoveSelectorConfig>();
        for (var selectorConfig : moveSelectorConfigList) {
            if (selectorConfig instanceof NearbyAutoConfigurationEnabled<?> nearbySelectorConfig) {
                if (AdaptiveMoveSelectorConfig.class.isAssignableFrom(nearbySelectorConfig.getClass())) {
                    updatedMoveSelectorList.add(nearbySelectorConfig.enableNearbySelection(distanceMeter, random));
                } else {
                    updatedMoveSelectorList.add((MoveSelectorConfig) selectorConfig.copyConfig());
                    updatedMoveSelectorList.add(nearbySelectorConfig.enableNearbySelection(distanceMeter, random));
                }
            } else {
                updatedMoveSelectorList.add((MoveSelectorConfig<?>) selectorConfig.copyConfig());
            }
        }
        nearbyConfig.withMoveSelectorList(updatedMoveSelectorList);
        return nearbyConfig;
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return moveSelectorConfigList != null
                && moveSelectorConfigList.stream().anyMatch(MoveSelectorConfig::hasNearbySelectionConfig);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + moveSelectorConfigList + ")";
    }

}
