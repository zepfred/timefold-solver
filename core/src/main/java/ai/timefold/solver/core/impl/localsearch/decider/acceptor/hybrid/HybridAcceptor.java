package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hybrid;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.greatdeluge.GreatDelugeAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.impl.util.Pair;

public class HybridAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private LocalSearchDecider<Solution_> decider;
    private final AbstractAcceptor<Solution_>[] innerAcceptors = new AbstractAcceptor[2];
    private final InnerScoreDirector<Solution_, ?>[] innerScoreDirector = new InnerScoreDirector[innerAcceptors.length];
    private final List<Solution_> innerAcceptorSolution;
    private Pair<Integer, InnerScore<?>> currentBestScore;
    private int bestSolutionStepIndex;

    public HybridAcceptor(int lateAcceptanceSize, int diversifiedLateAcceptanceSize, double waterLevelIncrementRatio) {
        var lateAcceptanceAcceptor = new LateAcceptanceAcceptor<Solution_>();
        lateAcceptanceAcceptor.setLateAcceptanceSize(lateAcceptanceSize);
        innerAcceptors[0] = lateAcceptanceAcceptor;
        //        lateAcceptanceAcceptor = new LateAcceptanceAcceptor<>();
        //        lateAcceptanceAcceptor.setLateAcceptanceSize(lateAcceptanceSize * 3);
        //        innerAcceptors[1] = lateAcceptanceAcceptor;
        //        var diversifiedLateAcceptanceAcceptor = new DiversifiedLateAcceptanceAcceptor<Solution_>();
        //        diversifiedLateAcceptanceAcceptor.setLateAcceptanceSize(diversifiedLateAcceptanceSize);
        //        innerAcceptors[1] = diversifiedLateAcceptanceAcceptor;
        var greatDelugeAcceptor = new GreatDelugeAcceptor<Solution_>();
        greatDelugeAcceptor.setWaterLevelIncrementRatio(waterLevelIncrementRatio);
        innerAcceptors[1] = greatDelugeAcceptor;
        //        var simulatedAnnealingAcceptor = new SimulatedAnnealingAcceptor<Solution_>();
        //        innerAcceptors[1] = simulatedAnnealingAcceptor;
        innerAcceptorSolution = new ArrayList<>(innerAcceptors.length);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        for (var acceptor : innerAcceptors) {
            acceptor.solvingStarted(solverScope);
        }
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        var solverScope = phaseScope.getSolverScope();
        innerScoreDirector[0] = solverScope.getScoreDirector();
        innerAcceptors[0].phaseStarted(phaseScope);
        innerAcceptorSolution.add(solverScope.getWorkingSolution());
        //        var simulatedAnnealingAcceptor = (SimulatedAnnealingAcceptor<Solution_>) innerAcceptors[1];
        //        simulatedAnnealingAcceptor.setStartingTemperature(phaseScope.getBestScore().raw().abs());
        for (var i = 1; i < innerAcceptors.length; i++) {
            innerAcceptorSolution.add(solverScope.getScoreDirector().cloneSolution(solverScope.getWorkingSolution()));
            innerScoreDirector[i] = solverScope.getScoreDirector().createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD);
            innerScoreDirector[i].setWorkingSolution(innerAcceptorSolution.get(i));
            innerScoreDirector[i].calculateScore();
            innerAcceptors[i].phaseStarted(phaseScope);
        }
        currentBestScore = new Pair<>(0, phaseScope.getBestScore());
        decider = phaseScope.getDecider();
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        for (var acceptor : innerAcceptors) {
            acceptor.stepStarted(stepScope);
        }
        bestSolutionStepIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        for (var acceptor : innerAcceptors) {
            acceptor.stepEnded(stepScope);
        }
        if (currentBestScore.key() != 0) {
            logger.info("New best solution found {}, strategy {}", currentBestScore.value().raw(), currentBestScore.key());
            var bestSolution = innerAcceptorSolution.get(currentBestScore.key());
            decider.setWorkingSolution(stepScope, bestSolution, currentBestScore.value());
            innerAcceptorSolution.set(0, stepScope.getWorkingSolution());
            currentBestScore = new Pair<>(0, currentBestScore.value());
            innerAcceptors[0].phaseStarted(stepScope.getPhaseScope());
        } else if (bestSolutionStepIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            logger.info("New best solution found {}, strategy {}", stepScope.getPhaseScope().getBestScore(), 0);
            currentBestScore = new Pair<>(0, stepScope.getPhaseScope().getBestScore());
            innerAcceptorSolution.set(1,
                    innerScoreDirector[1].cloneSolution(innerScoreDirector[1].cloneSolution(innerAcceptorSolution.get(0))));
            innerScoreDirector[1].setWorkingSolution(innerAcceptorSolution.get(1));
            //            var simulatedAnnealingAcceptor = (SimulatedAnnealingAcceptor<Solution_>) innerAcceptors[1];
            //            simulatedAnnealingAcceptor.setStartingTemperature(stepScope.getPhaseScope().getBestScore().raw().abs());
            //            innerAcceptors[1].phaseStarted(stepScope.getPhaseScope());
        }
        //        if (currentBestScore.key() != 0) {
        //            logger.info("New best solution found {}, strategy {}", currentBestScore.value().raw(), currentBestScore.key());
        //            var bestSolution = innerAcceptorSolution.get(currentBestScore.key());
        //            decider.setWorkingSolution(stepScope, bestSolution, currentBestScore.value());
        //            innerAcceptorSolution.set(0, stepScope.getWorkingSolution());
        //            for (var i = 1; i < innerAcceptors.length; i++) {
        //                if (i == currentBestScore.key()) {
        //                    continue;
        //                }
        //                innerAcceptorSolution.set(i, innerScoreDirector[i].cloneSolution(bestSolution));
        //                innerScoreDirector[i].setWorkingSolution(innerAcceptorSolution.get(i));
        //            }
        //            currentBestScore = new Pair<>(0, currentBestScore.value());
        //        } else if (bestSolutionStepIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
        //            logger.info("New best solution found {}, strategy {}", stepScope.getPhaseScope().getBestScore(), 0);
        //            currentBestScore = new Pair<>(0, stepScope.getPhaseScope().getBestScore());
        //            for (var i = 1; i < innerAcceptors.length; i++) {
        //                innerAcceptorSolution.set(i,
        //                        innerScoreDirector[i].cloneSolution(innerScoreDirector[i].cloneSolution(innerAcceptorSolution.get(0))));
        //                innerScoreDirector[i].setWorkingSolution(innerAcceptorSolution.get(i));
        //            }
        //        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        for (var acceptor : innerAcceptors) {
            acceptor.phaseEnded(phaseScope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        for (var acceptor : innerAcceptors) {
            acceptor.solvingEnded(solverScope);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var firstAccepted = false;
        for (var i = 0; i < innerAcceptors.length; i++) {
            var acceptorMoveScope = moveScope;
            var acceptor = innerAcceptors[i];
            if (i != 0) {
                var moveDirector = innerScoreDirector[i].getMoveDirector();
                var moveRebased = moveScope.getMove().rebase(moveDirector);
                if (!LegacyMoveAdapter.isDoable(moveDirector, moveRebased)) {
                    continue;
                }
                try {
                    var score = moveDirector.executeTemporary(moveRebased);
                    var innerMoveScope =
                            new LocalSearchMoveScope<>(moveScope.getStepScope(), moveScope.getMoveIndex(), moveRebased);
                    innerMoveScope.setScore(score);
                    acceptorMoveScope = innerMoveScope;
                } catch (Exception e) {
                    continue;
                }
            }
            if (acceptor.isAccepted(acceptorMoveScope)) {
                if (i == 0) {
                    firstAccepted = true;
                } else {
                    var moveDirector = innerScoreDirector[i].getMoveDirector();
                    moveDirector.execute(acceptorMoveScope.getMove());
                    //                    innerScoreDirector[i].calculateScore();
                }
                if (acceptorMoveScope.getScore().compareTo((InnerScore) currentBestScore.value()) > 0) {
                    currentBestScore = new Pair<>(i, acceptorMoveScope.getScore());
                    firstAccepted = true;
                }
            }
        }
        return firstAccepted;
    }
}
