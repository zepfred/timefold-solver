package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ReversingType;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

class RandomSubListChangeMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Iterator<SubList> subListIterator;
    private final Iterator<ElementPosition> destinationIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Random workingRandom;
    private final ReversingType reversingType;

    RandomSubListChangeMoveIterator(
            SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector,
            Random workingRandom,
            ReversingType reversingType) {
        this.subListIterator = subListSelector.iterator();
        this.destinationIterator = destinationSelector.iterator();
        this.listVariableDescriptor = subListSelector.getVariableDescriptor();
        this.workingRandom = workingRandom;
        this.reversingType = reversingType;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!subListIterator.hasNext() || !destinationIterator.hasNext()) {
            return noUpcomingSelection();
        }

        var subList = subListIterator.next();
        var destination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (destination == null) {
            return noUpcomingSelection();
        } else if (destination instanceof PositionInList destinationElement) {
            var reversing = reversingType.hasReversingType();
            if (reversingType.hasSequentialType() && reversing) {
                reversing = workingRandom.nextBoolean();
            }
            return new SubListChangeMove<>(listVariableDescriptor, subList, destinationElement.entity(),
                    destinationElement.index(), reversing);
        } else {
            // TODO add SubListAssignMove
            return new SubListUnassignMove<>(listVariableDescriptor, subList);
        }
    }
}
