# Latch — Proyecto de Programación de Aplicaciones de Escritorio

Aplicación de escritorio para administrar alquileres: propiedades, unidades,
inquilinos, contratos, cargos, tasas de cambio, pagos, recibos, estado de cuenta
y métricas. La interfaz JavaFX consume la **API REST local de Spring Boot**,
que también puede ejecutarse de forma independiente.

- JavaFX 21 + Hibernate 7.4.6.Final + PostgreSQL 18.
- JSON con Gson 2.13.2.
- Paquete raíz: `ni.edu.uam.proyectofinalprogramacionappescritorio`.

## Requisitos

- JDK 21 (configurar `JAVA_HOME`).
- PostgreSQL 18 para la persistencia real.
- Maven Wrapper incluido: `./mvnw.cmd` en Windows y `./mvnw` en Linux/macOS.
- JavaFX 21.0.13, Hibernate ORM 7.4.6.Final, JasperReports 7.0.8 y controlador
  PostgreSQL 42.7.14, declarados en `pom.xml`.

## Configuración

La aplicación carga `.env` desde la raíz del proyecto. Copiar `.env.example` a
`.env` y completar:

```dotenv
LATCH_DB_URL=jdbc:postgresql://localhost:5432/latch_db
LATCH_DB_USER=tu_usuario_postgresql
LATCH_DB_PASSWORD=tu_contraseña_postgresql
LATCH_DB_SCHEMA_MODE=validate
LATCH_API_PORT=8090
```

- `LATCH_API_PORT` es obligatorio: la API solo escucha en `127.0.0.1` y ese
  puerto. Si falta, la aplicación muestra un error y no continúa.
- Las variables de entorno tienen prioridad sobre el archivo.
- `.env` está ignorado por Git. No introducir credenciales en la URL JDBC ni en
  el código.
- Para `localhost`, la URL actual sin parámetros sigue siendo válida. Para una
  base remota, usar `sslmode=verify-full` (recomendado) o `sslmode=verify-ca`
  y añadir `sslrootcert` con la ruta absoluta a un certificado raíz existente y
  legible. Codificar los caracteres reservados de la ruta como `%XX` dentro de
  la URL; por ejemplo, `C:\certs\ca.crt` se escribe como
  `C%3A%5Ccerts%5Cca.crt`.
- La URL solo admite los parámetros `sslmode` y `sslrootcert`. Un host remoto
  rechaza `disable`, `allow`, `prefer` y `require`; `require` cifra la conexión,
  pero pgJDBC no valida el certificado del servidor en ese modo. Los modos
  `verify-ca` y `verify-full` requieren `sslrootcert`.
- `validate` (predeterminado) verifica el esquema sin modificarlo. `update` solo
  debe usarse en una base de desarrollo durante la inicialización.
- `LATCH_ADJUNTOS_DIR` (opcional) define la carpeta controlada donde se guardan
  fotografías, logotipos y documentos adjuntos (por defecto
  `%LOCALAPPDATA%/Latch/adjuntos` en Windows o `~/.latch/adjuntos`).

## Base de datos y migraciones

Crear un usuario local propio y una base `latch_db` de su propiedad. Para una
**instalación nueva sobre base vacía**, aplicar primero `V0__esquema_inicial.sql`
y después **en orden** las migraciones `V1`–`V16` de `docs/migrations`; luego
arrancar directamente con `LATCH_DB_SCHEMA_MODE=validate` (sin usar `update`).
Para **actualizar una base existente**, aplicar únicamente las migraciones
pendientes `V1`–`V16` en orden y mantener `validate`:

