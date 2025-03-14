package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import java.util.Collection;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class EntityTabuAcceptor<Solution_> extends AbstractTabuAcceptor<Solution_> {

    public EntityTabuAcceptor(String logIndentation) {
        super(logIndentation);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    protected Collection<? extends Object> findTabu(LocalSearchMoveScope<Solution_> moveScope) {
        return moveScope.getMove().extractPlanningEntities();
    }

    @Override
    protected Collection<? extends Object> findNewTabu(LocalSearchStepScope<Solution_> stepScope) {
        return stepScope.getStep().extractPlanningEntities();
    }

}
