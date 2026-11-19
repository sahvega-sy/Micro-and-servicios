package cl.clstore.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.clstore.usuarios.model.Usuario;
import cl.clstore.usuarios.repository.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);   // costo mínimo: tests rápidos

    @Mock
    private UsuarioRepository repositorio;

    @Test
    void run_tablaVacia_creaElAdministradorInicial() {
        when(repositorio.count()).thenReturn(0L);
        DataInitializer inicializador =
            new DataInitializer(repositorio, encoder, "ADMIN@Duoc.CL", "Admin123", "11.111.111-1");

        inicializador.run(null);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(guardado.capture());
        Usuario admin = guardado.getValue();
        assertEquals("Administrador", admin.getTipo());
        assertEquals("admin@duoc.cl", admin.getCorreo());                  // correo en minúscula
        assertEquals("111111111", admin.getRun());                         // RUN normalizado
        assertEquals("Administrador", admin.getNombre());
        assertEquals("CLstore", admin.getApellidos());
        assertEquals("Santiago", admin.getComuna());
        assertTrue(RunValidator.esValido(admin.getRun()));
        UUID.fromString(admin.getId());                                    // el id es un UUID válido
    }

    @Test
    void run_laContrasenaSeGuardaCifradaConBcrypt() {
        when(repositorio.count()).thenReturn(0L);
        DataInitializer inicializador =
            new DataInitializer(repositorio, encoder, "admin@duoc.cl", "Admin123", "111111111");

        inicializador.run(null);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(guardado.capture());
        String hash = guardado.getValue().getPasswordHash();
        assertNotEquals("Admin123", hash);
        assertTrue(encoder.matches("Admin123", hash));
    }

    @Test
    void run_siYaHayUsuarios_noCreaNada() {
        when(repositorio.count()).thenReturn(3L);
        DataInitializer inicializador =
            new DataInitializer(repositorio, encoder, "admin@duoc.cl", "Admin123", "111111111");

        inicializador.run(null);

        verify(repositorio, never()).save(any());
    }
}
