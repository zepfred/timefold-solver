package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class VariableMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private static <Solution_> List<Iterator<Move<Solution_>>> toMoveIteratorList(
            List<MoveSelector<Solution_>> childMoveSelectorList) {
        var list = new ArrayList<Iterator<Move<Solution_>>>(childMoveSelectorList.size());
        for (var moves : childMoveSelectorList) {
            var iterator = moves.iterator();
            if (iterator.hasNext()) {
                list.add(iterator);
            }
        }
        return list;
    }

    private final List<Iterator<Move<Solution_>>> moveIteratorList;
    private final int maxUnimprovedIterations;
    private int currentStrategy = 0;
    private final SolverScope<Solution_> solverScope;
    private int count = 0;
    private Score<?> currentBestScore;

    public VariableMoveIterator(SolverScope<Solution_> solverScope, List<MoveSelector<Solution_>> childMoveSelectorList,
            int maxUnimprovedIterations) {
        this.moveIteratorList = toMoveIteratorList(childMoveSelectorList);
        this.maxUnimprovedIterations = maxUnimprovedIterations;
        this.solverScope = solverScope;
        currentBestScore = solverScope.getBestScore();
    }

    @Override
    public boolean hasNext() {
        // Reset all stats if there is an improvement
        if (solverScope.getBestScore().compareTo(currentBestScore) > 0) {
            count = 0;
            currentBestScore = solverScope.getBestScore();
        }
        var exhausted = count >= maxUnimprovedIterations;
        if (exhausted) {
            currentStrategy++;
            if (currentStrategy < moveIteratorList.size()) {
                var hasNext = moveIteratorList.get(currentStrategy).hasNext();
                if (hasNext) {
                    count = 0;
                }
            }
        }
        return count < maxUnimprovedIterations && currentStrategy < moveIteratorList.size();
    }

    @Override
    public Move<Solution_> next() {
        count++;
        var moveIterator = moveIteratorList.get(currentStrategy);
        var hasNext = moveIterator.hasNext();
        if (count > maxUnimprovedIterations || !hasNext) {
            count = maxUnimprovedIterations;
            return NoChangeMove.getInstance();
        }
        return moveIterator.next();
    }

}
