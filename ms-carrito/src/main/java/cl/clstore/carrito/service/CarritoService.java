package cl.clstore.carrito.service;

import cl.clstore.carrito.client.ProductoClient;
import cl.clstore.carrito.dto.ItemCarritoResponse;
import cl.clstore.carrito.dto.ProductoResumen;
import cl.clstore.carrito.exception.RecursoNoEncontradoException;
import cl.clstore.carrito.exception.ReglaNegocioException;
import cl.clstore.carrito.model.CarritoItem;
import cl.clstore.carrito.repository.CarritoItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrito del usuario autenticado. Precio, nombre, imagen y stock se leen de ms-productos en cada consulta. */
@Service
public class CarritoService {

    private static final int MAX_POR_PRODUCTO = 99;

    private final CarritoItemRepository repositorio;
    private final ProductoClient productos;

    public CarritoService(CarritoItemRepository repositorio, ProductoClient productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    public List<ItemCarritoResponse> listar(String usuarioId) {
        List<CarritoItem> items = repositorio.findByUsuarioIdOrderByIdAsc(usuarioId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<String, ProductoResumen> catalogo = productos
            .obtenerPorIds(items.stream().map(CarritoItem::getProductoId).toList())
            .stream().collect(Collectors.toMap(ProductoResumen::id, Function.identity(), (a, b) -> a));

        List<ItemCarritoResponse> respuesta = new ArrayList<>();
        List<CarritoItem> huerfanos = new ArrayList<>();
        for (CarritoItem item : items) {
            ProductoResumen producto = catalogo.get(item.getProductoId());
            if (producto == null) {
                huerfanos.add(item); // el producto se eliminó del catálogo: sale del carrito
            } else {
                respuesta.add(new ItemCarritoResponse(item.getId(), item.getProductoId(), item.getCantidad(), producto));
            }
        }
        if (!huerfanos.isEmpty()) {
            repositorio.deleteAll(huerfanos);
        }
        return respuesta;
    }

    public List<ItemCarritoResponse> agregar(String usuarioId, String productoId, Integer cantidad) {
        String id = productoId.trim();
        if (!productos.existe(id)) {
            throw new ReglaNegocioException("El producto '" + id + "' no existe.");
        }
        int suma = cantidad == null ? 1 : cantidad;
        CarritoItem item = repositorio.findByUsuarioIdAndProductoId(usuarioId, id).orElseGet(() -> {
            CarritoItem nuevo = new CarritoItem();
            nuevo.setUsuarioId(usuarioId);
            nuevo.setProductoId(id);
            return nuevo;
        });
        item.setCantidad(Math.min(MAX_POR_PRODUCTO, item.getCantidad() + suma));
        repositorio.save(item);
        return listar(usuarioId);
    }

    public List<ItemCarritoResponse> cambiarCantidad(String usuarioId, String productoId, int cantidad) {
        CarritoItem item = repositorio.findByUsuarioIdAndProductoId(usuarioId, productoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto '" + productoId + "' no está en tu carrito."));
        item.setCantidad(cantidad);
        repositorio.save(item);
        return listar(usuarioId);
    }

    @Transactional
    public List<ItemCarritoResponse> eliminar(String usuarioId, String productoId) {
        repositorio.deleteByUsuarioIdAndProductoId(usuarioId, productoId);
        return listar(usuarioId);
    }

    @Transactional
    public void vaciar(String usuarioId) {
        repositorio.deleteByUsuarioId(usuarioId);
    }
}
