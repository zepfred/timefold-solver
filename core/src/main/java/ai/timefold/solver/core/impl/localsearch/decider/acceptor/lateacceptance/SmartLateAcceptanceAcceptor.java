package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.termination.UnimprovedBestSolutionTermination;

/**
 * Enhances the Late Acceptance default behavior by increasing diversification when the solver becomes stuck.
 * <p>
 * The termination UnimprovedBestSolutionTermination is used to identify if the solver is stuck.
 * <p>
 * The approach proposed to enhance diversification involves updating the size of the list of late elements.
 * If the solver is stuck, the size will be doubled.
 * When the best solution improves, the size is divided by two if it remains greater than the initial list size.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SmartLateAcceptanceAcceptor<Solution_> extends LateAcceptanceAcceptor<Solution_> {

    protected Double reconfigurationStopFlatLineDetectionRatio = null;
    protected Double reconfigurationNoStopFlatLineDetectionRatio = null;
    protected Long reconfigurationMinimalExecutionTimeSeconds = null;
    private UnimprovedBestSolutionTermination<Solution_> stuckTermination;
    private int minimumLateAcceptanceSize;
    private Score<?> initialScore;
    private Score<?> currentBest;

    public void setReconfigurationStopFlatLineDetectionRatio(Double reconfigurationStopFlatLineDetectionRatio) {
        this.reconfigurationStopFlatLineDetectionRatio = reconfigurationStopFlatLineDetectionRatio;
    }

    public void setReconfigurationNoStopFlatLineDetectionRatio(Double reconfigurationNoStopFlatLineDetectionRatio) {
        this.reconfigurationNoStopFlatLineDetectionRatio = reconfigurationNoStopFlatLineDetectionRatio;
    }

    public void setReconfigurationMinimalExecutionTimeSeconds(Long reconfigurationMinimalExecutionTimeSeconds) {
        this.reconfigurationMinimalExecutionTimeSeconds = reconfigurationMinimalExecutionTimeSeconds;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        stuckTermination = new UnimprovedBestSolutionTermination<>(reconfigurationStopFlatLineDetectionRatio,
                reconfigurationNoStopFlatLineDetectionRatio, reconfigurationMinimalExecutionTimeSeconds);
        stuckTermination.phaseStarted(phaseScope);
        initialScore = phaseScope.getBestScore();
        minimumLateAcceptanceSize = this.lateAcceptanceSize;
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return updateLateElementsListSize(moveScope);
    }

    private boolean updateLateElementsListSize(LocalSearchMoveScope<Solution_> moveScope) {
        var accepted = super.isAccepted(moveScope);
        if (!accepted && stuckTermination.isPhaseTerminated(moveScope.getStepScope().getPhaseScope())) {
            // Increases the size to improve the diversification
            var newLateAcceptanceSize = this.lateAcceptanceSize * 2;
            logger.trace("Increasing late elements list size from {} to {}", this.lateAcceptanceSize, newLateAcceptanceSize);
            var newPreviousElements = new Score<?>[newLateAcceptanceSize];
            System.arraycopy(this.previousScores, 0, newPreviousElements, 0, this.lateAcceptanceSize);
            Arrays.fill(newPreviousElements, lateAcceptanceSize, newLateAcceptanceSize, initialScore);
            this.lateScoreIndex = this.lateAcceptanceSize;
            this.lateAcceptanceSize = newLateAcceptanceSize;
            this.previousScores = newPreviousElements;
            this.stuckTermination.resetStartTime();
            accepted = true;
        }
        return accepted;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        stuckTermination.stepStarted(stepScope);
        currentBest = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        stuckTermination.stepEnded(stepScope);
        if (((Score) currentBest).compareTo(stepScope.getScore()) < 0) {
            // Decreases the size to improve the intensification
            var newLateAcceptanceSize = this.lateAcceptanceSize / 2;
            if (newLateAcceptanceSize >= minimumLateAcceptanceSize) {
                logger.trace("Decreasing late elements list size from {} to {}", this.lateAcceptanceSize,
                        newLateAcceptanceSize);
                var newPreviousElements = new Score<?>[newLateAcceptanceSize];
                System.arraycopy(this.previousScores, 0, newPreviousElements, 0, newLateAcceptanceSize);
                this.lateScoreIndex = 0;
                this.lateAcceptanceSize = newLateAcceptanceSize;
                this.previousScores = newPreviousElements;
            }
        }
    }

}
