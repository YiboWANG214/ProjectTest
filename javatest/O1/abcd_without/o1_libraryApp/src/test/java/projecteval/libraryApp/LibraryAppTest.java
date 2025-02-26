package projecteval.libraryApp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LibraryAppTest {

    @Mock
    private BookRepository mockRepo;

    @InjectMocks
    private LibraryApp libraryApp;

    @BeforeEach
    public void setup() {
        // Any common stubbing can be done here if required
    }

    @Test
    public void testFindByTitle() {
        // searchByTitle returns void, we just verify it's called
        doNothing().when(mockRepo).searchByTitle(anyString());
        libraryApp.findByTitle("title");
        verify(mockRepo).searchByTitle("title");
    }

    @Test
    public void testFindByISBN() {
        doNothing().when(mockRepo).searchByISBN(anyInt());
        libraryApp.findByISBN(100);
        verify(mockRepo).searchByISBN(100);
    }

    @Test
    public void testFindByGenre() {
        doReturn(true).when(mockRepo).searchByGenre(anyString());
        boolean result = libraryApp.findByGenre("Horror");
        verify(mockRepo).searchByGenre("Horror");
        assertTrue(result);

        doReturn(false).when(mockRepo).searchByGenre(anyString());
        result = libraryApp.findByGenre("Unknown");
        verify(mockRepo).searchByGenre("Unknown");
        assertFalse(result);
    }

    @Test
    public void testFindISBN() {
        doReturn(1).when(mockRepo).searchISBN(anyInt());
        int result = libraryApp.findISBN(200);
        verify(mockRepo).searchISBN(200);
        // We expect 1 back from the mock
        assertTrue(result == 1);
    }

    @Test
    public void testWithdrawBook() {
        doReturn(true).when(mockRepo).getBook(anyInt());
        boolean success = libraryApp.withdrawBook(123);
        verify(mockRepo).getBook(123);
        assertTrue(success);

        doReturn(false).when(mockRepo).getBook(anyInt());
        success = libraryApp.withdrawBook(456);
        verify(mockRepo).getBook(456);
        assertFalse(success);
    }

    @Test
    public void testDepositBook() {
        doReturn(true).when(mockRepo).submitBook(anyInt());
        boolean success = libraryApp.depositBook(123);
        verify(mockRepo).submitBook(123);
        assertTrue(success);

        doReturn(false).when(mockRepo).submitBook(anyInt());
        success = libraryApp.depositBook(456);
        verify(mockRepo).submitBook(456);
        assertFalse(success);
    }

    @Test
    public void testGetStatus() {
        // bookStatus returns void, just verify execution
        doNothing().when(mockRepo).bookStatus(anyInt());
        libraryApp.getStatus(789);
        verify(mockRepo).bookStatus(789);
    }
}


// -----------------------------------------------------------------------------
// BookRepositoryTest.java
// -----------------------------------------------------------------------------
