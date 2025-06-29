package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class UnionMoveSelectorTest {

    @Test
    void originSelection() {
        ArrayList<MoveSelector<TestdataSolution>> childMoveSelectorList = new ArrayList<>();
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector(
                new DummyMove("a1"), new DummyMove("a2"), new DummyMove("a3")));
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector(
                new DummyMove("b1"), new DummyMove("b2")));
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, false);

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        assertAllCodesOfMoveSelector(moveSelector, "a1", "a2", "a3", "b1", "b2");

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void emptyOriginSelection() {
        ArrayList<MoveSelector<TestdataSolution>> childMoveSelectorList = new ArrayList<>();
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, false);

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        assertAllCodesOfMoveSelector(moveSelector);

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void biasedRandomSelection() {
        ArrayList<MoveSelector<TestdataSolution>> childMoveSelectorList = new ArrayList<>();
        Map<MoveSelector<TestdataSolution>, Double> fixedProbabilityWeightMap = new HashMap<>();
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector(
                new DummyMove("a1"), new DummyMove("a2"), new DummyMove("a3")));
        fixedProbabilityWeightMap.put(childMoveSelectorList.get(0), 1000.0);
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector(
                new DummyMove("b1"), new DummyMove("b2")));
        fixedProbabilityWeightMap.put(childMoveSelectorList.get(1), 20.0);
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, true,
                        new FixedSelectorProbabilityWeightFactory<>(fixedProbabilityWeightMap));

        Random workingRandom = new TestRandom(
                1.0 / 1020.0,
                1019.0 / 1020.0,
                1000.0 / 1020.0,
                0.0,
                999.0 / 1020.0);
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = PlannerTestUtils.delegatingPhaseScope(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = PlannerTestUtils.delegatingStepScope(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        // A union of ending MoveSelectors does end, even with randomSelection
        assertAllCodesOfMoveSelector(moveSelector, "a1", "b1", "b2", "a2", "a3");

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void uniformRandomSelection() {
        List<MoveSelector<TestdataSolution>> childMoveSelectorList = List.of(
                SelectorTestUtils.mockMoveSelector(new DummyMove("a1"), new DummyMove("a2"),
                        new DummyMove("a3")),
                SelectorTestUtils.mockMoveSelector(new DummyMove("b1"), new DummyMove("b2")));
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, true, null);

        Random workingRandom = new TestRandom(0, 1, 1, 0, 0);
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = PlannerTestUtils.delegatingPhaseScope(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = PlannerTestUtils.delegatingStepScope(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        // A union of ending MoveSelectors does end, even with randomSelection
        assertAllCodesOfMoveSelector(moveSelector, "a1", "b1", "b2", "a2", "a3");

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void emptyUniformRandomSelection() {
        ArrayList<MoveSelector<TestdataSolution>> childMoveSelectorList = new ArrayList<>();
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, true, null);

        Random workingRandom = new TestRandom(1);

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = PlannerTestUtils.delegatingPhaseScope(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = PlannerTestUtils.delegatingStepScope(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        // A union of ending MoveSelectors does end, even with randomSelection
        assertAllCodesOfMoveSelector(moveSelector);

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void emptyBiasedRandomSelection() {
        ArrayList<MoveSelector<TestdataSolution>> childMoveSelectorList = new ArrayList<>();
        Map<MoveSelector<TestdataSolution>, Double> fixedProbabilityWeightMap = new HashMap<>();
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        fixedProbabilityWeightMap.put(childMoveSelectorList.get(0), 1000.0);
        childMoveSelectorList.add(SelectorTestUtils.mockMoveSelector());
        fixedProbabilityWeightMap.put(childMoveSelectorList.get(1), 20.0);
        UnionMoveSelector<TestdataSolution> moveSelector =
                new UnionMoveSelector<>(childMoveSelectorList, true,
                        new FixedSelectorProbabilityWeightFactory<>(fixedProbabilityWeightMap));

        Random workingRandom = new TestRandom(1);

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(workingRandom);
        moveSelector.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScopeA = PlannerTestUtils.delegatingPhaseScope(solverScope);
        moveSelector.phaseStarted(phaseScopeA);
        AbstractStepScope<TestdataSolution> stepScopeA1 = PlannerTestUtils.delegatingStepScope(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);

        // A union of ending MoveSelectors does end, even with randomSelection
        assertAllCodesOfMoveSelector(moveSelector);

        moveSelector.stepEnded(stepScopeA1);
        moveSelector.phaseEnded(phaseScopeA);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelectorList.get(0), 1, 1, 1);
        verifyPhaseLifecycle(childMoveSelectorList.get(1), 1, 1, 1);
    }

    @Test
    void testEnableNearbyMixedModel() {
        var moveSelectorConfig = new UnionMoveSelectorConfig();
        assertThat(moveSelectorConfig.canEnableNearbyInMixedModels()).isFalse();
    }
}
