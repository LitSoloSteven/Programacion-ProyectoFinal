# E4 · Empaquetado y Entrega del Candidato — Latch (2026-11-25)

Documento de salida de la fase **E4 · Empaquetar y entregar** según el [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md) y las [Reglas Canónicas](../AGENTES_Y_MODELOS.md).

> [!IMPORTANT]
> **Resultado de la entrega:** **Entrega del candidato del día con brechas declaradas** (listo para revisión humana, humo visual en monitor, etiqueta y entrega de Steven antes de las **13:00** hora de Managua). El proyecto permanece abierto para cerrar los paquetes `P1`–`P9` tras la entrega inmediata.

**Auditoría del paquete final:** las dos copias pesan `110.065.894` bytes y su SHA-256 coincide con el manifiesto Markdown y su archivo de texto. En una extracción nueva se comprobaron los dos binarios internos, diez migraciones, 95 JARs, los 94 terceros inventariados y el classpath del ejecutable. Una sonda ejecutada con el Java embebido, `JAVA_HOME` vacío y sin Java en el `PATH` encontró `Launcher` y las clases JavaFX de aplicación, controles y `FileChooser`, y compiló `recibo.jrxml` con JasperReports. La sonda no abrió una ventana. El arranque gráfico en el equipo de exposición y ENT-14 conservan sus estados pendientes.

## 1. Resumen ejecutivo e identificación del candidato

| Elemento | Valor verificado |
| --- | --- |
| Rama y SHA congelados | `feature/reportes-vista-previa` · `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`) |
| Respaldo estable | `main` · `c61187e0c2daf4e567db0cb728b3a5babb529085` (`c61187e`) |
| Paquete ZIP (Copia 1) | `target/entrega-latch-1b8de3a/Latch-1.0.0-win64-1b8de3a.zip` (`110.065.894` bytes) |
| Paquete ZIP (Copia 2) | `C:\Users\stive\.gemini\antigravity\brain\0ac85865-a564-433f-a3fa-fb030058880b\entrega-copia-2\Latch-1.0.0-win64-1b8de3a.zip` (`110.065.894` bytes) |
| SHA-256 idéntico (Copia 1 y 2) | `7BFE6519726C8F1CD95020033E56055BD157D890F79EBB230D9E95B4DD884B6F` |
| Manifiesto y licencias | [MANIFIESTO_ENTREGA_1b8de3a.md](MANIFIESTO_ENTREGA_1b8de3a.md) · [LICENCIAS_TERCEROS.md](LICENCIAS_TERCEROS.md) (`94` JARs runtime) |
| Guion de demostración | [GUION_DEMO_20MIN_1b8de3a.md](GUION_DEMO_20MIN_1b8de3a.md) |
| Matriz conciliada | [registro_aceptacion.md](registro_aceptacion.md) (`354` filas totales) |

## 2. Evidencia consolidada E1–E4 sobre `1b8de3a`

- **E1 ([E1_linea_base_y_bloqueos.md](E1_linea_base_y_bloqueos.md)):** línea base fijada, entorno PostgreSQL 18.6 desechable (`127.0.0.1:54472 / latch_test_e1`) y matriz inicial de `354` filas.
- **E2 ([E2_estabilizacion_y_congelamiento.md](E2_estabilizacion_y_congelamiento.md)):** `./mvnw.cmd verify -Ppostgres -B` verde (`234` H2 + `37` PostgreSQL 18.6), auditoría de los `11` `.jrxml` / `8` controladores UI y muestreo aleatorio estratificado de `18` filas (`seed=20261125`).
- **E3 ([E3_verificacion_candidato.md](E3_verificacion_candidato.md)):** instalación completa desde JAR (`update` → `V1`–`V10` → `validate` → `CrearPropietarioInicial`), recorrido E2E HTTP sobre `IniciarApi` (`127.0.0.1:48159`), generación/renderizado/exportación atómica de los `12` PDFs (`11` `.jrxml`, `0` `.tmp` residuales) y segunda corrida completa de `./mvnw.cmd verify -Ppostgres -B` verde.
- **E4 (prueba limpia desde ZIP extraído con `JAVA_HOME=''`):**
  - Extraído a carpeta limpia fuera del repo y ejecutado exclusivamente con `Latch\runtime\bin\java.exe` (OpenJDK `21.0.12.1+1` embebido) y `Latch\app\*` (`95` JARs):
    - `verificar-bd.cmd` (`update`) → `V1`–`V10` → `verificar-bd.cmd` (`validate`) → `crear-propietario-inicial.cmd` (`Propietario inicial creado.`) → `IniciarApi` (`127.0.0.1:48160`) → `POST /auth/login` (`200`) → `GET /dashboard/metricas` (`200`) → `POST /auth/logout` (`204`) → `GET /dashboard/metricas` (`401`).
  - Limpieza posterior de `latch_test_e1` confirmada vía `PostgresMigrationIT` (`0` tablas en `public`, `fn_reservar_numero()` = `f`).

