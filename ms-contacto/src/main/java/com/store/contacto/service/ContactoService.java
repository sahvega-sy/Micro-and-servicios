package com.store.contacto.service;
import com.store.contacto.model.DTO.ContactoRequestDTO;
import com.store.contacto.model.DTO.ContactoResponseDTO;
import com.store.contacto.model.Contacto;
import com.store.contacto.repository.ContactoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactoService {

    private final ContactoRepository contactoRepository;

    @Autowired
    public ContactoService(ContactoRepository contactoRepository) {
        this.contactoRepository = contactoRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactoResponseDTO> obtenerTodos() {
        return contactoRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContactoResponseDTO obtenerPorId(Long id) {
        Contacto entity = contactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje de contacto no encontrado con ID: " + id));
        return mapToDTO(entity);
    }

    @Transactional
    public ContactoResponseDTO crear(ContactoRequestDTO requestDTO) {
        Contacto entity = mapToEntity(requestDTO);
        Contacto guardado = contactoRepository.save(entity);
        return mapToDTO(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!contactoRepository.existsById(id)) {
            throw new RuntimeException("Mensaje de contacto no encontrado con ID: " + id);
        }
        contactoRepository.deleteById(id);
    }

    private ContactoResponseDTO mapToDTO(Contacto entity) {
        ContactoResponseDTO dto = new ContactoResponseDTO();
        dto.setId(entity.getId());
        dto.setNombre(entity.getNombre());
        dto.setEmail(entity.getEmail());
        dto.setMensaje(entity.getMensaje());
        dto.setCreadoEn(entity.getCreadoEn());
        return dto;
    }

    private Contacto mapToEntity(ContactoRequestDTO dto) {
        Contacto entity = new Contacto();
        entity.setNombre(dto.getNombre());
        entity.setEmail(dto.getEmail());
        entity.setMensaje(dto.getMensaje());
        return entity;
    }

}
