package com.phasmidsoftware.dsaipg.sort.linearithmic;

import com.phasmidsoftware.dsaipg.sort.generic.SortException;
import com.phasmidsoftware.dsaipg.sort.helper.Helper;
import com.phasmidsoftware.dsaipg.util.config.Config;

import java.util.ArrayList;
import java.util.List;

import static com.phasmidsoftware.dsaipg.sort.helper.InstrumentedComparatorHelper.getRunsConfig;

/**
 * Class QuickSort_DualPivot which extends QuickSort.
 * <p>
 * Dual-pivot QuickSort uses two pivot values (v1 and v2, where v1 <= v2) to partition the array
 * into three regions:
 *   [from, lt)  — elements less than v1
 *   [lt+1, gt)  — elements between v1 and v2 (inclusive)
 *   [gt+1, to)  — elements greater than v2
 * <p>
 * This typically reduces comparisons compared to classic single-pivot QuickSort because elements
 * between the two pivots are placed correctly without needing to recurse over the full range.
 * Java's Arrays.sort() uses this algorithm for primitive arrays.
 *
 * @param <X> the underlying comparable type.
 */
public class QuickSort_DualPivot<X extends Comparable<X>> extends QuickSort<X> {

    public static final String DESCRIPTION = "QuickSort dual pivot";

    /**
     * Creates and returns a Dual Pivot partitioner for use in sorting algorithms.
     * The returned partitioner leverages the dual-pivot partitioning strategy for dividing
     * the input data into multiple smaller partitions, which is commonly used in optimized quicksort implementations.
     *
     * @return a Partitioner instance using a dual-pivot strategy initialized with the associated helper.
     */
    public Partitioner<X> createPartitioner() {
        return new Partitioner_DualPivot(getHelper());
    }

    /**
     * Constructor for QuickSort_DualPivot.
     *
     * @param description a short description of the sorting instance.
     * @param N           the number of elements expected to be sorted.
     * @param nRuns       the number of times the sort operation will run for benchmarking or testing.
     * @param config      the configuration settings for the sorting process.
     */
    public QuickSort_DualPivot(String description, int N, int nRuns, Config config) {
        super(description, N, nRuns, config);
        setPartitioner(createPartitioner());
    }

    /**
     * Constructor for QuickSort_DualPivot.
     *
     * @param helper an explicit instance of Helper to be used.
     */
    public QuickSort_DualPivot(Helper<X> helper) {
        super(helper);
        setPartitioner(createPartitioner());
    }

    /**
     * Constructor for QuickSort_DualPivot.
     *
     * @param N      the number elements we expect to sort.
     * @param nRuns  the number of runs.
     * @param config the configuration.
     */
    public QuickSort_DualPivot(int N, int nRuns, Config config) {
        this(DESCRIPTION, N, nRuns, config);
    }

    /**
     * Constructor for QuickSort_DualPivot.
     *
     * @param N      the number elements we expect to sort.
     * @param config the configuration.
     */
    public QuickSort_DualPivot(int N, Config config) {
        this(DESCRIPTION, N, getRunsConfig(config), config);
    }

    /**
     * This class implements a dual-pivot partitioning strategy for use in sorting algorithms, such as quicksort.
     * Dual-pivot partitioning is an optimization of the classic quicksort partitioning approach, utilizing two pivots
     * to divide the input data into three distinct regions for more efficient sorting.
     * <p>
     * Partitioning steps:
     * 1. Ensure xs[p1] <= xs[p2] (swap if needed) — these become v1 and v2.
     * 2. Maintain three pointers: lt (left boundary), gt (right boundary), i (current element).
     * 3. For each element x at position i:
     *    - If x < v1: swap with xs[lt] and advance both lt and i.
     *    - If x > v2: swap with xs[gt] and retreat gt (do NOT advance i, new element at i is unknown).
     *    - Otherwise: advance i (element is in the middle region).
     * 4. Place pivots at their final positions: swap v1 into lt-1, v2 into gt+1.
     * 5. Return three sub-partitions: [p1, lt), [lt+1, gt), [gt+1, p2+1].
     */
    public class Partitioner_DualPivot implements Partitioner<X> {

