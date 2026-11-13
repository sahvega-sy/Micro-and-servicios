package cl.clstore.ordenes.exception;

/** La compra no se pudo completar (stock insuficiente, producto retirado...). La orden queda registrada como fallida. */
public class OrdenRechazadaException extends RuntimeException {

    private final Long ordenId;

    public OrdenRechazadaException(String mensaje, Long ordenId) {
        super(mensaje);
        this.ordenId = ordenId;
    }

    public Long getOrdenId() {
        return ordenId;
    }
}
