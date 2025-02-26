package projecteval.libraryApp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;


public class BookTest {

    @Test
    public void testBookConstructorAndGetters() {
        Book book = new Book(12345, "Test Title", "Test Author", "Test Genre", 10, 2);
        assertNotNull(book);
        assertEquals(12345, book.getIsbn());
        assertEquals("Test Title", book.getTitle());
        assertEquals("Test Author", book.getAuthor());
        assertEquals("Test Genre", book.getGenre());
        assertEquals(10, book.getQuantity());
        assertEquals(2, book.getCheckedOut());
        assertEquals(8, book.getCheckedIn()); // 10 - 2
    }

    @Test
    public void testSettersAndGetters() {
        Book book = new Book(0, "", "", "", 0, 0);
        book.setIsbn(99999);
        book.setTitle("New Title");
        book.setAuthor("New Author");
        book.setGenre("New Genre");
        book.setQuantity(20);
        book.setCheckedOut(5);
        book.setCheckedIn(15);

        assertEquals(99999, book.getIsbn());
        assertEquals("New Title", book.getTitle());
        assertEquals("New Author", book.getAuthor());
        assertEquals("New Genre", book.getGenre());
        assertEquals(20, book.getQuantity());
        assertEquals(5, book.getCheckedOut());
        assertEquals(15, book.getCheckedIn());
    }
}


// -----------------------------------------------------------------------------
// LibraryAppTest.java
// -----------------------------------------------------------------------------
