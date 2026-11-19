package cl.clstore.usuarios.testsupport;

import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.dto.UsuarioResponse;
import cl.clstore.usuarios.model.Usuario;
import java.time.Instant;
import java.time.LocalDate;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    public static final String USUARIO_ID = "usuario-1";
    /** RUN con dígito verificador válido (módulo 11). */
    public static final String RUN_VALIDO = "190110222";

    private DatosPrueba() {
    }

    public static Usuario usuario(String id, String tipo, String correo, String run, String passwordHash) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRun(run);
        u.setNombre("Ana");
        u.setApellidos("Pérez");
        u.setCorreo(correo);
        u.setPasswordHash(passwordHash);
        u.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        u.setTipo(tipo);
        u.setRegion("Región Metropolitana de Santiago");
        u.setComuna("Santiago");
        u.setDireccion("Calle Falsa 123");
        u.setCreatedAt(Instant.parse("2026-01-15T12:00:00Z"));
        return u;
    }

    public static Usuario usuario() {
        return usuario(USUARIO_ID, "Cliente", "ana@gmail.com", RUN_VALIDO, "hash-de-prueba");
    }

    /** Request válido; los datos personales son fijos y solo cambian los que importan al negocio. */
    public static UsuarioRequest request(String run, String tipo, String correo, String password) {
        return new UsuarioRequest(run, tipo, "Ana", "Pérez", correo, LocalDate.of(1990, 5, 20), password,
            "Región Metropolitana de Santiago", "Santiago", "Calle Falsa 123");
    }

    public static UsuarioResponse usuarioResponse() {
        return UsuarioResponse.desde(usuario());
    }
}
