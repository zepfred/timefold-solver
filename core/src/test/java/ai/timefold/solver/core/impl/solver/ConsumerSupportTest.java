package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ConsumerSupportTest {

    private ConsumerSupport<TestdataSolution, Long> consumerSupport;

    @AfterEach
    void close() {
        consumerSupport.close();
    }

    @Test
    @Timeout(60)
    void skipAhead() throws InterruptedException {
        DefaultSolver<TestdataSolution> solver = mock(DefaultSolver.class);
        CountDownLatch consumptionStarted = new CountDownLatch(1);
        CountDownLatch consumptionPaused = new CountDownLatch(1);
        CountDownLatch consumptionCompleted = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        List<TestdataSolution> consumedSolutions = Collections.synchronizedList(new ArrayList<>());
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        consumerSupport = new ConsumerSupport<>(solver, 1L, testdataSolution -> {
            try {
                consumptionStarted.countDown();
                consumptionPaused.await();
                consumedSolutions.add(testdataSolution);
                if (testdataSolution.getEntityList().size() == 3) { // The last best solution.
                    consumptionCompleted.countDown();
                }
            } catch (InterruptedException e) {
                error.set(new IllegalStateException("Interrupted waiting.", e));
            }
        }, null, null, null, null, bestSolutionHolder);

        var solution = TestdataSolution.generateSolution(1, 1);
        when(solver.cloneSolution(any(TestdataSolution.class))).thenReturn(solution);
        consumeIntermediateBestSolution(solution);
        consumptionStarted.await();
        // This solution should be skipped.
        var solution2 = TestdataSolution.generateSolution(2, 2);
        when(solver.cloneSolution(any(TestdataSolution.class))).thenReturn(solution2);
        consumeIntermediateBestSolution(solution2);
        // This solution should never be skipped.
        var solution3 = TestdataSolution.generateSolution(3, 3);
        when(solver.cloneSolution(any(TestdataSolution.class))).thenReturn(solution3);
        consumeIntermediateBestSolution(solution3);
        consumptionPaused.countDown();
        consumptionCompleted.await();
        assertThat(consumedSolutions).hasSize(2);
        assertThat(consumedSolutions.get(0).getEntityList()).hasSize(1);
        assertThat(consumedSolutions.get(1).getEntityList()).hasSize(3);

        if (error.get() != null) {
            fail("Exception during consumption.", error.get());
        }
    }

    @Test
    @Timeout(60)
    void problemChangesComplete_afterFinalBestSolutionIsConsumed() throws ExecutionException, InterruptedException {
        DefaultSolver<TestdataSolution> solver = mock(DefaultSolver.class);
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        AtomicReference<TestdataSolution> finalBestSolutionRef = new AtomicReference<>();
        consumerSupport =
                new ConsumerSupport<>(solver, 1L, null, finalBestSolutionRef::set, null, null, null, bestSolutionHolder);

        CompletableFuture<Void> futureProblemChange = addProblemChange(bestSolutionHolder);

        consumeIntermediateBestSolution(TestdataSolution.generateSolution());
        assertThat(futureProblemChange).isNotCompleted();
        TestdataSolution finalBestSolution = TestdataSolution.generateSolution();
        consumerSupport.consumeFinalBestSolution(finalBestSolution);
        futureProblemChange.get();
        assertThat(finalBestSolutionRef.get()).isSameAs(finalBestSolution);
        assertThat(futureProblemChange).isCompleted();
    }

    @Test
    @Timeout(60)
    void problemChangesCompleteExceptionally_afterExceptionInConsumer() {
        DefaultSolver<TestdataSolution> solver = mock(DefaultSolver.class);
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        final String errorMessage = "Test exception";
        Consumer<TestdataSolution> errorneousConsumer = bestSolution -> {
            throw new RuntimeException(errorMessage);
        };
        consumerSupport = new ConsumerSupport<>(solver, 1L, errorneousConsumer, null, null, null, null, bestSolutionHolder);

        CompletableFuture<Void> futureProblemChange = addProblemChange(bestSolutionHolder);
        consumeIntermediateBestSolution(TestdataSolution.generateSolution());

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                futureProblemChange::get)
                .havingRootCause()
                .isInstanceOf(RuntimeException.class)
                .withMessage(errorMessage);
        assertThat(futureProblemChange).isCompletedExceptionally();
    }

    @Test
    @Timeout(60)
    void pendingProblemChangesAreCanceled_afterFinalBestSolutionIsConsumed() throws ExecutionException, InterruptedException {
        DefaultSolver<TestdataSolution> solver = mock(DefaultSolver.class);
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        consumerSupport = new ConsumerSupport<>(solver, 1L, null, null,
                null, null, null, bestSolutionHolder);

        CompletableFuture<Void> futureProblemChange = addProblemChange(bestSolutionHolder);

        var solution = TestdataSolution.generateSolution();
        when(solver.cloneSolution(any(TestdataSolution.class))).thenReturn(solution);
        consumeIntermediateBestSolution(solution);
        assertThat(futureProblemChange).isNotCompleted();

        solution = TestdataSolution.generateSolution();
        when(solver.cloneSolution(any(TestdataSolution.class))).thenReturn(solution);
        CompletableFuture<Void> pendingProblemChange = addProblemChange(bestSolutionHolder);
        consumerSupport.consumeFinalBestSolution(solution);
        futureProblemChange.get();
        assertThat(futureProblemChange).isCompleted();

        assertThatExceptionOfType(CancellationException.class).isThrownBy(pendingProblemChange::get);
    }

    private CompletableFuture<Void> addProblemChange(BestSolutionHolder<TestdataSolution> bestSolutionHolder) {
        return bestSolutionHolder.addProblemChange(mock(Solver.class), List.of(mock(ProblemChange.class)));
    }

    private void consumeIntermediateBestSolution(TestdataSolution bestSolution) {
        consumerSupport.consumeIntermediateBestSolution(bestSolution, () -> true);
    }
}
