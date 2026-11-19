package cl.clstore.contacto.service;

import static cl.clstore.contacto.testsupport.DatosPrueba.contacto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.exception.RecursoNoEncontradoException;
import cl.clstore.contacto.model.Contacto;
import cl.clstore.contacto.repository.ContactoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    @Mock
    private ContactoRepository repositorio;

    @InjectMocks
    private ContactoService servicio;

    // ------------------------------------------------------------------ crear

    @Test
    void crear_datosValidos_guardaElMensajeSinEspaciosSobrantes() {
        when(repositorio.save(any(Contacto.class))).thenAnswer(inv -> inv.getArgument(0));

        Contacto resultado = servicio.crear(new ContactoRequest("  Ana Pérez ", " ana@gmail.com  ", "  Hola  "));

        ArgumentCaptor<Contacto> guardado = ArgumentCaptor.forClass(Contacto.class);
        verify(repositorio).save(guardado.capture());
        assertSame(resultado, guardado.getValue());
        assertEquals("Ana Pérez", resultado.getNombre());
        assertEquals("ana@gmail.com", resultado.getEmail());
        assertEquals("Hola", resultado.getMensaje());
    }

    @Test
    void crear_devuelveLaEntidadQueEntregaElRepositorio() {
        Contacto persistido = contacto();
        when(repositorio.save(any(Contacto.class))).thenReturn(persistido);

        Contacto resultado = servicio.crear(new ContactoRequest("Ana", "ana@gmail.com", "Hola"));

        assertSame(persistido, resultado);
    }

    // ----------------------------------------------------------------- listar

    @Test
    void listar_devuelveLaListaDelRepositorioMasRecientesPrimero() {
        Contacto nuevo = contacto(2L, "Luis", "luis@gmail.com", "Segundo", Instant.parse("2026-02-01T00:00:00Z"));
        Contacto viejo = contacto(1L, "Ana", "ana@gmail.com", "Primero", Instant.parse("2026-01-01T00:00:00Z"));
        when(repositorio.findAllByOrderByCreadoEnDesc()).thenReturn(List.of(nuevo, viejo));

        List<Contacto> resultado = servicio.listar();

        assertEquals(List.of(nuevo, viejo), resultado);
    }

    @Test
    void listar_sinMensajes_devuelveListaVacia() {
        when(repositorio.findAllByOrderByCreadoEnDesc()).thenReturn(List.of());

        assertEquals(List.of(), servicio.listar());
    }

    // --------------------------------------------------------------- eliminar

    @Test
    void eliminar_mensajeExistente_loBorra() {
        when(repositorio.existsById(5L)).thenReturn(true);

        servicio.eliminar(5L);

        verify(repositorio).deleteById(5L);
    }

    @Test
    void eliminar_mensajeInexistente_lanzaRecursoNoEncontradoYNoBorraNada() {
        when(repositorio.existsById(9L)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.eliminar(9L));

        assertEquals("No existe el mensaje #9.", ex.getMessage());
        verify(repositorio, never()).deleteById(any());
    }
}
