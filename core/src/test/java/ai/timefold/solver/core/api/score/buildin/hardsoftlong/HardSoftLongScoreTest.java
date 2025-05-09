package ai.timefold.solver.core.api.score.buildin.hardsoftlong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class HardSoftLongScoreTest extends AbstractScoreTest {

    @Test
    void of() {
        assertThat(HardSoftLongScore.ofHard(-147L)).isEqualTo(HardSoftLongScore.of(-147L, 0L));
        assertThat(HardSoftLongScore.ofSoft(-258L)).isEqualTo(HardSoftLongScore.of(0L, -258L));
    }

    @Test
    void parseScore() {
        assertThat(HardSoftLongScore.parseScore("-147hard/-258soft")).isEqualTo(HardSoftLongScore.of(-147L, -258L));
        assertThat(HardSoftLongScore.parseScore("-147hard/*soft")).isEqualTo(HardSoftLongScore.of(-147L, Long.MIN_VALUE));
    }

    @Test
    void toShortString() {
        assertThat(HardSoftLongScore.of(0L, 0L).toShortString()).isEqualTo("0");
        assertThat(HardSoftLongScore.of(0L, -258L).toShortString()).isEqualTo("-258soft");
        assertThat(HardSoftLongScore.of(-147L, 0L).toShortString()).isEqualTo("-147hard");
        assertThat(HardSoftLongScore.of(-147L, -258L).toShortString()).isEqualTo("-147hard/-258soft");
    }

    @Test
    void testToString() {
        assertThat(HardSoftLongScore.of(0L, -258L)).hasToString("0hard/-258soft");
        assertThat(HardSoftLongScore.of(-147L, -258L)).hasToString("-147hard/-258soft");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> HardSoftLongScore.parseScore("-147"));
    }

    @Test
    void feasible() {
        assertScoreNotFeasible(HardSoftLongScore.of(-5L, -300L));
        assertScoreFeasible(HardSoftLongScore.of(0L, -300L),
                HardSoftLongScore.of(2L, -300L));
    }

    @Test
    void add() {
        assertThat(HardSoftLongScore.of(20L, -20L).add(
                HardSoftLongScore.of(-1L, -300L))).isEqualTo(HardSoftLongScore.of(19L, -320L));
    }

    @Test
    void subtract() {
        assertThat(HardSoftLongScore.of(20L, -20L).subtract(
                HardSoftLongScore.of(-1L, -300L))).isEqualTo(HardSoftLongScore.of(21L, 280L));
    }

    @Test
    void multiply() {
        assertThat(HardSoftLongScore.of(5L, -5L).multiply(1.2)).isEqualTo(HardSoftLongScore.of(6L, -6L));
        assertThat(HardSoftLongScore.of(1L, -1L).multiply(1.2)).isEqualTo(HardSoftLongScore.of(1L, -2L));
        assertThat(HardSoftLongScore.of(4L, -4L).multiply(1.2)).isEqualTo(HardSoftLongScore.of(4L, -5L));
    }

    @Test
    void divide() {
        assertThat(HardSoftLongScore.of(25L, -25L).divide(5.0)).isEqualTo(HardSoftLongScore.of(5L, -5L));
        assertThat(HardSoftLongScore.of(21L, -21L).divide(5.0)).isEqualTo(HardSoftLongScore.of(4L, -5L));
        assertThat(HardSoftLongScore.of(24L, -24L).divide(5.0)).isEqualTo(HardSoftLongScore.of(4L, -5L));
    }

    @Test
    void power() {
        assertThat(HardSoftLongScore.of(-4L, 5L).power(2.0)).isEqualTo(HardSoftLongScore.of(16L, 25L));
        assertThat(HardSoftLongScore.of(16L, 25L).power(0.5)).isEqualTo(HardSoftLongScore.of(4L, 5L));
    }

    @Test
    void negate() {
        assertThat(HardSoftLongScore.of(4L, -5L).negate()).isEqualTo(HardSoftLongScore.of(-4L, 5L));
        assertThat(HardSoftLongScore.of(-4L, 5L).negate()).isEqualTo(HardSoftLongScore.of(4L, -5L));
    }

    @Test
    void abs() {
        assertThat(HardSoftLongScore.of(4L, 5L).abs()).isEqualTo(HardSoftLongScore.of(4L, 5L));
        assertThat(HardSoftLongScore.of(4L, -5L).abs()).isEqualTo(HardSoftLongScore.of(4L, 5L));
        assertThat(HardSoftLongScore.of(-4L, 5L).abs()).isEqualTo(HardSoftLongScore.of(4L, 5L));
        assertThat(HardSoftLongScore.of(-4L, -5L).abs()).isEqualTo(HardSoftLongScore.of(4L, 5L));
    }

    @Test
    void zero() {
        HardSoftLongScore manualZero = HardSoftLongScore.of(0, 0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            HardSoftLongScore manualOne = HardSoftLongScore.of(0, 1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(HardSoftLongScore.of(-10L, -200L),
                HardSoftLongScore.of(-10L, -200L));
        PlannerAssert.assertObjectsAreNotEqual(
                HardSoftLongScore.of(-10L, -200L),
                HardSoftLongScore.of(-30L, -200L),
                HardSoftLongScore.of(-10L, -400L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                HardSoftLongScore.of(-20L, Long.MIN_VALUE),
                HardSoftLongScore.of(-20L, -20L),
                HardSoftLongScore.of(-1L, -300L),
                HardSoftLongScore.of(-1L, 4000L),
                HardSoftLongScore.of(0L, -1L),
                HardSoftLongScore.of(0L, 0L),
                HardSoftLongScore.of(0L, 1L));
    }
}
