package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Default implementation of {@link LevelScoreState}.
 * <p>
 * Caches the best-solution step index and the corresponding score level values.
 * On each step, {@link #update} refreshes the cache when the best solution has changed,
 * and {@link #isScoreImproved} determines whether the {@link LateAcceptanceScoreBuffer} should be reset by checking:
 * <ul>
 * <li>whether any non-dominated level (hard for {@link IBendableScore IBendableScore}, hard and medium for all others)
 * differs from the cached values, or</li>
 * <li>if {@code softScoreImprovementRate} is greater than zero, whether any soft level improved by at least that
 * relative rate compared to the cached values.</li>
 * </ul>
 */
final class DefaultLevelScoreState<Solution_, Score_ extends Score<Score_>> implements LevelScoreState<Solution_> {

    private final int nonSoftLevelCount;
    private final double softScoreImprovementRate;
    private long previousBestScoreIndex;
    private Number[] previousBestScoreLevels;

    @SuppressWarnings("rawtypes")
    DefaultLevelScoreState(InnerScore<Score_> initialScore, ScoreDefinition scoreDefinition, double softScoreImprovementRate) {
        previousBestScoreLevels = initialScore.raw().toLevelNumbers();
        if (softScoreImprovementRate < 0 || softScoreImprovementRate > 1) {
            throw new IllegalArgumentException("softScoreImprovementRate must be between 0 and 1");
        }
        this.softScoreImprovementRate = softScoreImprovementRate;
        if (IBendableScore.class.isAssignableFrom(scoreDefinition.getScoreClass())) {
            nonSoftLevelCount = scoreDefinition.getFeasibleLevelsSize();
        } else {
            nonSoftLevelCount = scoreDefinition.getLevelsSize() - 1;
        }
    }

    @Override
    public void update(LocalSearchStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var bestSolutionStepIndex = phaseScope.getBestSolutionStepIndex();
        if (previousBestScoreIndex != bestSolutionStepIndex) {
            // Update the current best score information
            this.previousBestScoreIndex = bestSolutionStepIndex;
            this.previousBestScoreLevels = stepScope.getPhaseScope().getBestScore().raw().toLevelNumbers();
        }
    }

    /**
     * If non-dominated levels are updated (hard or medium), it is necessary to reset the late scores.
     * Failing to do so may cause the solver
     * to accept poor moves that do not affect the non-dominated scores but degrade the soft scores.
     * As a result,
     * any move that does not decrease the hard or medium score
     * but significantly worsens the soft score may be mistakenly accepted.
     * This could cause the working solution
     * to enter a bad region and require many additional steps to escape it.
     * <p>
     * In addition, when {@code softScoreImprovementRate} is greater than zero,
     * a large-enough relative improvement of a soft level also triggers a reset,
     * even though the non-dominated levels did not change.
     * This lets the buffer be refreshed around a meaningfully better soft score instead of continuing to compare against stale,
     * inferior late scores.
     *
     * @return true if any non-dominated score has changed, or a soft score improved by at least
     *         {@code softScoreImprovementRate}; otherwise, returns false
     */
    @Override
    public boolean isScoreImproved(LocalSearchStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var bestSolutionStepIndex = phaseScope.getBestSolutionStepIndex();
        if (previousBestScoreIndex != bestSolutionStepIndex) {
            var newBestScore = stepScope.getPhaseScope().getBestScore();
            var newBestScoreLevels = newBestScore.raw().toLevelNumbers();
            for (var i = 0; i < nonSoftLevelCount; i++) {
                if (!newBestScoreLevels[i].equals(previousBestScoreLevels[i])) {
                    return true;
                }
            }
            // Soft levels
            if (softScoreImprovementRate > 0) {
                for (var i = nonSoftLevelCount; i < previousBestScoreLevels.length; i++) {
                    var bestScoreLevel = previousBestScoreLevels[i].doubleValue();
                    var diff = newBestScoreLevels[i].doubleValue() - bestScoreLevel;
                    if (diff / Math.abs(bestScoreLevel) >= softScoreImprovementRate) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
