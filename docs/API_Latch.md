# API local de Latch

Servicio HTTP REST en `latch-api` (Spring Boot WebMVC empaquetado como `ROOT.war` para Apache Tomcat 11). La interfaz JavaFX del repositorio principal lo
consume por separado con `java.net.http.HttpClient`. El servidor escucha **solo** en
`127.0.0.1` y el cliente resuelve la URL mediante `LATCH_API_BASE_URL` (por defecto `http://127.0.0.1:8080`, o p. ej. `http://127.0.0.1:<LATCH_API_PORT>/latch-api` si se despliega en subcontexto).

- Base recomendada (`ROOT.war`): `http://127.0.0.1:<LATCH_API_PORT>` (subcontexto soportado normalizando `request.getContextPath()` en `FiltroApi`)
- Versión de rutas: `/api/v1`
- Formato: JSON UTF-8 (`application/octet-stream` únicamente para `POST /api/v1/adjuntos`)
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
- Tras 5 intentos fallidos para el usuario o 25 desde el mismo origen, el login
  queda bloqueado durante 15 minutos (`429`). Un login correcto restablece ambos
  contadores. Los fallos se auditan sin guardar usuario ni contraseña en claro.
- La cabecera `Host` debe coincidir con `127.0.0.1:<puerto>` y el servidor cierra
  solicitudes que tarden más de 30 segundos en recibirse por completo.
- Las rutas protegidas validan el token antes de devolver `404` o `405`; sin
  sesión válida, esos casos responden `401`.
- El mensaje de credenciales inválidas es idéntico exista o no el usuario.
- Si la contraseña temporal es válida y la cuenta tiene un cambio obligatorio,
  el login devuelve `409 CAMBIO_PASSWORD_REQUERIDO` sin emitir token. Repetir
  el mismo POST con `password` (la temporal) y `nuevaPassword` (12–128
  caracteres, distinta de la temporal). El token se emite después de guardar
  la nueva contraseña y registrar el cambio. La contraseña anterior deja de servir.

## Convenciones de la petición

| Regla | Valor |
| --- | --- |
| `Content-Type` | `application/json` obligatorio en cuerpos JSON (`415` si falta); binario acotado en `POST /api/v1/adjuntos` |
| Cuerpo máximo | 256 KiB para JSON (`413`); excepción explícita de **5 MiB** únicamente para subida de adjuntos en `POST /api/v1/adjuntos` |
| Idempotencia | Cabecera `Idempotency-Key: <UUID>` en escrituras financieras |
| Importes | Texto decimal con 2 decimales (`"1000.00"`); nunca números flotantes |
| Tasas | Texto decimal con 8 decimales (`"36.62430000"`) |
| Fechas | `AAAA-MM-DD` |
| Instantes de operación | ISO-8601 con zona `America/Managua` (`2026-11-13T13:30-06:00`); `expiraEn` de login sigue en UTC |
| Identificadores | UUID como texto |

### Idempotencia

`POST /api/v1/pagos` exige `Idempotency-Key` (UUID).

- Misma clave + mismo usuario + mismo contenido → se repite la respuesta original, sin duplicar el pago.
- Misma clave con contenido, usuario o recurso distinto → `409 CONFLICTO`.
- La clave se persiste en `solicitud_idempotente` dentro de la misma transacción del pago.
- Si la solicitud falla, no queda registro de idempotencia ni pago.
- La interfaz JavaFX genera un UUID por solicitud sin cambios y lo conserva para reintentos; al completar el pago, una nueva operación recibe una clave nueva.
- Las respuestas completadas se conservan 30 días desde `completado_en`. Antes de cada registro se eliminan las completadas anteriores al límite; `EN_PROCESO` no se elimina. Tras la limpieza, una clave vencida puede registrar una nueva operación.

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
| 409 | `CAMBIO_PASSWORD_REQUERIDO` | Contraseña temporal válida. Repetir el login con la nueva contraseña |
| 413 | `CUERPO_DEMASIADO_GRANDE` | Cuerpo > 256 KiB |
| 415 | `CONTENIDO_NO_SOPORTADO` | `Content-Type` distinto de JSON |
| 422 | `ENTIDAD_NO_PROCESABLE` | Regla de negocio incumplida |
| 429 | `DEMASIADOS_INTENTOS` | Bloqueo temporal de login |
| 503 | `ERR-057` | La fecha del equipo difiere más de cinco minutos de PostgreSQL |
| 500 | `ERROR_INTERNO` | Fallo técnico; nunca incluye SQLSTATE ni stack trace |

