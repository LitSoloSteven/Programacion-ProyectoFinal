# E1 · Línea Base, Entorno y Bloqueos — Latch (2026-11-25)

Documento de salida de la fase **E1 · Fijar la base** y arranque documental de **P0 · Línea base y contratos** según el [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md) y las [Reglas Canónicas](../AGENTES_Y_MODELOS.md).

**E1 cerrada el 24 de noviembre de 2026.** El candidato y su respaldo quedaron identificados, el registro de aceptación está preparado y la base PostgreSQL desechable se comprobó. El inventario local no mostró un Windows alterno utilizable. E4/P9 conserva la prueba en equipo independiente como pendiente.

## 1. Identificación del candidato y candidato estable de respaldo (E1-A)

| Elemento | Valor observado | Estado |
| --- | --- | --- |
| Rama activa | `feature/reportes-vista-previa` (sincronizada con `origin/feature/reportes-vista-previa`) | **Verificado** |
| SHA candidato | `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`) | **Verificado** |
| Candidato estable de respaldo | `main` en `c61187e0c2daf4e567db0cb728b3a5babb529085` (`c61187e` — `docs: actualizar comando del primer propietario (#17)`) | **Verificado** |
| Archivos sin rastrear | `docs/LATCH_PLAN_DE_CIERRE_2026-11-25.md` y carpeta nueva `docs/evidencia-cierre-2026-11-25/` (sin cambios en `src/`, `pom.xml` ni `docs/migrations/`) | **Verificado** |

### Commits en `main..HEAD` (para trazabilidad o rollback hacia `c61187e`)

1. `d09cdfe` — `chore: ignorar archivos locales de agentes en .gitignore`
2. `da68e45` — `feat(reportes): agregar vista previa JavaFX reutilizable e iconos`
3. `869e8af` — `feat(reportes): sanear reportes existentes, agregar 5 reportes y pruebas`
4. `9eea5b8` — `feat(ui): conectar vista previa y estandarizar botones en catalogos y cargos`
5. `1b8de3a` — `feat(ui): integrar vista previa y reportes en pagos, contratos, tasas y panel`

## 2. Entorno técnico, base desechable y equipo alterno (E1-A)

| Componente | Observación | Acción / Estado |
| --- | --- | --- |
| Sistema operativo | Windows 11 (`amd64`, locale `es_NI`, UTF-8) | **Verificado** |
| JDK | Eclipse Adoptium Temurin `21.0.12.1+1` (`C:\Users\stive\.jdks\temurin-21.0.12.1`) | **Verificado** |
| Maven Wrapper | `./mvnw.cmd` → Apache Maven `3.9.16` | **Verificado** |
| PostgreSQL 18 | Servicio `postgresql-x64-18` en estado `Running` | **Verificado** |
| `psql` CLI | Existe en `C:\Program Files\PostgreSQL\18\bin\psql.exe`, pero **no está en `PATH`** | **Verificado** (hallazgo operativo: añadir al `PATH` o invocar ruta completa) |
| `.env` local | Presente en la raíz del repo (no leído ni impreso) | **Verificado** |
| PostgreSQL desechable | Clúster independiente 18.6, `127.0.0.1:54472`, base `latch_test_e1`, rol `latch_e1_owner` | **Verificado** — conexión autenticada desde una segunda sesión, DDL dentro de transacción y rollback sin tabla residual |
| Variables `LATCH_TEST_DB_*` | Se cargan únicamente en el proceso que ejecuta la prueba. La contraseña sintética se recupera mediante DPAPI desde el directorio local indicado abajo. | **Preparadas** — sin variables permanentes ni datos de la aplicación |
| Windows alterno sin JDK | Sin inventario de Hyper-V disponible ni herramientas/servicios de VirtualBox o VMware detectados en la estación. No se identificó una VM Windows utilizable. | **No probado** para ENT-14 — probar el runtime fuera del IDE en la estación actual si sigue faltando el entorno alterno |
| Hora oficial y ejecución | 25 de noviembre, 13:00 Managua. Steven supervisa y los agentes ejecutan | **Confirmado por Steven** el 24 de noviembre |

### Reutilizar la base desechable en E2/E3

Ejecutar desde el repositorio en el mismo perfil de Windows, con acceso a loopback. El clúster y sus archivos están fuera del repositorio. Si se reinició el equipo o se detuvo el clúster, iniciarlo desde su directorio `data` con `pg_ctl` y comprobar el puerto antes de ejecutar el perfil.

```powershell
$latchE1Root = 'C:\Users\stive\.codex\visualizations\2026\11\18\01a1f011-9a1b-71f2-8612-ce206625c3c4\latch-e1-test-a15803e6'
$latchE1Port = [int](Get-Content -LiteralPath (Join-Path $latchE1Root 'port.txt'))
$latchE1Secure = Get-Content -LiteralPath (Join-Path $latchE1Root 'password.dpapi') | ConvertTo-SecureString
$env:LATCH_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:${latchE1Port}/latch_test_e1"
$env:LATCH_TEST_DB_USER = 'latch_e1_owner'
$env:LATCH_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $latchE1Secure).Password
try {
    ./mvnw.cmd verify -Ppostgres
} finally {
    Remove-Item Env:LATCH_TEST_DB_URL, Env:LATCH_TEST_DB_USER, Env:LATCH_TEST_DB_PASSWORD
}
```

