package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.ArrayList;
import java.util.List;

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
 * <p>
 * This implementation is based on the following work:
 * Parameter-less Late Acceptance Hill-climbing by Mosab Bazargani
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SmartLateAcceptanceAcceptor<Solution_> extends LateAcceptanceAcceptor<Solution_> {

    private static final int MAX_BEST_SCORES = 100;
    protected Double stopFlatLineDetectionRatio = null;
    protected Double noStopFlatLineDetectionRatio = null;
    protected Long delayFlatLineSecondsSpentLimit = null;
    private UnimprovedBestSolutionTermination<Solution_> stuckTermination;
    private List<Score<?>> bestScores;
    private Score<?> currentBest;

    public void setStopFlatLineDetectionRatio(Double stopFlatLineDetectionRatio) {
        this.stopFlatLineDetectionRatio = stopFlatLineDetectionRatio;
    }

    public void setNoStopFlatLineDetectionRatio(Double noStopFlatLineDetectionRatio) {
        this.noStopFlatLineDetectionRatio = noStopFlatLineDetectionRatio;
    }

    public void setDelayFlatLineSecondsSpentLimit(Long delayFlatLineSecondsSpentLimit) {
        this.delayFlatLineSecondsSpentLimit = delayFlatLineSecondsSpentLimit;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        stuckTermination = new UnimprovedBestSolutionTermination<>(stopFlatLineDetectionRatio,
                noStopFlatLineDetectionRatio, delayFlatLineSecondsSpentLimit);
        stuckTermination.phaseStarted(phaseScope);
        bestScores = new ArrayList<>(MAX_BEST_SCORES);
        bestScores.add(phaseScope.getBestScore());
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return updateLateElementsListSize(moveScope);
    }

    private boolean updateLateElementsListSize(LocalSearchMoveScope<Solution_> moveScope) {
        var accepted = super.isAccepted(moveScope);
        if (!accepted && stuckTermination.isPhaseTerminated(moveScope.getStepScope().getPhaseScope())) {
            // Increases the size to improve the diversification
            var newLateAcceptanceSize = this.lateAcceptanceSize * 4;
            logger.info("Increasing late elements list size from {} to {}, best score {}", this.lateAcceptanceSize,
                    newLateAcceptanceSize, moveScope.getStepScope().getPhaseScope().getBestScore());
            var newPreviousElements = new Score<?>[newLateAcceptanceSize];
            for (var i = 0; i < newLateAcceptanceSize; i++) {
                var idx = bestScores.size() - 1 - (i % bestScores.size());
                newPreviousElements[i] = bestScores.get(idx);
            }
            this.lateScoreIndex = 0;
            this.lateAcceptanceSize = newLateAcceptanceSize;
            this.previousScores = newPreviousElements;
            this.stuckTermination.resetLastImprovementTime();
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
        var lateScore = previousScores[lateScoreIndex];
        var moveScore = stepScope.getScore();
        if (((Score) moveScore).compareTo(lateScore) > 0) {
            previousScores[lateScoreIndex] = stepScope.getScore();
        }
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        stuckTermination.stepEnded(stepScope);
        if (((Score) currentBest).compareTo(moveScore) < 0) {
            logger.info("New best score is {}", moveScore);
        }
    }

}
