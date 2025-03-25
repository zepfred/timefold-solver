package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This simple strategy is activated once a maximum number of rejected moves is reached.
 */
public class UnimprovedMoveCountStuckCriterion<Solution_> implements StuckCriterion<Solution_> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private long lastMoveCount;
    private Score<?> lastCompletedScore;
    private int lastBestScoreIndex;
    protected int maxRejected;
    protected long maxUnimprovedTimeMillis;

    public void setMaxRejected(int maxRejected) {
        this.maxRejected = maxRejected;
    }

    @Override
    public boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope) {
        if (moveScope.getStepScope().getPhaseScope().getBestSolutionStepIndex() == -1) {
            // We should wait for the initial improvement and avoid causing unnecessary events
            return false;
        }
        return moveScope.getStepScope().getPhaseScope().getSolverScope().getMoveEvaluationCount() - lastMoveCount > maxRejected;
    }

    @Override
    public void reset(LocalSearchPhaseScope<Solution_> phaseScope) {
        lastMoveCount = phaseScope.getSolverScope().getMoveEvaluationCount();
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        lastMoveCount = phaseScope.getSolverScope().getMoveEvaluationCount();
        maxUnimprovedTimeMillis = 60000L; // One minute by default
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        lastCompletedScore = stepScope.getPhaseScope().getLastCompletedStepScope().getScore();
        lastBestScoreIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        if (((Score) stepScope.getScore()).compareTo(lastCompletedScore) > 0) {
            lastMoveCount = stepScope.getPhaseScope().getSolverScope().getMoveEvaluationCount();
        }
        //        if (lastBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
        //            maxUnimprovedTimeMillis = 60000L;
        //        }
        //        // We also trigger the criterion by time when there is no improvement of the best solution
        //        if (System.currentTimeMillis()
        //                - stepScope.getPhaseScope().getPhaseBestSolutionTimeMillis() >= maxUnimprovedTimeMillis) {
        //            logger.info("Restart triggered by time ({}s)", maxUnimprovedTimeMillis / 1000L);
        //            lastMoveCount -= maxRejected + 1;
        //            maxUnimprovedTimeMillis *= 3;
        //        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }
}
