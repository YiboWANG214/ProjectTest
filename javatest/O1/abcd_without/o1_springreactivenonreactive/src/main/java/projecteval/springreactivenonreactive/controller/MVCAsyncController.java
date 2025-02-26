package projecteval.springreactivenonreactive.controller;

import java.util.concurrent.CompletableFuture;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projecteval.springreactivenonreactive.model.Message;
import projecteval.springreactivenonreactive.repository.NonReactiveRepository;

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

