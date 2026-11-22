package cl.clstore.usuarios.service;

import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.UsuarioResponse;
import cl.clstore.usuarios.exception.ReglaNegocioException;
import cl.clstore.usuarios.model.Usuario;
import cl.clstore.usuarios.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Inicio de sesión con Google: verifica el ID token, busca al usuario por correo (o lo crea como Cliente)
 * y devuelve el mismo JWT propio que emite el login normal.
 */
@Service
public class GoogleAuthService {

    private static final List<String> DOMINIOS_PERMITIDOS = List.of("@duoc.cl", "@profesor.duoc.cl", "@gmail.com");
    private static final String SIN_INFORMAR = "Sin informar";

    private final GoogleTokenVerifier verificador;
    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public GoogleAuthService(GoogleTokenVerifier verificador, UsuarioRepository repositorio,
                             PasswordEncoder encoder, TokenService tokens) {
        this.verificador = verificador;
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public AuthResponse login(String credential) {
        GoogleTokenVerifier.IdentidadGoogle g = verificador.verificar(credential);
        String correo = g.correo().trim().toLowerCase(Locale.ROOT);
        Usuario u = repositorio.findByCorreoIgnoreCase(correo).orElseGet(() -> crear(g, correo));
        return new AuthResponse(tokens.emitir(u), UsuarioResponse.desde(u));
    }

    /** Primera vez con Google: se crea un Cliente. RUN y dirección quedan provisorios (el usuario los completa después). */
    private Usuario crear(GoogleTokenVerifier.IdentidadGoogle g, String correo) {
        if (DOMINIOS_PERMITIDOS.stream().noneMatch(correo::endsWith)) {
            throw new ReglaNegocioException("El correo solo puede ser @duoc.cl, @profesor.duoc.cl o @gmail.com.");
        }
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID().toString());
        u.setRun(runProvisorio());
        u.setNombre(recortar(g.nombre(), 100, "Usuario"));
        u.setApellidos(recortar(g.apellidos(), 100, "-"));
        u.setCorreo(correo);
        // Contraseña aleatoria que nadie conoce: esta cuenta solo entra con Google.
        u.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        u.setTipo("Cliente");
        u.setRegion(SIN_INFORMAR);
        u.setComuna(SIN_INFORMAR);
        u.setDireccion(SIN_INFORMAR);
        return repositorio.save(u);
    }

    /** "G" + 11 caracteres al azar (12 en total, el largo máximo de la columna run). */
    private String runProvisorio() {
        String run;
        do {
            run = "G" + UUID.randomUUID().toString().replace("-", "").substring(0, 11).toUpperCase(Locale.ROOT);
        } while (repositorio.existsByRun(run));
        return run;
    }

    private static String recortar(String valor, int max, String porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        String t = valor.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
