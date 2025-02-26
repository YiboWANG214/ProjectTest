package projecteval.springuploads3;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpringUploadS3ApplicationTest {

    @Test
    void testMainMethod() {
        // We only test that no exceptions are thrown
        assertDoesNotThrow(() -> {
            SpringUploadS3Application.main(new String[] {});
        });
    }
}