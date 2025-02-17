// heap/LeftistHeap.java

package projecteval.heap;

import java.util.ArrayList;

/*
 * This is a leftist heap that follows the same operations as a
 * binary min heap, but may be unbalanced at times and follows a
 * leftist property, in which the left side is more heavy on the
 * right based on the null-path length (npl) values.
 *
 * Source: https://iq.opengenus.org/leftist-heap/
 *
 */

public class LeftistHeap {
    private class Node {
        private int element, npl;
        private Node left, right;

        // Node constructor setting the data element and left/right pointers to null
        private Node(int element) {
            this.element = element;
            left = right = null;
            npl = 0;
        }
    }

    private Node root;

    // Constructor
    public LeftistHeap() {
        root = null;
    }

    // Checks if heap is empty
    public boolean isEmpty() {
        return root == null;
    }

    // Resets structure to initial state
    public void clear() {
        // We will put head is null
        root = null;
    }

    // Merge function that merges the contents of another leftist heap with the
    // current one
    public void merge(LeftistHeap h1) {
        // If the present function is rhs then we ignore the merge
        root = merge(root, h1.root);
        h1.root = null;
    }

    // Function merge with two Nodes a and b
    public Node merge(Node a, Node b) {
        if (a == null) return b;

        if (b == null) return a;

        // Violates leftist property, so must do a swap
        if (a.element > b.element) {
            Node temp = a;
            a = b;
            b = temp;
        }

        // Now we call the function merge to merge a and b
        a.right = merge(a.right, b);

        // Violates leftist property so must swap here
        if (a.left == null) {
            a.left = a.right;
            a.right = null;
        } else {
            if (a.left.npl < a.right.npl) {
                Node temp = a.left;
                a.left = a.right;
                a.right = temp;
            }
            a.npl = a.right.npl + 1;
        }
        return a;
    }

    // Function insert. Uses the merge function to add the data
    public void insert(int a) {
        root = merge(new Node(a), root);
    }

    // Returns and removes the minimum element in the heap
    public int extract_min() {
        // If is empty return -1
        if (isEmpty()) return -1;

        int min = root.element;
        root = merge(root.left, root.right);
        return min;
    }

    // Function returning a list of an in order traversal of the data structure
    public ArrayList<Integer> in_order() {
        ArrayList<Integer> lst = new ArrayList<>();
        in_order_aux(root, lst);
        return new ArrayList<>(lst);
    }

    // Auxiliary function for in_order
    private void in_order_aux(Node n, ArrayList<Integer> lst) {
        if (n == null) return;
        in_order_aux(n.left, lst);
        lst.add(n.element);
        in_order_aux(n.right, lst);
    }
}


// heap/FibonacciHeap.java

package projecteval.heap;

public class FibonacciHeap {

    private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
    private HeapNode min;
    private static int totalLinks = 0;
    private static int totalCuts = 0;
    private int numOfTrees = 0;
    private int numOfHeapNodes = 0;
    private int markedHeapNoodesCounter = 0;

    /*
     * a constructor for an empty Heap
     * set the min to be null
     */
    public FibonacciHeap() {
        this.min = null;
    }

    /*
     * a constructor for a Heap with one element
     * set the min to be the HeapNode with the given key
     * @pre key>=0
     * @post empty == false
     */
    public FibonacciHeap(int key) {
        this.min = new HeapNode(key);
        this.numOfTrees++;
        this.numOfHeapNodes++;
    }

    /*
     * check if the heap is empty
     * $ret == true - if the tree is empty
     */
    public boolean empty() {
        return (this.min == null);
    }

    /**
     * Creates a node (of type HeapNode) which contains the given key, and inserts it into the heap.
     *
     * @pre key>=0
     * @post (numOfnodes = = $prev numOfnodes + 1)
     * @post empty == false
     * $ret = the HeapNode we inserted
     */
    public HeapNode insert(int key) {
        HeapNode toInsert = new HeapNode(key); // creates the node
        if (this.empty()) {
            this.min = toInsert;
        } else { // tree is not empty
            min.setNext(toInsert);
            this.updateMin(toInsert);
        }
        this.numOfHeapNodes++;
        this.numOfTrees++;
        return toInsert;
    }

