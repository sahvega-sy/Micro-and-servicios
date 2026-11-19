package cl.clstore.categoria.service;

import static cl.clstore.categoria.testsupport.DatosPrueba.categoria;
import static cl.clstore.categoria.testsupport.DatosPrueba.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.categoria.client.ProductoClient;
import cl.clstore.categoria.exception.ConflictoException;
import cl.clstore.categoria.exception.RecursoNoEncontradoException;
import cl.clstore.categoria.exception.ReglaNegocioException;
import cl.clstore.categoria.exception.ServicioNoDisponibleException;
import cl.clstore.categoria.model.Categoria;
import cl.clstore.categoria.repository.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repositorio;

    @Mock
    private ProductoClient productos;

    @InjectMocks
    private CategoriaService servicio;

    // ----------------------------------------------------------------- listar

    @Test
    void listar_pideLasCategoriasOrdenadasPorEtiquetaAscendente() {
        List<Categoria> esperadas = List.of(categoria("a", "A", "fa-a"), categoria("b", "B", "fa-b"));
        when(repositorio.findAll(any(Sort.class))).thenReturn(esperadas);

        List<Categoria> resultado = servicio.listar();

        assertSame(esperadas, resultado);
        ArgumentCaptor<Sort> orden = ArgumentCaptor.forClass(Sort.class);
        verify(repositorio).findAll(orden.capture());
        assertEquals(Sort.by(Sort.Direction.ASC, "etiqueta"), orden.getValue());
    }

    // ---------------------------------------------------------------- obtener

    @Test
    void obtener_categoriaExistente_laDevuelve() {
        Categoria esperada = categoria();
        when(repositorio.findById("componentes")).thenReturn(Optional.of(esperada));

        assertSame(esperada, servicio.obtener("componentes"));
    }

    @Test
    void obtener_categoriaInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.obtener("fantasma"));

        assertEquals("No existe la categoría 'fantasma'.", ex.getMessage());
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_generaElValorDesdeLaEtiquetaYGuarda() {
        when(repositorio.existsById("consolas_laptops")).thenReturn(false);
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = servicio.crear(request("  Consolas & Laptops ", "fa-gamepad"));

        assertEquals("consolas_laptops", resultado.getValor());
        assertEquals("Consolas & Laptops", resultado.getEtiqueta());      // etiqueta sin espacios sobrantes
        assertEquals("fa-gamepad", resultado.getIcono());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void crear_sinIcono_usaElIconoPorDefecto(String iconoVacio) {
        when(repositorio.existsById("redes")).thenReturn(false);
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("fa-folder", servicio.crear(request("Redes", iconoVacio)).getIcono());
        assertEquals("fa-folder", servicio.crear(request("Redes", null)).getIcono());
    }

    @Test
    void crear_iconoInformado_seGuardaSinEspacios() {
        when(repositorio.existsById("redes")).thenReturn(false);
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("fa-wifi", servicio.crear(request("Redes", "  fa-wifi ")).getIcono());
    }

    @ParameterizedTest
    @ValueSource(strings = {"¡¡¡", "---", "***", "   !!   "})
    void crear_etiquetaSinLetrasNiNumeros_lanzaReglaNegocio(String etiqueta) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.crear(request(etiqueta, null)));

        assertEquals("Ingresa un nombre válido para la categoría.", ex.getMessage());
        verifyNoInteractions(repositorio);
    }

    @Test
    void crear_valorYaExistente_lanzaConflictoYNoGuarda() {
        when(repositorio.existsById("componentes")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.crear(request("Componentes", null)));

        assertEquals("Esa categoría ya existe.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_etiquetasQueGeneranElMismoValor_seConsideranDuplicadas() {
        when(repositorio.existsById("componentes")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.crear(request("COMPONÉNTES!", null)));
    }

    // -------------------------------------------------------------- actualizar

    @Test
    void actualizar_cambiaEtiquetaEIconoPeroNoElValor() {
        Categoria existente = categoria("componentes", "Componentes", "fa-microchip");
        when(repositorio.findById("componentes")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = servicio.actualizar("componentes", request("  Partes y piezas ", " fa-cog "));

        assertEquals("componentes", resultado.getValor());                // el valor es la referencia de los productos
        assertEquals("Partes y piezas", resultado.getEtiqueta());
        assertEquals("fa-cog", resultado.getIcono());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void actualizar_iconoEnBlanco_conservaElIconoActual(String iconoVacio) {
        Categoria existente = categoria("componentes", "Componentes", "fa-microchip");
        when(repositorio.findById("componentes")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("fa-microchip", servicio.actualizar("componentes", request("Nuevo", iconoVacio)).getIcono());
    }

    @Test
    void actualizar_iconoNull_conservaElIconoActual() {
        Categoria existente = categoria("componentes", "Componentes", "fa-microchip");
        when(repositorio.findById("componentes")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("fa-microchip", servicio.actualizar("componentes", request("Nuevo", null)).getIcono());
    }

    @Test
    void actualizar_categoriaInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizar("fantasma", request("X", null)));

        verify(repositorio, never()).save(any());
    }

    // --------------------------------------------------------------- eliminar

    @Test
    void eliminar_sinProductosAsociados_laBorra() {
        when(repositorio.findById("redes")).thenReturn(Optional.of(categoria("redes", "Redes", "fa-wifi")));
        when(productos.existenProductosEnCategoria("redes")).thenReturn(false);

        servicio.eliminar("redes");

        verify(repositorio).deleteById("redes");
    }

    @Test
    void eliminar_conProductosAsociados_lanzaConflictoYNoBorra() {
        when(repositorio.findById("componentes")).thenReturn(Optional.of(categoria()));
        when(productos.existenProductosEnCategoria("componentes")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> servicio.eliminar("componentes"));

        assertEquals("No se puede eliminar la categoría porque tiene productos asociados. "
            + "Cambia primero la categoría de esos productos.", ex.getMessage());
        verify(repositorio, never()).deleteById(any());
    }

    @Test
    void eliminar_categoriaInexistente_noConsultaAMsProductos() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar("fantasma"));

        verifyNoInteractions(productos);
        verify(repositorio, never()).deleteById(any());
    }

    @Test
    void eliminar_msProductosCaido_propagaServicioNoDisponibleYNoBorra() {
        when(repositorio.findById("redes")).thenReturn(Optional.of(categoria("redes", "Redes", "fa-wifi")));
        when(productos.existenProductosEnCategoria("redes"))
            .thenThrow(new ServicioNoDisponibleException("ms-productos no está disponible."));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.eliminar("redes"));

        verify(repositorio, never()).deleteById(any());
    }

    // ------------------------------------------------------------ generarValor

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Consolas & Laptops|consolas_laptops",
        "Componentes|componentes",
        "  Periféricos  |perifericos",
        "Cámaras y Fotografía|camaras_y_fotografia",
        "Año Nuevo 2026|ano_nuevo_2026",
        "__Redes__|redes",
        "a   b---c|a_b_c"})
    void generarValor_conviertePalabrasASlug(String texto, String esperado) {
        assertEquals(esperado, CategoriaService.generarValor(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "!!!", "¿?"})
    void generarValor_sinCaracteresValidos_devuelveCadenaVacia(String texto) {
        assertEquals("", CategoriaService.generarValor(texto));
    }
}
