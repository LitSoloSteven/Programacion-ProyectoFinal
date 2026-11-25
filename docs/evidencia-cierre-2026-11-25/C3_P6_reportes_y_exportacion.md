# C3 · P6 · Reportes y exportación (M10)

Rama de trabajo `feature/p1-base-seguridad-admin`, base `1b8de3a`, con los cambios acumulados de C1, C2 (P2–P5) y C3 (P6) sin commit. Sin cambios en `pom.xml` ni nuevas migraciones de base de datos (`V0`–`V16`).

## Resumen de verificación

| Criterio | Evidencia | Estado |
| --- | --- | --- |
| `REP-01` · `RF-M10-01` (`CP-067`) | `ReporteServiceTest.rep01ReciboSoloAdmitePdfYMuestraAvisoNoFiscalYTasaAplicada`, `reciboVigenteContieneNumeroMontosYSinMarcaAnulado`, `reciboAnuladoIncluyeMarcaVisibleAnuladoYNoAlteraConsecutivo`: incluye tasa aplicada en pagos multimoneda, cargos aplicados, crédito a favor y aviso no fiscal; rechaza exportación `XLSX` con `IllegalArgumentException`. | **Verificado** (H2) |
| `REP-02` · `RF-M10-05` (`CP-071`) | `ReporteServiceTest.rep02EstadoCuentaDesdeBasePorCorteGeneraPdfYXlsxNumerico`, `estadoCuentaAgrupaCargosYPagosYMuestraTotalesYGarantiaEnMonedaUnica`: consulta histórica al corte con movimientos, saldo, crédito y garantía en PDF y XLSX numérico. | **Verificado** (H2) |
| `REP-03` · `RF-M10-02` (`CP-068`) | `ReporteServiceTest.rep03CarteraVencidaClasificaTramosYExcluyeAnuladosEnPdfYXlsx` + `cargosCarteraSeparaSaldoYMoraEnUsdYNio`: corte, exclusión de anulados, tramos `Al día`, `1 a 30`, `31 a 60`, `61 a 90`, `Más de 90`, Principal vs Mora y separación USD/NIO en PDF (`cartera_vencida.jrxml`) y XLSX. | **Verificado** (H2) |
| `REP-04` · `RF-M10-03` (`CP-069`) | `ReporteServiceTest.rep04IngresosYGastosSeparaRentasOtrosGastosYGarantiasYExcluyeAnulados`: separa rentas netas, otros cobros, gastos de mantenimiento y movimientos de garantía (sin sumarlos a renta), identifica pagos anulados y los excluye del neto en PDF (`ingresos_gastos.jrxml`) y XLSX. | **Verificado** (H2) |
| `REP-05` · `RF-M10-04` (`CP-070`) | `ReporteServiceTest.rep05OcupacionIncluyeEnMantenimientoYExcluyeInactivaDelDenominador`: denominador `ocupadas / activas` al instante de generación sin selector de fecha histórica, incluyendo `En mantenimiento` en activas y excluyendo `Inactiva` del denominador (`ocupacion.jrxml`) + XLSX. | **Verificado** (H2) |
| `REP-06` · `RF-M10-06` (`CP-072`) | `ReporteServiceTest.rep06ContratosPorVencerFiltraUmbralYExcluyeTerminados`: umbral configurable (por defecto `dias_aviso_vencimiento` o 30 días), solo contratos activos en rango, excluye `Terminado`/`Cancelado` (`contratos_por_vencer.jrxml`) + XLSX. | **Verificado** (H2) |
| `REP-07` · `RF-M10-07` (`CP-073`) | `ReporteServiceTest.rep07HistorialPagosFiltraInquilinoYExcluyeAnuladosDelNeto`: filtros por inquilino, contrato y fechas; muestra `Registrado` y `Anulado` con motivo y recibo; excluye anulados del neto por moneda (`historial_pagos.jrxml`) + XLSX. | **Verificado** (H2) |
| `REP-08` · `RF-M10-08` (`CP-074`) | `ReporteServiceTest.rep08LiquidacionGarantiasConciliaInicialMovimientosYRemanente`: depósito inicial, aplicaciones, deducciones, devoluciones y remanente al corte conciliables por contrato y moneda (`garantias_liquidacion.jrxml`) + XLSX. | **Verificado** (H2) |
| `REP-09` · `RF-M10-11` (`CP-075`) | `ReporteServiceTest.rep09MantenimientoConciliaCostosConMantenimientoService`: solicitudes y gastos por propiedad/unidad/período/estado/prioridad/moneda conciliados con `MantenimientoService.consultarGastos` (`mantenimiento.jrxml`) + XLSX. | **Verificado** (H2) |
| `REP-10` · `RF-M10-13` (`CP-076`) | `ReporteServiceTest.rep10TasasUtilizadasMuestraSoloTasasAplicadasEnPagosYNoRedistribuyeCatalogo`: cruce `pago ⨝ tipo_cambio` con tasa a 8 decimales, fuente, fecha efectiva y autorizador; no redistribuye tasas no utilizadas (`tasas_utilizadas.jrxml`) + XLSX. | **Verificado** (H2) |
| `RF-M10-09`, `RNF-004` (`CP-100`) | `ExportadorXlsxTest.generaOoxmlValidoConTiposNumericosFechasYCaracteresEspeciales`, `benchmarkDiezMilFilasEnMenosDeTreintaSegundos`: exportador OOXML `.xlsx` nativo en Java puro (`ExportadorXlsx`) con celdas numéricas (`BigDecimal`), tasas a 8 decimales, fechas reales Excel, UTF-8 (`ñ`, `C$`, `US$`), filtros y totales separados; **10,000 filas** exportadas en ~0.1 s (< 30 s). | **Verificado** |
| `RF-M10-10`, `RF-M10-12`, `H5` | `VistaPreviaReporteTest`, `ExportadorXlsxTest.cancelacionCooperativaConservaArchivoPrevioYNoDejaTemporales`, `gestorH5ExigeInstanciaUnicaYLiberaAlCerrar`, `ReporteServiceTest.rep02ARep10ExigenPermisoConsultarReportesYRespetanCancelacionCooperativa`: instancia única en `GestorH5Reportes`, publicación atómica en temporal del mismo volumen con respaldo SHA-256, conservación del archivo previo ante fallo o cancelación cooperativa (`OperacionCanceladaException` / `MSG-089`) y verificación de `Permiso.CONSULTAR_REPORTES` dentro de la transacción. | **Verificado** |
| Interfaz JavaFX (`PAN-15`, `PAN-16`, `Mantenimiento`) | `FxmlControladoresTest`, `EstilosCssTest`, `ContrasteTest` verdes tras añadir `reportes-view.fxml`, `ReportesController`, navegación en `DashboardController`, botones `Exportar XLSX…` y `Cancelar` en `VistaPreviaReporteController`, y botón `Reporte` en `MantenimientoController`. | **Verificado** (estático) / **No probado** (humo visual interactivo) |
| Perfil PostgreSQL (`verify -Ppostgres`) | Variables `LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD` no configuradas en esta sesión (`PG_ENV_SET=False`). | **No probado** |

