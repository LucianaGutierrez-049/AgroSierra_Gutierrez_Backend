# Postman y Docker

## Postman

Importar `docs/AgroAPI.postman_collection.json`. Ejecutar primero los tres logins.
La colección guarda los tokens en variables de la colección; no se exportan tokens reales.
Incluye pruebas automáticas de 200, 201, 204, 400, 401, 403 y 409, y un recorrido de CRUD.
La baseUrl es `http://localhost:8081/api/v1`.

## Oracle y API en Docker

Abrir Docker Desktop y esperar a que el motor esté disponible.
Detener cualquier AgroAPI local que use 8081 antes de arrancar este conjunto.
Desde la raíz del backend ejecutar:

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

El primer inicio descarga imágenes y puede tardar varios minutos. El servicio API espera
el healthcheck de Oracle. Flyway crea las tablas; no se crean manualmente en Postman ni DBeaver.

Oracle: localhost:1523, servicio FREEPDB1, usuario AGRODB.
Contraseña local de ejemplo: AgroDb2026_Dev (o DB_PASSWORD en .env).
API: localhost:8081. El emulador usa 10.0.2.2:8081.
Volumen persistente: oracle-data. No ejecutar `down -v` si se desean conservar los datos.
No se modifica el esquema PHARMADB de PharmaSoft.

Alternativa: levantar solo Oracle con `docker compose up -d oracle` y ejecutar AgroAPI en IntelliJ
con el perfil `oracle`. El perfil local predeterminado continúa usando H2.

## Estado de validación

Backend y aplicación tienen compilación/pruebas verificadas con H2.
La instalación del APK y su arranque se comprobaron con ADB.
La configuración Oracle/Docker se entrega separada; no se debe dar por probada su ejecución
hasta observar ambos servicios activos y repetir la colección de Postman contra Oracle.

Documentación de la imagen: https://hub.docker.com/r/gvenzl/oracle-free
Orden de inicio: https://docs.docker.com/compose/how-tos/startup-order/
