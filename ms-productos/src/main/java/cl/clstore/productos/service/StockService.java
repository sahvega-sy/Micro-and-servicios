package cl.clstore.productos.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.clstore.productos.dto.StockRequest;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.repository.ProductoRepository;

@Service
public class StockService {

    private final ProductoRepository repositorio;

    public StockService(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void descontar(List<StockRequest.Item> items) {
        for (Map.Entry<String, Integer> e : agrupar(items).entrySet()) {
            if (repositorio.descontarStock(e.getKey(), e.getValue()) == 0) {
                var producto = repositorio.findById(e.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto '" + e.getKey() + "'."));
                throw new ConflictoException("Stock insuficiente para '" + producto.getNombre() + "': disponible "
                    + producto.getStock() + ", solicitado " + e.getValue() + ".");
            }
        }
    }

    /** Si un producto ya no existe se ignora: no hay stock que reponer. */
    @Transactional
    public void reponer(List<StockRequest.Item> items) {
        agrupar(items).forEach(repositorio::reponerStock);
    }

    /** Suma cantidades repetidas y ordena por id (orden fijo = menos riesgo de interbloqueos). */
    private Map<String, Integer> agrupar(List<StockRequest.Item> items) {
        Map<String, Integer> agrupado = new TreeMap<>();
        items.forEach(i -> agrupado.merge(i.id().trim(), i.cantidad(), Integer::sum));
        return agrupado;
    }
}
