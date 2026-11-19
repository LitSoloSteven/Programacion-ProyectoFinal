# Migración de Latch a Spring Boot · Fase 7

## HTTP 500 de login

La fase se ejecutó en `Steven`, sobre `2d00beb` con las fases 5 y 6
presentes como cambios locales. Se conservaron los cambios del checkout.

| Evidencia histórica | Petición y resultado |
| --- | --- |
| Fase 5, `fase5-verify-postgres-segundo-intento-final.log`, líneas 969–970 | `ApiFase1Test.bloqueaTrasCincoFallos:186` y `ApiSeguridadTest.bloqueaTrasCincoIntentosFallidos:373` esperaban 401 y recibieron 500 en Surefire con H2. |
| Fase 6, `fase6-bloque-1.log`, líneas 157–168 y 867 | `ApiSeguridadTest.bloqueaTrasCincoIntentosFallidos:373` volvió a recibir 500. El reintento histórico aprobó las 158 pruebas. |

En ambos métodos, la aserción está dentro del bucle de cinco
`POST /api/v1/auth/login` con credenciales sintéticas inválidas. Cada petición
debe devolver 401. El sexto intento debe devolver 429. Las corridas fallidas
no identifican el intento exacto dentro del bucle ni registran la excepción
original asociada a esa respuesta.

El controlador lee el cuerpo y llama al servicio en
`api/piloto/LoginController.java:27–30`. El servicio registra el fallo y
traduce las credenciales inválidas en
`api/auth/ServicioLogin.java:39–42`. El filtro y el manejador MVC convierten
las excepciones inesperadas en un error interno sanitizado mediante
`api/piloto/RespuestasApi.java:52–62`. El código HTTP histórico no permite
determinar en cuál de esos pasos se produjo el fallo.

- `ApiSeguridadTest` y `ApiFase1Test` crean cada una una `SessionFactory` con
  una base H2 en memoria de nombre único, `MODE=PostgreSQL` y `create-drop`.
  Cada clase crea su propio contexto Spring con esa fábrica, un Tomcat en
  puerto efímero y su cliente HTTP. Cierran el contexto antes de cerrar la
  fábrica. El contexto y los datos se comparten entre métodos de una misma
  clase. En `ApiSeguridadTest`, el fixture restaura rol y estado de sus dos
  cuentas después de cada método. El login fallido usa un nombre sintético
  nuevo y el contador por origen sigue siendo local al contexto.
- En el bloque original, `ApiCatalogosTest`, `ApiFase1Test` y
  `ApiPagosTest` precedieron a `ApiSeguridadTest`. El método aislado aprobó
  con un contexto nuevo. La repetición del bloque conservó las 22 clases y
  su orden. Las 158 pruebas aprobaron, así que ese orden por sí solo no
  reprodujo el fallo. El nombre único de cada H2 descarta que esas dos
  clases se conecten a la misma base en sus fixtures normales.
- La instrumentación temporal correlacionó puerto, ruta de una lista
  cerrada e intento sintético con la clase de excepción y sus marcos.
  Registró `AsyncRequestNotUsableException` → `ClientAbortException` →
  `ClosedChannelException` al escribir la respuesta de otra ruta. No hubo
  aserción fallida ni registro de 500 para la petición de login. Se retiró la
  instrumentación antes de las verificaciones finales.
- El sandbox bloqueó el primer arranque de Tomcat con
  `Permission denied: connect` antes de enviar ninguna petición. La prueba
  aislada aprobó con acceso local a loopback.

En la primera ronda faltó correlacionar un 500 de login con la excepción
del servidor. El 500 histórico de tasas sigue sin atribución.

### Seguimiento del HTTP 500 de login

Los informes históricos de las fases 5 y 6 coinciden en el orden de las
primeras cuatro clases: `ApiCatalogosTest`, `ApiFase1Test`, `ApiPagosTest` y
`ApiSeguridadTest`. El resumen histórico de Surefire
`fase6-bloque-1-seguridad.txt:4–14` conserva la aserción del cliente, sin
ordinal ni traza del servidor. El XML de la verificación exitosa de fase 7
sitúa `bloqueaTrasCincoIntentosFallidos` inmediatamente después de
`logoutRevocaElTokenDeInmediato`. La nueva corrida conservó ese orden. Los
artefactos históricos disponibles no permiten confirmar ese orden de
métodos para la corrida fallida.

La reproducción usó una copia temporal de `Steven`, H2 y el prefijo de
cuatro clases. Añadió identificadores sintéticos por intento en los dos
métodos históricos y registró el estado del cliente, el punto de traducción
de Spring o Tomcat y, para excepciones relevantes, sus clases y marcos.
Los registros de diagnóstico conservan metadatos. Las aserciones y los
datos de los fixtures se mantuvieron.

Comando en la copia temporal:
`./mvnw.cmd test '-Dtest=ApiCatalogosTest,ApiFase1Test,ApiPagosTest,ApiSeguridadTest'`.

