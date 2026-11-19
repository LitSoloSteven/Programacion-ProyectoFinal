# Revisión de seguridad — Latch

Documento de cierre de la Fase 5. Resume los controles implementados, cómo se
verifican y los riesgos que quedan aceptados para una aplicación académica de
escritorio.

## Modelo de amenazas

Latch es una aplicación local para un solo equipo:

- La API escucha únicamente en `127.0.0.1`.
- La base PostgreSQL puede ser local o accesible en la red local.
- Los usuarios son el propietario y sus asistentes.
- No hay exposición a Internet ni multiusuario remoto.

Las amenazas relevantes son: acceso desde otro proceso local, robo de tokens en
memoria, elevación de privilegios por rol, inyección de consultas, fuga de datos
en errores o logs, duplicación de cobros y manipulación de importes.

## Controles implementados

### Autenticación y contraseñas

- PBKDF2-HMAC-SHA256 con 600 000 iteraciones, sal aleatoria de 16 bytes y
  comparación en tiempo constante (`PasswordHasher`).
- Verificación contra una huella ficticia cuando el usuario no existe, para no
  filtrar su existencia por tiempo de respuesta.
- Mensaje único `CREDENCIALES_INVALIDAS` para usuario inexistente o contraseña
  incorrecta.
- Bloqueo de 15 minutos tras 5 fallos por usuario (`ControlIntentosLogin`).
- La contraseña se convierte a `char[]`, se usa y se limpia con `Arrays.fill`.
- Nunca se registra la contraseña ni su huella.

### Tokens de sesión

- Token opaco de 32 bytes generado con `SecureRandom`.
- Solo se almacena su huella SHA-256 en memoria; el valor original no se
  persiste ni se escribe en disco.
- Vigencia de 15 minutos verificada en cada petición.
- `logout` revoca el token de inmediato.
- Cada token conserva la versión de la cuenta al iniciar sesión. Un cambio de
  rol, desactivación u otra modificación versionada invalida todas las sesiones
  anteriores; restaurar el rol o reactivar la cuenta no rehabilita esos tokens.
- Se rechazan tokens vacíos o de longitud anómala.
- Los tokens se pierden al reiniciar el servidor (comportamiento intencional).

### Autorización

- Cada operación de negocio llama a `PermisoService.exigir` **dentro** de su
  transacción.
- El rol y el estado `activo` se leen de la base en cada petición; no se confía
  en el rol guardado en el token ni en la interfaz.
- Permisos exclusivos del propietario: `AUTORIZAR_TASA`, `ANULAR_PAGOS`,
  `GESTIONAR_USUARIOS`, `CAMBIAR_CONFIGURACION`, `CONSULTAR_AUDITORIA`.
- El asistente solo tiene `OPERAR_ALQUILERES` y `CONSULTAR_REPORTES`.

### Aislamiento de sesión

- La API identifica cada petición con `SesionUsuario.ejecutarComo`, que usa un
  `ThreadLocal`.
- El login HTTP **no** modifica la sesión global de JavaFX.
- Un contexto aislado nunca cae en la sesión global: si la identidad no es
  válida, la operación falla cerrada con `403`.
- El contexto se restaura en `finally`, incluso si la operación lanza una
  excepción.

### Entrada y salida

- `Content-Type: application/json` obligatorio (`415`).
- Cuerpo limitado a 256 KiB, comprobando `Content-Length` y el flujo real
  (`413`).
- Consultas HQL siempre parametrizadas; no se concatenan valores.
- Importes como `BigDecimal` con escala 2 y `HALF_EVEN`; tasas con escala 8.
- Errores uniformes sin SQLSTATE, nombres de tablas, stack traces ni causas
  técnicas.
- Sin CORS, con `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`.

### Escrituras financieras

- `Idempotency-Key` obligatoria y validada como UUID.
- Huella SHA-256 canónica del contenido de la petición.
- Registro persistente en la misma transacción del pago: si el pago falla, no
  queda idempotencia.
- La misma clave con otro usuario o contenido devuelve `409`; nunca revela la
  respuesta de otro usuario.
- Distribución de pagos validada contra cargos del mismo contrato, sin
  duplicados, sin cargos cancelados y sin superar saldos.
