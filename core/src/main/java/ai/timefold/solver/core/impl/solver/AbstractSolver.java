package ai.timefold.solver.core.impl.solver;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleSupport;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common code between {@link DefaultSolver} and child solvers.
 * <p>
 * Do not create a new child {@link Solver} to implement a new heuristic or metaheuristic,
 * just use a new {@link Phase} for that.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Solver
 * @see DefaultSolver
 */
public abstract class AbstractSolver<Solution_> implements Solver<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final SolverEventSupport<Solution_> solverEventSupport = new SolverEventSupport<>(this);
    private final PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    protected final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    // Note that the DefaultSolver.basicPlumbingTermination is a component of this termination.
    // Called "globalTermination" to clearly distinguish from "phaseTermination" inside AbstractPhase.
    protected final UniversalTermination<Solution_> globalTermination;
    protected final List<Phase<Solution_>> phaseList;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    protected AbstractSolver(BestSolutionRecaller<Solution_> bestSolutionRecaller,
            UniversalTermination<Solution_> globalTermination, List<Phase<Solution_>> phaseList) {
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.globalTermination = globalTermination;
        bestSolutionRecaller.setSolverEventSupport(solverEventSupport);
        this.phaseList = List.copyOf(phaseList);
    }

    public void solvingStarted(SolverScope<Solution_> solverScope) {
        solverScope.setWorkingSolutionFromBestSolution();
        bestSolutionRecaller.solvingStarted(solverScope);
        globalTermination.solvingStarted(solverScope);
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
        // Using value range manager from the same score director as the working solution; this is a correct use.
        var problemSizeStatistics = solverScope.getScoreDirector()
                .getValueRangeManager()
                .getProblemSizeStatistics();
        solverScope.setProblemSizeStatistics(problemSizeStatistics);
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingStarted(solverScope);
        }
    }

    protected void runPhases(SolverScope<Solution_> solverScope) {
        if (!solverScope.getSolutionDescriptor().hasMovableEntities(solverScope.getScoreDirector())) {
            logger.info("Skipped all phases ({}): out of {} planning entities, none are movable (non-pinned).",
                    phaseList.size(), solverScope.getWorkingEntityCount());
            return;
        }
        Iterator<Phase<Solution_>> it = phaseList.iterator();
        while (!globalTermination.isSolverTerminated(solverScope) && it.hasNext()) {
            Phase<Solution_> phase = it.next();
            phase.solve(solverScope);
            // If there is a next phase, it starts from the best solution, which might differ from the working solution.
            // If there isn't, no need to planning clone the best solution to the working solution.
            if (it.hasNext()) {
                solverScope.setWorkingSolutionFromBestSolution();
            }
        }
    }

    public void solvingEnded(SolverScope<Solution_> solverScope) {
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingEnded(solverScope);
        }
        bestSolutionRecaller.solvingEnded(solverScope);
        globalTermination.solvingEnded(solverScope);
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        phaseLifecycleSupport.fireSolvingError(solverScope, exception);
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingError(solverScope, exception);
        }
    }

    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        bestSolutionRecaller.phaseStarted(phaseScope);
        phaseLifecycleSupport.firePhaseStarted(phaseScope);
        globalTermination.phaseStarted(phaseScope);
        // Do not propagate to phases; the active phase does that for itself and they should not propagate further.
    }

    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        bestSolutionRecaller.phaseEnded(phaseScope);
        phaseLifecycleSupport.firePhaseEnded(phaseScope);
        globalTermination.phaseEnded(phaseScope);
        // Do not propagate to phases; the active phase does that for itself and they should not propagate further.
    }

    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        bestSolutionRecaller.stepStarted(stepScope);
        phaseLifecycleSupport.fireStepStarted(stepScope);
        globalTermination.stepStarted(stepScope);
        // Do not propagate to phases; the active phase does that for itself and they should not propagate further.
    }

    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        bestSolutionRecaller.stepEnded(stepScope);
        phaseLifecycleSupport.fireStepEnded(stepScope);
        globalTermination.stepEnded(stepScope);
        // Do not propagate to phases; the active phase does that for itself and they should not propagate further.
    }

    @Override
    public void addEventListener(SolverEventListener<Solution_> eventListener) {
        solverEventSupport.addEventListener(eventListener);
    }

    @Override
    public void removeEventListener(SolverEventListener<Solution_> eventListener) {
        solverEventSupport.removeEventListener(eventListener);
    }

    /**
     * Add a {@link PhaseLifecycleListener} that is notified
     * of {@link PhaseLifecycleListener#solvingStarted(SolverScope) solving} events
     * and also of the {@link PhaseLifecycleListener#phaseStarted(AbstractPhaseScope) phase}
     * and the {@link PhaseLifecycleListener#stepStarted(AbstractStepScope) step} starting/ending events of all phases.
     * <p>
     * To get notified for only 1 phase, use {@link Phase#addPhaseLifecycleListener(PhaseLifecycleListener)} instead.
     *
     * @param phaseLifecycleListener never null
     */
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.addEventListener(phaseLifecycleListener);
    }

    /**
     * @param phaseLifecycleListener never null
     * @see #addPhaseLifecycleListener(PhaseLifecycleListener)
     */
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.removeEventListener(phaseLifecycleListener);
    }

    public boolean isTerminationSameAsSolverTermination(PhaseTermination<Solution_> phaseTermination) {
        return phaseTermination == globalTermination;
    }

    public BestSolutionRecaller<Solution_> getBestSolutionRecaller() {
        return bestSolutionRecaller;
    }

    public List<Phase<Solution_>> getPhaseList() {
        return phaseList;
    }

}
