package com.store.ordenes.service;
import com.store.ordenes.model.DTO.OrdenRequestDTO;
import com.store.ordenes.model.DTO.OrdenResponseDTO;
import com.store.ordenes.model.Orden;
import com.store.ordenes.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdenService {
    private final OrdenRepository ordenRepository;

    @Autowired
    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDTO> obtenerTodas() {
        return ordenRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenResponseDTO obtenerPorId(Long id) {
        Orden entity = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
        return mapToDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDTO> obtenerPorUsuarioId(String usuarioId) {
        return ordenRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdenResponseDTO crear(OrdenRequestDTO requestDTO) {
        Orden entity = mapToEntity(requestDTO);
        Orden guardada = ordenRepository.save(entity);
        return mapToDTO(guardada);
    }

    @Transactional
    public OrdenResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Orden entity = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
        entity.setEstado(nuevoEstado);
        Orden actualizada = ordenRepository.save(entity);
        return mapToDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!ordenRepository.existsById(id)) {
            throw new RuntimeException("Orden no encontrada con ID: " + id);
        }
        ordenRepository.deleteById(id);
    }

    private OrdenResponseDTO mapToDTO(Orden entity) {
        OrdenResponseDTO dto = new OrdenResponseDTO();
        dto.setId(entity.getId());
        dto.setUsuarioId(entity.getUsuarioId());
        dto.setNombreComprador(entity.getNombreComprador());
        dto.setApellidosComprador(entity.getApellidosComprador());
        dto.setCorreoComprador(entity.getCorreoComprador());
        dto.setTelefono(entity.getTelefono());
        dto.setRegion(entity.getRegion());
        dto.setComuna(entity.getComuna());
        dto.setDireccion(entity.getDireccion());
        dto.setEstado(entity.getEstado());
        dto.setTotal(entity.getTotal());
        dto.setCreadoEn(entity.getCreadoEn());
        return dto;
    }

    private Orden mapToEntity(OrdenRequestDTO dto) {
        Orden entity = new Orden();
        entity.setUsuarioId(dto.getUsuarioId());
        entity.setNombreComprador(dto.getNombreComprador());
        entity.setApellidosComprador(dto.getApellidosComprador());
        entity.setCorreoComprador(dto.getCorreoComprador());
        entity.setTelefono(dto.getTelefono());
        entity.setRegion(dto.getRegion());
        entity.setComuna(dto.getComuna());
        entity.setDireccion(dto.getDireccion());
        entity.setEstado(dto.getEstado());
        entity.setTotal(dto.getTotal());
        return entity;
    }

}
