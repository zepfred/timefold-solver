package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class ConsumerMoveAdapter<Solution_, Value_> implements Move<Solution_> {

    private final Move<Solution_> move;
    private final Consumer<Value_> consumer;

    public ConsumerMoveAdapter(Move<Solution_> move, Consumer<Value_> consumer) {
        this.move = move;
        this.consumer = consumer;
    }

    public void consume(Value_ value) {
        consumer.accept(value);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return move.isMoveDoable(scoreDirector);
    }

    @Override
    public Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        return move.doMove(scoreDirector);
    }

    @Override
    public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        move.doMoveOnly(scoreDirector);
    }

    @Override
    public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return move.rebase(destinationScoreDirector);
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return move.getSimpleMoveTypeDescription();
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return move.getPlanningEntities();
    }

    @Override
    public Collection<?> getPlanningValues() {
        return move.getPlanningValues();
    }
}
