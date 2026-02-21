package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.Map;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Rebaser;

/**
 * Default representation of an individual for list variables.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
public final class ListVariableIndividual<Solution_, Score_ extends Score<Score_>>
        extends AbstractIndividual<Solution_, Score_> {

    private final Rebaser rebaser;
    private final ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;
    private final CountableValueRange<Object> cachedValueRange;
    private final Map<Object, Object[]> predecessorAndSuccessorMap;

    public ListVariableIndividual(Solution_ solution, InnerScore<Score_> score,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        super(solution, score);
        this.rebaser = scoreDirector.getMoveDirector();
        var listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        this.listVariableStateSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor);
        this.cachedValueRange =
                scoreDirector.getValueRangeManager().getFromSolution(listVariableDescriptor.getValueRangeDescriptor());
        // Currently, there is one score director and multiple individuals,
        // and the ListVariableStateSupply cannot be used to get the predecessor and successor values from different individuals. 
        // Consequently, we need to calculate those values in advance.
        predecessorAndSuccessorMap = CollectionUtils.newIdentityHashMap((int) cachedValueRange.getSize());
        loadPredecessorAndSuccessorMap(listVariableDescriptor);
    }

    private void loadPredecessorAndSuccessorMap(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        var allEntities = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        for (var entity : allEntities) {
            var valueList = listVariableDescriptor.getValue(entity);
            var valueListSize = valueList.size() - 1;
            for (var i = 0; i < valueList.size(); i++) {
                var predecessor = i > 0 ? valueList.get(i - 1) : null;
                var successor = i < valueListSize ? valueList.get(i + 1) : null;
                predecessorAndSuccessorMap.put(valueList.get(i), new Object[] { predecessor, successor });
            }
        }
    }

    @Override
    public double diff(Individual<Solution_, Score_> otherIndividual) {
        var otherListIndividual = (ListVariableIndividual<Solution_, Score_>) otherIndividual;
        var diff = 0;
        for (var i = 0; i < cachedValueRange.getSize(); i++) {
            if (cachedValueRange.get(i) == null) {
                continue;
            }
            var valuePair = getPredecessorAndSuccessor(i);
            var predecessor = valuePair[0];
            var successor = valuePair[1];
            var otherValuePair = otherListIndividual.getPredecessorAndSuccessor(i);
            var otherPredecessor = otherValuePair[0];
            var otherSuccessor = otherValuePair[1];
            // No match like: [0, 1] and [1, 0] 
            if (successor != otherSuccessor && successor != otherPredecessor) {
                diff++;
            }
            // No match between the first element and the last element of each value
            if (predecessor == null && otherPredecessor != null && otherSuccessor != null) {
                diff++;
            }
        }
        return (double) diff / (double) cachedValueRange.getSize();
    }

    private Object[] getPredecessorAndSuccessor(int index) {
        var predecessorAndSuccessor = predecessorAndSuccessorMap.get(cachedValueRange.get(index));
        return new Object[] { rebaser.rebase(predecessorAndSuccessor[0]), rebaser.rebase(predecessorAndSuccessor[1]) };
    }
}
