package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class UnimprovedBestSolutionTermination<Solution_> extends AbstractTermination<Solution_> {

    private final int retries;
    protected long lastUnimprovedMoveCount;
    protected long unimprovedMoveCountLimit;
    protected Score<?> currentBest;
    protected Boolean terminate;

    public UnimprovedBestSolutionTermination(int retries) {
        this.retries = retries;
    }

    public int getRetries() {
        return retries;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    @SuppressWarnings("unchecked")
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.delayPeriodMillis = clock.millis() + WAIT_PERIOD_MILLIS;
        this.lastSolutionImprovementMillis = 0;
        this.currentBest = phaseScope.getBestScore();
        this.flatLineIntervalMillis = 0;
        this.maxFlatLineMillis = 0;
        this.terminate = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        this.currentBest = stepScope.getPhaseScope().getBestScore();
        terminate = null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        var improved = ((Score) currentBest).compareTo(stepScope.getScore()) < 0;
        if (flatLineIntervalMillis == 0) {
            var currentTimeMillis = clock.millis();
            if (isStarting(currentTimeMillis)) {
                // The delay phase is finished and minFlatLineMillis is not calculated yet
                if (improved && lastSolutionImprovementMillis == 0) {
                    lastSolutionImprovementMillis = currentTimeMillis;
                } else if (improved) {
                    flatLineIntervalMillis = currentTimeMillis - lastSolutionImprovementMillis;
                    maxFlatLineMillis = (long) (flatLineIntervalMillis * maxFlatLineRatio);

                }
            }
        } else if (improved) {
            lastSolutionImprovementMillis = clock.millis();
        }
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        if (terminate != null) {
            return terminate;
        }
        // 1 - Delay phase
        var currentTimeMillis = clock.millis();
        if (isStarting(currentTimeMillis)) {
            return false;
        }
        // 2 - Active monitoring phase
        terminate = isActiveMonitoringPhaseFinished(currentTimeMillis);
        return terminate;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************
    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        // The value will change during the solving process.
        // Therefore, it is not possible to provide a number asymptotically incrementally
        return -1.0;
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    private boolean isStarting(long currentTimeMillis) {
        return currentTimeMillis >= delayPeriodMillis;
    }

    private boolean isActiveMonitoringPhaseFinished(long currentTimeMillis) {
        return (currentTimeMillis - lastSolutionImprovementMillis) >= maxFlatLineMillis;
    }

    @Override
    public UnimprovedBestSolutionTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new UnimprovedBestSolutionTermination<>(maxFlatLineRatio, clock);
    }

    @Override
    public String toString() {
        return "UnimprovedBestSolutionTermination(%.2f)".formatted(maxFlatLineRatio);
    }
}
