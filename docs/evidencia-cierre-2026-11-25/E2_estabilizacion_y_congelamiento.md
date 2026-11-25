# E2 · Estabilización, Auditoría y Congelamiento — Latch (2026-11-25)

Documento de salida de la fase **E2 · Estabilizar y congelar** según el [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md) y las [Reglas Canónicas](../AGENTES_Y_MODELOS.md).

**Cierre ratificado sobre `1b8de3a`.** La revisión cotejó las referencias Git, la huella del JAR, las rutas del classpath y las salidas XML de Surefire y Failsafe. También contó 11 plantillas, 8 controladores con acceso a vista previa y 354 identificadores únicos en el registro de aceptación. Se reutilizó la ejecución automatizada existente del mismo candidato. El arranque gráfico, los recorridos de la demo y la comprobación interactiva de los PDFs corresponden a E3.

## 1. Decisión de congelamiento del candidato

| Elemento | Valor observado | Estado |
| --- | --- | --- |
| Rama congelada | `feature/reportes-vista-previa` (sincronizada con `origin/feature/reportes-vista-previa`) | **Verificado** |
| SHA congelado | `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`) | **Verificado** |
| Respaldo estable | `main` en `c61187e0c2daf4e567db0cb728b3a5babb529085` (`c61187e`) | **Verificado** |
| Correcciones bloqueantes en E2 | Ninguna requerida (`0` fallos en H2 y PostgreSQL 18.6; sin cambios en `src/`, `pom.xml` ni `docs/migrations/`) | **Verificado** |
| Artefacto JAR (`1b8de3a`) | `target/ProyectoFInal-ProgramacionAppEscritorio-1.0-SNAPSHOT.jar` (`648.201` bytes, SHA-256 `F805134E61278DE5519C8FABC13A23D494E7985E045233696C4D48BE45C075D1`) | **Verificado** |

Las decisiones `Operativa` y `D-P0-1`..`D-P0-4` (incluyendo Argon2id, inactividad JavaFX, esquema inicial SQL, `S1` y Tabla 7) quedan reservadas para los paquetes `P1`–`P9` tras la entrega inmediata, conforme al [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md).

## 2. Evidencia de flujo crítico en PostgreSQL desechable (E2-A)

- **Clúster desechable revalidado:** PostgreSQL `18.6` en `127.0.0.1:54472`, base `latch_test_e1`, usuario `latch_e1_owner` (variables `LATCH_TEST_DB_*` cargadas solo en memoria durante la ejecución mediante DPAPI y removidas en `finally`).
- **Comando ejecutado:** `./mvnw.cmd verify -Ppostgres -B` sobre `1b8de3a`:
  - **Surefire (H2):** **234 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
  - **Failsafe (PostgreSQL 18.6):** **37 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas** (`ApiPagosTest`: 19, `PostgresMigrationIT`: 4, `InfraestructuraTest`: 14), `BUILD SUCCESS`.
  - **Limpieza posterior de `latch_test_e1`:** verificada vía `psql.exe` (`0` tablas en `public`, `to_regprocedure('public.fn_reservar_numero()')` = `f`).

### Cobertura del flujo crítico al cierre de E2

| Tramo del flujo crítico | Evidencia en `1b8de3a` | Estado | Paso exacto pendiente (si aplica) |
| --- | --- | --- | --- |
| Login, bloqueo 5 fallos, revocación y roles | H2: `ApiFase1Test`, `ApiSeguridadTest`, `ApiLoginSpringTest` e `InfraestructuraTest`. PostgreSQL: `InfraestructuraTest`. | **Verificado** (API/Servicio) / **Inferido** (UI) | Ejecutar login en `PAN-01` JavaFX en E3 ([demo-docente.md](../demo-docente.md) paso 1) |
| Catálogos, propiedades, unidades, inquilinos | H2: `ApiCatalogosTest`, `PropiedadServiceTest` e `InfraestructuraTest`. PostgreSQL: `InfraestructuraTest`. | **Verificado** (API/Servicio) / **Inferido** (UI) | Ejecutar pasos 2–3 de [demo-docente.md](../demo-docente.md) en E3 |
| Contratos, ocupación única (`V6`) y garantías | `ContratoServiceTest`, `PostgresMigrationIT.indiceParcialDeContratosSoloRestringeOcupacionesActivas` (`uq_contrato_activo`) | **Verificado** | Ejecutar paso 4 de [demo-docente.md](../demo-docente.md) en E3 |
| Pagos, conversión autorizada, idempotencia (`V5`/`V7`), recibo (`V8`), anulación y auditoría (`V10`) | `ApiPagosTest` (19/19 en H2 y PG 18.6), `PostgresMigrationIT` (4/4), trigger de rollback de auditoría | **Verificado** | Ejecutar pasos 5–7 de [demo-docente.md](../demo-docente.md) en E3 |
| Arranque gráfico JavaFX completo | `ComposicionAplicacionTest`, `FxmlControladoresTest`, `EstilosCssTest`, `ContrasteTest` | **Inferido** | Arrancar fuera del IDE (`Launcher` o `-Dprism.order=sw`) en E3/E4 |

