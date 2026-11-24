package cl.clstore.blogs.testsupport;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Genera JWT HS256 reales (igual que ms-usuarios) para probar la seguridad de punta a punta. */
public final class TokenDePrueba {

    private TokenDePrueba() {
    }

    /** @param validez negativa = token ya expirado; rol null = token sin claim "rol". */
    public static String generar(String secreto, String rol, Duration validez) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("usuario-prueba")
                .claim("rol", rol)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(validez)))
                .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de prueba", e);
        }
    }
}
