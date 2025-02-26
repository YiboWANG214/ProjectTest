package projecteval.heap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Unit tests for the LeftistHeap class.
 */
public class LeftistHeapTest {

    @Test
    public void testIsEmptyInitially() {
        LeftistHeap heap = new LeftistHeap();
        assertTrue(heap.isEmpty(), "A new heap should be empty.");
    }

    @Test
    public void testInsertAndIsEmpty() {
        LeftistHeap heap = new LeftistHeap();
        heap.insert(10);
        assertFalse(heap.isEmpty(), "Heap should not be empty after insertion.");
    }

    @Test
    public void testClear() {
        LeftistHeap heap = new LeftistHeap();
        heap.insert(1);
        heap.insert(2);
        heap.clear();
        assertTrue(heap.isEmpty(), "Heap should be empty after clear.");
    }

    @Test
    public void testInsertAndExtractMin() {
        LeftistHeap heap = new LeftistHeap();
        heap.insert(50);
        heap.insert(20);
        heap.insert(30);
        heap.insert(10);

        assertEquals(10, heap.extract_min(), "The minimum extracted should be 10.");
        assertEquals(20, heap.extract_min(), "The minimum extracted should be 20.");
        assertEquals(30, heap.extract_min(), "The minimum extracted should be 30.");
        assertEquals(50, heap.extract_min(), "The minimum extracted should be 50.");
        assertTrue(heap.isEmpty(), "Heap should be empty after extracting all elements.");
    }

    @Test
    public void testExtractMinFromEmpty() {
        LeftistHeap heap = new LeftistHeap();
        assertEquals(-1, heap.extract_min(), "Extracting min from an empty heap should return -1.");
    }

    @Test
    public void testMergeTwoNonEmptyHeaps() {
        LeftistHeap heap1 = new LeftistHeap();
        heap1.insert(10);
        heap1.insert(30);

        LeftistHeap heap2 = new LeftistHeap();
        heap2.insert(20);
        heap2.insert(5);

        heap1.merge(heap2);
        // After merge, heap2 should be empty
        assertTrue(heap2.isEmpty(), "Second heap should be empty after merge.");

        // Extract from merged heap
        assertEquals(5, heap1.extract_min(), "The new min should be 5 after merge.");
        assertEquals(10, heap1.extract_min(), "Next min should be 10.");
        assertEquals(20, heap1.extract_min(), "Next min should be 20.");
        assertEquals(30, heap1.extract_min(), "Next min should be 30.");
        assertTrue(heap1.isEmpty(), "All elements should be extracted.");
    }

    @Test
    public void testMergeEmptyHeapWithNonEmpty() {
        LeftistHeap emptyHeap = new LeftistHeap();
        LeftistHeap nonEmptyHeap = new LeftistHeap();
        nonEmptyHeap.insert(1);
        nonEmptyHeap.insert(2);

        emptyHeap.merge(nonEmptyHeap);

        assertTrue(nonEmptyHeap.isEmpty(), "After merging into empty heap, nonEmptyHeap should become empty.");
        assertFalse(emptyHeap.isEmpty(), "Now emptyHeap should contain all elements.");
        assertEquals(1, emptyHeap.extract_min(), "Minimum after merge should be 1.");
        assertEquals(2, emptyHeap.extract_min(), "Minimum after merge should be 2.");
        assertTrue(emptyHeap.isEmpty(), "Heap should be empty now.");
    }

    @Test
    public void testInOrder() {
        LeftistHeap heap = new LeftistHeap();
        heap.insert(5);
        heap.insert(2);
        heap.insert(8);
        heap.insert(1);

        // in_order should produce sorted traversal because it's in-order for the tree structure
        List<Integer> list = heap.in_order();
        // This list might not be perfectly sorted as a typical BST. It's just "in-order" of the leftist structure.
        // We'll just check that it contains all elements in some valid in-order form.
        assertTrue(list.contains(5));
        assertTrue(list.contains(2));
        assertTrue(list.contains(8));
        assertTrue(list.contains(1));
        assertEquals(4, list.size(), "Should contain exactly 4 elements.");
    }

}
