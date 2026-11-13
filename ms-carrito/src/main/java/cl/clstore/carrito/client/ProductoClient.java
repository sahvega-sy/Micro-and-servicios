package cl.clstore.carrito.client;

import cl.clstore.carrito.dto.ProductoResumen;
import cl.clstore.carrito.exception.ServicioNoDisponibleException;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Lecturas públicas a ms-productos. */
@Component
public class ProductoClient {

    private final RestClient rest;

    public ProductoClient(@Value("${app.services.productos-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public List<ProductoResumen> obtenerPorIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            List<ProductoResumen> productos = rest.get()
                .uri(uri -> uri.path("/api/productos").queryParam("ids", ids).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductoResumen>>() { });
            return productos == null ? List.of() : productos;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("ms-productos no está disponible.");
        }
    }

    public boolean existe(String productoId) {
        try {
            rest.get().uri("/api/productos/{id}", productoId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("ms-productos no está disponible.");
        }
    }
}
