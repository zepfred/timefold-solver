package ai.timefold.solver.core.api.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class SolverConfigOverride<Solution_> {

    private TerminationConfig terminationConfig = null;
    private List<Function<ConstraintFactory, Constraint>> constraints = new ArrayList<>();

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    @Nullable
    public Function<ConstraintFactory, Constraint>[] getConstraints() {
        if (constraints.isEmpty()) {
            return null;
        }
        return constraints.toArray(Function[]::new);
    }

    /**
     * Sets the solver {@link TerminationConfig}.
     *
     * @param terminationConfig allows overriding the default termination config of {@link Solver}
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        this.terminationConfig =
                Objects.requireNonNull(terminationConfig, "Invalid terminationConfig (null) given to SolverConfigOverride.");
        return this;
    }

    /**
     * Add new constraint or update a default constraint defined by the {@link ConstraintProvider}.
     *
     * @param constraint allows overriding the default constraints of {@link ConstraintProvider}
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_>
            withConstraint(@NonNull Function<ConstraintFactory, Constraint> constraint) {
        constraints.add(Objects.requireNonNull(constraint, "Invalid constraint (null) given to SolverConfigOverride."));
        return this;
    }
}
