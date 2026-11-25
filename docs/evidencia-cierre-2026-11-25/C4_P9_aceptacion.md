# C4 · Aceptación de Latch (P9)

El candidato técnico del 25 de noviembre de 2026 es `318b78fff795ce604d5a87a94f76a59ef1a55f2a`, de `feature/p1-base-seguridad-admin`. El código empaquetado coincide con `a4d4f61`. Se construyó desde un `git archive` del candidato con Temurin 21.0.12.1+1, dependencias Maven locales y PostgreSQL 18.6 desechable. La revisión de entrega queda a cargo de Steven. [Plan de cierre](../LATCH_PLAN_DE_CIERRE_2026-11-25.md)

## Cierre del plan

Steven dio por cerrado el plan el 25 de noviembre de 2026 con un candidato técnico y brechas declaradas. Los criterios pendientes de aceptación conservan su estado. El ZIP corresponde a `318b78f`. Las correcciones posteriores de IntelliJ y del mensaje de tasa duplicada quedaron en el código local. V12 se aplicó por separado en `latch_db` tras verificar la restauración de su respaldo.

## Resultado del candidato

| Criterio | Estado y evidencia |
| --- | --- |
| Suite automatizada | **Verificado.** `./mvnw.cmd -o -B verify -Ppostgres` devolvió `BUILD SUCCESS` con 335 pruebas normales y 41 de PostgreSQL, sin fallos, errores ni omisiones. Incluye la restauración con herramientas nativas, la exportación de diez PDF y nueve XLSX y las pruebas de FXML, CSS y contraste. |
| CP-032, CP-041, CP-042, CP-048 y RNF-021 | **Verificado.** `./mvnw.cmd -o -B -Dtest=PagosConcurrenciaDosJvmTest -Dlatch.test.postgres=true test` pasó los dos escenarios en una base desechable exclusiva. Las aserciones cubren cien recibos contiguos, diez rollbacks, diez anulaciones posteriores sin renumeración y la colisión sobre un saldo sin volverlo negativo. |
| ENT-09 | **Inferido.** La suite PostgreSQL ejercita la idempotencia HTTP y el comando dirigido confirma la numeración entre JVM. Para completar el control, reenviar la misma solicitud y clave a dos instancias sobre una base compartida y comprobar que ambas devuelvan el mismo recibo. |
| ENT-03 y CP-097 | **Verificado.** Desde el ZIP extraído se aplicaron V0–V16 con `psql -X -v ON_ERROR_STOP=1 --single-transaction` y arrancó `verificar-bd.cmd` en `validate`. En otra base, V10 fue rechazado por una columna ausente. Los dos `pg_dump --schema-only` fueron idénticos byte a byte. La base pudo validarse tras aplicar V11–V16. |
| RNF-028 | **Verificado.** La API del ZIP creó y releyó 20 propiedades en PostgreSQL. En las 20 coincidieron literalmente el nombre con `Ñandú` y la dirección acentuada. |
| Arranque local del paquete | **Verificado.** Con `JAVA_HOME` vacío y sin Java del sistema en `PATH`, el runtime incluido ejecutó la API. Login y métricas devolvieron 200, el cierre devolvió 204 y el token cerrado recibió 401. `Latch.exe` creó una ventana en esta estación. El recorrido gráfico integral requiere ejecución manual. |

## Paquete para revisión

`Latch-0.9.0-rc.1-win64-318b78f.zip` mide 117 458 305 bytes. Las dos copias SHA-256 coinciden en `27837F5545C15963EA3C816D03DDA766DFAB4E3F690549F6ADF9F2BC8B742876`. El JAR propio tiene SHA-256 `5EB668ADDF77C703F36470828D5FAC8C82BCFFA180C11126812E4E0C7E5C8805`.

La carpeta de artefactos de la estación es `C:\Users\stive\Documents\Codex\Latch-C4-318b78f`, con `copia-1` y `copia-2` en el mismo disco. Cada una contiene el ZIP y `SHA256.txt`. El ZIP incluye `INICIO.md`, README, contrato HTTP, guía de seguridad, protocolo del Plan, V0–V16, lanzadores, runtime y el inventario de 95 dependencias de terceros. El inventario incorpora `bcprov-jdk18on-1.87.jar` según su POM local. Para ENT-17 queda revisar los avisos individuales y resolver las 13 entradas que remiten a POM o `META-INF`. El nombre de versión identifica un candidato para revisión.

