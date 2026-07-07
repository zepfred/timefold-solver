package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.definition.BendableScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class LateAcceptanceAcceptorTest extends AbstractAcceptorTest {

    @Test
    void lateAcceptanceSize() {
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(3);
        acceptor.setHillClimbingEnabled(false);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(SimpleScore.of(Integer.MIN_VALUE));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        var scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        solverScope.setScoreDirector(scoreDirector);
        acceptor.phaseStarted(phaseScope);

        // lateScore = [-1000, -1000, -1000], currentIndex = 0
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isFalse(); // currentIndex = 2
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore((InnerScore) moveScope0.getScore());
        acceptor.stepEnded(stepScope0); // currentIndex = 0
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = [-1000, -1000, -500], currentIndex = 0
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1 = buildMoveScope(stepScope1, -700);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1000))).isFalse(); // currentIndex = 2
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isFalse(); // currentIndex = 0
        // The index is incremented and late score change to -1000
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1); // currentIndex = 1
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = [-700, -1000, -500], currentIndex = 1
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        var moveScope2 = buildMoveScope(stepScope1, -400);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -2000))).isFalse(); // currentIndex = 2
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -1001))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -699))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -699))).isTrue();
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore((InnerScore) moveScope2.getScore());
        acceptor.stepEnded(stepScope2); // currentIndex = 1
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = [-400, -1000, -500], currentIndex = 1
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        var moveScope3 = buildMoveScope(stepScope1, -200);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -1001))).isFalse(); // currentIndex = 2
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -499))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -501))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(moveScope3)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -2000))).isFalse(); // currentIndex = 1
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isFalse(); // currentIndex = 2
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore((InnerScore) moveScope3.getScore());
        acceptor.stepEnded(stepScope3); // currentIndex = 0
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = [-400, -1000, -200], currentIndex = 0
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        var moveScope4 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -1000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -500))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -2000))).isFalse(); // currentIndex = 2
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -201))).isFalse(); // currentIndex = 0
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -399))).isTrue();
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4); // currentIndex = 1
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = [-300, -1000, -200], currentIndex = 1
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        var moveScope5 = buildMoveScope(stepScope1, -100);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -1000))).isFalse(); // currentIndex = 2
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -400))).isFalse(); // // currentIndex = 0
        assertThat(acceptor.isAccepted(moveScope5)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -600))).isTrue(); // // currentIndex = 2
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -600))).isTrue(); // // currentIndex = 0
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void hillClimbingEnabled() {
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(2);
        acceptor.setHillClimbingEnabled(true);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        var scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        solverScope.setScoreDirector(scoreDirector);
        acceptor.phaseStarted(phaseScope);

        // lateScore = [-1000, -1000], lastCompletedStepScore = -1000, currentIndex = 0
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore((InnerScore) moveScope0.getScore());
        acceptor.stepEnded(stepScope0); // currentIndex = 0
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = [-1000, -500], lastCompletedStepScore = -500, currentIndex = 0
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1 = buildMoveScope(stepScope1, -700);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(moveScope1)).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1001))).isFalse(); // currentIndex = 0
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1); // currentIndex = 1
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = [-700, -500], lastCompletedStepScore = -700, currentIndex = 1
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        var moveScope2 = buildMoveScope(stepScope1, -400);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -700))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -2000))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -701))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -600))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -700))).isTrue();
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore((InnerScore) moveScope2.getScore());
        acceptor.stepEnded(stepScope2); // currentIndex = 0
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = [-700, -400], lastCompletedStepScore = -400, currentIndex = 0
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        var moveScope3 = buildMoveScope(stepScope1, -200);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -900))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -700))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -701))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(moveScope3)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -2000))).isFalse(); // currentIndex = 0
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isFalse();
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore((InnerScore) moveScope3.getScore());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = [-700, -200], lastCompletedStepScore = -200, currentIndex = 0
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        var moveScope4 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -400))).isTrue();
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -500))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -2000))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -401))).isFalse(); // currentIndex = 0
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -400))).isTrue();
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = [-300, -200], lastCompletedStepScore = -300, currentIndex = 1
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        var moveScope5 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -301))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -400))).isFalse(); // currentIndex = 1
        assertThat(acceptor.isAccepted(moveScope5)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -2000))).isFalse(); // currentIndex = 0
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -600))).isFalse(); // currentIndex = 1
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -301))).isFalse(); // currentIndex = 0
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void resetLateScoresOnHardScoreImprovement() {
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(3);
        acceptor.setHillClimbingEnabled(false);

        var scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getScoreDefinition()).thenReturn(new HardSoftScoreDefinition());

        var solverScope = new SolverScope<>();
        solverScope.setScoreDirector(scoreDirector);
        var initialBestScore = HardSoftScore.of(-1, -1000);
        solverScope.setInitializedBestScore(initialBestScore);

        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(initialBestScore);
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // Step 0: soft-only improvement — no reset expected
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope0);
        var softImprovedScore = HardSoftScore.of(-1, -500);
        phaseScope.setBestSolutionStepIndex(0);
        solverScope.setInitializedBestScore(softImprovedScore);
        stepScope0.setInitializedScore(softImprovedScore);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var initialInnerScore = InnerScore.fullyAssigned(initialBestScore);
        assertThat(acceptor.getScore(1)).isEqualTo(initialInnerScore);
        assertThat(acceptor.getScore(2)).isEqualTo(initialInnerScore);

        // Step 1: hard improvement — reset expected
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope1);
        var hardImprovedScore = HardSoftScore.of(0, -200);
        phaseScope.setBestSolutionStepIndex(1);
        solverScope.setInitializedBestScore(hardImprovedScore);
        stepScope1.setInitializedScore(hardImprovedScore);
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var hardImprovedInnerScore = InnerScore.fullyAssigned(hardImprovedScore);
        for (var i = 0; i < 3; i++) {
            assertThat(acceptor.getScore(i)).isEqualTo(hardImprovedInnerScore);
        }

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void resetLateScoresForBendableScoreOnMediumImprovement() {
        // BendableScore(1 hard, 2 soft): resetScoreLevelCount = getFeasibleLevelsSize() = 1
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(3);
        acceptor.setHillClimbingEnabled(false);

        var scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getScoreDefinition()).thenReturn(new BendableScoreDefinition(1, 2));

        var solverScope = new SolverScope<>();
        solverScope.setScoreDirector(scoreDirector);
        var initialBestScore = BendableScore.of(new long[] { -1 }, new long[] { 0, 0 });
        var initialInnerBestScore = InnerScore.fullyAssigned(initialBestScore);
        solverScope.setInitializedBestScore(initialBestScore);

        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setInitializedScore(initialBestScore);
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // Step 0: soft[0] improvement — reset not expected
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope0);
        var softImprovedScore = BendableScore.of(new long[] { -1 }, new long[] { 1, 0 });
        phaseScope.setBestSolutionStepIndex(0);
        solverScope.setInitializedBestScore(softImprovedScore);
        stepScope0.setInitializedScore(softImprovedScore);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, softImprovedScore))).isTrue();
        acceptor.stepEnded(stepScope0); // Update first late element
        phaseScope.setLastCompletedStepScope(stepScope0);

        var softImprovedInnerScore = InnerScore.fullyAssigned(softImprovedScore);
        assertThat(acceptor.getScore(0)).isEqualTo(softImprovedInnerScore);
        for (var i = 1; i < 3; i++) {
            assertThat(acceptor.getScore(i)).isEqualTo(initialInnerBestScore);
        }

        // Step 1: only soft[1] (last level) improvement — no reset
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope1);
        var lastSoftImprovedScore = BendableScore.of(new long[] { -1 }, new long[] { 1, 1 });
        phaseScope.setBestSolutionStepIndex(1);
        solverScope.setInitializedBestScore(lastSoftImprovedScore);
        stepScope1.setInitializedScore(lastSoftImprovedScore);
        acceptor.isAccepted(buildMoveScope(stepScope1, lastSoftImprovedScore));
        acceptor.stepEnded(stepScope1);

        // Slots 0 and 2 still return the reset value from step 0
        var lastSoftImprovedInnerScore = InnerScore.fullyAssigned(lastSoftImprovedScore);
        assertThat(acceptor.getScore(0)).isEqualTo(softImprovedInnerScore);
        assertThat(acceptor.getScore(1)).isEqualTo(lastSoftImprovedInnerScore);
        assertThat(acceptor.getScore(2)).isEqualTo(initialInnerBestScore);

        // Step 2: hard[0] improvement — reset required
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        acceptor.stepStarted(stepScope2);
        var hardImprovedScore = BendableScore.of(new long[] { 0 }, new long[] { -10, -10 });
        phaseScope.setBestSolutionStepIndex(2);
        solverScope.setInitializedBestScore(hardImprovedScore);
        stepScope2.setInitializedScore(hardImprovedScore);
        acceptor.isAccepted(buildMoveScope(stepScope2, hardImprovedScore));
        acceptor.stepEnded(stepScope2);

        var hardImprovedInnerScore = InnerScore.fullyAssigned(hardImprovedScore);
        for (var i = 0; i < 3; i++) {
            assertThat(acceptor.getScore(i)).isEqualTo(hardImprovedInnerScore);
        }

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void zeroLateAcceptanceSize() {
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(0);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }

    @Test
    void negativeLateAcceptanceSize() {
        var acceptor = new LateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(-1);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }
}
