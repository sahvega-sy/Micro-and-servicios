package cl.clstore.productos.service;

import static cl.clstore.productos.testsupport.DatosPrueba.producto;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.clstore.productos.dto.StockRequest;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.model.Producto;
import cl.clstore.productos.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductoRepository repositorio;

    @InjectMocks
    private StockService servicio;

    private static StockRequest.Item item(String id, int cantidad) {
        return new StockRequest.Item(id, cantidad);
    }

    // ---------------------------------------------------------------- descontar

    @Test
    void descontar_stockSuficiente_descuentaCadaProducto() {
        when(repositorio.descontarStock("A", 2)).thenReturn(1);
        when(repositorio.descontarStock("B", 1)).thenReturn(1);

        assertDoesNotThrow(() -> servicio.descontar(List.of(item("A", 2), item("B", 1))));

        verify(repositorio).descontarStock("A", 2);
        verify(repositorio).descontarStock("B", 1);
    }

    @Test
    void descontar_itemsRepetidos_sumaLasCantidadesEnUnaSolaOperacion() {
        when(repositorio.descontarStock("A", 5)).thenReturn(1);

        servicio.descontar(List.of(item("A", 2), item("A", 3)));

        verify(repositorio, times(1)).descontarStock("A", 5);
        verify(repositorio, times(1)).descontarStock(eq("A"), anyInt());
    }

    @Test
    void descontar_idConEspacios_seNormalizaConTrim() {
        when(repositorio.descontarStock("A", 2)).thenReturn(1);

        servicio.descontar(List.of(item("  A ", 1), item("A", 1)));

        // " A " y "A" son el mismo producto: se agrupan (1 + 1 = 2)
        verify(repositorio).descontarStock("A", 2);
    }

    @Test
    void descontar_procesaLosProductosOrdenadosPorId() {
        when(repositorio.descontarStock("A", 1)).thenReturn(1);
        when(repositorio.descontarStock("B", 1)).thenReturn(1);
        when(repositorio.descontarStock("C", 1)).thenReturn(1);

        servicio.descontar(List.of(item("C", 1), item("A", 1), item("B", 1)));

        InOrder orden = inOrder(repositorio);
        orden.verify(repositorio).descontarStock("A", 1);
        orden.verify(repositorio).descontarStock("B", 1);
        orden.verify(repositorio).descontarStock("C", 1);
    }

    @Test
    void descontar_stockInsuficiente_lanzaConflictoConElDetalle() {
        Producto mouse = producto("A", "Mouse Gamer", "perifericos", 2);
        when(repositorio.descontarStock("A", 5)).thenReturn(0);
        when(repositorio.findById("A")).thenReturn(Optional.of(mouse));

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.descontar(List.of(item("A", 5))));

        assertEquals("Stock insuficiente para 'Mouse Gamer': disponible 2, solicitado 5.", ex.getMessage());
    }

    @Test
    void descontar_productoInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.descontarStock("X", 1)).thenReturn(0);
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.descontar(List.of(item("X", 1))));

        assertEquals("No existe el producto 'X'.", ex.getMessage());
    }

    @Test
    void descontar_siUnProductoFalla_noProcesaLosSiguientes() {
        when(repositorio.descontarStock("A", 1)).thenReturn(1);
        when(repositorio.descontarStock("B", 9)).thenReturn(0);
        when(repositorio.findById("B")).thenReturn(Optional.of(producto("B", "Teclado", "perifericos", 3)));

        assertThrows(ConflictoException.class,
            () -> servicio.descontar(List.of(item("A", 1), item("B", 9), item("C", 1))));

        verify(repositorio).descontarStock("A", 1);
        verify(repositorio, never()).descontarStock(eq("C"), anyInt());
    }

    // ------------------------------------------------------------------ reponer

    @Test
    void reponer_sumaStockACadaProducto() {
        when(repositorio.reponerStock("A", 3)).thenReturn(1);
        when(repositorio.reponerStock("B", 1)).thenReturn(1);

        servicio.reponer(List.of(item("A", 3), item("B", 1)));

        verify(repositorio).reponerStock("A", 3);
        verify(repositorio).reponerStock("B", 1);
    }

    @Test
    void reponer_itemsRepetidos_sumaLasCantidades() {
        when(repositorio.reponerStock("A", 4)).thenReturn(1);

        servicio.reponer(List.of(item("A", 1), item("A", 3)));

        verify(repositorio, times(1)).reponerStock("A", 4);
    }

    @Test
    void reponer_productoInexistente_seIgnoraSinError() {
        when(repositorio.reponerStock("X", 2)).thenReturn(0);

        assertDoesNotThrow(() -> servicio.reponer(List.of(item("X", 2))));

        verify(repositorio).reponerStock("X", 2);
    }
}
