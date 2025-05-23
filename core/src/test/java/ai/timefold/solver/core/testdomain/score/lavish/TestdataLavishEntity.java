package ai.timefold.solver.core.testdomain.score.lavish;

import java.math.BigDecimal;
import java.math.BigInteger;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataLavishEntity extends TestdataObject {

    public static final String VALUE_FIELD = "value";

    public static EntityDescriptor<TestdataLavishSolution> buildEntityDescriptor() {
        return TestdataLavishSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataLavishEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataLavishSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataLavishEntityGroup entityGroup;
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private TestdataLavishValue value;

    private String stringProperty = "";
    private Integer integerProperty = 1;
    private Long longProperty = 1L;
    private BigInteger bigIntegerProperty = BigInteger.ONE;
    private BigDecimal bigDecimalProperty = BigDecimal.ONE;

    public TestdataLavishEntity() {
    }

    public TestdataLavishEntity(String code, TestdataLavishEntityGroup entityGroup) {
        this(code, entityGroup, null);
    }

    public TestdataLavishEntity(String code, TestdataLavishEntityGroup entityGroup, TestdataLavishValue value) {
        super(code);
        this.entityGroup = entityGroup;
        this.value = value;
    }

    // ************************************************************************
    // Getter/setters
    // ************************************************************************

    public TestdataLavishEntityGroup getEntityGroup() {
        return entityGroup;
    }

    public void setEntityGroup(TestdataLavishEntityGroup entityGroup) {
        this.entityGroup = entityGroup;
    }

    public TestdataLavishValue getValue() {
        return value;
    }

    public void setValue(TestdataLavishValue value) {
        this.value = value;
    }

    public String getStringProperty() {
        return stringProperty;
    }

    public void setStringProperty(String stringProperty) {
        this.stringProperty = stringProperty;
    }

    public Integer getIntegerProperty() {
        return integerProperty;
    }

    public void setIntegerProperty(Integer integerProperty) {
        this.integerProperty = integerProperty;
    }

    public Long getLongProperty() {
        return longProperty;
    }

    public void setLongProperty(Long longProperty) {
        this.longProperty = longProperty;
    }

    public BigInteger getBigIntegerProperty() {
        return bigIntegerProperty;
    }

    public void setBigIntegerProperty(BigInteger bigIntegerProperty) {
        this.bigIntegerProperty = bigIntegerProperty;
    }

    public BigDecimal getBigDecimalProperty() {
        return bigDecimalProperty;
    }

    public void setBigDecimalProperty(BigDecimal bigDecimalProperty) {
        this.bigDecimalProperty = bigDecimalProperty;
    }

}