## Conciliación de la matriz

Se contaron 354 filas y 354 identificadores únicos. La matriz E1–E4 conserva los estados históricos de `1b8de3a`. Para la muestra C4 se eligieron tres filas por familia, sin reemplazo, con `System.Random(31878)` y el orden de las filas del registro. [Matriz histórica](registro_aceptacion.md)

| Filas de la muestra | Evidencia sobre el candidato y acción |
| --- | --- |
| RF-M01-11, RF-M02-10, RF-M02-07 | **Verificado en servicio** para restablecimiento, cambio de clave de ambos roles y baja lógica de una propiedad sin unidades activas mediante `UsuarioServiceTest` y `PropiedadServiceTest`. **Inferido** para la sincronización integral de ocupación en RF-M02-07. Recorrer altas, activación y finalización de un contrato en JavaFX y cotejar unidad, panel y REP-05. |
| RN-27, RN-20, RN-03 | **Verificado en servicio** para transiciones terminales de mantenimiento, cancelación y reanudación de H1, concurrencia H1/H2 y conservación de moneda con cargos o pagos mediante `MantenimientoServiceTest`, `CobranzaH1H2ServiceTest` y `ContratoServiceTest`. Contrastar RN-03 con movimientos históricos e importes por moneda en el recorrido de contratos y cobro. |
| RNF-021, RNF-038, RNF-028 | **Verificado** RNF-021 y RNF-028 por las ejecuciones de esta acta. **No probado** RNF-038. Registrar respaldos diarios durante el ensayo operativo y restaurar tras el incidente simulado para calcular la pérdida a partir del último respaldo. |
| CP-061, CP-042, CP-011 | **Verificado** CP-042 con dos JVM. **No probado** CP-061, ya que el fixture 429 inspeccionado adjunta `Retry-After`. Ejecutar S1 con 429 sin cabecera y observar el plazo de reintento. **No probado** CP-011 con la precondición exacta. Intentar la baja desde JavaFX con un contrato activo y comprobar que se conserve la unidad. |
| REC-09, REC-04, REC-08 | **No probado** como recorrido completo. Ejecutar las variantes del Apéndice C para reportes y S1, cancelación y reanudación del lote, y liquidación junto al flujo de mantenimiento. Las pruebas de componentes se conservan como evidencia parcial. |
| ENT-14, ENT-02, ENT-03 | **Verificado** ENT-03. **Inferido** ENT-02 por manifiesto y copias, con etiqueta pendiente. **No probado** ENT-14. Extraer el ZIP en otro Windows sin JDK, iniciar `iniciar-latch-sw.cmd` con datos sintéticos y abrir un PDF. |

La fila histórica ENT-16 cita un inventario de licencias. Ese inventario aporta evidencia a ENT-17. ENT-16 queda **No probado** hasta que el manual y el protocolo indiquen la etiqueta y otro integrante repita REC-01–REC-10.

## Pendientes de aceptación

- Conciliar los 354 criterios con el candidato, completar los CP restantes y ejecutar íntegramente REC-01–REC-10 con los datos y roles previstos. Conservar resultado por identificador, variante y entorno, incluida la navegación por teclado y los reportes fuera del IDE.
- Repetir RNF-036/RNF-037 en el entorno V1, medir rendimiento con D4 y probar la variante LAN con PostgreSQL TLS y SMB. Ejecutar P4 con la credencial del proveedor real configurada por el responsable, usando el entorno del proceso. [Auditoría de C2](C2_auditoria_integrada.md) · [Auditoría de C3](C3_auditoria_integrada.md)
- Probar un Windows alterno sin JDK y completar el ensayo de veinte minutos en monitor y proyector a 100/150/200 %. Verificar los avisos de terceros y depositar la segunda copia en almacenamiento independiente accesible al equipo.
- Obtener revisión humana G6 y revisar las discrepancias de los contratos. Confirmar el acta y la huella del candidato antes de cualquier etiqueta o entrega formal.

**Qué invalidaría la evidencia.** Una huella distinta a la del manifiesto, una modificación del esquema V10 al ejecutar `validate`, un recibo perdido o duplicado en la prueba de dos JVM o una regresión reproducida sobre el mismo candidato reabre los criterios afectados.
