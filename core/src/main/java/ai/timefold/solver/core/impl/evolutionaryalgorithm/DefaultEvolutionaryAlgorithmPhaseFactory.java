package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryAlgorithmPhaseConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public class DefaultEvolutionaryAlgorithmPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, EvolutionaryAlgorithmPhaseConfig> {

    public DefaultEvolutionaryAlgorithmPhaseFactory(EvolutionaryAlgorithmPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public EvolutionaryAlgorithmPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        solverConfigPolicy.ensurePreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM);
        var phaseTermination = this.buildPhaseTermination(solverConfigPolicy, solverTermination);
        return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.EVOLUTIONARY_ALGORITHM)
                .buildEvolutionaryAlgorithmPhase(phaseIndex, phaseConfig, solverConfigPolicy, bestSolutionRecaller,
                        solverTermination, phaseTermination);
    }

}
