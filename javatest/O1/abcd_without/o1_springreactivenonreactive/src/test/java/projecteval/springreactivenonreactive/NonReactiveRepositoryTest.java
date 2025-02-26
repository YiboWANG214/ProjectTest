package projecteval.springreactivenonreactive;

////////////////////////////////////////////////////////////////////////////////
// NonReactiveRepositoryTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.NonReactiveRepository;

@ExtendWith(MockitoExtension.class)
public class NonReactiveRepositoryTest {

    @Mock
    private NonReactiveRepository nonReactiveRepository;

    private Message message;

    @BeforeEach
    public void setup() {
        message = new Message();
        message.setId("123");
        message.setContent("Test Content");
    }

    @Test
    public void testFindById() {
        when(nonReactiveRepository.findById("123")).thenReturn(Optional.of(message));

        Optional<Message> result = nonReactiveRepository.findById("123");
        assertTrue(result.isPresent());
        assertEquals("Test Content", result.get().getContent());
        verify(nonReactiveRepository, times(1)).findById("123");
    }

    @Test
    public void testSave() {
        when(nonReactiveRepository.save(message)).thenReturn(message);

        Message savedMessage = nonReactiveRepository.save(message);
        assertEquals("Test Content", savedMessage.getContent());
        verify(nonReactiveRepository, times(1)).save(message);
    }

    @Test
    public void testDelete() {
        doNothing().when(nonReactiveRepository).deleteById("123");

        nonReactiveRepository.deleteById("123");
        verify(nonReactiveRepository, times(1)).deleteById("123");
    }
}
