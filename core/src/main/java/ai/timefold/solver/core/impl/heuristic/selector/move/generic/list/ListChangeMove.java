package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * Moves an element of a {@link PlanningListVariable list variable}. The moved element is identified
 * by an entity instance and a position in that entity's list variable. The element is inserted at the given index
 * in the given destination entity's list variable.
 * <p>
 * An undo move is simply created by flipping the source and destination entity+index.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ListChangeMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final Object destinationEntity;
    private final int destinationIndex;

    private Object planningValue;

    /**
     * The move removes a planning value element from {@code sourceEntity.listVariable[sourceIndex]}
     * and inserts the planning value at {@code destinationEntity.listVariable[destinationIndex]}.
     *
     * <h4>ListChangeMove anatomy</h4>
     *
     * <pre>
     * {@code
     *                             / destinationEntity
     *                             |   / destinationIndex
     *                             |   |
     *                A {Ann[0]}->{Bob[2]}
     *                |  |   |
     * planning value /  |   \ sourceIndex
     *                   \ sourceEntity
     * }
     * </pre>
     *
     * <h4>Example 1 - source and destination entities are different</h4>
     *
     * <pre>
     * {@code
     * GIVEN
     * Ann.tasks = [A, B, C]
     * Bob.tasks = [X, Y]
     *
     * WHEN
     * ListChangeMove: A {Ann[0]->Bob[2]}
     *
     * THEN
     * Ann.tasks = [B, C]
     * Bob.tasks = [X, Y, A]
     * }
     * </pre>
     *
     * <h4>Example 2 - source and destination is the same entity</h4>
     *
     * <pre>
     * {@code
     * GIVEN
     * Ann.tasks = [A, B, C]
     *
     * WHEN
     * ListChangeMove: A {Ann[0]->Ann[2]}
     *
     * THEN
     * Ann.tasks = [B, C, A]
     * }
     * </pre>
     *
     * @param variableDescriptor descriptor of a list variable, for example {@code Employee.taskList}
     * @param sourceEntity planning entity instance from which a planning value will be removed, for example "Ann"
     * @param sourceIndex index in sourceEntity's list variable from which a planning value will be removed
     * @param destinationEntity planning entity instance to which a planning value will be moved, for example "Bob"
     * @param destinationIndex index in destinationEntity's list variable where the moved planning value will be inserted
     */
    public ListChangeMove(ListVariableDescriptor<Solution_> variableDescriptor,
            Object sourceEntity, int sourceIndex,
            Object destinationEntity, int destinationIndex) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
        this.destinationEntity = destinationEntity;
        this.destinationIndex = destinationIndex;
    }

    public Object getSourceEntity() {
        return sourceEntity;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public Object getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    public Object getMovedValue() {
        if (planningValue == null) {
            planningValue = variableDescriptor.getElement(sourceEntity, sourceIndex);
        }
        return planningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        // TODO maybe remove this because no such move should be generated
        // Do not use Object#equals on user-provided domain objects. Relying on user's implementation of Object#equals
        // opens the opportunity to shoot themselves in the foot if different entities can be equal.
        var sameEntity = destinationEntity == sourceEntity;
        var doable = !sameEntity
                || (destinationIndex != sourceIndex && destinationIndex != variableDescriptor.getListSize(sourceEntity));
        if (!doable || sameEntity || variableDescriptor.canExtractValueRangeFromSolution()) {
            return doable;
        }
        // When the source and destination are different,
        // and the value range is located at the entity,
        // we need to check if the destination's value range accepts the upcoming value
        var value = variableDescriptor.getElement(sourceEntity, sourceIndex);
        ValueRangeManager<Solution_> valueRangeManager =
                ((VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector).getValueRangeManager();
        return valueRangeManager
                .getFromEntity(variableDescriptor.getValueRangeDescriptor(), destinationEntity)
                .contains(value);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;

        if (sourceEntity == destinationEntity) {
            int fromIndex = Math.min(sourceIndex, destinationIndex);
            int toIndex = Math.max(sourceIndex, destinationIndex) + 1;
            castScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
            Object element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
            variableDescriptor.addElement(destinationEntity, destinationIndex, element);
            castScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
            planningValue = element;
        } else {
            castScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
            Object element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
            castScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

            castScoreDirector.beforeListVariableChanged(variableDescriptor,
                    destinationEntity, destinationIndex, destinationIndex);
            variableDescriptor.addElement(destinationEntity, destinationIndex, element);
            castScoreDirector.afterListVariableChanged(variableDescriptor,
                    destinationEntity, destinationIndex, destinationIndex + 1);
            planningValue = element;
        }
    }

    @Override
    public ListChangeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new ListChangeMove<>(
                variableDescriptor,
                destinationScoreDirector.lookUpWorkingObject(sourceEntity), sourceIndex,
                destinationScoreDirector.lookUpWorkingObject(destinationEntity), destinationIndex);
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        Set<Object> entities = new LinkedHashSet<>(2);
        entities.add(sourceEntity);
        entities.add(destinationEntity);
        return entities;
    }

    @Override
    public Collection<Object> getPlanningValues() {
        return Collections.singleton(planningValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListChangeMove<?> other = (ListChangeMove<?>) o;
        return sourceIndex == other.sourceIndex && destinationIndex == other.destinationIndex
                && Objects.equals(variableDescriptor, other.variableDescriptor)
                && Objects.equals(sourceEntity, other.sourceEntity)
                && Objects.equals(destinationEntity, other.destinationEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d] -> %s[%d]}",
                getMovedValue(), sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }
}
