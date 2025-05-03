package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

public enum ReversingType {
    ONLY_SEQUENTIAL(true, false),
    ONLY_REVERSING(false, true),
    BOTH_SEQUENTIAL_REVERSING(true, true);

    private final boolean hasSequentialType;
    private final boolean hasReversingType;

    ReversingType(boolean hasSequentialType, boolean hasReversingType) {
        this.hasSequentialType = hasSequentialType;
        this.hasReversingType = hasReversingType;
    }

    public boolean hasSequentialType() {
        return hasSequentialType;
    }

    public boolean hasReversingType() {
        return hasReversingType;
    }
}
