package cl.clstore.categoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @Column(name = "valor", length = 100)
    private String valor;

    @Column(name = "etiqueta", nullable = false, length = 100)
    private String etiqueta;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    protected Categoria() {
    }

    public Categoria(String valor, String etiqueta, String icono) {
        this.valor = valor;
        this.etiqueta = etiqueta;
        this.icono = icono;
    }

    public String getValor() { return valor; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
}
