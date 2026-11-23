# CLstore · Microservicios (Spring Boot + MySQL)

Ocho microservicios independientes que reemplazan por completo el backend de Supabase del front React.
Cada uno tiene su propia base de datos MySQL (*database-per-service*) y comparten un único secreto JWT.

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `ms-productos` | 8081 | `clstore_productos` | Catálogo, imágenes y **stock** |
| `ms-categoria` | 8082 | `clstore_categorias` | Categorías |
| `ms-blogs` | 8083 | `clstore_blogs` | Artículos e imágenes |
| `ms-usuarios` | 8084 | `clstore_usuarios` | Registro, login (emite el JWT) y gestión de usuarios |
| `ms-carrito` | 8085 | `clstore_carrito` | Carrito de compras del usuario |
| `ms-ordenes` | 8086 | `clstore_ordenes` | Compra: valida precios y stock, crea órdenes |
| `ms-resenas` | 8087 | `clstore_resenas` | Reseñas de productos |
| `ms-contacto` | 8088 | `clstore_contacto` | Mensajes del formulario de contacto |

Requisitos: **JDK 17+**, **Maven 3.9+**, **MySQL 8** (instalado o con Docker). Spring Boot 3.5.0.

## Cómo se hablan entre sí

```
front ──► ms-usuarios            (login / registro → JWT)
front ──► ms-productos, ms-categoria, ms-blogs, ms-carrito, ms-ordenes, ms-resenas, ms-contacto
ms-productos ──► ms-categoria    valida que la categoría exista
ms-categoria ──► ms-productos    bloquea borrar una categoría con productos
ms-carrito   ──► ms-productos    lee nombre, precio, imagen y stock de cada línea
ms-resenas   ──► ms-productos    valida que el producto exista
ms-ordenes   ──► ms-productos    lee precios y DESCUENTA / REPONE stock (con JWT de servicio)
```

## 1. Crear las bases de datos

**Con Docker** (ejecuta los scripts la primera vez): `docker compose up -d`

**Con MySQL ya instalado** (Workbench, XAMPP...), en orden y con un usuario administrador:

```bash
for f in database/0*.sql; do mysql -u root -p < "$f"; done
```

Los scripts `01` a `09` crean las 8 bases, el usuario `clstore` / `clstore123` (solo con acceso a ellas), las tablas y los datos de ejemplo
(10 productos, 3 categorías, 4 blogs, 12 reseñas). Son re-ejecutables.

## 2. Levantar los servicios

Una terminal por servicio (el orden no importa; los que dependen de otro solo fallan con 503 si ese no está arriba):

```bash
for s in ms-productos ms-categoria ms-blogs ms-usuarios ms-carrito ms-ordenes ms-resenas ms-contacto; do
  (cd $s && mvn spring-boot:run) &
done
```

**Primer ingreso:** al arrancar, `ms-usuarios` crea un administrador si la tabla está vacía:
**`admin@duoc.cl` / `Admin123`** (RUN `111111111`). Cámbialo con las variables `ADMIN_CORREO`, `ADMIN_PASSWORD`, `ADMIN_RUN`
*antes* del primer arranque, o con `PUT /api/usuarios/{id}` después.

### Variables de entorno (todas con valor por defecto para desarrollo local)

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/clstore_<...>` | Conexión MySQL |
| `DB_USER` / `DB_PASSWORD` | `clstore` / `clstore123` | Credenciales MySQL |
| **`JWT_SECRET`** | secreto de desarrollo (≥ 32 caracteres) | **Debe ser IGUAL en los 8 servicios.** Cámbialo fuera de desarrollo |
| `JWT_HORAS` | `8` | Duración del JWT (ms-usuarios) |
| `CORS_ORIGINS` | `http://localhost:5173` | Origen(es) del front, separados por coma |
| `MS_PRODUCTOS_URL` / `MS_CATEGORIAS_URL` | `http://localhost:8081` / `:8082` | Direcciones entre servicios |
| `UPLOADS_DIR` | `uploads` | Carpeta de imágenes (ms-productos, ms-blogs) |
| `ADMIN_CORREO`, `ADMIN_PASSWORD`, `ADMIN_RUN` | ver arriba | Administrador inicial |

## Documentación de la API (Swagger / OpenAPI)

Cada microservicio publica su propia documentación interactiva con **springdoc-openapi**:

| Servicio | Swagger UI | JSON OpenAPI |
|---|---|---|
| `ms-productos` | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| `ms-categoria` | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| `ms-blogs` | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| `ms-usuarios` | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| `ms-carrito` | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| `ms-ordenes` | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |
| `ms-resenas` | http://localhost:8087/swagger-ui.html | http://localhost:8087/v3/api-docs |
| `ms-contacto` | http://localhost:8088/swagger-ui.html | http://localhost:8088/v3/api-docs |

**Probar endpoints protegidos:** en el Swagger de `ms-usuarios` ejecuta `POST /api/auth/login` (`admin@duoc.cl` / `Admin123`),
copia el `token`, pulsa **Authorize** y pégalo **sin** el prefijo `Bearer`. Como los 8 servicios comparten `JWT_SECRET`, el mismo token sirve en todos.
Los endpoints con candado requieren JWT; los demás son públicos. Los endpoints internos de stock (`/api/productos/stock/**`) están ocultos.

En producción se puede desactivar con la variable `SWAGGER_ENABLED=false`.

## Seguridad

