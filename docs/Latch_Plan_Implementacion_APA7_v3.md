

## 1
Plan de Implementación de Latch
## Steven Cuadra
## Alfredo Montalvan
## Caleb Tardencilla
## Aris Gavarrete
Facultad de Ingeniería y Arquitectura, Universidad Americana (UAM)
Sede Managua, Ingeniería de Sistemas
CNU0525: Programación de Aplicaciones de Escritorio, Grupo 2
II semestre del II año
## José A. Durán García
26 de noviembre de 2026

## 2
## Resumen
El equipo construirá Latch mediante seis períodos de trabajo que abarcan del 22 de octubre al 1
de diciembre de 2026, con entrega interna de documentos y versión demostrable el 26 de
noviembre. El plan organiza 96 tareas, propone responsables nominales y distribuye 228 horas
de trabajo dentro de una capacidad estimada de 281,14 horas. La reserva cubre coordinación,
revisión adicional y correcciones sin aumentar la disponibilidad semanal supuesta de los
integrantes. La implementación conserva JavaFX, PostgreSQL, JDBC y Hibernate, diez
contenidos de reporte con JasperReports y consulta HTTP de referencia USD/NIO separada de la
autorización de tasa para cobros. Claro y Oscuro forman parte del primer sistema visual Aurora,
con preferencias locales y modalidad sin efectos. La estrategia de calidad define cien casos con
datos sintéticos, incluidos doce de concurrencia, catorce de HTTP simulado y dieciocho de
conformidad visual. Los resultados son criterios de ejecución del Apéndice B, en
correspondencia con los escenarios del PRD y TRD (Cuadra et al., 2026c, 2026d). El documento
incluye despliegue, respaldo, riesgos, contingencia de alcance, revisión académica y un guion
propuesto de veinte minutos. Las fechas académicas y la capacidad derivan del contexto
aportado y no constituyen confirmación institucional ni registro de horas trabajadas.
Palabras clave: implementación, JavaFX, PostgreSQL, concurrencia, pruebas, Aurora
UI, planificación

## 3
Plan de Implementación de Latch
## Introducción
Latch se construirá como una aplicación de escritorio para registrar alquileres, contratos,
cargos, pagos parciales, garantías y mantenimiento mediante JavaFX y PostgreSQL. Este plan
organiza el trabajo de cuatro integrantes desde la fecha base del 22 de octubre hasta el cierre
académico de referencia del 1 de diciembre de 2026. La entrega interna de documentos y versión
demostrable se propone para el 26 de noviembre. El equipo utilizará las tareas, dependencias y
criterios de aceptación para coordinar la construcción, mientras el docente podrá revisar la
correspondencia entre componentes y resultados de aprendizaje.
El brief define las composiciones Aurora UI, los tokens de Claro y Oscuro y las pantallas
PAN-01 a PAN-40 (Cuadra et al., 2026a). El flujo especifica recorridos FLU-01 a FLU-49,
incluidos recuperación de pagos y cierre controlado (Cuadra et al., 2026b). El PRD aporta los
identificadores RF-Mxx-nn, RN-01 a RN-38 y RNF que conserva este plan (Cuadra et al.,
2026c). El TRD fija arquitectura, versiones y presupuestos técnicos, y el esquema recibido
precisa restricciones y servicios transaccionales (Cuadra et al., 2026d, 2026e). Los cinco
documentos fueron consultados como fuentes del proyecto.
El Apéndice A contiene el backlog y el Apéndice B desarrolla los casos de prueba que
acompañan cada incremento. El Apéndice C fija el protocolo de revisión interna, y el Apéndice
D organiza la verificación de entrega. El Apéndice E desarrolla el guion de demostración,
mientras el Apéndice F identifica las fuentes y sus límites de uso. Las tablas del cuerpo
relacionan esas actividades con capacidad, responsables y evidencia académica. El equipo
consultará los procedimientos detallados antes de ejecutar una tarea o declarar terminado un
entregable del calendario de la Tabla 10.

## 4
Antecedentes y Límite de la Evidencia
Cook et al. (2024) revisaron decisiones de propietarios sobre su trayectoria,
mantenimiento de cartera y operación de propiedades. Su método delimitó literatura
principalmente estadounidense desde 2008 e incluyó 196 artículos. Ese antecedente permite
formular escenarios sobre contratos y mantenimiento, con un ámbito geográfico distinto al de
Latch. Balzarini y Boyd (2021) estudian estrategias de pequeños arrendadores para evitar
desalojos en Filadelfia, antecedente delimitado también por el PRD y TRD (Cuadra et al., 2026c,
2026d). La propuesta para pequeños propietarios de Managua sigue siendo una hipótesis de
diseño académico. El equipo no dispone de entrevistas, observaciones, registros privados ni un
propietario participante que permita afirmar beneficios locales medidos.
Los casos de demostración usarán personas, propiedades, montos y documentos sintéticos
identificados en la interfaz y en la evidencia. Propietario y Asistente son roles del producto, y el
inquilino será receptor de comprobantes sin acceso a la aplicación. Las actividades de prueba
desarrollan los escenarios del PRD y TRD (Cuadra et al., 2026c, 2026d) como procedimientos y
resultados esperados. La revisión efectuada para esta edición comprende documentos, fuentes
oficiales, aritmética del calendario y archivos de entrega. Una captura del documento o un
cálculo de capacidad no acredita que el sistema haya pasado sus pruebas.
Supuestos y Coherencia Documental
La fecha de portada del 26 de noviembre de 2026 es una fecha interna adoptada para
organizar la entrega documental. Las sesiones y evaluaciones proceden del resumen académico
aportado, sin consulta directa del syllabus original ni de UAM Virtual. La capacidad de doce
horas semanales por integrante es una estimación, y las asignaciones nominales son una
propuesta de trabajo. Ninguna de esas decisiones acredita disponibilidad confirmada, horas ya

## 5
trabajadas o aprobación docente. La consulta de fuentes externas de esta edición corresponde al
23 de octubre de 2026, diferente de la fecha base de planificación.
La Tabla 1 reúne las decisiones que resuelven diferencias entre documentos y precisan
sus efectos sobre la implementación. El contrato visual del proyecto, consolidado en el brief y el
flujo, recoge la evolución histórica de la paleta inicial, mientras el esquema concreta el uso de
una sola conexión durante cada operación financiera. La planificación conserva los estados
activos ampliados del PRD y la separación entre referencia cambiaria y autorización por fecha.
Cada decisión mantiene su naturaleza documental, técnica o estimativa. El registro permitirá
revisar una futura solicitud de cambio sin sustituir la base aprobada internamente por una
interpretación aislada del calendario.
## Tabla 1
Decisiones de Coherencia Adoptadas
TemaValor Adoptado y NaturalezaEfecto en el Plan
CalendarioBase 22/10/2026; entrega interna 26/11;
cierre 01/12. Supuesto del encargo.
Ninguna fecha acredita calendario
institucional verificado.
Motor y
persistencia
PostgreSQL 18.6; JDBC para seguridad,
bitácora y agregados; Hibernate para
negocio. Diseño compatible.
TAR-010 es prueba aislada; TAR-038
usa una transacción definitiva
compartida.
OcupaciónVigente, Por vencer y Vencido pendiente
de cierre reservan unidad. RN-01
ampliada.
CP-016 y CP-052 verifican
solapamiento y simultaneidad.
RecibosRegistrado y Anulado; contador por fila,
sin reinicio anual. RN-07 y RN-08.
Rollback no consume; anulación
conserva número; reimpresión no
emite otro.
AuroraContrato visual del proyecto: tokens
vigentes del brief y del flujo tras la
evolución histórica de la paleta inicial.
Claro y Oscuro desde Sprint 0; Segoe
UI; superficies opacas.
PreferenciasÚltima local antes del acceso; UUID
después; Claro ante lectura inválida.
## RN-36.
No crea tabla de dominio; fallo de
guardado conserva sesión y avisa.

## 6
TemaValor Adoptado y NaturalezaEfecto en el Plan
TasaReferencia informativa del proveedor
separada de autorización por fecha.
## RN-22.
Pago entre monedas bloqueado sin
autorización; misma moneda sigue
disponible.
## Compatibilid
ad
Conservar Hibernate 7.4.6.Final y
JasperReports 7.0.8; existen en fuentes
oficiales.
DTO a reportes, sin jasperreports-
hibernate; integración sujeta a
pruebas.
GarantíaRegistro Retenido de importe cero sin
movimiento; comprobante de garantía
separado.
No consume consecutivo de renta;
cierre exige remanente de fondos
cero.
RelojInstante y fecha operativa de finanzas
tomados de now() de BD; desfase local
mayor de cinco minutos bloquea nuevas
finanzas.
Banda técnica NIO/USD 1–100
inclusive; fuera de banda no sustituye
vigente y registra
## REFERENCIA_EN_CUARENTENA
en bitacora.
Objetivos y Alcance del Plan
El objetivo de la entrega interna es disponer el 26 de noviembre de un recorrido operativo
desde el acceso hasta la emisión y reimpresión de un recibo. Ese recorrido incluirá contrato
válido, cargo, pago parcial, auditoría y consulta de saldo mediante datos sintéticos. El
incremento deberá conservar ambos temas y mostrar tareas en segundo plano sin bloqueo de la
interfaz. La aceptación exigirá una compilación reproducible, extracción y arranque de la app-
image ZIP en un segundo equipo y respaldo restaurado. Los criterios son metas propuestas y su
cumplimiento se documentará mediante evidencia asociada a una revisión Git identificable.
El segundo objetivo es evidenciar RA1 a RA5 mediante componentes que puedan
demostrarse durante la defensa. RA1 aparecerá en controles y eventos JavaFX, RA2 en JDBC
parametrizado y RA4 en entidades Hibernate y reportes. Los trabajadores H1 a H6 cubrirán
concurrencia, sincronización y cierre, mientras H3 mostrará una solicitud HTTP y procesamiento
JSON para RA5. El equipo conservará pruebas de reversión, permisos y recuperación como
condiciones de entrega. La matriz final vincula cada indicador con una acción observable y el
archivo o registro que deberá conservarse.

## 7
Prioridad MoSCoW y Producto Mínimo Viable
La prioridad Must del PRD define la base funcional obligatoria de la versión académica.
El producto mínimo viable incluirá seguridad, catálogos, contratos, cobranza, recibos, garantía,
mantenimiento básico, tasas, diez reportes, panel, configuración y respaldo. Las operaciones
usarán componentes reutilizables para reducir el trabajo repetido en formularios y consultas.
Should y Could conservarán su clasificación documental, pero su aplazamiento deberá
registrarse de forma explícita al aplicar la contingencia. La Tabla 2 fija el orden de entrega y
evita presentar como equivalente una demostración sin persistencia o sin integración HTTP.
## Tabla 2
Prioridades y Producto Mínimo Viable
ClaseContenido ComprometidoMomento y Criterio
MustM01 a M12 en profundidad básica; 10
contenidos de reporte; H1 a H6; JDBC y
## ORM.
v0.1.0 a v0.9.0-rc.1; no reducir
invariantes, RA ni ambos temas.
## Must
visual
RF-M01-12 a RF-M01-15; RF-M11-07 a RF-
M11-10; RNF-047 a RNF-060.
Dos temas, selector y persistencia
en Sprint 0; regresión de 18 casos
## Aurora.
ShouldInactividad automática, contacto de
emergencia, adjuntos, previsualización
avanzada, responsable de mantenimiento RF-
M08-03, navegación desde indicadores y
cancelación H5.
Incorporación solo dentro de
reserva libre; primer grupo
funcional recortable después de
## Could.
CouldFotografía de unidad RF-M02-05 y logotipo
## RF-M12-03.
Aplazados de la línea de 228 h; se
conserva manejo de recurso
ausente.
Won’t en
esta
versión
Canales móviles/web, portal, mensajes
automáticos, OCR, firma y pasarelas.
Excluidos; no figuran como
capacidad adicional implícita.
Nota. MoSCoW conserva la prioridad del PRD; simplificar composición no rebaja un requisito
## Must.

## 8
El mínimo viable conserva la corrección del dinero, el contador de recibos, la
autorización de tasa y la auditoría en una sola transacción. Los reportes de cartera y estado de
cuenta usarán fecha de corte para explicar el saldo histórico sin reescribir movimientos
confirmados. Claro y Oscuro estarán disponibles desde el primer sistema visual, con selección
personal y modalidad sin efectos. La prioridad visual responde a una instrucción expresa del
proyecto y a requisitos Must del PRD. Su implementación se limita a recursos nativos de
presentación, sin introducir WebView ni bibliotecas web.
Exclusiones y Control del Alcance
La versión académica excluye contabilidad completa, facturación fiscal, pasarelas de
pago, aplicaciones móvil o web y portal del inquilino. También quedan fuera mensajes
automáticos por WhatsApp, SMS o correo, firma electrónica y OCR. El despliegue inicial reúne
aplicación y PostgreSQL en un equipo Windows, con verificación de dos clientes contra una
base en LAN como escenario adicional de consistencia. La propuesta no supone acceso a
equipos de propietarios reales. Cualquier solicitud que agregue esos canales o servicios deberá
clasificarse como trabajo posterior, con estimación independiente de las horas aquí distribuidas.
Steven Cuadra registrará cada cambio solicitado como una incidencia de alcance
vinculada al requisito afectado y a la sesión que lo origine. Alfredo Montalvan calculará su
efecto sobre datos y transacciones, Caleb Tardencilla sobre pantallas y pruebas, y Aris Gavarrete
sobre integración y distribución. La decisión comparará horas restantes, dependencias y
evidencia RA que pueda perderse. Si la carga supera la capacidad libre del período, el equipo
moverá primero trabajo Should o Could y conservará la base demostrable. La decisión quedará
propuesta en el tablero, sin atribuir aceptación a un cliente inexistente.

## 9
Metodología de Trabajo
El equipo aplicará una adaptación académica de Scrum con planificación por incrementos
y seguimiento en horas. Schwaber y Sutherland (2020) describen objetivos de sprint, revisión del
incremento y definición de terminado como elementos del marco. Latch adoptará esas prácticas
con seguimiento diario asíncrono y períodos inicial y final parciales, por lo que la organización
propuesta no equivale a una aplicación íntegra de Scrum. Steven facilitará coordinación y orden
del backlog sin representar a un cliente externo. Los cuatro integrantes participarán en decisiones
técnicas y revisarán trabajo escrito por otra persona.
Cadencia y Tablero
Cada planificación durará veinte minutos por integrante, con revisión de dependencias,
carga individual y criterio de salida. El seguimiento diario consistirá en un mensaje breve con
tarea activa, horas registradas, siguiente paso y bloqueo concreto. La revisión del incremento y la
retrospectiva compartirán veinticinco minutos por integrante al cierre del período. Esas reuniones
y el seguimiento consumen la reserva de coordinación, y sus duraciones son límites propuestos.
La Tabla 3 fija los estados del tablero, los responsables de actualización y la evidencia necesaria
para mover un elemento hacia terminado.
## Tabla 3
Estados del Tablero y Criterios de Movimiento
EstadoCondición y EvidenciaResponsable Propuesto
PreparadaRequisito, datos, horas y dependencias suficientes
para comenzar.
Steven coordina; autor
estima.
En cursoUna TAR activa por persona; actualización de
esfuerzo y bloqueo.
Responsable nominal.
En revisiónPull request enlazado; compilación y
comprobaciones pertinentes.
Revisor cruzado.

## 10
EstadoCondición y EvidenciaResponsable Propuesto
En pruebaApp-image ZIP ejecutable con etiqueta canónica de
la versión y casos aplicables con fixture definido.
Caleb coordina; otro
integrante opera.
TerminadaCriterio aceptado; evidencia, manual y trazabilidad
actualizados.
Persona A del entregable.
BloqueadaCausa y siguiente acción registradas; no suma
avance aceptado.
Autor y Steven revisan
tras un día.
GitHub Projects organizará incidencias por TAR, módulo, requisito, sprint, responsable,
horas y prioridad. Las columnas serán Preparada, En curso, En revisión, En prueba, Terminada y
Bloqueada, con un enlace al pull request cuando exista código. Cada integrante mantendrá una
sola tarea de construcción activa y podrá revisar una segunda tarea de otra persona. Un bloqueo
tendrá causa y siguiente acción, y seguirá contando dentro del trabajo pendiente. El tablero no
convertirá un porcentaje escrito por el autor en evidencia de avance; terminado exigirá cumplir el
criterio definido para el entregable.
Definición de Listo y de Terminado
Una tarea estará lista cuando tenga requisito existente, descripción verificable,
responsable propuesto, estimación, datos de entrada y dependencias identificadas. El integrante
verificará que pueda terminarla en el período sin esperar una técnica académica todavía no
cubierta, salvo estudio anticipado expresamente planificado. Las tareas amplias usarán subtareas
de ejecución sin crear capacidad adicional. Si el autor descubre una omisión técnica que supera
su estimación, registrará el desvío antes de consumir la reserva. Steven revisará ese desvío en el
seguimiento y propondrá una reasignación dentro de los límites individuales del sprint.
Una tarea estará terminada cuando compile, tenga revisión cruzada y los casos aplicables
presenten evidencia de ejecución. Las operaciones financieras deberán demostrar reversión sin
registros parciales y ausencia de duplicados. Las pantallas conservarán ambos temas, teclado,
mensajes y estados de carga o error, mientras las exportaciones deberán abrirse y coincidir con

## 11
su oráculo. El autor actualizará manual y trazabilidad cuando cambie una conducta observable.
La etiqueta solo incorporará tareas terminadas; una función parcialmente conectada conservará
su estado En curso aunque exista una captura de pantalla.
Estimación y Aprendizaje Anticipado
Las estimaciones representan horas de trabajo activo, sin valorar habilidades personales
ni duración de calendario. La programación usa tareas de dos o tres horas y supone experiencia
previa con formularios y SQL. Los estudios anticipados de Hibernate, JasperReports y HTTP
producirán interfaces o pruebas pequeñas que se revisarán al recibir la clase correspondiente. Esa
decisión reduce la dependencia tardía sin afirmar dominio previo de esas tecnologías. La reserva
cubre coordinación, revisión e incertidumbre, pero no permite aumentar la disponibilidad
semanal sin una nueva estimación explícita.
Equipo y Responsabilidades
La propuesta distribuye coordinación, seguridad y concurrencia a Steven Cuadra; datos,
PostgreSQL y persistencia a Alfredo Montalvan. Caleb Tardencilla coordinará JavaFX, FXML,
experiencia de usuario y pruebas, mientras Aris Gavarrete asumirá HTTP, reportes y
distribución. Los cuatro deberán conocer el recorrido financiero para mantener una demostración
viable si falta un integrante. La Tabla 4 muestra la responsabilidad principal y el relevo técnico
de cada persona. Estas asignaciones derivan de la base del encargo y no describen un acuerdo
celebrado ni una evaluación de competencias individuales.

## 12
## Tabla 4
Asignación Nominal y Relevo Técnico
IntegranteResponsabilidad PropuestaRevisión y Relevo
Steven CuadraCoordinación, requisitos, seguridad,
H1/H2/H6 y riesgos; H4: sección
crítica y bloqueo local.
Revisa interfaz y pruebas; Aris
cubre coordinación y cierre.
Alfredo MontalvanPostgreSQL, migraciones, JDBC,
entidades Hibernate y exactitud
financiera; H4: contador, transacción
única, numeración y TAR-038.
Revisa SQL y reportes; Steven
cubre invariantes, sección crítica
y bloqueo local; Alfredo
mantiene transacción única y
numeración.
Caleb TardencillaFXML, controles JavaFX, tokens
Aurora, preferencias y coordinación de
pruebas.
Revisa interacción y manuales;
Aris cubre recorrido de interfaz.
Aris GavarreteHttpClient, Gson, JasperReports,
empaquetado y despliegue.
Revisa concurrencia y
distribución; Alfredo cubre
reportes y Steven HTTP
simulado.
Nota. Propuesta basada en el encargo; capacidad estimada de 12 h semanales por integrante.
Revisión Cruzada y Decisión Técnica
Steven revisará seguridad y cierre con Aris como segundo lector, y Alfredo revisará los
cambios transaccionales antes de integrarlos. Caleb revisará las pantallas de Aris y los estados de
interacción, mientras Steven comprobará los criterios de prueba redactados por Caleb. El autor
no aprobará su propio pull request como única revisión. Cuando dos integrantes propongan
soluciones distintas, ambos deberán comparar la invariante, el requisito y el costo de prueba. La
alternativa adoptada quedará en un registro de decisión con fecha, consecuencias y evidencia
requerida para reconsiderarla.
La matriz RACI de la Tabla 5 asigna una sola persona A por entregable, responsable de
coordinar su cierre documental o técnico. La letra R identifica quien ejecuta y puede coincidir
con A; C requiere consulta antes de cerrar e I requiere comunicación del resultado. La matriz no

## 13
sustituye al responsable nominal de cada TAR. El docente es destinatario de evidencia
académica y no aparece como aprobador de tareas internas según el alcance de responsabilidad
académica descrito en el PRD (Cuadra et al., 2026c). El equipo conservará la responsabilidad
conjunta de la defensa y del acceso a los archivos entregados.
## Tabla 5
Matriz RACI de Entregables
EntregableSteven
## Cuadra
## Alfredo
## Montalvan
## Caleb
## Tardencilla
## Aris
## Gavarrete
Alcance y calendarioA/RCCC
Seguridad y rolesA/RCCR
Esquema y migracionesCA/RIC
Persistencia y transaccionesRA/RCC
FXML y temasCIA/RR
Concurrencia y cierreA/RRCC
HTTP y cachéRCCA/R
Reportes y oráculosCRCA/R
Pruebas y conformidad visualRRA/RR
Manual y defensaA/RRRR
App-image ZIP y respaldo de
entrega
## CRCA/R
Nota. A: coordina aceptación interna; R: ejecuta; C: consulta previa; I: información. Cada fila
tiene una sola A.
Capacidad Individual y Relevo
Cada integrante tiene una capacidad propuesta de 12/7 horas por día calendario dentro del
intervalo del proyecto. Sprint 0 y Cierre contienen seis días, por lo que cada persona dispone de
10,285714 horas en cada uno. Los sprints de siete días admiten doce horas individuales, y Sprint
4 contiene ocho días con 13,714286 horas por integrante. La carga programada será de ocho,

## 14
diez, diez, diez, once y ocho horas por persona en los seis períodos. La diferencia permanece
como reserva y no se reportará como tiempo efectivamente ejecutado.
Si falta una persona, el relevo tomará el recorrido demostrable y las tareas críticas que
quepan en su capacidad restante. El equipo reducirá alcance opcional antes de repartir trabajo
que exceda las horas disponibles de los demás. La ausencia de dos jornadas activará RSK-07 y
una revisión del calendario por Steven. Cada autor conservará instrucciones de construcción y
una explicación de su componente en el repositorio para facilitar ese relevo. Las credenciales
personales no se compartirán; el segundo expositor utilizará cuentas sintéticas propias con el rol
correspondiente.
Calendario Alineado al Contexto Académico
Sesiones y Evaluaciones de Referencia
El calendario de la Tabla 6 reproduce las fechas académicas aportadas como supuestos de
planificación, con entregables propuestos del equipo. La sesión del 21 de octubre antecede a la
fecha base y sirve únicamente como referencia de contenido previo. El 22 inicia la programación
de este plan y no acredita tareas históricas completadas. La evaluación de concurrencia del 27
exige un esqueleto demostrable de tareas, sincronización y cierre antes de integrar toda la gestión
de alquileres. La mención de cuarenta puntos se conserva sin inventar distribución,
ponderaciones ni descriptores de la rúbrica.
## Tabla 6
Calendario Académico Usado como Supuesto
Fecha de
## 2026
Sesión y ContenidoEvaluación de
## Referencia
## Entregable Previsto
21 y 22 oct.Sincronización y
detención de hilos.
Contenido previo y
sesión de base.
Esqueleto H1/H4/H6; el 21 no
se registra como trabajo
realizado.

## 15
Fecha de
## 2026
Sesión y ContenidoEvaluación de
## Referencia
## Entregable Previsto
27 oct.Concurrencia.Evaluación indicada.v0.1.0 con cancelación y sección
crítica.
28 oct. a 5
nov.
Hibernate ORM.Desarrollo de
persistencia.
Entidades y contratos;
integración del commit
financiero.
6 nov.Laboratorio Hibernate.Laboratorio indicado.Revisión de mapeos, cascadas y
rollback.
10 a 12 nov.JasperReports.Clases de reportes.Diez plantillas y DTO con
consulta oráculo.
16 nov.Reportes.Evaluación indicada.PDF y XLSX coherentes por
corte y moneda.
17 nov.Laboratorio
JasperReports.
Laboratorio indicado.v0.4.0 con comparación de
totales.
18 y 19 nov.REST y JSON.Integración de
servicios.
HttpClient, Gson y 14
escenarios simulados.
24 y 25 nov.Desarrollo guiado.Corrección y
preparación.
Regresión Must, app-image ZIP
y v0.9.0-rc.1 del 25.
26 nov.Presentación de
referencia.
Evaluación final
supuesta.
Documentos y versión
demostrable interna.
1 dic.Presentación y cierre de
referencia.
Evaluación final
supuesta.
v1.0.0, evidencia y copias
verificadas.
Nota. Fuente: resumen académico aportado. Se menciona una rúbrica de 40 puntos sin desglose
suministrado; no se asignan pesos.
Dependencias con las Clases
Sprint 0 usará JDBC para seguridad y para una prueba aislada del contador, con un Task
que procese contratos sintéticos en memoria. Esa prueba tiene el propósito de evidenciar RA3 el
27 y no será la versión final del cobro. Desde el 28, Alfredo incorporará Hibernate detrás de
repositorios y conservará JDBC para seguridad, auditoría y agregados. La primera operación
financiera definitiva nacerá con una única transacción ORM y helpers sobre su conexión. La
transición evita mantener dos implementaciones independientes que escriban pagos o consuman
consecutivos sobre la misma operación.

## 16
JasperReports tendrá una plantilla mínima de prueba anticipada, seguida del diseño de las
diez plantillas después del contenido del 10 al 12 de noviembre. El equipo entregará DTO
separados de entidades persistentes y comparará reportes con consultas por fecha de corte o, para
ocupación, por el instante de generación. HTTP tendrá un puerto de integración y fixtures desde
Sprint 0, pero el adaptador definitivo se integrará durante las sesiones REST y JSON del 18 y 19.
Los días 24 y 25 se reservan para regresión y correcciones guiadas. El calendario no depende de
que aparezcan clases adicionales antes de la presentación.
Plan por Sprints
Las iteraciones siguen los intervalos solicitados, con salidas que se acumulan en una
versión demostrable. La Figura 1 presenta el Gantt de los seis períodos y las dependencias
académicas de ORM, reportes e integración HTTP. Las barras representan ventanas propuestas
de trabajo, mientras los hitos marcan revisiones internas y sesiones de referencia. Los colores
distinguen grupos de trabajo y la leyenda forma parte de la figura. El detalle de fechas y
precedencias corresponde al Apéndice A, que conserva 96 tareas con asignación nominal y
estimación de esfuerzo.

## 17
## Figura 1
Gantt de Iteraciones y Dependencias Académicas
Nota. Ventanas y entregables propuestos; los hitos no acreditan actividades realizadas. Fechas
basadas en el resumen académico recibido.
## Sprint 0
Del 22 al 27 de octubre el equipo preparará una construcción repetible, seguridad JDBC y
el esqueleto de concurrencia. Caleb entregará acceso y ventana lateral FXML con los dos temas,
selector, persistencia local y modalidad sin efectos. Steven probará cancelación y cierre sobre un
lote sintético, mientras Alfredo comprobará el contador transaccional con rollback. Aris
producirá interfaces HTTP y una plantilla mínima como estudios anticipados limitados. La salida

