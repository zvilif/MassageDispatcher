package com.zvil.md;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * JWT utility
 * @author Zvi Lifshitz
 */
public class JwtService {
    private final String secretKey;

    /**
     * Instantiate the object with a predefined secret key that will be used to calculate the JWT
     * @param secretKey
     */
    public JwtService(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Create a JWT for a given user ID. Used by a user manager application that is out of the scope of this exercise but we can use
     * it for testing.
     * @param userID    The user ID
     * @return          The calculated JWT
     */
    public String createJWT(int userID) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        Date now = new Date();

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(String.valueOf(userID)).setIssuedAt(now);
        builder.signWith(signatureAlgorithm, signingKey);

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }

    /**
     * Decode a JWT to a user ID
     * @param jwt   the authentication token used by the user
     * @return  the decoded user ID. The method will throw an exception if the parameter is not a signed JWT.
     * @throws SignatureException if the jwt is not encoded properly.
     */
    int decodeJWT(String jwt) throws SignatureException {
        Claims claims = Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                .parseClaimsJws(jwt).getBody();
        return Integer.parseInt(claims.getId());
    }
    
    /**
     * Generate a random key. We use it internally to generate a secret key for testing.
     * @param length length of key in bytes
     * @return the key after encoding to Base64
     */
    public static String generateKey(int length)  {
        SecureRandom secureRandom = new SecureRandom();
        byte[] values = new byte[length];
        secureRandom.nextBytes(values);
        return Base64.getEncoder().encodeToString(values);
    }
}
