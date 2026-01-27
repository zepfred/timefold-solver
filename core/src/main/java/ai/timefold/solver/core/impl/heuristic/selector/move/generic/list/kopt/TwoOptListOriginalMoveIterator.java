package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The proposed iterator only supports 2-opt moves and implements the same strategy defined in the article:
 * "Hybrid Genetic Search for the CVRP: Open-source Implementation and SWAP* Neighborhood".
 */
@NullMarked
final class TwoOptListOriginalMoveIterator<Solution_, Node_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;
    private final Iterator<Object> originValueIterator;
    private final IterableValueSelector<Node_> innerValueSelector;

    private @Nullable PositionInList firstValuePosition;
    private Iterator<Object> innerSelectedValueIterator;
    private @Nullable Move<Solution_> secondMove = null;

    public TwoOptListOriginalMoveIterator(ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            IterableValueSelector<Node_> originSelector, IterableValueSelector<Node_> innerValueSelector) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableStateSupply = listVariableStateSupply;
        this.originValueIterator = originSelector.iterator();
        this.innerValueSelector = innerValueSelector;
        this.innerSelectedValueIterator = Collections.emptyIterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (secondMove != null) {
            var result = secondMove;
            secondMove = null;
            return result;
        }

        while (!innerSelectedValueIterator.hasNext()) {
            if (!originValueIterator.hasNext()) {
                return noUpcomingSelection();
            }
            var firstValue = originValueIterator.next();
            firstValuePosition = listVariableStateSupply.getElementPosition(firstValue).ensureAssigned();
            innerSelectedValueIterator = innerValueSelector.iterator();
        }

        Object secondValue = innerSelectedValueIterator.next();
        var secondValuePosition = listVariableStateSupply.getElementPosition(secondValue).ensureAssigned();
        if (Objects.requireNonNull(firstValuePosition).entity() != secondValuePosition.entity()) {
            this.secondMove = new TwoOptListMove<>(listVariableDescriptor, firstValuePosition.entity(),
                    secondValuePosition.entity(), firstValuePosition.index(), secondValuePosition.index(), true);
        } else {
            this.secondMove = null;
        }
        return new TwoOptListMove<>(listVariableDescriptor, firstValuePosition.entity(), secondValuePosition.entity(),
                firstValuePosition.index(), secondValuePosition.index());
    }
}