## 18
propuesta v0.1.0 exige recorrer acceso y tareas sin congelar JavaFX, con evidencia de la sección
crítica antes de la evaluación del 27.
Las tareas TAR-001 a TAR-016 suman 32 horas, distribuidas por igual entre los
integrantes. La reserva de 9,142857 horas cubrirá ceremonias, revisión adicional y correcciones
detectadas durante la evaluación de concurrencia. El riesgo principal es dedicar tiempo a efectos
visuales antes de demostrar cancelación y sincronización. La contingencia conservará luces
estáticas simplificadas con los mismos tokens y ambos temas, junto con un lote pequeño
reproducible. El estudio temprano de reportes no condicionará el acceso ni la evaluación de hilos
y deberá cerrarse dentro de su estimación de dos horas.
## Sprint 1
Del 28 de octubre al 3 de noviembre el objetivo será incorporar entidades Hibernate y
completar el recorrido de catálogos a contrato. Alfredo mapeará propiedades, unidades,
inquilinos y contratos con control de versión y auditoría en la misma conexión. Caleb integrará
formularios y filtros reutilizables sobre esos servicios, mientras Steven verificará ocupación,
duración contractual y permisos. Aris preparará DTO históricos y fixtures del servicio sin
consumir una cuenta real. La salida v0.2.0 exigirá activar un contrato y registrar garantía sin
dejar unidad o depósito en estados incompatibles.
Las tareas TAR-017 a TAR-032 suman cuarenta horas y la reserva de ocho, con diez
horas programadas por persona. Las clases de Hibernate comienzan dentro del período y
respaldan la transición desde los puertos preparados en Sprint 0. El riesgo de mapeos o cascadas
incorrectos se tratará con una base aislada y pruebas de rollback antes de conectar el cobro. El
revisor rechazará cascadas de borrado sobre historia financiera y entidades entregadas al

## 19
controlador después de cerrar sesión. Los campos o adornos opcionales no bloquearán la salida si
las validaciones y el historial mínimo funcionan.
## Sprint 2
Del 4 al 10 de noviembre el equipo integrará H1, H2, pagos parciales y numeración
comercial sobre el modelo definitivo. Steven implementará renta y mora con bloqueo por
contrato; Alfredo cerrará la unidad transaccional de pago, aplicaciones, contador, recibo y
auditoría. Caleb conectará cobranza, progreso y liquidación, con verificación de teclado y
cambios de tema. Aris generará el recibo mínimo desde una instantánea confirmada y preparará
consultas de reportes. La salida v0.3.0 exige un pago reproducible y una anulación autorizada
con conservación de número e historia.
El laboratorio ORM del 6 permite revisar los mapeos antes de cerrar la integración
financiera, y la clase de reportes del 10 permite ajustar la plantilla anticipada. Las cuarenta horas
de TAR-033 a TAR-048 incluyen doce escenarios concurrentes y revisión cruzada del commit
financiero, con ocho horas de reserva. El riesgo principal es una operación que confirme
auditoría o contador por separado. Su disparador será cualquier registro huérfano en CP-042 o
inconsistencia entre aplicaciones y saldo. La contingencia suspenderá nuevas funciones y
utilizará la reserva para corregir esa invariante antes de etiquetar v0.3.
## Sprint 3
Del 11 al 17 de noviembre el incremento completará mantenimiento, panel, respaldo y
los diez reportes documentados. Aris diseñará las plantillas con Studio 7.0.8 y entregará PDF y
XLSX tabular según corresponda. Alfredo preparará consultas oráculo y restauración aislada,
Caleb integrará configuración y revisará las cuarenta pantallas en ambos temas, y Steven
verificará permisos y ciclo de vida de trabajadores. Las clases, evaluación y laboratorio de

## 20
JasperReports sostienen la revisión del conjunto. La salida v0.4.0 exige totales reconciliados por
moneda y un respaldo íntegro con manifiesto de adjuntos.
TAR-049 a TAR-064 mantienen cuarenta horas programadas y ocho de reserva para el
período. El riesgo de incompatibilidad de plantillas se reduce al diseñar y compilar con la misma
generación 7.0.8, sin incorporar el adaptador Hibernate de JasperReports. Un error de enlace o
una cifra distinta al SQL bloquea la aceptación del reporte afectado. La contingencia simplificará
su composición y reutilizará cabeceras y consultas probadas, manteniendo los diez contenidos
exigidos. Ninguna simplificación permitirá sumar NIO con USD o exportar decoración Aurora
dentro de los comprobantes financieros.
## Sprint 4
Del 18 al 25 de noviembre el objetivo será integrar HTTP, cerrar la regresión y preparar
la versión candidata para presentar. Aris desarrollará HttpClient y Gson a partir de los fixtures
existentes, Alfredo separará caché de autorización y Steven verificará programación compartida
y fallos. Caleb ejecutará la matriz de temas, preferencias, popups y teclado junto con el recorrido
de cobro. El equipo medirá los umbrales del TRD en el entorno sintético definido y generará el
paquete Windows. La salida v0.9.0-rc.1 exige evidencia de HTTP simulado y un recorrido local
disponible ante caída de Internet.
El período contiene ocho días y admite 54,857143 horas, con cuarenta y cuatro
programadas en TAR-065 a TAR-080 y 10,857143 reservadas. Las tareas que dependen de
REST y JSON se realizan después del inicio de las clases del 18, y la regresión final usa el
desarrollo guiado del 24 y 25. El riesgo es confundir una demostración del servidor simulado con
disponibilidad real del proveedor comercial. El registro de evidencia distinguirá los dos entornos

## 21
y no incluirá una consulta autenticada si no fue ejecutada. El 25 se congelarán código, datos y
documentos de la entrega interna.
## Cierre
Del 26 de noviembre al 1 de diciembre el equipo presentará el incremento, registrará
observaciones académicas y realizará correcciones dirigidas. Las tareas TAR-081 a TAR-096
incluyen verificaciones previas, un ensayo el 30 y la defensa de referencia del 1 de diciembre.
Cada integrante tiene ocho horas programadas dentro de su capacidad parcial de 10,285714
horas. La reserva conjunta de 9,142857 horas cubre coordinación y fallos de última etapa. El
cierre documental exigirá conservar etiqueta, manifiesto, respaldo, manual y evidencia
accesibles, sin afirmar recepción o calificación antes de que exista constancia.
Las observaciones del 26 se clasificarán por requisito y severidad antes de modificar la
versión candidata. Un defecto que afecte dinero, permisos o cierre recibirá atención prioritaria y
obligará a repetir las pruebas de su dependencia. Las ampliaciones de alcance sin relación con
RA quedarán registradas como evolución posterior cuando no quepan en la capacidad libre. La
salida v1.0.0 no aceptará defectos críticos abiertos ni funciones Must sin recorrido verificable. Si
la corrección no alcanza esa condición, el equipo declarará el incumplimiento concreto y
presentará la última versión consistente con su limitación identificada.
Estructura de Desglose del Trabajo y Backlog
La estructura de desglose organiza trabajo por fundamentos, dominio, finanzas,
presentación, integración, calidad y entrega. Los doce módulos mantienen sus códigos M01 a
M12 y los seis trabajadores conservan H1 a H6. Cada tarea de producto TAR tiene un requisito
existente del PRD como vínculo principal; las tareas de gestión remiten a D44, D45 o D47 según
su propósito, además de responsable, horas, precedencia, fecha y sprint. El Apéndice A contiene

## 22
el backlog completo y la Tabla 7 resume los paquetes. La estimación supone formularios y
servicios reutilizables; las horas de revisión y pruebas explícitas ya forman parte de las tareas
programadas.
## Tabla 7
Paquetes de Trabajo y Dependencias
PaqueteTareas y MódulosSalida Verificable
FundamentosTAR-001 a TAR-016; M01, M05,
## M06, M09, M10, M11, M12.
Construcción, dos temas, JDBC y
evidencia RA3.
Dominio y ORMTAR-017 a TAR-032; M01 a M04,
M06, M09 a M12.
CRUD reutilizable, contrato y
garantía de activación.
FinanzasTAR-033 a TAR-048; M05 a M07,
M10 a M12.
Renta, mora, pago, contador y
liquidación en transacciones.
Reportes y
operación
TAR-049 a TAR-064; M01, M08,
M10 a M12.
Diez reportes, respaldo y panel.
Integración y
regresión
TAR-065 a TAR-080; M01, M06,
M09 a M12.
HTTP, matriz visual, medidas e
app-image ZIP v0.9.0-rc.1.
Entrega y cierreTAR-081 a TAR-096; M06, M09 a
## M12.
Dos sesiones de referencia,
correcciones y archivo de evidencia.
Capacidad Frente a Carga
La Tabla 8 compara capacidad exacta, carga y reserva por período sin redondear antes de
sumar. El intervalo comprende cuarenta y un días calendario y equivale a 48 × 41/7 =
281,142857 horas del equipo. Las 228 horas programadas dejan 53,142857 horas para
coordinación e incertidumbre, equivalentes al 18,902439 % de la capacidad. El plan asigna
9,142857 horas conjuntas de reserva a Sprint 0 y a Cierre, con redondeo únicamente para lectura.
Las cifras detalladas mantienen las fracciones que explican cualquier diferencia de centésimas en
los totales mostrados.

## 23
## Tabla 8
Capacidad y Carga por Período en Horas
PeríodoDíasCapacidad
## Equipo
CargaReservaCarga / Capacidad por
## Persona
## Sprint 0
22/10 a 27/10
## 641.142857329.1428578 / 10.285714
## Sprint 1
28/10 a 03/11
## 748.000000408.00000010 / 12.000000
## Sprint 2
04/11 a 10/11
## 748.000000408.00000010 / 12.000000
## Sprint 3
11/11 a 17/11
## 748.000000408.00000010 / 12.000000
## Sprint 4
18/11 a 25/11
## 854.8571434410.85714311 / 13.714286
## Cierre
26/11 a 01/12
## 641.142857329.1428578 / 10.285714
## Total41281.14285722853.14285757 / 70.285714
Nota. Capacidad = días × 48/7; las sumas usan precisión completa. La fracción horaria es una
estimación de disponibilidad.
La reserva tiene dos usos separados dentro de un mismo límite de capacidad. La Tabla 9
asigna 4,5 horas de coordinación por período y conserva el resto para correcciones, reestimación
o relevo. El seguimiento diario es asíncrono y su tiempo se integra en esas 4,5 horas, con
mensajes de hasta dos minutos por integrante en los días efectivos de actividad. Las revisiones ya
estimadas no se cobran de nuevo como trabajo adicional. La activación de reserva exige registrar
TAR afectada, causa y horas, y sigue prohibiendo superar la capacidad individual del período.
## Tabla 9
Uso Propuesto de la Reserva en Horas
PeríodoCoordinaciónCorrecciones e
## Incertidumbre
## Reserva Total
## Sprint 04.5000004.6428579.142857

## 24
PeríodoCoordinaciónCorrecciones e
## Incertidumbre
## Reserva Total
## Sprint 14.5000003.5000008.000000
## Sprint 24.5000003.5000008.000000
## Sprint 34.5000003.5000008.000000
## Sprint 44.5000006.35714310.857143
## Cierre4.5000004.6428579.142857
## Total27.00000026.14285753.142857
Nota. El seguimiento, planificación y revisión consumen coordinación. La reserva libre no se
duplica con las pruebas ya programadas.
Dependencias y Secuencia de Integración
La secuencia de construcción parte de catálogos y seguridad, continúa con contratos y
completa cobro antes de calcular reportes consolidados. H1 y H2 dependen de reglas
contractuales, mientras H4 requiere contador persistente y transacción financiera única. H5
consume DTO obtenidos desde una consulta consistente, sin mantener una sesión abierta durante
interacción o escritura de archivos. H3 guarda referencias sin retener bloqueos durante la espera
HTTP. El Gantt muestra ventanas paralelas posibles, pero una dependencia TAR dentro de la
misma ventana obliga a terminar primero el antecedente y registrar esa secuencia.
El trabajo financiero seguirá el orden de bloqueo del esquema: puerta de escritura,
unidad, contratos, cargos, pagos de origen, depósitos y contador. PostgreSQL documenta
bloqueos por fila y detección de interbloqueos como mecanismos que requieren coordinación de
la aplicación (The PostgreSQL Global Development Group [PGDG], s. f.-a). El equipo
mantendrá el bloqueo local H4 hasta conocer el resultado transaccional y usará el bloqueo
persistente para clientes distintos. Los reintentos por 40P01 o 40001 conservarán el UUID y
tendrán un máximo de tres intentos totales, con Session nueva en cada uno.

## 25
El contador comercial utilizará una fila actualizada dentro del mismo commit que el pago
y su recibo. PostgreSQL advierte que nextval no recupera números al revertir, por lo que una
secuencia no garantiza continuidad sin saltos (PGDG, s. f.-d). La prueba de rollback deberá
comprobar que el siguiente cobro confirmado reciba el número que no llegó a emitirse. Una
anulación conservará el número y su estado visible, mientras un fallo de PDF permitirá
reimprimir el mismo recibo. La interfaz nunca mostrará una reserva previa al commit como
comprobante emitido.
Hitos y Entregables
La Tabla 10 fija hitos propuestos con una evidencia prevista para cada criterio de salida.
Un hito de la Tabla 10 cambia de planificado a alcanzado cuando el revisor identifica revisión
Git, entorno, fecha y archivos de comprobación. El porcentaje de tareas terminadas no permite
dar por cumplida una invariante financiera que todavía falla. Los hitos de concurrencia,
integración ORM, reportes y HTTP acompañan las evaluaciones académicas de referencia. El 26
corresponde a la entrega interna y primera presentación supuesta; el 1 de diciembre corresponde
al cierre de referencia y no a una calificación previamente obtenida.
## Tabla 10
Hitos y Evidencia de Aceptación Propuesta
Hito y FechaEntregableCriterio de AceptaciónEvidencia Prevista
HIT-01; 27/10v0.1.0; FXML y
concurrencia.
Dos temas, selector
persistente; Task cancelable y
contador con rollback.
Video interno corto,
consultas y log H6.
HIT-02; 03/11v0.2.0; dominio
con ORM.
Alta, consulta, edición y baja
lógica según permisos; activar
contrato sin solape.
SQL y pruebas de
repositorios sobre
PostgreSQL.
HIT-03; 06/11Revisión de
laboratorio ORM.
Sin cascada de borrado
financiero ni sesiones
compartidas.
Árbol de dependencias
y registro de revisión.

## 26
Hito y FechaEntregableCriterio de AceptaciónEvidencia Prevista
HIT-04; 10/11v0.3.0; circuito
financiero.
Pago parcial, recibo, anulación
y garantía íntegros.
CP-026 a CP-036 y
CP-041 a CP-052.
HIT-05; 17/11v0.4.0; diez
reportes.
Totales por moneda y corte
iguales a SQL; respaldo
restaurable.
PDF, XLSX y oráculos
CP-067 a CP-076.
HIT-06; 21/11Adaptador HTTP.JSON válido, caché y fallos
previstos sin bloqueo de UI.
Registro del servidor S1
y casos HTTP.
HIT-07; 25/11v0.9.0-rc.1
congelada.
Cien casos y variantes
ejecutados; cero defecto
crítico/alto Must.
Informe de pruebas,
app-image ZIP y
manifiesto.
HIT-08; 26/11Entrega interna y
sesión.
Seis documentos, manual,
ejecutable y copias accesibles.
Manifiesto, hashes y
protocolo de apertura.
HIT-09; 30/11Ensayo final.Veinte minutos propuestos;
transiciones sin perder
escenario.
Registro de tiempos por
bloque y regresión
dirigida.
HIT-10; 01/12v1.0.0 y cierre.RA1 a RA5 mostrados;
correcciones verificadas y
archivos íntegros.
Etiqueta, historial de
defectos y paquete
archivado.
El paquete de cada hito conservará código etiquetado, instrucciones de arranque y un
resumen de los casos ejecutados sobre ese incremento. Aris verificará que los archivos abran
desde una copia distinta de su directorio de desarrollo y Alfredo revisará los scripts con una base
vacía. Caleb comprobará que el manual describa los rótulos reales de pantalla y Steven cotejará
los resultados RA cubiertos. Si un entregable falla, el equipo conserva la versión anterior y
registra el motivo de rechazo. La nueva etiqueta solo se emitirá después de repetir las
verificaciones relacionadas con la corrección.
Plan de Pruebas y Aseguramiento de Calidad
La estrategia combina pruebas unitarias de reglas con integración sobre PostgreSQL real,
servidor HTTP simulado y recorridos de escritorio. JUnit 5.12.2 se conserva como decisión del
TRD y los fixtures controlarán fecha, UUID y tasa sin requerir datos privados (Cuadra et al.,
2026d). El Apéndice B incluye cien casos CP con precondición, pasos, resultado esperado, tipo y

## 27
prioridad. Sus procedimientos están dirigidos a una persona distinta del autor del componente.
Ningún caso contiene un resultado obtenido atribuido a Latch porque esta entrega no incluye
ejecución de su código.
Entornos, Datos y Herramientas
El entorno de referencia será Windows 11 x64, CPU de cuatro núcleos o más, ocho GiB
de RAM y almacenamiento SSD. La aplicación utilizará el runtime Java 21 seleccionado y
PostgreSQL 18.6 en una base aislada para cada lote reproducible. Un segundo proceso Java o un
segundo equipo LAN verificará las secciones críticas entre clientes independientes. Los relojes
de prueba fijarán America/Managua para fechas locales y UTC para instantes persistidos. La
Tabla 11 define los conjuntos D0 a D4 y S1, con reinicio entre casos que alteren el saldo o el
contador.
## Tabla 11
Entornos y Datos Sintéticos Reproducibles
ConjuntoDatos y PrecondiciónUso y Reinicio
D0; base limpiaDos cuentas activas: Propietario creado
con Aprovisionar.java y Asistente dado
de alta mediante FLU-03; nombres
Demo; cuatro unidades activas en dos
propiedades; sin identificaciones reales.
Seguridad, catálogos y UI. Recrear
base con migraciones y semilla;
después aprovisionar el Propietario
y dar de alta al Asistente. Las
cuentas inactivas de T001 no
satisfacen D0.
D1; contratosTres contratos elegibles de prueba y uno
terminado; C1 de 100 USD, depósito
100 USD; estados controlados por reloj.
Equivalencia: CT-001 (BRI/FLU,
Apéndice B) = CT-01 (FLU E2E) = C1
## (PLA).
Contratos, renta, mantenimiento y
garantía. Cada escenario parte de
una copia independiente.
D2; cobranzaC1 con cargos octubre y noviembre de
100 USD cada uno, vencidos a su corte;
sin pagos salvo variante declarada.
Pago 40, excedente 230, anulación
y contador inicial 1; restaurar entre
casos.

## 28
ConjuntoDatos y PrecondiciónUso y Reinicio
D3; conversiónTasa ficticia 36.50000000 NIO/USD;
autorización del Propietario para la
fecha fija de cada caso.
1460 NIO = 40 USD; copia de tasa
y prueba de autorización faltante.
No es valor real ni tasa BCN.
D4; volumen500 contratos, 1000 inquilinos, 10000
cargos/filas y 100000 eventos; respaldo
de BD y adjuntos de 1 GiB cada uno.
Bases equivalentes por repetición;
semilla y tamaños en manifiesto; no
mezclar con D2 de demostración.
## S1; HTTP
simulado
HttpServer loopback; JSON success,
result=error, cuerpos inválidos,
cabeceras y latencias controladas.
No consulta proveedor ni usa
secreto real. Reloj virtual para
intervalos y monotónico para
latencia.
El servidor S1 se implementará con HttpServer del JDK en una dirección loopback y un
puerto asignado por el sistema operativo. Sus rutas reproducirán códigos, cabeceras, cuerpos y
demoras del contrato remoto sin exponer llaves reales. HttpClient admite envío asíncrono y
manejo de recursos del cliente, según su documentación oficial (Oracle, s. f.-a). Los dobles de
reloj y planificador acelerarán esperas de sesenta segundos o veinticuatro horas en pruebas
unitarias. Las mediciones de latencia real usarán tiempo monotónico y no sustituirán el reloj de
pared por un avance artificial.
Entrada, Salida y Cobertura
La entrada a integración exige compilación satisfactoria, migraciones aplicadas, fixtures
identificados y revisión previa del código modificado. Una prueba bloqueada por falta de entorno
no cuenta como aprobada y deberá registrar la causa. La salida del candidato exige cien casos
definidos ejecutados en sus variantes aplicables, sin defectos críticos o altos abiertos que afecten
requisitos Must. Los casos visuales multiplican observaciones por tema, efectos y rol cuando
corresponde, sin inflar el número de identificadores CP. La Tabla 12 distribuye los cien casos y
asigna la ventana principal de ejecución.

## 29
## Tabla 12
Distribución de Casos y Ventana Principal
CasosCantidad y TipoEjecución Propuesta y Responsable
CP-001 a CP-04040 funcionales e
integración.
Sprint 1 a Sprint 4; Caleb coordina;
revisores alternos.
CP-041 a CP-05212 de concurrencia.TAR-045 y regresión TAR-070;
Steven y Alfredo.
CP-053 a CP-06614 HTTP con servidor
simulado.
TAR-069; Steven y Aris, tras clase de
## REST/JSON.
CP-067 a CP-07610 de reportes y
reconciliación.
TAR-062 y regresión TAR-072;
Alfredo y Aris.
CP-077 a CP-09418 Aurora/usabilidad, con
variantes.
TAR-063, TAR-071 y TAR-079;
Caleb y otro operador.
CP-095 a CP-1006 de despliegue y
rendimiento.
TAR-072, TAR-074, TAR-076 y
TAR-078; Aris y Alfredo.
Total100 identificadores de caso.Variantes y repeticiones se registran
sin contarlas como nuevos CP.
La cobertura de reglas monetarias incluirá valores límite, reversión y orden de
operaciones, además del recorrido exitoso. Para pagos, la suma de aplicaciones más crédito
deberá igualar el total convertido y ningún cargo tendrá saldo negativo. Los depósitos
conservarán su ecuación inicial igual a aplicaciones, devoluciones y remanente, con renta
separada de fondos retenidos. La prueba de rol invocará el servicio aunque la acción esté oculta
en pantalla. El revisor comprobará consultas parametrizadas mediante entradas literales y
confirmará que un error de validación no deje cambios parciales en la base.
Concurrencia y Ciclo de Vida
CP-041 a CP-052 cubren diez emisores concurrentes repartidos en dos JVM, cien pagos y
diez reversiones adicionales antes de commit, reversión de contador, doble solicitud,
confirmación incierta, H1 paralelo, H1 contra H2 y cancelación entre contratos. También
incluyen dos pagos sobre el mismo saldo, cierre durante lote, cierre durante HTTP y reporte,

## 30
respaldo contra escritura LAN y activación simultánea. Las barreras de prueba controlarán
intercalaciones sin depender de pausas arbitrarias entre hilos. Cada ejecución conservará conteos,
saldos y números confirmados como oráculos. Las pruebas del mismo proceso y de dos JVM son
necesarias porque ReentrantLock no protege una base compartida por equipos distintos.
H6 solicitará cancelación de trabajos y esperará terminación fuera del hilo gráfico, con
diez segundos como umbral para mostrar un bloqueo. H4 finalizará su transacción antes de
liberar la sección crítica y nunca recibirá una interrupción destinada a abortar un commit
incierto. La prueba verificará que el proceso permanezca abierto con aviso cuando los
trabajadores no hayan terminado. Una cancelación de H1 conservará contratos completos ya
confirmados y dejará los pendientes para una repetición idempotente. Los temporales de reportes
no se publicarán hasta completar escritura y cierre de recursos.
Servicio Web y Trazabilidad Cambiaria
CP-053 a CP-066 cubren éxito, caída, tiempo agotado, JSON inválido, base incorrecta,
NIO inadmisible, cuota y respuestas 429. Los casos restantes revisan credenciales, tamaño o
MIME, fechas, programación LAN y tasa histórica sin autorización de cobro. El adaptador
conservará una caché válida cuando falle la consulta y rechazará cuerpos superiores a 256 KiB.
Cada prueba inspeccionará intentos, cabeceras redactadas y la ausencia de una nueva tasa
aceptada tras un error. La respuesta HTTP 200 no bastará para aceptar JSON con result igual a
error.
El proveedor seleccionado es ExchangeRate-API Free, de AYR Tech, por continuidad
con los documentos recibidos. Su contrato estándar admite base USD y su catálogo incluye NIO
(AYR Tech (Pty) Ltd., s. f.-c, s. f.-d). La cuota publicada es de 1500 solicitudes mensuales con
actualización diaria y precio anunciado de USD 0 (AYR Tech (Pty) Ltd., s. f.-b). Sus términos

## 31
permiten caché interna, restringen redistribución y califican las tasas para uso informativo (AYR
Tech (Pty) Ltd., s. f.-e). Latch mantiene por ello una autorización separada por fecha para
cualquier cobro entre monedas.
La autenticación usará Authorization Bearer y GET /v6/latest/USD, sin llave incorporada
en la dirección ni en archivos de evidencia (AYR Tech (Pty) Ltd., s. f.-a). El diseño limita a cien
los intentos mensuales compartidos y programa una consulta diaria con un reintento por red o
5xx tras sesenta segundos. HTTP 429 respetará Retry-After y, si falta, esperará veinticuatro
horas. El BCN publicó para 2026 deslizamiento de cero por ciento y referencia oficial de
36.6243 NIO por USD (Banco Central de Nicaragua, 2025). Esa publicación no convierte las
respuestas comerciales del proveedor en tasas oficiales.
Reportes y Exactitud del Corte
CP-067 a CP-076 cubren los diez contenidos de reporte del PRD, con consultas oráculo y
tolerancia monetaria de 0.00. La comparación usará los mismos filtros, corte y moneda que los
DTO entregados a JasperReports; en ocupación se cotejará el instante de generación, sin selector
histórico. El equipo excluirá la garantía retenida del ingreso por renta y conservará visibles los
pagos anulados cuando el reporte corresponda a historial. Los archivos XLSX tabulares
contendrán importes numéricos y fechas interpretables. El reporte de tasas mostrará la copia
usada al pagar, aunque la referencia actual haya cambiado después de la operación.
La generación usará Library y Studio 7.0.8, versiones publicadas por Jaspersoft, y
recompilará las plantillas del proyecto en esa generación (Jaspersoft, 2026). El POM oficial 7.0.8
declara una extensión Hibernate 6.3.1.Final distinta del ORM seleccionado para Latch
(Jaspersoft, s. f.). La integración entregará JavaBeans o DTO y excluirá esa extensión para evitar
dos familias de Hibernate. La prueba de compatibilidad deberá compilar, llenar y exportar las

