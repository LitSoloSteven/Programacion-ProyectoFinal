# Latch — Proyecto de Programación de Aplicaciones de Escritorio

Aplicación de escritorio para administrar alquileres: propiedades, unidades,
inquilinos, contratos, cargos, tasas de cambio, pagos, recibos, estado de cuenta
y métricas. La interfaz JavaFX consume una **API REST local propia** levantada
con `com.sun.net.httpserver.HttpServer`; no se usan servicios externos.

- JavaFX 21 + Hibernate 7.4.6.Final + PostgreSQL 18.
- JSON con Gson 2.13.2.
- Paquete raíz: `ni.edu.uam.proyectofinalprogramacionappescritorio`.
- **108 pruebas automatizadas**, todas en verde.

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
- `validate` (predeterminado) verifica el esquema sin modificarlo. `update` solo
  debe usarse en una base de desarrollo durante la inicialización.

## Base de datos y migraciones

Crear un usuario local propio y una base `latch_db` de su propiedad. Después,
aplicar **en orden** las migraciones de `docs/migrations`:

| Archivo | Contenido |
| --- | --- |
| `V1__propietario_perfil.sql` | Perfil de propietario y relación con propiedades |
| `V2__seguimiento_garantias.sql` | Seguimiento del depósito en garantía |
| `V3__descripcion_unidad.sql` | Descripción de unidades |
| `V4__tasas_autorizadas.sql` | Una tasa autorizada por fecha y tasa positiva |
| `V5__pagos_idempotencia_aplicaciones.sql` | Idempotencia, aplicaciones de pago y datos de anulación |

Ejemplo con `psql`:

```powershell
psql -h localhost -U tu_usuario -d latch_db -f docs/migrations/V1__propietario_perfil.sql
psql -h localhost -U tu_usuario -d latch_db -f docs/migrations/V2__seguimiento_garantias.sql
psql -h localhost -U tu_usuario -d latch_db -f docs/migrations/V3__descripcion_unidad.sql
psql -h localhost -U tu_usuario -d latch_db -f docs/migrations/V4__tasas_autorizadas.sql
psql -h localhost -U tu_usuario -d latch_db -f docs/migrations/V5__pagos_idempotencia_aplicaciones.sql
```

Comprobar la conexión:

```powershell
./mvnw.cmd compile exec:java
```

Debe aparecer `Conexión con PostgreSQL verificada correctamente.`.

## Primer propietario

Ejecutar una sola vez, con la aplicación detenida:

```powershell
$env:LATCH_INITIAL_USER = 'steven'
$claveInicial = Read-Host 'Contraseña inicial (12 a 128 caracteres)' -AsSecureString
$env:LATCH_INITIAL_PASSWORD = [System.Net.NetworkCredential]::new('', $claveInicial).Password
./mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.CrearPropietarioInicial'
Remove-Item Env:LATCH_INITIAL_PASSWORD
Remove-Item Env:LATCH_INITIAL_USER
```

La herramienta crea el propietario y su auditoría en una transacción y rechaza
la operación si ya existen usuarios.

## Ejecución

```powershell
./mvnw.cmd javafx:run
```

La aplicación levanta la API, inicia el cliente local y abre la interfaz. Si la
API no puede arrancar, muestra un diálogo de error y detiene el arranque.

Para ejecutar solo el servicio HTTP (útil para pruebas con `curl` o
PowerShell):

```powershell
./mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.IniciarApi'
```

## Pruebas

```powershell
./mvnw.cmd test
```

Las pruebas normales usan H2 en memoria con `MODE=PostgreSQL` y datos
sintéticos. La suite cubre autenticación, tokens, permisos vigentes, auditoría,
rollback, restricciones, versiones concurrentes, tasas, catálogos,
validaciones, pagos, idempotencia, recibos, anulación, estado de cuenta,
métricas y seguridad HTTP.

Para repetir la suite contra PostgreSQL, usar **una base desechable exclusiva**:

```powershell
./mvnw.cmd test '-Dlatch.test.postgres=true'
```

## Arquitectura

```text
JavaFX/FXML → Controller → ClienteApi (HTTP) → Router → Service de API
            → Services de negocio → Hibernate → PostgreSQL
```

| Paquete | Responsabilidad |
| --- | --- |
| `config/` | Configuración, SessionFactory y transacciones |
| `modelo/` | Entidades JPA y reglas de dominio |
| `dto/` | Copias inmutables para presentación y reportes |
| `dao/` | Acceso a datos reutilizable |
| `service/` | Autenticación, permisos, auditoría y operaciones de negocio |
| `api/comun/` | Servidor, router, tokens, JSON, errores y lectura acotada |
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
- `429` al iniciar sesión: demasiados intentos fallidos; esperar 15 minutos.
- Error de conexión: comprobar el servicio PostgreSQL, puerto, base, usuario y
  contraseña.

## Limitaciones conocidas

- `Recibo` usa `GenerationType.IDENTITY`: un rollback después de insertarlo
  puede dejar un hueco en la numeración.
- No hay refresh token; al expirar, la sesión debe iniciarse de nuevo.
- Si dos peticiones simultáneas usan la misma `Idempotency-Key`, una puede
  recibir `409` mientras la otra termina; reintentar con la misma clave
  devuelve la respuesta original.
- La interfaz aún no usa todos los clientes HTTP (catálogos, pagos y métricas);
  esas pantallas siguen apoyándose en los Services locales.
- El pool de conexiones de Hibernate es el básico; suficiente para la demo
  académica, no para producción.

## Alcance del equipo

- **Steven Cuadra:** infraestructura, seguridad, auditoría y concurrencia base.
- **Alfredo Montalvan:** entidades de alquileres, PostgreSQL, DAOs, CRUD y HQL.
- **Caleb Tardencilla:** FXML, Controllers, validaciones y flujos JavaFX.
- **Aris Gavarrete:** recibos, tipo de cambio, reportes y documentación final.

Consultar [la guía de integración](docs/steven-infraestructura.md) para ejemplos
del recorrido de demostración.