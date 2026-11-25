# C2 · P5 · Mantenimiento y adjuntos

Rama de trabajo `feature/p1-base-seguridad-admin`, base `1b8de3a`, con los cambios acumulados de C1 y P2–P5 sin commit. La migración `V16__p5_mantenimiento_y_adjuntos.sql` quedó lista para bases PostgreSQL 18.

| Criterio | Evidencia |
| --- | --- |
| RF-M08-01 a RF-M08-04, RF-M08-07, RN-27, CP-037 | **Verificado en H2 y HTTP.** `MantenimientoServiceTest` (`abreSolicitudEnUnidadOcupadaSinAlterarSuEstadoYPermiteResponsableNominal`, `cp037TransicionesValidasIdempotenciaTerminalesYErr066`, `sesionInvalidaFallaCerrada`) y `ApiMantenimientoTest` (`cicloCompletoHttpDeSolicitudesGastosYCodigosDeError`) comprueban apertura por unidad con prioridad y responsable nominal sin cuenta, máquina de estados (`Abierta` → `En proceso`/`Cancelada`, `En proceso` → `Resuelta`/`Cancelada`), terminalidad de `Resuelta`/`Cancelada`, idempotencia al repetir el estado actual sin duplicar eventos ni subir versión, rechazo `ERR-066` (`422`) en transiciones inválidas, conflicto optimista (`409`), motivo obligatorio de cancelación conservando gastos, y fallo cerrado ante sesión inválida. |
| RF-M08-05, RF-M08-06, RN-23, CP-038 | **Verificado en H2 y HTTP.** `MantenimientoServiceTest.cp038GastoNoGeneraCargoNiTocaGarantiaYRechazaPropiedadIncompatibleConErr065` y `ApiMantenimientoTest` comprueban que el gasto de 25.00 USD se asocia a la propiedad sin generar cargos al inquilino ni alterar garantías, rechaza propiedad incompatible con `ERR-065` (`422`), conserva gastos tras cancelar la solicitud, y consulta gastos por propiedad/período sumando USD y NIO por separado sin duplicar solicitudes. |
| RF-M02-05, RF-M03-03, RF-M03-05, RF-M04-07, RF-M12-03, RN-25, CP-014 | **Verificado en H2 y almacenamiento temporal.** `AdjuntoServiceTest` (`cp014AdjuntoPdfYHuellaSha256RechazaEjecutableYExcesoDeCincoMibSinDejarTemporales`, `fotoUnidadContratoYLogotipoEmisorRespetanTiposYPermisos`) comprueba validación de tamaño (1 B–5 MiB), firma mágica (`%PDF-`, PNG, JPEG) y dimensiones de imagen, rechazo `ERR-019` de ejecutables renombrados y traversal sin dejar temporales, huella SHA-256 en lectura, tipos permitidos por entidad, reemplazo de foto de unidad, exclusividad del Propietario para el logotipo del emisor y baja lógica auditada. `FormulariosFeedbackTest` comprueba el formulario de inquilino con contacto de emergencia opcional dividido en nombre y teléfono. |
| Interfaz JavaFX y rutas REST | **Verificado por compilación, pruebas estáticas y pruebas HTTP.** `ComposicionAplicacionTest` verifica las 55 rutas de la API local (incluidas las 6 de `/api/v1/mantenimiento/*`). `FxmlControladoresTest`, `EstilosCssTest`, `ContrasteTest`, `FormulariosFeedbackTest`, `VistaPreviaReporteTest` y `ApiSeguridadTest` pasaron sin fallos tras integrar `mantenimiento-view.fxml`, `MantenimientoController`, navegación en `DashboardController`, `DialogosAdjuntos` en `UnidadController`, `InquilinoController`, `ContratoController` y logotipo en `AdminController`. |

La verificación `./mvnw.cmd -o -B test` terminó con **292 pruebas de la suite normal H2**, con 0 fallos, 0 errores y 0 omisiones.

**No probado.** Queda pendiente ejecutar la puerta PostgreSQL 18 contra una base desechable (`LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER`, `LATCH_TEST_DB_PASSWORD` no estuvieron configuradas en esta sesión):

```powershell
./mvnw.cmd -o -B verify -Ppostgres
```

También quedan pendientes el humo visual interactivo en Windows (`Mantenimiento`, fotografía de unidad, adjuntos de inquilino/contrato y logotipo en administración) y la revisión humana G6.

Invalidaría el resultado una falla de `hbm2ddl=validate` o de restricciones `CHECK`/índices de `V16` en PostgreSQL 18, una transición no autorizada que modifique estado o auditoría, un gasto de mantenimiento que cree cargos o altere garantías, o un archivo rechazado que deje temporales en el repositorio de adjuntos.
