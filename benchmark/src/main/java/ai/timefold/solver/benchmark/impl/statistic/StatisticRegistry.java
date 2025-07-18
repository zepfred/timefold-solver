package ai.timefold.solver.benchmark.impl.statistic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.monitoring.SolverMetricUtil;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class StatisticRegistry<Solution_> extends SimpleMeterRegistry
        implements PhaseLifecycleListener<Solution_> {

    private static final String CONSTRAINT_PACKAGE_TAG = "constraint.package";
    private static final String CONSTRAINT_NAME_TAG = "constraint.name";

    List<Consumer<SolverScope<Solution_>>> solverMeterListenerList = new ArrayList<>();
    List<BiConsumer<Long, AbstractStepScope<Solution_>>> stepMeterListenerList = new ArrayList<>();
    List<BiConsumer<Long, AbstractStepScope<Solution_>>> bestSolutionMeterListenerList = new ArrayList<>();
    AbstractStepScope<Solution_> bestSolutionStepScope = null;
    long bestSolutionChangedTimestamp = Long.MIN_VALUE;
    boolean lastStepImprovedSolution = false;
    ScoreDefinition<?> scoreDefinition;
    final Function<Number, Number> scoreLevelNumberConverter;

    public StatisticRegistry(ScoreDefinition<?> scoreDefinition) {
        this.scoreDefinition = scoreDefinition;
        var zeroScoreLevel0 = scoreDefinition.getZeroScore().toLevelNumbers()[0];
        if (zeroScoreLevel0 instanceof BigDecimal) {
            scoreLevelNumberConverter = number -> BigDecimal.valueOf(number.doubleValue());
        } else if (zeroScoreLevel0 instanceof BigInteger) {
            scoreLevelNumberConverter = number -> BigInteger.valueOf(number.longValue());
        } else if (zeroScoreLevel0 instanceof Double) {
            scoreLevelNumberConverter = Number::doubleValue;
        } else if (zeroScoreLevel0 instanceof Float) {
            scoreLevelNumberConverter = Number::floatValue;
        } else if (zeroScoreLevel0 instanceof Long) {
            scoreLevelNumberConverter = Number::longValue;
        } else if (zeroScoreLevel0 instanceof Integer) {
            scoreLevelNumberConverter = Number::intValue;
        } else if (zeroScoreLevel0 instanceof Short) {
            scoreLevelNumberConverter = Number::shortValue;
        } else if (zeroScoreLevel0 instanceof Byte) {
            scoreLevelNumberConverter = Number::byteValue;
        } else {
            throw new IllegalStateException(
                    "Cannot determine score level type for score definition (" + scoreDefinition.getClass().getName() + ").");
        }
    }

    public void addListener(SolverMetric metric, Consumer<Long> listener) {
        addListener(metric, (timestamp, stepScope) -> listener.accept(timestamp));
    }

    public void addListener(SolverMetric metric, BiConsumer<Long, AbstractStepScope<Solution_>> listener) {
        if (metric.isMetricBestSolutionBased()) {
            bestSolutionMeterListenerList.add(listener);
        } else {
            stepMeterListenerList.add(listener);
        }
    }

    public void addListener(Consumer<SolverScope<Solution_>> listener) {
        solverMeterListenerList.add(listener);
    }

    public Set<Meter.Id> getMeterIds(SolverMetric metric, Tags runId) {
        return Search.in(this).name(name -> name.startsWith(metric.getMeterId())).tags(runId)
                .meters().stream().map(Meter::getId)
                .collect(Collectors.toSet());
    }

    public void extractScoreFromMeters(SolverMetric metric, Tags runId, Consumer<InnerScore<?>> scoreConsumer) {
        var score = SolverMetricUtil.extractScore(metric, scoreDefinition, id -> {
            var scoreLevelGauge = this.find(id).tags(runId).gauge();
            if (scoreLevelGauge != null && Double.isFinite(scoreLevelGauge.value())) {
                return scoreLevelNumberConverter.apply(scoreLevelGauge.value());
            } else {
                return null;
            }
        });
        if (score != null) {
            scoreConsumer.accept(score);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void extractConstraintSummariesFromMeters(SolverMetric metric, Tags runId,
            Consumer<ConstraintSummary<?>> constraintMatchTotalConsumer) {
        // Add the constraint ids from the meter ids
        getMeterIds(metric, runId)
                .stream()
                .map(meterId -> ConstraintRef.of(meterId.getTag(CONSTRAINT_PACKAGE_TAG), meterId.getTag(CONSTRAINT_NAME_TAG)))
                .distinct()
                .forEach(constraintRef -> {
                    var constraintMatchTotalRunId = runId.and(CONSTRAINT_PACKAGE_TAG, constraintRef.packageName())
                            .and(CONSTRAINT_NAME_TAG, constraintRef.constraintName());
                    // Get the score from the corresponding constraint package and constraint name meters
                    extractScoreFromMeters(metric, constraintMatchTotalRunId,
                            // Get the count gauge (add constraint package and constraint name to the run tags)
                            score -> {
                                var count = SolverMetricUtil.getGaugeValue(this, SolverMetricUtil.getGaugeName(metric, "count"),
                                        constraintMatchTotalRunId);
                                if (count != null) {
                                    constraintMatchTotalConsumer
                                            .accept(new ConstraintSummary(constraintRef, score.raw(), count.intValue()));
                                }
                            });
                });
    }

    public void extractMoveCountPerType(SolverScope<Solution_> solverScope, ObjLongConsumer<String> gaugeConsumer) {
        solverScope.getMoveCountTypes().forEach(type -> {
            var gauge = this.find(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + type)
                    .tags(solverScope.getMonitoringTags()).gauge();
            if (gauge != null) {
                gaugeConsumer.accept(type, (long) gauge.value());
            }
        });
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        var timestamp = System.currentTimeMillis() - stepScope.getPhaseScope().getSolverScope().getStartingSystemTimeMillis();
        stepMeterListenerList.forEach(listener -> listener.accept(timestamp, stepScope));
        if (stepScope.getBestScoreImproved()) {
            // Since best solution metrics are updated in a best solution listener, we need
            // to delay updating it until after the best solution listeners were processed
            bestSolutionStepScope = stepScope;
            bestSolutionChangedTimestamp = timestamp;
            lastStepImprovedSolution = true;
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // intentional empty
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        if (lastStepImprovedSolution) {
            bestSolutionMeterListenerList
                    .forEach(listener -> listener.accept(bestSolutionChangedTimestamp, bestSolutionStepScope));
            lastStepImprovedSolution = false;
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // intentional empty
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // intentional empty
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        if (lastStepImprovedSolution) {
            bestSolutionMeterListenerList
                    .forEach(listener -> listener.accept(bestSolutionChangedTimestamp, bestSolutionStepScope));
            lastStepImprovedSolution = false;
        }
        solverMeterListenerList.forEach(listener -> listener.accept(solverScope));
    }
}
