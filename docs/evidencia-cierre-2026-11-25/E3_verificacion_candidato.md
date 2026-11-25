# E3 · Verificación del Candidato — Latch (2026-11-25)

Documento de salida de la fase **E3 · Verificar el candidato** sobre el SHA congelado en E2 según el [Plan de Cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md) y las [Reglas Canónicas](../AGENTES_Y_MODELOS.md).

## 1. Decisión de puerta E3

| Elemento | Resultado | Estado |
| --- | --- | --- |
| Candidato verificado | Rama `feature/reportes-vista-previa`, SHA `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`) | **Verificado** |
| Decisión E3 | **APROBADO en puerta técnica y E2E fuera del IDE** (humo visual interactivo en JavaFX pendiente antes de exposición) | **Verificado** |
| Respaldo estable | `main` en `c61187e0c2daf4e567db0cb728b3a5babb529085` (`c61187e`) — no requerido (0 fallos en E3) | **Verificado** |
| Artefacto JAR verificado | `target/ProyectoFInal-ProgramacionAppEscritorio-1.0-SNAPSHOT.jar` (`648.201` bytes, SHA-256 `F805134E61278DE5519C8FABC13A23D494E7985E045233696C4D48BE45C075D1`) | **Verificado** |

## 2. Puerta técnica (`./mvnw.cmd verify -Ppostgres -B`, E3-A)

- **Entorno:** Windows 11, JDK Temurin `21.0.12.1+1`, PostgreSQL `18.6` aislado en `127.0.0.1:54472` (`latch_test_e1`, usuario `latch_e1_owner`, credenciales efímeras DPAPI).
- **Comando ejecutado tras el recorrido E2E y pruebas de PDFs:** `./mvnw.cmd verify -Ppostgres -B`:
  - **Surefire (H2):** **234 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
  - **Failsafe (PostgreSQL 18.6):** **37 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas** (`ApiPagosTest`: 19, `PostgresMigrationIT`: 4, `InfraestructuraTest`: 14), `BUILD SUCCESS` (`MVN_EXIT=0`).
- **Limpieza posterior de `latch_test_e1`:** comprobada vía `psql.exe` (`0` tablas en `public`, `to_regprocedure('public.fn_reservar_numero()')` = `f`).

## 3. Instalación documentada y recorrido E2E desde el JAR (E3-B)

Ejecutado directamente con `java -cp $cp` usando el JAR de `1b8de3a` y `target/latch-runtime.txt` sobre `latch_test_e1` vacía (`0` tablas iniciales):

| Paso | Operación ejecutada desde JAR / CLI | Resultado observado |
| --- | --- | --- |
| 1 | `HibernateUtil` (`LATCH_DB_SCHEMA_MODE=update`) | `Conexión con PostgreSQL verificada correctamente.` (código `0`) |
| 2 | `psql.exe` aplicando `V1`–`V10` con `ON_ERROR_STOP=1` | `10` migraciones aplicadas sin errores (`MIGRATIONS_APPLIED=10`) |
| 3 | `HibernateUtil` (`LATCH_DB_SCHEMA_MODE=validate`) | `Conexión con PostgreSQL verificada correctamente.` (código `0`) |
| 4 | `CrearPropietarioInicial` (`e3.demo.owner`) + `UsuarioService.crear` | `Propietario inicial creado.` y `OPERADOR_E3_CREADO=2/3` |
| 5 | `IniciarApi` en `127.0.0.1:48159` + `POST /auth/login` + `GET /dashboard/metricas` | `200`, token Bearer sin hash en cuerpo, cabeceras seguras (`no-store`, `nosniff`, sin CORS) |
| 6 | `POST /propiedades`, `POST /unidades`, `POST /inquilinos` | `201` en los tres catálogos sintéticos |
| 7 | `POST /contratos` (activo) + intento duplicado en misma unidad | `201` y rechazo de colisión (`422`) |
| 8 | `POST /tasas` + `POST /tasas/{id}/autorizar` (`36.65000000` y `36.62430000`) | `201` y `200` (`autorizada=true`) |
| 9 | `POST /pagos` (`25.00 USD`, `Idempotency-Key`) + repetición + conflicto | `201` (`recibo1=#1`), repetición idéntica (`201`, mismo `pagoId`/`#1`), monto distinto (`409`) |
| 10 | `GET /pagos/{id}`, `GET /recibos/1`, `GET /contratos/{id}/estado-cuenta` | `200`, recibo `anulado=false`, estado de cuenta en moneda única `USD` |
| 11 | `POST /pagos` (`915.61 NIO` convertido) + `POST /pagos/{id}/anulacion` + `GET /recibos/2` | `201` (`#2` contiguo), anulación `200` (`Anulado`), recibo `#2` conserva número con `anulado=true` |
| 12 | `POST /auth/logout` + `GET /dashboard/metricas` con token revocado | `204` y rechazo `401`; puerto `48159` liberado tras detener `IniciarApi` |

## 4. Generación, renderizado y exportación atómica de los 11 reportes PDF (E3-B)