## 3. Auditoría de reportes existentes y navegación (E2-B)

### Inventario técnico de las 11 plantillas `.jrxml` y los 8 controladores UI

- Los 8 controladores con reportes (`CargoController`, `ContratoController`, `DashboardController`, `InquilinoController`, `PagoController`, `PropiedadController`, `TasaController`, `UnidadController`) invocan `VistaPreviaReporte.generarYMostrar(...)` en hilo de fondo y abren `vista-previa-reporte.fxml` (`VistaPreviaReporteController`) con exportación PDF atómica (`VistaPreviaReporte.exportarPdfAtomico` — probada en `VistaPreviaReporteTest` con 5/5 pruebas verdes).
- `ReporteServiceTest` (15/15 verdes) verifica compilación, encabezado `%PDF-`, separación de totales `USD` y `NIO` sin mezclar monedas, exclusión de pagos anulados en netos, marca visible `ANULADO` en `recibo.jrxml` sin alterar consecutivo, y comprobación de `Permiso.CONSULTAR_REPORTES` dentro de la transacción.
- **Brechas declaradas por reporte (`REP-01` a `REP-10` frente a PRD Tabla 68):**
  - `REP-01` (Recibo): `recibo.jrxml` **Verificado** en PDF; falta humo manual de vista previa/guardado en E3.
  - `REP-02` (Estado de cuenta): `estado_cuenta.jrxml` **Verificado** en PDF; falta `XLSX` numérico (P6).
  - `REP-03` (Cartera vencida): `cargos_cartera.jrxml` **Verificado** en PDF (separa saldo y mora USD/NIO); faltan tramos de antigüedad 1–30/31–60/61–90/+90 y `XLSX` (P6).
  - `REP-04` (Ingresos y gastos): **No probado** — sin plantilla ni servicio (P6 tras P2/P5).
  - `REP-05` (Ocupación): `contratos_ocupacion.jrxml` **Verificado** en PDF; falta conciliación de filtros por propiedad/tipo y `XLSX` (P6).
  - `REP-06` (Contratos por vencer): **No probado** — sin plantilla dedicada ni `H2` (P3/P6).
  - `REP-07` (Historial de pagos): `pagos_tabla.jrxml` **Inferido** en PDF; falta filtro específico por inquilino, columna de motivo de anulación y `XLSX` (P6).
  - `REP-08` (Garantías): `garantias_depositos.jrxml` **Verificado** en PDF; falta detalle de movimientos de deducción/devolución (P2) y `XLSX` (P6).
  - `REP-09` (Mantenimiento): **No probado** — bloqueado por módulo M08 (P5/P6).
  - `REP-10` (Tasas utilizadas): `tasas_cambio.jrxml` **Inferido** en PDF; falta cruce por pago/fuente externa `H3` y `XLSX` (P4/P6).
  - Plantillas operativas adicionales existentes para la UI: `propiedades_unidades.jrxml`, `unidades.jrxml`, `inquilinos.jrxml`, `dashboard_resumen.jrxml` (**Verificadas** en PDF vía `ReporteServiceTest`).

## 4. Auditoría por muestreo aleatorio de `registro_aceptacion.md` (Puerta final)

En cumplimiento del control de invalidez de evidencia del [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md), se ejecutó una selección aleatoria estratificada de **18 filas** (`seed=20261125`) sobre las 354 filas de [registro_aceptacion.md](registro_aceptacion.md), verificando directamente el código y las aserciones (`assert*`) de cada prueba citada en `src/test/java/`:

