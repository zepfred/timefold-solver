package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;

public interface AcceptorRestartable<Solution_> {

    void restart(LocalSearchPhaseScope<Solution_> phaseScope);
}
