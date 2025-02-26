package projecteval.bankingApplication;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

class ConnectionTest {

    @Test
    void testGetConnectionNotNull() {
        // This is more of an integration test, but for completeness:
        // We'll just verify that the method doesn't return null.
        Connection conn = connection.getConnection();
        // Might be null if local DB isn't configured, but at least we can check the object reference
        assertNotNull(conn, "Expected a non-null Connection object (Check DB config if null).");
    }
}

//--------------------------------------------------------------
// BankTest.java
//--------------------------------------------------------------
