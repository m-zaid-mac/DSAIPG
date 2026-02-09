package com.phasmidsoftware.dsaipg.sort.elementary;

import com.phasmidsoftware.dsaipg.util.benchmark.Benchmark_Timer;
import com.phasmidsoftware.dsaipg.util.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Benchmark program for testing Insertion Sort performance with different array orderings.
 */
public class InsertionSortBenchmark {

    // Data structure to store results
    static class BenchmarkResult {
        int size;
        double randomTime;
        double orderedTime;
        double partiallyOrderedTime;
        double reverseOrderedTime;

        BenchmarkResult(int size, double random, double ordered, double partial, double reverse) {
            this.size = size;
            this.randomTime = random;
            this.orderedTime = ordered;
            this.partiallyOrderedTime = partial;
            this.reverseOrderedTime = reverse;
        }
    }

    public static void main(String[] args) {
        System.out.println("Insertion Sort Benchmark");
        System.out.println("========================\n");

        // Test with doubling sizes: 100, 200, 400, 800, 1600
        int[] sizes = {100, 200, 400, 800, 1600};
        int runs = 100; // Number of times to run each test

        List<BenchmarkResult> results = new ArrayList<>();

        try {
            Config config = Config.load();

            for (int n : sizes) {
                System.out.println("Array size: " + n);
                System.out.println("-".repeat(40));

                // Test 1: Random array
                double randomTime = benchmarkRandom(n, runs, config);
                System.out.printf("Random order:          %.3f ms%n", randomTime);

                // Test 2: Ordered array
                double orderedTime = benchmarkOrdered(n, runs, config);
                System.out.printf("Ordered:               %.3f ms%n", orderedTime);

                // Test 3: Partially ordered array
                double partiallyOrderedTime = benchmarkPartiallyOrdered(n, runs, config);
                System.out.printf("Partially ordered:     %.3f ms%n", partiallyOrderedTime);

                // Test 4: Reverse ordered array
                double reverseOrderedTime = benchmarkReverseOrdered(n, runs, config);
                System.out.printf("Reverse ordered:       %.3f ms%n", reverseOrderedTime);

                // Store results
                results.add(new BenchmarkResult(n, randomTime, orderedTime, partiallyOrderedTime, reverseOrderedTime));

                System.out.println();
            }

            // Print dynamic observations
            printObservations(results);

        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyze results and print dynamic observations
     */
    private static void printObservations(List<BenchmarkResult> results) {
        System.out.println("\nObservations:");
        System.out.println("=============\n");

        // Calculate growth ratios for different orderings
        System.out.println("1. GROWTH RATE ANALYSIS (Doubling Hypothesis):");
        System.out.println("   When n doubles, time should:");
        System.out.println("   - O(n): multiply by ~2x");
        System.out.println("   - O(n²): multiply by ~4x");
        System.out.println();

        analyzeGrowthRate("Random", results, r -> r.randomTime);
        analyzeGrowthRate("Ordered", results, r -> r.orderedTime);
        analyzeGrowthRate("Partially Ordered", results, r -> r.partiallyOrderedTime);
        analyzeGrowthRate("Reverse Ordered", results, r -> r.reverseOrderedTime);

        // Determine complexity based on average ratios
        System.out.println("\n2. TIME COMPLEXITY CONCLUSIONS:");

        double avgRandomRatio = calculateAverageRatio(results, r -> r.randomTime);
        double avgOrderedRatio = calculateAverageRatio(results, r -> r.orderedTime);
        double avgReverseRatio = calculateAverageRatio(results, r -> r.reverseOrderedTime);

        System.out.printf("   - Ordered: Average ratio = %.2fx → %s%n",
                avgOrderedRatio, classifyComplexity(avgOrderedRatio));
        System.out.printf("   - Random: Average ratio = %.2fx → %s%n",
                avgRandomRatio, classifyComplexity(avgRandomRatio));
        System.out.printf("   - Reverse: Average ratio = %.2fx → %s%n",
                avgReverseRatio, classifyComplexity(avgReverseRatio));

        // Performance comparison
        System.out.println("\n3. PERFORMANCE COMPARISON (at n=" + results.get(results.size()-1).size + "):");
        BenchmarkResult last = results.get(results.size() - 1);

        double fastest = Math.min(Math.min(last.orderedTime, last.randomTime),
                Math.min(last.partiallyOrderedTime, last.reverseOrderedTime));

        System.out.printf("   - Ordered:          %.3f ms (%.1fx of fastest)%n",
                last.orderedTime, last.orderedTime / fastest);
        System.out.printf("   - Partially Ordered: %.3f ms (%.1fx of fastest)%n",
                last.partiallyOrderedTime, last.partiallyOrderedTime / fastest);
        System.out.printf("   - Random:           %.3f ms (%.1fx of fastest)%n",
                last.randomTime, last.randomTime / fastest);
        System.out.printf("   - Reverse:          %.3f ms (%.1fx of fastest)%n",
                last.reverseOrderedTime, last.reverseOrderedTime / fastest);

        // Practical insights
        System.out.println("\n4. PRACTICAL INSIGHTS:");

        if (avgOrderedRatio < 1.5) {
            System.out.println("   ✓ Ordered arrays show O(n) behavior - excellent for nearly-sorted data!");
        }

        if (avgReverseRatio > 2.5) {
            System.out.println("   ✓ Random/Reverse arrays show O(n²) behavior - avoid for large unsorted data!");
        }

        double speedup = last.reverseOrderedTime / last.orderedTime;
        System.out.printf("   ✓ Pre-sorting can improve performance by %.1fx for worst-case data%n", speedup);

        if (last.reverseOrderedTime > 1.0) {
            System.out.println("   ⚠ For n>1600, consider O(n log n) algorithms like merge sort or quicksort");
        }
    }

    /**
     * Analyze growth rate for a specific ordering
     */
    private static void analyzeGrowthRate(String name, List<BenchmarkResult> results,
                                          java.util.function.Function<BenchmarkResult, Double> timeExtractor) {
        System.out.println("   " + name + ":");

        for (int i = 1; i < results.size(); i++) {
            BenchmarkResult prev = results.get(i - 1);
            BenchmarkResult curr = results.get(i);

            double prevTime = timeExtractor.apply(prev);
            double currTime = timeExtractor.apply(curr);

            if (prevTime > 0) {
                double ratio = currTime / prevTime;
                System.out.printf("     %d → %d: %.3f ms → %.3f ms (%.2fx)%n",
                        prev.size, curr.size, prevTime, currTime, ratio);
            }
        }
        System.out.println();
    }

    /**
     * Calculate average growth ratio
     */
    private static double calculateAverageRatio(List<BenchmarkResult> results,
                                                java.util.function.Function<BenchmarkResult, Double> timeExtractor) {
        double sum = 0;
        int count = 0;

        for (int i = 1; i < results.size(); i++) {
            double prevTime = timeExtractor.apply(results.get(i - 1));
            double currTime = timeExtractor.apply(results.get(i));

            if (prevTime > 0 && currTime > 0) {
                sum += currTime / prevTime;
                count++;
            }
        }

        return count > 0 ? sum / count : 0;
    }

    /**
     * Classify complexity based on growth ratio
     */
    private static String classifyComplexity(double ratio) {
        if (ratio < 1.5) {
            return "O(n) or better [Best Case]";
        } else if (ratio < 2.5) {
            return "Between O(n) and O(n²)";
        } else if (ratio < 5.0) {
            return "O(n²) [Quadratic Growth]";
        } else {
            return "Worse than O(n²)";
        }
    }

    /**
     * Benchmark with random array
     */
    private static double benchmarkRandom(int n, int runs, Config config) {
        Benchmark_Timer<Integer[]> benchmark = new Benchmark_Timer<>(
                "Random insertion sort",
                config,
                null,
                (array) -> InsertionSort.sort(array),
                null
        );

        return benchmark.runFromSupplier(() -> createRandomArray(n), runs);
    }

    /**
     * Benchmark with ordered array
     */
    private static double benchmarkOrdered(int n, int runs, Config config) {
        Benchmark_Timer<Integer[]> benchmark = new Benchmark_Timer<>(
                "Ordered insertion sort",
                config,
                null,
                (array) -> InsertionSort.sort(array),
                null
        );

        return benchmark.runFromSupplier(() -> createOrderedArray(n), runs);
    }

    /**
     * Benchmark with partially ordered array (first half sorted, second half random)
     */
    private static double benchmarkPartiallyOrdered(int n, int runs, Config config) {
        Benchmark_Timer<Integer[]> benchmark = new Benchmark_Timer<>(
                "Partially ordered insertion sort",
                config,
                null,
                (array) -> InsertionSort.sort(array),
                null
        );

        return benchmark.runFromSupplier(() -> createPartiallyOrderedArray(n), runs);
    }

    /**
     * Benchmark with reverse ordered array
     */
    private static double benchmarkReverseOrdered(int n, int runs, Config config) {
        Benchmark_Timer<Integer[]> benchmark = new Benchmark_Timer<>(
                "Reverse ordered insertion sort",
                config,
                null,
                (array) -> InsertionSort.sort(array),
                null
        );

        return benchmark.runFromSupplier(() -> createReverseOrderedArray(n), runs);
    }

    /**
     * Create a random array of integers
     */
    private static Integer[] createRandomArray(int n) {
        Random random = new Random();
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = random.nextInt(n * 10);
        }
        return array;
    }

    /**
     * Create an ordered array of integers
     */
    private static Integer[] createOrderedArray(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return array;
    }

    /**
     * Create a partially ordered array (first 50% sorted, rest random)
     */
    private static Integer[] createPartiallyOrderedArray(int n) {
        Random random = new Random();
        Integer[] array = new Integer[n];
        int half = n / 2;

        // First half ordered
        for (int i = 0; i < half; i++) {
            array[i] = i;
        }

        // Second half random
        for (int i = half; i < n; i++) {
            array[i] = random.nextInt(n * 10);
        }

        return array;
    }

    /**
     * Create a reverse ordered array of integers
     */
    private static Integer[] createReverseOrderedArray(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = n - i - 1;
        }
        return array;
    }
}