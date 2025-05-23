package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.function.Function;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Deprecated(forRemoval = true)
public class ScoreManagerTest {

    public static final SolverFactory<TestdataSolution> SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataSolverConfig.xml");

    @ParameterizedTest
    @EnumSource(ScoreManagerSource.class)
    void updateScore(ScoreManagerSource scoreManagerSource) {
        ScoreManager<TestdataSolution, ?> scoreManager = scoreManagerSource.createScoreManager(SOLVER_FACTORY);
        assertThat(scoreManager).isNotNull();
        TestdataSolution solution = TestdataSolution.generateSolution();
        assertThat(solution.getScore()).isNull();
        scoreManager.updateScore(solution);
        assertThat(solution.getScore()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ScoreManagerSource.class)
    void explainScore(ScoreManagerSource scoreManagerSource) {
        ScoreManager<TestdataSolution, ?> scoreManager = scoreManagerSource.createScoreManager(SOLVER_FACTORY);
        assertThat(scoreManager).isNotNull();
        TestdataSolution solution = TestdataSolution.generateSolution();
        ScoreExplanation<TestdataSolution, ?> scoreExplanation = scoreManager.explainScore(solution);
        assertThat(scoreExplanation).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(scoreExplanation.getScore()).isNotNull();
            softly.assertThat(scoreExplanation.getSummary()).isNotBlank();
            softly.assertThat(scoreExplanation.getConstraintMatchTotalMap())
                    .containsOnlyKeys("ai.timefold.solver.core.testdomain/testConstraint");
            softly.assertThat(scoreExplanation.getIndictmentMap())
                    .containsOnlyKeys(solution.getEntityList().toArray());

        });
    }

    public enum ScoreManagerSource {

        FROM_SOLVER_FACTORY(ScoreManager::create),
        FROM_SOLVER_MANAGER(solverFactory -> ScoreManager.create(SolverManager.create(solverFactory)));

        private final Function<SolverFactory, ScoreManager> scoreManagerConstructor;

        ScoreManagerSource(Function<SolverFactory, ScoreManager> scoreManagerConstructor) {
            this.scoreManagerConstructor = scoreManagerConstructor;
        }

        public <Solution_, Score_ extends Score<Score_>> ScoreManager<Solution_, Score_>
                createScoreManager(SolverFactory<Solution_> solverFactory) {
            return scoreManagerConstructor.apply(solverFactory);
        }

    }

}
