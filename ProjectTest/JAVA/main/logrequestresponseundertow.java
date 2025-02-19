// logrequestresponseundertow/Song.java

package projecttest.logrequestresponseundertow;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song {

    private Long id;
    private String name;
    private String author;
}


// logrequestresponseundertow/Application.java

package projecttest.logrequestresponseundertow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import io.undertow.server.handlers.RequestDumpingHandler;
import lombok.extern.log4j.Log4j2;

@SpringBootApplication
@Log4j2
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public UndertowServletWebServerFactory undertowServletWebServerFactory() {
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        factory.addDeploymentInfoCustomizers(deploymentInfo ->
                deploymentInfo.addInitialHandlerChainWrapper(handler -> {
            return new RequestDumpingHandler(handler);
        }));
        
        return factory;
    }
    
    
    @Bean
    public UndertowServletWebServerFactory UndertowServletWebServerFactory() {
        UndertowServletWebServerFactory UndertowServletWebServerFactory = new UndertowServletWebServerFactory();

        UndertowServletWebServerFactory.addDeploymentInfoCustomizers(deploymentInfo -> deploymentInfo.addInitialHandlerChainWrapper(handler -> {
            return new RequestDumpingHandler(handler);
        }));

        return UndertowServletWebServerFactory;
    }
}

// logrequestresponseundertow/SongController.java

package projecttest.logrequestresponseundertow;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.web.bind.annotation.*;

@RestController
public class SongController {
    
    @PostMapping("/songs")
    public Song createSong(@RequestBody Song song) {
        song.setId(new Random().nextLong());
        
        return song;
    }
    
    @GetMapping("/songs")
    public List<Song> getSongs() {
        List<Song> songs = new ArrayList<>();

        songs.add(Song.builder().id(1L).name("name1").author("author2").build());
        songs.add(Song.builder().id(2L).name("name2").author("author2").build());

        return songs;
    }

}


