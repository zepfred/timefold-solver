package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class AdaptiveLateAcceptanceAcceptorTest extends AbstractAcceptorTest {

    @Test
    void triggerStuckCriterion() {
        var stuckCriterion = mock(StuckCriterion.class);
        var adaptiveAcceptor = new AdaptiveLateAcceptanceAcceptor<>(stuckCriterion);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        when(stuckCriterion.isSolverStuck(any())).thenReturn(true);

        // Init
        adaptiveAcceptor.solvingStarted(solverScope);
        adaptiveAcceptor.phaseStarted(phaseScope);
        adaptiveAcceptor.stepStarted(stepScope0);

        // Trigger
        assertThat(phaseScope.isSolverStuck()).isFalse();
        adaptiveAcceptor.isAccepted(buildMoveScope(stepScope0, -900));
        assertThat(phaseScope.isSolverStuck()).isTrue();
    }

    @Test
    void restart() {
        var decider = mock(LocalSearchDecider.class);
        var stuckCriterion = mock(StuckCriterion.class);
        var adaptiveAcceptor = new AdaptiveLateAcceptanceAcceptor<>(stuckCriterion);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        when(stuckCriterion.isSolverStuck(any())).thenReturn(false);
        phaseScope.setDecider(decider);

        // Init
        adaptiveAcceptor.solvingStarted(solverScope);
        adaptiveAcceptor.phaseStarted(phaseScope);
        adaptiveAcceptor.stepStarted(stepScope0);

        // Update late elements
        assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores.length)
                .isEqualTo(AdaptiveLateAcceptanceAcceptor.LATE_ELEMENTS_SIZE[0]);
        adaptiveAcceptor.isAccepted(buildMoveScope(stepScope0, -800));
        assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores).containsAnyOf(SimpleScore.of(-1000),
                SimpleScore.of(-800));

        // Restart e restore the late elements list
        adaptiveAcceptor.restart(stepScope0);
        assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores.length)
                .isEqualTo(AdaptiveLateAcceptanceAcceptor.LATE_ELEMENTS_SIZE[1]);
        assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores).containsOnly(SimpleScore.of(-1000));
    }

    @Test
    void restartWithPerturbation() {
        var decider = mock(LocalSearchDecider.class);
        var stuckCriterion = mock(StuckCriterion.class);
        var adaptiveAcceptor = new AdaptiveLateAcceptanceAcceptor<>(stuckCriterion);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1100));
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        when(stuckCriterion.isSolverStuck(any())).thenReturn(false);
        phaseScope.setDecider(decider);

        // Init
        adaptiveAcceptor.solvingStarted(solverScope);
        adaptiveAcceptor.phaseStarted(phaseScope);
        adaptiveAcceptor.stepStarted(stepScope0);

        // Trigger the restart max times
        for (var i = 0; i < AdaptiveLateAcceptanceAcceptor.MAX_RESTART_WITHOUT_IMPROVEMENT_COUNT; i++) {
            adaptiveAcceptor.restart(stepScope0);
            assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores).containsOnly(SimpleScore.of(-1000));
        }

        // Perturbation
        adaptiveAcceptor.restart(stepScope0);
        assertThat(adaptiveAcceptor.lateAcceptanceAcceptor.previousScores).containsOnly(SimpleScore.of(-1100));
    }
}
