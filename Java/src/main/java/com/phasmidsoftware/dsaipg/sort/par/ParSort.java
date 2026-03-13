package com.phasmidsoftware.dsaipg.sort.par;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * ParSort implements a parallel merge-sort for integer arrays.
 *
 * Two separate schemes control when to parallelize a sub-problem:
 *   1. Cutoff: if the sub-array has fewer elements than cutoff, fall back to Arrays.sort.
 *   2. Depth limit: once the recursion depth reaches maxDepth, stop spawning async tasks.
 *      Setting maxDepth = Integer.MAX_VALUE disables this check (cutoff-only mode).
 */
final class ParSort {

    /**
     * Sub-array size threshold below which we fall back to sequential Arrays.sort.
     * Default: 1,000.
     */
    public static int cutoff = 1000;

    /**
     * Maximum recursion depth at which we are still allowed to spawn parallel tasks.
     * Default: Integer.MAX_VALUE (no depth limit – pure cutoff-only mode).
     * Set to (int)(Math.log(availableProcessors) / Math.log(2)) for depth-based mode.
     */
    public static int maxDepth = Integer.MAX_VALUE;

    /**
     * Sorts array[from..to) in place using a parallel merge-sort.
     *
     * @param array the array to be sorted
     * @param from  starting index (inclusive)
     * @param to    ending index (exclusive)
     */
    public static void sort(int[] array, int from, int to) {
        sort(array, from, to, 0);
    }

    /**
     * Extracts array[from..to), recursively sorts that slice, and returns the result
     * as a new array. The original array is never modified.
     */
    static int[] sortRecursive(int[] array, int from, int to) {
        return sortRecursive(array, from, to, 0);
    }

    /**
     * Merges two sorted arrays into a single sorted array.
     *
     * @param xs1 first sorted array
     * @param xs2 second sorted array
     * @return merged, sorted array
     */
    static int[] doMerge(int[] xs1, int[] xs2) {
        int[] result = new int[xs1.length + xs2.length];
        int i = 0;
        int j = 0;
        for (int k = 0; k < result.length; k++) {
            if (i >= xs1.length) result[k] = xs2[j++];
            else if (j >= xs2.length) result[k] = xs1[i++];
            else if (xs2[j] < xs1[i]) result[k] = xs2[j++];
            else result[k] = xs1[i++];
        }
        return result;
    }

    /**
     * Returns a CompletableFuture that asynchronously sorts array[from..to)
     * using the depth-0 overload.
     */
    static CompletableFuture<int[]> asyncSort(int[] array, int from, int to) {
        return asyncSort(array, from, to, 0);
    }

    // -------------------------------------------------------------------------
    // Private depth-aware implementation
    // -------------------------------------------------------------------------

    /**
     * Core recursive-parallel sort. Parallelises only when both conditions hold:
     *   - the slice is at least cutoff elements long, AND
     *   - the current recursion depth has not yet reached maxDepth.
     * When either condition fails we simply call Arrays.sort on the slice.
     */
    private static void sort(int[] array, int from, int to, int depth) {
        if (to - from >= cutoff && depth < maxDepth) {
            int mid = from + (to - from) / 2;
            CompletableFuture<int[]> completableFuture1 = asyncSort(array, from, mid, depth + 1);
            CompletableFuture<int[]> completableFuture2 = asyncSort(array, mid, to, depth + 1);
            CompletableFuture<int[]> completableFuture =
                    completableFuture1.thenCombine(completableFuture2, ParSort::doMerge);
            completableFuture.whenComplete(
                    (result, throwable) -> System.arraycopy(result, 0, array, from, result.length));
            completableFuture.join();
        } else {
            Arrays.sort(array, from, to);
        }
    }

    private static int[] sortRecursive(int[] array, int from, int to, int depth) {
        int[] result = new int[to - from];
        System.arraycopy(array, from, result, 0, to - from);
        sort(result, 0, result.length, depth);
        return result;
    }

    private static CompletableFuture<int[]> asyncSort(int[] array, int from, int to, int depth) {
        return CompletableFuture.supplyAsync(
                () -> sortRecursive(array, from, to, depth)
        );
    }
}