- Anulación exclusiva del propietario, con motivo obligatorio y auditoría.
- Instante y fecha operativa desde PostgreSQL; la fecha efectiva del pago queda
  separada. Un desfase absoluto mayor de cinco minutos bloquea las nuevas
  escrituras con `ERR-057` antes de persistir.
- Los instantes de operación se presentan en `America/Managua`; la base conserva
  el instante UTC. `pago.creado_en` requiere aplicar V10.

### Auditoría

- Inicio de sesión, registro de pagos, anulación, tasas registradas y
  autorizadas, y operaciones de catálogo.
- La auditoría participa en la misma transacción que la operación: si falla, no
  queda un registro de éxito.

## Verificación automatizada

`ApiSeguridadTest` comprueba:

1. `logout` revoca el token.
2. Token ausente, inventado, alterado o demasiado largo devuelve `401` con
   `WWW-Authenticate: Bearer`.
3. Cabeceras seguras y ausencia de CORS.
4. `Content-Type` obligatorio y límite de 256 KiB.
5. Rutas inexistentes (`404`) y métodos no permitidos (`405`).
6. Los errores no contienen SQLSTATE, excepciones, Hibernate ni rutas internas.
7. Cambiar el rol invalida el token anterior (degradarlo bloquea la autorización
   de tasas y restaurarlo no rehabilita ese token).
8. Desactivar y luego reactivar al usuario tampoco rehabilita el token anterior.
9. El login HTTP no modifica la sesión global de JavaFX.
10. La respuesta de login no contiene contraseña, hash ni PBKDF2.
11. Credenciales inválidas no revelan si el usuario existe.
12. Cinco fallos para una cuenta y 25 desde un origen bloquean el login con `429`.
13. Cada credencial inválida genera auditoría con identificador hash y origen,
    sin asociar el evento a una cuenta que podría no existir.
14. `Host` distinto de `127.0.0.1:<puerto>` se rechaza antes de resolver rutas.
15. Las rutas protegidas devuelven `401` antes de distinguir una ruta o método
    inexistente; el servidor limita a 30 segundos la recepción completa.
16. El reloj desfasado se devuelve como `503 ERR-057` sin detalles técnicos y
    los instantes de tasas se presentan con el desplazamiento de Managua.

Además, `ApiPagosTest` verifica que una `Idempotency-Key` de otro usuario
devuelve `409` y no duplica el pago, y `SesionUsuarioTest` cubre el aislamiento
y el fallo cerrado del contexto.

`RelojServiceTest` comprueba la lectura de hora de BD, la conversión de zona,
ambos lados del umbral de cinco minutos y el bloqueo. `ApiValidacionesTest`
comprueba el rechazo de `valor` por encima de 255 caracteres y el manejo de un
`mesesMinimos` extremo sin propagar `DateTimeException`.

La evidencia de regresión de Spring MVC y PostgreSQL se registra en
[el cierre técnico de la fase 5](migracion-spring-fase-5.md).

## Riesgos residuales aceptados

| Riesgo | Motivo | Mitigación |
| --- | --- | --- |
| Sin TLS | Tráfico solo por loopback | No exponer la API fuera de `127.0.0.1` |
| Token en memoria del proceso | Requisito de sesión local | Expiración de 15 min y `logout` |
| Sin refresh token | Simplicidad y superficie mínima | Reautenticación al expirar |
| Credenciales de BD en `.env` | Configuración local del equipo | `.env` ignorado por Git; permisos del sistema operativo |
| Huecos en la numeración de recibos | `GenerationType.IDENTITY` con rollback | Documentado; no afecta la unicidad |
| Un solo nodo | Aplicación de escritorio | La tabla de idempotencia es compartida si se agregara otro nodo |
| Operaciones de escritorio sin ruta HTTP | Alcance de 36 operaciones | Los servicios locales aplican permisos dentro de sus transacciones |

## Recomendaciones futuras

- Emitir tokens con un identificador de sesión revocable desde el servidor.
- Firmar los recibos o exportarlos a PDF con verificación.
- Rotar credenciales de base de datos y usar un usuario con permisos mínimos.
- Sustituir el pool básico de Hibernate por uno configurado para producción.
