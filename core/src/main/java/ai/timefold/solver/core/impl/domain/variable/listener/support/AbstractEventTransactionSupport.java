package ai.timefold.solver.core.impl.domain.variable.listener.support;

public abstract class AbstractEventTransactionSupport implements EventTransactionSupport {

    private long eventId = 0;
    private boolean visited = false;
    private EventTransactionStore eventTransactionStore;

    @Override
    public final void _internal_Timefold_Event_Support_init(EventTransactionStore eventTransactionStore) {
        if (this.eventTransactionStore == null) {
            this.eventTransactionStore = eventTransactionStore;
        }
        eventId = 0;
    }

    @Override
    public final boolean _internal_Timefold_Event_Support_isVisited() {
        if (eventTransactionStore == null) {
            return false;
        }
        if (eventId != eventTransactionStore.getEventId()) {
            eventId = eventTransactionStore.getEventId();
            visited = false;
        }
        return visited;
    }

    @Override
    public final void _internal_Timefold_Event_Support_visit() {
        if (eventTransactionStore == null) {
            return;
        }
        eventId = eventTransactionStore.getEventId();
        visited = true;
    }

}
