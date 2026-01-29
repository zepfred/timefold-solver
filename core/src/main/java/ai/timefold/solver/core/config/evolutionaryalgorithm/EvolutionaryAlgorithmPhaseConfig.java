package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "populationSize",
        "generationSize",
        "eliteSolutionSize",
})
@NullMarked
public class EvolutionaryAlgorithmPhaseConfig extends PhaseConfig<EvolutionaryAlgorithmPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "evolutionaryAlgorithm";

    @Nullable
    protected Integer populationSize = null;

    @Nullable
    protected Integer generationSize = null;

    @Nullable
    protected Integer eliteSolutionSize = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Integer getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(@Nullable Integer populationSize) {
        this.populationSize = populationSize;
    }

    public @Nullable Integer getGenerationSize() {
        return generationSize;
    }

    public void setGenerationSize(@Nullable Integer generationSize) {
        this.generationSize = generationSize;
    }

    public @Nullable Integer getEliteSolutionSize() {
        return eliteSolutionSize;
    }

    public void setEliteSolutionSize(@Nullable Integer eliteSolutionSize) {
        this.eliteSolutionSize = eliteSolutionSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAlgorithmPhaseConfig withPopulationSize(Integer populationSize) {
        this.populationSize = populationSize;
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withGenerationSize(Integer generationSize) {
        this.generationSize = generationSize;
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withEliteSolutionSize(Integer eliteSolutionSize) {
        this.eliteSolutionSize = eliteSolutionSize;
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig inherit(EvolutionaryAlgorithmPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        populationSize =
                ConfigUtils.inheritOverwritableProperty(populationSize, inheritedConfig.getPopulationSize());
        generationSize =
                ConfigUtils.inheritOverwritableProperty(generationSize, inheritedConfig.getGenerationSize());
        eliteSolutionSize =
                ConfigUtils.inheritOverwritableProperty(eliteSolutionSize, inheritedConfig.getEliteSolutionSize());
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
