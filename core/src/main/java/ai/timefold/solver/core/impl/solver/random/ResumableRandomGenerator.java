package ai.timefold.solver.core.impl.solver.random;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public final class ResumableRandomGenerator extends Random implements ResumableRandom {

    // Magical values extracted from the Random documentation
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;
    private final AtomicLong internalSeed;

    public ResumableRandomGenerator() {
        this(System.nanoTime());
    }

    public ResumableRandomGenerator(long seed) {
        this.internalSeed = new AtomicLong();
        this.setSeed(seed);
    }

    @Override
    public synchronized void setSeed(long seed) {
        if (internalSeed == null) {
            return;
        }
        // Mimic the logic from Random implementation
        this.internalSeed.set((seed ^ MULTIPLIER) & MASK);
        super.setSeed(seed);
    }

    @Override
    public synchronized long getSeed() {
        return internalSeed.get();
    }

    @Override
    public synchronized void resumeSeed(long seed) {
        internalSeed.set(seed);
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
}
