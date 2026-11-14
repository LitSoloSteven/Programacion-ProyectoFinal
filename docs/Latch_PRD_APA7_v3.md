

## 1
Documento de Requisitos del Producto de Latch
## Steven Cuadra
## Alfredo Montalvan
## Caleb Tardencilla
## Aris Gavarrete
Facultad de Ingeniería y Arquitectura, Universidad Americana (UAM)
CNU0525: Programación de Aplicaciones de Escritorio, Grupo 2
## José A. Durán García
26 de noviembre de 2026

## 2
## Resumen
Latch centraliza la administración de alquileres por unidad mediante una aplicación de escritorio
con contratos, cargos, pagos parciales, garantías, mantenimiento y reportes. Este proyecto
académico utiliza investigación documental y escenarios sintéticos para definir un alcance con
Java 21, JavaFX y PostgreSQL. El documento establece ciento veintiséis requisitos funcionales,
sesenta requisitos no funcionales, treinta y nueve historias de usuario y treinta y nueve casos de
uso, con doce especificaciones detalladas. Las reglas precisan generación mensual, mora sin
capitalización, consecutivos transaccionales y conservación de tasas por operación. La
integración USD/NIO distingue la referencia comercial orientativa de la tasa autorizada para
registrar un pago. La presentación adopta Aurora UI en dos temas completos, Claro y Oscuro,
con preferencias locales y una modalidad sin efectos que conserva lectura y operaciones. La
investigación contrasta artículos científicos, documentación de fabricantes, disposiciones
nicaragüenses de protección de datos y publicaciones del Banco Central de Nicaragua. Los
antecedentes de otros países no describen prácticas observadas en Managua. La planificación
sigue seis períodos del 22 de octubre al 1 de diciembre de 2026, con 228 h programadas, 281,14
h de capacidad y 53,14 h de reserva, y entrega interna el 26 de noviembre. Los criterios de
aceptación definen pruebas futuras; la revisión documental y de archivos no acredita una
aplicación implementada, aceptación de participantes ni certificación jurídica.
Palabras clave: arrendamiento, requisitos, JavaFX, PostgreSQL, Aurora UI,
concurrencia, Nicaragua

## 3
Documento de Requisitos del Producto de Latch
## Introducción
El equipo construirá Latch como una aplicación de escritorio para administrar alquileres
por unidad y contrato. El producto reunirá las obligaciones de pago, los abonos y los documentos
administrativos que explican cada saldo. Este PRD fija el comportamiento exigible y sus criterios
de aceptación para el docente y los cuatro integrantes del proyecto. El alcance procede de la base
académica y funcional suministrada para esta revisión. La investigación externa respalda
decisiones puntuales; las metas de rendimiento y los escenarios de uso son propuestas de diseño
y no resultados obtenidos con una aplicación terminada.
El Brief de Diseño de Interfaz y Experiencia de Usuario para Latch (Cuadra et al., 2026a)
especifica controles y pantallas; el Documento de Flujo de la Aplicación de Latch (Cuadra et al.,
2026b), recorridos y transiciones. El Documento de Requisitos Técnicos de Latch (Cuadra et al.,
2026d) fija la arquitectura y el Esquema de Base de Datos y Servicios de Latch (Cuadra et al.,
2026e), la persistencia. El Plan de Implementación de Latch (Cuadra et al., 2026f) es la fuente
del calendario, RACI, revisión cruzada, tareas TAR y casos de ejecución CP. Los seis
documentos comparten módulos M01 a M12 y mantienen referencias cruzadas.
La Tabla 1 presenta convenciones de Lectura.
## Tabla 1
Convenciones de Lectura
ElementoInterpretaciónAplicación
RF-Mxx-nnRequisito funcional atómicoUna obligación por fila y criterio Dado,
## Cuando, Entonces.
RN-nn y RNF-
nnn
Regla de negocio y requisito
no funcional
RN conserva invariantes; RNF mide calidad.
HU-Mxx-nn y
CU-nn
Historia de usuario y caso de
uso
Identifican necesidad y recorrido del actor.
MustObligatorio para la versión
demostrable
Se expresa con debe y condiciona la
aceptación.
ShouldCompromiso de calidad
priorizado después de Must
Se especifica con debe porque también es
verificable; no sustituye un Must.

## 4
ElementoInterpretaciónAplicación
CouldOpcional planificado con
menor prioridad
Solo fotografías y logotipo; la ausencia se
declara al evaluar.
Won’tExcluido de esta entregaConsta en la tabla de exclusiones; no genera
## RF.
Debería y podríaRecomendación y posibilidad
fuera del compromiso
obligatorio
Solo en justificaciones, nunca sustituyen el
criterio del RF.
Hecho, decisión y
supuesto
Fuente externa, elección del
equipo y estimación no
confirmada
Se distinguen en texto, tablas y Apéndice E.
La prioridad MoSCoW ordena el esfuerzo sin cambiar la precisión de las obligaciones
registradas. Un requisito conserva su identificador aunque cambie de prioridad y cualquier
cambio documenta motivo, impacto y responsable. La palabra «debe» define una condición
comprobable dentro del alcance propuesto para la evaluación académica, sin afirmar aprobación
del docente. Las referencias a RA e IL proceden del resumen académico aportado, porque el
syllabus original no estuvo disponible. El Apéndice A fija el vocabulario y el Apéndice C
permite recorrer la relación entre requisitos, historias, casos y resultados de aprendizaje.
Contexto y Planteamiento del Problema
Antecedentes Documentados del Alquiler a Pequeña Escala
Balzarini y Boyd (2021) estudiaron estrategias de arrendadores con 71 entrevistas
semiestructuradas en Filadelfia entre 2017 y 2019. El muestreo intencional incluyó propietarios
de varias escalas, sin representar a la población. El artículo describe cobros, mantenimiento y
negociación de planes de pago como tareas de gestión. Esas categorías ayudan a definir registros
y movimientos para Latch, sin demostrar la eficacia del software. El estudio corresponde a
Estados Unidos y no permite estimar prácticas, demanda de aplicaciones ni frecuencia de errores
entre propietarios de Managua.
Reosti et al. (2024) combinaron registros y una encuesta de casi cuatro mil arrendadores
para analizar regulación y prácticas en Seattle. El estudio identifica diferencias y semejanzas
entre propietarios de distinta escala, sin justificar un perfil universal del pequeño arrendador.

## 5
Blanco et al. (2016) reúnen nueve estudios nacionales sobre vivienda en alquiler en América
Latina y el Caribe; Nicaragua no integra esos casos. Ambos antecedentes acotan el uso de
evidencia extranjera en este proyecto académico. La conveniencia de reunir contratos, saldos y
comprobantes en Latch permanece como hipótesis de diseño que necesita evaluación posterior.
Problema de Diseño
El escenario académico exige conocer cuánto corresponde cobrar, cuánto se recibió y qué
deuda permanece abierta en un contrato. Un pago parcial cambia varios saldos y un pago en otra
moneda añade una tasa que debe conservarse. La garantía tiene una finalidad distinta de la renta
y requiere movimientos separados para su liquidación. Latch vinculará esos registros mediante
reglas verificables. El equipo no observó cuadernos, mensajes, hojas de cálculo ni expedientes de
propietarios reales; esas herramientas son alternativas conceptuales para la comparación y no
hallazgos de campo.
Hipótesis y Supuestos del Proyecto
El diseño supone que un Propietario necesita administrar varias unidades y puede delegar
operaciones cotidianas a un Asistente. Un inquilino recibe documentos, pero no inicia sesión ni
dispone de portal. La demostración usará contratos y personas ficticias identificadas como datos
sintéticos, en un equipo con PostgreSQL local y una extensión posible a tres sesiones en red
local. La hipótesis de utilidad consiste en que los registros relacionados permitan explicar saldos
sin reconstruirlos fuera del sistema. Esa hipótesis tendrá revisión documental y escenarios de
aceptación, sin atribuirle validación de mercado o aceptación de un cliente externo.
Evidencia Documental y Límites de Generalización
La investigación distingue el texto efectivamente leído de los resultados accesibles solo
mediante resúmenes. Las funciones comerciales de Buildium y AppFolio se toman de páginas de
sus fabricantes y describen sus productos, sin probar beneficios comparativos para Nicaragua.
Las fuentes normativas establecen el texto consultado y sus artículos, con límites sobre reformas
y aplicación a casos particulares. El Apéndice B registra evidencia y escenarios sintéticos; el

## 6
Apéndice E conserva fuentes y trazabilidad documental. No se realizaron entrevistas, mediciones
SUS, pruebas con arrendadores ni consultas directas al syllabus de la asignatura.
## Justificación
La justificación técnica parte de las relaciones entre contrato, cargo, pago y recibo. Una
transacción conserva la consistencia de estas operaciones cuando coinciden varios usuarios.
PostgreSQL documenta que las secuencias pueden dejar huecos tras una reversión; por ello,
Latch adopta un contador bloqueado dentro de la transacción del pago (The PostgreSQL Global
Development Group, s. f.-a). La aceptación exigirá comprobar ese comportamiento con
concurrencia y fallos inducidos. Esta elección resuelve una condición del proyecto, pero la
consulta del manual no acredita que la aplicación ya la cumpla.
La justificación social se limita a facilitar documentos administrativos legibles y una
explicación de obligaciones y abonos. El inquilino podrá recibir un estado de cuenta con fecha de
corte, conceptos y saldos identificados; el sistema no decidirá conflictos, desalojos ni
condiciones jurídicas de un contrato. En el plano académico, las operaciones propuestas conectan
interfaz, SQL, ORM, concurrencia y servicios web con evidencias concretas para RA1 a RA5. El
recibo digital permite prescindir de una impresión cuando el destinatario lo acepta, sin convertir
esa posibilidad en una reducción ambiental medida o en una cifra de ahorro de papel.
## Objetivos
## Objetivo General
Desarrollar una versión demostrable de Latch que administre el ciclo de alquiler por
unidad y contrato con los doce módulos definidos y una cadena trazable entre obligaciones,
pagos y documentos. El equipo evaluará el producto mediante los ciento veintiséis requisitos
funcionales y las reglas de negocio de esta propuesta. La entrega interna adoptada es el 26 de
noviembre de 2026, dentro del calendario del Plan (Cuadra et al., 2026f), del 22 de octubre al 1
de diciembre: 228 h programadas, 281,14 h de capacidad y 53,14 h de reserva. La evaluación
comprobará evidencias reproducibles con datos sintéticos y distinguirá los objetivos pendientes
de los resultados realmente registrados durante la implementación.

## 7
## Objetivos Específicos
La Tabla 2 presenta objetivos por Resultado de Aprendizaje.
## Tabla 2
Objetivos por Resultado de Aprendizaje
ObjetivoCondición MedibleEvidencia Prevista
RA1Completar diez recorridos GUI con formularios,
menús, barra y diálogos sin error bloqueante.
Guion, capturas y registro de
ejecución.
RA2Ejecutar CRUD SQL con PreparedStatement en
seguridad y bitácora y cuatro consultas
agregadas.
Sentencias parametrizadas y
contraste de filas.
RA3Ejecutar H1 a H6 con cancelación y cien
recibos concurrentes sin huecos confirmados.
Registro de tareas y prueba de
contención.
RA4Ejecutar CRUD ORM en ocho entidades y
generar diez reportes con JasperReports.
Pruebas de persistencia, plantillas y
archivos.
RA5Enviar HTTP, validar JSON e integrar
USD/NIO y cuatro clases de fallo.
Solicitud identificada, fixtures y
presentación de origen.
CalidadSatisfacer 100 % de Must y 38 RN; completar
los mismos diez recorridos en ambos temas y
sin efectos.
Matriz de aceptación firmada por
revisores del equipo.
## Metas Medibles
La Tabla 3 presenta metas de la Versión Demostrable.
## Tabla 3
Metas de la Versión Demostrable
MetaUmbralMomento
## Cobertura
funcional
126 RF con escenario y trazabilidad; 100 % de Must
conformes.
## 25/11/2026 (HIT-07)
## Integridad
financiera
0 centavos de diferencia en cien operaciones sintéticas.10/11 y 25/11/2026
## (HIT-04/HIT-07)
## Interfaz
concurrente
Latencia de respuesta del hilo JavaFX ≤ 200 ms en las
tareas previstas.
27/10 y 25/11/2026
## (HIT-01/HIT-07)
Documentos10 reportes conciliados, con PDF y XLSX cuando
corresponde.
## 17/11/2026 (HIT-05)
EntregaDOCX, PDF, código y versión ejecutable identificados
por revisión.
26/11 y 1/12/2026
## (HIT-08/HIT-10)

## 8
Nota. Fechas según Cuadra et al. (2026f), Tabla 10, HIT-01 a HIT-10. Los umbrales son metas
de aceptación; no se han medido en una implementación de Latch.
## Alcance
Dentro del Alcance
La Tabla 4 presenta módulos de Latch.
## Tabla 4
Módulos de Latch
CódigoMódulo y DescripciónPrioridad
M01Seguridad y Usuarios. Controlar el acceso por cuenta individual y
conservar la atribución de cada operación.
## Must
M02Propiedades y Unidades. Organizar propiedades y unidades alquilables
con sus condiciones de disponibilidad.
## Must
M03Inquilinos. Mantener datos de contacto y consultar el historial asociado a
cada inquilino.
## Must
M04Contratos. Formalizar el canon y el calendario de ocupación de una
unidad por un inquilino.
## Must
M05Cargos y Cobranza. Registrar las obligaciones exigibles y calcular su
saldo por contrato y período.
## Must
M06Pagos y Recibos. Registrar el dinero recibido y su aplicación trazable a
obligaciones y saldos a favor.
## Must
M07Depósitos de Garantía. Separar el dinero retenido como garantía de los
ingresos de renta y documentar su liquidación.
## Must
M08Mantenimiento. Registrar solicitudes por unidad y los costos efectivos de
su atención.
## Must
M09Tipo de Cambio. Obtener una referencia comercial USD/NIO y conservar
la procedencia de cada tasa utilizada.
## Must
M10Reportes. Producir documentos de consulta y archivos exportados que
concilien con los registros financieros.
## Must
M11Panel Principal. Mostrar ocupación al instante de generación y cobranza
con filtros y fecha de corte visibles.
## Must
M12Configuración y Auditoría. Administrar parámetros, auditar cambios y
conservar respaldos recuperables.
## Must
Nota. Must describe el módulo mínimo; dos RF de imágenes son Could y nueve condiciones
complementarias son Should según el Apéndice D.

## 9
Fuera del Alcance
La Tabla 5 presenta exclusiones de la Entrega.
## Tabla 5
Exclusiones de la Entrega
Función ExcluidaMotivo y Límite
Contabilidad completaNo incluye libro mayor, estados financieros ni conciliación
bancaria.
Facturación fiscalLos recibos son administrativos; no acreditan autorización
## DGI.
Pasarelas de pagoSolo registra dinero informado; no transmite ni captura
fondos.
Aplicación móvil o web y portal
del inquilino
La implementación corresponde a escritorio JavaFX.
Mensajes automáticos por
WhatsApp, SMS o correo
El usuario entrega los archivos por un medio externo.
Firma electrónica y OCRNo firma contratos ni extrae texto de documentos.
Decisiones jurídicas automáticasNo determina legalidad de canon, mora, desalojo o
deducción.
Prorrateo automático y
contabilidad multicotización
La renta mensual es completa y los totales permanecen
separados por moneda.
## Supuestos
La fecha de portada es una fecha interna de trabajo adoptada para entregar documentos y
una versión demostrable; no confirma el calendario institucional. El contexto aportado sitúa la
presentación y evaluación final el 26 de noviembre y el 1 de diciembre de 2026. Cada integrante
dispone de una capacidad estimada de doce horas semanales, sin evidencia de disponibilidad
confirmada. Los volúmenes y equipos de referencia son supuestos para aceptación. El canon de
demostración y la mora son valores configurables de escenario, y el PRD no los presenta como
condiciones legales obligatorias para arrendadores nicaragüenses.
## Restricciones
La aplicación usará JDK 21, JavaFX, PostgreSQL y pgJDBC, con JDBC para seguridad,
auditoría y consultas agregadas, e Hibernate para las entidades del negocio. La integración de
reportes recibirá DTO mediante JavaBeans y evitará el adaptador HQL de JasperReports para

## 10
reducir el riesgo de mezclar versiones de Hibernate. La ventana de trabajo termina con el cierre
académico de referencia del 1 de diciembre de 2026. Los cuatro integrantes revisarán código y
documentación de otra persona, sin que la asignación propuesta certifique experiencia,
disponibilidad o acuerdos previamente celebrados por el equipo.
Dirección Visual y Preferencias de Presentación
Latch adopta Aurora UI como dirección visual de luces violetas y turquesas de baja
intensidad, con superficies opacas bajo formularios, tablas e importes. Malewicz (s. f.) describe
este tratamiento en el fragmento accesible de su publicación; su artículo no demuestra mejoras de
usabilidad. El producto tendrá exactamente dos temas, Claro y Oscuro, con Segoe UI y acciones
índigo. Claro usará fondo neutro frío y paneles blancos; Oscuro, fondo azul noche y paneles azul
pizarra. El Brief (Cuadra et al., 2026a) especifica tokens y composiciones; el TRD (Cuadra et al.,
2026d), su implementación nativa con JavaFX y FXML.
Propietario y Asistente podrán cambiar el tema desde el acceso y desde la ventana
principal sin modificar permisos administrativos. Antes de autenticar, la aplicación recuperará la
última elección local; después, cargará la preferencia asociada al identificador opaco del usuario.
Si falta el valor o falla su lectura, usará Claro. Un fallo de guardado conservará el cambio en la
sesión y mostrará un aviso sin bloquear operaciones. La opción sin efectos funcionará en ambos
temas y no requerirá otra tabla del dominio. Las luces serán estáticas en la primera versión y no
crearán tareas concurrentes adicionales. El Brief es la fuente canónica de tokens de color,
tipografía, espaciado, dimensiones y componentes; toda excepción visual se justifica y registra
contra esa especificación.
La navegación usará barra lateral por módulos y barra superior de contexto dentro de una
ventana única. Menús, herramientas y diálogos conservarán las interacciones de RA1. Los
importes aparecerán sobre fondos uniformes y ningún degradado comunicará deuda, mora o
éxito. OpenJFX (s. f.-a, s. f.-b, s. f.-c) documenta CSS, GaussianBlur y RadialGradient para ese
tratamiento visual. El PRD fija comportamientos y el Brief (Cuadra et al., 2026a) especifica

## 11
valores gráficos. La revisión medirá contraste, foco y uso por teclado sobre la presentación
renderizada en ambos temas del producto.
## Dependencias
La Tabla 6 presenta base Tecnológica Adoptada.
## Tabla 6
## Base Tecnológica Adoptada
ComponenteVersión y Uso AdoptadoCondición y Evidencia
JavaTemurin JDK 21.0.12.1+1,
Windows x64
Línea LTS 21; 21.0.13 no se afirma
disponible en la tabla de soporte
consultada.
InterfazOpenJFX 21.0.13; FXML; Scene
## Builder 21
JavaFX 21 requiere JDK 17 o posterior;
se adopta JDK 21.
Base de datosPostgreSQL 18.6; pgJDBC 42.7.14Rama publicada y soportada; controlador
org.postgresql:postgresql.
ORMHibernate ORM 7.4.6.FinalVersión publicada y compatible con Java
21; no es aprobación docente.
ReportesJasperReports 7.0.8; Jaspersoft
## Studio 7.0.8
PDF/XLSX con DTO; no se promete
PDF/A ni PDF/UA.
HTTP y JSONjava.net.http.HttpClient de JDK 21;
## Gson 2.13.2
HTTP en segundo plano y parseo tipado
con validación de esquema.
ConstrucciónMaven 3.9.16; Git y GitHubDependencias fijadas y etiqueta de
entrega; IDE IntelliJ IDEA compatible
con JDK 21.
## Servicio
externo
ExchangeRate-API Free
autenticado; base USD
Consulta orientativa; Bearer, 1500
solicitudes/mes, actualización diaria; sin
SLA contratado.
PresentaciónAurora UI; Claro y Oscuro; Segoe
## UI
Implementación JavaFX nativa;
preferencias locales y opción sin efectos;
sin biblioteca Aurora homónima.
Hibernate (s. f.) y Jaspersoft (2026a, 2026b) confirman las versiones de la Tabla 6. La
extensión Hibernate de JasperReports 7.0.8 referencia 6.3.1.Final; Latch la excluirá y entregará
DTO al reporte (Jaspersoft, s. f.-a). JavaFX 21.0.13 admite JDK 21 (Gluon, s. f.; Oracle, s. f.-b).
Las fuentes oficiales respaldan soporte, construcción y JSON del conjunto elegido (Apache
Software Foundation, s. f.; Eclipse Adoptium, s. f.-b; Google, 2025; PostgreSQL JDBC Driver,

## 12
s. f.). La consulta es documental y no equivale a compilar juntos los componentes ni a ejecutar
pruebas de integración.
Usuarios y Partes Interesadas
Roles del Sistema
El Propietario administra la instalación y autoriza las operaciones que alteran políticas o
corrigen registros financieros. El Asistente registra inquilinos, contratos, cargos y pagos dentro
de los permisos asignados, además de consultar reportes. Cada persona usa una cuenta
individual; compartir credenciales impide atribuir operaciones y queda fuera del modelo de uso.
El inquilino figura como destinatario de documentos administrativos y no como usuario
autenticado. La matriz aplica controles en servicios de aplicación además de ocultar acciones en
la interfaz, porque una restricción visual por sí sola no protege la integridad de los movimientos.
Cada servicio verifica usuario, rol y caducidad antes de escribir y repite la autorización cuando
empieza una tarea diferida; la denegación conserva ERR-009 / MSG-062 y las excepciones de la
matriz.
La Tabla 7 presenta permisos por Módulo.
## Tabla 7
Permisos por Módulo
MóduloPropietarioAsistente
M01Acceso, gestión de usuarios y
desbloqueos y cambio de contraseña
propia; tema personal en acceso.
Acceso, cierre de su sesión y cambio de
contraseña propia. Tema personal en
acceso.
M02Alta, edición, consulta y baja lógicaConsulta de unidades y edición de
condiciones/foto (PAN-05 y PAN-21 en
edición); sin alta de propiedad ni unidad,
sin bajas ni restricción En mantenimiento.
M03Alta, edición, consulta y desactivaciónAlta, edición y consulta; sin bajas.
M04Preparar, activar, renovar, terminar y
consultar; confirmar el cierre con
garantía/liquidación.
Preparar, activar y consultar; preparar
renovación y terminación. El Propietario
confirma el cierre con
garantía/liquidación; el Asistente solo
puede confirmarlo con garantía inicial cero
y sin movimientos.

## 13
MóduloPropietarioAsistente
M05Generar, registrar, consultar y anular
cargos
Generar, registrar y consultar; sin
anulación.
M06Cobrar, aplicar, reimprimir y anular
recibos
Cobrar, aplicar y reimprimir; sin
anulación.
M07Registrar y liquidar garantíasRegistrar garantía al activar y consultar;
sin aplicar ni devolver.
M08Registrar, editar estados, costos y
consultar
Registrar, editar estados, costos y
consultar.
M09Consultar y autorizar tasa manualConsultar y solicitar actualización dentro
de la frecuencia; sin autorización manual.
M10Generar, consultar y exportar reportesGenerar, consultar y exportar reportes; sin
bitácora restringida.
M11Consultar indicadores y abrir detalle
Cambiar tema y efectos personales.
Consultar indicadores y abrir detalle.
Cambiar tema y efectos personales.
M12Configurar, consultar auditoría, respaldar
y restaurar
Sin parámetros administrativos, auditoría
ni respaldos; presentación personal
gestionada desde M01/M11.
Arquetipos Sintéticos de Usuario
Los arquetipos traducen el alcance a necesidades y no describen participantes reales. El
Propietario sintético administra doce unidades en tres propiedades, revisa garantías y autoriza
correcciones de recibos. El Asistente sintético registra abonos y prepara estados de cuenta en una
jornada de prueba. Un tercer perfil corresponde al Propietario que opera solo y requiere respaldo
y recuperación sin soporte permanente. Los volúmenes son datos de escenario, ajenos a una
encuesta. Los estudios aportan contexto sobre gestión del alquiler, sin validar estos perfiles ni sus
preferencias de interfaz.
La Tabla 8 presenta arquetipos de Revisión.
## Tabla 8
Arquetipos de Revisión
ArquetipoObjetivoNecesidades y Escenario
Propietario delegado,
sintético
Revisar saldos y autorizar
correcciones
Consulta cartera, revisa 12 unidades y
anula un recibo erróneo conservando el
número.

## 14
ArquetipoObjetivoNecesidades y Escenario
Asistente de cobro,
sintético
Registrar abonos de la
jornada
Requiere búsqueda de contrato,
aplicaciones visibles y aviso de tasa sin
autorización.
Propietario operador,
sintético
Conservar los registros y
operar sin red externa
Usa PostgreSQL local, tasa autorizada por
fecha, respaldo diario y recuperación
aislada.
## Partes Interesadas
La Tabla 9 presenta intereses y Comunicación Propuesta.
## Tabla 9
Intereses y Comunicación Propuesta
ParteInterésInfluenciaComunicación Propuesta
## José A. Durán
## García
Evaluar resultados de
aprendizaje y originalidad
## Alta,
evaluación
académica
Demostración del 26 de
noviembre y cierre de referencia
del 1 de diciembre; fechas
aportadas, sin confirmación
institucional.
Steven CuadraCoherencia del PRD,
seguridad y concurrencia
## Alta,
coordinación
propuesta
Seguimiento asíncrono diario
con TAR, horas y bloqueos;
revisión y retrospectiva por
sprint según el Plan (Cuadra et
al., 2026f).
## Alfredo
## Montalvan
Integridad y persistencia
PostgreSQL
## Alta,
responsable de
datos
propuesto
Seguimiento asíncrono diario y
revisión por sprint; revisión
cruzada según Plan (Cuadra et
al., 2026f), Tabla 4 y apartado
«Revisión Cruzada y Decisión
## Técnica».
## Caleb
## Tardencilla
Recorridos JavaFX y
pruebas
## Alta,
responsable de
interfaz
propuesto
Seguimiento asíncrono diario y
revisión por sprint; revisión
cruzada según Plan (Cuadra et
al., 2026f), Tabla 4 y apartado
«Revisión Cruzada y Decisión
## Técnica».
Aris GavarreteServicio HTTP, reportes y
distribución
## Alta,
responsable de
integración
propuesto
Seguimiento asíncrono diario y
revisión por sprint; revisión
cruzada según Plan (Cuadra et
al., 2026f), Tabla 4 y apartado
«Revisión Cruzada y Decisión
## Técnica».

## 15
ParteInterésInfluenciaComunicación Propuesta
Roles y
destinatarios
sintéticos
Permisos, comprobantes y
saldos explicables
Media como
criterio de
diseño
Revisión de escenarios por el
equipo; no hay contacto con
participantes reales.
Visión y Propuesta de Valor
Latch permitirá que un pequeño propietario consulte la situación de una unidad y
explique la deuda de su contrato con registros relacionados. El valor esperado depende de que los
pagos parciales, las conversiones y las garantías conserven sus vínculos y documentos. La meta
operativa es completar diez recorridos críticos con saldos conciliados y sin congelar la interfaz.
Un recibo mantendrá el mismo número cuando se reimprima o anule. Estas condiciones
describen una propuesta de producto para evaluación académica; el equipo no ha medido ahorro
de tiempo, satisfacción, rentabilidad ni adopción de propietarios de Managua.
Análisis de Alternativas
La comparación usa criterios observables: relación entre registros, concurrencia,
documentos y adecuación al escenario USD/NIO. El cuaderno y la hoja de cálculo son modelos
conceptuales con configuración manual definida para el análisis. Buildium y AppFolio
documentan cobros y operaciones del alquiler en sus sitios oficiales (AppFolio, s. f.; Buildium, s.
f.). Estas fuentes no verifican cobertura nicaragüense, soporte de NIO ni costo aplicable al
equipo. Latch responde al alcance académico y a los resultados de aprendizaje, sin una
afirmación de superioridad comercial o de beneficios medidos.
La Tabla 10 presenta alternativas con Criterios Delimitados.

## 16
## Tabla 10
Alternativas con Criterios Delimitados
CriterioCuaderno ConceptualHoja de Cálculo
## Conceptual
Productos ReconocidosLatch Propuesto
## Relación
contrato, cargo y
pago
Referencias escritas por
operador.
Depende de diseño y
fórmulas manuales.
Buildium y AppFolio
declaran gestión de alquileres.
Relaciones verificadas por
restricciones y RF.
## Operación
simultánea
Un ejemplar requiere
coordinación humana.
Depende de plataforma y
controles implementados.
No se evaluó concurrencia de
estos productos.
H4 y transacciones con prueba
prevista de cien recibos.
DocumentosRedacción o
reproducción manual.
Plantillas configuradas
por quien la administra.
Ambos declaran funciones de
reportes.
Diez catálogos definidos con
## PDF/XLSX.
## Escenario
## USD/NIO
Operador registra el
cálculo.
Operador configura tasas
y conversión.
Cobertura NIO y Nicaragua
no verificada.
Tasa por operación y
autorización de fuente.
## Adecuación
académica
No implementa RA1 a
## RA5.
No demuestra por sí sola
el conjunto Java exigido.
Comprar un producto no
implementa el proyecto.
Implementación propia con
evidencias por RA.
## Beneficios Esperados
La propuesta espera facilitar la revisión de deuda mediante un estado de cuenta que
reconcilie cargos, abonos y créditos a una fecha de corte. La comprobación exigirá diferencia
cero entre sus totales y los movimientos persistidos en el conjunto sintético. El panel reducirá la
navegación necesaria para abrir cartera o contratos por vencer mediante enlaces al detalle
filtrado. La meta de acceso al panel es de dos segundos en el percentil 95 del entorno de
referencia. El equipo medirá esas condiciones durante implementación; no existen resultados
históricos que permitan afirmar una mejora porcentual frente a otras herramientas.

## 17
Metodología de Levantamiento de Requisitos
Investigación Documental y Derivación
La búsqueda del 22 de octubre de 2026 cubrió pequeños arrendadores, gestión del
alquiler y tecnologías del proyecto. Las consultas combinaron small-scale landlords, rental
management, JavaFX themes y protección de datos Nicaragua. La selección exigió autor
identificado, publicación original y relación con una decisión del PRD. La revisión incluyó
artículos completos y limitó las páginas comerciales a funciones documentadas del proveedor.
Excluyó fuentes anónimas y publicidad como prueba de eficacia. El Apéndice E registra
afirmaciones, acceso, fecha y secciones consultadas para rastrear el uso de cada fuente.
El equipo derivará escenarios de cada operación y conservará su relación con reglas,
módulos y resultados de aprendizaje. Otro integrante revisará identificadores, permisos, estados
y criterios de aceptación de cada entrega. Las historias estiman complejidad relativa en puntos y
no representan horas de trabajo. La ausencia de campo impide validar preferencias, carga
cognitiva o demanda de mercado. La aceptación prevista será académica y técnica sobre datos
sintéticos; la investigación documental no sustituye pruebas con usuarios ni valida una
instalación operativa para contratos de personas reales. Cada incremento incluye revisión
cruzada y seguimiento de riesgos; las observaciones se clasifican como defecto, aclaración o
cambio de alcance, con responsable y resolución. Una actividad programada o revisión
documental no acredita una prueba ejecutada.

