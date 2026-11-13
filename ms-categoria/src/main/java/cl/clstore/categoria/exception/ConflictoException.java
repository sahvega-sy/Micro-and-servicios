package cl.clstore.categoria.exception;

/** 409: el recurso ya existe o no se puede modificar/eliminar. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
