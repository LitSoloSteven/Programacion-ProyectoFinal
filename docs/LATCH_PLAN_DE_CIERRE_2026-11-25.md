# Latch — Plan de ejecución y entrega del 25 de noviembre de 2026

## Objetivo y plazo

Entregar el miércoles **25 de noviembre de 2026**, hora de Managua, un candidato reproducible y acompañado por evidencia. El objetivo de alcance completo es cerrar los 126 requisitos funcionales del PRD —115 Must, 9 Should y 2 Could—, las 38 reglas de negocio, los 60 requisitos no funcionales, los diez reportes y los controles del Plan de Implementación. Se incluyen los Should y Could porque el objetivo pedido es terminar todo lo documentado. Las exclusiones explícitas del PRD quedan fuera.

- **Base de planificación:** `feature/reportes-vista-previa` en `1b8de3a`, observada el 24 de noviembre. Confirmar la rama de entrega y el SHA al iniciar. Los reportes locales guardados registraban 234 pruebas sin fallos. Sirven para orientar el diagnóstico, y el registro de aceptación debe identificar el candidato que se ejecute.
- **Corte oficial confirmado:** miércoles **25 de noviembre a las 13:00, hora de Managua**. Se reserva el tramo desde las 11:30 para preparar y comprobar la entrega.
- **Capacidad:** un solo responsable humano, Steven, y los modelos disponibles en su cuenta. La ventana propuesta 08:00–13:00 es de cinco horas de reloj. Usar un agente por defecto y hasta dos tareas en vuelo únicamente cuando sean independientes, haya capacidad y lo permitan las reglas de Latch.
- **Viabilidad:** con mantenimiento, automatización de cobranza, garantías, respaldo y varios reportes aún pendientes, esta jornada no da una base razonable para comprometer el alcance completo. Hay dos resultados posibles: **cierre completo** si se cumple toda la puerta final, o **entrega del candidato del día con brechas declaradas**. Una brecha abierta sigue siendo trabajo del proyecto, aunque el candidato se entregue a tiempo.

Fuentes de alcance: [PRD](Latch_PRD_APA7_v3.md), [Plan de Implementación](Latch_Plan_Implementacion_APA7_v3.md), [TRD](Latch_TRD_APA7_v3.md), [Esquema de base de datos y servicios](Latch_Backend_Schema_APA7_v3.md), [Flujos](Latch_App_Flow_APA7_v3.md), [Brief UI/UX](Latch_Brief_UIUX_APA7_v3.md). El calendario de este documento aplica la nueva fecha indicada para la entrega. Los requisitos y criterios de esos documentos siguen vigentes.

## Fases de entrega del miércoles 25

Los agentes preparan, implementan y verifican. Steven es el único responsable humano para las decisiones, revisiones críticas y entrega que reserva el repositorio. Las asignaciones son por tipo de trabajo: Sol para coordinación, riesgos y revisión, y Terra para tareas acotadas cuando corresponda. La ejecución puede hacerse en serie con los modelos disponibles.

### E1 · Fijar la base (08:00–08:25)

**Salida:** rama y SHA identificados, candidato de respaldo, entorno de prueba y registro de aceptación preparado.

**Estado:** cerrada el 24 de noviembre sobre `1b8de3a`. El agente que retome revalida el SHA y la conexión al clúster desechable antes de E2. La disponibilidad del Windows alterno se comprobó en el inventario local y no se encontró uno utilizable. La prueba ENT-14 se mantiene en E4/P9. [Evidencia y relevo](evidencia-cierre-2026-11-25/E1_linea_base_y_bloqueos.md).

| Hora (Managua) | Hasta dos tareas en vuelo | Resultado exigido al cerrar el tramo |
| --- | --- | --- |
| **08:00–08:25** | **A. Revalidar candidato y entorno.** **B. Preparar aceptación si puede hacerse en paralelo.** | Confirmar SHA y respaldo ya registrados, disponibilidad de base desechable y de entorno Windows independiente sin JDK. Reutilizar el registro de aceptación y los datos sintéticos de E1. |

### E2 · Estabilizar y congelar (08:25–10:00)

