package ai.timefold.solver.core.impl.domain.variable.listener.support;

public interface CacheListener {

    void setVisited(Object entity);

    boolean isVisited(Object entity);

    void resetCache();
}
