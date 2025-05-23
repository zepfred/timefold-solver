
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.quarkus.rest.TestdataQuarkusSolutionConfigResource;
import ai.timefold.solver.quarkus.testdomain.dummy.DummyDistanceMeter;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValidationException;

class TimefoldProcessorMultipleSolversInvalidNearbyClassTest {

    // No class found
    @RegisterExtension
    static final QuarkusUnitTest config1 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.spent-limit", "8s")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".nearby-distance-meter-class",
                    "ai.timefold.solver.quarkus.testdomain.dummy.DummyDistanceMeter")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".termination.spent-limit", "4s")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class, TestdataQuarkusSolutionConfigResource.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining(
                            "The config property quarkus.timefold.solver.\"solver1\".nearby-distance-meter-class with the config value")
                    .hasMessageContaining(DummyDistanceMeter.class.getName())
                    .hasMessageContaining("not found"));

    // Invalid class
    @RegisterExtension
    static final QuarkusUnitTest config2 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.spent-limit", "8s")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".termination.spent-limit", "4s")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".nearby-distance-meter-class",
                    "ai.timefold.solver.quarkus.rest.TestdataQuarkusSolutionConfigResource")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class, TestdataQuarkusSolutionConfigResource.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "The Nearby Selection Meter class")
                    .hasMessageContaining(TestdataQuarkusSolutionConfigResource.class.getName())
                    .hasMessageContaining("of the solver config (solver2) does not implement NearbyDistanceMeter"));

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
