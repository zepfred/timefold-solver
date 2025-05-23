package ai.timefold.solver.core.api.solver.event;

import java.util.EventListener;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@FunctionalInterface
public interface SolverEventListener<Solution_> extends EventListener {

    /**
     * Called once every time when a better {@link PlanningSolution} is found.
     * The {@link PlanningSolution} is guaranteed to be initialized.
     * Early in the solving process it's usually called more frequently than later on.
     * <p>
     * Called from the solver thread.
     * <b>Should return fast, because it steals time from the {@link Solver}.</b>
     * <p>
     * In real-time planning
     * If {@link Solver#addProblemChange(ProblemChange)} has been called once or more,
     * all {@link ProblemChange}s in the queue will be processed and this method is called only once.
     * In that case, the former best {@link PlanningSolution} is considered stale,
     * so it doesn't matter whether the new {@link Score} is better than that or not.
     */
    void bestSolutionChanged(@NonNull BestSolutionChangedEvent<Solution_> event);

}
