package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.IdentityHashMap;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.cascade.operation.CascadingUpdateOperation;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractCascadingUpdateShadowVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final ListVariableDescriptor<Solution_> sourceListVariable;
    final List<VariableDescriptor<Solution_>> targetVariableDescriptorList;
    final MemberAccessor targetMethod;
    private final CascadingUpdateOperation<Object> nextElementOperation;
    private final CascadingUpdateOperation<Object> inverseElementOperation;
    private final CascadingUpdateOperation<Integer> indexElementOperation;
    private IdentityHashMap<Object, boolean[]> entityVisitMap;

    AbstractCascadingUpdateShadowVariableListener(ListVariableDescriptor<Solution_> sourceListVariable,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList,
            CascadingUpdateOperation<Object> nextElementOperation, CascadingUpdateOperation<Object> inverseElementOperation,
            CascadingUpdateOperation<Integer> indexElementOperation, MemberAccessor targetMethod) {
        this.sourceListVariable = sourceListVariable;
        this.targetVariableDescriptorList = targetVariableDescriptorList;
        this.targetMethod = targetMethod;
        this.nextElementOperation = nextElementOperation;
        this.inverseElementOperation = inverseElementOperation;
        this.indexElementOperation = indexElementOperation;
        this.entityVisitMap = new IdentityHashMap<>();
    }

    abstract boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity);

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        entityVisitMap = new IdentityHashMap<>();
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        Object inverseElement = inverseElementOperation.getValue(entity);
        if (inverseElement == null) {
            executeWithoutCache(scoreDirector, entity);
        } else {
            executeWithCache(scoreDirector, entity, inverseElement);
        }
    }

    private void executeWithoutCache(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var currentEntity = entity;
        while (currentEntity != null) {
            if (!execute(scoreDirector, currentEntity)) {
                break;
            }
            currentEntity = nextElementOperation.getValue(currentEntity);
        }
    }

    private void executeWithCache(ScoreDirector<Solution_> scoreDirector, Object entity, Object inverseElement) {
        var currentEntity = entity;
        boolean[] entitiesVisited = entityVisitMap.computeIfAbsent(inverseElement,
                element -> new boolean[sourceListVariable.getListSize(inverseElement)]);
        int index = indexElementOperation.getValue(currentEntity);
        while (currentEntity != null && !entitiesVisited[index]) {
            if (!execute(scoreDirector, currentEntity)) {
                entitiesVisited[index] = true;
                break;
            }
            entitiesVisited[index++] = true;
            currentEntity = nextElementOperation.getValue(currentEntity);
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

}
