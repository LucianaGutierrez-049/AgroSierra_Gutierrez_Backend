# Backend Oracle verificado

Fecha: 27 de octubre de 2026.

## Entorno comprobado

- Java 21.0.12 y Maven Wrapper 3.9.16.
- Contenedor `agrosierra-oracle`, imagen `gvenzl/oracle-free:23.26.1-slim-faststart`.
- Oracle: `localhost:1522`, PDB `FREEPDB1`, esquema independiente `AGRODB`.
- AgroAPI: `http://localhost:8081/api/v1`, perfil `oracle`.
- La instancia anterior con H2 se detuvo antes de arrancar la instancia Oracle.
- Flyway administra el esquema; V1 crea las tablas y V2 corrige el índice de idempotencia.
- Compilación y empaquetado exitosos; las 3 pruebas automatizadas con H2 pasan.
- 28 comprobaciones HTTP sobre Oracle pasan; los registros temporales se eliminan al terminar.

## Correcciones realizadas

El importador de IntelliJ fallaba con Maven 3.10 y `ArtifactHandlerManager`. Se fijó el wrapper en 3.9.16.
Para actualizar el modelo del IDE usar Maven → Reload All Maven Projects. El proceso que se verificó
se ejecutó directamente con Java; la ejecución del botón de IntelliJ se comprueba por separado.

Oracle incluye claves parcialmente nulas en restricciones UNIQUE compuestas. La restricción original
rechazaba varias labores del mismo autor sin clienteId e impedía cargar las semillas.
V2 utiliza un índice por expresiones: cuando clienteId es nulo, ambas expresiones son nulas;
cuando existe clienteId, mantiene la unicidad por autor. Se conserva V1 sin alterar su checksum.

## Volver a ejecutar

1. Abrir Docker Desktop y esperar que el motor esté disponible.
2. Desde la raíz del backend ejecutar `docker compose up -d --wait oracle`.
3. Ejecutar `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=oracle"` con Java 21.

También se incluye `iniciar-oracle.cmd` para esos dos pasos. No iniciar una segunda instancia
mientras el puerto 8081 esté ocupado. La configuración `AgroAPI Oracle` se incluye en `.run`.

## Postman

Importar `docs/AgroAPI.postman_collection.json`. Ejecutar primero los logins y después
«Lotes paginados», que guarda los identificadores reales en variables de colección.
Usuarios: tecnico1, jefe1 y admin. Contraseña de demostración: Agro2026*.
La contraseña del esquema local es AgroDb2026_Dev, salvo que se configure DB_PASSWORD.

## Resultados HTTP

| Comprobación | HTTP | Resultado |
|---|---:|---|
| Login tecnico1 | 200 | Correcto |
| Login jefe1 | 200 | Correcto |
| Login admin | 200 | Correcto |
| Credenciales incorrectas | 401 | Correcto |
| Token ausente | 401 | Correcto |
| Firma JWT alterada | 401 | Correcto |
| Usuario actual | 200 | Correcto |
| Lotes paginados | 200 | Correcto |
| Filtro de campaña | 200 | Correcto |
| Recurso inexistente | 404 | Correcto |
| Privilegio de cierre | 403 | Correcto |
| Administrador no registra labores | 403 | Correcto |
| RN-01 campaña cerrada | 409 | Correcto |
| RN-04 dosis excesiva | 400 | Correcto |
| Crear labor | 201 | Correcto |
| Creación idempotente | 201 | Correcto |
| Editar labor propia | 200 | Correcto |
| RN-02 plazo de 24 horas | 403 | Correcto |
| Jefe corrige fuera del plazo | 200 | Correcto |
| Jefe registra labor | 201 | Correcto |
| RN-02 autor ajeno | 403 | Correcto |
| Técnico no elimina | 403 | Correcto |
| RN-03 cierre sin cosecha | 409 | Correcto |
| Registrar cosecha | 201 | Correcto |
| Cerrar campaña con cosecha | 200 | Correcto |
| Eliminar sin cuerpo | 204 | Correcto |
| Eliminar sin cuerpo | 204 | Correcto |
| Eliminar sin cuerpo | 204 | Correcto |

No se realizó una demostración visual dentro de Postman: esta verificación usa peticiones HTTP
automatizadas contra el servidor real. Los contenedores API y Android no forman parte de este paso.