## Comandos ejecutados

1. Pruebas dirigidas P6:
   ```powershell
   ./mvnw.cmd -o -B test "-Dtest=ExportadorXlsxTest,ReporteServiceTest,VistaPreviaReporteTest,FxmlControladoresTest,EstilosCssTest,ContrasteTest"
   ```
   Resultado: **40 pruebas ejecutadas, 0 fallos, 0 errores, 0 omisiones**.
2. Suite completa H2:
   ```powershell
   ./mvnw.cmd -o -B test
   ```
   Resultado: **309 pruebas ejecutadas, 0 fallos, 0 errores, 0 omisiones** (`BUILD SUCCESS`).

## Pasos pendientes para cierre de P6

- Ejecutar la puerta PostgreSQL sobre una base desechable (`LATCH_TEST_DB_*` configuradas, nunca `latch_db`):
  ```powershell
  ./mvnw.cmd -o -B verify -Ppostgres
  ```
- Ejecutar humo visual interactivo en Windows (`./mvnw.cmd javafx:run`): abrir **Reportes**, recorrer `REP-02` a `REP-10` con vista previa, exportación `PDF` y `XLSX` (abriendo los libros generados para confirmar formatos numéricos y acentos), probar `REP-01` en **Pagos** (solo PDF) y botón **Reporte** en **Mantenimiento**, y probar cancelación cooperativa durante exportación.
- Revisión humana **G6** sobre los cambios del paquete.

**Qué invalidaría esta evidencia:** un `.jrxml` que falle en tiempo de compilación/llenado contra datos reales en PostgreSQL, un importe exportado a `.xlsx` como texto en lugar de celda numérica, una suma que mezcle USD y NIO, un temporal `.tmp` residual tras cancelación o fallo, o la sustitución de un archivo previo cuando H5 es cancelada antes de publicar.
