package com.store.perfil.service;

import com.store.perfil.model.DTO.PerfilRequestDTO;
import com.store.perfil.model.DTO.PerfilResponseDTO;
import com.store.perfil.model.perfil;
import com.store.perfil.repository.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;

    @Autowired
    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Transactional(readOnly = true)
    public List<PerfilResponseDTO> obtenerTodos() {
        return perfilRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO obtenerPorId(Long id) {
        perfil entity = perfilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado con ID: " + id));
        return mapToDTO(entity);
    }

    @Transactional
    public PerfilResponseDTO crear(PerfilRequestDTO requestDTO) {
        if (perfilRepository.findByRun(requestDTO.getRun()).isPresent()) {
            throw new RuntimeException("Ya existe un perfil registrado con el RUN: " + requestDTO.getRun());
        }
        if (perfilRepository.findByCorreo(requestDTO.getCorreo()).isPresent()) {
            throw new RuntimeException("Ya existe un perfil registrado con el correo: " + requestDTO.getCorreo());
        }

        perfil entity = mapToEntity(requestDTO);
        perfil guardado = perfilRepository.save(entity);
        return mapToDTO(guardado);
    }

    @Transactional
    public PerfilResponseDTO actualizar(Long id, PerfilRequestDTO requestDTO) {
        perfil entity = perfilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado con ID: " + id));

        entity.setRun(requestDTO.getRun());
        entity.setNombre(requestDTO.getNombre());
        entity.setApellidos(requestDTO.getApellidos());
        entity.setCorreo(requestDTO.getCorreo());
        entity.setFechaNacimiento(requestDTO.getFechaNacimiento());
        if (requestDTO.getTipo() != null && !requestDTO.getTipo().isBlank()) {
            entity.setTipo(requestDTO.getTipo());
        }
        entity.setRegion(requestDTO.getRegion());
        entity.setComuna(requestDTO.getComuna());
        entity.setDireccion(requestDTO.getDireccion());

        perfil actualizado = perfilRepository.save(entity);
        return mapToDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!perfilRepository.existsById(id)) {
            throw new RuntimeException("Perfil no encontrado con ID: " + id);
        }
        perfilRepository.deleteById(id);
    }

    private PerfilResponseDTO mapToDTO(perfil entity) {
        PerfilResponseDTO dto = new PerfilResponseDTO();
        dto.setId(entity.getId());
        dto.setRun(entity.getRun());
        dto.setNombre(entity.getNombre());
        dto.setApellidos(entity.getApellidos());
        dto.setCorreo(entity.getCorreo());
        dto.setFechaNacimiento(entity.getFechaNacimiento());
        dto.setTipo(entity.getTipo());
        dto.setRegion(entity.getRegion());
        dto.setComuna(entity.getComuna());
        dto.setDireccion(entity.getDireccion());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private perfil mapToEntity(PerfilRequestDTO dto) {
        perfil entity = new perfil();
        entity.setRun(dto.getRun());
        entity.setNombre(dto.getNombre());
        entity.setApellidos(dto.getApellidos());
        entity.setCorreo(dto.getCorreo());
        entity.setFechaNacimiento(dto.getFechaNacimiento());
        entity.setTipo(dto.getTipo());
        entity.setRegion(dto.getRegion());
        entity.setComuna(dto.getComuna());
        entity.setDireccion(dto.getDireccion());
        return entity;
    }
}