## 18
Cronograma y Capacidad Propuestos
La Tabla 11 presenta plan de Trabajo con Dependencias y Capacidad.
## Tabla 11
Plan de Trabajo con Dependencias y Capacidad
IntervaloEntregable y DependenciaResponsables PropuestosCapacidad y Asignación
## Sprint 0: 22–
## 27/10/2026
Base repetible, acceso y esqueleto
FXML en ambos temas; concurrencia
y sección crítica antes de HIT-01.
TAR-001 a TAR-016 (Plan,
## Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
32 h programadas; capacidad
41,142857 h; reserva 9,142857 h.
## Sprint 1: 28/10–
## 3/11/2026
Seguridad, catálogos y contratos con
ORM; depende de base y escenarios
de Sprint 0. TAR-017 a TAR-032
(Plan, Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
40 h programadas; capacidad
48,000000 h; reserva 8,000000 h.
## Sprint 2: 4–
## 10/11/2026
H1/H2, pagos, garantías y H4;
depende de contratos y seguridad;
HIT-04. TAR-033 a TAR-048 (Plan,
## Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
40 h programadas; capacidad
48,000000 h; reserva 8,000000 h.
## Sprint 3: 11–
## 17/11/2026
Mantenimiento, panel, respaldo y
diez reportes; depende del circuito
financiero; HIT-05. TAR-049 a
TAR-064 (Plan, Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
40 h programadas; capacidad
48,000000 h; reserva 8,000000 h.
## Sprint 4: 18–
## 25/11/2026
HTTP, regresión, ambos temas,
recuperación y documentación;
depende de módulos integrados;
HIT-06/HIT-07. TAR-065 a
TAR-080 (Plan, Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
44 h programadas; capacidad
54,857143 h; reserva 10,857143
h.

## 19
IntervaloEntregable y DependenciaResponsables PropuestosCapacidad y Asignación
## Cierre: 26/11–
## 1/12/2026
Entrega interna, observaciones,
correcciones, ensayo y cierre;
depende de RC; HIT-08 a HIT-10.
TAR-081 a TAR-096 (Plan,
## Apéndice A).
Responsables nominales según las TAR
del Plan (Cuadra et al., 2026f, Apéndice
A); RACI y revisión en sus Tablas 4–5.
32 h programadas; capacidad
41,142857 h; reserva 9,142857 h.
## Total: 22/10–
## 1/12/2026 (41
días)
TAR-001 a TAR-096; seis períodos
según el Plan (Cuadra et al., 2026f,
## Apéndice A).
Cuatro integrantes; 57 h programadas
por persona.
228 h programadas; capacidad
281,142857 h (281,14); reserva
53,142857 h (53,14), equivalente
al 18,902439 % (18,90 %).
El calendario único es el del Plan (Cuadra et al., 2026f, Tabla 8 y Apéndice A): Sprint 0,
22–27/10; S1, 28/10–3/11; S2, 4–10/11; S3, 11–17/11; S4, 18–25/11; Cierre, 26/11–1/12/2026.
Las TAR-001 a TAR-096 suman 32 + 40 + 40 + 40 + 44 + 32 = 228 h programadas. La
capacidad del equipo es 48 × 41/7 = 281,142857 h y la reserva, 53,142857 h (18,902439 %); se
muestran 281,14 h, 53,14 h y 18,90 % al redondear. Cada integrante tiene 57 h programadas y
70,285714 h de capacidad. Son estimaciones, no horas ejecutadas.
La Tabla 12 remite a la matriz RACI y al mapa único de revisión cruzada del Plan de Implementación (Cuadra et al., 2026f).
## Tabla 12
Fuentes Canónicas de Responsabilidad y Revisión
ElementoFuente canónica
Matriz RACI de entregablesCuadra et al. (2026f), Tabla 5. Las asignaciones R/A/C/I se consultan allí; no se mantiene una
matriz alternativa en el PRD.
Responsabilidades, relevo y revisión
cruzada
Cuadra et al. (2026f), Tabla 4 y apartado «Revisión Cruzada y Decisión Técnica».

## 20
Nota. Se conserva el número de tabla. La única matriz RACI es la Tabla 5 del Plan (Cuadra et al., 2026f): R ejecuta, A responde por el
resultado y hay uno por entregable, C consulta e I recibe información. La asignación no acredita un acuerdo celebrado. Los cuatro
reciben información mediante el registro común de cambios. La Tabla 4 y el apartado «Revisión Cruzada y Decisión Técnica» del
Plan fijan el mapa único de revisión y relevo.
Reglas de Negocio
Las reglas fijan invariantes del producto y conservan RN-01 a RN-15 de la base común
con precisiones necesarias para operar. El estado Vencido de un contrato mantiene la reserva de
la unidad hasta un cierre explícito, porque la fecha final por sí sola no acredita desocupación. El
consecutivo sin saltos se define para pagos confirmados dentro de la base activa; una anulación
conserva su número y una transacción revertida no lo consume. Las reglas complementarias
resuelven redondeo, reintentos, estados y dependencias de reversión sin atribuir a la normativa
una decisión financiera que corresponde al diseño.
La Tabla 13 presenta reglas de Negocio y Estado de Revisión.
## Tabla 13
Reglas de Negocio y Estado de Revisión
IDReglaFuenteMódulosEstado de Validación
RN-01Una unidad admite a lo sumo un contrato activo en un mismo
intervalo; activo comprende Vigente, Por vencer y Vencido
pendiente de cierre.
Base RN-01
ampliada
M02 M04Especificada

## 21
IDReglaFuenteMódulosEstado de Validación
RN-02La fecha final cumple fin >= inicio + 1 mes calendario; si falta el
día en el mes destino se usa su último día. Un mes natural
completo del día 1 al 31 exige fin >= día 1 del mes siguiente.
Base RN-02
precisada
M04Especificada
RN-03El canon contractual es positivo y se pacta exclusivamente en
NIO o USD; su moneda permanece en los movimientos
históricos.
Base RN-03M02 M04
## M06
## Especificada
RN-04Hay una sola renta por contrato y período AAAA-MM; la
combinación es única en la base de datos, incluso al reintentar H1.
Base RN-04M05Especificada
RN-05La mora surge una sola vez por renta vencida tras la gracia, como
monto fijo o porcentaje del principal pendiente en ese instante;
excluye Mora de la base.
Base RN-05
precisada
M05 M12Especificada
RN-06El abono cubre primero cargos vencidos por fecha y luego
identificador, salvo selección manual; sin vencidos o con
excedente, queda a favor del mismo contrato y moneda. El crédito
se aplica al generar el siguiente cargo.
Base RN-06
precisada
M05 M06Especificada
RN-07Cada pago confirmado consume un consecutivo único contiguo.
Pago, aplicaciones, reserva del contador, recibo y auditoría
confirman en una transacción con una Session y una conexión;
rollback antes del commit no consume número, anulación
posterior conserva número e historial. nextval no sustituye el
contador transaccional.
Base RN-07
resuelta
M06Especificada
RN-08Pagos y recibos son inmutables tras confirmar; solo el Propietario
anula con motivo, actor e instante. El número permanece visible y
nunca se reutiliza.
Base RN-08M06Especificada
RN-09La conversión usa NIO por USD autorizada para la fecha efectiva
del pago, separada de la fecha operativa obtenida de la BD; NIO a
USD divide y USD a NIO multiplica. Se guardan originales,
conversión y procedencia.
Base RN-09
precisada
M06 M09Especificada

## 22
IDReglaFuenteMódulosEstado de Validación
RN-10Al activar se registra el depósito pactado ≥ 0 en moneda
contractual, separado de renta; con cero queda Retenido sin
movimientos ni recibo. Al terminar se aplica o devuelve con
conceptos por deducción y aprobación del Propietario.
Base RN-10
ampliada
M04 M07Especificada
RN-11La baja de unidad o inquilino es lógica, por Propietario y solo sin
contrato activo; la propiedad con unidades activas tampoco se da
de baja.
Base RN-11
ampliada
M02 M03Especificada
RN-12Un contrato activo pasa a Por vencer cuando faltan entre cero y
treinta días inclusive; el umbral se configura y H2 lo revisa cada
cinco minutos por defecto.
Base RN-12
precisada
## M04 M11
## M12
## Especificada
RN-13Solo el Propietario gestiona usuarios, cambia parámetros, anula
recibos y cargos, liquida garantía, restaura y realiza bajas lógicas.
Base RN-13
ampliada
## M01 M05
## M06 M07
## M12
## Especificada
RN-14Cada operación financiera conserva actor, instante UTC,
correlación y referencia en la misma transacción. El instante y la
fecha operativa se obtienen de now() de la BD; el visor usa
America/Managua. La fecha efectiva del pago se ingresa por
separado y no sustituye la fecha operativa. Si |reloj local − BD| >
5 min se bloquean nuevas operaciones financieras (ERR-057 /
MSG-080) hasta corregir el desfase; la comprobación se ejecuta
antes de escribir y no interrumpe una transacción financiera ya
iniciada ni sustituye la validación de la fecha efectiva.
pago.fecha_pago conserva la fecha efectiva y pago.creado_en el
instante de BD. Variante de CP-040.
Base RN-14
precisada
## M06 M07
## M12
## Especificada
RN-15Las contraseñas usan hash salado; cinco fallos consecutivos
bloquean quince minutos y un acceso correcto reinicia el
contador. Un Propietario no bloqueado puede desbloquear otra
cuenta.
Base RN-15
precisada
M01 M12Especificada

## 23
IDReglaFuenteMódulosEstado de Validación
RN-16Los catálogos comunes conservan los términos de la base; código
de unidad es único por propiedad y nombre de cuenta es único sin
distinguir mayúsculas.
Diseño adicionalM01 M02Especificada
RN-17El día de pago admite 1 a 31 y se ajusta al último día del mes. La
renta cubre un período mensual; se adopta canon completo sin
prorrateo automático.
Diseño adicionalM04 M05Especificada
RN-18Se recopilan datos mínimos para el contrato; identificación,
emergencia y adjuntos son opcionales. Se excluyen salud y
biometría innecesarias; los datos financieros del alquiler tienen
acceso restringido como datos sensibles.
Diseño de
minimización ;
## Ley 787
M03Especificada
RN-19Borrador puede activarse o cancelarse. Renovar solo se inicia
desde Vigente o Por vencer y crea un sucesor Borrador enlazado.
Terminar se permite desde Vigente, Por vencer o Vencido; el
cierre atómico termina el anterior y activa el sucesor. Vencido
conserva deuda y reserva la unidad hasta cierre explícito.
Diseño adicionalM04Especificada
RN-20H1 confirma por contrato y permite cancelar entre contratos; cada
reintento omite el período ya confirmado. H1 y H2 comparten
bloqueo por contrato al escribir cargos.
Diseño de
concurrencia
M05Especificada
RN-21Cada solicitud financiera lleva UUID de idempotencia único; al
reintentar se recupera el resultado confirmado en lugar de duplicar
pago o devolución.
Diseño de
integridad
M06 M07Especificada

## 24
IDReglaFuenteMódulosEstado de Validación
RN-22Una tasa comercial solo se muestra como referencia orientativa; el
JSON exige NIO/USD finito entre 1 y 100 inclusive. Fuera de
banda no se guarda como referencia vigente y se registra M09,
REFERENCIA_EN_CUARENTENA, con valor y huella en
bitacora, sin tabla nueva. Para una tasa manual positiva fuera de 1
a 100 NIO/USD, la banda es una advertencia de revisión que
exige confirmación del Propietario, sin restricción de banda en la
BD. El pago entre monedas exige tasa autorizada para su fecha
efectiva y fuente admisible. Una tasa de otra fecha se identifica
como histórica; ninguna tasa comercial se rotula oficial BCN.
Diseño cambiarioM09 M06Especificada
RN-23Un costo de mantenimiento es gasto del propietario; repercutirlo
al inquilino requiere cargo separado y concepto autorizado, sin
automatismo entre ambos registros.
Diseño financieroM05 M08Especificada
RN-24Si el saldo es cero el cargo es Pagado; si tiene saldo y
vencimiento pasado es Vencido; en otro caso es Parcial con abono
o Pendiente sin abono. Anulado prevalece.
Diseño de estadosM05 M10
## M11
## Especificada
RN-25Los adjuntos admitidos tienen máximo cinco MiB y se copian a
un directorio controlado con nombre interno aleatorio, validación
de firma y huella; se excluyen ejecutables.
Diseño de
archivos
## M02 M03
## M04 M12
## Especificada
RN-26Con depósito pactado cero se registra Retenido sin movimientos
ni recibo. El depósito inicial equivale a aplicaciones más
devoluciones más remanente. Con remanente y devoluciones es
Devuelto parcial; con remanente sin devolución es Retenido;
agotado con devolución es Devuelto, y agotado solo con
aplicaciones es Aplicado.
Diseño de estadosM07Especificada
RN-27Mantenimiento permite Abierta a En proceso o Cancelada y En
proceso a Resuelta o Cancelada; Resuelta y Cancelada son
terminales. Los gastos confirmados permanecen trazables.
Diseño de estadosM08Especificada

## 25
IDReglaFuenteMódulosEstado de Validación
RN-28Reportes mantienen filtros, corte y moneda; REP-05 y la
ocupación del panel usan el instante de generación sin fecha
histórica seleccionable. Los totales no mezclan monedas y las
exportaciones usan temporal y publicación por cambio de
nombre.
Diseño de
reportes
M10 M12Especificada
RN-29El respaldo incluye base y adjuntos bajo exclusión de escrituras,
cifrado AES-256-GCM con contraseña de respaldo; la
restauración exige mantenimiento, autenticidad verificada antes
de descifrar, hashes y copia previa. Credenciales de conexión
quedan fuera del paquete. Espera las escrituras ya iniciadas y
conserva una copia coherente de base, adjuntos y manifiesto. La
restauración exige clientes detenidos y validación aislada antes de
sustituir la instalación. El paquete excluye también claves. Si
existen recibos posteriores al respaldo, mantiene mantenimiento
hasta conciliar, sin reutilizar ni renumerar consecutivos.
Diseño de
recuperación
M12Especificada
RN-30H6 solicita cancelación y espera terminación de H1, H2, H3 y H5;
H4 termina con su transacción. Si no finalizan en diez segundos,
muestra el bloqueo y conserva el proceso abierto.
Diseño de ciclo de
vida
M12Especificada
RN-31Solo se anula un pago si sus créditos no tienen aplicaciones
posteriores activas; se revierten primero esas dependencias
mediante operaciones autorizadas con trazabilidad.
Resolución de
reversión
M06Especificada
RN-32Los importes se manejan en decimal, dos decimales monetarios y
ocho para tasa; redondeo HALF_EVEN al convertir el total del
pago y remanente asignado al último cargo.
Diseño de
precisión
## M05 M06
## M07 M09
## Especificada
RN-33Un nuevo contrato no modifica rentas confirmadas. Una
corrección financiera se documenta como anulación autorizada y
nueva operación, con referencia cruzada.
Diseño de
inmutabilidad
## M04 M05
## M06
## Especificada
RN-34Al pasar el día final sin terminación o renovación, H2 marca
Vencido. El primer y último período con contrato activo
conservan renta completa; una terminación no crea rentas futuras.
Diseño de
vencimiento
M04 M05Especificada

## 26
IDReglaFuenteMódulosEstado de Validación
RN-35La presentación admite exactamente Claro y Oscuro, ambos
Aurora UI; Claro inicia cuando falta preferencia válida. No hay
sincronización automática con Windows.
Decisión visual
del proyecto
M01 M11Especificada
RN-36La última elección local rige antes del acceso; después rige la
preferencia por id opaco de usuario. Error de lectura usa Claro;
error de guardado conserva sesión y avisa.
Decisión de
preferencia local
M01 M11Especificada
RN-37Tema y efectos son preferencias personales de ambos roles; no
cambian permisos, importes, reglas financieras ni contenido de
reportes.
Decisión de
separación
funcional
## M01 M06
## M10 M11
## Especificada
RN-38La modalidad sin efectos conserva ambos temas y elimina luces y
desenfoques decorativos. Las luces de la primera versión son
estáticas y no comunican estados financieros.
Decisión de
legibilidad
M01 M11Especificada
Nota. Especificada significa definida documentalmente; ninguna regla fue probada sobre una implementación de Latch. Las derivadas
de privacidad son controles propuestos, con límites en Requisitos de Datos. RN-01 incluye Vigente, Por vencer y Vencido para evitar
superposición accidental.
## Requisitos Funcionales
Los requisitos describen obligaciones observables del producto y usan un criterio Dado,
Cuando, Entonces por fila. La columna de trazabilidad identifica resultados e indicadores del
resumen académico suministrado y añade la regla aplicable. Los escenarios de aceptación son
sintéticos y previstos, sin resultados de ejecución de Latch. El Apéndice C conserva el vínculo
completo con las historias y casos de uso para revisar cobertura. La priorización mantiene como
Must las invariantes financieras; Should y Could ordenan condiciones complementarias dentro
del mismo catálogo, con identificación explícita para la evaluación de entrega.

## 27
M01 Seguridad y Usuarios
El acceso identifica al actor antes de consultar datos o registrar movimientos. Las cuentas
tienen un rol único y un estado que permite restringir operaciones sin borrar su historial. El
bloqueo temporal usa un contador persistente y evita revelar si un nombre de usuario existe. La
autorización se comprobará en los servicios y no dependerá de la visibilidad de un botón. Los
parámetros iniciales son cinco fallos, quince minutos de bloqueo y quince minutos de
inactividad. Esos valores son decisiones de diseño y pueden modificarse dentro de los límites de
configuración establecidos.
La Tabla 14 presenta requisitos de Seguridad y Usuarios.
## Tabla 14
Requisitos de Seguridad y Usuarios
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M01-01El sistema debe autenticar
cuentas activas por usuario y
contraseña.
MustDado una cuenta activa, Cuando se ingresa su
contraseña correcta, Entonces abre una sesión
del rol asignado.
## RA1 IL1.2; RA2 IL2.1;
## RN-15
RF-M01-02El sistema debe rechazar
credenciales incorrectas sin
revelar si existe la cuenta.
MustDado credenciales inválidas, Cuando se
intenta ingresar, Entonces muestra un mensaje
genérico y registra el intento fallido.
## RA1 IL1.3; RA2 IL2.2;
## RN-15
RF-M01-03El sistema debe almacenar
contraseñas con Argon2id y sal
única por cuenta.
MustDado una contraseña nueva, Cuando se guarda
la cuenta, Entonces persiste el hash y la sal sin
texto plano.
## RA2 IL2.3; RN-15
RF-M01-04El sistema debe bloquear el
acceso tras cinco intentos fallidos
consecutivos.
MustDado cuatro fallos previos, Cuando ocurre el
quinto fallo, Entonces bloquea la cuenta
durante quince minutos.
## RA2 IL2.3; RN-15

## 28
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M01-05El sistema debe restringir
acciones según el rol autorizado.
MustDado una sesión de Asistente, Cuando se
solicita anular un recibo, Entonces deniega la
operación sin alterar datos. Dada una tarea
diferida autorizada al programarse, Cuando
empieza a operar, Entonces el servicio
revalida usuario, rol y caducidad antes de
escribir; si ya no está autorizada, deniega con
## ERR-009 / MSG-062.
## RA1 IL1.2; RA2 IL2.3;
## RN-13
RF-M01-06El sistema debe permitir al
Propietario crear cuentas con uno
de los dos roles.
MustDado un Propietario autenticado, Cuando
registra un usuario único, Entonces crea la
cuenta activa con el rol elegido.
## RA2 IL2.2; RN-13
RF-M01-07El sistema debe cerrar la sesión
tras quince minutos de
inactividad.
ShouldDado una sesión abierta sin cambios
pendientes, Cuando transcurre el umbral,
Entonces cierra la sesión y vuelve al inicio de
sesión.
## RA1 IL1.2; RN-15
RF-M01-08El sistema debe cerrar la sesión
mediante una acción explícita.
MustDado una sesión abierta, Cuando se confirma
cerrar sesión, Entonces descarta credenciales
de memoria y presenta el acceso.
## RA1 IL1.3; RN-15
RF-M01-09El sistema debe permitir al
Propietario cambiar el rol de otra
cuenta activa.
MustDado una cuenta Asistente existente, Cuando
el Propietario confirma otro rol, Entonces
guarda el rol y exige nueva sesión de la cuenta
afectada.
## RA2 IL2.2, IL2.3;
## RN-13
RF-M01-10El sistema debe permitir al
Propietario desactivar otra cuenta
sin borrar su historial.
MustDado dos cuentas Propietario activas, Cuando
se confirma desactivar una, Entonces impide
nuevos accesos de esa cuenta y conserva el
último Propietario activo.
## RA2 IL2.2, IL2.3;
## RN-13

## 29
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M01-11El sistema debe permitir al
Propietario restablecer la
contraseña de otra cuenta y a
ambos roles cambiar su propia
contraseña.
MustDado una cuenta existente y contraseña
temporal válida, Cuando el Propietario
confirma el restablecimiento de otra cuenta,
Entonces reemplaza el hash y exige cambio al
próximo acceso. Dado un Propietario o
Asistente autenticado y su contraseña actual
válida, Cuando confirma una nueva contraseña
válida para su propia cuenta, Entonces
reemplaza el hash de esa cuenta.
## RA2 IL2.2, IL2.3;
## RN-15
RF-M01-12El sistema debe ofrecer Claro y
Oscuro en la pantalla de acceso.
MustDado el acceso visible, Cuando cualquier rol
elige Oscuro, Entonces cambia la presentación
sin autenticar ni alterar credenciales.
## RA1 IL1.1, IL1.2;
## RN-35
RF-M01-13El sistema debe restaurar la
última elección local antes de
autenticar.
MustDado un inicio posterior, Cuando la
preferencia local es válida, Entonces muestra
ese tema; si falta o no se lee, muestra Claro.
## RA1 IL1.2; RN-36
RF-M01-14El sistema debe cargar la
preferencia del usuario por su
identificador opaco después del
acceso.
MustDado usuario A autenticado, Cuando tiene
Oscuro guardado y el acceso usaba Claro,
Entonces aplica Oscuro; sin valor válido usa
## Claro.
## RA1 IL1.2; RN-36
RF-M01-15El sistema debe informar un fallo
al guardar la preferencia sin
revertir el cambio de sesión.
MustDado el almacén local sin escritura, Cuando
cambia el tema, Entonces conserva la elección
durante la sesión y muestra un aviso no
bloqueante.
## RA1 IL1.3; RN-36
M02 Propiedades y Unidades
Las propiedades agrupan unidades y permiten consultar su ubicación y condiciones de
referencia. Cada unidad conserva un tipo del catálogo y un código único dentro de su propiedad.
El canon de referencia orienta la preparación de contratos y no reemplaza el canon pactado en

## 30
ellos. La ocupación resulta del ciclo contractual, mientras que mantenimiento e inactividad
restringen nuevas activaciones. Una fotografía es opcional y su ausencia no impide alquilar la
unidad. El historial financiero permanece vinculado cuando el Propietario autoriza una baja
lógica que cumpla las reglas de integridad del inventario.
La Tabla 15 presenta requisitos de Propiedades y Unidades.
## Tabla 15
Requisitos de Propiedades y Unidades
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M02-01El sistema debe permitir al
Propietario registrar propiedades
con nombre y dirección.
MustDado datos válidos de propiedad, Cuando se
confirma el alta, Entonces guarda una
propiedad con identificador único.
## RA1 IL1.1; RA4 IL4.1;
## RN-11
RF-M02-02El sistema debe permitir al
Propietario registrar unidades de
tipo Casa, Apartamento, Cuarto o
Local comercial.
MustDado una propiedad activa, Cuando se guarda
una unidad, Entonces vincula la unidad y
conserva el tipo elegido.
## RA4 IL4.1; RN-16
RF-M02-03El sistema debe impedir códigos
de unidad repetidos dentro de
una propiedad.
MustDado un código existente en la propiedad,
Cuando se intenta reutilizarlo, Entonces
rechaza el duplicado y conserva la unidad
original.
## RA2 IL2.3; RN-16
RF-M02-04El sistema debe permitir al
Propietario y al Asistente guardar
el canon de referencia con monto
positivo y moneda NIO o USD.
MustDado una unidad registrada, Cuando se edita
el canon de referencia, Entonces conserva
monto y moneda sin cambiar contratos
existentes.
## RA4 IL4.1; RN-03
RF-M02-05El sistema debe permitir al
Propietario y al Asistente
adjuntar una fotografía opcional
de la unidad en PNG o JPEG.
CouldDado un archivo admitido de hasta cinco MiB,
Cuando se confirma el adjunto, Entonces
guarda la copia controlada y muestra su vista
previa.
## RA1 IL1.1; RN-25

## 31
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M02-06El sistema debe permitir al
Propietario y al Asistente filtrar
unidades por propiedad, tipo y
estado.
MustDado un catálogo con varios estados, Cuando
se aplican filtros, Entonces muestra solo
coincidencias y su cantidad.
## RA1 IL1.1; RA2 IL2.2;
## RN-16
RF-M02-07El sistema debe derivar la
ocupación de la unidad de los
contratos activos.
MustDado una unidad con contrato activo, Cuando
se consulta su estado, Entonces muestra
Ocupada y evita cambiarla a Disponible.
## RA4 IL4.1; RN-01
RF-M02-08El sistema debe permitir al
Propietario dar de baja lógica una
unidad sin contrato activo.
MustDado una unidad libre, Cuando se confirma la
baja, Entonces marca Inactiva y conserva su
historial.
## RA4 IL4.1; RN-11
RF-M02-09El sistema debe permitir al
Propietario editar nombre y
dirección de una propiedad
registrada.
MustDado una propiedad activa, Cuando se
confirman los datos corregidos, Entonces
actualiza el catálogo sin cambiar copias
históricas de recibos.
## RA4 IL4.1; RN-11
RF-M02-10El sistema debe permitir al
Propietario dar de baja lógica una
propiedad sin unidades activas.
MustDado una propiedad con todas sus unidades
Inactivas, Cuando se confirma la baja,
Entonces la excluye de nuevas selecciones y
conserva su historial.
## RA4 IL4.1; RN-11
RF-M02-11El sistema debe permitir al
Propietario marcar En
mantenimiento una unidad sin
contrato activo.
MustDado una unidad Disponible, Cuando se
confirma la restricción de mantenimiento,
Entonces impide nuevas activaciones hasta
restablecer Disponible.
## RA4 IL4.1; RN-01
## M03 Inquilinos
El expediente registra al titular contractual con los datos mínimos para distinguirlo y
contactarlo. Los adjuntos y el contacto de emergencia son opcionales y requieren una finalidad
documentada antes de utilizar datos reales. La búsqueda facilita seleccionar el registro existente
en lugar de crear otro por error. El historial reúne contratos y pagos mediante referencias, sin

## 32
duplicar sus importes en el expediente. Las correcciones de contacto no reescriben comprobantes
ya emitidos. La baja lógica conserva vínculos y queda reservada al Propietario cuando el
inquilino no tiene un contrato activo asociado.
La Tabla 16 presenta requisitos de Inquilinos.
## Tabla 16
Requisitos de Inquilinos
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M03-01El sistema debe registrar
inquilinos con nombre y un
medio de contacto.
MustDado datos mínimos válidos, Cuando se
confirma el alta, Entonces crea un inquilino
identificable sin exigir documento sensible.
## RA1 IL1.1; RA4 IL4.1;
## RN-18
RF-M03-02El sistema debe validar el
formato del correo cuando se
proporcione.
MustDado un correo sin dominio, Cuando se
guarda el formulario, Entonces señala el
campo y evita guardar hasta corregirlo.
## RA1 IL1.2; RA2 IL2.3;
## RN-18
RF-M03-03El sistema debe guardar un
contacto de emergencia opcional
separado del inquilino.
ShouldDado un contacto autorizado para esa
finalidad, Cuando se confirma su registro,
Entonces conserva nombre y teléfono sin
exigirlo al inquilino.
## RA4 IL4.1; RN-18
RF-M03-04El sistema debe advertir
coincidencias de identificación
antes de crear un inquilino.
MustDado un identificador ya registrado, Cuando
se intenta otra alta, Entonces muestra la
coincidencia y permite revisar el registro
existente.
## RA2 IL2.3; RN-18
RF-M03-05El sistema debe adjuntar
documentos opcionales en PDF,
PNG o JPEG.
ShouldDado un archivo válido de hasta cinco MiB,
Cuando se incorpora al expediente, Entonces
conserva archivo, nombre, tipo y huella
## SHA256.
## RA1 IL1.3; RA4 IL4.1;
## RN-25

## 33
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M03-06El sistema debe buscar inquilinos
por nombre o identificación.
MustDado varios inquilinos registrados, Cuando se
escribe un criterio, Entonces muestra
coincidencias sin exponer contraseñas ni
adjuntos.
## RA1 IL1.1; RA2 IL2.2;
## RN-18
RF-M03-07El sistema debe mostrar el
historial de contratos y pagos del
inquilino.
MustDado un inquilino con movimientos, Cuando
se abre su historial, Entonces presenta
vínculos a contratos, cargos y recibos.
## RA4 IL4.1; RN-14
RF-M03-08El sistema debe permitir al
Propietario desactivar un
inquilino sin contrato activo.
MustDado un inquilino sin contrato activo, Cuando
se confirma la baja, Entonces lo excluye de
nuevas selecciones y conserva movimientos.
## RA4 IL4.1; RN-11
RF-M03-09El sistema debe editar los medios
de contacto del inquilino con
auditoría.
MustDado un inquilino registrado, Cuando se
confirma el nuevo contacto, Entonces
actualiza el expediente y conserva la
referencia del cambio.
## RA4 IL4.1; RA2 IL2.3;
## RN-18
## M04 Contratos
El contrato fija la relación entre unidad, inquilino y calendario de obligaciones. El estado
Borrador permite revisar condiciones antes de activar ocupación y registrar la garantía. La
comprobación de superposición ocurre dentro de una transacción para cubrir activaciones
simultáneas. Renovar solo se inicia desde Vigente o Por vencer, conserva una referencia al
contrato anterior y crea un sucesor Borrador con sus propias condiciones. El cierre atómico
termina el anterior y activa el sucesor; cuenta como Terminar y se permite desde Vigente, Por
vencer o Vencido. La terminación detiene nuevas rentas posteriores y conserva obligaciones
existentes hasta su liquidación. H2 revisa vencimientos y distingue el aviso Por vencer del estado
Vencido, que requiere cierre explícito para liberar una unidad reservada.

## 34
La Tabla 17 presenta requisitos de Contratos.
## Tabla 17
Requisitos de Contratos
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M04-01El sistema debe crear contratos
en estado Borrador con unidad e
inquilino.
MustDado una unidad y un inquilino activos,
Cuando se guarda el formulario, Entonces crea
un Borrador sin generar ocupación ni cargos.
## RA1 IL1.1; RA4 IL4.1;
## RN-01
RF-M04-02El sistema debe validar fin >=
inicio + 1 mes calendario; un
mes natural completo del día 1 al
31 exige fin >= día 1 del mes
siguiente.
MustDado inicio 15 de enero y fin 14 de febrero
sintéticos, Cuando se intenta guardar,
Entonces rechaza la duración menor de un
mes. Dado inicio 1 de enero sintético, Cuando
se pretende cubrir el mes natural completo,
Entonces exige fin >= 1 de febrero.
## RA2 IL2.3; RN-02
RF-M04-03El sistema debe guardar el canon
contractual positivo en NIO o
## USD.
MustDado un Borrador válido, Cuando se fija el
canon, Entonces conserva monto y moneda
contractual.
## RA4 IL4.1; RN-03
RF-M04-04El sistema debe aceptar un día de
pago entre uno y treinta y uno.
MustDado un contrato con día treinta y uno,
Cuando se calcula un vencimiento de febrero,
Entonces usa el último día de febrero.
## RA2 IL2.3; RN-17
RF-M04-05El sistema debe impedir períodos
de ocupación superpuestos para
una unidad.
MustDado un contrato activo en el intervalo,
Cuando se intenta activar otro superpuesto,
Entonces rechaza la activación dentro de la
transacción.
## RA2 IL2.3; RA4 IL4.1;
## RN-01
RF-M04-06El sistema debe activar un
Borrador completo mediante
confirmación.
MustDado un Borrador sin conflictos, Cuando se
confirma activar, Entonces registra el depósito
y cambia a Vigente o Por vencer según fecha.
## RA4 IL4.1; RN-10
RF-M04-07El sistema debe adjuntar el
documento contractual opcional
en PDF.
ShouldDado un PDF admitido, Cuando se confirma
la incorporación, Entonces vincula el archivo
a la versión contractual.
## RA1 IL1.3; RA4 IL4.1;
## RN-25

## 35
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M04-08El sistema debe iniciar la
renovación solo desde Vigente o
Por vencer mediante un sucesor
Borrador enlazado.
MustDado un contrato Vigente o Por vencer,
Cuando se prepara la renovación, Entonces
crea el sucesor Borrador sin sobrescribir las
condiciones históricas; el cierre atómico de
Terminar termina el anterior y activa el
sucesor desde Vigente, Por vencer o Vencido.
## RA4 IL4.1; RN-19
RF-M04-09El sistema debe registrar la
terminación desde Vigente, Por
vencer o Vencido con fecha y
motivo, incluido el cierre
atómico del anterior y la
activación del sucesor.
MustDado un contrato Vigente, Por vencer o
Vencido, Cuando se confirma Terminar con
fecha y motivo, Entonces marca Terminado y
conserva cargos previos; si hay sucesor,
termina el anterior y lo activa en una
transacción atómica.
## RA4 IL4.1; RN-19
RF-M04-10El sistema debe marcar Por
vencer los contratos dentro de
treinta días del fin mediante H2.
MustDado un contrato activo a treinta días del fin,
Cuando H2 ejecuta su revisión, Entonces
actualiza el estado y deja la interfaz
disponible.
## RA3 IL3.1, IL3.3;
## RN-12
RF-M04-11El sistema debe cancelar
Borradores con motivo sin
generar movimientos financieros.
MustDado un Borrador sin cargos, Cuando se
confirma cancelar, Entonces marca Cancelado
y registra el motivo.
## RA4 IL4.1; RN-19
RF-M04-12El sistema debe mostrar contratos
filtrados por unidad, inquilino y
estado.
MustDado contratos en estados diferentes, Cuando
se aplican filtros, Entonces lista solo registros
coincidentes.
## RA1 IL1.1; RA2 IL2.2;
## RN-12
RF-M04-13El sistema debe editar las
condiciones de un contrato
Borrador antes de activar.
MustDado un Borrador registrado, Cuando se
confirman fechas y canon válidos, Entonces
guarda los cambios sin alterar ningún contrato
confirmado.
## RA4 IL4.1; RN-19

## 36
M05 Cargos y Cobranza
Los cargos representan obligaciones por concepto y vencimiento, con su moneda
contractual y período cuando corresponde. La generación mensual calcula una vista previa y
confirma cada contrato de forma independiente para permitir cancelación sin registros parciales.
H1 omite períodos existentes y H2 calcula mora sin usar recargos como base. El usuario registra
Reparación, Servicio u Otro con concepto suficiente para explicar su origen. Los pagos y
créditos cambian el saldo mediante aplicaciones identificadas. La cartera presenta antigüedad a
una fecha de corte y conserva las obligaciones anuladas para reconstruir el historial.
La Tabla 18 presenta requisitos de Cargos y Cobranza.
## Tabla 18
Requisitos de Cargos y Cobranza
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M05-01El sistema debe generar la renta
mensual de contratos elegibles
mediante H1.
MustDado contratos activos en el período elegido,
Cuando se confirma generar, Entonces crea un
cargo por contrato elegible con fecha y canon
vigentes.
## RA3 IL3.1; RA4 IL4.1;
## RN-04
RF-M05-02El sistema debe omitir rentas ya
generadas para el mismo contrato
y período.
MustDado un período con renta existente, Cuando
se repite H1, Entonces conserva un solo cargo
y reporta la omisión.
## RA2 IL2.3; RA3 IL3.2;
## RN-04
RF-M05-03El sistema debe mostrar el
progreso de H1 por contratos
procesados.
MustDado una generación en curso, Cuando
termina cada contrato, Entonces actualiza
cantidad procesada sobre total sin bloquear
controles.
## RA1 IL1.1; RA3 IL3.1;
## RN-20

## 37
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M05-04El sistema debe cancelar H1
entre contratos con transacciones
completas.
MustDado H1 activo con contratos pendientes,
Cuando se solicita cancelar, Entonces
conserva los confirmados y omite los
pendientes sin cargos parciales.
OperacionCanceladaException es cancelación
normal sin ERR; H1 conserva MSG-059,
termina la transacción del contrato en curso y
se detiene antes del siguiente. El resumen
identifica confirmados y pendientes.
## RA3 IL3.3; RN-20
RF-M05-05El sistema debe registrar cargos
manuales de Reparación,
Servicio u Otro con concepto.
MustDado un contrato seleccionado, Cuando se
confirma un monto positivo, Entonces crea el
cargo con tipo, vencimiento y autor.
## RA4 IL4.1; RN-23
RF-M05-06El sistema debe calcular la mora
mediante H2 una vez vencida la
gracia.
MustDado una renta con saldo y gracia cumplida,
Cuando H2 revisa el cargo, Entonces genera el
recargo conforme al registro de política
vigente y audita {politica_id, tipo, valor,
base}.
## RA3 IL3.1; RA4 IL4.1;
## RN-05
RF-M05-07El sistema debe excluir cargos de
Mora de la base de nuevos
recargos.
MustDado un contrato con mora previa, Cuando H2
calcula otro recargo, Entonces usa solo el
principal vencido elegible.
## RA2 IL2.3; RA3 IL3.2;
## RN-05
RF-M05-08El sistema debe derivar el estado
del cargo a partir del saldo y
vencimiento.
MustDado un cargo con abono y saldo positivo
vencido, Cuando se consulta el cargo,
Entonces muestra Vencido y el monto
abonado.
## RA4 IL4.1; RN-24
RF-M05-09El sistema debe permitir al
Propietario anular cargos sin
aplicaciones activas.
MustDado un cargo sin abonos activos, Cuando se
confirma motivo de anulación, Entonces
marca Anulado y conserva el importe
histórico.
## RA4 IL4.1; RN-13

## 38
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M05-10El sistema debe consultar cartera
por contrato, período y
antigüedad.
MustDado cargos con fechas distintas, Cuando se
fija una fecha de corte, Entonces agrupa saldos
en los intervalos definidos.
## RA2 IL2.2; RN-24
RF-M05-11El sistema debe aplicar
automáticamente el saldo a favor
al crear el siguiente cargo.
MustDado un contrato con crédito en su moneda,
Cuando se confirma nueva renta, Entonces
reduce el saldo mediante una aplicación
trazable sin otro recibo.
## RA4 IL4.1; RN-06
RF-M05-12El sistema debe previsualizar los
importes antes de confirmar una
generación.
ShouldDado contratos elegibles para un mes, Cuando
se solicita la vista previa, Entonces muestra
canon, período, vencimiento y omisiones sin
persistir cargos.
## RA1 IL1.3; RN-04
M06 Pagos y Recibos
El registro de un pago conserva el dinero recibido, la moneda original y su distribución a
cargos. El sistema prioriza vencimientos antiguos y permite seleccionar obligaciones del mismo
contrato sin superar su saldo. Una operación entre monedas exige una tasa autorizada para su
fecha y deja una copia inmutable de su procedencia. H4 protege el contador de recibos junto con
la transacción financiera y la bitácora. Una solicitud repetida recupera el recibo confirmado
mediante idempotencia. Las anulaciones requieren autorización del Propietario y reversión de
dependencias que hayan utilizado créditos posteriores.

## 39
La Tabla 19 presenta requisitos de Pagos y Recibos.
## Tabla 19
Requisitos de Pagos y Recibos
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M06-01El sistema debe registrar pagos
positivos vinculados a un
contrato.
MustDado un contrato y un método admitido,
Cuando se confirma un pago mayor que cero
con reloj válido, Entonces registra monto,
moneda, fecha efectiva, instante y fecha
operativa de la BD, método y usuario.
## RA1 IL1.1; RA4 IL4.1;
## RN-06
RF-M06-02El sistema debe aplicar pagos por
defecto al cargo vencido más
antiguo.
MustDado dos cargos vencidos con saldo, Cuando
se confirma un pago insuficiente para ambos,
Entonces abona primero el vencimiento más
antiguo.
## RA4 IL4.1; RN-06
RF-M06-03El sistema debe permitir
seleccionar manualmente cargos
del mismo contrato.
MustDado cargos abiertos del contrato, Cuando se
elige distribución válida, Entonces aplica los
montos indicados sin exceder saldos.
## RA1 IL1.2; RA4 IL4.1;
## RN-06
RF-M06-04El sistema debe conservar el
excedente como saldo a favor del
contrato.
MustDado un pago superior a la deuda, Cuando se
confirma el pago, Entonces registra el crédito
restante en moneda contractual.
## RA2 IL2.3; RA4 IL4.1;
## RN-06
RF-M06-05El sistema debe convertir pagos
USD o NIO a la moneda del
cargo con la tasa autorizada.
MustDado cargo USD y pago NIO con tasa de la
fecha, Cuando se confirma el pago, Entonces
divide NIO entre NIO por USD y guarda
ambos montos.
## RA5 IL5.3; RA4 IL4.1;
## RN-09
RF-M06-06El sistema debe guardar una
copia inmutable de la
procedencia de la tasa del pago.
MustDado un pago entre monedas, Cuando se
confirma la operación, Entonces conserva
tasa, fecha efectiva, fuente y condición manual
o comercial.
## RA5 IL5.2; RA4 IL4.1;
## RN-09

## 40
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M06-07El sistema debe asignar el
consecutivo de recibo mediante
la sección crítica H4.
MustDado dos pagos simultáneos válidos, Cuando
ambos confirman, Entonces entrega dos
números contiguos distintos dentro de sus
transacciones.
## RA3 IL3.2; RA2 IL2.3;
## RN-07
RF-M06-08El sistema debe generar un
recibo por cada pago confirmado.
MustDado un pago recién confirmado, Cuando se
abre su comprobante, Entonces muestra
número, contrato, monto, aplicaciones y saldo
a favor.
## RA4 IL4.2, IL4.3;
## RN-07
RF-M06-09El sistema debe permitir al
Propietario anular un recibo con
motivo.
MustDado un pago sin dependencias posteriores
activas, Cuando se confirma anular, Entonces
revierte sus aplicaciones y conserva el número
con estado Anulado.
## RA4 IL4.1; RA2 IL2.3;
## RN-08
RF-M06-10El sistema debe reimprimir el
recibo existente conservando
número y condición de anulado.
MustDado un recibo registrado o anulado, Cuando
se solicita reimpresión, Entonces produce la
misma identificación sin crear un pago.
## RA4 IL4.3; RN-08
RF-M06-11El sistema debe rechazar la
repetición de una misma solicitud
de pago.
MustDado una clave de idempotencia ya
confirmada, Cuando se reenvía la solicitud,
Entonces devuelve el recibo existente sin
duplicar monto.
## RA2 IL2.3; RA3 IL3.2;
## RN-21

## 41
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M06-12El sistema debe confirmar pago,
aplicaciones, recibo, contador y
auditoría en una transacción.
MustDado una falla anterior al commit, Cuando se
intenta registrar el pago, Entonces revierte
todos los cambios y conserva el consecutivo
disponible. La reserva del número, el pago,
sus aplicaciones, el recibo y la auditoría
utilizan una sola Session y una sola conexión.
Si la comprobación anterior al commit detecta
inconsistencia entre pago, aplicaciones,
movimientos de garantía o recibo,
IntegridadException usa ERR-063 /
MSG-086, revierte toda la operación y
conserva la causa técnica. MSG-086 solo se
muestra con rollback confirmado; commit
incierto conserva ERR-031 o ERR-054 /
MSG-064 y exige consultar el resultado antes
de reintentar.
## RA2 IL2.3; RA3 IL3.2;
## RA4 IL4.1; RN-14
M07 Depósitos de Garantía
La garantía registra dinero retenido por una finalidad distinta del cobro de renta. Al
activar un contrato, el sistema conserva su monto pactado ≥ 0 y moneda sin incluirlo en ingresos
de alquiler; con cero queda Retenido sin movimientos ni recibo. La liquidación permite aplicar
deducciones a obligaciones abiertas y devolver el remanente con autorización del Propietario.
Cada movimiento identifica su concepto y conserva la igualdad entre importe inicial,
aplicaciones, devoluciones y saldo retenido. Los estados del depósito resultan de esos
movimientos y admiten remanentes parciales. El documento de liquidación explica la
distribución sin producir un nuevo recibo de renta por dinero ya registrado.

## 42
La Tabla 20 presenta requisitos de Depósitos de Garantía.
## Tabla 20
Requisitos de Depósitos de Garantía
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M07-01El sistema debe registrar el
depósito pactado ≥ 0 al activar el
contrato.
MustDado un Borrador con depósito pactado ≥ 0,
Cuando se confirma la activación, Entonces
crea un depósito Retenido en moneda
contractual; con cero no genera movimientos
ni recibo. Variante de CP-017.
## RA4 IL4.1; RN-10
RF-M07-02El sistema debe mantener el
depósito separado del ingreso por
renta.
MustDado un depósito Retenido, Cuando se
consulta ingresos de renta, Entonces excluye
la garantía del total de renta.
## RA2 IL2.2; RN-10
RF-M07-03El sistema debe permitir al
Propietario aplicar parte del
depósito a deuda al terminar.
MustDado un contrato por terminar con cargos
abiertos, Cuando se confirma una aplicación
válida, Entonces reduce depósito y deuda por
igual con concepto. Si el cargo es inexistente,
está anulado o pertenece a otro contrato,
rechaza con CargoIncompatibleException,
ERR-061 / MSG-084, y conserva la propuesta.
Un importe superior al saldo conserva su error
específico.
## RA4 IL4.1; RN-10
RF-M07-04El sistema debe registrar cada
deducción del depósito con
importe y concepto.
MustDado una liquidación con deducción, Cuando
se intenta guardar sin concepto, Entonces
rechaza la deducción incompleta.
## RA1 IL1.2; RA2 IL2.3;
## RN-10
RF-M07-05El sistema debe registrar la
devolución parcial o total
autorizada por el Propietario.
MustDado un remanente de garantía, Cuando se
confirma la devolución, Entonces guarda
importe, fecha, método y beneficiario.
## RA4 IL4.1; RN-10

## 43
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M07-06El sistema debe derivar el estado
de depósito de sus movimientos
acumulados.
MustDado un depósito con devoluciones y
retención restante, Cuando se consulta su
estado, Entonces muestra Devuelto parcial y
su remanente.
## RA2 IL2.3; RN-26
RF-M07-07El sistema debe generar la
liquidación del depósito al
terminar el contrato.
MustDado aplicaciones y devoluciones
confirmadas, Cuando se solicita liquidación,
Entonces muestra importe inicial, deducciones
y remanente sin duplicar recibos de renta. Si
quedan decisiones, movimientos o remanente
sin resolver, bloquea terminar mediante
GarantiaPendienteException, ERR-062 /
MSG-085; la deuda residual puede mantenerse
después de terminar. Conserva la excepción de
garantía inicial cero sin movimientos
autorizada por DC-08.
## RA4 IL4.2, IL4.3;
## RN-26
## M08 Mantenimiento
Una solicitud vincula una necesidad de atención con la unidad afectada y mantiene su
descripción y prioridad. El responsable de atención es un dato operativo y no necesita una cuenta
del sistema. Los cambios de estado conservan sus fechas y respetan las transiciones definidas por
RN-27. Los costos efectivos se registran como gasto de la propiedad, separados de cualquier
cargo que se decida repercutir al inquilino. Una cancelación conserva el motivo y los costos
previos. El módulo facilita consultar el gasto por período sin convertir una solicitud en una
obligación contractual automática.

## 44
La Tabla 21 presenta requisitos de Mantenimiento.
## Tabla 21
Requisitos de Mantenimiento
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M08-01El sistema debe registrar
solicitudes de mantenimiento por
unidad con descripción.
MustDado una unidad existente, Cuando se guarda
la solicitud, Entonces crea una solicitud
Abierta con fecha y autor.
## RA1 IL1.1; RA4 IL4.1;
## RN-27
RF-M08-02El sistema debe asignar prioridad
Baja, Media o Alta a una
solicitud.
MustDado una solicitud Abierta, Cuando se elige
prioridad, Entonces guarda el valor elegido y
permite filtrarlo.
## RA4 IL4.1; RN-27
RF-M08-03El sistema debe asignar un
responsable nominal de atención.
ShouldDado una solicitud registrada, Cuando se
guarda el responsable, Entonces conserva su
nombre sin crear una cuenta obligatoria.
## RA4 IL4.1; RN-27
RF-M08-04El sistema debe cambiar
solicitudes entre estados
autorizados.
MustDado una solicitud En proceso, Cuando se
confirma resolver con fecha, Entonces marca
Resuelta y conserva el historial de estados.
Solo admite Abierta a En proceso o Cancelada
y En proceso a Resuelta o Cancelada; Resuelta
y Cancelada son terminales. Repetir el estado
actual devuelve ese estado sin duplicar
cambios ni eventos. Otra transición conserva
el estado anterior y produce
TransicionInvalidaException, ERR-066 /
## MSG-088.
## RA1 IL1.2; RA4 IL4.1;
## RN-27

## 45
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M08-05El sistema debe registrar costos
efectivos de mantenimiento por
moneda y fecha.
MustDado una solicitud con gasto comprobable
sintético, Cuando se confirma el costo,
Entonces vincula el gasto a la unidad y
propiedad sin cargar al inquilino. Si la unidad
de la solicitud pertenece a una propiedad
distinta de la seleccionada,
PropiedadIncompatibleException rechaza con
ERR-065 / MSG-087 y permite corregir la
selección.
## RA4 IL4.1; RN-23
RF-M08-06El sistema debe consultar gastos
de mantenimiento por propiedad
y período.
MustDado gastos de varias propiedades, Cuando se
aplican filtros, Entonces suma importes por
moneda sin duplicar solicitudes.
## RA2 IL2.2; RN-27
RF-M08-07El sistema debe registrar el
motivo de cancelación de una
solicitud.
ShouldDado una solicitud Abierta o En proceso,
Cuando se confirma cancelar con motivo,
Entonces marca Cancelada y conserva los
gastos existentes.
## RA4 IL4.1; RN-27
M09 Tipo de Cambio
El plan Free autenticado de ExchangeRate-API aporta una referencia orientativa para
USD/NIO. H3 consulta con HttpClient y actualiza una caché con fecha y origen visibles. Los
términos limitan su finalidad y la respuesta no autoriza una conversión financiera. Para registrar
pagos, el Propietario incorpora tasa, fuente admisible, fecha y motivo, como una referencia
publicada por el BCN. Cada operación conserva ese registro inmutable y bloquea la conversión
sin autorización válida; solo se admite una autorización por fecha efectiva y su corrección exige
procedimiento excepcional. Una falla externa permite continuar registros locales en una moneda
y consultar el historial de movimientos guardados.

## 46
La Tabla 22 presenta requisitos de Tipo de Cambio.
## Tabla 22
Requisitos de Tipo de Cambio
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M09-01El sistema debe consultar el
servicio USD/NIO mediante
HTTPS con HttpClient.
MustDado una conexión disponible, Cuando H3
solicita actualizar, Entonces envía una petición
al endpoint documentado para base USD.
## RA5 IL5.1; RA3 IL3.1;
## RN-09
RF-M09-02El sistema debe validar el JSON
del servicio, incluida la banda
técnica de 1 a 100 NIO/USD
inclusive, antes de aceptar una
referencia.
MustDado una respuesta inválida o NIO/USD fuera
de 1–100, Cuando se procesa el JSON,
Entonces no la guarda como referencia
vigente; para el valor fuera de banda registra
## M09, REFERENCIA_EN_CUARENTENA,
con valor y huella en bitacora, sin tabla nueva.
Exige base USD, NIO válido y fechas
coherentes; conserva la referencia anterior
ante rechazo o cuarentena. Los escenarios se
incorporan como variantes de CP-058.
## RA5 IL5.2; RN-09
RF-M09-03El sistema debe extraer la tasa
NIO numérica finita entre 1 y
100 inclusive del JSON válido.
MustDado una respuesta success con NIO
numérico finito entre 1 y 100 inclusive,
Cuando se procesa la respuesta, Entonces
conserva el valor decimal y la marca temporal
del proveedor.
## RA5 IL5.2; RN-09
RF-M09-04El sistema debe conservar una
caché local de la última respuesta
válida.
MustDado una consulta aceptada, Cuando se
confirma su almacenamiento, Entonces guarda
tasa, origen, consulta y fecha efectiva para uso
sin red.
## RA5 IL5.3; RA4 IL4.1;
## RN-22
RF-M09-05El sistema debe actualizar la tasa
mediante H3 según el intervalo
configurado con valor inicial de
veinticuatro horas.
MustDado una aplicación abierta y actualización
vencida, Cuando H3 se ejecuta, Entonces
consulta en segundo plano sin congelar la
interfaz.
## RA3 IL3.1, IL3.3; RA5
## IL5.1; RN-22

## 47
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M09-06El sistema debe mostrar la
antigüedad y procedencia de la
tasa en la interfaz.
MustDado una tasa en caché, Cuando se abre el
panel cambiario, Entonces muestra fecha
efectiva, consulta y origen comercial o
manual.
## RA5 IL5.3; RA1 IL1.1;
## RN-22
RF-M09-07El sistema debe permitir al
Propietario registrar una única
autorización de tasa por fecha
efectiva con motivo.
MustDado una falla de servicio o pago con fecha
efectiva histórica, Cuando se confirma tasa
positiva y referencia, Entonces guarda un
registro manual inmutable; si ya existe
autorización para esa fecha, rechaza con
ERR-060 y MSG-083: «Ya existe una tasa
autorizada para esa fecha»
(DuplicadoException). Si la tasa manual
positiva está fuera de 1 a 100 NIO/USD,
muestra advertencia de revisión y exige
confirmación del Propietario; la banda no
restringe la BD. La corrección exige
procedimiento excepcional.
## RA5 IL5.3; RA2 IL2.3;
## RN-22
RF-M09-08El sistema debe conservar un
historial inmutable de tasas
usadas en operaciones.
MustDado una tasa asociada a pago, Cuando se
registra otra tasa para otra fecha efectiva,
Entonces conserva el registro original del
pago; cada fecha admite una sola autorización
inmutable.
## RA4 IL4.1; RA5 IL5.3;
## RN-09
RF-M09-09El sistema debe rechazar un pago
entre monedas sin tasa autorizada
para su fecha.
MustDado caché fuera de la fecha del pago y sin
tasa manual, Cuando se intenta confirmar el
pago, Entonces bloquea la confirmación y
explica cómo obtener la tasa válida.
## RA5 IL5.3; RA1 IL1.3;
## RN-22
## M10 Reportes
Los reportes presentan filtros, moneda y corte identificados; REP-05 Ocupación toma el
instante de generación, sin selector de fecha histórica. JasperReports recibe datos preparados

## 48
mediante DTO para evitar que la plantilla escriba en la base o controle transacciones del negocio.
La generación y exportación ocurre mediante H5, con progreso y cancelación que no publica
archivos incompletos. Los totales de cada moneda permanecen separados y los pagos anulados se
distinguen de los cobros netos. El catálogo mínimo contiene diez reportes y sus columnas se fijan
en Requisitos de Reportes. Los archivos generados son comprobantes administrativos y no
facturas fiscales.
La Tabla 23 presenta requisitos de Reportes.
## Tabla 23
Requisitos de Reportes
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M10-01El sistema debe generar el recibo
de pago mediante JasperReports.
MustDado un recibo confirmado, Cuando se
solicita PDF, Entonces produce un archivo
que coincide con el registro.
## RA4 IL4.2, IL4.3;
## RN-07
RF-M10-02El sistema debe generar el
reporte de cartera vencida por
fecha de corte.
MustDado cargos abiertos a la fecha elegida,
Cuando se ejecuta el reporte, Entonces
presenta saldos y antigüedad por contrato.
## RA4 IL4.2, IL4.3;
## RN-24
RF-M10-03El sistema debe generar el
reporte de ingresos y gastos por
moneda.
MustDado movimientos en el período, Cuando se
genera el reporte, Entonces separa rentas,
otros cobros, gastos y garantías.
## RA4 IL4.2, IL4.3;
## RN-10
RF-M10-04El sistema debe generar el
reporte de ocupación por
propiedad.
MustDado unidades activas al instante de
generación, Cuando se solicita REP-05 sin
selector de fecha histórica, Entonces presenta
ocupadas sobre activas con denominador e
instante visibles.
## RA4 IL4.2, IL4.3;
## RN-01

## 49
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M10-05El sistema debe generar el estado
de cuenta de un contrato a una
fecha de corte.
MustDado un contrato con cargos y abonos,
Cuando se solicita su estado, Entonces detalla
movimientos, saldo por moneda y crédito
disponible.
## RA4 IL4.2, IL4.3;
## RN-24
RF-M10-06El sistema debe generar el
reporte de contratos por vencer
en el umbral configurado.
MustDado contratos activos con fecha final,
Cuando se solicita el reporte, Entonces lista
los que cumplen el intervalo sin incluir
terminados.
## RA4 IL4.2, IL4.3;
## RN-12
RF-M10-07El sistema debe generar el
historial de pagos del inquilino.
MustDado pagos registrados y anulados, Cuando se
consulta el período, Entonces identifica los
anulados y los excluye del total neto.
## RA4 IL4.2, IL4.3;
## RN-08
RF-M10-08El sistema debe generar el
reporte de liquidación de
depósitos.
MustDado depósitos con movimientos, Cuando se
solicita la liquidación, Entonces presenta
retenciones, aplicaciones y devoluciones
conciliables.
## RA4 IL4.2, IL4.3;
## RN-26
RF-M10-09El sistema debe exportar los
reportes tabulares a XLSX con
importes numéricos.
MustDado un reporte tabular REP-02 a REP-10 con
resultados, Cuando se elige XLSX mediante
FileChooser, Entonces crea hojas con
encabezados, filtros y moneda identificada;
REP-01 solo admite PDF.
## RA1 IL1.3; RA4 IL4.3;
## RN-28
RF-M10-10El sistema debe exportar los
reportes a PDF mediante H5.
MustDado un reporte seleccionado, Cuando se
confirma guardar, Entonces genera PDF en
segundo plano y muestra la ruta final.
## RA3 IL3.1, IL3.3; RA4
## IL4.3; RN-28
RF-M10-11El sistema debe generar el
reporte de mantenimiento por
propiedad y período.
MustDado un período con esos registros, Cuando se
elige el catálogo correspondiente, Entonces
produce solicitudes, costos y totales por
moneda.
## RA4 IL4.2, IL4.3;
## RN-28

## 50
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M10-12El sistema debe cancelar H5 sin
publicar archivos incompletos.
ShouldDada una exportación activa, Cuando se
solicita cancelar durante consulta, llenado o
exportación, Entonces H5 confirma la
cancelación cooperativa al terminar y limpiar
temporales, sin ERR y con MSG-089.
Conserva el archivo previo hasta publicación
exitosa; si la publicación ganó la carrera,
informa el resultado real. No revierte pagos ni
anulaciones ya confirmados.
## RA3 IL3.3; RA4 IL4.3;
## RN-28
RF-M10-13El sistema debe generar el
reporte de tasas utilizadas por
fecha y origen.
MustDado pagos con tasas históricas, Cuando se
solicita el reporte, Entonces presenta tasa,
fuente y pago sin redistribuir el catálogo
externo.
## RA4 IL4.2, IL4.3; RA5
## IL5.3; RN-28
## M11 Panel Principal
El panel resume ocupación al instante de generación y cobranza del período
seleccionado, y permite abrir los registros que componen cada indicador. La ocupación no
admite selector de fecha histórica; su instante y denominador son visibles. Los contratos por
vencer dependen de la revisión H2 y mantienen el mismo umbral que la lista detallada. La tasa
muestra origen y antigüedad, con un estado explícito cuando falta información disponible. Los
importes de NIO y USD se presentan por separado. La navegación desde un indicador conserva
los filtros para que el usuario pueda revisar su composición sin reconstruir la consulta.

## 51
La Tabla 24 presenta requisitos de Panel Principal.
## Tabla 24
Requisitos de Panel Principal
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M11-01El sistema debe mostrar el
porcentaje de ocupación con
corte en el instante de
generación, sin selector de fecha
histórica.
MustDado unidades activas con ocupación
conocida, Cuando se actualiza el panel,
Entonces muestra ocupadas sobre activas e
instante de generación, y usa cero cuando no
hay unidades.
## RA1 IL1.1; RA2 IL2.2;
## RN-01
RF-M11-02El sistema debe mostrar lo
cobrado y por cobrar por moneda
y período.
MustDado movimientos financieros existentes,
Cuando se aplica el filtro temporal, Entonces
presenta cobros netos y saldos sin sumar NIO
con USD.
## RA2 IL2.2; RA1 IL1.1;
## RN-24
RF-M11-03El sistema debe mostrar la mora
exigible separada del principal.
MustDado cargos de Mora con saldo, Cuando se
abre el panel, Entonces presenta su total en la
moneda de cada contrato.
## RA2 IL2.2; RN-05
RF-M11-04El sistema debe mostrar los
contratos por vencer detectados
por H2.
MustDado contratos en el umbral, Cuando H2
termina la revisión, Entonces actualiza el
contador y permite abrir su lista.
## RA3 IL3.1; RA1 IL1.2;
## RN-12
RF-M11-05El sistema debe mostrar la tasa
disponible con origen y
antigüedad.
MustDado una tasa aceptada o ausencia de tasa,
Cuando se abre el panel, Entonces muestra la
información o el estado Sin tasa disponible.
## RA5 IL5.3; RA1 IL1.1;
## RN-22
RF-M11-06El sistema debe abrir la lista
filtrada desde cada indicador.
ShouldDado un indicador de cartera con resultados,
Cuando se activa el indicador, Entonces abre
la vista de cargos con los mismos filtros.
## RA1 IL1.2, IL1.3;
## RN-24
RF-M11-07El sistema debe cambiar entre
Claro y Oscuro desde la ventana
principal sin perder entradas.
MustDado un pago sin confirmar, Cuando el
Asistente cambia el tema, Entonces conserva
campos, foco, filtros y permiso del formulario.
## RA1 IL1.1, IL1.2;
## RN-35, RN-37

## 52
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M11-08El sistema debe guardar la
elección de tema del usuario en
preferencias locales.
MustDado un usuario autenticado, Cuando elige un
tema y reinicia, Entonces restaura ese valor
después del acceso sin usar otra cuenta como
clave.
## RA1 IL1.2; RN-36
RF-M11-09El sistema debe ofrecer la
modalidad sin efectos en ambos
temas.
MustDado Claro u Oscuro, Cuando se desactivan
efectos, Entonces elimina luces y desenfoques
y conserva lectura de pagos y saldos y
operación por teclado.
## RA1 IL1.1, IL1.2;
## RN-38
RF-M11-10El sistema debe navegar por
módulos con barra lateral y barra
superior de contexto en una
ventana principal.
MustDado un módulo abierto, Cuando el usuario
elige otro autorizado, Entonces cambia el
contenido y mantiene disponibles menús y
diálogos pertinentes.
## RA1 IL1.1, IL1.3;
## RN-37
M12 Configuración y Auditoría
La configuración conserva parámetros operativos por inserción de registros con vigencia,
motivo y autor, sin UPDATE ni columna version; el vigente es el último con vigente_desde ≤
now() de la BD. También conserva los datos que aparecerán en nuevos recibos. Un cambio de
política no modifica movimientos confirmados ni documentos históricos. La auditoría conserva
el actor y la correlación de operaciones financieras, con consulta restringida y sin edición desde
los roles del sistema. El respaldo manual cifra base y adjuntos con AES-256-GCM y contraseña
de respaldo, junto con un manifiesto verificable; restaurar requiere autenticidad verificada antes
de descifrar, mantenimiento y copia previa. H6 coordina la detención de tareas al salir. Una
transacción que no finaliza dentro del umbral impide un cierre silencioso y presenta su estado al
usuario.

## 53
La Tabla 25 presenta requisitos de Configuración y Auditoría.
## Tabla 25
Requisitos de Configuración y Auditoría
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M12-01El sistema debe permitir al
Propietario añadir registros de
parámetros operativos sin
actualizar los anteriores.
MustDado un Propietario autenticado, Cuando
confirma valores válidos de la Tabla A3,
Entonces inserta clave, valor, vigente_desde,
motivo y creado_en/por; UNIQUE(clave,
vigente_desde), sin UPDATE ni columna
version. La vigente es la última fila con
vigente_desde ≤ now() de la BD. Para días o
montos configurables negativos cubiertos por
MSG-051, ParametroInvalidoException utiliza
ERR-064 / MSG-051 y conserva el
formulario; otras validaciones mantienen sus
mensajes específicos.
## RA2 IL2.2, IL2.3;
## RN-05
RF-M12-02El sistema debe guardar los datos
del propietario para los recibos.
MustDado datos de identificación y contacto
válidos, Cuando se confirma configuración,
Entonces usa esos datos en nuevos recibos sin
alterar los emitidos.
## RA1 IL1.1; RA4 IL4.1;
## RN-28
RF-M12-03El sistema debe incorporar un
logotipo opcional en PNG o
## JPEG.
CouldDado un archivo admitido, Cuando se
confirma la selección, Entonces guarda una
copia controlada para los reportes nuevos.
## RA1 IL1.3; RN-25
RF-M12-04El sistema debe registrar la
bitácora de operaciones
financieras y cambios de acceso.
MustDado una transacción confirmada, Cuando se
consulta su evento, Entonces muestra actor,
instante, acción y referencia sin contraseñas.
## RA2 IL2.2, IL2.3;
## RN-14
RF-M12-05El sistema debe permitir al
Propietario consultar auditoría
con filtros.
MustDado eventos en distintos períodos, Cuando se
aplican usuario, acción y fechas, Entonces
lista eventos coincidentes sin opción de
edición.
## RA1 IL1.1; RA2 IL2.2;
## RN-14

## 54
IdentificadorRequisitoPrioridadCriterio de AceptaciónTrazabilidad
RF-M12-06El sistema debe crear un respaldo
manual consistente de base de
datos y adjuntos.
MustDado un destino escribible con espacio
suficiente y contraseña de respaldo, Cuando el
Propietario confirma respaldar, Entonces
genera un paquete AES-256-GCM con
manifiesto, hashes y hora de corte.
## RA2 IL2.1; RA3 IL3.2;
## RN-29
RF-M12-07El sistema debe restaurar un
respaldo autorizado en modo de
mantenimiento.
MustDado un respaldo y ninguna sesión operativa,
Cuando el Propietario confirma restaurar con
su contraseña de respaldo, Entonces verifica
autenticidad, descifra y recupera datos y
adjuntos con copia previa; clave o autenticidad
inválida: ERR-059/MSG-082, sin restaurar.
## RA2 IL2.3; RA3 IL3.2;
## RN-29
RF-M12-08El sistema debe detener H1, H2,
H3 y H5 mediante H6 al cerrar la
aplicación.
MustDado tareas en curso con cambios pendientes,
Cuando se confirma salir, Entonces solicita
cancelación, espera transacciones y cierra
recursos.
## RA3 IL3.1, IL3.2,
## IL3.3; RN-30
Historias de Usuario
Las historias agrupan necesidades del Propietario y del Asistente sin reemplazar los
criterios individuales de los requisitos funcionales. Los puntos de historia expresan tamaño
relativo propuesto: el patrón inicial es tres para el primer grupo de cada módulo, cinco para el
segundo y ocho para el tercero; M01 y M11 incluyen historias adicionales con los puntos
indicados en sus tablas. La estimación prioriza discusión de complejidad y no se convierte de
forma automática en horas ni acredita velocidad del equipo. Cada grupo mantiene referencias
explícitas a RF y su recorrido CU correspondiente. El Apéndice C amplía la trazabilidad hasta
las reglas de negocio y los resultados e indicadores de aprendizaje.

## 55
M01 Seguridad y Usuarios
La Tabla 26 presenta historias de Seguridad y Usuarios.

## 56
## Tabla 26
Historias de Seguridad y Usuarios
IDHistoriaRequisitos RelacionadosPuntos
HU-M01-01Como Propietario, quiero iniciar
sesión con mi cuenta para atribuir
mis operaciones.
## RF-M01-01, RF-M01-023
HU-M01-02Como Propietario, quiero
administrar accesos y bloqueo para
limitar el uso de cuentas; como
Propietario o Asistente, quiero
cambiar mi propia contraseña para
proteger mi acceso.
## RF-M01-03, RF-M01-04, RF-
## M01-05, RF-M01-06, RF-M01-
## 09, RF-M01-10, RF-M01-11
## 5
HU-M01-03Como Asistente, quiero cerrar mi
sesión para proteger la operación
compartida.
## RF-M01-07, RF-M01-088
HU-M01-04Como Asistente, quiero recuperar mi
tema al acceder para leer con mi
preferencia personal.
## RF-M01-12, RF-M01-13, RF-
## M01-14, RF-M01-15
## 8
M02 Propiedades y Unidades
La Tabla 27 presenta historias de Propiedades y Unidades.
## Tabla 27
Historias de Propiedades y Unidades
IDHistoriaRequisitos RelacionadosPuntos
HU-M02-01Como Propietario, quiero registrar
propiedades y unidades para
organizar el inventario alquilable.
## RF-M02-01, RF-M02-02, RF-
## M02-03, RF-M02-09
## 3
HU-M02-02Como Asistente, quiero actualizar
condiciones y foto de la unidad para
mantener su información de
referencia.
## RF-M02-04, RF-M02-055
HU-M02-03Como Propietario, quiero controlar
disponibilidad y bajas para conservar
el historial de ocupación.
## RF-M02-06, RF-M02-07, RF-
## M02-08, RF-M02-10, RF-M02-
## 11
## 8

## 57
## M03 Inquilinos
La Tabla 28 presenta historias de Inquilinos.
## Tabla 28
Historias de Inquilinos
IDHistoriaRequisitos RelacionadosPuntos
HU-M03-01Como Asistente, quiero registrar
datos de contacto para identificar al
titular del contrato.
## RF-M03-01, RF-M03-02, RF-
## M03-09
## 3
HU-M03-02Como Asistente, quiero gestionar el
expediente del inquilino para
localizar sus documentos y
contactos.
## RF-M03-03, RF-M03-04, RF-
## M03-05
## 5
HU-M03-03Como Propietario, quiero revisar
historial y bajas para evitar perder
los movimientos anteriores.
## RF-M03-06, RF-M03-07, RF-
## M03-08
## 8
## M04 Contratos
La Tabla 29 presenta historias de Contratos.
## Tabla 29
Historias de Contratos
IDHistoriaRequisitos RelacionadosPuntos
HU-M04-01Como Asistente, quiero preparar el
contrato y sus fechas para
documentar las condiciones
pactadas.
## RF-M04-01, RF-M04-02, RF-
## M04-03, RF-M04-04, RF-M04-
## 13
## 3
HU-M04-02Como Asistente, quiero activar un
contrato sin superposición para
asignar la unidad correcta.
## RF-M04-05, RF-M04-06, RF-
## M04-07
## 5
HU-M04-03Como Asistente, quiero preparar la
renovación o terminación para
mantener actualizado el calendario;
el Propietario confirma el cierre con
garantía/liquidación, salvo garantía
inicial cero sin movimientos, que
también puedo confirmar.
## RF-M04-08, RF-M04-09, RF-
## M04-10, RF-M04-11, RF-M04-
## 12
## 8

## 58
M05 Cargos y Cobranza
La Tabla 30 presenta historias de Cargos y Cobranza.
## Tabla 30
Historias de Cargos y Cobranza
IDHistoriaRequisitos RelacionadosPuntos
HU-M05-01Como Asistente, quiero generar los
cargos de un mes para registrar
obligaciones sin duplicados.
## RF-M05-01, RF-M05-02, RF-
## M05-03, RF-M05-04, RF-M05-
## 12
## 3
HU-M05-02Como Asistente, quiero revisar
cargos manuales y mora para
conocer la deuda exigible.
## RF-M05-05, RF-M05-06, RF-
## M05-07, RF-M05-08
## 5
HU-M05-03Como Propietario, quiero consultar y
ajustar cargos autorizados para
mantener una cartera trazable.
## RF-M05-09, RF-M05-10, RF-
## M05-11
## 8
M06 Pagos y Recibos
La Tabla 31 presenta historias de Pagos y Recibos.
## Tabla 31
Historias de Pagos y Recibos
IDHistoriaRequisitos RelacionadosPuntos
HU-M06-01Como Asistente, quiero registrar
abonos y excedentes para reflejar el
dinero recibido.
## RF-M06-01, RF-M06-02, RF-
## M06-03, RF-M06-04
## 3
HU-M06-02Como Asistente, quiero cobrar en
otra moneda con recibo único para
documentar la conversión utilizada.
## RF-M06-05, RF-M06-06, RF-
## M06-07, RF-M06-08
## 5
HU-M06-03Como Propietario, quiero anular y
revisar recibos para corregir errores
con trazabilidad.
## RF-M06-09, RF-M06-10, RF-
## M06-11, RF-M06-12
## 8

## 59
M07 Depósitos de Garantía
La Tabla 32 presenta historias de Depósitos de Garantía.
## Tabla 32
Historias de Depósitos de Garantía
IDHistoriaRequisitos RelacionadosPuntos
HU-M07-01Como Asistente, quiero registrar la
garantía separada de la renta para
evitar mezclar su finalidad.
## RF-M07-01, RF-M07-023
HU-M07-02Como Propietario, quiero aplicar
deducciones justificadas para
liquidar las deudas autorizadas.
## RF-M07-03, RF-M07-045
HU-M07-03Como Propietario, quiero devolver el
remanente al terminar para
documentar la liquidación.
## RF-M07-05, RF-M07-06, RF-
## M07-07
## 8
## M08 Mantenimiento
La Tabla 33 presenta historias de Mantenimiento.
## Tabla 33
Historias de Mantenimiento
IDHistoriaRequisitos RelacionadosPuntos
HU-M08-01Como Asistente, quiero abrir y
priorizar solicitudes para ordenar la
atención de las unidades.
## RF-M08-01, RF-M08-023
HU-M08-02Como Asistente, quiero actualizar
atención y costos para registrar el
trabajo efectuado.
## RF-M08-03, RF-M08-04, RF-
## M08-05
## 5
HU-M08-03Como Asistente, quiero consultar
gastos y cancelaciones para rendir
cuenta por propiedad.
## RF-M08-06, RF-M08-078

## 60
M09 Tipo de Cambio
La Tabla 34 presenta historias de Tipo de Cambio.
## Tabla 34
Historias de Tipo de Cambio
IDHistoriaRequisitos RelacionadosPuntos
HU-M09-01Como Asistente, quiero consultar el
servicio USD/NIO para obtener una
referencia identificable.
## RF-M09-01, RF-M09-02, RF-
## M09-03
## 3
HU-M09-02Como Asistente, quiero ver la caché
y su antigüedad para distinguir una
tasa utilizable.
## RF-M09-04, RF-M09-05, RF-
## M09-06
## 5
HU-M09-03Como Propietario, quiero autorizar
la tasa de respaldo para resolver
pagos sin servicio disponible.
## RF-M09-07, RF-M09-08, RF-
## M09-09
## 8
## M10 Reportes
La Tabla 35 presenta historias de Reportes.
## Tabla 35
Historias de Reportes
IDHistoriaRequisitos RelacionadosPuntos
HU-M10-01Como Asistente, quiero generar
reportes de cartera e ingresos para
revisar la operación del período.
## RF-M10-01, RF-M10-02, RF-
## M10-03, RF-M10-04
## 3
HU-M10-02Como Asistente, quiero emitir el
estado de cuenta y la liquidación
para explicar los movimientos del
contrato.
## RF-M10-05, RF-M10-06, RF-
## M10-07, RF-M10-08
## 5
HU-M10-03Como Asistente, quiero exportar
PDF y XLSX en segundo plano para
entregar archivos y seguir operando.
## RF-M10-09, RF-M10-10, RF-
## M10-11, RF-M10-12, RF-M10-
## 13
## 8

## 61
## M11 Panel Principal
La Tabla 36 presenta historias de Panel Principal.
## Tabla 36
Historias de Panel Principal
IDHistoriaRequisitos RelacionadosPuntos
HU-M11-01Como Propietario, quiero consultar
ocupación y cobranza para revisar
los resultados del período.
## RF-M11-01, RF-M11-023
HU-M11-02Como Asistente, quiero ver alertas
de mora y vencimiento para priorizar
la revisión de contratos.
## RF-M11-03, RF-M11-045
HU-M11-03Como Asistente, quiero abrir el
detalle de los indicadores para
explicar su cálculo.
## RF-M11-05, RF-M11-068
HU-M11-04Como Propietario, quiero cambiar y
conservar mi tema para mantener mi
presentación entre sesiones.
## RF-M11-07, RF-M11-083
HU-M11-05Como Asistente, quiero desactivar
efectos y recorrer módulos para
operar con superficies uniformes.
## RF-M11-09, RF-M11-105
M12 Configuración y Auditoría
La Tabla 37 presenta historias de Configuración y Auditoría.
## Tabla 37
Historias de Configuración y Auditoría
IDHistoriaRequisitos RelacionadosPuntos
HU-M12-01Como Propietario, quiero configurar
parámetros y datos de recibos para
aplicar políticas versionadas.
## RF-M12-01, RF-M12-02, RF-
## M12-03
## 3
HU-M12-02Como Propietario, quiero consultar
la auditoría para reconstruir cambios
financieros.
## RF-M12-04, RF-M12-055
HU-M12-03Como Propietario, quiero respaldar,
restaurar y cerrar las tareas para
conservar una operación
recuperable.
## RF-M12-06, RF-M12-07, RF-
## M12-08
## 8

## 62
Casos de Uso
Diagrama de Casos de Uso
La Figura 1 representa los doce recorridos críticos detallados y sus actores principales
dentro del límite de Latch. El catálogo amplía esa vista a treinta y nueve casos, vinculados a los
módulos y las historias del producto. Propietario y Asistente realizan operaciones autenticadas;
el proveedor de tasas es un sistema externo consultado por HTTP. El inquilino recibe
documentos fuera de la aplicación y es destinatario sin cuenta operativa. Las asociaciones del
diagrama describen interacción y no secuencia temporal, mientras que los flujos detallados fijan
el orden de confirmaciones y sus alternativas de error.

## 63
## Figura 1
Casos Críticos y Actores de Latch
Nota. Vista de doce casos críticos. El Propietario también realiza los recorridos comunes
asociados al Asistente; véase la Tabla 7. El catálogo contiene treinta y nueve casos. El proveedor
externo participa solo en CU-25.
## Catálogo
La Tabla 38 presenta catálogo de Casos de Uso.

## 64
## Tabla 38
Catálogo de Casos de Uso
IDNombreActor PrincipalMóduloHistoria
CU-01Iniciar sesiónPropietarioM01HU-M01-01
CU-02Administrar acceso y bloqueo; cambiar
contraseña propia
Propietario para administrar
cuentas; Propietario o Asistente
para cambiar su contraseña
propia.
## M01HU-M01-02
CU-03Cerrar sesiónAsistenteM01HU-M01-03
CU-04Registrar inventarioPropietarioM02HU-M02-01
CU-05Editar condiciones de unidadAsistenteM02HU-M02-02
CU-06Consultar ocupación y dar de bajaPropietarioM02HU-M02-03
CU-07Registrar inquilinoAsistenteM03HU-M03-01
CU-08Gestionar expedienteAsistenteM03HU-M03-02
CU-09Consultar historial y desactivarPropietarioM03HU-M03-03
CU-10Crear contratoAsistenteM04HU-M04-01
CU-11Activar contratoAsistenteM04HU-M04-02
CU-12Renovar desde Vigente o Por vencer creando
sucesor Borrador; terminar desde Vigente, Por
vencer o Vencido, incluido el cierre atómico del
anterior y la activación del sucesor.
Asistente para preparar
renovación y terminación;
Propietario para confirmar el
cierre con garantía/liquidación,
salvo garantía inicial cero sin
movimientos, que también
puede confirmar el Asistente.
## M04HU-M04-03
CU-13Generar cargos del mesAsistenteM05HU-M05-01
CU-14Calcular mora y cargos manualesAsistenteM05HU-M05-02
CU-15Revisar cartera y créditoPropietarioM05HU-M05-03

## 65
IDNombreActor PrincipalMóduloHistoria
CU-16Registrar pago parcialAsistenteM06HU-M06-01
CU-17Registrar pago en otra monedaAsistenteM06HU-M06-02
CU-18Anular reciboPropietarioM06HU-M06-03
CU-19Registrar depósitoAsistenteM07HU-M07-01
CU-20Aplicar deduccionesPropietarioM07HU-M07-02
CU-21Terminar contrato con devolución de depósitoPropietarioM07HU-M07-03
CU-22Abrir solicitud de mantenimientoAsistenteM08HU-M08-01
CU-23Atender solicitud y registrar costosAsistenteM08HU-M08-02
CU-24Consultar gastos y cancelar solicitudAsistenteM08HU-M08-03
CU-25Actualizar tipo de cambioAsistenteM09HU-M09-01
CU-26Consultar caché cambiariaAsistenteM09HU-M09-02
CU-27Autorizar tasa manualPropietarioM09HU-M09-03
CU-28Generar reportes de gestiónAsistenteM10HU-M10-01
CU-29Generar estado de cuentaAsistenteM10HU-M10-02
CU-30Exportar reporteAsistenteM10HU-M10-03
CU-31Consultar indicadoresPropietarioM11HU-M11-01
CU-32Revisar alertasAsistenteM11HU-M11-02
CU-33Abrir detalle del panelAsistenteM11HU-M11-03
CU-34Configurar parámetrosPropietarioM12HU-M12-01
CU-35Consultar auditoríaPropietarioM12HU-M12-02
CU-36Respaldar y cerrar la aplicaciónPropietario para respaldo;
Propietario o Asistente para
cierre
## M12HU-M12-03
CU-37Seleccionar presentación en el accesoPropietario o AsistenteM01HU-M01-04
CU-38Cambiar y conservar tema personalPropietario o AsistenteM11HU-M11-04
CU-39Operar sin efectos y navegar módulosPropietario o AsistenteM11HU-M11-05

## 66
Nota. Los RF relacionados con cada caso constan por identificador individual en la Tabla C1, sin repetir esa matriz en este catálogo.
## Especificación Detallada
Cada caso crítico distingue la operación confirmada del intento fallido mediante
precondiciones, disparador y estado final. Los pasos numerados quedan dentro de tablas como
procedimiento de revisión. Un error previo al commit revierte la transacción y muestra una causa
que permita actuar. Las alternativas describen cambios de entrada o permiso y su efecto sobre
saldos. Los ejemplos son sintéticos, salvo la referencia oficial del BCN citada. La revisión
prevista cubrirá cada flujo principal y una alternativa antes de aceptar los requisitos de la versión
demostrable. Cada PAN define entrada, acciones, salida y estados aplicables de carga, vacío,
error y éxito. Al abandonar un formulario modificado se ofrecen Guardar, Descartar o Continuar
editando; una validación fallida conserva los datos válidos no secretos. Estas variantes utilizan
los casos existentes del Plan, sin ampliar los 100 CP. La respuesta distingue validación
rechazada, rollback confirmado, cancelación normal, commit incierto y fallo posterior al commit;
conserva los datos necesarios y ofrece acciones compatibles con la fase real. El Brief fija los
literales MSG, FLU la situación y recuperación, y TRD las excepciones por escenario; un ERR
puede repetirse si conserva su significado.
CU-01 Iniciar Sesión
La Tabla 39 presenta especificación de CU-01.

## 67
## Tabla 39
Especificación de CU-01
CampoDetalle
Actor y móduloPropietario; M01
PrecondicionesCuenta activa, rol asignado y conexión local a PostgreSQL disponible.
DisparadorEl usuario activa Ingresar.
Flujo principal1. Escribe usuario y contraseña.
- El sistema consulta la cuenta con PreparedStatement.
- Comprueba hash y bloqueo.
- Registra el acceso y reinicia fallos.
- Abre el panel del rol.
Flujos alternosContraseña incorrecta: incrementa fallos y muestra mensaje genérico.
Quinto fallo: bloquea quince minutos. Cuenta bloqueada: informa
restricción sin abrir sesión.
ExcepcionesBase no disponible: no autentica ni revela detalles de conexión; permite
reintentar sin sesión parcial.
PostcondicionesSesión atribuida a una cuenta; intento y resultado auditados.
Reglas y requisitosRN-13, RN-15; RF-M01-01 a RF-M01-06
Datos sintéticos y
criterio
Cuenta sintética propietario.demo; acceso correcto una vez y cinco fallos
con otra cuenta. Cero sesiones tras fallo; una sesión del rol correcto tras
credencial válida.
CU-10 Crear Contrato
La Tabla 40 presenta especificación de CU-10.
## Tabla 40
Especificación de CU-10
CampoDetalle
Actor y móduloAsistente; M04
PrecondicionesPropiedad, unidad e inquilino activos; el Asistente o Propietario está
autenticado.
DisparadorEl actor elige Nuevo contrato.
Flujo principal1. Selecciona unidad e inquilino.
- Registra inicio, fin, canon, moneda y día de pago.
## 3. Guarda Borrador.
- Revisa garantía y condiciones.
- Confirma activar.
- El sistema bloquea la unidad, comprueba superposición y confirma
contrato y depósito.

## 68
CampoDetalle
Flujos alternosSolo guardar: queda Borrador sin ocupación. Inicio posterior a la fecha de
activación: conserva Borrador conforme a FLU-16 (Cuadra et al., 2026b).
Día 31 en mes corto: vencimiento al último día. Otro contrato activo
superpuesto: rechaza activar.
ExcepcionesFalla al registrar depósito: revierte la activación completa. Conflicto
concurrente: conserva un solo contrato activo.
PostcondicionesContrato Vigente o Por vencer, unidad Ocupada y depósito Retenido
cuando corresponde.
Reglas y requisitosRN-01, RN-02, RN-03, RN-10, RN-17; RF-M04-01 a RF-M04-06; RF-
## M07-01
Datos sintéticos y
criterio
Contrato sintético con inicio 23/10/2026, fin 23/10/2027 y activación
23/10/2026, US$ 100.00 y garantía US$ 100.00. Una ocupación activa y
un depósito; cero movimientos si falla el commit.
CU-13 Generar Cargos del Mes
La Tabla 41 presenta especificación de CU-13.
## Tabla 41
Especificación de CU-13
CampoDetalle
Actor y móduloAsistente; M05
PrecondicionesContratos elegibles activos y período seleccionado; no hay otro H1 local en
curso.
DisparadorEl actor solicita Generar cargos del mes.
Flujo principal1. Elige período AAAA-MM.
- Revisa vista previa de elegibles y omisiones.
## 3. Confirma H1.
- H1 bloquea cada contrato y comprueba renta existente.
- Confirma cargo y auditoría por contrato.
- Actualiza progreso y resumen.
Flujos alternosPeríodo repetido: omite renta existente. Cancelación: termina el contrato en
curso y no inicia el siguiente. Crédito disponible: aplica crédito sin emitir
otro recibo.
ExcepcionesSQLSTATE 40P01/40001: rollback y hasta tres intentos totales con
Session nueva y mismo UUID; esperar 100 y 250 ms más jitter de 0–50 ms
fuera de transacción. Escrituras ordinarias: lock_timeout 2000 ms y
statement_timeout 5000 ms. Falla global: conserva solo transacciones
confirmadas.
PostcondicionesUna renta por contrato y período confirmado; resumen de generados,
omitidos y cancelados.

## 69
CampoDetalle
Reglas y requisitosRN-04, RN-06, RN-17, RN-20; RF-M05-01 a RF-M05-04; RF-M05-11;
## RF-M05-12
Datos sintéticos y
criterio
Lote sintético de 500 contratos elegibles, 20 rentas ya creadas y una
cancelación tras 100 confirmaciones. Cero duplicados; progreso coherente;
interfaz responde en 200 ms o menos.
CU-16 Registrar Pago Parcial
La Tabla 42 presenta especificación de CU-16.
## Tabla 42
Especificación de CU-16
CampoDetalle
Actor y móduloAsistente; M06
PrecondicionesContrato con cargos y sesión de cobro autorizada; UUID de solicitud
nuevo.
DisparadorEl actor confirma Registrar pago.
Flujo principal1. Selecciona contrato.
- Ingresa fecha efectiva, método, moneda y monto; el sistema obtiene
instante y fecha operativa de now() de la BD y bloquea si el desfase local
supera cinco minutos (ERR-057).
- Revisa aplicación al vencido más antiguo.
- Confirma el abono.
- H4 bloquea contador y cargos en orden estable.
- Confirma pago, aplicaciones, recibo y auditoría juntos.
- Muestra recibo.
Flujos alternosSelección manual: distribuye a cargos del mismo contrato. Excedente:
conserva crédito. Doble activación del botón: devuelve el recibo existente
por UUID.
ExcepcionesFalla antes de commit: cero cambios y número disponible. Cargo cambiado
por otro usuario: recalcula saldo y exige nueva confirmación.
PostcondicionesPago Registrado y saldos reducidos por el abono; recibo único accesible
después de commit.
Reglas y requisitosRN-06, RN-07, RN-14, RN-21, RN-32; RF-M06-01 a RF-M06-04; RF-
## M06-07; RF-M06-08; RF-M06-11; RF-M06-12
Datos sintéticos y
criterio
Dos rentas sintéticas NIO de C$ 1,000.00; pago C$ 600.00; UUID fijo para
el reintento. Saldo total C$ 1,400.00, un pago y un recibo aun con dos
solicitudes iguales.

## 70
CU-17 Registrar Pago en Otra Moneda
La Tabla 43 presenta especificación de CU-17.
## Tabla 43
Especificación de CU-17
CampoDetalle
Actor y móduloAsistente; M06
PrecondicionesContrato USD o NIO; tasa autorizada para la fecha efectiva del pago y
fuente identificada.
DisparadorEl actor elige una moneda de pago distinta.
Flujo principal1. Registra importe original y fecha efectiva, separada de la fecha
operativa.
- El sistema obtiene instante y fecha operativa de now() de la BD; verifica
desfase local ≤ cinco minutos y busca la tasa autorizada de la fecha
efectiva.
- Presenta origen, valor y conversión.
- El actor confirma la distribución.
- Convierte con decimal y redondea el total.
- Confirma la transacción financiera y copia inmutable de tasa.
Flujos alternosSolo existe tasa externa informativa: solicita autorización de fuente al
Propietario. Pago con fecha efectiva histórica sin referencia: el Propietario
registra una tasa por fecha y motivo.
ExcepcionesTasa ausente, cero, negativa o de otra fecha: bloquea confirmación. No
acepta reemplazo silencioso por la última tasa. Si el desfase local respecto
de la BD supera cinco minutos, bloquea nuevas operaciones financieras
## (ERR-057).
PostcondicionesMonto original, moneda, equivalente y tasa quedan vinculados al recibo sin
cambiar al actualizar caché.
Reglas y requisitosRN-09, RN-22, RN-32; RF-M06-05 a RF-M06-08; RF-M09-07 a RF-M09-
## 09
Datos sintéticos y
criterio
Ejemplo sintético: US$ 100.00 de obligación y C$ 3,662.43 recibidos; tasa
oficial documentada de 2026 de 36.6243 NIO/USD. Equivalente US$
100.00 y saldo cero; tasa original intacta tras nueva consulta.

## 71
CU-18 Anular Recibo
La Tabla 44 presenta especificación de CU-18.
## Tabla 44
Especificación de CU-18
CampoDetalle
Actor y móduloPropietario; M06
PrecondicionesPropietario autenticado; recibo Registrado; no hay dependencias
posteriores activas sobre su crédito.
DisparadorEl Propietario solicita Anular recibo.
Flujo principal1. Abre el recibo y sus aplicaciones.
- Escribe el motivo.
- Revisa dependencias y confirma.
- El sistema bloquea los movimientos afectados.
- Revierte aplicaciones y crédito en una transacción.
- Marca pago y recibo Anulado y registra el evento.
Flujos alternosCrédito usado después: exige revertir esas aplicaciones antes de anular.
Solicitud por Asistente: deniega sin cambio. Reimpresión: muestra
## Anulado.
ExcepcionesFalla durante reversión: conserva el pago original completo. Motivo vacío:
no permite confirmar.
PostcondicionesNúmero conservado, pago Anulado, saldos repuestos y bitácora con
usuario, fecha y motivo.
Reglas y requisitosRN-07, RN-08, RN-13, RN-14, RN-31; RF-M06-09 a RF-M06-12
Datos sintéticos y
criterio
Recibo sintético 000001 de C$ 600.00; cargo inicial C$ 1,000.00. Después
de anular, saldo C$ 1,000.00 y mismo número visible; cero eliminación
física.
CU-21 Terminar Contrato con Devolución de Depósito
La Tabla 45 presenta especificación de CU-21.
## Tabla 45
Especificación de CU-21
CampoDetalle
Actor y móduloPropietario; M07
PrecondicionesContrato activo o Vencido; Propietario autenticado; garantía con
remanente y cargos revisados.
DisparadorEl Propietario confirma terminar y liquidar.

## 72
CampoDetalle
Flujo principal1. Registra fecha y motivo de terminación.
- Revisa deuda y garantía.
- Define deducciones con concepto.
- Calcula remanente a devolver.
- Confirma aplicaciones, devolución y cierre en una transacción.
- Libera la unidad y genera liquidación.
Flujos alternosDeuda superior a garantía: aplica hasta el saldo y deja deuda abierta.
Devolución parcial: conserva remanente y estado Devuelto parcial. Sin
depósito: cierra con liquidación de cero.
ExcepcionesDeducción sin concepto o devolución superior al remanente: rechaza. Falla
de confirmación: conserva contrato y garantía anteriores.
PostcondicionesContrato Terminado; unidad Disponible si no está en mantenimiento;
garantía distribuida sin diferencia.
Reglas y requisitosRN-10, RN-19, RN-21, RN-26; RF-M04-09; RF-M07-03 a RF-M07-07
Datos sintéticos y
criterio
Garantía sintética US$ 100.00; deducción por deuda US$ 30.00 y
devolución US$ 70.00. 100 = 30 + 70 + 0; una devolución y estado
## Devuelto.
CU-23 Atender Solicitud y Registrar Costos
La Tabla 46 presenta especificación de CU-23.
## Tabla 46
Especificación de CU-23
CampoDetalle
Actor y móduloAsistente; M08
PrecondicionesSolicitud Abierta y unidad identificada; actor con permiso de
mantenimiento.
DisparadorEl actor abre la solicitud para atenderla.
Flujo principal1. Selecciona prioridad y responsable.
- Cambia a En proceso.
- Registra fecha, importe y moneda del gasto.
- Revisa si existe cargo separado autorizado.
- Confirma resolución y fecha.
- Consulta costo por propiedad.
Flujos alternosTrabajo sin costo: conserva cero gasto. Cancelación antes de resolver:
requiere motivo y conserva costos registrados. Repercusión al inquilino:
crea cargo independiente con referencia.
ExcepcionesImporte negativo o fecha inválida: impide guardar. Solicitud Resuelta: no
admite volver a En proceso.

## 73
CampoDetalle
PostcondicionesSolicitud Resuelta o Cancelada con historial y gasto asociado; ningún
cargo automático al contrato.
Reglas y requisitosRN-23, RN-27; RF-M08-02 a RF-M08-07; RF-M05-05
Datos sintéticos y
criterio
Solicitud sintética de reparación, prioridad Alta y gasto C$ 500.00. Un
gasto de C$ 500.00 en la propiedad y cero cargo al inquilino salvo registro
separado.
CU-25 Actualizar Tipo de Cambio
La Tabla 47 presenta especificación de CU-25.
## Tabla 47
Especificación de CU-25
CampoDetalle
Actor y móduloAsistente; M09
PrecondicionesClave leída de LATCH_FX_API_KEY o archivo externo con ACL;
conectividad o caché; H3 sin otra instancia local activa.
DisparadorH3 alcanza la hora de actualización o el usuario pide actualizar dentro del
límite.
Flujo principal1. Comprueba caché y frecuencia.
- Envía GET HTTPS con HttpClient y Authorization: Bearer.
- Valida HTTP, JSON, result y base USD.
- Valida conversion_rates.NIO numérico finito entre 1 y 100 inclusive y
marcas temporales.
- Guarda respuesta válida y procedencia.
- Actualiza el panel con atribución y condición informativa.
Flujos alternosSin red: muestra caché con antigüedad. Tasa informativa disponible: no la
habilita para cobro hasta registrar autorización de una fuente apta. Datos
ausentes: indica Sin tasa disponible.
ExcepcionesHTTP 429: respeta Retry-After o espera 24 h si falta. Timeout, HTTP 408
o 5xx: un reintento tras 60 s. Clave, cuenta o cuota inválidas: avisa sin
reintento. JSON inválido: conserva la última respuesta válida. Fuera de 1–
100: no guarda como referencia vigente y registra M09,
REFERENCIA_EN_CUARENTENA, con valor y huella en bitacora, sin
tabla nueva.
PostcondicionesCaché válida renovada o error trazable sin tasa inventada; saldos y pagos
existentes permanecen intactos.
Reglas y requisitosRN-09, RN-22; RF-M09-01 a RF-M09-09
Datos sintéticos y
criterio
Fixtures JSON sintéticos success, error, NIO ausente y base EUR; red
simulada sin endpoint real. Cien por ciento de fixtures inválidos
rechazados; nunca aplica tasa informativa por defecto.

## 74
CU-29 Generar Estado de Cuenta
La Tabla 48 presenta especificación de CU-29.
## Tabla 48
Especificación de CU-29
CampoDetalle
Actor y móduloAsistente; M10
PrecondicionesContrato con movimientos y fecha de corte válida; sesión autorizada.
DisparadorEl actor solicita Estado de cuenta.
Flujo principal1. Selecciona contrato y corte.
- El sistema toma una lectura consistente.
- Reúne cargos, abonos, anulaciones y créditos a esa fecha.
- Calcula saldo por moneda.
- Entrega DTO a JasperReports.
- Presenta vista previa y permite PDF o XLSX.
Flujos alternosSin movimientos: emite estado con saldo cero y filtros visibles. Pago
anulado: conserva referencia sin sumarlo como cobro neto.
ExcepcionesError de consulta: no presenta totales parciales. Error de plantilla: informa
el incidente sin cambiar datos financieros.
PostcondicionesDocumento con corte, movimientos y saldo conciliado; ninguna escritura
financiera por consultar.
Reglas y requisitosRN-08, RN-24, RN-28; RF-M10-05; RF-M10-07; RF-M10-09; RF-M10-
## 10
Datos sintéticos y
criterio
Renta sintética C$ 1,000.00, abono C$ 600.00 y corte posterior al abono.
Saldo C$ 400.00 y total del reporte igual al cálculo de persistencia.
CU-30 Exportar Reporte
La Tabla 49 presenta especificación de CU-30.
## Tabla 49
Especificación de CU-30
CampoDetalle
Actor y móduloAsistente; M10
PrecondicionesReporte seleccionado y destino de archivos accesible; H5 libre.
DisparadorEl actor confirma Exportar.

## 75
CampoDetalle
Flujo principal1. Selecciona PDF o XLSX permitido.
- Elige destino con FileChooser.
- H5 genera un archivo temporal.
- Presenta progreso o cancelación.
- Valida terminación y publica por cambio de nombre.
- Muestra ubicación del archivo.
Flujos alternosDestino existente: pide confirmar reemplazo. Cancelación H5: cooperativa
durante consulta, llenado y exportación; se confirma al terminar y limpiar
temporales, sin ERR y con MSG-089. Conserva archivo previo hasta
publicación exitosa; si ya se publicó, informa el resultado real y mantiene
pagos o anulaciones confirmados. Cero filas: exporta encabezados y nota
de ausencia de movimientos.
ExcepcionesDisco lleno o permiso denegado: informa ruta y causa, elimina temporal y
no anuncia éxito. Falla de plantilla: conserva diagnóstico sin datos
sensibles.
PostcondicionesArchivo final íntegro y datos sin cambios; PDF legible o XLSX con
importes numéricos.
Reglas y requisitosRN-25, RN-28; RF-M10-09 a RF-M10-12
Datos sintéticos y
criterio
Reporte sintético de 10000 filas; destino de prueba y un archivo previo
conocido. Exportación cancelada conserva el hash del archivo previo y no
deja archivo final parcial.
CU-36 Respaldar y Cerrar la Aplicación
La Tabla 50 presenta especificación de CU-36.
## Tabla 50
Especificación de CU-36
CampoDetalle
Actor y móduloPropietario para respaldo; Propietario o Asistente para cierre; M12
PrecondicionesPara cerrar: Propietario o Asistente con sesión abierta. Si se solicita
respaldo: Propietario autenticado, directorio controlado y contraseña de
respaldo disponible; sesiones operativas advertidas.
DisparadorEl Propietario o Asistente solicita el cierre de la aplicación; el Propietario
puede solicitar antes un respaldo.

## 76
CampoDetalle
Flujo principal1. Si el Propietario solicitó respaldo, solicita exclusión de nuevas
escrituras; si solo se solicita cierre, continúa en el paso 5.
- Espera transacciones activas.
- Exporta base y adjuntos con manifiesto.
- Verifica hashes, cifra el paquete con AES-256-GCM y contraseña de
respaldo, y libera mantenimiento.
- Al salir, H6 cancela H1, H2, H3 y H5.
- Espera H4 y cierra conexiones y ejecutores.
Flujos alternosNo se solicitó respaldo: el cierre por cualquiera de los dos roles revisa
antigüedad y ofrece advertencia; esta no concede permiso de respaldo al
Asistente. Restaurar: requiere clientes detenidos, copia previa,
mantenimiento y restauración aislada antes de sustituir la instalación;
verificar autenticidad AES-256-GCM y descifrar con la contraseña de
respaldo antes de recuperar datos.
ExcepcionesRespaldo fallido: conserva último paquete íntegro. Autenticidad o clave
inválida: ERR-059/MSG-082, sin restaurar. Tarea que no termina en diez
segundos: mantiene aplicación abierta y muestra bloqueo. Hash de
respaldo incorrecto: ERR-055 / MSG-079. Commit incierto: ERR-031 o
ERR-054 / MSG-064, sin afirmar ausencia de cambios. No reutilizar
ERR-063 de integridad financiera para estas causas.
PostcondicionesRespaldo identificado si fue solicitado; cierre sin transacciones parciales ni
tareas de aplicación huérfanas.
Reglas y requisitosRN-14, RN-29, RN-30; RF-M12-06 a RF-M12-08
Datos sintéticos y
criterio
Base y adjuntos sintéticos de un GiB cada uno, con H1 en curso. Hashes
coinciden; cierre en diez segundos o aviso de bloqueo explícito, sin cerrar
a la fuerza.
## Requisitos No Funcionales
El entorno V1 propuesto tiene Windows 11 x64, cuatro núcleos lógicos, ocho GiB de
memoria y SSD. PostgreSQL local contendrá 100 propiedades, 500 unidades, 1000 inquilinos,
2000 contratos históricos, 10000 cargos y 100000 eventos sintéticos. Cada medición identificará
revisión, equipo, carga, fecha y evidencia, sin presentar metas como resultados obtenidos. El
percentil 95 tomará el tiempo de posición 29 entre 30 observaciones ordenadas. Las pruebas de
clientes en red local usarán hasta tres sesiones y registrarán su latencia. RNF-021 usará diez
emisores (hilos) repartidos en dos JVM, sin ampliar ese límite de clientes de aplicación.
World Wide Web Consortium (2024, 2025) fundamenta contraste y teclado con WCAG
2.2 y la guía informativa WCAG2ICT para escritorio. Su adaptación no acredita certificación de

## 77
JavaFX. Latch exigirá los mismos recorridos en Claro y Oscuro, con efectos activos e inactivos.
Las contraseñas usarán Argon2id con mínimo de 19 MiB, dos iteraciones y paralelismo uno
(OWASP Foundation, s. f.). La implementación medirá el costo del hash en V1 antes de elevarlo.
Los requisitos de calidad fijan metas y métodos previstos, sin resultados de pruebas funcionales.
## Rendimiento
La Tabla 51 presenta condiciones de Rendimiento.
## Tabla 51
Condiciones de Rendimiento
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-001El sistema debe abrir el panel en un
percentil 95 de dos segundos con
diez mil cargos.
Medir treinta aperturas con caché
de base caliente y fixture V1.
## Must
RNF-002El sistema debe responder
búsquedas paginadas en un percentil
95 de un segundo con mil
inquilinos.
Medir treinta búsquedas con
cincuenta filas por página.
## Must
RNF-003El sistema debe generar quinientos
cargos con H1 en sesenta segundos
o menos.
Cronometrar cinco lotes sintéticos
en entorno V1.
## Must
RNF-004El sistema debe exportar diez mil
filas a XLSX con H5 en treinta
segundos o menos.
Cronometrar cinco exportaciones
sin otras tareas masivas.
## Should
## Seguridad
La Tabla 52 presenta condiciones de Seguridad.
## Tabla 52
Condiciones de Seguridad
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-005El sistema debe usar Argon2id con
memoria mínima de 19 MiB, dos
iteraciones y paralelismo uno para el
cien por ciento de cuentas.
Inspeccionar parámetros y
comprobar hashes de cinco cuentas
sintéticas.
## Must
RNF-006El sistema debe denegar el cien por
ciento de veinte intentos de acciones
exclusivas del Propietario desde
## Asistente.
Invocar servicio y pantalla con
sesión de Asistente.
## Must

## 78
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-007El sistema debe bloquear al quinto
intento fallido durante quince
minutos por defecto.
Usar reloj controlado y seis
intentos de la misma cuenta.
## Must
RNF-008El sistema debe eliminar el cien por
ciento de contraseñas y secretos de
los registros de auditoría y
diagnóstico.
Buscar secretos señuelo en
archivos tras diez operaciones
fallidas.
## Must
## Usabilidad
La Tabla 53 presenta condiciones de Usabilidad.
## Tabla 53
Condiciones de Usabilidad
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-009El sistema debe permitir completar
diez recorridos críticos solo con
teclado y cero bloqueos de foco.
Recorrer Tab, Shift Tab, Enter y
Escape con datos sintéticos.
## Must
RNF-010El sistema debe mostrar texto y
controles sin recorte al doscientos
por ciento de escala en 1920 por
1080 píxeles.
Revisar capturas de doce módulos
y sus diálogos.
## Should
RNF-011El sistema debe mantener contraste
de al menos 4.5 a 1 en texto normal
y 3 a 1 en texto grande.
Medir colores de cada estado
visual con calculador de
luminancia.
## Must
RNF-012El sistema debe identificar el campo
y la causa en el cien por ciento de
veinte errores de validación
previstos.
Ejecutar conjunto de entradas
inválidas del Apéndice B.
## Must
RNF-047El sistema debe ofrecer exactamente
2 temas completos, Claro y Oscuro,
en el 100 % de módulos y diálogos.
Comparar inventario de 12
módulos y diez recorridos en
ambos temas.
## Must
RNF-048El sistema debe usar Claro en el 100
% de 6 inicios sin preferencia válida
o con fallo de lectura.
Eliminar valor, corromperlo y
denegar lectura en dos roles;
verificar acceso e inicio
autenticado.
## Must
RNF-049El sistema debe cambiar tema en
500 ms o menos sin perder ninguno
de 20 campos y filtros editados.
Cronometrar 30 cambios por tema
con formularios sintéticos abiertos;
comparar valores y foco.
## Must

## 79
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-050El sistema debe restaurar la
preferencia local correcta en 10 de
10 reinicios por cada una de 2
cuentas.
Alternar temas y cuentas;
comprobar última elección local
antes y preferencia individual
después del acceso.
## Must
RNF-051El sistema debe mantener contraste
mínimo de 3:1 en bordes y símbolos
necesarios de los controles en ambos
temas.
Medir pares de color de estados
normal, foco, selección y error
sobre la superficie uniforme
renderizada.
## Must
RNF-052El sistema debe mostrar foco visible
con perímetro equivalente a 2 px y
contraste de 3:1 en el 100 % de
controles operativos.
Recorrer teclado en ambos temas,
comprobar foco no oculto y medir
geometría/contraste; meta propia
inspirada en criterio AAA.
## Must
RNF-053El sistema debe completar los
mismos 10 recorridos por teclado en
4 combinaciones de tema y efectos,
con 0 bloqueos.
Ejecutar Claro/Oscuro, efectos
activos/inactivos; Tab, Shift+Tab,
Enter y Escape; cotejar resultados.
## Must
RNF-054El sistema debe conservar el 100 %
de importes, permisos y datos de
reportes tras 20 cambios de tema y
efectos.
Comparar DTO y movimientos
antes/después; ignorar metadatos
técnicos del archivo; probar ambos
roles.
## Must
RNF-055El sistema debe conservar Segoe UI
y superficies opacas uniformes bajo
el 100 % de campos, tablas e
importes de ambos temas.
Inspeccionar capturas de 12
módulos; sin luces detrás de texto
ni estado expresado solo con color.
## Must
RNF-056El sistema debe eliminar el 100 %
de luces y desenfoques decorativos
al activar sin efectos en cada tema.
Revisar árbol de escena y capturas
de acceso, panel y cobro; conservar
texto y teclado.
## Must
RNF-058El sistema debe conservar el cambio
de tema durante 1 sesión y mostrar 1
aviso no bloqueante por fallo de
guardado.
Denegar escritura local con pago
sin confirmar; comprobar
continuidad y datos intactos en
ambos roles.
## Must
Confiabilidad y Disponibilidad
La Tabla 54 presenta condiciones de Confiabilidad y Disponibilidad.
## Tabla 54
Condiciones de Confiabilidad y Disponibilidad
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-013El sistema debe conservar cero
pagos a medio confirmar tras veinte
fallos inducidos antes de commit.
Interrumpir cada punto financiero
y contrastar base y auditoría.
## Must

## 80
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-014El sistema debe mantener la
operación de una jornada simulada
de ocho horas sin cierre inesperado.
Ejecutar sesión sintética con mil
operaciones y registrar incidentes.
## Should
RNF-015El sistema debe permitir diez
operaciones CRUD locales
consecutivas durante una
interrupción HTTP de treinta
minutos.
Deshabilitar red externa y
conservar PostgreSQL disponible.
## Must
Integridad de Datos
La Tabla 55 presenta condiciones de Integridad de Datos.
## Tabla 55
Condiciones de Integridad de Datos
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-016El sistema debe impedir el cien por
ciento de cien intentos de renta
duplicada por contrato y período.
Ejecutar inserciones repetidas y
concurrentes con restricción única.
## Must
RNF-017El sistema debe conservar diferencia
de cero centavos entre pago
convertido y suma de aplicaciones y
crédito.
Calcular cien combinaciones
decimales sintéticas.
## Must
RNF-018El sistema debe impedir dos o más
contratos activos superpuestos por
unidad en cien intentos simultáneos.
Lanzar pares de activaciones y
verificar máximo un éxito.
## Must
RNF-019El sistema debe conservar el cien
por ciento de números de recibo tras
veinte anulaciones.
Comparar catálogo antes y
después, sin reutilización.
## Must
## Concurrencia
La Tabla 56 presenta condiciones de Concurrencia.
## Tabla 56
Condiciones de Concurrencia
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-020El sistema debe mantener latencia
del hilo JavaFX menor o igual a
doscientos milisegundos durante
H1, H2, H3 y H5.
Medir veinte pulsos de interfaz por
tarea en V1.
## Must

## 81
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-021El sistema debe asignar con H4 cien
recibos concurrentes sin duplicados
ni huecos entre pagos confirmados.
Lanzar diez emisores concurrentes
en dos JVM, empezando en 1: a)
cien pagos confirmados y otras
diez solicitudes con rollback
anterior al commit dejan 100
recibos y siguiente_numero=101;
b) cien pagos confirmados y diez
anulaciones posteriores dejan 100
recibos, 90 pagos Registrado y 10
Anulado, con
siguiente_numero=101. El tope de
tres sesiones LAN limita clientes
de aplicación, no hilos de prueba.
Variantes de CP-041, CP-042 y
CP-032, sin crear CP.
## Must
RNF-022El sistema debe detener H1, H2, H3
y H5 mediante H6 en diez segundos
o presentar bloqueo explícito.
Solicitar salida durante cada tarea
y registrar su estado final.
## Must
RNF-023El sistema debe ejecutar a lo sumo
una instancia de H1, H2, H3 y H5
por aplicación.
Solicitar dos ejecuciones
simultáneas de cada tarea.
## Must
RNF-057El sistema debe usar 0 hilos
adicionales para luces estáticas y
cambiar tema solo en el hilo
JavaFX.
Inspeccionar código y volcado de
hilos antes/después de 30 cambios;
H1 a H6 conservan su ciclo.
## Must
## Mantenibilidad
La Tabla 57 presenta condiciones de Mantenibilidad.
## Tabla 57
Condiciones de Mantenibilidad
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-024El sistema debe compilar con una
sola orden Maven en dos equipos
limpios con JDK 21.
Clonar una revisión Git
identificada y registrar dos
compilaciones.
## Must
RNF-025El sistema debe pasar el cien por
ciento de pruebas de las treinta y
ocho reglas de negocio.
Ejecutar suite de escenarios por
RN con reporte de fallos.
## Must
RNF-026El sistema debe mantener cero
ciclos de dependencia entre
presentación, aplicación, dominio e
infraestructura.
Revisar grafo de paquetes en cada
integración semanal.
## Should

## 82
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-059El sistema debe almacenar 0
preferencias visuales en tablas de
dominio y 0 claves de preferencia
con nombre o cédula.
Inspeccionar esquema y almacén
local con dos usuarios; claves de
usuario por id opaco.
## Must
RNF-060El sistema debe mantener 0
bibliotecas Aurora homónimas y 2
hojas de tema bajo un servicio de
presentación común.
Revisar dependencias y recursos
FXML/CSS; modalidad sin efectos
como opción independiente.
## Must
Portabilidad y Compatibilidad
La Tabla 58 presenta condiciones de Portabilidad y Compatibilidad.
## Tabla 58
Condiciones de Portabilidad y Compatibilidad
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-027El sistema debe iniciar en Windows
11 x64 con un paquete que incluya
Java 21 y JavaFX 21.
Instalar el paquete en dos cuentas
de Windows sin JDK previo.
## Must
RNF-028El sistema debe conectar con
PostgreSQL 18 mediante pgJDBC
usando UTF8 en el cien por ciento
de veinte registros acentuados.
Guardar y releer fixture con ñ,
tildes y C$.
## Must
RNF-029El sistema debe operar con tres
sesiones simultáneas en red local y
cero escrituras perdidas en cincuenta
conflictos.
Ejecutar operaciones cruzadas con
control optimista y bloqueo
financiero.
## Should
## Localización
La Tabla 59 presenta condiciones de Localización.
## Tabla 59
Condiciones de Localización
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-030El sistema debe mostrar el cien por
ciento de etiquetas operativas y
mensajes de validación en español.
Revisar inventario de cadenas de
doce módulos.
## Must
RNF-031El sistema debe mostrar fechas
dd/MM/yyyy y moneda NIO o USD
en el cien por ciento de los diez
reportes.
Comparar diez muestras contra
catálogo de reportes.
## Must

## 83
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-032El sistema debe conservar instantes
UTC y presentar America/Managua
en veinte eventos de auditoría.
Comparar instante almacenado y
presentación con reloj controlado.
## Must
## Auditoría
La Tabla 60 presenta condiciones de Auditoría.
## Tabla 60
Condiciones de Auditoría
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-033El sistema debe registrar el cien por
ciento de cien operaciones
financieras confirmadas con actor e
instante.
Cruzar pagos, depósitos y sus
eventos por correlación.
## Must
RNF-034El sistema debe impedir el cien por
ciento de diez intentos de editar
bitácora desde los dos roles.
Invocar las rutas de aplicación con
cuentas operativas.
## Must
RNF-035El sistema debe recuperar veinte
eventos por referencia en dos
segundos o menos con cien mil
eventos.
Medir treinta consultas indexadas
en V1.
## Should
Respaldo y Recuperación
La Tabla 61 presenta condiciones de Respaldo y Recuperación.
## Tabla 61
Condiciones de Respaldo y Recuperación
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-036El sistema debe producir un
respaldo de una base de un GiB y
adjuntos de un GiB en quince
minutos o menos.
Cronometrar paquete en disco SSD
de V1 con manifiesto. Los quince
minutos son una meta de
aceptación pendiente de medición
en un entorno identificado; la
definición no acredita ejecución.
Variante de CP-051.
## Should

## 84
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-037El sistema debe restaurar el conjunto
de dos GiB en treinta minutos o
menos con cero hashes discordantes.
Restaurar en base aislada y
comparar filas y adjuntos.
Verificar hashes y autenticidad
GCM antes de sustituir la
instalación; mantener
mantenimiento si hay recibos
posteriores al respaldo hasta
conciliar, sin reutilizar ni
renumerar. Los treinta minutos son
una meta pendiente de medición,
no un resultado. Variante de
## CP-095.
## Must
RNF-038El sistema debe limitar la pérdida
objetivo a veinticuatro horas
mediante un respaldo manual diario.
Comparar fecha del último
respaldo al cerrar la jornada; emitir
alerta al exceder.
## Must
Integración con Servicios Externos
La Tabla 62 presenta condiciones de Integración con Servicios Externos.
## Tabla 62
Condiciones de Integración con Servicios Externos
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-039El sistema debe agotar la conexión
HTTP a los cinco segundos y la
solicitud completa a los diez
segundos.
Simular conexión y respuesta que
excedan cada umbral.
## Must
RNF-040El sistema debe consultar el servicio
USD/NIO a lo sumo una vez cada
veinticuatro horas por instalación
salvo reintento autorizado.
Inspeccionar registro de H3 y
caché con reloj simulado de setenta
y dos horas.
## Must
RNF-041El sistema debe rechazar el cien por
ciento de veinte respuestas sin base
USD, NIO numérico finito entre 1 y
100 inclusive o fecha válida.
Ejecutar veinte fixtures JSON
inválidos y dos adicionales con los
límites 1 y 100 admitidos (22 en
total). Para valores fuera de banda,
comprobar referencia vigente
intacta y evento M09,
## REFERENCIA_EN_CUARENTE
NA, con valor y huella, sin tabla
nueva. Variante de CP-058.
## Must
RNF-042El sistema debe conservar las veinte
tasas históricas de veinte pagos al
reemplazar la caché.
Actualizar veinte veces y
contrastar copias inmutables.
## Must

## 85
## Cumplimiento
La Tabla 63 presenta condiciones de Cumplimiento.
## Tabla 63
Condiciones de Cumplimiento
IDRequisito MedibleMétodo de VerificaciónPrioridad
RNF-043El sistema debe documentar la
finalidad y la base de tratamiento en
el cien por ciento de expedientes
con datos reales.
Revisar diez expedientes
autorizados antes de cualquier
piloto real.
## Must
RNF-044El sistema debe atender solicitudes
simuladas de acceso o rectificación
en un día hábil como meta interna.
Ejecutar cinco solicitudes ficticias
y registrar entrega autorizada.
## Should
RNF-045El sistema debe incluir avisos de
recibo no fiscal y de origen
comercial o manual en el cien por
ciento de comprobantes aplicables.
Inspeccionar diez PDF sintéticos y
sus etiquetas.
## Must
RNF-046El sistema debe identificar autor,
licencia y versión del cien por ciento
de dependencias distribuidas.
Revisar inventario del paquete y
avisos de licencia.
## Must
Requisitos de Datos
Entidades y Ciclo de Vida
El modelo distingue identidad operativa, condiciones contractuales y movimientos
financieros para conservar la historia sin duplicar saldos como datos independientes. Cada
entidad usa una clave interna y referencias con integridad en PostgreSQL; el recibo añade su
consecutivo de negocio. Los valores monetarios usan decimal de dos posiciones y las tasas
conservan ocho, con conversión y redondeo definidos por RN-32. Los volúmenes de la tabla son
supuestos de diseño para pruebas y no registros de un arrendador. La baja lógica preserva
vínculos y los documentos adjuntos quedan fuera de la base, con metadatos y huellas
verificables.
La Tabla 64 presenta entidades y Volúmenes Sintéticos.

## 86
## Tabla 64
Entidades y Volúmenes Sintéticos
EntidadDatos PrincipalesCiclo de VidaVolumenRegla de Conservación
Usuario y rolusuario, hash, sal, rol, activo,
bloqueo
Activo, bloqueado o desactivado5 cuentas; 2
roles
Credenciales no se exportan;
desactivar conserva atribución.
Propiedadnombre, dirección, propietarioActiva o baja lógica100Sin baja si tiene unidades
activas.
Unidadpropiedad, código, tipo, estado,
canon de referencia
## Disponible, Ocupada, En
mantenimiento, Inactiva
500Estado contractual prevalece
sobre disponibilidad manual.
Inquilinonombre, contacto,
identificación opcional
Activo, desactivado, bloqueado
por conservación
1000Acceso restringido; separar
contacto de emergencia.
Contratounidad, inquilino, fechas,
canon, moneda, depósito,
versión
Borrador, Vigente, Por vencer,
## Vencido, Terminado, Cancelado
## 2000
históricos; 500
activos
Una versión no sobreescribe
condiciones confirmadas.
Cargocontrato, tipo, período,
vencimiento, monto
## Pendiente, Parcial, Pagado,
## Vencido, Anulado
10000Saldo derivado; unicidad de
renta por período.
Pagocontrato, monto original,
moneda, fecha efectiva,
instante y fecha operativa de
BD, método, UUID
Registrado o Anulado20000No borrado físico desde la
aplicación.
Aplicaciónpago o crédito, cargo, importe
contractual
Activa o reversión vinculada30000Suma no excede saldo;
referencia a evento original.
Crédito a favorcontrato, moneda, origen, saldo
derivado
Disponible, aplicado o revertido2000No transferible a otro contrato
sin operación nueva fuera de
alcance.
Recibo y
contador
número, pago, datos de
emisión; siguiente número
Registrado o Anulado, derivados
del pago; contador transaccional
20000; un
contador
global
Número no reinicia cada año;
inicia en 000001 en base
nueva.

## 87
EntidadDatos PrincipalesCiclo de VidaVolumenRegla de Conservación
Depósito y
movimiento
monto inicial, moneda,
concepto, aplicación o
devolución
## Retenido, Aplicado, Devuelto
parcial, Devuelto
## 2000
depósitos;
## 4000
movimientos
Conserva ecuación de
liquidación y autor.
Mantenimiento y
gasto
unidad, prioridad, estado,
responsable, costo, moneda
Abierta, En proceso, Resuelta,
## Cancelada
## 5000
solicitudes;
5000 gastos
Gasto separado de cargo al
inquilino.
Tasa y
autorización
base, destino, valor, fuente,
fecha efectiva, consulta,
aprobador
Informativa, autorizada o
histórica
3650 tasas;
20000 copias
de pago
Autorización única por fecha
efectiva e inmutable;
corrección por procedimiento
excepcional. Mínimo diez años
de datos sintéticos para
prueba; sin redistribuir
catálogo externo.
## Parámetro
versionado
clave, valor, vigente_desde
TIMESTAMPTZ, motivo
## VARCHAR(500),
creado_en/por; UNIQUE(clave,
vigente_desde)
Solo inserción; vigente = última
fila con vigente_desde ≤ now()
de la BD.
100 versionesSin UPDATE ni columna
version; no recalcula importes
confirmados. Cada evento de
Mora registra {politica_id,
tipo, valor, base}.
Auditoríaactor, instante UTC, acción,
referencia, correlación
Solo inserción desde servicios100000Sin hashes de contraseña,
secretos ni adjuntos completos.
Adjunto y
respaldo
ruta interna, tipo, tamaño,
SHA256, manifiesto
Vigente, sustituido o purgado
según política
2000 adjuntos
de hasta 5 MiB
Un GiB de base y un GiB de
adjuntos para prueba de
recuperación.
Preferencias de
presentación
Último tema local, tema y sin
efectos por id opaco
Local; no entidad del dominio;
fallo usa Claro
2 temas y hasta
5 perfiles
sintéticos
Fuera de PostgreSQL; no se
requiere sincronización con
## Windows.

## 88
Privacidad y Datos Personales
La Ley N.º 787 cubre tratamientos identificables públicos y privados; su artículo 3.g
clasifica datos financieros o crediticios como sensibles. Los artículos 5 a 7 regulan
proporcionalidad, consentimiento e información; los artículos 11 y 12, seguridad y
confidencialidad (Asamblea Nacional de la República de Nicaragua, 2012). Latch restringirá
deuda y contactos a cuentas autorizadas y documentará finalidad y fundamento. La demostración
usará datos sintéticos. Las cuentas de acceso y otros metadatos de una persona real pueden
identificarla aunque los contratos de las pruebas sean ficticios.
El Decreto N.º 36-2012 exige consentimiento expreso para datos financieros o
patrimoniales en su artículo 7 y regula su prueba mediante mecanismos digitales en los artículos
10 y 11. Los artículos 14 a 17 desarrollan aviso y seguridad proporcional, mientras los artículos
18 a 24 concretan el ejercicio de derechos (Presidencia de la República de Nicaragua, 2012). El
diseño conservará versión del aviso, fundamento, fecha y responsable del registro. Una casilla
aislada no acredita cualquier supuesto de licitud. La integración de tasas enviará código USD y
credencial del integrador, sin expedientes, saldos ni documentos de inquilinos.
Retención y Derechos
La Ley N.º 787 fija diez días hábiles para acceso en su artículo 17.c y cinco para
modificación en el 19.b. Sus artículos 19 y 24 exigen considerar bloqueo y conservación antes de
prometer borrado inmediato (Asamblea Nacional de la República de Nicaragua, 2012). La meta

## 89
interna de un día hábil no altera esos plazos. El portal legislativo no respondió durante la
consulta. La revisión leyó originales de La Gaceta reproducidos y cotejados, sin certificar
reformas exhaustivas o trámites actuales. La Tabla 65 delimita la retención propuesta.
La Tabla 65 presenta política de Retención Adoptada para el Prototipo.
## Tabla 65
Política de Retención Adoptada para el Prototipo
CategoríaValor AdoptadoAcción y JustificaciónLímite
Demostración y
adjuntos sintéticos
Hasta el 31 de diciembre de
## 2026
Eliminar la base de demostración y sus respaldos
de trabajo después del cierre académico. Los
fixtures sin datos reales pueden permanecer en Git.
Decisión de proyecto; no
plazo legal.
## Expedientes
identificables y su
bitácora de auditoría
en BD en operación
futura
Bloqueo al terminar y ventana
administrativa propuesta de
cinco años desde cierre; la
bitácora de auditoría en BD,
de solo inserción, se conserva
con el expediente
Restringir consulta a Propietario; conservar solo la
finalidad y responsabilidades documentadas;
impedir purga si existe retención por controversia.
Modelo de diseño sujeto
al caso jurídico;
producción real excluida
de aceptación académica.
Contactos opcionales
y adjuntos
innecesarios
Revisión al cierre del
contrato; supresión cuando
cese finalidad sin deber de
conservación
Registrar solicitud, fundamento, decisión y
ejecución; evitar borrar referencias financieras
obligatorias.
No define cancelación
automática universal.
Registros técnicos de
diagnóstico sintéticos
Rotación a los noventa días o
purga de demo, lo primero
que ocurra
Rotar registros técnicos de diagnóstico y conservar
evidencias anonimizadas de evaluación; esta
rotación no aplica a la bitácora de auditoría en BD.
Sin secretos ni datos
financieros completos.
Respaldo manual
operativo de diseño
Siete diarios y cuatro
semanales
Cifrar el paquete con AES-256-GCM y contraseña
de respaldo; verificar autenticidad antes de
descifrar, controlar acceso local y borrar rotaciones
vencidas de forma registrada.
Retención técnica
propuesta; no sustituye
conservación legal del
expediente.

## 90
Ámbito Normativo y Límites Contractuales
La Ley N.º 787 regula cesión y transferencias en los artículos 13 a 15 e inscripción del
responsable en el artículo 22 (Asamblea Nacional de la República de Nicaragua, 2012). El
despliegue local no acredita exención ni registro completado. El alcance académico usará datos
sintéticos para demostrar las operaciones del sistema. Un uso posterior con personas
identificables requerirá documentar responsable, fundamento y procedimiento aplicable. La
consulta de monedas no envía movimientos del alquiler, aunque el proveedor recibe metadatos
de red y autenticación del integrador.
La revisión normativa cubre protección de datos y la política cambiaria publicada para
2026; no determina la legalidad de cada contrato de alquiler. Canon, mora, garantía y
deducciones son parámetros del escenario académico y no se presentan como límites legales
universales. El producto tampoco emite facturación fiscal ni interpreta conflictos entre las partes.
Los originales consultados permiten derivar controles concretos de privacidad, pero no
sustituyen el análisis de reformas o de un despliegue real. La aceptación de esta entrega se limita
al documento y a su plan técnico; no acredita certificación jurídica ni aprobación de una
autoridad.

## 91
Requisitos de Interfaces Externas
Servicio Web de Tipo de Cambio
AYR Tech (Pty) Ltd. (s. f.-a, s. f.-d, s. f.-e) documenta autenticación Bearer, base USD y
cobertura de NIO en ExchangeRate-API. Latch adopta el plan Free autenticado, con mil
quinientas solicitudes mensuales y actualización diaria según la comparación oficial de planes
(AYR Tech (Pty) Ltd., s. f.-b). Sus términos autorizan caché para uso final y restringen
redistribución y finalidad transaccional (AYR Tech (Pty) Ltd., s. f.-f). La clave queda fuera del
código y de los registros. La consulta orientativa y la tasa manual autorizada cumplen propósitos
distintos, como especifica la Tabla 66.
La Tabla 66 presenta contrato de Integración USD NIO.

## 92
## Tabla 66
Contrato de Integración USD NIO
AspectoDecisión del PRDErrores y Requisitos
Propósito y
endpoint
## GET
https://v6.exchangerate-api.com/v6/latest/USD;
backend HTTPS con Authorization: Bearer y
clave externa al código.
Solo referencia orientativa;
## RF-M09-01; RA5 IL5.1.
Proveedor y
licencia
AYR Tech (Pty) Ltd.; plan Free autenticado.
Uso final personal/comercial con caché; sin
atribución obligatoria en este plan. Se actualiza
la modalidad de la base a Free para controlar
cuota por cuenta y revocación de credencial; no
implica mayor precisión.
No redistribuir catálogo ni
API; datos
informativos/ilustrativos; sin
SLA contratado.
Base y contrato
## JSON
result=success; base_code=USD;
conversion_rates.NIO numérico finito entre 1 y
100 NIO/USD inclusive;
time_last_update_unix y
time_next_update_unix coherentes.
Rechazar error, base
incorrecta, JSON inválido,
NIO ausente o fecha
inválida. Valor fuera de 1–
100: no guardar como
referencia vigente; registrar
## M09,
## REFERENCIA_EN_CUAR
ENTENA, con valor y
huella en bitacora, sin tabla
nueva. RF-M09-02, RF-
## M09-03.
Frecuencia y
cuota
Actualización diaria; 1500 solicitudes/mes por
cuenta, verificadas al 22/10/2026; plan sujeto a
cambios. Una consulta por 24 h por instalación.
31 consultas/mes por
instalación como supuesto;
cuota compartida por clave.
Sin petición por pago.
Errores y
reintento
Conexión 5 s, petición 10 s; 1 reintento tras 60
s solo fallos transitorios de red, HTTP 408 o
5xx. unsupported-code, malformed-request,
invalid-key, inactive-account, quota-reached.
No reintentar errores de
clave, cuota o contrato.
HTTP 429 respeta Retry-
After; sin cabecera, espera
24 h y avisa.
Caché y
trazabilidad
Guardar proveedor, valor, base, destino, fecha
de cotización, descarga y próxima
actualización; vista histórica con antigüedad.
RF-M09-04 a RF-M09-06.
Caché no autoriza una tasa
financiera ni sobreescribe
pagos.

## 93
AspectoDecisión del PRDErrores y Requisitos
## Autorización
para pago
Propietario registra tasa manual con fecha
efectiva del pago, fuente admisible y motivo,
separada de la fecha operativa de BD. Una
autorización inmutable por fecha efectiva;
duplicado: ERR-060/MSG-083,
DuplicadoException. Corrección por
procedimiento excepcional. Fuente BCN
separada de referencia API. La tasa manual
positiva fuera de 1 a 100 NIO/USD requiere
advertencia de revisión y confirmación del
Propietario, sin restricción de banda en BD.
RF-M09-07 a RF-M09-09;
## RF-M06-05, RF-M06-06.
Prohibido usar tasa API para
eludir sus términos.
## Referencia
oficial
BCN publicó 0 % de deslizamiento y 36.6243
NIO/USD para 2026 en comunicado del
20/10/2025. Conservar fuente y vigencia.
No se ejecutó API BCN ni
consulta diaria; no extender
automáticamente a 2027.
Credencial y
datos enviados
Clave leída de LATCH_FX_API_KEY o de un
archivo externo con ACL; sin clave compartida
embebida ni inclusión en código, registros o
respaldos. Solo USD y autenticación.
invalid-key o clave ausente:
avisa y conserva operación
local. Sin nombres, saldos ni
documentos.
Alternativas y
alcance
FXRatesAPI y CurrencyFreaks quedan fuera
de selección: no se completó verificación
documental de todos los criterios. Se adopta
proveedor de operador identificado y contrato
verificable.
No se atribuyen a candidatos
cuotas, cobertura, gratuidad
ni defectos no consultados.
El BCN anunció expresamente para 2026 una tasa oficial de 36.6243 córdobas por dólar
y deslizamiento anual de cero por ciento (Banco Central de Nicaragua, 2025). Ese comunicado
respalda el año completo y no sustituye una consulta diaria ni una llamada API ejecutada. El
ejemplo de US$ 100.00 por la tasa BCN documentada de 36.6243 produce C$ 3,662.43 como
cálculo de contraste sintético. La política de conversión guarda moneda original y tasa
congelada, con HALF_EVEN al obtener el importe monetario. No se realizó una petición al
endpoint del proveedor ni se midieron su latencia, disponibilidad o exactitud empírica.
PostgreSQL y Persistencia
JDBC ejecutará sentencias parametrizadas de seguridad, bitácora y consultas agregadas
mediante pgJDBC. Hibernate mantendrá las entidades de negocio, con un solo propietario
transaccional por operación financiera. El servicio abrirá la transacción ORM e insertará la
auditoría por JDBC en esa conexión, sin commit independiente. H4 bloqueará contador y

## 94
obligaciones en orden estable dentro de la misma unidad. El manual documenta esos bloqueos;
las pruebas de concurrencia tendrán que comprobar su implementación y la reversión íntegra
ante fallos (The PostgreSQL Global Development Group, s. f.-b).
La Tabla 67 presenta interfaces de Persistencia Reportes y Archivos.
## Tabla 67
Interfaces de Persistencia Reportes y Archivos
InterfazProtocolo y DatosErroresRequisitos
PostgreSQL
local
jdbc:postgresql://localhost:5432/
latch; UTF8, credencial de mínimo
privilegio y base exclusiva. En
LAN, TLS con verificación de
servidor y acceso a hosts
autorizados.
Conexión rechazada,
autenticación, conflicto,
interbloqueo, timeout y
falta de espacio; rollback y
diagnóstico sin secreto.
M01 a M12;
## RF-M06-12;
## RN-14, RN-20,
## RN-29.
Hibernate y
## JDBC
Una sesión/unidad de trabajo por
tarea; no compartir EntityManager
entre hilos. CRUD ORM y
PreparedStatement en la frontera
definida.
Escrituras ordinarias:
lock_timeout 2000 ms y
statement_timeout 5000
ms. Para SQLSTATE
40P01/40001: tres intentos
totales (uno inicial y dos
reintentos), esperas base de
100 y 250 ms más jitter de
0–50 ms, fuera de
transacción, Session nueva
y mismo UUID. Conservar
control optimista en
formularios y bloqueo
pesimista financiero.
RA2 IL2.1 a
## IL2.3; RA4
IL4.1; H1, H2 y
## H4.
JasperReportsAPI Java y
JRBeanCollectionDataSource con
DTO inmutables. Núcleo y
jasperreports-pdf 7.0.8; exportador
XLSX del conjunto documentado.
Plantilla incompatible,
fuente ausente, error de
evaluación o memoria; no
cambiar datos por falla de
exportación.
RF-M10-01 a
## RF-M10-12;
## H5.
Sistema de
archivos
FileChooser y DirectoryChooser;
adjuntos PDF/PNG/JPEG, reportes
PDF/XLSX y respaldos con
manifiesto y SHA256.
Ruta inválida, permiso,
disco lleno, firma de
archivo no admitida y hash
distinto; no publicar
archivo incompleto.
## RF-M03-05;
## RF-M04-07;
## RF-M10-09;
RF-M12-06 y
## RF-M12-07.

## 95
Sistema de Archivos y Aislamiento
El usuario selecciona archivos mediante controles de JavaFX, pero el almacenamiento
interno usa nombres aleatorios para impedir colisiones y rutas fuera del directorio administrado.
La validación comprobará extensión, firma, tamaño y huella antes de vincular un adjunto. Los
reportes se producirán en temporales dentro del destino y solo se anunciarán después de
publicarse el archivo final. La documentación oficial de JasperReports describe PDF y XLSX, y
el POM identifica el módulo PDF y sus dependencias (Jaspersoft, s. f.-a, s. f.-c, 2026b). Esas
capacidades documentadas no certifican una plantilla ni conformidad PDF/A o PDF/UA.
Requisitos de Reportes
El catálogo mínimo contiene diez reportes con finalidad operativa, filtros y totales
definidos. Los importes conservan la moneda del contrato o movimiento y presentan
equivalentes únicamente cuando una tasa autorizada los respalda. Un saldo de cartera se calcula
a la fecha de corte y excluye anulaciones del total neto sin ocultar sus referencias históricas. El
recibo usa la versión de datos del propietario guardada al emitirlo, de modo que reimprimir no
altera su contenido. Los reportes tabulares admiten PDF y XLSX; el comprobante de recibo se
entrega en PDF para conservar su presentación administrativa.
La Tabla 68 presenta catálogo de Reportes.

## 96
## Tabla 68
Catálogo de Reportes
ReportePropósito y
## Usuario
FiltrosColumnasTotalesFormato y RF
REP-01 ReciboDocumentar el
pago; Propietario y
## Asistente
Número o pagoEmisor; inquilino; contrato; fecha;
método; monto original; tasa;
aplicaciones; crédito; estado
Total recibido y
aplicado; crédito
## PDF; RF-M10-
## 01; RF-M06-
## 08
REP-02 Estado
de cuenta
Explicar deuda
contractual;
Propietario y
## Asistente
## Contrato,
inquilino,
fecha de corte
Fecha; concepto; vencimiento; cargo;
abono; anulación; saldo; tasa de abonos
Cargos, abonos
netos, deuda y
crédito por moneda
## PDF/XLSX;
## RF-M10-05
REP-03 Cartera
vencida
Priorizar revisión
de saldos;
Propietario y
## Asistente
## Propiedad,
contrato, corte,
antigüedad
Contrato; inquilino; unidad;
vencimiento; días; principal; mora;
moneda
Saldo 1 a 30, 31 a
60, 61 a 90 y más de
90 días
## PDF/XLSX;
## RF-M10-02
REP-04 Ingresos
y gastos
## Revisar
movimientos del
período;
Propietario y
## Asistente
## Propiedad,
fechas,
moneda,
concepto
Fecha; referencia; tipo; ingreso; gasto;
moneda; estado
Cobros netos y
gastos por moneda;
garantía separada
## PDF/XLSX;
## RF-M10-03
## REP-05
## Ocupación
## Conocer
utilización del
inventario;
Propietario y
## Asistente
Propiedad y
tipo; corte en
el instante de
generación, sin
selector de
fecha histórica.
Unidad; estado; contrato activo; inicio;
fin
Unidades activas y
ocupadas;
porcentaje con
denominador
## PDF/XLSX;
## RF-M10-04
## REP-06
Contratos por
vencer
## Preparar
renovación o
cierre; Propietario
y Asistente
## Propiedad,
corte, umbral
Contrato; unidad; inquilino; fin; días
restantes; canon; moneda
Número de
contratos por
propiedad
## PDF/XLSX;
## RF-M10-06

## 97
ReportePropósito y
## Usuario
FiltrosColumnasTotalesFormato y RF
REP-07 Historial
de pagos
Revisar cobros del
inquilino;
Propietario y
## Asistente
## Inquilino,
contrato,
fechas, estado
Recibo; fecha; método; monto; moneda;
tasa; estado; motivo de anulación
Recibido neto y
anulaciones por
moneda
## PDF/XLSX;
## RF-M10-07
## REP-08
## Garantías
Explicar retención
y liquidación;
Propietario y
## Asistente
## Contrato,
inquilino, corte
Depósito inicial; movimiento; fecha;
concepto; deducción; devolución;
remanente
Inicial = aplicado +
devuelto + retenido
## PDF/XLSX;
## RF-M10-08
## REP-09
## Mantenimiento
Revisar costos por
propiedad;
Propietario y
## Asistente
## Propiedad,
unidad, fechas,
estado,
prioridad
Solicitud; unidad; responsable; estado;
fecha; costo; moneda
Gasto efectivo por
propiedad y moneda
## PDF/XLSX;
## RF-M10-11
REP-10 Tasas
utilizadas
## Rastrear
conversiones
registradas;
Propietario y
## Asistente
## Fechas,
contrato,
origen
Pago; fecha efectiva; base; destino;
tasa; fuente; autorizador; instante de
consulta
Cantidad de pagos
por fuente; sin
sumar tasas
## PDF/XLSX;
## RF-M10-13;
## RF-M09-08
Trazabilidad con el Syllabus
La trazabilidad usa el resumen académico recibido y conserva los cinco resultados e
indicadores aportados. La expresión «con el syllabus» nombra la sección exigida por el encargo,
sin afirmar lectura del programa original. Cada evidencia propuesta tendrá un escenario, datos
sintéticos y un archivo o registro que permita repetirla. El equipo seleccionará una revisión del
código para la presentación y evitará demostrar comportamientos de versiones distintas como si

## 98
pertenecieran al mismo paquete. La matriz resume cobertura por resultado; el Apéndice C
relaciona de forma individual todos los RF con HU, CU, RN y RA e IL.
La Tabla 69 presenta cobertura de Resultados e Indicadores.

## 99
## Tabla 69
Cobertura de Resultados e Indicadores
RA e IL del
## Resumen
Módulos y RF de ReferenciaEvidencia Prevista
## RA1 IL1.1
controles; IL1.2
eventos; IL1.3
menús, barras y
diálogos
M01 a M12; RF-M01-01; RF-
## M02-06; RF-M04-01; RF-M10-
09; RF-M11-06; RF-M01-12 a
RF-M01-15; RF-M11-07 a RF-
## M11-10
Recorrido con Label, Button, TextField,
PasswordField, ComboBox, ListView,
CheckBox, RadioButton, TableView,
DatePicker, ImageView, MenuBar,
ToolBar, ContextMenu, Alert, Dialog,
FileChooser y DirectoryChooser.
Mismos recorridos en Claro y Oscuro,
con efectos activos e inactivos.
## RA2 IL2.1
conexión; IL2.2
## SQL CRUD; IL2.3
validación
## M01, M05, M06, M11, M12;
RF-M01-01 a RF-M01-06; RF-
## M12-04; RF-M11-02
Conexión pgJDBC y operaciones SQL de
cuentas, parámetros y bitácora;
PreparedStatement y consultas
agregadas. Cuentas admiten baja lógica;
parametros y bitacora solo inserción y
consulta, sin UPDATE ni DELETE
operativo. Los parámetros se añaden con
vigencia, motivo y autor (Cuadra et al.,
## 2026e).
RA3 IL3.1 hilos;
## IL3.2
sincronización;
IL3.3 ciclo de vida
## M04, M05, M06, M09, M10,
M12; RF-M05-01 a RF-M05-04;
RF-M06-07 y RF-M06-12; RF-
## M12-08
H1 lote cancelable; H2
vencimientos/mora; H3 USD/NIO; H4
contador crítico; H5 exportación; H6
cierre controlado, con trazas y latencia
## UI.
## RA4 IL4.1 ORM;
IL4.2 diseño de
reportes; IL4.3
integración
M02 a M10; RF-M04-01; RF-
M06-12; RF-M10-01 a RF-M10-
## 12
CRUD Hibernate en propiedades,
unidades, inquilinos, contratos, cargos,
depósitos, mantenimiento y tasas; diez
catálogos Jasper con exportación.
## RA5 IL5.1 HTTP;
## IL5.2
procesamiento;
IL5.3 interfaz
M09 y M11; RF-M09-01 a RF-
## M09-09; RF-M11-05
Solicitud HTTPS al servicio USD/NIO y
fixtures JSON; caché, procedencia,
estado y fallos en JavaFX sin
congelación.
Métricas de Éxito
La Tabla 70 presenta indicadores de Aceptación.

## 100
## Tabla 70
Indicadores de Aceptación
IndicadorFórmulaFuente del DatoMetaMedición
## Propuesta
Aceptación MustMust conformes / Must definidos
## × 100
Matriz de pruebas por RF y
## RNF
## 100 %25/11/2026
## (HIT-07)
TrazabilidadRF con HU, CU, RN y RA / 126
## × 100
Apéndice C100 %Cierre de cada
sprint y del
proyecto: HIT-01,
## HIT-02, HIT-04,
HIT-05, HIT-07 y
HIT-10 (Plan,
## Tabla 10).
Integridad de cobro|importe convertido -
aplicaciones - crédito|
Consulta de contraste por
pago
Cero centavos en 100
operaciones
10/11 y 25/11/2026
## (HIT-04/HIT-07)
Rentas duplicadasFilas duplicadas por contrato y
## AAAA-MM
Consulta agregada tras
reintento H1
## 010/11/2026
## (HIT-04)
Continuidad de recibosNúmeros ausentes entre mínimo
y máximo confirmados
Libro de recibos, incluidos
anulados
0 con base íntegra de
prueba
10/11 y 25/11/2026
## (HIT-04/HIT-07)
Capacidad de respuestaObservación 29 de 30 tiempos
ordenados
Registro de medición del
panel V1
≤ 2 s25/11/2026
## (HIT-07)
RecuperaciónTiempo fin de restauración -
inicio
Manifiesto, cronómetro y
consulta de filas
≤ 30 min y 0 hashes
discordantes
17/11 y 25/11/2026
## (HIT-05/HIT-07)
Reportes conciliadosReportes con totales conformes /
## 10 × 100
Archivos y consulta de
referencia
## 100 %17/11/2026
## (HIT-05)
Cobertura RARA con evidencia reproducible /
## 5 × 100
Guion de exposición y
repositorio
100 %26/11 y 1/12/2026
## (HIT-08/HIT-10)

## 101
IndicadorFórmulaFuente del DatoMetaMedición
## Propuesta
Campo y usuariosNúmero de participantes reales
de validación
Alcance académico
declarado
0 previsto; sin
indicador de
satisfacción
26/11 y 1/12/2026
## (HIT-08/HIT-10)
Equivalencia de
presentación
Recorridos conformes por
combinación / 10 × 100
Guiones ESC-25 a ESC-27 y
capturas
100 % en 4
combinaciones; 0
pérdidas
## 25/11/2026
## (HIT-07)
Nota. Fechas según Cuadra et al. (2026f), Tabla 10, HIT-01 a HIT-10. No hay mediciones de una implementación de Latch. Los
porcentajes y tiempos son metas; el último indicador delimita el método y no mide beneficio.
Riesgos del Producto
Los riesgos locales del producto RSP-01 a RSP-16 se valoran con probabilidad e impacto
de uno a tres como estimación de planificación. El registro canónico es el del Plan de
Implementación, RSK-01 a RSK-18, con escala de uno a cinco; la valoración local no sustituye
ese registro ni convierte sus puntuaciones. El nivel corresponde al producto de ambos: uno o dos
es bajo, tres o cuatro es medio y seis o nueve es alto. Cada responsable nominal coordina la
mitigación propuesta y otro integrante revisa su evidencia. Los riesgos financieros tienen
prioridad porque un resultado visible puede ser incorrecto aunque la pantalla funcione. Las
contingencias conservan el alcance académico y evitan ocultar un requisito Must incumplido
detrás de una demostración reducida o de una afirmación de validación que no cuenta con
evidencia.

## 102
La Tabla 71 presenta matriz de Riesgos.
## Tabla 71
Matriz de Riesgos
ID y DescripciónPINivelMitigaciónContingenciaResponsable
## Propuesto
RSP-01 Recibo duplicado
o hueco confirmado
236 altoContador transaccional,
unicidad e idempotencia H4.
Asignación canónica: Plan
de Implementación, Tabla 4;
Steven, sección crítica y
bloqueo local; Alfredo,
contador, transacción única,
numeración y TAR-038.
Bloquear cobros y reconciliar
libro antes de reanudar.
## Alfredo Montalvan
RSP-02 Doble renta
mensual por concurrencia
236 altoUnicidad contrato/período y
bloqueo por contrato.
Revertir la operación fallida y
ejecutar consulta de duplicados.
## Alfredo Montalvan
RSP-03 Tasa externa no
apta para transacción
339 altoUso informativo y
autorización de fuente por
fecha.
Cobro en misma moneda o
referencia BCN manual
documentada.
## Aris Gavarrete
RSP-04 Falla o cambio de
esquema API
326 altoCaché, validación JSON y
límites de reintento.
Mostrar antigüedad y bloquear
conversión sin autorización.
## Aris Gavarrete
RSP-05 Interfaz
bloqueada por tareas
236 altoTask/Service, progreso y
cancelación cooperativa.
Deshabilitar tarea masiva hasta
corregir bloqueo y repetir
prueba.
## Caleb Tardencilla
RSP-06 Datos personales
reales en demo
236 altoFixtures sintéticos, mínimo
de campos y revisión de
archivos.
Retirar copia, documentar
incidente y regenerar base
sintética.
## Steven Cuadra
RSP-07 Mora o deducción
interpretada como legal
universal
236 altoParámetros de diseño y
análisis de ámbito
normativo.
Mantener demo sintética y retirar
uso real de condición no
evaluada.
## Steven Cuadra

## 103
ID y DescripciónPINivelMitigaciónContingenciaResponsable
## Propuesto
RSP-08 Incompatibilidad
Hibernate/Jasper
236 altoDTO, dependencias fijadas y
prueba de convergencia.
Aislar módulo de reporte y
corregir transitivas sin cambiar
## RF.
## Aris Gavarrete
RSP-09 Pérdida de base o
adjuntos
236 altoRespaldo consistente, hashes
y restauración aislada.
Recuperar último paquete íntegro
y medir pérdida respecto al
objetivo.
## Alfredo Montalvan
RSP-10 Transacciones
JDBC y ORM separadas
236 altoUn servicio financiero y
conexión compartida para
auditoría.
Detener operación afectada y
corregir frontera antes de
aceptación.
## Alfredo Montalvan
RSP-11 Disponibilidad de
12 h no confirmada
326 altoCapacidad prorrateada y
reserva de revisión.
Reasignar tareas y retirar
Could/Should antes de afectar
## Must.
## Steven Cuadra
RSP-12 Tabla o PDF
ilegible al exportar
224 medioMuestra de plantillas y
revisión visual con datos
largos.
Corregir fuente y anchos antes de
emitir versión final.
## Caleb Tardencilla
RSP-13 Restauración
reutiliza número ya
entregado externamente
133 medioConciliar manifiesto y
último libro antes de
habilitar cobros.
Mantener emisión bloqueada
hasta recuperar el libro y
consecutivo más recientes; sin
recuperación, no emitir recibos.
## Alfredo Montalvan
RSP-14 Evidencias de
distintas revisiones
mezcladas
224 medioEtiqueta Git, entorno y fecha
por prueba.
Repetir solo pruebas afectadas
sobre el paquete definitivo.
## Caleb Tardencilla
RSP-15 Tema oculta
campos o reduce contraste
224 medioSuperficies opacas, contraste
por tema, guiones con y sin
efectos.
Bloquear entrega visual hasta
corregir ambos temas; conservar
mismas operaciones.
## Caleb Tardencilla
RSP-16 Preferencia se
comparte entre cuentas o
falla
224 medioClaves por id opaco,
fallback Claro y manejo de
fallos locales.
Conservar elección de sesión y
aviso; reparar almacén sin
cambiar datos del dominio.
## Caleb Tardencilla

## 104
Criterios de Aceptación del Producto
Definición de Terminado
La versión termina cuando todos los Must y las treinta y ocho reglas tienen evidencia de
conformidad, sin defectos de saldos o permisos. Should y Could conservarán su estado explícito
para mostrar el alcance entregado. El paquete identificará revisión Git, dependencias, instalación
y base sintética restaurable. Los diez reportes conciliarán con sus consultas y el respaldo
superará una restauración aislada. Los cuatro integrantes revisarán entregables ajenos y
registrarán las discrepancias resueltas, sin atribuir aceptación de cliente o docente ni pruebas de
la aplicación que aún no se ejecutaron. Cada incremento identifica versión, fuentes, esquema
compatible, documentos, respaldo y huellas; cada cambio de alcance o configuración registra
motivo, impacto y responsable. C01 quedó resuelto por decisión aprobada el 25 de octubre de
2026: 66 ERR y 89 MSG por excepción limitada a DC-17; los demás inventarios se conservan.
La cancelación voluntaria es un resultado normal sin ERR; estas definiciones son actuales y no
se atribuyen a acuerdos históricos.
Condiciones de Entrega y Revisión
La Tabla 72 presenta puertas de Aceptación Interna.

## 105
## Tabla 72
Puertas de Aceptación Interna
CondiciónProcedimiento y Evidencia PrevistaResponsable y
## Revisor
FuncionalidadEjecutar 126 criterios Dado/Cuando/Entonces y casos
críticos; registrar esperado, obtenido real y archivo
de evidencia solo al ejecutarlos.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».
Presentación y
accesibilidad
Ejecutar ESC-25 a ESC-27: 2 temas completos,
cambios sin pérdida, preferencia local, teclado y sin
efectos.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».
Integridad y
concurrencia
Probar H1 a H6 y las dos variantes de D22 con diez
emisores en dos JVM: cien pagos más diez rollback
previos al commit, o cien pagos con diez anulaciones
posteriores; comprobar 100 recibos y
siguiente_numero=101 en ambas, además de
reintentos, estados y redondeo. Usar CP-041, CP-042
y CP-032 existentes.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».
Datos y
recuperación
Revisar restricciones, CRUD SQL/ORM, respaldo y
restauración sin pérdida de vínculos.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».
Integración y
reportes
Registrar petición HTTP real solo si se ejecuta; usar
fixtures para fallos; conciliar PDF/XLSX de 10
catálogos.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».
Academia y
documentación
Comprobar cinco RA y sus IL con evidencia de la
misma versión; citas APA, autoría y datos sintéticos.
Responsable nominal y
revisor según Plan
(Cuadra et al., 2026f),
Tablas 4–5 y «Revisión
Cruzada y Decisión
## Técnica».

## 106
CondiciónProcedimiento y Evidencia PrevistaResponsable y
## Revisor
Decisión de
cierre
Reunión propuesta 25 de noviembre: cada
responsable presenta evidencia; cambios Must
fallidos impiden declarar terminado. El cierre
relaciona requisitos, tareas, casos, resultados y
evidencias RA1–RA5 de la misma versión. Toda
excepción requiere justificación y aceptación
expresa; los pendientes aceptados quedan
identificados junto con versiones y huellas. Se
distinguen entrega documental, implementación y
validación del producto.
Participantes y decisión
interna según Plan
(Cuadra et al., 2026f),
## Tablas 4–5. Steven
registra la decisión.
## Entrega
documental de
este PRD
Contar identificadores y relaciones, revisar estilos,
convertir DOCX a PDF e inspeccionar páginas. Esta
verificación no ejecuta Latch.
Alcance de
comprobación del
archivo entregado,
separado de la
evaluación futura del
producto.
El plan de aceptación especifica las pruebas y todavía no es un informe de ejecución de
Latch. El Apéndice B contiene escenarios, datos, pasos y resultados esperados; el Apéndice D
resume prioridades para controlar cambios. Cada incidente identificará requisito, revisión,
responsable y fecha de corrección antes de repetir la prueba afectada. La entrega conservará el
contexto académico y las fechas internas adoptadas para el proyecto. La revisión jurídica de
producción, los estudios con usuarios y las mediciones de beneficios quedan fuera de la
evidencia de este prototipo.

## 107
## Referencias
American Psychological Association. (2025). Student paper setup guide.
https://apastyle.apa.org/instructional-aids/student-paper-setup-guide.pdf
Apache Software Foundation. (s. f.). Maven releases history. Recuperado el 22 de octubre de
2026, de https://maven.apache.org/docs/history.html
AppFolio. (s. f.). FolioSpace overview. Recuperado el 22 de octubre de 2026, de
https://www.appfolio.com/help/online-portal
Asamblea Nacional de la República de Nicaragua. (29 de marzo de 2012). Ley N.º 787, Ley de
Protección de Datos Personales. La Gaceta, Diario Oficial, 61, 2402–2410.
https://sajurin.enriquebolanos.org/docs/G-2012-03-29.pdf
AYR Tech (Pty) Ltd. (s. f.-a). API authentication notes. ExchangeRate-API. Recuperado el 22
de octubre de 2026, de https://www.exchangerate-api.com/docs/authentication
AYR Tech (Pty) Ltd. (s. f.-b). ExchangeRate-API. ExchangeRate-API. Recuperado el 22 de
octubre de 2026, de https://www.exchangerate-api.com/
AYR Tech (Pty) Ltd. (s. f.-c). Our exchange rate data. ExchangeRate-API. Recuperado el 22 de
octubre de 2026, de https://www.exchangerate-api.com/product/our-exchange-rate-data
AYR Tech (Pty) Ltd. (s. f.-d). Standard requests. ExchangeRate-API. Recuperado el 22 de
octubre de 2026, de https://www.exchangerate-api.com/docs/standard-requests
AYR Tech (Pty) Ltd. (s. f.-e). Supported currencies. ExchangeRate-API. Recuperado el 22 de
octubre de 2026, de https://www.exchangerate-api.com/docs/supported-currencies
AYR Tech (Pty) Ltd. (s. f.-f). Terms of service. ExchangeRate-API. Recuperado el 22 de octubre
de 2026, de https://www.exchangerate-api.com/terms
Balzarini, J., y Boyd, M. L. (2021). Working with them: Small-scale landlord strategies for
avoiding evictions. Housing Policy Debate, 31(3–5), 425–445.
https://doi.org/10.1080/10511482.2020.1800779
Banco Central de Nicaragua. (20 de octubre de 2025). BCN informa que la tasa de deslizamiento
del tipo de cambio del córdoba respecto al dólar de los Estados Unidos de América

## 108
continuará en cero por ciento (0 %) anual en el año 2026.
https://www.bcn.gob.ni/divulgacion-prensa/bcn-informa-que-la-tasa-de-deslizamiento-
del-tipo-de-cambio-del-cordoba-respecto
Blanco, A. G., Fretes Cibils, V., y Muñoz, A. F. (Eds.). (2016). Rental housing wanted: Options
for expanding housing policy. Inter-American Development Bank.
https://publications.iadb.org/publications/english/document/Rental-Housing-Wanted-
Options-for-Expanding-Housing-Policy.pdf
Buildium. (s. f.). Open API, powered by Buildium. Recuperado el 22 de octubre de 2026, de
https://developer.buildium.com/
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026a). Brief de diseño de interfaz y
experiencia de usuario para Latch [Documento académico no publicado]. Universidad
Americana (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026b). Documento de flujo de la
aplicación de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026d). Documento de requisitos
técnicos de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026e). Esquema de base de datos y
servicios de Latch [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026f). Plan de implementación de
Latch [Documento académico no publicado]. Universidad Americana (UAM).
Eclipse Adoptium. (s. f.-a). Latest assets for Java 21, HotSpot, Windows x64 [Respuesta de API].
Recuperado el 22 de octubre de 2026, de
https://api.adoptium.net/v3/assets/latest/21/hotspot?
architecture=x64&image_type=jdk&os=windows

## 109
Eclipse Adoptium. (s. f.-b). Temurin support. Recuperado el 22 de octubre de 2026, de
https://adoptium.net/support/
Gluon. (s. f.). OpenJFX 21 release notes. Recuperado el 22 de octubre de 2026, de
https://gluonhq.com/products/javafx/openjfx-21-release-notes/
Google. (s. f.). Gson [README de la etiqueta 2.13.2]. Recuperado el 22 de octubre de 2026, de
https://raw.githubusercontent.com/google/gson/gson-parent-2.13.2/README.md
Google. (10 de septiembre de 2025). Gson 2.13.2 [Software].
https://github.com/google/gson/releases/tag/gson-parent-2.13.2
Hibernate. (s. f.). 7.4 series. Recuperado el 22 de octubre de 2026, de
https://hibernate.org/orm/releases/7.4/
Jaspersoft. (s. f.-a). JasperReports Library Artifacts [pom-parent.xml de la etiqueta 7.0.8].
Recuperado el 22 de octubre de 2026, de
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/pom-parent.xml
Jaspersoft. (s. f.-b). JRBeanCollectionDataSource (JasperReports Javadoc 7.0.8 API).
Recuperado el 22 de octubre de 2026, de
https://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/data/
JRBeanCollectionDataSource.html
Jaspersoft. (s. f.-c). README [etiqueta 7.0.8]. Recuperado el 22 de octubre de 2026, de
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/README.md
Jaspersoft. (2026a). Jaspersoft Studio community edition 7.0.8.
https://community.jaspersoft.com/download-jaspersoft/community-edition/jaspersoft-
studio_7.0.8
Jaspersoft. (2026b, 5 de agosto). JasperReports Library 7.0.8 [Software].
https://sourceforge.net/projects/jasperreports/files/jasperreports/JasperReports%207.0.8/
Malewicz, M. (s. f.). Aurora UI: new visual trend for 2021. Hype4 Academy. Recuperado el 22
de octubre de 2026, de https://hype4.academy/articles/design/aurora-ui-new-visual-trend-
for-2021

## 110
OpenJFX. (s. f.-a). GaussianBlur (JavaFX 21). Recuperado el 22 de octubre de 2026, de
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/effect/GaussianBlur.html
OpenJFX. (s. f.-b). JavaFX CSS reference guide. Recuperado el 22 de octubre de 2026, de
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html
OpenJFX. (s. f.-c). RadialGradient (JavaFX 21). Recuperado el 22 de octubre de 2026, de
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/paint/RadialGradient.html
OpenJFX. (s. f.-d). Service (JavaFX 21). Recuperado el 22 de octubre de 2026, de
https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Service.html
OpenJFX. (s. f.-e). Task (JavaFX 21). Recuperado el 22 de octubre de 2026, de
https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html
Oracle. (s. f.-a). HttpClient (Java SE 21 & JDK 21). Recuperado el 22 de octubre de 2026, de
https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/
HttpClient.html
Oracle. (s. f.-b). Supported JDK versions. Recuperado el 22 de octubre de 2026, de
https://docs.oracle.com/en/java/java-components/javafx/21/javafx-users-guide/supported-
jdk-versions.html
OWASP Foundation. (s. f.). Password storage cheat sheet. Recuperado el 22 de octubre de 2026,
de https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
The PostgreSQL Global Development Group. (s. f.-a). 9.17. Sequence manipulation functions.
Recuperado el 22 de octubre de 2026, de https://www.postgresql.org/docs/18/functions-
sequence.html
The PostgreSQL Global Development Group. (s. f.-b). 13.3. Explicit locking. Recuperado el 22
de octubre de 2026, de https://www.postgresql.org/docs/18/explicit-locking.html
The PostgreSQL Global Development Group. (s. f.-c). Versioning policy. Recuperado el 22 de
octubre de 2026, de https://www.postgresql.org/support/versioning/
PostgreSQL JDBC Driver. (s. f.). Download. Recuperado el 22 de octubre de 2026, de
https://jdbc.postgresql.org/download/

## 111
Presidencia de la República de Nicaragua. (19 de octubre de 2012). Decreto N.º 36-2012,
Reglamento de la Ley N.º 787, Ley de Protección de Datos Personales. La Gaceta, Diario
Oficial, 200, 8381–8386. https://sajurin.enriquebolanos.org/docs/G-2012-10-19.pdf
Reosti, A., Hess, C., Allen, C., y Crowder, K. (2024). “Mom-and-pop” landlords and regulatory
backlash: A Seattle case study. Socius: Sociological Research for a Dynamic World, 10.
https://doi.org/10.1177/23780231241296169
World Wide Web Consortium. (12 de diciembre de 2024). Web content accessibility guidelines
(WCAG) 2.2. https://www.w3.org/TR/2024/REC-WCAG22-20241212/
World Wide Web Consortium. (11 de diciembre de 2025). Guidance on applying WCAG 2 to
non-web information and communications technologies (WCAG2ICT).
https://www.w3.org/TR/2025/NOTE-wcag2ict-22-20251211/

## 112
## Apéndice A
## Glosario
La Tabla A1 presenta términos del producto.
## Tabla A1
Términos del Producto
TérminoDefinición
UnidadEspacio alquilable con código propio dentro de una propiedad.
CanonMonto pactado de renta para el período contractual.
CargoObligación por concepto e importe con fecha de vencimiento.
MoraRecargo de diseño por retraso después de la gracia; su legalidad
concreta no se presume.
Depósito de garantíaImporte retenido separado de renta, sujeto a liquidación documentada.
ReciboComprobante administrativo de un pago registrado; conserva su
número si se anula.
Estado de cuentaRelación de cargos, abonos y saldo de un contrato a una fecha de corte.
Tasa de cambioValor NIO por USD con fecha y fuente, congelado en cada operación
entre monedas.
Período de facturaciónMes AAAA-MM al que corresponde el cargo de renta; no implica
factura fiscal.
Saldo a favorCrédito del contrato en moneda contractual que se aplica a un cargo
posterior.
AplicaciónVínculo que distribuye un pago, crédito o garantía a una obligación.
IdempotenciaUna misma identidad de solicitud produce una sola operación
confirmada aunque se reenvíe.
Baja lógicaDesactivación que conserva el registro y sus referencias históricas.
DTOObjeto de datos preparado por servicios para presentar o generar
reportes sin escritura.
Commit y rollbackConfirmación y reversión de una transacción, respectivamente.
CachéÚltima respuesta válida guardada con procedencia y antigüedad.
Dato sintéticoRegistro ficticio elaborado para un escenario y no atribuible a una
persona real.
RA e ILResultado de aprendizaje e indicador de logro del resumen académico
aportado.
NIO y USDCórdoba nicaragüense C$ y dólar estadounidense US$, únicas monedas
del catálogo.
H1 a H6H1 lote; H2 vencimientos y mora; H3 tasas; H4 contador; H5 reportes;
H6 detención controlada.

## 113
TérminoDefinición
Aurora UIDirección visual con luces orgánicas estáticas de baja intensidad; no
biblioteca de software.
Claro y OscuroLos dos temas completos de presentación personal de Latch.
Modalidad sin efectosOpción independiente del tema que elimina luces y desenfoques
decorativos sin alterar operaciones.
La Tabla A2 presenta catálogos compartidos.
## Tabla A2
## Catálogos Compartidos
CatálogoValores Exactos
Monedas y rolesNIO (C$), USD (US$); Propietario, Asistente.
ContratoBorrador, Vigente, Por vencer, Vencido, Terminado, Cancelado.
UnidadDisponible, Ocupada, En mantenimiento, Inactiva.
CargoPendiente, Parcial, Pagado, Vencido, Anulado.
PagoRegistrado, Anulado.
MantenimientoAbierta, En proceso, Resuelta, Cancelada.
DepósitoRetenido, Aplicado, Devuelto parcial, Devuelto.
Tipo de cargoRenta, Mora, Reparación, Servicio, Otro.
Método de pagoEfectivo, Transferencia, Depósito bancario, Otro.
Tipo de unidadCasa, Apartamento, Cuarto, Local comercial.
La Tabla A3 presenta parámetros de configuración adoptados.
## Tabla A3
Parámetros de Configuración Adoptados
ParámetroValor Inicial y RangoAplicación
Intentos de acceso5; entero de 3 a 10Bloqueo por cuenta; RN-15, RF-M01-04.
Duración de bloqueo15 minutos; de 5 a 60
minutos
Cambio por Propietario para nuevos
bloqueos.
Inactividad de sesión15 minutos; de 5 a 60
minutos
Aviso previo de 60 s; conserva borrador
local sin secretos antes de cerrar.
Gracia de mora3 días calendario; de 0 a 30La mora inicia el día posterior al
vencimiento más la gracia.
Modalidad de moraPorcentaje; Porcentaje o
Monto fijo
Solo una modalidad por registro de política
y una aplicación por renta; inserción con
vigente_desde y motivo, sin UPDATE de
parámetros.

## 114
ParámetroValor Inicial y RangoAplicación
Valor porcentual0 %; de 0 a 10 % inclusiveBase: principal vencido pendiente al
generar; nunca Mora. El evento de cada
Mora conserva {politica_id, tipo, valor,
base}.
Valor fijo0; de 0 a 10000 en moneda
contractual
Con valor cero no genera recargo; rango
técnico de demo, sin afirmar legalidad.
Aviso Por vencer30 días; entero de 1 a 90Días calendario hasta fin; comparte filtro
con panel y reportes.
Revisión H25 minutos; de 1 a 60 minutosSolo mientras la aplicación está abierta;
evita instancias locales superpuestas.
Actualización H324 horas; mínimo 24 y
máximo 168 horas
Respeta próxima actualización del
proveedor y caché compartida.
HTTP y reintentoConexión 5 s; petición 10 s;
1 reintento tras 60 s
Valores fijos; HTTP 429 respeta Retry-
After o espera 24 h si falta. No reintenta
clave o cuota. HTTP 408 se trata como
timeout transitorio y comparte el único
reintento autorizado tras 60 s.
Cierre H610 segundosValor fijo; si una transacción no finaliza,
muestra bloqueo y conserva proceso.
Adjuntos5 MiB por archivo; PDF,
PNG y JPEG según módulo
Límite fijo; fotografía y logotipo solo
## PNG/JPEG.
Aviso de respaldo24 horas; siete diarios y
cuatro semanales
Alerta por antigüedad; respaldo manual por
Propietario, cifrado AES-256-GCM con
contraseña de respaldo. Verificar
autenticidad y descifrar antes de restaurar;
autenticidad o clave inválida:
## ERR-059/MSG-082.
Contador de recibos000001 en base nueva;
creciente global
No editable por ningún rol; no reinicia por
año.
Precisión y
calendario
2 decimales monetarios, 8
de tasa; America/Managua
HALF_EVEN; instante y fecha operativa
de now() de la BD, presentación
America/Managua; fecha efectiva del pago
separada. Desfase local > 5 min bloquea
nuevas operaciones financieras (ERR-057 /
MSG-080) hasta corregirlo, sin interrumpir
una transacción financiera ya iniciada ni
sustituir la validación de la fecha efectiva.
Tema personalClaro; exactamente Claro u
## Oscuro
Última elección local antes del acceso y
preferencia por id opaco después; ambos
roles.

## 115
ParámetroValor Inicial y RangoAplicación
Modalidad sin
efectos
Desactivada al inicio;
opción activable en ambos
temas
Suprime luces y desenfoques;
independiente del tema; no modifica
dominio ni reportes.

## 116
## Apéndice B
Matriz de Evidencia Documental y Escenarios Sintéticos de Revisión
La evidencia disponible comprende investigación documental y revisión de archivos. Los
escenarios fijan datos, pasos y resultados esperados para una implementación futura. Solo una
ejecución identificada por entorno, fecha, revisión y evidencia podrá agregar resultados
obtenidos. La Tabla B1 separa fuentes y límites; la Tabla B2 conserva los 27 escenarios ESC de
diseño y añade «CP equivalente». CP-001 a CP-100 del Plan (Cuadra et al., 2026f, Apéndice B)
son los únicos identificadores de ejecución; las ampliaciones se registran como variantes de un
CP existente. La tabla inversa corresponde al Plan. Los nombres, contratos y montos son
sintéticos y no representan arrendadores ni inquilinos reales. La referencia cambiaria oficial
conserva su fuente y alcance anual, sin convertirse en una cotización comercial medida durante
la revisión documental.
## Tabla B1
Base de Evidencia y Límite
ElementoEvidencia DisponibleUso PermitidoLímite
Contexto del
proyecto
Base académica y funcional aportada,
más los seis documentos del proyecto
(Cuadra et al., 2026a, 2026b, 2026c,
## 2026d, 2026e, 2026f).
Fuente de módulos, catálogos,
autores y mínimos.
No incluye el syllabus original; las
decisiones de los documentos hermanos se
contrastan con el canon de coherencia.
CienciaDos artículos científicos y un libro
institucional con alcance delimitado
en Apéndice E.
Antecedentes de administración
y digitalización.
No validan demanda, preferencias o
beneficios en Managua.

## 117
ElementoEvidencia DisponibleUso PermitidoLímite
TecnologíasDocumentación oficial y fuentes
versionadas.
Existencia, capacidades y
compatibilidad declarada.
No hubo compilación, conexión, exportación
Jasper ni pruebas de concurrencia de Latch.
Normativa y
cambio
Publicaciones BCN y facsímiles de
textos legales.
Análisis de artículos y
distinción tasa
oficial/comercial.
Sin certificación de reformas exhaustivas,
dictamen contractual ni llamada de tasas.
Revisión del
producto
Criterios RF/RNF y guiones
sintéticos.
Planificar aceptación
académica reproducible.
No hubo participantes reales ni resultados
funcionales obtenidos.
## Tabla B2
Escenarios de Aceptación Previstos
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-01 /
## CU-01
Cuenta sintética
propietario.demo;
acceso correcto una
vez y cinco fallos
con otra cuenta.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-01. Registrar
revisión, entorno y fecha
al ejecutarlo.
Sesión atribuida a una
cuenta; intento y resultado
auditados.
Cero sesiones tras
fallo; una sesión del
rol correcto tras
credencial válida.
## CP-001, CP-002,
## CP-003
## ESC-02 /
## CU-10
Contrato sintético
con inicio
23/10/2026, fin
23/10/2027 y
activación
## 23/10/2026, US$
100.00 y garantía
## US$ 100.00.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-10. Registrar
revisión, entorno y fecha
al ejecutarlo.
Contrato Vigente o Por
vencer, unidad Ocupada y
depósito Retenido cuando
corresponde.
Una ocupación activa
y un depósito; cero
movimientos si falla el
commit.
## CP-017, CP-042

## 118
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-03 /
## CU-13
Lote sintético de
500 contratos
elegibles, 20 rentas
ya creadas y una
cancelación tras 100
confirmaciones.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-13. Registrar
revisión, entorno y fecha
al ejecutarlo.
Una renta por contrato y
período confirmado;
resumen de generados,
omitidos y cancelados.
Cero duplicados;
progreso coherente;
interfaz responde en
200 ms o menos.
## CP-021, CP-045,
## CP-047, CP-099
## ESC-04 /
## CU-16
Dos rentas sintéticas
NIO de C$
1,000.00; pago C$
600.00; UUID fijo
para el reintento.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-16. Registrar
revisión, entorno y fecha
al ejecutarlo.
Pago Registrado y saldos
reducidos por el abono;
recibo único accesible
después de commit.
Saldo total C$
1,400.00, un pago y un
recibo aun con dos
solicitudes iguales.
## CP-026, CP-043
## ESC-05 /
## CU-17
Ejemplo sintético:
US$ 100.00 de
obligación y C$
3,662.43 recibidos;
tasa oficial
documentada de
2026 de 36.6243
## NIO/USD.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-17. Registrar
revisión, entorno y fecha
al ejecutarlo.
Monto original, moneda,
equivalente y tasa quedan
vinculados al recibo sin
cambiar al actualizar caché.
Equivalente US$
100.00 y saldo cero;
tasa original intacta
tras nueva consulta.
## CP-029/CP-030
(variantes con
tasa BCN
## 36.6243),
## CP-066
## ESC-06 /
## CU-18
Recibo sintético
000001 de C$
600.00; cargo inicial
## C$ 1,000.00.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-18. Registrar
revisión, entorno y fecha
al ejecutarlo.
Número conservado, pago
Anulado, saldos repuestos y
bitácora con usuario, fecha
y motivo.
Después de anular,
saldo C$ 1,000.00 y
mismo número visible;
cero eliminación
física.
## CP-032, CP-033

## 119
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-07 /
## CU-21
Garantía sintética
## US$ 100.00;
deducción por
deuda US$ 30.00 y
devolución US$
## 70.00.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-21. Registrar
revisión, entorno y fecha
al ejecutarlo.
## Contrato Terminado;
unidad Disponible si no
está en mantenimiento;
garantía distribuida sin
diferencia.
100 = 30 + 70 + 0; una
devolución y estado
## Devuelto.
## CP-034, CP-035,
## CP-036
## ESC-08 /
## CU-23
Solicitud sintética
de reparación,
prioridad Alta y
gasto C$ 500.00.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-23. Registrar
revisión, entorno y fecha
al ejecutarlo.
Solicitud Resuelta o
Cancelada con historial y
gasto asociado; ningún
cargo automático al
contrato.
Un gasto de C$ 500.00
en la propiedad y cero
cargo al inquilino
salvo registro
separado.
## CP-037, CP-038
## ESC-09 /
## CU-25
Fixtures JSON
sintéticos: success,
error, NIO ausente,
base EUR, NIO=1 y
## NIO=100
admitidos;
NIO=0.99 y
NIO=100.01 en
cuarentena; red
simulada.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-25. Registrar
revisión, entorno y fecha
al ejecutarlo.
Caché válida renovada o
error trazable sin tasa
inventada; saldos y pagos
existentes permanecen
intactos. Fuera de banda no
guarda referencia vigente y
registra M09,
## REFERENCIA_EN_CUAR
ENTENA, con valor y
huella, sin tabla nueva.
Cien por ciento de
fixtures inválidos
rechazados; nunca
aplica tasa informativa
por defecto.
CP-053 a
## CP-066; CP-058
(variantes de
banda 1–100)
## ESC-10 /
## CU-29
Renta sintética C$
1,000.00, abono C$
600.00 y corte
posterior al abono.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-29. Registrar
revisión, entorno y fecha
al ejecutarlo.
Documento con corte,
movimientos y saldo
conciliado; ninguna
escritura financiera por
consultar.
Saldo C$ 400.00 y
total del reporte igual
al cálculo de
persistencia.
## CP-071

## 120
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-11 /
## CU-30
Reporte sintético de
10000 filas; destino
de prueba y un
archivo previo
conocido.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-30. Registrar
revisión, entorno y fecha
al ejecutarlo.
Archivo final íntegro y
datos sin cambios; PDF
legible o XLSX con
importes numéricos.
Exportación cancelada
conserva el hash del
archivo previo y no
deja archivo final
parcial.
CP-100 (variante
de cancelación),
## CP-050
## ESC-12 /
## CU-36
Base y adjuntos
sintéticos de un GiB
cada uno, con H1 en
curso.
Preparar precondiciones;
ejecutar flujo principal y la
primera alternativa del
caso CU-36. Registrar
revisión, entorno y fecha
al ejecutarlo.
Respaldo identificado si fue
solicitado; cierre sin
transacciones parciales ni
tareas de aplicación
huérfanas.
Hashes coinciden;
cierre en diez
segundos o aviso de
bloqueo explícito, sin
cerrar a la fuerza.
## CP-051, CP-095,
## CP-049, CP-050
## ESC-13 /
## H4
Diez emisores
concurrentes (hilos)
repartidos en dos
JVM; cien pagos y
diez reversiones
adicionales antes de
commit.
Lanzar los diez hilos en
dos JVM, forzar diez
rollbacks antes de commit
y consultar recibos
incluidos anulados. El
límite de tres sesiones
LAN cuenta clientes de
aplicación, no hilos de
prueba.
Un recibo por pago
confirmado; reversiones sin
consumir número.
100 números únicos
contiguos
confirmados; 0
duplicados y 0 huecos.
CP-041 (10 hilos
en 2 JVM),
## CP-042 (10
reversiones)
## ESC-14 /
## H2
## Renta C$ 1,000.00,
abono C$ 400.00,
gracia 3 días y mora
5 % una vez.
Fijar vencimiento 5/11;
revisar al 8/11 y al 9/11;
repetir H2 el 10/11.
Al 8/11 no genera; al 9/11
genera C$ 30.00 sobre C$
600.00; repetir no duplica.
Un cargo Mora C$
30.00 y ninguna mora
sobre mora.
CP-022 (variante
con abono
parcial), CP-046
## ESC-15 /
## RN-02
## Inicio 31/01/2027;
fin 28/02/2027; otro
fin 27/02/2027.
Guardar ambos intervalos
en borradores distintos.
Admite un mes ajustado al
último día; rechaza un día
menos.
1 aceptado y 1
rechazado con causa
visible.
## CP-015
## ESC-16 /
## RN-01
Dos sesiones y una
unidad disponible.
Intentar activar dos
contratos superpuestos al
mismo instante.
Una activación confirma y
la otra informa conflicto.
Máximo un contrato
activo para el
intervalo.
## CP-016, CP-052

## 121
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-17 /
seguridad
Cuenta Asistente y
veinte acciones
exclusivas del
## Propietario.
Invocar pantallas y
servicios con la misma
sesión. Para M02, intentar
altas de propiedad/unidad,
bajas y restricción En
mantenimiento; comprobar
consulta y edición de
condiciones/foto de unidad
permitidas.
Deniega sin cambios ni
exposición de bitácora
restringida.
20 denegaciones y 0
efectos persistidos.
## CP-005
(variantes de
permisos M02),
## CP-094
## ESC-18 /
archivos
PDF válido 4 MiB;
ejecutable
renombrado; PDF 6
MiB.
Intentar adjuntar cada
archivo por FileChooser.
Solo acepta PDF válido;
rechaza firma y tamaño
inválidos.
1 aceptado y 2
rechazados; ninguna
ruta externa persistida.
## CP-014
## ESC-19 /
estado de
cargo
## Cargo C$ 1,000.00
con abono C$
200.00 antes y
después de vencer.
Consultar en fecha anterior
y posterior; luego pagar
remanente.
Parcial antes del
vencimiento, Vencido
después y Pagado sin saldo.
Estados y saldo
coinciden con RN-24
en 3 consultas.
## CP-024
## ESC-20 /
restauración
Respaldo AES-256-
GCM de dos GiB
con manifiesto;
variantes de clave
incorrecta,
autenticidad
alterada y huella de
adjunto discordante.
Verificar autenticidad y
descifrar con la contraseña
de respaldo; restaurar el
paquete íntegro en base
aislada e intentar cada
variante inválida.
Restaura el íntegro; clave o
autenticidad inválida
produce
ERR-059/MSG-082 antes
de restaurar; una huella
discordante impide sustituir
la base activa.
0 hashes discordantes
en éxito; rechazo
independiente de cada
variante inválida y 0
sustituciones de base
activa.
## CP-095
(variantes de
clave/autenticida
d y huellas)
## ESC-21 /
accesibilida
d
Doce módulos a
escala 200 % y diez
recorridos solo
teclado.
Recorrer foco, controles y
errores; revisar capturas y
contrastes.
Sin controles inaccesibles
ni texto recortado; foco
visible.
0 bloqueos, contraste
≥ 4.5:1 normal y ≥ 3:1
grande.
## CP-078, CP-079,
## CP-080, CP-081,
## CP-082

## 122
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-22 /
cierre H6
H1 y H5 activos, H3
esperando respuesta
y H4 confirmando.
Solicitar salir y registrar
estado de cada tarea y
conexión.
Cancela tareas cooperativas
y espera transacción; si
excede umbral avisa y sigue
abierto.
Cierre ≤ 10 s o
bloqueo explícito; 0
transacciones
parciales.
## CP-049, CP-050
## ESC-23 /
datos y SQL
Cuentas y
parámetros
sintéticos; consulta
de auditoría.
Ejecutar alta, lectura,
edición y baja lógica de
cuentas por
PreparedStatement; para
parámetros, insertar un
registro con vigencia y
consultar sin UPDATE;
consultar auditoría e
intentar inyección como
nombre.
Cuentas con CRUD
consistente; parámetros
append-only y vigentes
según now() de la BD;
bitácora sin edición. La
entrada se trata como dato,
sin ejecutar SQL adicional.
0 accesos por
inyección; 4
operaciones SQL de
cuentas verificadas; 0
UPDATE de
parámetros y bitácora.
## CP-007
(variantes JDBC
de cuentas y
parámetros
append-only),
## CP-040
## ESC-24 /
precisión
100 combinaciones
con tasas de ocho
decimales y pagos
parciales.
Convertir total con
HALF_EVEN y repartir a
cargos, ajustando última
aplicación.
Suma de aplicaciones más
crédito coincide con total
convertido.
Diferencia absoluta
0.00 en 100 casos.
## CP-031 (100
variantes)
## ESC-25 /
presentació
n
2 cuentas sintéticas
A y B; A=Oscuro,
B=Claro; último
local=Oscuro.
Abrir acceso; autenticar A;
cambiar a Claro; reiniciar;
autenticar B.
Acceso usa último local;
cada usuario carga su
preferencia por id opaco,
sin compartir nombre.
10 reinicios por cuenta
con 100 %
restauración correcta;
0 cambios de
permisos.
## CP-089
## ESC-26 /
fallos
locales
Preferencia ausente,
valor ilegible y
almacén sin
permisos de
escritura.
Probar inicio y cambios en
ambos roles con un pago
de C$ 600.00 sin
confirmar.
Falta o lectura fallida usa
Claro; escritura fallida
conserva cambio y avisa sin
bloquear.
0 campos perdidos, 0
pagos confirmados por
cambiar tema; 1 aviso
por fallo.
## CP-090, CP-091,
## CP-087

## 123
EscenarioDatos SintéticosProcedimientoResultado EsperadoCriterio MedibleCP equivalente
## ESC-27 /
equivalenci
a visual
10 recorridos
críticos en
Claro/Oscuro y
efectos
activos/inactivos.
Recorrer por teclado,
capturar módulos y medir
pares de contraste;
contrastar DTO de
reportes.
Mismos resultados, campos
legibles sobre superficies
uniformes y foco visible sin
luces.
40 recorridos sin
bloqueo; texto 4.5:1,
controles y foco 3:1; 0
diferencias
financieras.
CP-077 a
## CP-086, CP-092,
## CP-093, CP-094
Nota. La correspondencia ESC→CP es documental y no acredita ejecución ni equivalencia de fixtures. Ningún escenario fue
ejecutado sobre Latch durante la actualización documental. Los umbrales y resultados son esperados; no hay mediciones de usuarios
ni resultados de pruebas de la aplicación.

## 124
## Apéndice C
Matriz de Trazabilidad Completa
La Tabla C1 relaciona cada RF con módulo, historia, caso, regla y aprendizaje. Los
identificadores compatibles se conservan y los requisitos nuevos de presentación amplían M01 y
M11. Cada fila permite localizar el recorrido que ejercita un criterio observable del producto. Las
historias agrupan requisitos sin fusionar sus condiciones de aceptación. La cobertura académica
procede del resumen aportado y no de una consulta al syllabus original. La matriz describe
trazabilidad documental; la implementación aportará evidencia de cada Must antes de declarar
conformidad en la revisión interna prevista.
## Tabla C1
Trazabilidad Individual de Requisitos
RFMóduloHUCURNRA e IL
## RF-M01-01M01HU-M01-01CU-01RN-15RA1 IL1.2; RA2 IL2.1
## RF-M01-02M01HU-M01-01CU-01RN-15RA1 IL1.3; RA2 IL2.2
## RF-M01-03M01HU-M01-02CU-02RN-15RA2 IL2.3
## RF-M01-04M01HU-M01-02CU-02RN-15RA2 IL2.3
## RF-M01-05M01HU-M01-02CU-02RN-13RA1 IL1.2; RA2 IL2.3
## RF-M01-06M01HU-M01-02CU-02RN-13RA2 IL2.2
## RF-M01-07M01HU-M01-03CU-03RN-15RA1 IL1.2
## RF-M01-08M01HU-M01-03CU-03RN-15RA1 IL1.3
## RF-M01-09M01HU-M01-02CU-02RN-13RA2 IL2.2, IL2.3
## RF-M01-10M01HU-M01-02CU-02RN-13RA2 IL2.2, IL2.3

## 125
RFMóduloHUCURNRA e IL
## RF-M01-11M01HU-M01-02CU-02
## (restablec
imiento
por
## Propietari
o; cambio
propio
por
ambos
roles)
## RN-15RA2 IL2.2, IL2.3
## RF-M01-12M01HU-M01-04CU-37RN-35RA1 IL1.1, IL1.2
## RF-M01-13M01HU-M01-04CU-37RN-36RA1 IL1.2
## RF-M01-14M01HU-M01-04CU-37RN-36RA1 IL1.2
## RF-M01-15M01HU-M01-04CU-37RN-36RA1 IL1.3
## RF-M02-01M02HU-M02-01CU-04RN-11RA1 IL1.1; RA4 IL4.1
## RF-M02-02M02HU-M02-01CU-04RN-16RA4 IL4.1
## RF-M02-03M02HU-M02-01CU-04RN-16RA2 IL2.3
## RF-M02-04M02HU-M02-02CU-05RN-03RA4 IL4.1
## RF-M02-05M02HU-M02-02CU-05RN-25RA1 IL1.1
## RF-M02-06M02HU-M02-03CU-06RN-16RA1 IL1.1; RA2 IL2.2
## RF-M02-07M02HU-M02-03CU-06RN-01RA4 IL4.1
## RF-M02-08M02HU-M02-03CU-06RN-11RA4 IL4.1
## RF-M02-09M02HU-M02-01CU-04RN-11RA4 IL4.1
## RF-M02-10M02HU-M02-03CU-06RN-11RA4 IL4.1
## RF-M02-11M02HU-M02-03CU-06RN-01RA4 IL4.1
## RF-M03-01M03HU-M03-01CU-07RN-18RA1 IL1.1; RA4 IL4.1
## RF-M03-02M03HU-M03-01CU-07RN-18RA1 IL1.2; RA2 IL2.3

## 126
RFMóduloHUCURNRA e IL
## RF-M03-03M03HU-M03-02CU-08RN-18RA4 IL4.1
## RF-M03-04M03HU-M03-02CU-08RN-18RA2 IL2.3
## RF-M03-05M03HU-M03-02CU-08RN-25RA1 IL1.3; RA4 IL4.1
## RF-M03-06M03HU-M03-03CU-09RN-18RA1 IL1.1; RA2 IL2.2
## RF-M03-07M03HU-M03-03CU-09RN-14RA4 IL4.1
## RF-M03-08M03HU-M03-03CU-09RN-11RA4 IL4.1
## RF-M03-09M03HU-M03-01CU-07RN-18RA4 IL4.1; RA2 IL2.3
## RF-M04-01M04HU-M04-01CU-10RN-01RA1 IL1.1; RA4 IL4.1
## RF-M04-02M04HU-M04-01CU-10RN-02RA2 IL2.3
## RF-M04-03M04HU-M04-01CU-10RN-03RA4 IL4.1
## RF-M04-04M04HU-M04-01CU-10RN-17RA2 IL2.3
## RF-M04-05M04HU-M04-02CU-11RN-01RA2 IL2.3; RA4 IL4.1
## RF-M04-06M04HU-M04-02CU-11RN-10RA4 IL4.1
## RF-M04-07M04HU-M04-02CU-11RN-25RA1 IL1.3; RA4 IL4.1
## RF-M04-08M04HU-M04-03CU-12RN-19RA4 IL4.1
## RF-M04-09M04HU-M04-03CU-12RN-19RA4 IL4.1
## RF-M04-10M04HU-M04-03CU-12RN-12RA3 IL3.1, IL3.3
## RF-M04-11M04HU-M04-03CU-12RN-19RA4 IL4.1
## RF-M04-12M04HU-M04-03CU-12RN-12RA1 IL1.1; RA2 IL2.2
## RF-M04-13M04HU-M04-01CU-10RN-19RA4 IL4.1
## RF-M05-01M05HU-M05-01CU-13RN-04RA3 IL3.1; RA4 IL4.1
## RF-M05-02M05HU-M05-01CU-13RN-04RA2 IL2.3; RA3 IL3.2
## RF-M05-03M05HU-M05-01CU-13RN-20RA1 IL1.1; RA3 IL3.1
## RF-M05-04M05HU-M05-01CU-13RN-20RA3 IL3.3
## RF-M05-05M05HU-M05-02CU-14RN-23RA4 IL4.1

## 127
RFMóduloHUCURNRA e IL
## RF-M05-06M05HU-M05-02CU-14RN-05RA3 IL3.1; RA4 IL4.1
## RF-M05-07M05HU-M05-02CU-14RN-05RA2 IL2.3; RA3 IL3.2
## RF-M05-08M05HU-M05-02CU-14RN-24RA4 IL4.1
## RF-M05-09M05HU-M05-03CU-15RN-13RA4 IL4.1
## RF-M05-10M05HU-M05-03CU-15RN-24RA2 IL2.2
## RF-M05-11M05HU-M05-03CU-15RN-06RA4 IL4.1
## RF-M05-12M05HU-M05-01CU-13RN-04RA1 IL1.3
## RF-M06-01M06HU-M06-01CU-16RN-06RA1 IL1.1; RA4 IL4.1
## RF-M06-02M06HU-M06-01CU-16RN-06RA4 IL4.1
## RF-M06-03M06HU-M06-01CU-16RN-06RA1 IL1.2; RA4 IL4.1
## RF-M06-04M06HU-M06-01CU-16RN-06RA2 IL2.3; RA4 IL4.1
## RF-M06-05M06HU-M06-02CU-17RN-09RA5 IL5.3; RA4 IL4.1
## RF-M06-06M06HU-M06-02CU-17RN-09RA5 IL5.2; RA4 IL4.1
## RF-M06-07M06HU-M06-02CU-17RN-07RA3 IL3.2; RA2 IL2.3
## RF-M06-08M06HU-M06-02CU-17RN-07RA4 IL4.2, IL4.3
## RF-M06-09M06HU-M06-03CU-18RN-08RA4 IL4.1; RA2 IL2.3
## RF-M06-10M06HU-M06-03CU-18RN-08RA4 IL4.3
## RF-M06-11M06HU-M06-03CU-18RN-21RA2 IL2.3; RA3 IL3.2
## RF-M06-12M06HU-M06-03CU-18RN-14RA2 IL2.3; RA3 IL3.2; RA4 IL4.1
## RF-M07-01M07HU-M07-01CU-19RN-10RA4 IL4.1
## RF-M07-02M07HU-M07-01CU-19RN-10RA2 IL2.2
## RF-M07-03M07HU-M07-02CU-20RN-10RA4 IL4.1
## RF-M07-04M07HU-M07-02CU-20RN-10RA1 IL1.2; RA2 IL2.3
## RF-M07-05M07HU-M07-03CU-21RN-10RA4 IL4.1
## RF-M07-06M07HU-M07-03CU-21RN-26RA2 IL2.3

## 128
RFMóduloHUCURNRA e IL
## RF-M07-07M07HU-M07-03CU-21RN-26RA4 IL4.2, IL4.3
## RF-M08-01M08HU-M08-01CU-22RN-27RA1 IL1.1; RA4 IL4.1
## RF-M08-02M08HU-M08-01CU-22RN-27RA4 IL4.1
## RF-M08-03M08HU-M08-02CU-23RN-27RA4 IL4.1
## RF-M08-04M08HU-M08-02CU-23RN-27RA1 IL1.2; RA4 IL4.1
## RF-M08-05M08HU-M08-02CU-23RN-23RA4 IL4.1
## RF-M08-06M08HU-M08-03CU-24RN-27RA2 IL2.2
## RF-M08-07M08HU-M08-03CU-24RN-27RA4 IL4.1
## RF-M09-01M09HU-M09-01CU-25RN-09RA5 IL5.1; RA3 IL3.1
## RF-M09-02M09HU-M09-01CU-25RN-09RA5 IL5.2
## RF-M09-03M09HU-M09-01CU-25RN-09RA5 IL5.2
## RF-M09-04M09HU-M09-02CU-26RN-22RA5 IL5.3; RA4 IL4.1
## RF-M09-05M09HU-M09-02CU-26RN-22RA3 IL3.1, IL3.3; RA5 IL5.1
## RF-M09-06M09HU-M09-02CU-26RN-22RA5 IL5.3; RA1 IL1.1
## RF-M09-07M09HU-M09-03CU-27RN-22RA5 IL5.3; RA2 IL2.3
## RF-M09-08M09HU-M09-03CU-27RN-09RA4 IL4.1; RA5 IL5.3
## RF-M09-09M09HU-M09-03CU-27RN-22RA5 IL5.3; RA1 IL1.3
## RF-M10-01M10HU-M10-01CU-28RN-07RA4 IL4.2, IL4.3
## RF-M10-02M10HU-M10-01CU-28RN-24RA4 IL4.2, IL4.3
## RF-M10-03M10HU-M10-01CU-28RN-10RA4 IL4.2, IL4.3
## RF-M10-04M10HU-M10-01CU-28RN-01RA4 IL4.2, IL4.3
## RF-M10-05M10HU-M10-02CU-29RN-24RA4 IL4.2, IL4.3
## RF-M10-06M10HU-M10-02CU-29RN-12RA4 IL4.2, IL4.3
## RF-M10-07M10HU-M10-02CU-29RN-08RA4 IL4.2, IL4.3
## RF-M10-08M10HU-M10-02CU-29RN-26RA4 IL4.2, IL4.3

## 129
RFMóduloHUCURNRA e IL
## RF-M10-09M10HU-M10-03CU-30RN-28RA1 IL1.3; RA4 IL4.3
## RF-M10-10M10HU-M10-03CU-30RN-28RA3 IL3.1, IL3.3; RA4 IL4.3
## RF-M10-11M10HU-M10-03CU-30RN-28RA4 IL4.2, IL4.3
## RF-M10-12M10HU-M10-03CU-30RN-28RA3 IL3.3; RA4 IL4.3
## RF-M10-13M10HU-M10-03CU-30RN-28RA4 IL4.2, IL4.3; RA5 IL5.3
## RF-M11-01M11HU-M11-01CU-31RN-01RA1 IL1.1; RA2 IL2.2
## RF-M11-02M11HU-M11-01CU-31RN-24RA2 IL2.2; RA1 IL1.1
## RF-M11-03M11HU-M11-02CU-32RN-05RA2 IL2.2
## RF-M11-04M11HU-M11-02CU-32RN-12RA3 IL3.1; RA1 IL1.2
## RF-M11-05M11HU-M11-03CU-33RN-22RA5 IL5.3; RA1 IL1.1
## RF-M11-06M11HU-M11-03CU-33RN-24RA1 IL1.2, IL1.3
## RF-M11-07M11HU-M11-04CU-38RN-35, RN-37RA1 IL1.1, IL1.2
## RF-M11-08M11HU-M11-04CU-38RN-36RA1 IL1.2
## RF-M11-09M11HU-M11-05CU-39RN-38RA1 IL1.1, IL1.2
## RF-M11-10M11HU-M11-05CU-39RN-37RA1 IL1.1, IL1.3
## RF-M12-01M12HU-M12-01CU-34RN-05RA2 IL2.2, IL2.3
## RF-M12-02M12HU-M12-01CU-34RN-28RA1 IL1.1; RA4 IL4.1
## RF-M12-03M12HU-M12-01CU-34RN-25RA1 IL1.3
## RF-M12-04M12HU-M12-02CU-35RN-14RA2 IL2.2, IL2.3
## RF-M12-05M12HU-M12-02CU-35RN-14RA1 IL1.1; RA2 IL2.2
## RF-M12-06M12HU-M12-03CU-36RN-29RA2 IL2.1; RA3 IL3.2
## RF-M12-07M12HU-M12-03CU-36RN-29RA2 IL2.3; RA3 IL3.2
## RF-M12-08M12HU-M12-03CU-36RN-30RA3 IL3.1, IL3.2, IL3.3

## 130
## Apéndice D
Resumen de Priorización MoSCoW
La prioridad expresa el compromiso de la versión demostrable y conserva la precisión de
los requisitos. La Tabla D1 resume su distribución; la Tabla D2 compara módulos y mínimos
exigidos. Las reglas financieras y los dos temas son Must por su efecto sobre integridad y
lectura. Should y Could ordenan complementos dentro de la capacidad estimada. Cada cambio
conservará identificador, motivo y evidencia del estado final. La Tabla D3 verifica cantidades
documentales, sin convertir esa cobertura en prueba de software ni en aceptación académica
anticipada.
## Tabla D1
Distribución de Prioridades
PrioridadCantidadPorcentajeTratamiento
Must11591,27 %Condición obligatoria de aceptación.
Should97,14 %Complemento priorizado tras Must; documentar estado.
Could21,59 %Foto y logotipo; no sustituyen invariantes.
Won’t0 RFFuera de RFExclusiones de la Tabla 5.

## 131
## Tabla D2
Requisitos por Módulo y Prioridad
MóduloMustShouldCouldTotalMínimo
## M011410158
## M021001118
## M0372098
## M0412101312
## M0511101212
## M0612001212
## M0770077
## M0852077
## M0990099
## M1012101312
## M11910106
## M1270188

## 132
## Tabla D3
Cobertura de Cantidades Exigidas
ElementoCantidadMínimo
Reglas de negocio3825
Requisitos funcionales126109
Historias de usuario3935
Casos de catálogo3925
Casos detallados1212
Requisitos no funcionales6040
## Reportes1010
## Riesgos1612

## 133
## Apéndice E
Fuentes y Trazabilidad Documental
La Tabla E1 registra fuentes consultadas y las afirmaciones que respaldan. Los estudios
aportan antecedentes delimitados por país y método; los fabricantes documentan componentes y
condiciones. Los textos legales conservan emisor, publicación y artículos usados, con sus límites
de acceso. Se conservan las fechas originales de consulta externa del 22 de octubre de 2026; las
referencias cruzadas del proyecto se cotejaron el 23 de octubre. D52 conserva la fecha de
consulta propia de cada documento; no se unifican las fechas de las fuentes compartidas.
Ninguna consulta acredita ejecución de Latch. Las decisiones conservan su carácter de diseño
aunque un manual explique el mecanismo elegido. La matriz permite contrastar citas y
referencias sin presentar publicidad, hipótesis o metas de aceptación como resultados empíricos
del proyecto.

## 134
## Tabla E1
Fuentes y Trazabilidad Documental
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
## American Psychological
## Association (2025). Guía
estudiantil: márgenes 2,54 cm,
doble espacio, sangría 1,27 cm,
paginación desde portada y
jerarquía de encabezados;
prevalecen instrucciones
institucionales.
Student paper setup guide.
guía institucional. Autor:
## American Psychological
## Association.
## Publicación:
## 2025.
## Consulta: 22
de octubre de
## 2026.
Formato académico. PDF
oficial; actualizado 8 de
octubre de 2025. Fuente
primaria; alcance
limitado a lo
documentado.
https://apastyle.apa.org/
instructional-aids/
student-paper-setup-
guide.pdf
## Apache Software Foundation (s.
f.). Maven 3.9.16 se publicó el 13
de mayo de 2026 y figura
mantenida; Java 8 mínimo
permite utilizar JDK 21, aunque
no sea la GA numéricamente
mayor.
Maven releases history.
documentación técnica.
## Autor: Apache Software
## Foundation.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
maven.apache.org/docs/
history.html
AppFolio (s. f.). Ayuda
documenta saldos, cargos, pagos,
historial, contratos y
mantenimiento; permite interfaz
en español.
FolioSpace overview.
documentación de producto.
Autor: AppFolio.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
## Alternativas. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
www.appfolio.com/
help/online-portal

## 135
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Asamblea Nacional de la
República de Nicaragua (2012).
Finalidad, minimización,
consentimiento, seguridad,
derechos, transferencias e
inscripción; acceso diez días
hábiles y modificación cinco.
Ley N.º 787, Ley de
Protección de Datos
Personales. norma jurídica.
## Autor: Asamblea Nacional
de la República de
## Nicaragua.
## Publicación:
## 2012.
## Consulta: 22
de octubre de
## 2026.
Marco jurídico. Original
de La Gaceta leído en
reproducción documental
y cotejo OEA; host
Asamblea inaccesible.
Texto normativo original
reproducido; no se
verificó vigencia
operativa de
procedimientos de
autoridad en 2026.
https://
sajurin.enriquebolanos.or
g/docs/G-2012-03-29.pdf
AYR Tech (Pty) Ltd. (s. f.-a).
Bearer evita colocar clave en
segmento del URL;
documentación advierte
exposición de claves mediante
registros.
API authentication notes.
documentación técnica.
Autor: AYR Tech (Pty) Ltd..
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/docs/
authentication
AYR Tech (Pty) Ltd. (s. f.-b). La
página raíz del proveedor
presenta el plan Free autenticado:
1.500 solicitudes al mes y
actualización diaria.
ExchangeRate-API.
documentación de planes.
Autor: AYR Tech (Pty) Ltd..
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/
AYR Tech (Pty) Ltd. (s. f.-c).
Proveedor declara tasas medias
indicativas compuestas; no
identificarlas como tasa oficial
## BCN.
Our exchange rate data.
documentación técnica.
Autor: AYR Tech (Pty) Ltd..
## Publicación:
s. f.-c.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/product/our-
exchange-rate-data

## 136
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
AYR Tech (Pty) Ltd. (s. f.-d).
JSON Standard: result,
base_code, conversion_rates y
tiempos de actualización; base
USD y conversion_rates.NIO;
errores documentados.
Standard requests.
documentación técnica.
Autor: AYR Tech (Pty) Ltd..
## Publicación:
s. f.-d.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/docs/standard-
requests
AYR Tech (Pty) Ltd. (s. f.-e).
Lista incluye USD y NIO.
Supported currencies.
documentación técnica.
Autor: AYR Tech (Pty) Ltd..
## Publicación:
s. f.-e.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/docs/supported-
currencies
AYR Tech (Pty) Ltd. (s. f.-f).
Uso informativo/ilustrativo,
caché de uso final permitida,
redistribución prohibida,
exactitud y continuidad no
garantizadas.
Terms of service. términos
del proveedor. Autor: AYR
Tech (Pty) Ltd..
## Publicación:
s. f.-f.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://
www.exchangerate-
api.com/terms
Balzarini y Boyd (2021). 71
entrevistas intencionales de
Filadelfia (2017–2019) muestran
gestión financiera, mantenimiento
y acuerdos de pago; no mide
Managua ni efectividad del
software.
Working with them: Small-
scale landlord strategies for
avoiding evictions. artículo
científico. Autor: Balzarini,
## J., & Boyd, M. L..
## Publicación:
## 2021.
## Consulta: 22
de octubre de
## 2026.
## Antecedentes. Texto
editorial completo
anticipado 2020 leído en
reproducción NLIHC;
referencia del número
final 2021. Artículo
científico original;
muestra cualitativa no
representativa.
https://doi.org/
## 10.1080/10511482.2020.
## 1800779

## 137
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Banco Central de Nicaragua
(2025). Nota del 20 octubre 2025:
deslizamiento anual 0 % y
36.6243 NIO/USD en 2026; no es
cotización comercial en vivo.
BCN informa que la tasa de
deslizamiento del tipo de
cambio del córdoba respecto
al dólar de los Estados
Unidos de América
continuará en cero por ciento
(0 %) anual en el año 2026.
nota institucional. Autor:
Banco Central de Nicaragua.
## Publicación:
## 2025.
## Consulta: 22
de octubre de
## 2026.
Contexto monetario.
Página y PDF oficiales
leídos. Fuente primaria;
alcance limitado a lo
documentado.
https://www.bcn.gob.ni/
divulgacion-prensa/bcn-
informa-que-la-tasa-de-
deslizamiento-del-tipo-
de-cambio-del-cordoba-
respecto
Blanco et al. (2016). Nueve
estudios de país describen oferta
diversa, privada y de pequeña
escala; Nicaragua no figura entre
los nueve casos.
Rental housing wanted:
Options for expanding
housing policy. libro
institucional. Autor: Blanco,
## A. G., Fretes Cibils, V., &
Muñoz, A. F. (Eds.)..
## Publicación:
## 2016.
## Consulta: 22
de octubre de
## 2026.
Antecedentes. PDF
original BID; síntesis y
pasajes pertinentes leídos.
Publicación institucional
primaria; contextual e
histórica.
https://
publications.iadb.org/
publications/english/
document/Rental-
Housing-Wanted-
Options-for-Expanding-
Housing-Policy.pdf
Buildium (s. f.). Documenta
creación de pagos, consulta de
transacciones por contrato y
órdenes de trabajo; no prueba
eficacia ni operación en
## Nicaragua.
Open API, powered by
Buildium. documentación de
producto. Autor: Buildium.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
Alternativas. HTML
oficial recuperado
mediante PowerShell tras
límite de tamaño del
lector web. Fuente
primaria; alcance
limitado a lo
documentado.
https://
developer.buildium.com/
Eclipse Adoptium (s. f.-a).
## Respuesta
release_name=jdk-21.0.12.1+1,
actualizada el 21 de agosto de
2026; no inventar un build
## 21.0.13.
Latest assets for Java 21,
HotSpot, Windows x64.
respuesta de API. Autor:
## Eclipse Adoptium.
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Tecnologías. JSON
oficial leído mediante
Invoke-RestMethod.
Fuente primaria; alcance
limitado a lo
documentado.
https://api.adoptium.net/
v3/assets/latest/21/
hotspot?
architecture=x64&image
## _type=jdk&os=windows

## 138
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Eclipse Adoptium (s. f.-b).
Temurin 21.0.12.1+1 figura
disponible; 21.0.13 aparece
próxima en la tabla consultada. El
soporte comunitario no ofrece
## SLA.
Temurin support.
documentación técnica.
## Autor: Eclipse Adoptium.
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://adoptium.net/
support/
Gluon (s. f.). JavaFX 21.0.13 se
publicó el 20 de octubre de 2026;
las notas específicas estaban más
actualizadas que la página general
del producto.
OpenJFX 21 release notes.
documentación técnica.
## Autor: Gluon.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
Interfaz. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://gluonhq.com/
products/javafx/
openjfx-21-release-notes/
Google (s. f.). Dependencia
## Maven
com.google.code.gson:gson:2.13.
2, Java 8 como mínimo y módulo
com.google.gson; proyecto en
mantenimiento y sin soporte
oficial de Google.
Gson. código fuente. Autor:
## Google.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
Tecnologías. README
oficial etiqueta 2.13.2.
Fuente primaria; alcance
limitado a lo
documentado.
https://
raw.githubusercontent.co
m/google/gson/gson-
parent-2.13.2/
README.md
Google (2025). La etiqueta 2.13.2
se publicó el 10 de septiembre de
2025 según la API oficial de
GitHub; no fechar 2026 por
coincidir con la consulta.
Gson 2.13.2. software.
## Autor: Google.
## Publicación:
## 2025.
## Consulta: 22
de octubre de
## 2026.
Tecnologías. Release y
metadatos API oficiales.
Fuente primaria; alcance
limitado a lo
documentado.
https://github.com/
google/gson/releases/
tag/gson-parent-2.13.2
Hibernate (s. f.). La serie admite
Java 21 y Jakarta Persistence 3.2;
7.4.6.Final se publicó el 23 de
agosto de 2026. Es una versión
fijada para el proyecto, no la
última.
7.4 series. documentación
técnica. Autor: Hibernate.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://hibernate.org/
orm/releases/7.4/

## 139
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Jaspersoft (s. f.-a). El POM de la
etiqueta 7.0.8 contiene
hibernate.version=6.3.1.Final; no
certifica la combinación con
## Hibernate 7.4.
JasperReports Library
Artifacts. código fuente.
## Autor: Jaspersoft.
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Tecnologías. POM oficial
fijado por etiqueta 7.0.8.
Fuente primaria; alcance
limitado a lo
documentado.
https://
raw.githubusercontent.co
m/Jaspersoft/
jasperreports/7.0.8/pom-
parent.xml
Jaspersoft (s. f.-b). Fuente de
datos acepta colección de
JavaBeans: alternativa a acoplar
módulo Hibernate del reporte.
JRBeanCollectionDataSourc
e (JasperReports Javadoc
7.0.8 API). documentación
técnica. Autor: Jaspersoft.
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
jasperreports.sourceforge.
net/api/net/sf/
jasperreports/engine/
data/
JRBeanCollectionDataSo
urce.html
Jaspersoft (s. f.-c). JasperReports
7 rompe compatibilidad de
plantillas compiladas; formatos
de versión 6 requieren conversión
y recompilación; funciones
separadas en módulos opcionales.
README. código fuente.
## Autor: Jaspersoft.
## Publicación:
s. f.-c.
## Consulta: 22
de octubre de
## 2026.
Tecnologías. README
oficial etiqueta 7.0.8.
Fuente primaria; alcance
limitado a lo
documentado.
https://
raw.githubusercontent.co
m/Jaspersoft/
jasperreports/7.0.8/
README.md
## Jaspersoft (2026b). Distribución
oficial del 5 de agosto de 2026
con módulos PDF y fuentes;
versión publicada, no la más
reciente.
JasperReports Library 7.0.8.
software. Autor: Jaspersoft.
## Publicación:
## 2026b.
## Consulta: 22
de octubre de
## 2026.
## Tecnologías. Distribución
y registro de cambios
oficiales. Fuente
primaria; alcance
limitado a lo
documentado.
https://sourceforge.net/
projects/jasperreports/
files/jasperreports/
JasperReports%207.0.8/

## 140
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Malewicz (s. f.). Descripción
pública de fondos orgánicos con
gradientes desenfocados;
referencia visual, no estándar ni
biblioteca JavaFX.
Aurora UI: new visual trend
for 2021. referencia de
diseño. Autor: Malewicz,
## M..
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
Aurora UI. Solo título y
descripción públicos;
contenido principal
restringido a miembros.
Fuente original de
concepto visual; acceso
parcial, no evidencia
empírica.
https://hype4.academy/
articles/design/aurora-ui-
new-visual-trend-
for-2021
OpenJFX (s. f.-a). Desenfoque
gaussiano con radio de 0 a 63,
predeterminado 10; no usar 80 o
100 como radio válido.
GaussianBlur (JavaFX 21).
documentación técnica.
Autor: OpenJFX.
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Aurora UI. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://openjfx.io/
javadoc/21/
javafx.graphics/javafx/
scene/effect/
GaussianBlur.html
OpenJFX (s. f.-b). CSS de
JavaFX usa propiedades -fx- y
colores buscados; no asumir
variables CSS ni backdrop-filter
de navegador.
JavaFX CSS reference guide.
documentación técnica.
Autor: OpenJFX.
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
Aurora UI. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://openjfx.io/
javadoc/21/
javafx.graphics/javafx/
scene/doc-files/
cssref.html
OpenJFX (s. f.-c). Gradiente
circular configurable con al
menos dos colores/paradas.
RadialGradient (JavaFX 21).
documentación técnica.
Autor: OpenJFX.
## Publicación:
s. f.-c.
## Consulta: 22
de octubre de
## 2026.
Aurora UI. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://openjfx.io/
javadoc/21/
javafx.graphics/javafx/
scene/paint/
RadialGradient.html
OpenJFX (s. f.-d). Gestiona Task
y permite reinicio; comunicación
de estados observables y
cancelación.
Service (JavaFX 21).
documentación técnica.
Autor: OpenJFX.
## Publicación:
s. f.-d.
## Consulta: 22
de octubre de
## 2026.
Interfaz. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://openjfx.io/
javadoc/21/
javafx.graphics/javafx/
concurrent/Service.html

## 141
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
OpenJFX (s. f.-e). call() trabaja
en segundo plano; grafo activo no
debe modificarse allí; tarea de un
solo uso.
Task (JavaFX 21).
documentación técnica.
Autor: OpenJFX.
## Publicación:
s. f.-e.
## Consulta: 22
de octubre de
## 2026.
Interfaz. Texto original
abierto. Fuente primaria;
alcance limitado a lo
documentado.
https://openjfx.io/
javadoc/21/
javafx.graphics/javafx/
concurrent/Task.html
Oracle (s. f.-a). HttpClient
reutilizable e inmutable; send
bloquea, sendAsync retorna
CompletableFuture; timeout
conexión y por petición.
HttpClient (Java SE 21 &
JDK 21). documentación
técnica. Autor: Oracle.
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Conversión USD/NIO.
Texto original abierto.
Fuente primaria; alcance
limitado a lo
documentado.
https://docs.oracle.com/
en/java/javase/21/docs/
api/java.net.http/java/
net/http/HttpClient.html
Oracle (s. f.-b). JavaFX 21 admite
JDK 17 y JDK 21.
Supported JDK versions.
documentación técnica.
## Autor: Oracle.
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://docs.oracle.com/
en/java/java-
components/javafx/21/
javafx-users-guide/
supported-jdk-
versions.html
OWASP Foundation (s. f.).
Argon 2 id con mínimo de 19
MiB, dos iteraciones y
paralelismo uno; hash adaptativo
con salt, no cifrado reversible;
medir los parámetros.
Password storage cheat
sheet. guía de seguridad.
Autor: OWASP Foundation.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
Seguridad. OWASP
## Cheat Sheet Series
original. Fuente primaria;
alcance limitado a lo
documentado.
https://
cheatsheetseries.owasp.or
g/cheatsheets/
Password_Storage_Cheat
_Sheet.html
Jaspersoft (2026a). La ficha
declara soporte completo para
Library 7.0.8 y ausencia de
código fuente desde Studio
6.21.3; no denominar Studio
software de código abierto.
Jaspersoft Studio community
edition 7.0.8. software.
## Autor: Jaspersoft.
## Publicación:
## 2026a.
## Consulta: 22
de octubre de
## 2026.
## Tecnologías. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
community.jaspersoft.co
m/download-jaspersoft/
community-edition/
jaspersoft-studio_7.0.8

## 142
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
PostgreSQL JDBC Driver (s. f.).
pgJDBC 42.7.14 figura como
controlador actual para Java 8 o
posterior; JDK 21 cumple el
umbral.
Download. documentación
técnica. Autor: PostgreSQL
JDBC Driver.
## Publicación:
s. f.. Consulta:
22 de octubre
de 2026.
## Persistencia. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
jdbc.postgresql.org/
download/
Presidencia de la República de
## Nicaragua (2012).
Consentimiento expreso para
datos financieros/patrimoniales,
aviso, seguridad, derechos,
cancelación y bloqueo. Plazo 50
días corresponde a DIPRODAP.
## Decreto N.º 36-2012,
Reglamento de la Ley N.º
787, Ley de Protección de
Datos Personales. norma
jurídica. Autor: Presidencia
de la República de
## Nicaragua.
## Publicación:
## 2012.
## Consulta: 22
de octubre de
## 2026.
Marco jurídico. Original
de La Gaceta leído en
reproducción
documental; host
Asamblea inaccesible.
Texto normativo original
reproducido; no es
dictamen de
cumplimiento.
https://
sajurin.enriquebolanos.or
g/docs/G-2012-10-19.pdf
Reosti et al. (2024). Registros y
encuesta de casi 4.000
arrendadores de Seattle
documentan heterogeneidad; no
probar superioridad universal de
pequeños arrendadores ni
extrapolar a Managua.
“Mom-and-pop” landlords
and regulatory backlash: A
Seattle case study. artículo
científico. Autor: Reosti, A.,
## Hess, C., Allen, C., &
## Crowder, K..
## Publicación:
## 2024.
## Consulta: 22
de octubre de
## 2026.
## Antecedentes. Texto
editorial abierto. Artículo
científico original;
contexto estadounidense
delimitado.
https://doi.org/
## 10.1177/2378023124129
## 6169
The PostgreSQL Global
Development Group (s. f.-b).
SELECT FOR UPDATE bloquea
filas recuperadas hasta terminar
transacción; no protege filas
inexistentes; orden consistente
reduce interbloqueos.
13.3. Explicit locking.
documentación técnica.
Autor: The PostgreSQL
## Global Development Group.
## Publicación:
s. f.-b.
## Consulta: 22
de octubre de
## 2026.
Persistencia. PostgreSQL
18 oficial. Fuente
primaria; alcance
limitado a lo
documentado.
https://
www.postgresql.org/
docs/18/explicit-
locking.html

## 143
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
The PostgreSQL Global
Development Group (s. f.-a).
nextval atómico entrega valores
distintos concurrentes; rollback
no recupera números; no
garantiza folios sin huecos.
9.17. Sequence manipulation
functions. documentación
técnica. Autor: The
PostgreSQL Global
## Development Group.
## Publicación:
s. f.-a.
## Consulta: 22
de octubre de
## 2026.
Persistencia. PostgreSQL
18 oficial. Fuente
primaria; alcance
limitado a lo
documentado.
https://
www.postgresql.org/
docs/18/functions-
sequence.html
The PostgreSQL Global
Development Group (s. f.-c).
PostgreSQL 18.6 figura
soportada; la mayor 18 se publicó
el 25 de septiembre de 2025 y
tiene soporte hasta el 14 de
noviembre de 2030; no adoptar
19 RC 1 como estable.
Versioning policy.
documentación técnica.
Autor: The PostgreSQL
## Global Development Group.
## Publicación:
s. f.-c.
## Consulta: 22
de octubre de
## 2026.
## Persistencia. Texto
original abierto. Fuente
primaria; alcance
limitado a lo
documentado.
https://
www.postgresql.org/
support/versioning/
## World Wide Web Consortium
(2024). Contraste de texto 4.5:1,
o 3:1 para texto grande; no usar
solo color; componentes 3:1 y
foco visible. Los criterios 2.4.13
y 2.3.3 son AAA.
Web content accessibility
guidelines (WCAG) 2.2.
recomendación técnica.
## Autor: World Wide Web
## Consortium.
## Publicación:
## 2024.
## Consulta: 22
de octubre de
## 2026.
## Accesibilidad.
Recomendación original
fechada 12 diciembre
- Fuente primaria;
alcance limitado a lo
documentado.
https://www.w3.org/TR/
## 2024/REC-WCAG22-
## 20241212/
## World Wide Web Consortium
(2025). Orientación informativa
para aplicar WCAG a
software/documentos no web; no
impone norma autónoma.
Guidance on applying
WCAG 2 to non-web
information and
communications
technologies (WCAG2ICT).
nota técnica. Autor: World
## Wide Web Consortium.
## Publicación:
## 2025.
## Consulta: 22
de octubre de
## 2026.
Accesibilidad. Nota de
grupo original fechada 11
diciembre 2025. Fuente
primaria; alcance
limitado a lo
documentado.
https://www.w3.org/TR/
## 2025/NOTE-
wcag2ict-22-20251211/

## 144
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Cuadra et al. (2026a). Pantallas,
tokens, permisos y catálogo de
mensajes; D14, D15, D19 y D27.
Brief de diseño de interfaz y
experiencia de usuario para
## Latch. Documento
académico del proyecto.
## Autoría: Steven Cuadra,
## Alfredo Montalvan, Caleb
Tardencilla y Aris Gavarrete.
## Publicación:
## 2026a.
Consulta de
coherencia:
23 de octubre
de 2026.
Fuente primaria interna.
Se contrastan decisiones;
no certifica ejecución de
## Latch.
Latch_Brief_UIUX_APA
7_v3.docx (archivo local;
sin URL pública).
Cuadra et al. (2026b). Recorridos,
activación y estados;
CU-10/ESC-02 y reglas
financieras.
Documento de flujo de la
aplicación de Latch.
Documento académico del
proyecto. Autoría: Steven
## Cuadra, Alfredo Montalvan,
Caleb Tardencilla y Aris
## Gavarrete.
## Publicación:
## 2026b.
Consulta de
coherencia:
23 de octubre
de 2026.
Fuente primaria interna.
Se contrastan decisiones;
no certifica ejecución de
## Latch.
Latch_App_Flow_APA7
_v3.docx (archivo local;
sin URL pública).
Cuadra et al. (2026d).
Concurrencia, transacciones,
secretos y cifrado de respaldos.
Documento de requisitos
técnicos de Latch.
Documento académico del
proyecto. Autoría: Steven
## Cuadra, Alfredo Montalvan,
Caleb Tardencilla y Aris
## Gavarrete.
## Publicación:
## 2026d.
Consulta de
coherencia:
23 de octubre
de 2026.
Fuente primaria interna.
Se contrastan decisiones;
no certifica ejecución de
## Latch.
Latch_TRD_APA7_v3.d
ocx (archivo local; sin
URL pública).
Cuadra et al. (2026e). Datos,
parámetros append-only,
auditoría y autorización única de
tasa.
Esquema de base de datos y
servicios de Latch.
Documento académico del
proyecto. Autoría: Steven
## Cuadra, Alfredo Montalvan,
Caleb Tardencilla y Aris
## Gavarrete.
## Publicación:
## 2026e.
Consulta de
coherencia:
23 de octubre
de 2026.
Fuente primaria interna.
Se contrastan decisiones;
no certifica ejecución de
## Latch.
Latch_Backend_Schema
_APA7_v3.docx (archivo
local; sin URL pública).

## 145
Afirmación y CitaObra y Tipo de FuenteFechasSección y ConfiabilidadURL Directa o DOI
Cuadra et al. (2026f). Calendario,
HIT-01 a HIT-10, RACI, revisión
cruzada, TAR-001 a TAR-096 y
CP-001 a CP-100.
Plan de implementación de
## Latch. Documento
académico del proyecto.
## Autoría: Steven Cuadra,
## Alfredo Montalvan, Caleb
Tardencilla y Aris Gavarrete.
## Publicación:
## 2026f.
Consulta de
coherencia:
23 de octubre
de 2026.
Fuente primaria interna.
Se contrastan decisiones;
no certifica ejecución de
## Latch.
Latch_Plan_Implementac
ion_APA7_v3.docx
(archivo local; sin URL
pública).