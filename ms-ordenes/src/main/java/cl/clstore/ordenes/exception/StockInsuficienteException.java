package cl.clstore.ordenes.exception;

/** Interna: ms-productos informó que no hay stock suficiente. */
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
