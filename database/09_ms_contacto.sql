-- ms-contacto · base clstore_contacto
USE clstore_contacto;

CREATE TABLE IF NOT EXISTS contactos (
  id        BIGINT        NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(100)  NOT NULL,
  email     VARCHAR(150)  NOT NULL,
  mensaje   VARCHAR(2000) NOT NULL,
  creado_en DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_contactos_creado (creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
