# C3 · P8 · Panel, temas y cierre (M01, M11 y M12)

Rama de trabajo `feature/p1-base-seguridad-admin`, base `1b8de3a`, con los cambios acumulados de C1, C2 (P2–P5) y C3 (P6–P8) sin commit. Sin cambios en `pom.xml`, `AGENTS.md`, `docs/AGENTES_Y_MODELOS.md` ni nuevas migraciones de base de datos (`V0`–`V16`).

## Resumen de verificación

| Criterio | Evidencia | Estado |
| --- | --- | --- |
| `RF-M11-01` · Ocupación actual con instante de generación | `DashboardService.obtenerMetricas` calcula `unidadesActivas`, `unidadesOcupadas`, `porcentajeOcupacion` (excluyendo `Inactiva` del denominador y `0.00` cuando no hay activas) e `instanteGeneracion` desde el reloj de BD; `dashboard-view.fxml` / `DashboardController` muestran el corte sin selector de fecha histórica; verificado en `DashboardServiceTest` (4/4) y `ApiIndicadoresSpringTest` (1/1). | **Verificado** (H2 / API) |
| `RF-M11-02`, `RF-M11-03` · Cobrado/por cobrar por moneda y período; mora exigible separada | `DashboardService.obtenerMetricas(YearMonth)` y `GET /api/v1/dashboard/metricas?periodo=AAAA-MM` separan `USD` y `NIO` y distinguen `moraExigible` de `principalPendiente`; `DashboardController` integra `cmbPeriodoDashboard`; verificado en `DashboardServiceTest` y `ApiIndicadoresSpringTest` (incluye `400` ante período inválido). | **Verificado** (H2 / API) |
| `RF-M11-04`, `RF-M11-05` · Contratos por vencer (H2) y tasa disponible | `MetricasDashboardDTO` / `MetricasResponse` exponen `contratosPorVencer`, `contratosVencidos` y `ResumenTasaDashboardDTO` / `TasaResumenResponse` (`AUTORIZADA`, `INFORMATIVA`, `HISTORICA` o `"Sin tasa disponible"` con origen, fecha efectiva y antigüedad); verificado en `DashboardServiceTest` y `ApiIndicadoresSpringTest`. | **Verificado** (H2 / API) |
| `RF-M11-06`, `RF-M11-10` · Navegación lateral y apertura filtrada desde indicadores | `DashboardController` abre `contrato-view.fxml` (`"Vigente"` / `"Por vencer"`), `cargo-view.fxml` (`"Mora"` o período), `pago-view.fxml` y `unidad-view.fxml` desde tarjetas KPI accesibles por ratón y teclado (`Enter`/`Space`), usando `aplicarFiltroEstadoInicial` / `aplicarFiltroInicial`; verificado en `FxmlControladoresTest`. | **Verificado** (estático) / **No probado** (humo visual interactivo) |
| `RF-M01-12`..`RF-M01-15`, `RF-M11-07`..`RF-M11-09`, `RN-35`..`RN-38`, `RNF-047`..`RNF-060` · Temas Claro/Oscuro, preferencias por ID opaco y sin efectos | `AlmacenPreferencias` (`ni/edu/uam/latch/presentation` con `lastTheme`, `lastSinEfectos`, `users/<uuid>/theme` y `users/<uuid>/sinEfectos`, 0 nombres/cédulas), `TemaAplicacion` (fallback Claro en 6 casos, 30 cambios < 500 ms preservando 20 campos y `focusOwner`, 0 hilos adicionales, aviso no bloqueante sin revertir sesión), `estilos.css` (Segoe UI, Claro/Oscuro en login y módulos, foco 2 px, `.no-effects`/`.aurora-layer`), `LoginController` y `DashboardController`; verificado en `TemaAplicacionTest` (4/4), `EstilosCssTest` (1/1), `ContrasteTest` (1/1), `FxmlControladoresTest` (1/1) y `FormulariosFeedbackTest` (4/4). | **Verificado** (H2 / JavaFX unitario) / **No probado** (inspección visual en monitor de 12 módulos) |
| `RF-M01-07` (`D-P0-2`, `FLU-05`) · Cierre de sesión por inactividad | `MonitorInactividad` lee `minutos_inactividad` (5–60 min, defecto 15), emite aviso previo a 60 s (`Notificaciones.mostrarAdvertencia`), se reinicia con interacción local sin extender el token Bearer de 15 min y revoca/cierra sesión al vencer usando `Timeline` en el hilo JavaFX (0 hilos nuevos); verificado en `MonitorInactividadTest` (2/2). | **Verificado** (unitario) / **No probado** (humo interactivo en ventana) |
| `RF-M12-08`, `RN-30`, `RNF-022`, `RNF-023`, `ERR-056` / `MSG-061` · Cierre controlado H6 e instancia única H1/H2/H3/H5 | `RegistroTareaUnica` (`H1`, `H2`), `GestorH5Reportes`, `ActualizacionMoraH2`, `ActualizacionTasaH3`, `CobranzaService.revisarMoraYVencimientos*` con cancelación cooperativa, `TareasSegundoPlano.rechazarNuevas()` (respeta H4 en curso), `CoordinadorCierreH6` (espera hasta 10 s fuera del hilo FX, cierra recursos solo al liberar todo o devuelve `ERR-056`/`MSG-061` con tareas pendientes manteniendo proceso abierto) y diálogo en `HelloApplication`; verificado en `CoordinadorCierreH6Test` (4/4). | **Verificado** (H2 / unitario) / **No probado** (cierre manual en ventana con H1/H5 activo) |
| Puerta PostgreSQL (`./mvnw.cmd -o -B verify -Ppostgres`) | En esta sesión `LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD` no estaban configuradas (`PG_ENV_SET=False`). | **No probado** |

