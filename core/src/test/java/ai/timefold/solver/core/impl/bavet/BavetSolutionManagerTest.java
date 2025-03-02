package ai.timefold.solver.core.impl.bavet;

import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.impl.score.stream.common.AbstractSolutionManagerTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListConstraintProvider;

final class BavetSolutionManagerTest extends AbstractSolutionManagerTest {

    @Override
    protected ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class);
    }

    @Override
    protected ScoreDirectorFactoryConfig buildUnassignedWithPinningScoreDirectorFactoryConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataPinnedUnassignedValuesListConstraintProvider.class);
    }

}
