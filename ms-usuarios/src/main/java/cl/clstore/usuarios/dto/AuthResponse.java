package cl.clstore.usuarios.dto;

/** Respuesta de login y registro: el JWT para enviar como "Authorization: Bearer" y el perfil del usuario. */
public record AuthResponse(String token, UsuarioResponse usuario) {
}
