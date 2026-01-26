package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractOriginalSwapIterator;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractRandomSwapIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;

public class SubListSwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final SubListSelector<Solution_> leftSubListSelector;
    private final SubListSelector<Solution_> rightSubListSelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final boolean selectReversingMoveToo;
    private final boolean randomSelection;

    public SubListSwapMoveSelector(SubListSelector<Solution_> leftSubListSelector,
            SubListSelector<Solution_> rightSubListSelector, boolean selectReversingMoveToo, boolean randomSelection) {
        this.leftSubListSelector = leftSubListSelector;
        this.rightSubListSelector = rightSubListSelector;
        this.listVariableDescriptor = leftSubListSelector.getVariableDescriptor();
        if (leftSubListSelector.getVariableDescriptor() != rightSubListSelector.getVariableDescriptor()) {
            throw new IllegalStateException("The selector (" + this
                    + ") has a leftSubListSelector's variableDescriptor ("
                    + leftSubListSelector.getVariableDescriptor()
                    + ") which is not equal to the rightSubListSelector's variableDescriptor ("
                    + rightSubListSelector.getVariableDescriptor() + ").");
        }
        this.selectReversingMoveToo = selectReversingMoveToo;
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(leftSubListSelector);
        phaseLifecycleSupport.addEventListener(rightSubListSelector);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new AbstractRandomSwapIterator<>(leftSubListSelector, rightSubListSelector) {
                @Override
                protected Move<Solution_> newSwapSelection(SubList leftSubSelection, SubList rightSubSelection) {
                    boolean reversing = selectReversingMoveToo && workingRandom.nextBoolean();
                    return new SubListSwapMove<>(listVariableDescriptor, leftSubSelection, rightSubSelection, reversing);
                }
            };
        } else {
            return new AbstractOriginalSwapIterator<>(leftSubListSelector, rightSubListSelector) {
                @Override
                protected Move<Solution_> newSwapSelection(SubList leftSubSelection, SubList rightSubSelection) {
                    return new SubListSwapMove<>(listVariableDescriptor, leftSubSelection, rightSubSelection,
                            selectReversingMoveToo);
                }
            };
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
        long leftSubListCount = leftSubListSelector.getSize();
        long rightSubListCount = rightSubListSelector.getSize();
        return leftSubListCount * rightSubListCount * (selectReversingMoveToo ? 2 : 1);
    }

    boolean isSelectReversingMoveToo() {
        return selectReversingMoveToo;
    }

    SubListSelector<Solution_> getLeftSubListSelector() {
        return leftSubListSelector;
    }

    SubListSelector<Solution_> getRightSubListSelector() {
        return rightSubListSelector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + leftSubListSelector + ", " + rightSubListSelector + ")";
    }
}
