package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultPopulation<Solution_, Score_ extends Score<Score_>> implements Population<Solution_, Score_> {

    private final Random workingRandom;
    private final int generationSize;
    private final int eliteSolutionSize;
    private final int maxSize;
    private final List<InternalIndividual<Solution_, Score_>> feasiableIndividualList;
    private final List<InternalIndividual<Solution_, Score_>> infeasiableIndividualList;
    private final PopulationDiffMap<Solution_, Score_> diffMap;
    private @Nullable Individual<Solution_, Score_> bestIndividual = null;

    public DefaultPopulation(Random workingRandom, int populationSize, int generationSize, int eliteSolutionSize) {
        this.workingRandom = workingRandom;
        this.generationSize = generationSize;
        this.eliteSolutionSize = eliteSolutionSize;
        this.maxSize = populationSize + generationSize;
        this.feasiableIndividualList = new ArrayList<>(maxSize);
        this.infeasiableIndividualList = new ArrayList<>(maxSize);
        // The map can store at most maxSize elements from both lists
        this.diffMap = new PopulationDiffMap<>(maxSize * 2);
    }

    @Override
    public List<Individual<Solution_, Score_>> getAllIndividuals() {
        var populationList =
                new ArrayList<Individual<Solution_, Score_>>(feasiableIndividualList.size() + infeasiableIndividualList.size());
        populationList.addAll(feasiableIndividualList);
        populationList.addAll(infeasiableIndividualList);
        return populationList;
    }

    @Override
    public boolean addIndividual(Individual<Solution_, Score_> individual) {
        var individualList = individual.isFeasiable() ? feasiableIndividualList : infeasiableIndividualList;
        var pos = 0;
        if (!individualList.isEmpty()) {
            // We use the insertion sort method to implement the proposed survival strategy
            pos = Collections.binarySearch(individualList, individual);
        }
        var internalIndividual = new InternalIndividual<>(individual);
        individualList.add(pos, internalIndividual);
        // Calculate the difference between the new individual and each individual in the related list
        computeDiff(internalIndividual, individualList);
        // Analyze andaApply the survival selection strategy
        analyzeSubpopulationList(individualList);
        if (bestIndividual == null || internalIndividual.compareTo(bestIndividual) > 0) {
            bestIndividual = individual;
        }
        return bestIndividual == individual;
    }

    /**
     * Calculates the difference between the given individual and all other individuals from the given list.
     *
     * @param individual the first individual
     * @param individualList the list to be evaluated
     */
    private void computeDiff(Individual<Solution_, Score_> individual,
            List<InternalIndividual<Solution_, Score_>> individualList) {
        for (var otherIndividual : individualList) {
            if (individual == otherIndividual) {
                continue;
            }
            var diff = individual.diff(otherIndividual.innerIndividual);
            diffMap.addIndividualDiff(individual, otherIndividual, diff);
        }
    }

    /**
     * The survival method removes the worst individual from the population until the population size is restored.
     * This removal is based on the fitness of each individual,
     * which is calculated according to their contribution to the diversity of the population.
     * 
     * @param subpopulationList the population to be analyzed
     */
    private void analyzeSubpopulationList(List<InternalIndividual<Solution_, Score_>> subpopulationList) {
        if (subpopulationList.size() < maxSize) {
            return;
        }
        // The number of individuals exceeds the maximum size,
        // which is the population size + the generation size
        var sizeToRemove = subpopulationList.size() - generationSize;
        for (int i = 0; i < sizeToRemove; i++) {
            // Starting by updating the fitness of the individuals
            updateSubpopulationFitness(subpopulationList);
            // Find and remove the worst individual
            var worstIndividualIndex = 0;
            InternalIndividual<Solution_, Score_> worstIndividual = null;
            // It means all other individuals from the subpopulation have the same solution
            var hasWorstIndividualSameSolution = false;
            for (var j = 0; j < subpopulationList.size(); j++) {
                var otherIndividual = subpopulationList.get(j);
                // The average value will be the sum of all diffs.
                // If all other solutions have diff equal to zero, 
                // it means all individuals have the same solution
                var hasSameSolution = averageDiff(otherIndividual, 1) == 0d;
                // 1 - We select the individual if it has no diff and the current worst element has any diff
                // 2 - We also select the individual if the fitness is higher, which means it is worse
                if ((worstIndividual == null) || (hasSameSolution && !hasWorstIndividualSameSolution)
                        || (hasWorstIndividualSameSolution == hasSameSolution
                                && otherIndividual.getFitness() > worstIndividual.getFitness())) {
                    worstIndividualIndex = j;
                    worstIndividual = otherIndividual;
                    hasWorstIndividualSameSolution = hasSameSolution;
                }
            }
            subpopulationList.remove(worstIndividualIndex);
            diffMap.removeIndividualDiff(worstIndividual);
        }
    }

    /**
     * The ranking method follows the logic proposed in the HGS article,
     * using both solution quality and diversity contribution to estimate fitness.
     */
    private void updateSubpopulationFitness(List<InternalIndividual<Solution_, Score_>> subpopulationList) {
        var subpopulationSize = subpopulationList.size();
        var rankingFitnessList = new ArrayList<Pair<Double, Integer>>(subpopulationSize);
        for (var i = 0; i < subpopulationSize; i++) {
            rankingFitnessList.add(new Pair<>(-averageDiff(subpopulationList.get(i), eliteSolutionSize), i));
        }
        // Rank according to the average diff and contribution to the diversity
        rankingFitnessList.sort(Comparator.comparingDouble(Pair::key));
        if (rankingFitnessList.size() > 1) {
            for (var i = 0; i < rankingFitnessList.size(); i++) {
                int idx = rankingFitnessList.get(i).value();
                // The list is already sorted by the score
                var scoreRank = (double) i / (double) (subpopulationSize - 1);
                var diffRank = (double) idx / (double) (subpopulationSize - 1);
                // The population must have at least eliteSolutionSize individuals
                if (subpopulationSize < eliteSolutionSize) {
                    subpopulationList.get(idx).setFitness(diffRank);
                } else {
                    var fitness =
                            diffRank + (1.0 - (double) eliteSolutionSize / (double) subpopulationSize) * scoreRank;
                    subpopulationList.get(idx).setFitness(fitness);
                }
            }
        }
    }

    /**
     * Calculates the average diff to individual according to the given limit.
     *
     * @param individual the individual to be evaluated
     * @param size a value used to control the average calculation
     *
     * @return a double number where a higher value reflects a greater average difference.
     */
    private double averageDiff(Individual<Solution_, Score_> individual, int size) {
        var result = 0.d;
        var individualDiffMap = diffMap.getIndividualDiffMap(individual);
        var maxLimit = Math.max(size, individualDiffMap.size());
        for (var entry : individualDiffMap.entrySet()) {
            result += entry.getValue();
        }
        return result / (double) maxLimit;
    }

    @Override
    public Individual<Solution_, Score_> selectIndividual() {
        var size = feasiableIndividualList.size() + infeasiableIndividualList.size();
        var firstIdx = workingRandom.nextInt(0, size);
        var secondIdx = workingRandom.nextInt(0, size);
        var firstIndividual = (firstIdx >= feasiableIndividualList.size())
                ? infeasiableIndividualList.get(firstIdx - feasiableIndividualList.size())
                : feasiableIndividualList.get(firstIdx);
        var secondIndividual = (secondIdx >= feasiableIndividualList.size())
                ? infeasiableIndividualList.get(secondIdx - feasiableIndividualList.size())
                : feasiableIndividualList.get(secondIdx);
        return firstIndividual.getFitness() < secondIndividual.getFitness() ? firstIndividual : secondIndividual;
    }

    @Override
    public @Nullable Individual<Solution_, Score_> getBestIndividual() {
        return bestIndividual;
    }

    private static class InternalIndividual<Solution_, Score_ extends Score<Score_>> implements Individual<Solution_, Score_> {

        private final Individual<Solution_, Score_> innerIndividual;
        private double fitness;

        private InternalIndividual(Individual<Solution_, Score_> innerIndividual) {
            this.innerIndividual = innerIndividual;
        }

        @Override
        public Solution_ getSolution() {
            return innerIndividual.getSolution();
        }

        @Override
        public double diff(Individual<Solution_, Score_> otherIndividual) {
            return innerIndividual.diff(otherIndividual);
        }

        @Override
        public boolean isFeasiable() {
            return innerIndividual.isFeasiable();
        }

        @Override
        public InnerScore<Score_> getScore() {
            return innerIndividual.getScore();
        }

        @Override
        public int compareTo(Individual<Solution_, Score_> o) {
            return innerIndividual.compareTo(o);
        }

        public double getFitness() {
            return fitness;
        }

        public void setFitness(double fitness) {
            this.fitness = fitness;
        }
    }
}