## Resumen de endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | pública |
| POST | `/api/v1/auth/logout` | autenticado |
| POST | `/api/v1/auth/cambiar-password` | autenticado (cuenta propia) |
| GET | `/api/v1/salud` | pública (solo `127.0.0.1`) |
| GET | `/api/v1/tasas` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/tasas/vigente?fecha=AAAA-MM-DD` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/tasas/referencia` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/tasas/{id}` | `CONSULTAR_REPORTES` |
| POST | `/api/v1/tasas` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/tasas/{id}/revisiones` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/tasas/{id}/autorizar` | `AUTORIZAR_TASA` (solo propietario) |
| GET | `/api/v1/propiedades` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/propiedades` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/propiedades/{id}` | `OPERAR_ALQUILERES` |
| DELETE | `/api/v1/propiedades/{id}` | `ELIMINAR_CATALOGOS` (solo propietario) |
| GET | `/api/v1/unidades`, `/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/unidades` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/unidades/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/unidades/{id}` | `OPERAR_ALQUILERES` |
| DELETE | `/api/v1/unidades/{id}` | `ELIMINAR_CATALOGOS` (solo propietario) |
| GET | `/api/v1/inquilinos`, `/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/inquilinos/{id}/historial` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/inquilinos/coincidencias?identificacion=...&excluirId=...` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/inquilinos` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/inquilinos/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/inquilinos/{id}` | `OPERAR_ALQUILERES` |
| DELETE | `/api/v1/inquilinos/{id}` | `ELIMINAR_CATALOGOS` (solo propietario) |
| GET | `/api/v1/contratos`, `/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/contratos/{id}` | `OPERAR_ALQUILERES` |
| PATCH | `/api/v1/contratos/{id}` | `OPERAR_ALQUILERES` |
| DELETE | `/api/v1/contratos/{id}` | `ELIMINAR_CATALOGOS` (solo propietario) |
| POST | `/api/v1/contratos/{id}/activar` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos/{id}/cancelar` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos/{id}/renovar` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos/{id}/terminar` | `OPERAR_ALQUILERES` (`LIQUIDAR_GARANTIA` si hay depósito > 0 o movimientos) |
| GET | `/api/v1/contratos/{id}/garantia` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/contratos/{id}/garantia/movimientos` | `LIQUIDAR_GARANTIA` + `Idempotency-Key` / `liquidacionId` |
| GET | `/api/v1/garantias` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/validaciones` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/pagos` | `OPERAR_ALQUILERES` + `Idempotency-Key` |
| GET | `/api/v1/pagos?pagina=1&tamano=50&texto=...` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/cargos?pagina=1&tamano=50&texto=...` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/cargos/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/cargos` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/cargos/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/cargos/cartera?corte=AAAA-MM-DD&contratoId=...&periodo=AAAA-MM&moneda=...&texto=...` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/cargos/generacion-mes/vista-previa?periodo=AAAA-MM` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/cargos/generacion-mes` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/cargos/revisar-mora` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/cargos/{id}/anulacion` | `ANULAR_CARGOS` (solo propietario) |
| GET | `/api/v1/pagos/{id}` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/recibos/{numero}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/pagos/{id}/anulacion` | `ANULAR_PAGOS` (solo propietario) |
| POST | `/api/v1/pagos/aplicaciones/{id}/reversion` | `ANULAR_PAGOS` (solo propietario) |
| GET | `/api/v1/contratos/{id}/estado-cuenta?corte=AAAA-MM-DD` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/mantenimiento/solicitudes` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/mantenimiento/solicitudes` | `OPERAR_ALQUILERES` |
| PUT | `/api/v1/mantenimiento/solicitudes/{id}` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/mantenimiento/solicitudes/{id}/estado` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/mantenimiento/gastos` | `OPERAR_ALQUILERES` |
| POST | `/api/v1/mantenimiento/gastos` | `OPERAR_ALQUILERES` |
| GET | `/api/v1/dashboard/metricas` | `CONSULTAR_REPORTES` |
| GET / POST / PUT / PATCH | `/api/v1/usuarios/**` (`desbloquear`, `rol`, `estado`, `restablecer-password`) | `GESTIONAR_USUARIOS` (solo propietario) |
| GET | `/api/v1/configuracion/inactividad` | autenticado |
| GET / PUT | `/api/v1/admin/emisor` | `CAMBIAR_CONFIGURACION` (solo propietario) |
| GET / POST | `/api/v1/admin/parametros` | `CAMBIAR_CONFIGURACION` (solo propietario) |
| GET | `/api/v1/admin/auditoria` | `CONSULTAR_AUDITORIA` (solo propietario) |
| GET / POST | `/api/v1/admin/respaldos/estado`, `/respaldos`, `/verificar`, `/restaurar` | `CAMBIAR_CONFIGURACION` (solo propietario) |
| GET / POST / DELETE | `/api/v1/adjuntos`, `/adjuntos/{id}/contenido`, `/adjuntos/{id}` | `OPERAR_ALQUILERES` (`CAMBIAR_CONFIGURACION` para `EMISOR_LOGO`) |
| POST / GET / DELETE | `/api/v1/reportes/generar`, `/reportes/documentos/{id}/paginas/{indice}`, `/pdf`, `/xlsx`, `/{id}` | `CONSULTAR_REPORTES` |
| GET | `/api/v1/recibos/buscar?numero=&texto=` | `CONSULTAR_REPORTES` |

Las rutas `PUT` requieren el campo `version` del registro leído; si otro usuario
lo modificó, responden `409` y el cliente debe recargar.

## Catálogos

Listados paginados con `pagina` (1..N), `tamano` (1..50) y filtros propios:

| Recurso | Filtros |
| --- | --- |
| `propiedades` | `texto`, `activa` |
| `unidades` | `propiedadId`, `estado`, `tipo`, `texto` |
| `inquilinos` | `texto`, `activo` |
| `contratos` | `estado`, `unidadId`, `inquilinoId`, `texto` |

Respuesta:

```json
{
  "elementos": [ { "…": "…" } ],
  "pagina": 1,
  "tamano": 20,
  "total": 3
}
```

`PATCH` hace baja lógica o cambio de estado, según el recurso. El cuerpo debe
incluir el campo correspondiente (`activa`, `activo` o `estado`); si falta o
su valor no es válido, la API responde `422` y conserva el estado actual.

## Tasas de cambio

La referencia comercial se consulta con `GET /api/v1/tasas/referencia`. La ruta
devuelve `200` con `estado` (`INFORMATIVA`, `HISTORICA`, `CONSULTANDO` o
`NO_DISPONIBLE`). Si hay referencia incluye `fuente`, `nioPorUsd` como texto
de ocho decimales, `fechaEfectiva`, `consultadaEn` con desplazamiento de
Managua y `antiguedadSegundos`. Puede incluir un `aviso` sobre el último
incidente de H3. La consulta lee la caché. La operación de pago usa la tasa
autorizada para su fecha efectiva.

Las tasas son inmutables. Registrar una tasa crea una instantánea
**informativa**. Cada fecha abierta admite una propuesta vigente. Se puede
corregir su importe y fuente con `POST /api/v1/tasas/{id}/revisiones`, usando
la misma fecha y el mismo cuerpo que al registrar. Responde `201` con el ID
de la nueva versión y conserva la anterior en el historial. Una versión
anterior devuelve `409` al editarla o intentar autorizarla.

La autorización por el propietario conserva la propuesta, crea la tasa
autorizada de esa fecha y cierra el registro y la edición para esa misma
fecha efectiva. Ambos intentos posteriores devuelven `409`.
`GET /api/v1/tasas?vista=operativa` muestra una fila por fecha, con la tasa
autorizada si existe o la propuesta vigente. La consulta sin `vista`
conserva acceso al historial completo sujeto al límite de la consulta.

```http
POST /api/v1/tasas
{ "fecha": "2026-11-13", "tasa": "36.62430000", "fuente": "BCN" }

POST /api/v1/tasas/{id}/autorizar
{ "motivo": "Tasa oficial del día", "confirmarFueraDeBanda": false }
```

No se admiten fechas futuras (`422`). Autorizar dos veces la misma fecha
devuelve `409` con código `ERR-060` y el mensaje `MSG-083`, sin insertar otro
registro. La banda 1–100 NIO/USD es inclusiva y solo es una advertencia: fuera
de ella el propietario debe enviar `"confirmarFueraDeBanda": true`; la base de
datos no impone un `CHECK` de banda, solo de tasa positiva.

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
  saldo. No se permiten cargos repetidos. También puede enviarse
  `"aplicacionAutomatica": true` (sin `distribucion`) para abonar primero los
  cargos vencidos más antiguos (por `venceEl` e `id`).
- `credito = montoContrato − Σ aplicaciones`.
- El recibo y la auditoría se crean en la misma transacción.

`GET /api/v1/pagos/{id}` devuelve el detalle con `aplicaciones`, `numeroRecibo`,
`credito` y, si fue anulado, `motivoAnulacion`, `anuladoPor` y `anuladoEn`.

`GET /api/v1/pagos` y `GET /api/v1/cargos` devuelven páginas de hasta 50 filas.
`pagina` empieza en 1, `tamano` usa 50 por defecto y rechaza valores mayores
que 50; `texto` filtra por contrato, referencia/método en pagos y por contrato,
concepto/tipo en cargos. La respuesta tiene `pagina`, `tamano`, `total` y
`elementos`; fechas e importes son cadenas para conservar su formato.

`GET /api/v1/recibos/{numero}` devuelve el recibo con importes como texto.

`POST /api/v1/pagos/{id}/anulacion` exige `{ "motivo": "…" }`, solo lo puede
hacer el propietario, no se puede repetir (`409`) y queda auditado. Si el pago
tiene crédito aplicado a cargos posteriores activos, exige revertir primero esas
aplicaciones mediante `POST /api/v1/pagos/aplicaciones/{id}/reversion` con
`{ "motivo": "…" }`.

### Generación mensual, mora y cartera (M05)

- `GET /api/v1/cargos/generacion-mes/vista-previa?periodo=AAAA-MM` devuelve la
  vista previa sin persistir cargos (canon, vencimiento ajustado, omisiones y
  crédito aplicable).
- `POST /api/v1/cargos/generacion-mes` recibe `{ "periodo": "AAAA-MM" }` y
  genera rentas con una transacción por contrato, aplicando automáticamente
  crédito disponible en la misma moneda.
- `POST /api/v1/cargos/revisar-mora` recibe `{}` o `{ "fecha": "AAAA-MM-DD" }`
  y ejecuta la revisión H2 de vencimientos contractuales (`Por vencer` /
  `Vencido`) y mora única sobre principal vencido tras la gracia.
- `GET /api/v1/cargos/cartera` admite `corte`, `contratoId`, `periodo`, `moneda`
  y `texto`; agrupa saldos por tramos (`Al día`, `1 a 30`, `31 a 60`, `61 a 90`,
  `Más de 90`) con totales separados por moneda.
- `POST /api/v1/cargos/{id}/anulacion` exige `{ "motivo": "…" }`, solo lo
  puede hacer el propietario y rechaza (`422`) cargos con aplicaciones activas.

### Estado de cuenta

`GET /api/v1/contratos/{id}/estado-cuenta?corte=2026-11-30`

Devuelve cargos con importe/aplicado/saldo, pagos con aplicado/crédito,
totales y garantía, todo en la moneda del contrato. Los cargos cancelados no
entran en los totales y los pagos anulados no aportan aplicado ni crédito.

## Mantenimiento y gastos (M08)

- `GET /api/v1/mantenimiento/solicitudes` admite `propiedadId`, `unidadId`,
  `prioridad` (`Baja`, `Media`, `Alta`) y `estado` (`Abierta`, `En proceso`,
  `Resuelta`, `Cancelada`).
- `POST /api/v1/mantenimiento/solicitudes` crea una solicitud `Abierta` por
  unidad (sin exigir unidad disponible ni cambiar su estado).
- `PUT /api/v1/mantenimiento/solicitudes/{id}` requiere `version`; responde `409`
  ante conflicto optimista y `422 ERR-066` si la solicitud ya está en estado
  terminal.
- `POST /api/v1/mantenimiento/solicitudes/{id}/estado` aplica la máquina RN-27
  (`Abierta` → `En proceso` | `Cancelada`; `En proceso` → `Resuelta` |
  `Cancelada`). Repetir el estado actual es idempotente; otra transición responde
  `422 ERR-066`. `Cancelada` exige `motivo` y conserva gastos confirmados;
  `Resuelta` exige detalle de trabajo y `fechaCierre`.
- `GET /api/v1/mantenimiento/gastos` admite `propiedadId`, `desde`, `hasta` y
  `moneda` (`USD` o `NIO`) y devuelve `totalUsd` y `totalNio` separados.
- `POST /api/v1/mantenimiento/gastos` registra un gasto efectivo de propiedad
  sin crear cargos al inquilino ni modificar garantías. Si `solicitudId`
  pertenece a una unidad de otra propiedad, responde `422 ERR-065`.

## Indicadores

`GET /api/v1/dashboard/metricas` (acepta `?periodo=AAAA-MM` opcional para filtrar cargos por cobrar y pagos registrados por período).

```json
{
  "propiedades": 1, "unidades": 1, "unidadesActivas": 1, "unidadesOcupadas": 1,
  "porcentajeOcupacion": "100.00", "instanteGeneracion": "2026-11-25T09:30:00-06:00",
  "periodo": "2026-11", "inquilinos": 1, "contratosVigentes": 1,
  "contratosPorVencer": 0, "contratosVencidos": 0,
  "cargosPendientes": [ { "moneda": "USD", "cantidad": 2, "total": "1000.00" } ],
  "principalPendiente": [ { "moneda": "USD", "cantidad": 2, "total": "1000.00" } ],
  "moraExigible": [ { "moneda": "USD", "cantidad": 0, "total": "0.00" } ],
  "pagosRegistrados": [ { "moneda": "USD", "cantidad": 1, "total": "1000.00" } ],
  "tasaDisponible": {
    "disponible": true, "estado": "AUTORIZADA", "valor": "36.62430000",
    "origen": "Autorizada · Banco Central", "fechaEfectiva": "2026-11-25",
    "antiguedadSegundos": 120, "autorizadaParaPagos": true
  }
}
```

Los totales, porcentajes y tasas siempre van separados por moneda y como texto decimal; nunca se
suman USD con NIO. Si `?periodo` no usa el formato `AAAA-MM`, responde `400 SOLICITUD_INVALIDA`.

## Reportes y recibos

- `POST /api/v1/reportes/generar` admite los 19 documentos del Centro de Reportes (`RECIBO` solo PDF; los 18 restantes en PDF y XLSX). `TASAS_CATALOGO` acepta `desde` y `hasta` opcionales (`AAAA-MM-DD`, validando `hasta >= desde`).
- `GET /api/v1/recibos/buscar?numero=&texto=` devuelve hasta 100 recibos ordenados del más reciente al más antiguo (incluyendo su estado `anulado`); `texto` busca por número (incluso con prefijo `#`), inquilino, contrato, referencia o método.

## Validaciones

`POST /api/v1/validaciones`

```json
{ "tipo": "correo", "valor": "alguien@dominio.com" }
```

Tipos: `correo`, `telefono`, `cedula`, `monto`, `rangoFechas`. Para
`rangoFechas` se envían `inicio`, `fin` y `mesesMinimos` opcional. `valor`
admite hasta 255 caracteres; una solicitud más larga devuelve
`400 SOLICITUD_INVALIDA`.

```json
{ "valido": true, "mensaje": null }
```

## Uso desde JavaFX

El cliente HTTP está en el paquete `cliente` del repositorio principal. Los DTO HTTP se compilan junto al escritorio.

| Clase | Uso |
| --- | --- |
| `ClienteConfig` | Resuelve y valida `latch.api.baseUrl` / `LATCH_API_BASE_URL` |
| `ClienteLatch` | Inicia la URL base y expone los subclientes HTTP |
| `SesionCliente` | Identidad en memoria del cliente FX (sin contraseñas ni BD) |
| `ClienteApi` | GET/POST/PUT/PATCH/DELETE + binarios, token en memoria, timeouts 5 s / 10 s y 60 s para reportes |
| `ClienteAuth` | Login, cambio de contraseña y logout |
| `ClienteCatalogos` | Paginación, CRUD completo, historial/coincidencias de inquilinos, garantías y eliminaciones físicas |
| `ClientePagos` | Cargos (CRUD + H1), historial, registro de pago, detalle, recibo y anulación |
| `ClienteMantenimiento` | Solicitudes de mantenimiento, transiciones y gastos de propiedad |
| `ClienteTasas` | Consulta, registro y autorización de tasas |
| `ClienteAdmin` | Usuarios, configuración/emisor, auditoría y respaldos |
| `ClienteAdjuntos` | Consulta, subida binaria (≤ 5 MiB), descarga y desactivación de adjuntos |
| `ClienteReportes` | Generación remota, render PNG bajo demanda, descarga PDF/XLSX, limpieza y búsqueda de recibos |
| `ClienteValidaciones` | Validaciones |
| `ErrorClienteException` | Error controlado con código, estado HTTP (`0 SIN_CONEXION` si API caída) y mensaje |

Las llamadas se ejecutan en `javafx.concurrent.Task` mediante
`util.TareasSegundoPlano`, nunca en el hilo de JavaFX. El token no se registra
en logs ni se guarda en disco. Ante un `401` del token activo, JavaFX vuelve
al login. Tras un PUT de inquilino, JavaFX recarga la lista para leer la
versión persistida. Ante un conflicto de edición `409`, recarga el registro y
pide abrirlo de nuevo.

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
