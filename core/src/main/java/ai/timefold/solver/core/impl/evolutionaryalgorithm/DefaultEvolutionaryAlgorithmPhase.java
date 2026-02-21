package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import java.util.function.IntFunction;

import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

public final class DefaultEvolutionaryAlgorithmPhase<Solution_> extends AbstractPhase<Solution_>
        implements EvolutionaryAlgorithmPhase<Solution_> {

    private final EvolutionaryDecider<Solution_, ?> evolutionaryDecider;

    public DefaultEvolutionaryAlgorithmPhase(Builder<Solution_> builder) {
        super(builder);
        this.evolutionaryDecider = builder.evolutionaryDecider;
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
        var population = evolutionaryDecider.generateInitialPopulation(phaseScope);
        phaseScope.setPopulation(population);
        while (!phaseTermination.isPhaseTerminated(phaseScope)) {
            var stepScope = new EvolutionaryAlgorithmStepScope<>(phaseScope);
            stepStarted(stepScope);
            // Evolve the current population using the related evolutionary strategy.
            // All logic related to executing operators,
            // individual selection, post-optimization, and so on is handled in this step.
            evolutionaryDecider.evolvePopulation(stepScope);
            stepEnded(stepScope);
        }
        phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        evolutionaryDecider.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        evolutionaryDecider.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        evolutionaryDecider.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        evolutionaryDecider.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        evolutionaryDecider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        evolutionaryDecider.stepEnded(stepScope);
        var solver = stepScope.getPhaseScope().getSolverScope().getSolver();
        solver.getBestSolutionRecaller().processWorkingSolutionDuringStep(stepScope);
        var phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    EA step ({}), time spent ({}), score ({}), {} best score ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore().raw(),
                    (stepScope.getBestScoreImproved() ? "new" : "   "), phaseScope.getBestScore().raw());
        }
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final EvolutionaryDecider<Solution_, ?> evolutionaryDecider;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                EvolutionaryDecider<Solution_, ?> evolutionaryDecider) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.evolutionaryDecider = evolutionaryDecider;
        }

        @Override
        public DefaultEvolutionaryAlgorithmPhase<Solution_> build() {
            return new DefaultEvolutionaryAlgorithmPhase<>(this);
        }
    }
}
