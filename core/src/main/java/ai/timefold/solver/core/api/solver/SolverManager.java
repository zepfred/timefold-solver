package ai.timefold.solver.core.api.solver;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A SolverManager solves multiple planning problems of the same domain,
 * asynchronously without blocking the calling thread.
 * <p>
 * To create a SolverManager, use {@link #create(SolverFactory, SolverManagerConfig)}.
 * To solve a planning problem, call {@link #solve(Object, Object, Consumer)}
 * or {@link #solveAndListen(Object, Object, Consumer)}.
 * <p>
 * These methods are thread-safe unless explicitly stated otherwise.
 * <p>
 * Internally a SolverManager manages a thread pool of solver threads (which call {@link Solver#solve(Object)})
 * and consumer threads (to handle the {@link BestSolutionChangedEvent}s).
 * <p>
 * To learn more about problem change semantics, please refer to the {@link ProblemChange} Javadoc.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
 */
public interface SolverManager<Solution_, ProblemId_> extends AutoCloseable {

    // ************************************************************************
    // Static creation methods: SolverConfig and SolverFactory
    // ************************************************************************

    /**
     * Use a {@link SolverConfig} to build a {@link SolverManager}.
     * <p>
     * When using {@link SolutionManager} too, use {@link #create(SolverFactory)} instead
     * so they reuse the same {@link SolverFactory} instance.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, ProblemId_> @NonNull SolverManager<Solution_, ProblemId_> create(
            @NonNull SolverConfig solverConfig) {
        return create(solverConfig, new SolverManagerConfig());
    }

    /**
     * Use a {@link SolverConfig} and a {@link SolverManagerConfig} to build a {@link SolverManager}.
     * <p>
     * When using {@link SolutionManager} too, use {@link #create(SolverFactory, SolverManagerConfig)} instead
     * so they reuse the same {@link SolverFactory} instance.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
     */
    static <Solution_, ProblemId_> @NonNull SolverManager<Solution_, ProblemId_> create(
            @NonNull SolverConfig solverConfig, @NonNull SolverManagerConfig solverManagerConfig) {
        return create(SolverFactory.create(solverConfig), solverManagerConfig);
    }

    /**
     * Use a {@link SolverFactory} to build a {@link SolverManager}.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, ProblemId_> @NonNull SolverManager<Solution_, ProblemId_> create(
            @NonNull SolverFactory<Solution_> solverFactory) {
        return create(solverFactory, new SolverManagerConfig());
    }

    /**
     * Use a {@link SolverFactory} and a {@link SolverManagerConfig} to build a {@link SolverManager}.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
     */
    static <Solution_, ProblemId_> @NonNull SolverManager<Solution_, ProblemId_> create(
            @NonNull SolverFactory<Solution_> solverFactory, @NonNull SolverManagerConfig solverManagerConfig) {
        return new DefaultSolverManager<>(solverFactory, solverManagerConfig);
    }

    // ************************************************************************
    // Builder method
    // ************************************************************************

    /**
     * Creates a Builder that allows to customize and submit a planning problem to solve.
     *
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_> solveBuilder();

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * Submits a planning problem to solve and returns immediately.
     * The planning problem is solved on a solver {@link Thread}, as soon as one is available.
     * To retrieve the final best solution, use {@link SolverJob#getFinalBestSolution()}.
     * <p>
     * In server applications, it's recommended to use {@link #solve(Object, Object, Consumer)} instead,
     * to avoid loading the problem going stale if solving can't start immediately.
     * To listen to intermediate best solutions too, use {@link #solveAndListen(Object, Object, Consumer)} instead.
     * <p>
     * Defaults to logging exceptions as an error.
     * <p>
     * To stop a solver job before it naturally terminates, call {@link SolverJob#terminateEarly()}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     * @param problem a {@link PlanningSolution} usually with uninitialized planning variables
     */
    default @NonNull SolverJob<Solution_, ProblemId_> solve(@NonNull ProblemId_ problemId, @NonNull Solution_ problem) {
        return solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem)
                .run();
    }

    /**
     * As defined by {@link #solve(Object, Object)}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problem a {@link PlanningSolution} usually with uninitialized planning variables
     * @param finalBestSolutionConsumer called only once, at the end, on a consumer thread
     */
    default @NonNull SolverJob<Solution_, ProblemId_> solve(@NonNull ProblemId_ problemId,
            @NonNull Solution_ problem, @Nullable Consumer<? super Solution_> finalBestSolutionConsumer) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem);
        if (finalBestSolutionConsumer != null) {
            builder.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        }
        return builder.run();
    }

    /**
     * As defined by {@link #solve(Object, Object)}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problem a {@link PlanningSolution} usually with uninitialized planning variables
     * @param finalBestSolutionConsumer called only once, at the end, on a consumer thread
     * @param exceptionHandler called if an exception or error occurs.
     *        If null it defaults to logging the exception as an error.
     * @deprecated It is recommended to use {@link #solveBuilder()}
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solve(@NonNull ProblemId_ problemId,
            @NonNull Solution_ problem, @Nullable Consumer<? super Solution_> finalBestSolutionConsumer,
            @Nullable BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem);
        if (finalBestSolutionConsumer != null) {
            builder.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        }
        if (exceptionHandler != null) {
            builder.withExceptionHandler(exceptionHandler);
        }
        return builder.run();
    }

    /**
     * Submits a planning problem to solve and returns immediately.
     * The planning problem is solved on a solver {@link Thread}, as soon as one is available.
     * <p>
     * When the solver terminates, the {@code finalBestSolutionConsumer} is called once with the final best solution,
     * on a consumer {@link Thread}, as soon as one is available.
     * To listen to intermediate best solutions too, use {@link #solveAndListen(Object, Object, Consumer)} instead.
     * <p>
     * Defaults to logging exceptions as an error.
     * <p>
     * To stop a solver job before it naturally terminates, call {@link #terminateEarly(Object)}.
     *
     * @param problemId an ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problemFinder a function that returns a {@link PlanningSolution}, usually with uninitialized planning
     *        variables
     * @param finalBestSolutionConsumer called only once, at the end, on a consumer thread
     * @deprecated It is recommended to use {@link #solveBuilder()}
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solve(@NonNull ProblemId_ problemId,
            @NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder,
            @Nullable Consumer<? super Solution_> finalBestSolutionConsumer) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(problemFinder);
        if (finalBestSolutionConsumer != null) {
            builder.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        }
        return builder.run();
    }

    /**
     * As defined by {@link #solve(Object, Function, Consumer)}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problemFinder function that returns a {@link PlanningSolution}, usually with uninitialized planning
     *        variables
     * @param finalBestSolutionConsumer called only once, at the end, on a consumer thread
     * @param exceptionHandler called if an exception or error occurs.
     *        If null it defaults to logging the exception as an error.
     * @deprecated It is recommended to use {@link #solveBuilder()}
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solve(@NonNull ProblemId_ problemId,
            @NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder,
            @Nullable Consumer<? super Solution_> finalBestSolutionConsumer,
            @Nullable BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(problemFinder);
        if (finalBestSolutionConsumer != null) {
            builder.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        }
        if (exceptionHandler != null) {
            builder.withExceptionHandler(exceptionHandler);
        }
        return builder.run();
    }

    /**
     * Submits a planning problem to solve and returns immediately.
     * The planning problem is solved on a solver {@link Thread}, as soon as one is available.
     * <p>
     * When the solver finds a new best solution, the {@code bestSolutionConsumer} is called every time,
     * on a consumer {@link Thread}, as soon as one is available (taking into account any throttling waiting time),
     * unless a newer best solution is already available by then (in which case skip ahead discards it).
     * <p>
     * Defaults to logging exceptions as an error.
     * <p>
     * To stop a solver job before it naturally terminates, call {@link #terminateEarly(Object)}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problemFinder a function that returns a {@link PlanningSolution}, usually with uninitialized planning
     *        variables
     * @param bestSolutionConsumer called multiple times, on a consumer thread
     * @deprecated It is recommended to use {@link #solveBuilder()} while also providing a consumer for the best solution
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solveAndListen(@NonNull ProblemId_ problemId,
            @NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder,
            @NonNull Consumer<? super Solution_> bestSolutionConsumer) {
        return solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(problemFinder)
                .withBestSolutionConsumer(bestSolutionConsumer)
                .run();
    }

    /**
     * Submits a planning problem to solve and returns immediately.
     * The planning problem is solved on a solver {@link Thread}, as soon as one is available.
     * <p>
     * When the solver finds a new best solution, the {@code bestSolutionConsumer} is called every time,
     * on a consumer {@link Thread}, as soon as one is available (taking into account any throttling waiting time),
     * unless a newer best solution is already available by then (in which case skip ahead discards it).
     * <p>
     * Defaults to logging exceptions as an error.
     * <p>
     * To stop a solver job before it naturally terminates, call {@link #terminateEarly(Object)}.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problem a {@link PlanningSolution} usually with uninitialized planning variables
     * @param bestSolutionConsumer called multiple times, on a consumer thread
     */
    default @NonNull SolverJob<Solution_, ProblemId_> solveAndListen(@NonNull ProblemId_ problemId, @NonNull Solution_ problem,
            @NonNull Consumer<? super Solution_> bestSolutionConsumer) {
        return solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem)
                .withBestSolutionConsumer(bestSolutionConsumer)
                .run();
    }

    /**
     * As defined by {@link #solveAndListen(Object, Function, Consumer)}.
     *
     * @param problemId an ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problemFinder function that returns a {@link PlanningSolution}, usually with uninitialized planning
     *        variables
     * @param bestSolutionConsumer called multiple times, on a consumer thread
     * @param exceptionHandler called if an exception or error occurs.
     *        If null it defaults to logging the exception as an error.
     * @deprecated It is recommended to use {@link #solveBuilder()} while also providing a consumer for the best solution
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solveAndListen(@NonNull ProblemId_ problemId,
            @NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder,
            @NonNull Consumer<? super Solution_> bestSolutionConsumer,
            @Nullable BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(problemFinder)
                .withBestSolutionConsumer(bestSolutionConsumer);
        if (exceptionHandler != null) {
            builder.withExceptionHandler(exceptionHandler);
        }
        return builder.run();
    }

    /**
     * As defined by {@link #solveAndListen(Object, Function, Consumer)}.
     * <p>
     * The final best solution is delivered twice:
     * first to the {@code bestSolutionConsumer} when it is found
     * and then again to the {@code finalBestSolutionConsumer} when the solver terminates.
     * Do not store the solution twice.
     * This allows for use cases that only process the {@link Score} first (during best solution changed events)
     * and then store the solution upon termination.
     *
     * @param problemId an ID for each planning problem. This must be unique.
     *        Use this problemId to {@link #terminateEarly(Object) terminate} the solver early,
     *        {@link #getSolverStatus(Object) to get the status} or if the problem changes while solving.
     * @param problemFinder function that returns a {@link PlanningSolution}, usually with uninitialized planning
     *        variables
     * @param bestSolutionConsumer called multiple times, on a consumer thread
     * @param finalBestSolutionConsumer called only once, at the end, on a consumer thread.
     *        That final best solution is already consumed by the bestSolutionConsumer earlier.
     * @param exceptionHandler called if an exception or error occurs.
     *        If null it defaults to logging the exception as an error.
     * @deprecated It is recommended to use {@link #solveBuilder()} while also providing a consumer for the best solution
     */
    @Deprecated(forRemoval = true, since = "1.6.0")
    default @NonNull SolverJob<Solution_, ProblemId_> solveAndListen(@NonNull ProblemId_ problemId,
            @NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder,
            @NonNull Consumer<? super Solution_> bestSolutionConsumer,
            @Nullable Consumer<? super Solution_> finalBestSolutionConsumer,
            @Nullable BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        SolverJobBuilder<Solution_, ProblemId_> builder = solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(problemFinder)
                .withBestSolutionConsumer(bestSolutionConsumer);
        if (finalBestSolutionConsumer != null) {
            builder.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        }
        if (exceptionHandler != null) {
            builder.withExceptionHandler(exceptionHandler);
        }
        return builder.run();
    }

    /**
     * Returns if the {@link Solver} is scheduled to solve, actively solving or not.
     * <p>
     * Returns {@link SolverStatus#NOT_SOLVING} if the solver already terminated or if the problemId was never added.
     * To distinguish between both cases, use {@link SolverJob#getSolverStatus()} instead.
     * Here, that distinction is not supported because it would cause a memory leak.
     *
     * @param problemId a value given to {@link #solve(Object, Object, Consumer)}
     *        or {@link #solveAndListen(Object, Object, Consumer)}
     */
    @NonNull
    SolverStatus getSolverStatus(@NonNull ProblemId_ problemId);

    /**
     * As defined by {@link #addProblemChanges(Object, List)}, only with a single {@link ProblemChange}.
     * Prefer to submit multiple {@link ProblemChange}s at once to reduce the considerable overhead of multiple calls.
     */
    @NonNull
    default CompletableFuture<Void> addProblemChange(@NonNull ProblemId_ problemId,
            @NonNull ProblemChange<Solution_> problemChange) {
        return addProblemChanges(problemId, Collections.singletonList(problemChange));
    }

    /**
     * Schedules a batch of {@link ProblemChange problem changes} to be processed
     * by the underlying {@link Solver} and returns immediately.
     * If the solver already terminated or the problemId was never added, throws an exception.
     * The same applies if the underlying {@link Solver} is not in the {@link SolverStatus#SOLVING_ACTIVE} state.
     *
     * @param problemId a value given to {@link #solve(Object, Object, Consumer)}
     *        or {@link #solveAndListen(Object, Object, Consumer)}
     * @param problemChangeList a list of {@link ProblemChange}s to apply to the problem
     * @return completes after the best solution containing this change has been consumed.
     * @throws IllegalStateException if there is no solver actively solving the problem associated with the problemId
     * @see ProblemChange Learn more about problem change semantics.
     */
    @NonNull
    CompletableFuture<Void> addProblemChanges(@NonNull ProblemId_ problemId,
            @NonNull List<ProblemChange<Solution_>> problemChangeList);

    /**
     * Terminates the solver or cancels the solver job if it hasn't (re)started yet.
     * <p>
     * Does nothing if the solver already terminated or the problemId was never added.
     * To distinguish between both cases, use {@link SolverJob#terminateEarly()} instead.
     * Here, that distinction is not supported because it would cause a memory leak.
     * <p>
     * Waits for the termination or cancellation to complete before returning.
     * During termination, a {@code bestSolutionConsumer} could still be called. When the solver terminates,
     * the {@code finalBestSolutionConsumer} is executed with the latest best solution.
     * These consumers run on a consumer thread independently of the termination and may still run even after
     * this method returns.
     *
     * @param problemId a value given to {@link #solve(Object, Object, Consumer)}
     *        or {@link #solveAndListen(Object, Object, Consumer)}
     */
    void terminateEarly(@NonNull ProblemId_ problemId);

    /**
     * Terminates all solvers, cancels all solver jobs that haven't (re)started yet
     * and discards all queued {@link ProblemChange}s.
     * Releases all thread pool resources.
     * <p>
     * No new planning problems can be submitted after calling this method.
     */
    @Override
    void close();

}
