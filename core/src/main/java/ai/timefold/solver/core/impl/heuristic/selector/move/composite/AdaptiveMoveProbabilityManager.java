package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

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

public class AdaptiveMoveProbabilityManager<Solution_> implements PhaseLifecycleListener<Solution_> {

    private final List<MoveSelector<Solution_>> childMoveSelectorList;
    private final List<ProbabilityItem<Solution_>> probabilityItemList;
    private Score<?> currentBest;
    private static final double IMPROVEMENT_INCREMENT = 0.1;
    private static final double MAX_RESET_WEIGHT = 50;
    private double probabilityWeightTotal;
    private double resetAfterWeight;

    public AdaptiveMoveProbabilityManager(List<MoveSelector<Solution_>> childMoveSelectorList) {
        this.childMoveSelectorList = childMoveSelectorList;
        this.probabilityItemList = new ArrayList<>(childMoveSelectorList.size());
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    protected void reset() {
        var probabilitySum = 0.0;
        probabilityItemList.clear();
        for (var moveSelector : childMoveSelectorList) {
            var moveIterator = moveSelector.iterator();
            if (moveIterator.hasNext()) {
                ProbabilityItem<Solution_> probabilityItem = new ProbabilityItem<>();
                probabilityItem.moveSelector = moveSelector;
                probabilityItem.moveIterator = new AdaptiveIteratorAdapter<>(moveIterator, probabilityItem);
                probabilityItem.weight = 1;
                probabilityItemList.add(probabilityItem);
                probabilitySum += 1.0;
            }
        }
        this.probabilityWeightTotal = probabilitySum;
        this.resetAfterWeight = probabilityWeightTotal + MAX_RESET_WEIGHT;
    }

    public Iterator<Move<Solution_>> getMoveIterator(int pos) {
        return probabilityItemList.get(pos).moveIterator;
    }

    public double getMoveWeight(int pos) {
        return probabilityItemList.get(pos).weight;
    }

    public int getMoveListSize() {
        return probabilityItemList.size();
    }

    public double getProbabilityWeightTotal() {
        return probabilityWeightTotal;
    }

    // ************************************************************************
    // lifecycle methods
    // ************************************************************************
    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
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
            adaptiveMoveAdapter.probabilityItem.increment(IMPROVEMENT_INCREMENT);
            this.probabilityWeightTotal += IMPROVEMENT_INCREMENT;
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
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
            return new AdaptiveMoveAdapter<>(childMoveIterator.next(), probabilityItem);
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
        MoveSelector<Solution_> moveSelector;
        Iterator<Move<Solution_>> moveIterator;
        double weight;

        public void increment(double value) {
            weight += value;
        }
    }
}
