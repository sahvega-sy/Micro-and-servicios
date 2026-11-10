package cl.clstore.blogs.model;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "blogs")
public class Blog {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "resumen", length = 1000)
    private String resumen;

    @Column(name = "imagen", length = 1000)
    private String imagen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contenido", nullable = false)
    private List<String> contenido;

    @Column(name = "fuente_nombre")
    private String fuenteNombre;

    @Column(name = "fuente_url", length = 1000)
    private String fuenteUrl;

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
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public List<String> getContenido() { return contenido; }
    public void setContenido(List<String> contenido) { this.contenido = contenido; }
    public String getFuenteNombre() { return fuenteNombre; }
    public void setFuenteNombre(String fuenteNombre) { this.fuenteNombre = fuenteNombre; }
    public String getFuenteUrl() { return fuenteUrl; }
    public void setFuenteUrl(String fuenteUrl) { this.fuenteUrl = fuenteUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
