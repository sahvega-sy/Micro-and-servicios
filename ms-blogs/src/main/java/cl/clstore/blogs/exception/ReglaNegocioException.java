package cl.clstore.blogs.exception;

// 400: la petición incumple una regla de negocio.
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
