package cl.clstore.ordenes.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.clstore.ordenes.dto.ProductoResumen;
import cl.clstore.ordenes.exception.ServicioNoDisponibleException;
import cl.clstore.ordenes.exception.StockInsuficienteException;
import cl.clstore.ordenes.service.TokenServicio;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Llamadas a ms-productos: lectura pública y, para el stock, endpoints internos con JWT de servicio. */
@Component
public class ProductoClient {

    private static final Logger log = LoggerFactory.getLogger(ProductoClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record ItemStock(String id, int cantidad) {
    }

    private final RestClient rest;
    private final TokenServicio tokens;

    public ProductoClient(@Value("${app.services.productos-url}") String baseUrl, TokenServicio tokens) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.tokens = tokens;
    }

    public List<ProductoResumen> obtenerPorIds(Collection<String> ids) {
        try {
            List<ProductoResumen> productos = rest.get()
                .uri(uri -> uri.path("/api/productos").queryParam("ids", ids).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductoResumen>>() { });
            return productos == null ? List.of() : productos;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo verificar el stock: ms-productos no está disponible.");
        }
    }

    /** Todo o nada. @throws StockInsuficienteException con el mensaje de ms-productos si algún producto no alcanza. */
    public void descontarStock(List<ItemStock> items) {
        try {
            rest.post().uri("/api/productos/stock/descontar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.emitir())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", items))
                .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.NotFound e) {
            throw new StockInsuficienteException(detalle(e));
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo reservar el stock: ms-productos no está disponible.");
        }
    }

    /** Compensación: si algo falla después de descontar, se devuelve el stock. Mejor esfuerzo (se registra el fallo). */
    public void reponerStock(List<ItemStock> items) {
        try {
            rest.post().uri("/api/productos/stock/reponer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.emitir())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", items))
                .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.error("No se pudo reponer el stock {} tras un fallo; requiere corrección manual", items, e);
        }
    }

    private String detalle(RestClientResponseException e) {
        try {
            String detalle = MAPPER.readTree(e.getResponseBodyAsString()).path("detail").asText("");
            if (!detalle.isBlank()) {
                return detalle;
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON: se usa el mensaje genérico
        }
        return "No hay stock suficiente para completar la compra.";
    }
}
