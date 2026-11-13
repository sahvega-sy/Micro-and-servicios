package cl.clstore.resenas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false, length = 100)
    private String productoId;

    /** Nombre que se muestra (sale del JWT, no del cuerpo de la petición). */
    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "calificacion", nullable = false)
    private int calificacion;

    @Column(name = "comentario", nullable = false, length = 1000)
    private String comentario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @PrePersist
    void antesDeInsertar() {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
    }

    public Long getId() { return id; }
    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDate getFecha() { return fecha; }
}
