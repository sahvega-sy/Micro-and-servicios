package cl.clstore.ordenes.service;

import static cl.clstore.ordenes.testsupport.DatosPrueba.USUARIO;
import static cl.clstore.ordenes.testsupport.DatosPrueba.linea;
import static cl.clstore.ordenes.testsupport.DatosPrueba.orden;
import static cl.clstore.ordenes.testsupport.DatosPrueba.producto;
import static cl.clstore.ordenes.testsupport.DatosPrueba.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.ordenes.client.ProductoClient;
import cl.clstore.ordenes.client.ProductoClient.ItemStock;
import cl.clstore.ordenes.dto.CrearOrdenResponse;
import cl.clstore.ordenes.dto.ItemOrdenResponse;
import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.dto.OrdenResponse;
import cl.clstore.ordenes.dto.ProductoResumen;
import cl.clstore.ordenes.exception.OrdenRechazadaException;
import cl.clstore.ordenes.exception.ProhibidoException;
import cl.clstore.ordenes.exception.RecursoNoEncontradoException;
import cl.clstore.ordenes.exception.ReglaNegocioException;
import cl.clstore.ordenes.exception.ServicioNoDisponibleException;
import cl.clstore.ordenes.exception.StockInsuficienteException;
import cl.clstore.ordenes.model.Orden;
import cl.clstore.ordenes.model.OrdenItem;
import cl.clstore.ordenes.repository.OrdenItemRepository;
import cl.clstore.ordenes.repository.OrdenRepository;
import java.math.BigDecimal;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenes;

    @Mock
    private OrdenItemRepository items;

    @Mock
    private ProductoClient productos;

    @InjectMocks
    private OrdenService servicio;

    private final ProductoResumen cpu = producto("CPU-001", "Procesador Ryzen 5", "129990", 15, 10);
    private final ProductoResumen ram = producto("RAM-002", "Memoria 16GB", "59990", null, 4);

    /** Simula el INSERT: devuelve la misma orden con el id que asignaría la base de datos. */
    private void guardarAsignandoId(long id) {
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", id);
            return o;
        });
    }

    private Orden ordenGuardada() {
        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenes).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------ crear: éxito

    @Test
    void crear_compraValida_calculaElTotalConDescuentosYGuardaLaOrdenExitosa() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(cpu, ram));
        guardarAsignandoId(100L);

        CrearOrdenResponse respuesta = servicio.crear(request(linea("CPU-001", 2), linea("RAM-002", 1)), USUARIO);

        // CPU: 129990 con 15% = 110492 (redondeo a peso entero) x 2 = 220984; RAM: 59990 x 1.
        assertEquals(100L, respuesta.ordenId());
        assertThat(respuesta.total()).isEqualByComparingTo("280974");
        assertEquals("exitosa", respuesta.estado());

        Orden guardada = ordenGuardada();
        assertEquals(Orden.EXITOSA, guardada.getEstado());
        assertEquals(USUARIO, guardada.getUsuarioId());
        assertEquals(2, guardada.getItems().size());
        OrdenItem lineaCpu = guardada.getItems().get(0);
        assertEquals("CPU-001", lineaCpu.getProductoId());
        assertEquals("Procesador Ryzen 5", lineaCpu.getNombreProducto());   // nombre "congelado" en la boleta
        assertEquals(2, lineaCpu.getCantidad());
        assertThat(lineaCpu.getPrecioUnitario()).isEqualByComparingTo("110492");
    }

    @Test
    void crear_descuentaElStockDeTodosLosProductosEnUnaSolaLlamada() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(cpu, ram));
        guardarAsignandoId(100L);

        servicio.crear(request(linea("CPU-001", 2), linea("RAM-002", 1)), USUARIO);

        verify(productos).descontarStock(List.of(new ItemStock("CPU-001", 2), new ItemStock("RAM-002", 1)));
        verify(productos, never()).reponerStock(anyList());
    }

    @Test
    void crear_productosRepetidosOConEspacios_seAgrupanSumandoLasCantidades() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(ram));
        guardarAsignandoId(100L);

        CrearOrdenResponse respuesta = servicio.crear(
            request(linea("RAM-002", 1), linea("  RAM-002 ", 2)), USUARIO);

        verify(productos).descontarStock(List.of(new ItemStock("RAM-002", 3)));
        assertThat(respuesta.total()).isEqualByComparingTo("179970");       // 59990 x 3
        assertEquals(1, ordenGuardada().getItems().size());
    }

    @Test
    void crear_elPrecioSeTomaDeMsProductosNuncaDelCliente() {
        // El request ni siquiera tiene campo de precio: el total sale solo del catálogo.
        when(productos.obtenerPorIds(any())).thenReturn(List.of(producto("RAM-002", "Memoria", "1000", null, 5)));
        guardarAsignandoId(1L);

        assertThat(servicio.crear(request(linea("RAM-002", 3)), USUARIO).total()).isEqualByComparingTo("3000");
    }

    @Test
    void crear_compraDeInvitado_guardaUsuarioNull() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(ram));
        guardarAsignandoId(7L);

        servicio.crear(request(linea("RAM-002", 1)), null);

        assertNull(ordenGuardada().getUsuarioId());
    }

    @Test
    void crear_datosDelCompradorSeGuardanSinEspaciosSobrantes() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(ram));
        guardarAsignandoId(7L);
        OrdenRequest r = new OrdenRequest("  Ana ", " Pérez  ", " ana@gmail.com ", "  +56911111111 ",
            " Valparaíso ", " Viña del Mar ", "  Av. Siempre Viva 742 ", List.of(linea("RAM-002", 1)));

        servicio.crear(r, USUARIO);

        Orden o = ordenGuardada();
        assertEquals("Ana", o.getNombreComprador());
        assertEquals("Pérez", o.getApellidosComprador());
        assertEquals("ana@gmail.com", o.getCorreoComprador());
        assertEquals("+56911111111", o.getTelefono());
        assertEquals("Valparaíso", o.getRegion());
        assertEquals("Viña del Mar", o.getComuna());
        assertEquals("Av. Siempre Viva 742", o.getDireccion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void crear_telefonoEnBlanco_seGuardaComoNull(String telefono) {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(ram));
        guardarAsignandoId(7L);
        OrdenRequest r = new OrdenRequest("Ana", "Pérez", "ana@gmail.com", telefono, "R", "C", "D",
            List.of(linea("RAM-002", 1)));

        servicio.crear(r, USUARIO);

        assertNull(ordenGuardada().getTelefono());
    }

    // --------------------------------------------------------- crear: rechazos

    @Test
    void crear_productoYaNoDisponible_guardaOrdenFallidaYLanzaOrdenRechazada() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of());
        guardarAsignandoId(55L);

        OrdenRechazadaException ex = assertThrows(OrdenRechazadaException.class,
            () -> servicio.crear(request(linea("CPU-001", 1)), USUARIO));

        assertEquals("Hay productos que ya no están disponibles: CPU-001.", ex.getMessage());
        assertEquals(55L, ex.getOrdenId());
        Orden guardada = ordenGuardada();
        assertEquals(Orden.FALLIDA, guardada.getEstado());
        assertTrue(guardada.getItems().isEmpty());                          // la orden fallida no lleva líneas
        verify(productos, never()).descontarStock(anyList());
    }

    @Test
    void crear_variosProductosNoDisponibles_losListaTodosEnElMensaje() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(ram));
        guardarAsignandoId(56L);

        OrdenRechazadaException ex = assertThrows(OrdenRechazadaException.class,
            () -> servicio.crear(request(linea("CPU-001", 1), linea("RAM-002", 1), linea("GPU-009", 1)), USUARIO));

        assertEquals("Hay productos que ya no están disponibles: CPU-001, GPU-009.", ex.getMessage());
    }

    @Test
    void crear_stockInsuficiente_guardaOrdenFallidaConElMensajeDeMsProductos() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(cpu));
        doThrow(new StockInsuficienteException("Stock insuficiente para 'Procesador Ryzen 5': disponible 1, solicitado 5."))
            .when(productos).descontarStock(anyList());
        guardarAsignandoId(57L);

        OrdenRechazadaException ex = assertThrows(OrdenRechazadaException.class,
            () -> servicio.crear(request(linea("CPU-001", 5)), USUARIO));

        assertEquals("Stock insuficiente para 'Procesador Ryzen 5': disponible 1, solicitado 5.", ex.getMessage());
        assertEquals(57L, ex.getOrdenId());
        assertEquals(Orden.FALLIDA, ordenGuardada().getEstado());
        verify(productos, never()).reponerStock(anyList());                 // no se descontó nada: no hay que reponer
    }

    // ------------------------------------------------------ crear: fallos técnicos

    @Test
    void crear_msProductosCaidoAlLeerElCatalogo_propagaYNoGuardaNada() {
        when(productos.obtenerPorIds(any()))
            .thenThrow(new ServicioNoDisponibleException("No se pudo verificar el stock: ms-productos no está disponible."));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.crear(request(), USUARIO));

        verifyNoInteractions(ordenes);
    }

    @Test
    void crear_msProductosCaidoAlDescontar_propagaSinGuardarNiReponer() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(cpu));
        doThrow(new ServicioNoDisponibleException("No se pudo reservar el stock: ms-productos no está disponible."))
            .when(productos).descontarStock(anyList());

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.crear(request(), USUARIO));

        verify(ordenes, never()).save(any());
        verify(productos, never()).reponerStock(anyList());
    }

    @Test
    void crear_fallaElGuardadoDespuesDeDescontar_reponeElStockYRelanzaElError() {
        when(productos.obtenerPorIds(any())).thenReturn(List.of(cpu));
        when(ordenes.save(any(Orden.class))).thenThrow(new IllegalStateException("BD caída"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> servicio.crear(request(linea("CPU-001", 2)), USUARIO));

        assertEquals("BD caída", ex.getMessage());
        verify(productos).descontarStock(List.of(new ItemStock("CPU-001", 2)));
        verify(productos).reponerStock(List.of(new ItemStock("CPU-001", 2)));   // compensación
    }

    // --------------------------------------------------------------- precioFinal

    @ParameterizedTest
    @CsvSource({
        "10000,10,9000",
        "129990,15,110492",     // 110491,5 -> redondea hacia arriba
        "12990,50,6495",
        "999,95,50",            // 49,95 -> 50
        "10000,0,10000",
        "10000,-5,10000"})
    void precioFinal_aplicaElDescuentoYRedondeaAPesoEntero(String precio, int descuento, String esperado) {
        ProductoResumen p = producto("X", "X", precio, descuento, 1);

        assertThat(OrdenService.precioFinal(p)).isEqualByComparingTo(esperado);
    }

    @Test
    void precioFinal_sinDescuento_devuelveElPrecioTalCual() {
        assertThat(OrdenService.precioFinal(producto("X", "X", "5990", null, 1))).isEqualByComparingTo("5990");
    }

    @Test
    void precioFinal_precioNull_cuentaComoCero() {
        ProductoResumen sinPrecio = new ProductoResumen("X", "X", null, null, 20, 1);

        assertThat(OrdenService.precioFinal(sinPrecio)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------- listar

    @Test
    void listar_sinEstado_devuelveTodasMasRecientesPrimeroYSinItems() {
        when(ordenes.findAllByOrderByCreadoEnDesc()).thenReturn(List.of(orden(), orden(4L, null, Orden.FALLIDA, "0", null)));

        List<OrdenResponse> resultado = servicio.listar(null);

        assertEquals(2, resultado.size());
        assertEquals(5L, resultado.get(0).id());
        assertNull(resultado.get(0).items());                              // el listado no incluye las líneas
        assertEquals("fallida", resultado.get(1).estado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void listar_estadoEnBlanco_seTrataComoSinFiltro(String estado) {
        when(ordenes.findAllByOrderByCreadoEnDesc()).thenReturn(List.of());

        assertEquals(List.of(), servicio.listar(estado));

        verify(ordenes, never()).findByEstadoOrderByCreadoEnDesc(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"exitosa", "fallida"})
    void listar_conEstadoValido_filtraPorEseEstado(String estado) {
        when(ordenes.findByEstadoOrderByCreadoEnDesc(estado)).thenReturn(List.of(orden(1L, USUARIO, estado, "100", null)));

        List<OrdenResponse> resultado = servicio.listar(estado);

        assertEquals(1, resultado.size());
        assertEquals(estado, resultado.get(0).estado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pendiente", "EXITOSA", "x"})
    void listar_estadoDesconocido_lanzaReglaNegocio(String estado) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.listar(estado));

        assertEquals("El estado debe ser 'exitosa' o 'fallida'.", ex.getMessage());
        verifyNoInteractions(ordenes);
    }

    @Test
    void listarDeUsuario_devuelveSoloLasOrdenesDeEseUsuario() {
        when(ordenes.findByUsuarioIdOrderByCreadoEnDesc(USUARIO)).thenReturn(List.of(orden()));

        List<OrdenResponse> resultado = servicio.listarDeUsuario(USUARIO);

        assertEquals(1, resultado.size());
        assertEquals(USUARIO, resultado.get(0).usuarioId());
        assertNull(resultado.get(0).items());
    }

    @Test
    void todosLosItems_conviertenCadaLineaAlDto() {
        OrdenItem linea = new OrdenItem(orden(), "CPU-001", "Procesador Ryzen 5", 2, new BigDecimal("110492"));
        when(items.findAll()).thenReturn(List.of(linea));

        List<ItemOrdenResponse> resultado = servicio.todosLosItems();

        assertEquals(1, resultado.size());
        assertEquals("CPU-001", resultado.get(0).productoId());
        assertEquals("Procesador Ryzen 5", resultado.get(0).nombreProducto());
        assertEquals(2, resultado.get(0).cantidad());
        assertThat(resultado.get(0).precioUnitario()).isEqualByComparingTo("110492");
    }

    // ------------------------------------------------------------------- obtener

    @Test
    void obtener_ordenInexistente_lanzaRecursoNoEncontrado() {
        when(ordenes.findById(9L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.obtener(9L, USUARIO, true));

        assertEquals("No existe la orden #9.", ex.getMessage());
    }

    @Test
    void obtener_suDuenio_veElDetalleConItems() {
        Orden o = orden();
        o.getItems().add(new OrdenItem(o, "CPU-001", "Procesador Ryzen 5", 2, new BigDecimal("110492")));
        when(ordenes.findById(5L)).thenReturn(Optional.of(o));

        OrdenResponse resultado = servicio.obtener(5L, USUARIO, false);

        assertEquals(5L, resultado.id());
        assertEquals(1, resultado.items().size());
        assertEquals("CPU-001", resultado.items().get(0).productoId());
    }

    @Test
    void obtener_personalDelNegocio_veOrdenesAjenas() {
        when(ordenes.findById(5L)).thenReturn(Optional.of(orden()));

        assertEquals(5L, servicio.obtener(5L, "otro-usuario", true).id());
    }

    @Test
    void obtener_otroUsuarioSinSerPersonal_lanzaProhibido() {
        when(ordenes.findById(5L)).thenReturn(Optional.of(orden()));

        ProhibidoException ex = assertThrows(ProhibidoException.class,
            () -> servicio.obtener(5L, "otro-usuario", false));

        assertEquals("No tienes acceso a esta orden.", ex.getMessage());
    }

    @Test
    void obtener_ordenDeInvitado_soloLaVePersonal() {
        when(ordenes.findById(8L)).thenReturn(Optional.of(orden(8L, null, Orden.EXITOSA, "100", null)));

        assertThrows(ProhibidoException.class, () -> servicio.obtener(8L, USUARIO, false));
        assertEquals(8L, servicio.obtener(8L, USUARIO, true).id());
    }
}
