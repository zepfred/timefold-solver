package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class TwoOptListOriginalMoveIteratorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = new TestdataListEntity("A", v1, v2);
        var b = new TestdataListEntity("B", v3, v4);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 2;
        var listVariableDescriptor = TestdataListSolution.buildSolutionDescriptor().getListVariableDescriptor();
        IterableValueSelector<TestdataListSolution> originalValueSelector =
                mockIterableValueSelector(TestdataListEntity.class, "valueList", v1, v2, v3, v4);
        doReturn(listVariableDescriptor).when(originalValueSelector).getVariableDescriptor();
        IterableValueSelector<TestdataListSolution> valueSelector =
                mockIterableValueSelector(TestdataListEntity.class, "valueList", v1, v2, v3, v4);
        doReturn(listVariableDescriptor).when(valueSelector).getVariableDescriptor();
        var moveSelector =
                new KOptListMoveSelector<>(listVariableDescriptor, originalValueSelector, valueSelector,
                        minimumSubListSize, minimumSubListSize, null, false);

        solvingStarted(moveSelector, scoreDirector);

        // Every possible subList is selected.
        assertAllCodesOfIterator(moveSelector.iterator(),
                "{A[0],A[0]}",
                "{A[0],A[1]}",
                "{A[0],B[0]}",
                "{A[0],B[0] reversingTail}",
                "{A[0],B[1]}",
                "{A[0],B[1] reversingTail}",
                "{A[1],A[0]}",
                "{A[1],A[1]}",
                "{A[1],B[0]}",
                "{A[1],B[0] reversingTail}",
                "{A[1],B[1]}",
                "{A[1],B[1] reversingTail}",
                "{B[0],A[0]}",
                "{B[0],A[0] reversingTail}",
                "{B[0],A[1]}",
                "{B[0],A[1] reversingTail}",
                "{B[0],B[0]}",
                "{B[0],B[1]}",
                "{B[1],A[0]}",
                "{B[1],A[0] reversingTail}",
                "{B[1],A[1]}",
                "{B[1],A[1] reversingTail}",
                "{B[1],B[0]}",
                "{B[1],B[1]}");
    }

}