## 32
diez plantillas bajo JDK 21. La existencia de los artefactos documentados no equivale a haber
ejecutado ese ensayo de integración.
Conformidad Aurora y Usabilidad
CP-077 a CP-094 fijan dieciocho escenarios de presentación con resultados esperados en
Claro y Oscuro. La matriz incluye consistencia, contraste, teclado, foco, selección de tablas,
escalado, capa decorativa, recursos ausentes y reportes sin luces. También cubre cambio de tema
con formulario modificado o tarea activa, reinicios por usuario, errores del almacén local y
popups. El equipo ejecutará cada variante aplicable durante Sprint 4 y conservará capturas con
revisión y entorno. En esta edición permanecen como pruebas planificadas, sin atribuir tiempos,
satisfacción ni resultados observados en una aplicación inexistente.
El brief fija fondo Claro #F5F7FC y Oscuro #0F1424, superficies #FFFFFF y #182136, y
acentos #4F46E5 y #A5B4FC (Cuadra et al., 2026a). La Tabla 13 conserva pares funcionales
para comprobar contraste mediante luminancia relativa sRGB. World Wide Web Consortium
(W3C, 2024) aporta los umbrales de contraste de texto y señales que el proyecto adopta como
criterios técnicos de escritorio, sin afirmar una certificación WCAG completa. La preferencia
personal no cambia permisos ni importes. La modalidad sin efectos debe retirar luces y
desenfoques en los dos temas y conservar campos, mensajes y foco.
## Tabla 13
Pares Funcionales y Contraste Calculado
Tema y UsoFrente / FondoRazónUmbral Adoptado
Claro; Texto#17213A / #FFFFFF15.96870818:14.5:1; cumple en cálculo
Claro; Secundario#4B5568 / #FFFFFF7.50411060:14.5:1; cumple en cálculo
Claro; Acción#FFFFFF / #4F46E56.28752860:14.5:1; cumple en cálculo
Claro; Borde#64748B / #FFFFFF4.75884279:13:1; cumple en cálculo
Claro; Foco#4338CA / #FFFFFF7.90311148:13:1; cumple en cálculo

## 33
Tema y UsoFrente / FondoRazónUmbral Adoptado
Claro; Éxito#166534 / #F0FDF46.81165163:14.5:1; cumple en cálculo
Claro; Aviso#854D0E / #FFFBEB6.60658223:14.5:1; cumple en cálculo
Claro; Error#B42318 / #FEF2F26.00989513:14.5:1; cumple en cálculo
Oscuro; Texto#F1F5F9 / #18213614.63676799:14.5:1; cumple en cálculo
Oscuro; Secundario#B7C3D8 / #1821369.01850224:14.5:1; cumple en cálculo
Oscuro; Acción#111827 / #A5B4FC8.89860092:14.5:1; cumple en cálculo
Oscuro; Borde#94A3B8 / #1821366.25373330:13:1; cumple en cálculo
Oscuro; Foco#C4B5FD / #1821368.68558820:13:1; cumple en cálculo
Oscuro; Éxito#86EFAC / #12352A9.53585379:14.5:1; cumple en cálculo
Oscuro; Aviso#FDE68A / #3A2E1610.65685380:14.5:1; cumple en cálculo
Oscuro; Error#FCA5A5 / #3B20287.77432268:14.5:1; cumple en cálculo
Nota. Cálculo documental efectuado sobre tokens del brief; no acredita colores renderizados ni
conformidad de una aplicación implementada.
La implementación mantendrá Segoe UI y superficies opacas bajo tablas, formularios,
importes y comprobantes. OpenJFX documenta CSS y controles, además de RadialGradient y
GaussianBlur para decoración nativa (OpenJFX, s. f.-a, s. f.-c, s. f.-d). Aurora procede de la
descripción pública de fondos orgánicos difuminados de Malewicz (s. f.), según la entradilla
pública identificada en la Tabla F1. Las luces estáticas estarán fuera de la interacción mediante
mouseTransparent y no crearán hilos adicionales. El cambio de tema sustituirá colores sin
reconstruir formularios modificados ni alterar sus datos o permisos.
Gestión de Defectos y Evidencia
Cada defecto tendrá identificador DEF, título con conducta observada, revisión Git,
entorno, CP relacionado, datos usados y pasos reproducibles. El reporte añadirá resultado
esperado, resultado observado, severidad, prioridad y evidencia con secretos redactados. La
severidad Crítica bloqueará liberación por pérdida de dinero, acceso indebido o corrupción; Alta
impedirá una función Must; Media afectará una ruta con alternativa y Baja un detalle de

## 34
presentación. La prioridad definirá orden de atención y podrá cambiar sin alterar el impacto
técnico. La Tabla 14 fija el ciclo de vida y el criterio verificable para cerrar un defecto.
## Tabla 14
Ciclo de Vida de Defectos
EstadoAcción y ResponsableCriterio para Avanzar
NuevoOperador registra DEF, CP, entorno,
pasos y evidencia.
Reproducible y sin secreto en
adjuntos.
ClasificadoCaleb propone severidad y prioridad;
Steven resuelve conflicto.
Impacto y requisito identificados.
AsignadoDueño acepta corrección dentro de
capacidad disponible.
Rama y TAR vinculadas.
En correcciónAutor implementa cambio y prueba
pertinente.
Pull request con evidencia y
revisión cruzada.
En verificaciónOtra persona repite caso y regresión de
dependencias.
Conducta esperada observada en
revisión identificada.
CerradoCaleb registra verificación y etiqueta que
lo incluye.
Sin contradicción en regresión.
## Reabierto /
diferido
Reabrir por persistencia; diferir solo
Baja/Media sin incumplir Must.
Motivo, efecto y versión futura
explícitos; críticos/altos no se
difieren para v0.9.0-rc.1.
Caleb coordinará la ejecución y asignará a otra persona la confirmación de cada
corrección. El autor resolverá el defecto en una rama vinculada y añadirá una regresión cuando la
falla afecte una regla que pueda volver a romperse. El revisor repetirá el caso original y los casos
de sus dependencias, sin ampliar la batería por rutina cuando ya no exista una preocupación
concreta. Un defecto reabierto conservará su identificador e historial. La evidencia de salida
incluirá casos aprobados, fallidos y bloqueados con fecha, sin sustituir ausencias por porcentajes
estimados de conformidad.
Gestión de Configuración y Calidad de Código
El repositorio tendrá una rama main para incrementos revisados y ramas cortas
feature/TAR-nnn-descripcion-corta o fix/DEF-nnn-descripcion-corta. Los commits usarán feat|

## 35
fix|test|docs|build: texto (TAR-nnn). Cada pull request explicará comportamiento, requisito,
riesgo y verificaciones, con revisión ajena al autor. GitHub documenta protección de ramas y
comprobaciones, disponibles según visibilidad y plan de cuenta (GitHub, s. f.). El equipo
propone un repositorio público con GitHub Free que contenga código y datos sintéticos, para
utilizar esas protecciones sin contratar un plan privado. Esta propuesta documental no publica
archivos, crea cuentas ni modifica los permisos actuales del equipo.
Convenciones y Revisión
El código usará paquetes por presentación, aplicación, dominio e infraestructura, con
nombres consistentes con el glosario. Los controladores no construirán SQL ni iniciarán
commits, y cada servicio comprobará autorización antes de modificar datos. Hibernate
mantendrá una Session por caso de uso, sin compartirla entre hilos; ORM y JDBC usarán la
misma Connection financiera (Hibernate, s. f.-b). El revisor verificará parámetros SQL,
BigDecimal, excepciones, cierre de recursos y ausencia de secretos. Los comentarios explicarán
decisiones o restricciones que el código no exprese por sí mismo.
La Tabla 15 define una lista de revisión que se incorporará a la descripción de los pull
requests. El revisor comprobará cada criterio aplicable con un enlace a código o prueba y
justificará una exclusión por alcance. Los cambios de CSS deberán abrir acceso, panel, tabla y
diálogo en los dos temas antes de integrarse. Los cambios financieros exigirán comparación de
saldo y reversión, y los cambios de concurrencia deberán identificar recurso compartido y punto
seguro de cancelación. La aprobación de formato no sustituirá esas comprobaciones ni permitirá
omitir una migración que altere la estructura persistida.

## 36
## Tabla 15
Criterios de Revisión del Código
ÁreaComprobación ConcretaEvidencia Prevista
AutorizaciónServicio rechaza acciones reservadas aunque la
UI las oculte.
CP-005; guardas del caso de
uso.
PersistenciaPreparedStatement; sesión por operación; sin
commit JDBC independiente en finanzas.
Consulta revisada y prueba de
rollback.
DineroBigDecimal; dos decimales; tasa ocho;
## HALF_EVEN.
CP-029 a CP-031;
conservación de totales.
ConcurrenciaOrden de bloqueo, idempotencia, cancelación
entre contratos y H6.
CP-041: 10 hilos en 2 JVM,
100 pagos y 10 reversiones;
CP-042 a CP-052 según
variante.
PresentaciónClaro/Oscuro, preferencias, foco, errores y
superficie opaca.
CP-077 a CP-094 y capturas.
HTTPBearer externo redactado; tamaño/tiempos;
caché y cuota.
CP-053 a CP-066.
ReportesDTO fuera de sesión; corte/moneda; temporal
y sin aurora.
CP-067 a CP-076 y CP-086.
DistribuciónVersiones fijadas; licencias, manifiesto,
recursos y migraciones.
CP-095 a CP-097; árbol
Maven resuelto.
Versiones, Respaldos y Acceso
Las etiquetas v0.1.0, v0.2.0, v0.3.0 y v0.4.0 identificarán incrementos internos, v0.9.0-
rc.1 la versión congelada del 25 de noviembre y v1.0.0 el cierre propuesto. Cada etiqueta deberá
apuntar a una revisión inmutable con scripts, manual y manifiesto de distribución. Aris generará
una copia del repositorio con historial y otra del paquete de entrega en un medio distinto al
equipo principal. Steven comprobará acceso de los cuatro integrantes y apertura de los archivos
antes de cada presentación. Esta medida responde a la exigencia de acceso, versión y buen estado
indicada en el resumen académico aportado, sin atribuir lectura del syllabus original.
Las copias de trabajo no contendrán llaves HTTP, contraseñas de PostgreSQL ni datos
personales reales. El paquete conservará una configuración de ejemplo con valores sintéticos y

## 37
referencias a variables externas, sin credenciales operativas reutilizables. Un respaldo de datos
incluirá base, adjuntos y manifiesto cifrados con AES-256-GCM y contraseña de respaldo, bajo
la puerta exclusiva de RN-29 (Cuadra et al., 2026d). La restauración verificará autenticidad antes
de descifrar; una clave o etiqueta inválida producirá MSG-082 sin reemplazar datos. pg_dump
produce una exportación consistente de la base, pero Latch añade exclusión de escrituras para
coordinar también los archivos adjuntos (PGDG, s. f.-c). Alfredo verificará restauración en otra
base y cotejará conteos y hashes antes de considerar utilizable la copia.
Plan de Despliegue y Entrega
El destino principal será Windows 11 x64 con PostgreSQL 18.6 local y aplicación
empaquetada con su runtime. Aris generará con jpackage una app-image y la distribuirá como
ZIP, según ADR-14 del TRD (Cuadra et al., 2026d). Oracle (s. f.-c) documenta la creación de
imágenes de aplicación. El usuario de demostración extraerá el ZIP y ejecutará la aplicación sin
instalar un JDK de desarrollo. PostgreSQL y sus utilidades se instalarán por separado, con una
cuenta de ejecución de permisos mínimos y una base sintética identificada.
Instalación y Preparación de Datos
Alfredo ejecutará las migraciones versionadas sobre una base vacía y cargará D0, D1, D2
y D3 en un entorno de demostración separado de los ensayos de volumen. Hibernate utilizará
hbm2ddl.auto=validate y rechazará un esquema incompatible sin crearlo o alterarlo desde la
aplicación. Aris verificará rutas de recursos, fuentes de reportes y escritura en el directorio de
exportación. Caleb revisará tamaño de pantalla, escalado y proyección con ambos temas. Steven
comprobará acceso de las cuentas sintéticas y ausencia de secretos en los registros que se
mostrarán durante la defensa.

## 38
La Tabla 16 fija el contenido de distribución y el criterio para aceptar cada archivo del
paquete. Las herramientas externas conservarán su licencia y PostgreSQL no se confundirá con
una base embebida dentro del ejecutable Java. Los scripts de demostración tendrán un nombre
que identifique su finalidad y no apuntarán a una base distinta de la seleccionada para el ensayo.
El paquete app-image ZIP no ejecutará una restauración destructiva durante un arranque normal.
La recuperación requerirá modo de mantenimiento, copia previa e integridad del respaldo, con
confirmación del rol Propietario dentro del producto.
## Tabla 16
Contenido del Paquete y Verificación
ElementoContenido ConcretoAceptación Prevista
AplicaciónApp-image ZIP generada con jpackage,
Java 21 y recursos JavaFX.
Arranca en equipo sin JDK;
versión corresponde a etiqueta.
BaseMigraciones numeradas, catálogos y
semilla sintética separada.
Crea esquema desde cero;
validate acepta versión correcta.
Datos DemoD0 a D3 con fechas, identificadores y tasa
ficticia rotulados.
Recorrido produce saldos del
guion; cero dato privado.
ReportesDiez .jrxml y compilados 7.0.8, fuentes y
exportadores requeridos.
10 PDF y 9 XLSX se abren;
recibo solo PDF sin dependencias
de IDE.
ConfiguraciónEjemplo sin secretos y guía de
variables/rutas protegidas.
Ninguna llave o contraseña en
repositorio, log o captura.
RespaldoDump, adjuntos, manifiesto, hashes y
versión de esquema; paquete cifrado
AES-256-GCM con contraseña de
respaldo.
Autenticidad verificada antes de
descifrar; restauración aislada con
huellas iguales; clave o
autenticidad inválida: MSG-082
sin reemplazo.
DocumentaciónSeis documentos, manual de usuario y
técnico.
Archivos abren y coinciden con
etiqueta.
InventarioLICENSES, THIRD-PARTY-NOTICES,
dependencias, SHA-256 y README.
Todos los binarios distribuidos
tienen versión y aviso aplicable.

## 39
Día de Presentación y Alternativa
El 25 de noviembre y el 30 se congelarán copias distintas para las dos sesiones de
referencia. Treinta minutos antes de cada presentación, Aris verificará ejecutable, exportación y
servidor simulado; Alfredo revisará base y respaldo; Caleb comprobará proyección y teclado;
Steven coordinará cuentas y guion. El Apéndice D contiene la verificación con resultados
esperados y responsables, sin casillas que aparenten aprobación previa. Cada integrante tendrá
acceso a la copia de entrega y a su explicación del recorrido. La demostración comenzará con
una base conocida para evitar que un ensayo anterior cambie los saldos esperados.
Ante caída de Internet, el equipo mostrará HTTP contra S1, caché histórica y
autorización manual sintética por fecha, con rótulos claros. Si falla el equipo principal, usará la
app-image y una copia restaurada en el equipo alterno antes de reiniciar el recorrido. Si
PostgreSQL no arranca en ninguno, presentará el respaldo de evidencia técnica y declarará que la
operación en vivo no pudo ejecutarse. Una grabación o captura no se presentará como ejecución
actual. El equipo conservará la última versión consistente y evitará editar código durante los
veinte minutos de defensa propuestos.
Plan de Demostración y Revisión Académica
Las revisiones internas usarán escenarios sintéticos y responsables del equipo para
comprobar conformidad con los documentos. La Tabla 17 propone sesiones el 27 de octubre, 6,
17, 25, 26 y 30 de noviembre y 1 de diciembre, cada una con objetivo y evidencia prevista. El
Apéndice C contiene el protocolo completo, criterios y resultados esperados de diez recorridos.
La revisión no requiere cliente externo ni firmas de aceptación. El docente recibirá evidencia
académica cuando corresponda, y cualquier observación se registrará después de la sesión con su
procedencia verificable.

## 40
## Tabla 17
Sesiones Propuestas de Revisión y Demostración
FechaObjetivo y EscenarioResponsables y Evidencia Prevista
27/10Concurrencia sobre lote sintético y
contador con rollback.
Steven y Alfredo; registro H1/H4/H6.
06/11Mapeos ORM y activación contractual
## D1.
Alfredo y Caleb; consulta y rollback.
17/11Diez reportes D1/D2 y corte por
moneda.
Aris y Alfredo; archivos y oráculos.
25/11Regresión completa, dos temas y
paquete limpio.
Caleb coordina los cuatro; informe CP y
manifiesto v0.9.0-rc.1.
26/11Primera presentación de referencia con
## D2/D3.
Los cuatro; registro de observaciones
después de la sesión.
30/11Ensayo de veinte minutos y plan
alterno.
Steven facilita; tiempos por expositor y
copia alterna.
01/12Defensa y cierre de referencia.Los cuatro; evidencia RA, versión y
archivo final.
Protocolo y Registro
El facilitador fijará revisión Git, versiones, conjunto de datos y reloj antes de iniciar cada
recorrido. La persona que opera no será quien escribió el componente bajo revisión, y la persona
observadora registrará desvíos sin corregir el escenario durante su ejecución. Cada paso tendrá
un resultado esperado y una regla para distinguir fallo, bloqueo o conformidad. Al terminar, el
equipo abrirá incidencias reproducibles y repetirá únicamente los casos afectados después de
corregir. El protocolo conserva los rótulos de rol y el carácter sintético de documentos e importes
durante toda la sesión.
La conformidad visual recorrerá Claro y Oscuro con efectos activos e inactivos, sobre los
mismos datos y permisos. Las variantes de reinicio y cambio de usuario usarán dos UUID
distintos, con última preferencia local antes de autenticar y preferencia individual después. Los
formularios modificados conservarán campos y foco al cambiar la hoja de tema, incluso con H1

## 41
o H5 activos. El protocolo exigirá que un fallo de guardado local genere aviso sin impedir el
cobro. Los resultados esperados permanecen definidos aunque todavía no exista evidencia de
ejecución de la aplicación.
Manual y Material de Apoyo
El manual de usuario cubrirá acceso, roles, navegación, propiedades, inquilinos,
contratos, cobranza, recibos, garantías, mantenimiento, tasas, reportes y recuperación. Cada
sección explicará precondición, pasos y error, con captura por tema cuando cambie su lectura. El
manual técnico incluirá construcción, configuración externa, esquema, migraciones, respaldo,
restauración y diagnóstico de trabajadores. Ambos conservarán versión y fecha de la etiqueta que
describen. La Tabla 18 fija estructura y revisión para que otro integrante repita el recorrido sin
instrucciones orales del autor ni acceso al entorno de desarrollo.
## Tabla 18
Estructura y Conformidad del Manual
ManualContenidoCriterio de Revisión
UsuarioAcceso, roles, Claro/Oscuro, navegación y
preferencias locales.
Otro integrante inicia sesión y
cambia tema sin explicación oral.
UsuarioCatálogos, contrato, renta, pago parcial,
recibo y anulación.
Reproduce D2 y saldo 60 USD en
cargo octubre.
UsuarioGarantía, mantenimiento, tasas y reportes.Identifica autorización por fecha y
separación de renta/depósito.
UsuarioMensajes, cancelación, cierre y respaldo.Reconoce bloqueo de H6 y no
fuerza pérdida de transacción.
TécnicoConstrucción, versiones, estructura,
dependencias y licencias.
Segundo equipo compila la etiqueta
con Wrapper.
TécnicoMigraciones, configuración externa,
respaldo y recuperación.
Copia aislada se restaura con
hashes iguales.
TécnicoHilos, registros, errores, pruebas y
diagnóstico de HTTP.
Repite CP con S1 y localiza
incidente por correlación.

## 42
Plan de Presentación y Defensa
Distribución del Tiempo
La defensa tendrá veinte minutos como duración propuesta para organizar el ensayo, sin
atribuir ese límite al docente. Cada integrante dispone de cinco minutos con una transición
definida y un resultado de aprendizaje principal. La Tabla 19 resume los bloques y el Apéndice E
desarrolla el guion con acciones, frases de enlace y alternativas ante fallos. El recorrido iniciará
con una base sintética conocida y terminará con evidencia de cierre controlado. Las preguntas
posteriores se tratarán fuera de esos veinte minutos salvo que la dinámica académica de la sesión
indique otra distribución.
## Tabla 19
Distribución Propuesta de Veinte Minutos
TiempoResponsableAcción y Evidencia RA
00:00 a 05:00Caleb TardencillaAcceso, FXML, controles, eventos, dos temas y
teclado. RA1. Entrega contrato C1 a Alfredo.
05:00 a 10:00Alfredo MontalvanJDBC parametrizado, CRUD ORM, contrato y
consulta de saldo. RA2 e IL4.1. Entrega período a
## Steven.
10:00 a 15:00Steven CuadraH1, mora, cobro parcial, H4 y explicación de H6 con
evidencia preparada. RA3. Entrega recibo confirmado
a Aris.
15:00 a 20:00Aris GavarreteRecibo y cartera, PDF/XLSX, solicitud JSON, fallo
simulado y caché. RA4 y RA5. Cierra con versión y
evidencia.
Evidencias y Transiciones
Caleb iniciará el acceso, mostrará ambos temas y registrará los controles y eventos que
evidencian RA1. Alfredo recibirá el contrato seleccionado y explicará JDBC, Hibernate y la
unidad transaccional antes de consultar saldo. Steven generará cargos, ejecutará un pago parcial
y mostrará sincronización y cierre seguro con una intercalación preparada. Aris continuará con

## 43
recibo, cartera, JSON y manejo de caída del servicio. La transición conservará el mismo contrato
y período para reducir cambios de contexto y permitir que la audiencia compare saldo, aplicación
y reporte con el mismo conjunto sintético.
La persona de respaldo conocerá los accesos rápidos y la posición del guion para
continuar si falta el expositor principal. El ensayo del 30 medirá duración por bloque y reservará
los últimos segundos de cada uno para la transición. Una explicación extensa de arquitectura no
desplazará la ejecución observable de HTTP, ORM, reportes o trabajadores. La evidencia
adjunta mostrará revisión y entorno aunque una captura no pueda abrirse durante la exposición.
La Tabla 20 anticipa preguntas técnicas con respuestas limitadas a decisiones documentadas y a
las pruebas que el equipo deberá ejecutar antes de afirmar resultados.
## Tabla 20
Preguntas Técnicas Probables y Respuestas
PreguntaRespuesta Técnica Prevista
¿Por qué JDBC y Hibernate?JDBC evidencia SQL parametrizado en seguridad y agregados;
ORM administra negocio. El servicio comparte Connection
durante auditoría financiera.
¿Cómo evitan saltos en
recibos?
Contador por fila con FOR UPDATE dentro de
pago/recibo/bitácora; rollback revierte incremento; anulación
conserva número.
¿Basta synchronized o
ReentrantLock?
Protege un proceso; PostgreSQL protege entre JVM. La prueba
usa dos clientes independientes.
¿Qué ocurre si cae la red
después de commit?
Consultar UUID y huella de solicitud antes de reintentar;
recuperar recibo confirmado sin emitir otro.
¿La tasa consultada es
oficial?
Es informativa del proveedor; cobro exige autorización por
fecha. El comunicado BCN es una fuente distinta.
¿Cómo detienen los hilos?Cancelar entre unidades de trabajo, esperar fuera de JavaFX y
mantener proceso abierto con aviso si excede 10 s.
¿Por qué no mezclar
monedas?
Totales separados NIO/USD; conversión conserva original, tasa
y resultado redondeado.

## 44
PreguntaRespuesta Técnica Prevista
¿El estilo agrega
dependencias web?
Usa FXML, CSS JavaFX y luces estáticas nativas; no necesita
WebView ni un hilo ambiental.
¿Cómo comprueban
reportes?
Mismo filtro y corte en SQL oráculo y DTO; tolerancia de
importes 0.00; XLSX con números.
¿Qué evidencia tienen de
usuarios reales?
La modalidad es documental y sintética. La revisión interna no
equivale a estudio de campo o validación con propietarios.
¿Las versiones ya funcionan
juntas?
Su publicación y requisitos se verificaron documentalmente; solo
un ensayo sobre la etiqueta acredita funcionamiento integrado.
¿Quién aprobó calendario y
veinte minutos?
Son supuestos del encargo y propuesta de ensayo; no se recibió
una confirmación institucional ni desglose de rúbrica.
Gestión de Riesgos
La Tabla 21 contiene dieciocho riesgos con probabilidad, impacto, disparador, mitigación
y contingencia. La probabilidad e impacto usan escala de uno a cinco como estimación del
equipo, con producto de quince o más en rojo, ocho a catorce en ámbar y hasta siete en verde.
Esos números ordenan seguimiento y no representan frecuencias observadas. Steven revisará el
registro en cada planificación y al aparecer un disparador. El dueño del riesgo comunicará el
efecto sobre una TAR, capacidad restante y evidencia RA, y activará la contingencia dentro de la
reserva disponible del período.
## Tabla 21
Registro de Riesgos y Respuesta Propuesta
Riesgo y DueñoP × I y DisparadorMitigaciónContingencia
## RSK-01
PostgreSQL no
disponible
## Alfredo
## 3 × 5 = 15
Servicio no inicia o
puerto inaccesible al
ensayo.
Instalación temprana y
restauración en
segundo equipo.
Usar equipo alterno; si
falla, declarar caída y
mostrar evidencia
identificada.
## RSK-02
Dialectos y mapeos
## Hibernate
## Alfredo
## 3 × 5 = 15
validate falla o
consulta usa dialecto
incorrecto.
PostgreSQLDialect
por metadatos,
pruebas en motor real.
Congelar nuevas
entidades; corregir
mapeo con reserva y
repetir integración.

## 45
Riesgo y DueñoP × I y DisparadorMitigaciónContingencia
## RSK-03
## Compatibilidad
JasperReports
## Aris
## 3 × 4 = 12
Clase ausente o
plantilla no compila
en 7.0.8.
Studio/Library
iguales; DTO; excluir
extensión ORM.
Simplificar plantilla sin
retirar contenido;
reconstruir y
reconciliar.
## RSK-04
Caída o límite HTTP
## Aris
## 4 × 3 = 12
5xx, timeout, 429 o
quota-reached.
Caché, presupuesto
100/mes y pruebas S1.
## Demostrar S1
identificado;
autorización manual
por fecha y cobro local.
## RSK-05
Falta de tiempo
## Steven
## 4 × 5 = 20
Desvío >20 % o
reserva menor al
riesgo pendiente.
Límite individual y
revisión diaria.
Aplicar Tabla 22 y
conservar finanzas, RA
y ambos temas.
## RSK-06
Pérdida de código
## Aris
## 2 × 5 = 10
Falla de disco o copia
remota inaccesible.
Push diario y copia
con historial fuera del
equipo.
Restaurar última
etiqueta y registrar
trabajo perdido; no
sobrescribir otra copia.
## RSK-07
Ausencia de
integrante
## Steven
## 3 × 4 = 12
Dos jornadas de
trabajo sin
disponibilidad.
Relevo y explicación
compartida por
componente.
Repartir solo horas
libres y aplazar
Should/Could; ensayar
con tres voces.
## RSK-08
Cambio de alcance
académico
## Steven
## 3 × 4 = 12
Observación exige
función no estimada.
Vincular solicitud con
RA/requisito y estimar
antes.
Priorizar corrección
Must; posponer
ampliación que exceda
capacidad.
## RSK-09
## Conflictos Git
## Caleb
## 3 × 3 = 9
Ramas editan mismo
FXML o migración.
Ramas cortas y
propietario temporal
del archivo.
Resolver con
autor/revisor;
reconstruir y repetir
recorrido afectado.
## RSK-10
## Numeración
inconsistente
## Alfredo
## 3 × 5 = 15
Duplicado, salto por
rollback o doble pago.
Fila contador e
idempotencia; dos
JVM en pruebas.
Bloquear liberación
financiera y corregir
con reserva prioritaria.
## RSK-11
UI congelada o cierre
incompleto
## Steven
## 3 × 4 = 12
Pulso >200 ms o
trabajadores no
terminan.
I/O fuera de JavaFX,
puntos de cancelación
y H6.
Desactivar efectos;
aislar tarea; mantener
proceso abierto si
commit activo.
## RSK-12
Pérdida de
preferencias
## Caleb
## 3 × 3 = 9
Almacén local ilegible
o sin escritura.
Puerto de preferencias
y tests de
error/reinicio.
Claro ante lectura
fallida; conservar tema
de sesión y aviso al
guardar.

