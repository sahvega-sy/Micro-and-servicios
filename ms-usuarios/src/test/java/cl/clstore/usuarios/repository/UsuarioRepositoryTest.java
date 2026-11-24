package cl.clstore.usuarios.repository;

import static cl.clstore.usuarios.testsupport.DatosPrueba.usuario;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.usuarios.model.Usuario;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar las consultas derivadas con SQL real
 * (búsquedas por correo sin distinguir mayúsculas, existencia de RUN y correo). Cada test se revierte solo.
 */
@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void cargarDatos() {
        repositorio.saveAllAndFlush(List.of(
            usuario("id-ana", "Cliente", "ana@gmail.com", "190110222", "hash-ana"),
            usuario("id-luis", "Vendedor", "luis@duoc.cl", "10000013K", "hash-luis")));
    }

    // --------------------------------------------------------------- por correo

    @Test
    void findByCorreoIgnoreCase_noDistingueMayusculas() {
        assertThat(repositorio.findByCorreoIgnoreCase("ANA@Gmail.COM")).get().extracting(Usuario::getId).isEqualTo("id-ana");
        assertThat(repositorio.findByCorreoIgnoreCase("ana@gmail.com")).isPresent();
    }

    @Test
    void findByCorreoIgnoreCase_correoInexistente_devuelveVacio() {
        assertThat(repositorio.findByCorreoIgnoreCase("nadie@gmail.com")).isEmpty();
    }

    @Test
    void existsByCorreoIgnoreCase_distingueEntreExistenteEInexistente() {
        assertThat(repositorio.existsByCorreoIgnoreCase("LUIS@duoc.cl")).isTrue();
        assertThat(repositorio.existsByCorreoIgnoreCase("nadie@duoc.cl")).isFalse();
    }

    @Test
    void existsByCorreoIgnoreCaseAndIdNot_ignoraAlPropioUsuario() {
        // Mismo correo y mismo id: es el propio usuario editándose, no un duplicado.
        assertThat(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-ana")).isFalse();
        // Mismo correo con otro id: ya lo usa otra cuenta.
        assertThat(repositorio.existsByCorreoIgnoreCaseAndIdNot("ana@gmail.com", "id-luis")).isTrue();
        assertThat(repositorio.existsByCorreoIgnoreCaseAndIdNot("libre@gmail.com", "id-luis")).isFalse();
    }

    // ------------------------------------------------------------------- por RUN

    @Test
    void existsByRun_distingueEntreExistenteEInexistente() {
        assertThat(repositorio.existsByRun("190110222")).isTrue();
        assertThat(repositorio.existsByRun("10000013K")).isTrue();
        assertThat(repositorio.existsByRun("123456785")).isFalse();
    }

    // ------------------------------------------------------------------- mapeo

    @Test
    void guardar_yLeerDeNuevo_conservaTodosLosCampos() {
        Usuario u = usuario("id-maria", "Administrador", "maria@gmail.com", "123456785", "hash-maria");
        u.setNombre("María José");
        u.setFechaNacimiento(LocalDate.of(1985, 12, 31));
        repositorio.saveAndFlush(u);
        em.clear();   // fuerza a leer de la base y no de la caché de la sesión

        Usuario leido = repositorio.findById("id-maria").orElseThrow();

        assertThat(leido.getNombre()).isEqualTo("María José");
        assertThat(leido.getApellidos()).isEqualTo("Pérez");
        assertThat(leido.getRun()).isEqualTo("123456785");
        assertThat(leido.getPasswordHash()).isEqualTo("hash-maria");
        assertThat(leido.getTipo()).isEqualTo("Administrador");
        assertThat(leido.getFechaNacimiento()).isEqualTo(LocalDate.of(1985, 12, 31));
        assertThat(leido.getCreatedAt()).isEqualTo(Instant.parse("2026-01-15T12:00:00Z"));
    }

    @Test
    void guardar_sinFechaDeCreacion_laAsignaAutomaticamente() {
        Usuario u = usuario("id-nuevo", "Cliente", "nuevo@gmail.com", "111111111", "hash");
        u.setCreatedAt(null);

        Usuario guardado = repositorio.saveAndFlush(u);

        assertThat(guardado.getCreatedAt()).isNotNull();
    }

    @Test
    void guardar_fechaDeNacimientoOpcional_sePersisteComoNula() {
        Usuario u = usuario("id-sin-fecha", "Cliente", "sinfecha@gmail.com", "111111111", "hash");
        u.setFechaNacimiento(null);
        repositorio.saveAndFlush(u);
        em.clear();

        assertThat(repositorio.findById("id-sin-fecha").orElseThrow().getFechaNacimiento()).isNull();
    }

    @Test
    void findAll_ordenadoPorNombreAscendente_comoLoPideElServicio() {
        Usuario beto = usuario("id-beto", "Cliente", "beto@gmail.com", "111111111", "hash");
        beto.setNombre("Beto");
        Usuario zoe = usuario("id-zoe", "Cliente", "zoe@gmail.com", "76543216", "hash");
        zoe.setNombre("Zoe");
        repositorio.saveAllAndFlush(List.of(zoe, beto));

        List<String> nombres = repositorio.findAll(Sort.by(Sort.Direction.ASC, "nombre"))
            .stream().map(Usuario::getNombre).toList();

        assertThat(nombres).containsExactly("Ana", "Ana", "Beto", "Zoe");
    }

    @Test
    void eliminar_porId_quitaAlUsuario() {
        repositorio.deleteById("id-ana");
        repositorio.flush();

        assertThat(repositorio.existsById("id-ana")).isFalse();
        assertThat(repositorio.count()).isEqualTo(1);
    }
}