## 3. Evidencia de verificación base y arranque (E1-A / E1-B)

- **Suite H2 automatizada (`./mvnw.cmd test -B`) sobre `1b8de3a`:** **Verificado** → **234 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas (`BUILD SUCCESS`)** en 45 clases de prueba.
  - Incluye arranque y wiring (`ComposicionAplicacionTest`: 2), FXML/controladores (`FxmlControladoresTest`: 1), CSS y contraste (`EstilosCssTest`: 1, `ContrasteTest`: 1), seguridad HTTP/Spring (`ApiSeguridadTest`: 16, `ApiFase1Test`: 12, `ApiLoginSpringTest`: 12, `ApiValidacionesSpringTest`: 6, `ApiIndicadoresSpringTest`: 1), cobranza e idempotencia (`ApiPagosTest`: 19, `CargoServiceTest`: 7, `ContratoServiceTest`: 6), tasas (`ApiTasasTest`: 10, `TipoCambioServiceTest`: 9), catálogos (`ApiCatalogosTest`: 9, `PropiedadServiceTest`: 5), reportes PDF y vista previa (`ReporteServiceTest`: 15, `VistaPreviaReporteTest`: 5), infraestructura/concurrencia (`InfraestructuraTest`: 14, `RelojServiceTest`: 4, `SesionUsuarioTest`: 6).
- **Entorno PostgreSQL de E1:** **Verificado**. `initdb` con SCRAM, creación de `latch_test_e1` mediante `psql`, cero tablas iniciales y comprobación `BEGIN; CREATE TABLE …; ROLLBACK` con el resultado de ausencia de la tabla igual a `t`. Se invalidaría la evidencia si cambia el destino o la conexión no es a la instancia desechable.
- **Perfil PostgreSQL de Maven (`./mvnw.cmd verify -Ppostgres`, `PostgresMigrationIT`):** **No probado en este cierre de E1**. Ejecutar en E2/E3 con el comando anterior.
- **Arranque interactivo de escritorio (`./mvnw.cmd javafx:run` o JAR con `-Dprism.order=sw`):** **No probado** (requiere sesión gráfica interactiva y base `latch_demo` conforme a [guía de demostración](../demo-docente.md)).
- **Datos sintéticos:** existe el guion de 7 pasos en [demo-docente.md](../demo-docente.md); los fixtures automatizados `D0`–`D4` y el servidor simulado `S1` están definidos en [el Plan de Implementación](../Latch_Plan_Implementacion_APA7_v3.md), pero no existen aún como generadores/servidor ejecutables en el repositorio (**No probado** / brecha).

## 4. Bloqueos y brechas priorizados por impacto (E1-B)

### P0 — Bloqueos operativos para E2–E4 (entrega 25 de noviembre)

1. **Perfil PostgreSQL por ejecutar:** el clúster está preparado. Cargar las variables de proceso y ejecutar `verify -Ppostgres` para obtener evidencia de las migraciones y la integración.
2. **Arranque JavaFX y humo crítico pendientes en `1b8de3a`:** validar manualmente `./mvnw.cmd javafx:run` (y variante `-Dprism.order=sw` ante posible fallo nativo de `glass.dll` documentado en [README.md](../../README.md)), los 7 pasos de [demo-docente.md](../demo-docente.md) y las 11 plantillas `.jrxml` en vista previa y guardado PDF.
3. **Entorno de prueba alterno:** ENT-14 sigue pendiente. Si E4 no dispone de otro entorno Windows, hacer la comprobación local fuera del IDE y conservar la prueba independiente en P9.
4. **Cambios P0 acordados:** la implementación y su evidencia siguen pendientes en P1/P4. Consultar las resoluciones vigentes antes de tocar seguridad, esquema o permisos.

### P1 / P2 — Brechas de alcance completo (`C1`–`C4`, paquetes `P1`–`P9`)