## 46
Riesgo y DueñoP × I y DisparadorMitigaciónContingencia
## RSK-13
Confusión de tasa
comercial
## Aris
## 3 × 5 = 15
Pago usa caché
histórica o rótulo
oficial incorrecto.
Separar referencia y
autorización por
fecha.
Bloquear conversión
sin autorización y
corregir
rótulos/registro.
## RSK-14
Acceso indebido o
secreto expuesto
## Steven
## 2 × 5 = 10
Asistente invoca
operación reservada o
log muestra llave.
Guardas de servicio,
parámetros y
redacción.
Revocar llave expuesta;
corregir permiso;
repetir pruebas antes de
v0.9.0-rc.1.
## RSK-15
Copia inconsistente
de adjuntos
## Alfredo
## 3 × 4 = 12
Hash faltante o dump
no coincide con
archivos.
Puerta exclusiva y
manifiesto con
## SHA-256.
Restaurar copia anterior
verificada; no usar
respaldo sin integridad.
## RSK-16
## Legibilidad Aurora
## Caleb
## 3 × 3 = 9
Foco invisible, popup
mixto o importe sobre
luz.
Tokens únicos y
matriz de cuatro
combinaciones.
Retirar luces, conservar
ambos temas y corregir
superficie uniforme.
## RSK-17
Paquete o proyector
incompatible
## Aris
## 3 × 4 = 12
Falla en equipo
limpio, extracción ZIP
o runtime o escala 150
## %.
App-image ZIP
temprana y ensayo de
extracción, arranque y
proyección.
Usar copia ZIP
verificada en equipo
alterno; no reconstruir
durante exposición.
## RSK-18
## Evidencia
insuficiente para RA
## Steven
## 2 × 5 = 10
Guion omite SQL,
ORM, HTTP o ciclo
de hilos.
Matriz de quince IL y
ensayo cronometrado.
Sustituir explicación
redundante por acción
verificable del IL
faltante.
Nota. P e I son estimaciones de 1 a 5. Rojo >=15; ámbar de 8 a 14; verde <=7. No son
probabilidades estadísticas.
Un riesgo materializado se convierte en una incidencia con responsable y fecha de
tratamiento, sin desaparecer del registro de exposición. La revisión distinguirá la falla técnica de
su consecuencia sobre el calendario para evitar contar dos veces las mismas horas. El equipo
conservará la capacidad individual y aplicará recorte de alcance cuando la reserva no cubra una
corrección crítica. El estado rojo exige una acción antes del siguiente hito, mientras ámbar
permite continuar con vigilancia del disparador. La etiqueta del candidato no cambiará a
aprobada por la mera ausencia de un incidente registrado.

## 47
Plan de Contingencia de Alcance
El orden de recorte de la Tabla 22 conserva requisitos Must y evidencia necesaria de RA1
a RA5. Primero se aplazarán fotografía y logotipo opcionales; después adjuntos, responsable de
mantenimiento y navegación secundaria clasificados Should. La simplificación visual reducirá
luces y efectos dentro del mismo contrato de dos temas, sin retirar Oscuro ni el selector
persistente. La versión local seguirá siendo el destino principal, mientras el ensayo LAN mínimo
mantendrá la comprobación de dos clientes sobre numeración. La contingencia no convierte un
requisito incumplido en opcional ni atribuye una aprobación académica inexistente.
## Tabla 22
Orden de Recorte y Evidencia Conservada
Orden y
## Disparador
Recorte o SimplificaciónEfecto Académico
1; reserva
insuficiente
Aplazar foto RF-M02-05 y logotipo RF-
M12-03, ambos Could.
RA1 mantiene ImageView con
recurso de demostración no
decorativo; recursos ausentes
controlados.
2; trabajo
opcional
bloquea Must
Aplazar Should de adjuntos y
emergencia; conservar datos mínimos
de contacto.
CRUD SQL/ORM permanece; no
cambia exactitud financiera.
3; atrasos de
interfaz
Simplificar previsualización avanzada,
navegación desde indicador y
asignación de mantenimiento.
Tablas, filtros, menús y diálogos
básicos permanecen; diez reportes
conservados.
4; costo visual
alto
Reducir luces a composición estática
simple o usar sin efectos.
Claro/Oscuro, selector,
persistencia, teclado y contraste se
conservan.
5; integración
LAN tardía
Presentar instalación local; mantener
ensayo técnico mínimo de dos procesos
sobre contador.
RA3 evidencia sincronización
persistente; no prometer despliegue
LAN no ensayado.
6; proveedor
inaccesible
Usar S1 y caché rotulados, con
autorización manual de demostración
por fecha.
RA5 conserva HTTP y JSON
reales contra simulador; no acredita
disponibilidad comercial.

## 48
Orden y
## Disparador
Recorte o SimplificaciónEfecto Académico
## Protección
obligatoria
No recortar JDBC, ORM, H1/H4/H6,
HTTP, reportes, permisos, auditoría ni
invariantes.
Se conservan RA1 a RA5; un Must
fallido se declara incumplimiento.
Si una ampliación consume la reserva, Steven propondrá aplazarla antes de desplazar
pruebas de consistencia. Una falla de reportes se resolverá con una plantilla sencilla de iguales
datos, y una caída HTTP mediante servidor simulado identificado y operación local. Los diez
reportes y el contrato HTTP conservarán sus criterios de aceptación. Cuando una función Must
siga fallando, el paquete declarará la limitación y mantendrá evidencia de la última versión
consistente. La presentación no calificará un video, fixture o esqueleto como sustituto de toda la
aplicación implementada.
Recursos, Costos y Licencias
La Tabla 23 conserva las versiones centrales compatibles del TRD y separa licencia,
precio aplicable y gasto comercial opcional. PostgreSQL 18.6 pertenece a una rama soportada
hasta noviembre de 2030, y pgJDBC 42.7.14 admite Java 8 o posterior (PGDG, s. f.-e;
PostgreSQL JDBC Driver, s. f.-a). Hibernate enumera 7.4.6.Final y compatibilidad de la serie
con Java 21, por lo que la versión del resumen existe (Hibernate, s. f.-a). El plan mantiene esa
versión para coherencia documental aunque la página publique revisiones posteriores. La
aceptación del conjunto seguirá requiriendo compilación y ejecución integrada.

## 49
## Tabla 23
Herramientas Fijadas y Condiciones Aplicables
Herramienta y
## Edición
Versión y LicenciaCosto y CondiciónFuente
## Consultada
## Eclipse Temurin;
## JDK
21.0.12.1+1 x64; GPLv2
con excepciones del
## JDK.
Sin cargo de licencia;
soporte comercial
opcional fuera del plan.
## Eclipse Adoptium
(s. f.); Free
## Software
## Foundation (1991)
y Oracle (s. f.-b).
OpenJFX SDK21.0.13; GPL con
## Classpath Exception.
Descarga sin cargo;
contrato LTS comercial
no contratado.
Gluon (s. f.-a);
OpenJFX (s. f.-b).
Scene Builder21.0.0 archivado; BSD.Sin cargo; FXML
restringido a controles de
JavaFX 21.
Gluon (s. f.-b, s.
f.-c).
PostgreSQL18.6; PostgreSQL
## License.
Sin cargo de licencia;
instalación y soporte
requieren tiempo propio.
PGDG (s. f.-b, s.
f.-e).
pgJDBC42.7.14; BSD-2-Clause.Sin cargo; conservar
avisos en distribución.
PostgreSQL
JDBC Driver (s.
f.-a, s. f.-b).
Hibernate ORM7.4.6.Final; Apache 2.0;
## Jakarta Persistence 3.2.
Sin cargo; usar Java 21 y
no mezclar
javax.persistence.
Hibernate (s. f.-a).
JasperReports
## Library
7.0.8; LGPLv3.Sin cargo de licencia;
conservar avisos y
obligaciones aplicables.
## Jaspersoft (2026,
s. f.).
Jaspersoft Studio CE7.0.8; licencia de tipo
BSD del proveedor.
Descarga comunitaria; no
presumir licencia de
servidor comercial.
## Jaspersoft (2026).
Gson2.13.2; Apache 2.0.Sin cargo; cliente JSON
propio separado de
Jackson transitivo.
Google (s. f.-a, s.
f.-b).
IntelliJ IDEA
## Community
2025.2; Apache 2.0 en
código Community.
Sin licencia Ultimate
requerida.
JetBrains (s. f.-a,
s. f.-b).
Apache Maven3.9.16; Apache 2.0.Sin cargo; Wrapper y
versiones explícitas.
## Apache Software
## Foundation (2004,
s. f.).

## 50
Herramienta y
## Edición
Versión y LicenciaCosto y CondiciónFuente
## Consultada
Git / GitHub FreeGit 2.51.0.windows.1
del TRD; GPLv2 /
servicio sujeto a plan.
Repositorio público
sintético; protección
según disponibilidad
documentada.
Cuadra et al.
(2026d); GitHub
(s. f.).
HttpClient y
jpackage
JDK 21; licencia de la
distribución Java.
Sin SDK HTTP adicional;
generar app-image en
Windows y distribuir ZIP.
Oracle (s. f.-a, s.
f.-c).
JUnit / herramientas
auxiliares
JUnit 5.12.2, HikariCP
6.3.0 y versiones
auxiliares fijadas en
## TRD.
Conservar inventario
transitivo y avisos al
resolver construcción.
Cuadra et al.
## (2026d),
inventario técnico
consultado.
ExchangeRate-API
## Free
Servicio v6; términos de
AYR Tech.
USD 0/mes; 1500
solicitudes/mes; diario;
sin garantía de
continuidad.
AYR Tech (Pty)
Ltd. (s. f.-a, s. f.-
b, s. f.-c, s. f.-d, s.
f.-e).
Nota. Licencias de terceros no se sustituyen por las del código Latch. Las herramientas auxiliares
conservadas del TRD no se presentan como ejecutadas en conjunto.
Condiciones de Distribución
PostgreSQL permite uso y distribución bajo PostgreSQL License, y pgJDBC usa BSD de
dos cláusulas con avisos (PGDG, s. f.-b; PostgreSQL JDBC Driver, s. f.-b). JasperReports
Library utiliza LGPLv3; Studio 7.0.8 usa licencia de tipo BSD sin código fuente desde la
generación indicada (Jaspersoft, 2026). Aris conservará licencias y avisos, con inventario de
dependencias distribuidas. La licencia del motor no equivale a contratar alojamiento, soporte o
servicios comerciales. El plan no presupone que una licencia académica de un producto
comercial esté concedida al equipo.
Gluon publica JavaFX 21.0.13 con JDK mínimo 17, y Adoptium identifica Temurin
21.0.12.1+1 del TRD (Gluon, s. f.-a; Eclipse Adoptium, s. f.). OpenJFX declara GPL con
Classpath Exception y OpenJDK publica su licencia y excepciones (OpenJFX, s. f.-b; Free
Software Foundation, 1991; Oracle, s. f.-b). Scene Builder 21.0.0 conserva su lanzamiento

## 51
archivado (Gluon, s. f.-b). Maven usa Apache 2.0 y Community de IntelliJ mantiene su licencia
(Apache Software Foundation, 2004; JetBrains, s. f.-a). Las herramientas de desarrollo quedan
fuera del paquete app-image ZIP del usuario.
Presupuesto del Equipo
La estimación programa 57 horas por persona y 228 horas del equipo, más una capacidad
reservada conjunta de 53,142857 horas. El presupuesto adopta USD 0 de desembolso
incremental por software seleccionado y servicio Free, suponiendo equipos, electricidad y
conectividad ya disponibles. Esa cifra es un supuesto de planificación y no una cotización del
costo total del proyecto. El trabajo estudiantil no tiene una tarifa pactada, por lo que su costo
monetario no se calcula. La Tabla 24 separa los recursos disponibles por supuesto y los servicios
opcionales que el equipo no contrata mediante este plan.
## Tabla 24
Presupuesto Propuesto y Recursos Disponibles
RecursoValor AdoptadoNaturaleza y Condición
Trabajo programado57 h por persona; 228 h
equipo.
Estimación optimista de ejecución
sobre componentes reutilizables; no son
horas trabajadas.
Capacidad total70.285714 h por persona;
281.142857 h equipo.
Supuesto 12 h/semana, ajustado por 41
días.
Reserva53.142857 h: 27 coordinación
y 26.142857 incertidumbre.
No representa contratación de tiempo
adicional.
Software y API
elegidos
USD 0 de desembolso
incremental previsto.
Free/comunitario; sujeto a términos
consultados y uso dentro de cuotas.
Equipos, SSD, LAN
y proyector
Disponibles por supuesto; sin
compra presupuestada.
Ensayos confirman disponibilidad; su
costo histórico no se inventa.
Conectividad y
electricidad
Recursos existentes; gasto
incremental no estimado.
No equivale a costo total cero.
Hosting, soporte y
planes comerciales
No contratados; fuera del
presupuesto.
Requieren decisión posterior si el
alcance los necesita.

## 52
RecursoValor AdoptadoNaturaleza y Condición
Trabajo estudiantil
monetizado
Sin tarifa asignada.No se inventa salario ni valor comercial
del proyecto.
Comunicación y Seguimiento
GitHub Issues y Projects conservarán tareas, defectos, decisiones y enlaces a evidencia
para que los cuatro integrantes consulten el mismo estado. Un canal de mensajería del equipo se
usará para avisos de bloqueo, pero cada decisión que cambie alcance o versión deberá copiarse al
registro correspondiente. El plan propone un seguimiento asíncrono diario y una revisión de
capacidad al final del período. Steven preparará el reporte con datos de horas registradas y tareas
terminadas, sin asignar avance a trabajo no revisado. La Tabla 25 fija frecuencia, responsable,
contenido y acción requerida ante desviaciones.
## Tabla 25
Canales e Indicadores de Seguimiento
Canal y FrecuenciaResponsableContenido y Acción
Projects; actualización
diaria
Cada responsableTAR, horas, bloqueo y siguiente paso; no marcar
terminado sin evidencia.
Mensaje asíncrono; día
de actividad
Los cuatroMáximo dos minutos por persona; bloqueo de un
día escala a Steven.
Issues; al detectar
defecto
Operador y CalebDEF con pasos/entorno; crítico o alto bloquea
candidato.
Reporte de sprint; al
cierre
StevenMust aceptados por módulo; horas
registradas/estimadas; reserva y riesgo mayor.
Pull request; antes de
integrar
Autor y revisor
cruzado
Requisito, comportamiento y verificaciones;
respuesta a observaciones.
Registro de decisión; al
cambiar diseño
Steven con dueño
técnico
Alternativas, motivo, impacto y evidencia; sin
acuerdos inventados.
Paquete de hito; cada
etiqueta
Aris y AlfredoHashes, copia de datos, documentos y apertura en
equipo distinto.
El avance por módulo se calculará como requisitos Must aceptados dividido entre
requisitos Must comprometidos para ese incremento. Las horas reales frente a estimadas se

## 53
informarán por TAR y sprint, con variación porcentual únicamente cuando exista tiempo
registrado. El reporte añadirá defectos críticos y altos abiertos, casos bloqueados, reserva restante
y semáforo del riesgo mayor. Una tarea que exceda veinte por ciento de su estimación o un
bloqueo de un día activará revisión de carga. El indicador no justificará aumentar disponibilidad;
deberá originar corrección, reasignación dentro de capacidad o recorte del alcance opcional.
Criterios de Aceptación Final
La aceptación propuesta exige requisitos Must operativos, evidencia de RA1 a RA5 y
archivos íntegros que puedan abrirse desde una copia de entrega. La Tabla 26 funciona como
lista de verificación con umbrales y evidencia previstos, sin ponderaciones inventadas de la
rúbrica. El resumen académico menciona cuarenta puntos, pero no suministra su desglose ni
criterios detallados. El equipo usará por ello criterios internos para decidir si etiqueta la versión,
manteniendo al docente como evaluador de su propia rúbrica. La finalización documental de este
plan no implica que esa aceptación técnica ya haya ocurrido.
## Tabla 26
Lista de Aceptación Final Propuesta
CriterioUmbral VerificableEvidencia Prevista
RA1; interfaz40 pantallas según inventario; 2 temas;
teclado y estados sin bloqueo.
CP-077 a CP-094, capturas y
recorrido.
RA2; SQLConexión pgJDBC y CRUD JDBC
parametrizado, validación y consistencia.
Código y CP-007, CP-040.
RA3; trabajadoresH1 a H6; contador sin duplicados/saltos
por rollback; cierre controlado.
CP-041 a CP-052; dos JVM.
RA4; ORMCRUD Hibernate y commit financiero
compartido con JDBC.
Mapeos, pruebas de
repositorios y rollback.
RA4; reportesDiez contenidos, PDF y XLSX tabular;
tolerancia 0.00 frente a SQL.
CP-067 a CP-076; archivos
abiertos.
RA5; servicioHTTP/JSON, errores, cuota y caché, sin
secreto; origen visible.
CP-053 a CP-066 y registro
## S1.

## 54
CriterioUmbral VerificableEvidencia Prevista
Finanzas y rolesCero corrupción, duplicados, saldo
negativo o operación reservada aceptada.
Consultas de conservación y
pruebas negativas.
RendimientoPanel p95 <=2 s; H1 500 <=60 s; XLSX
10000 <=30 s; UI <=200 ms.
CP-098 a CP-100; condiciones
y tiempos individuales.
Preferencias30 cambios por sentido <=500 ms; 20
reinicios por cuentas sin desvíos.
CP-087, CP-089 a CP-091.
Calidad100 casos y variantes aplicables
ejecutados; cero crítico/alto Must abierto.
Informe con fallos y bloqueos
visibles.
EntregaInstalación limpia, respaldo restaurado,
seis documentos y manuales accesibles.
CP-095 a CP-097; manifiesto
y hashes.
La condición financiera exige cero recibos duplicados, ningún salto provocado por una
reversión y conciliación exacta de pagos, aplicaciones, crédito y garantía. La condición de
presentación exige los dos temas en las cuarenta pantallas y recorridos por teclado sin pérdida de
campos o permisos. Los reportes deberán abrirse, respetar el corte y concordar con SQL,
mientras HTTP deberá explicar origen y antigüedad de cada referencia. El cierre deberá detener
recursos o comunicar el bloqueo sin abandonar un commit incierto. El revisor conservará
evidencia de cada criterio y registrará cualquier incumplimiento con su requisito asociado.
Matriz de Cumplimiento del Syllabus
La Tabla 27 vincula los cinco RA y sus quince indicadores con componentes y evidencia
de Latch. Su denominación conserva el encabezado solicitado, pero la fuente académica es el
resumen aportado en el encargo, no un syllabus original consultado. Los vínculos RF, RNF, TAR
y CP permiten seguir la evidencia desde la tarea hasta la demostración prevista. La matriz no
asigna una nota ni asegura acreditación del resultado antes de la evaluación. Los Apéndices A a
E desarrollan ejecución y revisión, y el Apéndice F documenta las fuentes consultadas para
decisiones externas.

## 55
## Tabla 27
RA e IL Frente a Componentes y Evidencia
Resultado e
## Indicador
Componente y TareaEvidencia a Mostrar
RA1; IL1.1FXML: Label, Button, TextField,
PasswordField, ComboBox, ListView,
CheckBox, RadioButton, TableView,
DatePicker e ImageView. TAR-007,
## TAR-019.
PAN y formularios con
controles adecuados; CP-077,
## CP-092.
RA1; IL1.2Eventos de guardar, validar, filtrar, cambiar
tema y cancelar. TAR-011, TAR-035,
## TAR-039.
## CP-079, CP-087, CP-088;
acción y resultado sin perder
campos.
RA1; IL1.3MenuBar, ToolBar, ContextMenu, Alert,
Dialog, FileChooser, DirectoryChooser.
## TAR-007, TAR-055.
Menú, confirmación,
exportación y destino de
respaldo; CP-080, CP-092.
RA2; IL2.1DataSource y pgJDBC; TAR-006.Conexión en base aislada y
error controlado.
RA2; IL2.2CRUD JDBC de usuarios y parámetros con
PreparedStatement; TAR-006, TAR-009,
## TAR-055.
SQL visible y operación
permitida; CP-001, CP-005,
## CP-007.
RA2; IL2.3FK, UNIQUE, CHECK, validación y
rollback; TAR-010, TAR-038.
## CP-009, CP-015, CP-042;
ningún registro parcial.
RA3; IL3.1H1/H2/H3/H5 fuera de JavaFX; TAR-005,
## TAR-037, TAR-065.
Progreso, consulta y
navegación; CP-047, CP-055,
## CP-099.
RA3; IL3.2H4 y bloqueo persistente; TAR-038,
## TAR-045.
Dos JVM y números
contiguos; CP-041, CP-048,
## CP-052.
RA3; IL3.3H6 y cancelación; TAR-057.CP-049 y CP-050; recursos
cerrados o aviso tras 10 s.
RA4; IL4.1Entidades y repositorios Hibernate;
## TAR-018, TAR-022, TAR-034.
CRUD de negocio, @Version
y rollback; CP-008, CP-017,
## CP-052.
RA4; IL4.2Diez plantillas JasperReports; TAR-052,
## TAR-056, TAR-060.
Fuentes, DTO, corte y totales
SQL; CP-067 a CP-076.
RA4; IL4.3Visor/exportación integrada H5; TAR-060.PDF/XLSX guardados y
abiertos; CP-086, CP-100.

## 56
Resultado e
## Indicador
Componente y TareaEvidencia a Mostrar
RA5; IL5.1HttpClient GET Bearer; TAR-068.Petición a S1 con cabeceras
redactadas; CP-053.
RA5; IL5.2Gson y validación; TAR-068, TAR-069.Éxito/error JSON, NIO y
fechas; CP-056 a CP-064.
RA5; IL5.3M09/M11: origen, antigüedad y aviso;
## TAR-067.
CP-066, CP-088; UI operable
con caché o falla.
Nota. RA e IL proceden del resumen académico aportado. Los criterios no constituyen un
desglose de la rúbrica institucional.

## 57
## Referencias
Apache Software Foundation (s. f.). Downloading Apache Maven and Maven Daemon.
https://maven.apache.org/download.cgi
Apache Software Foundation (2004). Apache license, version 2.0.
https://www.apache.org/licenses/LICENSE-2.0
AYR Tech (Pty) Ltd. (s. f.-a). API authentication notes.
https://www.exchangerate-api.com/docs/authentication
AYR Tech (Pty) Ltd. (s. f.-b). Exchange rate API. https://www.exchangerate-api.com/
AYR Tech (Pty) Ltd. (s. f.-c). Standard requests.
https://www.exchangerate-api.com/docs/standard-requests
AYR Tech (Pty) Ltd. (s. f.-d). Supported currency codes and countries.
https://www.exchangerate-api.com/docs/supported-currencies
AYR Tech (Pty) Ltd. (s. f.-e). Terms of service. https://www.exchangerate-api.com/terms
Balzarini, J., y Boyd, M. L. (2021). Working with them: Small-scale landlord strategies for
avoiding evictions. Housing Policy Debate, 31(3–5), 425–445.
https://doi.org/10.1080/10511482.2020.1800779
Banco Central de Nicaragua (20 de octubre de 2025). BCN informa que la tasa de deslizamiento
del tipo de cambio del córdoba respecto al dólar de los Estados Unidos de América
continuará en cero por ciento (0%) anual en el año 2026.
https://www.bcn.gob.ni/divulgacion-prensa/bcn-informa-que-la-tasa-de-deslizamiento-
del-tipo-de-cambio-del-cordoba-respecto

## 58
Cook, T. M., Rongerude, J., Mueller, E. y Kuhlmann, D. (2024). Providing rental housing: A
systematic literature review of residential rental property owner decision making. Journal
of Planning Literature, 39(4), 535–547. https://doi.org/10.1177/08854122241239571
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026a). Brief de diseño de interfaz y
experiencia de usuario para Latch [Documento académico no publicado]. Universidad
Americana (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026b). Documento de flujo de la
aplicación de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026c). Documento de requisitos del
producto de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026d). Documento de requisitos
técnicos de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026e). Esquema de base de datos y
servicios de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Eclipse Adoptium (s. f.). Temurin support. https://adoptium.net/support
Free Software Foundation (1991). The GNU general public license version 2.
https://raw.githubusercontent.com/openjdk/jdk21u/master/LICENSE

## 59
GitHub (s. f.). About protected branches. https://docs.github.com/en/repositories/configuring-
branches-and-merges-in-your-repository/managing-protected-branches/about-protected-
branches
Gluon (s. f.-a). JavaFX. https://gluonhq.com/products/javafx/
Gluon (s. f.-b). Release 21.0.0. https://github.com/gluonhq/scenebuilder/releases/tag/21.0.0
Gluon (s. f.-c). Scene Builder. https://gluonhq.com/products/scene-builder/
Google (s. f.-a). Gson 2.13.2. https://github.com/google/gson/releases/tag/gson-parent-2.13.2
Google (s. f.-b). License.
https://raw.githubusercontent.com/google/gson/gson-parent-2.13.2/LICENSE
Hibernate (s. f.-a). 7.4 series. https://hibernate.org/orm/releases/7.4/
Hibernate (s. f.-b). Hibernate ORM user guide.
https://docs.hibernate.org/orm/7.4/userguide/html_single/
Jaspersoft (s. f.). JasperReports Library artifacts POM 7.0.8.
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/pom-parent.xml
Jaspersoft (2026). Jaspersoft community edition. https://community.jaspersoft.com/download-
jaspersoft/community-edition/
JetBrains (s. f.-a). IntelliJ IDEA Community Edition license. https://github.com/JetBrains/intellij-
community/blob/master/LICENSE.txt
JetBrains (s. f.-b). What’s new in IntelliJ IDEA 2025.2.
https://www.jetbrains.com/idea/whatsnew/2025-2/
Malewicz, M. (s. f.). Aurora UI: New visual trend for 2021.
https://hype4.academy/articles/design/aurora-ui-new-visual-trend-for-2021

## 60
OpenJFX (s. f.-a). GaussianBlur.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/effect/GaussianBlur.html
OpenJFX (s. f.-b). JavaFX. https://openjfx.io/
OpenJFX (s. f.-c). JavaFX CSS reference guide.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html
OpenJFX (s. f.-d). RadialGradient.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/paint/RadialGradient.html
Oracle (s. f.-a). HttpClient.
https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/
HttpClient.html
Oracle (s. f.-b). OpenJDK assembly exception.
https://raw.githubusercontent.com/openjdk/jdk21u/master/ASSEMBLY_EXCEPTION
Oracle (s. f.-c). Packaging overview.
https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html
PostgreSQL JDBC Driver (s. f.-a). Download. https://jdbc.postgresql.org/download/
PostgreSQL JDBC Driver (s. f.-b). License. https://jdbc.postgresql.org/license/
Schwaber, K. y Sutherland, J. (2020). The Scrum guide. https://scrumguides.org/scrum-
guide.html
The PostgreSQL Global Development Group (s. f.-a). Explicit locking.
https://www.postgresql.org/docs/18/explicit-locking.html
The PostgreSQL Global Development Group (s. f.-b). License.
https://www.postgresql.org/about/licence/

## 61
The PostgreSQL Global Development Group (s. f.-c). pg_dump.
https://www.postgresql.org/docs/18/app-pgdump.html
The PostgreSQL Global Development Group (s. f.-d). Sequence manipulation functions.
https://www.postgresql.org/docs/18/functions-sequence.html
The PostgreSQL Global Development Group (s. f.-e). Versioning policy.
https://www.postgresql.org/support/versioning/
World Wide Web Consortium (12 de diciembre de 2024). Web content accessibility guidelines
(WCAG) 2.2. https://www.w3.org/TR/2024/REC-WCAG22-20241212/

