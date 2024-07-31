package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Differs from {@link VariableListenerNotifiable} because it uses {@link EventTransactionStore} and improve
 * the event system lifecycle.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class VariableListenerTransactionSupportNotifiable<Solution_> extends VariableListenerNotifiable<Solution_> {

    private final EventTransactionStore eventTransactionStore;

    VariableListenerTransactionSupportNotifiable(
            ScoreDirector<Solution_> scoreDirector,
            VariableListener<Solution_, Object> variableListener,
            Collection<Notification<Solution_, ? super VariableListener<Solution_, Object>>> notificationQueue,
            EventTransactionStore eventTransactionStore,
            int globalOrder) {
        super(scoreDirector, variableListener, notificationQueue, globalOrder);
        this.eventTransactionStore = eventTransactionStore;
    }

    @Override
    public void triggerAllNotifications() {
        eventTransactionStore.increment();
        super.triggerAllNotifications();
    }
}
