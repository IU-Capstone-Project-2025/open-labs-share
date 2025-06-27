package olsh.backend.authservice.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {
    @Value("${token.signing.key}")
    private String jwtSigningKey;

    @Value("${token.access.expiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${token.refresh.expiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    // In-memory token blacklist for now
    // TODO: Replace with a persistent store, maybe database.
    private static final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String uniqueTokenId = java.util.UUID.randomUUID().toString();
        extraClaims.put("tokenId", uniqueTokenId);

        String username = userDetails != null ? userDetails.getUsername() : "";

        return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User customUserDetails) {
            claims.put("id", customUserDetails.getUserId());
            claims.put("email", customUserDetails.getUsername());
            claims.put("role", customUserDetails.getRole());
        }
        return generateToken(claims, userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String username = "";

        if (userDetails instanceof User customUserDetails) {
            username = customUserDetails.getUsername();
            claims.put("id", customUserDetails.getUserId());
            claims.put("email", customUserDetails.getUsername());
            claims.put("role", customUserDetails.getRole());
            claims.put("type", "refresh");
            claims.put("tokenId", java.util.UUID.randomUUID().toString());
        } else if (userDetails != null) {
            username = userDetails.getUsername();
            claims.put("tokenId", java.util.UUID.randomUUID().toString());
            claims.put("type", "refresh");
        }

        return Jwts
            .builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(
                System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUsername(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
            .parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void blacklistToken(String token) {
        try {
            blacklistedTokens.add(token);
            String username = extractUsername(token);
            log.info("Token for user {} has been blacklisted", username);
        } catch (Exception e) {
            log.warn("Error blacklisting token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public boolean isTokenValidAndNotBlacklisted(String token, UserDetails userDetails) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        return isTokenValid(token, userDetails);
    }
}
