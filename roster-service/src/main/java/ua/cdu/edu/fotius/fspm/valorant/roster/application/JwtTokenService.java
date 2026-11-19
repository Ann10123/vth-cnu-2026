package ua.cdu.edu.fotius.fspm.valorant.roster.application;

import jakarta.inject.Singleton;

/**
 * Stateless JWT-style token service (educational mock — no cryptography).
 *
 * Token format: "jwt-token-for-<playerTag>"
 *
 * Validation is a simple prefix check. No signing keys, no encryption,
 * nothing secret is committed to the repository. In a production system
 * this would be replaced with a real JWT library (e.g. Nimbus, JJWT).
 */
@Singleton
public class JwtTokenService {

    private static final String TOKEN_PREFIX = "jwt-token-for-";

    /**
     * Creates a mock JWT token for the given player tag.
     * Stateless — nothing is stored in the database.
     */
    public String createToken(String playerTag) {
        return TOKEN_PREFIX + playerTag;
    }

    /**
     * Validates that the token has the expected mock format.
     */
    public boolean isValidToken(String token) {
        return token != null && token.startsWith(TOKEN_PREFIX);
    }

    /**
     * Extracts the player tag from a valid mock token.
     * Caller should check {@link #isValidToken(String)} first.
     */
    public String extractPlayerTag(String token) {
        return token.substring(TOKEN_PREFIX.length());
    }
}