## 62
## Apéndice A
## Backlog Completo
El backlog contiene 96 tareas propuestas y 228 horas programadas, con trazabilidad principal a requisitos del PRD (Cuadra et
al., 2026c) o, en tareas de gestión, a las decisiones D44, D45 y D47. Las fechas indican ventanas de ejecución y las precedencias
dentro de una misma ventana se resolverán terminando primero la tarea antecedente. Cada integrante tiene la carga individual de la
Tabla 8 y usará la reserva solo con registro de causa y horas. Las Tablas A1 a A6 separan los seis períodos para facilitar el
seguimiento de dependencias. Las asignaciones no acreditan disponibilidad confirmada, trabajo realizado ni acuerdos celebrados por
el equipo.
La Tabla A1 detalla las tareas de Sprint 0, del 22/10 al 27/10 de 2026.
## Tabla A1
Tareas de Sprint 0
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-001Consolidar alcance, RN y
trazabilidad de los cinco
documentos
## M12D44; D45;
## D47
Steven Cuadra2Sin
antecedente
22/10 a
## 23/10
## Must
TAR-002Crear migraciones
PostgreSQL, catálogos y
restricciones iniciales
M02RF-M02-01Alfredo
## Montalvan
2TAR-00122/10 a
## 23/10
## Must
TAR-003Fijar tokens del brief en CSS
Claro y Oscuro
M11RNF-047Caleb
## Tardencilla
2TAR-00122/10 a
## 23/10
## Must

## 63
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-004Fijar POM, Wrapper y
compilación reproducible
## JDK 21
M12RNF-024Aris Gavarrete2TAR-00122/10 a
## 23/10
## Must
TAR-005Probar H1 y H6 con Task,
cancelación y ejecutor
controlado
M05RF-M05-04Steven Cuadra2TAR-00423/10 a
## 24/10
## Must
TAR-006Implementar conexión JDBC
y seguridad con
PreparedStatement
M01RF-M01-01Alfredo
## Montalvan
## 2TAR-002,
## TAR-004
23/10 a
## 24/10
## Must
TAR-007Construir acceso FXML y
ventana lateral con controles
del curso
M01RF-M01-12Caleb
## Tardencilla
## 2TAR-003,
## TAR-004
23/10 a
## 24/10
## Must
TAR-008Preparar interfaces HTTP y
DTO, sin conexión al
proveedor
M09RF-M09-01Aris Gavarrete2TAR-00423/10 a
## 24/10
## Must
TAR-009Implementar roles, hash
Argon2id y bloqueo de
cuentas
M01RF-M01-03Steven Cuadra2TAR-00624/10 a
## 25/10
## Must
TAR-010Probar contador
transaccional, reversión y
auditoría por JDBC
M06RF-M06-12Alfredo
## Montalvan
2TAR-00624/10 a
## 25/10
## Must
TAR-011Integrar selector, Preferences
por usuario y modalidad sin
efectos
M11RF-M11-08Caleb
## Tardencilla
2TAR-00724/10 a
## 25/10
## Must
TAR-012Probar plantilla mínima y
app-image como estudios
anticipados
M10RF-M10-01Aris Gavarrete2TAR-00424/10 a
## 25/10
## Must

## 64
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-013Revisar evidencia H1, H4 y
H6 para evaluación de
concurrencia
M12RF-M12-08Steven Cuadra2TAR-005,
## TAR-010
26/10 a
## 27/10
## Must
TAR-014Revisar SQL parametrizado y
restauración de migraciones
iniciales
M12RNF-028Alfredo
## Montalvan
## 2TAR-009,
## TAR-010
26/10 a
## 27/10
## Must
TAR-015Revisar Aurora estática,
teclado, popups y capa
mouseTransparent
M11RNF-055Caleb
## Tardencilla
2TAR-01126/10 a
## 27/10
## Must
TAR-016Respaldar incremento v0.1.0
y registrar revisión cruzada
M12D44; D45Aris Gavarrete2TAR-012,
## TAR-013,
## TAR-014,
## TAR-015
26/10 a
## 27/10
## Must
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Sprint 0. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.
La Tabla A2 detalla las tareas de Sprint 1, del 28/10 al 03/11 de 2026.
## Tabla A2
Tareas de Sprint 1
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-017Definir servicios
transaccionales y comprobar
permisos fuera de UI
M01RF-M01-05Steven Cuadra3TAR-01628/10 a
## 29/10
## Must

## 65
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-018Mapear Propiedad y Unidad
con Hibernate y @Version
M02RF-M02-02Alfredo
## Montalvan
3TAR-01628/10 a
## 29/10
## Must
TAR-019Integrar CRUD reutilizable de
propiedades y unidades
M02RF-M02-06Caleb
## Tardencilla
3TAR-01828/10 a
## 29/10
## Must
TAR-020Preparar consultas DTO para
reportes y plantillas vacías
M10RF-M10-05Aris Gavarrete3TAR-012,
## TAR-018
28/10 a
## 29/10
## Must
TAR-021Implementar validaciones de
contrato y estados activos
ampliados
M04RF-M04-05Steven Cuadra3TAR-01730/10 a
## 31/10
## Must
TAR-022Mapear Inquilino y Contrato;
integrar auditoría en doWork
M04RF-M04-01Alfredo
## Montalvan
3TAR-01830/10 a
## 31/10
## Must
TAR-023Integrar formulario e historial
de inquilinos
M03RF-M03-01Caleb
## Tardencilla
## 3TAR-019,
## TAR-022
30/10 a
## 31/10
## Must
TAR-024Preparar fixtures JSON y
servidor HttpServer de
pruebas
M09RF-M09-02Aris Gavarrete3TAR-00830/10 a
## 31/10
## Must
TAR-025Definir invariantes de pagos,
crédito, garantía y redondeo
M06RF-M06-04Steven Cuadra2TAR-02101/11 a
## 02/11
## Must
TAR-026Implementar activación
atómica, depósito y
ocupación
M04RF-M04-06Alfredo
## Montalvan
## 2TAR-022,
## TAR-025
01/11 a
## 02/11
## Must
TAR-027Integrar borrador, fechas,
canon y activación de
contrato
M04RF-M04-13Caleb
## Tardencilla
## 2TAR-023,
## TAR-026
01/11 a
## 02/11
## Must

## 66
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-028Diseñar DTO inmutable de
recibo y corte de estado de
cuenta
M10RF-M10-01Aris Gavarrete2TAR-020,
## TAR-025
01/11 a
## 02/11
## Must
TAR-029Probar roles, bloqueo y
consistencia del contrato
integrado
M01RF-M01-04Steven Cuadra2TAR-026,
## TAR-027
02/11 a
## 03/11
## Must
TAR-030Revisar mapeos, cascadas,
rollback y SQL del
incremento
M04RF-M04-05Alfredo
## Montalvan
2TAR-02602/11 a
## 03/11
## Must
TAR-031Probar formularios en dos
temas y revisar estados por
teclado
M11RNF-053Caleb
## Tardencilla
2TAR-02702/11 a
## 03/11
## Must
TAR-032Consolidar v0.2.0 y respaldo
del incremento con ORM
M12D44; D45Aris Gavarrete2TAR-028,
## TAR-029,
## TAR-030,
## TAR-031
02/11 a
## 03/11
## Must
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Sprint 1. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.
La Tabla A3 detalla las tareas de Sprint 2, del 04/11 al 10/11 de 2026.

## 67
## Tabla A3
Tareas de Sprint 2
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-033Implementar H1 idempotente
por contrato y período
M05RF-M05-01Steven Cuadra3TAR-03204/11 a
## 05/11
## Must
TAR-034Mapear cargos, pagos,
aplicaciones y contador con
## ORM
M06RF-M06-12Alfredo
## Montalvan
3TAR-03004/11 a
## 05/11
## Must
TAR-035Integrar cobro con
distribución automática y
selección manual
M06RF-M06-03Caleb
## Tardencilla
3TAR-03404/11 a
## 05/11
## Must
TAR-036Implementar snapshot de
recibo y generación mínima
## PDF
M10RF-M10-01Aris Gavarrete3TAR-028,
## TAR-034
04/11 a
## 05/11
## Must
TAR-037Implementar H2, gracia, mora
única y vencimientos
M05RF-M05-06Steven Cuadra3TAR-03306/11 a
## 07/11
## Must
TAR-038Integrar commit financiero
único, idempotencia y
numeración
M06RF-M06-07Alfredo
## Montalvan
3TAR-03406/11 a
## 07/11
## Must
TAR-039Integrar generación masiva,
progreso y cancelación visible
M05RF-M05-03Caleb
## Tardencilla
## 3TAR-033,
## TAR-035
06/11 a
## 07/11
## Must
TAR-040Construir consultas históricas
y prototipo de diez reportes
M10RF-M10-05Aris Gavarrete3TAR-03606/11 a
## 07/11
## Must
TAR-041Implementar anulación
autorizada con control de
dependencias
M06RF-M06-09Steven Cuadra2TAR-03808/11 a
## 09/11
## Must

## 68
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-042Implementar liquidación y
devolución de garantía
M07RF-M07-05Alfredo
## Montalvan
## 2TAR-026,
## TAR-038
08/11 a
## 09/11
## Must
TAR-043Integrar liquidación y
terminación con confirmación
por rol
M07RF-M07-07Caleb
## Tardencilla
2TAR-04208/11 a
## 09/11
## Must
TAR-044Integrar empaquetado
temprano y revisión de
licencias
M12RNF-046Aris Gavarrete2TAR-03608/11 a
## 09/11
## Must
TAR-045Probar 12 escenarios
concurrentes con dos clientes
PostgreSQL
M06RN-07Steven Cuadra2TAR-037,
## TAR-038,
## TAR-041
09/11 a
## 10/11
## Must
TAR-046Revisar laboratorio ORM y
reconciliación
pago/aplicación/bitácora
M06RF-M06-12Alfredo
## Montalvan
## 2TAR-041,
## TAR-042
09/11 a
## 10/11
## Must
TAR-047Probar pagos por teclado y
cambio de tema con tareas
activas
M11RNF-049Caleb
## Tardencilla
## 2TAR-039,
## TAR-043
09/11 a
## 10/11
## Must
TAR-048Revisar reporte mínimo tras
clase del 10 y etiquetar v0.3.0
M10RF-M10-01Aris Gavarrete2TAR-040,
## TAR-044,
## TAR-045,
## TAR-046,
## TAR-047
09/11 a
## 10/11
## Must
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Sprint 2. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.
La Tabla A4 detalla las tareas de Sprint 3, del 11/11 al 17/11 de 2026.

## 69
## Tabla A4
Tareas de Sprint 3
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-049Definir puerta de respaldo y
manejo de estados de
mantenimiento
M12RF-M12-06Steven Cuadra3TAR-04811/11 a
## 12/11
## Must
TAR-050Implementar solicitudes,
gastos y agregados de
mantenimiento
M08RF-M08-05Alfredo
## Montalvan
3TAR-04811/11 a
## 12/11
## Must
TAR-051Integrar mantenimiento y
panel con filtros; ocupación al
instante de generación
M11RF-M11-02Caleb
## Tardencilla
3TAR-05011/11 a
## 12/11
## Must
TAR-052Diseñar recibo, cartera, estado
de cuenta y ocupación en
## Studio
M10RF-M10-04Aris Gavarrete3TAR-040,
## TAR-048
11/11 a
## 12/11
## Must
TAR-053Probar seguridad, adjuntos y
rechazo de inyección SQL
básica
M01RF-M01-05Steven Cuadra3TAR-04913/11 a
## 14/11
## Must
TAR-054Implementar respaldo
AES-256-GCM, manifiesto y
restauración autenticada
aislada
M12RF-M12-07Alfredo
## Montalvan
3TAR-04913/11 a
## 14/11
## Must
TAR-055Integrar configuración,
bitácora y PAN-36/37 con
pwdClaveRespaldo y
## MSG-082
M12RF-M12-05Caleb
## Tardencilla
## 3TAR-051,
## TAR-054
13/11 a
## 14/11
## Must

## 70
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-056Diseñar ingresos/gastos,
contratos por vencer e
historial de pagos
M10RF-M10-03Aris Gavarrete3TAR-05213/11 a
## 14/11
## Must
TAR-057Automatizar comprobaciones
de cierre y recuperación de
tareas
M12RF-M12-08Steven Cuadra2TAR-05315/11 a
## 16/11
## Must
TAR-058Preparar SQL oráculo de diez
reportes, con monedas
separadas
M10RN-28Alfredo
## Montalvan
## 2TAR-050,
## TAR-056
15/11 a
## 16/11
## Must
TAR-059Revisar las 40 pantallas y
mensajes en Claro y Oscuro
M11RNF-047Caleb
## Tardencilla
2TAR-05515/11 a
## 16/11
## Must
TAR-060Completar liquidación,
mantenimiento y tasas;
exportar PDF/XLSX
M10RF-M10-13Aris Gavarrete2TAR-056,
## TAR-058
15/11 a
## 16/11
## Must
TAR-061Registrar riesgos y revisar
protocolo académico del 17
M12D44; D45Steven Cuadra2TAR-05716/11 a
## 17/11
## Must
TAR-062Reconciliar reportes, totales y
corte; preparar fixtures de
desempeño
M10RNF-004Alfredo
## Montalvan
## 2TAR-058,
## TAR-060
16/11 a
## 17/11
## Must
TAR-063Ejecutar ensayo visual interno
y revisar defectos del
incremento
M11RNF-053Caleb
## Tardencilla
## 2TAR-059,
## TAR-060
16/11 a
## 17/11
## Must
TAR-064Verificar laboratorio Jasper,
diez plantillas y versión
v0.4.0
M10RF-M10-10Aris Gavarrete2TAR-061,
## TAR-062,
## TAR-063
16/11 a
## 17/11
## Must

## 71
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Sprint 3. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.
La Tabla A5 detalla las tareas de Sprint 4, del 18/11 al 25/11 de 2026.
## Tabla A5
Tareas de Sprint 4
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-065Implementar coordinación
H3, presupuesto y cierre
durante HTTP
M09RF-M09-05Steven Cuadra3TAR-06418/11 a
## 19/11
## Must
TAR-066Persistir caché y autorización
por fecha sin alterar pagos
previos
M09RF-M09-08Alfredo
## Montalvan
3TAR-06418/11 a
## 19/11
## Must
TAR-067Integrar panel de tasas, aviso
histórico y autorización del
## Propietario
M09RF-M09-07Caleb
## Tardencilla
3TAR-06618/11 a
## 19/11
## Must
TAR-068Implementar cliente
HttpClient y parser Gson
contra contrato simulado
M09RF-M09-03Aris Gavarrete3TAR-024,
## TAR-064
18/11 a
## 19/11
## Must
TAR-069Probar roles y 14 escenarios
de fallos HTTP con reloj
controlado
M09RF-M09-02Steven Cuadra3TAR-065,
## TAR-068
20/11 a
## 21/11
## Must
TAR-070Probar bloqueo, reversión,
crédito y respaldo LAN
M06RF-M06-12Alfredo
## Montalvan
3TAR-06620/11 a
## 21/11
## Must

## 72
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-071Ejecutar matriz Aurora,
reinicio, cambio de usuario y
fallo local
M11RNF-050Caleb
## Tardencilla
## 3TAR-067,
## TAR-068
20/11 a
## 21/11
## Must
TAR-072Medir 500 cargos, 10000 filas
y latencia UI en equipo
definido
M10RNF-004Aris Gavarrete3TAR-06820/11 a
## 21/11
## Must
TAR-073Revisar regresión Must y
corregir seguridad o
concurrencia
M01RF-M01-05Steven Cuadra3TAR-069,
## TAR-070
22/11 a
## 23/11
## Must
TAR-074Corregir consistencia y
validar restauración con
hashes
M12RNF-037Alfredo
## Montalvan
## 3TAR-070,
## TAR-072
22/11 a
## 23/11
## Must
TAR-075Corregir interfaz y revisar
manual con capturas en dos
temas
M11RNF-055Caleb
## Tardencilla
3TAR-07122/11 a
## 23/11
## Must
TAR-076Generar app-image ZIP con
jpackage e inventario de
distribución
M12RNF-046Aris Gavarrete3TAR-072,
## TAR-074
22/11 a
## 23/11
## Must
TAR-077Revisar guion, trazabilidad
RA y aceptación del 25
## M12D44; D45;
## D47
Steven Cuadra2TAR-07324/11 a
## 25/11
## Must
TAR-078Validar migraciones desde
cero y datos de demostración
congelados
M12RNF-028Alfredo
## Montalvan
2TAR-07424/11 a
## 25/11
## Must
TAR-079Ensayar diez recorridos y
corregir accesibilidad
bloqueante
M11RNF-053Caleb
## Tardencilla
2TAR-07524/11 a
## 25/11
## Must

## 73
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-080Cerrar v0.9.0-rc.1 del 25 con
manual, respaldo y seis
documentos
## M12D44; D45;
## D47
Aris Gavarrete2TAR-076,
## TAR-077,
## TAR-078,
## TAR-079
24/11 a
## 25/11
## Must
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Sprint 4. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.
La Tabla A6 detalla las tareas de Cierre, del 26/11 al 01/12 de 2026.
## Tabla A6
Tareas de Cierre
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-081Coordinar defensa del 26 y
recoger observaciones
académicas
M12D44; D45Steven Cuadra2TAR-08026/11 a
## 26/11
## Must
TAR-082Verificar base y respaldo
antes de la sesión del 26
M12RF-M12-06Alfredo
## Montalvan
2TAR-08026/11 a
## 26/11
## Must
TAR-083Verificar interfaz y
proyección antes de la sesión
del 26
M11RNF-047Caleb
## Tardencilla
2TAR-08026/11 a
## 26/11
## Must
TAR-084Verificar app-image ZIP y
contingencia HTTP antes del
## 26
M09RF-M09-01Aris Gavarrete2TAR-08026/11 a
## 26/11
## Must

## 74
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-085Clasificar observaciones sin
admitir alcance incompatible
M12D44; D45Steven Cuadra2TAR-08127/11 a
## 29/11
## Must
TAR-086Corregir datos o persistencia
según regresión dirigida
M06RF-M06-12Alfredo
## Montalvan
## 2TAR-082,
## TAR-085
27/11 a
## 29/11
## Must
TAR-087Corregir hallazgos de UI y
mantener ambos temas
M11RNF-047Caleb
## Tardencilla
## 2TAR-083,
## TAR-085
27/11 a
## 29/11
## Must
TAR-088Corregir integración, reportes
o paquete y regenerar
manifiesto
M10RF-M10-10Aris Gavarrete2TAR-084,
## TAR-085
27/11 a
## 29/11
## Must
TAR-089Revisar evidencia RA1 a RA5
y defectos antes del cierre
## M12D44; D45;
## D47
Steven Cuadra2TAR-086,
## TAR-087,
## TAR-088
30/11 a
## 30/11
## Must
TAR-090Ejecutar regresión de finanzas
y restauración final
M12RNF-037Alfredo
## Montalvan
2TAR-08630/11 a
## 30/11
## Must
TAR-091Ensayar defensa del 30 y
verificar operación por
teclado
M11RNF-053Caleb
## Tardencilla
2TAR-08730/11 a
## 30/11
## Must
TAR-092Consolidar v1.0.0 y dos
copias verificadas para el 1 de
diciembre
M12RNF-046Aris Gavarrete2TAR-088,
## TAR-089,
## TAR-090,
## TAR-091
30/11 a
## 30/11
## Must
TAR-093Presentar concurrencia,
seguridad y cierre del 1 de
diciembre
M12RF-M12-08Steven Cuadra2TAR-09201/12 a
## 01/12
## Must
TAR-094Presentar JDBC, Hibernate y
consistencia de operaciones
M06RF-M06-12Alfredo
## Montalvan
2TAR-09201/12 a
## 01/12
## Must

## 75
TareaDescripciónMóduloRequisito
## Principal
## Responsable
## Propuesto
hDepende deVentana
## 2026
## Prioridad
TAR-095Presentar controles, FXML,
temas y recorrido de cobro
M11RNF-047Caleb
## Tardencilla
2TAR-09201/12 a
## 01/12
## Must
TAR-096Presentar reportes, HTTP y
archivar evidencia académica
M10RF-M10-10Aris Gavarrete2TAR-092,
## TAR-093,
## TAR-094,
## TAR-095
01/12 a
## 01/12
## Must
Nota. h indica horas estimadas. Todas las tareas de esta tabla pertenecen a Cierre. Las precedencias TAR de la misma fecha se
ejecutan en orden, sin trabajo simultáneo dependiente.

## 76
## Apéndice B
Casos de Prueba Completos
Los cien casos usan los conjuntos D0 a D4 y el servidor S1 definidos en la Tabla 11, con restauración aislada entre escenarios
que modifican datos. Cada fila identifica módulo, requisito o regla, precondición, procedimiento, resultado esperado, tipo y prioridad.
El operador registrará revisión Git, fecha, entorno y evidencia cuando ejecute el caso, sin completar resultados históricos por
anticipado. Las Tablas B1 a B6 agrupan pruebas según su propósito y conservan los identificadores CP. La matriz visual se ejecutará
en Claro y Oscuro, con ambos modos de efectos y roles aplicables.
La Tabla B1 contiene CP-001 a CP-040, correspondientes a funcionales e integración.
## Tabla B1
Funcionales e Integración
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-001
Acceso correcto
## M01
## RF-M01-01
D0; cuenta
Propietario activa
Abrir PAN-01; escribir
credenciales de D0; activar
## Ingresar
Abre PAN-02 una vez y registra
acceso sin contraseña.
## Funcional /
integración
## Prioridad
## Alta
## CP-002
## Credenciales
inválidas
## M01
## RF-M01-02
D0; cuenta
inexistente y
contraseña errada
Intentar acceso con cada
combinación; comparar
aviso
Mismo mensaje genérico; cero
sesiones autorizadas.
## Funcional /
integración
## Prioridad
## Alta

## 77
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-003
Quinto fallo y
desbloqueo
## M01
## RN-15
D0; reloj fijo; cero
fallos
Enviar cinco claves erradas;
intentar clave válida;
avanzar 15 min
Quinto fallo bloquea; antes del
plazo rechaza; después permite
y reinicia contador.
## Funcional /
integración
## Prioridad
## Alta
## CP-004
Hash y sal por
cuenta
## M01
## RF-M01-03
D0; dos cuentas
nuevas
Guardar igual contraseña;
consultar columnas de hash
y sal como inspector
Hashes diferentes; verificación
correcta; cero contraseñas
legibles en BD o log.
## Funcional /
integración
## Prioridad
## Alta
## CP-005
Permisos del
## Asistente
## M01
## RN-13
D0; sesión AsistenteInvocar por UI y servicio
anular recibo, restaurar,
gestionar usuarios y
parámetros Variante D19:
revocar capacidad antes de
que una tarea diferida
empiece a operar y volver a
comprobar en servicio.
Cuatro acciones rechazadas en
servicio; cero mutaciones; tema
sigue permitido. Denegación
ERR-009/MSG-062; sin
escritura no autorizada y con
excepciones de la matriz
vigente.
## Funcional /
integración
## Prioridad
## Alta
## CP-006
## Último
## Propietario
## M01
## RF-M01-10
D0; un Propietario
activo
Solicitar su baja y cambio
de rol; recargar cuentas
Conserva al menos un
Propietario activo; muestra
rechazo sin baja.
## Funcional /
integración
## Prioridad
## Alta
## CP-007
Inyección básica
## M01
## RNF-005
D0; cadena literal "'
## OR '1'='1"
Pegar cadena en acceso y
búsqueda; enviar como dato
parametrizado
Cero acceso indebido;
estructura de consulta intacta;
cero tablas alteradas.
## Funcional /
integración
## Prioridad
## Alta
## CP-008
Alta y edición de
propiedad
## M02
## RF-M02-01
D0; nombre
## Propiedad Demo
Guardar propiedad; cambiar
dirección; reiniciar consulta
Una propiedad con UUID
estable y dirección nueva;
auditoría de cambio.
## Funcional /
integración
## Prioridad
## Alta

## 78
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-009
Código de
unidad único
## M02
## RN-16
D0; propiedad P1
con U01
Crear U01 en P1; después
crear U01 en P2
Rechaza duplicado en P1;
acepta mismo código en P2.
## Funcional /
integración
## Prioridad
## Alta
## CP-010
Tipos y canon
## M02
## RF-M02-02
D0; unidad nuevaGuardar cada tipo admitido;
intentar canon cero y
moneda EUR
Acepta cuatro tipos; rechaza
cero y EUR; persiste solo NIO
o USD.
## Funcional /
integración
## Prioridad
## Alta
## CP-011
Baja con
contrato activo
## M02
## RN-11
D1; C1 VigentePropietario solicita baja de
U01; luego termina y
liquida C1; reintenta
Primero rechaza; después baja
lógica con historial conservado.
## Funcional /
integración
## Prioridad
## Alta
## CP-012
Datos mínimos
de inquilino
## M03
## RF-M03-01
D0; nombre
## Inquilino Demo
Guardar sin contacto;
agregar correo válido;
guardar y buscar por
nombre
Rechaza ausencia de contacto;
alta única; búsqueda recupera el
expediente.
## Funcional /
integración
## Prioridad
## Alta
## CP-013
Correo e
identificación
repetida
## M03
## RF-M03-02
D0; expediente con
## ID-DEMO-1
Probar correo sin dominio;
repetir identificación
existente
Correo inválido bloquea;
identificación coincidente
advierte antes de confirmar.
## Funcional /
integración
## Prioridad
## Alta
## CP-014
Adjunto y huella
## M03
## RN-25
D0; PDF válido de 1
MiB y ejecutable
renombrado
Adjuntar PDF; verificar
copia y hash; intentar
ejecutable; probar 6 MiB
Copia válida con SHA-256
concordante; rechaza firma
inválida y exceso de 5 MiB.
## Funcional /
integración
## Prioridad
## Alta
## CP-015
Mes calendario
mínimo
## M04
## RN-02
D0; contrato
## Borrador
Probar 15/01/2026 a
14/02/2026; luego 15/02;
probar 31/01 a 28/02
Rechaza primer intervalo;
acepta los otros dos según
ajuste de mes.
## Funcional /
integración
## Prioridad
## Alta

## 79
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-016
Estados que
reservan unidad
## M04
## RN-01
D1; unidad con
contrato activo
Repetir intento de activar
sucesor solapado con
anterior Vigente, Por vencer
y Vencido
Tres rechazos; un solo intervalo
de ocupación para la unidad.
## Funcional /
integración
## Prioridad
## Alta
## CP-017
Activación y
garantía
## M04
## RF-M04-06
D0; inicio y
activación
23/10/2026, fin
23/10/2027; depósito
100 USD y variante
## 0 USD.
Activar con fecha operativa
23/10/2026 de BD;
consultar unidad, contrato y
depósito; repetir desde D0
con garantía cero.
Vigente, Ocupada y Retenido
juntos. Con 0: depósito
Retenido de importe cero, sin
movimientos ni recibo; renta
separada.
## Funcional /
integración
## Prioridad
## Alta
## CP-018
## Renovación
enlazada
## M04
## RF-M04-08
D1; C1 termina
## 30/11/2026
Crear sucesor 01/12/2026;
intentar activarlo el 23/10;
cerrar C1 al llegar fecha
Antes permanece Borrador;
después activa sin solapar;
conserva enlace a C1.
## Funcional /
integración
## Prioridad
## Alta
## CP-019
Día 31 y renta
completa
## M04
## RN-17
D0; contrato con
pago día 31
Calcular vencimiento de
febrero de 2027 y período
parcial inicial
Vence 28/02; cobra canon
completo; cero prorrateo
implícito.
## Funcional /
integración
## Prioridad
## Alta
## CP-020
Cancelación de
## Borrador
## M04
## RN-19
D0; Borrador sin
cargos
Cancelar con motivo; contar
cargos, pagos y depósitos
efectivos
Estado Cancelado; cero
movimientos financieros
nuevos.
## Funcional /
integración
## Prioridad
## Alta
## CP-021
Renta mensual
única
## M05
## RF-M05-01
D1; 3 contratos
elegibles y uno
terminado
Generar noviembre; repetir
el mismo período
Tres rentas; segunda ejecución
crea cero duplicados; terminado
no genera.
## Funcional /
integración
## Prioridad
## Alta

