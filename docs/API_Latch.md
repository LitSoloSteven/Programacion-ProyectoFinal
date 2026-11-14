# API local de Latch

Servicio HTTP propio de la aplicación, construido sobre
`com.sun.net.httpserver.HttpServer`, sin frameworks externos. La interfaz JavaFX
lo consume con `java.net.http.HttpClient`. El servidor escucha **solo** en
`127.0.0.1` y el puerto se toma de `LATCH_API_PORT`.

- Base: `http://127.0.0.1:<LATCH_API_PORT>`
- Versión de rutas: `/api/v1`
- Formato: JSON UTF-8
- Monedas admitidas: `USD` y `NIO`

## Autenticación

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "usuario": "steven", "password": "…" }
```

```json
{
  "token": "opaco-de-32-bytes",
  "expiraEn": "2026-11-13T19:30:00Z",
  "rol": "PROPIETARIO",
  "usuarioId": 1,
  "nombre": "steven"
}
```

- El token vive **solo en memoria del servidor**; se guarda su huella SHA-256.
- Vigencia: **15 minutos**. No hay refresh: al expirar hay que iniciar sesión de nuevo.
- Cada petición protegida envía `Authorization: Bearer <token>`.
- El rol y el estado `activo` del usuario se **revalidan contra la base de datos en cada operación**.
- `POST /api/v1/auth/logout` revoca el token de inmediato (`204`).
- Tras 5 intentos fallidos, el usuario queda bloqueado 15 minutos (`429`).
- El mensaje de credenciales inválidas es idéntico exista o no el usuario.

## Convenciones de la petición

| Regla | Valor |
| --- | --- |
| `Content-Type` | `application/json` obligatorio en cuerpos JSON (`415` si falta) |
| Cuerpo máximo | 256 KiB (`413`) |
| Idempotencia | Cabecera `Idempotency-Key: <UUID>` en escrituras financieras |
| Importes | Texto decimal con 2 decimales (`"1000.00"`); nunca números flotantes |
| Tasas | Texto decimal con 8 decimales (`"36.62430000"`) |
| Fechas | `AAAA-MM-DD` |
| Instantes | ISO-8601 UTC (`2026-11-13T19:30:00Z`) |
| Identificadores | UUID como texto |

### Idempotencia

`POST /api/v1/pagos` exige `Idempotency-Key` (UUID).

- Misma clave + mismo usuario + mismo contenido → se repite la respuesta original, sin duplicar el pago.
- Misma clave con contenido, usuario o recurso distinto → `409 CONFLICTO`.
- La clave se persiste en `solicitud_idempotente` dentro de la misma transacción del pago.
- Si la solicitud falla, no queda registro de idempotencia ni pago.

## Errores

Cuerpo uniforme:

```json
{ "error": { "codigo": "SOLICITUD_INVALIDA", "mensaje": "El campo monto es obligatorio." } }
```

| HTTP | Código | Uso |
| --- | --- | --- |
| 400 | `SOLICITUD_INVALIDA`, `JSON_INVALIDO` | Datos o JSON mal formados |
| 401 | `NO_AUTENTICADO`, `CREDENCIALES_INVALIDAS` | Sin token, token inválido/expirado o login fallido |
| 403 | `SIN_PERMISO` | Rol insuficiente, usuario inactivo o sesión cerrada |
| 404 | `NO_ENCONTRADO`, `TASA_NO_DISPONIBLE` | Recurso inexistente |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no registrado para la ruta |
| 409 | `CONFLICTO` | Duplicado, versión desactualizada o idempotencia incompatible |
| 413 | `CUERPO_DEMASIADO_GRANDE` | Cuerpo > 256 KiB |
| 415 | `CONTENIDO_NO_SOPORTADO` | `Content-Type` distinto de JSON |
| 422 | `ENTIDAD_NO_PROCESABLE` | Regla de negocio incumplida |
| 429 | `DEMASIADOS_INTENTOS` | Bloqueo temporal de login |
| 500 | `ERROR_INTERNO` | Fallo técnico; nunca incluye SQLSTATE ni stack trace |

## Resumen de endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | pública |
| POST | `/api/v1/auth/logout` | autenticado |
| GET | `/api/v1/tasas` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/tasas/vigente?fecha=AAAA-MM-DD` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/tasas/{id}` | `CONSULTAR_REPORTES` |
| POST | `/api/v1/tasas` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/tasas/{id}/autorizar` | `AUTORIZAR_TASA` (solo propietario) |
| GET | `/api/v1/propiedades` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/propiedades` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/unidades`, `/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/unidades` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/unidades/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/unidades/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/inquilinos`, `/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/inquilinos` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/inquilinos/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/inquilinos/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/contratos`, `/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/contratos/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/contratos/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/validaciones` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/pagos` | `OPERAR_ALQUILERES` + `Idempotency-Key` |
| GET | `/api/v1/pagos/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/recibos/{numero}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/pagos/{id}/anulacion` | `ANULAR_PAGOS` (solo propietario) |
| GET | `/api/v1/contratos/{id}/estado-cuenta?corte=AAAA-MM-DD` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/dashboard/metricas` | `CONSULTAR_REPORTES` |

Las rutas `PUT` requieren el campo `version` del registro leído; si otro usuario
lo modificó, responden `409` y el cliente debe recargar.

## Catálogos

Listados paginados con `pagina` (1..N), `tamano` (1..100) y filtros propios:

| Recurso | Filtros |
| --- | --- |
| `propiedades` | `texto`, `propietarioId` |
| `unidades` | `propiedadId`, `estado`, `tipo`, `texto` |
| `inquilinos` | `texto`, `activo` |
| `contratos` | `estado`, `unidadId`, `inquilinoId`, `texto` |

