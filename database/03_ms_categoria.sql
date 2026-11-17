-- ms-categoria · base clstore_categorias
USE clstore_categorias;

CREATE TABLE IF NOT EXISTS categorias (
  valor    VARCHAR(100) NOT NULL,                -- slug estable, lo referencian los productos
  etiqueta VARCHAR(100) NOT NULL,                -- nombre visible
  icono    VARCHAR(50)  NOT NULL DEFAULT 'fa-folder',
  PRIMARY KEY (valor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Categorías que ya usan los productos de ejemplo (etiquetas tomadas de Productos.jsx).
INSERT IGNORE INTO categorias (valor, etiqueta, icono) VALUES
('componentes', 'Componentes', 'fa-microchip'),
('consolas', 'Consolas & Laptops', 'fa-gamepad'),
('perifericos', 'Periféricos & Monitores', 'fa-keyboard');
