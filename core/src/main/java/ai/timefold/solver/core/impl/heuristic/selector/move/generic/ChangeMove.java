package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ChangeMove<Solution_> extends AbstractMove<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected final Object entity;
    protected final Object toPlanningValue;

    public ChangeMove(GenuineVariableDescriptor<Solution_> variableDescriptor, Object entity, Object toPlanningValue) {
        this.variableDescriptor = variableDescriptor;
        this.entity = entity;
        this.toPlanningValue = toPlanningValue;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public Object getEntity() {
        return entity;
    }

    public Object getToPlanningValue() {
        return toPlanningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        Object oldValue = variableDescriptor.getValue(entity);
        return !Objects.equals(oldValue, toPlanningValue);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        castScoreDirector.changeVariableFacade(variableDescriptor, entity, toPlanningValue);
    }

    @Override
    public ChangeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new ChangeMove<>(variableDescriptor, destinationScoreDirector.lookUpWorkingObject(entity),
                destinationScoreDirector.lookUpWorkingObject(toPlanningValue));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return Collections.singletonList(entity);
    }

    @Override
    public Collection<?> getPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ChangeMove<?> other = (ChangeMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(entity, other.entity) &&
                Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, entity, toPlanningValue);
    }

    @Override
    public String toString() {
        Object oldValue = variableDescriptor.getValue(entity);
        return entity + " {" + oldValue + " -> " + toPlanningValue + "}";
    }

}
