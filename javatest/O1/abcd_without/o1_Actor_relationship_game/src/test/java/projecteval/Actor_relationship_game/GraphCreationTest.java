package projecteval.Actor_relationship_game.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.Actor_relationship_game.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.ResponseBody;

import java.io.*;
import java.util.*;

/* 
  -------------------------------------------------------------------
  GraphCreationTest.java
  -------------------------------------------------------------------
*/
@ExtendWith(MockitoExtension.class)
public class GraphCreationTest {

    @Mock
    private TMDBApi mockTmdbApi;

    @InjectMocks
    private GraphCreation graphCreation;

    @BeforeEach
    public void setup() {
        // We inject the mockTmdbApi into GraphCreation by reflection,
        // or ensure the constructor can accept a mock in real scenarios.
        // For demonstration, we assume we can do it via reflection or 
        // an alternative constructor. 
        // If not possible, you can refactor the code to allow injection 
        // or partial mocking.
    }

    @Test
    public void testCreateGraph() throws IOException {
        // Mock popular actors JSON
        String mockPopularActorsJson = "{ \"results\": [" +
                "{ \"id\": 1, \"name\": \"Actor One\" }, " +
                "{ \"id\": 2, \"name\": \"Actor Two\" } " +
                "] }";
        when(mockTmdbApi.searchPopularActors()).thenReturn(mockPopularActorsJson);

        // Mock movies for each actor
        String mockMovieJsonActor1 = "{ \"cast\": [" +
                "{ \"id\": 101, \"title\": \"Movie A\" }," +
                "{ \"id\": 102, \"title\": \"Movie B\" }" +
                "] }";
        String mockMovieJsonActor2 = "{ \"cast\": [" +
                "{ \"id\": 201, \"title\": \"Movie C\" }" +
                "] }";

        when(mockTmdbApi.getMoviesByActorId("1")).thenReturn(mockMovieJsonActor1);
        when(mockTmdbApi.getMoviesByActorId("2")).thenReturn(mockMovieJsonActor2);

        String testFile = "test_graph.ser";
        try {
            graphCreation.createGraph(testFile);

            // Verify the file was created
            File file = new File(testFile);
            assertTrue(file.exists());

            // Verify content
            ActorGraph loadedGraph = loadGraphFromFile(testFile);
            assertNotNull(loadedGraph);
            assertEquals(2, loadedGraph.getActors().size());
            assertEquals(3, loadedGraph.getMovies().size());

        } finally {
            // cleanup
            File file = new File("test_graph.ser");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    // Helper to load the actor graph from the file
    private ActorGraph loadGraphFromFile(String fileName) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            return (ActorGraph) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testPopulateGraphWithActors_WhenEmptyJson() throws IOException {
        when(mockTmdbApi.searchPopularActors()).thenReturn("{ \"results\": [] }");
        String testFile = "empty_test_graph.ser";
        try {
            graphCreation.createGraph(testFile);

            ActorGraph loadedGraph = loadGraphFromFile(testFile);
            assertNotNull(loadedGraph);
            assertTrue(loadedGraph.getActors().isEmpty());
            assertTrue(loadedGraph.getMovies().isEmpty());
        } finally {
            // Cleanup
            File file = new File(testFile);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
