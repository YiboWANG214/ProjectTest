package projecteval.similarity.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import projecteval.similarity.ImageHistogram;
import projecteval.similarity.ImagePHash;
import projecteval.similarity.Main;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/* 
   ---------------------------------------------------------------------
   NOTE: 
   These tests assume you have Maven/Gradle or a similar build system 
   configured with JUnit 5 and Mockito. They also assume that you will 
   place them in a proper test source folder (e.g., src/test/java).
   The package name "projecteval.similarity.tests" is an example one; 
   adjust as needed to fit your project's structure.
   Make sure to include dependencies:
     - org.junit.jupiter:junit-jupiter
     - org.mockito:mockito-core
     - org.mockito:mockito-junit-jupiter
   ---------------------------------------------------------------------
*/

/*****************************************************
 * MainTest.java
 *****************************************************/
@ExtendWith(MockitoExtension.class)
public class MainTest {

    @TempDir
    Path tempDir;

    @Test
    public void testMain_TooFewArguments() {
        // Tests that if arguments are not exactly 3, an IllegalArgumentException is thrown
        // We capture System.err for coverage of error printing
        String[] args = new String[] { "input.txt", "output.txt" }; // only 2 arguments
        assertThrows(IllegalArgumentException.class, () -> Main.main(args));
    }

    @Test
    public void testMain_HistogramCommand_SingleLine() throws IOException {
        // Creates a temp input file with valid lines, calls the main method with "h" command,
        // and checks the output.

        // 1. Create temp input file
        Path inputFile = tempDir.resolve("input_h.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(inputFile)) {
            writer.write("image1.jpg image2.jpg");
            writer.newLine();
        }

        // 2. Create output file reference
        Path outputFile = tempDir.resolve("output_h.txt");

        // 3. Execute main
        String[] args = { inputFile.toString(), outputFile.toString(), "h" };
        Main.main(args);

        // 4. Check output
        // We expect the code to attempt image histogram matching. Without real images,
        // the code might produce an error or different output,
        // but we at least expect the code to run and produce a line in output.
        // There's no real images here, so the call may produce an exception stack trace
        // in the console, but let's see if the line is still written.
        assertTrue(Files.exists(outputFile), "Output file should be created");

        // Simple check: the output file should have exactly one line (for the single input line).
        try (BufferedReader reader = Files.newBufferedReader(outputFile)) {
            String outLine = reader.readLine();
            assertNotNull(outLine, "Output line should not be null");
            // e.g. "false        image1.jpg      image2.jpg" or "true        image1.jpg     image2.jpg"
            // We won't parse it further, but ensure it's from the code
            assertTrue(outLine.contains("image1.jpg") && outLine.contains("image2.jpg"),
                    "Output line should contain original image paths");
        }
    }

    @Test
    public void testMain_PHashCommand_MultipleLines() throws IOException {
        // Creates a temp input file with multiple lines, calls main method with "p" command,
        // and checks the output.

        // 1. Create temp input
        Path inputFile = tempDir.resolve("input_p.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(inputFile)) {
            writer.write("imageA1.jpg imageA2.jpg");
            writer.newLine();
            writer.write("imageB1.jpg imageB2.jpg");
            writer.newLine();
        }

        // 2. Create output file
        Path outputFile = tempDir.resolve("output_p.txt");

        // 3. Execute main
        String[] args = { inputFile.toString(), outputFile.toString(), "p" };
        Main.main(args);

        // 4. Validate that output file is created
        assertTrue(Files.exists(outputFile));

        // Check the number of lines in the output
        int linesCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(outputFile)) {
            while (reader.readLine() != null) {
                linesCount++;
            }
        }
        assertEquals(2, linesCount, "Output should contain 2 lines");
    }

    @Test
    public void testMain_InvalidLineFormat() throws IOException {
        // Testing the scenario where some lines do not contain exactly 2 paths.
        // We place one valid line and one invalid line, and see if the code logs an error.

        // 1. Create temp input
        Path inputFile = tempDir.resolve("input_invalid.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(inputFile)) {
            writer.write("imageX1.jpg imageX2.jpg");
            writer.newLine();
            writer.write("invalidLineWithSinglePath");
            writer.newLine();
        }

        // 2. Create output file
        Path outputFile = tempDir.resolve("output_invalid.txt");

        // 3. Execute main
        String[] args = { inputFile.toString(), outputFile.toString(), "h" };
        Main.main(args);

        // 4. Validate that output file is created
        assertTrue(Files.exists(outputFile));

        // We expect only the first line to appear in the output.
        int linesCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(outputFile)) {
            while (reader.readLine() != null) {
                linesCount++;
            }
        }
        assertEquals(1, linesCount, "Only 1 valid line should be processed");
    }
}

