/*
 * Copyright (c) 2017. Phasmid Software
 */

package com.phasmidsoftware.dsaipg.util.benchmark;

import com.phasmidsoftware.dsaipg.sort.classic.BucketSort;
import com.phasmidsoftware.dsaipg.sort.counting.LSDStringSort;
import com.phasmidsoftware.dsaipg.sort.counting.MSDStringSort;
import com.phasmidsoftware.dsaipg.sort.elementary.*;
import com.phasmidsoftware.dsaipg.sort.generic.Sort;
import com.phasmidsoftware.dsaipg.sort.generic.SortException;
import com.phasmidsoftware.dsaipg.sort.generic.SortWithComparableHelper;
import com.phasmidsoftware.dsaipg.sort.generic.SortWithHelper;
import com.phasmidsoftware.dsaipg.sort.helper.Helper;
import com.phasmidsoftware.dsaipg.sort.helper.NonInstrumentingComparableHelper;
import com.phasmidsoftware.dsaipg.sort.linearithmic.*;
import com.phasmidsoftware.dsaipg.util.config.Config;
import com.phasmidsoftware.dsaipg.util.general.CodePointMapper;
import com.phasmidsoftware.dsaipg.util.general.Utilities;
import com.phasmidsoftware.dsaipg.util.logging.LazyLogger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.phasmidsoftware.dsaipg.sort.helper.InstrumentedComparatorHelper.AT;
import static com.phasmidsoftware.dsaipg.sort.linearithmic.MergeSort.MERGESORT;
import static com.phasmidsoftware.dsaipg.util.benchmark.SortBenchmarkHelper.*;
import static com.phasmidsoftware.dsaipg.util.config.Config_Benchmark.isInstrumented;
import static com.phasmidsoftware.dsaipg.util.general.Utilities.formatWhole;

/**
 * <p>This class runs a suite of sorting benchmarks.
 * </p>
 * In order to make it work you need to do two things:
 * <ol><li>Edit config.ini</li>
 * <li>Provide command line arguments to specify the problem sizes that you want</li></ol>
 * <p>Note that each benchmark smaller than 512,000 is designed to run in approximately 10 seconds.
 * When the size equals or exceeds 512,000, that period of time will be roughly proportional to the size.</p>
 */
public class SortBenchmark {

    /**
     * The main method serves as the entry point for the SortBenchmark application.
     *
     * @param args The command-line arguments, representing word counts to be processed.
     * @throws IOException If an IO error occurs during loading configuration or execution.
     */
    public static void main(String[] args) throws IOException {
        Config config = Config.load(SortBenchmark.class);
        logger.info("!!!!!!!!!!!!!!!!!!!! SortBenchmark Start !!!!!!!!!!!!!!!!!!!!\n");
        logger.info("SortBenchmark.main: version " + config.get("sortbenchmark", "version") + " with word counts: " + Arrays.toString(args));
        if (args.length == 0) logger.warn("No word counts specified on the command line");
        new SortBenchmark(config).doMain(args);
    }

    /**
     * Extracts words from the given input line based on the Leipzig regex pattern.
     *
     * @param line the input string from which words are to be extracted.
     * @return a collection of strings containing the extracted words.
     */
    public static Collection<String> getLeipzigWords(String line) {
        return getWords(regexLeipzig, line);
    }

    /**
     * Method to run a sorting benchmark, using an explicit preProcessor.
     *
     * @param words        an array of available words (to be chosen randomly).
     * @param nWords       the number of words to be sorted.
     * @param nRuns        the number of runs of the sort to be performed.
     * @param sorter       the sorter to use.
     * @param preProcessor the pre-processor function, if any.
     * @param timeLoggers  a set of timeLoggers to be used.
     */
    static void runStringSortBenchmark(String[] words, int nWords, int nRuns, SortWithHelper<String> sorter, UnaryOperator<String[]> preProcessor, TimeLogger[] timeLoggers) {
        logger.info("****************************** String sort: " + nRuns + " runs of " + nWords + " " + sorter.getDescription() + " ******************************");
        // Initialize helper with n and nRuns so the StatPack is allocated and random()
        // generates arrays of the correct size. Without this, random() uses n=0 and
        // produces empty arrays, causing NaN timing and <unset> stats.
        sorter.getHelper().init(nWords, nRuns);
        new SorterBenchmark<>(String.class, preProcessor, sorter, words, nRuns, timeLoggers).run(getDescription(nWords, sorter), nWords);
        sorter.close();
    }

