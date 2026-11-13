package cl.clstore.resenas.client;

import cl.clstore.resenas.exception.ReglaNegocioException;
import cl.clstore.resenas.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Valida contra ms-productos (lectura pública) que el producto reseñado exista. */
@Component
public class ProductoClient {

    private final RestClient rest;

    public ProductoClient(@Value("${app.services.productos-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void validarExiste(String productoId) {
        try {
            rest.get().uri("/api/productos/{id}", productoId).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ReglaNegocioException("El producto '" + productoId + "' no existe.");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo validar el producto: ms-productos no está disponible.");
        }
    }
}