Respuesta:

```json
{
  "contenido": [ { "…": "…" } ],
  "pagina": 1,
  "tamano": 20,
  "total": 3,
  "totalPaginas": 1
}
```

`PATCH` hace baja lógica o cambio de estado, según el recurso, y también
requiere `version`.

## Tasas de cambio

Las tasas son inmutables. Registrar una tasa crea una instantánea
**informativa**; autorizarla crea una nueva instantánea **autorizada** y
conserva la original. Solo hay una tasa autorizada por fecha, y solo puede
autorizarla el propietario.

```http
POST /api/v1/tasas
{ "fecha": "2026-11-13", "tasa": "36.62430000", "fuente": "BCN" }

POST /api/v1/tasas/{id}/autorizar
{ "motivo": "Tasa oficial del día" }
```

No se admiten fechas futuras (`422`). Autorizar dos veces la misma fecha
devuelve `409`.

## Cobranza y pagos

```http
POST /api/v1/pagos
Authorization: Bearer <token>
Idempotency-Key: 6f1c0a52-8a5e-4c3f-9b1e-2d4f6a8b0c1d
Content-Type: application/json

{
  "contratoId": "…",
  "fechaPago": "2026-11-10",
  "moneda": "NIO",
  "monto": "36624.30",
  "metodo": "Transferencia",
  "referencia": "REF-2026-11",
  "notas": "Pago de noviembre",
  "distribucion": [
    { "cargoId": "…", "monto": "400.00" },
    { "cargoId": "…", "monto": "600.00" }
  ]
}
```

```json
{ "pagoId": "…", "numeroRecibo": 17, "montoContrato": "1000.00", "credito": "0.00" }
```

- Si la moneda del pago coincide con la del contrato, no se consulta tasa.
- Si difiere, se exige una tasa **autorizada** de la fecha del pago; si no
  existe, `422` y **no se escribe nada**.
- La distribución es opcional y va **en la moneda del contrato**. Se valida que
  cada cargo exista, pertenezca al contrato, no esté cancelado y no supere su
  saldo. No se permiten cargos repetidos.
- `credito = montoContrato − Σ aplicaciones`.
- El recibo y la auditoría se crean en la misma transacción.

`GET /api/v1/pagos/{id}` devuelve el detalle con `aplicaciones`, `numeroRecibo`,
`credito` y, si fue anulado, `motivoAnulacion`, `anuladoPor` y `anuladoEn`.

`GET /api/v1/recibos/{numero}` devuelve el recibo con importes como texto.

`POST /api/v1/pagos/{id}/anulacion` exige `{ "motivo": "…" }`, solo lo puede
hacer el propietario, no se puede repetir (`409`) y queda auditado.

### Estado de cuenta

`GET /api/v1/contratos/{id}/estado-cuenta?corte=2026-11-30`

Devuelve cargos con importe/aplicado/saldo, pagos con aplicado/crédito,
totales y garantía, todo en la moneda del contrato. Los cargos cancelados no
entran en los totales y los pagos anulados no aportan aplicado ni crédito.

## Indicadores

`GET /api/v1/dashboard/metricas`

```json
{
  "propiedades": 1, "unidades": 1, "inquilinos": 1, "contratosVigentes": 1,
  "cargosPendientes": [ { "moneda": "USD", "cantidad": 2, "total": "1000.00" } ],
  "pagosRegistrados": [ { "moneda": "USD", "cantidad": 1, "total": "1000.00" } ]
}
```

Los totales siempre van separados por moneda y como texto decimal; nunca se
suman USD con NIO.

## Validaciones

`POST /api/v1/validaciones`

```json
{ "tipo": "correo", "valor": "alguien@dominio.com" }
```

Tipos: `correo`, `telefono`, `cedula`, `monto`, `rangoFechas`. Para
`rangoFechas` se envían `valor` y `valorSecundario` con las dos fechas.

```json
{ "valido": true, "mensaje": null }
```

## Uso desde JavaFX

El cliente está en `cliente/`:

| Clase | Uso |
| --- | --- |
| `ClienteLatch` | Inicia/cierra el cliente con el puerto local |
| `ClienteApi` | GET/POST/PUT/PATCH, token en memoria, timeouts 5 s / 10 s |
| `ClienteAuth` | Login y logout |
| `ClienteTasas` | Consulta y registro de tasas |
| `ClienteValidaciones` | Validaciones |
| `ErrorClienteException` | Error controlado con código y mensaje del servidor |

Las llamadas se ejecutan en `javafx.concurrent.Task` mediante
`util.TareasSegundoPlano`, nunca en el hilo de JavaFX. El token no se registra
en logs ni se guarda en disco.

## Seguridad

- Escucha únicamente en `127.0.0.1`; el host no es configurable.
- Tokens opacos de 32 bytes, guardados solo como huella SHA-256, con expiración.
- Contraseñas con PBKDF2-HMAC-SHA256 (600 000 iteraciones, sal aleatoria) y
  comparación en tiempo constante.
- Rol y estado del usuario revalidados en cada operación.
- Una petición HTTP nunca hereda la sesión global de JavaFX: si la identidad
  aislada no es válida, la operación falla cerrada con `403`.
- Límite de cuerpo, `Content-Type` obligatorio y errores sin detalles técnicos.
- Sin cabeceras CORS: un navegador no puede consumir la API desde otro origen.
- `Cache-Control: no-store` y `X-Content-Type-Options: nosniff` en las respuestas.
- Nunca se registran contraseñas, tokens ni la cabecera `Authorization`.