-- ms-resenas · base clstore_resenas
USE clstore_resenas;

CREATE TABLE IF NOT EXISTS resenas (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  producto_id  VARCHAR(100)  NOT NULL,                -- id en ms-productos (sin FK: otra base de datos)
  usuario      VARCHAR(100)  NOT NULL,                -- nombre visible del autor
  calificacion INT           NOT NULL,
  comentario   VARCHAR(1000) NOT NULL,
  fecha        DATE          NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_resenas_producto (producto_id),
  CONSTRAINT chk_resenas_calificacion CHECK (calificacion BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed de reseñas (convertido desde supabase/seed_productos.sql). Solo se carga si la tabla está vacía.
INSERT INTO resenas (producto_id, usuario, calificacion, comentario, fecha)
SELECT * FROM (
  SELECT 'rtx3080' AS producto_id, 'Carlos M.' AS usuario, 5 AS calificacion, 'Rendimiento impecable para 1440p. Muy silenciosa.' AS comentario, '2026-08-10' AS fecha
  UNION ALL
  SELECT 'monitorZowie', 'Matías K.', 5, 'La mejor tasa de refresco para competitivo. 10/10.', '2026-09-02'
  UNION ALL
  SELECT 'switch2', 'Ana G.', 4, 'Excelente salto gráfico, la pantalla se ve increíble.', '2026-08-15'
  UNION ALL
  SELECT 'switch2', 'Felipe R.', 5, 'Los nuevos joy-cons son mucho más cómodos.', '2026-09-01'
  UNION ALL
  SELECT 'notebookLegion', 'Luis P.', 5, 'Corre todo en ultra sin calentarse demasiado.', '2026-07-20'
  UNION ALL
  SELECT 'ryzen7', 'Javier V.', 5, 'Potencia bruta para multitarea y gaming.', '2026-09-05'
  UNION ALL
  SELECT 'refCorsair', 'Diego R.', 4, 'Mantiene mi CPU bajo los 60 grados jugando. El RGB es hermoso.', '2026-08-12'
  UNION ALL
  SELECT 'tecladoProX60', 'Sofía T.', 5, 'Tamaño perfecto para el escritorio y switches súper suaves.', '2026-08-28'
  UNION ALL
  SELECT 'superlight2', 'Felipe C.', 5, 'No pesa absolutamente nada y la batería dura semanas.', '2026-09-10'
  UNION ALL
  SELECT 'audifonosRazer', 'Camila L.', 5, 'El diseño de Gengar es precioso y el audio espacial funciona genial.', '2026-09-01'
  UNION ALL
  SELECT 'Nvidia5090', 'Tomás H.', 5, 'Una verdadera bestia. Asegúrense de tener una buena fuente de poder.', '2026-09-14'
  UNION ALL
  SELECT 'Nvidia5090', 'Eduardo S.', 4, 'El precio es altísimo, pero el rendimiento no tiene rival.', '2026-09-16'
) AS semilla
WHERE NOT EXISTS (SELECT 1 FROM resenas);
