package cl.clstore.categoria.repository;

import static cl.clstore.categoria.testsupport.DatosPrueba.categoria;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.categoria.model.Categoria;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar el mapeo de la entidad y el orden
 * que usa el servicio. Cada test se revierte solo al terminar (@DataJpaTest es transaccional).
 */
@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @Test
    void guardar_yLeerDeNuevo_conservaTodosLosCampos() {
        repositorio.saveAndFlush(categoria("consolas_laptops", "Consolas & Laptops", "fa-gamepad"));
        em.clear();   // fuerza a leer de la base y no de la caché de la sesión

        Categoria leida = repositorio.findById("consolas_laptops").orElseThrow();

        assertThat(leida.getValor()).isEqualTo("consolas_laptops");
        assertThat(leida.getEtiqueta()).isEqualTo("Consolas & Laptops");
        assertThat(leida.getIcono()).isEqualTo("fa-gamepad");
    }

    @Test
    void findAll_ordenadoPorEtiquetaAscendente_comoLoPideElServicio() {
        repositorio.saveAllAndFlush(List.of(
            categoria("zeta", "Zeta", "fa-z"),
            categoria("alfa", "Alfa", "fa-a"),
            categoria("mouse", "Mouse", "fa-m")));

        List<String> etiquetas = repositorio.findAll(Sort.by(Sort.Direction.ASC, "etiqueta"))
            .stream().map(Categoria::getEtiqueta).toList();

        assertThat(etiquetas).containsExactly("Alfa", "Mouse", "Zeta");
    }

    @Test
    void guardar_conElMismoValor_actualizaLaFilaEnVezDeDuplicarla() {
        repositorio.saveAndFlush(categoria("redes", "Redes", "fa-wifi"));
        em.clear();

        repositorio.saveAndFlush(categoria("redes", "Redes y conectividad", "fa-network-wired"));
        em.clear();

        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(repositorio.findById("redes").orElseThrow().getEtiqueta()).isEqualTo("Redes y conectividad");
    }

    @Test
    void actualizarEtiquetaEIcono_sePersisteYElValorNoCambia() {
        repositorio.saveAndFlush(categoria("redes", "Redes", "fa-wifi"));
        Categoria c = repositorio.findById("redes").orElseThrow();

        c.setEtiqueta("Conectividad");
        c.setIcono("fa-signal");
        repositorio.saveAndFlush(c);
        em.clear();

        Categoria leida = repositorio.findById("redes").orElseThrow();
        assertThat(leida.getValor()).isEqualTo("redes");
        assertThat(leida.getEtiqueta()).isEqualTo("Conectividad");
        assertThat(leida.getIcono()).isEqualTo("fa-signal");
    }

    @Test
    void existsById_distingueEntreExistenteEInexistente() {
        repositorio.saveAndFlush(categoria("redes", "Redes", "fa-wifi"));

        assertThat(repositorio.existsById("redes")).isTrue();
        assertThat(repositorio.existsById("fantasma")).isFalse();
    }

    @Test
    void eliminar_porValor_quitaLaCategoria() {
        repositorio.saveAndFlush(categoria("redes", "Redes", "fa-wifi"));

        repositorio.deleteById("redes");
        repositorio.flush();

        assertThat(repositorio.findById("redes")).isEmpty();
    }
}
