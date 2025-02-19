package projecttest.springreactivenonreactive.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import projecttest.springreactivenonreactive.model.Message;

@Repository
public interface ReactiveRepository extends ReactiveMongoRepository<Message, String> {

}
