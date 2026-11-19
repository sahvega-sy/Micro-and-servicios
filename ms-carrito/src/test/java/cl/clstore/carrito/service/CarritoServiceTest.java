package cl.clstore.carrito.service;

import static cl.clstore.carrito.testsupport.DatosPrueba.USUARIO;
import static cl.clstore.carrito.testsupport.DatosPrueba.item;
import static cl.clstore.carrito.testsupport.DatosPrueba.producto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.carrito.client.ProductoClient;
import cl.clstore.carrito.dto.ItemCarritoResponse;
import cl.clstore.carrito.dto.ProductoResumen;
import cl.clstore.carrito.exception.RecursoNoEncontradoException;
import cl.clstore.carrito.exception.ReglaNegocioException;
import cl.clstore.carrito.exception.ServicioNoDisponibleException;
import cl.clstore.carrito.model.CarritoItem;
import cl.clstore.carrito.repository.CarritoItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private CarritoItemRepository repositorio;

    @Mock
    private ProductoClient productos;

    @InjectMocks
    private CarritoService servicio;

    // ----------------------------------------------------------------- listar

    @Test
    void listar_carritoVacio_devuelveListaVaciaSinConsultarAMsProductos() {
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of());

        assertEquals(List.of(), servicio.listar(USUARIO));

        verifyNoInteractions(productos);
    }

    @Test
    void listar_conItems_losCombinaConLosDatosActualesDelProducto() {
        CarritoItem a = item(10L, USUARIO, "CPU-001", 2);
        CarritoItem b = item(11L, USUARIO, "RAM-002", 1);
        ProductoResumen cpu = producto("CPU-001", "Procesador Ryzen 5", "129990", 15, 10);
        ProductoResumen ram = producto("RAM-002", "Memoria 16GB", "59990", null, 4);
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(a, b));
        when(productos.obtenerPorIds(List.of("CPU-001", "RAM-002"))).thenReturn(List.of(ram, cpu));

        List<ItemCarritoResponse> resultado = servicio.listar(USUARIO);

        assertEquals(2, resultado.size());
        assertEquals(10L, resultado.get(0).id());                     // se mantiene el orden del carrito
        assertEquals("CPU-001", resultado.get(0).productoId());
        assertEquals(2, resultado.get(0).cantidad());
        assertSame(cpu, resultado.get(0).productos());
        assertEquals(11L, resultado.get(1).id());
        assertSame(ram, resultado.get(1).productos());
    }

    @Test
    void listar_productoRetiradoDelCatalogo_saleDelCarritoYSeBorra() {
        CarritoItem vigente = item(10L, USUARIO, "CPU-001", 2);
        CarritoItem huerfano = item(11L, USUARIO, "RETIRADO", 1);
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(vigente, huerfano));
        when(productos.obtenerPorIds(List.of("CPU-001", "RETIRADO"))).thenReturn(List.of(producto()));

        List<ItemCarritoResponse> resultado = servicio.listar(USUARIO);

        assertEquals(1, resultado.size());
        assertEquals("CPU-001", resultado.get(0).productoId());
        verify(repositorio).deleteAll(List.of(huerfano));
    }

    @Test
    void listar_sinHuerfanos_noBorraNada() {
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(item(10L, USUARIO, "CPU-001", 2)));
        when(productos.obtenerPorIds(List.of("CPU-001"))).thenReturn(List.of(producto()));

        servicio.listar(USUARIO);

        verify(repositorio, never()).deleteAll(anyList());
    }

    @Test
    void listar_catalogoConIdRepetido_usaElPrimero() {
        ProductoResumen primero = producto("CPU-001", "Primero", "100", null, 1);
        ProductoResumen segundo = producto("CPU-001", "Segundo", "200", null, 1);
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(item(10L, USUARIO, "CPU-001", 1)));
        when(productos.obtenerPorIds(List.of("CPU-001"))).thenReturn(List.of(primero, segundo));

        assertSame(primero, servicio.listar(USUARIO).get(0).productos());
    }

    @Test
    void listar_msProductosCaido_propagaServicioNoDisponibleYNoBorraNada() {
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(item(10L, USUARIO, "CPU-001", 2)));
        when(productos.obtenerPorIds(List.of("CPU-001")))
            .thenThrow(new ServicioNoDisponibleException("ms-productos no está disponible."));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.listar(USUARIO));

        verify(repositorio, never()).deleteAll(anyList());
    }

    // ---------------------------------------------------------------- agregar

    @Test
    void agregar_productoNuevoSinCantidad_agregaUnaUnidad() {
        when(productos.existe("CPU-001")).thenReturn(true);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.empty());

        servicio.agregar(USUARIO, "  CPU-001 ", null);

        ArgumentCaptor<CarritoItem> guardado = ArgumentCaptor.forClass(CarritoItem.class);
        verify(repositorio).save(guardado.capture());
        assertEquals(USUARIO, guardado.getValue().getUsuarioId());
        assertEquals("CPU-001", guardado.getValue().getProductoId());     // id sin espacios
        assertEquals(1, guardado.getValue().getCantidad());
    }

    @Test
    void agregar_productoNuevoConCantidad_laRespeta() {
        when(productos.existe("CPU-001")).thenReturn(true);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.empty());

        servicio.agregar(USUARIO, "CPU-001", 4);

        ArgumentCaptor<CarritoItem> guardado = ArgumentCaptor.forClass(CarritoItem.class);
        verify(repositorio).save(guardado.capture());
        assertEquals(4, guardado.getValue().getCantidad());
    }

    @Test
    void agregar_productoYaEnElCarrito_sumaLaCantidad() {
        CarritoItem existente = item(10L, USUARIO, "CPU-001", 2);
        when(productos.existe("CPU-001")).thenReturn(true);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.of(existente));

        servicio.agregar(USUARIO, "CPU-001", 3);

        verify(repositorio).save(existente);
        assertEquals(5, existente.getCantidad());
    }

    @Test
    void agregar_superaElMaximo_sePoneEnNoventaYNueve() {
        CarritoItem existente = item(10L, USUARIO, "CPU-001", 98);
        when(productos.existe("CPU-001")).thenReturn(true);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.of(existente));

        servicio.agregar(USUARIO, "CPU-001", 5);

        assertEquals(99, existente.getCantidad());
    }

    @Test
    void agregar_productoInexistente_lanzaReglaNegocioYNoGuarda() {
        when(productos.existe("FANTASMA")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.agregar(USUARIO, "FANTASMA", 1));

        assertEquals("El producto 'FANTASMA' no existe.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void agregar_msProductosCaido_propagaServicioNoDisponibleYNoGuarda() {
        when(productos.existe("CPU-001")).thenThrow(new ServicioNoDisponibleException("ms-productos no está disponible."));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.agregar(USUARIO, "CPU-001", 1));

        verify(repositorio, never()).save(any());
    }

    @Test
    void agregar_devuelveElCarritoActualizado() {
        CarritoItem existente = item(10L, USUARIO, "CPU-001", 2);
        ProductoResumen cpu = producto();
        when(productos.existe("CPU-001")).thenReturn(true);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.of(existente));
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(existente));
        when(productos.obtenerPorIds(List.of("CPU-001"))).thenReturn(List.of(cpu));

        List<ItemCarritoResponse> carrito = servicio.agregar(USUARIO, "CPU-001", 1);

        assertEquals(1, carrito.size());
        assertEquals(3, carrito.get(0).cantidad());
        assertSame(cpu, carrito.get(0).productos());
    }

    // --------------------------------------------------------- cambiarCantidad

    @Test
    void cambiarCantidad_productoEnElCarrito_reemplazaLaCantidad() {
        CarritoItem existente = item(10L, USUARIO, "CPU-001", 2);
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.of(existente));

        servicio.cambiarCantidad(USUARIO, "CPU-001", 7);

        assertEquals(7, existente.getCantidad());                         // reemplaza, no suma
        verify(repositorio).save(existente);
    }

    @Test
    void cambiarCantidad_productoFueraDelCarrito_lanzaRecursoNoEncontrado() {
        when(repositorio.findByUsuarioIdAndProductoId(USUARIO, "CPU-001")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.cambiarCantidad(USUARIO, "CPU-001", 3));

        assertEquals("El producto 'CPU-001' no está en tu carrito.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    // --------------------------------------------------------- eliminar / vaciar

    @Test
    void eliminar_borraElProductoDelUsuarioYDevuelveElCarritoRestante() {
        CarritoItem restante = item(11L, USUARIO, "RAM-002", 1);
        ProductoResumen ram = producto("RAM-002", "Memoria 16GB", "59990", null, 4);
        when(repositorio.findByUsuarioIdOrderByIdAsc(USUARIO)).thenReturn(List.of(restante));
        when(productos.obtenerPorIds(List.of("RAM-002"))).thenReturn(List.of(ram));

        List<ItemCarritoResponse> carrito = servicio.eliminar(USUARIO, "CPU-001");

        verify(repositorio).deleteByUsuarioIdAndProductoId(USUARIO, "CPU-001");
        assertEquals(1, carrito.size());
        assertEquals("RAM-002", carrito.get(0).productoId());
    }

    @Test
    void eliminar_ultimoProducto_devuelveCarritoVacio() {
        List<ItemCarritoResponse> carrito = servicio.eliminar(USUARIO, "CPU-001");

        verify(repositorio).deleteByUsuarioIdAndProductoId(USUARIO, "CPU-001");
        assertTrue(carrito.isEmpty());
    }

    @Test
    void vaciar_borraTodoElCarritoDelUsuario() {
        servicio.vaciar(USUARIO);

        verify(repositorio).deleteByUsuarioId(USUARIO);
    }
}
