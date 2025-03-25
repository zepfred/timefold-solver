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

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    // The original implementation of Timefold does not increase the late index in every evaluation.
    // As a result, late elements are updated more slowly than the original author's proposal intended.
    // This does not mean that the implementation is incorrect;
    // it simply behaves differently from what was originally proposed.
    // This flag allows disabling this feature if desired.
    private final boolean increaseLateIndexPerMove;

    public LateAcceptanceAcceptor() {
        this(false);
    }

    public LateAcceptanceAcceptor(boolean increaseLateIndexPerMove) {
        this.increaseLateIndexPerMove = increaseLateIndexPerMove;
    }

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
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
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        var acceptLateScore = moveScore.compareTo(lateScore) >= 0;
        var acceptLastStepScore = moveScore.compareTo(lastStepScore) >= 0;
        if (!increaseLateIndexPerMove) {
            return acceptLateScore || (hillClimbingEnabled && acceptLastStepScore);
        } else {
            var accept = acceptLateScore || acceptLastStepScore;
            if (accept) {
                previousScores[lateScoreIndex] = moveScore;
            } else {
                previousScores[lateScoreIndex] = lastStepScore;
            }
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
            return accept;
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (!increaseLateIndexPerMove) {
            previousScores[lateScoreIndex] = stepScope.getScore();
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
    }

    protected void resetLateElementsScore(int size, Score<?> score) {
        this.lateScoreIndex = 0;
        this.lateAcceptanceSize = size;
        this.previousScores = new Score[size];
        Arrays.fill(previousScores, score);
    }

}