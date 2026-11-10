CREATE TABLE IF NOT EXISTS perfiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    fecha_nacimiento DATE NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'Cliente',
    region VARCHAR(100),
    comuna VARCHAR(100),
    direccion VARCHAR(255),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO perfiles (id, run, nombre, apellidos, correo, fecha_nacimiento, tipo, region, comuna, direccion, created_at) VALUES 
(1, '200805143', 'Juan', 'Tello', 'juan@duoc.cl', NULL, 'Cliente', 'Atacama', 'Tierra Amarilla', 'calle no se donde es 1236767', '2026-10-13 16:29:53.769552'),
(2, '111111111', 'admin', 'minmin', 'admin@duoc.cl', '2026-10-12', 'Administrador', 'Metropolitana de Santiago', 'La Cisterna', 'av hola 123', '2026-10-10 23:35:45.613021'),
(3, '210284982', 'Carolina', 'Ramirez', 'caro@duoc.cl', '2026-10-10', 'Vendedor', 'Región Metropolitana de Santiago', 'La Cisterna', 'av goy 123', '2026-10-11 00:31:15.096557'),
(4, '112272291', 'Maria', 'Diaz', 'maria@duoc.cl', '2026-10-12', 'Cliente', 'Región Metropolitana de Santiago', 'Ñuñoa', 'Av calle ficticia 123', '2026-10-13 01:39:03.595190'),
(5, '221170113', 'juan de dios', 'garrido', 'juande@duoc.cl', '2026-10-13', 'Cliente', 'Región de la Araucanía', 'Nueva Imperial', 'calle no se 123', '2026-10-13 16:46:33.379671');