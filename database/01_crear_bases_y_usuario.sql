-- =========================================================
-- CLstore · MySQL local
-- Una base de datos por microservicio (database-per-service).
-- Ejecutar los scripts 01 → 09 en orden (Workbench, consola mysql o docker-compose).
-- =========================================================

CREATE DATABASE IF NOT EXISTS clstore_productos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_categorias CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_blogs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_carrito CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_ordenes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_resenas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS clstore_contacto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usuario de desarrollo con acceso SOLO a esas bases (cambia la contraseña si lo expones).
CREATE USER IF NOT EXISTS 'clstore'@'%' IDENTIFIED BY 'clstore123';
GRANT ALL PRIVILEGES ON clstore_productos.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_categorias.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_blogs.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_usuarios.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_carrito.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_ordenes.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_resenas.* TO 'clstore'@'%';
GRANT ALL PRIVILEGES ON clstore_contacto.* TO 'clstore'@'%';
