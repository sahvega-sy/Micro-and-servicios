-- ms-usuarios · base clstore_usuarios
USE clstore_usuarios;

CREATE TABLE IF NOT EXISTS usuarios (
  id               VARCHAR(36)  NOT NULL,           -- UUID; es el "sub" del JWT
  run              VARCHAR(12)  NOT NULL,           -- sin puntos ni guion, DV en mayúscula
  nombre           VARCHAR(100) NOT NULL,
  apellidos        VARCHAR(100) NOT NULL,
  correo           VARCHAR(150) NOT NULL,
  password_hash    VARCHAR(100) NOT NULL,           -- BCrypt; nunca se devuelve por la API
  fecha_nacimiento DATE         NULL,
  tipo             VARCHAR(20)  NOT NULL DEFAULT 'Cliente',
  region           VARCHAR(100) NOT NULL,
  comuna           VARCHAR(100) NOT NULL,
  direccion        VARCHAR(255) NOT NULL,
  created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuarios_run (run),
  UNIQUE KEY uq_usuarios_correo (correo),            -- la colación _ci lo hace insensible a mayúsculas
  CONSTRAINT chk_usuarios_tipo CHECK (tipo IN ('Administrador', 'Vendedor', 'Cliente'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- No se siembran usuarios: al arrancar, ms-usuarios crea el administrador inicial si la tabla está vacía
-- (variables ADMIN_CORREO / ADMIN_PASSWORD / ADMIN_RUN; por defecto admin@duoc.cl / Admin123).
