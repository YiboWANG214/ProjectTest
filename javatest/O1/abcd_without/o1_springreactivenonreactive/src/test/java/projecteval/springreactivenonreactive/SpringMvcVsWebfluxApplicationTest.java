package projecteval.springreactivenonreactive;



////////////////////////////////////////////////////////////////////////////////
// SpringMvcVsWebfluxApplicationTest.java
////////////////////////////////////////////////////////////////////////////////

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringMvcVsWebfluxApplicationTest {

    @Test
    void contextLoads() {
        // Just checks if the Spring application context loads successfully
    }

    @Test
    void mainMethodTest() {
        SpringMvcVsWebfluxApplication.main(new String[] {});
        // If no exception is thrown, the test passes
    }
}