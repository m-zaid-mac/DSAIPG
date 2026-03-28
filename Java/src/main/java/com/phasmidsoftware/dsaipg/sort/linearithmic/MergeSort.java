package com.phasmidsoftware.dsaipg.sort.linearithmic;

import com.phasmidsoftware.dsaipg.sort.elementary.InsertionSort;
import com.phasmidsoftware.dsaipg.sort.generic.SortException;
import com.phasmidsoftware.dsaipg.sort.generic.SortWithComparableHelper;
import com.phasmidsoftware.dsaipg.sort.helper.Helper;
import com.phasmidsoftware.dsaipg.util.config.Config;

import java.util.Arrays;

import static com.phasmidsoftware.dsaipg.util.config.Config_Benchmark.*;

/**
 * A generic implementation of the MergeSort algorithm for sorting elements of type X,
 * where X extends Comparable<X>. This class provides optimized sorting techniques such as
 * insurance and no-copy optimizations, offering scalable sorting solutions. It makes use of
 * a helper class for monitoring and performing additional utilities during the sorting process.
 *
 * @param <X> The type of elements to be sorted, which must implement the Comparable interface.
 */
public class MergeSort<X extends Comparable<X>> extends SortWithComparableHelper<X> {

    public static final String DESCRIPTION = "MergeSort";

    /**
     * Constructor for MergeSort
     * <p>
     * NOTE this is used only by unit tests, using its own instrumented helper.
     *
     * @param helper an explicit instance of Helper to be used.
     */
    public MergeSort(Helper<X> helper) {
        super(helper);
        insertionSort = setupInsertionSort(helper);
    }

    /**
     * Constructor for MergeSort
     *
     * @param N      the number elements we expect to sort.
     * @param nRuns  the expected number of runs.
     * @param config the configuration.
     */
    public MergeSort(int N, int nRuns, Config config) {
        super(DESCRIPTION + getConfigString(config), N, nRuns, config);
        insertionSort = setupInsertionSort(getHelper());
    }

    /**
     * Sorts the given array in-place or by creating a copy, depending on the parameter makeCopy.
     * This method initializes a sorting helper, allocates additional memory as necessary,
     * and performs the sort.
     *
     * @param xs       the array to be sorted
     * @param makeCopy if true, the array will be copied before sorting. Otherwise, sorting is done in-place.
     * @return the sorted array; either the modified original array (if makeCopy is false) or a new sorted array (if makeCopy is true)
     */
    public X[] sort(X[] xs, boolean makeCopy) {
        getHelper().init(xs.length);
        additionalMemory(xs.length);
        X[] result = makeCopy ? Arrays.copyOf(xs, xs.length) : xs;
        sort(result, 0, result.length);
        additionalMemory(-xs.length);
        return result;
    }

    /**
     * Sorts the specified portion of the array using the MergeSort algorithm.
     *
     * @param a    the array to be sorted
     * @param from the starting index of the range to sort, inclusive
     * @param to   the ending index of the range to sort, exclusive
     */
    public void sort(X[] a, int from, int to) {
        Config config = helper.getConfig();
        boolean noCopy = config.getBoolean(MERGESORT, NOCOPY);
        @SuppressWarnings("unchecked") X[] aux = noCopy ? helper.copyArray(a) : (X[]) new Comparable[a.length];
        sort(a, aux, from, to);
    }

    /**
     * Sets the memory for the array if it hasn't been set previously.
     * If `arrayMemory` has not been initialized (i.e., equals -1), it sets its value to `n`.
     * Additionally, allocates and updates additional memory using the `additionalMemory` method.
     *
     * @param n the amount of memory to be set for the array.
     */
    public void setArrayMemory(int n) {
        if (arrayMemory == -1) {
            arrayMemory = n;
            additionalMemory(n);
        }
    }

    /**
     * Updates the value of additionalMemory by adding the provided amount and adjusts maxMemory if necessary.
     *
     * @param n the amount of memory to be added to additionalMemory.
     */
    public void additionalMemory(int n) {
        additionalMemory += n;
        if (maxMemory < additionalMemory) maxMemory = additionalMemory;
    }

    /**
     * Computes the memory factor, which represents the ratio of the maximum memory available
     * to the array memory size. This calculation helps in determining the efficiency or feasibility
     * of operations concerning memory usage.
     *
     * @return the memory factor as a Double.
     * Throws a SortException if the array memory has not been set (i.e., arrayMemory == -1).
     */
    public Double getMemoryFactor() {
        if (arrayMemory == -1)
            throw new SortException("Array memory has not been set");
        return 1.0 * maxMemory / arrayMemory;
    }

    /**
     * Sets up an instance of InsertionSort using a cloned helper with a specific description.
     *
     * @param helper an instance of Helper to be cloned and used by the InsertionSort instance.
     * @return an instance of InsertionSort configured with the cloned helper.
     */
    private InsertionSort<X> setupInsertionSort(final Helper<X> helper) {
        return new InsertionSort<>(helper.clone("MergeSort: insertion sort"));
    }

