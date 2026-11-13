package cl.clstore.productos.exception;

/** 503: otro microservicio del que depende esta operación no responde. */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
