package ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.childnot;

public class TestdataMultipleChildNotAnnotatedChildEntity extends TestdataMultipleChildNotAnnotatedSecondChildEntity {

    @SuppressWarnings("unused")
    public TestdataMultipleChildNotAnnotatedChildEntity() {
    }

    public TestdataMultipleChildNotAnnotatedChildEntity(long id) {
        super(id);
    }
}
