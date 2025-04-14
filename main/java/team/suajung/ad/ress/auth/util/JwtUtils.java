package team.suajung.ad.ress.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import team.suajung.ad.ress.auth.exception.JwtAuthenticationException;
import team.suajung.ad.ress.auth.service.UserDetailsImpl;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public void validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("유효하지 않은 JWT 토큰 형식입니다");
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("인증 토큰이 만료되었습니다. 다시 로그인해주세요");
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("지원되지 않는 JWT 토큰입니다");
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims 문자열이 비어있습니다");
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
    }
}

