-- ms-productos · base clstore_productos
USE clstore_productos;

CREATE TABLE IF NOT EXISTS productos (
  id                   VARCHAR(100)  NOT NULL,
  nombre               VARCHAR(100)  NOT NULL,
  descripcion          VARCHAR(500)  NULL,
  precio               DECIMAL(12,2) NOT NULL,
  stock                INT           NOT NULL DEFAULT 0,
  stock_critico        INT           NULL,
  categoria            VARCHAR(100)  NOT NULL,   -- "valor" de ms-categoria (sin FK: otra base de datos)
  imagen               VARCHAR(1000) NULL,
  imagenes             JSON          NULL,
  busqueda             VARCHAR(255)  NULL,
  destacado            JSON          NULL,
  especificaciones     JSON          NULL,
  descuento_porcentaje INT           NULL,
  created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT chk_productos_precio    CHECK (precio >= 0),
  CONSTRAINT chk_productos_stock     CHECK (stock >= 0),
  CONSTRAINT chk_productos_descuento CHECK (descuento_porcentaje IS NULL OR descuento_porcentaje BETWEEN 1 AND 95),
  INDEX idx_productos_categoria (categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed de productos (convertido desde supabase/seed_productos.sql)
INSERT IGNORE INTO productos (id, nombre, descripcion, precio, stock, stock_critico, categoria, imagen, busqueda, destacado, especificaciones, created_at) VALUES
('rtx3080', 'RTX 3080', 'Tarjeta de video NVIDIA RTX 3080, gran rendimiento para gaming en 1440p y 4K.', 409990, 10, 2, 'componentes', '/img/rtx3080.jpg', 'rtx 3080 nvidia tarjeta grafica', '{"titulo": "Rendimiento Extremo", "texto": "Si buscas lo mejor, esta gráfica es para ti. Rendimiento ideal para jugar a máximos ajustes, altas tasas de FPS y tareas de renderizado profesional."}', '{"Fabricante": "NVIDIA", "Categoría": "Tarjeta gráfica", "Card Bus": "PCI-E 4.0", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 0 MINUTE),
('monitorZowie', 'Monitor Zowie', 'Monitor gamer BenQ Zowie de alta frecuencia de actualización, pensado para esports.', 1590990, 6, 2, 'perifericos', '/img/monitor.jpg', 'monitor zowie benq gamer', '{"titulo": "Ventaja Competitiva", "texto": "Cada milisegundo cuenta. Diseñado específicamente para eSports con tecnología DyAc para una claridad de movimiento inigualable."}', '{"Fabricante": "BenQ Zowie", "Categoría": "Monitor gamer", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 1 MINUTE),
('switch2', 'Switch 2 256GB', 'Consola híbrida Nintendo Switch 2, con 256GB de almacenamiento interno.', 556990, 15, 3, 'consolas', '/img/switch2.avif', 'switch 2 256gb nintendo consolas', '{"titulo": "Nueva Generación", "texto": "Lleva tus juegos favoritos a cualquier parte con gráficos mejorados, mayor duración de batería y una pantalla vibrante."}', '{"Fabricante": "Nintendo", "Categoría": "Consola", "Almacenamiento": "256GB", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 2 MINUTE),
('notebookLegion', 'Notebook Gamer Legion', 'Notebook gamer Lenovo Legion, pensado para gaming y trabajo exigente.', 1290990, 5, 2, 'consolas', '/img/notebookGamer.webp', 'notebook gamer legion lenovo laptop', '{"titulo": "Potencia Portátil", "texto": "El equilibrio perfecto entre rendimiento térmico y potencia bruta. Todo el poder de un PC de escritorio en tu mochila."}', '{"Fabricante": "Lenovo", "Categoría": "Notebook gamer", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 3 MINUTE),
('ryzen7', 'Procesador Ryzen 7', 'Procesador AMD Ryzen 7, gran rendimiento multi-hilo para gaming y multitarea.', 346990, 12, 3, 'componentes', '/img/proceRyzen7.jpg', 'procesador ryzen 7 amd cpu', '{"titulo": "Multitarea sin Límites", "texto": "Arquitectura de última generación para exprimir tus juegos y ejecutar aplicaciones pesadas simultáneamente sin caídas de rendimiento."}', '{"Fabricante": "AMD", "Categoría": "Procesador", "Socket": "AM5", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 4 MINUTE),
('refCorsair', 'Refrigeracion Liquida Corsair', 'Sistema de refrigeración líquida Corsair para mantener tu CPU a bajas temperaturas.', 250990, 8, 2, 'componentes', '/img/refrigeracionLiquida.jpg', 'refrigeracion liquida corsair', '{"titulo": "Temperaturas a Raya", "texto": "Disipación térmica superior con un toque estético inigualable. Mantén tu equipo frío incluso en las sesiones de juego más exigentes."}', '{"Fabricante": "Corsair", "Categoría": "Refrigeración líquida", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 5 MINUTE),
('tecladoProX60', 'Teclado Logitech PRO X60', 'Teclado mecánico gamer Logitech PRO X60, compacto e inalámbrico.', 189990, 20, 4, 'perifericos', '/img/tecladoLogitech.png', 'teclado logitech pro x60', '{"titulo": "Diseño Compacto", "texto": "Libera espacio en tu escritorio sin sacrificar funcionalidad. Switches mecánicos ultra rápidos ideales para torneos profesionales."}', '{"Fabricante": "Logitech", "Categoría": "Teclado gamer", "Conectividad": "Inalámbrico", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 6 MINUTE),
('superlight2', 'Logitech Superlight 2', 'Mouse gamer ultraliviano Logitech Superlight 2, ideal para esports.', 245990, 18, 4, 'perifericos', '/img/mouse logitech.jpg', 'mouse logitech superlight 2', '{"titulo": "Precisión Absoluta", "texto": "Equipado con el sensor HERO 2 y un peso que casi no se siente en la mano, permitiendo que tus movimientos fluyan sin resistencia."}', '{"Fabricante": "Logitech", "Categoría": "Mouse gamer", "Conectividad": "Inalámbrico", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 7 MINUTE),
('audifonosRazer', 'Audifonos Razer Gengar', 'Audífonos gamer Razer edición Gengar, sonido envolvente y micrófono integrado.', 282990, 14, 3, 'perifericos', '/img/audifonosRazer.jpg', 'audifonos razer gengar', '{"titulo": "Audio Inmersivo", "texto": "Siente cada paso del enemigo con audio espacial 7.1, luciendo un diseño exclusivo de edición limitada que no pasará desapercibido."}', '{"Fabricante": "Razer", "Categoría": "Audífonos gamer", "Conectividad": "Inalámbrico", "Garantía": "12 meses"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 8 MINUTE),
('Nvidia5090', 'Nvidia GeForce 5090', 'Tarjeta de video Nvidia GeForce 5090 32GB VRAM.', 3899990, 3, 1, 'componentes', '/img/rtx.jpg', 'nvidia geforce 5090 rtx tarjeta grafica', '{"titulo": "El Futuro del Gaming", "texto": "Potencia desmesurada para resoluciones 4K, 8K y el máximo aprovechamiento de la inteligencia artificial. La gráfica definitiva."}', '{"Color": "Negro", "Capacidad": "32GB VRAM", "Fabricante": "NVIDIA", "Card Bus": "PCI-E 5.0", "Frecuencia": "3200Mhz", "Procesadores CUDA": "41245", "Tipo de Memoria": "GDDR7", "OpenGL": "4.6", "Puertos de Salida": "Hdmi x 1 DisplayPort x 4", "Puertos de Energía": "1"}', TIMESTAMP('2026-01-01 00:00:00') + INTERVAL 9 MINUTE);