    /**
     * Delete the node containing the minimum key in the heap
     * updates new min
     *
     * @post (numOfnodes = = $prev numOfnodes - 1)
     */
    public void deleteMin() {
        if (this.empty()) {
            return;
        }
        if (this.numOfHeapNodes == 1) { // if there is only one tree
            this.min = null;
            this.numOfTrees--;
            this.numOfHeapNodes--;
            return;
        }
        // change all children's parent to null//
        if (this.min.child != null) { // min has a child
            HeapNode child = this.min.child;
            HeapNode tmpChild = child;
            child.parent = null;
            while (child.next != tmpChild) {
                child = child.next;
                child.parent = null;
            }
        }
        // delete the node//
        if (this.numOfTrees > 1) {
            (this.min.prev).next = this.min.next;
            (this.min.next).prev = this.min.prev;
            if (this.min.child != null) {
                (this.min.prev).setNext(this.min.child);
            }
        } else { // this.numOfTrees = 1
            this.min = this.min.child;
        }
        this.numOfHeapNodes--;
        this.successiveLink(this.min.getNext());
    }

    /**
     * Return the node of the heap whose key is minimal.
     * $ret == null if (empty==true)
     */
    public HeapNode findMin() {
        return this.min;
    }

    /**
     * Meld the heap with heap2
     *
     * @pre heap2 != null
     * @post (numOfnodes = = $prev numOfnodes + heap2.numOfnodes)
     */
    public void meld(FibonacciHeap heap2) {
        if (heap2.empty()) {
            return;
        }
        if (this.empty()) {
            this.min = heap2.min;
        } else {
            this.min.setNext(heap2.min);
            this.updateMin(heap2.min);
        }
        this.numOfTrees += heap2.numOfTrees;
        this.numOfHeapNodes += heap2.numOfHeapNodes;
    }

    /**
     * Return the number of elements in the heap
     * $ret == 0 if heap is empty
     */
    public int size() {
        return this.numOfHeapNodes;
    }

    /**
     * Return a counters array, where the value of the i-th index is the number of trees with rank i
     * in the heap. returns an empty array for an empty heap
     */
    public int[] countersRep() {
        if (this.empty()) {
            return new int[0]; /// return an empty array
        }
        int[] rankArray = new int[(int) Math.floor(Math.log(this.size()) / Math.log(GOLDEN_RATIO)) + 1]; // creates the array
        rankArray[this.min.rank]++;
        HeapNode curr = this.min.next;
        while (curr != this.min) {
            rankArray[curr.rank]++;
            curr = curr.next;
        }
        return rankArray;
    }

    /**
     * Deletes the node x from the heap (using decreaseKey(x) to -1)
     *
     * @pre heap contains x
     * @post (numOfnodes = = $prev numOfnodes - 1)
     */
    public void delete(HeapNode x) {
        this.decreaseKey(x, x.getKey() + 1); // change key to be the minimal (-1)
        this.deleteMin(); // delete it
    }

    /**
     * The function decreases the key of the node x by delta.
     *
     * @pre x.key >= delta (we don't realize it when calling from delete())
     * @pre heap contains x
     */
    private void decreaseKey(HeapNode x, int delta) {
        int newKey = x.getKey() - delta;
        x.key = newKey;
        if (x.isRoot()) { // no parent to x
            this.updateMin(x);
            return;
        }
        if (x.getKey() >= x.parent.getKey()) {
            return;
        } // we don't need to cut
        HeapNode prevParent = x.parent;
        this.cut(x);
        this.cascadingCuts(prevParent);
    }

    /**
     * returns the current potential of the heap, which is:
     * Potential = #trees + 2*#markedNodes
     */
    public int potential() {
        return numOfTrees + (2 * markedHeapNoodesCounter);
    }

    /**
     * This static function returns the total number of link operations made during the run-time of
     * the program. A link operation is the operation which gets as input two trees of the same
     * rank, and generates a tree of rank bigger by one.
     */
    public static int totalLinks() {
        return totalLinks;
    }

    /**
     * This static function returns the total number of cut operations made during the run-time of
     * the program. A cut operation is the operation which disconnects a subtree from its parent
     * (during decreaseKey/delete methods).
     */
    public static int totalCuts() {
        return totalCuts;
    }

