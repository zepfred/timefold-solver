package ai.timefold.solver.core.impl.solver.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Random;

import ai.timefold.solver.core.impl.util.Pair;

import org.junit.jupiter.api.Test;

class ResumableRandomGeneratorTest {

    @Test
    void compatibility() {
        var seed = System.nanoTime();
        var random = new Random(seed);
        var resumableRandom = new ResumableRandomGenerator(seed);
        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextInt()).isEqualTo(resumableRandom.nextInt());
        }
        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextDouble()).isEqualTo(resumableRandom.nextDouble());
        }
        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextFloat()).isEqualTo(resumableRandom.nextFloat());
        }
        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextBoolean()).isEqualTo(resumableRandom.nextBoolean());
        }
    }

    @Test
    void resume() {
        var seed = System.nanoTime();
        var resumableRandom = new ResumableRandomGenerator(seed);
        var numbers = new ArrayList<Pair<Integer, Long>>(1000);
        for (int i = 0; i < 1000; i++) {
            var currentSeed = resumableRandom.getSeed();
            numbers.add(new Pair<>(resumableRandom.nextInt(), currentSeed));
        }
        for (int i = 0; i < 1000; i++) {
            var number = numbers.get(i);
            resumableRandom.resumeSeed(number.value());
            for (int j = i; j < 1000; j++) {
                assertThat(resumableRandom.nextInt()).isEqualTo(numbers.get(j).key());
            }
        }
    }
}
