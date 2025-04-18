package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Quadruple;

public final class Group0Mapping4CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupQuadNode<OldA, OldB, OldC, OldD, QuadTuple<A, B, C, D>, Void, Object, Quadruple<A, B, C, D>> {

    private final int outputStoreSize;

    public Group0Mapping4CollectorQuadNode(int groupStoreIndex, int undoStoreIndex,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerA_, A> collectorA,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerB_, B> collectorB,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerC_, C> collectorC,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                null, mergeCollectors(collectorA, collectorB, collectorC, collectorD),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <OldA, OldB, OldC, OldD, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
            QuadConstraintCollector<OldA, OldB, OldC, OldD, Object, Quadruple<A, B, C, D>> mergeCollectors(
                    QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerA_, A> collectorA,
                    QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerB_, B> collectorB,
                    QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerC_, C> collectorC,
                    QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerD_, D> collectorD) {
        return (QuadConstraintCollector<OldA, OldB, OldC, OldD, Object, Quadruple<A, B, C, D>>) ConstraintCollectors.compose(
                collectorA, collectorB, collectorC, collectorD, Quadruple::new);
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(Void groupKey) {
        return new QuadTuple<>(null, null, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Quadruple<A, B, C, D> result) {
        outTuple.factA = result.a();
        outTuple.factB = result.b();
        outTuple.factC = result.c();
        outTuple.factD = result.d();
    }

}