## Comandos ejecutados

1. Suite dirigida de P8 (tras la última edición):
   Los 11 archivos de prueba de P8 ejecutados dentro de la corrida final suman **55 pruebas, 0 fallos, 0 errores, 0 omisiones**:
   `DashboardServiceTest` (4), `ApiIndicadoresSpringTest` (1), `TemaAplicacionTest` (4), `MonitorInactividadTest` (2), `CoordinadorCierreH6Test` (4), `FxmlControladoresTest` (1), `EstilosCssTest` (1), `ContrasteTest` (1), `FormulariosFeedbackTest` (4), `ReporteServiceTest` (26), `CobranzaH1H2ServiceTest` (7).
2. Suite completa H2:
   ```powershell
   ./mvnw.cmd -o -B test -q
   ```
   Resultado: **333 pruebas ejecutadas en 61 archivos de suite, 0 fallos, 0 errores, 0 omisiones** (`TEST_EXIT=0`).
3. Higiene de formato:
   ```powershell
   git diff --check
   ```
   Resultado: salida limpia (`GIT_DIFF_CHECK_EXIT=0`).

## Archivos creados o modificados en P8

- Nuevos (`src/main/java/...`): `dto/ResumenTasaDashboardDTO.java`, `api/indicadores/dto/TasaResumenResponse.java`, `api/pagos/ActualizacionMoraH2.java`, `util/AlmacenPreferencias.java`, `util/MonitorInactividad.java`, `util/RegistroTareaUnica.java`, `util/TareasActivasException.java`, `util/CoordinadorCierreH6.java`.
- Modificados (`src/main`): `dto/MetricasDashboardDTO.java`, `api/indicadores/dto/MetricasResponse.java`, `api/indicadores/RutasIndicadores.java`, `api/pagos/ServicioPagos.java`, `api/tasas/ActualizacionTasaH3.java`, `api/piloto/PilotoLoginApplication.java`, `service/DashboardService.java`, `service/ConfiguracionService.java`, `service/CobranzaService.java`, `util/TemaAplicacion.java`, `util/Notificaciones.java`, `util/GestorH5Reportes.java`, `util/TareasSegundoPlano.java`, `HelloApplication.java`, `controller/LoginController.java`, `controller/DashboardController.java`, `controller/ContratoController.java`, `controller/CargoController.java`, `controller/PagoController.java`, `controller/UnidadController.java`, `resources/.../login-view.fxml`, `resources/.../dashboard-view.fxml`, `resources/.../estilos.css`.
- Pruebas (`src/test/java/...`): `util/TemaAplicacionTest.java`, `util/MonitorInactividadTest.java`, `util/CoordinadorCierreH6Test.java`, y ampliaciones en `service/DashboardServiceTest.java` y `api/piloto/ApiIndicadoresSpringTest.java`.
- Documentación: `docs/API_Latch.md`, `docs/LATCH_PLAN_DE_CIERRE_2026-11-25.md`, `docs/evidencia-cierre-2026-11-25/C3_P8_panel_temas_y_cierre.md`.

## Pasos pendientes para cierre de P8

- Ejecutar la puerta PostgreSQL sobre una base desechable (`LATCH_TEST_DB_*` configuradas, nunca `latch_db` ni `latch_demo`):
  ```powershell
  ./mvnw.cmd -o -B verify -Ppostgres
  ```
- Ejecutar humo visual interactivo (`./mvnw.cmd javafx:run`):
  1. Alternar Claro/Oscuro y `"Sin efectos"` en acceso (`login-view.fxml`), autenticar con dos cuentas distintas y comprobar la restauración personal por usuario.
  2. Cambiar tema con un formulario editado abierto y verificar preservación de campos y foco, además de navegación por teclado (`Tab`, `Shift+Tab`, `Enter`, `Escape`) en Claro/Oscuro con efectos activos e inactivos.
  3. Probar selector de período en el panel principal y apertura de vistas filtradas desde los 9 indicadores KPI.
  4. Comprobar aviso previo de 60 s y cierre por inactividad, y cierre de ventana (`H6`) con tarea activa > 10 s (`ERR-056` / `MSG-061`).
- Revisión humana **G6** sobre inactividad de sesión (`D-P0-2`), manejo de preferencias y cierre controlado H6.

**Qué invalidaría esta evidencia:** pérdida de entradas o foco al cambiar de tema, almacenamiento de nombres o cédulas en `Preferences`, extensión del token Bearer tras interacción local, cierre abrupto de `SessionFactory`/`HttpClient` durante un pago H4 o una tarea H1/H2/H3/H5 activa, o fallo en `verify -Ppostgres` sobre base desechable.
