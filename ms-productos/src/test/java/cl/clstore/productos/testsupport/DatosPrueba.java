package cl.clstore.productos.testsupport;

import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.model.Producto;
import java.math.BigDecimal;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    public static Producto producto(String id, String nombre, String categoria, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setDescripcion("Descripción de " + nombre);
        p.setPrecio(new BigDecimal("12990"));
        p.setStock(stock);
        p.setStockCritico(2);
        p.setCategoria(categoria);
        p.setImagen("/img/Logo.png");
        p.setBusqueda(nombre.toLowerCase());
        return p;
    }

    public static Producto producto() {
        return producto("CPU-001", "Procesador Ryzen 5", "componentes", 10);
    }

    /** Request válido mínimo; imagen, imagenes, destacado, especificaciones y descuento quedan en null. */
    public static ProductoRequest request(String id, String nombre, String categoria) {
        return new ProductoRequest(id, nombre, "Descripción", new BigDecimal("129990"),
            10, 3, categoria, null, null, null, null, null);
    }
}
