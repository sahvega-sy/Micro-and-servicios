package cl.clstore.ordenes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Sin Spring: el JWT de servicio debe poder validarlo ms-productos con el mismo secreto compartido. */
class TokenServicioTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";

    private static JwtDecoder decoder(String secreto) {
        return NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void emitir_generaUnJwtHs256ValidoConRolServicio() {
        Jwt jwt = decoder(SECRETO).decode(new TokenServicio(SECRETO).emitir());

        assertEquals("ms-ordenes", jwt.getSubject());
        assertEquals("Servicio", jwt.getClaimAsString("rol"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertNotNull(jwt.getIssuedAt());
    }

    @Test
    void emitir_elTokenCaducaALosDosMinutos() {
        Jwt jwt = decoder(SECRETO).decode(new TokenServicio(SECRETO).emitir());

        assertEquals(Duration.ofMinutes(2), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void emitir_noLoAceptaQuienTengaOtroSecreto() {
        String token = new TokenServicio(SECRETO).emitir();

        assertThrows(JwtException.class,
            () -> decoder("otro-secreto-distinto-de-mas-de-32-caracteres").decode(token));
    }

    @Test
    void constructor_secretoMenorA32Bytes_lanzaIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new TokenServicio("corto"));

        assertEquals("app.jwt.secret debe tener al menos 32 caracteres (HS256).", ex.getMessage());
    }
}
