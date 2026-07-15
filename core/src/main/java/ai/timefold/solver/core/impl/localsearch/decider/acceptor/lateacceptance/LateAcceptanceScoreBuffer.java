package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Circular buffer implementation for managing late scores,
 * enabling simpler reset logic.
 * When {@link #tryReset} is called,
 * instead of filling all slots,
 * an epoch counter is incremented,
 * allowing the action to avoid reloading the score array with the new score.
 * <p>
 * When the buffer is reset, acceptance of the reset score is governed by {@code resetAcceptanceRate}.
 * This strategy introduces some randomness into the algorithm
 * and prevents the LA acceptor from behaving solely as a hill-climbing refinement heuristic after a reset operation.
 */
final class LateAcceptanceScoreBuffer {

    // Late score fields
    private final InnerScore<?>[] scores;
    private int currentIndex = 0;
    private final int size;
    // All required epoch fields
    private final double resetAcceptanceRate;
    private final long[] slotEpoch;
    private long resetEpoch = 0;
    private InnerScore<?> resetScore = null;
    private boolean writtenSinceReset = false;
    private final RandomGenerator workingRandom;

    LateAcceptanceScoreBuffer(int size, InnerScore<?> initialScore, double resetAcceptanceRate, RandomGenerator workingRandom) {
        this.size = size;
        this.scores = new InnerScore[size];
        if (resetAcceptanceRate < 0 || resetAcceptanceRate > 1) {
            throw new IllegalArgumentException("Reset acceptance rate must be between 0 and 1");
        }
        this.resetAcceptanceRate = resetAcceptanceRate;
        Arrays.fill(scores, initialScore);
        // By default,
        // the score is set to zero,
        // and it means all scores will be read initially.
        this.slotEpoch = new long[size];
        this.workingRandom = workingRandom;
    }

    <Score_ extends Score<Score_>> InnerScore<Score_> getCurrent() {
        return get(currentIndex);
    }

    @SuppressWarnings("unchecked")
    <Score_ extends Score<Score_>> InnerScore<Score_> get(int index) {
        if (resetAcceptanceRate == 0.0) {
            return (InnerScore<Score_>) scores[index];
        }
        if (slotEpoch[index] < resetEpoch
                && (resetAcceptanceRate == 1.0 || workingRandom.nextDouble(1.0) <= resetAcceptanceRate)) {
            return (InnerScore<Score_>) resetScore;
        }
        return (InnerScore<Score_>) scores[index];
    }

    /**
     * Update the score and advance the current late index.
     * 
     * @param score the score to be added to the buffer
     */
    void update(InnerScore<?> score) {
        scores[currentIndex] = score;
        slotEpoch[currentIndex] = resetEpoch;
        writtenSinceReset = true;
        currentIndex = (currentIndex + 1) % size;
    }

    /**
     * Lazily resets all slots to {@code newScore}.
     * Updating the score array is unnecessary since the related counter ensures the new score is returned if no changes have
     * occurred.
     *
     * @param newScore the score to be used to reset the buffer
     */
    void tryReset(InnerScore<?> newScore) {
        // Skips the reset action when no slot has been written since the last reset and the score is unchanged
        if (writtenSinceReset || !Objects.equals(newScore, resetScore)) {
            resetScore = newScore;
            resetEpoch++;
            writtenSinceReset = false;
        }
    }
}
