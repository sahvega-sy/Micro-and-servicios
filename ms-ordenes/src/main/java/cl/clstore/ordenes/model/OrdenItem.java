package cl.clstore.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "orden_items")
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    /** Copia de solo lectura de la FK, para leer el id de la orden sin cargarla. */
    @Column(name = "orden_id", insertable = false, updatable = false)
    private Long ordenId;

    @Column(name = "producto_id", nullable = false, length = 100)
    private String productoId;

    /** Nombre y precio "congelados" al comprar: la boleta no cambia aunque el producto cambie después. */
    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected OrdenItem() {
    }

    public OrdenItem(Orden orden, String productoId, String nombreProducto, int cantidad, BigDecimal precioUnitario) {
        this.orden = orden;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public String getProductoId() { return productoId; }
    public String getNombreProducto() { return nombreProducto; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