**Salida:** candidato seleccionado a las **10:00**, correcciones revisadas y pendientes asignados. Las decisiones de alcance ya se registraron. Requiere revalidar la base de E1.

**Estado:** cerrada el 24 de noviembre sobre `1b8de3a` sin correcciones bloqueantes (`234` pruebas H2 + `37` PostgreSQL 18.6 en `latch_test_e1`, auditoría de los 11 `.jrxml` y muestreo aleatorio de 18 filas en el registro de aceptación). [Evidencia y congelamiento](evidencia-cierre-2026-11-25/E2_estabilizacion_y_congelamiento.md).

| Hora (Managua) | Hasta dos tareas en vuelo | Resultado exigido al cerrar el tramo |
| --- | --- | --- |
| **08:25–09:30** | **A. Validar flujo crítico.** **B. Revisar reportes existentes si el entorno y la capacidad lo permiten.** | Priorizar arranque, roles, alta, contrato, cobro y recibo. Revisar anulación, idempotencia, vista previa y guardado PDF según capacidad. |
| **09:30–10:00** | **A. Seleccionar candidato.** **B. Preparar instrucciones del paquete cuando sea independiente.** | Cerrar correcciones bloqueantes ya verificadas, registrar brechas y congelar el SHA. Si una corrección no llega a revisión, usar el candidato estable con evidencia aplicable. |

La migración a Argon2id y los demás cambios de P1–P9 se mantienen en el alcance de cierre. Para este candidato solo entran correcciones bloqueantes que puedan quedar verificadas antes de las 10:00.

### E3 · Verificar el candidato (10:00–11:30)

**Salida:** resultados técnicos y de uso asociados al SHA seleccionado, incluida la decisión ante cualquier prueba bloqueante. Depende de E2.

**Estado:** cerrada el 24 de noviembre sobre `1b8de3a` (`./mvnw.cmd verify -Ppostgres -B` verde: `234` H2 + `37` PostgreSQL 18.6; instalación `update`/`V1`–`V10`/`validate`, recorrido E2E desde JAR sobre `IniciarApi` y `12` PDFs/`11` `.jrxml` renderizados y exportados atómicamente; humo visual en monitor pendiente para ejecución humana). [Evidencia y relevo](evidencia-cierre-2026-11-25/E3_verificacion_candidato.md).

| Hora (Managua) | Hasta dos tareas en vuelo | Resultado exigido al cerrar el tramo |
| --- | --- | --- |
| **10:00–11:30** | **A. Puerta técnica.** **B. Puerta de uso cuando pueda correr de forma independiente.** | Ejecutar Maven y el perfil PostgreSQL según el alcance, con base desechable. Probar la demo y los PDFs del mismo SHA. Si una prueba bloquea la entrega, usar un candidato de respaldo con evidencia aplicable y registrar cualquier comprobación pendiente. |

### E4 · Empaquetar y entregar (11:30–13:00)

**Salida:** paquete y evidencia del mismo SHA, copias comprobadas y revisión humana. Depende de E3.

**Estado:** cerrada el 24 de noviembre sobre `1b8de3a` (**entrega del candidato del día con brechas declaradas**): paquete `app-image` ZIP (`110.065.894` B, SHA-256 `7BFE6519726C8F1CD95020033E56055BD157D890F79EBB230D9E95B4DD884B6F`) verificado en dos copias y probado fuera del IDE con `JAVA_HOME=''`, manifiesto, licencias (`94` JARs) y guion de demo listos para revisión humana de Steven antes de las 13:00. [Evidencia y entrega](evidencia-cierre-2026-11-25/E4_empaquetado_y_entrega.md).

**Antes de exponer:** registrar el [humo visual en monitor](evidencia-cierre-2026-11-25/E3_verificacion_candidato.md) con el mismo candidato sobre `latch_demo`. La preparación del paquete puede avanzar mientras se verifica. Incluir en E4 el runtime, las dependencias y las licencias, el manifiesto SHA-256, la matriz conciliada, las brechas y el guion de exposición.

