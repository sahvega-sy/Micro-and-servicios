package cl.clstore.contacto.repository;

import static cl.clstore.contacto.testsupport.DatosPrueba.contacto;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.contacto.model.Contacto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar SQL real: el orden de la bandeja
 * y el mapeo de la entidad. Cada test se revierte solo al terminar (@DataJpaTest es transaccional).
 */
@DataJpaTest
class ContactoRepositoryTest {

    @Autowired
    private ContactoRepository repositorio;

    @Autowired
    private TestEntityManager em;

    // ------------------------------------------------------------------- orden

    @Test
    void findAllByOrderByCreadoEnDesc_devuelveLosMasRecientesPrimero() {
        repositorio.saveAll(List.of(
            contacto(null, "Más antiguo", "a@gmail.com", "m1", Instant.parse("2026-01-01T10:00:00Z")),
            contacto(null, "Más nuevo", "b@gmail.com", "m2", Instant.parse("2026-03-01T10:00:00Z")),
            contacto(null, "Intermedio", "c@gmail.com", "m3", Instant.parse("2026-02-01T10:00:00Z"))));
        repositorio.flush();

        List<String> nombres = repositorio.findAllByOrderByCreadoEnDesc().stream().map(Contacto::getNombre).toList();

        assertThat(nombres).containsExactly("Más nuevo", "Intermedio", "Más antiguo");
    }

    @Test
    void findAllByOrderByCreadoEnDesc_sinDatos_devuelveListaVacia() {
        assertThat(repositorio.findAllByOrderByCreadoEnDesc()).isEmpty();
    }

    // ------------------------------------------------------------------ mapeo

    @Test
    void guardar_sinFecha_asignaCreadoEnEIdAutomaticamente() {
        Contacto guardado = repositorio.saveAndFlush(contacto(null, "Ana", "ana@gmail.com", "Hola", null));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCreadoEn()).isNotNull();
    }

    @Test
    void guardar_conFechaInformada_laConserva() {
        Instant fecha = Instant.parse("2026-01-15T12:00:00Z");

        Contacto guardado = repositorio.saveAndFlush(contacto(null, "Ana", "ana@gmail.com", "Hola", fecha));

        assertThat(guardado.getCreadoEn()).isEqualTo(fecha);
    }

    @Test
    void guardar_yLeerDeNuevo_conservaTodosLosCampos() {
        Long id = repositorio.saveAndFlush(
            contacto(null, "Ana Pérez", "ana@gmail.com", "Mensaje con tildes: ñandú", Instant.parse("2026-01-15T12:00:00Z"))).getId();
        em.clear();   // fuerza a leer de la base y no de la caché de la sesión

        Contacto leido = repositorio.findById(id).orElseThrow();

        assertThat(leido.getNombre()).isEqualTo("Ana Pérez");
        assertThat(leido.getEmail()).isEqualTo("ana@gmail.com");
        assertThat(leido.getMensaje()).isEqualTo("Mensaje con tildes: ñandú");
        assertThat(leido.getCreadoEn()).isEqualTo(Instant.parse("2026-01-15T12:00:00Z"));
    }

    @Test
    void guardar_mensajeDeDosMilCaracteres_cabeEnLaColumna() {
        String largo = "m".repeat(2000);

        Long id = repositorio.saveAndFlush(contacto(null, "Ana", "ana@gmail.com", largo, null)).getId();
        em.clear();

        assertThat(repositorio.findById(id).orElseThrow().getMensaje()).hasSize(2000);
    }

    @Test
    void eliminar_porId_quitaElRegistro() {
        Long id = repositorio.saveAndFlush(contacto(null, "Ana", "ana@gmail.com", "Hola", null)).getId();

        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(repositorio.existsById(id)).isFalse();
    }
}