| ID | Estado inicial | Prueba / evidencia inspeccionada | Resultado de auditoría y estado final |
| --- | --- | --- | --- |
| `RF-M01-01` | Verificado | `ApiFase1Test.loginCorrectoEmiteTokenSinTocarSesionGlobal` (L79–90), `ApiLoginSpringTest`, `InfraestructuraTest` | Cobertura parcial (solo backend/API; falta ingreso en `PAN-01` JavaFX) → ajustado a **Inferido** |
| `RF-M05-09` | Verificado | `CargoServiceTest` (`asistenteNoPuedeAnular...` L121–130, `propietarioAnula...` L133–141, `noPermiteCambiarImporte...` L144–169) | Aserciones cubren todo el criterio → **Verificado** confirmado |
| `RF-M06-10` | Verificado | `ReporteServiceTest.reciboVigenteContieneNumeroMontosYSinMarcaAnulado` (L276–293) y `reciboAnuladoIncluyeMarcaVisibleAnuladoYNoAlteraConsecutivo` | Aserciones cubren número, montos y marca `VIGENTE`/`ANULADO` → **Verificado** confirmado |
| `RF-M09-09` | Verificado | `ApiPagosTest.convierteConTasaAutorizadaYFallaSinTasa` (L564–573: `422 ENTIDAD_NO_PROCESABLE`, `count(Pago)==1`) | Aserciones cubren el rechazo sin tasa → **Verificado** confirmado |
| `RN-08` | Verificado | `ApiPagosTest.soloElPropietarioAnulaYQuedaAuditado` (L602–635: `403`, `422`, `200`, `409`, `PAGO_ANULADO`, recibo `anulado=true`) | Aserciones cubren inmutabilidad y anulación → **Verificado** confirmado |
| `RN-01` | Verificado | `ContratoServiceTest.impideActivarOGuardarUnSegundoContratoActivoParaLaUnidad` (L123–136) + `PostgresMigrationIT` (`V6`) | Confirmado en H2 y PostgreSQL 18.6 → **Verificado** confirmado |
| `RNF-032` | Verificado | `RelojServiceTest.fechaOperativaYPresentacionUsanManagua` (L57–62) y `ApiSeguridadTest.presentaElInstanteDeTasaEnZonaManagua` (L144–149) | Cobertura parcial (no mide 20 eventos de auditoría) → ajustado a **Inferido** |
| `RNF-017` | Verificado | `ApiPagosTest.convierteConTasaAutorizadaYFallaSinTasa` (L547–562: `36624.30 NIO -> 1000.00 USD`, crédito `1000.00`) | Cobertura parcial (1 caso sin múltiples aplicaciones parciales) → ajustado a **Inferido** |
| `CP-010` | Verificado | `ApiCatalogosTest.flujoCompletoDeCatalogos` (L165–167) y `rechazaExponenteExtremoSinCrearUnidad` (L247–265) | Cobertura parcial (solo prueba `Apartamento`; faltan `Casa`, `Cuarto`, `Local comercial` y canon `0`/negativo) → ajustado a **Inferido** |
| `CP-067` | Verificado | `ReporteServiceTest` (`reciboVigente...` y `reciboAnulado...`) | Cobertura parcial (solo PDF en servicio; falta vista previa/guardado UI) → ajustado a **Inferido** |
| `CP-030` | Verificado | `ApiPagosTest.convierteConTasaAutorizadaYFallaSinTasa` (L549–551 envía `"NIO"` sobre contrato `"USD"`) | Prueba cubría NIO→USD (`CP-029`), no USD→NIO (`CP-030`) → ajustado a **No probado** |
| `CP-071` | Verificado | `ReporteServiceTest.estadoCuentaAgrupaCargosYPagosYMuestraTotalesYGarantiaEnMonedaUnica` (L384–423) | Cobertura parcial (solo PDF en memoria; falta vista previa y `XLSX` `REP-02`) → ajustado a **Inferido** |
| `CP-076` | Inferido | `ReporteServiceTest.tasasCambioFormateaOchoDecimalesYMuestraEstadosYPermisos` (L452–476) | Cobertura parcial confirmada → **Inferido** |
| `RF-M10-07` | Inferido | `ReporteServiceTest.pagosReporteUsaEncabezadosEstaticosYExcluyeAnuladosDelNetoPorMoneda` (L94–116) | Cobertura parcial confirmada → **Inferido** |
| `RN-35` | Inferido | `EstilosCssTest`, `ContrasteTest` | Cobertura parcial confirmada → **Inferido** |
| `CP-020` | Inferido | `ContratoServiceTest.contratoCanceladoNoPuedeActivarse` | Cobertura parcial confirmada → **Inferido** |
| `RNF-003` | No probado | `H1` inexistente en `1b8de3a` | Brecha confirmada → **No probado** |
| `RNF-016` | No probado | `H1` inexistente en `1b8de3a` | Brecha confirmada → **No probado** |

