package ai.timefold.solver.core.impl.domain.variable.cascade.operation;

import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableSupply;

public class NextElementSupplyOperation implements CascadingUpdateOperation<Object> {

    private final NextElementVariableSupply nextElementVariableSupply;

    public NextElementSupplyOperation(NextElementVariableSupply nextElementVariableSupply) {
        this.nextElementVariableSupply = nextElementVariableSupply;
    }

    @Override
    public Object getValue(Object entity) {
        return nextElementVariableSupply.getNext(entity);
    }
}
