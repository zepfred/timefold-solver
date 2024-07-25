package ai.timefold.solver.core.impl.domain.variable.cascade.operation;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;

public class InverseElementSupplyOperation implements CascadingUpdateOperation<Object> {

    private final SingletonInverseVariableSupply inverseVariableSupply;

    public InverseElementSupplyOperation(SingletonInverseVariableSupply inverseVariableSupply) {
        this.inverseVariableSupply = inverseVariableSupply;
    }

    @Override
    public Object getValue(Object entity) {
        return inverseVariableSupply.getInverseSingleton(entity);
    }
}
