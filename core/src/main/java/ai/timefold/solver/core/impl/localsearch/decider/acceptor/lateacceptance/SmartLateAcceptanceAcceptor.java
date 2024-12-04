package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

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

    Clock clock = Clock.systemUTC();
    private static final int MAX_BEST_SCORES = 100;
    private List<Score<?>> bestScores;
    private int posBestScore = 0;
    private Score<?> currentBest;
    private long lastBestScoreMillis;
    // Geometric restart
    private static final double GEOMETRIC_FACTOR = 1.3;
    private double geometricGrowFactor;

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        bestScores = new ArrayList<>(MAX_BEST_SCORES);
        bestScores.add(phaseScope.getBestScore());
        lastBestScoreMillis = clock.millis();
        geometricGrowFactor = 5; // Starting with five unimproved seconds
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return updateLateElementsListSize(moveScope);
    }

    private boolean needRestart() {
        if ((clock.millis() - lastBestScoreMillis) >= geometricGrowFactor * 1_000) {
            logger.info("Restarting LA with geometric factor {}", geometricGrowFactor);
            geometricGrowFactor = Math.ceil(geometricGrowFactor * GEOMETRIC_FACTOR);
            this.lastBestScoreMillis = clock.millis();
            return true;
        }
        return false;
    }

    private boolean updateLateElementsListSize(LocalSearchMoveScope<Solution_> moveScope) {
        var accepted = super.isAccepted(moveScope);
        if (!accepted && needRestart()) {
            moveScope.getStepScope().getPhaseScope().changeStuck();
            accepted = true;
        }
        return accepted;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        //        if (stepScope.getPhaseScope().isStuck()) {
        //            // Increases the size to improve the diversification
        //            var newLateAcceptanceSize = lateAcceptanceSize * 2;
        //            logger.info("Increasing late elements list size from {} to {}, best score {}", this.lateAcceptanceSize,
        //                    newLateAcceptanceSize, stepScope.getPhaseScope().getBestScore());
        //            var newPreviousElements = new Score<?>[newLateAcceptanceSize];
        //            for (var i = 0; i < newLateAcceptanceSize; i++) {
        //                var idx = bestScores.size() - 1 - (i % bestScores.size());
        //                newPreviousElements[i] = bestScores.get(idx);
        //            }
        //            this.lateScoreIndex = 0;
        //            this.lateAcceptanceSize = newLateAcceptanceSize;
        //            this.previousScores = newPreviousElements;
        //            this.lastBestScoreMillis = clock.millis();
        //        }
        stepScope.getPhaseScope().changeUnstuck();
        currentBest = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        var moveScore = stepScope.getScore();
        if (((Score) currentBest).compareTo(moveScore) < 0) {
            //            logger.info("New best score is {}", moveScore);
            posBestScore = (posBestScore + 1) % MAX_BEST_SCORES;
            if (posBestScore < bestScores.size()) {
                bestScores.set(posBestScore, moveScore);
            } else {
                bestScores.add(moveScore);
            }
            lastBestScoreMillis = clock.millis();
        }
    }

}
