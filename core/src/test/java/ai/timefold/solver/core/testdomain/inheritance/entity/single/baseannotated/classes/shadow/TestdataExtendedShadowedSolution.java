package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataExtendedShadowedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataExtendedShadowedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedShadowedSolution.class,
                TestdataExtendedShadowedParentEntity.class, TestdataExtendedShadowedChildEntity.class);
    }

    private List<TestdataValue> valueList;
    private List<TestdataExtendedShadowedParentEntity> entityList;

    private SimpleScore score;

    public TestdataExtendedShadowedSolution() {
    }

    public TestdataExtendedShadowedSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataExtendedShadowedParentEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataExtendedShadowedParentEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
