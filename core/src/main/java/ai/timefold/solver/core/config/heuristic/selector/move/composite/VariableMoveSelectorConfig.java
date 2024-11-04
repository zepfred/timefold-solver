package ai.timefold.solver.core.config.heuristic.selector.move.composite;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.localsearch.decider.refinement.LocalSearchRefinementConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public class VariableMoveSelectorConfig
        extends MoveSelectorConfig<VariableMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<VariableMoveSelectorConfig> {

    private LocalSearchRefinementConfig refinementConfig;
    private UnionMoveSelectorConfig unionMoveSelectorConfig;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public VariableMoveSelectorConfig() {
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

    public List<MoveSelectorConfig> getMoveSelectorList() {
        return unionMoveSelectorConfig.getMoveSelectorList();
    }

    public void setMoveSelectorList(List<MoveSelectorConfig> moveSelectorConfigList) {
        unionMoveSelectorConfig.setMoveSelectorList(moveSelectorConfigList);
    }

    public void setUnionMoveSelectorConfig(UnionMoveSelectorConfig unionMoveSelectorConfig) {
        this.unionMoveSelectorConfig = unionMoveSelectorConfig;
    }

    public LocalSearchRefinementConfig getRefinementConfig() {
        return refinementConfig;
    }

    public void setRefinementConfig(LocalSearchRefinementConfig refinementConfig) {
        this.refinementConfig = refinementConfig;
    }

    public UnionMoveSelectorConfig getUnionMoveSelectorConfig() {
        return unionMoveSelectorConfig;
    }
    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void extractLeafMoveSelectorConfigsIntoList(List<MoveSelectorConfig> leafMoveSelectorConfigList) {
        unionMoveSelectorConfig.extractLeafMoveSelectorConfigsIntoList(leafMoveSelectorConfigList);
    }

    @Override
    public VariableMoveSelectorConfig inherit(VariableMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        unionMoveSelectorConfig =
                ConfigUtils.inheritConfig(unionMoveSelectorConfig, inheritedConfig.unionMoveSelectorConfig);
        refinementConfig =
                ConfigUtils.inheritConfig(refinementConfig, inheritedConfig.refinementConfig);
        return this;
    }

    @Override
    public VariableMoveSelectorConfig copyConfig() {
        return new VariableMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (unionMoveSelectorConfig != null) {
            unionMoveSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (refinementConfig != null) {
            refinementConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public VariableMoveSelectorConfig enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random) {
        var nearbyConfig = copyConfig();
        if (unionMoveSelectorConfig != null) {
            var nearbyUnionMoveConfig = unionMoveSelectorConfig.enableNearbySelection(distanceMeter, random);
            nearbyConfig.setUnionMoveSelectorConfig(nearbyUnionMoveConfig);
        }
        return nearbyConfig;
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return unionMoveSelectorConfig != null
                && unionMoveSelectorConfig.getMoveSelectorList().stream()
                        .anyMatch(MoveSelectorConfig::hasNearbySelectionConfig);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + unionMoveSelectorConfig.getMoveSelectorList() + ")";
    }

}
