package cl.clstore.categoria.testsupport;

import cl.clstore.categoria.dto.CategoriaRequest;
import cl.clstore.categoria.model.Categoria;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    public static Categoria categoria(String valor, String etiqueta, String icono) {
        return new Categoria(valor, etiqueta, icono);
    }

    public static Categoria categoria() {
        return categoria("componentes", "Componentes", "fa-microchip");
    }

    public static CategoriaRequest request(String etiqueta, String icono) {
        return new CategoriaRequest(etiqueta, icono);
    }
}
