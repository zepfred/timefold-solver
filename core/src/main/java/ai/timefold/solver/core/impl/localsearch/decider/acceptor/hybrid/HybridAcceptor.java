package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hybrid;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class HybridAcceptor <Solution_> extends AbstractAcceptor<Solution_> {

    private AbstractAcceptor<Solution_>[] innerAcceptors;
    private Solution_[] innerAcceptorSolution;

    public HybridAcceptor(int lateAcceptanceSize, int diversifiedLateAcceptanceSize) {

    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return false;
    }
}