Sobre los datos persistidos en `latch_test_e1` (y fila sintética de cartera para `cargos_cartera.jrxml`), se ejecutó `VerificarPdfsE3` usando `ReporteService`, `RenderizadorVistaPreviaReporte.renderizarPagina(..., 0, 1.0f)` y `VistaPreviaReporte.exportarPdfAtomico(...)` sobre el JAR congelado (`0` archivos `.tmp` residuales):

| Archivo verificado (`scratch/e3/pdfs/`) | Plantilla JRXML | Tamaño (bytes) | Cabecera `%PDF-` | Renderizado vista previa |
| --- | --- | ---: | --- | --- |
| `recibo_vigente.pdf` | `recibo.jrxml` | `1.943` | Sí | `595x842` (trazos visibles) |
| `recibo_anulado.pdf` | `recibo.jrxml` | `2.064` | Sí | `595x842` (trazos visibles) |
| `estado_cuenta.pdf` | `estado_cuenta.jrxml` | `1.988` | Sí | `842x595` (trazos visibles) |
| `cargos_cartera.pdf` | `cargos_cartera.jrxml` | `1.752` | Sí | `842x595` (trazos visibles) |
| `contratos_ocupacion.pdf` | `contratos_ocupacion.jrxml` | `1.801` | Sí | `842x595` (trazos visibles) |
| `garantias_depositos.pdf` | `garantias_depositos.jrxml` | `1.788` | Sí | `842x595` (trazos visibles) |
| `pagos_tabla.pdf` | `pagos_tabla.jrxml` | `1.822` | Sí | `842x595` (trazos visibles) |
| `tasas_cambio.pdf` | `tasas_cambio.jrxml` | `1.861` | Sí | `842x595` (trazos visibles) |
| `dashboard_resumen.pdf` | `dashboard_resumen.jrxml` | `1.789` | Sí | `842x595` (trazos visibles) |
| `propiedades_unidades.pdf` | `propiedades_unidades.jrxml` | `1.800` | Sí | `842x595` (trazos visibles) |
| `unidades.pdf` | `unidades.jrxml` | `1.757` | Sí | `842x595` (trazos visibles) |
| `inquilinos.pdf` | `inquilinos.jrxml` | `1.751` | Sí | `842x595` (trazos visibles) |

## 5. Humo visual interactivo pendiente para ejecución humana en monitor

Por tratarse de interacción gráfica en ventana viva (`Stage`), quedan pendientes de comprobación manual por Steven sobre `latch_demo` ([demo-docente.md](../demo-docente.md)):

1. Arrancar fuera del IDE con `java '-Dprism.order=sw' -cp $cp "ni.edu.uam.proyectofinalprogramacionappescritorio.Launcher"` (o sin `-Dprism.order=sw` en equipo sin fallo `glass.dll`).
2. Ejecutar los 7 pasos de [demo-docente.md](../demo-docente.md) recorriendo formularios con `Tab` / `Shift+Tab` / `Enter` / `Escape`.
3. Alternar los temas **Claro** y **Oscuro** en las pantallas que ofrezcan el selector y comprobar legibilidad y foco. Abrir la vista previa del recibo desde la interfaz. Abrir el `FileChooser`, cancelar una vez, volver a abrirlo, guardar en una carpeta de prueba y abrir el PDF resultante. Confirmar que el foco vuelve a la interfaz y que el recibo corresponde al pago mostrado.

**Qué invalidaría el humo visual.** Un fallo de renderizado de JavaFX o de la vista previa en el equipo de exposición, incluido un fallo en `glass.dll` con el arranque normal, o un error del diálogo modal `FileChooser` al cancelar o guardar. Si falla el arranque normal, repetir con `-Dprism.order=sw` y registrar qué modo funciona. Si el diálogo falla, registrar el paso y mantener el control visual como **No probado** o **Fallido**, según el resultado. La llamada directa a `VistaPreviaReporte.exportarPdfAtomico` y el recorrido con `IniciarApi` acreditan sus propios flujos, y no cubren el diálogo gráfico.

## 6. Estado de cobertura y relevo E3 → E4

- **Matriz actualizada:** [registro_aceptacion.md](registro_aceptacion.md) (354 filas).

```text
Relevo E3 → E4:
- Candidato verificado: feature/reportes-vista-previa en 1b8de3a (JAR 648.201 B, SHA-256 F805134E...075D1).
- Puerta técnica E3-A: ./mvnw.cmd verify -Ppostgres -B → 234 H2 + 37 PostgreSQL 18.6 (0 fallos);
  latch_test_e1 limpia (0 tablas, fn_reservar_numero() = f).
- Puerta E2E E3-B desde JAR: update + V1-V10 + validate + CrearPropietarioInicial + IniciarApi (12 pasos)
  + 12 PDFs (11 .jrxml) renderizados y exportados atómicamente con 0 .tmp residuales.
- Siguiente fase E4 (11:30–13:00): paquete con runtime, dependencias y licencias, manifiesto SHA-256,
  conciliación final de matriz/brechas, guion de exposición y revisión final con Steven.
- Antes de exponer: ejecutar y registrar el humo visual en monitor. Puede prepararse el paquete mientras esta prueba está pendiente.
```
