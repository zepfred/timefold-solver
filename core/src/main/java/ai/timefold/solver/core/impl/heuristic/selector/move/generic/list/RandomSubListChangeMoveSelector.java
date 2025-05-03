package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ReversingType;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;

public class RandomSubListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final SubListSelector<Solution_> subListSelector;
    private final DestinationSelector<Solution_> destinationSelector;
    private final ReversingType reversingType;

    public RandomSubListChangeMoveSelector(SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector, ReversingType reversingType) {
        this.subListSelector = subListSelector;
        this.destinationSelector = destinationSelector;
        this.reversingType = reversingType;
        phaseLifecycleSupport.addEventListener(subListSelector);
        phaseLifecycleSupport.addEventListener(destinationSelector);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new RandomSubListChangeMoveIterator<>(subListSelector, destinationSelector, workingRandom, reversingType);
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }

    @Override
    public long getSize() {
        long subListCount = subListSelector.getSize();
        long destinationCount = destinationSelector.getSize();
        return subListCount * destinationCount
                * (reversingType.hasSequentialType() && reversingType.hasReversingType() ? 2 : 1);
    }

    boolean isSelectReversingMoveToo() {
        return reversingType.hasReversingType();
    }

    SubListSelector<Solution_> getSubListSelector() {
        return subListSelector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + subListSelector + ", " + destinationSelector + ")";
    }
}
