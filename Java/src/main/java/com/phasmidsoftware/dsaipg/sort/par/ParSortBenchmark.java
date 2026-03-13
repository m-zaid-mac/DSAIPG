package com.phasmidsoftware.dsaipg.sort.par;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class ParSortBenchmark {

    private static final int WARMUP_RUNS  = 3;
    private static final int TIMED_RUNS   = 7;
    private static final int RANDOM_BOUND = 10_000_000;

    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int NATURAL_DEPTH =
            Math.max(1, (int)(Math.log(PROCESSORS) / Math.log(2)));

    public static void main(String[] args) {
        int overrideCutoff = (args.length > 0) ? Integer.parseInt(args[0]) : -1;

        System.out.println("========================================================================");
        System.out.println("  ParSort Benchmark");
        System.out.println("========================================================================");
        System.out.println("  Available processors : " + PROCESSORS);
        System.out.println("  Common-pool threads  : " + ForkJoinPool.getCommonPoolParallelism());
        System.out.println("  Natural depth limit  : " + NATURAL_DEPTH + "  (~" + (1 << NATURAL_DEPTH) + " parallel tasks)");
        System.out.println("  Warm-up / timed runs : " + WARMUP_RUNS + " / " + TIMED_RUNS);
        System.out.println();

        int fixedSize = 2_000_000;
        int[] cutoffs = {500, 1_000, 2_000, 5_000, 10_000, 25_000, 50_000, 100_000, 250_000, 500_000};
        runExperiment1(fixedSize, cutoffs);

        int bestCutoff = (overrideCutoff > 0) ? overrideCutoff : pickBestCutoff(fixedSize, cutoffs);
        int[] sizes = {100_000, 250_000, 500_000, 1_000_000, 2_000_000, 4_000_000, 8_000_000};
        runExperiment2(bestCutoff, sizes);

        int[] schemeSizes = {500_000, 1_000_000, 2_000_000, 4_000_000};
        runExperiment3(bestCutoff, schemeSizes);

    }

    private static void runExperiment1(int arraySize, int[] cutoffs) {
        System.out.println("--- Experiment 1: Cutoff sweep  (N = " + arraySize + ", scheme = cutoff-only) ---");
        System.out.printf("%-14s  %-12s  %-12s  %-8s%n", "Cutoff", "Par (ms)", "Seq (ms)", "Speedup");
        System.out.println("-------------------------------------------------------");

        double seqTime = measureSequential(arraySize);
        for (int cutoff : cutoffs) {
            ParSort.cutoff   = cutoff;
            ParSort.maxDepth = Integer.MAX_VALUE;
            double parTime = measureParallel(arraySize);
            double speedup = seqTime / parTime;
            System.out.printf("%-14d  %-12.2f  %-12.2f  %.2fx%n", cutoff, parTime, seqTime, speedup);
        }
        System.out.println();
    }

    private static void runExperiment2(int cutoff, int[] sizes) {
        System.out.println("--- Experiment 2: Array-size sweep  (cutoff = " + cutoff + ", cutoff-only) ---");
        System.out.printf("%-14s  %-12s  %-12s  %-8s%n", "Array size", "Par (ms)", "Seq (ms)", "Speedup");
        System.out.println("-------------------------------------------------------");

        ParSort.maxDepth = Integer.MAX_VALUE;
        for (int n : sizes) {
            ParSort.cutoff = cutoff;
            double parTime = measureParallel(n);
            double seqTime = measureSequential(n);
            double speedup = seqTime / parTime;
            System.out.printf("%-14d  %-12.2f  %-12.2f  %.2fx%n", n, parTime, seqTime, speedup);
        }
        System.out.println();
    }

    private static void runExperiment3(int cutoff, int[] sizes) {
        System.out.println("--- Experiment 3: Scheme comparison  (cutoff=" + cutoff + ", naturalDepth=" + NATURAL_DEPTH + ") ---");
        System.out.printf("%-14s  %-12s  %-12s  %-12s  %-12s%n",
                "Array size", "Seq (ms)", "CutoffOnly", "DepthOnly", "Combined");
        System.out.println("------------------------------------------------------------------------");

        for (int n : sizes) {
            double seqTime = measureSequential(n);

            ParSort.cutoff   = cutoff;
            ParSort.maxDepth = Integer.MAX_VALUE;
            double cutoffTime = measureParallel(n);

            ParSort.cutoff   = 1;
            ParSort.maxDepth = NATURAL_DEPTH;
            double depthTime = measureParallel(n);

            ParSort.cutoff   = cutoff;
            ParSort.maxDepth = NATURAL_DEPTH;
            double combinedTime = measureParallel(n);

            System.out.printf("%-14d  %-12.2f  %-12.2f  %-12.2f  %-12.2f%n",
                    n, seqTime, cutoffTime, depthTime, combinedTime);
        }
        System.out.println();
    }

    private static double measureSequential(int n) {
        Random rng = new Random(42);
        int[] array = new int[n];
        for (int r = 0; r < WARMUP_RUNS; r++) { fillRandom(array, rng); Arrays.sort(array); }
        long total = 0;
        for (int r = 0; r < TIMED_RUNS; r++) {
            fillRandom(array, rng);
            long start = System.nanoTime();
            Arrays.sort(array);
            total += System.nanoTime() - start;
        }
        return total / 1_000_000.0 / TIMED_RUNS;
    }

    private static double measureParallel(int n) {
        Random rng = new Random(42);
        int[] array = new int[n];
        for (int r = 0; r < WARMUP_RUNS; r++) { fillRandom(array, rng); ParSort.sort(array, 0, n); }
        long total = 0;
        for (int r = 0; r < TIMED_RUNS; r++) {
            fillRandom(array, rng);
            long start = System.nanoTime();
            ParSort.sort(array, 0, n);
            total += System.nanoTime() - start;
        }
        return total / 1_000_000.0 / TIMED_RUNS;
    }

    private static void fillRandom(int[] array, Random rng) {
        for (int i = 0; i < array.length; i++) array[i] = rng.nextInt(RANDOM_BOUND);
    }

    private static int pickBestCutoff(int arraySize, int[] cutoffs) {
        System.out.println("(Selecting best cutoff for subsequent experiments...)");
        double best = Double.MAX_VALUE;
        int bestCutoff = cutoffs[0];
        ParSort.maxDepth = Integer.MAX_VALUE;
        for (int c : cutoffs) {
            ParSort.cutoff = c;
            double t = measureParallel(arraySize);
            if (t < best) { best = t; bestCutoff = c; }
        }
        System.out.println("  Best cutoff = " + bestCutoff + "  (" + String.format("%.2f", best) + " ms)\n");
        return bestCutoff;
    }
}