# C3 · P7 · Respaldo y restauración (M12)

Rama de trabajo `feature/p1-base-seguridad-admin`, base `1b8de3a`, con los cambios acumulados de C1, C2 (P2–P5) y C3 (P6–P7) sin commit. Sin cambios en `pom.xml`, `AGENTS.md`, `docs/AGENTES_Y_MODELOS.md` ni nuevas migraciones de base de datos (`V0`–`V16`).

## Resumen de verificación

| Criterio | Evidencia | Estado |
| --- | --- | --- |
| `RF-M12-06` (`CP-051`) · Respaldo cifrado consistente | `RespaldoService.crearRespaldo`, `CifradoRespaldo` (`AES/GCM/NoPadding`, `PBKDF2WithHmacSHA256` 600,000 iteraciones, sal 16B, IV base 12B, etiqueta 128 bits, AAD por bloque), `PuertaMantenimiento` exclusiva (`FOR UPDATE` + `respaldo_lock_timeout_ms`), manifiesto JSON sin credenciales (`ManifiestoRespaldoDTO`), auto-verificación previa a publicación atómica y auditoría `RESPALDO_CREADO`; verificado en `CifradoRespaldoTest` (5/5) y `RespaldoServiceTest` (8/8). | **Verificado** (H2 / flujo) |
| `RF-M12-07` (`CP-095` / `ESC-20`) · Restauración aislada autenticada | `RespaldoService.restaurarEnEntornoAislado` exige modo mantenimiento (`chkModoMantenimiento` / flag), permiso `CAMBIAR_CONFIGURACION`, 0 sesiones activas, verificación AES-256-GCM previa a extracción (`ERR-059 / MSG-082` en `RestauracionException`), validación SHA-256 de dump y adjuntos (`ERR-055 / MSG-079`), detección de recibos posteriores (`ERR-058 / MSG-081` sin renumerar), copia previa obligatoria y conservación del destino ante fallo; verificado en `RespaldoServiceTest`. | **Verificado** (H2) |
| `RN-29` · Exclusión de escrituras y retención | `RespaldoServiceTest.puertaExclusivaRespetaLockTimeoutCuandoHayEscrituraActiva` (contención de `puerta_mantenimiento` con `respaldo_lock_timeout_ms=300` → `ConflictoDatosException` sin publicar paquete); `PoliticaRetencionRespaldos` (7 conjuntos diarios y 4 semanales conservando el más reciente y sin tocar archivos ajenos). | **Verificado** (H2) |
| `RNF-036`, `RNF-037` · Metas de tamaño, tiempo y 0 hashes discordantes | `CifradoRespaldoTest.rendimientoDeFlujoMultiBloqueEsCompatibleConRnf036YRnf037` (cifrado + descifrado en flujo O(1) de 8 MiB < 15 s) y `RespaldoServiceTest.respaldoCompletoYRestauracionAisladaConCeroHashesDiscordantesYSinSecretos` (`hashesDiscordantes == 0`). La medición exacta con 1 GiB de base + 1 GiB de adjuntos en SSD de V1 queda pendiente. | **Parcial** (lógica y flujo verificados; benchmark 2 GiB en SSD V1 **No probado**) |
| `RNF-038` · Alerta de respaldo tras 24 horas | `RespaldoService.consultarEstadoAlerta` evalúa `respaldo.ultimo_utc` / auditoría `RESPALDO_CREADO` frente a `now()` de la BD; verificado en `RespaldoServiceTest.alerta24HorasYPoliticaDeRetencionSieteDiariosCuatroSemanales`. | **Verificado** (H2) |
| Interfaz JavaFX (`admin-view.fxml` / `AdminController`) | Pestaña `"Respaldo y restauración"` integrada en `AdminController` (ejecución fuera del hilo JavaFX vía `TareasJavaFx.ejecutar`, limpieza inmediata de `PasswordField`); `FxmlControladoresTest`, `EstilosCssTest` y `ContrasteTest` verdes. | **Verificado** (estático) / **No probado** (humo visual interactivo) |
| PostgreSQL + `pg_dump` / `pg_restore` (`PostgresMigrationIT.RespaldoYRestauracionPostgres`) | Prueba de integración escrita en `PostgresMigrationIT` usando `MotorPostgresRespaldo` (`pg_dump --format=custom`, `pg_restore --clean --if-exists --exit-on-error`, `pg_stat_activity`, `hbm2ddl=validate`). En esta sesión `LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD` no estaban configuradas (`PG_ENV_SET=False`). | **No probado** |

## Comandos ejecutados

1. Pruebas dirigidas P7:
   ```powershell
   ./mvnw.cmd -o -B test "-Dtest=CifradoRespaldoTest,RespaldoServiceTest,FxmlControladoresTest,EstilosCssTest,ContrasteTest"
   ```
   Resultado: **16 pruebas ejecutadas, 0 fallos, 0 errores, 0 omisiones** (`BUILD SUCCESS`).
2. Suite completa H2:
   ```powershell
   ./mvnw.cmd -o -B test -q
   ```
   Resultado: **322 pruebas ejecutadas en 58 archivos de suite, 0 fallos, 0 errores, 0 omisiones** (`TEST_EXIT=0`).

## Archivos creados o modificados en P7

- Nuevos (`src/main/java/...`): `service/RespaldoException.java`, `service/RestauracionException.java`, `service/CifradoRespaldo.java`, `service/PoliticaRetencionRespaldos.java`, `service/MotorBaseRespaldo.java`, `service/MotorPostgresRespaldo.java`, `service/MotorH2Respaldo.java`, `service/RespaldoService.java`, `dto/ManifiestoRespaldoDTO.java`, `dto/ResultadoRespaldoDTO.java`, `dto/ResultadoVerificacionRespaldoDTO.java`, `dto/ResultadoRestauracionDTO.java`, `dto/EstadoAlertaRespaldoDTO.java`.
- Modificados (`src/main`): `config/HibernateTransactions.java`, `config/DatabaseConfig.java`, `ComposicionAplicacion.java`, `controller/AdminController.java`, `resources/.../admin-view.fxml`.
- Pruebas (`src/test/java/...`): `service/CifradoRespaldoTest.java`, `service/RespaldoServiceTest.java`, `config/PostgresMigrationIT.java` (`RespaldoYRestauracionPostgres`).

## Pasos pendientes para cierre de P7

- Ejecutar la puerta PostgreSQL sobre una base desechable (`LATCH_TEST_DB_*` configuradas, nunca `latch_db` ni `latch_demo`):
  ```powershell
  ./mvnw.cmd -o -B verify -Ppostgres
  ```
- Ejecutar humo visual interactivo (`./mvnw.cmd javafx:run`): iniciar sesión como Propietario, abrir **Administración → Respaldo y restauración**, crear respaldo cifrado, verificar integridad con clave válida e inválida (`ERR-059`), y probar restauración en base desechable aislada con copia previa.
- Medir cronometraje `RNF-036` / `RNF-037` con conjunto de 2 GiB (1 GiB base + 1 GiB adjuntos) en SSD de referencia cuando se disponga del entorno de aceptación.
- Revisión humana **G6** sobre cifrado, puertas de exclusión, manejo de contraseñas y restauración.

**Qué invalidaría esta evidencia:** un fallo de `pg_dump`/`pg_restore` o de `hbm2ddl=validate` tras restaurar en `PostgresMigrationIT`, la retención de un archivo plano tras un fallo `ERR-059`/`ERR-055`, la sustitución del directorio de adjuntos o base destino cuando la restauración falla o existen recibos posteriores (`ERR-058`), o cualquier fuga de contraseñas en manifiestos o bitácora.
