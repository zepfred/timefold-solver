package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;
import java.util.Random;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private final Random random;
    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;
    protected boolean diversificationEnabled = false;
    protected boolean enableLocalMinimaDetection = false;
    protected int localMinimaDetectionPoolTimeSeconds = 0;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    // Current accepted solution
    protected Score<?> current;
    // Previous accepted solution
    protected Score<?> previous;
    // The best score in the late elements list
    protected Score<?> lateBest;
    // Number of occurrences of lateBest in the late elements
    protected int lateBestOccurrences = -1;
    // Unsuccessful trials after executing a given numbe of moves
    protected int countUnsuccessfulTrials = -1;
    protected long nextUnsuccessfulTrialMoveCount = -1L;
    protected int countAcceptNextSolutions = -1;

    public LateAcceptanceAcceptor(Random random) {
        this.random = random;
    }

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    public void setDiversificationEnabled(boolean diversificationEnabled) {
        this.diversificationEnabled = diversificationEnabled;
    }

    public void setEnableLocalMinimaDetection(boolean enableLocalMinimaDetection) {
        this.enableLocalMinimaDetection = enableLocalMinimaDetection;
    }

    public void setLocalMinimaDetectionPoolTimeSeconds(int localMinimaDetectionPoolTimeSeconds) {
        this.localMinimaDetectionPoolTimeSeconds = localMinimaDetectionPoolTimeSeconds;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        init(phaseScope.getBestScore());
        resetTrials(0L);
    }

    private void init(Score<?> initialScore) {
        previousScores = new Score[lateAcceptanceSize];
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
        lateBestOccurrences = lateAcceptanceSize;
        current = initialScore;
        lateBest = initialScore;
        previous = initialScore;
    }

    private void resetTrials(long nextCount) {
        countUnsuccessfulTrials = 0;
        nextUnsuccessfulTrialMoveCount = nextCount;
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
        if (enableLocalMinimaDetection && localMinimaDetectionPoolTimeSeconds <= 0) {
            throw new IllegalArgumentException("The localMinimaDetectionPoolTimeSeconds (%d) cannot be negative or zero."
                    .formatted(localMinimaDetectionPoolTimeSeconds));
        }
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return diversificationEnabled ? isAcceptedDiversifiedStrategy(moveScope) : isAcceptedDefaultStrategy(moveScope);
    }

    private boolean isAcceptedDiversifiedStrategy(LocalSearchMoveScope<Solution_> moveScope) {
        var accepted = executeAcceptanceDiversifiedStrategy(moveScope);
        executeReplacementDiversificationStrategy(moveScope);
        if (enableLocalMinimaDetection) {
            executeLocalMinimaValidation(moveScope);
        }
        return accepted;
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
    private boolean executeAcceptanceDiversifiedStrategy(LocalSearchMoveScope<Solution_> moveScope) {
        // The first condition allows new solutions
        // to be accepted when the score is equal to lateBest and all late elements are set to lateBest.
        // This is useful in the initial and final iterations.
        var moveScore = moveScope.getScore();
        return compare(moveScore, current) == 0 || compare(moveScore, lateBest) > 0 || countAcceptNextSolutions > 0;
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
        if (executeAcceptanceDiversifiedStrategy(moveScope)) {
            if (countAcceptNextSolutions > 0) {
                countAcceptNextSolutions--;
                if (countAcceptNextSolutions == 0) {
                    logger.info("Maybe stuck on a local minima and accepting the solution with score ({})", moveScore);
                    init(moveScore);
                    resetTrials(nextTrialMoveCount(moveScope));
                }
                return;
            } else {
                previous = current;
                current = moveScore;
                resetTrials(nextTrialMoveCount(moveScope));
            }
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
                recomputeMaxLateElement();
                resetTrials(nextTrialMoveCount(moveScope));
            }
        } else {
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        }
    }

    private void executeLocalMinimaValidation(LocalSearchMoveScope<Solution_> moveScope) {
        if (countAcceptNextSolutions > 0) {
            return;
        }
        var solverScope = moveScope.getStepScope().getPhaseScope().getSolverScope();
        if (countUnsuccessfulTrials == lateAcceptanceSize) {
            var isStuck = compare(current, lateBest) == 0 && countOccurrences(lateBest) == lateAcceptanceSize;
            if (isStuck) {
                countAcceptNextSolutions = random.nextInt(lateAcceptanceSize) + 1;
                lateScoreIndex = 0;
                resetTrials(nextTrialMoveCount(moveScope));
                logger.info("Maybe stuck on local minima with score ({}), applying a perturbation with {} move(s)", current,
                        countAcceptNextSolutions);
            } else {
                logger.info("Max trials reached ({}), but stuck criteria not met.", countUnsuccessfulTrials);
                nextUnsuccessfulTrialMoveCount = nextTrialMoveCount(moveScope);
            }
        } else if (moveScope.getStepIndex() > 0 && (nextUnsuccessfulTrialMoveCount == 0L
                || solverScope.getMoveEvaluationCount() >= nextUnsuccessfulTrialMoveCount)) {
            nextUnsuccessfulTrialMoveCount = nextTrialMoveCount(moveScope);
            countUnsuccessfulTrials++;
            logger.info("Unsuccessful trial ({}), current move count ({}), next move count ({})", countUnsuccessfulTrials,
                    solverScope.getMoveEvaluationCount(), nextUnsuccessfulTrialMoveCount);
        }
    }

    private void recomputeMaxLateElement() {
        lateBest = previousScores[0];
        // Recompute the new lateBest and the number of occurrences
        for (var i = 1; i < lateAcceptanceSize; i++) {
            if (compare(previousScores[i], lateBest) > 0) {
                lateBest = previousScores[i];
            }
        }
        lateBestOccurrences = countOccurrences(lateBest);
    }

    private long nextTrialMoveCount(LocalSearchMoveScope<Solution_> moveScope) {
        var solverScope = moveScope.getStepScope().getPhaseScope().getSolverScope();
        return solverScope.getMoveEvaluationCount()
                + solverScope.getMoveEvaluationSpeed() * localMinimaDetectionPoolTimeSeconds;
    }

    private int countOccurrences(Score<?> score) {
        var occurrences = 0;
        for (var i = 0; i < lateAcceptanceSize; i++) {
            if (compare(previousScores[i], score) == 0) {
                occurrences++;
            }
        }
        return occurrences;
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
