# Verificación de AgroAPI

27 de octubre de 2026. Java 21, Maven Wrapper, Spring Boot 4.1.2.
Comando ejecutado: `./mvnw.cmd test -B -ntp`.
Resultado: BUILD SUCCESS; 3 pruebas, 0 fallos, 0 errores.
Incluye servidor HTTP real, login correcto e incorrecto, JWT alterado, 401/403,
paginación, RN-01, RN-03, RN-04, edición ajena, idempotencia, PUT y DELETE 204.
La base probada es H2 con migraciones Flyway; Oracle todavía no está validado.
Los informes reproducibles se generan en target/surefire-reports (excluidos de Git).
