package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;

public class SubListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final SubListSelector<Solution_> subListSelector;
    private final DestinationSelector<Solution_> destinationSelector;
    private final boolean selectReversingMoveToo;
    private final boolean randomSelection;

    public SubListChangeMoveSelector(SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector, boolean selectReversingMoveToo, boolean randomSelection) {
        this.subListSelector = subListSelector;
        this.destinationSelector = destinationSelector;
        this.selectReversingMoveToo = selectReversingMoveToo;
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(subListSelector);
        phaseLifecycleSupport.addEventListener(destinationSelector);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomSubListChangeMoveIterator<>(
                    subListSelector,
                    destinationSelector,
                    workingRandom,
                    selectReversingMoveToo);
        } else {
            return new OriginalConsecutiveSubListChangeMoveIterator<>(
                    subListSelector,
                    destinationSelector,
                    selectReversingMoveToo);
        }
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection;
    }

    @Override
    public long getSize() {
        long subListCount = subListSelector.getSize();
        long destinationCount = destinationSelector.getSize();
        var size = subListCount * destinationCount;
        if (randomSelection && selectReversingMoveToo) {
            size *= 2;
        }
        return size;
    }

    boolean isSelectReversingMoveToo() {
        return selectReversingMoveToo;
    }

    SubListSelector<Solution_> getSubListSelector() {
        return subListSelector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + subListSelector + ", " + destinationSelector + ")";
    }
}
