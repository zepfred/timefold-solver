package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;

final class FilterLimitMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private final SelectionIterator<Move<Solution_>> internalIterator;
    private final int limit;
    private int count;

    public FilterLimitMoveIterator(int limit, SelectionIterator<Move<Solution_>> internalIterator) {
        this.internalIterator = internalIterator;
        this.limit = limit;
    }

    @Override
    public boolean hasNext() {
        return count < limit && internalIterator.hasNext();
    }

    @Override
    public Move<Solution_> next() {
        count++;
        return internalIterator.next();
    }
}
