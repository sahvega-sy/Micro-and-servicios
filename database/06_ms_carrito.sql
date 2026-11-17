-- ms-carrito · base clstore_carrito
USE clstore_carrito;

CREATE TABLE IF NOT EXISTS carrito_items (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  usuario_id  VARCHAR(36)  NOT NULL,                -- id en ms-usuarios (sin FK: otra base de datos)
  producto_id VARCHAR(100) NOT NULL,                -- id en ms-productos (sin FK: otra base de datos)
  cantidad    INT          NOT NULL,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_carrito_usuario_producto (usuario_id, producto_id),
  CONSTRAINT chk_carrito_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
