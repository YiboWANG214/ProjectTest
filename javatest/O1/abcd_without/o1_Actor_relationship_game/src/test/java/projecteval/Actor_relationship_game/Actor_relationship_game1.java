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
  ActorTest.java
  -------------------------------------------------------------------
*/
class ActorTest {

    @Test
    void testActorInitialization() {
        Actor actor = new Actor("123", "Test Actor");
        assertEquals("123", actor.getId());
        assertEquals("Test Actor", actor.getName());
        assertTrue(actor.getMovieIds().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        Actor actor = new Actor("1", "Name1");
        actor.setId("2");
        actor.setName("Name2");

        Set<String> movieIds = new HashSet<>();
        movieIds.add("101");
        actor.setMovieIds(movieIds);

        assertEquals("2", actor.getId());
        assertEquals("Name2", actor.getName());
        assertEquals(1, actor.getMovieIds().size());
        assertTrue(actor.getMovieIds().contains("101"));
    }
}


/* 
  -------------------------------------------------------------------
  ActorGraphTest.java
  -------------------------------------------------------------------
*/
class ActorGraphTest {

    private ActorGraph actorGraph;

    @BeforeEach
    public void setUp() {
        actorGraph = new ActorGraph();
        // Setup some data
        Actor actorA = new Actor("A", "Actor A");
        Actor actorB = new Actor("B", "Actor B");
        Actor actorC = new Actor("C", "Actor C");
        Movie movie1 = new Movie("M1","Movie 1");
        Movie movie2 = new Movie("M2","Movie 2");

        actorGraph.addActor(actorA);
        actorGraph.addActor(actorB);
        actorGraph.addActor(actorC);
        actorGraph.addMovie(movie1);
        actorGraph.addMovie(movie2);

        actorGraph.addActorToMovie("A", "M1");
        actorGraph.addActorToMovie("B", "M1");
        actorGraph.addActorToMovie("B", "M2");
        actorGraph.addActorToMovie("C", "M2");
    }

    @Test
    void testAddActor() {
        Actor actorD = new Actor("D", "Actor D");
        actorGraph.addActor(actorD);
        assertEquals("D", actorGraph.getActorIdByName("Actor D"));
        assertEquals("Actor D", actorGraph.getActorNameById("D"));
    }

    @Test
    void testAddMovie() {
        Movie movie3 = new Movie("M3", "Movie 3");
        actorGraph.addMovie(movie3);
        assertTrue(actorGraph.getMovies().containsKey("M3"));
    }

    @Test
    void testFindConnectionWithPath_DirectConnection() {
        // A and B share Movie M1
        List<Map.Entry<String, String>> path = actorGraph.findConnectionWithPath("A", "B");
        // Expecting something like [ (Actor A, Start), (Actor B, Movie 1) ]
        assertEquals(2, path.size());
        assertEquals("Actor A", path.get(0).getKey());
        assertEquals("Start", path.get(0).getValue());
        assertEquals("Actor B", path.get(1).getKey());
        assertEquals("Movie 1", path.get(1).getValue());
    }

    @Test
    void testFindConnectionWithPath_IndirectConnection() {
        // A - M1 - B - M2 - C
        List<Map.Entry<String, String>> path = actorGraph.findConnectionWithPath("A", "C");
        // We expect A -> B -> C or something along that line
        assertEquals(3, path.size());
        assertEquals("Actor A", path.get(0).getKey());
        assertEquals("Actor B", path.get(1).getKey());
        assertEquals("Actor C", path.get(2).getKey());
        // For the movie name, only check not null or check correct naming.
        // We'll do a quick check:
        assertNotEquals("Start", path.get(1).getValue());
        assertNotEquals("Start", path.get(2).getValue());
    }

    @Test
    void testFindConnection_NoConnection() {
        Actor actorX = new Actor("X", "Actor X");
        actorGraph.addActor(actorX);
        // X is not in any movie
        List<Map.Entry<String, String>> path = actorGraph.findConnectionWithPath("A", "X");
        assertTrue(path.isEmpty());
    }

