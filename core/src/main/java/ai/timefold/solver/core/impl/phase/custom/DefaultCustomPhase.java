package ai.timefold.solver.core.impl.phase.custom;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomPhaseScope;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

/**
 * Default implementation of {@link CustomPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class DefaultCustomPhase<Solution_> extends AbstractPhase<Solution_> implements CustomPhase<Solution_> {

    private final List<CustomPhaseCommand<Solution_>> customPhaseCommandList;

    private DefaultCustomPhase(Builder<Solution_> builder) {
        super(builder);
        customPhaseCommandList = builder.customPhaseCommandList;
    }

    @Override
    public String getPhaseTypeString() {
        return "Custom";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        CustomPhaseScope<Solution_> phaseScope = new CustomPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);
        for (CustomPhaseCommand<Solution_> customPhaseCommand : customPhaseCommandList) {
            solverScope.checkYielding();
            if (phaseTermination.isPhaseTerminated(phaseScope)) {
                break;
            }
            CustomStepScope<Solution_> stepScope = new CustomStepScope<>(phaseScope);
            stepStarted(stepScope);
            doStep(stepScope, customPhaseCommand);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    private void doStep(CustomStepScope<Solution_> stepScope, CustomPhaseCommand<Solution_> customPhaseCommand) {
        InnerScoreDirector<Solution_, ?> scoreDirector = stepScope.getScoreDirector();
        customPhaseCommand.changeWorkingSolution(scoreDirector);
        calculateWorkingStepScore(stepScope, customPhaseCommand);
        solver.getBestSolutionRecaller().processWorkingSolutionDuringStep(stepScope);
    }

    public void stepEnded(CustomStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        CustomPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    Custom step ({}), time spent ({}), score ({}), {} best score ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore(),
                    stepScope.getBestScoreImproved() ? "new" : "   ",
                    phaseScope.getBestScore());
        }
    }

    public void phaseEnded(CustomPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Custom phase ({}) ended: time spent ({}), best score ({}),"
                + " move evaluation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore(),
                phaseScope.getPhaseMoveEvaluationSpeed(),
                phaseScope.getNextStepIndex());
    }

    public static final class Builder<Solution_> extends AbstractPhase.Builder<Solution_> {

        private final List<CustomPhaseCommand<Solution_>> customPhaseCommandList;

        public Builder(int phaseIndex, boolean triggerFirstInitializedSolutionEvent, String logIndentation,
                Termination<Solution_> phaseTermination,
                List<CustomPhaseCommand<Solution_>> customPhaseCommandList) {
            super(phaseIndex, triggerFirstInitializedSolutionEvent, logIndentation, phaseTermination);
            this.customPhaseCommandList = List.copyOf(customPhaseCommandList);
        }

        @Override
        public DefaultCustomPhase<Solution_> build() {
            return new DefaultCustomPhase<>(this);
        }
    }
}
