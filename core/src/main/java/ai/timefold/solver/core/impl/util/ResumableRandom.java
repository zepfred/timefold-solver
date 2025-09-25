package ai.timefold.solver.core.impl.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ResumableRandom extends Random {

    // Magical values extracted from the Random documentation
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;
    private final AtomicLong internalSeed;

    public ResumableRandom() {
        this(System.nanoTime());
    }

    public ResumableRandom(long seed) {
        this.internalSeed = new AtomicLong();
        this.setSeed(seed);
    }

    @Override
    public synchronized void setSeed(long seed) {
        // Mimic the logic from Random implementation
        this.internalSeed.set((seed ^ MULTIPLIER) & MASK);
        super.setSeed(seed);
    }

    /**
     * The method is central to all other random method strategies.
     * The current version uses the same method as the {@link Random} implementation,
     * but allows for the preservation of the internal state, enabling the resumption of a specific state later.
     *
     * @param bits random bits
     * @return the next pseudorandom value
     */
    @Override
    protected int next(int bits) {
        long oldSeed;
        long newSeed;
        do {
            oldSeed = internalSeed.get();
            newSeed = (oldSeed * MULTIPLIER + ADDEND) & MASK;
        } while (!internalSeed.compareAndSet(oldSeed, newSeed));
        return (int) (newSeed >>> (48 - bits));
    }

    public long getSeed() {
        return internalSeed.get();
    }

    public void resumeSeed(long seed) {
        internalSeed.set(seed);
    }
}