## 3. Inventario consolidado de brechas abiertas (entrega con brechas declaradas)

| Categoría | Brecha / Identificador | Responsable | Siguiente acción y evidencia faltante |
| --- | --- | --- | --- |
| **Humo visual en monitor (`E3`/`E4`)** | Arranque `iniciar-latch-sw.cmd` / `iniciar-latch.cmd` sobre `latch_demo`, teclado (`Tab`/`Shift+Tab`/`Enter`/`Escape`), Claro/Oscuro en `Stage` vivo, `FileChooser` y ensayo de `20` min (`REC-01`–`REC-10`, `ENT-15`) | Steven | Ejecutar en monitor antes de la exposición usando [GUION_DEMO_20MIN_1b8de3a.md](GUION_DEMO_20MIN_1b8de3a.md) y [demo-docente.md](../demo-docente.md). |
| **Equipo alterno (`ENT-14`)** | Prueba en segundo equipo Windows limpio sin JDK | Steven / P9 | No disponible en estación local durante E1–E4; ejecutar al disponer de VM o equipo alterno. |
| **Cierre humano (`ENT-02`/`ENT-20`)** | Revisión final de artefactos, etiqueta Git y publicación | Steven | Revisar Copia 1 y Copia 2, verificar huellas de [MANIFIESTO_ENTREGA_1b8de3a.md](MANIFIESTO_ENTREGA_1b8de3a.md) y etiquetar tras revisión. |
| **Paquete `P1` (`M01`, `M02`, `M03`, `M12`)** | Migración gradual a **Argon2id** (`D-P0-1`), script SQL base vacía (`D-P0-3`), UI de usuarios/cambio-restablecimiento de clave/parámetros/auditoría, bajas lógicas y permisos Tabla 7 (`D-P0-4`) | Sol (con Steven en revisión) | Implementar según [LATCH_PLAN_DE_CIERRE_2026-11-25.md](../LATCH_PLAN_DE_CIERRE_2026-11-25.md) tras la entrega. |
| **Paquete `P2` (`M04`, `M07`)** | Movimientos tipados de garantía (deducción, aplicación, devolución, liquidación), renovación enlazada y cierre atómico de contratos | Sol / Terra | Implementar tras `P1` y verificar rollback y liquidación parcial. |
| **Paquete `P3` (`M05`, `M06`)** | Hilos `H1` (rentas mensuales), `H2` (mora sin capitalización y vencimientos), crédito automático, `CP-030` (USD→NIO) y prueba `2` JVM / `100` recibos (`CP-041`) | Sol / Terra | Implementar tras `P2` y verificar en `latch_test_e1`. |
| **Paquete `P4` (`M09`)** | Cliente HTTPS USD/NIO, caché/antigüedad, `H3` y servidor simulado `S1` (`D-P0-4`) | Sol / Terra | Implementar tras `P0` e integrar con `P3`. |
| **Paquete `P5` (`M08`, adjuntos)** | Módulo `M08` Mantenimiento, contacto de emergencia, adjuntos (`5` MiB), foto de unidad y logotipo | Sol / Terra | Implementar tras `P1`. |
| **Paquete `P6` (`M10`)** | Exportación tabular `XLSX` (`REP-02`–`REP-10`), `REP-04`, `REP-06`, `REP-09`, tramos `REP-03`, filtro `REP-07`, cruce `REP-10` y tarea `H5` | Terra / Sol | Implementar tras `P2`–`P5`. |
| **Paquete `P7` (`M12`)** | Respaldo y restauración cifrada `AES-256-GCM` + `SHA-256` y modo mantenimiento | Sol | Implementar tras `P1`/`P5`. |
| **Paquete `P8` (`M01`, `M11`, `M12`)** | Cierre por inactividad JavaFX (`D-P0-2`), indicadores filtrados en panel, preferencia de tema por cuenta, modo sin efectos y coordinador `H6` | Terra / Sol | Implementar tras `P2`–`P6`. |
| **Paquete `P9` (`RNF`, `CP`, `REC`, `ENT`)** | Pruebas de volumen `D4` (`10.000` cargos, `p95`), `100` `CP`, `10` `REC`, `20` `ENT` y variante LAN TLS + SMB (`D-P0-4`) | Sol / Steven | Cierre completo del proyecto. |

