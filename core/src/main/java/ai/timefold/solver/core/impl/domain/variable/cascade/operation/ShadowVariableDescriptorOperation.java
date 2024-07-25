package ai.timefold.solver.core.impl.domain.variable.cascade.operation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;

public class ShadowVariableDescriptorOperation<Solution_, T> implements CascadingUpdateOperation<T> {
    private final ShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public ShadowVariableDescriptorOperation(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    @Override
    public T getValue(Object entity) {
        return (T) shadowVariableDescriptor.getValue(entity);
    }
}
