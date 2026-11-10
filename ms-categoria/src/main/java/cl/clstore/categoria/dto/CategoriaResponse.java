package cl.clstore.categoria.dto;

import cl.clstore.categoria.model.Categoria;

public record CategoriaResponse(String valor, String etiqueta, String icono) {

    public static CategoriaResponse desde(Categoria c) {
        return new CategoriaResponse(c.getValor(), c.getEtiqueta(), c.getIcono());
    }
}
