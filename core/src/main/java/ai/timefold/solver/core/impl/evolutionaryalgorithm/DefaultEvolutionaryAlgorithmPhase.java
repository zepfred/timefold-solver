package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import java.util.function.IntFunction;

import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy.EvolutionaryStrategy;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

public final class DefaultEvolutionaryAlgorithmPhase<Solution_> extends AbstractPhase<Solution_>
        implements EvolutionaryAlgorithmPhase<Solution_> {

    private final EvolutionaryStrategy<Solution_, ?> evolutionaryStrategy;

    public DefaultEvolutionaryAlgorithmPhase(Builder<Solution_> builder) {
        super(builder);
        this.evolutionaryStrategy = builder.evolutionaryStrategy;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public PhaseType getPhaseType() {
        return PhaseType.EVOLUTIONARY_ALGORITHM;
    }

    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return EventProducerId::evolutionaryAlgorithm;
    }

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = new EvolutionaryAlgorithmPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);
        // Build the initial population
        var population = evolutionaryStrategy.generateInitialPopulation(phaseScope);
        phaseScope.setPopulation(population);
        while (!phaseTermination.isPhaseTerminated(phaseScope)) {
            // Process the new generation according to the current population
            // All logic related to executing the operator and individual selection is handled in this step
            evolutionaryStrategy.execute(phaseScope);
        }
        phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        evolutionaryStrategy.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        evolutionaryStrategy.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        evolutionaryStrategy.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        evolutionaryStrategy.phaseEnded(phaseScope);
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final EvolutionaryStrategy<Solution_, ?> evolutionaryStrategy;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                EvolutionaryStrategy<Solution_, ?> evolutionaryStrategy) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.evolutionaryStrategy = evolutionaryStrategy;
        }

        @Override
        public DefaultEvolutionaryAlgorithmPhase<Solution_> build() {
            return new DefaultEvolutionaryAlgorithmPhase<>(this);
        }
    }
}
