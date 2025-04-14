package team.suajung.ad.ress.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.suajung.ad.ress.auth.util.JwtUtils;
import team.suajung.ad.ress.dao.RedisDao;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private final RedisDao redisDao;
    private final JwtUtils jwtUtils;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public void addBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        Claims claims = jwtUtils.parseClaims(token);
        Date expiration = claims.getExpiration();
        long now = new Date().getTime();
        long remainingExpiration = expiration.getTime() - now;

        if (remainingExpiration > 0) {
            redisDao.setValues(key, "", Duration.ofMillis(remainingExpiration));
        }
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisDao.getValues(key) != null;
    }
}

