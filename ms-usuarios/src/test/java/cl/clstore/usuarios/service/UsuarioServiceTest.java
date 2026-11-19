package cl.clstore.usuarios.service;

import static cl.clstore.usuarios.testsupport.DatosPrueba.RUN_VALIDO;
import static cl.clstore.usuarios.testsupport.DatosPrueba.request;
import static cl.clstore.usuarios.testsupport.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.LoginRequest;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.exception.ConflictoException;
import cl.clstore.usuarios.exception.NoAutorizadoException;
import cl.clstore.usuarios.exception.ProhibidoException;
import cl.clstore.usuarios.exception.RecursoNoEncontradoException;
import cl.clstore.usuarios.exception.ReglaNegocioException;
import cl.clstore.usuarios.model.Usuario;
import cl.clstore.usuarios.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Usa un BCrypt real (costo mínimo) para comprobar de verdad el cifrado y la verificación de contraseñas. */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String ADMIN = "Administrador";
    private static final String VENDEDOR = "Vendedor";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository repositorio;

    @Mock
    private TokenService tokens;

    private UsuarioService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new UsuarioService(repositorio, encoder, tokens);
    }

    private Usuario conClave(String id, String tipo, String correo, String clave) {
        return usuario(id, tipo, correo, RUN_VALIDO, encoder.encode(clave));
    }

    /** Prepara el repositorio para que acepte un usuario nuevo y devuelva lo que se guarda. */
    private void repositorioAceptaNuevos() {
        when(repositorio.existsByRun(any())).thenReturn(false);
        when(repositorio.existsByCorreoIgnoreCase(any())).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuarioGuardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------------- login

    @Test
    void login_credencialesCorrectas_devuelveTokenYPerfil() {
        Usuario u = conClave("id-1", "Cliente", "ana@gmail.com", "clave123");
        when(repositorio.findByCorreoIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(u));
        when(tokens.emitir(u)).thenReturn("jwt-falso");

        AuthResponse respuesta = servicio.login(new LoginRequest("ana@gmail.com", "clave123"));

        assertEquals("jwt-falso", respuesta.token());
        assertEquals("id-1", respuesta.usuario().id());
        assertEquals("ana@gmail.com", respuesta.usuario().correo());
        assertEquals("Cliente", respuesta.usuario().tipo());
    }

    @Test
    void login_correoConEspacios_seBuscaSinEllos() {
        Usuario u = conClave("id-1", "Cliente", "ana@gmail.com", "clave123");
        when(repositorio.findByCorreoIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(u));
        when(tokens.emitir(u)).thenReturn("jwt-falso");

        assertEquals("jwt-falso", servicio.login(new LoginRequest("  ana@gmail.com ", "clave123")).token());
    }

    @Test
    void login_contrasenaIncorrecta_lanzaNoAutorizadoYNoEmiteToken() {
        Usuario u = conClave("id-1", "Cliente", "ana@gmail.com", "clave123");
        when(repositorio.findByCorreoIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(u));

        NoAutorizadoException ex = assertThrows(NoAutorizadoException.class,
            () -> servicio.login(new LoginRequest("ana@gmail.com", "otra-clave")));

        assertEquals("Correo o contraseña incorrectos.", ex.getMessage());
        verifyNoInteractions(tokens);
    }

    @Test
    void login_correoInexistente_lanzaNoAutorizadoYNoEmiteToken() {
        when(repositorio.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

        NoAutorizadoException ex = assertThrows(NoAutorizadoException.class,
            () -> servicio.login(new LoginRequest("nadie@gmail.com", "clave123")));

        assertEquals("Correo o contraseña incorrectos.", ex.getMessage());
        verifyNoInteractions(tokens);
    }

    @Test
    void login_elMensajeDeErrorEsIgualConCorreoInexistenteQueConClaveMala() {
        // Evita que alguien descubra qué correos están registrados.
        when(repositorio.findByCorreoIgnoreCase("ana@gmail.com"))
            .thenReturn(Optional.of(conClave("id-1", "Cliente", "ana@gmail.com", "clave123")));
        when(repositorio.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

        String conClaveMala = assertThrows(NoAutorizadoException.class,
            () -> servicio.login(new LoginRequest("ana@gmail.com", "mala"))).getMessage();
        String sinUsuario = assertThrows(NoAutorizadoException.class,
            () -> servicio.login(new LoginRequest("nadie@gmail.com", "mala"))).getMessage();

        assertEquals(conClaveMala, sinUsuario);
    }

    // ---------------------------------------------------------------- registrar

    @Test
    void registrar_datosValidos_creaUnClienteConSesionIniciada() {
        repositorioAceptaNuevos();
        when(tokens.emitir(any(Usuario.class))).thenReturn("jwt-falso");

        AuthResponse respuesta = servicio.registrar(request("19.011.022-2", null, "  Ana@Gmail.COM ", "clave123"));

        Usuario u = usuarioGuardado();
        assertEquals("jwt-falso", respuesta.token());
        assertEquals("Cliente", u.getTipo());
        assertEquals("190110222", u.getRun());                              // RUN normalizado
        assertEquals("ana@gmail.com", u.getCorreo());                       // correo recortado y en minúscula
        assertEquals("Ana", u.getNombre());
        assertEquals("Santiago", u.getComuna());
        assertEquals(LocalDate.of(1990, 5, 20), u.getFechaNacimiento());
        UUID.fromString(u.getId());                                         // id UUID generado por el servicio
    }

    @Test
    void registrar_laContrasenaSeGuardaCifrada() {
        repositorioAceptaNuevos();
        when(tokens.emitir(any(Usuario.class))).thenReturn("jwt-falso");

        servicio.registrar(request(RUN_VALIDO, null, "ana@gmail.com", "clave123"));

        String hash = usuarioGuardado().getPasswordHash();
        assertNotEquals("clave123", hash);
        assertTrue(encoder.matches("clave123", hash));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Administrador", "Vendedor"})
    void registrar_ignoraElTipoEnviado_siempreCreaCliente(String tipoSolicitado) {
        repositorioAceptaNuevos();
        when(tokens.emitir(any(Usuario.class))).thenReturn("jwt-falso");

        servicio.registrar(request(RUN_VALIDO, tipoSolicitado, "ana@gmail.com", "clave123"));

        assertEquals("Cliente", usuarioGuardado().getTipo());               // evita escalada de privilegios
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@duoc.cl", "ana@profesor.duoc.cl", "ana@gmail.com", "ANA@GMAIL.COM"})
    void registrar_dominiosPermitidos_seAceptan(String correo) {
        repositorioAceptaNuevos();
        when(tokens.emitir(any(Usuario.class))).thenReturn("jwt-falso");

        assertEquals("jwt-falso", servicio.registrar(request(RUN_VALIDO, null, correo, "clave123")).token());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@hotmail.com", "ana@yahoo.cl", "ana@fakegmail.com", "ana@gmail.com.evil.com", "ana@mi.duoc.cl"})
    void registrar_dominioNoPermitido_lanzaReglaNegocioSinTocarLaBase(String correo) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.registrar(request(RUN_VALIDO, null, correo, "clave123")));

        assertEquals("El correo solo puede ser @duoc.cl, @profesor.duoc.cl o @gmail.com.", ex.getMessage());
        verifyNoInteractions(repositorio, tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {"190110223", "123", "ABCDEFGHI"})
    void registrar_runInvalido_lanzaReglaNegocio(String run) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.registrar(request(run, null, "ana@gmail.com", "clave123")));

        assertTrue(ex.getMessage().startsWith("El RUN ingresado no es válido"));
        verifyNoInteractions(repositorio, tokens);
    }

    @Test
    void registrar_runNull_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
            () -> servicio.registrar(request(null, null, "ana@gmail.com", "clave123")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void registrar_contrasenaEnBlanco_lanzaReglaNegocio(String clave) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.registrar(request(RUN_VALIDO, null, "ana@gmail.com", clave)));

        assertEquals("La contraseña es obligatoria (mínimo 6 caracteres).", ex.getMessage());
        verifyNoInteractions(repositorio);
    }

    @Test
    void registrar_contrasenaNull_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
            () -> servicio.registrar(request(RUN_VALIDO, null, "ana@gmail.com", null)));
    }

    @Test
    void registrar_runYaRegistrado_lanzaConflictoYNoGuarda() {
        when(repositorio.existsByRun(RUN_VALIDO)).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.registrar(request(RUN_VALIDO, null, "ana@gmail.com", "clave123")));

        assertEquals("Ya existe un usuario con ese RUN.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void registrar_correoYaRegistrado_lanzaConflictoYNoGuarda() {
        when(repositorio.existsByRun(RUN_VALIDO)).thenReturn(false);
        when(repositorio.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.registrar(request(RUN_VALIDO, null, "ana@gmail.com", "clave123")));

        assertEquals("Ya existe un usuario con ese correo.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    // ------------------------------------------------------------ perfil / lecturas

    @Test
    void perfil_usuarioExistente_loDevuelve() {
        Usuario u = usuario();
        when(repositorio.findById("usuario-1")).thenReturn(Optional.of(u));

        assertSame(u, servicio.perfil("usuario-1"));
    }

    @Test
    void perfil_usuarioEliminado_lanzaNoAutorizado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        NoAutorizadoException ex = assertThrows(NoAutorizadoException.class, () -> servicio.perfil("fantasma"));

        assertEquals("La sesión ya no es válida. Vuelve a iniciar sesión.", ex.getMessage());
    }

    @Test
    void listar_pideLosUsuariosOrdenadosPorNombreAscendente() {
        List<Usuario> esperados = List.of(usuario());
        when(repositorio.findAll(any(Sort.class))).thenReturn(esperados);

        assertSame(esperados, servicio.listar());

        ArgumentCaptor<Sort> orden = ArgumentCaptor.forClass(Sort.class);
        verify(repositorio).findAll(orden.capture());
        assertEquals(Sort.by(Sort.Direction.ASC, "nombre"), orden.getValue());
    }

    @Test
    void obtener_usuarioInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.obtener("fantasma"));

        assertEquals("No existe el usuario 'fantasma'.", ex.getMessage());
    }

    // -------------------------------------------------------------------- crear

    @Test
    void crear_administradorCreaOtroAdministrador_ok() {
        repositorioAceptaNuevos();

        Usuario creado = servicio.crear(request(RUN_VALIDO, "Administrador", "nuevo@gmail.com", "clave123"), ADMIN);

        assertEquals("Administrador", creado.getTipo());
    }

    @Test
    void crear_vendedorIntentaCrearAdministrador_lanzaProhibidoYNoGuarda() {
        ProhibidoException ex = assertThrows(ProhibidoException.class,
            () -> servicio.crear(request(RUN_VALIDO, "Administrador", "nuevo@gmail.com", "clave123"), VENDEDOR));

        assertEquals("Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador.", ex.getMessage());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vendedor", "Cliente"})
    void crear_vendedorPuedeCrearVendedoresYClientes(String tipo) {
        repositorioAceptaNuevos();

        assertEquals(tipo, servicio.crear(request(RUN_VALIDO, tipo, "nuevo@gmail.com", "clave123"), VENDEDOR).getTipo());
    }

    @Test
    void crear_sinTipo_creaUnCliente() {
        repositorioAceptaNuevos();

        assertEquals("Cliente", servicio.crear(request(RUN_VALIDO, null, "nuevo@gmail.com", "clave123"), VENDEDOR).getTipo());
    }

    @Test
    void crear_porAdministracion_noExigeDominioDeCorreoPermitido() {
        repositorioAceptaNuevos();

        Usuario creado = servicio.crear(request(RUN_VALIDO, "Cliente", "ana@empresa.cl", "clave123"), ADMIN);

        assertEquals("ana@empresa.cl", creado.getCorreo());     // la restricción de dominio es solo del registro público
    }

    // ---------------------------------------------------------------- actualizar

    @Test
    void actualizar_usuarioInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.actualizar("fantasma", request(null, null, "ana@gmail.com", null), ADMIN));

        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_cambiaLosDatosPeroNoElRunNiElId() {
        Usuario u = usuario("id-1", "Cliente", "viejo@gmail.com", RUN_VALIDO, "hash-original");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("nuevo@gmail.com", "id-1")).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioRequest r = new UsuarioRequest("999999999", null, "  Luisa ", " Gómez ", " Nuevo@Gmail.com ",
            LocalDate.of(2000, 1, 2), null, " Valparaíso ", " Viña del Mar ", " Av. Libertad 1 ");

        Usuario resultado = servicio.actualizar("id-1", r, ADMIN);

        assertEquals("id-1", resultado.getId());
        assertEquals(RUN_VALIDO, resultado.getRun());                       // el RUN del request se ignora
        assertEquals("Luisa", resultado.getNombre());
        assertEquals("Gómez", resultado.getApellidos());
        assertEquals("nuevo@gmail.com", resultado.getCorreo());
        assertEquals(LocalDate.of(2000, 1, 2), resultado.getFechaNacimiento());
        assertEquals("Valparaíso", resultado.getRegion());
        assertEquals("Viña del Mar", resultado.getComuna());
        assertEquals("Av. Libertad 1", resultado.getDireccion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void actualizar_contrasenaVaciaONula_conservaElHashActual(String clave) {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash-original");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-1")).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("hash-original", servicio.actualizar("id-1", request(null, null, "ana@gmail.com", clave), ADMIN)
            .getPasswordHash());
        assertEquals("hash-original", servicio.actualizar("id-1", request(null, null, "ana@gmail.com", null), ADMIN)
            .getPasswordHash());
    }

    @Test
    void actualizar_contrasenaNueva_seCifraYReemplazaElHash() {
        Usuario u = conClave("id-1", "Cliente", "ana@gmail.com", "clave-vieja");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-1")).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = servicio.actualizar("id-1", request(null, null, "ana@gmail.com", "clave-nueva"), ADMIN);

        assertTrue(encoder.matches("clave-nueva", resultado.getPasswordHash()));
        assertTrue(!encoder.matches("clave-vieja", resultado.getPasswordHash()));
    }

    @Test
    void actualizar_sinTipo_conservaElTipoActual() {
        Usuario u = usuario("id-1", "Vendedor", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-1")).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Vendedor", servicio.actualizar("id-1", request(null, null, "ana@gmail.com", null), ADMIN).getTipo());
    }

    @Test
    void actualizar_administradorCambiaElTipo_seAplica() {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-1")).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Vendedor", servicio.actualizar("id-1", request(null, "Vendedor", "ana@gmail.com", null), ADMIN).getTipo());
    }

    @Test
    void actualizar_correoDeOtroUsuario_lanzaConflictoYNoGuarda() {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("ocupado@gmail.com", "id-1")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.actualizar("id-1", request(null, null, "ocupado@gmail.com", null), ADMIN));

        assertEquals("Ya existe un usuario con ese correo.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_vendedorEditandoUnAdministrador_lanzaProhibido() {
        Usuario admin = usuario("id-admin", "Administrador", "admin@duoc.cl", RUN_VALIDO, "hash");
        when(repositorio.findById("id-admin")).thenReturn(Optional.of(admin));

        assertThrows(ProhibidoException.class,
            () -> servicio.actualizar("id-admin", request(null, null, "admin@duoc.cl", "otra-clave"), VENDEDOR));

        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_vendedorAscendiendoAAdministrador_lanzaProhibido() {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));

        assertThrows(ProhibidoException.class,
            () -> servicio.actualizar("id-1", request(null, "Administrador", "ana@gmail.com", null), VENDEDOR));

        verify(repositorio, never()).save(any());
        assertEquals("Cliente", u.getTipo());                               // el tipo no se tocó
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminar_elPropioUsuario_lanzaConflictoSinTocarLaBase() {
        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.eliminar("id-1", "id-1", ADMIN));

        assertEquals("No puedes eliminar el usuario con el que iniciaste sesión.", ex.getMessage());
        verifyNoInteractions(repositorio);
    }

    @Test
    void eliminar_usuarioInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar("fantasma", "id-actor", ADMIN));
    }

    @Test
    void eliminar_administradorBorraUnCliente_ok() {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));

        servicio.eliminar("id-1", "id-actor", ADMIN);

        verify(repositorio).delete(u);
    }

    @Test
    void eliminar_vendedorBorraUnCliente_ok() {
        Usuario u = usuario("id-1", "Cliente", "ana@gmail.com", RUN_VALIDO, "hash");
        when(repositorio.findById("id-1")).thenReturn(Optional.of(u));

        servicio.eliminar("id-1", "id-actor", VENDEDOR);

        verify(repositorio).delete(u);
    }

    @Test
    void eliminar_vendedorIntentaBorrarUnAdministrador_lanzaProhibidoYNoBorra() {
        Usuario admin = usuario("id-admin", "Administrador", "admin@duoc.cl", RUN_VALIDO, "hash");
        when(repositorio.findById("id-admin")).thenReturn(Optional.of(admin));

        assertThrows(ProhibidoException.class, () -> servicio.eliminar("id-admin", "id-actor", VENDEDOR));

        verify(repositorio, never()).delete(any(Usuario.class));
    }
}
