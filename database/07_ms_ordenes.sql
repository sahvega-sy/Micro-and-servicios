-- ms-ordenes · base clstore_ordenes
USE clstore_ordenes;

CREATE TABLE IF NOT EXISTS ordenes (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  usuario_id          VARCHAR(36)   NULL,             -- NULL = compra de invitado
  nombre_comprador    VARCHAR(100)  NOT NULL,
  apellidos_comprador VARCHAR(100)  NOT NULL,
  correo_comprador    VARCHAR(150)  NOT NULL,
  telefono            VARCHAR(30)   NULL,
  region              VARCHAR(100)  NOT NULL,
  comuna              VARCHAR(100)  NOT NULL,
  direccion           VARCHAR(255)  NOT NULL,
  total               DECIMAL(12,2) NOT NULL DEFAULT 0,
  estado              VARCHAR(20)   NOT NULL,
  creado_en           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_ordenes_usuario (usuario_id),
  INDEX idx_ordenes_creado (creado_en),
  CONSTRAINT chk_ordenes_estado CHECK (estado IN ('exitosa', 'fallida')),
  CONSTRAINT chk_ordenes_total  CHECK (total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orden_items (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  orden_id        BIGINT        NOT NULL,
  producto_id     VARCHAR(100)  NOT NULL,             -- id en ms-productos (sin FK: otra base de datos)
  nombre_producto VARCHAR(255)  NOT NULL,             -- "congelado" al comprar
  cantidad        INT           NOT NULL,
  precio_unitario DECIMAL(12,2) NOT NULL,             -- precio final (con descuento) al comprar
  PRIMARY KEY (id),
  INDEX idx_orden_items_orden (orden_id),
  CONSTRAINT fk_orden_items_orden FOREIGN KEY (orden_id) REFERENCES ordenes (id) ON DELETE CASCADE,
  CONSTRAINT chk_orden_items_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
