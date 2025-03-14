package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedIfExistsUniNode<A, B> extends AbstractUnindexedIfExistsNode<UniTuple<A>, B> {

    private final BiPredicate<A, B> filtering;

    public UnindexedIfExistsUniNode(boolean shouldExist,
            int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightEntry,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle) {
        this(shouldExist,
                inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public UnindexedIfExistsUniNode(boolean shouldExist,
            int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList, int inputStoreIndexRightEntry,
            int inputStoreIndexRightTrackerList,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            BiPredicate<A, B> filtering) {
        super(shouldExist,
                inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList, inputStoreIndexRightEntry,
                inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.factA, rightTuple.factA);
    }

}
