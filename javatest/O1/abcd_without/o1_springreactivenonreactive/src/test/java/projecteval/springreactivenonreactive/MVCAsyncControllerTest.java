package projecteval.springreactivenonreactive;



////////////////////////////////////////////////////////////////////////////////
// MVCAsyncControllerTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.springreactivenonreactive.controller.MVCAsyncController;
import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.NonReactiveRepository;

@ExtendWith(MockitoExtension.class)
public class MVCAsyncControllerTest {

    @InjectMocks
    private MVCAsyncController mvcAsyncController;

    @Mock
    private NonReactiveRepository nonReactiveRepository;

    @Test
    public void testFindByIdAsync() {
        Message message = new Message();
        message.setId("999");
        message.setContent("MVC Async Test");

        when(nonReactiveRepository.findById("999")).thenReturn(Optional.of(message));

        CompletableFuture<Message> future = mvcAsyncController.findById("999");
        Message result = future.join();
        assertEquals("MVC Async Test", result.getContent());
    }

    @Test
    public void testPostAsync() {
        Message message = new Message();
        message.setId("777");
        message.setContent("MVC Async Post");

        when(nonReactiveRepository.save(message)).thenReturn(message);

        CompletableFuture<Message> future = mvcAsyncController.post(message);
        Message saved = future.join();
        assertEquals("MVC Async Post", saved.getContent());
    }
}
