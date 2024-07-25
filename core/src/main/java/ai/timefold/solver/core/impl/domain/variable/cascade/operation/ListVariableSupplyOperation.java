package ai.timefold.solver.core.impl.domain.variable.cascade.operation;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;

public class ListVariableSupplyOperation<Solution_> implements CascadingUpdateOperation<Integer> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;

    public ListVariableSupplyOperation(ListVariableStateSupply<Solution_> listVariableStateSupply) {
        this.listVariableStateSupply = listVariableStateSupply;
    }

    @Override
    public Integer getValue(Object entity) {
        return listVariableStateSupply.getIndex(entity);
    }
}
