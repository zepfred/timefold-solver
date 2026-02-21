package ai.timefold.solver.core.impl.evolutionaryalgorithm.strategy;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.DefaultPopulation;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.ListVariableIndividual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the Hybrid Genetic Search algorithm based on the work:
 * <p>
 * Hybrid Genetic Search for th CVRP: Open-Source Implementation and SWAP* Neighborhood by Thibaut Vidal
 *
 * @param <Solution_> the solution type
 * @param <Score_>> the score type
 */
@NullMarked
public final class HybridGeneticSearch<Solution_, Score_ extends Score<Score_>>
        implements EvolutionaryStrategy<Solution_, Score_> {

    private final HybridSearchConfiguration configuration;
    private final Phase<Solution_> constructionHeuristicPhase;
    private final Phase<Solution_> localSearchPhase;
    // Implementation of the SWAP* method. It can be null if Nearby is not enabled
    private final @Nullable Phase<Solution_> swapStarPhase;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;

    public HybridGeneticSearch(HybridSearchConfiguration configuration, Phase<Solution_> constructionHeuristicPhase,
            Phase<Solution_> localSearchPhase, Phase<Solution_> swapStarPhase,
            BestSolutionRecaller<Solution_> bestSolutionRecaller) {
        this.configuration = configuration;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.localSearchPhase = localSearchPhase;
        this.swapStarPhase = swapStarPhase;
        this.bestSolutionRecaller = bestSolutionRecaller;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Population<Solution_, Score_> generateInitialPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        var bestScore = phaseScope.getBestScore() != null ? phaseScope.getBestScore().raw() : null;
        var initialSolution = solverScope.getScoreDirector().getWorkingSolution();
        var population = new DefaultPopulation<Solution_, Score_>(phaseScope.getWorkingRandom(), configuration.populationSize(),
                configuration.generationSize(), configuration.eliteSolutionSize());
        for (int i = 0; i < configuration.populationSize(); i++) {
            solverScope.getScoreDirector().setWorkingSolution(solverScope.getScoreDirector().cloneSolution(initialSolution));
            var individual = generateIndividual(scoreDirector, solverScope);
            Score individualScore = individual.getScore().raw();
            population.addIndividual(individual);
            if (bestScore == null || individualScore.compareTo(bestScore) > 0) {
                bestScore = individualScore;
            }
        }
        // We update the current best solution after generating the initial population
        var step = new EvolutionaryAlgorithmStepScope<>(phaseScope, population.getBestIndividual());
        step.setScore(population.getBestIndividual().getScore());
        bestSolutionRecaller.processWorkingSolutionDuringStep(step);
        return population;
    }

    private Individual<Solution_, Score_> generateIndividual(InnerScoreDirector<Solution_, Score_> innerScoreDirector,
            SolverScope<Solution_> solverScope) {
        var individualSolverScope = new SolverScope<Solution_>(solverScope.getClock());
        individualSolverScope.setSolver(solverScope.getSolver());
        individualSolverScope.setScoreDirector(innerScoreDirector);
        individualSolverScope.setWorkingRandom(solverScope.getWorkingRandom());
        individualSolverScope.setProblemSizeStatistics(solverScope.getProblemSizeStatistics());
        var bestScore = solverScope.getBestScore();
        individualSolverScope.setBestScore(bestScore);
        individualSolverScope.startingNow();
        applyPhase(individualSolverScope, constructionHeuristicPhase);
        applyPhase(individualSolverScope, localSearchPhase);
        if (swapStarPhase != null) {
            applyPhase(individualSolverScope, swapStarPhase);
        }
        return new ListVariableIndividual<>(individualSolverScope.getWorkingSolution(),
                individualSolverScope.getBestScore(), innerScoreDirector);
    }

    private void applyPhase(SolverScope<Solution_> solverScope, Phase<Solution_> phase) {
        phase.solvingStarted(solverScope);
        phase.solve(solverScope);
        phase.solvingEnded(solverScope);
        solverScope.getScoreDirector().triggerVariableListeners();
    }

    @Override
    public void execute(EvolutionaryAlgorithmPhaseScope<Solution_> stepScope) {

    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> abstractPhaseScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> abstractPhaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> abstractStepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> abstractStepScope) {
        // Do nothing
    }

}
