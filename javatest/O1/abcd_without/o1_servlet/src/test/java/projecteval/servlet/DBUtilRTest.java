package projecteval.servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

/*
  Tests for DBUtilR.java. It's a static init with a real connection.
  We'll just call getDBConnection() to ensure it doesn't error out 
  under your local environment. In real usage, you'd configure or mock 
  the DB driver or environment.
*/

public class DBUtilRTest {

    @Test
    public void testGetDBConnection() {
        Connection con = DBUtilR.getDBConnection();
        // Depending on environment, con may or may not be null if your DB is accessible.
        // We'll just assert it's not forcibly closed if no exceptions are thrown at load time.
        // If your environment doesn't have a DB, please mock/adjust accordingly.
        assertNotNull(con, "DB Connection should not be null if initialization succeeded.");
    }
}