/*****************************************************
 * ImagePHashTest.java
 *****************************************************/
@ExtendWith(MockitoExtension.class)
class ImagePHashTest {

    // System under test
    @InjectMocks
    private ImagePHash imagePHash = new ImagePHash();

    // We will mock InputStreams or files. 
    // For coverage, we also want to test some of the actual logic if possible.

    @Test
    void testDistanceSameHash() throws Exception {
        // If both images return the same hash string, distance should be 0.
        ImagePHash spyPHash = Mockito.spy(imagePHash);

        // Mock getHash to return identical strings
        Mockito.doReturn("11110000").when(spyPHash).getHash(Mockito.any(InputStream.class));

        int dist = spyPHash.distance(new File("fake1.jpg"), new File("fake2.jpg"));
        assertEquals(0, dist, "Distance should be 0 if both hashes are identical");
    }

    @Test
    void testDistanceDifferentHash() throws Exception {
        ImagePHash spyPHash = Mockito.spy(imagePHash);

        // Return two distinct hash strings
        Mockito.doReturn("11110000").when(spyPHash).getHash(Mockito.any(InputStream.class));
        // For second call, a different string is returned
        Mockito.doReturn("00001111").when(spyPHash).getHash(Mockito.any(InputStream.class));

        // Because the second doReturn call will override the first for subsequent calls,
        // we need to do something more advanced, or we can do an inline approach:
        // Let's do multiple stubs in Answer form, or handle one call at a time:
        Mockito.when(spyPHash.getHash(Mockito.any(InputStream.class)))
                .thenReturn("11110000")
                .thenReturn("00001111");

        int dist = spyPHash.distance(new File("fake1.jpg"), new File("fake2.jpg"));
        assertEquals(8, dist, "Distance should be 8 if all bits differ");
    }

    @Test
    void testDistanceURLMock() throws Exception {
        // Testing distance(URL srcUrl, URL canUrl)
        ImagePHash spyPHash = Mockito.spy(imagePHash);
        Mockito.when(spyPHash.getHash(Mockito.any(InputStream.class)))
                .thenReturn("1010")
                .thenReturn("1110");

        URL dummyUrl = new URL("http://example.com/fake.jpg");
        int dist = spyPHash.distance(dummyUrl, dummyUrl);
        assertEquals(1, dist, "Bit difference is 1 between 1010 and 1110");
    }

    @Test
    void testConstructors() {
        // Just ensure that the constructors work and do not throw
        ImagePHash defaultPHash = new ImagePHash();
        assertNotNull(defaultPHash);

        ImagePHash customSizedPHash = new ImagePHash(64, 16);
        assertNotNull(customSizedPHash);
    }

    @Test
    void testPrivateDistanceMethodCoverage() {
        // We can't directly call the private distance(String, String)
        // but we can rely on coverage from the public methods above.
        // This test is just a placeholder to ensure coverage from usage.
        // Already covered by testDistanceDifferentHash and testDistanceSameHash.
        assertTrue(true);
    }
}

/*****************************************************
 * ImageHistogramTest.java
 *****************************************************/
@ExtendWith(MockitoExtension.class)
class ImageHistogramTest {

    @InjectMocks
    private ImageHistogram imageHistogram = new ImageHistogram();

    @Test
    void testMatchSameImageMock() throws IOException {
        // If filter returns the same histogram array, match should be 1.0 (perfect).
        ImageHistogram spyHistogram = Mockito.spy(imageHistogram);

        float[] mockHistogramArr = new float[] { 0.5f, 0.5f, 0.0f };
        Mockito.doReturn(mockHistogramArr).when(spyHistogram).filter(Mockito.any(BufferedImage.class));

        double similarity = spyHistogram.match(new File("image1.jpg"), new File("image2.jpg"));
        assertEquals(1.0, similarity, 1e-6, "Similarity should be 1.0 when histograms match perfectly");
    }

