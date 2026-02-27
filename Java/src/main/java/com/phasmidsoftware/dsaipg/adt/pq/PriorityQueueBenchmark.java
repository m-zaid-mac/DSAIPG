package com.phasmidsoftware.dsaipg.adt.pq;

import com.phasmidsoftware.dsaipg.util.benchmark.Benchmark_Timer;
import com.phasmidsoftware.dsaipg.util.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Benchmark program for comparing different Priority Queue implementations.
 * Tests binary heap (2-ary), 4-ary heap, with and without Floyd's trick, and Fibonacci heap.
 */
public class PriorityQueueBenchmark {

    private static final int MAX_CAPACITY = 4095;  // M = 4095
    private static final int INSERTIONS = 16000;    // Insert 16,000 elements
    private static final int DELETIONS = 4000;      // Remove 4,000 elements
    private static final int RUNS = 10;             // Number of benchmark runs

    public static void main(String[] args) {
        System.out.println("Priority Queue Benchmark");
        System.out.println("========================");
        System.out.println("Configuration:");
        System.out.println("  Max capacity (M): " + MAX_CAPACITY);
        System.out.println("  Insertions: " + INSERTIONS);
        System.out.println("  Deletions: " + DELETIONS);
        System.out.println("  Runs per test: " + RUNS);
        System.out.println();

        try {
            Config config = Config.load();

            // 1. Basic binary heap (2-ary, no Floyd)
            System.out.println("1. Basic Binary Heap (2-ary, no Floyd):");
            System.out.println("-".repeat(50));
            benchmarkHeap("Binary Heap (basic)", config, 2, false, false);

            // 2. Binary heap with Floyd's trick
            System.out.println("\n2. Binary Heap with Floyd's Trick:");
            System.out.println("-".repeat(50));
            benchmarkHeap("Binary Heap (Floyd)", config, 2, true, false);

            // 3. 4-ary heap
            System.out.println("\n3. 4-ary Heap (no Floyd):");
            System.out.println("-".repeat(50));
            benchmarkHeap("4-ary Heap (basic)", config, 4, false, false);

            // 4. 4-ary heap with Floyd's trick
            System.out.println("\n4. 4-ary Heap with Floyd's Trick:");
            System.out.println("-".repeat(50));
            benchmarkHeap("4-ary Heap (Floyd)", config, 4, true, false);

            // 5. Fibonacci Heap (bonus)
            System.out.println("\n5. Fibonacci Heap (BONUS):");
            System.out.println("-".repeat(50));
            benchmarkHeap("Fibonacci Heap", config, 0, false, true);

        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Benchmark a specific heap configuration
     */
    private static void benchmarkHeap(String name, Config config, int arity, boolean useFloyd, boolean isFibonacci) {
        Benchmark_Timer<HeapTestData> benchmark = new Benchmark_Timer<>(
                name + " operations",
                config,
                null,  // no pre-function
                (data) -> {
                    // Create appropriate heap type
                    PriorityQueue<Integer> pq;

                    if (isFibonacci) {
                        pq = new PriorityQueue_FibonacciHeap<Integer>(MAX_CAPACITY, true, Comparator.<Integer>naturalOrder(), useFloyd);
                    } else {
                        // Use extended binary heap with configurable arity
                        pq = new PriorityQueue_BinaryHeap<Integer>(MAX_CAPACITY, 1, true, Comparator.<Integer>naturalOrder(), useFloyd, arity);
                    }

                    // Track spilled elements
                    data.spilledElements.clear();

                    // Insert 16,000 elements
                    for (int i = 0; i < INSERTIONS; i++) {
                        int value = data.randomValues[i];

                        // If heap is full, track what gets spilled
                        if (pq.size() >= MAX_CAPACITY) {
                            data.spilledElements.add(value);
                        }

                        pq.give(value);
                    }

                    // Remove 4,000 elements
                    try {
                        for (int i = 0; i < DELETIONS; i++) {
                            if (!pq.isEmpty()) {
                                pq.take();
                            }
                        }
                    } catch (PQException e) {
                        System.err.println("Error during deletion: " + e.getMessage());
                    }
                },
                null  // no post-function
        );

        // Run benchmark
        double avgTime = benchmark.runFromSupplier(() -> new HeapTestData(), RUNS);

        System.out.printf("Average time: %.3f ms%n", avgTime);

        // Report highest priority spilled element
        reportSpilledElements(arity, useFloyd, isFibonacci);
    }

    /**
     * Run one test to report spilled elements
     */
    private static void reportSpilledElements(int arity, boolean useFloyd, boolean isFibonacci) {
        HeapTestData finalData = new HeapTestData();
        PriorityQueue<Integer> pq;

        if (isFibonacci) {
            pq = new PriorityQueue_FibonacciHeap<Integer>(MAX_CAPACITY, true, Comparator.<Integer>naturalOrder(), useFloyd);
        } else {
            pq = new PriorityQueue_BinaryHeap<Integer>(MAX_CAPACITY, 1, true, Comparator.<Integer>naturalOrder(), useFloyd, arity);
        }

        try {
            for (int i = 0; i < INSERTIONS; i++) {
                int value = finalData.randomValues[i];
                if (pq.size() >= MAX_CAPACITY) {
                    finalData.spilledElements.add(value);
                }
                pq.give(value);
            }

            if (!finalData.spilledElements.isEmpty()) {
                int highestSpilled = finalData.spilledElements.stream()
                        .max(Comparator.naturalOrder())
                        .orElse(-1);
                System.out.printf("Elements spilled: %d%n", finalData.spilledElements.size());
                System.out.printf("Highest priority spilled element: %d%n", highestSpilled);
            } else {
                System.out.println("No elements spilled (all fit in heap)");
            }
        } catch (Exception e) {
            System.err.println("Error during spilled element analysis: " + e.getMessage());
        }
    }

    /**
     * Data structure to hold test data
     */
    static class HeapTestData {
        int[] randomValues;
        List<Integer> spilledElements;

        HeapTestData() {
            // Generate random values
            Random random = new Random(42);  // Fixed seed for reproducibility
            randomValues = new int[INSERTIONS];
            for (int i = 0; i < INSERTIONS; i++) {
                randomValues[i] = random.nextInt(100000);
            }
            spilledElements = new ArrayList<>();
        }
    }
}