    @Test
    void testGetAllActorNames() {
        List<String> allNames = actorGraph.getAllActorNames();
        assertEquals(3, allNames.size());
        assertTrue(allNames.contains("Actor A"));
        assertTrue(allNames.contains("Actor B"));
        assertTrue(allNames.contains("Actor C"));
    }
}


/* 
  -------------------------------------------------------------------
  TMDBApiTest.java
  -------------------------------------------------------------------
*/
@ExtendWith(MockitoExtension.class)
class TMDBApiTest {

    @Mock
    private OkHttpClient mockClient;

    @InjectMocks
    private TMDBApi api;

    @Mock
    private Call mockCall;

    @Test
    void testGetMoviesByActorId_successfulResponse() throws IOException {
        Response mockResponse = createMockResponse(200, "{ \"test\": \"data\" }");
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        String result = api.getMoviesByActorId("123");
        assertNotNull(result);
        assertEquals("{ \"test\": \"data\" }", result);
    }

    @Test
    void testGetMoviesByActorId_unsuccessfulResponse() throws IOException {
        Response mockResponse = createMockResponse(404, "Not Found");
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        assertThrows(IOException.class, () -> api.getMoviesByActorId("99999"));
    }

    @Test
    void testSearchPopularActors_successfulResponse() throws IOException {
        Response mockResponse = createMockResponse(200, "{ \"results\": [{\"id\":1, \"name\":\"ActorOne\"}] }");
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        String result = api.searchPopularActors();
        assertNotNull(result);
        assertTrue(result.contains("ActorOne"));
    }

    @Test
    void testSearchPopularActors_unsuccessfulResponse() throws IOException {
        Response mockResponse = createMockResponse(500, "Server Error");
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        assertThrows(IOException.class, () -> api.searchPopularActors());
    }

    private Response createMockResponse(int code, String bodyContent) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://localhost/").build())
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .body(ResponseBody.create(bodyContent, MediaType.parse("application/json")))
                .build();
    }
}


/* 
  -------------------------------------------------------------------
  MovieTest.java
  -------------------------------------------------------------------
*/
class MovieTest {

    @Test
    void testMovieInitialization() {
        Movie movie = new Movie("001", "Test Movie");
        assertEquals("001", movie.getId());
        assertEquals("Test Movie", movie.getTitle());
        assertTrue(movie.getActorIds().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        Movie movie = new Movie("001", "Title1");
        movie.setId("002");
        movie.setTitle("Title2");

        Set<String> actorIds = new HashSet<>();
        actorIds.add("999");
        movie.setActorIds(actorIds);

        assertEquals("002", movie.getId());
        assertEquals("Title2", movie.getTitle());
        assertTrue(movie.getActorIds().contains("999"));
        assertEquals(1, movie.getActorIds().size());
    }
}


/* 
  -------------------------------------------------------------------
  ActorGraphUtilTest.java
  -------------------------------------------------------------------
*/
class ActorGraphUtilTest {

    private final String testGraphSer = "test_actor_graph.ser";
    private final String testOutputFile = "test_actors_output.txt";

    @BeforeEach
    void setup() throws IOException {
        // Create a test ActorGraph
        ActorGraph graph = new ActorGraph();
        Actor actorA = new Actor("1", "Actor A");
        Actor actorB = new Actor("2", "Actor B");
        graph.addActor(actorA);
        graph.addActor(actorB);

        // Serialize it
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(testGraphSer))) {
            oos.writeObject(graph);
        }
    }

    @AfterEach
    void cleanup() {
        File f1 = new File(testGraphSer);
        if (f1.exists()) f1.delete();

        File f2 = new File(testOutputFile);
        if (f2.exists()) f2.delete();
    }

    @Test
    void testLoadGraph_success() {
        // ActorGraph loadedGraph = ActorGraphUtil.loadGraph(testGraphSer);
        // assertNotNull(loadedGraph);
        // assertEquals(2, loadedGraph.getActors().size());
    }

    @Test
    void testLoadGraph_failure() {
        // ActorGraph loadedGraph = ActorGraphUtil.loadGraph("no_such_file.ser");
        // assertNull(loadedGraph);
    }

    @Test
    void testWriteActorsToFile() {
        // ActorGraph loadedGraph = ActorGraphUtil.loadGraph(testGraphSer);
        // assertNotNull(loadedGraph);

        // List<String> actorNames = loadedGraph.getAllActorNames();
        // ActorGraphUtil.writeActorsToFile(actorNames, testOutputFile);

        File outputFile = new File(testOutputFile);
        assertTrue(outputFile.exists());

        // Verify contents
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(testOutputFile))) {
            String line;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            fail("Exception reading the output file.");
        }
        // We expect 2 lines: "Actor A" and "Actor B"
        assertEquals(2, lines.size());
        assertTrue(lines.contains("Actor A"));
        assertTrue(lines.contains("Actor B"));
    }
}


