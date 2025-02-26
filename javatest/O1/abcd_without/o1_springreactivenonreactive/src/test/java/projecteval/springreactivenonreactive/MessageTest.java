package projecteval.springreactivenonreactive;


////////////////////////////////////////////////////////////////////////////////
// MessageTest.java
////////////////////////////////////////////////////////////////////////////////

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import projecteval.springreactivenonreactive.model.Message;

public class MessageTest {

    private Message message;

    @BeforeEach
    public void setup() {
        message = new Message();
    }

    @Test
    public void testGetSetId() {
        message.setId("testId");
        assertEquals("testId", message.getId());
    }

    @Test
    public void testGetSetContent() {
        message.setContent("Hello World");
        assertEquals("Hello World", message.getContent());
    }

    @Test
    public void testCreatedAtNotNull() {
        assertNotNull(message.getCreatedAt());
    }

    @Test
    public void testToString() {
        message.setId("id");
        message.setContent("content");
        String result = message.toString();
        assertNotNull(result);
        // Basic sanity check for presence of fields in toString
        assertEquals(true, result.contains("id"));
        assertEquals(true, result.contains("content"));
    }
}

