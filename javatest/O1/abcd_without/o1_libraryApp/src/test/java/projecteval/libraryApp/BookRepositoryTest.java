package projecteval.libraryApp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class BookRepositoryTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    BookRepository repository;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        repository = new BookRepository();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    public void testSearchByTitle() {
        repository.searchByTitle("Pride");
        String output = outContent.toString();
        // We expect at least 1 book found
        // Checking the substring to see if "Pride and Prejudice C" is mentioned
        assertTrue(output.contains("Pride and Prejudice C"));
        assertTrue(output.contains("1 Book Found"));
    }

    @Test
    public void testSearchByISBNFound() {
        repository.searchByISBN(253910);
        String output = outContent.toString();
        // Should find exactly 1 Book
        assertTrue(output.contains("Pride and Prejudice C"));
        assertTrue(output.contains("1 Book Found"));
    }

    @Test
    public void testSearchByISBNNotFound() {
        repository.searchByISBN(999999);
        String output = outContent.toString();
        // Should find 0 Books
        assertTrue(output.contains("0 Book Found"));
    }

    @Test
    public void testSearchByGenreSuccess() {
        boolean found = repository.searchByGenre("Horror");
        String output = outContent.toString();
        assertTrue(found);
        assertTrue(output.contains("It"));
        assertTrue(output.contains("1 Book Found"));
    }

    @Test
    public void testSearchByGenreFailure() {
        boolean found = repository.searchByGenre("Non-Existing Genre");
        String output = outContent.toString();
        assertFalse(found);
        assertTrue(output.contains("0 Book Found"));
    }

    @Test
    public void testSearchISBN() {
        int result = repository.searchISBN(253910);
        assertEquals(1, result);
        result = repository.searchISBN(999999);
        assertEquals(0, result);
    }

    @Test
    public void testGetBook_Success() {
        // getBook increments "Checked Out" if available
        // For example, Pride and Prejudice has 10 quantity and 7 checked out
        boolean success = repository.getBook(253910);
        // Should be able to check out if 10 - 7 > 0
        assertTrue(success);
    }

    @Test
    public void testGetBook_Failure() {
        // Suppose we know a book is at max capacity or doesn't exist
        // We'll try an ISBN that doesn't exist
        boolean success = repository.getBook(999999);
        // Should fail
        assertFalse(success);
    }

    @Test
    public void testSubmitBook_Success() {
        // submitBook decreases "Checked Out" count if possible
        // For any book in the list, if quantity > checkedIn, it means there's room to return more
        // We'll pick one that has some checkedOut > 0
        // Let's pick 253910: 10 quantity, 7 checkedOut => checkedIn = 3 => quantity(10) > checkedIn(3)
        boolean success = repository.submitBook(253910);
        assertTrue(success);
    }

    @Test
    public void testSubmitBook_Failure() {
        // If the book doesn't exist or quantity <= checkedIn, let’s check a scenario
        // We'll pick an ISBN that doesn't exist
        boolean success = repository.submitBook(999999);
        assertFalse(success);
    }

    @Test
    public void testBookStatus() {
        repository.bookStatus(253910);
        String output = outContent.toString();
        assertTrue(output.contains("Pride and Prejudice C"));
    }
}


// -----------------------------------------------------------------------------
// MainTest.java
// -----------------------------------------------------------------------------
// NOTE: Testing the Main class is often more of an integration/system test 
// because it involves user input. Here, we'll demonstrate a basic test that
// mocks user input using a ByteArrayInputStream so we can automate the menu.

