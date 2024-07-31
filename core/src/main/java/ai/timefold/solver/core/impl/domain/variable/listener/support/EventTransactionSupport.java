package ai.timefold.solver.core.impl.domain.variable.listener.support;

/**
 * The class provides the base contract with utility methods to track when planning entities are visited during event system
 * processing.
 *
 * The method names use an unusual pattern to make them unique.
 */
public interface EventTransactionSupport {

    void _internal_Timefold_Event_Support_init(EventTransactionStore eventTransactionStore);

    boolean _internal_Timefold_Event_Support_isVisited();

    void _internal_Timefold_Event_Support_visit();

    void _internal_Timefold_Event_Support_executeTargetMethod(String targetMethod);

    Object _internal_Timefold_Event_Support_getFieldValue(String fieldName);
}
