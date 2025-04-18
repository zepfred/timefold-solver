package ai.timefold.solver.core.impl.localsearch.decider.acceptor.greatdeluge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.junit.jupiter.api.Test;

class GreatDelugeAcceptorTest extends AbstractAcceptorTest {

    @Test
    void waterLevelIncrementScore_SimpleScore() {
        var acceptor = new GreatDelugeAcceptor<>();
        acceptor.setWaterLevelIncrementScore(SimpleScore.of(100));

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(SimpleScore.of(-1000));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lastCompletedStepScore = -1000
        // water level -1000
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();

        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore((InnerScore) moveScope0.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lastCompletedStepScore = -500
        // water level -900
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -600);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -700))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1000))).isFalse();
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -500))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -901))).isFalse();

        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        solverScope.setBestScore((InnerScore) moveScope1.getScore());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lastCompletedStepScore = -600
        // water level -800
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope1, -350);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -900))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -700))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -801))).isFalse();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -500))).isTrue();

        stepScope1.setStep(moveScope2.getMove());
        stepScope1.setScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void waterLevelIncrementScore_HardMediumSoftScore() {
        var acceptor = new GreatDelugeAcceptor<>();
        acceptor.setInitialWaterLevel(HardMediumSoftScore.of(0, -100, -400));
        acceptor.setWaterLevelIncrementScore(HardMediumSoftScore.of(0, 100, 100));

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(HardMediumSoftScore.of(0, -200, -1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(HardMediumSoftScore.of(0, -200, -1000));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lastCompletedStepScore = 0/-200/-1000
        // water level 0/-100/-400
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope0);
        var moveScope0 = new LocalSearchMoveScope<>(stepScope0, 0, mock(Move.class));
        moveScope0.setInitializedScore(HardMediumSoftScore.of(0, -100, -300));
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        var moveScope1 = new LocalSearchMoveScope<>(stepScope0, 0, mock(Move.class));
        moveScope1.setInitializedScore(HardMediumSoftScore.of(0, -100, -500));
        // Aspiration
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        var moveScope2 = new LocalSearchMoveScope<>(stepScope0, 0, mock(Move.class));
        moveScope2.setInitializedScore(HardMediumSoftScore.of(0, -50, -800));
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        var moveScope3 = new LocalSearchMoveScope<>(stepScope0, 0, mock(Move.class));
        moveScope3.setInitializedScore(HardMediumSoftScore.of(-5, -50, -100));
        assertThat(acceptor.isAccepted(moveScope3)).isFalse();
        var moveScope4 = new LocalSearchMoveScope<>(stepScope0, 0, mock(Move.class));
        moveScope4.setInitializedScore(HardMediumSoftScore.of(0, -22, -200));
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();

        stepScope0.setStep(moveScope4.getMove());
        stepScope0.setScore(moveScope4.getScore());
        solverScope.setBestScore(moveScope4.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void waterLevelIncrementRatio() {
        var acceptor = new GreatDelugeAcceptor<>();
        acceptor.setWaterLevelIncrementRatio(0.1);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-8));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(SimpleScore.of(-8));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lastCompletedStepScore = -8
        // water level -8
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -5);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -8))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -7))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -9))).isFalse();

        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore((InnerScore) moveScope0.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lastCompletedStepScore = -5
        // water level -8 (rounded down from -7.2)
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -6);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -10))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -7))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -9))).isFalse();
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -8))).isTrue();

        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        solverScope.setBestScore((InnerScore) moveScope1.getScore());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lastCompletedStepScore = -6
        // water level -7 (rounded down from -6.4)
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope1, -4);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -9))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -8))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -7))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();

        stepScope1.setStep(moveScope2.getMove());
        stepScope1.setScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        acceptor.phaseEnded(phaseScope);
    }

}
