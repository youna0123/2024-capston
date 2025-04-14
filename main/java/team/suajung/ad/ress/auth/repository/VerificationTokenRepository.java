package team.suajung.ad.ress.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import team.suajung.ad.ress.auth.model.VerificationToken;

import java.util.Optional;

public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserIdAndType(String userId, VerificationToken.TokenType type);
}
