package cl.clstore.categoria.exception;

/** 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
