package projecteval.logrequestresponseundertow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;

class ApplicationTest {

    @Nested
    @DisplayName("Test UndertowServletWebServerFactory Bean Creation")
    class UndertowFactoryTest {

        @Test
        @DisplayName("Test undertowServletWebServerFactory() not null")
        void testUndertowServletWebServerFactoryMethod() {
            // Arrange
            Application application = new Application();

            // Act
            UndertowServletWebServerFactory factory = application.undertowServletWebServerFactory();

            // Assert
            assertThat(factory).isNotNull();
        }

        @Test
        @DisplayName("Test UndertowServletWebServerFactory() second bean not null")
        void testUndertowServletWebServerFactoryBeanMethod() {
            // Arrange
            Application application = new Application();

            // Act
            UndertowServletWebServerFactory factory = application.UndertowServletWebServerFactory();

            // Assert
            assertThat(factory).isNotNull();
        }
    }
}

// ======================================================================

