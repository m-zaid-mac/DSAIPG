package com.phasmidsoftware.dsaipg.util.benchmark;

import com.phasmidsoftware.dsaipg.sort.generic.SortWithHelper;
import com.phasmidsoftware.dsaipg.util.config.Config;
import com.phasmidsoftware.dsaipg.util.logging.LazyLogger;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.phasmidsoftware.dsaipg.util.general.Utilities.formatWhole;

/**
 * Class to extend Benchmark_Timer for sorting an array of T values.
 *
 * @param <T> the underlying type to be sorted.
 */
public class SorterBenchmark<T extends Comparable<T>> extends Benchmark_Timer<T[]> {

    public void run(String description, int N) {
        if (nRuns > 0) {
            logger.info("run: sort " + formatWhole(N) + " elements with " + this);
            // FIX: Use getHelper().init(N, nRuns) instead of sorter.init(N).
            // The original sorter.init(N) only passes the array size but NOT the
            // number of runs, so the InstrumentedComparatorHelper never allocates
            // its StatPack. This causes:
            //   - All stats to show <unset> (StatPack not created)
            //   - random() to produce zero-length arrays (n not properly set)
            //   - Timing to return NaN (no actual sorting work done)
            // The fix passes both N and nRuns so the StatPack is fully initialized.
            sorter.getHelper().init(N, nRuns);
            final double time = super.runFromSupplier(() -> generateRandomArray(ts), nRuns);
            for (TimeLogger timeLogger : timeLoggers) timeLogger.log(description, time, N);
        } else
            logger.warn("run: skipping " + this);
    }

    @Override
    public String toString() {
        return "SorterBenchmark on " + tClass + " from " + formatWhole(ts.length) + " total elements and " + formatWhole(nRuns) + " runs";
    }

    public SorterBenchmark(Class<T> tClass, UnaryOperator<T[]> preProcessor, SortWithHelper<T> sorter, Consumer<T[]> postProcessor, T[] ts, int nRuns, TimeLogger[] timeLoggers, Config config) {
        super(sorter.toString(), config, preProcessor, sorter::mutatingSort, postProcessor);
        this.sorter = sorter;
        this.tClass = tClass;
        this.ts = ts;
        this.nRuns = nRuns;
        this.timeLoggers = timeLoggers;
    }

    public SorterBenchmark(Class<T> tClass, UnaryOperator<T[]> preProcessor, SortWithHelper<T> sorter, T[] ts, int nRuns, TimeLogger[] timeLoggers) {
        this(tClass, preProcessor, sorter, sorter::postProcess, ts, nRuns, timeLoggers, Config.getConfig(tClass));
    }

    public SorterBenchmark(Class<T> tClass, SortWithHelper<T> sorter, T[] ts, int nRuns, TimeLogger[] timeLoggers) {
        this(tClass, null, sorter, ts, nRuns, timeLoggers);
    }

    private T[] generateRandomArray(T[] lookupArray) {
        return sorter.getHelper().random(tClass, (r) -> lookupArray[r.nextInt(lookupArray.length)]);
    }

    protected final SortWithHelper<T> sorter;
    protected final T[] ts;
    protected final int nRuns;
    protected final TimeLogger[] timeLoggers;
    private final static LazyLogger logger = new LazyLogger(SorterBenchmark.class);
    private final Class<T> tClass;
}