| Archivo | Contenido |
| --- | --- |
| `V0__esquema_inicial.sql` | Esquema base para instalaciones limpias sin DDL automático |
| `V1__propietario_perfil.sql` | Perfil de propietario y relación con propiedades |
| `V2__seguimiento_garantias.sql` | Seguimiento del depósito en garantía |
| `V3__descripcion_unidad.sql` | Descripción de unidades |
| `V4__tasas_autorizadas.sql` | Una tasa autorizada por fecha y tasa positiva |
| `V5__pagos_idempotencia_aplicaciones.sql` | Idempotencia, aplicaciones de pago y datos de anulación |
| `V6__contratos_activos_por_unidad.sql` | Una ocupación activa por unidad |
| `V7__retencion_idempotencia_pagos.sql` | Índice para limpiar idempotencias vencidas |
| `V8__consecutivo_recibos_transaccional.sql` | Consecutivo de recibos sin huecos por rollback |
| `V9__auditoria_login_fallido.sql` | Auditoría de login fallido sin requerir una cuenta existente |
| `V10__instante_operativo_pago.sql` | Instante UTC de registro del pago, separado de su fecha efectiva |
| `V11__idempotencia_respuesta_text.sql` | Alinea `solicitud_idempotente.respuesta_json` a `TEXT` en bases previas |
| `V12__p1_cuentas_y_parametros_historial.sql` | Cambio de clave pendiente, emisor en recibos y parámetros con historial |
| `V13__p2_contratos_y_movimientos_garantia.sql` | Renovación enlazada, motivo de cierre, duración mínima y movimientos de garantía |
| `V14__p3_cobranza_mora_y_creditos.sql` | Mora automática (`cargo_origen_id`), anulación trazable de cargos, reversión RN-31 y créditos |
| `V15__p4_referencias_y_control_fx.sql` | Historial de referencias USD/NIO y concesión, calendario y cuota compartidos de H3 |
| `V16__p5_mantenimiento_y_adjuntos.sql` | Solicitudes de mantenimiento, gastos de propiedad, metadatos de adjuntos y puerta de exclusión |

Ejemplo en PowerShell para una instalación nueva. En una base existente,
seleccionar únicamente los archivos pendientes (sin V0). `psql` se detiene en
el primer error y revierte las migraciones de esa ejecución:

```powershell
$migraciones = Get-ChildItem docs/migrations -Filter 'V*.sql' -File |
    Sort-Object { [int]($_.BaseName -replace '^V(\d+)__.*$', '$1') }
$psqlArgs = @('-X', '-h', 'localhost', '-U', 'tu_usuario', '-d', 'latch_db',
    '-v', 'ON_ERROR_STOP=1', '--single-transaction')
foreach ($archivo in $migraciones) { $psqlArgs += @('-f', $archivo.FullName) }
psql @psqlArgs
if ($LASTEXITCODE -ne 0) { throw 'Falló la migración. Revisa el primer error antes de reintentar.' }
```

Para **V11 en una base histórica con `respuesta_json` de tipo `oid`**, detener
la aplicación antes de migrar y ejecutar V11 con el rol propietario de
`solicitud_idempotente`. Si otro rol creó los Large Objects, ser dueño de la
tabla o la base no concede lectura sobre esos objetos. El error de V11
conserva el OID en el detalle original. Consultar los objetos referenciados y
sus propietarios antes de reintentar:

```sql
SELECT DISTINCT s.respuesta_json AS oid, pg_get_userbyid(m.lomowner) AS propietario
FROM solicitud_idempotente s
JOIN pg_largeobject_metadata m ON m.oid = s.respuesta_json
ORDER BY oid;
```

El propietario de cada objeto que reporte un error concede únicamente
`GRANT SELECT ON LARGE OBJECT <oid> TO <rol_migracion>;` con los valores reales.
Reintentar el mismo lote y comprobar que termina correctamente. Si los
permisos se otorgaron solo para la migración, el propietario puede revocarlos
al terminar. Con una columna ya de tipo `text`, V11 no necesita esos permisos.

La regresión de este caso se puede ejecutar con `psql -f` y el archivo
[`v11_large_object_ownership.sql`](src/test/resources/postgres/v11_large_object_ownership.sql)
como administrador de un clúster desechable, indicando la conexión a la base
de pruebas. La prueba usa dos roles sin privilegios elevados y revierte su
transacción al terminar.

Comprobar la conexión:

```powershell
./mvnw.cmd compile exec:java
```

Debe aparecer `Conexión con PostgreSQL verificada correctamente.`.

## Primer propietario

En Windows, abre PowerShell en la carpeta que contiene `pom.xml`. Ejecuta este
bloque una sola vez, con PostgreSQL configurado y la aplicación detenida.

