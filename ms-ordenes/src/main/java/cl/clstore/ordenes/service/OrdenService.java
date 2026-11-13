package cl.clstore.ordenes.service;

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
import cl.clstore.ordenes.exception.StockInsuficienteException;
import cl.clstore.ordenes.model.Orden;
import cl.clstore.ordenes.model.OrdenItem;
import cl.clstore.ordenes.repository.OrdenItemRepository;
import cl.clstore.ordenes.repository.OrdenRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenService {

    private final OrdenRepository ordenes;
    private final OrdenItemRepository items;
    private final ProductoClient productos;

    public OrdenService(OrdenRepository ordenes, OrdenItemRepository items, ProductoClient productos) {
        this.ordenes = ordenes;
        this.items = items;
        this.productos = productos;
    }

    /**
     * Crea la orden. Los precios y el stock se toman de ms-productos, nunca del navegador.
     * 1) lee los productos  2) descuenta el stock (todo o nada)  3) guarda la orden.
     * Si el paso 3 falla se devuelve el stock. Si algo del negocio falla (producto retirado o sin stock) la
     * orden se guarda como "fallida" y se lanza OrdenRechazadaException (409) con su id.
     *
     * No es @Transactional a propósito: hay llamadas HTTP de por medio y no conviene mantener una conexión abierta.
     */
    public CrearOrdenResponse crear(OrdenRequest r, String usuarioId) {
        Map<String, Integer> pedidos = new LinkedHashMap<>();
        r.items().forEach(i -> pedidos.merge(i.productoId().trim(), i.cantidad(), Integer::sum));

        Map<String, ProductoResumen> catalogo = productos.obtenerPorIds(pedidos.keySet()).stream()
            .collect(Collectors.toMap(ProductoResumen::id, Function.identity(), (a, b) -> a));

        Orden orden = cabecera(r, usuarioId);
        List<OrdenItem> lineas = new ArrayList<>();
        List<String> noDisponibles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> pedido : pedidos.entrySet()) {
            ProductoResumen p = catalogo.get(pedido.getKey());
            if (p == null) {
                noDisponibles.add(pedido.getKey());
                continue;
            }
            BigDecimal precio = precioFinal(p);
            total = total.add(precio.multiply(BigDecimal.valueOf(pedido.getValue())));
            lineas.add(new OrdenItem(orden, p.id(), p.nombre(), pedido.getValue(), precio));
        }
        orden.setTotal(total);

        if (!noDisponibles.isEmpty()) {
            throw rechazar(orden, "Hay productos que ya no están disponibles: " + String.join(", ", noDisponibles) + ".");
        }

        List<ItemStock> aDescontar = pedidos.entrySet().stream().map(e -> new ItemStock(e.getKey(), e.getValue())).toList();
        try {
            productos.descontarStock(aDescontar);
        } catch (StockInsuficienteException e) {
            throw rechazar(orden, e.getMessage());
        }

        orden.setEstado(Orden.EXITOSA);
        orden.getItems().addAll(lineas);
        try {
            Orden guardada = ordenes.save(orden);
            return new CrearOrdenResponse(guardada.getId(), guardada.getTotal(), guardada.getEstado());
        } catch (RuntimeException e) {
            productos.reponerStock(aDescontar); // compensación
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(String estado) {
        if (estado != null && !estado.isBlank() && !Orden.EXITOSA.equals(estado) && !Orden.FALLIDA.equals(estado)) {
            throw new ReglaNegocioException("El estado debe ser 'exitosa' o 'fallida'.");
        }
        List<Orden> lista = (estado == null || estado.isBlank())
            ? ordenes.findAllByOrderByCreadoEnDesc()
            : ordenes.findByEstadoOrderByCreadoEnDesc(estado);
        return lista.stream().map(o -> OrdenResponse.desde(o, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listarDeUsuario(String usuarioId) {
        return ordenes.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream().map(o -> OrdenResponse.desde(o, false)).toList();
    }

    /** Todas las líneas de todas las órdenes (para los reportes). */
    @Transactional(readOnly = true)
    public List<ItemOrdenResponse> todosLosItems() {
        return items.findAll().stream().map(ItemOrdenResponse::desde).toList();
    }

    /** Detalle con boleta. Lo puede ver su dueño o el personal (Administrador / Vendedor). */
    @Transactional(readOnly = true)
    public OrdenResponse obtener(Long id, String usuarioId, boolean esPersonal) {
        Orden o = ordenes.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden #" + id + "."));
        boolean esDuenio = o.getUsuarioId() != null && o.getUsuarioId().equals(usuarioId);
        if (!esPersonal && !esDuenio) {
            throw new ProhibidoException("No tienes acceso a esta orden.");
        }
        return OrdenResponse.desde(o, true);
    }

    // ------------------------------------------------------------------ internos

    private OrdenRechazadaException rechazar(Orden orden, String motivo) {
        orden.setEstado(Orden.FALLIDA);
        Orden guardada = ordenes.save(orden); // sin líneas: igual que registrar_orden_fallida
        return new OrdenRechazadaException(motivo, guardada.getId());
    }

    private Orden cabecera(OrdenRequest r, String usuarioId) {
        Orden o = new Orden();
        o.setUsuarioId(usuarioId);
        o.setNombreComprador(r.nombre().trim());
        o.setApellidosComprador(r.apellidos().trim());
        o.setCorreoComprador(r.correo().trim());
        o.setTelefono(r.telefono() == null || r.telefono().isBlank() ? null : r.telefono().trim());
        o.setRegion(r.region().trim());
        o.setComuna(r.comuna().trim());
        o.setDireccion(r.direccion().trim());
        return o;
    }

    /** Misma regla que calcularPrecioFinal() del front: con descuento, redondeo a peso entero. */
    static BigDecimal precioFinal(ProductoResumen p) {
        BigDecimal precio = p.precio() == null ? BigDecimal.ZERO : p.precio();
        int descuento = p.descuentoPorcentaje() == null ? 0 : p.descuentoPorcentaje();
        if (descuento <= 0) {
            return precio;
        }
        return precio.multiply(BigDecimal.valueOf(100L - descuento)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
}