| Hora (Managua) | Hasta dos tareas en vuelo | Resultado exigido al cerrar el tramo |
| --- | --- | --- |
| **11:30–12:25** | **A. Empaquetar y probar.** **B. Conciliar la matriz y preparar la demo.** | Ejecutar fuera del IDE, verificar archivos y huellas y probar en Windows limpio sin JDK si está disponible. Registrar las comprobaciones ejecutadas y las que queden pendientes. |
| **12:25–13:00** | **A. Preparar artefactos y copias.** **B. Revisión final con Steven.** | Dejar listo el paquete y la evidencia del mismo SHA para que Steven efectúe la entrega antes de las 13:00. Incluir las brechas con siguiente acción y evidencia faltante. |

## Decisiones registradas para E2 y P1

El 24 de noviembre, Steven confirmó el plazo y la ejecución por agentes, aprobó Argon2id y delegó los demás criterios de planificación. El [relevo de E1](evidencia-cierre-2026-11-25/E1_linea_base_y_bloqueos.md) conserva la línea base y el estado de las pruebas.

| Decisión | Resolución y alcance |
| --- | --- |
| **Operativa** | Corte oficial a las **13:00** del 25 de noviembre. Steven supervisa y los agentes ejecutan. Buscar primero una VM o un equipo Windows independiente sin JDK. Si no está disponible, probar el runtime incluido fuera del IDE en el equipo actual y dejar ENT-14 pendiente de verificación en equipo alterno. |
| **D-P0-1 · Aprobación explícita** | Migrar a **Argon2id en P1** conforme a RF-M01-03/RNF-005, con sal única y mínimos del PRD. Admitir el hash PBKDF2 existente durante la transición y actualizarlo tras autenticación correcta o restablecimiento autorizado. Probar ambos caminos y registrar la retirada del formato antiguo. Mantener el candidato inmediato con la implementación vigente. La integración debe completar la revisión crítica y el trámite aplicable a cambios de dependencias y reglas canónicas. |
| **D-P0-2 · Decisión delegada** | Conservar el vencimiento fijo del token a 15 minutos. Añadir el cierre por inactividad en JavaFX según RF-M01-07 como comprobación independiente, revocar el token al cerrar y verificar ambos relojes y el tratamiento de cambios pendientes según el Flujo. La actividad local por sí sola no amplía la vida del token. |
| **D-P0-3 · Decisión delegada** | Para la demo inmediata, usar la ruta documentada en una base aislada. En P1, preparar un script SQL de esquema inicial para instalaciones nuevas, aplicar después V1–V10 sin editarlas y las migraciones posteriores que correspondan, y arrancar en `validate`. Probar por separado base vacía y actualización de una base existente. |
| **D-P0-4 · Decisión delegada** | P4 implementará referencia informativa USD/NIO por HTTPS, caché y fixture S1 local para éxito, caída y cuota. Se preserva la autorización explícita para pagos. La API de cada escritorio permanece en `127.0.0.1`. La variante LAN se verifica con hasta tres escritorios conectados al mismo PostgreSQL mediante TLS, con adjuntos en el repositorio SMB restringido y una instancia designada para actualizar la caché común. P1 aplica la Tabla 7: el Asistente consulta unidades y edita las condiciones/foto permitidas, sin altas, bajas ni cambio a En mantenimiento en M02. En M03 puede crear, editar y consultar inquilinos, con desactivación reservada al Propietario. Validar reglas en servicio/API e interfaz. |

La disponibilidad de una base PostgreSQL desechable y del entorno Windows alterno se comprueba en E1. Cada requisito que dependa de un entorno ausente conserva su estado de verificación pendiente.

## Fases de cierre del alcance completo

Cada paquete P0–P9 se divide en tickets y PR según las reglas del proyecto. El equipo puede aprovechar la evidencia de E1–E4 para cerrar un criterio cuando corresponda a la revisión evaluada. Los paquetes sin cerrar el 25 se calendarizan con sus responsables a partir del esfuerzo restante y las dependencias de cada fila.

### C1 · Acordar contratos y completar la base (P0–P1)

**Salida:** matriz inicial, discrepancias resueltas por el equipo e instalación y administración verificadas.

**Estado de la rama de trabajo:** implementación y verificación automatizada completadas. Quedan el humo interactivo y la revisión humana de los cambios críticos. [Evidencia de C1](evidencia-cierre-2026-11-25/C1_verificacion_p0_p1.md).

