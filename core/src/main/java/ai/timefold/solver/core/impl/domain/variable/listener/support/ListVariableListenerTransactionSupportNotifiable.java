package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Differs from {@link ListVariableListenerNotifiable} because it uses {@link EventTransactionStore} and improve
 * the event system lifecycle.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class ListVariableListenerTransactionSupportNotifiable<Solution_>
        extends ListVariableListenerNotifiable<Solution_> {

    private final EventTransactionStore eventTransactionStore;

    ListVariableListenerTransactionSupportNotifiable(
            ScoreDirector<Solution_> scoreDirector,
            ListVariableListener<Solution_, Object, Object> variableListener,
            Collection<Notification<Solution_, ? super ListVariableListener<Solution_, Object, Object>>> notificationQueue,
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
