package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "initialPopulationSize",
        "newPopulationSize"
})
@NullMarked
public class EvolutionaryAlgorithmPhaseConfig extends PhaseConfig<EvolutionaryAlgorithmPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "evolutionaryAlgorithm";

    @Nullable
    protected Integer initialPopulationSize = null;

    @Nullable
    protected Integer newPopulationSize = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Integer getInitialPopulationSize() {
        return initialPopulationSize;
    }

    public void setInitialPopulationSize(@Nullable Integer initialPopulationSize) {
        this.initialPopulationSize = initialPopulationSize;
    }

    public @Nullable Integer getNewPopulationSize() {
        return newPopulationSize;
    }

    public void setNewPopulationSize(@Nullable Integer newPopulationSize) {
        this.newPopulationSize = newPopulationSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAlgorithmPhaseConfig withInitialPopulationSize(Integer initialPopulationCount) {
        this.initialPopulationSize = initialPopulationCount;
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withNewPopulationSize(Integer newPopulationCount) {
        this.newPopulationSize = newPopulationCount;
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig inherit(EvolutionaryAlgorithmPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        initialPopulationSize =
                ConfigUtils.inheritOverwritableProperty(initialPopulationSize, inheritedConfig.getInitialPopulationSize());
        newPopulationSize =
                ConfigUtils.inheritOverwritableProperty(newPopulationSize, inheritedConfig.getNewPopulationSize());
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig copyConfig() {
        return new EvolutionaryAlgorithmPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        // No references
    }
}
