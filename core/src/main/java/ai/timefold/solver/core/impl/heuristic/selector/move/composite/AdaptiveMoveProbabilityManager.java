package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveMoveProbabilityManager<Solution_> implements PhaseLifecycleListener<Solution_> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveMoveProbabilityManager.class);
    private final List<MoveSelector<Solution_>> childMoveSelectorList;
    private final List<ProbabilityItem<Solution_>> probabilityItemList;
    private Score<?> currentBest;
    private double probabilityWeightTotal;

    private static final int MAX_MOVE_EVALUATION_PER_ITER = 10_000;
    private int selectedMoveIndex;

    public AdaptiveMoveProbabilityManager(List<MoveSelector<Solution_>> childMoveSelectorList) {
        this.childMoveSelectorList = childMoveSelectorList;
        this.probabilityItemList = new ArrayList<>(childMoveSelectorList.size());
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    protected void reset() {
        probabilityItemList.clear();
        for (var moveSelector : childMoveSelectorList) {
            var moveIterator = moveSelector.iterator();
            if (moveIterator.hasNext()) {
                ProbabilityItem<Solution_> probabilityItem = new ProbabilityItem<>(this);
                probabilityItem.moveSelector = moveSelector;
                probabilityItem.moveIterator = new AdaptiveIteratorAdapter<>(moveIterator, probabilityItem);
                probabilityItem.weight = 1;
                probabilityItem.moveCount = 0;
                probabilityItem.maxMoveCount = MAX_MOVE_EVALUATION_PER_ITER;
                probabilityItemList.add(probabilityItem);
                probabilityWeightTotal++;
            }
        }
    }

    protected boolean hasIteratorsAvailable() {
        return probabilityWeightTotal > 0;
    }

    protected Iterator<Move<Solution_>> getSelectedMoveIterator() {
        return probabilityItemList.get(selectedMoveIndex).moveIterator;
    }

    public void removeSelectedMoveIterator() {
        var item = probabilityItemList.remove(selectedMoveIndex);
        probabilityWeightTotal -= item.weight;
        recalculateMoveCount();
    }

    private void incrementMoveIndex() {
        selectedMoveIndex = (selectedMoveIndex + 1) % probabilityItemList.size();
        if (selectedMoveIndex == 0) {
            recalculateMoveCount();
        }
    }

    private void recalculateMoveCount() {
        for (var item : probabilityItemList) {
            item.moveCount = 0;
            item.maxMoveCount = (int) Math.floor((item.weight / probabilityWeightTotal) * MAX_MOVE_EVALUATION_PER_ITER);
        }
    }

    // ************************************************************************
    // lifecycle methods
    // ************************************************************************
    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        this.probabilityWeightTotal = 0;
        this.selectedMoveIndex = 0;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        this.currentBest = stepScope.getPhaseScope().getBestScore();
        if (stepScope.getPhaseScope().isStuck() || probabilityItemList.isEmpty()) {
            reset();
        } else {
            for (var item : probabilityItemList) {
                item.moveIterator = new AdaptiveIteratorAdapter<>(item.moveSelector.iterator(), item);
            }
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        var improved = ((Score) currentBest).compareTo(stepScope.getScore()) < 0;
        if (improved && stepScope instanceof LocalSearchStepScope<Solution_> localSearchStepScope
                && localSearchStepScope.getStep() instanceof LegacyMoveAdapter<Solution_> legacyMoveAdapter
                && legacyMoveAdapter.legacyMove() instanceof AdaptiveMoveAdapter<Solution_> adaptiveMoveAdapter) {
            log.info("Best solution improved {}, {}", stepScope.getScore(), legacyMoveAdapter.legacyMove());
            adaptiveMoveAdapter.probabilityItem.incrementWeight();
            this.probabilityWeightTotal++;
        }

    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
        log.info("Move probability distribution: max moves {}, {}", MAX_MOVE_EVALUATION_PER_ITER, probabilityItemList.stream()
                .map(i -> "%s=%.2f%%".formatted(i.moveSelector.toString().substring(0, i.moveSelector.toString().indexOf("(")),
                        (i.weight / probabilityWeightTotal) * 100))
                .collect(joining(";")));
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    // ************************************************************************
    // Adapter classes
    // ************************************************************************
    private record AdaptiveIteratorAdapter<Solution_>(Iterator<Move<Solution_>> childMoveIterator,
            ProbabilityItem<Solution_> probabilityItem) implements Iterator<Move<Solution_>> {

        @Override
        public void remove() {
            childMoveIterator.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super Move<Solution_>> action) {
            childMoveIterator.forEachRemaining(action);
        }

        @Override
        public boolean hasNext() {
            return childMoveIterator.hasNext();
        }

        @Override
        public Move<Solution_> next() {
            var move = new AdaptiveMoveAdapter<>(childMoveIterator.next(), probabilityItem);
            probabilityItem.incrementCount();
            if (probabilityItem.moveCount >= probabilityItem.maxMoveCount) {
                probabilityItem.manager.incrementMoveIndex();
            }
            return move;
        }
    }

    private record AdaptiveMoveAdapter<Solution_>(Move<Solution_> move,
            ProbabilityItem<Solution_> probabilityItem) implements Move<Solution_> {

        @Override
        public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
            return move.isMoveDoable(scoreDirector);
        }

        @Override
        public Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
            return move.doMove(scoreDirector);
        }

        @Override
        public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
            move.doMoveOnly(scoreDirector);
        }

        @Override
        public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
            return move.rebase(destinationScoreDirector);
        }

        @Override
        public String getSimpleMoveTypeDescription() {
            return move.getSimpleMoveTypeDescription();
        }

        @Override
        public Collection<?> getPlanningEntities() {
            return move.getPlanningEntities();
        }

        @Override
        public Collection<?> getPlanningValues() {
            return move.getPlanningValues();
        }

        @Override
        public String toString() {
            return move.toString();
        }
    }

    private static final class ProbabilityItem<Solution_> {
        AdaptiveMoveProbabilityManager<Solution_> manager;
        MoveSelector<Solution_> moveSelector;
        Iterator<Move<Solution_>> moveIterator;
        double weight;
        int moveCount;
        int maxMoveCount;

        public ProbabilityItem(AdaptiveMoveProbabilityManager<Solution_> manager) {
            this.manager = manager;
        }

        public void incrementCount() {
            this.moveCount++;
        }

        public void incrementWeight() {
            this.weight++;
        }
    }
}