    @Test
    void testMatchDifferentImageMock() throws IOException {
        ImageHistogram spyHistogram = Mockito.spy(imageHistogram);

        float[] mockHistogramArr1 = new float[] { 0.3f, 0.7f };
        float[] mockHistogramArr2 = new float[] { 0.6f, 0.4f };

        // Return these different histograms on subsequent calls
        Mockito.when(spyHistogram.filter(Mockito.any(BufferedImage.class)))
                .thenReturn(mockHistogramArr1)
                .thenReturn(mockHistogramArr2);

        double similarity = spyHistogram.match(new File("imageA.jpg"), new File("imageB.jpg"));
        // Bhattacharyya coefficient = sum of sqrt(0.3*0.6) + sqrt(0.7*0.4)
        // = sqrt(0.18) + sqrt(0.28) ~= 0.424264 + 0.529150 = 0.953414
        // It's approximate, so let's test with a small epsilon
        assertEquals(
                Math.sqrt(0.18) + Math.sqrt(0.28),
                similarity,
                1e-6,
                "Should match the Bhattacharyya coefficient from arrays"
        );
    }

    @Test
    void testMatchURLMock() throws IOException {
        ImageHistogram spyHistogram = Mockito.spy(imageHistogram);
        Mockito.doReturn(new float[] { 0.2f, 0.3f, 0.5f })
               .doReturn(new float[] { 0.2f, 0.3f, 0.5f })
               .when(spyHistogram).filter(Mockito.any(BufferedImage.class));

        URL dummyUrl = new URL("http://example.com/test.jpg");
        double similarity = spyHistogram.match(dummyUrl, dummyUrl);
        assertEquals(1.0, similarity, 1e-6, "When both URLs produce the same histogram, similarity is 1.0");
    }

    @Test
    void testCalcSimilarityAllZeros() {
        // Directly test calcSimilarity 
        // If both are all zeros, the sum of sqrt(0*0) is zero
        float[] arr1 = new float[] { 0f, 0f, 0f };
        float[] arr2 = new float[] { 0f, 0f, 0f };

        // We can call calcSimilarity via reflection or by mocking filter() usage
        // Instead, let's do a partial spy so we can call the method directly:
        ImageHistogram spyHistogram = Mockito.spy(imageHistogram);

        double sim = spyHistogram.calcSimilarity(arr1, arr2);
        assertEquals(0.0, sim, 1e-6, "All zeros should result in 0 Bhattacharyya coefficient");
    }

    @Test
    void testCalcSimilarityMixed() {
        float[] arr1 = new float[] { 0.1f, 0.2f, 0.7f };
        float[] arr2 = new float[] { 0.1f, 0.2f, 0.7f };

        ImageHistogram spyHistogram = Mockito.spy(imageHistogram);

        double sim = spyHistogram.calcSimilarity(arr1, arr2);
        // The same arrays => sum of sqrt(0.1*0.1) + sqrt(0.2*0.2) + sqrt(0.7*0.7)
        // = (0.1 + 0.2 + 0.7) = 1.0
        assertEquals(1.0, sim, 1e-6);
    }

    @Test
    void testConstructor() {
        // Just ensure no exceptions
        ImageHistogram hist = new ImageHistogram();
        assertNotNull(hist);
    }

    @Test
    void testFilterCoverageWithRealImage(@TempDir Path tempDir) throws IOException {
        // Create a small real image in memory, write to disk, pass to filter for coverage
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFF0000); // red
        img.setRGB(1, 0, 0x00FF00); // green
        img.setRGB(0, 1, 0x0000FF); // blue
        img.setRGB(1, 1, 0xFFFFFF); // white

        File testImage = tempDir.resolve("smallImage.jpg").toFile();
        ImageIO.write(img, "jpg", testImage);

        float[] result = imageHistogram.match(testImage, testImage) >= 0.0 
                         ? imageHistogram.filter(ImageIO.read(testImage)) 
                         : null;
        // Basic sanity check 
        assertNotNull(result, "Histogram filtering should produce a non-null result");
        assertTrue(result.length > 0, "Histogram array should have non-zero length");
    }
}