    /**
     * Sorts the given range of the array using merge sort with optional optimizations for insurance and no-copy.
     *
     * When noCopy is true, both primary and secondary hold identical data for the range [from, to)
     * before each recursive call. We sort the secondary array and merge results back into primary,
     * avoiding a final copy-back at every level.
     *
     * When noCopy is false (standard path), we sort primary recursively, merge into secondary,
     * then copy secondary back into primary.
     *
     * The insurance optimization skips the merge entirely when the two halves are already in
     * sorted order relative to each other (i.e., primary[mid-1] <= primary[mid]).
     *
     * @param primary    the primary (output) array for this recursive level.
     * @param secondary  the auxiliary array used as source when noCopy is true, or as merge target otherwise.
     * @param from       the starting index (inclusive) of the range to be sorted.
     * @param to         the ending index (exclusive) of the range to be sorted.
     */
    private void sort(X[] primary, X[] secondary, int from, int to) {
        Config config = helper.getConfig();
        // NOTE: test noCopy before insurance as recommended in the TODO comment
        boolean noCopy = config.getBoolean(MERGESORT, NOCOPY);
        boolean insurance = config.getBoolean(MERGESORT, INSURANCE);

        // Verify invariant: when noCopy, both arrays must hold the same data in [from, to)
        assert !noCopy || Arrays.compare(primary, from, to, secondary, from, to) == 0
                : "MergeSort::sort: partitions are not the same";

        // Base case: for small partitions, insertion sort is faster
        // A cutoff of 1 means every partition falls through to insertion sort immediately (no recursion benefit)
        if (to <= from + helper.cutoff()) {
            insertionSort.sort(primary, from, to);
            return;
        }

        int mid = from + (to - from) / 2;

        if (noCopy) {
            // No-copy path:
            // Roles are swapped: we recursively sort into secondary (using primary as its aux),
            // so that after recursion, secondary[from..mid) and secondary[mid..to) are sorted.
            // Then we merge secondary -> primary directly, with no copy-back needed.
            sort(secondary, primary, from, mid);
            sort(secondary, primary, mid, to);

            // Insurance check: if secondary[mid-1] <= secondary[mid], the range is already sorted.
            // Just copy the block from secondary into primary and return.
            if (insurance && helper.compare(secondary, mid - 1, mid) < 0) {
                helper.copyBlock(secondary, from, primary, from, to - from);
                return;
            }
            merge(secondary, primary, from, mid, to);

        } else {
            // Standard path:
            // Recursively sort both halves of primary in place.
            sort(primary, secondary, from, mid);
            sort(primary, secondary, mid, to);

            // Insurance check: if primary[mid-1] <= primary[mid], both halves are already in order.
            // No merge needed; primary is already sorted in this range.
            if (insurance && helper.compare(primary, mid - 1, mid) < 0) {
                return;
            }
            // Merge sorted halves from primary into secondary, then copy back.
            merge(primary, secondary, from, mid, to);
            helper.copyBlock(secondary, from, primary, from, to - from);
        }
    }

    /**
     * Merges two sorted partitions (subarrays) into a combined sorted partition (subarray).
     * The first partition is defined as [from, mid) and the second partition as [mid, to).
     * Elements from these partitions are copied into the result array in sorted order.
     * CONSIDER combine with MergeSortBasic, perhaps.
     *
     * @param sorted The source array containing the two sorted partitions to be merged.
     * @param result The destination array where the merged results will be stored.
     * @param from   The starting index (inclusive) of the first partition.
     * @param mid    The ending index (exclusive) of the first partition and the starting index of the second partition.
     * @param to     The ending index (exclusive) of the second partition.
     */
    private void merge(X[] sorted, X[] result, int from, int mid, int to) {
        int i = from;
        int j = mid;
        X v = helper.get(sorted, i);
        X w = helper.get(sorted, j);
        for (int k = from; k < to; k++) {
            if (i >= mid) {
                helper.copy(w, result, k);
                if (++j < to) w = helper.get(sorted, j);
            } else if (j >= to) {
                helper.copy(v, result, k);
                if (++i < mid) v = helper.get(sorted, i);
            } else if (helper.notInverted(w, v)) {
                helper.incrementFixes(mid - i);
                helper.copy(w, result, k);
                if (++j < to) w = helper.get(sorted, j);
            } else {
                helper.copy(v, result, k);
                if (++i < mid) v = helper.get(sorted, i);
            }
        }
    }

    public static final String MERGESORT = "mergesort";
    public static final String NOCOPY = "nocopy";
    public static final String INSURANCE = "insurance";

    /**
     * Builds a configuration string based on the provided configuration settings.
     * The configuration string describes certain properties such as whether
     * insurance comparison, no-copy, or specific cutoff values are enabled.
     *
     * @param config the configuration object used to determine the settings for the string.
     * @return a string representing the configuration settings.
     */
    private static String getConfigString(Config config) {
        StringBuilder stringBuilder = new StringBuilder();
        if (config.getBoolean(MERGESORT, INSURANCE)) stringBuilder.append(" with insurance comparison");
        if (config.getBoolean(MERGESORT, NOCOPY)) stringBuilder.append(" with no copy");
        int cutoff = config.getInt(HELPER, CUTOFF, CUTOFF_DEFAULT);
        if (cutoff != CUTOFF_DEFAULT) {
            if (cutoff == 1) stringBuilder.append(" with no cutoff");
            else stringBuilder.append(" with cutoff ").append(cutoff);
        }
        return stringBuilder.toString();
    }

    private final InsertionSort<X> insertionSort;
    private int arrayMemory = -1;
    private int additionalMemory;
    private int maxMemory;
}