**500 correlacionado.** En
`ApiSeguridadTest.bloqueaTrasCincoIntentosFallidos`, los intentos `S1` y
`S2` devolvieron 401. En `S3`, el cliente recibió 500 por HTTP/1.1.
`RespuestasApi.manejar` registró el mismo identificador y un
`NullPointerException`. El primer marco fue `FiltroApi$2.read`, línea
120 de la copia instrumentada, que corresponde a
`api/piloto/FiltroApi.java:114` en `Steven`. La llamada
`recepcion.antesDeLeer()` desreferenció un estado de recepción ausente.
El fallo ocurrió al leer el cuerpo, antes de autenticar o consultar H2
para esa petición. `RespuestasApi.java:21–24,52–62` convirtió la
excepción en el 500 que registró el cliente. Evidencia
`fase7-login-base-metadatos.log:26–37`.

`ProtocoloApi.java:58–60` obtiene el estado del mapa del socket y lo
publica como atributo de la petición. `FiltroApi.java:107–114` lo recupera.
La captura anterior demuestra su ausencia al desreferenciarlo. Para
atribuirla a una condición del ciclo de vida todavía hay que observar
si el mapa ya estaba vacío al entrar al procesador o si el atributo se
perdió antes de llegar al filtro. También se observó un 500 de catálogo
en la primera corrida, sin traza asociada. Queda sin atribución.

| Corrida dirigida | Única variación respecto a la anterior | Resultado |
| --- | --- | --- |
| `fase7-login-base-metadatos.log` | Prefijo histórico de cuatro clases con correlación por intento | 500 en `S3`. Los seis intentos de `ApiFase1Test` devolvieron cinco 401 y un 429. |
| `fase7-login-estado-metadatos.log` | Observación de presencia en el mapa, Coyote y servlet | 56 aprobadas. Estado presente en los 12 intentos marcados. |
| `fase7-login-repeticiones-metadatos.log` | Cuatro invocaciones por método histórico, dentro de sus fixtures | 62 aprobadas y 48 respuestas marcadas. |
| `fase7-login-diferido-metadatos.log` | Emisión de las marcas de presencia solo al faltar el estado o al recibir 500, para reducir el efecto de la salida durante la lectura | 62 aprobadas y 48 respuestas marcadas. |

Después de la reproducción se observaron 90 respuestas 401 y 18 respuestas
429 en las tres corridas dirigidas. Los informes nuevos confirman que las
repeticiones de `ApiSeguridadTest` siguieron inmediatamente al logout.
El canal cerrado de la primera ronda queda separado del 500 correlacionado.
Al terminar esa ronda quedaba por identificar por qué faltó el estado de
recepción. La instrumentación solo se ejecutó en la copia temporal.

### Origen del estado ausente y corrección

El mapa `Entrada.pendientes` usa como clave la instancia
`SocketWrapperBase`. En `ProtocoloApi.java:75–95`, `Entrada.processSocket`
crea y publica una `Recepcion` para un socket abierto al recibir
`OPEN_READ`. `Http11Processor.service` consulta el mapa y publica el
objeto en el atributo `RECEPCION` de Coyote antes de delegar en Tomcat
(`ProtocoloApi.java:54–70`). Catalina expone ese atributo a
`FiltroApi.leerCuerpo` (`FiltroApi.java:107–126`).

Al completarse el cuerpo, `Recepcion.completar` verifica el tiempo
restante y cancela la tarea. El adaptador también la completa si la
petición ya está recibida. Si vence el plazo, la tarea cierra el socket
incompleto y quita su entrada con `remove(socket, recepcion)`. Una
lectura que alcanza el plazo también puede cerrar el socket. El
`finally` del procesador quita la entrada y cancela la tarea cuando
`service` devuelve un estado distinto de `LONG`
(`ProtocoloApi.java:66–69,85–93,105–134,159–161`).

**Causa demostrada.** El personalizador de Spring Boot sobrescribía el
límite de una petición por conexión de `ProtocoloApi`. La traza del
arranque registró **1** al configurar el protocolo y **100** durante el
procesamiento. Con la conexión persistente activa, Tomcat 11.0.27 podía
atender la siguiente petición en la misma llamada a
`Http11Processor.service`. `Http11InputBuffer.nextRequest` ejecuta
`Request.recycle`, que vacía los atributos de Coyote. La consulta del
mapa y la publicación del atributo solo ocurren al entrar a
`service`, por lo que no se repiten dentro de ese bucle.

En la copia temporal se instrumentaron alta, consulta, transferencia y
limpieza con identificadores de socket, estado e intento. Los tiempos
son nanosegundos monotónicos relativos a cada ejecución. Se usaron el
prefijo de cuatro clases de la ronda anterior y las peticiones sintéticas
del test original. Una pausa en el servidor después de responder
`S1` hizo observable el solapamiento con `S2`. La pausa y la traza
fueron iguales en los dos ensayos controlados.

