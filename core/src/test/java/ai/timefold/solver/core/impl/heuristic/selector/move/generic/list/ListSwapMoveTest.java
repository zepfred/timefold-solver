package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ListSwapMoveTest {

    private final TestdataListValue v1 = new TestdataListValue("1");
    private final TestdataListValue v2 = new TestdataListValue("2");
    private final TestdataListValue v3 = new TestdataListValue("3");

    private final InnerScoreDirector<TestdataListSolution, ?> innerScoreDirector = mock(InnerScoreDirector.class);
    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();
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
    void isMoveDoable() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        // same entity, same index => not doable because the move doesn't change anything
        assertThat(new ListSwapMove<>(variableDescriptor, e1, 1, e1, 1).isMoveDoable(innerScoreDirector)).isFalse();
        // same entity, different index => doable
        assertThat(new ListSwapMove<>(variableDescriptor, e1, 0, e1, 1).isMoveDoable(innerScoreDirector)).isTrue();
        // different entity => doable
        assertThat(new ListSwapMove<>(variableDescriptor, e1, 0, e2, 0).isMoveDoable(innerScoreDirector)).isTrue();
    }

    @Disabled("Temporarily disabled")
    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var value1 = new TestdataListEntityProvidingValue("1");
        var value2 = new TestdataListEntityProvidingValue("2");
        var value3 = new TestdataListEntityProvidingValue("3");
        var value4 = new TestdataListEntityProvidingValue("4");
        var entity1 = new TestdataListEntityProvidingEntity("e1", List.of(value1, value2, value3), List.of(value1, value2));
        var entity2 = new TestdataListEntityProvidingEntity("e2", List.of(value1, value3, value4), List.of(value3, value4));
        // different entity => valid left and right
        assertThat(new ListSwapMove<>(otherVariableDescriptor, entity1, 0, entity2, 0).isMoveDoable(otherInnerScoreDirector))
                .isTrue();
        // different entity => invalid left
        assertThat(new ListSwapMove<>(otherVariableDescriptor, entity1, 1, entity2, 0).isMoveDoable(otherInnerScoreDirector))
                .isFalse();
        // different entity => invalid right
        assertThat(new ListSwapMove<>(otherVariableDescriptor, entity1, 0, entity2, 1).isMoveDoable(otherInnerScoreDirector))
                .isFalse();
    }

    @Test
    void doMove() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        // Swap Move 1: between two entities
        moveDirector.executeTemporary(new ListSwapMove<>(variableDescriptor, e1, 0, e2, 0),
                (__, ___) -> {
                    assertThat(e1.getValueList()).containsExactly(v3, v2);
                    assertThat(e2.getValueList()).containsExactly(v1);

                    verify(innerScoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 1);
                    verify(innerScoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 1);
                    verify(innerScoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 1);
                    verify(innerScoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 1);
                    verify(innerScoreDirector, atLeastOnce()).triggerVariableListeners();
                    return null;
                });

        // Swap Move 2: same entity
        moveDirector.execute(new ListSwapMove<>(variableDescriptor, e1, 0, e1, 1));
        assertThat(e1.getValueList()).containsExactly(v2, v1);
    }

    @Test
    void rebase() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        TestdataListValue destinationV1 = new TestdataListValue("1");
        TestdataListValue destinationV2 = new TestdataListValue("2");
        TestdataListValue destinationV3 = new TestdataListValue("3");
        TestdataListEntity destinationE1 = new TestdataListEntity("e1", destinationV1, destinationV2);
        TestdataListEntity destinationE2 = new TestdataListEntity("e2", destinationV3);

        ScoreDirector<TestdataListSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { v3, destinationV3 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                });

        assertSameProperties(
                destinationE1, 1, destinationV2,
                destinationE2, 0, destinationV3,
                new ListSwapMove<>(variableDescriptor, e1, 1, e2, 0).rebase(destinationScoreDirector));
        assertSameProperties(
                destinationE1, 0, destinationV1,
                destinationE1, 1, destinationV2,
                new ListSwapMove<>(variableDescriptor, e1, 0, e1, 1).rebase(destinationScoreDirector));
    }

    static void assertSameProperties(
            Object leftEntity, int leftIndex, Object leftValue,
            Object rightEntity, int rightIndex, Object rightValue,
            ListSwapMove<?> move) {
        assertThat(move.getLeftEntity()).isSameAs(leftEntity);
        assertThat(move.getLeftIndex()).isEqualTo(leftIndex);
        assertThat(move.getLeftValue()).isSameAs(leftValue);
        assertThat(move.getRightEntity()).isSameAs(rightEntity);
        assertThat(move.getRightIndex()).isEqualTo(rightIndex);
        assertThat(move.getRightValue()).isSameAs(rightValue);
    }

    @Test
    void tabuIntrospection_twoEntities() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        ListSwapMove<TestdataListSolution> moveTwoEntities = new ListSwapMove<>(variableDescriptor, e1, 0, e2, 0);
        // Do the move first because that might affect the returned values.
        moveTwoEntities.doMoveOnGenuineVariables(innerScoreDirector);
        assertThat(moveTwoEntities.getPlanningEntities()).containsExactly(e1, e2);
        assertThat(moveTwoEntities.getPlanningValues()).containsExactlyInAnyOrder(v3, v1);
    }

    @Test
    void tabuIntrospection_oneEntity() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);

        ListSwapMove<TestdataListSolution> moveOneEntity = new ListSwapMove<>(variableDescriptor, e1, 0, e1, 1);
        // Do the move first because that might affect the returned values.
        moveOneEntity.doMoveOnGenuineVariables(innerScoreDirector);
        assertThat(moveOneEntity.getPlanningEntities()).containsExactly(e1);
        assertThat(moveOneEntity.getPlanningValues()).containsExactly(v2, v1);
    }

    @Test
    void toStringTest() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        assertThat(new ListSwapMove<>(variableDescriptor, e1, 0, e1, 1)).hasToString("1 {e1[0]} <-> 2 {e1[1]}");
        assertThat(new ListSwapMove<>(variableDescriptor, e1, 1, e2, 0)).hasToString("2 {e1[1]} <-> 3 {e2[0]}");
    }

    @Test
    void testEnableNearbyMixedModel() {
        var moveSelectorConfig = new ListSwapMoveSelectorConfig();
        assertThat(moveSelectorConfig.canEnableNearbyInMixedModels()).isTrue();
    }
}
