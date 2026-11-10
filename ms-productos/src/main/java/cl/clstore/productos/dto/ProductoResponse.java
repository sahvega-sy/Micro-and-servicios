package cl.clstore.productos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import cl.clstore.productos.model.Producto;

public record ProductoResponse(
    String id,
    String nombre,
    String descripcion,
    BigDecimal precio,
    Integer stock,
    @JsonProperty("stock_critico") Integer stockCritico,
    String categoria,
    String imagen,
    List<String> imagenes,
    String busqueda,
    Map<String, Object> destacado,
    Map<String, Object> especificaciones,
    @JsonProperty("descuento_porcentaje") Integer descuentoPorcentaje,
    @JsonProperty("created_at") Instant createdAt
) {
    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(
            p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getStock(),
            p.getStockCritico(), p.getCategoria(), p.getImagen(), p.getImagenes(), p.getBusqueda(),
            p.getDestacado(), p.getEspecificaciones(), p.getDescuentoPorcentaje(), p.getCreatedAt());
    }
}