| Impacto | Paquete | Brecha observada en `1b8de3a` |
| --- | --- | --- |
| **Alto (P1)** | `P1` | Sin pantallas UI para gestión de usuarios (`UsuarioService` sin controller), cambio/restablecimiento de contraseña, parámetros (`ConfiguracionService` sin UI/historial) ni consulta filtrada de auditoría (`AuditoriaService`). Falta script de instalación limpia desde base vacía sin `update`. |
| **Alto (P1)** | `P2` | Garantías (`GarantiaService`) solo tienen ajuste de saldo; faltan movimientos tipados (deducción, aplicación a deuda, devolución, liquidación al cierre). En contratos falta renovación enlazada y cierre atómico con liquidación. |
| **Alto (P1)** | `P3` | Faltan automatizaciones `H1` (rentas mensuales idempotentes) y `H2` (mora sin capitalización y vencimientos), además de pruebas de 2 JVM y 100 recibos. |
| **Alto (P1)** | `P4` | Falta cliente HTTPS externo para tipo de cambio, servidor simulado `S1`, caché/antigüedad y actualización automática `H3`. |
| **Alto (P1)** | `P5` | Módulo `M08` Mantenimiento inexistente (sin entidad, servicio, migración ni UI). Faltan contacto de emergencia, adjuntos de inquilino/contrato, foto de unidad y logotipo. |
| **Medio-Alto (P2)** | `P6` | Faltan 3 reportes de catálogo (`REP-04` Ingresos y gastos, `REP-06` Contratos por vencer, `REP-09` Mantenimiento), falta exportación **XLSX** numérica para `REP-02` a `REP-10`, y falta tarea `H5` cancelable. |
| **Alto (P1)** | `P7` | Módulo de respaldo y restauración cifrada (`AES-256-GCM`, manifiesto `SHA-256`, modo mantenimiento) inexistente. |
| **Medio (P2)** | `P8` | Faltan indicadores avanzados del panel con detalle filtrado, preferencia de tema por cuenta, modo sin efectos y coordinador `H6` de cierre ordenado. |
| **Alto (P1)** | `P9` | Faltan generadores/pruebas de rendimiento `D4`/`V1` (10.000 cargos, 30 mediciones p95), empaquetado `app-image` ZIP con runtime, prueba en equipo alterno sin JDK y ensayo de 20 minutos. |

## 5. Discrepancias contractuales de P0 y resolución

El 24 de noviembre Steven aprobó explícitamente Argon2id y delegó las demás decisiones. La resolución completa y vigente está en el [plan de cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md). La tabla conserva el contraste con el código de la línea base y el destino de cada cambio.

| # | Tema | Regla / Código vigente actual | Documento PRD / Plan | Resolución |
| --- | --- | --- | --- | --- |
| **D-P0-1** | Hash de contraseñas | `PBKDF2-HMAC-SHA256`, 600.000 iteraciones, sal 16 bytes ([SEGURIDAD.md](../SEGURIDAD.md), `PasswordHasher`) | `Argon2id` mínimo 19 MiB, 2 iteraciones, paralelismo 1 (`RF-M01-03`, `RNF-005`) | Migración gradual a Argon2id en P1, aprobada por Steven. |
| **D-P0-2** | Expiración de sesión | Token Bearer con expiración **fija** de 15 minutos (`TokenService`, [SEGURIDAD.md](../SEGURIDAD.md)) | Cierre tras 15 minutos de **inactividad** (`RF-M01-07`) | Conservar la vida fija del token e implementar y comprobar inactividad de JavaFX por separado. |
| **D-P0-3** | Instalación en base vacía | Hibernate `LATCH_DB_SCHEMA_MODE=update` inicial + `V1`–`V10` y luego `validate` ([README.md](../../README.md), [demo-docente.md](../demo-docente.md)) | Instalación de base vacía mediante migraciones SQL puras y `validate` sin `ddl` automático (`ENT-03`, `PLAN L1202`) | Preparar esquema inicial SQL y conservar V1–V10 en P1. |
| **D-P0-4** | Alcance USD/NIO + `S1`, variante LAN y roles | API estricta en `127.0.0.1` ([SEGURIDAD.md](../SEGURIDAD.md)); Asistente opera catálogos con `OPERAR_ALQUILERES` (`ApiCatalogosTest`) | Variante LAN hasta 3 sesiones (`PRD RNF`), `S1` para tasas externas, y PRD Tabla 7 restringe altas/bajas de propiedades/unidades al Asistente | Tasa informativa externa con S1, API en loopback incluso en LAN y permisos del Asistente por la Tabla 7. |

## 6. Cierre de E1 y relevo

- **Registro maestro de cobertura:** [registro_aceptacion.md](registro_aceptacion.md) (354 filas: 126 `RF`, 38 `RN`, 60 `RNF`, 100 `CP`, 10 `REC`, 20 `ENT`).

```text
Relevo E1 → E2 / P0:
- Candidato: rama feature/reportes-vista-previa en 1b8de3a (respaldo estable: main c61187e).
- Verificación ejecutada: ./mvnw.cmd test -B → 234 pruebas verdes (0 fallos/errores/omitidas).
- Artefactos creados: docs/evidencia-cierre-2026-11-25/E1_linea_base_y_bloqueos.md y registro_aceptacion.md (354 filas).
- Entorno E1: PostgreSQL 18.6 aislado, latch_test_e1 en 127.0.0.1:54472; conexión y rollback verificados.
- Corte confirmado: 25 de noviembre, 13:00 Managua; agentes ejecutores y Steven como único responsable humano.
- Pendientes E2/E3: perfil Maven PostgreSQL y humo JavaFX/PDF. ENT-14 permanece en E4/P9.
```
