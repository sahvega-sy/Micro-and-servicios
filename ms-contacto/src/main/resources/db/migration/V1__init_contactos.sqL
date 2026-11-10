CREATE TABLE IF NOT EXISTS contactos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    creado_en DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO contactos (id, nombre, email, mensaje, creado_en) VALUES 
(1, 'dasfadsdas', 'dasdasads@duoc.cl', 'qwe1132134232', '2026-10-12 02:27:27.715944');