| Ensayo | Secuencia correlacionada | Evidencia |
| --- | --- | --- |
| Prefijo sin pausa | 56 aprobadas. En el socket 21, `S1` y `S2` devolvieron 401. Cada una pasó por un nuevo despacho y llegó al filtro con su propio estado | `fase7-recepcion-prefijo-ciclo.log:1856–1878` |
| Antes del arreglo | En el socket 21, `S1` llegó al filtro con el estado y obtuvo 401. `S2` entró al adaptador y al filtro con estado 0, sin una nueva consulta al mapa. MVC devolvió 500 y solo después el procesador eliminó la entrada original al cerrar | `fase7-recepcion-control-antes-ciclo.log:2054–2069`, `fase7-recepcion-control-antes-cliente.log:26–34` |
| Después del arreglo | Con límite efectivo 1, `S1` llegó por el socket 191 y `S2` por el 192. Ambos estados se crearon y llegaron al filtro. Los cinco intentos devolvieron 401 y el sexto 429. Aprobaron las 56 pruebas del prefijo | `fase7-recepcion-control-despues-ciclo.log:2030–2054`, `fase7-recepcion-control-despues-cliente.log:28–47` |

En la petición fallida, el estado existía y seguía en el mapa con la
misma clave. Lo que faltaba era el atributo de Coyote para la segunda
petición. Su ausencia explica el mismo `NullPointerException` en
`FiltroApi.java:114` capturado en la ronda anterior.

`PilotoLoginApplication.java:68–70` fija el límite también en la
configuración efectiva de Spring Boot mediante
`--server.tomcat.max-keep-alive-requests=1`. La corrección conserva el
plazo total de 30 segundos y el contrato de autenticación. La regresión
`ApiLoginSpringTest.loginInvalidoCierraLaConexionAlResponder` comprueba
el 401 y `Connection: close` en una petición normal contra el Tomcat
real. Antes del cambio falló porque la cabecera estaba ausente
(`fase7-recepcion-regresion-antes.log`). Los dos ensayos controlados
verificaron por separado el 500 con la condición anterior y los 401
esperados con el límite corregido.

### Pruebas de la corrección

Se retiró la instrumentación de la copia temporal y se verificó
`Steven` con la corrección y la nueva regresión. Tanto la regresión
como `cierraSpringAlVencerElPlazoTotalAunqueSiganLlegandoBytes` quedaron
verdes en `ApiLoginSpringTest`.

| Comando | Resultado | Evidencia |
| --- | --- | --- |
| `./mvnw.cmd test '-Dtest=ApiLoginSpringTest,ApiSeguridadTest,ApiFase1Test'` | 40 aprobadas | `fase7-recepcion-dirigidas.log` |
| `./mvnw.cmd clean test` | 213 aprobadas | `fase7-recepcion-clean-test.log` |
| `./mvnw.cmd verify -Ppostgres` | 213 de Surefire y 37 de Failsafe aprobadas en PostgreSQL desechable | `fase7-recepcion-verify-postgres.log` |

Una segunda petición HTTP atendida en un socket ya usado, o una
conexión incompleta que supere el plazo total de recepción, invalidaría
la corrección. La revisión humana T3 de la fase 7 fue aprobada.

## Verificación final

| Comando o recorrido | Resultado | Evidencia |
| --- | --- | --- |
| `./mvnw.cmd test '-Dtest=ApiSeguridadTest#bloqueaTrasCincoIntentosFallidos'` | 1 prueba aprobada con acceso a loopback | `fase7-login-aislado-loopback.log` |
| `./mvnw.cmd test -Dtest=<22 clases del bloque original>` con diagnóstico | 158 aprobadas en el mismo orden del bloque de fase 6 | `fase7-bloque-diagnostico.log` |
| `./mvnw.cmd clean test` | 212 aprobadas, cero fallos, errores u omisiones | `fase7-clean-test.log:949–952` |
| `./mvnw.cmd verify -Ppostgres` | 212 de Surefire y 37 de Failsafe aprobadas con PostgreSQL 18 desechable. Incluye las migraciones V1 a V10 | `fase7-verify-postgres.log:942–1039` |
| JAR con `IniciarApi` | Login 200, métricas 200 y logout 204. Puerto liberado al detener el proceso | `fase7-api-http.log` |
| JAR con `Launcher` y `-Dprism.order=sw` | Arranque en loopback. El recorrido automatizado usó la misma clase JavaFX `HelloApplication` desde el JAR | `fase7-launcher-check.log`, `fase7-javafx-humo.log` |

El classpath del JAR excluyó Lombok, que figura como dependencia estática en
su descriptor. La suite mantuvo el recuento de 36 operaciones REST. Se detuvo
el clúster desechable después de finalizar la prueba.

## JavaFX

La fase 6 registró un fallo nativo en `glass.dll` al abrir un recibo, mediante
`fase6-javafx-humo-primer-intento.log`, línea 69. En fase 7, el recorrido
sintético con `-Dprism.order=sw` completó login, dashboard, propiedad,
unidad, inquilino, contrato, cobro de 25.00 USD, recibo n.º 1 y logout.
El cierre normal de JavaFX liberó el puerto. Se guardaron `07-recibo.png`
y `09-logout.png`.

Un recibo ausente o con importe distinto, un logout que no vuelva a login o
un puerto retenido invalidarían el humo. El arranque con GPU queda sin
verificar en esta fase. El comando y los datos sintéticos están en la
[guía para el docente](demo-docente.md).
