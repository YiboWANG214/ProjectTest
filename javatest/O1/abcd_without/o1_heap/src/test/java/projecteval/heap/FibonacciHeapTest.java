package projecteval.heap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;


/**
 * Unit tests for the FibonacciHeap class.
 */
public class FibonacciHeapTest {

    @Test
    public void testEmptyHeap() {
        FibonacciHeap fibHeap = new FibonacciHeap();
        assertTrue(fibHeap.empty(), "Newly created FibonacciHeap should be empty.");
    }

    @Test
    public void testSingleElementHeap() {
        FibonacciHeap fibHeap = new FibonacciHeap(10);
        assertFalse(fibHeap.empty(), "FibonacciHeap with one key should not be empty.");
        assertEquals(10, fibHeap.findMin().getKey(), "Min should be 10.");
    }

    @Test
    public void testInsertAndFindMin() {
        FibonacciHeap fibHeap = new FibonacciHeap();
        fibHeap.insert(40);
        fibHeap.insert(20);
        fibHeap.insert(30);
        fibHeap.insert(10);
        assertEquals(10, fibHeap.findMin().getKey(), "Minimum should be 10.");
    }

    @Test
    public void testDeleteMinSingleElement() {
        FibonacciHeap fibHeap = new FibonacciHeap(10);
        fibHeap.deleteMin();
        assertTrue(fibHeap.empty(), "Heap should be empty after deleting the single element.");
    }

    @Test
    public void testDeleteMinMultipleElements() {
        FibonacciHeap fibHeap = new FibonacciHeap();
        fibHeap.insert(40);
        fibHeap.insert(20);
        fibHeap.insert(30);
        fibHeap.insert(10);

        fibHeap.deleteMin(); // remove 10
        assertEquals(20, fibHeap.findMin().getKey(), "After removing 10, min should be 20.");
        fibHeap.deleteMin(); // remove 20
        assertEquals(30, fibHeap.findMin().getKey(), "After removing 20, min should be 30.");
        fibHeap.deleteMin(); // remove 30
        assertEquals(40, fibHeap.findMin().getKey(), "Next min is 40.");
        fibHeap.deleteMin(); // remove 40
        assertTrue(fibHeap.empty(), "Heap is now empty.");
    }

    @Test
    public void testMeldTwoHeaps() {
        FibonacciHeap fib1 = new FibonacciHeap();
        fib1.insert(1);
        fib1.insert(3);

        FibonacciHeap fib2 = new FibonacciHeap();
        fib2.insert(2);
        fib2.insert(5);

        fib1.meld(fib2);
        assertFalse(fib1.empty(), "After meld, fib1 should not be empty.");
        assertTrue(fib2.empty(), "After meld, fib2 should be empty.");
        assertEquals(1, fib1.findMin().getKey(), "Melded heap's min should be the smaller of the two heaps' mins.");
    }

    @Test
    public void testSize() {
        FibonacciHeap fib = new FibonacciHeap();
        fib.insert(1);
        fib.insert(2);
        fib.insert(3);
        assertEquals(3, fib.size(), "Heap should have exactly three elements.");
    }

    @Test
    public void testCountersRepOnEmptyHeap() {
        FibonacciHeap fib = new FibonacciHeap();
        assertEquals(0, fib.countersRep().length, "Empty heap should return an empty array from countersRep.");
    }

    @Test
    public void testCountersRepOnNonEmptyHeap() {
        FibonacciHeap fib = new FibonacciHeap();
        fib.insert(10);
        fib.insert(20);
        fib.insert(5);

        int[] counters = fib.countersRep();
        // Not strictly guaranteed how the distribution of ranks looks, but the array should not be empty.
        assertTrue(counters.length > 0, "countersRep should return a non-empty array for a non-empty heap.");
        // The sum of counters should match the number of trees (which will be <= size).
        int sum = 0;
        for (int c : counters) {
            sum += c;
        }
        assertTrue(sum <= fib.size(), "Sum of ranks counters should not exceed total size of heap.");
    }

    @Test
    public void testDeleteArbitraryNode() {
        FibonacciHeap fib = new FibonacciHeap();
        FibonacciHeap.HeapNode node1 = fib.insert(10);
        FibonacciHeap.HeapNode node2 = fib.insert(5);
        FibonacciHeap.HeapNode node3 = fib.insert(30);

        // Delete node3 which has key 30
        fib.delete(node3);
        assertEquals(2, fib.size(), "Heap size should be 2 after deleting one node.");
        // Next min should be 5
        assertEquals(5, fib.findMin().getKey(), "Min should be 5.");
        // Ensure 10 is still in the heap
        fib.deleteMin(); // remove 5
        assertEquals(10, fib.findMin().getKey(), "Min should now be 10.");
    }

    @Test
    public void testPotential() {
        FibonacciHeap fib = new FibonacciHeap();
        fib.insert(10);
        fib.insert(20);
        fib.insert(30);
        // Potential = #trees + 2*#markedNodes
        // Right now, all newly inserted nodes are unmarked, so #markedNodes=0, #trees=3
        // Potential should be 3.
        assertEquals(3, fib.potential(), "Potential should be correct for multiple root nodes.");
    }

    @Test
    public void testTotalLinksAndCuts() {
        // totalLinks and totalCuts are static accumulative counters.
        // We can check that they do not throw exceptions and that they make sense.
        // The values might depend on the entire test run, so we just ensure no exception is thrown.
        int links = FibonacciHeap.totalLinks();
        int cuts = FibonacciHeap.totalCuts();
        assertTrue(links >= 0, "totalLinks should be non-negative.");
        assertTrue(cuts >= 0, "totalCuts should be non-negative.");
    }
}
