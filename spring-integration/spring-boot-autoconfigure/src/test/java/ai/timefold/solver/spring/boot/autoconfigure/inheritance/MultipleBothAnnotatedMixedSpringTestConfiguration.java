package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.mixed.TestdataMultipleMixedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataMultipleMixedSolution.class })
public class MultipleBothAnnotatedMixedSpringTestConfiguration {

}
