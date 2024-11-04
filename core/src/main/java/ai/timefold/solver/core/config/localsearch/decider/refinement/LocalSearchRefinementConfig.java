package ai.timefold.solver.core.config.localsearch.decider.refinement;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "maxIterationsMultiplier"
})
public class LocalSearchRefinementConfig extends AbstractConfig<LocalSearchRefinementConfig> {

    protected Integer maxIterationsMultiplier = null;

    public Integer getMaxIterationsMultiplier() {
        return maxIterationsMultiplier;
    }

    public void setMaxIterationsMultiplier(Integer maxIterationsMultiplier) {
        this.maxIterationsMultiplier = maxIterationsMultiplier;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public LocalSearchRefinementConfig withMaxIterationsMultiplier(int maxIterationMultiplier) {
        this.maxIterationsMultiplier = maxIterationMultiplier;
        return this;
    }

    @Override
    public LocalSearchRefinementConfig inherit(LocalSearchRefinementConfig inheritedConfig) {
        maxIterationsMultiplier = ConfigUtils.inheritOverwritableProperty(maxIterationsMultiplier,
                inheritedConfig.getMaxIterationsMultiplier());
        return this;
    }

    @Override
    public LocalSearchRefinementConfig copyConfig() {
        return new LocalSearchRefinementConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        // No referenced classes
    }

}
