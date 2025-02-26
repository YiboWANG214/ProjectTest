package projecteval.springreactivenonreactive;

////////////////////////////////////////////////////////////////////////////////
// WebFluxControllerTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.springreactivenonreactive.controller.WebFluxController;
import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.ReactiveRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class WebFluxControllerTest {

    @InjectMocks
    private WebFluxController webFluxController;

    @Mock
    private ReactiveRepository reactiveRepository;

    @Test
    public void testFindByIdReactive() {
        Message message = new Message();
        message.setId("123");
        message.setContent("Reactive Controller Test");

        when(reactiveRepository.findById("123")).thenReturn(Mono.just(message));

        Mono<Message> result = webFluxController.findByIdReactive("123");
        StepVerifier.create(result)
                .assertNext(msg -> assertEquals("Reactive Controller Test", msg.getContent()))
                .verifyComplete();
    }

    @Test
    public void testPostReactive() {
        Message message = new Message();
        message.setId("456");
        message.setContent("Post Test");

        when(reactiveRepository.save(message)).thenReturn(Mono.just(message));

        Mono<Message> result = webFluxController.postReactive(message);
        StepVerifier.create(result)
                .assertNext(msg -> assertEquals("Post Test", msg.getContent()))
                .verifyComplete();
    }

    @Test
    public void testDeleteAllReactive() {
        when(reactiveRepository.deleteAll()).thenReturn(Mono.empty());

        Mono<Void> result = webFluxController.deleteAllReactive();
        StepVerifier.create(result)
                .verifyComplete();

        verify(reactiveRepository, times(1)).deleteAll();
    }
}
