package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link Acceptor}.
 *
 * @see Acceptor
 */
public abstract class AbstractAcceptor<Solution_> extends LocalSearchPhaseLifecycleListenerAdapter<Solution_>
        implements Acceptor<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isRestartSupported() {
        // By default, acceptors do not support restarting and the child classes must override this behavior
        return false;
    }

    @Override
    public void restart(LocalSearchPhaseScope<Solution_> phaseScope) {
        // By default, the method executes nothing
    }
}
