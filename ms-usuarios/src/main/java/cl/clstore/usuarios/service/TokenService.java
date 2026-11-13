package cl.clstore.usuarios.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import cl.clstore.usuarios.model.Usuario;
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
 * Emite el JWT (HS256) que validan todos los microservicios con el mismo app.jwt.secret.
 * Claims: sub = id del usuario, rol = tipo, nombre, correo, iat y exp.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long horas;

    public TokenService(@Value("${app.jwt.secret}") String secreto, @Value("${app.auth.horas}") long horas) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres (HS256).");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(bytes, "HmacSHA256")));
        this.horas = horas;
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(usuario.getId())
            .issuedAt(ahora)
            .expiresAt(ahora.plus(horas, ChronoUnit.HOURS))
            .claim("rol", usuario.getTipo())
            .claim("nombre", usuario.getNombre())
            .claim("correo", usuario.getCorreo())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