```powershell
$env:LATCH_INITIAL_USER = Read-Host 'Usuario del primer propietario'
$claveInicial = Read-Host 'Contraseña inicial (12 a 128 caracteres)' -AsSecureString
$env:LATCH_INITIAL_PASSWORD = [System.Net.NetworkCredential]::new('', $claveInicial).Password

try {
    .\mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.CrearPropietarioInicial'
} finally {
    Remove-Item Env:LATCH_INITIAL_USER, Env:LATCH_INITIAL_PASSWORD
    Remove-Variable claveInicial
}
```

Al completar el registro, verás `Propietario inicial creado.`.

La herramienta crea el propietario y su auditoría en una transacción y rechaza
la operación si ya existen usuarios.

## Ejecución

En IntelliJ IDEA, selecciona la configuración Maven **Latch** y pulsa **Run**.
Esta configuración resuelve las dependencias con Maven e inicia `Launcher`, el
mismo punto de entrada del ejecutable de escritorio. Si IntelliJ todavía muestra
una configuración antigua de tipo **Application**, recarga el proyecto desde la
ventana Maven y vuelve a seleccionar **Latch**.

```powershell
./mvnw.cmd javafx:run
```

La aplicación levanta Spring Boot en loopback, inicia el cliente local cuando la API está lista y abre la interfaz. Si la
API no puede arrancar, muestra un diálogo de error y detiene el arranque.

Para ejecutar solo el servicio HTTP (útil para pruebas con `curl` o
PowerShell):

```powershell
./mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.IniciarApi'
```

### JAR y demostración en Windows

Desde la raíz del repositorio, después de empaquetar con `verify -Ppostgres`,
genera el classpath de ejecución y elige uno de los dos puntos de entrada.
Ambos necesitan la configuración local descrita arriba y un puerto libre.

### Referencia comercial USD/NIO

H3 se habilita al arrancar el backend con `LATCH_FX_API_KEY` en el entorno del
proceso. En LAN, configurar la llave en el equipo consultor designado. Para
desactivar las llamadas salientes en un equipo, usar `LATCH_FX_CONSULTOR=false`.
La pantalla Tasas de cambio lee la última referencia guardada y muestra fuente,
fecha efectiva, consulta, antigüedad y aviso. Actualizar en pantalla vuelve a
leer la caché. H3 usa el parámetro `fx_intervalo_horas` (24 a 168, 24 por
defecto) y comparte el límite de 100 intentos por mes local. Si se corrige una
credencial suspendida, reiniciar el backend con el nuevo valor. Los pagos
entre monedas requieren una tasa autorizada por el Propietario.

```powershell
$runtimeFile = Join-Path (Get-Location) 'target\latch-runtime.txt'
./mvnw.cmd org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath '-DincludeScope=runtime' "-Dmdep.outputFile=$runtimeFile"
$jar = (Resolve-Path 'target\ProyectoFInal-ProgramacionAppEscritorio-1.0-SNAPSHOT.jar').Path
$cp = "$jar;$((Get-Content -Raw -LiteralPath $runtimeFile).Trim())"
$paquete = 'ni.edu.uam.proyectofinalprogramacionappescritorio'

# Escritorio con Spring Boot integrado
java -cp $cp "$paquete.Launcher"

# Solo API REST, en una ejecución aparte
java -cp $cp "$paquete.herramientas.IniciarApi"
```

Si JavaFX se cierra con un fallo nativo en `glass.dll` al abrir el recibo,
reinicia el escritorio con renderizado por software. Para la demo en el equipo
donde se observó ese fallo, inicia directamente con esta variante.

```powershell
java '-Dprism.order=sw' -cp $cp "$paquete.Launcher"
```

Consulta la [guía de demostración con datos sintéticos](docs/demo-docente.md).

## Pruebas

```powershell
./mvnw.cmd clean test
```

Las pruebas normales usan H2 en memoria con `MODE=PostgreSQL` y datos
sintéticos. La suite cubre autenticación, tokens, permisos vigentes, auditoría,
rollback, restricciones, versiones concurrentes, tasas, catálogos,
validaciones, pagos, idempotencia, recibos, anulación, estado de cuenta,
métricas y seguridad HTTP.

