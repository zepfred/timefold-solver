package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataBendableLongScoreSolution;

import org.junit.jupiter.api.Test;

class BendableLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataBendableLongScoreSolution, BendableLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactHard() {
        var constraintWeight = buildScore(90, 0, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(270, 0, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft1() {
        var constraintWeight = buildScore(0, 90, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft2() {
        var constraintWeight = buildScore(0, 0, 90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = buildScore(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(300, 3_000, 30_000));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataBendableLongScoreSolution> buildSolutionDescriptor() {
        return TestdataBendableLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<BendableLongScore> buildScoreInliner(Map<Constraint, BendableLongScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        return new BendableLongScoreInliner(constraintWeightMap, constraintMatchPolicy, 1, 2);
    }

    private BendableLongScore buildScore(long hard, long soft1, long soft2) {
        return BendableLongScore.of(
                new long[] { hard },
                new long[] { soft1, soft2 });
    }

}
