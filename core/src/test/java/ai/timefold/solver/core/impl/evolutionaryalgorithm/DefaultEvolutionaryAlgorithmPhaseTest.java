package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.LongAdder;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryAlgorithmPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultivarIncrementalScoreCalculator;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DefaultEvolutionaryAlgorithmPhaseTest extends AbstractMeterTest {

    @Test
    @Disabled("Temporarily disabled due to a focus on the list variables")
    void solveBasicVar() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(10))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());

        var solution = TestdataSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    @Disabled("Temporarily disabled due to a focus on the list variables")
    void solveMultiBasicVar() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataMultiVarSolution.class)
                .withEntityClasses(TestdataMultiVarEntity.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withIncrementalScoreCalculatorClass(TestdataMultivarIncrementalScoreCalculator.class))
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(10))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());
        var solution = TestdataMultiVarSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveListVar() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestingListSingleValueEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());

        var solution = TestdataListSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    public static final class TestingListSingleValueEasyScoreCalculator
            implements EasyScoreCalculator<TestdataListSolution, SimpleScore> {
        public @NonNull SimpleScore calculateScore(@NonNull TestdataListSolution testdataSolution) {
            LongAdder sum = new LongAdder();
            testdataSolution.getEntityList().forEach(e -> {
                int size = e.getValueList().size();
                if (size == 0) {
                    sum.increment();
                } else if (size > 1) {
                    double penalty = Math.pow(size - 1, 2);
                    sum.add((long) penalty);
                }
            });
            return SimpleScore.of(-sum.intValue());
        }
    }

}
