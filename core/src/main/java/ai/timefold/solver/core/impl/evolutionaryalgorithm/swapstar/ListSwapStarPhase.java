package ai.timefold.solver.core.impl.evolutionaryalgorithm.swapstar;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

/**
 * Implementation of the SWAP* method described in the article:
 * <p>
 * Hybrid Genetic Search for th CVRP: Open-Source Implementation and SWAP* Neighborhood by Thibaut Vidal
 * <p>
 * The author explains
 * that the method involves selecting the best swap move between two planning values from different planning entities.
 * Instead of being applied in place, the swap move allows each planning value to be positioned differently,
 * resembling a change move instead.
 * <p>
 * The original implementation uses a geometric calculation based on polar sectors
 * to apply the move only to overlapping routes.
 * Conversely, the author mentions the option of using other strategies to locate nearby planning entities,
 * such as distance.
 * Therefore,
 * the proposed approach uses the Nearby feature
 * and evaluates only the three closest planning entities for a given source.
 * 
 * @param <Solution_> the solution type
 */
public class ListSwapStarPhase<Solution_> extends AbstractPhase<Solution_> {

    private final EntitySelector<Solution_> originalEntitySelector;
    private final EntitySelector<Solution_> innerEntitySelector;

    private ListVariableDescriptor<Solution_> listVariableDescriptor;