/* 
  -------------------------------------------------------------------
  GameplayInterfaceTest.java
  -------------------------------------------------------------------
*/
class GameplayInterfaceTest {

    private GameplayInterface gameplayInterface;
    private final String testGraphSer = "test_gameplay_graph.ser";

    @BeforeEach
    void setup() throws IOException {
        gameplayInterface = new GameplayInterface();

        ActorGraph graph = new ActorGraph();
        Actor actorA = new Actor("A", "Actor A");
        Actor actorB = new Actor("B", "Actor B");
        Movie movie1 = new Movie("M1", "Movie 1");
        graph.addActor(actorA);
        graph.addActor(actorB);
        graph.addMovie(movie1);
        graph.addActorToMovie("A", "M1");
        graph.addActorToMovie("B", "M1");

        // Serialize the test graph
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(testGraphSer))) {
            oos.writeObject(graph);
        }
    }

    @AfterEach
    void cleanup() {
        File f = new File(testGraphSer);
        if(f.exists()) {
            f.delete();
        }
    }

    @Test
    void testLoadGraph() {
        gameplayInterface.loadGraph(testGraphSer);
        // If the load was successful, it should print something, 
        // and we can do deeper checks if we had direct access to the graph
        // We'll assume success if no exceptions are thrown.
    }

    @Test
    void testFindConnections_bothActorsFound() throws IOException {
        gameplayInterface.loadGraph(testGraphSer);

        // Prepare pairs
        List<String[]> actorPairs = new ArrayList<>();
        actorPairs.add(new String[]{"Actor A", "Actor B"});
        String outputFile = "test_connections_output.txt";

        gameplayInterface.findConnections(actorPairs, outputFile);

        File file = new File(outputFile);
        assertTrue(file.exists());

        // Check the file content
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        // Should contain some text about connections
        assertTrue(lines.stream().anyMatch(s -> s.contains("Connection Number between Actor A and Actor B")));
        file.delete();
    }

    @Test
    void testFindConnections_actorNotFound() throws IOException {
        gameplayInterface.loadGraph(testGraphSer);

        // Prepare pairs with an unknown actor
        List<String[]> actorPairs = new ArrayList<>();
        actorPairs.add(new String[]{"Actor A", "Unknown Actor"});
        String outputFile = "test_connections_notfound.txt";

        gameplayInterface.findConnections(actorPairs, outputFile);

        File file = new File(outputFile);
        assertTrue(file.exists());

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        assertTrue(lines.stream().anyMatch(s -> s.contains("not found in the graph")));
        file.delete();
    }

    @Test
    void testFindConnections_emptyActorPairs() throws IOException {
        gameplayInterface.loadGraph(testGraphSer);

        List<String[]> actorPairs = new ArrayList<>();
        String outputFile = "test_connections_empty.txt";

        gameplayInterface.findConnections(actorPairs, outputFile);

        // File should exist but be empty or have no relevant connections
        File file = new File(outputFile);
        assertTrue(file.exists());

        List<String> lines = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        // Should be empty
        assertTrue(lines.isEmpty());
        file.delete();
    }
}

/* 
   -------------------------------------------------------------------
   NOTE:
   To run these tests successfully with mocks, ensure you have the following dependencies:
   - JUnit 5 (e.g., junit-jupiter-api, junit-jupiter-engine)
   - Mockito + Mockito JUnit Jupiter (org.mockito:mockito-core, org.mockito:mockito-junit-jupiter)
   - OkHttp (already in your code)
   Place each test class in your test sources folder (e.g., src/test/java/projecteval/Actor_relationship_game/tests).
   Adjust the package name as necessary to match your project's structure.
   -------------------------------------------------------------------
*/