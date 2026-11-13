package cl.clstore.usuarios.exception;

/** 401: credenciales inválidas o sesión que ya no es válida. */
public class NoAutorizadoException extends RuntimeException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
