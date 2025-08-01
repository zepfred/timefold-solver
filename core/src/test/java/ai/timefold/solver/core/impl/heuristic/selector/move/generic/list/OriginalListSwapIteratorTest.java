package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class OriginalListSwapIteratorTest {

    private final TestdataListValue v1 = new TestdataListValue("v1");
    private final TestdataListValue v2 = new TestdataListValue("v2");

    @Test
    void emptyLeftValueSelector() {
        assertEmptyIterator(emptyList(), asList(v1, v2));
    }

    @Test
    void emptyRightValueSelector() {
        assertEmptyIterator(asList(v1, v2), emptyList());
    }

    static void assertEmptyIterator(List<Object> leftValues, List<Object> rightValues) {
        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        OriginalListSwapIterator<TestdataListSolution> listSwapIterator = new OriginalListSwapIterator<>(
                scoreDirector.getSupplyManager().demand(listVariableDescriptor.getStateDemand()),
                mockIterableValueSelector(listVariableDescriptor, leftValues.toArray()),
                mockIterableValueSelector(listVariableDescriptor, rightValues.toArray()));

        assertThat(listSwapIterator).isExhausted();
    }
}
