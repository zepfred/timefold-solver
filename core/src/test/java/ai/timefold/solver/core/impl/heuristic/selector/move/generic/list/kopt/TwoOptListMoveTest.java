package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TwoOptListMoveTest {

    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();
    private final InnerScoreDirector<TestdataListSolution, ?> scoreDirector =
            PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());
    private final InnerScoreDirector<TestdataListEntityProvidingSolution, ?> otherInnerScoreDirector =
            mock(InnerScoreDirector.class);
    private final ListVariableDescriptor<TestdataListEntityProvidingSolution> otherVariableDescriptor =
            TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList();

    @BeforeEach
    void setUp() {
        when(otherInnerScoreDirector.getValueRangeManager())
                .thenReturn(new ValueRangeManager<>(otherVariableDescriptor.getEntityDescriptor().getSolutionDescriptor()));
    }

    @Test
    void doMove() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v5, v4, v3, v6, v7, v8);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        // 2-Opt((v2, v5), (v3, v6))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 5);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 5);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 5);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void isMoveDoable() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v5, v4, v3, v6, v7, v8);

        // 2-Opt((v2, v5), (v3, v6))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 5);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        // 2-Opt((v2, v3), (v2, v3))
        move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 2);
        assertThat(move.isMoveDoable(scoreDirector)).isFalse();

        // 2-Opt((v2, v3), (v3, v4))
        move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 3);
        assertThat(move.isMoveDoable(scoreDirector)).isFalse();

        // 2-Opt((v2, v3), (v4, v5))
        move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 4);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        // 2-Opt((v2, v3), (v1, v2))
        move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 1);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
    }

    @Disabled("Temporarily disabled")
    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        var v3 = new TestdataListEntityProvidingValue("3");
        var v4 = new TestdataListEntityProvidingValue("4");
        var v5 = new TestdataListEntityProvidingValue("5");
        var v6 = new TestdataListEntityProvidingValue("6");
        var e1 = new TestdataListEntityProvidingEntity("e1", List.of(v1, v2, v3, v5, v6), List.of(v2, v1, v5));
        var e2 = new TestdataListEntityProvidingEntity("e2", List.of(v1, v3, v4, v5, v6), List.of(v4, v3, v6));

        // different entity => valid left and right
        assertThat(new TwoOptListMove<>(otherVariableDescriptor, e1, e2, 1, 1).isMoveDoable(otherInnerScoreDirector))
                .isTrue();
        // different entity => invalid left
        assertThat(new TwoOptListMove<>(otherVariableDescriptor, e1, e2, 0, 1).isMoveDoable(otherInnerScoreDirector))
                .isFalse();
        // different entity => invalid right
        assertThat(new TwoOptListMove<>(otherVariableDescriptor, e1, e2, 1, 0).isMoveDoable(otherInnerScoreDirector))
                .isFalse();
    }

    @Test
    void isMoveDoableTailSwap() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListValue v9 = new TestdataListValue("9");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8, v9);

        // 2-Opt((v2, v3), (v6, v7))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e2, 2, 2);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        move = new TwoOptListMove<>(variableDescriptor,
                e1, e2, 1, 2);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        move = new TwoOptListMove<>(variableDescriptor,
                e1, e2, 2, 1);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
    }

    @Test
    void doTailSwap() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListValue v9 = new TestdataListValue("9");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8, v9);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7, v8, v9));
        scoreDirector.setWorkingSolution(solution);

        // 2-Opt((v2, v3), (v6, v7))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e2, 2, 2);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v7, v8, v9);
        assertThat(e2.getValueList()).containsExactly(v5, v6, v3, v4);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 2, 5);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 2, 4);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void doMoveSecondEndsBeforeFirst() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v8, v7, v3, v4, v5, v6, v2, v1);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        // 2-Opt((v6, v2), (v7, v3))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 6, 2);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v8, v1, v2, v3, v4, v5, v6, v7);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void doMoveSecondEndsBeforeFirstUnbalanced() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v5, v2, v3, v4, v1, v7, v6);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7));
        scoreDirector.setWorkingSolution(solution);

        // 2-Opt((v4, v1), (v5, v2))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 4, 1);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v5, v6, v7, v1, v2, v3, v4);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 7);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 7);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void doMoveFirstEndsBeforeSecondUnbalanced() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v2, v1, v7, v4, v5, v6, v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7));
        scoreDirector.setWorkingSolution(solution);

        // 2-Opt((v4, v1), (v5, v2))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptor,
                e1, e1, 2, 1);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v2, v3, v6, v5, v4, v7, v1);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 7);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 7);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void doMoveSecondEndsBeforeFirstPinned() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListValue v6 = new TestdataListValue("6");
        TestdataListValue v7 = new TestdataListValue("7");
        TestdataListValue v8 = new TestdataListValue("8");
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v8, v7, v3, v4, v5, v6, v2, v1);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v5, v4, v3, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        var variableDescriptorSpy = Mockito.spy(variableDescriptor);
        var entityDescriptor = Mockito.spy(TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntity.class));
        Mockito.when(variableDescriptorSpy.getEntityDescriptor()).thenReturn(entityDescriptor);
        Mockito.when(variableDescriptorSpy.getFirstUnpinnedIndex(e1)).thenReturn(1);
        Mockito.when(entityDescriptor.supportsPinning()).thenReturn(true);

        // 2-Opt((v6, v2), (v7, v3))
        TwoOptListMove<TestdataListSolution> move = new TwoOptListMove<>(variableDescriptorSpy,
                e1, e1, 6, 2);
        move.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v8, v2, v3, v4, v5, v6, v7, v1);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e1, 1, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e1, 1, 8);
        verify(scoreDirector).triggerVariableListeners();
    }

    @Test
    void rebase() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListEntity e1 = new TestdataListEntity("e1");

        TestdataListValue destinationV1 = new TestdataListValue("1");
        TestdataListValue destinationV2 = new TestdataListValue("2");
        TestdataListEntity destinationE1 = new TestdataListEntity("e1");

        InnerScoreDirector<TestdataListSolution, SimpleScore> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                });
        doReturn(scoreDirector.getSupplyManager()).when(destinationScoreDirector).getSupplyManager();

        assertSameProperties(
                destinationE1, 0, 1,
                new TwoOptListMove<>(variableDescriptor, e1, e1, 0, 1)
                        .rebase(destinationScoreDirector));
    }

    static void assertSameProperties(Object destinationEntity, int destinationV1, int destinationV2, TwoOptListMove<?> move) {
        assertThat(move.getFirstEntity()).isSameAs(destinationEntity);
        assertThat(move.getFirstEdgeEndpoint()).isEqualTo(destinationV1);
        assertThat(move.getSecondEdgeEndpoint()).isEqualTo(destinationV2);
    }
}
