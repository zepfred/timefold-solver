package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
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

    protected CollectionCascadingUpdateShadowEventSupportVariableListener(
            ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        super(sourceListVariableDescriptor, targetVariableDescriptorList, targetMethod, listVariableStateSupply);
    }

    @Override
    boolean isVisited(Object entity) {
        return ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_isVisited();
    }

    @Override
    void markAsVisited(Object entity) {
        ((AbstractEventTransactionSupport) entity)._internal_Timefold_Event_Support_visit();
    }
}
