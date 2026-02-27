/*
 * Copyright (c) 2018. Phasmid Software
 */
package com.phasmidsoftware.dsaipg.adt.pq;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Priority Queue Data Structure which uses a binary heap.
 * Extended to support both 2-ary (binary) and 4-ary (quaternary) heaps.
 *
 * @param <K>
 */
public class PriorityQueue_BinaryHeap<K> implements PriorityQueue<K>, Iterable<K> {

    /**
     * @return true if the current size is zero.
     */
    public boolean isEmpty() {
        return m == 0;
    }

    /**
     * @return the number of elements actually stored in this Priority Queue
     */
    public int size() {
        return m;
    }

    /**
     * Adds a key to the priority queue. If the priority queue is at its capacity,
     * the last element is removed to make space for the new key.
     */
    public void give(K key) {
        if (m == binHeap.length - first) m--;
        binHeap[++m + first - 1] = key;
        swimUp(m + first - 1);
    }

    /**
     * Remove the root element from this Priority Queue and adjust the binary heap accordingly.
     */
    public K take() throws PQException {
        if (isEmpty()) throw new PQException("Priority queue is empty");
        return doTake(floyd ? this::snake : this::sink);
    }

    /**
     * Package-private method to remove the root element from the priority queue.
     */
    K doTake(Consumer<Integer> f) {
        K result = binHeap[first];
        swap(first, m-- + first - 1);
        f.accept(first);
        binHeap[m + first] = null;
        return result;
    }

    /**
     * Sink the element at index k down
     */
    void sink(@SuppressWarnings("SameParameterValue") int k) {
        doHeapifyStandard(k);
    }

    /**
     * Special sink method that sinks the element and then swim the element back
     */
    void snake(@SuppressWarnings("SameParameterValue") int k) {
        swimUp(doHeapify(k, (a, b) -> false));
    }

    /**
     * Swim the element at index k up
     */
    void swimUp(int k) {
        int i = k;
        while (i > first && inverted(parent(i), i)) {
            swap(i, parent(i));
            i = parent(i);
        }
    }

    /**
     * Initializes or reconstructs the binary heap to satisfy the heap property for all elements.
     */
    public void heapConstructor() {
        for (int k = parent(m + first); k >= first; k--) sink(k);
    }

    /**
     * Compare the elements at indices i and j.
     */
    boolean inverted(int i, int j) {
        return (comparator.compare(binHeap[i], binHeap[j]) > 0) ^ max;
    }

    /**
     * Non-mutating iterator over all values of this PriorityQueue_BinaryHeap.
     */
    @NotNull
    public Iterator<K> iterator() {
        Collection<K> copy = new ArrayList<>(Arrays.asList(Arrays.copyOf(binHeap, m + first)));
        Iterator<K> result = copy.iterator();
        if (first > 0) result.next();
        return result;
    }

    /**
     * Primary constructor that takes the max value, an actual array of elements, and a comparator.
     */
    public PriorityQueue_BinaryHeap(boolean max, Object[] binHeap, int first, int m, Comparator<K> comparator, boolean floyd, int arity) {
        this.max = max;
        this.first = first;
        this.comparator = comparator;
        this.m = m;
        //noinspection unchecked
        this.binHeap = (K[]) binHeap;
        this.floyd = floyd;
        this.arity = arity;
    }

    /**
     * Constructor without arity - defaults to binary heap (arity = 2)
     */
    public PriorityQueue_BinaryHeap(boolean max, Object[] binHeap, int first, int m, Comparator<K> comparator, boolean floyd) {
        this(max, binHeap, first, m, comparator, floyd, 2);
    }

    /**
     * Secondary constructor which takes only the priority queue's maximum capacity and a comparator
     */
    public PriorityQueue_BinaryHeap(int n, int first, boolean max, Comparator<K> comparator, boolean floyd, int arity) {
        this(max, new Object[n + first], first, 0, comparator, floyd, arity);
    }

    public PriorityQueue_BinaryHeap(int n, int first, boolean max, Comparator<K> comparator, boolean floyd) {
        this(n, first, max, comparator, floyd, 2);  // Default to binary (arity=2)
    }

    public PriorityQueue_BinaryHeap(int n, boolean max, Comparator<K> comparator, boolean floyd) {
        this(n, 1, max, comparator, floyd, 2);
    }

    public PriorityQueue_BinaryHeap(int n, boolean max, Comparator<K> comparator) {
        this(n, max, comparator, false);
    }

    public PriorityQueue_BinaryHeap(int n, Comparator<K> comparator) {
        this(n, 0, true, comparator, true, 2);
    }

    public PriorityQueue_BinaryHeap(Collection<K> ks, Comparator<K> comparator) {
        this(ks.size(), comparator);
        int i = 0;
        for (K k : ks) binHeap[i++] = k;
        m = ks.size();
        heapConstructor();
    }

    /**
     * Adjusts a subtree rooted at index k to ensure it satisfies the heap property.
     */
    private int doHeapify(int k, BiPredicate<Integer, Integer> p) {
        int i = k;
        while (true) {
            int firstChildIdx = firstChild(i);
            if (!(firstChildIdx <= m + first - 1)) break;

            int bestChild = firstChildIdx;

            // Optimized logic for binary heap (arity = 2)
            if (arity == 2) {
                // For binary heap, only check if second child exists and is better
                if (firstChildIdx < m + first - 1 && inverted(bestChild, firstChildIdx + 1)) {
                    bestChild = firstChildIdx + 1;
                }
            }
            // General logic for higher arity heaps
            else {
                // Find the best child among all children
                for (int c = firstChildIdx + 1; c < Math.min(firstChildIdx + arity, m + first); c++) {
                    if (inverted(bestChild, c)) {
                        bestChild = c;
                    }
                }
            }

            if (p.test(i, bestChild)) break;
            swap(i, bestChild);
            i = bestChild;
        }
        return i;
    }

    private int doHeapifyStandard(int k) {
        return doHeapify(k, (a, b) -> !inverted(a, b));
    }

    /**
     * Exchange the values at indices i and j
     */
    private void swap(int i, int j) {
        K tmp = binHeap[i];
        binHeap[i] = binHeap[j];
        binHeap[j] = tmp;
    }

    /**
     * Get the index of the parent of the element at index k
     * Works for both binary (arity=2) and d-ary heaps
     */
    private int parent(int k) {
        if (arity == 2) {
            return (k + 1 - first) / 2 + first - 1;
        } else {
            // For d-ary heap: parent(k) = floor((k - first + d - 1) / d) + first - 1
            return (k - first + arity - 1) / arity + first - 1;
        }
    }

    /**
     * Get the index of the first child of the element at index k.
     * Works for both binary (arity=2) and d-ary heaps
     */
    private int firstChild(int k) {
        if (arity == 2) {
            return (k + 1 - first) * 2 + first - 1;
        } else {
            // For d-ary heap: firstChild(k) = d * (k - first + 1) + first - (d - 1)
            return arity * (k - first + 1) + first - (arity - 1);
        }
    }

    public K peek(int k) {
        return binHeap[k];
    }

    public boolean getMax() {
        return max;
    }

    private final boolean max;
    private final int first;
    private final Comparator<K> comparator;
    private final K[] binHeap;
    private int m;
    private final boolean floyd;
    private final int arity;  // NEW: 2 for binary, 4 for quaternary, etc.
}