package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiDataStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractBiDataStream<Solution_, A, B> extends AbstractDataStream<Solution_>
        implements BiDataStream<Solution_, A, B> {

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory) {
        super(dataStreamFactory, null);
    }

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            @Nullable AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public final BiDataStream<Solution_, A, B> filter(BiDataFilter<Solution_, A, B> filter) {
        return shareAndAddChild(new FilterBiDataStream<>(dataStreamFactory, this, filter));
    }

    public BiDataset<Solution_, A, B> createDataset() {
        var stream = shareAndAddChild(new TerminalBiDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
