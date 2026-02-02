package com.phasmidsoftware.dsaipg.adt.threesum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of ThreeSum which follows the simple optimization of
 * requiring a sorted array, then using binary search to find an element x where
 * -x the sum of a pair of elements.
 * <p>
 * The array provided in the constructor MUST be ordered.
 * <p>
 * This algorithm runs in O(N^2 log N) time.
 */
class ThreeSumQuadrithmic implements ThreeSum {
    /**
     * Construct a ThreeSumQuadrithmic on a.
     *
     * @param a a sorted array.
     */
    public ThreeSumQuadrithmic(int[] a) {
        this.a = a;
        length = a.length;
    }

    /**
     * Retrieves an array of unique, sorted triples that represent combinations
     * of three integers in the underlying sorted array whose sum equals zero.
     * <p>
     * This method iterates over all pairs of elements in the array, uses a helper
     * method to find the third element that satisfies the condition, and returns
     * all such combinations as distinct {@code Triple} objects sorted in natural order.
     *
     * @return an array of unique {@code Triple} objects representing all valid combinations
     * of three integers in the array that sum to zero.
     */
    public Triple[] getTriples() {
        List<Triple> triples = new ArrayList<>();
        for (int i = 0; i < length - 2; i++) {
            for (int j = i + 1; j < length; j++) {
                Triple triple = getTriple(i, j);
                if (triple != null) triples.add(triple);
            }
        }
        Collections.sort(triples);
        return triples.stream().distinct().toArray(Triple[]::new);
    }

    /**
     * Finds a "triple" consisting of three integers from the sorted array such that their sum equals zero,
     * given two indices representing the first two elements of the triple.
     * Uses binary search to find the third element k such that a[i] + a[j] + a[k] = 0.
     * This means finding k where a[k] = -(a[i] + a[j]).
     *
     * @param i the index of the first element in the triple.
     * @param j the index of the second element in the triple. Must satisfy j > i.
     * @return a {@code Triple} object representing the three integers if such a combination exists,
     * or {@code null} if no such triple can be found.
     */
    Triple getTriple(int i, int j) {
        // We need to find k such that a[i] + a[j] + a[k] = 0
        // This means a[k] = -(a[i] + a[j])
        int target = -(a[i] + a[j]);

        // Use binary search to find target in the sorted array
        int k = Arrays.binarySearch(a, target);

        // If found (k >= 0) and k is different from i and j, AND k > j (to prevent duplicates)
        // we have a valid triple
        if (k >= 0 && k != i && k != j && k > j) {
            return new Triple(a[i], a[j], a[k]);
        }

        return null;
    }

    private final int[] a;
    private final int length;
}