    /**
     * Method to apply integer sorting operations on a stream of integer sizes.
     *
     * @param wordCounts a stream of integer values representing the sizes of the integer datasets to be sorted
     */
    void sortIntegers(Stream<Long> wordCounts) {
        wordCounts.forEach(this::runIntegerSorts);
    }

    /**
     * Executes sorting operations on integer datasets using multiple sorting algorithms.
     *
     * @param N the size of the dataset to be sorted.
     */
    void runIntegerSorts(long N) {
        if (N > Integer.MAX_VALUE) throw new SortException("number of elements is too large");
        double totalWork = getTotalWork(N, config, "benchmarkintegersorters");
        if (isConfigBenchmarkIntegerSorter("shellsort"))
            sortIntegersByShellSort((int) N, 12 * estimateRuns(totalWork, Math.pow(N, 4.0 / 3)));
        if (isConfigBenchmarkIntegerSorter("bucketsort"))
            runIntegerBucketSort((int) N, estimateRuns(totalWork * 2, N));
        if (isConfigBenchmarkIntegerSorter("quicksort"))
            runIntegerQuickSort((int) N, 10 * estimateRuns(totalWork, Math.log(N) * N));
    }

    /**
     * Method to benchmark local date time sorts.
     *
     * @param n      the number of dateTimes.
     * @param config the configuration.
     * @throws IOException if there's an exception.
     */
    void sortLocalDateTimes(final int n, Config config) throws IOException {
        logger.info("Beginning LocalDateTime sorts");
        Supplier<LocalDateTime[]> localDateTimeSupplier = () -> generateRandomLocalDateTimeArray(n);
        Helper<ChronoLocalDateTime<?>> helper = new NonInstrumentingComparableHelper<>("DateTimeHelper", config);
        final LocalDateTime[] localDateTimes = generateRandomLocalDateTimeArray(n);

        if (isConfigBenchmarkDateSorter("timsort"))
            logger.info(benchmarkFactory("ProcessingSort LocalDateTimes using Arrays::sort (TimSort)", config, Arrays::sort, null).runFromSupplier(localDateTimeSupplier, 100) + "ms");

        if (isConfigBenchmarkDateSorter("timsort")) {
            logger.info(benchmarkFactory("Repeat ProcessingSort LocalDateTimes using timSort::mutatingSort", config, new TimSortWrapper<>(helper)::mutatingSort, null).runFromSupplier(localDateTimeSupplier, 100) + "ms");
            runDateTimeSortBenchmark(LocalDateTime.class, localDateTimes, n, 100);
        }
    }

