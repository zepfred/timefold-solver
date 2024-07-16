package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeSolution;

@PlanningEntity
public class TestdataCascadeWrongMethod {

    public static EntityDescriptor<TestdataCascadeSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataCascadeSolution.class, TestdataCascadeEntity.class,
                        TestdataCascadeWrongMethod.class)
                .findEntityDescriptorOrFail(TestdataCascadeWrongMethod.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeWrongMethod previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeWrongMethod next;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "badUpdateCascadeValueWithReturnType")
    private Integer cascadeValueReturnType;
    private Integer value;

    public TestdataCascadeWrongMethod(Integer value) {
        this.value = value;
    }

    public TestdataCascadeEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataCascadeEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadeWrongMethod getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadeWrongMethod previous) {
        this.previous = previous;
    }

    public TestdataCascadeWrongMethod getNext() {
        return next;
    }

    public void setNext(TestdataCascadeWrongMethod next) {
        this.next = next;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getCascadeValue() {
        return cascadeValue;
    }

    public Integer getCascadeValueReturnType() {
        return cascadeValueReturnType;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        if (value != null) {
            value = value + 1;
        }
    }

    public Integer updateCascadeValueWithReturnType() {
        updateCascadeValue();
        cascadeValueReturnType = cascadeValue;
        return cascadeValueReturnType;
    }
}