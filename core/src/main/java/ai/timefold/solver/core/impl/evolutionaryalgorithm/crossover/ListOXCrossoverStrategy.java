package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;

/**
 * Default implementation of OX crossover strategy for list variables.
 */
@NullMarked
public class ListOXCrossoverStrategy<Solution_, Score_ extends Score<Score_>> implements CrossoverStrategy<Solution_, Score_> {

    /**
     * The method
     * used to transfer genetic material from parents to offspring analyzes the solution as a single sequence of planned values.
     * Let's consider a solution with two entities e1[v1, v2, v3] and e2[v4, v5].
     * The encoded solution is represented by a single sequence of planning values [v1, v2, v3, v4, v5].
     * <p>
     * Let's assume the cut point is [1, 3].
     * The planning values from the first parent to incorporate into the individual are [v2, v3, v4].
     * The remaining values are added based on the solution provided by the second parent.
     * 
     * @param context the crossover context
     * @return a new individual generated according to the OX strategy.
     */
    @Override
    public CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context) {
        // Generate the cut point
        var size = context.firstIndividual().size();
        var startIdx = context.workingRandom().nextInt(size);
        var endIdx = context.workingRandom().nextInt(size);
        while (startIdx == endIdx) {
            endIdx = context.workingRandom().nextInt(size);
        }
        if (startIdx > endIdx) {
            var newEndIdx = startIdx;
            startIdx = endIdx;
            endIdx = newEndIdx;
        }
        // The solution must be already set in the offspring solver scope
        var scoreDirector = context.scoreDirector();
        var offspringSolution = scoreDirector.cloneSolution(context.firstIndividual().getSolution());
        scoreDirector.setWorkingSolution(offspringSolution);
        // Remove all elements that will be inherited by the second parent
        var preservedElementSet = removeElementsOutsideRange(offspringSolution, startIdx, endIdx, scoreDirector);
        // Add the remaining planning values from the second parent
        addRemainingElements(offspringSolution, context.secondIndividual().getSolution(), preservedElementSet, scoreDirector);
        return new CrossoverResult<>(offspringSolution, scoreDirector.calculateScore());
    }

    private Set<Object> removeElementsOutsideRange(Solution_ solution, int start, int end,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var allPreservedValueSet = CollectionUtils.newIdentityHashSet(end - start + 1);
        var listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        var allEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        var unassignMoveList = new ArrayList<Move<Solution_>>(
                (int) scoreDirector.getValueRangeManager().getProblemSizeStatistics().approximateValueCount());
        var listVariableMetaModel = scoreDirector.getSolutionDescriptor().getListVariableDescriptor().getVariableMetaModel();
        var idx = 0;
        for (var entity : allEntityList) {
            var valueList = listVariableDescriptor.getValue(entity);
            var expectedSize = idx + valueList.size();
            if (expectedSize < start || idx > end) {
                idx += valueList.size();
                for (var i = 0; i < valueList.size(); i++) {
                    unassignMoveList.add(Moves.unassign(listVariableMetaModel, entity, i));
                }
            } else {
                for (var i = 0; i < valueList.size(); i++) {
                    var value = valueList.get(i);
                    if (idx >= start && idx <= end) {
                        allPreservedValueSet.add(value);
                    } else {
                        unassignMoveList.add(Moves.unassign(listVariableMetaModel, entity, i));
                    }
                    idx++;
                }
            }
        }
        // Unassign all discarded values
        Collections.reverse(unassignMoveList);
        scoreDirector.executeMove(Moves.compose(unassignMoveList));
        return allPreservedValueSet;
    }

    private void addRemainingElements(Solution_ offspringSolution, Solution_ parentSolution, Set<Object> preservedElementSet,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        var allOffspringEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(offspringSolution);
        var allParentEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(parentSolution);
        var assignMoveList = new ArrayList<Move<Solution_>>(
                (int) scoreDirector.getValueRangeManager().getProblemSizeStatistics().approximateValueCount());
        var listVariableMetaModel = scoreDirector.getSolutionDescriptor().getListVariableDescriptor().getVariableMetaModel();
        for (var i = 0; i < allOffspringEntityList.size(); i++) {
            var offspringEntity = allOffspringEntityList.get(i);
            var offspringListSize = listVariableDescriptor.getValue(offspringEntity).size();
            var parentEntity = allParentEntityList.get(i);
            var parentValueList = listVariableDescriptor.getValue(parentEntity);
            var idx = 0;
            for (var value : parentValueList) {
                var rebasedValue = scoreDirector.getMoveDirector().rebase(value);
                if (preservedElementSet.contains(rebasedValue)) {
                    continue;
                }
                assignMoveList.add(Moves.assign(listVariableMetaModel, Objects.requireNonNull(rebasedValue), offspringEntity,
                        offspringListSize + idx));
                idx++;
            }
        }
        // Assign all new values
        scoreDirector.executeMove(Moves.compose(assignMoveList));
    }
}