| Orden y dependencia | Área y modelo previsto | Trabajo y criterio de salida |
| --- | --- | --- |
| **P0 · Inicio** | **Línea base y contratos — Sol, con decisiones de Steven.** RF, RN, RNF, CP, REC y ENT. | Registrar cada requisito y sus casos a partir de los apéndices originales. Resolver por escrito PBKDF2 de las reglas vigentes frente a Argon2id del PRD, expiración fija frente a inactividad, y el camino de instalación de base vacía frente al esquema inicial de Hibernate. Precisar el alcance de la referencia USD/NIO, el fixture S1, la variante LAN y los roles antes de tocar esos contratos. Conservar las reglas vigentes hasta resolver las discrepancias. |
| **P1 · Después de P0 y de las decisiones escritas aplicables** | **Persistencia, seguridad y administración — Sol.** M01, M02, M03 y M12. | Validar instalación limpia y actualización de un esquema previo. Habilitar gestión de cuentas, cambio y restablecimiento de contraseña y revocación según el contrato aprobado. Completar bajas lógicas, historial por inquilino, advertencia por identificación y auditoría de cambios. Agregar parámetros con historial, perfil emisor para recibos y consulta de auditoría con filtros. Se cierra con pruebas de roles, historia preservada, trazabilidad y ambas rutas de instalación. |

### C2 · Completar los módulos operativos (P2–P5)

**Salida:** contratos y garantías, cobranza y pagos, tasas, mantenimiento y adjuntos tienen datos, flujo de uso y pruebas. La columna de dependencias indica cuándo puede empezar cada paquete, incluido P4 tras P0.

**Auditoría integrada de C2:** pasaron V0–V16 con `psql.exe`, el perfil PostgreSQL completo y los dos escenarios de concurrencia multiproceso sobre PostgreSQL 18.6. Los estados por paquete de abajo conservan sus hitos históricos. Para el cierre formal quedan el humo interactivo, la validación del proveedor externo y de LAN, y G6. [Evidencia integrada](evidencia-cierre-2026-11-25/C2_auditoria_integrada.md).

**Estado de P2 en la rama de trabajo:** implementación y pruebas H2 completadas (`./mvnw.cmd -B test`: **265** pruebas, 0 fallos). Quedan la verificación PostgreSQL (`./mvnw.cmd -B verify -Ppostgres` con `LATCH_TEST_DB_*` desechable), el humo interactivo en JavaFX y la revisión humana G6. [Evidencia de P2](evidencia-cierre-2026-11-25/C2_P2_verificacion_contratos_garantias.md).

**Estado de P3 en la rama de trabajo:** implementación y pruebas H2/2-JVM completadas (`./mvnw.cmd -B test`: **280** pruebas, 0 fallos, incluidas pruebas de 2 JVMs con 100 recibos y 10 rollbacks). Quedan la verificación PostgreSQL (`./mvnw.cmd -B verify -Ppostgres` con `LATCH_TEST_DB_*` desechable), el humo interactivo en JavaFX y la revisión humana G6. [Evidencia de P3](evidencia-cierre-2026-11-25/C2_P3_verificacion_cobranza_pagos.md).

**Estado de P4 en la rama de trabajo:** implementados el cliente HTTPS, la migración V15, H3, el simulador S1 de Spring Boot y la consulta informativa en Spring Boot y JavaFX. Pasaron 285 pruebas de la suite normal y 40 del perfil PostgreSQL. Se verificó la vista y el botón Actualizar en JavaFX con datos sintéticos. Quedan la revisión humana G6 y la comprobación con una credencial del proveedor. [Evidencia de P4](evidencia-cierre-2026-11-25/C2_P4_cliente_https_y_S1.md).

