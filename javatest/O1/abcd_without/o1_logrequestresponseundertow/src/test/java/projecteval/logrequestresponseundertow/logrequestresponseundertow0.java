package projecteval.logrequestresponseundertow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;

@SpringBootTest
class SongTest {

    @Test
    @DisplayName("Test Song getters and setters")
    void testSongGettersAndSetters() {
        Song song = new Song();
        song.setId(10L);
        song.setName("TestSong");
        song.setAuthor("TestAuthor");

        assertThat(song.getId()).isEqualTo(10L);
        assertThat(song.getName()).isEqualTo("TestSong");
        assertThat(song.getAuthor()).isEqualTo("TestAuthor");
    }

    @Test
    @DisplayName("Test Song builder")
    void testSongBuilder() {
        Song song = Song.builder()
                .id(1L)
                .name("BuilderSong")
                .author("BuilderAuthor")
                .build();

        assertThat(song.getId()).isEqualTo(1L);
        assertThat(song.getName()).isEqualTo("BuilderSong");
        assertThat(song.getAuthor()).isEqualTo("BuilderAuthor");
    }
}

// ======================================================================

