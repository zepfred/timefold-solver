package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

class OriginalConsecutiveSubListChangeMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Iterator<SubList> subListIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private Iterator<ElementPosition> destinationIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final boolean selectReversingMove;

    private SubList upcomingSubList;

    OriginalConsecutiveSubListChangeMoveIterator(SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector, boolean selectReversingMove) {
        this.subListIterator = subListSelector.iterator();
        this.destinationSelector = destinationSelector;
        this.destinationIterator = Collections.emptyIterator();
        this.listVariableDescriptor = subListSelector.getVariableDescriptor();
        this.selectReversingMove = selectReversingMove;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        while (!destinationIterator.hasNext()) {
            if (!subListIterator.hasNext()) {
                return noUpcomingSelection();
            }
            upcomingSubList = subListIterator.next();
            destinationIterator = destinationSelector.iterator();
        }
        var destination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (destination == null) {
            return noUpcomingSelection();
        } else {
            // Only assigned positions are expected
            var destinationElement = destination.ensureAssigned();
            return new SubListChangeMove<>(listVariableDescriptor, upcomingSubList, destinationElement.entity(),
                    destinationElement.index(), selectReversingMove);
        }
    }
}
