package ai.timefold.solver.core.impl.score.stream.common;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

/**
 * FP streams implementation of {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see ScoreDirectorFactory
 */
public abstract class AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, Factory_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_> {

    protected AbstractConstraintStreamScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        super(solutionDescriptor);
    }

    /**
     * Creates a new score director, inserts facts and calculates score.
     *
     * @param facts never null
     * @return never null
     */
    public abstract AbstractScoreInliner<Score_> fireAndForget(Object... facts);

    public abstract ConstraintMetaModel getConstraintMetaModel();

    @Override
    public boolean supportsConstraintMatching() {
        return true;
    }

}
