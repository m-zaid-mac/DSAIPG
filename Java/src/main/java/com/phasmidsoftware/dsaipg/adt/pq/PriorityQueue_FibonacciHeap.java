package com.phasmidsoftware.dsaipg.adt.pq;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Fibonacci Heap implementation of Priority Queue.
 * Provides O(1) amortized insert and O(log n) amortized delete-min.
 *
 * @param <K> the type of elements in the priority queue
 */
public class PriorityQueue_FibonacciHeap<K> implements PriorityQueue<K>, Iterable<K> {

    /**
     * Node class for Fibonacci Heap
     */
    private static class Node<K> {
        K key;
        Node<K> parent;
        Node<K> child;
        Node<K> left;
        Node<K> right;
        int degree;
        boolean marked;

        Node(K key) {
            this.key = key;
            this.left = this;
            this.right = this;
            this.degree = 0;
            this.marked = false;
            this.parent = null;
            this.child = null;
        }
    }

    private Node<K> minNode;
    private int size;
    private final int maxCapacity;
    private final Comparator<K> comparator;
    private final boolean max;

    public PriorityQueue_FibonacciHeap(int maxCapacity, boolean max, Comparator<K> comparator, boolean floyd) {
        this.maxCapacity = maxCapacity;
        this.max = max;
        this.comparator = max ? comparator.reversed() : comparator;
        this.minNode = null;
        this.size = 0;
        // Note: floyd parameter not used in Fibonacci heap (no array-based sinking)
    }

    @Override
    public boolean isEmpty() {
        return minNode == null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void give(K key) {
        Node<K> node = new Node<>(key);

        // If at capacity, remove minimum element first
        if (size >= maxCapacity && minNode != null) {
            try {
                take();
            } catch (PQException e) {
                // Should not happen
            }
        }

        if (minNode == null) {
            minNode = node;
        } else {
            insertNodeIntoList(minNode, node);
            if (comparator.compare(node.key, minNode.key) < 0) {
                minNode = node;
            }
        }

        size++;
    }

    @Override
    public K take() throws PQException {
        if (isEmpty()) {
            throw new PQException("Priority queue is empty");
        }

        Node<K> z = minNode;
        K result = z.key;

        // Add all children to root list
        if (z.child != null) {
            Node<K> child = z.child;
            Node<K> start = child;
            do {
                Node<K> nextChild = child.right;
                child.parent = null;
                child = nextChild;
            } while (child != start);

            // Merge child list with root list
            Node<K> minRight = minNode.right;
            Node<K> childLeft = z.child.left;

            minNode.right = z.child;
            z.child.left = minNode;
            childLeft.right = minRight;
            minRight.left = childLeft;
        }

        // Remove z from root list
        if (z == z.right) {
            minNode = null;
        } else {
            minNode = z.right;
            removeNodeFromList(z);
            consolidate();
        }

        size--;
        return result;
    }

    /**
     * Required by PriorityQueue interface.
     * Fibonacci heap doesn't need explicit heap construction.
     */
    public void heapConstructor() {
        // No-op for Fibonacci heap
    }

    /**
     * Consolidate trees of same degree
     */
    private void consolidate() {
        if (minNode == null) return;

        int maxDegree = Math.max(1, (int) Math.ceil(Math.log(size) / Math.log(2)) + 1);
        @SuppressWarnings("unchecked")
        Node<K>[] degreeTable = new Node[maxDegree + 1];

        // Collect all root nodes
        List<Node<K>> rootNodes = new ArrayList<>();
        Node<K> current = minNode;
        do {
            rootNodes.add(current);
            current = current.right;
        } while (current != minNode);

        // Consolidate
        for (Node<K> w : rootNodes) {
            Node<K> x = w;
            int d = x.degree;

            while (d < degreeTable.length && degreeTable[d] != null) {
                Node<K> y = degreeTable[d];

                if (comparator.compare(x.key, y.key) > 0) {
                    Node<K> temp = x;
                    x = y;
                    y = temp;
                }

                link(y, x);
                degreeTable[d] = null;
                d++;
            }

            if (d < degreeTable.length) {
                degreeTable[d] = x;
            }
        }

        // Rebuild root list and find new minimum
        minNode = null;
        for (Node<K> node : degreeTable) {
            if (node != null) {
                node.left = node;
                node.right = node;

                if (minNode == null) {
                    minNode = node;
                } else {
                    insertNodeIntoList(minNode, node);
                    if (comparator.compare(node.key, minNode.key) < 0) {
                        minNode = node;
                    }
                }
            }
        }
    }

    /**
     * Link node y as a child of node x
     */
    private void link(Node<K> y, Node<K> x) {
        removeNodeFromList(y);
        y.parent = x;

        if (x.child == null) {
            x.child = y;
            y.right = y;
            y.left = y;
        } else {
            insertNodeIntoList(x.child, y);
        }

        x.degree++;
        y.marked = false;
    }

    /**
     * Insert node into circular doubly linked list
     */
    private void insertNodeIntoList(Node<K> list, Node<K> node) {
        node.right = list.right;
        node.left = list;
        list.right.left = node;
        list.right = node;
    }

    /**
     * Remove node from circular doubly linked list
     */
    private void removeNodeFromList(Node<K> node) {
        if (node.right == node) return;
        node.left.right = node.right;
        node.right.left = node.left;
    }

    @NotNull
    @Override
    public Iterator<K> iterator() {
        List<K> elements = new ArrayList<>();
        if (minNode != null) {
            collectElements(minNode, elements, new HashSet<>());
        }
        return elements.iterator();
    }

    /**
     * Collect all elements for iteration
     */
    private void collectElements(Node<K> start, List<K> elements, Set<Node<K>> visited) {
        if (start == null || visited.contains(start)) return;

        Node<K> current = start;
        do {
            visited.add(current);
            elements.add(current.key);
            if (current.child != null && !visited.contains(current.child)) {
                collectElements(current.child, elements, visited);
            }
            current = current.right;
        } while (current != start && !visited.contains(current));
    }

    /**
     * Peek at element at index k
     * For Fibonacci heap, only the minimum is efficiently accessible
     */
    public K peek(int k) {
        if (k == 0 && minNode != null) {
            return minNode.key;
        }
        // For other indices, collect all elements
        List<K> elements = new ArrayList<>();
        if (minNode != null) {
            collectElements(minNode, elements, new HashSet<>());
        }
        if (k >= 0 && k < elements.size()) {
            return elements.get(k);
        }
        return null;
    }

    /**
     * Get max property of the heap
     */
    public boolean getMax() {
        return max;
    }
}