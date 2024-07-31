package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.support.AbstractEventTransactionSupport;

/**
 * Differs from {@link SingleCascadingUpdateShadowVariableListener} because it uses the
 * {@link ai.timefold.solver.core.impl.domain.variable.listener.support.EventTransactionStore} to track whether the entity was
 * already processed during the event lifecycle.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SingleCascadingUpdateShadowVariableTransactionSupportListener<Solution_>
        extends SingleCascadingUpdateShadowVariableListener<Solution_> {

    private final String targetMethodName;
    private final String sourceListFieldName;

    SingleCascadingUpdateShadowVariableTransactionSupportListener(
            ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        super(sourceListVariableDescriptor, targetVariableDescriptorList, targetMethod, listVariableStateSupply);
        targetMethodName = targetMethod.getName();
        sourceListFieldName = sourceListVariableDescriptor.getVariableName();
    }

    @Override
    boolean isVisited(Object entity) {
        return ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_isVisited();
    }

    @Override
    void markAsVisited(Object entity) {
        ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_visit();
    }

    @Override
    protected List<Object> getPlanningListValues(Object entity) {
        return (List<Object>) ((AbstractEventTransactionSupport) entity)
                ._internal_Timefold_Event_Support_getFieldValue(sourceListFieldName);
    }

    @Override
    protected boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValue =
                ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_getFieldValue(getVariableName());
        scoreDirector.beforeVariableChanged(entity, getVariableName());
        ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_executeTargetMethod(targetMethodName);
        var newValue =
                ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_getFieldValue(getVariableName());
        scoreDirector.afterVariableChanged(entity, getVariableName());
        return !Objects.equals(oldValue, newValue);
    }
}
