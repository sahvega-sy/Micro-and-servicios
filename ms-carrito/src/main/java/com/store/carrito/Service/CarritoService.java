package com.store.carrito.Service;
import com.store.carrito.Model.Carrito;
import com.store.carrito.Repository.CarritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
public class CarritoService {

    
    @Autowired
    private CarritoRepository repository;

    public CarritoService(CarritoRepository repository) {
        this.repository = repository;
    }

    public Carrito saveCarrito(Carrito carrito) {
        return repository.save(carrito);
    }

    public List<Carrito> getAllCarritos() {
        return repository.findAll();
    }

    public List<Carrito> getCarritoByUsuario(String usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public void deleteItem(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void clearUsuarioCarrito(String usuarioId) {
        repository.deleteByUsuarioId(usuarioId);
    }


}
