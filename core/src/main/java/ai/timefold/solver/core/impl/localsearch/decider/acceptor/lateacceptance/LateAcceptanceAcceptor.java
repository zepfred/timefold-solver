package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;
    protected boolean diversificationEnabled = false;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    // Previous accepted solution
    protected Score<?> previous;
    // The best score in the late elements list
    protected Score<?> lateBest;
    // Number of occurrences of lateBest in the late elements
    protected int lateBestOccurrences = -1;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    public void setDiversificationEnabled(boolean diversificationEnabled) {
        this.diversificationEnabled = diversificationEnabled;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        previousScores = new Score[lateAcceptanceSize];
        var initialScore = phaseScope.getBestScore();
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
        lateBestOccurrences = lateAcceptanceSize;
        lateBest = phaseScope.getBestScore();
        previous = phaseScope.getBestScore();
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException("The lateAcceptanceSize (" + lateAcceptanceSize
                    + ") cannot be negative or zero.");
        }
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (diversificationEnabled) {
            var accepted = isAcceptedDiversifiedStrategy(moveScope);
            executeReplacementDiversificationStrategy(moveScope);
            return accepted;
        }
        return isAcceptedDefaultStrategy(moveScope);
    }

    private boolean isAcceptedDefaultStrategy(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        if (compare(moveScore, lateScore) >= 0) {
            return true;
        }
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
            return compare(moveScore, lastStepScore) >= 0;
        }
        return false;
    }

    /**
     * The acceptance strategy is based on the work:
     * Diversified Late Acceptance Search by M. Namazi, C. Sanderson, M. A. H. Newton, M. M. A. Polash, and A. Sattar
     */
    private boolean isAcceptedDiversifiedStrategy(LocalSearchMoveScope<Solution_> moveScope) {
        // The first condition allows new solutions
        // to be accepted when the score is equal to lateBest and all late elements are set to lateBest.
        // This is useful in the initial and final iterations.
        var moveScore = moveScope.getScore();
        var current = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        return compare(moveScore, current) == 0 || compare(moveScore, lateBest) > 0;
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (!diversificationEnabled) {
            updateLateScore(stepScope.getScore());
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private void updateLateScore(Score score) {
        previousScores[lateScoreIndex] = score;
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
    }

    /**
     * The replacement strategy is based on the work:
     * Diversified Late Acceptance Search by M. Namazi, C. Sanderson, M. A. H. Newton, M. M. A. Polash, and A. Sattar
     */
    private void executeReplacementDiversificationStrategy(LocalSearchMoveScope<Solution_> moveScope) {
        var lateScore = previousScores[lateScoreIndex];
        var moveScore = moveScope.getScore();
        var current = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        if (isAcceptedDiversifiedStrategy(moveScope)) {
            previous = current;
            current = moveScore;
        }
        var lateCmp = compare(current, lateScore);
        if (lateCmp < 0) {
            // Improves the diversification to allow the next iterations to find a better solution
            updateLateScore(current);
        } else if (lateCmp > 0 && compare(current, previous) > 0) {
            // Improves the intensification
            // but avoids replacing values when the search falls into a plateau or local minima
            if (compare(lateScore, lateBest) == 0) {
                lateBestOccurrences--;
            }
            updateLateScore(current);
            if (lateBestOccurrences == 0) {
                lateBest = previousScores[0];
                // Recompute the new lateBest and the number of occurrences
                for (var i = 1; i < lateAcceptanceSize; i++) {
                    if (compare(previousScores[i], lateBest) > 0) {
                        lateBest = previousScores[i];
                    }
                }
                for (var i = 0; i < lateAcceptanceSize; i++) {
                    if (compare(previousScores[i], lateBest) == 0) {
                        lateBestOccurrences++;
                    }
                }
            }
        } else {
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
        lateBestOccurrences = -1;
        previous = null;
        lateBest = null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int compare(Score<?> first, Score<?> second) {
        return ((Score) first).compareTo(second);
    }

}
