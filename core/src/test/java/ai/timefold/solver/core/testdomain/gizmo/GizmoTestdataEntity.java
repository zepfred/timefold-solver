package ai.timefold.solver.core.testdomain.gizmo;

import java.util.Collection;
import java.util.Map;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class GizmoTestdataEntity {

    private String id;

    @PlanningVariable
    public TestdataValue value;

    public boolean isPinned;

    public Collection<Map<String, String>> genericField;

    public GizmoTestdataEntity(String id, TestdataValue value, boolean isPinned) {
        this.id = id;
        this.value = value;
        this.isPinned = isPinned;
    }

    @PlanningId
    public String getId() {
        return id;
    }

    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @PlanningPin
    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String readMethod() {
        return "Read Method";
    }

    public String methodWithParameters(String parameter) {
        return parameter;
    }

    public void getVoid() {
        // Ignore
    }

    public void voidMethod() {
        // Ignore
    }

    public String isAMethodThatHasABadName() {
        return "It should start with get not is.";
    }

    private String getBadMethod() {
        return "Creating a Member Descriptor for this method should throw as it is private.";
    }
}
