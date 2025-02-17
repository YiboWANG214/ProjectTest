package projecteval.springreactivenonreactive.controller;

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

