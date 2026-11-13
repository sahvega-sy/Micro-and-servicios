package cl.clstore.ordenes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
public class Orden {

    public static final String EXITOSA = "exitosa";
    public static final String FALLIDA = "fallida";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null en compras de invitado. Es el "sub" del JWT (usuario en ms-usuarios). */
    @Column(name = "usuario_id", length = 36)
    private String usuarioId;

    @Column(name = "nombre_comprador", nullable = false, length = 100)
    private String nombreComprador;

    @Column(name = "apellidos_comprador", nullable = false, length = 100)
    private String apellidosComprador;

    @Column(name = "correo_comprador", nullable = false, length = 150)
    private String correoComprador;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "comuna", nullable = false, length = 100)
    private String comuna;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenItem> items = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getNombreComprador() { return nombreComprador; }
    public void setNombreComprador(String nombreComprador) { this.nombreComprador = nombreComprador; }
    public String getApellidosComprador() { return apellidosComprador; }
    public void setApellidosComprador(String apellidosComprador) { this.apellidosComprador = apellidosComprador; }
    public String getCorreoComprador() { return correoComprador; }
    public void setCorreoComprador(String correoComprador) { this.correoComprador = correoComprador; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getCreadoEn() { return creadoEn; }
    public List<OrdenItem> getItems() { return items; }
}
