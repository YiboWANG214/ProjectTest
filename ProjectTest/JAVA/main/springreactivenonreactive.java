// springreactivenonreactive/repository/NonReactiveRepository.java

package projecttest.springreactivenonreactive.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import projecttest.springreactivenonreactive.model.Message;

@Repository
public interface NonReactiveRepository extends MongoRepository<Message, String> {

}


// springreactivenonreactive/repository/ReactiveRepository.java

package projecttest.springreactivenonreactive.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import projecttest.springreactivenonreactive.model.Message;

@Repository
public interface ReactiveRepository extends ReactiveMongoRepository<Message, String> {

}


// springreactivenonreactive/model/Message.java

package projecttest.springreactivenonreactive.model;

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Message {
    
    @Id
    private String id;
    
    @NotBlank
    private String content;
    
    @NotNull
    private Date createdAt = new Date();
    
}

// springreactivenonreactive/controller/WebFluxController.java

package projecttest.springreactivenonreactive.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projecttest.springreactivenonreactive.model.Message;
import projecttest.springreactivenonreactive.repository.ReactiveRepository;

import reactor.core.publisher.Mono;

@RestController
public class WebFluxController {
    
    @Autowired
    ReactiveRepository reactiveRepository;
    
    @RequestMapping("/webflux/{id}")
    public Mono<Message> findByIdReactive(@PathVariable(value = "id") String id) {
        return reactiveRepository.findById(id);
    }
    
    @PostMapping("/webflux")
    public Mono<Message> postReactive(@Valid @RequestBody Message message) {
        return reactiveRepository.save(message);
    }
    
    @DeleteMapping("/webflux")
    public Mono<Void> deleteAllReactive() {
        return reactiveRepository.deleteAll();
    }
}



// springreactivenonreactive/controller/MVCSyncController.java

package projecttest.springreactivenonreactive.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projecttest.springreactivenonreactive.model.Message;
import projecttest.springreactivenonreactive.repository.NonReactiveRepository;

@RestController
public class MVCSyncController {
    
    @Autowired
    NonReactiveRepository nonReactiveRepository;
    
    @RequestMapping("/mvcsync/{id}")
    public Message findById(@PathVariable(value = "id") String id) {
        return nonReactiveRepository.findById(id).orElse(null);
    }
    
    @PostMapping("/mvcsync")
    public Message post(@Valid @RequestBody Message message) {
        return nonReactiveRepository.save(message);
    }
}



// springreactivenonreactive/controller/MVCAsyncController.java

package projecttest.springreactivenonreactive.controller;

import java.util.concurrent.CompletableFuture;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projecttest.springreactivenonreactive.model.Message;
import projecttest.springreactivenonreactive.repository.NonReactiveRepository;

@RestController
public class MVCAsyncController {
    
    @Autowired
    NonReactiveRepository nonReactiveRepository;
    
    @RequestMapping("/mvcasync/{id}")
    public CompletableFuture<Message> findById(@PathVariable(value = "id") String id) {
        return CompletableFuture.supplyAsync(() -> nonReactiveRepository.findById(id).orElse(null));
    }
    
    @PostMapping("/mvcasync")
    public CompletableFuture<Message> post(@Valid @RequestBody Message message) {
        return CompletableFuture.supplyAsync(() -> nonReactiveRepository.save(message));
    }
}



// springreactivenonreactive/SpringMvcVsWebfluxApplication.java

package projecttest.springreactivenonreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringMvcVsWebfluxApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringMvcVsWebfluxApplication.class, args);
	}

}


