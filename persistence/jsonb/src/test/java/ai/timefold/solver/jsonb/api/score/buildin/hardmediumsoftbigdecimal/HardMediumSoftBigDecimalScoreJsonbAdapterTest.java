package ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoftbigdecimal;

import java.math.BigDecimal;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapterTest;

import org.junit.jupiter.api.Test;

class HardMediumSoftBigDecimalScoreJsonbAdapterTest extends AbstractScoreJsonbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftBigDecimalScoreWrapper(null));
        HardMediumSoftBigDecimalScore score = HardMediumSoftBigDecimalScore.of(new BigDecimal("1200.0021"),
                new BigDecimal("-3.1415"),
                new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardMediumSoftBigDecimalScoreWrapper(score));
    }

    public static class TestHardMediumSoftBigDecimalScoreWrapper extends TestScoreWrapper<HardMediumSoftBigDecimalScore> {

        @JsonbTypeAdapter(HardMediumSoftBigDecimalScoreJsonbAdapter.class)
        private HardMediumSoftBigDecimalScore score;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestHardMediumSoftBigDecimalScoreWrapper() {
        }

        public TestHardMediumSoftBigDecimalScoreWrapper(HardMediumSoftBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftBigDecimalScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardMediumSoftBigDecimalScore score) {
            this.score = score;
        }

    }
}