    /**
     * Method to run string sorter benchmarks.
     *
     * @param words  the word source.
     * @param nWords the number of words to be sorted.
     */
    void benchmarkStringSorters(String[] words, int nWords) {
        double totalWork = getTotalWork(nWords, config, BENCHMARKSTRINGSORTERS);
        logger.info("benchmarkStringSorters: sorting " + formatWhole(nWords) + " words" + (isInstrumented(config) ? " and instrumented" : "") + " with total work (for estimating runs): " + totalWork);
        if (isInstrumented(config))
            logger.info("    normalization of statistics is based on n ln n");
        Random random = new Random();
        int nRunsLinearithmic = estimateRuns(totalWork, minComparisons(nWords));
        int nRunsLinear = estimateRuns(totalWork, 45.0 * nWords);
        int nRunsBucket = estimateRuns(totalWork, 2.0 * nWords + 0.5 * nWords * nWords / BucketSort.DIGRAPHS_SIZE);

        if (isConfigBenchmarkStringSorter("puresystemsort") && nRunsLinearithmic > 0)
            runPureSystemSortBenchmark(words, nWords, nRunsLinearithmic, random);

        if (isConfigBenchmarkStringSorter("bucketsort") && nRunsBucket > 0)
            try (SortWithHelper<String> sorter = BucketSort.CaseIndependentBucketSort(BucketSort::classifyStringDigraph, BucketSort.DIGRAPHS_SIZE, nWords, config)) {
                runStringSortBenchmark(words, nWords, nRunsBucket, sorter, timeLoggersLinear);
            }

        if (isConfigBenchmarkStringSorter("LSD") && nRunsLinear > 0) {
            int nRuns = 12 * nRunsLinear;
            try (SortWithHelper<String> sorter = new LSDStringSort(nWords, 20, String::compareTo, nRuns, config)) {
                runStringSortBenchmark(words, nWords, nRuns, sorter, timeLoggersLinear);
            }
        }

        if (isConfigBenchmarkStringSorter("MSD") && nRunsLinear > 0) {
            int nRuns = 25 * nRunsLinear;
            try (SortWithHelper<String> sorter = new MSDStringSort(CodePointMapper.ASCIIExt, nWords, nRuns, config)) {
                runStringSortBenchmark(words, nWords, nRuns, sorter, timeLoggersLinear);
            }
        }

        if (isConfigBenchmarkStringSorter("timsort") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = TimSortWrapper.CaseInsensitiveSort(nWords, config)) {
                runStringSortBenchmark(words, nWords, 3 * nRunsLinearithmic, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter(MERGESORT))
            runMergeSortBenchmark(words, nWords, 5 * nRunsLinearithmic, config);

        if (isConfigBenchmarkStringSorter("quicksort3way") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = new QuickSort_3way<>(nWords, nRunsLinearithmic, config)) {
                runStringSortBenchmark(words, nWords, 9 * nRunsLinearithmic / 2, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter("quicksortDualPivot") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = new QuickSort_DualPivot<>(nWords, nRunsLinearithmic, config)) {
                runStringSortBenchmark(words, nWords, 6 * nRunsLinearithmic, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter("quicksort") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = new QuickSort_Basic<>(nWords, nRunsLinearithmic, config)) {
                runStringSortBenchmark(words, nWords, 6 * nRunsLinearithmic, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter("heapsort") && nRunsLinearithmic > 0) {
            try (SortWithHelper<String> sorter = new HeapSort<>(nWords, nRunsLinearithmic, config)) {
                runStringSortBenchmark(words, nWords, 9 * nRunsLinearithmic / 2, sorter, timeLoggersLinearithmic);
            }
        }

        if (isConfigBenchmarkStringSorter("introsort") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = new IntroSort<>(nWords, nRunsLinearithmic, config)) {
                runStringSortBenchmark(words, nWords, 9 * nRunsLinearithmic / 2, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter("randomsort") && nRunsLinearithmic > 0)
            try (SortWithHelper<String> sorter = new RandomSort<>(nWords, config)) {
                runStringSortBenchmark(words, nWords, nRunsLinearithmic, sorter, timeLoggersLinearithmic);
            }

        if (isConfigBenchmarkStringSorter("shellsort")) {
            int mode = config.getInt(BENCHMARKSTRINGSORTERS, "shellsortmode", 4);
            double growthExponent = 6.0 / 5;
            if (mode != 4) { growthExponent = 4.0 / 3; }
            int nRunsSubQuadratic = estimateRuns(totalWork, Math.pow(nWords, growthExponent) / 2);
            if (nRunsSubQuadratic > 0)
                try (SortWithHelper<String> sorter = new ShellSort<>(mode, nWords, nRunsSubQuadratic, config)) {
                    runStringSortBenchmark(words, nWords, nRunsSubQuadratic, sorter, timeLoggersSubQuadratic(growthExponent));
                }
        }

        if (isQuadratic()) {
            double inversions = meanInversions(nWords);
            int nRunsInsertionOpt = estimateRuns(totalWork * 300, inversions) / 2;
            int nRunsBubble = estimateRuns(totalWork * 40, inversions) / 10;
            int nRunsSelection = estimateRuns(totalWork * 90, inversions) / 10;
            int nRunsInsertion = estimateRuns(totalWork * 120, inversions) / 6;

            if (isConfigBenchmarkStringSorter("insertionsortopt") && nRunsInsertionOpt > 0)
                try (SortWithHelper<String> sorter = new InsertionSortOpt<>(InsertionSortOpt.DESCRIPTION, nWords, nRunsInsertionOpt, config)) {
                    runStringSortBenchmark(words, nWords, nRunsInsertionOpt, sorter, timeLoggersQuadratic);
                }

            if (isConfigBenchmarkStringSorter("insertionsort") && nRunsInsertion > 0)
                try (SortWithHelper<String> sorter = new InsertionSort<>(InsertionSort.DESCRIPTION, nWords, nRunsInsertion, config)) {
                    runStringSortBenchmark(words, nWords, nRunsInsertion, sorter, timeLoggersQuadratic);
                }

            if (isConfigBenchmarkStringSorter("bubblesort") && nRunsBubble > 0)
                try (SortWithHelper<String> sorter = new BubbleSort<>(nWords, nRunsBubble, config)) {
                    runStringSortBenchmark(words, nWords, nRunsBubble, sorter, timeLoggersQuadratic);
                }

            if (isConfigBenchmarkStringSorter("selectionsort") && nRunsSelection > 0)
                try (SortWithHelper<String> sorter = new SelectionSort<>(nWords, nRunsSelection, config)) {
                    runStringSortBenchmark(words, nWords, nRunsSelection, sorter, timeLoggersQuadratic);
                }
        }
    }

    /**
     * Method to allow unit testing of the main program.
     *
     * @param args the command-line arguments.
     */
    void doMain(String[] args) {
        sortStrings(getWordCounts(args));
        sortIntegers(getWordCounts(args));
    }

    /**
     * Constructor for the SortBenchmark class.
     *
     * @param config the configuration object to set up the benchmark environment
     */
    public SortBenchmark(Config config) {
        this.config = config;
    }

    private static double getTotalWork(long n, Config config, final String configSection) {
        long z = config.getLong(configSection, "totalwork", 100_000_000L);
        long x = n / 512_000 + 1;
        return (double) z * x;
    }

    private int estimateRuns(double totalWork, double workPerRun) {
        long result = Utilities.round(totalWork / workPerRun);
        if (result >= 0 && result < Integer.MAX_VALUE)
            if (result < 10_000_000)
                return (int) result;
            else
                throw new SortException("estimated number of runs is too large (max is 10 million): " + result + ". Reduce the value of totalwork accordingly");
        else
            throw new RuntimeException("estimated number of runs is not a positive Integer: " + result);
    }

    private void runIntegerBucketSort(int N, final int runs) {
        int bucketSize = config.getInt(BENCHMARKINTEGERSORTERS, "bucketsize", 16);
        int buckets = (N + bucketSize - 1) / bucketSize;
        BucketSort<Integer> sorter = new BucketSort<>(null, buckets, N, config);
        Helper<Integer> helper = sorter.getHelper();
        helper.init(N);
        Integer[] xs = helper.random(N, Integer.class, r -> r.nextInt(1000));
        runIntegerSortBenchmark(xs, N, runs, sorter, null, timeLoggersLinearithmic);
        helper.close();
    }

    private boolean isQuadratic() {
        return isConfigBenchmarkStringSorter("insertionsort") || isConfigBenchmarkStringSorter("insertionsortopt") || isConfigBenchmarkStringSorter("bubblesort") || isConfigBenchmarkStringSorter("selectionsort");
    }

    private void runPureSystemSortBenchmark(String[] words, int nWords, int nRuns, Random random) {
        Benchmark<String[]> benchmark = new Benchmark_Timer<>("SystemSort", config, null, Arrays::sort, null);
        doPureBenchmark(words, nWords, nRuns, random, benchmark);
    }

    private void sortIntegersByShellSort(int N, int runs) {
        int m = config.getInt(BENCHMARKINTEGERSORTERS, "mode", 4);
        double growthExponent = 6.0 / 5;
        if (m != 3) { growthExponent = 4.0 / 3; }
        SortWithHelper<Integer> sorter = new ShellSort<>(m, N, runs, config);
        Integer[] numbers = sorter.getHelper().random(Integer.class, Random::nextInt);
        runIntegerSortBenchmark(numbers, N, runs, sorter, sorter::preProcess, timeLoggersSubQuadratic(growthExponent));
    }

    private void runIntegerQuickSort(int N, final int runs) {
        SortWithHelper<Integer> sorter = new QuickSort_DualPivot<>(N, runs, config);
        Integer[] numbers = sorter.getHelper().random(Integer.class, Random::nextInt);
        runIntegerSortBenchmark(numbers, N, runs, sorter, sorter::preProcess, timeLoggersLinearithmic);
    }

    private void sortStrings(Stream<Long> wordCounts) {
        boolean showProgress = config.getBoolean("timer", "showprogress");
        logger.info("Beginning String Sorts, showing progress: " + showProgress);
        wordCounts.forEach(this::doLeipzigBenchmarkEnglish);
    }

    private void doLeipzigBenchmarkEnglish(long N) {
        if (N > Integer.MAX_VALUE) throw new SortException("number of elements is too large");
        int x = (int) N;
        logger.info("############################### " + x + " words ###############################");
        String resource = "/eng-uk_web_2002_" + (x < 50000 ? "10K" : "100K") + "-sentences.txt";
        try {
            benchmarkStringSorters(getWords(resource, SortBenchmark::getLeipzigWords), x);
        } catch (FileNotFoundException e) {
            logger.warn("Unable to find resource: " + resource + "because:", e);
        } catch (Exception e) {
            logger.warn("Unable to run benchmark with N: " + N + "because:", e);
        }
    }

    /**
     * Method to run a sorting benchmark using the standard preProcess method of the sorter.
     * Initializes the helper with nWords and nRuns before running so that the instrumented
     * StatPack is properly allocated and stats are collected correctly.
     *
     * @param words       an array of available words (to be chosen randomly).
     * @param nWords      the number of words to be sorted.
     * @param nRuns       the number of runs of the sort to be performed.
     * @param sorter      the sorter to use.
     * @param timeLoggers a set of timeLoggers to be used.
     */
    public static void runStringSortBenchmark(String[] words, int nWords, int nRuns, SortWithHelper<String> sorter, TimeLogger[] timeLoggers) {
        // Initialize helper with both n and nRuns — required for the instrumented StatPack
        // to be allocated before the benchmark loop starts. Without this, all metrics show
        // <unset> and timing returns NaN because the StatPack was never created.
        sorter.getHelper().init(nWords, nRuns);
        try (Stopwatch stopwatch = new Stopwatch()) {
            runStringSortBenchmark(words, nWords, nRuns, sorter, sorter::preProcess, timeLoggers);
            logger.info("************************************************************ (" + stopwatch.lap() / 1000.0 + " sec.)");
        }
    }

    static void runIntegerSortBenchmark(Integer[] numbers, int n, int nRuns, SortWithHelper<Integer> sorter, UnaryOperator<Integer[]> preProcessor, TimeLogger[] timeLoggers) {
        logger.info("****************************** Integer sort: " + n + " " + sorter.getDescription() + " ******************************");
        try (Stopwatch stopwatch = new Stopwatch()) {
            // Initialize helper with n and nRuns so the instrumented StatPack is allocated.
            sorter.getHelper().init(n, nRuns);
            new SorterBenchmark<>(Integer.class, preProcessor, sorter, numbers, nRuns, timeLoggers).run(getDescription(n, sorter), n);
            sorter.close();
            logger.info("************************************************************ (" + stopwatch.lap() / 1000.0 + " sec.)");
        }
    }

    public static final String BENCHMARKSTRINGSORTERS = "benchmarkstringsorters";
    public static final String BENCHMARKINTEGERSORTERS = "benchmarkintegersorters";
    public static final TimeLogger TIME_LOGGER_RAW = new TimeLogger("Raw time per run {mSec}: ", null);

    public final static TimeLogger[] timeLoggersLinearithmic = {
            TIME_LOGGER_RAW,
            new TimeLogger("Normalized time per run {n log n}: ", SortBenchmark::minComparisons)
    };

    public final static TimeLogger[] timeLoggersLinear = {
            TIME_LOGGER_RAW,
            new TimeLogger("Normalized time per run {n}: ", n -> n * 1.0)
    };

    final static TimeLogger[] timeLoggersQuadratic = {
            TIME_LOGGER_RAW,
            new TimeLogger("Normalized time per run {n^2}: ", SortBenchmark::meanInversions)
    };

    static TimeLogger[] timeLoggersSubQuadratic(double power) {
        return new TimeLogger[]{
                TIME_LOGGER_RAW,
                new TimeLogger("Normalized time per run {n^" + power + "}: ", n -> Math.pow(n, power))
        };
    }

    final static LazyLogger logger = new LazyLogger(SortBenchmark.class);

    static double minComparisons(int n) {
        double lgN = Utilities.lg(n);
        return n * (lgN - LgE) + lgN / 2 + 1.33;
    }

    static double meanInversions(int n) {
        return 0.25 * n * (n - 1);
    }

    private static Collection<String> lineAsList(String line) {
        List<String> words = new ArrayList<>();
        words.add(line);
        return words;
    }

    private static Benchmark<LocalDateTime[]> benchmarkFactory(String description, Config config, Consumer<LocalDateTime[]> sorter, Consumer<LocalDateTime[]> checker) {
        return new Benchmark_Timer<>(
                description,
                config,
                (xs) -> Arrays.copyOf(xs, xs.length),
                sorter,
                checker);
    }

    private static void doPureBenchmark(String[] words, int nWords, int nRuns, Random random, Benchmark<String[]> benchmark) {
        final double time = benchmark.runFromSupplier(() -> Utilities.fillRandomArray(String.class, random, nWords, r -> words[r.nextInt(words.length)]), nRuns);
        for (TimeLogger timeLogger : timeLoggersLinearithmic) timeLogger.log("pure benchmark", time, nWords);
    }

    private static Stream<Long> getWordCounts(String[] args) {
        return Arrays.stream(args).map(SortBenchmark::parseInt);
    }

    static long parseInt(String w) {
        long result = 1L;
        String expression = w.replaceAll("[gG]", "mk").replaceAll("[mM]", "kk").replaceAll("[kK]", "*1024");
        for (String split : expression.split("\\*")) result *= Integer.parseInt(split);
        return result;
    }

    private void runMergeSortBenchmark(String[] words, int nWords, int nRuns, Config config) {
        try (SortWithComparableHelper<String> sorter = new MergeSort<>(nWords, nRuns, config)) {
            runStringSortBenchmark(words, nWords, nRuns, sorter, timeLoggersLinearithmic);
        }
    }

    private static <X> String getDescription(int n, Sort<X> sorter) {
        return n + AT + sorter.getDescription();
    }

    @SuppressWarnings("SameParameterValue")
    private void runDateTimeSortBenchmark(Class<?> tClass, ChronoLocalDateTime<?>[] dateTimes, int N, int m) throws IOException {
        final SortWithHelper<ChronoLocalDateTime<?>> sorter = new TimSortWrapper<>();
        logger.info("****************************** DateTime sort: " + N + " " + sorter.getDescription() + " ******************************");
        @SuppressWarnings("unchecked") final SorterBenchmark<ChronoLocalDateTime<?>> sorterBenchmark = new SorterBenchmark<>((Class<ChronoLocalDateTime<?>>) tClass, (xs) -> Arrays.copyOf(xs, xs.length), sorter, dateTimes, m, timeLoggersLinearithmic);
        sorterBenchmark.run(getDescription(N, sorter), N);
        sorter.close();
        logger.info("************************************************************");
    }

    private static final double LgE = Utilities.lg(Math.E);

    private boolean isConfigBenchmarkStringSorter(String option) {
        return isConfigBoolean(BENCHMARKSTRINGSORTERS, option);
    }

    private boolean isConfigBenchmarkDateSorter(String option) {
        return isConfigBoolean("benchmarkdatesorters", option);
    }

    private boolean isConfigBenchmarkIntegerSorter(String option) {
        return isConfigBoolean(BENCHMARKINTEGERSORTERS, option);
    }

    private boolean isConfigBoolean(String section, String option) {
        return config.getBoolean(section, option);
    }

    private final Config config;
}