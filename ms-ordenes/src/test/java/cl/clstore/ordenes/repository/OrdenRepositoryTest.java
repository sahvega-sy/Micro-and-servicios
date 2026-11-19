package cl.clstore.ordenes.repository;

import static cl.clstore.ordenes.testsupport.DatosPrueba.orden;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.ordenes.model.Orden;
import cl.clstore.ordenes.model.OrdenItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar SQL real: las consultas derivadas,
 * la cascada orden -> líneas y la columna orden_id de solo lectura. Cada test se revierte solo al terminar.
 */
@DataJpaTest
class OrdenRepositoryTest {

    @Autowired
    private OrdenRepository repositorio;

    @Autowired
    private OrdenItemRepository itemsRepositorio;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void cargarDatos() {
        repositorio.saveAllAndFlush(List.of(
            orden(null, "ana", Orden.EXITOSA, "1000", Instant.parse("2026-01-01T10:00:00Z")),
            orden(null, "ana", Orden.FALLIDA, "0", Instant.parse("2026-03-01T10:00:00Z")),
            orden(null, "luis", Orden.EXITOSA, "2000", Instant.parse("2026-02-01T10:00:00Z")),
            orden(null, null, Orden.EXITOSA, "3000", Instant.parse("2026-04-01T10:00:00Z"))));   // invitado
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void findAllByOrderByCreadoEnDesc_devuelveLasMasRecientesPrimero() {
        List<Orden> todas = repositorio.findAllByOrderByCreadoEnDesc();

        assertThat(todas).extracting(Orden::getEstado)
            .containsExactly(Orden.EXITOSA, Orden.FALLIDA, Orden.EXITOSA, Orden.EXITOSA);
        assertThat(todas.get(0).getCreadoEn()).isEqualTo(Instant.parse("2026-04-01T10:00:00Z"));
        assertThat(todas.get(3).getCreadoEn()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void findByEstadoOrderByCreadoEnDesc_filtraYOrdena() {
        List<Orden> exitosas = repositorio.findByEstadoOrderByCreadoEnDesc(Orden.EXITOSA);
        List<Orden> fallidas = repositorio.findByEstadoOrderByCreadoEnDesc(Orden.FALLIDA);

        assertThat(exitosas).hasSize(3);
        assertThat(exitosas.get(0).getCreadoEn()).isAfter(exitosas.get(2).getCreadoEn());
        assertThat(fallidas).hasSize(1);
    }

    @Test
    void findByUsuarioIdOrderByCreadoEnDesc_devuelveSoloLasDeEseUsuario() {
        List<Orden> deAna = repositorio.findByUsuarioIdOrderByCreadoEnDesc("ana");

        assertThat(deAna).hasSize(2);
        assertThat(deAna).extracting(Orden::getEstado).containsExactly(Orden.FALLIDA, Orden.EXITOSA);
        assertThat(repositorio.findByUsuarioIdOrderByCreadoEnDesc("nadie")).isEmpty();
    }

    @Test
    void ordenesDeInvitado_noAparecenEnLasConsultasPorUsuario() {
        assertThat(repositorio.findAll().stream().filter(o -> o.getUsuarioId() == null)).hasSize(1);
        assertThat(repositorio.findByUsuarioIdOrderByCreadoEnDesc("ana")).noneMatch(o -> o.getUsuarioId() == null);
    }

    // --------------------------------------------------------------- mapeo y cascada

    @Test
    void guardarOrdenConLineas_laCascadaGuardaTambienLosItems() {
        Orden nueva = orden(null, "maria", Orden.EXITOSA, "280974", null);
        nueva.getItems().add(new OrdenItem(nueva, "CPU-001", "Procesador Ryzen 5", 2, new BigDecimal("110492")));
        nueva.getItems().add(new OrdenItem(nueva, "RAM-002", "Memoria 16GB", 1, new BigDecimal("59990")));

        Long id = repositorio.saveAndFlush(nueva).getId();
        em.clear();

        Orden leida = repositorio.findById(id).orElseThrow();
        assertThat(leida.getCreadoEn()).isNotNull();                          // @PrePersist
        assertThat(leida.getItems()).hasSize(2);
        assertThat(leida.getItems()).extracting(OrdenItem::getProductoId).containsExactlyInAnyOrder("CPU-001", "RAM-002");
        assertThat(itemsRepositorio.count()).isEqualTo(2);
    }

    @Test
    void lineaGuardada_conservaNombreYPrecioCongelados_yExponeElIdDeLaOrden() {
        Orden nueva = orden(null, "maria", Orden.EXITOSA, "110492", null);
        nueva.getItems().add(new OrdenItem(nueva, "CPU-001", "Procesador Ryzen 5", 1, new BigDecimal("110492")));
        Long ordenId = repositorio.saveAndFlush(nueva).getId();
        em.clear();

        OrdenItem item = itemsRepositorio.findAll().get(0);

        assertThat(item.getOrdenId()).isEqualTo(ordenId);                     // copia de solo lectura de la FK
        assertThat(item.getNombreProducto()).isEqualTo("Procesador Ryzen 5");
        assertThat(item.getCantidad()).isEqualTo(1);
        assertThat(item.getPrecioUnitario()).isEqualByComparingTo("110492");
    }

    @Test
    void guardar_yLeerDeNuevo_conservaLosDatosDelComprador() {
        Long id = repositorio.saveAndFlush(
            orden(null, "maria", Orden.EXITOSA, "12990", Instant.parse("2026-05-01T00:00:00Z"))).getId();
        em.clear();

        Orden leida = repositorio.findById(id).orElseThrow();

        assertThat(leida.getNombreComprador()).isEqualTo("Ana");
        assertThat(leida.getApellidosComprador()).isEqualTo("Pérez");
        assertThat(leida.getCorreoComprador()).isEqualTo("ana@gmail.com");
        assertThat(leida.getRegion()).isEqualTo("Región Metropolitana de Santiago");
        assertThat(leida.getDireccion()).isEqualTo("Calle Falsa 123");
        assertThat(leida.getTotal()).isEqualByComparingTo("12990");
    }

    @Test
    void eliminarOrden_borraEnCascadaSusLineas() {
        Orden nueva = orden(null, "maria", Orden.EXITOSA, "100", null);
        nueva.getItems().add(new OrdenItem(nueva, "CPU-001", "Procesador", 1, new BigDecimal("100")));
        Long id = repositorio.saveAndFlush(nueva).getId();
        em.clear();

        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(itemsRepositorio.count()).isZero();
    }
}
