package io.github.eeki.cxr.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Сервис для работы с JWT-токенами.
 *
 * <p>Генерирует токены при логине, извлекает из них данные,
 * проверяет срок действия и подпись. Секретный ключ читается
 * из конфигурации ({@code app.jwt.secret}) и должен быть
 * base64-строкой минимум на 256 бит.</p>
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Создаёт сервис с заданным секретом и временем жизни токена.
     *
     * @param secret       base64-строка с секретом подписи
     * @param expirationMs срок жизни токена в миллисекундах
     */
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /** Извлекает логин пользователя из токена. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Извлекает произвольное поле из payload токена. */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    /** Генерирует JWT-токен для пользователя. */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /** Проверяет, что токен валиден и принадлежит указанному пользователю. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && extractClaim(token, Claims::getExpiration).after(new Date());
    }
}
