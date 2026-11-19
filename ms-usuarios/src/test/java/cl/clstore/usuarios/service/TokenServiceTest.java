package cl.clstore.usuarios.service;

import static cl.clstore.usuarios.testsupport.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.clstore.usuarios.model.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Sin Spring: el JWT que emite ms-usuarios debe poder validarlo cualquier microservicio con el mismo secreto. */
class TokenServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";

    private static JwtDecoder decoder(String secreto) {
        return NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void emitir_incluyeLosClaimsQueUsanLosDemasMicroservicios() {
        Usuario u = usuario("id-123", "Vendedor", "ana@gmail.com", "190110222", "hash");

        Jwt jwt = decoder(SECRETO).decode(new TokenService(SECRETO, 8).emitir(u));

        assertEquals("id-123", jwt.getSubject());                      // sub = id del usuario
        assertEquals("Vendedor", jwt.getClaimAsString("rol"));         // rol = tipo
        assertEquals("Ana", jwt.getClaimAsString("nombre"));
        assertEquals("ana@gmail.com", jwt.getClaimAsString("correo"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertNotNull(jwt.getIssuedAt());
    }

    @Test
    void emitir_nuncaIncluyeLaContrasenaNiSuHash() {
        Jwt jwt = decoder(SECRETO).decode(new TokenService(SECRETO, 8).emitir(usuario()));

        assertNull(jwt.getClaim("password"));
        assertNull(jwt.getClaim("password_hash"));
        assertNull(jwt.getClaim("passwordHash"));
    }

    @Test
    void emitir_duracionConfigurableEnHoras() {
        Jwt ocho = decoder(SECRETO).decode(new TokenService(SECRETO, 8).emitir(usuario()));
        Jwt una = decoder(SECRETO).decode(new TokenService(SECRETO, 1).emitir(usuario()));

        assertEquals(Duration.ofHours(8), Duration.between(ocho.getIssuedAt(), ocho.getExpiresAt()));
        assertEquals(Duration.ofHours(1), Duration.between(una.getIssuedAt(), una.getExpiresAt()));
    }

    @Test
    void emitir_noLoAceptaQuienTengaOtroSecreto() {
        String token = new TokenService(SECRETO, 8).emitir(usuario());

        assertThrows(JwtException.class,
            () -> decoder("otro-secreto-distinto-de-mas-de-32-caracteres").decode(token));
    }

    @Test
    void constructor_secretoMenorA32Bytes_lanzaIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new TokenService("corto", 8));

        assertEquals("app.jwt.secret debe tener al menos 32 caracteres (HS256).", ex.getMessage());
    }
}
