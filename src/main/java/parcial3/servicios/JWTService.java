package parcial3.servicios;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Servicio para generar y validar JWTs.
 */
public class JWTService {

    // Clave secreta (en un proyecto real, usar ENV o un Vault)
    // Puedes usar System.getenv("JWT_SECRET") si prefieres.
    private static final String SECRET_KEY = "MI_SECRETO_SUPER_SEGURO_1234567890_XXXX";

    // Tiempo de expiración de tokens (por ejemplo, 1 hora).
    private static final long EXPIRATION_TIME = 3600000L;

    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * Generar un JWT con el username.
     */
    public static String generateToken(String username) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validar un JWT y retornar el "subject" (username) si es válido.
     * Lanza excepción si no es válido o expiró.
     */
    public static String validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return claimsJws.getBody().getSubject();
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido o expirado.");
        }
    }
}
