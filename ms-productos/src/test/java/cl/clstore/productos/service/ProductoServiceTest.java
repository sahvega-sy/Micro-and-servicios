package cl.clstore.productos.service;

import static cl.clstore.productos.testsupport.DatosPrueba.producto;
import static cl.clstore.productos.testsupport.DatosPrueba.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.productos.client.CategoriaClient;
import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.exception.ReglaNegocioException;
import cl.clstore.productos.exception.ServicioNoDisponibleException;
import cl.clstore.productos.model.Producto;
import cl.clstore.productos.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repositorio;

    @Mock
    private CategoriaClient categorias;

    @InjectMocks
    private ProductoService servicio;

    // ---------------------------------------------------------------- obtener

    @Test
    void obtener_productoExistente_loDevuelve() {
        Producto esperado = producto();
        when(repositorio.findById("CPU-001")).thenReturn(Optional.of(esperado));

        Producto resultado = servicio.obtener("CPU-001");

        assertSame(esperado, resultado);
        verify(repositorio).findById("CPU-001");
    }

    @Test
    void obtener_productoInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("NO-EXISTE")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.obtener("NO-EXISTE"));

        assertEquals("No existe el producto 'NO-EXISTE'.", ex.getMessage());
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_datosValidos_guardaProductoNormalizado() {
        when(repositorio.existsById("CPU-001")).thenReturn(false);
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = new ProductoRequest(" CPU-001 ", "  Procesador Ryzen 5 ", "Descripción",
            new BigDecimal("129990"), 10, 3, " componentes ", null, null, null, null, null);

        Producto resultado = servicio.crear(r);

        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(repositorio).save(guardado.capture());
        Producto p = guardado.getValue();
        assertSame(p, resultado);
        assertEquals("CPU-001", p.getId());                       // id sin espacios
        assertEquals("Procesador Ryzen 5", p.getNombre());        // nombre sin espacios
        assertEquals("procesador ryzen 5", p.getBusqueda());      // campo de búsqueda en minúscula
        assertEquals("componentes", p.getCategoria());            // categoría sin espacios
        assertEquals(new BigDecimal("129990"), p.getPrecio());
        assertEquals(10, p.getStock());
        assertEquals(3, p.getStockCritico());
        assertEquals("/img/Logo.png", p.getImagen());             // imagen por defecto
        verify(categorias).validarExiste("componentes");
    }

    @Test
    void crear_conImagenInformada_laConservaSinEspacios() {
        when(repositorio.existsById("CPU-001")).thenReturn(false);
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = new ProductoRequest("CPU-001", "Ryzen", null, new BigDecimal("1"), 1, null,
            "componentes", "  http://x/img.png  ", null, null, null, null);

        Producto resultado = servicio.crear(r);

        assertEquals("http://x/img.png", resultado.getImagen());
    }

    @Test
    void crear_conOpcionalesInformados_losGuarda() {
        when(repositorio.existsById("CPU-001")).thenReturn(false);
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = new ProductoRequest("CPU-001", "Ryzen", "desc", new BigDecimal("1"), 1, 1,
            "componentes", null, List.of("a.png", "b.png"), Map.of("tag", "nuevo"),
            Map.of("nucleos", 6), 25);

        Producto resultado = servicio.crear(r);

        assertEquals(List.of("a.png", "b.png"), resultado.getImagenes());
        assertEquals(Map.of("tag", "nuevo"), resultado.getDestacado());
        assertEquals(Map.of("nucleos", 6), resultado.getEspecificaciones());
        assertEquals(25, resultado.getDescuentoPorcentaje());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void crear_conIdVacioOEnBlanco_lanzaReglaNegocio(String idInvalido) {
        ProductoRequest r = request(idInvalido, "Ryzen", "componentes");

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(r));

        verifyNoInteractions(repositorio, categorias);
    }

    @Test
    void crear_sinId_lanzaReglaNegocio() {
        ProductoRequest r = request(null, "Ryzen", "componentes");

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(r));

        verifyNoInteractions(repositorio, categorias);
    }

    @Test
    void crear_idYaExistente_lanzaConflictoSinGuardar() {
        when(repositorio.existsById("CPU-001")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.crear(request("CPU-001", "Ryzen", "componentes")));

        assertEquals("Ya existe un producto con ese código.", ex.getMessage());
        verify(repositorio, never()).save(any());
        verifyNoInteractions(categorias);
    }

    @Test
    void crear_categoriaInexistente_propagaReglaNegocioSinGuardar() {
        when(repositorio.existsById("CPU-001")).thenReturn(false);
        doThrow(new ReglaNegocioException("La categoría 'x' no existe."))
            .when(categorias).validarExiste("x");

        assertThrows(ReglaNegocioException.class,
            () -> servicio.crear(request("CPU-001", "Ryzen", "x")));

        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_msCategoriaCaido_propagaServicioNoDisponibleSinGuardar() {
        when(repositorio.existsById("CPU-001")).thenReturn(false);
        doThrow(new ServicioNoDisponibleException("ms-categoria no está disponible."))
            .when(categorias).validarExiste("componentes");

        assertThrows(ServicioNoDisponibleException.class,
            () -> servicio.crear(request("CPU-001", "Ryzen", "componentes")));

        verify(repositorio, never()).save(any());
    }

    // -------------------------------------------------------------- actualizar

    @Test
    void actualizar_reemplazaCamposBasicos() {
        Producto existente = producto("CPU-001", "Viejo", "componentes", 5);
        when(repositorio.findById("CPU-001")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = new ProductoRequest(null, "  Nuevo Nombre ", "Nueva desc", new BigDecimal("500"),
            7, 1, " perifericos ", "  /img/x.png ", null, null, null, 20);

        Producto resultado = servicio.actualizar("CPU-001", r);

        assertEquals("CPU-001", resultado.getId());               // el id no cambia
        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals("nuevo nombre", resultado.getBusqueda());
        assertEquals("Nueva desc", resultado.getDescripcion());
        assertEquals(new BigDecimal("500"), resultado.getPrecio());
        assertEquals(7, resultado.getStock());
        assertEquals(1, resultado.getStockCritico());
        assertEquals("perifericos", resultado.getCategoria());
        assertEquals("/img/x.png", resultado.getImagen());
        assertEquals(20, resultado.getDescuentoPorcentaje());
        verify(categorias).validarExiste("perifericos");
    }

    @Test
    void actualizar_sinImagenesDestacadoNiEspecificaciones_noLosPisa() {
        Producto existente = producto();
        existente.setImagenes(List.of("a.png"));
        existente.setDestacado(Map.of("tag", "oferta"));
        existente.setEspecificaciones(Map.of("nucleos", 6));
        when(repositorio.findById("CPU-001")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = servicio.actualizar("CPU-001", request(null, "Ryzen", "componentes"));

        assertEquals(List.of("a.png"), resultado.getImagenes());
        assertEquals(Map.of("tag", "oferta"), resultado.getDestacado());
        assertEquals(Map.of("nucleos", 6), resultado.getEspecificaciones());
    }

    @Test
    void actualizar_conImagenesDestacadoYEspecificaciones_losReemplaza() {
        Producto existente = producto();
        existente.setImagenes(List.of("a.png"));
        when(repositorio.findById("CPU-001")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest r = new ProductoRequest(null, "Ryzen", null, new BigDecimal("1"), 1, null,
            "componentes", null, List.of("nueva.png"), Map.of("tag", "x"), Map.of("k", "v"), null);

        Producto resultado = servicio.actualizar("CPU-001", r);

        assertEquals(List.of("nueva.png"), resultado.getImagenes());
        assertEquals(Map.of("tag", "x"), resultado.getDestacado());
        assertEquals(Map.of("k", "v"), resultado.getEspecificaciones());
    }

    @Test
    void actualizar_productoInexistente_lanzaRecursoNoEncontradoSinValidarCategoria() {
        when(repositorio.findById("NO-EXISTE")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.actualizar("NO-EXISTE", request(null, "Ryzen", "componentes")));

        verifyNoInteractions(categorias);
        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_categoriaInexistente_noGuarda() {
        when(repositorio.findById("CPU-001")).thenReturn(Optional.of(producto()));
        doThrow(new ReglaNegocioException("no existe")).when(categorias).validarExiste("x");

        assertThrows(ReglaNegocioException.class,
            () -> servicio.actualizar("CPU-001", request(null, "Ryzen", "x")));

        verify(repositorio, never()).save(any());
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    void eliminar_productoExistente_loBorra() {
        when(repositorio.existsById("CPU-001")).thenReturn(true);

        servicio.eliminar("CPU-001");

        verify(repositorio).deleteById("CPU-001");
    }

    @Test
    void eliminar_productoInexistente_lanzaRecursoNoEncontradoSinBorrar() {
        when(repositorio.existsById("NO-EXISTE")).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar("NO-EXISTE"));

        verify(repositorio, never()).deleteById(any());
    }

    // ------------------------------------------------------------------ listar

    @Test
    @SuppressWarnings("unchecked")
    void listar_sinLimite_usaListadoSinPaginarConOrdenPorNombre() {
        List<Producto> esperado = List.of(producto());
        when(repositorio.findAll(any(Specification.class), any(Sort.class))).thenReturn(esperado);

        List<Producto> resultado = servicio.listar(null, null, false, null, null, null, null);

        assertSame(esperado, resultado);
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repositorio).findAll(any(Specification.class), sort.capture());
        Sort.Order orden = sort.getValue().getOrderFor("nombre");
        assertNotNull(orden);
        assertEquals(Sort.Direction.ASC, orden.getDirection());
        verify(repositorio, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @SuppressWarnings("unchecked")
    void listar_conLimiteCeroONegativo_noPagina(int limite) {
        when(repositorio.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        servicio.listar(null, null, false, null, null, null, limite);

        verify(repositorio, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "3,3", "200,200", "201,200", "5000,200"})
    @SuppressWarnings("unchecked")
    void listar_conLimite_paginaYAcotaAlMaximoDe200(int pedido, int esperado) {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(producto())));

        List<Producto> resultado = servicio.listar(null, null, false, null, null, null, pedido);

        assertEquals(1, resultado.size());
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), pagina.capture());
        assertEquals(0, pagina.getValue().getPageNumber());
        assertEquals(esperado, pagina.getValue().getPageSize());
    }

    @ParameterizedTest
    @CsvSource({
        "creado_asc,createdAt,ASC",
        "creado_desc,createdAt,DESC",
        "descuento_desc,descuentoPorcentaje,DESC",
        "nombre,nombre,ASC",
        "valor_desconocido,nombre,ASC"
    })
    @SuppressWarnings("unchecked")
    void listar_ordenSolicitado_seTraduceAlCampoYDireccionCorrectos(String orden, String campo, Sort.Direction direccion) {
        when(repositorio.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        servicio.listar(null, null, false, null, null, orden, null);

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repositorio).findAll(any(Specification.class), sort.capture());
        Sort.Order o = sort.getValue().getOrderFor(campo);
        assertNotNull(o, "debería ordenar por " + campo);
        assertEquals(direccion, o.getDirection());
    }
}
