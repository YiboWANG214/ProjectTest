package projecteval.springreactivenonreactive;


////////////////////////////////////////////////////////////////////////////////
// MVCSyncControllerTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.springreactivenonreactive.controller.MVCSyncController;
import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.NonReactiveRepository;

@ExtendWith(MockitoExtension.class)
public class MVCSyncControllerTest {

    @InjectMocks
    private MVCSyncController mvcSyncController;

    @Mock
    private NonReactiveRepository nonReactiveRepository;

    @Test
    public void testFindById() {
        Message message = new Message();
        message.setId("789");
        message.setContent("MVC Sync Test");

        when(nonReactiveRepository.findById("789")).thenReturn(Optional.of(message));

        Message result = mvcSyncController.findById("789");
        assertEquals("MVC Sync Test", result.getContent());
    }

    @Test
    public void testPost() {
        Message message = new Message();
        message.setId("2022");
        message.setContent("MVC Sync Post");

        when(nonReactiveRepository.save(message)).thenReturn(message);

        Message saved = mvcSyncController.post(message);
        assertEquals("MVC Sync Post", saved.getContent());
    }
}