Con estas correcciones más las actualizaciones de PostgreSQL 18.6 (`verify -Ppostgres`), el registro queda en **86 Verificado**, **82 Inferido** y **186 No probado** (354 filas totales).

## 5. Instrucciones reproducibles de empaquetado y arranque fuera del IDE (para E3 / E4)

Se verificó el JAR y que las entradas del classpath runtime existan. Los comandos de arranque quedan como pasos de E3/E4:

```powershell
# 1. Empaquetar JAR (generado tras verify -Ppostgres) y calcular classpath runtime
$runtimeFile = Join-Path (Get-Location) 'target\latch-runtime.txt'
./mvnw.cmd org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath '-DincludeScope=runtime' "-Dmdep.outputFile=$runtimeFile"
$jar = (Resolve-Path 'target\ProyectoFInal-ProgramacionAppEscritorio-1.0-SNAPSHOT.jar').Path
$cp = "$jar;$((Get-Content -Raw -LiteralPath $runtimeFile).Trim())"
$paquete = 'ni.edu.uam.proyectofinalprogramacionappescritorio'

# 2. Huella SHA-256 observada del JAR en 1b8de3a:
#    F805134E61278DE5519C8FABC13A23D494E7985E045233696C4D48BE45C075D1

# 3. Arranque fuera del IDE (con .env o variables LATCH_DB_* / LATCH_API_PORT configuradas en base latch_demo)
java -cp $cp "$paquete.Launcher"
# Variante ante fallo nativo en glass.dll al abrir vista previa/recibo:
java '-Dprism.order=sw' -cp $cp "$paquete.Launcher"

# 4. Arranque independiente solo API REST (en ejecución aparte y puerto libre)
java -cp $cp "$paquete.herramientas.IniciarApi"
```

- **Preparación de `latch_demo`:** seguir [demo-docente.md](../demo-docente.md) usando `C:\Program Files\PostgreSQL\18\bin\psql.exe` (añadiendo su carpeta al `PATH`).
- **Pendiente para E4 / P9:** `ENT-14` (prueba en Windows limpio sin JDK) sigue marcada como pendiente si no se dispone de una VM o equipo alterno; en ese escenario se verifica el paquete con su runtime fuera del IDE en la estación actual y se documenta la brecha.

## 6. Brechas declaradas para paquetes de cierre (P1–P9) y relevo E2 → E3

- Quedan declaradas para cierre posterior al 25 de noviembre las brechas `P1`–`P9` detalladas en [E1_linea_base_y_bloqueos.md](E1_linea_base_y_bloqueos.md) y [LATCH_PLAN_DE_CIERRE_2026-11-25.md](../LATCH_PLAN_DE_CIERRE_2026-11-25.md), incluyendo la implementación de `D-P0-1` (Argon2id en P1), `D-P0-2` (inactividad en JavaFX), `D-P0-3` (script SQL inicial), `D-P0-4` (cliente `S1`/HTTPS en P4 y permisos de Tabla 7 en P1), `M08`, `H1`–`H6`, respaldo `AES-256-GCM` y exportación `XLSX`.

```text
Relevo E2 → E3:
- Candidato congelado: rama feature/reportes-vista-previa en 1b8de3a (respaldo: main c61187e).
- Puerta PostgreSQL verificada: ./mvnw.cmd verify -Ppostgres -B → 234 H2 + 37 PostgreSQL 18.6 (0 fallos);
  latch_test_e1 quedó limpia (0 tablas, fn_reservar_numero() = f).
- Artefacto JAR: 648.201 bytes, SHA-256 F805134E61278DE5519C8FABC13A23D494E7985E045233696C4D48BE45C075D1;
  target/latch-runtime.txt generado correctamente (9.282 bytes).
- Auditoría E2: 11 .jrxml y 8 controladores revisados; muestra de 18 filas (seed=20261125) ajustada en
  registro_aceptacion.md (86 Verificado, 82 Inferido, 186 No probado).
- Siguiente fase E3 (10:00–11:30): puerta de uso manual en JavaFX (demo-docente.md, roles, Claro/Oscuro,
  vista previa y guardado PDF sobre 1b8de3a).
```
