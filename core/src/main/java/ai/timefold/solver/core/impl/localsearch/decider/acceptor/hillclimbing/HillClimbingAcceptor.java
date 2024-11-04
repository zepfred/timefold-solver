package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

public class HillClimbingAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private boolean globalSearch = false;

    public void enableGlobalSearch() {
        this.globalSearch = true;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        Score moveScore = moveScope.getScore();
        if (globalSearch) {
            return moveScore.compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0;
        } else {
            return moveScore.compareTo(moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore()) >= 0;
        }
    }

}
