package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionListIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class OriginalConsecutiveSubListSelector<Solution_> extends AbstractSelector<Solution_>
        implements SubListSelector<Solution_> {

    private final EntitySelector<Solution_> entitySelector;
    private final IterableValueSelector<Solution_> valueSelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final int minimumSubListSize;
    private final int maximumSubListSize;

    private ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;

    public OriginalConsecutiveSubListSelector(
            EntitySelector<Solution_> entitySelector,
            IterableValueSelector<Solution_> valueSelector,
            int minimumSubListSize, int maximumSubListSize) {
        this.entitySelector = entitySelector;
        this.valueSelector = filterPinnedListPlanningVariableValuesWithIndex(valueSelector, this::getListVariableStateSupply);
        this.listVariableDescriptor = (ListVariableDescriptor<Solution_>) valueSelector.getVariableDescriptor();
        if (minimumSubListSize < 1) {
            throw new IllegalArgumentException(
                    "The minimumSubListSize (%d) must be greater than 0.".formatted(minimumSubListSize));
        }
        if (minimumSubListSize != maximumSubListSize) {
            throw new IllegalArgumentException(
                    "The minimumSubListSize (%d) must be or equal to the maximumSubListSize (%d) in original sublist selectors."
                            .formatted(minimumSubListSize, maximumSubListSize));
        }
        this.minimumSubListSize = minimumSubListSize;
        this.maximumSubListSize = maximumSubListSize;

        phaseLifecycleSupport.addEventListener(this.entitySelector);
        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    private ListVariableStateSupply<Solution_, Object, Object> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
    }

    @Override
    public ListVariableDescriptor<Solution_> getVariableDescriptor() {
        return listVariableDescriptor;
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public long getSize() {
        long subListCount = 0;
        for (Object entity : ((Iterable<Object>) entitySelector::endingIterator)) {
            int listSize = listVariableDescriptor.getUnpinnedSubListSize(entity);
            // Only consecutive values
            subListCount += Math.round((double) listSize / (double) minimumSubListSize) + 1;
        }
        return subListCount;
    }

    @Override
    public Iterator<Object> endingValueIterator() {
        // Child value selector is entity independent, so passing null entity is OK.
        return valueSelector.endingIterator(null);
    }

    @Override
    public long getValueCount() {
        return valueSelector.getSize();
    }

    @Override
    public Iterator<SubList> iterator() {
        return new OriginalSubListIterator(entitySelector.iterator(), minimumSubListSize);
    }

    @Override
    public ListIterator<SubList> listIterator() {
        return new OriginalSubListIterator(entitySelector.iterator(), minimumSubListSize);
    }

    @Override
    public ListIterator<SubList> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMinimumSubListSize() {
        return minimumSubListSize;
    }

    @Override
    public int getMaximumSubListSize() {
        return maximumSubListSize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelector + ")";
    }

    private final class OriginalSubListIterator extends UpcomingSelectionListIterator<SubList> {

        private final Iterator<Object> entityIterator;
        private final int sublistSize;

        private Object selectedEntity;
        private int selectedEntitySize;
        private int selectedEntityIndex;

        private OriginalSubListIterator(Iterator<Object> entityIterator, int sublistSize) {
            this.entityIterator = entityIterator;
            this.sublistSize = sublistSize;
        }

        private boolean pickNextEntity() {
            if (selectedEntity != null && (selectedEntityIndex + sublistSize) <= selectedEntitySize) {
                return true;
            }
            selectedEntity = null;
            while (entityIterator.hasNext() && selectedEntity == null) {
                selectedEntity = entityIterator.next();
                selectedEntitySize = listVariableDescriptor.getListSize(selectedEntity);
                var unpinnedEntityListSize = listVariableDescriptor.getUnpinnedSubListSize(selectedEntity);
                if (unpinnedEntityListSize < sublistSize) {
                    selectedEntity = null;
                    continue;
                }
                selectedEntityIndex = listVariableDescriptor.getFirstUnpinnedIndex(selectedEntity);
            }
            return selectedEntity != null;
        }

        @Override
        protected SubList createUpcomingSelection() {
            if (!pickNextEntity()) {
                return noUpcomingSelection();
            }
            var idx = selectedEntityIndex;
            selectedEntityIndex++;
            return new SubList(selectedEntity, idx, sublistSize);
        }

        @Override
        protected SubList createPreviousSelection() {
            throw new UnsupportedOperationException();
        }
    }

}
