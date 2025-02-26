package projecteval.springdatamongowithcluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;


/**
 * Unit tests for SpringDataMongoWithClusterApplication.
 */
@ExtendWith(MockitoExtension.class)
public class SpringDataMongoWithClusterApplicationTests {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SpringDataMongoWithClusterApplication application;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test for main method to ensure no exceptions are thrown.
     * This won't start the full Spring context because of how tests are structured.
     */
    @Test
    public void testMainMethod() {
        // Just ensure it doesn't throw
        SpringDataMongoWithClusterApplication.main(new String[]{});
        // No exception means success
        Assertions.assertTrue(true);
    }

    /**
     * Test the run method to verify the MongoTemplate interactions.
     */
    @Test
    public void testRunMethod() {
        when(mongoTemplate.count(any(Query.class), any(Class.class))).thenReturn(2L);

        // Execute the 'run' method
        application.run();

        // Verify that dropCollection was called once
        verify(mongoTemplate, times(1)).dropCollection(Car.class);

        // Verify that save was called twice - for "Tesla Model S" and "Tesla Model 3"
        verify(mongoTemplate, times(2)).save(any(Car.class));

        // Verify that count was indeed called
        verify(mongoTemplate, times(1)).count(any(Query.class), any(Class.class));
    }
}


