package com.causal.identity.auth;

import com.causal.identity.user.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String KEY_ID = "identity-key-1";

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    void loadKeys() {
        this.publicKey = parsePublicKey();
        this.privateKey = parsePrivateKey();
    }

    public String extractUsername(String token) {
        return (String) parseClaims(token).getClaim("email");
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpirationTime();
    }

    private JWTClaimsSet parseClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT signature");
            }
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException("Failed to parse JWT", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    private RSAPublicKey parsePublicKey() {
        try {
            String pem = Files.readString(Paths.get(publicKeyPath));
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read public key from " + publicKeyPath, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }

    private RSAPrivateKey parsePrivateKey() {
        try {
            String pem = Files.readString(Paths.get(privateKeyPath));
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read private key from " + privateKeyPath, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    public RSAKey getJWK() {
        return new RSAKey.Builder(publicKey)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    public String generateToken(User user) {
        try {
            JWSSigner signer = new RSASSASigner(privateKey);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + jwtExpiration))
                    .claim("email", user.getEmail())
                    .claim("roles", user.getRoles().stream().map(role -> role.getName()).toList())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
                    claimsSet
            );
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !extractExpiration(token).before(new Date()));
    }
}
