package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    private boolean updateLateScore = false;
    // Relative soft-level improvement (0 to 1) that allows to reset the buffer based on soft scores
    // 0 disables this soft-level check entirely
    private double softScoreImprovementRate = 0;

    private LateAcceptanceScoreBuffer scoreBuffer;
    private LevelScoreState<Solution_> bestScoreState;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    public double getSoftScoreImprovementRate() {
        return softScoreImprovementRate;
    }

    public void setSoftScoreImprovementRate(double softScoreImprovementRate) {
        this.softScoreImprovementRate = softScoreImprovementRate;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        var initialScore = phaseScope.getBestScore();
        scoreBuffer = new LateAcceptanceScoreBuffer(lateAcceptanceSize, initialScore);
        var scoreDefinition = phaseScope.getSolverScope().getScoreDefinition();
        bestScoreState = scoreDefinition.getLevelsSize() > 1 || softScoreImprovementRate > 0
                ? new DefaultLevelScoreState<>(initialScore, scoreDefinition, softScoreImprovementRate)
                : new NoOpLevelScoreState<>();
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = (InnerScore) moveScope.getScore();
        var lateScore = scoreBuffer.getCurrent();
        // Accepts if it is better than the late score
        if (moveScore.compareTo(lateScore) > 0) {
            updateLateScore = true;
            return true;
        }
        var accepted = false;
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope()
                    .getLastCompletedStepScope().getScore();
            accepted = moveScore.compareTo(lastStepScore) >= 0;
        }
        // If the move is not accepted, we increase the current late index
        if (!accepted) {
            scoreBuffer.increment();
        }
        return accepted;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        bestScoreState.update(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (updateLateScore) {
            scoreBuffer.update(stepScope.getScore());
            updateLateScore = false;
        }
        if (bestScoreState.isScoreImproved(stepScope)) {
            scoreBuffer.tryReset(stepScope.getScore());
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        scoreBuffer = null;
        bestScoreState = null;
    }

    protected <Score_ extends Score<Score_>> InnerScore<Score_> getScore(int i) {
        return scoreBuffer.get(i);
    }

}
