package cl.clstore.carrito.repository;

import static cl.clstore.carrito.testsupport.DatosPrueba.item;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.carrito.model.CarritoItem;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar las consultas derivadas con SQL real.
 * Cada test se revierte solo al terminar (@DataJpaTest es transaccional).
 */
@DataJpaTest
class CarritoItemRepositoryTest {

    @Autowired
    private CarritoItemRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void cargarDatos() {
        repositorio.saveAllAndFlush(List.of(
            item(null, "ana", "CPU-001", 2),
            item(null, "ana", "RAM-002", 1),
            item(null, "luis", "CPU-001", 5)));
    }

    private List<String> productosDe(String usuario) {
        return repositorio.findByUsuarioIdOrderByIdAsc(usuario).stream().map(CarritoItem::getProductoId).toList();
    }

    // ------------------------------------------------------------------ lecturas

    @Test
    void findByUsuarioIdOrderByIdAsc_devuelveSoloLosDelUsuarioEnOrdenDeInsercion() {
        assertThat(productosDe("ana")).containsExactly("CPU-001", "RAM-002");
        assertThat(productosDe("luis")).containsExactly("CPU-001");
    }

    @Test
    void findByUsuarioIdOrderByIdAsc_usuarioSinCarrito_devuelveListaVacia() {
        assertThat(productosDe("nadie")).isEmpty();
    }

    @Test
    void findByUsuarioIdAndProductoId_encuentraLaLineaDelUsuario() {
        CarritoItem encontrado = repositorio.findByUsuarioIdAndProductoId("luis", "CPU-001").orElseThrow();

        assertThat(encontrado.getCantidad()).isEqualTo(5);
        assertThat(encontrado.getUsuarioId()).isEqualTo("luis");
    }

    @Test
    void findByUsuarioIdAndProductoId_productoDeOtroUsuario_noLoEncuentra() {
        assertThat(repositorio.findByUsuarioIdAndProductoId("luis", "RAM-002")).isEmpty();
    }

    // ------------------------------------------------------------------- mapeo

    @Test
    void guardar_asignaIdYFechaDeCreacion() {
        CarritoItem guardado = repositorio.saveAndFlush(item(null, "maria", "MOU-001", 1));

        assertThat(guardado.getId()).isNotNull();
        em.clear();
        assertThat(repositorio.findById(guardado.getId())).isPresent();
    }

    @Test
    void actualizarCantidad_sePersiste() {
        CarritoItem item = repositorio.findByUsuarioIdAndProductoId("ana", "CPU-001").orElseThrow();

        item.setCantidad(9);
        repositorio.saveAndFlush(item);
        em.clear();

        assertThat(repositorio.findByUsuarioIdAndProductoId("ana", "CPU-001").orElseThrow().getCantidad()).isEqualTo(9);
    }

    // ------------------------------------------------------------------ borrados

    @Test
    void deleteByUsuarioIdAndProductoId_borraSoloEsaLinea() {
        repositorio.deleteByUsuarioIdAndProductoId("ana", "CPU-001");
        repositorio.flush();

        assertThat(productosDe("ana")).containsExactly("RAM-002");
        assertThat(productosDe("luis")).containsExactly("CPU-001");      // el mismo producto de otro usuario sigue
    }

    @Test
    void deleteByUsuarioIdAndProductoId_lineaInexistente_noHaceNada() {
        repositorio.deleteByUsuarioIdAndProductoId("ana", "NO-EXISTE");
        repositorio.flush();

        assertThat(repositorio.count()).isEqualTo(3);
    }

    @Test
    void deleteByUsuarioId_vaciaElCarritoDeEseUsuarioSolamente() {
        repositorio.deleteByUsuarioId("ana");
        repositorio.flush();

        assertThat(productosDe("ana")).isEmpty();
        assertThat(productosDe("luis")).containsExactly("CPU-001");
    }
}
