package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class DiversifiedLateAcceptanceAcceptor<Solution_> extends LateAcceptanceAcceptor<Solution_> {

    // The worst score in the late elements list
    protected Score<?> lateWorse;
    // Number of occurrences of lateWorse in the late elements
    protected int lateWorseOccurrences = -1;

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        lateWorseOccurrences = lateAcceptanceSize;
        lateWorse = phaseScope.getBestScore();
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
        if (accept) {
            current = moveScore;
        }
        // Improves the diversification to allow the next iterations to find a better solution
        var lateUnimprovedCmp = compare(current, lateScore) < 0;
        // Improves the intensification,
        // but avoids replacing values when the search falls into a plateau or local minima
        var lateImprovedCmp = compare(current, lateScore) > 0 && compare(current, previous) > 0;
        if (lateUnimprovedCmp || lateImprovedCmp) {
            updateLateScore(current);
            if (lateWorseOccurrences == 0) {
                lateWorse = previousScores[0];
                lateWorseOccurrences = 1;
                // Recompute the new lateBest and the number of occurrences
                for (var i = 1; i < lateAcceptanceSize; i++) {
                    var worseCmp = compare(previousScores[i], lateWorse);
                    if (worseCmp < 0) {
                        lateWorse = previousScores[i];
                        lateWorseOccurrences = 1;
                    } else if (worseCmp == 0) {
                        lateWorseOccurrences++;
                    }
                }
            }
        } else if (accept) {
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        }
        //        if (accept && moveScore.compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0) {
        //            logger.info("New best solution at {} - {}", moveScore, moveScope.getMove());
        //        }
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
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
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