## 80
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-022
Gracia y mora
única
## M05
## RN-05
D1; renta 100 USD;
gracia 5 días; mora 5
## %
Fijar reloj en último día de
gracia; avanzar un día;
ejecutar H2 dos veces
Cero mora en gracia; una mora
de 5.00 USD después; ninguna
mora sobre Mora.
## Funcional /
integración
## Prioridad
## Alta
## CP-023
Cargos manuales
## M05
## RF-M05-05
D1; C1 activoCrear Reparación, Servicio
y Otro con concepto;
intentar monto negativo
Tres cargos trazables; rechazo
del negativo sin escritura.
## Funcional /
integración
## Prioridad
## Alta
## CP-024
Precedencia de
estados
## M05
## RN-24
D1; cargos sin pago,
parciales, vencidos y
anulados
Consultar antes y después
de vencimiento y de
liquidación
Pagado con saldo cero; Vencido
prevalece sobre Parcial;
Anulado prevalece.
## Funcional /
integración
## Prioridad
## Alta
## CP-025
Anular cargo
con aplicaciones
## M05
## RF-M05-09
D2; cargo octubre de
100 USD con abono
previo de 40 USD
Solicitar anulación como
Propietario; comparar saldo
y auditoría
Rechaza mientras tenga
aplicaciones activas; saldo
permanece 60.00 USD.
## Funcional /
integración
## Prioridad
## Alta
## CP-026
## Aplicación
automática
parcial
## M06
## RN-06
D2; octubre 100
USD y noviembre
## 100 USD
Pagar 40 USD por Efectivo;
consultar aplicaciones y
recibo
Octubre queda 60; noviembre
100; un pago y un recibo de 40
## USD.
## Funcional /
integración
## Prioridad
## Alta
## CP-027
## Selección
manual
## M06
## RF-M06-03
D2; dos cargos del
mismo contrato
Asignar 40 USD al segundo;
intentar seleccionar cargo de
otro contrato
Abona el segundo por elección
explícita; rechaza mezcla de
contratos.
## Funcional /
integración
## Prioridad
## Alta
## CP-028
Excedente y
siguiente renta
## M06
## RF-M06-04
D2; deuda total 200
## USD
Pagar 230 USD; generar
siguiente cargo 100 USD
Crédito 30 al confirmar;
siguiente cargo queda en 70;
crédito restante cero.
## Funcional /
integración
## Prioridad
## Alta

## 81
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-029
Conversión NIO
a USD
## M06
## RN-09
D3; tasa ficticia
autorizada
## 36.50000000;
variante BCN
autorizada
36.62430000 para la
fecha.
Pagar 1460.00 NIO y
cotejar la copia de tasa
ficticia. Variante BCN:
pagar 3662.43 NIO sobre
obligación de 100 USD.
Base ficticia: abono 40.00
USD; variante BCN: 100.00
USD y saldo cero. Original,
tasa, origen y fecha inmutables
en cada variante.
## Funcional /
integración
## Prioridad
## Alta
## CP-030
Conversión USD
a NIO
## M06
## RN-09
D3; cargo 2000 NIO
y tasa ficticia
## 36.50000000;
variante BCN
autorizada
## 36.62430000.
Pagar 40.00 USD; consultar
conversión y saldo. Variante
BCN: pagar 100.00 USD
sobre obligación de 3662.43
## NIO.
Base ficticia: abono 1460.00
NIO y saldo 540.00 NIO;
variante BCN: abono 3662.43
NIO y saldo cero; no invierte
tasa.
## Funcional /
integración
## Prioridad
## Alta
## CP-031
## Redondeo
## HALF_EVEN
## M06
## RN-32
D3; tasa sintética
2.00000000; cargos
en USD
Convertir 2.01 NIO y
distribuir el total en dos
cargos
Total 1.00 USD; suma de
aplicaciones más crédito igual
al total convertido.
## Funcional /
integración
## Prioridad
## Alta
## CP-032
## Anulación
trazable
## M06
## RF-M06-09
D2; pago confirmado
sin crédito aplicado
después
Anular como Propietario
con motivo; reimprimir
## Variante D22
independiente: confirmar
100 pagos con diez emisores
en dos JVM y anular diez
después del commit.
Pago y recibo Anulado;
conserva número; saldo
restaurado y motivo visible.
Variante: 100 recibos
conservados, 90 pagos
Registrado y 10 Anulado;
contador inicial 1 y
siguiente_numero=101; sin
reutilización.
## Funcional /
integración
## Prioridad
## Alta

## 82
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-033
Crédito con
dependencia
posterior
## M06
## RN-31
D2; pago excedente
usado en siguiente
cargo
Intentar anular pago origen;
revertir primero aplicación
dependiente autorizada
Primer intento rechaza; segundo
respeta orden y deja referencias
cruzadas.
## Funcional /
integración
## Prioridad
## Alta
## CP-034
Conservación de
garantía
## M07
## RN-26
D1; depósito 100
## USD
Aplicar 30 a deuda y
devolver 50 en un paquete;
repetirlo con el mismo
liquidacion_id y hash;
consultar remanente y
estado Variante C01: aplicar
a cargo inexistente, anulado
o de otro contrato; conservar
error específico de exceso
de saldo.
## 100 = 30 + 50 + 20; Devuelto
parcial; renta no aumenta por
depósito. Movimientos con
liquidacion_id y
liquidacion_hash compartidos,
orden distinto; reintento sin
duplicados. Cargo
incompatible:
ERR-061/MSG-084, sin
aplicación y con propuesta
conservada.
## Funcional /
integración
## Prioridad
## Alta
## CP-035
Deducción sin
concepto
## M07
## RF-M07-04
D1; depósito
## Retenido
Intentar deducir 20 USD sin
concepto; luego agregar
concepto y confirmar con
liquidacion_id,
liquidacion_hash del
paquete y orden 1
Primer intento no escribe;
segundo conserva importe,
concepto y actor.
## Funcional /
integración
## Prioridad
## Alta

## 83
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-036
Devolución total
## M07
## RF-M07-05
D1; depósito 100
USD sin deuda
Devolver 100 como
Propietario; repetir el
paquete con el mismo
liquidacion_id y
liquidacion_hash, orden 1
Variante C01: intentar
terminar antes de completar
la liquidación; resolver
remanente y confirmar
cierre autorizado. Contrastar
garantía inicial cero sin
movimientos conforme
## DC-08.
Devuelto, remanente cero; una
sola devolución y un
comprobante de garantía.
Liquidación pendiente:
ERR-062/MSG-085 y cierre
bloqueado; deuda residual
compatible con terminar;
excepción DC-08 conservada.
## Funcional /
integración
## Prioridad
## Alta
## CP-037
Estados de
mantenimiento
## M08
## RN-27
D0; solicitud AbiertaPasar En proceso y
Resuelta; intentar volver a
## Abierta Variantes C01:
Abierta a Cancelada; En
proceso a Cancelada;
estados terminales; repetir
estado actual; intentar otras
transiciones.
Transiciones válidas auditadas;
retorno desde estado terminal
rechazado. Solo las transiciones
aprobadas cambian estado;
repetición idempotente sin
nuevos eventos; transición
inválida: ERR-066/MSG-088 y
estado intacto.
## Funcional /
integración
## Prioridad
## Alta
## CP-038
Gasto y cargo
separados
## M08
## RN-23
D1; reparación con
costo 25 USD
Registrar gasto; consultar
deuda; crear cargo manual
autorizado aparte Variante
C01: seleccionar una
propiedad distinta de la
unidad de la solicitud de
mantenimiento.
Gasto 25; deuda sin cambio
hasta crear cargo; ninguna
doble imputación automática.
## Incompatibilidad:
ERR-065/MSG-087, sin
registro y selección corregible.
## Funcional /
integración
## Prioridad
## Alta

## 84
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-039
Panel al instante
de generación
## M11
## RF-M11-01
D0 + variante D1
con 2 contratos
activos; 4 unidades
activas, 2 ocupadas
Abrir panel; fijar el instante
de generación de BD y
comparar conteo SQL y
moneda, sin selector
histórico de ocupación.
Ocupación 50 % en ese
instante; instante visible; USD y
NIO en agregados separados.
## Funcional /
integración
## Prioridad
## Alta
## CP-040
## Auditoría
financiera
atómica
## M12
## RF-M12-04
D2; pago 40 USD;
BD con now()
controlado; reloj
local alineado y
variantes ±5 min y
±5 min 1 s.
Confirmar y consultar
auditoría; intentar editar
evento. Repetir con desfases
de reloj: instante y fecha
financiera provienen de
now() de BD. Variantes
D24/C01: conservar fecha
efectiva ingresada y
creado_en de BD; no
interrumpir una transacción
ya iniciada por desfase.
Inyectar inconsistencia antes
de commit; distinguir
rollback confirmado de
commit incierto.
Evento atómico e inmutable.
Con |local − BD| ≤5 min
permite; >5 min bloquea nuevas
finanzas con
ERR-057/MSG-080 y cero
escritura financiera.
ERR-063/MSG-086 solo con
rollback confirmado y cero
cambios; commit incierto:
ERR-031 o
ERR-054/MSG-064, sin afirmar
reversión.
## Funcional /
integración
## Prioridad
## Alta
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.
La Tabla B2 contiene CP-041 a CP-052, correspondientes a concurrencia.

## 85
## Tabla B2
## Concurrencia
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-041
Diez emisores en
dos JVM
## M06
## RNF-021;
## RN-07
D2; 10 hilos
repartidos en 2 JVM;
contador inicial 1;
100 pagos y 10
solicitudes
adicionales para
rollback.
Sincronizar 10 hilos con
barreras; confirmar 100
pagos e inyectar 10
reversiones antes de
commit. El tope de 3
sesiones LAN cuenta
clientes, no hilos.
100 recibos únicos contiguos 1–
100; siguiente_numero=101; 10
rollbacks sin consumo ni
huérfanos; pagos = aplicaciones
+ crédito. La variante de diez
anulaciones posteriores se
comprueba por separado en
## CP-032 (D22).
## Concurrencia
## Prioridad
## Alta
## CP-042
Rollback tras
reservar número
## M06
## RN-07
D2; contador 1; falla
inyectada antes de
commit
A reserva y revierte; B
confirma después; consultar
contador y recibos. Variante
conjunta con CP-041: forzar
10 rollbacks adicionales
antes de commit
Base aislada: B recibe 1 y
siguiente_numero=2; A sin
huérfanos. Variante CP-041:
100 recibos 1–100,
siguiente_numero=101; 10
rollbacks sin consumo.
## Concurrencia
## Prioridad
## Alta
## CP-043
Doble clic con
mismo UUID
## M06
## RN-21
D2; misma solicitud
y huella
Enviar la misma solicitud
desde dos hilos al mismo
tiempo
Un pago y un recibo; ambas
respuestas recuperan el mismo
resultado.
## Concurrencia
## Prioridad
## Alta
## CP-044
Resultado de
commit incierto
## M06
## RN-21
D2; proxy de prueba
corta respuesta tras
commit
Confirmar; cortar respuesta;
reintentar con el mismo
UUID y contenido
Recupera recibo existente;
contador avanza una sola vez.
## Concurrencia
## Prioridad
## Alta
## CP-045
H1 contra H1 en
dos clientes
## M05
## RN-20
D1; 500 contratos;
mismo período
Arrancar dos lotes a la vez;
comparar claves
contrato/período
Exactamente 500 rentas; cero
duplicados y errores
recuperables identificados.
## Concurrencia
## Prioridad
## Alta

## 86
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-046
H1 contra H2
## M05
## RN-20
D1; contratos
vencidos y mismo
bloqueo contractual
Soltar ambos trabajadores
con barrera; consultar renta
y mora
Una renta por período; una
mora por renta elegible; cero
sumas perdidas.
## Concurrencia
## Prioridad
## Alta
## CP-047
## Cancelar
generación
masiva
## M05
## RF-M05-04
D1; 500 contratos;
pausa después de 20
commits
Cancelar H1 en la pausa;
reiniciar lote del mismo
período
20 contratos completos al
cancelar; reinicio alcanza 500
sin duplicados.
## Concurrencia
## Prioridad
## Alta
## CP-048
Dos pagos al
mismo saldo
## M06
## RF-M06-12
D2; un cargo de 100
USD; dos pagos de
## 80
Confirmar ambos desde
JVM distintas; consultar
saldo y crédito
Saldo cero, crédito total 60
USD; ningún saldo negativo.
## Concurrencia
## Prioridad
## Alta
## CP-049
Cerrar durante
## H1
## M12
## RN-30
D1; H1 pausado
entre contratos
Cerrar; aceptar cancelación;
liberar barrera; observar
ejecutores y BD Variante
C01: cancelar durante un
contrato y revisar el
resumen del lote.
Solo transacciones completas;
hilos terminan; si pasan 10 s
proceso sigue abierto con aviso.
Cancelación sin ERR,
MSG-059 literal del Brief;
termina el contrato en curso,
conserva confirmados e
identifica pendientes.
## Concurrencia
## Prioridad
## Alta
## CP-050
Cerrar durante
HTTP y reporte
## M12
## RN-30
## D0; HTTP
bloqueado y H5
escribiendo temporal
Solicitar cierre; cancelar
trabajos; revisar archivo
final y conexiones Variantes
C01: cancelar en consulta,
llenado y exportación H5;
resolver carrera con
publicación, partiendo de
destino previo y finanzas
confirmadas.
Cero archivo parcial publicado;
recursos cerrados sin congelar
interfaz. MSG-089 sin ERR
solo tras terminar y limpiar
temporales antes de publicar;
destino previo y finanzas
confirmadas conservados. Si ya
publicó, informa resultado real.
## Concurrencia
## Prioridad
## Alta

## 87
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-051
Respaldo contra
escritura LAN
## M12
## RN-29
D1; puerta exclusiva
de respaldo
adquirida
Intentar pago desde segundo
cliente; completar respaldo;
restaurar copia Variantes
D37: iniciar respaldo con
escritura en curso; esperar
su finalización y excluir
nuevas escrituras; cotejar
base, adjuntos y manifiesto.
Pago espera o falla por límite de
bloqueo; respaldo y adjuntos
conservan mismas huellas.
Copia coherente sin
credenciales ni claves; no se
acredita medición por definir el
caso.
## Concurrencia
## Prioridad
## Alta
## CP-052
## Activación
simultánea y
control optimista
## M04
## RN-01
D0; dos Borradores
para U01 y dos
editores
Activar a la vez; después
editar misma versión desde
dos sesiones
Un solo contrato activo;
segunda edición desactualizada
rechazada sin sobreescritura.
## Concurrencia
## Prioridad
## Alta
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.
La Tabla B3 contiene CP-053 a CP-066, correspondientes a http con servidor simulado.
## Tabla B3
HTTP con Servidor Simulado
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-053
Éxito con Bearer
## M09
## RF-M09-01
Servidor S1 con
fixture JSON válido
Solicitar actualización;
inspeccionar petición y
cache
GET /v6/latest/USD, Bearer
redactado; base USD, NIO
ficticio 36.5 y marcas
guardadas.
## HTTP
simulado
## Prioridad
## Alta

## 88
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-054
Servidor caído
## M09
## RF-M09-02
S1 detenido; caché
previa válida
Variantes S1: HTTP
5xx y 408.
Actualizar; avanzar reloj 60
s; permitir un reintento
Conserva caché; dos intentos
como máximo; aviso no
bloqueante. 5xx/408
transitorios: único reintento tras
60 s; 429 sigue CP-060/061 y
política de cuota (D23).
## HTTP
simulado
## Prioridad
## Alta
## CP-055
Tiempo agotado
## M09
## RF-M09-01
S1 acepta conexión
pero no envía cuerpo
Actualizar; controlar reloj
monotónico y mantener
navegación
Cancela al presupuesto de 10 s;
conexión máximo 5 s; UI sigue
operable.
## HTTP
simulado
## Prioridad
## Alta
## CP-056
JSON inválido
## M09
## RF-M09-02
S1 devuelve HTTP
200 y cuerpo
## {invalido
Actualizar; consultar caché
y log
Rechaza cuerpo; conserva
referencia anterior; incidente
sin credencial.
## HTTP
simulado
## Prioridad
## Alta
## CP-057
Base incorrecta
## M09
## RF-M09-02
S1 success con base
EUR y NIO positivo
Actualizar; inspeccionar
historial
Cero nueva tasa aceptada; error
de esquema identificable.
## HTTP
simulado
## Prioridad
## Alta

## 89
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-058
NIO inválido y
banda técnica 1–
## 100
## M09
## RF-M09-02;
## RN-22;
## RNF-041
S1: NIO ausente, 0,
negativo, texto, no
finito; límites 1 y
100; fuera de banda
0.99 y 100.01.
Probar cada variante aislada
con JSON restante válido;
contrastar referencia vigente
y eventos M09 tras cada
respuesta. Variante D23:
respuesta válida dentro de la
consulta diaria y referencia
comercial sin autorización
automática de pagos; tasa
manual fuera de banda con
revisión y confirmación del
Propietario, sin CHECK de
banda en BD.
Admite 1 y 100; rechaza
ausentes/tipos inválidos. Fuera
de banda: vigente intacta y
evento
## REFERENCIA_EN_CUAREN
TENA con valor y huella; sin
tabla nueva. Referencia
comercial y autorización
manual mantienen contratos
distintos; JSON, base y fechas
se contrastan con
## CP-056/057/064.
## HTTP
simulado
## Prioridad
## Alta
## CP-059
Cuota agotada
con HTTP 200
## M09
## RF-M09-02
S1 result=error y
error-type=quota-
reached
Actualizar; avanzar reloj un
minuto; volver a solicitar
Suspende consultas
automáticas; informa cuota; no
convierte error en tasa.
## HTTP
simulado
## Prioridad
## Alta
## CP-060
HTTP 429 con
Retry-After
## M09
## RF-M09-05
S1 devuelve 429 y
Retry-After: 120
Actualizar; intentar a 119 s
y a 120 s
No reintenta antes de 120 s;
programación también respeta
ventana diaria.
## HTTP
simulado
## Prioridad
## Alta
## CP-061
429 sin Retry-
## After
## M09
## RF-M09-05
S1 devuelve 429 sin
cabecera
Actualizar; avanzar 23 h;
solicitar; avanzar hasta 24 h
No solicita durante 24 h; aviso
y caché conservados.
## HTTP
simulado
## Prioridad
## Alta
## CP-062
## Credencial
inválida
## M09
## RF-M09-01
S1 devuelve 401,
luego 403 y error
invalid-key
Ejecutar cada variante con
reinicio del fixture
Tres rechazos; suspende hasta
corregir configuración; cero
secreto en log.
## HTTP
simulado
## Prioridad
## Alta

## 90
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-063
Cuerpo o MIME
inadmitido
## M09
## RF-M09-02
S1 devuelve 257 KiB
o text/html
Solicitar ambas variantes;
observar consumo y caché
Rechaza más de 256 KiB y
MIME ajeno; referencia
anterior intacta.
## HTTP
simulado
## Prioridad
## Alta
## CP-064
## Fechas
incoherentes
## M09
## RF-M09-02
S1 envía last_update
futuro >5 min o
next_update anterior
Actualizar con ambas
variantes y reloj fijo
Dos rechazos; conserva
historial anterior y explica fecha
incoherente.
## HTTP
simulado
## Prioridad
## Alta
## CP-065
## Programación
compartida LAN
## M09
## RF-M09-05
D0; dos clientes; S1
cuenta solicitudes
Abrir ambos clientes;
adelantar reloj un día;
solicitar en ambos
Una consulta ordinaria
compartida; presupuesto interno
máximo 100 intentos/mes.
## HTTP
simulado
## Prioridad
## Alta
## CP-066
Tasa histórica y
autorización
## M09
## RF-M09-09
D3; caché de ayer y
autorización ausente
hoy
Intentar pago entre
monedas; autorizar hoy
como Propietario; reintentar
Primero bloquea; después usa
tasa autorizada de hoy; pago
misma moneda sigue
disponible.
## HTTP
simulado
## Prioridad
## Alta
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.
La Tabla B4 contiene CP-067 a CP-076, correspondientes a reportes y reconciliación.

## 91
## Tabla B4
Reportes y Reconciliación
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-067
## Recibo
## M10
## RF-M10-01
D2; pago confirmado
## 40 USD
Exportar PDF; comparar
DTO, número, cargo y
monto con SQL
Un recibo de 40.00 USD;
número y condición de
anulación coinciden.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-068
Cartera vencida
## M10
## RF-M10-02
D2; corte 30/11/2026Generar PDF y XLSX;
ejecutar fn_cargos_corte
con igual corte
Principal y mora iguales al
oráculo por moneda; tolerancia
monetaria 0.00.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-069
Ingresos y gastos
## M10
## RF-M10-03
D1; renta 40 USD,
depósito 100 USD,
gasto 25 USD
Exportar período; comparar
SQL de ingresos y gastos
Ingreso por renta 40; gasto 25;
depósito excluido de ingreso;
NIO separado.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-070
## Ocupación
## M10
## RF-M10-04
D0; dos de cuatro
unidades activas
ocupadas
Generar al instante actual de
BD; cotejar SQL del mismo
instante; verificar ausencia
de selector de fecha
histórica de ocupación.
50 % y conteos 2/4 al instante
de generación visible; Inactiva
fuera del denominador activo;
sin reconstrucción histórica.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-071
Estado de cuenta
histórico
## M10
## RF-M10-05
D2; pago posterior a
fecha de corte
Generar antes y después del
pago; consultar oráculo en
ambas fechas
Abono posterior no reduce corte
anterior; saldos coinciden a
centavo.
## Reporte /
reconciliació
n
## Prioridad
## Alta

## 92
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-072
Contratos por
vencer
## M10
## RF-M10-06
D1; contratos a 30 y
31 días del fin
Generar con umbral 30;
comparar selección SQL
Incluye 30; excluye 31; fechas
y unidad legibles.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-073
Historial de
pagos
## M10
## RF-M10-07
D2; un Registrado y
un Anulado
Exportar historial de
inquilino por período
Incluye ambos con estado; total
neto excluye Anulado según
filtro documentado.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-074
Liquidación de
depósito
## M10
## RF-M10-08
## D1; 100 = 30
aplicado +50
devuelto +20
retenido
Generar liquidación; cotejar
movimientos
Muestra cuatro importes y
conceptos; no altera
consecutivo de recibos de renta.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-075
## Mantenimiento
por propiedad
## M10
## RF-M10-11
D1; gastos 25 USD y
500 NIO en
propiedades distintas
Filtrar propiedad y período;
exportar; cotejar SQL
Solo gastos del filtro; monedas
separadas; importes XLSX
numéricos.
## Reporte /
reconciliació
n
## Prioridad
## Alta
## CP-076
Tasas utilizadas
## M10
## RF-M10-13
D3; dos pagos con
autorizaciones
distintas
Generar historial; cambiar
referencia actual y regenerar
Copias históricas idénticas; no
sustituye tasa usada por
referencia actual.
## Reporte /
reconciliació
n
## Prioridad
## Alta
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.

## 93
La Tabla B5 contiene CP-077 a CP-094, correspondientes a conformidad aurora y usabilidad.
## Tabla B5
Conformidad Aurora y Usabilidad
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-077
Coherencia entre
vistas
## M11
## RNF-047
D0; inventario
PAN-01 a PAN-40
Abrir 40 pantallas en Claro
y Oscuro; revisar tokens
contra brief
80 observaciones sin tercer
tema; misma navegación,
tipografía y superficies opacas.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-078
Contraste de
texto y estados
## M11
## RNF-051
D0; normal, error,
éxito, aviso y
selección
Capturar ambos temas;
muestrear pares sólidos;
calcular luminancia sRGB
Texto >=4.5:1; señales y bordes
necesarios >=3:1 en cada
estado.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-079
Cobro por
teclado
## M11
## RNF-053
D2; sin ratón; ambos
roles autorizados a
cobro
En cada tema y modo de
efectos: Tab, Shift+Tab,
Enter; pagar 40 USD
Cuatro recorridos completados
sin trampa; importe y recibo
iguales.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-080
Foco en diálogos
## M11
## RNF-052
D0; pago sin
confirmar
Abrir confirmación, recorrer
Tab, cancelar con Escape y
volver
Foco visible, perímetro
equivalente 2 px y 3:1; vuelve
al control invocador.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-081
Selección en
TableView
## M11
## RNF-051
D1; 50 filas con
selección y fila de
error
Seleccionar con teclado;
perder foco; volver; alternar
tema
Selección distinguible con texto
legible y foco; no depende solo
del color.
## Aurora /
usabilidad
## Prioridad
## Alta

## 94
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-082
## Redimensionado
y escalado
## M11
## RNF-055
## D0; Windows 100
%, 125 %, 150 % y
## 200 %
Probar 1366×768 y
1920x1080 en ambos temas;
desplazar formularios
Cero importes cortados; foco
accesible; acciones disponibles
por desplazamiento.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-083
Decoración no
captura clics
## M11
## RN-38
D0; capa de luces
activa
## Inspeccionar
mouseTransparent; pulsar
20 controles cubiertos por
área decorativa
20 acciones alcanzan su control;
capa decorativa fuera del
recorrido de foco.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-084
Sin efectos
## M11
## RNF-056
D0; acceso, panel y
cobro
En ambos temas activar Sin
efectos; inspeccionar escena
y volver a activar
Cero luces o blur decorativos;
mismos controles y datos en
cuatro combinaciones.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-085
Recurso ausente
## M11
## RNF-055
D0; CSS del tema
presente; recurso
decorativo retirado
Arrancar y navegar; forzar
además ausencia de
preferencia
Fondo uniforme legible; Claro
si preferencia falta; ningún
bloqueo de operación.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-086
Reporte sin
aurora
## M11
## RNF-054
## D2; 10 PDF
(incluido recibo) y 9
XLSX, uno por cada
reporte distinto del
recibo.
Exportar 10 reportes en
cuatro combinaciones de
tema/efectos: recibo solo
PDF; otros nueve en PDF y
XLSX; comparar datos e
imágenes.
Por combinación: 10 PDF + 9
XLSX = 19 archivos. Total: 40
PDF + 36 XLSX = 76, sin
Aurora y con totales idénticos;
recibo sin XLSX.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-087
Cambio con
formulario
modificado
## M11
## RNF-049
D2; 20 entradas y
filtros editados
Alternar 30 veces por
sentido sin guardar; medir
desde acción a layout
Cero campos perdidos; foco y
filtros intactos; cada cambio
<=500 ms.
## Aurora /
usabilidad
## Prioridad
## Alta

## 95
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-088
Cambio con
tarea activa
## M11
## RNF-049
D1; H1 o H5 activo
con progreso
Alternar tema y efectos;
completar o cancelar tarea
Progreso continúa; cero
duplicados, cancelación intacta
y resultado financiero sin
cambio.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-089
Reinicio y
última
preferencia local
## M11
## RNF-050
D0; A Oscuro y B
## Claro
Alternar cuentas y reiniciar
diez veces por cuenta;
observar antes/después de
acceso
Antes última elección local;
después preferencia del UUID
de cada cuenta; 20 reinicios
correctos.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-090
Lectura local
fallida
## M11
## RNF-048
D0; valor ausente,
corrupto o acceso
denegado
Probar tres variantes con
ambos roles; abrir acceso y
autenticar
Seis inicios usan Claro;
controles operables; no concede
permisos administrativos.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-091
Guardado local
fallido
## M11
## RNF-058
D2; almacén local
sin escritura
Cambiar tema en pago sin
confirmar como cada rol;
continuar cobro
Conserva tema de sesión y
datos; un aviso no bloqueante
por fallo; cobro posible.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-092
Popups y
controles del
curso
## M11
## RNF-047
D0; DatePicker,
ComboBox,
ContextMenu y Alert
Abrir cada popup en ambos
temas; navegar con teclado
y cerrarlo
Ocho popups legibles; foco
visible; ningún panel con
colores del tema anterior.
## Aurora /
usabilidad
## Prioridad
## Alta

