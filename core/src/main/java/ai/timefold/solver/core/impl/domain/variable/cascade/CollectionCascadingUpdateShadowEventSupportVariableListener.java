package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
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
 * Differs from {@link CollectionCascadingUpdateShadowVariableListener}
 * because it uses the {@link ai.timefold.solver.core.impl.domain.variable.listener.support.EventTransactionStore}
 * to track whether the entity was already processed during the event lifecycle.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CollectionCascadingUpdateShadowEventSupportVariableListener<Solution_>
        extends CollectionCascadingUpdateShadowVariableListener<Solution_> {

    private final String targetMethodName;
    private final String sourceListFieldName;

    protected CollectionCascadingUpdateShadowEventSupportVariableListener(
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
        var targetMethoNameList = getTargetVariableNames();
        var oldValueList = new ArrayList<>(targetMethoNameList.size());
        for (var targetVariableName : targetMethoNameList) {
            scoreDirector.beforeVariableChanged(entity, targetVariableName);
            oldValueList.add(((AbstractEventTransactionSupport) entity)
                    ._internal_Timefold_Event_Support_getFieldValue(targetVariableName));
        }
        ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_executeTargetMethod(targetMethodName);
        var hasChange = false;
        for (int i = 0; i < targetMethoNameList.size(); i++) {
            var targetVariableName = targetMethoNameList.get(i);
            var newValue = ((AbstractEventTransactionSupport) entity)
                    ._internal_Timefold_Event_Support_getFieldValue(targetVariableName);
            scoreDirector.afterVariableChanged(entity, targetVariableName);
            if (!hasChange && !Objects.equals(oldValueList.get(i), newValue)) {
                hasChange = true;
            }
        }
        return hasChange;
    }
}