**Estado de P5 en la rama de trabajo:** implementados la migración V16, entidades y servicios de mantenimiento y adjuntos (`MantenimientoService`, `AlmacenAdjuntos`, `AdjuntoService`), las 6 rutas REST en Spring Boot (`/api/v1/mantenimiento/*`), `ClienteMantenimiento`, la vista `mantenimiento-view.fxml`, fotografía de unidad, contacto de emergencia opcional y adjuntos en inquilinos/contratos y logotipo del emisor. Pasaron **292 pruebas** de la suite normal H2 (`./mvnw.cmd -o -B test`), con 0 fallos. Quedan `./mvnw.cmd -o -B verify -Ppostgres` en base desechable, el humo interactivo en JavaFX y la revisión humana G6. [Evidencia de P5](evidencia-cierre-2026-11-25/C2_P5_mantenimiento_y_adjuntos.md).

| Orden y dependencia | Área y modelo previsto | Trabajo y criterio de salida |
| --- | --- | --- |
| **P2 · Después de P0 y del esquema de P1** | **Contratos y garantías — Sol para dominio y Terra para interfaz.** M04 y M07. | Completar filtros y transiciones de borrador, activación, vigencia, cancelación, renovación enlazada y terminación con fecha y motivo. Registrar movimientos de depósito, deducción, aplicación a deuda y devolución, con liquidación en el cierre, historial y conciliación. Probar colisión de ocupación, cierre y activación atómicos, rollback, liquidación parcial y autorización. |
| **P3 · Después de P2** | **Cobranza y pagos — Sol para dominio y Terra para interfaz.** M05 y M06. | Implementar H1 con rentas elegibles, deduplicación por período, progreso, cancelación entre contratos, reanudación y previsualización. Implementar H2 para mora según gracia y principal, sin capitalización, y detección de contratos por vencer. Aplicar crédito al siguiente cargo y completar cartera por período y antigüedad. Verificar abonos parciales, aplicaciones, anulación, restitución de saldos y crédito, recibo e idempotencia bajo concurrencia y rollback, con dos JVM y 100 recibos según el Plan. |
| **P4 · Después de P0; integrar con P3** | **Tipo de cambio — Sol, con Terra para tareas acotadas.** M09. | Mantener la API REST de tasas de Latch en Spring Boot / Spring MVC. Desde el servicio del backend, consumir por HTTPS el proveedor externo con el cliente HTTP indicado en el PRD. Validar JSON y banda de 1–100, implementar caché, origen, antigüedad, actualización H3 y tolerancia a caída/cuota. Distinguir referencia informativa de tasa autorizada y probar que el pago conserva la tasa y procedencia efectivamente usadas. S1 aporta fixtures de éxito, caída y cuota. |
| **P5 · Después de P1** | **Mantenimiento y adjuntos — Sol para esquema y Terra para interfaz.** M02, M03, M04 y M08. | Crear solicitudes por unidad, prioridad, responsable, transiciones, motivo de cancelación, costo por moneda/fecha y consulta por propiedad/período. Completar contacto de emergencia y adjuntos del inquilino, documento contractual, foto de unidad y logotipo, con validación y acceso según el Esquema y el Brief. Verificar costo, permisos, almacenamiento e integridad de adjuntos. |

**Restricción del docente para P4.** Toda API HTTP que implemente el proyecto usa Spring Boot, incluida S1 si se levanta como servidor de pruebas. Esto actualiza la elección de `HttpServer` para S1 en el [TRD](Latch_TRD_APA7_v3.md). El cliente HTTPS es una llamada saliente al proveedor y se ejecuta dentro del backend de Spring Boot. JavaFX sigue consultando la API local de Latch en `127.0.0.1`.

### C3 · Cerrar reportes, recuperación y experiencia de uso (P6–P8)

**Salida:** diez PDFs y nueve XLSX conciliados, restauración demostrada en un entorno aislado y panel, temas y cierre de tareas verificados.

C3 pasó la auditoría técnica en el equipo disponible. La [auditoría integrada de C3](evidencia-cierre-2026-11-25/C3_auditoria_integrada.md) registra las pruebas, la medición de 2 GiB y las comprobaciones de aceptación formal pendientes. Las actas de cada paquete conservan el estado de su revisión inicial.

**Estado de P6:** verificados diez PDF, nueve XLSX numéricos, instancia única H5 y conservación del archivo anterior al cancelar. JavaFX abrió el centro de reportes y renderizó REP-03. [Acta de P6](evidencia-cierre-2026-11-25/C3_P6_reportes_y_exportacion.md).