## 96
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-093
Diez recorridos
completos
## M11
## RNF-053
## D0/D1/D2/D3;
catálogo del
## Apéndice C
Ejecutar diez recorridos en
dos temas y dos modos de
efectos, dos roles donde
aplique Variantes
D15/D27/C01: estados
aplicables
carga/vacío/error/éxito;
formulario modificado con
Guardar/Descartar/Continua
r editando; conservar datos
válidos no secretos tras
rechazo; días o montos
configurables negativos.
40 variantes por rol aplicable;
cero bloqueos; registrar cada
variante con sufijo. Rangos
negativos cubiertos:
ERR-064/MSG-051, formulario
conservado; los demás errores
mantienen sus mensajes
específicos. Registrar
recuperación según la fase.
## Aurora /
usabilidad
## Prioridad
## Alta
## CP-094
Tema separado
de permisos
## M11
## RNF-054
D0; Propietario y
## Asistente
Alternar veinte cambios;
comparar permisos, DTO de
pago y totales por moneda
Cero cambio de permisos o
valores; ambos roles eligen
tema sin alterar M12.
## Aurora /
usabilidad
## Prioridad
## Alta
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.
La Tabla B6 contiene CP-095 a CP-100, correspondientes a despliegue y rendimiento.

## 97
## Tabla B6
Despliegue y Rendimiento
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-095
Respaldo y
restauración
## M12
## RF-M12-06
D4; BD 1 GiB y
adjuntos 1 GiB;
contraseña correcta,
incorrecta y etiqueta
GCM alterada.
Crear paquete AES-256-
GCM; verificar autenticidad
y descifrar antes de restaurar
en base aislada; cotejar
hashes. Repetir clave
incorrecta y GCM alterado.
Variantes D37: clientes
detenidos; paquete sin
claves ni credenciales;
restaurar aislado antes de
sustituir. Con recibos
posteriores al respaldo,
mantener mantenimiento
hasta conciliar sin reutilizar
números.
Respaldo ≤15 min; restauración
≤30 min; cero huellas
discordantes.
Clave/autenticidad inválida:
ERR-059/MSG-082, sin
reemplazo ni secretos en
evidencia. 15/30 min son metas
pendientes de medición en
entorno identificado; registrar
tiempos reales, versión y
huellas. Hash incorrecto:
ERR-055/MSG-079; no
sustituir instalación ante fallo.
## Despliegue /
rendimiento
## Prioridad
## Alta
## CP-096
## Despliegue
limpio de app-
image ZIP
## M12
## RNF-046
Windows 11 x64, 8
GiB RAM, SSD; sin
Java instalado
Extraer app-image ZIP;
crear BD con scripts; cargar
D0; iniciar
Arranque con runtime incluido;
versión y esquema correctos;
acceso y recibo disponibles.
## Despliegue /
rendimiento
## Prioridad
## Alta
## CP-097
## Esquema
incompatible
## M12
## RNF-028
D0; versión de
esquema anterior
Arrancar con
hbm2ddl.auto=validate;
comparar tablas
antes/después
Rechaza esquema incompatible
sin alterarlo; indica migración
requerida.
## Despliegue /
rendimiento
## Prioridad
## Alta

## 98
Caso y
## Escenario
Módulo y
## Vínculo
Precondición y
## Datos
ProcedimientoResultado Esperado y
## Criterio
Tipo y
## Prioridad
## CP-098
Rendimiento del
panel
## M11
## RNF-001
D4; 10000 cargos,
caché caliente
Abrir panel 30 veces;
ordenar tiempos
monotónicos
Tiempo en posición 29 <=2 s;
informe conserva los 30 valores
y entorno.
## Despliegue /
rendimiento
## Prioridad
## Alta
## CP-099
Carga de renta y
## UI
## M05
## RNF-003
D4; 500 contratos
equivalentes; 5 bases
reiniciadas
Ejecutar H1 cinco veces;
muestrear 20 pulsos UI por
lote
Cada lote <=60 s; demora UI
máxima <=200 ms; sin
duplicados.
## Despliegue /
rendimiento
## Prioridad
## Alta
## CP-100
Exportación de
10000 filas
## M10
## RNF-004
D4; 10000 filas;
SSD; sin otro lote
Exportar XLSX cinco veces
a rutas distintas; abrir y
contar filas
Cada exportación <=30 s;
10000 filas íntegras; cero
archivo parcial.
## Despliegue /
rendimiento
## Prioridad
## Media
Nota. Cada escenario parte de su fixture restaurado. Los pasos se ejecutan en el orden escrito; los resultados son esperados y no una
ejecución acreditada.

## 99
## Tabla B7
Equivalencias de Escenarios de Diseño e Identificadores de Ejecución
EscenarioFuenteCP equivalente y variante
ESC-01PRD 2026c, Tabla B2CP-001, CP-002, CP-003
ESC-02PRD 2026c, Tabla B2CP-017, CP-042
ESC-03PRD 2026c, Tabla B2CP-021, CP-045, CP-047, CP-099
ESC-04PRD 2026c, Tabla B2CP-026, CP-043
ESC-05PRD 2026c, Tabla B2CP-029/CP-030 (variantes con tasa BCN 36.6243), CP-066
ESC-06PRD 2026c, Tabla B2CP-032, CP-033
ESC-07PRD 2026c, Tabla B2CP-034, CP-035, CP-036
ESC-08PRD 2026c, Tabla B2CP-037, CP-038
ESC-09PRD 2026c, Tabla B2CP-053 a CP-066; CP-058 (variantes de banda 1–100)
ESC-10PRD 2026c, Tabla B2CP-071
ESC-11PRD 2026c, Tabla B2CP-100 (variante de cancelación), CP-050
ESC-12PRD 2026c, Tabla B2CP-051, CP-095, CP-049, CP-050
ESC-13PRD 2026c, Tabla B2CP-041 (10 hilos en 2 JVM), CP-042 (10 reversiones)
ESC-14PRD 2026c, Tabla B2CP-022 (variante con abono parcial), CP-046
ESC-15PRD 2026c, Tabla B2CP-015
ESC-16PRD 2026c, Tabla B2CP-016, CP-052
ESC-17PRD 2026c, Tabla B2CP-005 (variantes de permisos M02), CP-094
ESC-18PRD 2026c, Tabla B2CP-014
ESC-19PRD 2026c, Tabla B2CP-024

## 100
EscenarioFuenteCP equivalente y variante
ESC-20PRD 2026c, Tabla B2CP-095 (variantes de clave/autenticidad y huellas)
ESC-21PRD 2026c, Tabla B2CP-078, CP-079, CP-080, CP-081, CP-082
ESC-22PRD 2026c, Tabla B2CP-049, CP-050
ESC-23PRD 2026c, Tabla B2CP-007 (variantes JDBC de cuentas y parámetros append-only), CP-040
ESC-24PRD 2026c, Tabla B2CP-031 (100 variantes)
ESC-25PRD 2026c, Tabla B2CP-089
ESC-26PRD 2026c, Tabla B2CP-090, CP-091, CP-087
ESC-27PRD 2026c, Tabla B2CP-077 a CP-086, CP-092, CP-093, CP-094
PT-01TRD 2026d, Tabla 10CP-002 a CP-004
PT-02TRD 2026d, Tabla 10CP-005
PT-03TRD 2026d, Tabla 10CP-016, CP-021, CP-045, CP-052 (M04)
PT-04TRD 2026d, Tabla 10CP-041 y CP-042 (variantes D22)
PT-05TRD 2026d, Tabla 10CP-042 a CP-044, CP-048
PT-06TRD 2026d, Tabla 10CP-021, CP-022, CP-045 a CP-047, CP-099
PT-07TRD 2026d, Tabla 10CP-053 a CP-065 (CP-058 con variantes D23)
PT-08TRD 2026d, Tabla 10CP-067 a CP-076, CP-086, CP-100
PT-09TRD 2026d, Tabla 10CP-077 a CP-084, CP-087, CP-092 a CP-094
PT-10TRD 2026d, Tabla 10CP-089 a CP-091
PT-11TRD 2026d, Tabla 10CP-051, CP-095 (variantes D37)
PT-12TRD 2026d, Tabla 10CP-049, CP-050 (variantes por H)
PT-13TRD 2026d, Tabla 10CP-052, CP-098, CP-099; búsqueda/auditoría como variantes de CP-040
PT-14TRD 2026d, Tabla 10CP-015 a CP-040; CP-040 con variante D24

## 101
EscenarioFuenteCP equivalente y variante
PT-15TRD 2026d, Tabla 10CP-096 y CP-097
PT-16TRD 2026d, Tabla 10CP-007, CP-054, CP-066, CP-096 (variantes de estabilidad/cobertura)
PV-01Flujo 2026b, Tabla E1CP-001 a CP-003
PV-02Flujo 2026b, Tabla E1CP-005 (variantes)
PV-03Flujo 2026b, Tabla E1CP-084, CP-085, CP-087, CP-089 a CP-091
PV-04Flujo 2026b, Tabla E1CP-016, CP-017, CP-052
PV-05Flujo 2026b, Tabla E1CP-021, CP-045, CP-047, CP-099
PV-06Flujo 2026b, Tabla E1CP-022, CP-024, CP-046, CP-048 (variante H2/pago)
PV-07Flujo 2026b, Tabla E1CP-026 a CP-028
PV-08Flujo 2026b, Tabla E1CP-029 a CP-031, CP-066 (variantes)
PV-09Flujo 2026b, Tabla E1CP-041, CP-042, CP-032 (D22)
PV-10Flujo 2026b, Tabla E1CP-043, CP-044 (variantes)
PV-11Flujo 2026b, Tabla E1CP-032, CP-033
PV-12Flujo 2026b, Tabla E1CP-034 a CP-036, CP-040 (variante rollback)
PV-13Flujo 2026b, Tabla E1CP-053 a CP-066 (variante CP-058: banda)
PV-14Flujo 2026b, Tabla E1CP-067 a CP-076, CP-086, CP-100 (variantes)
PV-15Flujo 2026b, Tabla E1CP-049 a CP-051, CP-095 (variantes)
PV-16Flujo 2026b, Tabla E1CP-078 a CP-080, CP-093, CP-098
EV-01Brief 2026a, Tabla 27CP-093 (variante de lectura de cartera)
EV-02Brief 2026a, Tabla 27CP-093 (variante de navegación); CP-015, CP-017
EV-03Brief 2026a, Tabla 27CP-029 (variante con tasa BCN 36.62430000); CP-026
EV-04Brief 2026a, Tabla 27CP-015; CP-026 (variante de monto inválido)

## 102
EscenarioFuenteCP equivalente y variante
EV-05Brief 2026a, Tabla 27CP-079, CP-080, CP-092, CP-093
EV-06Brief 2026a, Tabla 27CP-084, CP-087, CP-088
EV-07Brief 2026a, Tabla 27CP-054, CP-058, CP-066
EV-08Brief 2026a, Tabla 27CP-021; CP-047 (variante: 50 contratos, cancelar al 30)
EV-09Brief 2026a, Tabla 27CP-078, CP-082
EV-10Brief 2026a, Tabla 27CP-005, CP-032, CP-086
Nota. ESC, PT, PV y EV identifican escenarios de diseño; la ejecución se registra exclusivamente con CP-001 a CP-100 y variantes,
sin crear identificadores CP adicionales. Cada equivalencia conserva los datos, pasos y umbrales de la fila fuente como variantes del
CP señalado. La tasa ficticia 36.5 del Plan se distingue de las variantes BCN 36.6243 de las fuentes. En PV-09, las anulaciones
confirmadas corresponden a CP-032; los rollbacks antes de commit, a CP-042. Las equivalencias no constituyen resultados
ejecutados.

## 103
## Apéndice C
Protocolo de Revisión Académica y Resultados Esperados
La revisión académica interna utilizará la versión etiquetada y una copia aislada de los
datos sintéticos antes de cada sesión. Steven facilitará la secuencia, Caleb coordinará la
observación, Alfredo comprobará los saldos y Aris verificará los archivos y la integración HTTP.
El operador será distinto del autor del componente cuando la disponibilidad lo permita dentro de
la capacidad programada. La Tabla C1 define preparación y cierre, y la Tabla C2 desarrolla diez
recorridos con criterios verificables. Los resultados son esperados y no acreditan sesiones
realizadas, acuerdos con terceros ni aprobación docente.
## Tabla C1
Preparación y Cierre de la Revisión
FaseResponsable y
## Momento
ProcedimientoResultado Esperado
PreparaciónSteven Cuadra; 25/11
y 30/11 antes del
ensayo.
Identificar etiqueta, revisión
Git, versiones y equipo; abrir
protocolo y registrar inicio.
Una base documental
consistente y una
versión identificada;
sin cliente externo.
DatosAlfredo Montalvan.Restaurar copia D0 a D3;
comprobar contador 1; fijar
fecha del caso y respaldo
previo.
Saldos y fechas
reproducibles;
conjuntos de volumen
separados del
escenario de defensa.
PresentaciónCaleb Tardencilla.Verificar 1366×768/1920x1080
y escalas 100/150/200; preparar
dos UUID y cuatro
combinaciones visuales.
Claro/Oscuro y efectos
activos/inactivos
disponibles;
preferencia por cuenta
aislada.
IntegraciónAris Gavarrete.Iniciar S1 loopback; revisar
fixtures y directorio de
exportación; ocultar secretos
externos.
Peticiones y errores
controlados; salidas
accesibles sin llave
real visible.

## 104
FaseResponsable y
## Momento
ProcedimientoResultado Esperado
EjecuciónOperador cruzado por
recorrido.
Seguir Tabla C2; conservar
capturas y consultas por
identificador de variante.
Resultado aprobado
solo si cada criterio
coincide; fallo genera
DEF con evidencia.
ClasificaciónCaleb y Steven.Distinguir fallo funcional,
entorno bloqueado y
observación editorial; asignar
severidad y TAR.
Defecto reproducible;
ningún caso bloqueado
contabilizado como
aprobado.
CierreLos cuatro
integrantes.
Guardar informe con
fecha/revisión, tiempos
medidos si existen, casos y
anexos; emitir candidato solo si
cumple Tabla 26.
Evidencia íntegra y
accesible; sin firmas ni
resultados inventados.
## Tabla C2
Diez Recorridos de Revisión
## Recorri
do
ObjetivoResponsable
y Datos
PasosConformidad Esperada
REC-01Acceso, cambio
de usuario y
preferencia
Caleb; opera
## Aris. D0.
Abrir sin preferencia;
elegir Oscuro; entrar
como A; salir; entrar
como B Claro; reiniciar
diez veces por cuenta.
Claro ante ausencia;
última local antes del
acceso; UUID individual
después. CP-001,
## CP-077, CP-089,
## CP-090.
REC-02CRUD y
validación de
catálogos
## Alfredo;
opera Caleb.
## D0.
Crear propiedad y
unidad; editar; buscar
inquilino; probar
duplicado y baja con
contrato activo.
UUID estable; error
localizable; baja lógica
autorizada; parámetros
SQL sin concatenación.
CP-008 a CP-014.
REC-03Contrato y
garantía
## Alfredo;
opera Steven.
## D1.
Crear Borrador; probar
mes mínimo; activar;
intentar contrato
solapado; revisar
Retenido y ocupación.
Una activación;
Vigente/Ocupada/Reteni
do consistentes; sin
movimiento de renta por
depósito. CP-015 a
## CP-020.

## 105
## Recorri
do
ObjetivoResponsable
y Datos
PasosConformidad Esperada
REC-04Generación
masiva y
cancelación
Steven; opera
Aris. D1 o
## D4.
Ejecutar H1; navegar
con lote activo; cancelar
entre contratos; repetir
período.
Cargos completos; cero
duplicados; progreso
visible; hilos fuera de
JavaFX. CP-021,
## CP-045, CP-047,
## CP-099.
REC-05Mora y carteraSteven; opera
## Alfredo. D1.
Fijar gracia; ejecutar H2
al límite y después;
repetir; consultar cartera
al corte.
Una mora por renta
elegible, sin mora sobre
Mora; corte y moneda
conservados. CP-022,
## CP-024, CP-068.
REC-06Cobro parcial
por teclado
Caleb; opera
## Steven.
## D2/D3.
Pagar 1460 NIO con
tasa ficticia autorizada
36.5; Tab/Enter; alternar
tema con campos
modificados.
Abono 40 USD y saldo
octubre 60; recibo único;
ningún campo o permiso
perdido. CP-026,
## CP-029, CP-079,
## CP-087.
REC-07Simultaneidad,
recuperación y
anulación
Steven; opera
## Alfredo. D2.
Enviar dos pagos y un
reintento con UUID;
cortar respuesta tras
commit; anular como
Asistente y luego
## Propietario.
Número único;
recuperación sin doble
emisión; Asistente
rechazado; anulado
conserva número.
## CP-005, CP-032, CP-041
a CP-044.
REC-08Liquidación y
mantenimiento
## Alfredo;
opera Caleb.
## D1.
Aplicar 30 de depósito,
devolver 50 y consultar
remanente; registrar
gasto 25 sin cargo
automático; terminar
según reglas.
100 = 30 + 50 + 20 antes
del cierre; no confundir
gasto con cargo; cierre no
deja fondos sin
resolución. CP-034 a
## CP-038.
REC-09Reportes y
## HTTP
Aris; opera
## Caleb.
## D2/D3/S1.
Exportar diez
contenidos; cotejar
SQL; simular éxito,
timeout y cuota;
observar referencia y
autorización separadas.
Tolerancia 0.00; archivos
sin aurora; caché
conserva origen; cobro
sin tasa autorizada
rechazado. CP-053 a
## CP-076, CP-086.

## 106
## Recorri
do
ObjetivoResponsable
y Datos
PasosConformidad Esperada
REC-10Respaldo,
preferencias
fallidas y cierre
Aris; opera
## Steven.
## D4/D0.
Denegar guardado local;
cambiar tema; verificar
autenticidad y descifrar
respaldo AES-256-
GCM antes de
restaurarlo aislado;
cerrar durante H1 y H5;
exceder 10 s con
barrera.
Un aviso no bloqueante;
hashes iguales;
temporales no
publicados; proceso
sigue abierto si trabajo
no termina. CP-049,
## CP-050, CP-091,
## CP-095.
Las variantes visuales tendrán sufijos CL-E, CL-S, OS-E y OS-S para Claro con efectos,
Claro sin efectos, Oscuro con efectos y Oscuro sin efectos. El operador añadirá el rol aplicable y
conservará el mismo conjunto sintético para comparar resultados sin variar saldos o permisos. El
registro de ejecución incluirá evidencia, conducta observada y dictamen solo después del ensayo.
Una falla impedirá aceptar el criterio afectado y originará un DEF con referencia al recorrido y
CP. El equipo repetirá la variante corregida y las dependencias financieras o visuales
relacionadas antes de cerrar la incidencia.

## 107
## Apéndice D
Lista de Verificación de Entrega
La entrega se revisará con una copia del paquete distinta del directorio de desarrollo para
detectar rutas o dependencias accidentales. Cada integrante comprobará los elementos de la
Tabla D1 antes de la sesión del 26 de noviembre y repetirá la revisión el 30 para el cierre del 1
de diciembre. El resultado esperado describe la condición de conformidad y no marca una
actividad como realizada. Un elemento que falle generará una incidencia con responsable y
efecto sobre la presentación. El manifiesto conservará revisión, archivos y huellas para comparar
ambas copias sin depender del nombre del directorio.
## Tabla D1
Verificación Propuesta del Paquete
ControlResponsabl
e
Acción de VerificaciónResultado Esperado
ENT-01StevenSeis documentos abren y
portada/autores/fecha coinciden.
Sin archivos dañados ni citas
de entregables no recibidos.
ENT-02ArisEtiqueta Git y manifiesto
corresponden al ejecutable.
SHA-256 coincide entre copia
principal y alterna.
ENT-03AlfredoMigraciones crean base vacía y
validate acepta esquema.
Sin ddl automático de
Hibernate ni tablas faltantes.
ENT-04CalebDatos y personas de demostración
tienen rótulo sintético.
No hay identificaciones,
teléfonos ni documentos
privados reales.
ENT-05StevenPropietario y Asistente tienen
acciones correctas.
Cero privilegio administrativo
otorgado por cambiar tema.
ENT-06CalebClaro y Oscuro disponibles en
acceso, ventana y popups.
Sin tercer tema; Claro como
respaldo; sin campos sobre
luces.
ENT-07CalebTeclado completa cobro y foco
regresa del diálogo.
Tab, Shift+Tab, Enter y
Escape sin trampa.
ENT-08StevenH1, H2, H3 y H5 dejan UI operable;
H6 coordina salida.
Demora UI dentro de meta;
aviso si cierre supera 10 s.

## 108
ControlResponsabl
e
Acción de VerificaciónResultado Esperado
ENT-09AlfredoNumeración e idempotencia
comprobadas entre dos JVM.
Sin duplicados ni saltos por
rollback.
ENT-10ArisDiez reportes y XLSX tabular abren
fuera del IDE.
Totales y corte coinciden con
consultas; sin fondo Aurora.
ENT-11ArisS1 inicia y fixtures cubren éxito,
caída y cuota.
HTTP simulado identificado;
secreto real ausente de
evidencia.
ENT-12AlfredoTasa informativa y autorización por
fecha separadas.
1460 NIO sintéticos abonan 40
USD con tasa ficticia 36.5.
ENT-13AlfredoRespaldo AES-256-GCM
autenticado, descifrado y restaurado
en base aislada.
Cero huella distinta; clave o
autenticidad inválida rechaza
sin reemplazo (MSG-082).
ENT-14ArisApp-image ZIP extraída y ejecutada
en equipo alterno sin JDK.
Runtime incluido; recursos y
fuentes disponibles.
ENT-15CalebPantalla y proyector probados con
escalado definido.
Importes y acciones visibles a
## 100/150/200 %.
ENT-16StevenManual y protocolo describen la
etiqueta de entrega.
Otro integrante repite REC-01
a REC-10.
ENT-17ArisLicencias y avisos corresponden al
inventario transitivo.
Cada binario tiene
versión/licencia identificadas.
ENT-18StevenGuion de veinte minutos ensayado
con relevo.
Cinco minutos por persona;
RA1 a RA5 presentes.
ENT-19CalebInforme CP informa aprobados,
fallidos y bloqueados.
Cero defecto crítico/alto Must
abierto para v0.9.0-rc.1.
ENT-20ArisDos copias independientes y acceso
del equipo.
Los cuatro pueden abrir
archivos antes de presentar.

## 109
## Apéndice E
Guion de Demostración
El guion usa el mismo contrato sintético C1 y el período señalado para mantener
continuidad entre expositores. Los veinte minutos son una duración propuesta de ensayo, con
cinco minutos por integrante y preguntas posteriores fuera del bloque cuando la sesión lo
permita. La Tabla E1 asigna acciones, transición y alternativa ante una falla concreta. El equipo
no afirmará resultados de pruebas que no figuren en su evidencia de versión. Aris conservará S1
disponible y Alfredo preparará una copia restaurable para que una falla de Internet no altere las
cifras de los casos de cobro.
## Tabla E1
Acciones y Transiciones de la Defensa
TiempoResponsab
le
AcciónTransición y
## Evidencia
## Alternativa
00:00 a
## 01:00
CalebPresentar problema de
diseño, modalidad
documental y datos
sintéticos.
«Usaremos C1 para
seguir contrato,
saldo y recibo».
Si falta proyección,
abrir copia local
con mismo
escalado.
01:00 a
## 03:00
CalebAcceder; mostrar
Claro/Oscuro, selector y
última preferencia;
operar barra lateral.
Señalar controles
FXML, eventos y
superficies
uniformes. RA1.
Sin efectos
conserva ambos
temas y
navegación.
03:00 a
## 05:00
CalebAbrir inquilino y
Borrador; validar datos
con teclado; mostrar
diálogo y foco.
«Alfredo continuará
con la persistencia
del mismo
contrato».
Usar copia D1
preparada si el
ensayo previo
alteró el estado.
05:00 a
## 07:00
AlfredoMostrar conexión
pgJDBC y CRUD JDBC
de seguridad o
parámetros con consulta
parametrizada.
Explicar permiso de
servicio y validación
## SQL. RA2.
Usar evidencia de
consulta
identificada si no
cabe abrir editor.

## 110
TiempoResponsab
le
AcciónTransición y
## Evidencia
## Alternativa
07:00 a
## 10:00
AlfredoMostrar entidad
Hibernate, @Version,
activación y garantía;
consultar saldo.
«Steven generará el
período y registrará
el pago sobre C1».
Si falta tiempo,
mostrar mapeo y
ejecutar consulta de
contrato sin repetir
alta.
10:00 a
## 12:00
StevenEjecutar H1 con
progreso; cancelar lote
preparado; explicar H2 y
no mora sobre Mora.
Mostrar que
navegación continúa
mientras trabaja H1.
## RA3.
Usar lote pequeño;
no cambiar criterio
de consistencia.
12:00 a
## 14:00
StevenCobrar 1460 NIO a tasa
ficticia autorizada 36.5;
mostrar 40 USD
aplicados y 60 de saldo.
## Explicar H4,
contador por fila,
UUID y commit
único.
Si falla Internet el
pago usa
autorización
sintética por fecha,
no tasa comercial.
14:00 a
## 15:00
StevenMostrar evidencia de dos
JVM y H6 sin forzar
terminación de commit.
«Aris abrirá el
recibo confirmado y
el estado de cuenta».
Si hay tarea activa,
conservar ventana
y mostrar aviso de
10 s; no matar
proceso.
15:00 a
## 17:00
ArisAbrir recibo, estado de
cuenta y XLSX;
comparar total con
consulta oráculo.
## Explicar
JasperReports con
DTO e historia por
corte. RA4.
Abrir exportación
de la etiqueta ya
preparada y aclarar
su fecha.
17:00 a
## 19:00
ArisEjecutar GET contra S1;
mostrar JSON, NIO,
fuente, fecha; simular
cuota y conservar caché.
Distinguir referencia
orientativa,
autorización y
publicación BCN.
## RA5.
El simulador se
identifica; no se
afirma una consulta
comercial real.
19:00 a
## 20:00
ArisMostrar versión, app-
image ZIP, respaldo
cifrado y ubicación de
evidencia; cerrar
recorrido.
Dejar visible lista
RA y archivos
disponibles para
preguntas.
Si una función
falló, declarar
requisito y alcance
de la limitación
observada.

