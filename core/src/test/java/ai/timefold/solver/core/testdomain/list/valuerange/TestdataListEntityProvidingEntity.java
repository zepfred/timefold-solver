package ai.timefold.solver.core.testdomain.list.valuerange;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataListEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityProvidingEntity.class);
    }

    public static ListVariableDescriptor<TestdataListEntityProvidingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListEntityProvidingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    // Temporarily disabled
    // @ValueRangeProvider(id = "valueRange")
    private final List<TestdataListEntityProvidingValue> valueRange;
    // Temporarily disabled
    // @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    @PlanningListVariable
    private List<TestdataListEntityProvidingValue> valueList;

    public TestdataListEntityProvidingEntity() {
        // Required for cloning
        valueRange = new ArrayList<>();
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingEntity(String code, List<TestdataListEntityProvidingValue> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingEntity(String code, List<TestdataListEntityProvidingValue> valueRange,
            List<TestdataListEntityProvidingValue> valueList) {
        super(code);
        this.valueRange = valueRange;
        this.valueList = valueList;
        for (var i = 0; i < valueList.size(); i++) {
            var value = valueList.get(i);
            value.setEntity(this);
            value.setIndex(i);
        }
    }

    public List<TestdataListEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataListEntityProvidingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListEntityProvidingValue> valueList) {
        this.valueList = valueList;
    }
}
