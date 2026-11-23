package cl.clstore.usuarios.service;

import cl.clstore.usuarios.exception.NoAutorizadoException;
import cl.clstore.usuarios.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;


@Component
public class GoogleTokenVerifier {

    private static final String CLAVES_PUBLICAS = "https://www.googleapis.com/oauth2/v3/certs";

    
    public record IdentidadGoogle(String correo, String nombre, String apellidos) {
    }

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(CLAVES_PUBLICAS).build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            emisorYAudiencia(this.clientId)));
        this.decoder = nimbus;
    }

    public IdentidadGoogle verificar(String credential) {
        if (clientId.isBlank()) {
            throw new ReglaNegocioException("El inicio de sesión con Google no está configurado en el servidor.");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (JwtException e) {
            throw new NoAutorizadoException("No se pudo validar el inicio de sesión con Google.");
        }
        String correo = jwt.getClaimAsString("email");
        boolean verificado = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        if (correo == null || correo.isBlank() || !verificado) {
            throw new NoAutorizadoException("Tu cuenta de Google no tiene el correo verificado.");
        }
        String nombre = jwt.getClaimAsString("given_name");
        if (nombre == null || nombre.isBlank()) {
            nombre = jwt.getClaimAsString("name");
        }
        return new IdentidadGoogle(correo, nombre, jwt.getClaimAsString("family_name"));
    }

    private static OAuth2TokenValidator<Jwt> emisorYAudiencia(String clientId) {
        return jwt -> {
            String emisor = String.valueOf(jwt.getClaims().get("iss"));
            boolean emisorOk = "https://accounts.google.com".equals(emisor) || "accounts.google.com".equals(emisor);
            List<String> audiencia = jwt.getAudience();
            boolean audienciaOk = !clientId.isBlank() && audiencia != null && audiencia.contains(clientId);
            if (emisorOk && audienciaOk) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token de Google no válido para esta aplicación.", null));
        };
    }
}
