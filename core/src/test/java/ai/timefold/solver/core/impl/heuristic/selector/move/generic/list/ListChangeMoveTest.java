package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ListChangeMoveTest {

    private final TestdataListValue v0 = new TestdataListValue("0");
    private final TestdataListValue v1 = new TestdataListValue("1");
    private final TestdataListValue v2 = new TestdataListValue("2");
    private final TestdataListValue v3 = new TestdataListValue("3");
    private final TestdataListValue v4 = new TestdataListValue("4");

    private final InnerScoreDirector<TestdataListSolution, ?> scoreDirector = mock(InnerScoreDirector.class);
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
        assertThat(new ListChangeMove<>(variableDescriptor, e1, 1, e1, 1).isMoveDoable(scoreDirector)).isFalse();
        // same entity, different index => doable
        assertThat(new ListChangeMove<>(variableDescriptor, e1, 0, e1, 1).isMoveDoable(scoreDirector)).isTrue();
        // same entity, index == list size => not doable because the element is first removed (list size is reduced by 1)
        assertThat(new ListChangeMove<>(variableDescriptor, e1, 0, e1, 2).isMoveDoable(scoreDirector)).isFalse();
        // different entity => doable
        assertThat(new ListChangeMove<>(variableDescriptor, e1, 0, e2, 0).isMoveDoable(scoreDirector)).isTrue();
    }

    @Disabled("Temporarily disabled")
    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var value1 = new TestdataListEntityProvidingValue("1");
        var value2 = new TestdataListEntityProvidingValue("2");
        var value3 = new TestdataListEntityProvidingValue("3");
        var entity1 = new TestdataListEntityProvidingEntity("e1", List.of(value1, value2), List.of(value1, value2));
        var entity2 = new TestdataListEntityProvidingEntity("e2", List.of(value1, value3), List.of(value3));
        // different entity => valid value
        assertThat(new ListChangeMove<>(otherVariableDescriptor, entity1, 0, entity2, 0).isMoveDoable(otherInnerScoreDirector))
                .isTrue();
        // different entity => invalid value
        assertThat(new ListChangeMove<>(otherVariableDescriptor, entity1, 1, entity2, 0).isMoveDoable(otherInnerScoreDirector))
                .isFalse();
    }

    @Test
    void doMove() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        ListChangeMove<TestdataListSolution> move = new ListChangeMove<>(variableDescriptor, e1, 1, e2, 1);

        move.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1);
        assertThat(e2.getValueList()).containsExactly(v3, v2);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 1, 2);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 1, 1);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 1, 1);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 1, 2);
        verify(scoreDirector).triggerVariableListeners();
        verifyNoMoreInteractions(scoreDirector);
    }

    static Stream<Arguments> doAndUndoMoveOnTheSameEntity() {
        // Given E.valueList = [V0, V1, V2, V3, V4],
        // when V2 is moved to destinationIndex (arg0),
        // then the resulting valueList should be arg1.
        return Stream.of(
                arguments(0, asList("2", "0", "1", "3", "4"), 0, 3),
                arguments(1, asList("0", "2", "1", "3", "4"), 1, 3),
                arguments(2, null, -1, -1), // ephemeral (no-op)
                arguments(3, asList("0", "1", "3", "2", "4"), 2, 4),
                arguments(4, asList("0", "1", "3", "4", "2"), 2, 5),
                arguments(5, null, -1, -1) // ephemeral (out of bounds)
        );
    }

    @ParameterizedTest
    @MethodSource
    void doAndUndoMoveOnTheSameEntity(int destinationIndex, List<String> expectedValueList, int fromIndex, int toIndex) {
        // Given...
        final int sourceIndex = 2; // we're always moving V2
        TestdataListEntity e = new TestdataListEntity("E", v0, v1, v2, v3, v4);

        // When V2 is moved to destinationIndex...
        ListChangeMove<TestdataListSolution> move =
                new ListChangeMove<>(variableDescriptor, e, sourceIndex, e, destinationIndex);

        // Some destinationIndexes make the move ephemeral.
        if (expectedValueList == null) {
            assertThat(move.isMoveDoable(scoreDirector)).isFalse();
            return;
        }

        // Otherwise, the move is doable...
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        // ...and when it's done...
        move.doMoveOnly(scoreDirector);
        // ...V2 ends up at the destinationIndex
        assertThat(e.getValueList().indexOf(v2)).isEqualTo(destinationIndex);
        assertThat((TestdataListValue) variableDescriptor.getElement(e, destinationIndex)).isEqualTo(v2);
        // ...and the modified value list matches the expectation.
        assertThat(e.getValueList()).map(TestdataObject::toString).isEqualTo(expectedValueList);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e, fromIndex, toIndex);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e, fromIndex, toIndex);
        verify(scoreDirector).triggerVariableListeners();
        verifyNoMoreInteractions(scoreDirector);
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
                destinationE1, 0, destinationV1,
                destinationE2, 1,
                new ListChangeMove<>(variableDescriptor, e1, 0, e2, 1).rebase(destinationScoreDirector));
        assertSameProperties(
                destinationE2, 0, destinationV3,
                destinationE2, 0,
                new ListChangeMove<>(variableDescriptor, e2, 0, e2, 0).rebase(destinationScoreDirector));
    }

    static void assertSameProperties(Object sourceEntity, int sourceIndex, Object movedValue, Object destinationEntity,
            int destinationIndex, ListChangeMove<?> move) {
        assertThat(move.getSourceEntity()).isSameAs(sourceEntity);
        assertThat(move.getSourceIndex()).isEqualTo(sourceIndex);
        assertThat(move.getMovedValue()).isSameAs(movedValue);
        assertThat(move.getDestinationEntity()).isSameAs(destinationEntity);
        assertThat(move.getDestinationIndex()).isEqualTo(destinationIndex);
    }

    @Test
    void tabuIntrospection_twoEntities() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        ListChangeMove<TestdataListSolution> moveTwoEntities = new ListChangeMove<>(variableDescriptor, e1, 1, e2, 1);
        // Do the move first because that might affect the returned values.
        moveTwoEntities.doMoveOnGenuineVariables(scoreDirector);
        assertThat(moveTwoEntities.getPlanningEntities()).containsExactly(e1, e2);
        assertThat(moveTwoEntities.getPlanningValues()).containsExactly(v2);
    }

    @Test
    void tabuIntrospection_oneEntity() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);

        ListChangeMove<TestdataListSolution> moveOneEntity = new ListChangeMove<>(variableDescriptor, e1, 0, e1, 1);
        // Do the move first because that might affect the returned values.
        moveOneEntity.doMoveOnGenuineVariables(scoreDirector);
        assertThat(moveOneEntity.getPlanningEntities()).containsExactly(e1);
        assertThat(moveOneEntity.getPlanningValues()).containsExactly(v1);
    }

    @Test
    void toStringTest() {
        TestdataListEntity e1 = new TestdataListEntity("e1", v1, v2);
        TestdataListEntity e2 = new TestdataListEntity("e2", v3);

        assertThat(new ListChangeMove<>(variableDescriptor, e1, 1, e1, 0)).hasToString("2 {e1[1] -> e1[0]}");
        assertThat(new ListChangeMove<>(variableDescriptor, e1, 0, e2, 1)).hasToString("1 {e1[0] -> e2[1]}");
    }

    @Test
    void testEnableNearbyMixedModel() {
        var moveSelectorConfig = new ListChangeMoveSelectorConfig();
        assertThat(moveSelectorConfig.canEnableNearbyInMixedModels()).isTrue();
    }
}
