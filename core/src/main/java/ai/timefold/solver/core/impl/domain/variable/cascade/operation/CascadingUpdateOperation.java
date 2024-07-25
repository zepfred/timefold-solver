package ai.timefold.solver.core.impl.domain.variable.cascade.operation;

public interface CascadingUpdateOperation<T> {

    T getValue(Object entity);
}