La suite normal permanece en H2. Para ejecutar la verificación adicional en PostgreSQL,
configurar una base **desechable y exclusiva** con permisos para crear y eliminar tablas.
El perfil genera el esquema con Hibernate y aplica las migraciones V1 a V10,
y al terminar elimina las tablas creadas, la función de recibos y su contador auxiliar.
No usar la base configurada para la aplicación:

```powershell
$env:LATCH_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/latch_test'
$env:LATCH_TEST_DB_USER = 'latch_test'
$env:LATCH_TEST_DB_PASSWORD = 'contraseña_de_prueba'
./mvnw.cmd verify -Ppostgres
Remove-Item Env:LATCH_TEST_DB_URL
Remove-Item Env:LATCH_TEST_DB_USER
Remove-Item Env:LATCH_TEST_DB_PASSWORD
```

El perfil vuelve a ejecutar `InfraestructuraTest` y `ApiPagosTest` sobre PostgreSQL para
cubrir persistencia y concurrencia, y ejecuta `PostgresMigrationIT` para comprobar los
índices parciales, el consecutivo transaccional y la columna `pago.creado_en`. La suite
H2 se ejecuta primero. El perfil requiere que PostgreSQL esté disponible; si faltan las
variables de `LATCH_TEST_DB_*` o no se puede conectar, la verificación falla.

Para ejecutar solo las pruebas H2:

```powershell
./mvnw.cmd test
```

## Arquitectura

```text
JavaFX/FXML → Controller → ClienteApi (HTTP) → Spring MVC → Service de API
            → Services de negocio → Hibernate → PostgreSQL
```

| Paquete | Responsabilidad |
| --- | --- |
| `config/` | Configuración, SessionFactory y transacciones |
| `modelo/` | Entidades JPA y reglas de dominio |
| `dto/` | Copias inmutables para presentación y reportes |
| `dao/` | Acceso a datos reutilizable |
| `service/` | Autenticación, permisos, auditoría y operaciones de negocio |
| `api/comun/` | Tokens, JSON, errores y lectura acotada |
| `api/piloto/` | Arranque de Spring Boot, filtros HTTP y controladores MVC |
| `api/auth/` | Inicio y cierre de sesión HTTP |
| `api/tasas/` | Consulta, registro y autorización de tasas |
| `api/catalogos/` | Propiedades, unidades, inquilinos y contratos |
| `api/validaciones/` | Validaciones de entrada |
| `api/pagos/` | Pagos, recibos, anulación y estado de cuenta |
| `api/indicadores/` | Métricas del panel |
| `cliente/` | Cliente HTTP de la interfaz, con token en memoria |
| `util/` | Contraseñas, formato de moneda, validaciones y tareas de fondo |

Reglas transversales:

- Importes con `BigDecimal` y 2 decimales, redondeo `HALF_EVEN`; tasas con 8
  decimales. En JSON los importes viajan como texto.
- Solo se admiten `USD` y `NIO`; los totales nunca mezclan monedas.
- Cada Service de negocio llama a `PermisoService.exigir` dentro de su
  transacción. Deshabilitar un botón no sustituye esa comprobación.
- Los errores de validación usan `IllegalArgumentException`, los de permisos
  `SecurityException`, los conflictos `ConflictoDatosException` y los técnicos
  `IllegalStateException`. Al cliente nunca se le muestra un stack trace.

## API local

La referencia completa está en [docs/API_Latch.md](docs/API_Latch.md).

- Base: `http://127.0.0.1:<LATCH_API_PORT>/api/v1`
- Token Bearer de 15 minutos, guardado solo en memoria.
- Errores uniformes: `{"error":{"codigo":"...","mensaje":"..."}}`.
- Escrituras financieras con `Idempotency-Key` (UUID).
- Cuerpo máximo 256 KiB y `Content-Type: application/json` obligatorio.

| API | Rutas principales |
| --- | --- |
| Autenticación | `POST /auth/login`, `POST /auth/logout` |
| Tasas | `GET/POST /tasas`, `POST /tasas/{id}/autorizar` |
| Catálogos | `GET/POST /propiedades`, `/unidades`, `/inquilinos`, `/contratos` y `GET/PUT/PATCH /{id}` |
| Validaciones | `POST /validaciones` |
| Cobranza | `POST /pagos`, `GET /pagos/{id}`, `GET /recibos/{numero}`, `POST /pagos/{id}/anulacion`, `GET /contratos/{id}/estado-cuenta` |
| Indicadores | `GET /dashboard/metricas` |