**Estado de P7:** verificados el respaldo cifrado, la restauración aislada, la integridad de los adjuntos y el rollback con PostgreSQL 18.6 y herramientas nativas. La auditoría integrada registra el resultado de capacidad. [Acta de P7](evidencia-cierre-2026-11-25/C3_P7_respaldo_y_restauracion.md).

**Estado de P8:** verificados indicadores, preferencias, temas y foco, cálculo de inactividad y recuperación tras un cierre H6 bloqueado. [Acta de P8](evidencia-cierre-2026-11-25/C3_P8_panel_temas_y_cierre.md).

| Orden y dependencia | Área y modelo previsto | Trabajo y criterio de salida |
| --- | --- | --- |
| **P6 · Después de P2–P5 para los datos que use cada reporte** | **Reportes y exportación — Terra, con revisión de Sol.** M10. | Mantener recibo y estado de cuenta con sus pruebas de número, anulación y corte. Completar cartera vencida por corte y tramos, ocupación con denominador y filtros, historial por inquilino, garantías con movimientos y tasas efectivamente usadas por pago. Crear ingresos y gastos, contratos por vencer y mantenimiento. Cerrar PDF para REP-01 a REP-10 y XLSX numérico para REP-02 a REP-10. Conciliar filtros, corte, columnas y totales por moneda con las consultas. Probar H5, cancelación y que un fallo conserve el archivo anterior. |
| **P7 · Después del esquema y adjuntos de P1/P5** | **Respaldo y restauración — Sol.** M12. | Generar respaldo coherente de base y adjuntos, con manifiesto, SHA-256 y AES-256-GCM. Restaurar en modo de mantenimiento primero en un entorno aislado. Verificar clave/autenticidad inválida, archivo alterado, recuperación completa, conservación del destino ante fallo y las metas de tamaño, tiempo y pérdida de datos del TRD. |
| **P8 · Después de P2–P6 para los servicios que consuma** | **Panel, temas y cierre — Terra para interfaz y Sol para H6.** M01, M11 y M12. | Completar indicadores de ocupación, corte, moneda/período, mora, vencimientos y procedencia de tasa, con detalle filtrado. Verificar Claro/Oscuro en acceso, preferencias locales por cuenta, modo sin efectos, teclado, foco y persistencia de entradas. Completar H6 para coordinar H1, H2, H3 y H5, con cancelación y aviso de demora. |

### C4 · Aceptar y cerrar el proyecto (P9)

**Salida:** matriz completa, entrega reproducible y revisión humana con cero pendientes abiertos.

| Orden y dependencia | Área y modelo previsto | Trabajo y criterio de salida |
| --- | --- | --- |
| **P9 · Después de P1–P8** | **Aceptación y entrega completas — Sol coordina, Steven aprueba.** RNF, CP, REC y ENT. | Cerrar matriz de requisitos, rendimiento, accesibilidad, seguridad, recuperación y concurrencia. Verificar los cien CP del Plan, los diez REC en las variantes exigidas y los veinte ENT. Empaquetar la app-image ZIP con runtime, licencias, manual, protocolo y datos sintéticos. Probar en otro equipo sin JDK y fuera del IDE. Ensayar guion de veinte minutos, comprobar proyector y escalado, etiqueta, manifiesto y dos copias. |

P4 y P5 pueden avanzar junto con un paquete independiente cuando los contratos estén congelados y haya sitio en el límite de dos tareas. Cada tarea usa su propio escritor, migración, controlador y base desechable, cuando apliquen.

## Reglas de ejecución y verificación

