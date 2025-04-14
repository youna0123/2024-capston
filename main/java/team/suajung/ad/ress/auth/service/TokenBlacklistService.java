package team.suajung.ad.ress.auth.service;

import org.springframework.stereotype.Service;

@Service
public interface TokenBlacklistService {
    void addBlacklist(String token);
    boolean isBlacklisted(String token);
}
