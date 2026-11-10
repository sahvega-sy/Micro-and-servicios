CREATE TABLE IF NOT EXISTS ordenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id VARCHAR(50) NULL,
    nombre_comprador VARCHAR(100) NOT NULL,
    apellidos_comprador VARCHAR(100) NOT NULL,
    correo_comprador VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NULL,
    region VARCHAR(100) NOT NULL,
    comuna VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'exitosa',
    total DECIMAL(12,2) NOT NULL,
    creado_en DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ordenes (id, usuario_id, nombre_comprador, apellidos_comprador, correo_comprador, telefono, region, comuna, direccion, estado, total, creado_en) VALUES 
(1, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'Carolina', 'Ramirez', 'admin@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'Conchalí', 'Av hola 123', 'exitosa', 2147980, '2026-10-10 23:33:58.696698'),
(2, NULL, 'Juan', 'Mirra', 'juan@duoc.cl', '1234', 'Región del Biobío', 'Santa Juana', 'av muy lejos 123', 'exitosa', 1637980, '2026-10-11 00:40:23.347753'),
(3, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Arica y Parinacota', 'Arica', 'av hola 123', 'exitosa', 4497970, '2026-10-11 19:55:54.215815'),
(4, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'Ñuñoa', 'av hola 123', 'exitosa', 538482, '2026-10-11 22:00:35.953240'),
(6, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Valparaíso', 'Casablanca', 'av hola 123', 'exitosa', 315000, '2026-10-11 22:53:20.111011'),
(7, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Región de Los Ríos', 'Lago Ranco', 'av hola 123', 'exitosa', 3181980, '2026-10-11 23:04:55.203163'),
(8, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admina', 'minmina', 'admin@duoc.cl', NULL, 'Región de Magallanes y de la Antártica Chilena', 'Torres del Paine', 'av hola 123', 'exitosa', 1602466, '2026-10-11 23:52:28.773319'),
(9, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Tarapacá', 'Iquique', 'av hola 123', 'exitosa', 348492, '2026-10-12 00:59:25.812133'),
(12, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Antofagasta', 'Mejillones', 'av hola 123', 'fallida', 7799980, '2026-10-12 01:06:28.382664'),
(13, '6eac834a-c495-4efc-90ad-4ca0474e8e73', 'Carolina', 'Ramirez', 'caro@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'La Cisterna', 'av goy 123', 'exitosa', 451483, '2026-10-12 01:07:50.223057'),
(14, '6eac834a-c495-4efc-90ad-4ca0474e8e73', 'Carolina', 'Ramirez', 'caro@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'La Cisterna', 'av goy 123', 'fallida', 7799980, '2026-10-12 01:08:03.661089'),
(15, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Tarapacá', 'Iquique', 'av hola 123', 'fallida', 8345264, '2026-10-12 02:27:34.402703'),
(16, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'Ñuñoa', 'av hola 123', 'exitosa', 348492, '2026-10-13 01:31:15.640438'),
(17, '3a4cd91d-55bc-4a71-926d-1ebd1e18f9fd', 'admin', 'minmin', 'admin@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'Macul', 'av hola 123', 'exitosa', 4246980, '2026-10-13 01:44:42.476593'),
(18, '1607106f-932c-4a58-aa65-879c9986ca08', 'Nicolas', 'Tello', 'nico@duoc.cl', NULL, 'Región Metropolitana de Santiago', 'Huechuraba', 'calle uno 123', 'exitosa', 545284, '2026-10-13 16:26:49.499433'),
(19, 'cf679094-91a9-46a6-9583-cb9940c3ba4a', 'juan de dios', 'garrido', 'juande@duoc.cl', NULL, 'Región de la Araucanía', 'Nueva Imperial', 'calle no se 123', 'exitosa', 590376, '2026-10-13 16:48:05.346401'),
(20, 'cf679094-91a9-46a6-9583-cb9940c3ba4a', 'juan de dios', 'garrido', 'juande@duoc.cl', NULL, 'Región de la Araucanía', 'Nueva Imperial', 'calle no se 123', 'fallida', 7799980, '2026-10-13 16:48:28.584489'),
(21, 'cf679094-91a9-46a6-9583-cb9940c3ba4a', 'juan de dios', 'garrido', 'juande@duoc.cl', NULL, 'Región de la Araucanía', 'Nueva Imperial', 'calle no se 123', 'exitosa', 1637980, '2026-10-13 16:55:38.875043');