## 4. Uso de la guía con el ZIP extraído

`docs/demo-docente.md` del ZIP conserva instrucciones para un clon del repositorio. Para usar el paquete sin JDK, abre PowerShell **en la raíz extraída**, donde están `iniciar-latch-sw.cmd`, `.env.example` y `migrations/`. Carga allí las variables de conexión de `latch_demo` indicadas en la guía o configura su `.env` local. PostgreSQL y `psql` siguen siendo necesarios para preparar la base.

- Si `latch_demo` ya está preparada, arranca directamente con `./iniciar-latch-sw.cmd` desde esa raíz. Utiliza la cuenta de demo existente.
- Para una `latch_demo` vacía, sigue el orden de la guía: ejecuta `./verificar-bd.cmd` con el modo `update`, aplica los SQL de **`./migrations`** en orden numérico con `psql`, cambia a `validate` y ejecuta `./verificar-bd.cmd` de nuevo. Con las variables de alta inicial preparadas según la guía, usa `./crear-propietario-inicial.cmd` una vez. Los tres lanzadores usan el Java incluido.
- Para el guion y sus enlaces usa la [copia del repositorio](GUION_DEMO_20MIN_1b8de3a.md). El ZIP auditado conserva su huella.

## 5. Lista de verificación final para Steven (antes de las 13:00)

1. Confirmar que **Copia 1** (`target/entrega-latch-1b8de3a/Latch-1.0.0-win64-1b8de3a.zip`) y **Copia 2** (`...\entrega-copia-2\Latch-1.0.0-win64-1b8de3a.zip`) están accesibles y conservan SHA-256 `7BFE6519726C8F1CD95020033E56055BD157D890F79EBB230D9E95B4DD884B6F`.
2. En la base `latch_demo` ([demo-docente.md](../demo-docente.md)), hacer humo gráfico rápido con `iniciar-latch-sw.cmd` siguiendo [GUION_DEMO_20MIN_1b8de3a.md](GUION_DEMO_20MIN_1b8de3a.md).
3. Revisar esta acta y el [registro de aceptación](registro_aceptacion.md), confirmar la huella del archivo que se va a entregar y etiquetar en Git el commit congelado `1b8de3a`. Declarar `P1`–`P9` y `ENT-14` como trabajo abierto, y marcar únicamente las casillas respaldadas por evidencia en la [puerta final](../LATCH_PLAN_DE_CIERRE_2026-11-25.md).

**Qué invalidaría la entrega técnica.** Que el SHA-256 del ZIP final difiera del manifiesto, o que al extraerlo en un Windows limpio sin JDK falle la carga gráfica de JavaFX o JasperReports desde `iniciar-latch-sw.cmd` o `Latch.exe`. El resultado local con el runtime embebido aporta evidencia de carga de clases y compilación Jasper. Para cerrar ENT-14 hace falta el arranque y recorrido gráfico en ese entorno independiente.
