package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCompareToEquals;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCompareToOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotalScoreSolverRankingComparatorTest extends AbstractSolverRankingComparatorTest {

    private BenchmarkReport benchmarkReport;
    private TotalScoreSolverRankingComparator comparator;
    private SolverBenchmarkResult a;
    private SolverBenchmarkResult b;
    private List<SingleBenchmarkResult> aSingleBenchmarkResultList;
    private List<SingleBenchmarkResult> bSingleBenchmarkResultList;

    @BeforeEach
    void setUp() {
        benchmarkReport = mock(BenchmarkReport.class);
        comparator = new TotalScoreSolverRankingComparator();
        a = new SolverBenchmarkResult(null);
        a.setScoreDefinition(new SimpleScoreDefinition());
        b = new SolverBenchmarkResult(null);
        b.setScoreDefinition(new SimpleScoreDefinition());
        aSingleBenchmarkResultList = new ArrayList<>();
        bSingleBenchmarkResultList = new ArrayList<>();
    }

    @Test
    void normal() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -50, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -200, -50, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -50, -50, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void totalIsEqual() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1005, -30, -1005);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -200, -30, -1005);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1005);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -35, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -200, -35, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -35, -35, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void differentScoreDefinitions() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, 0, -1000, 0, -50, -10, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, 0, -200, 0, -50, -10, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, -7, -50, 0, -50, -10, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void uninitializedSingleBenchmarks() {
        var a0 = addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        var b0 = addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        var b1 = addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToEquals(comparator, a, b);

        a0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        b1.setAverageAndTotalScoreForTesting(SimpleScore.of(-400), false);
        a.accumulateResults(benchmarkReport);
        b.accumulateResults(benchmarkReport);
        // uninitialized variable count and total score are equal, A is worse on worst score (tie-breaker)
        assertCompareToOrder(comparator, a, b);

        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        b.accumulateResults(benchmarkReport);
        // uninitialized variable count is bigger in B
        assertCompareToOrder(comparator, b, a);

        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), true);
        b1.setAverageAndTotalScoreForTesting(SimpleScore.of(-400), false);
        b.accumulateResults(benchmarkReport);
        // uninitialized variable count is bigger in A
        assertCompareToOrder(comparator, a, b);
    }

}
