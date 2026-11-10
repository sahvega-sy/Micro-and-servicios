package com.store.resenia.Service;
import com.store.resenia.Model.Resenia;
import com.store.resenia.Repository.ReseniaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;



@Service
public class ReseniaService {

    
    @Autowired
    private ReseniaRepository repository;

    public ReseniaService(ReseniaRepository repository) {
        this.repository = repository;
    }

    public Resenia saveResenia(Resenia resenia) {
        return repository.save(resenia);
    }

    public List<Resenia> getAllResenias() {
        return repository.findAll();
    }

    public List<Resenia> getReseniasByProducto(String productoId) {
        return repository.findByProductoId(productoId);
    }

    public void deleteResenia(Long id) {
        repository.deleteById(id);
    }


}
