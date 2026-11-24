package cl.clstore.resenas.repository;

import static cl.clstore.resenas.testsupport.DatosPrueba.resena;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.resenas.model.Resena;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** OPCIONAL. H2 en memoria (reemplaza a MySQL) para probar el orden de la consulta derivada y el mapeo. */
@DataJpaTest
class ResenaRepositoryTest {

    @Autowired
    private ResenaRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByProductoId_ordenaPorFechaDescYLuegoPorIdDesc() {
        repositorio.saveAllAndFlush(List.of(
            resena(null, "CPU-001", "A", 3, "antigua", LocalDate.of(2026, 1, 10)),
            resena(null, "CPU-001", "B", 4, "marzo-primera", LocalDate.of(2026, 3, 10)),
            resena(null, "CPU-001", "C", 5, "marzo-segunda", LocalDate.of(2026, 3, 10)),
            resena(null, "RAM-002", "D", 2, "otro producto", LocalDate.of(2026, 4, 1))));

        List<String> comentarios = repositorio.findByProductoIdOrderByFechaDescIdDesc("CPU-001")
            .stream().map(Resena::getComentario).toList();

        assertThat(comentarios).containsExactly("marzo-segunda", "marzo-primera", "antigua");
    }

    @Test
    void findByProductoId_productoSinResenas_devuelveListaVacia() {
        assertThat(repositorio.findByProductoIdOrderByFechaDescIdDesc("NUEVO")).isEmpty();
    }

    @Test
    void guardar_sinFecha_asignaIdYFechaDeHoy() {
        Resena guardada = repositorio.saveAndFlush(resena(null, "CPU-001", "Ana", 5, "Ok", null));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getFecha()).isNotNull().isBeforeOrEqualTo(LocalDate.now());
    }

    @Test
    void guardar_yLeerDeNuevo_conservaTodosLosCampos() {
        Long id = repositorio.saveAndFlush(
            resena(null, "CPU-001", "Ana Pérez", 4, "Muy bueno, ñandú", LocalDate.of(2026, 3, 10))).getId();
        em.clear();

        Resena leida = repositorio.findById(id).orElseThrow();

        assertThat(leida.getProductoId()).isEqualTo("CPU-001");
        assertThat(leida.getUsuario()).isEqualTo("Ana Pérez");
        assertThat(leida.getCalificacion()).isEqualTo(4);
        assertThat(leida.getComentario()).isEqualTo("Muy bueno, ñandú");
        assertThat(leida.getFecha()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void guardar_comentarioDeMilCaracteres_cabeEnLaColumna() {
        Long id = repositorio.saveAndFlush(resena(null, "CPU-001", "Ana", 5, "c".repeat(1000), null)).getId();
        em.clear();

        assertThat(repositorio.findById(id).orElseThrow().getComentario()).hasSize(1000);
    }

    @Test
    void eliminar_porId_quitaLaResena() {
        Long id = repositorio.saveAndFlush(resena(null, "CPU-001", "Ana", 5, "Ok", null)).getId();

        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(repositorio.existsById(id)).isFalse();
    }
}
