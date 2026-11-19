package cl.clstore.resenas.service;

import static cl.clstore.resenas.testsupport.DatosPrueba.request;
import static cl.clstore.resenas.testsupport.DatosPrueba.resena;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.clstore.resenas.client.ProductoClient;
import cl.clstore.resenas.exception.RecursoNoEncontradoException;
import cl.clstore.resenas.exception.ReglaNegocioException;
import cl.clstore.resenas.exception.ServicioNoDisponibleException;
import cl.clstore.resenas.model.Resena;
import cl.clstore.resenas.repository.ResenaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository repositorio;

    @Mock
    private ProductoClient productos;

    @InjectMocks
    private ResenaService servicio;

    // --------------------------------------------------------- listarDeProducto

    @Test
    void listarDeProducto_devuelveLasReseniasDelProductoMasRecientesPrimero() {
        List<Resena> esperadas = List.of(resena(2L, "CPU-001", "Luis", 4, "Bueno", null), resena());
        when(repositorio.findByProductoIdOrderByFechaDescIdDesc("CPU-001")).thenReturn(esperadas);

        assertSame(esperadas, servicio.listarDeProducto("CPU-001"));
    }

    @Test
    void listarDeProducto_sinResenias_devuelveListaVacia() {
        when(repositorio.findByProductoIdOrderByFechaDescIdDesc("NUEVO")).thenReturn(List.of());

        assertEquals(List.of(), servicio.listarDeProducto("NUEVO"));
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_datosValidos_guardaLaResenaConElNombreDelToken() {
        when(repositorio.save(any(Resena.class))).thenAnswer(inv -> inv.getArgument(0));

        Resena resultado = servicio.crear(request(" CPU-001 ", 5, "  Excelente procesador.  "), "Ana Pérez");

        ArgumentCaptor<Resena> guardada = ArgumentCaptor.forClass(Resena.class);
        verify(repositorio).save(guardada.capture());
        assertSame(resultado, guardada.getValue());
        assertEquals("CPU-001", resultado.getProductoId());                // id sin espacios
        assertEquals("Ana Pérez", resultado.getUsuario());                 // el autor es el del token, no el del cuerpo
        assertEquals(5, resultado.getCalificacion());
        assertEquals("Excelente procesador.", resultado.getComentario());  // comentario sin espacios sobrantes
    }

    @Test
    void crear_validaQueElProductoExistaConElIdYaRecortado() {
        when(repositorio.save(any(Resena.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.crear(request("  RAM-002  ", 3, "Normal"), "Ana");

        verify(productos).validarExiste("RAM-002");
    }

    @Test
    void crear_validaElProductoAntesDeGuardar() {
        when(repositorio.save(any(Resena.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.crear(request("CPU-001", 5, "Ok"), "Ana");

        InOrder orden = inOrder(productos, repositorio);
        orden.verify(productos).validarExiste("CPU-001");
        orden.verify(repositorio).save(any(Resena.class));
    }

    @Test
    void crear_productoInexistente_lanzaReglaNegocioYNoGuarda() {
        doThrow(new ReglaNegocioException("El producto 'FANTASMA' no existe."))
            .when(productos).validarExiste("FANTASMA");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.crear(request("FANTASMA", 5, "Ok"), "Ana"));

        assertEquals("El producto 'FANTASMA' no existe.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_msProductosCaido_propagaServicioNoDisponibleYNoGuarda() {
        doThrow(new ServicioNoDisponibleException("No se pudo validar el producto: ms-productos no está disponible."))
            .when(productos).validarExiste("CPU-001");

        assertThrows(ServicioNoDisponibleException.class,
            () -> servicio.crear(request("CPU-001", 5, "Ok"), "Ana"));

        verify(repositorio, never()).save(any());
    }

    // --------------------------------------------------------------- eliminar

    @Test
    void eliminar_resenaExistente_laBorra() {
        when(repositorio.existsById(7L)).thenReturn(true);

        servicio.eliminar(7L);

        verify(repositorio).deleteById(7L);
    }

    @Test
    void eliminar_resenaInexistente_lanzaRecursoNoEncontradoYNoBorra() {
        when(repositorio.existsById(9L)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.eliminar(9L));

        assertEquals("No existe la reseña #9.", ex.getMessage());
        verify(repositorio, never()).deleteById(any());
    }
}
