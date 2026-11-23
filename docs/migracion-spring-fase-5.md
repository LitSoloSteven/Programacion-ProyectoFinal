# Migración de Latch a Spring Boot · Fase 5

## Arranque y alcance

La fase 4 estaba en los cambios locales de la rama `Steven`, sobre `2d00beb`.
Ambos puntos de entrada usan el mismo contexto de Spring Boot y escuchan en
`127.0.0.1` con `LATCH_API_PORT`. JavaFX inicia Spring fuera de su hilo gráfico,
crea el cliente cuando Tomcat ha arrancado y cierra el contexto al salir.
Se retiraron `ServidorApi`, `Router` y sus adaptadores sin consumidores.
Spring MVC registra 36 operaciones. La adopción general de Lombok corresponde
a la fase 6.

JavaFX usa `ClienteApi` en tareas de fondo para login, logout, catálogos,
métricas, tasas, listados de cargos y pagos, alta y anulación de pagos,
detalle y recibos. El GET de estado de cuenta sigue disponible por HTTP.
Esta fase conserva las pantallas existentes.

| Operación de escritorio sin ruta REST equivalente | Servicio local |
| --- | --- |
| Eliminar físicamente propiedades, unidades, inquilinos y contratos | Servicios de cada catálogo |
| Consultar los datos de seguimiento necesarios para ajustar una garantía y ajustar su saldo | `GarantiaService` |
| Cargar el detalle versionado de un cargo para editarlo, crear o editar cargos y anularlos | `CargoService` |

## Verificación

| Comando o prueba | Resultado | Evidencia |
| --- | --- | --- |
| `./mvnw.cmd clean test` | 212 aprobadas, incluida `FxmlControladoresTest` | `fase5-clean-test.log` |
| `./mvnw.cmd verify -Ppostgres` | 212 de Surefire y 37 de Failsafe en PostgreSQL desechable, incluidas V1–V10 | `fase5-verify-postgres.log` |
| `./mvnw.cmd javafx:run` | Tomcat arrancó en el puerto configurado y se mostró el login | `fase5-javafx-final.log` |
| `./mvnw.cmd exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.IniciarApi'` | Métricas HTTP 200 y logout 204 desde un cliente externo. El puerto 48155 quedó disponible al cerrar | `fase5-api-final.log`, `fase5-api-http.log`, `fase5-api-cierre.log` |
| JavaFX en el puerto usado por la API independiente | Mostró el aviso de puerto ocupado. La API independiente siguió respondiendo 200 y 204 | `fase5-puerto-ocupado.log`, `fase5-api-http.log` |
| Recorrido visual en PostgreSQL desechable | Login, dashboard, propiedad, unidad, inquilino, contrato, revisión del cobro, recibo y logout. El cierre liberó el puerto 48156 | `Fase5Humo.java`, `fase5-humo-ui.log`, capturas 01–09 |

Durante la verificación hubo respuestas HTTP 500 intermitentes en corridas H2
del perfil completo, en pruebas de tasa sin fuente e intentos de login. La
corrida con diagnóstico temporal aprobó las 212 pruebas y las 37 de PostgreSQL
sin reproducir esos fallos. Se guardaron las salidas de ambos intentos fallidos
y se retiró la instrumentación del código.


El recorrido revocó vía HTTP el token que seguía usando JavaFX. El siguiente
acceso protegido devolvió 401 y JavaFX limpió la sesión y volvió al login.
`TokenServiceTest` cubre por separado la expiración temporal.
`ClientePagosTest` comprobó que, tras un 503, el mismo cobro conserva su
`Idempotency-Key` hasta recibir el recibo. Las pruebas HTTP con PostgreSQL
comprobaron que repetir la clave y el contenido devuelve el mismo recibo y
que usar la misma clave con otro contenido da 409.

En la prueba visual del inquilino, la versión inicial fue 0, el PUT devolvió 0
y el GET siguiente devolvió 1. Un segundo editor intentó guardar con la
versión 0 y recibió 409. JavaFX avisó del conflicto, cerró el formulario y
recargó la tabla con el registro actualizado. El contrato HTTP del PUT conserva
la respuesta anterior al commit en este caso.

`ApiPagosTest.desfaseDelRelojImpidePersistirPagoReciboEIdempotencia` desplazó
diez minutos el instante que PostgreSQL devuelve al fixture, sin alterar el
reloj del equipo. El POST atendido por Spring devolvió 503 `ERR-057` y no creó
pago, recibo ni registro de idempotencia. Al quitar el desfase, el reintento con
la misma clave devolvió 201.

Un segundo recibo para el mismo reintento, un puerto retenido al cerrar o un
formulario que volviera a guardar con una versión obsoleta invalidarían esta
verificación. El cierre técnico queda para revisión humana T3 de la fase 5.
