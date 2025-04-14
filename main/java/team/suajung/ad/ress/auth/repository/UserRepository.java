package team.suajung.ad.ress.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import team.suajung.ad.ress.auth.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}

