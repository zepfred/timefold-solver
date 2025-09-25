package ai.timefold.solver.core.impl.solver.random;

public interface ResumableRandom {

    /**
     * Return the current internal state of the random generator.
     */
    long getSeed();

    /**
     * Restores the internal state with the given seed,
     * allowing the pseudo-random number generation to return to a previous point.
     */
    void resumeSeed(long seed);
}
