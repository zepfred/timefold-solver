package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.DiminishedReturnsTermination;

import org.junit.jupiter.api.Test;

class UnimprovedMoveCountStuckCriterionTest {

    @Test
    void isSolverStuck() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var lastStepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        var termination = mock(DiminishedReturnsTermination.class);

        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(moveScope.getScore()).thenReturn(SimpleScore.of(1));
        when(lastStepScope.getScore()).thenReturn(SimpleScore.of(1));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1));
        when(phaseScope.getLastCompletedStepScope()).thenReturn(lastStepScope);
        when(termination.isTerminated(anyLong(), any())).thenReturn(false, true);

        // No restart
        var strategy = new UnimprovedMoveCountStuckCriterion<>();
        strategy.setMaxRejected(1);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();

        // Wait for the first best score
        strategy.stepStarted(stepScope);
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();
        strategy.stepEnded(stepScope);

        // Trigger Restart
        // First iter without improvement
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();
        // Second iter without improvement
        assertThat(strategy.isSolverStuck(moveScope)).isTrue();
    }

    @Test
    void reset() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var lastStepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        var termination = mock(DiminishedReturnsTermination.class);

        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(moveScope.getScore()).thenReturn(SimpleScore.of(1));
        when(lastStepScope.getScore()).thenReturn(SimpleScore.of(1));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1));
        when(phaseScope.getLastCompletedStepScope()).thenReturn(lastStepScope);
        when(termination.isTerminated(anyLong(), any())).thenReturn(false, true);

        var strategy = new UnimprovedMoveCountStuckCriterion<>();
        strategy.setMaxRejected(1);
        strategy.phaseStarted(phaseScope);
        strategy.stepStarted(stepScope);
        strategy.stepEnded(stepScope);

        // Trigger Restart
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();
        assertThat(strategy.isSolverStuck(moveScope)).isTrue();

        // Reset
        when(moveScope.getScore()).thenReturn(SimpleScore.of(3));
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();
    }
}
