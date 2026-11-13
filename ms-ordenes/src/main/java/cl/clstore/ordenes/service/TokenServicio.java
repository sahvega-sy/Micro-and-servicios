package cl.clstore.ordenes.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * JWT de corta duración con rol "Servicio" para llamar a los endpoints internos de ms-productos
 * (descontar / reponer stock). Los usuarios nunca reciben un token con este rol.
 */
@Service
public class TokenServicio {

    private final JwtEncoder encoder;

    public TokenServicio(@Value("${app.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres (HS256).");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(bytes, "HmacSHA256")));
    }

    public String emitir() {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject("ms-ordenes")
            .issuedAt(ahora)
            .expiresAt(ahora.plus(2, ChronoUnit.MINUTES))
            .claim("rol", "Servicio")
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
