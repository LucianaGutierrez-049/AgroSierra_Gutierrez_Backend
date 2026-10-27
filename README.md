# AgroAPI Gutierrez

Backend del caso AgroSierra. Kotlin, Spring Boot, Maven, Java 21, JPA y Flyway.

## Ejecutar

Desde esta carpeta: `./mvnw.cmd spring-boot:run` (PowerShell).
Escucha en http://localhost:8081; evita colisionar con PharmaSoft en 8080.
La configuración predeterminada usa H2 **persistente en disco**, con migraciones y datos semilla reales.
Oracle 23+ se habilita con el perfil `oracle` y las variables DB_URL, DB_USER y DB_PASSWORD.
No se reutiliza ni modifica PHARMADB. El perfil Oracle requiere validar las migraciones en la instancia elegida.
JWT_SECRET puede reemplazar la clave exclusiva de desarrollo local. No publicar con la clave de ejemplo.

## Contrato

POST /api/v1/auth/login: usuario, contrasena. JWT HS256 válido una hora.
GET /api/v1/usuarios/me: identidad y privilegios.
GET /api/v1/lotes?pagina=0&tamanio=20&valle=Mantaro&campaniaCerrada=false
GET /api/v1/lotes/{id}
GET /api/v1/lotes/{id}/labores?pagina=0&tamanio=20
POST /api/v1/labores; PUT y DELETE /api/v1/labores/{id}
POST /api/v1/lotes/{id}/cerrar-campania

Los listados devuelven contenido, pagina, tamanio, totalElementos, totalPaginas y ultima.
Los errores devuelven timestamp, status, error, message, path y validationErrors.
POST labor acepta clienteId opcional para reintentos idempotentes de la cola offline.

## Usuarios de demostración

tecnico1, jefe1 y admin; contraseña Agro2026*.
Técnico: lectura, crear y editar propias dentro de 24 horas.
Jefe: lectura, crear, editar todas, eliminar y cerrar campaña.
Administrador: lectura y USUARIO_GESTIONAR; no registra labores.
Los privilegios y el alcance de edición están persistidos en rol y rol_privilegio.

## Arquitectura y pruebas

config: seguridad JWT, errores y carga inicial. dominio: entidades. repositorio: JPA.
servicio: reglas RN-01 a RN-04 y transacciones. web/dto: contrato HTTP.
`./mvnw.cmd test` ejecuta pruebas contra el servidor HTTP real con una base H2 en memoria.

## Git

Mantener repositorio propio: main, develop, ramas feature y PR hacia develop; fusiones --no-ff.
No crear historia retrospectiva. El historial real previo no se obtiene con este código inicial.
Etiqueta v1.0-unidad2 solo cuando el producto haya sido evaluado/verificado.

## Repositorio

https://github.com/LucianaGutierrez-049/AgroSierra_Gutierrez_Backend.git
