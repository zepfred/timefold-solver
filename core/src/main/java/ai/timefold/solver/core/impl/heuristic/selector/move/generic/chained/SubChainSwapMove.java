package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 * This {@link Move} is not cacheable.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubChainSwapMove<Solution_> extends AbstractMove<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected final SubChain leftSubChain;
    protected final Object leftTrailingLastEntity;
    protected final SubChain rightSubChain;
    protected final Object rightTrailingLastEntity;

    public SubChainSwapMove(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply,
            SubChain leftSubChain, SubChain rightSubChain) {
        this.variableDescriptor = variableDescriptor;
        this.leftSubChain = leftSubChain;
        leftTrailingLastEntity = inverseVariableSupply.getInverseSingleton(leftSubChain.getLastEntity());
        this.rightSubChain = rightSubChain;
        rightTrailingLastEntity = inverseVariableSupply.getInverseSingleton(rightSubChain.getLastEntity());
    }

    public SubChainSwapMove(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SubChain leftSubChain, Object leftTrailingLastEntity, SubChain rightSubChain,
            Object rightTrailingLastEntity) {
        this.variableDescriptor = variableDescriptor;
        this.leftSubChain = leftSubChain;
        this.rightSubChain = rightSubChain;
        this.leftTrailingLastEntity = leftTrailingLastEntity;
        this.rightTrailingLastEntity = rightTrailingLastEntity;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public SubChain getLeftSubChain() {
        return leftSubChain;
    }

    public SubChain getRightSubChain() {
        return rightSubChain;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return !containsAnyOf(rightSubChain, leftSubChain);
    }

    static boolean containsAnyOf(SubChain rightSubChain, SubChain leftSubChain) {
        int leftSubChainSize = leftSubChain.getSize();
        if (leftSubChainSize == 0) {
            return false;
        } else if (leftSubChainSize == 1) { // No optimization possible.
            return rightSubChain.getEntityList().contains(leftSubChain.getFirstEntity());
        }
        /*
         * In order to find an entity in another subchain, we need to do contains() on a List.
         * List.contains() is O(n), the performance gets worse with increasing size.
         * Subchains here can easily have hundreds, thousands of elements.
         * As Set.contains() is O(1), independent of set size, copying the list outperforms the lookup by a lot.
         * Therefore this code converts the List lookup to HashSet lookup, in situations with repeat lookup.
         */
        Set<Object> rightSubChainEntityFastLookupSet = new HashSet<>(rightSubChain.getEntityList());
        for (Object leftEntity : leftSubChain.getEntityList()) {
            if (rightSubChainEntityFastLookupSet.contains(leftEntity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var leftFirstEntity = leftSubChain.getFirstEntity();
        var leftFirstValue = variableDescriptor.getValue(leftFirstEntity);
        var leftLastEntity = leftSubChain.getLastEntity();
        var rightFirstEntity = rightSubChain.getFirstEntity();
        var rightFirstValue = variableDescriptor.getValue(rightFirstEntity);
        var rightLastEntity = rightSubChain.getLastEntity();
        // Change the entities
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (leftLastEntity != rightFirstValue) {
            castScoreDirector.changeVariableFacade(variableDescriptor, leftFirstEntity, rightFirstValue);
        }
        if (rightLastEntity != leftFirstValue) {
            castScoreDirector.changeVariableFacade(variableDescriptor, rightFirstEntity, leftFirstValue);
        }
        // Reroute the new chains
        if (leftTrailingLastEntity != null) {
            if (leftTrailingLastEntity != rightFirstEntity) {
                castScoreDirector.changeVariableFacade(variableDescriptor, leftTrailingLastEntity, rightLastEntity);
            } else {
                castScoreDirector.changeVariableFacade(variableDescriptor, leftFirstEntity, rightLastEntity);
            }
        }
        if (rightTrailingLastEntity != null) {
            if (rightTrailingLastEntity != leftFirstEntity) {
                castScoreDirector.changeVariableFacade(variableDescriptor, rightTrailingLastEntity, leftLastEntity);
            } else {
                castScoreDirector.changeVariableFacade(variableDescriptor, rightFirstEntity, leftLastEntity);
            }
        }
    }

    @Override
    public SubChainSwapMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubChainSwapMove<>(variableDescriptor,
                leftSubChain.rebase(destinationScoreDirector),
                destinationScoreDirector.lookUpWorkingObject(leftTrailingLastEntity),
                rightSubChain.rebase(destinationScoreDirector),
                destinationScoreDirector.lookUpWorkingObject(rightTrailingLastEntity));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return CollectionUtils.concat(leftSubChain.getEntityList(), rightSubChain.getEntityList());
    }

    @Override
    public Collection<?> getPlanningValues() {
        return Arrays.asList(
                variableDescriptor.getValue(leftSubChain.getFirstEntity()),
                variableDescriptor.getValue(rightSubChain.getFirstEntity()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SubChainSwapMove<?> other = (SubChainSwapMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(leftSubChain, other.leftSubChain) &&
                Objects.equals(rightSubChain, other.rightSubChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, leftSubChain, rightSubChain);
    }

    @Override
    public String toString() {
        Object oldLeftValue = variableDescriptor.getValue(leftSubChain.getFirstEntity());
        Object oldRightValue = variableDescriptor.getValue(rightSubChain.getFirstEntity());
        return leftSubChain.toDottedString() + " {" + oldLeftValue + "} <-> "
                + rightSubChain.toDottedString() + " {" + oldRightValue + "}";
    }

}