- Antes de cada ticket, declarar clase T0–T6, modelo/esfuerzo, presupuesto, escritor y comprobación. Para T2, Sol planifica/revisa y Terra ejecuta con contrato congelado. Para T3, Sol dirige y ejecuta cambios de dinero, permisos o esquema con revisión humana. Clasificar las discrepancias de arquitectura como T4 cuando corresponda. Mantener un escritor por archivo y worktree, y un máximo de dos tareas en vuelo.
- Un PR por comportamiento con el tamaño de la regla canónica. Separar el esquema, servicio y conectores cuando lo requiera el tamaño. Mantener migraciones append-only, usar los siguientes números disponibles y validar con `hbm2ddl=validate`. Las decisiones sobre dinero, permisos, migraciones, seguridad, datos reales o contratos incompatibles pasan por su responsable. Dependencias nuevas, `pom.xml`, scripts de despliegue y configuración protegida requieren la aprobación prevista en el repositorio. La firma, etiqueta y publicación corresponden al equipo humano.
- Mantener importes y tasas en las escalas del dominio, separar USD/NIO, comprobar permiso dentro de la transacción, usar hora operativa PostgreSQL e idempotencia en las escrituras financieras. Mantener la API en loopback, los errores sanitizados y las operaciones de base, red y archivos fuera del hilo de JavaFX.
- Para código, comenzar con `./mvnw.cmd test -Dtest=ClaseTest` y usar `./mvnw.cmd test` en la puerta del hito. Para persistencia, migraciones y concurrencia, ejecutar `./mvnw.cmd verify -Ppostgres` con `LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD` configuradas para una base desechable exclusiva. La base de demo se mantiene separada. Para UI, aplicar `FxmlControladoresTest`, `EstilosCssTest`, `ContrasteTest` y humo manual. Para API, ejecutar las pruebas `Api*Test` pertinentes y revisar estados y formato de error.
- Guardar evidencia del candidato con SHA, comando o pasos, resultado, archivos, prueba que detecta la regresión cuando aplique, responsable y condición que invalidaría el resultado. Seguir las reglas de parada y repetir las comprobaciones que invalide el último cambio. Registrar **Verificado**, **Inferido** o **No probado** y, para los dos últimos estados, el paso exacto para verificar. Si una sesión termina, dejar el relevo breve que exige la regla canónica.

Referencias operativas: [reglas canónicas](AGENTES_Y_MODELOS.md), [README y comandos](../README.md), [contrato de seguridad](SEGURIDAD.md), [contrato HTTP](API_Latch.md), [guía de demo](demo-docente.md).

## Puerta final

El registro de aceptación debe tener una fila por identificador, con prioridad, responsable, caso, SHA, evidencia y estado. Usar los criterios originales de los 126 RF, 38 RN, 60 RNF, CP-001 a CP-100, REC-01 a REC-10 y ENT-01 a ENT-20, con enlaces cruzados cuando una ejecución cubra varias filas. Los benchmarks y recorridos requieren resultado observado en el entorno indicado por los documentos.

**Qué invalidaría la evidencia.** Sobre `1b8de3a`, un fallo en un flujo crítico que ya figure como verificado en H2 al ejecutar `./mvnw.cmd verify -Ppostgres` en una base desechable o al seguir el [humo manual](demo-docente.md). Registrar el fallo en su entorno y volver a abrir el criterio afectado. Antes de aceptar el registro, tomar una muestra aleatoria de sus [354 filas](evidencia-cierre-2026-11-25/registro_aceptacion.md), guardar los identificadores seleccionados y comprobar que las aserciones de cada prueba citada ejercitan el criterio. Una cita que solo cubra parte del criterio debe quedar con cobertura parcial y un paso de verificación explícito.

- [ ] **Alcance completo:** todos los requisitos y criterios acordados están verificados, incluidos Should y Could de este encargo. Las discrepancias documentales están resueltas y reflejadas en los documentos aplicables. Quedan cero pendientes abiertos para declarar el alcance completo.
- [ ] **Datos y finanzas:** instalación en base vacía, actualización, rollback, idempotencia, concurrencia, separación por moneda y restauración aislada tienen evidencia válida de este candidato.
- [ ] **Uso y reportes:** los diez recorridos, permisos, variantes de tema y accesibilidad, diez PDFs y nueve XLSX tabulares tienen evidencia reproducible fuera del IDE.
- [ ] **Entrega:** equipo humano revisa el informe, ejecutable, guía, versión, licencias, manifiesto de huellas, ensayo y dos copias. El candidato y las evidencias identifican la misma revisión.
- [ ] **Si se entrega con brechas:** el informe indica qué se verificó, qué falló, qué quedó sin probar o implementar y quién lo retoma. El proyecto queda abierto hasta que la puerta de alcance completo se cumpla.
