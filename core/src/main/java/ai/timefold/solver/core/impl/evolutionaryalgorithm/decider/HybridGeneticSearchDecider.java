package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.context.EvolutionaryContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.context.HybridGeneticSearchContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.context.IndividualGenerator;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScore;
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
public final class HybridGeneticSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends EvolutionaryDecider<Solution_, Score_> {

    private final HybridGeneticSearchContext<Solution_, Score_> evolutionaryContext;
    private final HybridSearchConfiguration configuration;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;

    public HybridGeneticSearchDecider(HybridSearchConfiguration configuration,
            EvolutionaryContext<Solution_, Score_> evolutionaryContext,
            BestSolutionRecaller<Solution_> bestSolutionRecaller) {
        this.evolutionaryContext = (HybridGeneticSearchContext<Solution_, Score_>) evolutionaryContext;
        this.configuration = configuration;
        this.bestSolutionRecaller = bestSolutionRecaller;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Population<Solution_, Score_> generateInitialPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        var solverScope = phaseScope.getSolverScope();
        var bestScore = phaseScope.getBestScore() != null ? phaseScope.<Score_> getBestScore().raw() : null;
        var initialSolution = solverScope.getScoreDirector().getWorkingSolution();
        var initialScore = solverScope.<Score_> getScoreDirector().calculateScore();
        var population = evolutionaryContext.generatePopulation(phaseScope.getWorkingRandom(), configuration.populationSize(),
                configuration.generationSize(), configuration.eliteSolutionSize(), evolutionaryContext);
        for (int i = 0; i < configuration.populationSize(); i++) {
            var individual = createIndividual(solverScope, solverScope.getScoreDirector(),
                    solverScope.<Score_> getScoreDirector().cloneSolution(initialSolution), initialSolution, initialScore,
                    population);
            Score_ individualScore = individual.getScore().raw();
            population.addIndividual(individual);
            if (bestScore == null || individualScore.compareTo(bestScore) > 0) {
                bestScore = individualScore;
                // We update the current best solution after generating the individual
                var step = new EvolutionaryAlgorithmStepScope<>(phaseScope, individual.getSolution());
                step.setScore(individual.getScore());
                bestSolutionRecaller.processWorkingSolutionDuringStep(step);
            }
        }
        return population;
    }

    private Individual<Solution_, Score_> createIndividual(SolverScope<Solution_> rootSolverScope,
            InnerScoreDirector<Solution_, Score_> individualScoreDirector, Solution_ individualSolution, Solution_ bestSolution,
            InnerScore<Score_> bestScore, IndividualGenerator<Solution_, Score_> individualGenerator) {
        var individualSolverScope =
                buildIndividualScope(rootSolverScope, individualScoreDirector, individualSolution, bestSolution, bestScore);
        applyPhase(individualSolverScope, evolutionaryContext.getConstructionHeuristicPhase());
        applyPhase(individualSolverScope, evolutionaryContext.getLocalSearchPhase());
        applyPhase(individualSolverScope, evolutionaryContext.getSwapStarPhase());
        return individualGenerator.generateIndividual(individualSolverScope.getBestSolution(),
                individualSolverScope.getBestScore(), rootSolverScope.getScoreDirector());
    }

    private SolverScope<Solution_> buildIndividualScope(SolverScope<Solution_> rootSolverScope,
            InnerScoreDirector<Solution_, Score_> individualScoreDirector, Solution_ individualSolution, Solution_ bestSolution,
            InnerScore<Score_> bestScore) {
        var individualSolverScope = new SolverScope<Solution_>(rootSolverScope.getClock());
        individualSolverScope.setSolver(rootSolverScope.getSolver());
        individualSolverScope.setScoreDirector(individualScoreDirector);
        individualSolverScope.setWorkingRandom(rootSolverScope.getWorkingRandom());
        individualSolverScope.setProblemSizeStatistics(rootSolverScope.getProblemSizeStatistics());
        if (individualScoreDirector.getWorkingSolution() != individualSolution) {
            individualScoreDirector.setWorkingSolution(individualSolution);
        }
        individualSolverScope.setBestSolution(bestSolution);
        individualSolverScope.setBestScore(bestScore);
        individualSolverScope.startingNow();
        return individualSolverScope;
    }

    private void applyPhase(SolverScope<Solution_> solverScope, @Nullable Phase<Solution_> phase) {
        if (phase == null) {
            return;
        }
        phase.solvingStarted(solverScope);
        phase.solve(solverScope);
        phase.solvingEnded(solverScope);
        solverScope.getScoreDirector().triggerVariableListeners();
    }

    @Override
    public void evolvePopulation(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var firstIndividual = phaseScope.<Score_> getPopulation().selectIndividual();
        var secondIndividual = phaseScope.<Score_> getPopulation().selectIndividual();
        while (firstIndividual == secondIndividual) {
            secondIndividual = phaseScope.<Score_> getPopulation().selectIndividual();
        }
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        var offspringResult = evolutionaryContext.getCrossoverStrategy().apply(
                new CrossoverContext<>(scoreDirector, solverScope.getWorkingRandom(), firstIndividual, secondIndividual));
        var offspringScope = buildIndividualScope(solverScope, Objects.requireNonNull(scoreDirector),
                offspringResult.offspringSolution(), offspringResult.offspringSolution(), offspringResult.offspringScore());
        applyPhase(offspringScope, evolutionaryContext.getLocalSearchPhase());
        stepScope.setBestSolution(offspringScope.getBestSolution());
        stepScope.setScore(offspringScope.getBestScore());
        // Update the population
        var population = phaseScope.<Score_> getPopulation();
        var offspringIndividual = population.generateIndividual(offspringScope.getBestSolution(), offspringScope.getBestScore(),
                scoreDirector);
        population.addIndividual(offspringIndividual);
    }
}
