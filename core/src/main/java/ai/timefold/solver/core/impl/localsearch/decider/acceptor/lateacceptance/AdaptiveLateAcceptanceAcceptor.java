package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.ArrayDeque;
import java.util.Deque;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.UnimprovedMoveCountStuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * The method increases the size of the late elements list whenever the solver gets stuck on a solution.
 * Additionally, it activates a flag that prompts the LA method to update the late element index in every iteration,
 * similar to the original proposal.
 * The list of late elements is reset to the initial score from the previous phase.
 * We believe this allows the LA to escape solution areas without making random explorations by accepting everything.
 * <p>
 * For the stuck criterion,
 * the main idea is
 * to initiate the restart event if all late elements have been compared and no improvements have been achieved.
 * With the exception of the combination of 2500 elements and 5000 rejections,
 * the values were taken from the second article.
 * <p>
 * The approach is based on the following works:
 * <p>
 * Parameter-less Late Acceptance Hill-climbing: Foundations & Applications by Mosab Bazargani.
 * <p>
 * Stochastic local search with learning automaton for the swap-body vehicle routing problem by Túlio A.M. Toffolo,
 * Jan Christiaens, Sam Van Malderen, Tony Wauters, Greet Vanden Berghe
 */
public class AdaptiveLateAcceptanceAcceptor<Solution_> extends RestartableAcceptor<Solution_> {

    protected static final int MAX_BEST_SCORES_SIZE = 50;
    protected static final int MAX_RESTART_WITHOUT_IMPROVEMENT_COUNT = 5; // All sizes tested in a row
    protected static final int[] LATE_ELEMENTS_SIZE =
            new int[] { 500, 5_000, 12_500, 25_000, 25_000 }; // Extracted from the second article
    private static final int[] LATE_ELEMENTS_MAX_REJECTIONS =
            new int[] { 5_000, 5_000, 12_500, 25_000, 50_000 };
    protected final LateAcceptanceAcceptor<Solution_> lateAcceptanceAcceptor;
    private int restartWithoutImprovementCount;
    private int lateIndex;
    private int lastBestSolutionStepIndex;
    private int lastBestScoreIndex;
    private Deque<Score<?>> lastBestScoreQueue;

    public AdaptiveLateAcceptanceAcceptor(StuckCriterion<Solution_> stuckCriterion) {
        super(true, stuckCriterion);
        this.lateAcceptanceAcceptor = new LateAcceptanceAcceptor<>(false);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        lateIndex = 0;
        restartWithoutImprovementCount = 0;
        lastBestSolutionStepIndex = -1;
        lateAcceptanceAcceptor.setLateAcceptanceSize(LATE_ELEMENTS_SIZE[lateIndex]);
        lateAcceptanceAcceptor.phaseStarted(phaseScope);
        stuckCriterion.reset(phaseScope);
        lastBestScoreQueue = new ArrayDeque<>(MAX_BEST_SCORES_SIZE);
        lastBestScoreQueue.add(phaseScope.getBestScore());
        if (stuckCriterion instanceof UnimprovedMoveCountStuckCriterion<Solution_> stepCountStuckCriterion) {
            stepCountStuckCriterion.setMaxRejected(LATE_ELEMENTS_MAX_REJECTIONS[lateIndex]);
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.lateAcceptanceAcceptor.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        lateAcceptanceAcceptor.stepStarted(stepScope);
        lastBestScoreIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        lateAcceptanceAcceptor.stepEnded(stepScope);
        if (lastBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            if (lastBestScoreQueue.size() < MAX_RESTART_WITHOUT_IMPROVEMENT_COUNT) {
                lastBestScoreQueue.addLast(stepScope.getScore());
            } else {
                lastBestScoreQueue.pollFirst();
                lastBestScoreQueue.addLast(stepScope.getScore());
            }
        }
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        lateIndex = (lateIndex + 1) % LATE_ELEMENTS_SIZE.length;
        var phaseScope = stepScope.getPhaseScope();
        var decider = phaseScope.getDecider();
        var lateScore = lastBestScoreQueue.getFirst();
        if (lastBestSolutionStepIndex == -1) {
            lastBestSolutionStepIndex = phaseScope.getBestSolutionStepIndex();
            restartWithoutImprovementCount = 0;
        } else if (lastBestSolutionStepIndex != phaseScope.getBestSolutionStepIndex()) {
            lastBestSolutionStepIndex = -1;
            restartWithoutImprovementCount = 0;
        }
        restartWithoutImprovementCount++;
        if (restartWithoutImprovementCount > MAX_RESTART_WITHOUT_IMPROVEMENT_COUNT) {
            // Perturbation mechanism
            // that assists the solver
            // in escaping local minima when the acceptor has no acceptable moves.
            // This will help the solver with very small and simple datasets by accepting the step solution.
            restartWithoutImprovementCount = 1;
            // We use the smallest size
            lateIndex = 0;
            lastBestSolutionStepIndex = -1;
            lateScore = stepScope.getScore();
        } else {
            // Restore the current best solution
            decider.restoreCurrentBestSolution(stepScope);
        }
        logger.info(
                "Restart event triggered, step count ({}), late elements size ({}), max rejections ({}), best score ({}), restart without improvement ({}), new late score ({}),",
                stepScope.getStepIndex(), LATE_ELEMENTS_SIZE[lateIndex], LATE_ELEMENTS_MAX_REJECTIONS[lateIndex],
                stepScope.getPhaseScope().getBestScore(), restartWithoutImprovementCount, lateScore);
        lateAcceptanceAcceptor.resetLateElementsScore(LATE_ELEMENTS_SIZE[lateIndex], lateScore);
        if (stuckCriterion instanceof UnimprovedMoveCountStuckCriterion<Solution_> stepCountStuckCriterion) {
            stepCountStuckCriterion.setMaxRejected(LATE_ELEMENTS_MAX_REJECTIONS[lateIndex]);
        }
        stuckCriterion.reset(stepScope.getPhaseScope());
    }

    @Override
    public boolean accept(LocalSearchMoveScope<Solution_> moveScope) {
        return lateAcceptanceAcceptor.isAccepted(moveScope);
    }
}
