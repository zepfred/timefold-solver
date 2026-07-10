package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * An Acceptor accepts or rejects a selected {@link Move}.
 * Note that the {@link LocalSearchForager} can still ignore the advice of the {@link Acceptor}.
 *
 * @see AbstractAcceptor
 */
public interface Acceptor<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * @param moveScope not null
     * @return true if accepted
     */
    boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope);

    /**
     * @return true if the acceptor supports restart operation, false otherwise.
     */
    boolean isRestartSupported();

    /**
     * Restart the acceptor using a defined strategy.
     * This method can be paired with the decider to take action if the solver is stuck.
     */
    void restart(LocalSearchPhaseScope<Solution_> phaseScope);

}
