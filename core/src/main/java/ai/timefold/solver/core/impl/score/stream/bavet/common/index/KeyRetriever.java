package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Function;

sealed interface KeyRetriever<Key_> extends Function<Object, Key_>
        permits ManyKeyRetriever, SingleKeyRetriever {

}