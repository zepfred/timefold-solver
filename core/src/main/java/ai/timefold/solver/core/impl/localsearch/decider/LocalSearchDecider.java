package ai.timefold.solver.core.impl.localsearch.decider;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class LocalSearchDecider<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final String logIndentation;
    protected final Termination<Solution_> termination;
    protected final MoveSelector<Solution_> moveSelector;
    protected final Acceptor<Solution_> acceptor;
    protected final LocalSearchForager<Solution_> forager;
    protected final MoveSelector<Solution_> refinementMoveSelector;
    protected final Acceptor<Solution_> refinementAcceptor;
    protected final LocalSearchForager<Solution_> refinementForager;

    protected boolean assertMoveScoreFromScratch = false;
    protected boolean assertExpectedUndoMoveScore = false;

    public LocalSearchDecider(String logIndentation, Termination<Solution_> termination, MoveSelector<Solution_> moveSelector,
            MoveSelector<Solution_> refinementMoveSelector, Acceptor<Solution_> acceptor,
            Acceptor<Solution_> refinementAcceptor, LocalSearchForager<Solution_> forager,
            LocalSearchForager<Solution_> refinementForager) {
        this.logIndentation = logIndentation;
        this.termination = termination;
        this.moveSelector = moveSelector;
        this.refinementMoveSelector = refinementMoveSelector;
        this.acceptor = acceptor;
        this.refinementAcceptor = refinementAcceptor;
        this.forager = forager;
        this.refinementForager = refinementForager;
    }

    public Termination<Solution_> getTermination() {
        return termination;
    }

    public MoveSelector<Solution_> getMoveSelector() {
        return moveSelector;
    }

    public Acceptor<Solution_> getAcceptor() {
        return acceptor;
    }

    public LocalSearchForager<Solution_> getForager() {
        return forager;
    }

    public void setAssertMoveScoreFromScratch(boolean assertMoveScoreFromScratch) {
        this.assertMoveScoreFromScratch = assertMoveScoreFromScratch;
    }

    public void setAssertExpectedUndoMoveScore(boolean assertExpectedUndoMoveScore) {
        this.assertExpectedUndoMoveScore = assertExpectedUndoMoveScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveSelector.solvingStarted(solverScope);
        acceptor.solvingStarted(solverScope);
        forager.solvingStarted(solverScope);
        if (isRefinementEnabled()) {
            refinementMoveSelector.solvingStarted(solverScope);
            refinementAcceptor.solvingStarted(solverScope);
            refinementForager.solvingStarted(solverScope);
        }
    }

    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveSelector.phaseStarted(phaseScope);
        acceptor.phaseStarted(phaseScope);
        forager.phaseStarted(phaseScope);
        if (isRefinementEnabled()) {
            refinementMoveSelector.phaseStarted(phaseScope);
            refinementAcceptor.phaseStarted(phaseScope);
            refinementForager.phaseStarted(phaseScope);
        }
    }

    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        moveSelector.stepStarted(stepScope);
        acceptor.stepStarted(stepScope);
        forager.stepStarted(stepScope);
    }

    public void refinementStepStarted(LocalSearchStepScope<Solution_> stepScope) {
        if (isRefinementEnabled()) {
            refinementMoveSelector.stepStarted(stepScope);
            refinementAcceptor.stepStarted(stepScope);
            refinementForager.stepStarted(stepScope);
        }
    }

    public void decideNextStep(LocalSearchStepScope<Solution_> stepScope) {
        executeStepSearch(stepScope, moveSelector, acceptor, forager, false);
    }

    public void refine(LocalSearchStepScope<Solution_> stepScope) {
        if (!isRefinementEnabled() || isRefinementTerminated() || termination.isPhaseTerminated(stepScope.getPhaseScope())) {
            return;
        }
        executeStepSearch(stepScope, refinementMoveSelector, refinementAcceptor, refinementForager, true);
    }

    private void executeStepSearch(LocalSearchStepScope<Solution_> stepScope, MoveSelector<Solution_> stepMoveSelector,
            Acceptor<Solution_> stepAcceptor, LocalSearchForager<Solution_> stepForager, boolean isRefinement) {
        var scoreDirector = stepScope.getScoreDirector();
        scoreDirector.setAllChangesWillBeUndoneBeforeStepEnds(true);
        int moveIndex = 0;
        for (var move : stepMoveSelector) {
            var adaptedMove = new LegacyMoveAdapter<>(move);
            LocalSearchMoveScope<Solution_> moveScope = new LocalSearchMoveScope<>(stepScope, moveIndex, adaptedMove);
            moveIndex++;
            doMove(moveScope, stepAcceptor, stepForager);
            if (stepForager.isQuitEarly()) {
                break;
            }
            stepScope.getPhaseScope().getSolverScope().checkYielding();
            if (termination.isPhaseTerminated(stepScope.getPhaseScope()) || (isRefinement && isRefinementTerminated())) {
                break;
            }
        }
        scoreDirector.setAllChangesWillBeUndoneBeforeStepEnds(false);
        pickMove(stepScope, stepForager);
    }

    private boolean isRefinementEnabled() {
        return refinementMoveSelector != null && refinementAcceptor != null && refinementForager != null;
    }

    public boolean isRefinementTerminated() {
        return !isRefinementEnabled() || !refinementMoveSelector.iterator().hasNext();
    }

    @SuppressWarnings("unchecked")
    protected <Score_ extends Score<Score_>> void doMove(LocalSearchMoveScope<Solution_> moveScope,
            Acceptor<Solution_> moveAcceptor, LocalSearchForager<Solution_> moveForager) {
        InnerScoreDirector<Solution_, Score_> scoreDirector = moveScope.getScoreDirector();
        var moveDirector = moveScope.getStepScope().getMoveDirector();
        var move = moveScope.getMove();
        if (!LegacyMoveAdapter.isDoable(moveDirector, move)) {
            throw new IllegalStateException("Impossible state: Local search move selector (" + moveSelector
                    + ") provided a non-doable move (" + moveScope.getMove() + ").");
        }
        scoreDirector.doAndProcessMove(moveScope.getMove(), assertMoveScoreFromScratch, score -> {
            moveScope.setScore(score);
            boolean accepted = moveAcceptor.isAccepted(moveScope);
            moveScope.setAccepted(accepted);
            moveForager.addMove(moveScope);
        });
        if (assertExpectedUndoMoveScore) {
            scoreDirector.assertExpectedUndoMoveScore(moveScope.getMove(),
                    (Score_) moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore(),
                    SolverLifecyclePoint.of(moveScope));
        }
        logger.trace("{}        Move index ({}), score ({}), accepted ({}), move ({}).", logIndentation,
                moveScope.getMoveIndex(), moveScope.getScore(), moveScope.getAccepted(), moveScope.getMove());
    }

    protected void pickMove(LocalSearchStepScope<Solution_> stepScope, LocalSearchForager<Solution_> moveForager) {
        var pickedMoveScope = moveForager.pickMove(stepScope);
        if (pickedMoveScope != null) {
            var step = pickedMoveScope.getMove();
            stepScope.setStep(step);
            if (logger.isDebugEnabled()) {
                stepScope.setStepString(step.toString());
            }
            stepScope.setScore(pickedMoveScope.getScore());
        }
    }

    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        moveSelector.stepEnded(stepScope);
        acceptor.stepEnded(stepScope);
        forager.stepEnded(stepScope);
    }

    public void refinementStepEnded(LocalSearchStepScope<Solution_> stepScope) {
        if (isRefinementEnabled()) {
            refinementMoveSelector.stepEnded(stepScope);
            refinementAcceptor.stepEnded(stepScope);
            refinementForager.stepEnded(stepScope);
        }
    }

    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveSelector.phaseEnded(phaseScope);
        acceptor.phaseEnded(phaseScope);
        forager.phaseEnded(phaseScope);
        if (isRefinementEnabled()) {
            refinementMoveSelector.phaseEnded(phaseScope);
            refinementAcceptor.phaseEnded(phaseScope);
            refinementForager.phaseEnded(phaseScope);
        }
    }

    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveSelector.solvingEnded(solverScope);
        acceptor.solvingEnded(solverScope);
        forager.solvingEnded(solverScope);
        if (isRefinementEnabled()) {
            refinementMoveSelector.solvingEnded(solverScope);
            refinementAcceptor.solvingEnded(solverScope);
            refinementForager.solvingEnded(solverScope);
        }
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // Overridable by a subclass.
    }
}
