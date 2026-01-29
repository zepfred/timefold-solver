package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListUtils;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.junit.jupiter.api.Test;

class OriginalConsecutiveSubListSelectorTest {

    @Test
    void originalUnrestricted() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var v5 = new TestdataListValue("5");
        var a = new TestdataListEntity("A", v1, v2, v3, v4);
        var b = new TestdataListEntity("B", v5);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // Size of 2
        var sublistSize = 2;
        var selector = new OriginalConsecutiveSubListSelector<>(mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v1),
                sublistSize, sublistSize);
        solvingStarted(selector, scoreDirector);
        assertAllCodesOfIterator(selector.iterator(), "A[0+2]", "A[1+2]", "A[2+2]");

        // Size of 3
        sublistSize = 3;
        selector = new OriginalConsecutiveSubListSelector<>(mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v1),
                sublistSize, sublistSize);
        solvingStarted(selector, scoreDirector);
        assertAllCodesOfIterator(selector.iterator(), "A[0+3]", "A[1+3]");

        // Size of 4
        sublistSize = 4;
        selector = new OriginalConsecutiveSubListSelector<>(mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v1),
                sublistSize, sublistSize);
        solvingStarted(selector, scoreDirector);
        assertAllCodesOfIterator(selector.iterator(), "A[0+4]");

        // Size of 5
        sublistSize = 5;
        selector = new OriginalConsecutiveSubListSelector<>(mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v1),
                sublistSize, sublistSize);
        solvingStarted(selector, scoreDirector);
        assertAllCodesOfIterator(selector.iterator());
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = new TestdataPinnedWithIndexListEntity("A", v1, v2, v3, v4);
        a.setPlanningPinToIndex(1); // Ignore v1.
        var b = new TestdataPinnedWithIndexListEntity("B");
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // Size of 2
        var sublistSize = 2;
        var selector = new OriginalConsecutiveSubListSelector<>(
                mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v1, v2),
                sublistSize, sublistSize);
        var solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(selector, solverScope);
        assertAllCodesOfIterator(selector.iterator(), "A[1+2]", "A[2+2]");

        // Size of 3
        sublistSize = 3;
        selector = new OriginalConsecutiveSubListSelector<>(
                mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v1, v2),
                sublistSize, sublistSize);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(selector, solverScope);
        assertAllCodesOfIterator(selector.iterator(), "A[1+3]");

        // Size of 4
        sublistSize = 4;
        selector = new OriginalConsecutiveSubListSelector<>(
                mockEntitySelector(a, b),
                TestdataListUtils.mockNeverEndingIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v1, v2),
                sublistSize, sublistSize);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(selector, solverScope);
        assertAllCodesOfIterator(selector.iterator());
    }

}
