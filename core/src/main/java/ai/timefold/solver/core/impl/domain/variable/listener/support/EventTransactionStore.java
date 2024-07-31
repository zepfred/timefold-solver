package ai.timefold.solver.core.impl.domain.variable.listener.support;

public interface EventTransactionStore {

    long getEventId();

    void increment();

    void reset();
}