        /**
         * Constructor for Partitioner_DualPivot.
         *
         * @param helper a Helper instance that provides utility methods and support for the partitioning process.
         */
        public Partitioner_DualPivot(Helper<X> helper) {
            this.helper = helper;
        }

        /**
         * Method to partition the given partition into three smaller partitions using dual-pivot strategy.
         *
         * @param partition the partition to divide up. Must have at least 3 elements.
         * @return a list of three partitions: less-than-v1, between-v1-and-v2, greater-than-v2.
         * @throws SortException if the partition has fewer than 3 elements.
         */
        public List<Partition<X>> partition(Partition<X> partition) {
            int n = partition.to - partition.from;
            if (n < 3) throw new SortException("cannot use DualPivot partitioning when size is less than 3");
            final X[] xs = partition.xs;
            final int p1 = partition.from;
            final int p2 = partition.to - 1;

            // Ensure the left pivot <= right pivot, then capture pivot values.
            // We read them as plain array accesses (no hit counted) because these
            // are pivot setup reads, not data-comparison reads.
            helper.swapConditional(xs, p1, p2);
            final X v1 = xs[p1]; // left pivot value — fixed for the entire loop
            final X v2 = xs[p2]; // right pivot value — fixed for the entire loop
            int lt = p1 + 1;
            int gt = p2 - 1;
            int i = lt;

            // Single unified loop. Compare each xs[i] against the captured pivot values
            // using helper.compare(xs[i], v1) / helper.compare(xs[i], v2) — these count
            // one hit for xs[i] per comparison (the pivot values are already loaded).
            // helper.swap counts hits and fixes correctly for both instrumented and plain helpers.
            if (helper.instrumented()) {
                // NOTE: we are trying to avoid checking on instrumented for every time in the inner loop
                // for performance reasons (probably a silly idea).
                // NOTE: if we were using Scala, it would be easy to set up a comparer function and a
                // swapper function. With java, it's possible but much messier.
                X xlt = helper.get(xs, lt);
                X xgt = helper.get(xs, gt);
                X x = xs[i]; // no hit since i = lt
                while (i <= gt) {
                    // Each time around the loop, we invoke: 2, 1, or 1 hits; 1, 2, or 2 lookups
                    if (helper.compare(x, v1) < 0) { // no hits, one lookup
                        helper.swapVW(xlt, x, xs, lt++, i++); // no hits or lookups
                        x = helper.get(xs, i);   // one hit
                        xlt = helper.get(xs, lt); // one hit (CONSIDER is this correct?)
                        if (i == gt) xgt = x;
                    } else if (helper.compare(x, v2) > 0) { // no hits, one lookup (but it's already in cache)
                        helper.swapVW(x, xgt, xs, i, gt--); // no hits or lookups
                        if (i == lt) xlt = xgt;
                        x = xgt;
                        xgt = helper.get(xs, gt); // one hit
                    } else {
                        i++;
                        x = helper.get(xs, i); // one hit
                    }
                }
                if (p1 != lt - 1) helper.swap(xs, p1, --lt); else --lt;
                if (p2 != gt + 1) helper.swap(xs, p2, ++gt); else ++gt;
            } else {
                // Non-instrumented path — plain comparisons and swaps.
                while (i <= gt) {
                    X x = xs[i];
                    if (x.compareTo(v1) < 0) {
                        if (lt != i) { X t = xs[lt]; xs[lt] = xs[i]; xs[i] = t; }
                        lt++; i++;
                    } else if (x.compareTo(v2) > 0) {
                        if (i != gt) { X t = xs[i]; xs[i] = xs[gt]; xs[gt] = t; }
                        gt--;
                    } else i++;
                }
                if (p1 != lt - 1) { X t = xs[p1]; xs[p1] = xs[--lt]; xs[lt] = t; } else --lt;
                if (p2 != gt + 1) { X t = xs[p2]; xs[p2] = xs[++gt]; xs[gt] = t; } else ++gt;
            }

            List<Partition<X>> partitions = new ArrayList<>();
            partitions.add(new Partition<>(xs, p1, lt));
            partitions.add(new Partition<>(xs, lt + 1, gt));
            partitions.add(new Partition<>(xs, gt + 1, p2 + 1));
            return partitions;
        }

        private final Helper<X> helper;
    }
}