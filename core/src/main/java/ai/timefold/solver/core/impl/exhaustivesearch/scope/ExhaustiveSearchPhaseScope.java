package ai.timefold.solver.core.impl.exhaustivesearch.scope;

import java.util.List;
import java.util.SortedSet;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class ExhaustiveSearchPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private List<ExhaustiveSearchLayer> layerList;
    private SortedSet<ExhaustiveSearchNode> expandableNodeQueue;
    private InnerScore<?> bestPessimisticBound;

    private ExhaustiveSearchStepScope<Solution_> lastCompletedStepScope;

    public ExhaustiveSearchPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex, false);
        lastCompletedStepScope = new ExhaustiveSearchStepScope<>(this, -1);
    }

    public List<ExhaustiveSearchLayer> getLayerList() {
        return layerList;
    }

    public void setLayerList(List<ExhaustiveSearchLayer> layerList) {
        this.layerList = layerList;
    }

    public SortedSet<ExhaustiveSearchNode> getExpandableNodeQueue() {
        return expandableNodeQueue;
    }

    public void setExpandableNodeQueue(SortedSet<ExhaustiveSearchNode> expandableNodeQueue) {
        this.expandableNodeQueue = expandableNodeQueue;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScore<Score_> getBestPessimisticBound() {
        return (InnerScore<Score_>) bestPessimisticBound;
    }

    public void setBestPessimisticBound(InnerScore<?> bestPessimisticBound) {
        this.bestPessimisticBound = bestPessimisticBound;
    }

    @Override
    public ExhaustiveSearchStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    public void setLastCompletedStepScope(ExhaustiveSearchStepScope<Solution_> lastCompletedStepScope) {
        this.lastCompletedStepScope = lastCompletedStepScope;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public int getDepthSize() {
        return layerList.size();
    }

    public <Score_ extends Score<Score_>> void registerPessimisticBound(InnerScore<Score_> pessimisticBound) {
        var castBestPessimisticBound = this.<Score_> getBestPessimisticBound();
        if (pessimisticBound.compareTo(castBestPessimisticBound) > 0) {
            bestPessimisticBound = pessimisticBound;
            // Prune the queue
            // TODO optimize this because expandableNodeQueue is too long to iterate
            expandableNodeQueue.removeIf(node -> {
                var optimistic = node.<Score_> getOptimisticBound();
                return optimistic.compareTo(pessimisticBound) <= 0;
            });
        }
    }

    public void addExpandableNode(ExhaustiveSearchNode moveNode) {
        expandableNodeQueue.add(moveNode);
        moveNode.setExpandable(true);
    }

}
