package cl.clstore.productos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "stock_critico")
    private Integer stockCritico;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "imagen", length = 1000)
    private String imagen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "imagenes")
    private List<String> imagenes;

    @Column(name = "busqueda")
    private String busqueda;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "destacado")
    private Map<String, Object> destacado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "especificaciones")
    private Map<String, Object> especificaciones;

    @Column(name = "descuento_porcentaje")
    private Integer descuentoPorcentaje;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void antesDeInsertar() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStockCritico() { return stockCritico; }
    public void setStockCritico(Integer stockCritico) { this.stockCritico = stockCritico; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public List<String> getImagenes() { return imagenes; }
    public void setImagenes(List<String> imagenes) { this.imagenes = imagenes; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public Map<String, Object> getDestacado() { return destacado; }
    public void setDestacado(Map<String, Object> destacado) { this.destacado = destacado; }
    public Map<String, Object> getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(Map<String, Object> especificaciones) { this.especificaciones = especificaciones; }
    public Integer getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(Integer descuentoPorcentaje) { this.descuentoPorcentaje = descuentoPorcentaje; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
