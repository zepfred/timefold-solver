package ai.timefold.solver.core.impl.heuristic.selector.entity;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * This is the common {@link EntitySelector} implementation.
 */
public final class FromSolutionEntitySelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final SelectionCacheType minimumCacheType;
    private final boolean randomSelection;

    private List<Object> cachedEntityList = null;
    private Long cachedEntityListRevision = null;
    private boolean cachedEntityListIsDirty = false;

    public FromSolutionEntitySelector(EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        this.entityDescriptor = entityDescriptor;
        this.minimumCacheType = minimumCacheType;
        this.randomSelection = randomSelection;
    }

    @Override
    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return entityDescriptor;
    }

    /**
     * @return never null, at least {@link SelectionCacheType#STEP}
     */
    @Override
    public SelectionCacheType getCacheType() {
        var intrinsicCacheType = SelectionCacheType.STEP;
        return (intrinsicCacheType.compareTo(minimumCacheType) > 0)
                ? intrinsicCacheType
                : minimumCacheType;
    }

    // ************************************************************************
    // Cache lifecycle methods
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        InnerScoreDirector<Solution_, ?> scoreDirector = phaseScope.getScoreDirector();
        reloadCachedEntityList(scoreDirector);
    }

    private void reloadCachedEntityList(InnerScoreDirector<Solution_, ?> scoreDirector) {
        cachedEntityList = entityDescriptor.extractEntities(scoreDirector.getWorkingSolution());
        cachedEntityListRevision = scoreDirector.getWorkingEntityListRevision();
        cachedEntityListIsDirty = false;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        var scoreDirector = stepScope.getScoreDirector();
        if (scoreDirector.isWorkingEntityListDirty(cachedEntityListRevision)) {
            if (minimumCacheType.compareTo(SelectionCacheType.STEP) > 0) {
                cachedEntityListIsDirty = true;
            } else {
                reloadCachedEntityList(scoreDirector);
            }
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        cachedEntityList = null;
        cachedEntityListRevision = null;
        cachedEntityListIsDirty = false;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        // CachedListRandomIterator is neverEnding
        return randomSelection;
    }

    @Override
    public long getSize() {
        return cachedEntityList.size();
    }

    @Override
    public Iterator<Object> iterator() {
        checkCachedEntityListIsDirty();
        if (!randomSelection) {
            return cachedEntityList.iterator();
        } else {
            return new CachedListRandomIterator<>(cachedEntityList, workingRandom);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        checkCachedEntityListIsDirty();
        if (!randomSelection) {
            return cachedEntityList.listIterator();
        } else {
            throw new IllegalStateException("The selector (" + this
                    + ") does not support a ListIterator with randomSelection (" + randomSelection + ").");
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        checkCachedEntityListIsDirty();
        if (!randomSelection) {
            return cachedEntityList.listIterator(index);
        } else {
            throw new IllegalStateException("The selector (" + this
                    + ") does not support a ListIterator with randomSelection (" + randomSelection + ").");
        }
    }

    @Override
    public Iterator<Object> endingIterator() {
        checkCachedEntityListIsDirty();
        return cachedEntityList.iterator();
    }

    private void checkCachedEntityListIsDirty() {
        if (cachedEntityListIsDirty) {
            throw new IllegalStateException("The selector (" + this + ") with minimumCacheType (" + minimumCacheType
                    + ")'s workingEntityList became dirty between steps but is still used afterwards.");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        var that = (FromSolutionEntitySelector<?>) other;
        return randomSelection == that.randomSelection && Objects.equals(entityDescriptor, that.entityDescriptor)
                && minimumCacheType == that.minimumCacheType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityDescriptor, minimumCacheType, randomSelection);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entityDescriptor.getEntityClass().getSimpleName() + ")";
    }

}
