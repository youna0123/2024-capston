package team.suajung.ad.ress.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import team.suajung.ad.ress.auth.model.Role;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {

    Optional<Role> findByName(Role.ERole name);
}