## 111
## Apéndice F
Fuentes y Trazabilidad Documental
La Tabla F1 identifica las fuentes efectivamente consultadas para las decisiones externas y la coherencia con los cinco
documentos suministrados. La consulta corresponde al 23 de octubre de 2026 y el campo de fecha conserva s. f. cuando la obra no
presenta una fecha verificable de publicación. Los títulos de versión no se utilizan para deducir un año de edición. La fuente científica
se limita a su ámbito geográfico y las especificaciones de fabricantes respaldan únicamente sus propios productos. La revisión
documental no incluye acceso autenticado al proveedor, ejecución de Latch ni consulta del syllabus institucional original.
## Tabla F1
Afirmaciones y Fuentes Consultadas
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Maven 3.9.16 publicado y
mantenido; JDK mínimo
## 8.
## Apache Software
## Foundation
Documentación oficial
Downloading Apache Maven and Maven
## Daemon
Publicación: s. f.
https://maven.apache.org/download.cgi
## Previous Stable 3.9.x Release.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 112
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Licencia de herramientas
Apache y conservación de
avisos.
## Apache Software
## Foundation
Licencia oficial
Apache license, version 2.0
## Publicación: 2004
https://www.apache.org/licenses/LICENSE
## -2.0
Terms and Conditions;
distribución.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Bearer sin llave en URL;
custodia externa.
AYR Tech (Pty) Ltd.
Contrato y condiciones
del proveedor
API authentication notes
Publicación: s. f.-a
https://www.exchangerate-api.com/docs/au
thentication
## Token Bearer Authentication.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Plan Free: USD 0, 1500
consultas mensuales y
actualización diaria.
AYR Tech (Pty) Ltd.
Contrato y condiciones
del proveedor
Exchange rate API
Publicación: s. f.-b
https://www.exchangerate-api.com/
## Plans & Pricing; Free.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Contrato JSON con result,
base_code,
conversion_rates y
timestamps.
AYR Tech (Pty) Ltd.
Contrato y condiciones
del proveedor
Standard requests
Publicación: s. f.-c
https://www.exchangerate-api.com/docs/st
andard-requests
## Usage Guide; Error Responses.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 113
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Soporte de NIO.AYR Tech (Pty) Ltd.
Contrato y condiciones
del proveedor
Supported currency codes and countries
Publicación: s. f.-d
https://www.exchangerate-api.com/docs/su
pported-currencies
Catálogo; NIO / Nicaragua.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Caché interna, límites de
redistribución y uso
informativo.
AYR Tech (Pty) Ltd.
Contrato y condiciones
del proveedor
Terms of service
Publicación: s. f.-e
https://www.exchangerate-api.com/terms
## License; Disclaimer; Data Caching
## Policy; Free Plans.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Estrategias de pequeños
arrendadores para evitar
desalojos; estudio situado
en Filadelfia, sin
validación local de Latch.
Balzarini, J., y Boyd,
## M. L.
Artículo científico
Working with them: Small-scale landlord
strategies for avoiding evictions
## Publicación: 2021
## Housing Policy Debate, 31(3–5), 425–445
https://doi.org/10.1080/10511482.2020.18
## 00779
Antecedente citado en PRD/TRD
(Cuadra et al., 2026c, 2026d).
Versión en línea de 2020; la cita
bibliográfica usa el volumen de
- Consulta documental: 23 de
octubre de 2026. No acredita
resultados en Managua.

## 114
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Referencia oficial 2026: 0
% y 36.6243 NIO/USD,
distinta de la comercial.
Banco Central de
## Nicaragua
## Publicación
institucional
BCN informa que la tasa de deslizamiento
del tipo de cambio del córdoba respecto al
dólar de los Estados Unidos de América
continuará en cero por ciento (0%) anual
en el año 2026
Publicación: 20 de octubre de 2025
https://www.bcn.gob.ni/divulgacion-
prensa/bcn-informa-que-la-tasa-de-
deslizamiento-del-tipo-de-cambio-del-
cordoba-respecto
Primer párrafo del comunicado.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Antecedente documental:
decisiones de propietarios
y operaciones; 196
artículos, ámbito
principalmente
estadounidense.
## Cook, T. M.,
## Rongerude, J.,
Mueller, E. y
## Kuhlmann, D.
Artículo científico
revisado por pares
Providing rental housing: A systematic
literature review of residential rental
property owner decision making
## Publicación: 2024
https://doi.org/10.1177/088541222412395
## 71
## Abstract; Introduction; Methods.
Texto editorial consultado.
Consulta: 23 de octubre de 2026
Confiabilidad: Artículo con
autoría, revista editorial y DOI;
método delimitado y ámbito
geográfico explícito.
PAN-01 a PAN-40,
tokens, temas, teclado y
mensajes.
## Cuadra, S., Montalvan,
A., Tardencilla, C. y
## Gavarrete, A.
Documento académico
suministrado
Brief de diseño de interfaz y experiencia
de usuario para Latch
## Publicación: 2026a
Latch_Brief_UIUX_APA7_v3.pdf
Sistema de Diseño; Paleta
## Semántica; Preferencias.
Consulta: 23 de octubre de 2026
Confiabilidad: Fuente primaria del
proyecto, recibida y consultada;
especifica decisiones e
identificadores.

## 115
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
FLU-01 a FLU-49;
actualización de paleta,
activación y cierre.
## Cuadra, S., Montalvan,
A., Tardencilla, C. y
## Gavarrete, A.
Documento académico
suministrado
Documento de flujo de la aplicación de
## Latch
## Publicación: 2026b
Latch_App_Flow_APA7_v3.pdf
Supuestos y Decisiones; FLU-06,
FLU-24 a FLU-28 y FLU-48.
Consulta: 23 de octubre de 2026
Confiabilidad: Fuente primaria del
proyecto, recibida y consultada;
especifica decisiones e
identificadores.
RF-Mxx-nn, RN-01 a
RN-38 y RNF; prioridades
y umbrales.
## Cuadra, S., Montalvan,
A., Tardencilla, C. y
## Gavarrete, A.
Documento académico
suministrado
Documento de requisitos del producto de
## Latch
## Publicación: 2026c
Latch_PRD_APA7_v3.pdf
Reglas de Negocio; Requisitos
Funcionales y No Funcionales.
Consulta: 23 de octubre de 2026
Confiabilidad: Fuente primaria del
proyecto, recibida y consultada;
especifica decisiones e
identificadores.
Versiones, arquitectura,
hilos y metas de
desempeño.
## Cuadra, S., Montalvan,
A., Tardencilla, C. y
## Gavarrete, A.
Documento académico
suministrado
Documento de requisitos técnicos de Latch
## Publicación: 2026d
Latch_TRD_APA7_v3.pdf
## Componentes Fijados; Persistencia;
## Concurrencia; Integración;
## Calidad.
Consulta: 23 de octubre de 2026
Confiabilidad: Fuente primaria del
proyecto, recibida y consultada;
especifica decisiones e
identificadores.

## 116
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Contador, conexión
compartida, corte y
ecuaciones financieras.
## Cuadra, S., Montalvan,
A., Tardencilla, C. y
## Gavarrete, A.
Documento académico
suministrado
Esquema de base de datos y servicios de
## Latch
## Publicación: 2026e
Latch_Backend_Schema_APA7_v3.pdf
Consecutivo sin Saltos; Orden de
## Bloqueo; Mapeo Objeto-
## Relacional.
Consulta: 23 de octubre de 2026
Confiabilidad: Fuente primaria del
proyecto, recibida y consultada;
especifica decisiones e
identificadores.
## Temurin 21.0.12.1+1
conservado; distribución
de la familia 21.
## Eclipse Adoptium
Documentación oficial
Temurin support
Publicación: s. f.
https://adoptium.net/support
Release Roadmap; fila Java 21.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Licencia GPLv2 incluida
en OpenJDK.
## Free Software
## Foundation
Texto de licencia en
repositorio oficial
The GNU general public license version 2
## Publicación: 1991
https://raw.githubusercontent.com/openjdk
/jdk21u/master/LICENSE
Terms and Conditions; versión 2,
junio de 1991.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Protección, revisión y
comprobaciones; Free en
repositorios públicos.
GitHub
Documentación oficial
About protected branches
Publicación: s. f.
https://docs.github.com/en/repositories/con
figuring-branches-and-merges-in-your-
repository/managing-protected-branches/
about-protected-branches
Who can use this feature?; About
branch protection settings.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 117
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
JavaFX 21.0.13; JDK
mínimo 17 y Windows
x64.
## Gluon
Documentación oficial
JavaFX
Publicación: s. f.-a
https://gluonhq.com/products/javafx/
Roadmap; Supported platforms;
## Downloads.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
## Scene Builder 21.0.0
publicado; usa JDK y
JavaFX 21.
## Gluon
Lanzamiento oficial
archivado
## Release 21.0.0
Publicación: s. f.-b
https://github.com/gluonhq/scenebuilder/re
leases/tag/21.0.0
Changelog; Use JDK and JavaFX
## 21.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Herramienta FXML y
licencia BSD del
producto.
## Gluon
Documentación oficial
## Scene Builder
Publicación: s. f.-c
https://gluonhq.com/products/scene-
builder/
## Integrated; Supported; Download.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Gson 2.13.2 publicado;
conservar cliente JSON
del TRD.
## Google
Lanzamiento del
proyecto oficial
## Gson 2.13.2
Publicación: s. f.-a
https://github.com/google/gson/releases/ta
g/gson-parent-2.13.2
## Release; What’s Changed.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 118
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Gson 2.13.2 bajo Apache
## 2.0.
## Google
Licencia oficial
etiquetada
## License
Publicación: s. f.-b
https://raw.githubusercontent.com/google/
gson/gson-parent-2.13.2/LICENSE
Apache License; versión 2.0;
distribución.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
7.4.6.Final existe; serie
admite Java 21 y Jakarta
## Persistence 3.2; Apache
## 2.0.
## Hibernate
Documentación oficial
7.4 series
Publicación: s. f.-a
https://hibernate.org/orm/releases/7.4/
Compatibility; Releases in this
series; 7.4.6.Final.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Sesión por caso de uso,
transacciones, versiones y
acceso JDBC.
## Hibernate
Documentación oficial
Hibernate ORM user guide
Publicación: s. f.-b
https://docs.hibernate.org/orm/7.4/userguid
e/html_single/
## Persistence Contexts; Transactions;
JDBC Work. Manual de serie 7.4.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Bytecode Java 8 y
extensión Hibernate
6.3.1.Final; integrar DTO
sin esa extensión.
## Jaspersoft
Código oficial
etiquetado
JasperReports Library artifacts POM 7.0.8
Publicación: s. f.
https://raw.githubusercontent.com/Jasperso
ft/jasperreports/7.0.8/pom-parent.xml
properties: revision,
source.version, target.version,
hibernate.version.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 119
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
## Library 7.0.8
## (05/08/2026), Studio 7.0.8
(14/08/2026), LGPLv3 y
licencia de tipo BSD.
## Jaspersoft
Documentación oficial
Jaspersoft community edition
## Publicación: 2026
https://community.jaspersoft.com/downloa
d-jaspersoft/community-edition/
JasperReports Library; Jaspersoft
Studio; licencias y lanzamientos.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Código Community bajo
Apache 2.0; no inferir
licencia Ultimate.
JetBrains
Licencia oficial
IntelliJ IDEA Community Edition license
Publicación: s. f.-a
https://github.com/JetBrains/intellij-
community/blob/master/LICENSE.txt
Texto de licencia.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
## Edición 2025.2
documentada; conservar
línea del TRD.
JetBrains
Documentación oficial
What’s new in IntelliJ IDEA 2025.2
Publicación: s. f.-b
https://www.jetbrains.com/idea/whatsnew/
## 2025-2/
## Versión 2025.2.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Procedencia visual:
fondos orgánicos
difuminados; no evidencia
de eficacia.
## Malewicz, M.
Publicación original de
diseño
Aurora UI: New visual trend for 2021
Publicación: s. f.
https://hype4.academy/articles/design/auro
ra-ui-new-visual-trend-for-2021
Autor, título y entradilla pública.
Alcance: autor, título y entradilla
pública citados en Cuadra et al.
(2026a), sistema visual.
Consulta: 23 de octubre de 2026
Confiabilidad: Autor identificado y
entradilla pública; excepción
admitida para procedencia visual,
sin lectura de suscripción.

## 120
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Desenfoque nativo
estático, separado de
lectura.
OpenJFX
Documentación oficial
GaussianBlur
Publicación: s. f.-a
https://openjfx.io/javadoc/21/javafx.graphi
cs/javafx/scene/effect/GaussianBlur.html
Class description; radius property.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
GPL con Classpath
Exception; entorno nativo
JavaFX.
OpenJFX
Documentación oficial
JavaFX
Publicación: s. f.-b
https://openjfx.io/
Develop; licencia del proyecto.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Hojas de estilo nativas y
estados de controles
JavaFX.
OpenJFX
Documentación oficial
JavaFX CSS reference guide
Publicación: s. f.-c
https://openjfx.io/javadoc/21/javafx.graphi
cs/javafx/scene/doc-files/cssref.html
CSS and the JavaFX Scene Graph;
## Controls.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Luces estáticas nativas sin
biblioteca web.
OpenJFX
Documentación oficial
RadialGradient
Publicación: s. f.-d
https://openjfx.io/javadoc/21/javafx.graphi
cs/javafx/scene/paint/RadialGradient.html
Class description; constructor.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 121
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Cliente reutilizable,
sendAsync, timeout y
cierre de recursos.
## Oracle
Documentación oficial
HttpClient
Publicación: s. f.-a
https://docs.oracle.com/en/java/javase/21/d
ocs/api/java.net.http/java/net/http/
HttpClient.html
Class description; sendAsync;
shutdown.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Excepciones y
combinación de módulos
OpenJDK.
## Oracle
Licencia en repositorio
oficial
OpenJDK assembly exception
Publicación: s. f.-b
https://raw.githubusercontent.com/openjdk
/jdk21u/master/ASSEMBLY_EXCEPTIO
## N
Texto íntegro, con referencia a
## Classpath Exception.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
jpackage genera una app-
image con runtime para
distribuirla como ZIP en
## Windows.
## Oracle
Documentación oficial
Packaging overview
Publicación: s. f.-c
https://docs.oracle.com/en/java/javase/21/j
package/packaging-overview.html
Packaging Pre-Reqs; Java Runtime
## Requirements.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
pgJDBC 42.7.14 y
requerimiento Java 8 o
posterior.
PostgreSQL JDBC
## Driver
Documentación oficial
## Download
Publicación: s. f.-a
https://jdbc.postgresql.org/download/
## Latest Versions; Java 8.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 122
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
BSD-2-Clause y
reproducción de avisos.
PostgreSQL JDBC
## Driver
Licencia oficial
## License
Publicación: s. f.-b
https://jdbc.postgresql.org/license/
BSD 2-clause Simplified License.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Base de incrementos,
eventos y definición de
terminado; adaptación
académica explícita.
Schwaber, K. y
## Sutherland, J.
Guía original
The Scrum guide
## Publicación: 2020
https://scrumguides.org/scrum-guide.html
## Scrum Events; Scrum Artifacts;
Definition of Done.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Bloqueos persistentes y
orden de adquisición.
The PostgreSQL
## Global Development
## Group
Documentación oficial
Explicit locking
Publicación: s. f.-a
https://www.postgresql.org/docs/18/explici
t-locking.html
13.3.2 Row-Level Locks; 13.3.4
## Deadlocks.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
PostgreSQL License; uso
y distribución sin cargo de
licencia.
The PostgreSQL
## Global Development
## Group
Documentación oficial
## License
Publicación: s. f.-b
https://www.postgresql.org/about/licence/
Texto de licencia.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.

## 123
Afirmación o DecisiónAutor y Tipo de
## Fuente
Título Fecha y URL o ArchivoSección Consulta y Motivo de
## Confiabilidad
Exportación consistente
de BD; adjuntos requieren
coordinación adicional de
## Latch.
The PostgreSQL
## Global Development
## Group
Documentación oficial
pg_dump
Publicación: s. f.-c
https://www.postgresql.org/docs/18/app-
pgdump.html
## Description.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
nextval no revierte
consumo; no sirve como
contador comercial sin
saltos.
The PostgreSQL
## Global Development
## Group
Documentación oficial
Sequence manipulation functions
Publicación: s. f.-d
https://www.postgresql.org/docs/18/functi
ons-sequence.html
## 9.17; Caution.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
PostgreSQL 18.6
soportado; fin de rama el
## 14/11/2030.
The PostgreSQL
## Global Development
## Group
Documentación oficial
Versioning policy
Publicación: s. f.-e
https://www.postgresql.org/support/versio
ning/
Releases; fila 18.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Umbrales de contraste y
referencia de teclado/foco,
adoptados como metas de
escritorio.
## World Wide Web
## Consortium
## Recomendación
técnica W3C
Web content accessibility guidelines
## (WCAG) 2.2
Publicación: 12 de diciembre de 2024
https://www.w3.org/TR/2024/REC-
## WCAG22-20241212/
1.4.3, 1.4.11, 2.1.1, 2.4.7 y 2.4.13.
Consulta: 23 de octubre de 2026
## Confiabilidad: Organización
responsable del producto o
publicación; fuente primaria para el
dato específico.
Nota. Las fuentes locales se identifican por nombre de PDF y autoría del conjunto. El resumen académico del encargo no es una
consulta del syllabus original. Las referencias sin fecha no toman el año del copyright del sitio.

## 124
## Apéndice G
Registro de Decisiones y Conflictos
Este registro conserva los identificadores citados en los documentos. D02, D07, D21 y
D52 mantienen su contenido previamente documentado. Las definiciones de D14, D15, D19,
D22, D23, D24, D27, D37, D44, D45 y D47 y la resolución de C01 fueron aprobadas
expresamente el 25 de octubre de 2026 como decisiones actuales, sin atribuirles acuerdos
históricos. Su aprobación define el diseño y sus criterios de aceptación; no acredita
implementación ni ejecución de pruebas.
D02. Nombres de negocio completos en español sin tildes; clases en UpperCamelCase,
métodos y campos en lowerCamelCase y constantes en UPPER_SNAKE_CASE, con sufijos de
rol técnicos en inglés. Uso: TRD, convenciones de código.
D07. Un Controller y un FXML por cada uno de los 40 PAN del Brief; nombres
españoles del catálogo, UpperCamelCase para clases y kebab-case para FXML. Uso: TRD,
convenciones y Apéndice B.
D14. Contrato visual y tokens. El Brief es la fuente canónica de colores, tipografía,
espaciado, dimensiones y componentes compartidos. Las pantallas utilizan los tokens y servicios
de tema definidos, con sus variantes de tema, escala y efectos simplificados. Toda excepción
visual se justifica y registra. La revisión de la interfaz se realiza contra esta especificación
común.
D15. Pantallas, navegación y conservación de datos. Cada PAN define entrada, acciones,
salida y los estados aplicables de carga, vacío, error y éxito. Al abandonar un formulario
modificado se ofrecen Guardar, Descartar o Continuar editando. Una validación fallida conserva

## 125
los datos válidos no secretos. Los casos existentes del Plan reflejan estos recorridos mediante
variantes cuando corresponda, sin aumentar los 100 CP.
D19. Autorización por capacidades. La matriz de permisos determina las acciones de
Propietario y Asistente. Cada servicio comprueba la autorización antes de escribir y vuelve a
validarla cuando una tarea diferida empieza a operar. Ocultar o deshabilitar un botón ayuda a la
interfaz; el control efectivo se realiza en el servicio. La denegación conserva ERR-009 /
MSG-062 y las excepciones expresas de la matriz vigente.
D21. Reintento transaccional solo ante 40P01 o 40001, conservando el UUID, con
Session nueva y máximo de tres intentos totales. Uso: política de concurrencia del TRD y
dependencias de integración del PLA.
D22. Numeración transaccional y concurrencia. La reserva del número, el pago, sus
aplicaciones, el recibo y la auditoría pertenecen a la misma transacción, con una Session y una
conexión. El rollback anterior al commit no consume número; la anulación posterior conserva
número e historial. Se separan dos variantes de aceptación con diez emisores en dos JVM: a) 100
pagos confirmados y otras diez solicitudes con rollback dejan 100 recibos y
siguiente_numero=101 al comenzar en 1; b) 100 pagos confirmados y diez anulaciones
posteriores dejan 100 recibos, 90 pagos Registrado y 10 Anulado, con siguiente_numero=101. Se
mantiene el contador transaccional; nextval no sustituye esta política. El máximo de tres sesiones
LAN limita clientes de aplicación, no hilos de prueba. Las variantes se integran en CP-041,
CP-042 y CP-032 sin crear CP adicionales.
D23. Consulta y validación de la referencia comercial. Se aprueba el protocolo completo:
JSON válido, base USD, NIO válido, fechas coherentes y banda inclusiva 1–100 NIO/USD. Una
respuesta inválida o fuera de banda conserva la referencia anterior y registra rechazo o

## 126
cuarentena, según corresponda. La conexión dispone de 5 s y la operación de 10 s; se admite una
consulta diaria y un único reintento después de 60 s para fallos transitorios de red o 5xx. HTTP
408 se trata como timeout transitorio; HTTP 429 respeta Retry-After y la política de cuota. La
referencia comercial nunca autoriza pagos automáticamente. Conforme a DC-15, una tasa
manual fuera de banda exige revisión y confirmación del Propietario, sin imponer esa banda
como restricción de BD. Los escenarios se cubren mediante los CP existentes, incluidas las
variantes de CP-058.
D24. Desfase del reloj y fecha financiera. El instante operativo financiero procede de
now() de la BD. Si la diferencia absoluta con el reloj local es mayor de cinco minutos, se
bloquean nuevas operaciones financieras con ERR-057 / MSG-080 hasta corregirla. Conforme a
DC-01, pago.fecha_pago contiene la fecha efectiva ingresada y pago.creado_en el instante de
BD, cuya fecha operativa se presenta en America/Managua. El control del reloj no sustituye la
validación de la fecha efectiva ni interrumpe una transacción financiera ya iniciada. Su
aceptación se incorpora como variante de CP-040.
D27. Mensajes y recuperación según la fase. El Brief es la fuente del texto literal de los
MSG; FLU define situación y recuperación; TRD relaciona las excepciones por escenario. Un
ERR puede aparecer en varias excepciones si conserva su significado. La respuesta distingue
validación rechazada, rollback confirmado, cancelación normal, commit incierto y fallo posterior
al commit. Cada caso ofrece una acción compatible con lo ocurrido y conserva los datos
necesarios para continuar. Los casos asociados se sincronizan mediante variantes de los CP
existentes.
D37. Respaldo, restauración e integridad. Se aprueba el protocolo completo: el respaldo
obtiene exclusión de escrituras, espera las ya iniciadas y mantiene una copia coherente de base,

## 127
adjuntos y manifiesto. La restauración requiere clientes detenidos, validación del paquete y
restauración aislada antes de sustituir la instalación. Se comprueban hashes y autenticidad GCM;
el paquete excluye credenciales y claves. Si existen recibos posteriores al respaldo, se mantiene
el mantenimiento hasta conciliar, sin reutilizar ni renumerar consecutivos. Los 15 min de
respaldo y 30 min de recuperación con base y adjuntos de 1 GiB cada uno son metas de
aceptación pendientes de medición en un entorno identificado. Las variantes se integran en
CP-051 y CP-095; su definición no acredita ejecución ni resultados.
D44. Configuración, versiones y entregables. Cada incremento identifica su versión,
fuentes, esquema compatible, documentos, respaldo y huellas de entrega. Todo cambio de
alcance o configuración tiene un registro con motivo, impacto y responsable. Esta decisión
regula la reproducción de los incrementos y la conservación de sus entregables.
D45. Revisión cruzada, riesgos y coordinación académica. Cada incremento tiene
revisión cruzada, observaciones identificables y seguimiento de riesgos. Las observaciones se
clasifican como defecto, aclaración o cambio de alcance, con responsable y resolución. La
evidencia académica describe lo realmente disponible: una actividad programada o una revisión
documental no acreditan una prueba ejecutada.
D47. Trazabilidad, aceptación y cierre. El cierre relaciona requisitos, tareas, casos,
resultados y evidencias RA1–RA5. Los criterios obligatorios cuentan con evidencia; toda
excepción requiere justificación y aceptación expresa. Se distingue entrega documental,
implementación y validación del producto. Los pendientes aceptados quedan identificados junto
con las versiones y huellas entregadas.

## 128
D52. Cerrada. Cada documento conserva sus propias fechas de consulta: el PRD
distingue fuentes externas del 22 de octubre de 2026 y fuentes internas del 23; este Plan declara
el 23 de octubre de 2026. No se unifican fechas entre documentos.
C01. Resuelto por decisión aprobada el 25 de octubre de 2026. Se incorporan seis ERR y
seis MSG: el catálogo pasa a 66 ERR y 89 MSG por excepción expresa a DC-17; los demás
inventarios permanecen vigentes. La cancelación voluntaria es un resultado normal sin ERR.
Estas definiciones son actuales y no se atribuyen a acuerdos históricos.
CargoIncompatibleException — ERR-061 / MSG-084. Se intenta aplicar garantía a un
cargo inexistente, anulado o perteneciente a otro contrato. Un importe superior al saldo conserva
su error específico. Se rechaza la aplicación y se conserva la propuesta para corregirla. Texto de
MSG-084: «El cargo no es válido para esta aplicación de garantía. Seleccione un cargo del
mismo contrato que no esté anulado.».
GarantiaPendienteException — ERR-062 / MSG-085. Se intenta terminar el contrato sin
liquidación completa y autorizada de la garantía: quedan decisiones o movimientos pendientes, o
un remanente sin resolver. Se conserva el bloqueo del cierre; la deuda residual puede mantenerse
después de terminar. Se respetan las excepciones autorizadas de garantía inicial cero sin
movimientos de DC-08. Texto de MSG-085: «La garantía tiene una liquidación pendiente.
Complete las aplicaciones y la devolución antes de terminar el contrato.».
IntegridadException — ERR-063 / MSG-086. La comprobación anterior al commit
detecta una inconsistencia entre pago, aplicaciones, movimientos de garantía o recibo. Se
revierte toda la operación y se conserva la causa técnica. MSG-086 se muestra únicamente con
rollback confirmado: «La operación se revirtió por una inconsistencia de datos. No se guardaron
cambios. Solicite revisión antes de reintentar.». El hash incorrecto de respaldo conserva

## 129
ERR-055 / MSG-079; autenticidad o contraseña inválida, ERR-059 / MSG-082. Un commit
incierto conserva ERR-031 o ERR-054 y MSG-064; no se afirma ausencia de cambios sin
certeza.
OperacionCanceladaException — sin ERR. H1 conserva literalmente MSG-059: «Se
solicitó cancelar. El contrato en curso terminará antes de detener el lote». La transacción en
curso termina y el lote se detiene antes de comenzar otro contrato; permanecen los contratos
confirmados y se identifican los pendientes en el resumen. H5 utiliza MSG-089: «La operación
se canceló antes de publicar el archivo. Puede volver a generarlo con los mismos parámetros.».
La cancelación de H5 es cooperativa durante consulta, llenado y exportación. Se
confirma cuando el trabajo termina y limpia sus temporales. La publicación final resuelve la
carrera entre cancelar y publicar: si el archivo ya se publicó, se informa el resultado real. Se
conserva el archivo de destino previo mientras no exista publicación exitosa y se mantienen todo
pago o anulación ya confirmados.
ParametroInvalidoException — ERR-064 / MSG-051 existente. Se intenta guardar un
valor negativo en días o montos configurables cubiertos por MSG-051. Se conserva el formulario
para corregir el rango; las demás validaciones mantienen sus mensajes específicos. Texto de
MSG-051: «Revise los valores. Los días y montos configurados no pueden ser negativos».
PropiedadIncompatibleException — ERR-065 / MSG-087. Al registrar un gasto asociado
a una solicitud de mantenimiento, la unidad de esa solicitud pertenece a una propiedad distinta de
la seleccionada. Se rechaza el registro y se permite corregir la selección. Texto de MSG-087:
«La solicitud de mantenimiento pertenece a otra propiedad. Revise la propiedad o la solicitud
seleccionada.».

## 130
TransicionInvalidaException — ERR-066 / MSG-088. Solo se admiten Abierta a En
proceso o Cancelada, y En proceso a Resuelta o Cancelada. Resuelta y Cancelada son terminales.
Repetir una solicitud hacia el estado actual devuelve ese estado sin duplicar cambios ni eventos.
Cualquier otra transición se rechaza conservando el estado anterior. Texto de MSG-088: «El
cambio de estado solicitado no está permitido. Se conserva el estado actual.».