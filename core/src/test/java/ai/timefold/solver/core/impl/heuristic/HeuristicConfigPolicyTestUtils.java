package ai.timefold.solver.core.impl.heuristic;

import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

public final class HeuristicConfigPolicyTestUtils {

    public static HeuristicConfigPolicy<TestdataSolution> buildHeuristicConfigPolicy() {
        return buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());
    }

    public static <Solution_> HeuristicConfigPolicy<Solution_>
            buildHeuristicConfigPolicy(SolutionDescriptor<Solution_> solutionDescriptor) {
        return new HeuristicConfigPolicy.Builder<Solution_>()
                .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                .withRandom(new Random())
                .withSolutionDescriptor(solutionDescriptor)
                .withClassInstanceCache(ClassInstanceCache.create())
                .build();
    }

    private HeuristicConfigPolicyTestUtils() {
    }
}
