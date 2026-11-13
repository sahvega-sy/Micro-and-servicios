package cl.clstore.ordenes.exception;

/** 403: el usuario no tiene acceso a este recurso. */
public class ProhibidoException extends RuntimeException {
    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
