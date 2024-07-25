package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplyEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplySolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplyValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingDuplicatedSources;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingNoSources;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SingleCascadingUpdateShadowVariableWithSupplyListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongSource::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but the shadow variable \"bad\" cannot be found")
                .withMessageContaining(
                        "Maybe update sourceVariableName to an existing shadow variable in the entity class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongMethod::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValueReturnType)")
                .withMessageContaining("but the method \"badUpdateCascadeValueWithReturnType\" cannot be found");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingNoSources::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingNoSources)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but neither the sourceVariableName nor the sourceVariableNames properties are set.")
                .withMessageContaining(
                        "Maybe update the field \"cascadeValue\" and set one of the properties ([sourceVariableName, sourceVariableNames]).");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingDuplicatedSources::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingDuplicatedSources)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but it is only possible to define either sourceVariableName or sourceVariableNames.")
                .withMessageContaining(
                        "Maybe update the field \"cascadeValue\" to set only one of the properties ([sourceVariableName, sourceVariableNames]).");
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(0).getCascadeValueReturnType()).isEqualTo(3);
        assertThat(entity.getValueList().get(0).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValueReturnType()).isEqualTo(4);
        assertThat(entity.getValueList().get(1).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValueReturnType()).isEqualTo(5);
        assertThat(entity.getValueList().get(2).getSecondNumberOfCalls()).isOne();
    }

    @Test
    void updateOnlyMiddleValue() {
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();
        solution.getValueList().forEach(TestdataSingleCascadingWithSupplyValue::reset);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 3);
        entity.setValueList(
                List.of(solution.getValueList().get(0), solution.getValueList().get(2), solution.getValueList().get(1)));
        scoreDirector.afterListVariableChanged(entity, "valueList", 1, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isZero();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(4);
        // Called from previous and inverse element change
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        // Called from update next val
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isOne();
    }

    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();
        solution.getValueList().forEach(TestdataSingleCascadingWithSupplyValue::reset);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(
                List.of(solution.getValueList().get(2), solution.getValueList().get(0), solution.getValueList().get(1)));
        solution.getValueList().get(2).setCascadeValue(4);
        solution.getValueList().get(0).setCascadeValue(2);
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        // Stop on value2
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isZero();
    }
}