    /*
     * updates the min of the heap (if needed)
     * @pre this.min == @param (posMin) if and only if (posMin.key < this.min.key)
     */
    private void updateMin(HeapNode posMin) {
        if (posMin.getKey() < this.min.getKey()) {
            this.min = posMin;
        }
    }

    /*
     * Recursively "runs" all the way up from @param (curr) and mark the nodes.
     * stop the recursion if we had arrived to a marked node or to a root.
     * if we arrived to a marked node, we cut it and continue recursively.
     * called after a node was cut.
     * @post (numOfnodes == $prev numOfnodes)
     */
    private void cascadingCuts(HeapNode curr) {
        if (!curr.isMarked()) { // stop the recursion
            curr.mark();
            if (!curr.isRoot()) this.markedHeapNoodesCounter++;
        } else {
            if (curr.isRoot()) {
                return;
            }
            HeapNode prevParent = curr.parent;
            this.cut(curr);
            this.cascadingCuts(prevParent);
        }
    }

    /*
     * cut a node (and his "subtree") from his origin tree and connect it to the heap as a new tree.
     * called after a node was cut.
     * @post (numOfnodes == $prev numOfnodes)
     */
    private void cut(HeapNode curr) {
        curr.parent.rank--;
        if (curr.marked) {
            this.markedHeapNoodesCounter--;
            curr.marked = false;
        }
        if (curr.parent.child == curr) { // we should change the parent's child
            if (curr.next == curr) { // curr do not have brothers
                curr.parent.child = null;
            } else { // curr have brothers
                curr.parent.child = curr.next;
            }
        }
        curr.prev.next = curr.next;
        curr.next.prev = curr.prev;
        curr.next = curr;
        curr.prev = curr;
        curr.parent = null;
        this.min.setNext(curr);
        this.updateMin(curr);
        this.numOfTrees++;
        totalCuts++;
    }

    /*
     *
     */
    private void successiveLink(HeapNode curr) {
        HeapNode[] buckets = this.toBuckets(curr);
        this.min = this.fromBuckets(buckets);
    }

    /*
     *
     */
    private HeapNode[] toBuckets(HeapNode curr) {
        HeapNode[] buckets = new HeapNode[(int) Math.floor(Math.log(this.size()) / Math.log(GOLDEN_RATIO)) + 1];
        curr.prev.next = null;
        HeapNode tmpCurr;
        while (curr != null) {
            tmpCurr = curr;
            curr = curr.next;
            tmpCurr.next = tmpCurr;
            tmpCurr.prev = tmpCurr;
            while (buckets[tmpCurr.rank] != null) {
                tmpCurr = this.link(tmpCurr, buckets[tmpCurr.rank]);
                buckets[tmpCurr.rank - 1] = null;
            }
            buckets[tmpCurr.rank] = tmpCurr;
        }
        return buckets;
    }

    /*
     *
     */
    private HeapNode fromBuckets(HeapNode[] buckets) {
        HeapNode tmpMin = null;
        this.numOfTrees = 0;
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] != null) {
                this.numOfTrees++;
                if (tmpMin == null) {
                    tmpMin = buckets[i];
                    tmpMin.next = tmpMin;
                    tmpMin.prev = tmpMin;
                } else {
                    tmpMin.setNext(buckets[i]);
                    if (buckets[i].getKey() < tmpMin.getKey()) {
                        tmpMin = buckets[i];
                    }
                }
            }
        }
        return tmpMin;
    }

    /*
     * link between two nodes (and their trees)
     * defines the smaller node to be the parent
     */
    private HeapNode link(HeapNode c1, HeapNode c2) {
        if (c1.getKey() > c2.getKey()) {
            HeapNode c3 = c1;
            c1 = c2;
            c2 = c3;
        }
        if (c1.child == null) {
            c1.child = c2;
        } else {
            c1.child.setNext(c2);
        }
        c2.parent = c1;
        c1.rank++;
        totalLinks++;
        return c1;
    }

    /**
     * public class HeapNode
     * each HeapNode belongs to a heap (Inner class)
     */
    public class HeapNode {

        public int key;
        private int rank;
        private boolean marked;
        private HeapNode child;
        private HeapNode next;
        private HeapNode prev;
        private HeapNode parent;

        /*
         * a constructor for a heapNode withe key @param (key)
         * prev == next == this
         * parent == child == null
         */
        public HeapNode(int key) {
            this.key = key;
            this.marked = false;
            this.next = this;
            this.prev = this;
        }

        /*
         * returns the key of the node.
         */
        public int getKey() {
            return this.key;
        }

        /*
         * checks whether the node is marked
         * $ret = true if one child has been cut
         */
        private boolean isMarked() {
            return this.marked;
        }

        /*
         * mark a node (after a child was cut)
         * @inv root.mark() == false.
         */
        private void mark() {
            if (this.isRoot()) {
                return;
            } // check if the node is a root
            this.marked = true;
        }

        /*
         * add the node @param (newNext) to be between this and this.next
         * works fine also if @param (newNext) does not "stands" alone
         */
        private void setNext(HeapNode newNext) {
            HeapNode tmpNext = this.next;
            this.next = newNext;
            this.next.prev.next = tmpNext;
            tmpNext.prev = newNext.prev;
            this.next.prev = this;
        }

        /*
         * returns the next node to this node
         */
        private HeapNode getNext() {
            return this.next;
        }

        /*
         * check if the node is a root
         * root definition - this.parent == null (uppest in his tree)
         */
        private boolean isRoot() {
            return (this.parent == null);
        }
    }
}


