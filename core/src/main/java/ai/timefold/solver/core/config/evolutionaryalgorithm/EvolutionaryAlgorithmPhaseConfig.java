package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "populationSize",
        "generationSize",
        "eliteSolutionSize",
        "enableGranularNeighborhood",
        "enableSwapStar",
        "localSearchPhaseConfig"
})
@NullMarked
public class EvolutionaryAlgorithmPhaseConfig extends PhaseConfig<EvolutionaryAlgorithmPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "evolutionaryAlgorithm";

    @Nullable
    private Integer populationSize = null;

    @Nullable
    private Integer generationSize = null;

    @Nullable
    private Integer eliteSolutionSize = null;

    @Nullable
    private Boolean enableGranularNeighborhood = null;

    @Nullable
    private Boolean enableSwapStar = null;

    @Nullable
    private LocalSearchPhaseConfig localSearchPhaseConfig = null;

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

    public @Nullable Boolean getEnableGranularNeighborhood() {
        return enableGranularNeighborhood;
    }

    public void setEnableGranularNeighborhood(@Nullable Boolean enableGranularNeighborhood) {
        this.enableGranularNeighborhood = enableGranularNeighborhood;
    }

    public @Nullable Boolean getEnableSwapStar() {
        return enableSwapStar;
    }

    public void setEnableSwapStar(@Nullable Boolean enableSwapStar) {
        this.enableSwapStar = enableSwapStar;
    }

    public @Nullable LocalSearchPhaseConfig getLocalSearchPhaseConfig() {
        return localSearchPhaseConfig;
    }

    public void setLocalSearchPhaseConfig(@Nullable LocalSearchPhaseConfig localSearchPhaseConfig) {
        this.localSearchPhaseConfig = localSearchPhaseConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAlgorithmPhaseConfig withPopulationSize(Integer populationSize) {
        setPopulationSize(populationSize);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withGenerationSize(Integer generationSize) {
        setGenerationSize(generationSize);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withEliteSolutionSize(Integer eliteSolutionSize) {
        setEliteSolutionSize(eliteSolutionSize);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withEnableGranularNeighborhood(Boolean enableGranularNeighborhood) {
        setEnableGranularNeighborhood(enableGranularNeighborhood);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withEnableSwapStar(Boolean enableSwapStar) {
        setEnableSwapStar(enableSwapStar);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withLocalSearchPhaseConfig(LocalSearchPhaseConfig localSearchPhaseConfig) {
        setLocalSearchPhaseConfig(localSearchPhaseConfig);
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig inherit(EvolutionaryAlgorithmPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        populationSize = ConfigUtils.inheritOverwritableProperty(populationSize, inheritedConfig.getPopulationSize());
        generationSize = ConfigUtils.inheritOverwritableProperty(generationSize, inheritedConfig.getGenerationSize());
        eliteSolutionSize = ConfigUtils.inheritOverwritableProperty(eliteSolutionSize, inheritedConfig.getEliteSolutionSize());
        enableGranularNeighborhood = ConfigUtils.inheritOverwritableProperty(enableGranularNeighborhood,
                inheritedConfig.getEnableGranularNeighborhood());
        enableSwapStar = ConfigUtils.inheritOverwritableProperty(enableSwapStar, inheritedConfig.getEnableSwapStar());
        localSearchPhaseConfig = ConfigUtils.inheritConfig(localSearchPhaseConfig, inheritedConfig.getLocalSearchPhaseConfig());
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig copyConfig() {
        return new EvolutionaryAlgorithmPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (localSearchPhaseConfig != null) {
            localSearchPhaseConfig.visitReferencedClasses(classVisitor);
        }
    }
}
