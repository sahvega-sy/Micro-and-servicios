package cl.clstore.productos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.productos.model.Producto;
import cl.clstore.productos.testsupport.DatosPrueba;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar SQL real:
 * el descuento atómico de stock y los filtros combinables del listado.
 * Cada test se revierte solo al terminar (@DataJpaTest es transaccional).
 */
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repositorio;

    @BeforeEach
    void cargarDatos() {
        Producto ryzen = DatosPrueba.producto("CPU-001", "Procesador Ryzen 5", "componentes", 10);
        Producto intel = DatosPrueba.producto("CPU-002", "Procesador Intel i7", "componentes", 4);
        intel.setDescuentoPorcentaje(15);
        Producto mouse = DatosPrueba.producto("MOU-001", "Mouse Gamer", "perifericos", 0);
        mouse.setDescuentoPorcentaje(10);
        Producto teclado = DatosPrueba.producto("TEC-001", "Teclado Mecánico", "perifericos", 7);
        repositorio.saveAllAndFlush(List.of(ryzen, intel, mouse, teclado));
    }

    private List<String> ids(String categoria, String texto, boolean ofertas, String excluir, List<String> ids) {
        return repositorio.findAll(ProductoSpecs.filtrar(categoria, texto, ofertas, excluir, ids))
            .stream().map(Producto::getId).toList();
    }

    // ------------------------------------------------------------ ProductoSpecs

    @Test
    void filtrar_sinCriterios_devuelveTodos() {
        assertThat(ids(null, null, false, null, null))
            .containsExactlyInAnyOrder("CPU-001", "CPU-002", "MOU-001", "TEC-001");
    }

    @Test
    void filtrar_porCategoria() {
        assertThat(ids("perifericos", null, false, null, null)).containsExactlyInAnyOrder("MOU-001", "TEC-001");
    }

    @Test
    void filtrar_porTexto_ignoraMayusculasYBuscaEnElNombre() {
        assertThat(ids(null, "RYZEN", false, null, null)).containsExactly("CPU-001");
        assertThat(ids(null, "procesador", false, null, null)).containsExactlyInAnyOrder("CPU-001", "CPU-002");
    }

    @Test
    void filtrar_soloOfertas_devuelveLosQueTienenDescuento() {
        assertThat(ids(null, null, true, null, null)).containsExactlyInAnyOrder("CPU-002", "MOU-001");
    }

    @Test
    void filtrar_excluirId_omiteEseProducto() {
        assertThat(ids("componentes", null, false, "CPU-001", null)).containsExactly("CPU-002");
    }

    @Test
    void filtrar_porListaDeIds() {
        assertThat(ids(null, null, false, null, List.of("TEC-001", "CPU-001")))
            .containsExactlyInAnyOrder("TEC-001", "CPU-001");
    }

    @Test
    void filtrar_criteriosCombinados_seAplicanTodosJuntos() {
        assertThat(ids("componentes", "procesador", true, null, null)).containsExactly("CPU-002");
    }

    @Test
    void filtrar_criteriosEnBlancoSeIgnoran() {
        assertThat(ids("  ", "", false, " ", List.of())).hasSize(4);
    }

    // ------------------------------------------------ descuento / reposición de stock

    private int stockDe(String id) {
        return repositorio.findById(id).orElseThrow().getStock();
    }

    @Test
    void descontarStock_conStockSuficiente_restaYDevuelveUnaFila() {
        assertThat(repositorio.descontarStock("CPU-001", 3)).isEqualTo(1);

        assertThat(stockDe("CPU-001")).isEqualTo(7);
    }

    @Test
    void descontarStock_cantidadIgualAlStock_dejaEnCero() {
        assertThat(repositorio.descontarStock("CPU-002", 4)).isEqualTo(1);

        assertThat(stockDe("CPU-002")).isZero();
    }

    @Test
    void descontarStock_cantidadMayorAlStock_noModificaNadaYDevuelveCero() {
        assertThat(repositorio.descontarStock("CPU-002", 5)).isZero();

        assertThat(stockDe("CPU-002")).isEqualTo(4);
    }

    @Test
    void descontarStock_productoSinStock_devuelveCero() {
        assertThat(repositorio.descontarStock("MOU-001", 1)).isZero();
    }

    @Test
    void descontarStock_productoInexistente_devuelveCero() {
        assertThat(repositorio.descontarStock("NO-EXISTE", 1)).isZero();
    }

    @Test
    void reponerStock_sumaLaCantidad() {
        assertThat(repositorio.reponerStock("MOU-001", 5)).isEqualTo(1);

        assertThat(stockDe("MOU-001")).isEqualTo(5);
    }

    @Test
    void reponerStock_productoInexistente_devuelveCero() {
        assertThat(repositorio.reponerStock("NO-EXISTE", 5)).isZero();
    }
}