// heap/HeapPerformanceTest.java

package projecteval.heap;

import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;

public class HeapPerformanceTest {

    private final int numElements;

    public HeapPerformanceTest(int numElements) {
        this.numElements = numElements;
    }

    public HashMap<String, Long> test() {
        // Number of elements to test
        // Create heaps for insertion and deletion tests
        FibonacciHeap fibonacciHeap = new FibonacciHeap();
        LeftistHeap leftistHeap = new LeftistHeap();
        HashMap<String, Long> ret = new HashMap<>();

        // Insertion test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numElements; i++) {
            fibonacciHeap.insert(new Random().nextInt());
        }
        long endTime = System.currentTimeMillis();
        ret.put("Fibonacci Heap Insertion Time", endTime - startTime);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < numElements; i++) {
            leftistHeap.insert(new Random().nextInt());
        }
        endTime = System.currentTimeMillis();
        ret.put("Leftist Heap Insertion Time", endTime - startTime);

        // Deletion test
        startTime = System.currentTimeMillis();
        while (!fibonacciHeap.empty()) {
            fibonacciHeap.deleteMin();
        }
        endTime = System.currentTimeMillis();
        ret.put("Fibonacci Heap Deletion Time", endTime - startTime);

        startTime = System.currentTimeMillis();
        while (!leftistHeap.isEmpty()) {
            leftistHeap.extract_min();
        }
        endTime = System.currentTimeMillis();
        ret.put("Leftist Heap Deletion Time", endTime - startTime);

        // Merge test
        FibonacciHeap fibonacciHeap1 = new FibonacciHeap();
        FibonacciHeap fibonacciHeap2 = new FibonacciHeap();
        LeftistHeap leftistHeap1 = new LeftistHeap();
        LeftistHeap leftistHeap2 = new LeftistHeap();

        // Populate the heaps for merge test
        for (int i = 0; i < numElements / 2; i++) {
            fibonacciHeap1.insert(new Random().nextInt());
            fibonacciHeap2.insert(new Random().nextInt());
            leftistHeap1.insert(new Random().nextInt());
            leftistHeap2.insert(new Random().nextInt());
        }

        // Merge performance test
        startTime = System.currentTimeMillis();
        fibonacciHeap1.meld(fibonacciHeap2);
        endTime = System.currentTimeMillis();
        ret.put("Fibonacci Heap Merge Time", endTime - startTime);

        startTime = System.currentTimeMillis();
        leftistHeap1.merge(leftistHeap2);
        endTime = System.currentTimeMillis();
        ret.put("Leftist Heap Merge Time", endTime - startTime);
        return ret;
    }

    public static void main(String[] args){
        int numElements = Integer.parseInt(args[0]);

        HeapPerformanceTest t = new HeapPerformanceTest(numElements);
        HashMap<String, Long> ret = t.test();
        for (String key : ret.keySet()) {
            System.out.println(key + ": " + ret.get(key) + "ms");
        }
    }
}


