package org.optaplanner.constraint.streams.bavet.tri;

import org.optaplanner.constraint.streams.bavet.bi.BiTuple;
import org.optaplanner.constraint.streams.bavet.common.AbstractUnindexedJoinNode;
import org.optaplanner.constraint.streams.bavet.common.TupleLifecycle;
import org.optaplanner.constraint.streams.bavet.uni.UniTuple;

final class UnindexedJoinTriNode<A, B, C>
        extends AbstractUnindexedJoinNode<BiTuple<A, B>, C, TriTuple<A, B, C>, TriTupleImpl<A, B, C>> {

    private final int outputStoreSize;

    public UnindexedJoinTriNode(
            int inputStoreIndexLeftEntry, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightEntry, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize,
            int outputStoreIndexLeftOutEntry, int outputStoreIndexRightOutEntry) {
        super(inputStoreIndexLeftEntry, inputStoreIndexLeftOutTupleList,
                inputStoreIndexRightEntry, inputStoreIndexRightOutTupleList,
                nextNodesTupleLifecycle,
                outputStoreIndexLeftOutEntry, outputStoreIndexRightOutEntry);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTupleImpl<A, B, C> createOutTuple(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return new TriTupleImpl<>(leftTuple.getFactA(), leftTuple.getFactB(), rightTuple.getFactA(), outputStoreSize);
    }

    @Override
    protected void setOutTupleLeftFacts(TriTupleImpl<A, B, C> outTuple, BiTuple<A, B> leftTuple) {
        outTuple.factA = leftTuple.getFactA();
        outTuple.factB = leftTuple.getFactB();
    }

    @Override
    protected void setOutTupleRightFact(TriTupleImpl<A, B, C> outTuple, UniTuple<C> rightTuple) {
        outTuple.factC = rightTuple.getFactA();
    }

}