- **JWT HS256** firmado por ms-usuarios con claims `sub` (id), `rol` (`Administrador` | `Vendedor` | `Cliente`), `nombre`, `correo`.
  Cada servicio lo valida con el mismo secreto. Contraseñas con **BCrypt**; la API nunca devuelve el hash.
- Los `GET` del catálogo son públicos; la escritura requiere `Administrador` o `Vendedor`. Carrito y `me` requieren sesión (cualquier rol).
  La compra (`POST /api/ordenes`) y el contacto (`POST /api/contactos`) son públicos para permitir compras de invitado.
- Un **Vendedor no puede** crear, editar ni eliminar cuentas de **Administrador** (evita escalar privilegios), ni eliminarse a sí mismo.
- Los endpoints de stock de ms-productos solo aceptan un JWT de **rol `Servicio`**, que únicamente ms-ordenes emite (2 minutos de vida);
  ningún usuario puede obtener un token con ese rol.
- El token de un usuario inválido o vencido se rechaza con 401 incluso en rutas públicas (comportamiento estándar de Spring Security).

## Endpoints

Errores en formato RFC 7807: el mensaje legible está en `detail`.

**ms-usuarios** — `POST /api/auth/login` · `POST /api/auth/registro` (siempre crea Cliente; solo correos @duoc.cl, @profesor.duoc.cl, @gmail.com; RUN con módulo 11) ·
`GET /api/usuarios/me` (sesión) · `GET|POST /api/usuarios` · `GET|PUT|DELETE /api/usuarios/{id}` (Administrador/Vendedor).

**ms-productos** — `GET /api/productos` con filtros `categoria`, `q`, `oferta=true`, `excluir`, `ids`, `orden`, `limit` · `GET /{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` ·
`POST /api/productos/imagenes` (multipart `archivo`, ≤ 5 MB) · imágenes en `GET /uploads/**` ·
*interno:* `POST /api/productos/stock/descontar` y `/reponer` (solo rol Servicio; `descontar` es todo-o-nada, 409 con el detalle si no alcanza).

**ms-categoria** — `GET /api/categorias` · `GET /{valor}` · `POST` · `PUT /{valor}` · `DELETE /{valor}` (409 si tiene productos).

**ms-blogs** — `GET /api/blogs` · `GET /{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` · `POST /api/blogs/imagenes`.

**ms-carrito** (sesión) — `GET /api/carrito` · `POST /api/carrito/items` · `PUT /items/{productoId}` · `DELETE /items/{productoId}` · `DELETE /api/carrito`.
Cada operación devuelve el carrito actualizado con el producto embebido. Los productos eliminados del catálogo desaparecen solos del carrito.

**ms-ordenes** — `POST /api/ordenes` (público; con token queda a nombre del usuario) · `GET /api/ordenes?estado=` y `GET /api/ordenes/items` (personal) ·
`GET /api/ordenes/mias` (sesión) · `GET /api/ordenes/{id}` (con `items`; su dueño o el personal).

**ms-resenas** — `GET /api/resenas?producto_id=` (público) · `POST` (sesión; el autor sale del token) · `DELETE /{id}` (personal).

**ms-contacto** — `POST /api/contactos` (público) · `GET` y `DELETE /{id}` (personal).

### Flujo de compra (`POST /api/ordenes`)

1. Lee los productos en ms-productos (**el cliente no envía precios**) y calcula el total con el descuento (mismo redondeo que el front).
2. Descuenta el stock en ms-productos de forma **atómica y todo-o-nada**.
3. Guarda la orden `exitosa` con sus líneas (nombre y precio "congelados"). Si este paso falla, **devuelve el stock** (compensación).
4. Si no hay stock o un producto fue retirado: guarda la orden como `fallida` y responde `409` con `detail` y `orden_id`.

## Probar con curl

```bash
TOKEN=$(curl -s localhost:8084/api/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"admin@duoc.cl","password":"Admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl localhost:8081/api/productos?categoria=componentes
curl localhost:8084/api/usuarios -H "Authorization: Bearer $TOKEN"

curl -X POST localhost:8086/api/ordenes -H 'Content-Type: application/json' -d '{
  "nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com","region":"R","comuna":"C","direccion":"Calle 1",
  "items":[{"producto_id":"rtx3080","cantidad":1}]}'
```

`scripts/GenerarToken.java` sigue disponible para generar un JWT de pruebas sin pasar por el login.

## Decisiones y límites a tener presentes

- **Sin transacciones distribuidas.** La consistencia entre servicios se resuelve con validaciones HTTP y compensación (reponer stock).
  Si ms-ordenes cae justo entre descontar y guardar, el stock puede quedar descontado sin orden (se registra en el log para corregirlo a mano).
  Para producción serio conviene una saga/outbox o mensajería.
- **No hay llaves foráneas entre bases.** Borrar un usuario no borra su carrito/órdenes; el `valor` de una categoría es inmutable.
- **No se migran usuarios de Supabase** (sus contraseñas están hasheadas por Supabase): se registran de nuevo o los crea el admin.
- **Sin protección contra fuerza bruta** en el login ni límite de peticiones (rate limiting) en los endpoints públicos (compra, contacto, registro).
- **Imágenes en disco local** con URL absoluta (`http://localhost:8081/...`): válido en desarrollo; no se borran al eliminar el producto/blog,
  y para varias instancias o producción conviene almacenamiento de objetos.
- El JWT vive en `localStorage` en el front (expuesto a XSS) y no hay *refresh token*: al vencer (8 h) se vuelve a iniciar sesión.
- Todos los servicios se conectan a MySQL con un usuario que tiene permisos solo sobre sus bases; cambia la contraseña de desarrollo.
