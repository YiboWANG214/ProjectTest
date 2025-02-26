package projecteval.springreactivenonreactive;

////////////////////////////////////////////////////////////////////////////////
// ReactiveRepositoryTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.ReactiveRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class ReactiveRepositoryTest {

    @Mock
    private ReactiveRepository reactiveRepository;

    @Test
    public void testFindById() {
        Message message = new Message();
        message.setId("abc");
        message.setContent("Reactive Content");

        when(reactiveRepository.findById("abc")).thenReturn(Mono.just(message));

        Mono<Message> result = reactiveRepository.findById("abc");
        StepVerifier.create(result)
                .expectNextMatches(msg -> msg.getContent().equals("Reactive Content"))
                .verifyComplete();

        verify(reactiveRepository, times(1)).findById("abc");
    }

    @Test
    public void testSave() {
        Message message = new Message();
        message.setId("abc");
        message.setContent("Save Reactive");

        when(reactiveRepository.save(message)).thenReturn(Mono.just(message));

        Mono<Message> saved = reactiveRepository.save(message);
        StepVerifier.create(saved)
                .expectNextMatches(msg -> msg.getContent().equals("Save Reactive"))
                .verifyComplete();

        verify(reactiveRepository, times(1)).save(message);
    }
}