    protected ListSwapStarPhase(Builder<Solution_> builder) {
        super(builder);
        this.originalEntitySelector = builder.originalEntitySelector;
        this.innerEntitySelector = builder.innerEntitySelector;

    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public PhaseType getPhaseType() {
        return PhaseType.LOCAL_SEARCH;
    }

    @Override
    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return EventProducerId::localSearch;
    }

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        phaseStarted(phaseScope);
        var originalEntityIterator = originalEntitySelector.iterator();
        var lastUpdateEntityMap = new LastUpdateVersionMap();
        BestMoveMap<Solution_, ?> bestMoveMap = new BestMoveMap<>((int) solverScope.getProblemSizeStatistics().entityCount());
        while (originalEntityIterator.hasNext()) {
            var sourceEntity = originalEntityIterator.next();
            if (listVariableDescriptor.getListSize(sourceEntity) == 0) {
                continue;
            }
            var sourceEntityVersion = lastUpdateEntityMap.getVersion(sourceEntity);
            var otherEntityIterator = innerEntitySelector.iterator();
            while (otherEntityIterator.hasNext()) {
                var otherEntity = otherEntityIterator.next();
                var otherEntityVersion = lastUpdateEntityMap.getVersion(otherEntity);
                if (listVariableDescriptor.getListSize(otherEntity) == 0
                        || (otherEntityVersion > -1 && sourceEntityVersion >= otherEntityVersion)) {
                    continue;
                }
                swapStar(solverScope, lastUpdateEntityMap, bestMoveMap, sourceEntity, otherEntity);
            }
        }
        phaseEnded(phaseScope);
    }

    private <Score_ extends Score<Score_>> void swapStar(SolverScope<Solution_> solverScope,
            LastUpdateVersionMap lastUpdateVersionMap,
            BestMoveMap<Solution_, Score_> bestMoveMap, Object sourceEntity, Object otherEntity) {
        // Compute the best three moves for both entities
        findThreeBestLocations(solverScope, bestMoveMap, sourceEntity, otherEntity);

        // Use the three best moves from each entity to evaluate the best composite move among them
        var sourceBestMoveLocation = bestMoveMap.getBestMoveLocation(sourceEntity, otherEntity);
        var otherBestMoveLocation = bestMoveMap.getBestMoveLocation(otherEntity, sourceEntity);
        var bestPairScore = sourceBestMoveLocation.bestMoves[2];
        if (otherBestMoveLocation.compareBest(sourceBestMoveLocation) > 0) {
            bestPairScore = otherBestMoveLocation.bestMoves[2];
        }

        for (var i = 0; i < 3; i++) {
            if (sourceBestMoveLocation.bestMoves[i] == null) {
                continue;
            }
            for (var j = i + 1; j < 3; j++) {
                if (otherBestMoveLocation.bestMoves[j] == null) {
                    continue;
                }
                var compositeMoveDescriptor = computeCompositeBestLocation(solverScope, sourceBestMoveLocation.bestMoves[i],
                        otherBestMoveLocation.bestMoves[j]);
                if (bestPairScore == null || compositeMoveDescriptor.score().compareTo(bestPairScore.score()) > 0) {
                    bestPairScore = compositeMoveDescriptor;
                }
            }
        }

        // Apply the best score
        if (bestPairScore.score().compareTo(solverScope.<Score_> getBestScore().raw()) > 0) {
            solverScope.<Score_> getScoreDirector().executeMove(bestPairScore.move());
            lastUpdateVersionMap.updateVersion(sourceEntity, otherEntity);
        }
    }

    private <Score_ extends Score<Score_>> void findThreeBestLocations(SolverScope<Solution_> solverScope,
            BestMoveMap<Solution_, Score_> bestMoveMap, Object sourceEntity, Object otherEntity) {
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        var sourceStartyPos = listVariableDescriptor.getFirstUnpinnedIndex(sourceEntity);
        var otherStartPos = listVariableDescriptor.getFirstUnpinnedIndex(otherEntity);

        // Find the best move for each value between both entities
        for (var i = sourceStartyPos; i < listVariableDescriptor.getListSize(sourceEntity); i++) {
            var otherEntityListSize = listVariableDescriptor.getListSize(otherEntity);
            for (var j = otherStartPos; j <= otherEntityListSize; j++) {
                var listChangeMove = new ListChangeMove<>(listVariableDescriptor, sourceEntity, i, otherEntity, j);
                var moveScore = scoreDirector.getMoveDirector()
                        .executeTemporary(listChangeMove, (score, move) -> score);
                bestMoveMap.updateBestLocation(sourceEntity, i, otherEntity, j, listChangeMove, moveScore.raw());
                if (j < otherEntityListSize) {
                    var otherListChangeMove = new ListChangeMove<>(listVariableDescriptor, otherEntity, j, sourceEntity, i);
                    var otherMoveScore = scoreDirector.getMoveDirector()
                            .executeTemporary(otherListChangeMove, (score, move) -> score);
                    bestMoveMap.updateBestLocation(otherEntity, j, sourceEntity, i, otherListChangeMove, otherMoveScore.raw());
                }
            }
        }
        // One last iteration to compute last position for otherEntity
        var sourceEntityListSize = listVariableDescriptor.getListSize(sourceEntity);
        for (var j = otherStartPos; j < listVariableDescriptor.getListSize(otherEntity); j++) {
            var otherListChangeMove =
                    new ListChangeMove<>(listVariableDescriptor, otherEntity, j, sourceEntity, sourceEntityListSize);
            var otherMoveScore = scoreDirector.getMoveDirector()
                    .executeTemporary(otherListChangeMove, (score, move) -> score);
            bestMoveMap.updateBestLocation(otherEntity, j, sourceEntity, sourceEntityListSize, otherListChangeMove,
                    otherMoveScore.raw());
        }
    }

    private <Score_ extends Score<Score_>> MoveDescriptor<Solution_, Score_> computeCompositeBestLocation(
            SolverScope<Solution_> solverScope, MoveDescriptor<Solution_, Score_> sourceMoveDescriptor,
            MoveDescriptor<Solution_, Score_> otherMoveDescriptor) {
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        var sourceList = listVariableDescriptor.getValue(sourceMoveDescriptor.sourceEntity());
        var otherList = listVariableDescriptor.getValue(otherMoveDescriptor.sourceEntity());

        var unassignSourceMove = new ListUnassignMove<>(listVariableDescriptor, sourceMoveDescriptor.sourceEntity(),
                sourceMoveDescriptor.i());
        var sourceJ = sourceMoveDescriptor.j();
        if (sourceJ == otherList.size()) {
            sourceJ--;
        }
        var assignSourceMove = new ListAssignMove<>(listVariableDescriptor, sourceList.get(sourceMoveDescriptor.i()),
                sourceMoveDescriptor.otherEntity, sourceJ);

        var unassignOtherMove = new ListUnassignMove<>(listVariableDescriptor, otherMoveDescriptor.sourceEntity(),
                otherMoveDescriptor.i());
        var otherJ = otherMoveDescriptor.j();
        if (otherJ == sourceList.size()) {
            otherJ--;
        }
        var assignOtherMove = new ListAssignMove<>(listVariableDescriptor, otherList.get(otherMoveDescriptor.i()),
                otherMoveDescriptor.otherEntity, otherJ);

        // Unassign both values and reassign them
        var compositeMove = Moves.compose(unassignSourceMove, unassignOtherMove, assignSourceMove, assignOtherMove);
        var moveScore = scoreDirector.getMoveDirector().executeTemporary(compositeMove, (score, move) -> score.raw());
        return new MoveDescriptor<>(sourceMoveDescriptor.sourceEntity(), -1, otherMoveDescriptor.sourceEntity(), -1,
                compositeMove, moveScore);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        originalEntitySelector.solvingStarted(solverScope);
        innerEntitySelector.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        originalEntitySelector.solvingEnded(solverScope);
        innerEntitySelector.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        originalEntitySelector.phaseStarted(phaseScope);
        innerEntitySelector.phaseStarted(phaseScope);
        this.listVariableDescriptor = phaseScope.getScoreDirector().getSolutionDescriptor().getListVariableDescriptor();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        originalEntitySelector.phaseEnded(phaseScope);
        innerEntitySelector.phaseEnded(phaseScope);
        this.listVariableDescriptor = null;
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final EntitySelector<Solution_> originalEntitySelector;
        private final EntitySelector<Solution_> innerEntitySelector;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                EntitySelector<Solution_> originalEntitySelector, EntitySelector<Solution_> innerEntitySelector) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.originalEntitySelector = originalEntitySelector;
            this.innerEntitySelector = innerEntitySelector;
        }

        @Override
        public ListSwapStarPhase<Solution_> build() {
            return new ListSwapStarPhase<>(this);
        }
    }

    private static class LastUpdateVersionMap {
        private int currentVersion = 0;
        private final Map<Object, Integer> versionMap = new IdentityHashMap<>();

        void updateVersion(Object entity, Object otherEntity) {
            currentVersion++;
            versionMap.compute(entity, (key, value) -> currentVersion);
            versionMap.compute(otherEntity, (key, value) -> currentVersion);
        }

        int getVersion(Object entity) {
            var version = versionMap.get(entity);
            if (version == null) {
                versionMap.put(entity, currentVersion);
                return -1;
            }
            return version;
        }
    }

    private static class BestMoveMap<Solution_, Score_ extends Score<Score_>> {

        private final int entitySize;
        private final Map<Object, Map<Object, BestMoveLocation<Solution_, Score_>>> valuesMap;

        BestMoveMap(int entitySize) {
            this.entitySize = entitySize;
            valuesMap = CollectionUtils.newIdentityHashMap(entitySize);
        }

        void updateBestLocation(Object sourceEntity, int i, Object otherEntity, int j, Move<Solution_> move, Score_ score) {
            var bestMoveLocation = getBestMoveLocation(sourceEntity, otherEntity);
            bestMoveLocation.updateLocation(sourceEntity, i, otherEntity, j, move, score);
        }

        BestMoveLocation<Solution_, Score_> getBestMoveLocation(Object sourceEntity, Object otherEntity) {
            var sourceMap = valuesMap.get(sourceEntity);
            if (sourceMap == null) {
                sourceMap = CollectionUtils.newIdentityHashMap(entitySize);
                valuesMap.put(sourceEntity, sourceMap);
            }
            var bestMoveLocation = sourceMap.get(otherEntity);
            if (bestMoveLocation == null) {
                bestMoveLocation = new BestMoveLocation<>();
                sourceMap.put(otherEntity, bestMoveLocation);
            }
            return bestMoveLocation;
        }
    }

    private static class BestMoveLocation<Solution_, Score_ extends Score<Score_>> {

        private MoveDescriptor<Solution_, Score_>[] bestMoves = new MoveDescriptor[3];

        void updateLocation(Object sourceEntity, int i, Object otherEntity, int j, Move<Solution_> move, Score_ score) {
            if (bestMoves[2] == null || score.compareTo(bestMoves[2].score()) > 0) {
                bestMoves[0] = bestMoves[1];
                bestMoves[1] = bestMoves[2];
                bestMoves[2] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            } else if (bestMoves[1] == null || score.compareTo(bestMoves[1].score()) > 0) {
                bestMoves[0] = bestMoves[1];
                bestMoves[1] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            } else if (bestMoves[0] == null || score.compareTo(bestMoves[0].score()) > 0) {
                bestMoves[0] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            }
        }

        private int compareBest(BestMoveLocation<Solution_, Score_> otherLocation) {
            if (bestMoves[2] != null && otherLocation != null && otherLocation.bestMoves[2] != null) {
                return bestMoves[2].score().compareTo(otherLocation.bestMoves[2].score());
            } else if (bestMoves[2] != null) {
                return 1;
            } else if (otherLocation != null && otherLocation.bestMoves[2] != null) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private record MoveDescriptor<Solution_, Score_ extends Score<Score_>>(Object sourceEntity, int i, Object otherEntity,
            int j, Move<Solution_> move, Score_ score) {
    }
}
