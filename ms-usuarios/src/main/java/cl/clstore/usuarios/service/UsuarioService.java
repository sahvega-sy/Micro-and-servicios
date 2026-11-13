package cl.clstore.usuarios.service;

import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.LoginRequest;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.dto.UsuarioResponse;
import cl.clstore.usuarios.exception.ConflictoException;
import cl.clstore.usuarios.exception.NoAutorizadoException;
import cl.clstore.usuarios.exception.ProhibidoException;
import cl.clstore.usuarios.exception.RecursoNoEncontradoException;
import cl.clstore.usuarios.exception.ReglaNegocioException;
import cl.clstore.usuarios.model.Usuario;
import cl.clstore.usuarios.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private static final String ADMINISTRADOR = "Administrador";
    private static final String CLIENTE = "Cliente";
    private static final List<String> DOMINIOS_PERMITIDOS = List.of("@duoc.cl", "@profesor.duoc.cl", "@gmail.com");

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    /** Hash de relleno: se compara contra él cuando el correo no existe, para que la respuesta tarde igual. */
    private final String hashDeRelleno;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, TokenService tokens) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.tokens = tokens;
        this.hashDeRelleno = encoder.encode(UUID.randomUUID().toString());
    }

    // ------------------------------------------------------------------ autenticación

    public AuthResponse login(LoginRequest r) {
        String mensaje = "Correo o contraseña incorrectos.";
        Usuario u = repositorio.findByCorreoIgnoreCase(r.correo().trim()).orElse(null);
        if (u == null) {
            encoder.matches(r.password(), hashDeRelleno);
            throw new NoAutorizadoException(mensaje);
        }
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            throw new NoAutorizadoException(mensaje);
        }
        return new AuthResponse(tokens.emitir(u), UsuarioResponse.desde(u));
    }

    /** Registro público: siempre crea un Cliente y deja la sesión iniciada. */
    public AuthResponse registrar(UsuarioRequest r) {
        String correo = r.correo().trim().toLowerCase(Locale.ROOT);
        if (DOMINIOS_PERMITIDOS.stream().noneMatch(correo::endsWith)) {
            throw new ReglaNegocioException("El correo solo puede ser @duoc.cl, @profesor.duoc.cl o @gmail.com.");
        }
        Usuario u = nuevo(r, CLIENTE);
        return new AuthResponse(tokens.emitir(u), UsuarioResponse.desde(u));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Usuario perfil(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new NoAutorizadoException("La sesión ya no es válida. Vuelve a iniciar sesión."));
    }

    // ------------------------------------------------------------------ administración

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repositorio.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Usuario obtener(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario '" + id + "'."));
    }

    public Usuario crear(UsuarioRequest r, String rolActor) {
        String tipo = r.tipo() == null ? CLIENTE : r.tipo();
        exigirPermisoSobre(tipo, rolActor);
        return nuevo(r, tipo);
    }

    public Usuario actualizar(String id, UsuarioRequest r, String rolActor) {
        Usuario u = obtener(id);
        String nuevoTipo = r.tipo() == null ? u.getTipo() : r.tipo();
        exigirPermisoSobre(u.getTipo(), rolActor);
        exigirPermisoSobre(nuevoTipo, rolActor);

        String correo = r.correo().trim().toLowerCase(Locale.ROOT);
        if (repositorio.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new ConflictoException("Ya existe un usuario con ese correo.");
        }
        aplicarDatos(u, r);
        u.setTipo(nuevoTipo);
        if (r.password() != null && !r.password().isBlank()) {
            u.setPasswordHash(encoder.encode(r.password()));
        }
        return repositorio.save(u);
    }

    public void eliminar(String id, String idActor, String rolActor) {
        if (id.equals(idActor)) {
            throw new ConflictoException("No puedes eliminar el usuario con el que iniciaste sesión.");
        }
        Usuario u = obtener(id);
        exigirPermisoSobre(u.getTipo(), rolActor);
        repositorio.delete(u);
    }

    // ------------------------------------------------------------------ internos

    /** Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador (evita escalar privilegios). */
    private void exigirPermisoSobre(String tipoObjetivo, String rolActor) {
        if (ADMINISTRADOR.equals(tipoObjetivo) && !ADMINISTRADOR.equals(rolActor)) {
            throw new ProhibidoException("Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador.");
        }
    }

    private Usuario nuevo(UsuarioRequest r, String tipo) {
        if (r.run() == null || !RunValidator.esValido(r.run())) {
            throw new ReglaNegocioException("El RUN ingresado no es válido. Ingrésalo sin puntos ni guion (Ej: 190110222 o 10000013K).");
        }
        if (r.password() == null || r.password().isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria (mínimo 6 caracteres).");
        }
        String run = RunValidator.normalizar(r.run());
        String correo = r.correo().trim().toLowerCase(Locale.ROOT);
        if (repositorio.existsByRun(run)) {
            throw new ConflictoException("Ya existe un usuario con ese RUN.");
        }
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe un usuario con ese correo.");
        }
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID().toString());
        u.setRun(run);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setTipo(tipo);
        aplicarDatos(u, r);
        return repositorio.save(u);
    }

    private void aplicarDatos(Usuario u, UsuarioRequest r) {
        u.setNombre(r.nombre().trim());
        u.setApellidos(r.apellidos().trim());
        u.setCorreo(r.correo().trim().toLowerCase(Locale.ROOT));
        u.setFechaNacimiento(r.fechaNacimiento());
        u.setRegion(r.region().trim());
        u.setComuna(r.comuna().trim());
        u.setDireccion(r.direccion().trim());
    }
}
