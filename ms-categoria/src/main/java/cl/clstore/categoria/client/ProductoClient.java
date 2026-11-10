package cl.clstore.categoria.client;

import cl.clstore.categoria.exception.ServicioNoDisponibleException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductoClient {

    private final RestClient rest;

    public ProductoClient(@Value("${app.services.productos-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean existenProductosEnCategoria(String valor) {
        try {
            List<Map<String, Object>> productos = rest.get()
                .uri(uri -> uri.path("/api/productos").queryParam("categoria", valor).queryParam("limit", 1).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            return productos != null && !productos.isEmpty();
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                "No se pudo verificar si la categoría tiene productos: ms-productos no está disponible.");
        }
    }
}