## Permisos

| Operación | Propietario | Asistente |
| --- | --- | --- |
| Gestionar usuarios y configuración | Sí | No |
| Consultar auditoría | Sí | No |
| Operar alquileres | Sí | Sí |
| Consultar reportes y métricas | Sí | Sí |
| Registrar tasas informativas | Sí | Sí |
| Autorizar tasas | Sí | No |
| Anular pagos | Sí | No |

## Seguridad

- La API solo escucha en `127.0.0.1`; el host no se puede configurar.
- Tokens opacos de 32 bytes; se guarda solo su huella SHA-256 y expiran a los
  15 minutos. `logout` los revoca de inmediato.
- Contraseñas con PBKDF2-HMAC-SHA256 (600 000 iteraciones, sal aleatoria y
  comparación en tiempo constante).
- El rol y el estado del usuario se revalidan contra la base en cada operación.
- Al cambiar el rol o desactivar un usuario, la versión de su cuenta invalida
  todas las sesiones anteriores; reactivarlo no restaura los tokens revocados.
- Las escrituras financieras toman su instante y fecha operativa de PostgreSQL.
  Si el reloj local difiere más de cinco minutos, devuelven `ERR-057` y no
  escriben la operación; las fechas efectivas de pago se conservan aparte.
- Una petición HTTP nunca hereda la sesión global de JavaFX: si su identidad
  aislada no es válida, la operación falla cerrada.
- Sin cabeceras CORS, con `Cache-Control: no-store` y
  `X-Content-Type-Options: nosniff`.
- Nunca se registran contraseñas, tokens ni la cabecera `Authorization`.

El detalle de controles, verificación y riesgos aceptados está en
[docs/SEGURIDAD.md](docs/SEGURIDAD.md).

## Problemas frecuentes

- `JAVA_HOME` inválido: apuntar a la carpeta del JDK 21, no a `bin`.
- `Falta configurar LATCH_API_PORT`: agregar la variable al `.env`.
- Tabla ausente con `validate`: aplicar las migraciones pendientes; usar
  `update` solo en una base de desarrollo.
- `409` al actualizar: otro usuario modificó el registro; recargar y reintentar.
- `409` al repetir un pago: la `Idempotency-Key` se usó con otro contenido u
  otro usuario. Usar una clave nueva para un pago distinto.
- `401` tras 15 minutos: la sesión expiró; cerrar sesión y volver a iniciar.
- `401` después de cambiar el rol o el estado de la cuenta: iniciar sesión otra
  vez para obtener un token vigente.
- `429` al iniciar sesión: cinco fallos para la cuenta o 25 desde el mismo
  origen; esperar 15 minutos.
- Error de conexión: comprobar el servicio PostgreSQL, puerto, base, usuario y
  contraseña.

## Limitaciones conocidas

- `Recibo` usa `GenerationType.IDENTITY`: un rollback después de insertarlo
  puede dejar un hueco en la numeración.
- No hay refresh token; al expirar, la sesión debe iniciarse de nuevo.
- Si dos peticiones simultáneas usan la misma `Idempotency-Key`, una puede
  recibir `409` mientras la otra termina; reintentar con la misma clave
  devuelve la respuesta original.
- Las eliminaciones físicas de catálogos, el ajuste de garantías y las
  escrituras de cargos se ejecutan localmente. El detalle está en
  [el cierre técnico de la fase 5](docs/migracion-spring-fase-5.md).
- El pool de conexiones de Hibernate es el básico; suficiente para la demo
  académica, no para producción.

## Alcance del equipo

- **Steven Cuadra:** infraestructura, seguridad, auditoría y concurrencia base.
- **Alfredo Montalvan:** entidades de alquileres, PostgreSQL, DAOs, CRUD y HQL.
- **Caleb Tardencilla:** FXML, Controllers, validaciones y flujos JavaFX.
- **Aris Gavarrete:** recibos, tipo de cambio, reportes y documentación final.

Consultar [la guía de integración](docs/steven-infraestructura.md) para ejemplos
del recorrido de demostración.
