package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import java.util.ArrayList;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.context.IndividualGenerator;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;

/**
 * Default implementation of OX crossover strategy for list variables.
 */
@NullMarked
public class ListOXCrossoverStrategy<Solution_, Score_ extends Score<Score_>> implements CrossoverStrategy<Solution_, Score_> {

    private final IndividualGenerator<Solution_, Score_> individualGenerator;

    public ListOXCrossoverStrategy(IndividualGenerator<Solution_, Score_> individualGenerator) {
        this.individualGenerator = individualGenerator;
    }

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
     * @param offspringSolverScope the offspring solver scope
     * @param firstIndividual the first parent
     * @param secondIndividual the second parent
     * @return a new individual generated according to the OX strategy.
     */
    @Override
    public Individual<Solution_, Score_> apply(SolverScope<Solution_> offspringSolverScope,
            Individual<Solution_, Score_> firstIndividual, Individual<Solution_, Score_> secondIndividual) {
        // Generate the cut point
        var size = firstIndividual.size();
        var startIdx = offspringSolverScope.getWorkingRandom().nextInt(size);
        var endIdx = offspringSolverScope.getWorkingRandom().nextInt(size);
        while (startIdx == endIdx) {
            endIdx = offspringSolverScope.getWorkingRandom().nextInt(size);
        }
        if (startIdx > endIdx) {
            var newEndIdx = startIdx;
            startIdx = endIdx;
            endIdx = newEndIdx;
        }
        // Clone the solution and generate the offspring that will receive the new solution
        var scoreDirector = offspringSolverScope.<Score_> getScoreDirector();
        var offspringSolution = scoreDirector.cloneSolution(firstIndividual.getSolution());
        scoreDirector.setWorkingSolution(offspringSolution);
        // Remove all elements that will be inherited by the second parent
        var preservedElementSet = removeElementsOutsideRange(offspringSolution, startIdx, endIdx, scoreDirector);
        // Add the remaining planning values from the second parent
        addRemainingElements(offspringSolution, secondIndividual.getSolution(), preservedElementSet, scoreDirector);
        scoreDirector.forceTriggerVariableListeners();
        var score = scoreDirector.calculateScore();
        offspringSolverScope.setBestSolution(offspringSolution);
        offspringSolverScope.setBestScore(score);
        offspringSolverScope.setStartingInitializedScore(score.raw());
        return individualGenerator.generateIndividual(offspringSolution, score, scoreDirector);
    }

    private Set<Object> removeElementsOutsideRange(Solution_ solution, int start, int end,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var allPreservedValueSet = CollectionUtils.newIdentityHashSet(end - start + 1);
        var listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        var allEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        var idx = 0;
        for (var entity : allEntityList) {
            var valueList = listVariableDescriptor.getValue(entity);
            var preservedValueList = new ArrayList<>(valueList.size());
            for (Object object : valueList) {
                if (idx >= start && idx <= end) {
                    preservedValueList.add(object);
                }
                idx++;
            }
            valueList.clear();
            valueList.addAll(preservedValueList);
            allPreservedValueSet.addAll(preservedValueList);
        }
        return allPreservedValueSet;
    }

    private void addRemainingElements(Solution_ offspringSolution, Solution_ parentSolution, Set<Object> preservedElementSet,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        var allOffspringEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(offspringSolution);
        var allParentEntityList = listVariableDescriptor.getEntityDescriptor().extractEntities(parentSolution);
        for (var i = 0; i < allOffspringEntityList.size(); i++) {
            var offspringEntity = allOffspringEntityList.get(i);
            var offspringValueList = listVariableDescriptor.getValue(offspringEntity);
            var parentEntity = allParentEntityList.get(i);
            var parentValueList = listVariableDescriptor.getValue(parentEntity);
            for (var value : parentValueList) {
                var rebasedValue = scoreDirector.getMoveDirector().rebase(value);
                if (preservedElementSet.contains(rebasedValue)) {
                    continue;
                }
                offspringValueList.add(rebasedValue);
            }
        }
    }
}
