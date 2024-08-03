package ai.timefold.solver.core.impl.domain.variable.cascade;

public record CascadingUpdateVariableInformation<T, V>(T source, V previous, V next) {
}
