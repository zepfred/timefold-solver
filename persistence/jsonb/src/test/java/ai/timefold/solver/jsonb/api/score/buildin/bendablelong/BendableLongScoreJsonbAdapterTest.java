package ai.timefold.solver.jsonb.api.score.buildin.bendablelong;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapterTest;

import org.junit.jupiter.api.Test;

class BendableLongScoreJsonbAdapterTest extends AbstractScoreJsonbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableLongScoreWrapper(null));
        BendableLongScore score = BendableLongScore.of(new long[] { 1000L, 200L }, new long[] { 34L });
        assertSerializeAndDeserialize(score, new TestBendableLongScoreWrapper(score));
    }

    public static class TestBendableLongScoreWrapper extends TestScoreWrapper<BendableLongScore> {

        @JsonbTypeAdapter(BendableLongScoreJsonbAdapter.class)
        private BendableLongScore score;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestBendableLongScoreWrapper() {
        }

        public TestBendableLongScoreWrapper(BendableLongScore score) {
            this.score = score;
        }

        @Override
        public BendableLongScore getScore() {
            return score;
        }

        @Override
        public void setScore(BendableLongScore score) {
            this.score = score;
        }

    }
}
