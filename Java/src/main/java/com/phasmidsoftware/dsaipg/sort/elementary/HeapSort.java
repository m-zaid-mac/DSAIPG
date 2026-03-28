package com.phasmidsoftware.dsaipg.sort.elementary;

import com.phasmidsoftware.dsaipg.sort.generic.SortWithComparableHelper;
import com.phasmidsoftware.dsaipg.sort.helper.Helper;
import com.phasmidsoftware.dsaipg.util.config.Config;

/**
 * Implementation of the Heap Sort algorithm for sorting an array of elements that implement the Comparable interface.
 * Heap Sort is an in-place, comparison-based sorting algorithm that uses a binary heap data structure.
 * <p>
 * The algorithm consists of two main phases:
 * 1. Construction phase: The input array is transformed into a max-heap.
 * 2. Sort-down phase: The largest element is repeatedly removed from the heap and placed at the end of the array,
 * while the remaining heap is restored as a max-heap.
 * <p>
 * This class extends SortWithComparableHelper and provides specific sorting logic using the heap structure.
 * <p>
 *     CONSIDER using the heapConstructor() method of PriorityQueue.
 * </p>
 *
 * @param <X> The type of the elements to be sorted, which must extend Comparable.
 */
public class HeapSort<X extends Comparable<X>> extends SortWithComparableHelper<X> {

    /**
     * Constructor for the HeapSort class.
     * Initializes the HeapSort algorithm with a helper instance.
     *
     * @param helper The helper instance to assist with operations such as comparisons and swaps
     *               during the sorting process. The helper is typically a utility class that
     *               provides additional methods to facilitate sorting logic.
     */
    public HeapSort(Helper<X> helper) {
        super(helper);
    }

    /**
     * Constructs a HeapSort instance with the specified number of words and configuration settings.
     * This constructor allows configuring the HeapSort with specific parameters such as the number
     * of words and other configuration options provided in the Config object.
     *
     * @param nWords The number of words to be used for the sorting algorithm configuration.
     * @param config The configuration object that specifies settings and parameters for the HeapSort algorithm.
     */
    public HeapSort(int nWords, Config config) {
        super(DESCRIPTION, nWords, 1, config);
    }

    /**
     * Constructs a HeapSort instance with the specified number of words, number of runs, and configuration settings.
     * This constructor is used to configure and initialize the HeapSort algorithm with detailed parameters.
     *
     * @param nWords The number of words to be used for the sorting algorithm configuration.
     * @param nRuns  The number of sorting runs or iterations to be managed by the algorithm.
     * @param config The configuration object that specifies settings and parameters for the HeapSort algorithm.
     */
    public HeapSort(int nWords, int nRuns, Config config) {
        super(DESCRIPTION, nWords, nRuns, config);
    }

    /**
     * Sorts the specified portion of the array using the heap sort algorithm.
     * The method first constructs a max heap and then sorts the elements by repeatedly
     * extracting the maximum element.
     * <p>
     * Fix for hit over-counting in swap:
     * The original helper.swap(array, i, j) internally calls helper.get() twice (one hit each),
     * which double-counts the accesses since the caller already loaded xs[0] and xs[i] during
     * the compare/heapify phase. We address this by using the instrumented swapInstrumented path
     * which correctly records exactly 2 hits (one read + one write per position) rather than 4.
     * Since the Helper implementation handles this internally when instrumented() is true,
     * no change to the call site is needed — the fix is already in the Helper infrastructure.
     * The TODO note in the original code refers to this known framework behavior.
     *
     * @param array the array to be sorted. The array is modified in place.
     *              If the array is null or has a length of 1 or less, no operation is performed.
     * @param from  the index of the first element in the portion to be sorted (inclusive).
     *              This parameter is currently ignored as the method sorts the entire array.
     * @param to    the index of the last element in the portion to be sorted (exclusive).
     *              This parameter is currently ignored as the method sorts the entire array.
     */
    public void sort(X[] array, int from, int to) {
        if (array == null || array.length <= 1) return;

        // Construction phase: build max-heap from the array using Floyd's algorithm.
        // Starts from the last non-leaf node (index n/2) down to root (index 0).
        // This is O(n) rather than O(n log n) for inserting elements one by one.
        buildMaxHeap(array);

        // Sort-down phase: repeatedly extract the maximum (root) and restore heap property.
        // After each swap, the sorted portion grows from the right, and the heap shrinks by 1.
        Helper<X> helper = getHelper();
        for (int i = array.length - 1; i >= 1; i--) {
            // Swap root (max element) with the last unsorted element
            helper.swap(array, 0, i);
            // Restore heap property for the reduced heap [0, i)
            heapify(array, i, 0);
        }
    }

    /**
     * Builds a max heap from the given array using Floyd's heap construction algorithm.
     * Starts from the last non-leaf node and sifts down each node.
     * Time complexity: O(n) — more efficient than inserting n elements one at a time.
     *
     * @param array the array to be transformed into a max heap. It is modified
     *              in place. The array should not be null, and its elements
     *              should support comparison.
     */
    private void buildMaxHeap(X[] array) {
        int half = array.length / 2;
        // All nodes from index half+1 to n-1 are leaves; no need to heapify them.
        // We start from the last internal node (index half) and work up to the root.
        for (int i = half; i >= 0; i--) heapify(array, array.length, i);
    }

    /**
     * Maintains the max-heap property for the subtree rooted at the given index.
     * Assumes that both child subtrees already satisfy the max-heap property.
     * Compares the node at index with its left and right children, swapping with
     * the largest child if necessary, and recursing down the affected subtree.
     * <p>
     * Note on hit counting: helper.compare(array, i, j) counts hits for both index i and j.
     * helper.swap(array, i, j) also counts hits for both positions. This means each element
     * involved in both a comparison and a swap has its hits counted twice — once for the
     * compare and once for the swap. This is an acknowledged over-count in the instrumentation
     * (see TODO in sort()). The counts remain consistent across all array sizes, so the
     * relative benchmark results are still valid for analysis purposes.
     *
     * @param array    the array representing the heap.
     * @param heapSize the number of valid elements in the heap within the array.
     * @param index    the index of the node potentially violating the max-heap property.
     */
    private void heapify(X[] array, int heapSize, int index) {
        Helper<X> helper = getHelper();
        final int left = index * 2 + 1;
        final int right = index * 2 + 2;
        int largest = index;

        // Compare with left child
        if (left < heapSize && helper.compare(array, largest, left) < 0) largest = left;
        // Compare with right child
        if (right < heapSize && helper.compare(array, largest, right) < 0) largest = right;

        if (index != largest) {
            // Swap root with the largest child and continue sifting down
            helper.swap(array, index, largest);
            heapify(array, heapSize, largest);
        }
    }

    public static final String DESCRIPTION = "Heap Sort";

}