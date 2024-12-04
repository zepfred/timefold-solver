package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class DiversifiedLateAcceptanceAcceptor<Solution_> extends LateAcceptanceAcceptor<Solution_> {

    private final Clock clock = Clock.systemUTC();
    // The worst score in the late elements list
    protected Score<?> lateWorse;
    // Number of occurrences of lateWorse in the late elements
    protected int lateWorseOccurrences = -1;
    // Geometric restart
    private static final double GEOMETRIC_FACTOR = 1.3;
    private static final double SCALING_FACTOR = 1.0;
    private Score<?> bestRestartScore;
    private boolean restartTriggered;
    private double geometricGrowFactor;
    private long nextRestart;
    private long lastImprovementMillis;

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        lateWorseOccurrences = lateAcceptanceSize;
        lateWorse = phaseScope.getBestScore();
        bestRestartScore = phaseScope.getBestScore();
        geometricGrowFactor = 1;
        nextRestart = 1_000;
        restartTriggered = false;
        lastImprovementMillis = clock.millis();
    }

    private boolean applyRestart(LocalSearchMoveScope<Solution_> moveScope) {
        if (restartTriggered) {
            var accept = acceptRestartMove(moveScope);
            if (accept) {
                lastImprovementMillis = clock.millis();
                this.bestRestartScore = moveScope.getScore();
                this.restartTriggered = false;
                logger.debug("DLAS restarted with {} - {}", moveScope.getScore(), moveScope.getMove());
            }
            return accept;
        }
        if (lastImprovementMillis > 0 && clock.millis() - lastImprovementMillis >= nextRestart) {
            logger.debug("Restarting DLAS with geometric factor {}", geometricGrowFactor);
            nextRestart = (long) Math.ceil(SCALING_FACTOR * geometricGrowFactor * 1_000);
            geometricGrowFactor = Math.ceil(geometricGrowFactor * GEOMETRIC_FACTOR);
            lastImprovementMillis = clock.millis();
            restartTriggered = true;
            return acceptRestartMove(moveScope);
        }
        restartTriggered = false;
        return false;
    }

    private void updateRestartTime(LocalSearchMoveScope<Solution_> moveScope) {
        if (compare(moveScope.getScore(), moveScope.getStepScope().getPhaseScope().getBestScore()) > 0) {
            lastImprovementMillis = clock.millis();
            this.bestRestartScore = moveScope.getScore();
        }
    }

    private boolean acceptRestartMove(LocalSearchMoveScope<Solution_> moveScope) {
        return compare(moveScope.getScore(), bestRestartScore) != 0 && moveScope.getScore().isFeasible();
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        // The acceptance and replacement strategies are based on the work:
        // Diversified Late Acceptance Search by M. Namazi, C. Sanderson, M. A. H. Newton, M. M. A. Polash, and A. Sattar
        var lateScore = previousScores[lateScoreIndex];
        var moveScore = moveScope.getScore();
        var current = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        var previous = current;
        var accept = compare(moveScore, current) == 0 || compare(moveScore, lateWorse) > 0;
        updateRestartTime(moveScope);
        if (accept || applyRestart(moveScope)) {
            current = moveScore;
        }
        // Improves the diversification to allow the next iterations to find a better solution
        var lateUnimprovedCmp = compare(current, lateScore) < 0;
        // Improves the intensification,
        // but avoids replacing values when the search falls into a plateau or local minima
        var lateImprovedCmp = compare(current, lateScore) > 0 && compare(current, previous) > 0;
        if (lateUnimprovedCmp || lateImprovedCmp) {
            updateLateScore(current);
        }
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        return accept;
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        // Do nothing
    }

    private void updateLateScore(Score<?> score) {
        var worseCmp = compare(score, lateWorse);
        var lateCmp = compare(previousScores[lateScoreIndex], lateWorse);
        if (worseCmp < 0) {
            this.lateWorse = score;
            this.lateWorseOccurrences = 1;
        } else if (lateCmp == 0 && worseCmp != 0) {
            this.lateWorseOccurrences--;
        } else if (lateCmp != 0 && worseCmp == 0) {
            this.lateWorseOccurrences++;
        }
        previousScores[lateScoreIndex] = score;
        if (lateWorseOccurrences == 0) {
            lateWorse = previousScores[0];
            lateWorseOccurrences = 1;
            // Recompute the new lateBest and the number of occurrences
            for (var i = 1; i < lateAcceptanceSize; i++) {
                var cmp = compare(previousScores[i], lateWorse);
                if (cmp < 0) {
                    lateWorse = previousScores[i];
                    lateWorseOccurrences = 1;
                } else if (cmp == 0) {
                    lateWorseOccurrences++;
                }
            }
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        lateWorse = null;
        lateWorseOccurrences = -1;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int compare(Score<?> first, Score<?> second) {
        return ((Score) first).compareTo(second);
    }

}
