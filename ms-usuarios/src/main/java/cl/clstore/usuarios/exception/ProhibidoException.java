package cl.clstore.usuarios.exception;

/** 403: el usuario autenticado no tiene permiso para esta acción. */
public class ProhibidoException extends RuntimeException {
    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
