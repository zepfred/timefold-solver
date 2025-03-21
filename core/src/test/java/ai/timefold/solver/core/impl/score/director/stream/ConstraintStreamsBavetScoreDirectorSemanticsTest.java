package ai.timefold.solver.core.impl.score.director.stream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfigurationSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintWeightConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;

final class ConstraintStreamsBavetScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected InnerScoreDirectorFactory<TestdataConstraintConfigurationSolution, SimpleScore>
            buildInnerScoreDirectorFactoryWithConstraintConfiguration(
                    SolutionDescriptor<TestdataConstraintConfigurationSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintWeightConstraintProvider.class);
        var scoreDirectorFactoryFactory = new ScoreDirectorFactoryFactory<TestdataConstraintConfigurationSolution, SimpleScore>(
                scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected InnerScoreDirectorFactory<TestdataSolution, SimpleScore>
            buildInnerScoreDirectorFactory(SolutionDescriptor<TestdataSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.REPRODUCIBLE, solutionDescriptor);
    }

    @Override
    protected InnerScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore>
            buildInnerScoreDirectorFactoryWithListVariableEntityPin(
                    SolutionDescriptor<TestdataPinnedListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataPinnedListConstraintProvider.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected InnerScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>
            buildInnerScoreDirectorFactoryWithListVariablePinIndex(
                    SolutionDescriptor<TestdataPinnedWithIndexListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataPinnedWithIndexListConstraintProvider.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedWithIndexListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

}
