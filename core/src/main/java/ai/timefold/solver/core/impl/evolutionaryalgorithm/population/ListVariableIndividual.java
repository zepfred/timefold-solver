package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Default representation of an individual for list variables.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
@NullMarked
public final class ListVariableIndividual<Solution_, Score_ extends Score<Score_>>
        extends AbstractIndividual<Solution_, Score_> {

    private final MemberAccessor planningIdAccessor;
    private final Map<Object, PositionPair> predecessorAndSuccessorMap;

    public ListVariableIndividual(Solution_ solution, InnerScore<Score_> score,
            InnerScoreDirector<Solution_, Score_> scoreDirector) {
        super(solution, score);
        var listVariableDescriptor = Objects.requireNonNull(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
        this.planningIdAccessor =
                scoreDirector.getSolutionDescriptor().getPlanningIdAccessor(listVariableDescriptor.getElementType());
        if (planningIdAccessor == null) {
            throw new IllegalStateException(
                    "The planning value class (%s) must include a planning value id field."
                            .formatted(listVariableDescriptor.getElementType()));
        }
        this.predecessorAndSuccessorMap = new LinkedHashMap<>(
                (int) scoreDirector.getValueRangeManager().getProblemSizeStatistics().approximateValueCount());
        load(solution, listVariableDescriptor);
    }

    private void load(Solution_ solution, ListVariableDescriptor<Solution_> listVariableDescriptor) {
        var allEntities = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        for (var entity : allEntities) {
            var valueList = listVariableDescriptor.getValue(entity);
            var valueListSize = valueList.size() - 1;
            for (var i = 0; i < valueList.size(); i++) {
                var valueId = planningIdAccessor.executeGetter(valueList.get(i));
                var predecessorId = i > 0 ? planningIdAccessor.executeGetter(valueList.get(i - 1)) : null;
                var successorId = i < valueListSize ? planningIdAccessor.executeGetter(valueList.get(i + 1)) : null;
                predecessorAndSuccessorMap.put(valueId, new PositionPair(predecessorId, successorId));
            }
        }
    }

    @Override
    public int size() {
        return predecessorAndSuccessorMap.size();
    }

    @Override
    public double diff(Individual<Solution_, Score_> otherIndividual) {
        var otherListIndividual = (ListVariableIndividual<Solution_, Score_>) otherIndividual;
        var diff = 0;
        for (var valueEntry : predecessorAndSuccessorMap.entrySet()) {
            var valuePosition = valueEntry.getValue();
            var otherValuePosition = otherListIndividual.predecessorAndSuccessorMap.get(valueEntry.getKey());
            if (otherValuePosition == null) {
                diff++;
                continue;
            }
            // No match like: [0, 1] and [1, 0] 
            if (valuePosition.successor() != otherValuePosition.successor()
                    && valuePosition.successor() != otherValuePosition.predecessor()) {
                diff++;
            }
            // No match between the first element and the last element of each value
            if (valuePosition.predecessor() == null && otherValuePosition.predecessor() != null
                    && otherValuePosition.successor() != null) {
                diff++;
            }
        }
        return (double) diff / (double) predecessorAndSuccessorMap.size();
    }

    private record PositionPair(@Nullable Object predecessor, @Nullable Object successor) {

    }
}
