package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.basenot.interfaces.TestdataBaseNotAnnotatedInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(
        basePackageClasses = { TestdataBaseNotAnnotatedInterfaceSolution.class, DummyConstraintProvider.class })
public class OnlyChildAnnotatedInterfaceSpringTestConfiguration {

}
