

## 1
Documento de Requisitos Técnicos de Latch
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
Latch organizará contratos, cargos, pagos, garantías y reportes mediante una aplicación de
escritorio con Java 21, JavaFX y PostgreSQL. El diseño técnico conserva los requisitos del
producto suministrado y adopta una arquitectura por capas con controladores MVC y servicios
transaccionales. Hibernate administrará entidades de negocio, mientras JDBC resolverá
seguridad, auditoría y consultas agregadas; las escrituras financieras compartirán una conexión y
una confirmación. El consecutivo del recibo combinará exclusión local, bloqueo del contador
persistente e idempotencia por solicitud. Las tareas H1 a H6 precisan ejecución, sincronización,
cancelación y cierre, con criterios para mantener disponible el hilo gráfico. La consulta de
ExchangeRate-API aportará una referencia informativa USD/NIO, separada de la autorización
por fecha y de la referencia oficial del Banco Central de Nicaragua. JasperReports generará diez
reportes a partir de objetos de transferencia: diez salidas PDF y nueve XLSX; el recibo será solo
PDF. La presentación aplicará Aurora UI mediante dos temas completos y preferencias locales.
El documento establece veintidós decisiones, sesenta vínculos de requisitos no funcionales y
dieciséis riesgos, junto con escenarios técnicos y presupuestos de aceptación. Las fuentes se
consultaron el 23 de octubre de 2026. Los volúmenes y la capacidad son supuestos de
planificación, y las pruebas descritas expresan resultados esperados sin acreditar una
implementación ejecutada.
Palabras clave: JavaFX, PostgreSQL, arquitectura, concurrencia, JasperReports, Aurora
UI, trazabilidad

## 3
Documento de Requisitos Técnicos de Latch
## Introducción
Latch administrará alquileres mediante una aplicación de escritorio organizada por
propiedades, unidades y contratos. El diseño usa Java 21, JavaFX y PostgreSQL para mantener
cargos, pagos parciales, garantías y recibos relacionados. Este documento fija decisiones técnicas
y criterios de aceptación para la versión demostrable del equipo académico. La audiencia
comprende a Steven Cuadra, Alfredo Montalvan, Caleb Tardencilla, Aris Gavarrete y al docente
José A. Durán García. Los resultados RA1 a RA5 se relacionan con la trazabilidad académica del
Plan de implementación (Cuadra et al., 2026f).
El PRD define 126 requisitos funcionales, 60 requisitos no funcionales y 38 reglas de
negocio (Cuadra et al., 2026c). El Brief fija pantallas y tokens visuales (Cuadra et al., 2026a); el
Flujo detalla transiciones y recuperación (Cuadra et al., 2026b). El Esquema especifica DDL y
los doce servicios de aplicación (Cuadra et al., 2026e), y el Plan establece calendario,
responsabilidades y CP-001 a CP-100 (Cuadra et al., 2026f). Este TRD coordina esas fuentes
bajo las decisiones canónicas aplicables. Los identificadores ADR registran arquitectura, RNF
conservan los códigos del PRD y RST identifican riesgos técnicos.
Balzarini y Boyd estudian estrategias de pequeños arrendadores mediante entrevistas en
Filadelfia y describen arreglos relacionados con atrasos de renta (Balzarini y Boyd, 2021). Ese
antecedente permite considerar pagos parciales como escenario de diseño, con un alcance
geográfico que debe conservarse al interpretar la evidencia. La pertinencia de Latch para
Managua sigue siendo una hipótesis del proyecto académico. El equipo trabajará con personas,
propiedades y movimientos sintéticos identificados en las demostraciones. La investigación

## 4
documental sustituye la participación de propietarios, las entrevistas y la observación de campo,
actividades que no forman parte de esta modalidad académica.
Alcance y Convenciones
La aplicación comprende M01 Seguridad y Usuarios, M02 Propiedades y Unidades, M03
Inquilinos y M04 Contratos. M05 administra cargos, M06 pagos y recibos, M07 garantías y M08
mantenimiento. M09 consulta tasas, M10 produce reportes, M11 presenta indicadores y M12
conserva configuración y auditoría. Los roles operativos son Propietario y Asistente; el inquilino
recibe documentos fuera de la aplicación. El alcance excluye contabilidad integral, facturación
fiscal, pasarelas, portales, aplicaciones móviles, envío automático de mensajes, firma electrónica
y OCR. Las pruebas usan datos sintéticos.
Cada cifra de rendimiento expresa una meta de aceptación y cada volumen corresponde a
un conjunto sintético. Las versiones y condiciones externas se verificaron el 23 de octubre de
2026 mediante documentación original, con alcance registrado en el Apéndice D. La fecha base
de planificación permanece en el 22 de octubre y la portada adopta el 26 de noviembre como
entrega interna. Esa fecha no confirma el calendario institucional; la presentación y evaluación
de referencia abarcan el 26 de noviembre y el 1 de diciembre. Las tablas distinguen hechos
documentados, decisiones de diseño y supuestos de capacidad.
Supuestos y Evidencia
La instalación inicial reúne aplicación y PostgreSQL en Windows 11 x64; la variante
local admite tres sesiones conectadas al mismo servidor. El equipo supone doce horas semanales
por integrante, con ajustes proporcionales para semanas parciales y una reserva explícita de
revisión. La planificación no acredita disponibilidad confirmada ni trabajo ya realizado por
ninguna persona. Tampoco constituye aprobación del docente, aceptación de un cliente o prueba

## 5
de un programa implementado. Los apéndices incluyen configuraciones ilustrativas y un
glosario; su revisión documental no sustituye la compilación, las pruebas de integración ni la
ejecución de SQL en PostgreSQL.
Decisiones de Arquitectura
La arquitectura adopta un monolito de escritorio con capas y casos de uso
transaccionales. El tamaño académico del equipo y el despliegue local favorecen una sola
distribución y una base compartida, sin servidores de aplicación adicionales. Cada ADR
identifica el problema, la alternativa descartada y una consecuencia que deberá revisar el
responsable propuesto. La Tabla 1 constituye el registro de decisiones adoptadas para el diseño y
no expresa aprobación externa. Las decisiones financieras prevalecen sobre simplificaciones de
interfaz que produzcan pagos duplicados, cambios parciales o numeración consumida por una
operación que terminó en reversión.

## 6
## Tabla 1
Registro de Decisiones de Arquitectura
IDContextoDecisiónAlternativa
## Considerada
JustificaciónConsecuenciaEstado
ADR-01Construcción
reproducible del
equipo.
Maven 3.9.16 y
Wrapper fijado.
Gradle y
dependencias
copiadas.
Conserva el PRD y
centraliza versiones y
perfiles.
Una orden verify;
cambios de versión
pasan por revisión.
## Adoptada
para el
diseño
ADR-02Pantallas y
controles RA1.
JavaFX 21.0.13,
FXML y Scene
## Builder 21.
Swing o interfaz
web embebida.
## Mantiene
herramientas
académicas y
composición
declarativa.
FXML sin lógica
financiera; fábrica
común de
controladores.
## Adoptada
para el
diseño
ADR-03Separación entre
eventos y reglas.
MVC con servicios
de aplicación y DTO.
MVVM con
framework
adicional.
Los controladores
coordinan formularios
y servicios sin otra
biblioteca.
Pruebas de reglas sin
iniciar JavaFX.
## Adoptada
para el
diseño
ADR-04Evidenciar SQL
y ORM.
## JDBC
PreparedStatement en
M01, auditoría y
agregados; Hibernate
en negocio.
Solo ORM o
JDBC integral.
Conserva RA2 y RA4
con responsabilidades
explícitas.
JDBC financiero
recibe la conexión
de Hibernate; no
hace commit.
## Adoptada
para el
diseño
ADR-05Conexiones
compartidas por
tareas.
## Un
HikariDataSource,
máximo 6 conexiones
por cliente.
DriverManager
por operación o
pool propio.
Reduce creación
repetida y limita
concurrencia de
acceso.
SessionFactory
recibe el mismo
DataSource; cierres
ordenados.
## Adoptada
para el
diseño
ADR-06Integridad y
soporte del
motor.
PostgreSQL 18.6 y
pgJDBC 42.7.14.
SQLite, MySQL
o versión
preliminar 19.
Preserva PRD y rama
soportada
documentada.
Migraciones y
pruebas usan
PostgreSQL real, sin
equivalencia con H2.
## Adoptada
para el
diseño

## 7
IDContextoDecisiónAlternativa
## Considerada
JustificaciónConsecuenciaEstado
ADR-07Contraseñas
almacenadas.
Argon2id, 19 MiB,
t=2, p=1, sal de 16
bytes.
## SHA-256
directo o bcrypt.
Respeta RNF-005 y
mínimo OWASP.
Hash parametrizado;
medición local antes
de elevar costo.
## Adoptada
para el
diseño
ADR-08Consulta HTTP
y JSON.
HttpClient Java 21
con Gson 2.13.2.
Cliente HTTP
adicional o
Jackson para el
cliente propio.
Evita otra pila HTTP
y conserva decisión
del PRD.
Jackson transitivo de
Jasper no se excluye;
DTO valida el
contrato.
## Adoptada
para el
diseño
ADR-09Plantillas y
motor del
reporte.
## Studio 7.0.8;
compilación con
Library 7.0.8 en
construcción.
## Compilar
plantillas en
cada cobro.
Separa fallos de
plantilla del registro
financiero.
La distribución
incluye .jasper
propios de esa
versión.
## Adoptada
para el
diseño
ADR-10Dependencia
entre Jasper e
## Hibernate.
JRBeanCollectionDat
aSource con
JavaBeans
inmutables.
## Extensión
jasperreports-
hibernate.
El POM de Jasper
referencia Hibernate
6.3.1.Final.
No cargar dos ORM;
ninguna sesión
abierta durante
exportación.
## Adoptada
para el
diseño
ADR-11Trabajo
concurrente y
## UI.
Task, Service y
ejecutores acotados.
Thread directo
por botón o
virtuales sin
límites.
Expone progreso y
cancelación y limita
recursos.
Un trabajo H1, H2,
H3 y H5 por
proceso; rechazo
visible de cola llena.
## Adoptada
para el
diseño
ADR-12Diagnóstico
técnico.
JUL con FileHandler;
SLF4J 2.0.20 hacia
## JUL.
Archivos ad hoc
o múltiples
proveedores.
Unifica salida y
rotación sin mezclar
bitácora financiera.
Cinco archivos de 10
MiB; secretos
redactados.
## Adoptada
para el
diseño
ADR-13Pruebas
verificables.
JUnit 5.12.2, JaCoCo
0.8.13 y PostgreSQL
aislado.
Solo revisión
manual o base
sustituta.
Permite probar reglas
y semántica del motor
elegido.
Meta de 80 % de
líneas en dominio y
aplicación.
## Adoptada
para el
diseño
ADR-14Distribución
académica en
## Windows.
jpackage de JDK 21,
formato app-image
## ZIP.
## MSI
dependiente de
WiX o JAR
suelto.
Incluye runtime y
evita JDK previo en
destino.
Se instala por
extracción; no se
promete firma ni
instalador MSI.
## Adoptada
para el
diseño

## 8
IDContextoDecisiónAlternativa
## Considerada
JustificaciónConsecuenciaEstado
ADR-15Trabajo
simultáneo del
equipo.
main protegida,
ramas cortas y PR
revisada.
Git Flow con
rama develop
permanente.
Reduce líneas de
integración durante
cinco semanas.
Un revisor distinto
del autor y CI antes
de integrar.
## Adoptada
para el
diseño
ADR-16Consecutivo sin
saltos.
ReentrantLock justo
local y
consecutivo_recibo
transaccional del
Esquema mediante
fn_reservar_numero()
## .
## SERIAL,
SEQUENCE o
MAX(numero)
## +1.
La reversión debe
devolver el número
disponible.
Lock hasta
commit/rollback;
Recibo @Immutable
por Hibernate,
auditoría e
integridad sobre la
misma conexión.
## Adoptada
para el
diseño
ADR-17Evolución del
esquema.
SQL versionado y
ejecución controlada
antes de arrancar.
hibernate.hbm2d
dl.auto=update.
Evita cambios
implícitos y pérdida
accidental.
Hibernate valida;
migrador usa
credencial separada.
## Adoptada
para el
diseño
ADR-18Dos temas y
preferencia
personal.
ThemeService y
Preferences por
UUID de usuario.
Tabla visual en
PostgreSQL o
sincronización
con Windows.
Conserva RN-35 a
RN-38 y RNF-059.
Sin privilegios de
administración; fallo
usa Claro o sesión
vigente.
## Adoptada
para el
diseño
ADR-19Identidad
Aurora y
legibilidad.
Tres luces estáticas
nativas con tokens
del Brief, Tablas 3,
11, 13, C2 y C3.
Blur sobre
contenido o
fondo animado
permanente.
Expresa la dirección
del usuario con costo
acotado.
Paneles opacos; sin
efectos y fondo
simplificado con
color uniforme del
tema.
## Adoptada
para el
diseño
ADR-20Origen y uso de
tasa.
ExchangeRate-API
Free informativa;
autorización manual
por fecha.
Cobrar directo
con cualquier
respuesta HTTP.
Términos limitan el
uso a referencia
informativa.
Copia de tasa
autorizada en pago;
bloqueo si falta
autorización.
## Adoptada
para el
diseño
ADR-21Garantías y
cambios
parciales.
UUID de
idempotencia y
transacción por caso
de uso.
Reintento ciego
tras perder la
conexión.
Un resultado incierto
no autoriza un
segundo cobro.
Buscar UUID en
servidor antes de
reenviar la
operación.
## Adoptada
para el
diseño

## 9
IDContextoDecisiónAlternativa
## Considerada
JustificaciónConsecuenciaEstado
ADR-22Adjuntos y
respaldo
coherente.
## Repositorio
controlado y puerta
por fila: FOR
SHARE por escritura,
FOR UPDATE para
respaldo.
Rutas elegidas
por usuario o
copia
concurrente
independiente.
Conserva referencias y
hashes entre base y
adjuntos.
puerta_mantenimien
to compartida en
## LAN; AES-256-
GCM y
mantenimiento para
restaurar con cuenta
administrativa
externa.
## Adoptada
para el
diseño

## 10
Stack Tecnológico y Versiones
La línea de construcción mantiene las versiones compatibles del PRD, aunque algunas
páginas publican revisiones posteriores. Hibernate enumera 7.4.6.Final entre sus lanzamientos y
admite Java 21 con Jakarta Persistence 3.2 (Hibernate, s. f.-a). JasperReports identifica 7.0.8 en
el código oficial y Studio declara soporte de esa biblioteca (Jaspersoft, s. f.-c; Jaspersoft, 2026).
Por tanto, las dos versiones del resumen académico existen y no necesitan una sustitución por
inexistencia. La compatibilidad del conjunto se sustenta en requisitos y metadatos publicados; su
aceptación técnica exige compilar y ejecutar los escenarios descritos en la estrategia de pruebas.
La Tabla 2 presenta componentes Fijados y Licencias.
## Tabla 2
Componentes Fijados y Licencias
ComponenteVersión
## Fijada
Propósito y
## Licencia
## Fuente Oficial Consultada
Temurin JDK21.0.12.1+1,
Windows x64
Compilar y
distribuir Java 21;
GPLv2 con
## Classpath
## Exception.
Tabla de soporte y hoja de licencias
de la distribución (Eclipse
Adoptium, s. f.).
OpenJFX21.0.13Controles y FXML;
GPLv2 con
## Classpath
## Exception.
Roadmap de Gluon; JDK mínimo 17
(Gluon, s. f.-a).
Scene Builder21.0.0Edición de FXML;
licencia BSD de
## Gluon.
Distribución oficial y código de
Gluon (Gluon, s. f.-c; Gluon, s. f.-b).
PostgreSQL18.6Base transaccional;
PostgreSQL
## License.
Política de versiones y licencia del
proyecto (The PostgreSQL Global
Development Group, s. f.-f; The
PostgreSQL Global Development
Group, s. f.-e).
pgJDBC42.7.14Controlador JDBC
4.2; BSD de dos
cláusulas.
Descarga y licencia del controlador
(PostgreSQL JDBC Driver, s. f.-a;
PostgreSQL JDBC Driver, s. f.-b).

## 11
ComponenteVersión
## Fijada
Propósito y
## Licencia
## Fuente Oficial Consultada
Hibernate ORM7.4.6.FinalEntidades y
transacciones;
## Apache 2.0.
Serie 7.4 y sus coordenadas
(Hibernate, s. f.-a).
## Jakarta
## Persistence
3.2.0Anotaciones y API;
EPL 2.0 o GPLv2
con Classpath
## Exception.
Especificación y publicación oficial
(Eclipse Foundation, 2024).
HikariCP6.3.0Pool compartido;
## Apache 2.0.
POM y README etiquetados
(Wooldridge, s. f.-a; Wooldridge, s.
f.-b).
Gson2.13.2Contrato JSON
propio; Apache 2.0.
Lanzamiento del proyecto de Google
(Google, 2025).
JasperReports
## Library
7.0.8Motor, PDF y
## XLSX; LGPL 3.0.
POM y licencia de la biblioteca
(Jaspersoft, s. f.-c; Free Software
## Foundation, 2007).
Jaspersoft Studio7.0.8Diseñar .jrxml;
licencia de tipo BSD
del distribuidor.
Página de Community Edition
(Jaspersoft, 2026).
## Bouncy Castlebcprov-
jdk18on 1.86
Argon2id en Java;
## Bouncy Castle
## License.
Distribución para Java 8 y
posteriores (Legion of the Bouncy
Castle, s. f.).
SLF4J y
proveedor JUL
2.0.20Unificar logs; MIT.Manual y licencia oficiales (QOS.ch,
s. f.-b; QOS.ch, s. f.-a).
Log4j hacia JULlog4j-to-jul
## 2.25.4
Enviar API Log4j
transitiva al destino
JUL; Apache 2.0.
Manual y POM oficial etiquetado
(Apache Software Foundation, s. f.-
c; Apache Software Foundation, s.
f.-b).
JUnit Jupiter5.12.2Pruebas; EPL 2.0.Guía versionada del equipo JUnit
(Bechtold et al., s. f.).
JaCoCo0.8.13Cobertura; EPL 2.0.Historial de lanzamientos (JaCoCo,
s. f.).
Apache Maven3.9.16Construcción;
## Apache 2.0.
Versión estable anterior, conservada
por PRD (Apache Software
Foundation, s. f.-a).
## Git2.51.0.window
s.1
Control de
versiones; GPLv2.
Publicación oficial de Git for
Windows (Git for Windows, s. f.).
IntelliJ IDEA
## Community
2025.2IDE del equipo;
Apache 2.0 para
edición Community.
Notas de versión y repositorio
JetBrains (JetBrains, 2025;
JetBrains, s. f.).
HttpClient, JUL
y jpackage
Integrados en
## JDK 21
HTTP, diagnóstico y
paquete; licencia del
## JDK.
Documentación de Java SE 21
(Oracle, s. f.-b; Oracle, s. f.-c).

## 12
Compatibilidad del Conjunto
JavaFX 21 requiere JDK 17 o posterior y la distribución seleccionada conserva la misma
familia 21 del entorno de construcción (Gluon, s. f.-a). pgJDBC publica 42.7.14 para Java 8 o
posterior, condición que satisface Java 21 (PostgreSQL JDBC Driver, s. f.-a). El motor
PostgreSQL 18.6 pertenece a una rama soportada hasta noviembre de 2030 (The PostgreSQL
Global Development Group, s. f.-f). Hibernate seleccionará PostgreSQLDialect a partir de los
metadatos JDBC y usará Jakarta Persistence 3.2; no se incluirán paquetes javax.persistence. La
conexión de red aplicará sslmode=verify-full con certificado confiable y nombre correcto,
conforme a la documentación del controlador (PostgreSQL JDBC Driver, s. f.-c).
El POM de JasperReports 7.0.8 declara objetivo de bytecode Java 8 y una versión
6.3.1.Final para su extensión Hibernate (Jaspersoft, s. f.-c). Latch conserva Hibernate 7.4.6.Final
y excluye esa extensión, con datos entregados como JavaBeans separados de entidades
persistentes. La construcción fijará el motor y sus extensiones PDF, fuentes y XLSX en 7.0.8 y
conservará los módulos transitivos necesarios. La aceptación exige compilar diez plantillas y
exportarlas en JDK 21, sin errores de enlace ni de clases. Este criterio resuelve la frontera de
integración sin presentar metadatos de compatibilidad como una prueba ya ejecutada.
Reproducibilidad y Dependencias Transitivas
Maven conservará las versiones explícitas del Apéndice A, además del árbol resuelto y
las licencias de la distribución. El equipo usará release=21, UTF-8 y el mismo Wrapper para
verificar la revisión etiquetada en dos equipos limpios. JasperReports podrá incorporar Jackson
como dependencia interna aunque el cliente HTTP propio use Gson; ambos cumplen funciones
diferentes y no requieren eliminarse entre sí. La integración rechazará dependencias
SNAPSHOT, versiones dinámicas y dos artefactos hibernate-core de familias distintas. El

## 13
inventario técnico debe incluir coordenadas, versión y licencia de cada archivo realmente
distribuido, conforme a RNF-046.
Arquitectura de Software
## Vista Lógica
La presentación contiene FXML, CSS y controladores que dependen de casos de uso, sin
construir consultas SQL. Los servicios aplican autorización, validación y límites transaccionales;
las interfaces de repositorio pertenecen a la aplicación y reciben tipos del dominio.
Infraestructura implementa esas interfaces mediante Hibernate, JDBC, HTTP y archivos,
mientras el dominio conserva importes, reglas y estados. El punto de composición crea las
implementaciones y las inyecta mediante una fábrica de controladores. La Figura 1 muestra las
dependencias admitidas; una prueba de paquetes rechazará ciclos y referencias de dominio a
JavaFX o infraestructura. Como excepción documentada, dominio podrá importar
jakarta.persistence.*, org.hibernate.annotations.* y Hibernate.getClass para los mapeos del
Esquema; la prueba de paquetes permitirá únicamente esa dependencia de persistencia y
mantendrá el rechazo a JavaFX, infraestructura y ciclos.

## 14
## Figura 1
Capas y Reglas de Dependencia
Vista de Componentes
Los componentes agrupan responsabilidades de los doce módulos sin introducir un
proceso por módulo. Seguridad comparte una sesión de aplicación inmutable con todos los
servicios, y Finanzas coordina contratos, cargos, pagos y garantías. Tipo de Cambio publica
referencias informativas y autorizaciones; Reportes recibe proyecciones que ya tienen filtros y
moneda definida. Auditoría participa en los commits financieros y Operación administra
archivos, respaldo y cierre. La Figura 2 relaciona esos componentes con sus adaptadores; cada
interfaz pública declara precondiciones, errores esperables y resultado para evitar que un
controlador encadene escrituras que deberían ser atómicas.

## 15
## Figura 2
Componentes y Puertos de la Aplicación
Vista de Despliegue
La instalación individual ejecuta Latch y PostgreSQL en el mismo equipo, con acceso a
la base por loopback y adjuntos bajo una carpeta controlada. La variante LAN instala el paquete
en tres clientes que comparten servidor PostgreSQL y un repositorio de archivos accesible por
SMB con permisos restringidos. Cada cliente ejecuta sus servicios y comparte los bloqueos
persistentes para operaciones financieras; una instancia designada consulta el proveedor y
registra la caché común. La Figura 3 muestra las dos topologías y el límite de Internet. El puerto
5432 solo se habilitará para direcciones permitidas de la red local.

## 16
## Figura 3
Despliegue Individual y Variante en Red Local
Vista de Procesos e Hilos
El hilo JavaFX procesa eventos y dibuja controles; los ejecutores realizan acceso a datos,
cálculo masivo y exportación. H1 y H5 son trabajos únicos por solicitud, mientras H2 y H3 se
programan con demora posterior a la finalización previa. H4 identifica una sección crítica de la
tarea de cobro y H6 coordina la detención, por lo que los seis códigos no implican seis hilos
permanentes. La Figura 4 distingue temporizadores, trabajos y UI. Los DTO cruzan las fronteras
de ejecución como valores inmutables, sin transferir una Session ni una Connection entre hilos.

## 17
## Figura 4
Procesos y Ejecutores de H1 a H6
Vista de Persistencia Híbrida
Una operación financiera comienza en un servicio que abre Session y transacción,
verifica permisos y bloquea los registros necesarios. Hibernate escribe las entidades de negocio y
persiste Recibo, declarado @Immutable; Session.doWork entrega la misma conexión a JDBC
para fn_reservar_numero(), auditoría y sp_verificar_integridad. El orden es flush del pago y
aplicaciones, reserva de número, persistencia y flush de Recibo, auditoría JDBC, CALL
sp_verificar_integridad y commit. Solo el servicio coordinador confirma o revierte. La Figura 5
representa la transacción compartida. Una prueba inducirá un fallo tras auditoría para verificar la
reversión conjunta de pago, aplicaciones, recibo, consecutivo y evento.

## 18
## Figura 5
Una Transacción para ORM y JDBC Financiero
Patrones de Diseño Aplicados
Latch aplica Repository para aislar consultas, Strategy para distribuir pagos y Adapter
para integrar proveedor de tasas y Preferences. El servicio de aplicación cumple la unidad de
trabajo transaccional, y una fábrica central crea controladores con dependencias explícitas.
ThemeService comunica cambios observables de presentación sin modificar las entidades del
negocio. La Figura 6 concreta esa separación para los dos temas y sus ventanas asociadas. Cada
patrón responde a una frontera que necesita sustitución o prueba; el equipo evitará fábricas y
jerarquías adicionales cuando una clase final con constructor explícito resuelva la misma
responsabilidad.

## 19
## Figura 6
Tema Común y Preferencias Locales
Estructura del Proyecto
El proyecto usará ni.edu.uam.latch como paquete raíz y ni.edu.uam como groupId. Sus
capas serán arranque, presentacion, aplicacion, dominio e infraestructura. Los nombres del
negocio se escribirán en español sin tildes; clases en UpperCamelCase, métodos y campos en
lowerCamelCase y constantes en UPPER_SNAKE_CASE. Los roles conservarán los sufijos
Controller, Service, Repository, Dto, Command, Task, Exception y Converter. ThemeService,
AuroraBackground y UiMetrics conservarán sus nombres. Habrá un Controller y un FXML por
cada uno de los 40 PAN, derivados del nombre español del catálogo del Brief, Tabla 20, con
UpperCamelCase para la clase y kebab-case para FXML. El inventario de referencia es la Tabla
C1 del Brief, aplicando D07 a sus nombres anteriores. El Apéndice B desarrolla paquetes y
recursos.
latch/

## 20
pom.xml
src/main/java/ni/edu/uam/latch/
arranque/
presentacion/
aplicacion/
dominio/
infraestructura/
src/main/resources/
ui/fxml/
ui/css/
ui/imagenes/
ui/i18n/mensajes_es_NI.properties
reportes/
src/test/java/ni/edu/uam/latch/
src/test/resources/fixtures/
db/migrations/
packaging/
docs/adr/
La raíz classpath será /ui/: fxml/ plano, css/ con components.css, claro.css, oscuro.css y
print.css, imagenes/ e i18n/mensajes_es_NI.properties. Los reportes estarán en /reportes/.
ThemeService.resource() cargará "/ui/css/" + archivo. UiMetrics y AuroraBackground aplicarán
los tokens del Brief, Tablas 3, 11, 13, C2 y C3 (Cuadra et al., 2026a); el fondo simplificado será
un color uniforme del tema. La implementación cargará tema y componentes sin duplicar paletas
en FXML y comprobará la igualdad de nombres de tokens en Claro y Oscuro.
Capa de Presentación con JavaFX
Arranque y Navegación
Application.init leerá configuración y preparará recursos sin construir controles;
Application.start creará la escena y mostrará el acceso. El punto de composición cargará FXML
con setControllerFactory e inyectará los servicios necesarios antes de iniciar la navegación. Una
sesión autenticada reemplazará el centro del BorderPane principal por la vista seleccionada,
dentro de una sola ventana de módulos. Los DTO de selección pasarán por un contexto tipado y
no por variables estáticas. Al cambiar de vista, el controlador liberará listeners y enlaces, y
advertirá si existen cambios sin guardar que el usuario deba descartar.

## 21
La raíz visual será un StackPane con AuroraBackground detrás de un BorderPane y sus
superficies opacas. La barra lateral abrirá los módulos, la barra superior indicará contexto y una
ToolBar ofrecerá las acciones del formulario activo. MenuBar conservará Archivo, Ver,
Operaciones y Ayuda, con ContextMenu para acciones de fila permitidas por rol. La UI usará
Label, Button, TextField, PasswordField, ComboBox, ListView, CheckBox, RadioButton,
TableView, DatePicker e ImageView según la naturaleza del dato. Los controles compartidos
proporcionarán nombre accesible, foco visible y estados vacíos con una acción concreta o
explicación suficiente. El registro de atajos se rige por el Brief, Tabla 10, sin colisiones con
mnemónicos de menú (Cuadra et al., 2026a).
Eventos, Formularios y Diálogos
Los botones dispararán casos de uso mediante comandos que validan datos antes de crear
tareas. Enter confirmará formularios cuando no interfiera con edición multilínea y Escape cerrará
diálogos cancelables sin anular una transacción ya confirmada. El doble clic del ratón abrirá el
detalle con la misma autorización que el menú contextual. Alert comunicará errores
recuperables, Dialog recogerá conceptos de anulación y FileChooser o DirectoryChooser
seleccionarán rutas de exportación y respaldo. Cada campo inválido mostrará causa y corrección,
conservará su valor y recibirá foco según el orden del formulario, con resumen accesible cuando
existan varias fallas. Al abandonar un formulario modificado se ofrecerá Guardar, Descartar o
Continuar editando; una validación fallida conservará los datos válidos no secretos. Cada PAN
define entrada, acciones, salida y los estados de carga, vacío, error y éxito que correspondan.
Task.call ejecutará operaciones fuera del hilo JavaFX y devolverá un DTO; sus estados
de éxito o falla actualizarán la pantalla en el hilo de aplicación (OpenJFX, s. f.-d). Los
controladores enlazarán progreso y mensaje mediante propiedades observables en lugar de

## 22
publicar un runLater por fila. Platform.runLater se reservará para eventos externos y descartará
actualizaciones de una vista que ya se cerró o cambió de sesión. Ningún controlador esperará
Future.get, join ni una llamada JDBC en el hilo gráfico. RNF-020 exigirá latencia máxima de
doscientos milisegundos durante las tareas H1, H2, H3 y H5.
Aurora UI Nativa y Contrato CSS
La identidad Aurora UI seguirá el Brief, Tablas 3, 11, 13, C2 y C3 (Cuadra et al., 2026a),
única fuente de tokens. La propuesta pública de Malewicz aporta el antecedente del estilo, con
alcance limitado al título, autor e introducción disponibles (Malewicz, s. f.). AuroraBackground
usará tres luces estáticas violeta, turquesa y lavanda, con alfas Claro/Oscuro de 0.20/0.16,
0.18/0.14 y 0.14/0.12, respectivamente. El grupo decorativo tendrá mouseTransparent=true y
focusTraversable=false; las superficies de texto y controles serán opacas. No habrá animación
permanente ni un hilo dedicado a decoración.
components.css consumirá los tokens del Brief mediante looked-up colors de JavaFX
(OpenJFX, s. f.-b): -latch-error para error, -latch-focus para foco, y los valores on-accent y
muted definidos en esa fuente. Los radios serán 8/12 px y los espacios 4/8/12/16/24/32 px según
el componente. El pseudoestado :invalid se activará con pseudoClassStateChanged; :focused
conservará el foco nativo. El diseño inicial prescinde de GaussianBlur; si se usa, solo afectará al
grupo decorativo, con radio máximo 63 de la API elegida (OpenJFX, s. f.-a).
La implementación de claro.css y oscuro.css se remite al Brief, Tablas 3, 11, 13, C2 y C3
(Cuadra et al., 2026a). Este TRD no replica una paleta ni un ejemplo CSS con valores
independientes.

## 23
Temas y Preferencias
ThemeService aceptará únicamente Claro y Oscuro, conservará un registro débil de
escenas y aplicará el cambio en el hilo JavaFX. Cada ventana secundaria y DialogPane se
registrará al crearse; los ContextMenu, DatePicker y listas desplegables recibirán sus hojas al
mostrarse y heredarán los mismos tokens. El servicio conservará el nodo enfocado y el estado del
formulario al sustituir la hoja de colores, sin recargar FXML. RNF-049 establece quinientos
milisegundos para el cambio y preservación de veinte campos o filtros. Los selectores nativos del
sistema conservarán el aspecto que Windows determine, pues no son popups estilizados por
JavaFX.
El adaptador de java.util.prefs.Preferences guardará lastTheme para la pantalla previa al
acceso y users/UUID/theme para cada usuario identificado. Al autenticar, leerá la preferencia
personal por id opaco; un valor ausente, desconocido o inaccesible seleccionará Claro. La opción
sin efectos usará otra clave personal y eliminará todas las luces y desenfoques en cualquiera de
los dos temas. Un fallo de escritura conservará el cambio en memoria y emitirá un aviso por
sesión, sin bloquear el formulario. El servicio no solicitará privilegios administrativos, no creará
tablas y no sincronizará su elección con Windows. El nodo raíz de Preferences será
ni/edu/uam/latch/presentation.
La Tabla 3 identifica la fuente canónica del contrato de presentación.
## Tabla 3
Remisión al Contrato Visual del Brief
ElementoFuente canónicaAplicación en el TRD
Colores y estadosBrief Tablas 3, 11 y 13; C2
y C3
Consumir los tokens; error con -latch-error y
foco con -latch-focus.
Métricas y
tipografía
Brief Tablas 3, 11 y 13; C2
y C3
Radios 8/12 px; espacios 4/8/12/16/24/32 px;
UI Segoe UI.

## 24
ElementoFuente canónicaAplicación en el TRD
Luces AuroraBrief Tablas 3, 11 y 13; C2
y C3
Violeta, turquesa y lavanda; alfas
Claro/Oscuro 0.20/0.16, 0.18/0.14 y
## 0.14/0.12.
Reportes y fondo
simplificado
Brief y contrato de reportes
de este TRD
Reportes DejaVu Sans 10/12/14 pt; sin
efectos y fondo simplificado usan color
uniforme del tema.
Los tokens del Brief son la única fuente visual (Cuadra et al., 2026a). WCAG 2.2 aporta
criterios de contraste y teclado; WCAG2ICT explica su adaptación informativa al escritorio
(World Wide Web Consortium, 2024; World Wide Web Consortium, 2025). La revisión medirá
texto normal 4.5:1, controles necesarios 3:1 y foco con perímetro equivalente a dos píxeles. Cada
estado tendrá texto o símbolo además del color, y sin efectos conservará controles, permisos y
datos financieros.
Capa de Persistencia
Reparto de Responsabilidades
Hibernate administrará propiedades, unidades, inquilinos, contratos, cargos, pagos,
aplicaciones, depósitos y solicitudes de mantenimiento. JDBC con PreparedStatement resolverá
autenticación, usuarios, parámetros, bitácora y consultas agregadas del panel o reportes. En un
caso financiero, los adaptadores JDBC recibirán Connection desde Session.doWork y no abrirán
otra conexión ni modificarán autocommit. Las consultas agregadas independientes podrán
solicitar una conexión del mismo DataSource dentro de una transacción de lectura. La Tabla 4
delimita estas rutas y sus responsables, con el fin de evidenciar operaciones CRUD SQL de RA2
y operaciones CRUD ORM de RA4 sin escrituras financieras separadas.

## 25
## Tabla 4
Rutas de Persistencia y Fronteras Transaccionales
RutaTecnologíaFrontera y ContenidoVerificación
## M01 Seguridad
y Usuarios
## JDBC
PreparedStatement
Transacción propia para alta,
cambio de rol, hash y
actualización de intentos.
Auditoría administrativa en la
misma conexión.
CRUD SQL y
prueba de cinco
fallos
concurrentes.
M02, M03 y
## M08
HibernateUna transacción por alta o
edición, versión y evento si
afecta gasto. Metadatos de
adjunto referencian archivo ya
publicado.
CRUD ORM y
conflicto de
@Version.
## M04
## Activación
Hibernate y JDBC
compartido
Bloqueo de unidad, contrato y
garantía; evento de auditoría en
la conexión de Session.
Dos activaciones
simultáneas
producen un solo
contrato aceptado.
M05 H1 y H2Hibernate y JDBC
compartido
Transacción por contrato con
indicador local y bloqueo de
fila. H1 aplica crédito sin
recibo adicional y registra
resumen durable M05
LOTE_RENTAS; H2 sin
concesión por instalación.
Reintento no
duplica renta ni
mora.
M06 y M07Hibernate y JDBC
compartido
Una transacción de
PagoService o
DepositoService: flush del
pago, fn_reservar_numero(),
Recibo @Immutable por
Hibernate, auditoría JDBC,
sp_verificar_integridad y
commit. Misma conexión.
Fallo en cada
punto revierte el
conjunto.
M09 TasasHibernateHistorial informativo y
autorización por fecha en
transacciones separadas de la
solicitud HTTP.
Ninguna conexión
retenida mientras
se espera Internet.
M10 y M11JDBC solo lecturaConsulta parametrizada o
snapshot corto; salida a DTO y
liberación de conexión antes de
renderizar.
Reporte no
modifica saldos ni
conserva Session.

## 26
RutaTecnologíaFrontera y ContenidoVerificación
## M12
Parámetros y
## Auditoría
JDBCParámetros con historial y actor
conforme al Esquema. Bitácora
con INSERT y SELECT;
UPDATE y DELETE
denegados a latch_runtime.
Autorización en
servicios y rechazo
SQL de
modificación.
SessionFactory será única por proceso y cerrará después de finalizar las tareas que usan
persistencia. Cada trabajo abrirá su propia Session, cuyo alcance termina al producir el DTO del
caso de uso; una Session nunca se comparte entre hilos. Hibernate documenta sesiones,
transacciones y carga de asociaciones como responsabilidades diferentes (Hibernate, s. f.-b). El
pool adoptará maximumPoolSize=6, minimumIdle=1, connectionTimeout=3000 ms,
validationTimeout=1000 ms y maxLifetime=1800000 ms, con tcpKeepAlive habilitado
(Wooldridge, s. f.-b). Tres clientes podrán abrir hasta dieciocho conexiones operativas, por lo
que el servidor objetivo reservará treinta para aplicación, operación y mantenimiento.
Mapeo, Carga y Conflictos
Las entidades usarán UUID interno, BigDecimal para importes y LocalDate para fechas
contractuales. PostgreSQL conservará NUMERIC(19,2) para dinero, NUMERIC(19,8) para tasa
y TIMESTAMPTZ para instantes; la presentación aplicará America/Managua. Cada entidad
editable tendrá columna version de tipo BIGINT y propiedad Long con @Version, inicializada
por Hibernate al persistir. Las asociaciones se cargarán de forma diferida y los repositorios
proyectarán lo requerido antes de cerrar Session. Una búsqueda paginada evitará fetch join de
colecciones con límite, pues esa combinación puede multiplicar filas; su consulta separará ids de
página y datos del detalle.
@Entity
@Table(name = "contrato")
public class Contrato {
@Id
private UUID id;
@Version
@Column(nullable = false)

## 27
private Long version;
@Column(precision = 19, scale = 2, nullable = false)
private BigDecimal canon;
## }
Un conflicto de versión producirá ConflictoConcurrenciaException y permitirá comparar
datos actuales antes de editar. Las finanzas bloquearán contrato y cargos para coordinar sumas
entre filas. El orden será puerta de mantenimiento, unidad, contratos por UUID, cargos por
vencimiento e id, pagos de origen, depósitos y contador al final. PostgreSQL admite bloqueos
por fila y detecta interbloqueos (The PostgreSQL Global Development Group, s. f.-a). Un
resultado incierto exige consultar el UUID antes de reintentar, con una Session nueva para la
operación posterior que requiera persistencia.
Restricciones y Cálculo Financiero
Una restricción única parcial protegerá la renta por contrato y período cuando el tipo sea
Renta, conservando también los cargos anulados para impedir una segunda renta del mismo
período. La aplicación corregirá errores mediante movimientos autorizados con referencia
cruzada según RN-33. Un bloqueo de unidad y el índice único parcial uq_contrato_activo
impedirán más de un contrato Vigente, Por vencer o Vencido por unidad. La fecha final debe
cumplir inicio.plusMonths(1) como duración mínima; un mes natural completo del día 1 al 31
exige fin al menos el día 1 del mes siguiente; el día de pago se ajustará al último día del mes. La
definición SQL del Esquema mantiene estos invariantes y los catálogos del PRD (Cuadra et al.,
2026e; Cuadra et al., 2026c).
Las restricciones CHECK, UNIQUE y FOREIGN KEY protegerán valores y relaciones
dentro de sus posibilidades declarativas (The PostgreSQL Global Development Group, s. f.-c).
La suma de aplicaciones, crédito y pago convertido requiere validación del servicio bajo
bloqueo, porque combina varias filas. RN-32 fija HALF_EVEN al convertir el total a dos
decimales, con tasa almacenada a ocho posiciones y remanente asignado a la última aplicación.

## 28
Un pago en NIO contra cargo USD divide entre la tasa NIO por USD; un pago USD contra cargo
NIO multiplica. PagoCommand incorpora fechaEfectiva: pago.fecha_pago es la fecha efectiva
ingresada por el usuario y no puede ser futura respecto a la fecha operativa. La tasa autorizada se
busca por esa fecha efectiva. La ecuación financiera debe conservar una diferencia de cero
centavos en cien combinaciones sintéticas.
Interfaces y Excepciones de Acceso
Los puertos públicos devolverán DTO o agregados de dominio sin exponer Session,
ResultSet ni Connection a los controladores. ContratoRepository buscará versiones y bloqueará
contratos; CargoRepository consultará saldo y período; PagoRepository recuperará resultados
por UUID. AuditoriaRepository recibirá un contexto transaccional interno cuando participe en
escritura financiera y solo expondrá consultas paginadas al resto del sistema. El adaptador
transformará SQLSTATE o excepciones Hibernate en FalloAccesoDatosException,
ConflictoConcurrenciaException, DuplicadoException o IntegridadException. El servicio
conservará la causa técnica para el registro y devolverá un mensaje operativo que indique si la
operación se revirtió, se confirmó o necesita conciliación.
Concurrencia y Sincronización
Política Común de Ejecución
El gestor creará un ejecutor financiero de dos trabajadores y cola de veinte solicitudes,
otro de un trabajador y cola uno para reportes y un scheduler de dos hilos. H1 usará un trabajador
dedicado mientras esté activo, con exclusión por aplicación y bloqueo persistente por contrato.
Los nombres incluirán latch-finanzas, latch-lote, latch-reportes o latch-programador para facilitar
el diagnóstico. Una cola llena rechazará la solicitud antes de escribir y mostrará la posibilidad de

## 29
reintentar. Los ejecutores controlados serán no daemon para permitir el cierre coordinado, y
ningún rechazo transferirá trabajo al hilo JavaFX mediante CallerRunsPolicy.
Tras validar el esquema en el arranque (FLU-49), se iniciarán H2 y H3. El scheduler
disparará H2 cada cinco minutos después de terminar su ejecución anterior y H3 cuando venza
su ventana diaria. H2 combinará un indicador atómico local con bloqueo de fila por contrato,
también en LAN; no tendrá una concesión por instalación. Solo H3 usará una concesión
persistente breve en control_fx, cerrando la transacción antes de consultar HTTP. La Tabla 5
especifica propósito, ejecución, sincronización y detención de H1 a H6. La cancelación se
comprueba en el límite transaccional; interrumpir un hilo no acredita rollback.

## 30
## Tabla 5
Diseño Operativo de H1 a H6
Hilo y clasePropósitoEjecución y
## Frecuencia
Recursos y
## Sincronización
Cancelación y
## Detención
Error y
## Recuperación
## Comunicación
## UI
## H1
GeneracionCargosTask
## Rentas
mensuales
masivas,
## RF-M05-
01 a 04.
## Inicio
explícito tras
vista previa.
Un H1 por
proceso.
Contratos y
cargos; bloqueo
de fila de
contrato, clave
única de renta y
puerta
compartida de
escritura.
## Comprueba
cancelación
entre
contratos; la
transacción
del contrato en
curso termina
antes de
detener el lote.
## Conserva
confirmados y
resume
pendientes con
## MSG-059.
Evento durable
## M05
## LOTE_RENTAS:
correlación y
conteos generados,
omitidos, fallidos y
pendientes. Falla
general detiene
lote; reintento
omite
confirmados.
updateProgress y
updateMessage;
DTO de
confirmados,
omitidos, fallidos
y pendientes.
## H2
RevisionVencimientosTask
## Vencimient
os, Por
vencer y
mora; RF-
## M04-10,
## RF-M05-
## 06.
Demora fija
de 5
minutos;
ejecución
inicial tras
validar el
esquema en
el arranque
## (FLU-49).
## Indicador
atómico local y
bloqueo de fila
por contrato.
Mora única por
cargo; sin
concesión por
instalación.
## Future.cancel(
true); verifica
bandera entre
contratos y
respeta
rollback del
actual.
Los incidentes se
auditan como
## INCIDENTE_H2
con
detalle.origen='H2'
. Un error de
contrato queda
identificado; caída
de DB detiene
pasada y avisa una
vez.
Evento agregado
al hilo JavaFX;
refresco de
indicadores sin
actualizar por
fila.

## 31
Hilo y clasePropósitoEjecución y
## Frecuencia
Recursos y
## Sincronización
Cancelación y
## Detención
Error y
## Recuperación
## Comunicación
## UI
## H3
ActualizacionTasaTask
## Referencia
## USD/NIO;
## RF-M09-
01 a 09.
## Desde
scheduler,
iniciado tras
validar el
esquema en
el arranque
## (FLU-49);
un intento
ordinario
cada 24 h
por
instalación.
Caché inmutable
y concesión
persistente en
control_fx con
presupuesto
compartido;
consultor
designado en
## LAN.
## Cancela
CompletableF
uture y cierre
de stream;
concesión
expira si cae el
proceso.
Los incidentes se
auditan como
## INCIDENTE_H3
con
detalle.origen='H3'
. Timeout conserva
caché histórica;
errores de llave o
cuota suspenden
reintentos; un
reintento tras 60 s
por fallos
transitorios de red,
HTTP 5xx o HTTP
## 408.
## Estado
consultando,
vigente
informativa,
histórica o no
disponible;
origen y hora
visibles.
## H4
EmisorRecibos en
PagoService
## Asignar
recibo
único sin
saltos; RF-
## M06-07.
Sección del
trabajador
financiero
por pago; no
es hilo
permanente.
ReentrantLock(tr
ue) local hasta
commit/rollback;
consecutivo_reci
bo por
fn_reservar_num
ero(), UUID fijo
y restricciones
únicas del
## Esquema.
lockInterrupti
bly antes de
comenzar.
Iniciada la
transacción,
commit o
rollback debe
terminar; no
se cancela
solo el
contador.
Rollback no
consume número.
Desconexión de
resultado incierto
exige consulta de
UUID antes de
reintentar.
## Resultado
confirmado luego
del commit; error
PDF solo permite
reimpresión.

## 32
Hilo y clasePropósitoEjecución y
## Frecuencia
Recursos y
## Sincronización
Cancelación y
## Detención
Error y
## Recuperación
## Comunicación
## UI
## H5
ExportacionReporteTask
## Llenar,
previsualiz
ar y
exportar
reportes;
## RF-M10-
10 a 12.
## Bajo
demanda;
una instancia
por
aplicación.
DTO de lectura,
temporal privado
y JasperPrint no
compartido; sin
conexión durante
escritura del
archivo.
## Cancelación
cooperativa
durante
consulta,
llenado y
exportación;
confirma
MSG-089 al
terminar y
limpiar
temporales,
antes de
publicar.
Falla o cancelación
no revierte
finanzas
confirmadas. La
publicación final
resuelve la carrera:
si ya publicó,
informa el
resultado real;
conserva destino
previo hasta
publicación
exitosa.
Progreso por fase
y cancelación;
enlace al archivo
solo tras
publicación
exitosa.
## H6
CoordinadorCierre
## Cerrar
recursos;
## RF-M12-
08 y
## RN-30.
## Coordinador
asíncrono al
confirmar
salida; único
por proceso.
Bandera de cierre
impide entradas,
registro de tareas
y futuros,
SessionFactory,
pool y
HttpClient.
## Detiene
scheduler,
solicita
cancelación y
espera hasta
10 s fuera del
hilo JavaFX.
H4 conserva
su frontera.
Si falta terminar,
avisa y mantiene
proceso abierto; no
fuerza salida ni
cierra conexiones
utilizadas.
Diálogo de cierre
con tarea
bloqueante;
Platform.exit solo
después de cerrar
recursos.

## 33
Detalle de Cargos y Tareas Periódicas
H1 tomará ids elegibles, período y versión de parámetros antes del lote. Por contrato
bloqueará su fila, comprobará fechas y renta existente, calculará canon y aplicará crédito dentro
de la misma transacción. La cancelación conservará contratos confirmados y evitará cargos
parciales, conforme a RN-20. H2 usará el mismo bloqueo y generará mora una sola vez por
renta, sobre principal pendiente después del vencimiento más tres días de gracia; iniciará con
porcentaje cero y admitirá monto fijo según el catálogo del Apéndice A. La base excluirá Mora
del cálculo y los cambios de política conservarán importes confirmados. El resumen durable de
H1 será el evento de bitácora M05 LOTE_RENTAS con correlación y conteos generados,
omitidos, fallidos y pendientes.
H2 tendrá un indicador atómico local y bloqueo de fila por contrato para impedir
superposición local y coordinar clientes LAN. H3 reservará una concesión de noventa segundos
en control_fx y cerrará esa transacción antes del GET. Si la instancia designada deja de
responder, el Propietario podrá designar otra; el vencimiento de la concesión permitirá recuperar
un intento interrumpido. La fecha de próximo intento y el consumo mensual se compartirán en
PostgreSQL para evitar que tres clientes multipliquen consultas. Una reinicialización de la
aplicación conservará ese estado, sin convertir cada acceso en una llamada al proveedor.
Consecutivo de Recibo
RN-07 exige consecutivos contiguos para pagos confirmados en la base activa, con inicio
000001 y sin reinicio anual (Cuadra et al., 2026c). Una secuencia PostgreSQL entrega números
distintos, pero sus incrementos no se revierten cuando falla la transacción (The PostgreSQL
Global Development Group, s. f.-d). Latch usará una fila consecutivo_recibo con
siguiente_numero e id UUID fijo 00000000-0000-0000-0000-000000000001 y la bloqueará

## 34
mediante SELECT FOR UPDATE al final de la operación financiera. El ReentrantLock local
abarcará el caso de uso hasta completar commit o rollback, mientras la base coordinará clientes
diferentes. La Figura 7 muestra dos solicitudes competidoras y el punto único de publicación del
número confirmado.
## Figura 7
Secuencia Transaccional del Consecutivo de Recibo
PagoService recibirá un PagoCommand con UUID de solicitud y huella canónica,
buscará un pago previo y bloqueará contrato y cargos antes de calcular aplicaciones. El orden
canónico será flush del pago y aplicaciones → fn_reservar_numero() → persistir Recibo
@Immutable mediante Hibernate y hacer flush → auditoría JDBC → CALL
sp_verificar_integridad(contratoId) → commit. Todo usará la conexión de Session; JDBC no
insertará Recibo ni confirmará por separado. Una restricción única impedirá repetir número,
pago o solicitud, y el recibo solo será visible tras commit. Si dos solicitudes comparten UUID, la
perdedora revertirá y recuperará el resultado en una Session nueva; una huella distinta producirá

## 35
ConflictoIdempotenciaException. La reserva, el pago, sus aplicaciones, el recibo y la auditoría
pertenecen a una sola transacción, una Session y una conexión. El rollback anterior al commit no
consume número; la anulación posterior conserva número e historial. nextval no sustituye el
contador transaccional.
-- Dentro de la transacción de PagoService y tras flush del pago.
-- fn_reservar_numero bloquea y actualiza la fila UUID canónica.
SELECT latch.fn_reservar_numero();
-- Persistir Recibo @Immutable con Hibernate y ejecutar flush.
-- Insertar auditoría JDBC sobre la conexión de Session.
-- Enlazar el UUID del contrato en CallableStatement.
CALL latch.sp_verificar_integridad(?);
-- Solo PagoService ejecuta commit o rollback.
Una anulación conservará número, motivo, actor e instante, y revertirá aplicaciones
dentro de su propia transacción autorizada. La operación bloqueará dependencias del crédito y
exigirá revertir primero aplicaciones posteriores activas, como indica RN-31. El PDF se generará
después del commit con los datos inmutables del recibo; un error de archivo habilitará
reimpresión y no consumirá otro número. La garantía se limita a la base activa bajo esta ruta de
escritura y sus restricciones. Una restauración histórica permanecerá en mantenimiento hasta
conciliar recibos emitidos, pues recuperar una copia antigua puede perder operaciones
posteriores al respaldo.
Ciclo de Vida y Cierre
Los hilos de plataforma pasan por NEW antes de iniciar y por RUNNABLE durante
ejecución disponible o efectiva. BLOCKED describe espera por monitor intrínseco;
ReentrantLock, esperas de futuros y estacionamiento suelen observarse como WAITING o
TIMED_WAITING. TERMINATED indica que el hilo terminó, mientras los trabajadores de un
pool pueden permanecer esperando otra tarea sin haber finalizado su vida. El estado de Task es
distinto y usa READY, SCHEDULED, RUNNING, SUCCEEDED, FAILED o CANCELLED.

## 36
Las pruebas registrarán ambas perspectivas y no confundirán cancelación lógica del Worker con
terminación física del trabajador o confirmación del rollback.
Al solicitar salida, H6 marcará cierre y consumirá el evento de ventana para mantener
visible el progreso. El coordinador detendrá nuevos disparos, cancelará productores y esperará
trabajos activos sin bloquear JavaFX; H4 completará la transacción que ya comenzó. Después
cerrará HttpClient, SessionFactory y pool, y pedirá Platform.exit desde el hilo gráfico. El plazo
total objetivo será diez segundos y un exceso mostrará la tarea que sigue activa, con el proceso
abierto hasta liberar recursos. Thread.stop, System.exit durante escritura y cierre anticipado de
conexiones quedan prohibidos por la política de ciclo de vida del diseño.
Carreras, Interbloqueos e Inanición
Las carreras previstas incluyen doble activación de unidad, renta repetida, mora
simultánea con pago y dos cobros contra el mismo saldo. El orden de bloqueos, @Version y las
restricciones de negocio cubren esas rutas con pruebas de competencia y reversión.
lock_timeout=2000 ms y statement_timeout=5000 ms limitarán las transacciones de escritura
ordinaria, sin aplicarse de forma ciega a exportaciones o respaldo. SQLSTATE 40P01/40001
permitirá tres intentos totales: uno inicial y dos reintentos, con el mismo UUID. Las esperas base
serán 100 y 250 ms más jitter de 0 a 50 ms, fuera de transacción. Cada intento abrirá una Session
nueva y nunca reutilizará una transacción fallida.
El riesgo de inanición se controla con colas limitadas, ReentrantLock justo y
transacciones financieras cortas. Los procesos masivos liberarán el contrato después de cada
commit y cederán la ejecución antes de seguir con el próximo id. El sistema no mantendrá
bloqueos mientras espera interacción, HTTP o exportación de archivos, y una cancelación
pendiente evitará comenzar otra transacción. La equidad del lock local no garantiza equidad

## 37
global de PostgreSQL, por lo que las pruebas medirán esperas y rechazos por cliente. Si un
trabajo excede su presupuesto, el registro identificará el recurso bloqueante y el servicio
presentará una recuperación concreta.
Integración con el Servicio Web de Tipo de Cambio
Selección y Condiciones del Proveedor
ExchangeRate-API, de AYR Tech, admite NIO y base USD en solicitudes estándar
(AYR Tech (Pty) Ltd., s. f.-d; AYR Tech (Pty) Ltd., s. f.-c). El plan Free anuncia actualización
diaria y mil quinientas consultas mensuales (AYR Tech (Pty) Ltd., s. f.-b). Los términos
permiten caché interna, restringen redistribución y desaconsejan usar sus tasas informativas para
transacciones (AYR Tech (Pty) Ltd., s. f.-e). Latch conservará el proveedor del PRD para
consulta orientativa y RA5. Cada pago entre monedas requerirá una tasa autorizada por el
Propietario para la fecha local, con origen e instante conservados en el registro.
La Tabla 6 compara dos proveedores con contrato técnico y organización identificables,
más la referencia institucional del BCN. La selección favorece continuidad con el PRD,
autenticación Bearer documentada y una frecuencia compatible con la actualización diaria. Open
Exchange Rates sigue siendo una alternativa publicada y no se descarta por falta de NIO.
FXRatesAPI y CurrencyFreaks no se adoptan: la comparación ya dispone de alternativas
atribuibles y documentadas, sin necesitar afirmar reputación, cobertura o condiciones de esos
candidatos iniciales. La decisión no incluye registro de cuenta, contratación, consumo
autenticado ni prueba de disponibilidad efectiva de ningún proveedor.

## 38
## Tabla 6
Comparación de Proveedores y Referencia Oficial
OpciónCobertura y ContratoCuota, Condiciones y
## Vigencia
## Decisión
ExchangeRate-
API Free
USD como base y NIO
en conversion_rates.
GET estándar;
## Authorization Bearer
sin llave en URL (AYR
Tech (Pty) Ltd., s. f.-a;
AYR Tech (Pty) Ltd., s.
f.-d).
1500 solicitudes/mes,
actualización diaria y costo
publicado USD 0. Caché
interna permitida; uso
orientativo y sin garantía de
continuidad (AYR Tech (Pty)
Ltd., s. f.-b; AYR Tech (Pty)
Ltd., s. f.-e).
Adoptada para
consulta
informativa. Las
tasas de cobro se
autorizan por
fecha.
## Open Exchange
## Rates Free
NIO en catálogo, base
USD; GET latest.json
con app_id y campos
rates, base y timestamp
(Open Exchange Rates,
s. f.-b; Open Exchange
Rates, s. f.-a).
1000 solicitudes/mes y
actualización horaria.
Términos propios y
condiciones adicionales de
servicios; no se presume SLA
(Open Exchange Rates, s. f.-c;
## Open Exchange Rates, 2022).
## Alternativa
descartada por
continuidad del
PRD y menor
necesidad de
actualización.
## Banco Central
de Nicaragua
## Publicación
institucional del tipo
oficial para 2026,
expresado en NIO por
USD (Banco Central de
## Nicaragua, 2025).
Comunicado público; no se
verificó un contrato JSON
público equivalente al de los
proveedores.
Fuente oficial
para referencia y
autorización
manual. No se
inventa endpoint
API del BCN.
Contrato de Solicitud y Respuesta
El cliente realizará GET a https://v6.exchangerate-api.com/v6/latest/USD con Accept:
application/json y Authorization: Bearer seguido de la llave externa (AYR Tech (Pty) Ltd., s. f.-
a). La respuesta válida tendrá result igual a success, base_code igual a USD y
conversion_rates.NIO entre 1 y 100 NIO/USD, ambos inclusive, además de marcas de
actualización interpretables (AYR Tech (Pty) Ltd., s. f.-c). El adaptador convertirá números a
BigDecimal y rechazará cuerpo inválido, base distinta, ausencia de NIO o fechas futuras
incompatibles con el reloj. El siguiente JSON es un fixture sintético reducido a las monedas
relevantes; sus importes y marcas no proceden de una consulta real y no representan una tasa
oficial.

## 39
## {
## "result": "success",
## "documentation": "https://www.exchangerate-api.com/docs",
## "terms_of_use": "https://www.exchangerate-api.com/terms",
## "time_last_update_unix": 1792713600,
"time_last_update_utc": "Fri, 23 Oct 2026 00:00:00 +0000",
## "time_next_update_unix": 1792800000,
"time_next_update_utc": "Sat, 24 Oct 2026 00:00:00 +0000",
"base_code": "USD",
## "conversion_rates": {
## "USD": 1,
## "NIO": 36.5
## }
## }
La Figura 8 separa consulta, validación, caché y presentación de la autorización que
necesita el pago. HttpClient será reutilizable y la tarea realizará el envío fuera del hilo JavaFX,
con conexión máxima de cinco segundos y presupuesto total de diez segundos (Oracle, s. f.-b).
Un suscriptor limitado rechazará cuerpos superiores a 256 KiB y contenido que no sea JSON, sin
registrar el cuerpo completo. La validación permitirá una tolerancia de cinco minutos frente al
reloj para timestamps futuros y exigirá next_update posterior a last_update. Un error de esquema
conservará la caché anterior y dejará un incidente técnico con correlación. Una referencia fuera
de la banda 1–100 NIO/USD quedará en cuarentena: no sustituirá la referencia vigente y
registrará un evento M09 REFERENCIA_EN_CUARENTENA en bitacora, con valor y huella;
no se creará una tabla de cuarentena.

## 40
## Figura 8
Consulta HTTP y Autorización de la TipoCambio
Errores, Caché y Límites
La política admite una consulta diaria y un único reintento después de sesenta segundos
para fallos transitorios de red, HTTP 5xx o HTTP 408, tratado como timeout transitorio. HTTP
429 respetará Retry-After y, si falta esa cabecera, esperará veinticuatro horas con aviso al
usuario. Los códigos 401 o 403, invalid-key, inactive-account y quota-reached suspenderán la
consulta hasta corregir configuración o cuota. El adaptador también leerá result=error aunque el
servidor responda HTTP 200. La UI comunicará referencia no disponible y mantendrá las
funciones locales que no requieren una nueva tasa, con caché y antigüedad visibles.
H3 consultará una vez cada veinticuatro horas por instalación y conservará fetched_at,
last_update, next_update, proveedor, base, destino y valor. La programación tomará el mayor
instante entre la ventana diaria y la actualización informada, con revisión al volver del modo
suspendido del equipo. Un mes de treinta y un días admite treinta y una consultas ordinarias y

## 41
hasta treinta y un reintentos, para un máximo propuesto de sesenta y dos. El presupuesto interno
será cien intentos mensuales compartidos, por debajo de la cuota publicada. Los refrescos
manuales leerán caché salvo reintento autorizado, sin reiniciar el contador al abrir otra sesión.
La caché actual se conservará como una versión inmutable y su sustitución no actualizará
pagos históricos. La presentación mostrará fuente, instante de consulta y antigüedad; una
referencia de otra fecha local se identificará como histórica. El dominio almacenará por separado
tasa autorizada, fecha efectiva, fundamento y UUID del Propietario, sin promover una referencia
comercial al estado de tasa oficial. Si falta autorización para la fecha efectiva del pago, la
operación entre monedas se bloqueará antes de iniciar su transacción. Un pago en la misma
moneda seguirá disponible con su conversión identidad y sin necesidad de consultar Internet ni
de inventar una tasa externa. Una tasa manual fuera de la banda 1–100 exige revisión y
confirmación del Propietario; esa banda no será una restricción de base de datos para la
autorización manual.
Referencia del BCN y Custodia de la Llave
El BCN anunció el 20 de octubre de 2025 un deslizamiento de cero por ciento y un tipo
oficial de 36.6243 córdobas por dólar para 2026 (Banco Central de Nicaragua, 2025). Ese dato
institucional describe una referencia publicada y no convierte las tasas comerciales del proveedor
en oficiales. Latch registrará una autorización manual por fecha con valor, fuente y actor; podrá
citar el comunicado si ese es el fundamento utilizado por el Propietario. El programa no
precargará valores ficticios como datos reales ni actualizará cifras históricas cuando cambie la
referencia disponible. Los recibos identificarán el origen y la tasa efectivamente aplicada a cada
pago.

## 42
La llave se leerá desde un archivo externo con ACL restringida o variable
LATCH_FX_API_KEY y no aparecerá en FXML, repositorio ni archivos de evidencia. El
servicio usará Bearer para evitar que la llave forme parte de la URL, y el registro redactará la
cabecera Authorization. El paquete académico distribuirá únicamente claves de configuración sin
secretos y el entorno de pruebas usará un servidor HTTP local simulado. En LAN, el consultor
designado será el único equipo que necesite la llave del proveedor. La protección supone cuentas
de Windows confiables; un administrador del equipo puede inspeccionar procesos y queda fuera
del aislamiento que ofrece una aplicación de escritorio.
Reportes con JasperReports
Diseño, Compilación y Datos
Studio 7.0.8 diseñará plantillas .jrxml de la misma generación que Library 7.0.8
(Jaspersoft, 2026). La construcción compilará todas las plantillas con JasperCompileManager y
detendrá la distribución ante cualquier error, conservando fuentes en Git y compilados dentro del
paquete. JasperReports 7 rompe compatibilidad con plantillas compiladas de versiones previas y
reorganiza extensiones, por lo que el proceso evitará reutilizar archivos .jasper ajenos a la
versión fijada (Jaspersoft, s. f.-a). La Figura 9 muestra el flujo desde parámetros y consulta hasta
exportación publicada. El recibo se renderizará después de confirmar el pago, con datos
históricos que permitan reimprimirlo sin recalcular su saldo. H5 confirma la cancelación normal
con MSG-089 únicamente al terminar el trabajo y limpiar temporales antes de publicar. La
publicación final resuelve la carrera con la solicitud de cancelación: si el archivo ya se publicó,
se informa el resultado real; el destino previo se conserva hasta publicación exitosa y no se
revierten pagos ni anulaciones confirmados.

## 43
## Figura 9
Generación y Publicación de Reportes
Los servicios de reporte producirán JavaBeans con getters, filtros, corte cuando
corresponda y moneda explícita para JRBeanCollectionDataSource. La elección evita acoplar
Jasper a entidades con asociaciones diferidas o al adaptador Hibernate de otra versión. Una
consulta JDBC parametrizada podrá formar esos DTO bajo snapshot corto, pero liberará la
conexión antes de llenar JasperPrint. Los subinformes recibirán colecciones y rutas de recursos
internos, nunca una ruta arbitraria del usuario. Los totales se separarán por moneda; cualquier
consolidación convertida mostrará tasa y fundamento, y no sumará columnas NIO y USD como
si fueran importes comparables. La ocupación de REP-05 y del panel se calculará al instante de
generación, sin selector de fecha histórica.
Catálogo, Exportación y Vista Previa
La Tabla 7 presenta plantillas y Contenido de los Diez Reportes.

## 44
## Tabla 7
Plantillas y Contenido de los Diez Reportes
Reporte y plantillaParámetros y datosSalida y criterio
REP-01 Recibo
recibo.jrxml
Número, pago, aplicaciones,
tasa, emisor y actor.
Solo PDF; importes del pago
confirmado y aviso no fiscal.
REP-02 Estado de
cuenta
estado-cuenta.jrxml
Contrato, corte y moneda.PDF y XLSX; cargos, aplicaciones,
crédito y saldo cronológico.
REP-03 Cartera vencida
cartera-vencida.jrxml
Corte, propiedad, contrato y
moneda.
PDF y XLSX; saldos por antigüedad,
sin movimientos anulados.
REP-04 Ingresos y
gastos
ingresos-gastos.jrxml
Intervalo, propiedad y
moneda.
PDF y XLSX; ingresos y gastos
separados, sin tratar garantías como
renta.
REP-05 Ocupación
ocupacion.jrxml
Propiedad e instante de
generación; sin selector
histórico.
PDF y XLSX; ocupación al generar,
también para el panel.
REP-06 Contratos por
vencer
contratos-por-
vencer.jrxml
Fecha base y umbral inicial
configurable de 30 días.
PDF y XLSX; unidad y fecha de
vencimiento.
REP-07 Historial de
pagos
historial-pagos.jrxml
Contrato o inquilino e
intervalo.
PDF y XLSX; pagos Registrado y
Anulado, recibo y método.
REP-08 Garantías
garantias.jrxml
Depósito, contrato y
movimientos.
PDF y XLSX; inicial, deducciones,
devoluciones y remanente.
REP-09 Mantenimiento
mantenimiento.jrxml
Intervalo, propiedad, unidad y
estado.
PDF y XLSX; solicitudes, gastos y
vínculos conforme al Esquema.
REP-10 Tasas utilizadas
tasas-utilizadas.jrxml
Intervalo, pagos, autorización
y tipo de cambio.
PDF y XLSX; consulta pago   ⨝
autorizacion_tasa   tipo_cambio, ⨝
con origen y valor aplicado.
La Tabla 7 conserva los diez reportes del PRD: el recibo se exporta solo en PDF y los
otros nueve en PDF y XLSX, para 19 archivos por recorrido completo. La auditoría se consultará
únicamente en PAN-19. PDF usará JasperExportManager o el exportador PDF; JRXlsxExporter
conservará números y fechas como tipos, sin convertirlos en texto monetario. El paquete incluirá
DejaVu Sans con licencia redistribuible (DejaVu Fonts, s. f.), a 10/12/14 pt según el Brief; la UI
seguirá en Segoe UI. La aceptación comprobará acentos, ñ, C$, US$ y totales en diez PDF y
nueve XLSX.

## 45
H5 publicará exportaciones mediante un temporal y movimiento atómico; si no está
disponible, copiará a un nombre de preparación, verificará tamaño y huella y renombrará al
terminar. Un fallo eliminará residuos y conservará el archivo anterior. PAN-11 y PAN-16
tendrán un visor integrado: JasperPrintManager renderizará cada página a PNG en memoria
mediante ImageIO y la UI la mostrará en ImageView, sin Swing UI ni SwingFXUtils.
HostServices solo abrirá la carpeta o el archivo ya exportado. El visor no tendrá Imprimir,
btnImprimir ni Ctrl+P; la impresión se realizará desde el PDF exportado. FileChooser elegirá
destino y confirmará sobrescritura antes de exportar. Al cerrar la vista se liberarán JasperPrint,
imágenes y Dto.
## Seguridad
Autenticación y Autorización
Argon2id usará diecinueve MiB de memoria, dos iteraciones, paralelismo uno y sal
criptográfica aleatoria de dieciséis bytes por credencial, de acuerdo con el mínimo adoptado del
PRD y OWASP (OWASP Foundation, s. f.). Bouncy Castle proporcionará la implementación
Java y el servicio guardará parámetros, versión de algoritmo y derivado de treinta y dos bytes
(Legion of the Bouncy Castle, s. f.). La comparación usará una función de tiempo constante y la
migración de costo ocurrirá después de un acceso válido. La política aceptará contraseñas de
doce a ciento veintiocho caracteres, sin truncamiento silencioso, y limitará entradas
extraordinarias antes de consumir memoria de cálculo.
El servicio de acceso bloqueará la fila de usuario al actualizar intentos y no mantendrá
esa conexión mientras calcula el hash. Un contador de versión permitirá repetir la actualización
si otro intento cambió el estado durante el cálculo, con una verificación final de bloqueo antes de
autenticar. Cinco fallos consecutivos activarán quince minutos de bloqueo y un acceso correcto

## 46
reiniciará el contador, conforme a RN-15. Los mensajes no revelarán si el usuario existe y el
intento inexistente calculará un hash de prueba equivalente. La sesión cerrará tras quince minutos
sin actividad o mediante acción explícita, con UUID y rol inmutables.
El Propietario podrá gestionar usuarios, cambiar parámetros, dar bajas lógicas y anular
recibos; el Asistente operará inquilinos, contratos, cobros y reportes permitidos. Cada servicio
comprobará la capacidad antes de escribir y volverá a validar autorización y sesión activa cuando
una tarea diferida empiece a operar; la denegación conserva ERR-009 / MSG-062 y las
excepciones expresas de la matriz vigente. La interfaz ocultará acciones exclusivas, pero las
pruebas invocarán también los servicios para verificar el rechazo. Los cambios de rol o
desactivación invalidarán sesiones mediante revisión periódica y control previo a operación
financiera. Cambiar Claro, Oscuro o sin efectos seguirá siendo una preferencia personal de
ambos roles, sin permisos de administración ni eventos financieros.
SQL, Secretos y Privilegios
PreparedStatement parametrizará valores y las consultas Hibernate utilizarán parámetros
enlazados, sin concatenar entradas en sentencias. Los nombres de columna y órdenes
seleccionables procederán de una lista interna, porque no pueden parametrizarse como valores.
El usuario PostgreSQL de ejecución tendrá permisos mínimos sobre las tablas requeridas y
carecerá de CREATE, ALTER, DROP y superusuario. La bitácora permitirá inserción y
consulta, con UPDATE y DELETE denegados a esa credencial; el migrador y el operador de
restauración usarán credenciales distintas. La aplicación no distribuirá la contraseña del
superusuario ni la utilizará para su conexión ordinaria de trabajo.
La configuración externa protegerá usuario de base, contraseña y llave HTTP mediante
permisos de Windows y directorios separados de los binarios. El repositorio excluirá secretos,

## 47
copias de datos reales, respaldos y archivos de preferencias; el paquete conservará ejemplos sin
credenciales válidas. Las conexiones LAN usarán TLS con validación de servidor y SCRAM-
SHA-256 para autenticación de base, con certificados provisionados para la instalación. Este
modelo confía en los equipos que alojan el cliente y no sustituye un servidor de autorización
frente a un usuario con control administrativo del sistema operativo. La revisión registrará esta
frontera al evaluar acceso local y distribución de credenciales.
Datos Personales y Adjuntos
La demostración usará exclusivamente datos sintéticos y limitará los expedientes a
información necesaria para representar contratos y cobros. Un eventual piloto real exigirá
registrar finalidad y base de tratamiento por expediente según RNF-043, con revisión
institucional antes de cargar información identificable. Esa condición de aceptación procede del
PRD y no constituye una declaración de conformidad jurídica emitida por este TRD. Las
solicitudes ficticias de acceso o rectificación tendrán una meta interna de un día hábil, sin atribuir
ese plazo a una ley. Los roles autorizados verán solo los datos necesarios y los logs excluirán
identificación personal y contenido de documentos.
Los adjuntos admitirán PDF, PNG y JPEG de hasta cinco MiB, con extensión y firma de
archivo verificadas antes de copiar. El servicio generará nombre UUID, calculará SHA-256 y
normalizará la ruta para comprobar que permanezca dentro del repositorio controlado. Las
fotografías deberán decodificarse con límites de dimensiones para evitar consumos excesivos, y
los PDF no se ejecutarán como contenido activo dentro de Latch. El archivo se publicará antes
de confirmar su metadato; una reversión podrá dejar un huérfano recuperable que el recolector
eliminará bajo exclusión y tras veinticuatro horas. La ruta original elegida por el usuario no
formará parte del recibo.

## 48
Auditoría y Frontera de Confianza
Toda operación financiera confirmada incluirá actor, instante UTC, acción, referencia y
UUID de correlación en la misma transacción. El registro conservará conceptos y diferencias
financieras necesarias sin incluir contraseñas, hashes de credenciales, cabeceras, cadenas de
conexión completas ni adjuntos. Una consulta paginada permitirá reconstruir el pago y sus
anulaciones, con índices por referencia e instante; los roles operativos no editarán eventos. La
auditoría de aplicación es trazabilidad funcional y no una garantía de invulnerabilidad frente al
administrador de PostgreSQL. El respaldo conservará esa historia y las pruebas verificarán cien
operaciones confirmadas con cien eventos financieros correspondientes. El instante financiero se
obtiene de now() de PostgreSQL y se guarda en pago.creado_en; la fecha operativa corresponde
a creado_en en America/Managua. pago.fecha_pago conserva la fecha efectiva ingresada por el
usuario. El control de desfase de reloj usa la fecha operativa, no la fecha efectiva.
ContextoOperacion comprobará la diferencia absoluta entre reloj local y BD; si supera cinco
minutos, RelojDesfasadoException bloqueará nuevas operaciones financieras con ERR-057 /
MSG-080 hasta corregir el reloj. ContextoOperacion.sistema(tarea) permite auditoría con actor
NULL únicamente para M05: MORA_AUTOMATICA, REVISION_VENCIMIENTO e
INCIDENTE_H2, con detalle.origen='H2'; y M09: REFERENCIA_ACTUALIZADA,
REFERENCIA_EN_CUARENTENA e INCIDENTE_H3, con detalle.origen='H3'. fn_auditar
valida esta lista cerrada. autorizacion_tasa exige siempre un actor humano. El control no
sustituye la validación de la fecha efectiva ni interrumpe una transacción financiera ya iniciada.
Validación, Manejo de Errores y Registro
La validación de presentación explicará campos y formatos antes del envío; la capa de
aplicación repetirá las reglas que afectan permisos, importes o fechas. El dominio impondrá

## 49
estados y ecuaciones financieras, mientras PostgreSQL rechazará duplicados y referencias
inválidas aun si falla una comprobación previa. Un mensaje al usuario indicará acción fallida,
estado de confirmación y opción de recuperación, con un identificador de incidente para soporte.
La Tabla 8 relaciona las excepciones técnicas y de negocio con ese tratamiento. Los mensajes
operativos estarán en español y conservarán los valores ingresados cuando una corrección no
requiera descartar el formulario. La jerarquía tiene base única LatchException y une las
excepciones del TRD con los doce contratos del Esquema. La Tabla 8 usa ERR del Flujo, Tabla
80, y MSG del Brief, con las ampliaciones canónicas; C01 queda resuelto conforme al Apéndice
G del Plan de implementación y la aprobación del 25 de octubre de 2026: 66 ERR y 89 MSG; los
demás inventarios se conservan. Un rechazo de validación conserva el formulario, un rollback
confirmado permite indicar ausencia de cambios y un commit incierto exige consultar por UUID
antes de reenviar. La cancelación normal y el fallo de reporte posterior al commit conservan toda
operación financiera confirmada.
## Tabla 8
Jerarquía de Excepciones y Respuesta Operativa
ExcepciónERRMSGRespuesta operativa
LatchException——Base única; no se muestra
directamente.
AccesoDenegadoExceptionERR-009MSG-062Validar capacidad antes de
escribir y al iniciar tarea
diferida; denegar sin escrituras.
ArchivoExceptionERR-019
## ERR-050
## MSG-063
## MSG-047
Validar firma/tamaño o elegir
otro destino.
CargoAplicadoExceptionERR-026MSG-024Revertir dependencias
autorizadas antes de anular.
CargoIncompatibleExceptionERR-061MSG-084Rechazar cargo inexistente,
anulado o de otro contrato;
conservar propuesta. El exceso
sobre saldo mantiene su error
específico.

## 50
ExcepciónERRMSGRespuesta operativa
ConflictoConcurrenciaExceptionERR-052
## ERR-053
## MSG-078
## MSG-006
Comparar versión; reintento
solo con política D21.
ConflictoIdempotenciaExceptionERR-030MSG-075Conservar UUID y recuperar
solicitud original.
ConsultaReporteExceptionERR-048MSG-045Corregir contrato obligatorio
antes de consultar.
CuentaBloqueadaExceptionERR-008MSG-002Respetar plazo y revisar
desbloqueo autorizado.
CuotaFxExceptionERR-043MSG-077Respetar cuota y Retry-After;
conservar caché.
DependenciaActivaExceptionERR-011
## ERR-014
## ERR-016
## MSG-054
## MSG-008
## MSG-011
Conservar último Propietario o
dependencias activas.
DependenciaCreditoExceptionERR-033MSG-032Revertir aplicaciones posteriores
por orden inverso.
DuplicadoExceptionERR-010
## ERR-015
## ERR-060
## MSG-053
## MSG-010
## MSG-083
Corregir código; autorización
por fecha ya existente.
ExportacionExceptionERR-050
## ERR-051
## MSG-047
## MSG-048
No publicar temporal; elegir
destino o confirmar reemplazo.
FalloAccesoDatosExceptionERR-001
## ERR-002
## ERR-003
## ERR-025
## ERR-053
## ERR-054
## MSG-071
## MSG-006
## MSG-072
## MSG-060
## MSG-064
Redactar causa y distinguir
rollback de resultado incierto.
GarantiaInsuficienteExceptionERR-036MSG-035Releer remanente y rechazar
exceso.
GarantiaPendienteExceptionERR-062MSG-085Bloquear terminación hasta
liquidación completa y
autorizada, sin movimientos o
remanente pendientes; respetar
garantía inicial cero sin
movimientos (DC-08). La deuda
residual puede mantenerse.
IntegridadExceptionERR-063MSG-086Inconsistencia financiera antes
del commit: revertir todo y
conservar causa técnica. Mostrar
MSG-086 solo con rollback
confirmado; commit incierto
conserva ERR-031/ERR-054 y
## MSG-064.

## 51
ExcepciónERRMSGRespuesta operativa
OperacionCanceladaException—MSG-059
## (H1);
## MSG-089
## (H5)
H1 termina el contrato en curso
y detiene el lote antes del
siguiente; conserva confirmados
e identifica pendientes. H5
confirma cancelación al
terminar y limpiar temporales,
antes de publicar. Si ya publicó,
informa el resultado real;
conserva destino previo hasta
publicación exitosa y finanzas
confirmadas.
ParametroInvalidoExceptionERR-064MSG-051Rechazar días o montos
configurables negativos y
conservar formulario; otras
validaciones mantienen sus
mensajes específicos.
PeriodoDuplicadoExceptionERR-024MSG-060Omisión idempotente en
resumen de H1.
PlantillaExceptionERR-049MSG-046Conservar parámetros y repetir
generación.
PropiedadIncompatibleExceptionERR-065MSG-087Rechazar gasto si la solicitud de
mantenimiento pertenece a otra
propiedad; conservar selección
para corregirla.
RelojDesfasadoExceptionERR-057MSG-080Bloquear nuevas finanzas si
desfase absoluto supera 5 min;
no interrumpir transacción ya
iniciada.
ReporteExceptionERR-034
## ERR-049
## MSG-033
## MSG-046
Conservar pago y número;
repetir H5.
RespaldoExceptionERR-055MSG-079No publicar paquete con
integridad fallida.
RespuestaFxInvalidaExceptionERR-044
## ERR-045
MSG-043Rechazar contrato o poner valor
fuera de banda en cuarentena.
RestauracionExceptionERR-055
## ERR-058
## ERR-059
## MSG-079
## MSG-081
## MSG-082
Integridad, conciliación o
autenticidad; mantener
mantenimiento.
ResultadoInciertoExceptionERR-031
## ERR-054
MSG-064Consultar UUID con conexión
nueva antes de reenviar.
SaldoInsuficienteExceptionERR-029MSG-028Releer saldo bajo bloqueo y
reconfirmar.

## 52
ExcepciónERRMSGRespuesta operativa
ServicioExternoExceptionERR-038
## ERR-039
## ERR-040
## ERR-041
## ERR-042
## ERR-046
## MSG-042
## MSG-076
## MSG-043
## MSG-041
Conservar caché; aplicar
recuperación según causa.
TareasActivasExceptionERR-056MSG-061Mantener proceso abierto y
mostrar tarea pendiente.
TasaNoAutorizadaExceptionERR-028MSG-027Bloquear conversión y abrir
autorización por fecha.
TransicionInvalidaExceptionERR-066MSG-088Permitir Abierta a En proceso o
Cancelada, y En proceso a
Resuelta o Cancelada. Resuelta
y Cancelada son terminales;
repetir estado actual es
idempotente, sin duplicar
eventos. Rechazar otras
transiciones conservando estado.
UnidadReservadaExceptionERR-023MSG-022Revertir activación y releer
reserva de unidad.
ValidacionExceptionERR-012
## ERR-017
## ERR-018
## ERR-020
## ERR-021
## ERR-022
## ERR-027
## ERR-032
## ERR-035
## ERR-037
## ERR-047
## MSG-074
## MSG-013
## MSG-014
## MSG-019
## MSG-020
## MSG-021
## MSG-026
## MSG-031
## MSG-034
## MSG-037
## MSG-017
Corregir campo y conservar
datos no secretos.
Manejado en la interfazERR-004—La interfaz trata este estado sin
lanzar una excepción de
dominio.
Manejado en la interfazERR-005—La interfaz trata este estado sin
lanzar una excepción de
dominio.
Manejado en la interfazERR-006—La interfaz trata este estado sin
lanzar una excepción de
dominio.
Manejado en la interfazERR-007—La interfaz trata este estado sin
lanzar una excepción de
dominio.

## 53
ExcepciónERRMSGRespuesta operativa
Manejado en la interfazERR-013—La interfaz trata este estado sin
lanzar una excepción de
dominio.
Nota. Un ERR puede aparecer en más de una excepción según la capa. ERR-004,
ERR-005, ERR-006, ERR-007 y ERR-013 se manejan en la interfaz. C01 está resuelto por la
decisión aprobada del 25 de octubre de 2026, documentada en el Apéndice G del Plan de
implementación; Tabla 8 incorpora ERR-061 a ERR-066 y MSG-084 a MSG-089, con MSG-051
existente para ERR-064. La cancelación normal no tiene ERR. Las tareas H2/H3 auditan
incidentes de sistema según la lista cerrada de fn_auditar, sin crear códigos ERR ni MSG.
JUL emitirá SEVERE para fallos de confirmación, WARNING para condiciones
recuperables e INFO para inicio, fin y duración. FINE quedará limitado a desarrollo sintético, sin
secretos ni valores SQL sensibles. FileHandler rotará cinco archivos de diez MiB bajo un
directorio protegido. SLF4J usará slf4j-jdk14 y la API Log4j transitiva usará log4j-to-jul 2.25.4,
sin puentes inversos (QOS.ch, s. f.-b; Apache Software Foundation, s. f.-c). Cada entrada
incluirá instante, componente y correlación, con causas redactadas antes de escribir. La bitácora
financiera conservará su historia en PostgreSQL aunque roten los archivos locales de
diagnóstico.
Rendimiento y Capacidad
Volúmenes y Presupuestos
El entorno V1 del PRD supone Windows 11 x64, cuatro núcleos lógicos, ocho GiB de
RAM y SSD (Cuadra et al., 2026c). El conjunto sintético tendrá cien propiedades, quinientas
unidades, mil inquilinos, dos mil contratos, diez mil cargos, veinte mil pagos y cien mil eventos.
La selección representa una carga de prueba exigente para la escala académica y no una
medición de propietarios de Managua. Los ensayos LAN usarán tres sesiones contra el mismo

## 54
servidor y registrarán latencia previa para interpretar las diferencias. La Tabla 9 fija los
presupuestos por operación y las condiciones que hacen comparables las mediciones.
## Tabla 9
Presupuestos de Rendimiento y Método
OperaciónMeta AdoptadaCondición de MediciónReferencia
Abrir panel M11p95 de 2 s o menos30 aperturas, 10000 cargos y
base caliente; posición 29
entre 30 tiempos ordenados.
## RNF-001
Buscar inquilinosp95 de 1 s o menos30 búsquedas entre 1000
inquilinos, páginas de 50 filas.
## RNF-002
Generar renta H1500 cargos en 60 s
o menos
5 lotes sobre copias
equivalentes; sin otras
operaciones masivas.
## RNF-003
Exportar XLSX H510000 filas en 30 s
o menos
5 exportaciones, disco SSD y
sin otro trabajo masivo.
## RNF-004
Responder UIMáximo 200 ms20 pulsos por tarea H1, H2,
H3 y H5; reloj monotónico.
## RNF-020
Consultar auditoría20 eventos en 2 s o
menos
30 consultas por referencia
sobre 100000 eventos
indexados.
## RNF-035
Cambiar temaMáximo 500 ms30 cambios en cada sentido;
verificar 20 campos, filtros y
foco.
## RNF-049
Redimensionar y
navegar
p95 de 200 ms y
500 ms
30 ciclos entre 1366×768 y
1920×1080 y 30
navegaciones; dos temas y
dos modos de efectos.
Meta adicional de
presentación
Memoria de
escritorio
Heap usado de 512
MiB o menos tras
estabilizar
JFR durante 30 min, 100
navegaciones y 30 cambios de
tema; reportar pausas y
objetos retenidos.
Meta adicional, no
medición realizada
Respaldo y
restauración
15 min y 30 minBase sintética de 1 GiB y
adjuntos de 1 GiB; cero
hashes discordantes al
restaurar.
RNF-036 y
## RNF-037
Las consultas usarán paginación de cincuenta filas, índices por contrato, vencimiento,
período y referencias de auditoría, además de proyecciones para los indicadores. Los repositorios
evitarán cargar todo el historial para mostrar un saldo y limitarán la selección de relaciones al

## 55
detalle solicitado. El equipo registrará SQL y plan EXPLAIN ANALYZE solo sobre la base
sintética, con parámetros redactados en evidencia compartida. Los ensayos identificarán revisión
Git, versiones, equipo, fecha, estado de caché y operaciones concurrentes. Las comparaciones
usarán los mismos datos y separarán arranque frío de consultas calientes para evitar atribuir
cambios de entorno al código.
## Presupuesto Visual Aurora
AuroraBackground tendrá un máximo de tres luces estáticas por escena, dibujadas con
RadialGradient y sin animación ambiental permanente. Las celdas de TableView no crearán
efectos ni recalcularán fondos al actualizar importes; sus superficies permanecerán opacas y
uniformes. El redimensionado podrá reconstruir geometría decorativa una vez por pulso gráfico,
con reutilización de nodos y sin crear trabajos H1 a H6. La opción sin efectos eliminará el grupo
de luces en ambos temas y mantendrá contenido, acciones y foco. Estas reglas limitan el costo
inicial del diseño sin afirmar una mejora medida ni atribuir eficacia de uso al estilo visual.
La medición de navegación comenzará al aceptar la acción y terminará cuando la vista
aplique CSS y layout en el hilo JavaFX. El ensayo de redimensionado registrará pulsos y
demoras del hilo gráfico, con JFR para observar asignaciones y memoria retenida. El equipo
comparará las cuatro combinaciones de Claro u Oscuro y efectos activos o inactivos, sin alterar
filtros ni importes de referencia. Si el presupuesto se incumple, la configuración iniciará con
fondo simplificado de color uniforme del tema. RNF-056 seguirá exigiendo cero luces al elegir
sin efectos y ningún control podrá desaparecer por esa elección.

## 56
Estrategia de Pruebas Técnicas
Niveles y Entorno de Prueba
Las pruebas unitarias cubrirán reglas, cálculo decimal, permisos y transiciones mediante
JUnit 5.12.2, que admite Java 21 (Bechtold et al., s. f.). La integración utilizará una instancia
PostgreSQL 18.6 aislada, creada con las mismas migraciones y una cuenta de prueba sin acceso
a la base demostrable. Los escenarios de aceptación recorrerán JavaFX con teclado, estados de
error, reportes y tareas concurrentes. JaCoCo 0.8.13 medirá una meta del ochenta por ciento de
líneas en dominio y aplicación, sin convertir cobertura en una afirmación de corrección (JaCoCo,
s. f.). Los treinta y ocho RN deberán tener al menos un escenario aceptado o rechazado
verificable.
El servidor simulado de tasas usará HttpServer de JDK 21 en loopback y un puerto
efímero, con respuesta JSON sintética controlada por escenario. La configuración de producción
mantendrá HTTPS y host permitido, mientras el perfil de pruebas habilitará HTTP local sin
relajar esa regla en el paquete. Las pruebas de tiempo usarán un proveedor inyectado de hora de
BD para gracia, vencimiento y fecha financiera, y Clock controlado para comparación local y
caché; las de concurrencia emplearán CountDownLatch para alinear clientes, con espera
limitada. La Tabla 10 define escenarios técnicos con procedimiento y resultado esperado. Se
trata de un plan de ejecución, sin resultados funcionales obtenidos en una implementación de
Latch. PT se conserva como escenario de diseño; su columna CP equivalente remite a los
identificadores de ejecución CP-001 a CP-100 del Plan. Las ampliaciones se ejecutarán como
variantes de esos CP.

## 57
## Tabla 10
Escenarios Técnicos y Criterios de Aceptación
IDEscenario y Datos
## Sintéticos
ProcedimientoResultado Esperado y
## Umbral
Resp.CP equivalente
PT-01Hash y bloqueo con 5
cuentas, reloj fijo y 6
intentos.
Crear cuentas, inspeccionar
parámetros y provocar 5 fallos;
intentar acceso antes y después
de 15 min.
100 % Argon2id m=19456
KiB, t=2, p=1; bloqueo al
quinto y desbloqueo según
reloj.
StevenCP-002 a CP-004
PT-02Permisos de
Asistente, 20 acciones
exclusivas.
Invocar UI y servicios para
anular, dar baja, cambiar
parámetros y gestionar usuarios.
Revalidar capacidad y sesión
cuando una tarea diferida
empiece a operar.
20 rechazos y cero escrituras;
cambio de tema sí permitido
en ambos roles. Denegación
ERR-009 / MSG-062 también
en servicio y tarea diferida,
según la matriz vigente.
StevenCP-005
PT-03Activaciones
simultáneas y renta
repetida, 100 intentos.
Alinear pares por unidad y
lanzar inserciones repetidas por
contrato y período.
Máximo una activación
compatible y cero rentas
duplicadas.
AlfredoCP-016, CP-021,
## CP-045, CP-052
## (M04)
PT-04Dos variantes con
diez emisores (hilos)
en dos JVM: a) 100
pagos confirmados y
diez solicitudes con
rollback; b) 100 pagos
confirmados y diez
anulaciones
posteriores.
Alinear diez emisores en dos
JVM y comenzar en 1. En a),
inyectar rollback antes del
commit en diez solicitudes
adicionales; en b), anular diez de
los cien pagos confirmados. El
máximo de tres sesiones LAN
limita clientes de aplicación, no
hilos de prueba.
En ambas variantes: 100
recibos, cero duplicados y
siguiente_numero=101. En a),
diez rollback no consumen
número; en b), 90 pagos
Registrado y 10 Anulado
conservan sus cien números e
historial.
AlfredoCP-041, CP-042 y
CP-032 (variantes
## D22)
PT-0520 fallos en puntos
financieros y
desconexión
poscommit.
Interrumpir después de pago,
aplicación, contador, recibo o
auditoría; consultar UUID tras
pérdida de respuesta.
Cero operaciones parciales;
resultado incierto recupera el
mismo recibo, sin segundo
pago.
StevenCP-042 a CP-044,
## CP-048

## 58
IDEscenario y Datos
## Sintéticos
ProcedimientoResultado Esperado y
## Umbral
Resp.CP equivalente
PT-06H1 y H2 compiten
con cancelación a
mitad de 500
contratos.
Iniciar ambas tareas, cancelar
entre contratos y reanudar con el
mismo período.
Cargos previos conservados,
ninguno parcial, renta y mora
sin duplicados; H1 completo
en 60 s o menos.
StevenCP-021, CP-022,
CP-045 a CP-047,
## CP-099
PT-0720 JSON inválidos,
banda técnica 1–100
## NIO/USD, 408, 429,
5xx y timeout.
Programar respuestas sin NIO,
base distinta, fecha inválida y
pausas de 6 o 11 s; simular 72 h.
Probar límites 1 y 100, valores
fuera de banda y cuarentena
## M09
## REFERENCIA_EN_CUAREN
TENA, sin sustituir referencia
vigente. Simular HTTP 408
como timeout transitorio y 429
con Retry-After, sin promover la
referencia comercial a
autorización de pago; probar
tasa manual fuera de banda con
revisión y confirmación del
## Propietario.
20 rechazos de esquema;
conexión 5 s y operación 10 s;
una consulta diaria salvo un
reintento de red, 5xx o HTTP
408 tras 60 s. Límites
inclusivos aceptados; valores
fuera de banda con valor y
huella en bitacora. HTTP 429
respeta Retry-After y cuota;
caché histórica permanece
ante rechazo o cuarentena. La
tasa manual no recibe una
restricción de banda en BD.
ArisCP-053 a CP-065
(CP-058 con
variantes D23)
PT-08Diez reportes con
tildes, montos y
anulación.
Compilar con Library 7.0.8,
llenar Dto y exportar 10 PDF y
9 XLSX; recibo solo PDF.
Comprobar visor integrado en
PAN-11/PAN-16, sin Imprimir
ni Ctrl+P.
19 archivos legibles; cero
errores de enlace; monedas
separadas, DejaVu Sans y
recibo no fiscal; 10000 filas
XLSX en 30 s.
ArisCP-067 a CP-076,
## CP-086, CP-100
PT-09Doce módulos, dos
temas y dos modos de
efectos.
Recorrer 10 flujos por teclado;
escalar 200 % en 1920x1080;
medir foco y contrastes de
estados.
Cero bloqueos y recortes;
texto 4.5:1, controles 3:1, foco
de 2 px y cambio de tema en
500 ms.
CalebCP-077 a CP-084,
CP-087, CP-092 a
## CP-094

## 59
IDEscenario y Datos
## Sintéticos
ProcedimientoResultado Esperado y
## Umbral
Resp.CP equivalente
PT-10Dos perfiles locales
con preferencia
inválida o acceso
denegado.
Alternar cuentas y reiniciar 10
veces por cuenta; simular lectura
y escritura fallidas.
## Restauración 10/10, Claro
ante lectura fallida y un aviso
no bloqueante por fallo de
guardado.
CalebCP-089 a CP-091
PT-11Base y adjuntos de 1
GiB cada uno, H1 en
curso.
Solicitar respaldo con H1 activo;
verificar exclusión y espera de
escrituras iniciadas, coherencia
de base/adjuntos/manifiesto y
ausencia de credenciales y
claves. Con clientes detenidos,
validar paquete y restaurar
aislado antes de sustituir
instalación; comprobar hashes,
contraseña y autenticidad GCM.
Simular recibos posteriores al
respaldo y mantener
mantenimiento hasta conciliar,
sin reutilizar ni renumerar.
Metas pendientes de medición
en entorno identificado:
respaldo en 15 min y
recuperación en 30 min con
base y adjuntos de 1 GiB cada
uno; cero hashes discordantes
y ningún secreto en paquete.
No acredita ejecución ni
resultados.
AlfredoCP-051, CP-095
## (variantes D37)
PT-12Cierre durante H1,
H2, H3, H4 y H5.
Solicitar salida en cada fase,
simular espera de DB e
inspeccionar futuros y recursos.
En 10 s finalizan o se informa
bloqueo y proceso abierto;
ningún commit partido ni
conexión cerrada en uso.
StevenCP-049, CP-050
(variantes por H)
PT-13Fixture V1 y 3
sesiones LAN, 50
conflictos.
Medir panel, búsqueda,
auditoría y escrituras cruzadas;
registrar latencia de red.
Umbrales de Tabla 9 y cero
escrituras perdidas; 30
observaciones para p95.
CalebCP-052, CP-098,
## CP-099;
búsqueda/auditoría
como variantes de
## CP-040

## 60
IDEscenario y Datos
## Sintéticos
ProcedimientoResultado Esperado y
## Umbral
Resp.CP equivalente
PT-1438 reglas, 100 sumas
y 20 anulaciones.
Ejecutar casos por RN, probar
HALF_EVEN y dependencias
de crédito antes de anular.
Incluir como variante reloj local
a más de 5 min de BD:
ERR-057/MSG-080 y cero
nuevas finanzas. El control no
interrumpe finanzas ya iniciadas
ni sustituye la validación de
fecha efectiva; comprobar
pago.fecha_pago y
pago.creado_en por separado.
100 % de RN con prueba;
cero centavos de diferencia y
20 números conservados.
AlfredoCP-015 a CP-040;
CP-040 con variante
## D24
PT-15Paquete en dos
cuentas Windows sin
## JDK.
Descomprimir app-image,
iniciar, conectar con UTF8 y
guardar 20 registros acentuados.
Inicio en ambos destinos y 20
lecturas idénticas; una orden
verify en dos entornos
limpios.
ArisCP-096 y CP-097
PT-16Sesión de 8 h con
1000 operaciones y
10 CRUD sin
## Internet.
Mantener PostgreSQL, cortar
HTTP durante 30 min y registrar
incidentes con secretos señuelo.
Cero cierres inesperados; 10
CRUD locales y cero secretos
en logs; cobertura objetivo 80
## %.
CalebCP-007, CP-054,
## CP-066, CP-096
(variantes de
estabilidad/cobertura
## )

## 61
Cobertura, Evidencia y Responsables
El equipo conservará informes de JUnit y JaCoCo, SQL de preparación, fixtures y
capturas bajo una carpeta de evidencia identificada por revisión y fecha. Cada ejecución
registrará versión del paquete, entorno, procedimiento y comparación entre resultado esperado y
observado, sin reutilizar un informe de otra revisión. Los fallos de integridad, permisos,
duplicación de recibos o fuga de secretos bloquearán la entrega, aunque el porcentaje de
cobertura alcance la meta. El Plan de implementación fija fechas y procedimientos de CP-001 a
CP-100; la Tabla 10 conserva PT como escenarios de diseño y explicita sus equivalencias
(Cuadra et al., 2026f). La presente revisión documental no acredita ejecuciones funcionales.
La asignación nominal, el relevo y la revisión cruzada se rigen por las Tablas 4 y 5 del
Plan y su apartado Revisión Cruzada y Decisión Técnica (Cuadra et al., 2026f). La Tabla 11
remite a esa matriz única; no establece otra asignación local. El autor no será el único aprobador
de su cambio y la revisión seguirá los responsables de cada TAR. R identifica ejecución, A
coordinación de aceptación interna, C consulta e I información; ninguna asignación acredita
horas realizadas ni aprobación académica. Para H4, el Plan asigna a Steven la sección crítica y el
bloqueo local, y a Alfredo el contador, la transacción única, la numeración y TAR-038.
## Tabla 11
Remisión a la Matriz RACI y Revisión Cruzada del Plan
ContenidoFuente canónicaAplicación en el TRD
Responsabilidad y
relevo
Plan Tabla 4Usar el responsable y relevo nominal de
la fuente.
Matriz RACI únicaPlan Tabla 5Una A por entregable; cada TAR
conserva responsable propio.
Revisión cruzada y
decisión técnica
Plan apartado del mismo
nombre
Aplicar ese mapa; autor sin aprobación
única de su cambio.

## 62
Configuración, Despliegue y Operación
Requisitos e Inicialización
El equipo académico objetivo usará Windows 11 x64, cuatro núcleos lógicos, ocho GiB
de RAM y SSD, con diez GiB libres para datos y herramientas. La variante servidor LAN
supondrá cuatro núcleos, dieciséis GiB de RAM, veinte GiB libres y acceso restringido para tres
clientes en una red privada. Estas capacidades son supuestos de instalación que deberán
contrastarse con el equipo usado en PT-15, sin afirmar un inventario local no inspeccionado. El
paquete incluirá runtime Java 21 y JavaFX; PostgreSQL 18.6 y sus herramientas se instalarán
como dependencia separada. Internet será necesario para actualizar la referencia comercial, no
para ejecutar CRUD local.
La instalación usará los roles latch_owner, latch_migrator, latch_runtime y latch_backup
con los privilegios del Esquema (Cuadra et al., 2026e). El migrador aplicará V001__modelo.sql,
V002__objetos.sql, V003__catalogos.sql y V004__privilegios.sql y registrará versión, checksum
e instante en esquema_migracion; T001 y R001 serán scripts de prueba y reportes. La fila de
consecutivo_recibo tendrá UUID 00000000-0000-0000-0000-000000000001. Hibernate iniciará
con hbm2ddl.auto=validate y rechazará esquemas incompatibles. El aprovisionamiento local
creará el primer Propietario con contraseña ingresada y hash salado, sin una contraseña fija
distribuida.
Configuración Externa y Paquete
La configuración no sensible fijará host, puerto, base, directorio de adjuntos, nivel de
diagnóstico y consultor de tasas designado. Las credenciales se leerán desde un archivo externo
con ACL restringida y los secretos podrán sobreponerse mediante variables del proceso. Los
parámetros de negocio tendrán historial en PostgreSQL y solo el Propietario podrá cambiarlos; el

## 63
tema y la modalidad sin efectos permanecerán en Preferences por usuario. Una configuración
inválida mostrará la clave afectada sin imprimir su valor secreto y no abrirá una sesión
incompleta. El Apéndice A incluye un ejemplo de arranque programático con DataSource
compartido, sin credenciales reales.
jpackage de JDK 21 generará app-image en Windows, con el lanzador, dependencias y
runtime empaquetados en un ZIP versionado (Oracle, s. f.-c). La aplicación usará classpath para
sus bibliotecas y mantendrá los módulos JavaFX necesarios en el runtime, sin exigir modularizar
Hibernate y Jasper como condición de entrega. El proceso conservará LICENSES, THIRD-
PARTY-NOTICES, revisión Git y huella del archivo publicado. La instalación por extracción
evitará dependencia de WiX, y un acceso directo podrá apuntar al lanzador incluido. Las pruebas
de distribución comprobarán dos cuentas sin JDK previo; no se afirma firma digital ni
producción de un instalador MSI.
Respaldo, Restauración y Mantenimiento
El respaldo será manual diario con alerta tras veinticuatro horas desde el último conjunto
válido. pg_dump en formato personalizado y pg_restore copiarán y recuperarán la base (The
PostgreSQL Global Development Group, s. f.-b). La coordinación de base y adjuntos seguirá el
Esquema: cada escritura invocará fn_puerta_escritura() para adquirir FOR SHARE sobre
latch.puerta_mantenimiento, UUID 00000000-0000-0000-0000-000000000002. El respaldo
abrirá BEGIN en una conexión coordinadora y tomará FOR UPDATE sobre la misma fila, con
respaldo_lock_timeout_ms=3000 y SET LOCAL lock_timeout='3000ms'. Mantendrá esa
transacción mientras otro proceso ejecuta pg_dump, copia adjuntos y valida el paquete; liberará
la puerta mediante commit o rollback incluso si falla. Sin exclusión o validación no se publicará

## 64
un respaldo válido. La exclusión espera a que finalicen las escrituras ya iniciadas y mantiene
coherentes la base, los adjuntos y el manifiesto.
El respaldo contendrá dump, adjuntos, manifest.json, esquema, revisión y SHA-256 de
cada archivo, sin credenciales ni claves. Los quince minutos de respaldo y treinta minutos de
recuperación, con un GiB de base y uno de adjuntos, son metas de aceptación pendientes de
medición en un entorno identificado; no se presume un volumen real ni una prueba ejecutada. La
restauración exigirá clientes detenidos, mantenimiento, copia previa, validación del paquete y
restauración aislada, con verificación de hashes y autenticidad GCM antes de sustituir la
instalación. El operador contrastará contador y recibos con el último historial disponible y
mantendrá el mantenimiento hasta conciliar si existen recibos posteriores al respaldo, sin
reutilizar ni renumerar consecutivos. La pérdida objetivo de veinticuatro horas depende de
ejecutar el respaldo diario y no equivale a recuperación sin pérdida. La restauración usará una
cuenta administrativa del clúster con CREATEDB, externa a los roles de Latch. Una falla de
autenticidad o contraseña producirá RestauracionException, ERR-059 / MSG-082, antes de
publicar archivos.
El archivo de respaldo conservará siete conjuntos diarios y cuatro semanales, según la
retención técnica del PRD. Latch concretará AES-256 mediante Cipher con
AES/GCM/NoPadding, cuya API exige un IV distinto para cada cifrado (Oracle, s. f.-a). La
herramienta integrada derivará la clave de una contraseña de respaldo con PBKDF2-HMAC-
SHA256, seiscientas mil iteraciones y sal aleatoria de dieciséis bytes; usará IV de doce bytes y
etiqueta de ciento veintiocho bits. El encabezado guardará parámetros no secretos y versión de
formato. La aplicación verificará autenticidad antes de publicar archivos restaurados y retirará
temporales en claro después de cerrar el paquete.

## 65
Actualización y Desinstalación
Cada actualización detendrá clientes, verificará respaldo, aplicará migraciones
compatibles y ejecutará una comprobación de apertura y consulta antes de admitir escrituras. El
paquete guardará la versión anterior de binarios en un directorio separado; una reversión de
esquema requerirá restaurar el conjunto completo validado y no copiar solo ejecutables antiguos.
Los adjuntos y la configuración permanecerán fuera de la carpeta de programa para facilitar
sustitución de versión. La desinstalación retirará binarios y accesos directos, pero conservará
base, respaldos y archivos salvo solicitud expresa de eliminación. El procedimiento de retiro
dejará un inventario de ubicaciones para que el propietario de la instalación controle sus datos.
El calendario único es el del Plan de implementación, Apéndice A y Tabla 8 (Cuadra et
al., 2026f). TAR-001 a TAR-096 suman 228 h: 32 + 40 + 40 + 40 + 44 + 32. Los 41 días del 22
de octubre al 1 de diciembre aportan capacidad estimada de 48 × 41/7 = 281,142857 h; la reserva
es 53,142857 h, equivalente al 18,902439 % de la capacidad. Por integrante corresponden 57 h
programadas y 70,285714 h de capacidad. La Tabla 12 usa los rangos TAR y redondea
únicamente la presentación. Ninguna cifra acredita trabajo realizado ni disponibilidad
confirmada.
## Tabla 12
Hitos Técnicos y Capacidad Propuesta
Período e hitoRango TAR y dependenciaHoras
programadas
Capacidad y reserva
## Sprint 0 · 22–
## 27/10
TAR-001 a TAR-016;
## Fundamentos. Dependencias
en Plan Apéndice A.
32 hCapacidad 41.142857
h; reserva 9.142857 h.
## Sprint 1 · 28/10–
## 3/11
TAR-017 a TAR-032;
Dominio y ORM.
Dependencias en Plan
## Apéndice A.
40 hCapacidad 48.000000
h; reserva 8.000000 h.

## 66
Período e hitoRango TAR y dependenciaHoras
programadas
Capacidad y reserva
Sprint 2 · 4–10/11TAR-033 a TAR-048;
Finanzas. Dependencias en
## Plan Apéndice A.
40 hCapacidad 48.000000
h; reserva 8.000000 h.
## Sprint 3 · 11–
## 17/11
TAR-049 a TAR-064;
Reportes y operación.
Dependencias en Plan
## Apéndice A.
40 hCapacidad 48.000000
h; reserva 8.000000 h.
## Sprint 4 · 18–
## 25/11
TAR-065 a TAR-080;
Integración y regresión.
Dependencias en Plan
## Apéndice A.
44 hCapacidad 54.857143
h; reserva 10.857143
h.
## Cierre · 26/11–
## 1/12
TAR-081 a TAR-096; Entrega
y cierre. Dependencias en Plan
## Apéndice A.
32 hCapacidad 41.142857
h; reserva 9.142857 h.
Total · 41 díasTAR-001 a TAR-096228 h281.142857 h; reserva
53.142857 h
## (18.902439 %).
Control de Versiones y Convenciones de Código
El repositorio mantendrá main protegida y ramas feature/TAR-nnn-descripcion-corta o
fix/DEF-nnn-descripcion-corta. Los commits seguirán feat|fix|test|docs|build: texto (TAR-nnn).
Cada integración incluirá revisión distinta del autor, resultado de verify y vínculo al requisito o
ADR. Las etiquetas serán v0.1.0, v0.2.0, v0.3.0, v0.4.0, v0.9.0-rc.1 para la candidata del 25 de
noviembre y v1.0.0. Un manifiesto conservará el hash del commit; el calendario y la revisión
cruzada remiten al Plan (Cuadra et al., 2026f). Las dependencias cambiarán de forma explícita
bajo revisión. Cada incremento identificará versión, fuentes, esquema compatible, documentos,
respaldo y huellas; un cambio de alcance o configuración registrará motivo, impacto y
responsable (D44). Las observaciones de revisión cruzada se clasificarán como defecto,
aclaración o cambio de alcance, con responsable y resolución; una revisión documental o
actividad programada no acredita pruebas ejecutadas (D45).

## 67
Las clases seguirán cuatro espacios de sangría y archivos UTF-8, con nombres de
negocio completos en español sin tildes y sufijos de rol estándar en inglés según D02. No se fijan
aún límites de longitud ni complejidad; corresponderán al documento de convenciones. Javadoc
documentará contratos públicos, unidades monetarias, errores y requisitos de hilo cuando esa
información no resulte visible en la firma. Los comentarios explicarán una restricción de negocio
o concurrencia y evitarán repetir lo que una instrucción ya expresa. El equipo prohibirá lógica de
negocio dentro de listeners FXML, campos estáticos mutables para sesión y captura silenciosa de
excepciones. La compilación advertirá APIs obsoletas y la revisión verificará cierres mediante
try-with-resources, parámetros SQL y conservación de causas al traducir errores.
Trazabilidad de Requisitos No Funcionales
La Tabla 13 mantiene los sesenta códigos del PRD y vincula sus umbrales con
mecanismos y pruebas propuestos (Cuadra et al., 2026c). La agrupación por categoría conserva
seguridad, usabilidad, integridad, concurrencia, localización y recuperación sin sustituir los
identificadores originales. Los RNF visuales de la ampliación se relacionan con el mismo
ThemeService y con cuatro combinaciones de presentación, sin crear un tercer tema. Cada fila
indica una evidencia esperada; la matriz no acredita que las pruebas hayan ocurrido ni que el
producto haya sido aceptado. Los mecanismos transaccionales y de autorización permanecen
exigibles aunque la interfaz o un reporte cambien de apariencia. El cierre relacionará requisitos,
tareas, casos, resultados y evidencias RA1–RA5; los criterios obligatorios requieren evidencia y
toda excepción, justificación y aceptación expresa. Se distinguirán entrega documental,
implementación y validación del producto, con pendientes aceptados, versiones y huellas
identificados (D47).

## 68
## Tabla 13
Trazabilidad Individual de los Sesenta RNF
IDCategoríaUmbral del PRDMecanismo TécnicoPrueba
RNF-001RendimientoPanel p95 ≤ 2 s, 10000
cargos.
Agregados JDBC, índices y
DTO; 30 aperturas.
## PT-13
RNF-002RendimientoBúsqueda p95 ≤ 1 s,
1000 inquilinos.
Paginación de 50 filas y filtro
parametrizado.
## PT-13
RNF-003Rendimiento500 cargos H1 ≤ 60 s.Una transacción por contrato y
omisión idempotente.
## PT-06
RNF-004RendimientoXLSX 10000 filas ≤ 30
s.
H5 y proyección de datos sin
entidades diferidas.
## PT-08
RNF-005Seguridad100 % hashes
Argon2id, 19 MiB, t=2,
p=1.
Servicio de credenciales con sal
de 16 bytes.
## PT-01
RNF-006Seguridad20 de 20 acciones
exclusivas denegadas a
## Asistente.
Capacidades verificadas en cada
servicio.
## PT-02
RNF-007SeguridadBloqueo al quinto fallo
durante 15 min.
Contador transaccional y Clock
inyectado.
## PT-01
RNF-008SeguridadCero contraseñas o
secretos en logs.
Redacción de causas y
cabeceras; señuelos.
## PT-16
RNF-009Usabilidad10 recorridos por
teclado, cero bloqueos.
Orden de foco, Enter, Escape y
acciones accesibles.
## PT-09
RNF-010UsabilidadSin recortes a 200 % en
## 1920x1080.
Layouts redimensionables y
scroll de formularios.
## PT-09
RNF-011UsabilidadTexto normal 4.5:1 y
grande 3:1.
Tokens semánticos y superficies
uniformes.
## PT-09
RNF-012Usabilidad20 errores identifican
campo y causa.
Validadores de formulario y
resumen de errores.
## PT-09
RNF-013Confiabilidad20 fallos antes de
commit, cero pagos
parciales.
Una transacción ORM/JDBC y
rollback completo.
## PT-05
RNF-014Disponibilida
d
8 h y 1000 operaciones
sin cierre inesperado.
Gestión de errores y recursos
con seguimiento de memoria.
## PT-16
RNF-015Disponibilida
d
10 CRUD durante 30
min sin HTTP.
Base local y separación de
proveedor externo.
## PT-16
RNF-016Integridad100 intentos, cero
rentas duplicadas.
Restricción única
contrato/período para Renta.
## PT-03
RNF-017IntegridadCero centavos de
diferencia en 100
combinaciones.
BigDecimal, HALF_EVEN y
remanente final.
## PT-14

## 69
IDCategoríaUmbral del PRDMecanismo TécnicoPrueba
RNF-018Integridad100 intentos sin
contratos activos
superpuestos.
Bloqueo de unidad y restricción
de ocupación.
## PT-03
RNF-019Integridad20 anulaciones
conservan números.
Anulación lógica y restricciones
del recibo.
## PT-14
RNF-020ConcurrenciaLatencia JavaFX ≤ 200
ms durante H1, H2, H3
y H5.
Ejecutores acotados y
actualizaciones agregadas.
## PT-06,
## 13
RNF-021Concurrencia100 recibos
simultáneos sin huecos
ni duplicados.
Lock local y contador bloqueado
en mismo commit.
## PT-04
RNF-022ConcurrenciaH6 finaliza en 10 s o
informa bloqueo.
Cancelar productores, esperar
transacciones y conservar
proceso.
## PT-12
RNF-023ConcurrenciaMáximo un H1, H2, H3
y H5 por aplicación.
Service y banderas atómicas por
tipo de tarea.
## PT-06,
## 12
RNF-024Mantenibilida
d
Una orden Maven en
dos equipos limpios.
Wrapper, versiones fijadas y
perfil verify.
## PT-15
RNF-025Mantenibilida
d
100 % pruebas de las
38 RN aprobadas.
Suite parametrizada y
trazabilidad de reglas.
## PT-14
RNF-026Mantenibilida
d
Cero ciclos entre cuatro
capas.
Inspección de imports y grafo de
paquetes.
## PT-15
RNF-027Compatibilid
ad
Dos cuentas Windows
11 sin JDK previo.
app-image con runtime Java 21
y JavaFX 21.
## PT-15
RNF-028Compatibilid
ad
20 registros acentuados
idénticos en
PostgreSQL 18.
UTF8 y pgJDBC 42.7.14.PT-15
RNF-029Compatibilid
ad
Tres sesiones LAN y
cero pérdidas en 50
conflictos.
@Version, bloqueo financiero y
autoridad de base.
## PT-13
RNF-030Localización100 % etiquetas y
validaciones en
español.
ResourceBundle es_NI y
catálogo de cadenas.
## PT-09
RNF-031Localización10 reportes con
dd/MM/yyyy y NIO o
## USD.
Parámetros de locale y columnas
separadas por moneda.
## PT-08
RNF-032Localización20 eventos UTC
mostrados en
America/Managua.
Instant, TIMESTAMPTZ y
conversión en presentación.
## PT-14
RNF-033Auditoría100 eventos por 100
operaciones
financieras.
INSERT JDBC con la conexión
del commit.
## PT-05

## 70
IDCategoríaUmbral del PRDMecanismo TécnicoPrueba
RNF-034Auditoría10 de 10 ediciones
denegadas a ambos
roles.
Servicio sin edición y
privilegios SQL sin
## UPDATE/DELETE.
## PT-02
RNF-035Auditoría20 eventos en ≤ 2 s con
100000 eventos.
Índice referencia/instante y
paginación.
## PT-13
RNF-036Respaldo1 GiB base más 1 GiB
adjuntos en ≤ 15 min.
Exclusión global, pg_dump y
manifiesto.
## PT-11
RNF-037RecuperaciónDos GiB en ≤ 30 min,
cero hashes
discordantes.
Restauración aislada y cotejo
## SHA-256.
## PT-11
RNF-038RecuperaciónPérdida objetivo ≤ 24
h.
Respaldo manual diario y alerta
de antigüedad.
## PT-11
RNF-039IntegraciónConexión 5 s, solicitud
completa 10 s.
connectTimeout y plazo total del
futuro HTTP.
## PT-07
RNF-040IntegraciónUna consulta cada 24 h
salvo reintento
autorizado.
Caché persistente, concesión y
presupuesto común.
## PT-07
RNF-041Integración20 respuestas inválidas
rechazadas.
DTO con base USD, NIO
positivo y fechas válidas.
## PT-07
RNF-042Integración20 tasas de pago
intactas tras 20
cambios de caché.
Snapshot inmutable en cada
pago.
## PT-07,
## 14
RNF-043Cumplimient
o
Finalidad y base en 100
% expedientes reales.
Campo de procedencia y puerta
de carga real; demostración
sintética.
## PT-14
RNF-044Cumplimient
o
Cinco solicitudes
simuladas en un día
hábil.
Procedimiento interno y
exportación autorizada.
## PT-14
RNF-045Cumplimient
o
Avisos en 100 % de 10
PDF aplicables.
Plantillas con recibo no fiscal y
origen de tasa.
## PT-08
RNF-046Cumplimient
o
100 % dependencias
con autor, licencia y
versión.
Inventario de distribución y
avisos de terceros.
## PT-15
RNF-047UsabilidadExactamente dos temas
en 12 módulos y
diálogos.
ThemeService, claro.css y
oscuro.css.
## PT-09
RNF-048UsabilidadClaro en 6 de 6 inicios
sin preferencia válida.
Adaptador Preferences con valor
seguro de lectura.
## PT-10
RNF-049UsabilidadCambio ≤ 500 ms,
conserva 20 campos y
filtros.
Cambio de stylesheet sin
reconstruir FXML ni perder
foco.
## PT-09

## 71
IDCategoríaUmbral del PRDMecanismo TécnicoPrueba
RNF-050UsabilidadPreferencia correcta en
10 reinicios por dos
cuentas.
Último tema local y clave de
usuario por UUID.
## PT-10
RNF-051UsabilidadContraste de controles
necesarios ≥ 3:1.
Tokens de borde, símbolo,
selección y error.
## PT-09
RNF-052Usabilidad100 % controles con
foco de 2 px y 3:1.
Regla :focused y foco no oculto;
meta propia inspirada en AAA.
## PT-09
RNF-053Usabilidad10 recorridos en 4
combinaciones, cero
bloqueos.
Claro/Oscuro y efectos
activos/inactivos
independientes.
## PT-09
RNF-054Usabilidad100 % datos y permisos
tras 20 cambios.
Tema desacoplado del dominio
y comparación de DTO.
## PT-09
RNF-055UsabilidadSegoe UI y superficies
opacas en 100 % de
controles.
Stylesheet común y paneles sin
transparencia bajo datos.
## PT-09
RNF-056UsabilidadCero luces y blur al
elegir sin efectos.
Retirar grupo decorativo sin
cambiar contenido.
## PT-09
RNF-057ConcurrenciaCero hilos para luces;
tema solo en JavaFX.
RadialGradient estático y
comprobación del hilo gráfico.
## PT-09
RNF-058UsabilidadCambio conservado
una sesión y un aviso
por falla.
Estado en memoria y
deduplicación del aviso local.
## PT-10
RNF-059Mantenibilida
d
Cero preferencias en
dominio y cero claves
con nombre o cédula.
Almacén local por UUID opaco,
fuera de PostgreSQL.
## PT-10,
## 15
RNF-060Mantenibilida
d
Cero bibliotecas
Aurora y dos hojas bajo
servicio común.
Recursos nativos, métricas
compartidas y prueba de tokens
iguales.
## PT-09,
## 15
## Riesgos Técnicos
La Tabla 14 registra riesgos de la solución con probabilidad e impacto cualitativos
propuestos para orientar el trabajo del equipo. Alta, media y baja describen juicio de diseño y no
frecuencias calculadas a partir de incidentes de Latch. Cada fila identifica prevención y una
contingencia que conserva integridad o permite detener la operación con explicación suficiente.
El responsable revisará el riesgo cuando cambie una dependencia o falle el caso técnico
relacionado, sin dar por desaparecido un riesgo porque exista una prueba escrita. La revisión de
cierre conservará los riesgos residuales que dependan de respaldo manual, conectividad o

## 72
confianza en estaciones locales. Los identificadores locales RST-01 a RST-16 y su valoración
técnica cualitativa remiten al registro canónico del Plan de implementación, RSK-01 a RSK-18,
con escala de 1 a 5; no lo sustituyen.

## 73
## Tabla 14
Registro de Riesgos Técnicos y Contingencias
IDRiesgoProb.ImpactoMitigaciónContingenciaResp.
RST-01Jasper y Hibernate
cargan familias
incompatibles.
MediaAltoDTO JavaBeans; excluir
adaptador ORM de Jasper;
revisar árbol Maven.
Bloquear paquete y
regenerar plantillas con
dependencias fijadas.
## Aris
RST-02Dos pagos consumen el
mismo consecutivo.
MediaAltoReentrantLock local,
consecutivo_recibo UUID
fijo, fn_reservar_numero(),
## Recibo Hibernate
@Immutable y prueba
## PT-04 / CP-041–042.
Rollback y consulta de
UUID; suspender emisión si
falla el invariante.
## Alfredo
RST-03Conexión cae después
del commit y el usuario
repite.
MediaAltoUUID y huella de solicitud;
estado de resultado incierto.
Recuperar resultado
existente antes de habilitar
otro envío.
## Alfredo
RST-04H1 o reporte congela
JavaFX.
MediaAltoEjecutores acotados, DTO y
cero esperas bloqueantes en
## UI.
Cancelar trabajo
cooperativamente y
conservar datos
confirmados.
## Caleb
RST-05Proveedor cambia cuota,
contrato o
disponibilidad.
MediaMedioCaché diaria, presupuesto
100/mes y cliente aislado.
Mantener referencia
histórica y exigir tasa
manual autorizada.
## Aris
RST-06Tasa comercial se usa
como oficial o de otra
fecha.
MediaAltoSeparar referencia y
autorización; snapshot y
fecha local.
Rechazar conversión antes
de commit; corregir con
nueva operación trazable.
## Steven
RST-07Reporte cambia al
reimprimir un pago
antiguo.
MediaAltoSnapshot de emisor, tasa,
aplicaciones y número.
Reimprimir solo datos
históricos; marcar anulación
sin recalcular importe.
## Aris

## 74
IDRiesgoProb.ImpactoMitigaciónContingenciaResp.
RST-08Restore histórico
reutiliza un recibo
emitido.
MediaAltoMantenimiento, manifiesto
y conciliación previa a
habilitar cobros.
Conservar solo lectura hasta
recuperar o conciliar el
historial faltante.
## Alfredo
RST-09Archivo adjunto
malicioso o ruta fuera
de carpeta.
MediaAltoFirmas, cinco MiB, UUID,
normalización y permisos.
Rechazar archivo y registrar
incidente sin abrirlo.
## Steven
RST-10Luces o cambio de tema
deterioran contraste o
memoria.
MediaMedioTres capas estáticas, paneles
opacos y pruebas de cuatro
combinaciones.
Fondo simplificado o sin
efectos, con mismos
controles.
## Caleb
RST-11Hilos no terminan al
solicitar salida.
MediaAltoTimeouts, cancelación y
espera ordenada H6.
Avisar bloqueo tras 10 s y
mantener proceso abierto.
## Steven
RST-12Respaldos no incluyen
los adjuntos correctos.
MediaAltoFOR SHARE por escritura y
FOR UPDATE en
puerta_mantenimiento para
respaldo; manifiesto
SHA-256 y autenticidad
## AES-256-GCM.
Descartar copia incompleta
y conservar último conjunto
validado.
## Alfredo
RST-13Binarios nativos o
fuentes faltan en otro
## Windows.
MediaMedioapp-image x64, fuente
redistribuible y PT-15.
Regenerar paquete de
destino; no depender de
JDK instalado.
## Aris
RST-14Credenciales del cliente
accesibles a
administrador local.
AltaAltoACL, mínimos privilegios,
TLS y equipos confiables.
Revocar secretos y
deshabilitar cliente
comprometido; limitar
instalación a entorno
controlado.
## Steven
RST-15Tiempo del equipo
supera capacidad
propuesta.
MediaAltoPlan único: 228 h TAR-001
–096, capacidad 281,14 h y
reserva 53,14 h; revisión
cruzada según Plan Tablas 4
## –5.
Priorizar Must; fotografías y
logotipo opcionales no
bloquean integridad.
## Steven

## 75
IDRiesgoProb.ImpactoMitigaciónContingenciaResp.
RST-16Conflictos de cargo,
mora y depósito crean
bloqueo circular.
MediaAltoOrden global de filas,
transacciones cortas y
timeouts.
Tres intentos totales con
mismo UUID, Session
nueva y esperas base
100/250 ms + jitter 0–50 ms
fuera de transacción; luego
informar conflicto.
## Alfredo

## 76
## Referencias
American Psychological Association. (s. f.). Sample student paper annotated.
https://apastyle.apa.org/style-grammar-guidelines/paper-format/student-annotated.pdf
Apache Software Foundation. (s. f.-a). Downloading Apache Maven and Maven Daemon.
https://maven.apache.org/download.cgi
Apache Software Foundation. (s. f.-b). Log4j to JUL Bridge 2.25.4 POM.
https://raw.githubusercontent.com/apache/logging-log4j2/rel/2.25.4/log4j-to-jul/pom.xml
Apache Software Foundation. (s. f.-c). Log4j-to-JUL bridge.
https://logging.apache.org/log4j/2.x/log4j-to-jul.html
Apache Software Foundation. (s. f.-d). Maven Compiler Plugin 3.14.0 POM.
https://raw.githubusercontent.com/apache/maven-compiler-plugin/maven-compiler-
plugin-3.14.0/pom.xml
Apache Software Foundation. (s. f.-e). Maven Surefire 3.5.3 POM.
https://raw.githubusercontent.com/apache/maven-surefire/surefire-3.5.3/pom.xml
AYR Tech (Pty) Ltd. (s. f.-a). API authentication notes.
https://www.exchangerate-api.com/docs/authentication
AYR Tech (Pty) Ltd. (s. f.-b). ExchangeRate-API Free and Pro currency converter API.
https://www.exchangerate-api.com/
AYR Tech (Pty) Ltd. (s. f.-c). Standard requests.
https://www.exchangerate-api.com/docs/standard-requests
AYR Tech (Pty) Ltd. (s. f.-d). Supported currencies.
https://www.exchangerate-api.com/docs/supported-currencies
AYR Tech (Pty) Ltd. (s. f.-e). Terms of service. https://www.exchangerate-api.com/terms

## 77
Balzarini, J., y Boyd, M. L. (2021). Working with them: Small-scale landlord strategies for
avoiding evictions. Housing Policy Debate, 31(3–5), 425–445.
https://doi.org/10.1080/10511482.2020.1800779
Banco Central de Nicaragua. (20 de octubre de 2025). BCN informa que la tasa de deslizamiento
del tipo de cambio del córdoba respecto al dólar de los Estados Unidos de América
continuará en cero por ciento (0%) anual en el año 2026. https://bcn.gob.ni/divulgacion-
prensa/bcn-informa-que-la-tasa-de-deslizamiento-del-tipo-de-cambio-del-cordoba-
respecto
Bechtold, S., Brannen, S., Link, J., Merdes, M., Philipp, M., de Rancourt, J., y Stein, C. (s. f.).
JUnit 5 user guide versión 5.12.2. https://docs.junit.org/5.12.2/user-guide/
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026a). Brief de diseño de interfaz y
experiencia de usuario para Latch. [Documento académico no publicado]. Universidad
Americana (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026b). Documento de flujo de la
aplicación de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026c). Documento de requisitos del
producto de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026e). Esquema de base de datos y
servicios de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).

## 78
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026f). Plan de implementación de
Latch. [Documento académico no publicado]. Universidad Americana (UAM).
DejaVu Fonts. (s. f.). License. https://dejavu-fonts.github.io/License.html
Eclipse Adoptium. (s. f.). Temurin support. https://adoptium.net/support/
## Eclipse Foundation. (2024). Jakarta Persistence 3.2.
https://jakarta.ee/specifications/persistence/3.2/
Free Software Foundation. (29 de junio de 2007). GNU Lesser General Public License versión 3.
https://jasperreports.sourceforge.net/license.html
Git for Windows. (s. f.). Git for Windows v2.51.0.windows.1. https://github.com/git-for-
windows/git/releases/tag/v2.51.0.windows.1
Gluon. (s. f.-a). JavaFX. https://gluonhq.com/products/javafx/
Gluon. (s. f.-b). Scene Builder. https://gluonhq.com/products/scene-builder/
Gluon. (s. f.-c). Scene Builder release 21.0.0.
https://github.com/gluonhq/scenebuilder/releases/tag/21.0.0
Google. (10 de septiembre de 2025). Gson 2.13.2.
https://github.com/google/gson/releases/tag/gson-parent-2.13.2
Hibernate. (s. f.-a). 7.4 series. https://hibernate.org/orm/releases/7.4/
Hibernate. (s. f.-b). Hibernate ORM user guide.
https://docs.hibernate.org/orm/7.4/userguide/html_single/
JaCoCo. (s. f.). Change history. https://www.jacoco.org/jacoco/trunk/doc/changes.html
Jaspersoft. (s. f.-a). JasperReports documentation.
https://jasperreports.sourceforge.net/README.html

## 79
Jaspersoft. (s. f.-b). JasperReports Excel POI 7.0.8 POM.
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/ext/excel-poi/pom.xml
Jaspersoft. (s. f.-c). JasperReports Library artifacts de la etiqueta 7.0.8.
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/pom-parent.xml
Jaspersoft. (s. f.-d). JasperReports PDF 7.0.8 POM.
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/ext/pdf/pom.xml
Jaspersoft. (2026). Jaspersoft Studio community edition 7.0.8.
https://community.jaspersoft.com/download-jaspersoft/community-edition/jaspersoft-
studio_7.0.8/
JetBrains. (s. f.). IntelliJ Community license.
https://github.com/JetBrains/intellij-community/blob/master/LICENSE.txt
JetBrains. (2025). What is new in IntelliJ IDEA 2025.2.
https://www.jetbrains.com/idea/whatsnew/2025-2/
Legion of the Bouncy Castle. (s. f.). Download Bouncy Castle for Java.
https://www.bouncycastle.org/download/bouncy-castle-java/
Malewicz, M. (s. f.). Aurora UI: New visual trend for 2021.
https://hype4.academy/articles/design/aurora-ui-new-visual-trend-for-2021
Open Exchange Rates. (s. f.-a). Currencies JSON.
https://openexchangerates.org/api/currencies.json
Open Exchange Rates. (s. f.-b). Latest JSON.
https://docs.openexchangerates.org/reference/latest-json
Open Exchange Rates. (s. f.-c). The forever free plan. https://openexchangerates.org/signup/free

## 80
Open Exchange Rates. (2022). Terms and conditions of website use.
https://openexchangerates.org/terms
OpenJFX. (s. f.-a). GaussianBlur JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/effect/GaussianBlur.html
OpenJFX. (s. f.-b). JavaFX CSS reference guide.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html
OpenJFX. (s. f.-c). RadialGradient JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/paint/RadialGradient.html
OpenJFX. (s. f.-d). Task JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html
Oracle. (s. f.-a). Cipher Java SE 21.
https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/Cipher.html
Oracle. (s. f.-b). HttpClient Java SE 21.
https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/
HttpClient.html
Oracle. (s. f.-c). Packaging overview Java 21.
https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html
OWASP Foundation. (s. f.). Password storage cheat sheet.
https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
PostgreSQL JDBC Driver. (s. f.-a). Download. https://jdbc.postgresql.org/download/
PostgreSQL JDBC Driver. (s. f.-b). License de pgJDBC 42.7.14.
https://raw.githubusercontent.com/pgjdbc/pgjdbc/REL42.7.14/LICENSE
PostgreSQL JDBC Driver. (s. f.-c). Using SSL. https://jdbc.postgresql.org/documentation/ssl/

## 81
QOS.ch. (s. f.-a). SLF4J license. https://www.slf4j.org/license.html
QOS.ch. (s. f.-b). SLF4J user manual. https://www.slf4j.org/manual.html
The PostgreSQL Global Development Group. (s. f.-a). 13.3. Explicit locking.
https://www.postgresql.org/docs/18/explicit-locking.html
The PostgreSQL Global Development Group. (s. f.-b). 25.1. SQL dump.
https://www.postgresql.org/docs/18/backup-dump.html
The PostgreSQL Global Development Group. (s. f.-c). 5.5. Constraints.
https://www.postgresql.org/docs/18/ddl-constraints.html
The PostgreSQL Global Development Group. (s. f.-d). 9.17. Sequence manipulation functions.
https://www.postgresql.org/docs/18/functions-sequence.html
The PostgreSQL Global Development Group. (s. f.-e). License.
https://www.postgresql.org/about/licence/
The PostgreSQL Global Development Group. (s. f.-f). Versioning policy.
https://www.postgresql.org/support/versioning/
Wooldridge, B. (s. f.-a). HikariCP 6.3.0 POM.
https://raw.githubusercontent.com/brettwooldridge/HikariCP/HikariCP-6.3.0/pom.xml
Wooldridge, B. (s. f.-b). HikariCP README de la etiqueta 6.3.0.
https://raw.githubusercontent.com/brettwooldridge/HikariCP/HikariCP-6.3.0/
README.md
World Wide Web Consortium. (12 de diciembre de 2024). Web Content Accessibility Guidelines
WCAG 2.2. https://www.w3.org/TR/WCAG22/

## 82
World Wide Web Consortium. (11 de diciembre de 2025). Guidance on applying WCAG 2 to
non-web information and communications technologies WCAG2ICT.
https://www.w3.org/TR/wcag2ict-22/

## 83
## Apéndice A
Fragmentos de Configuración
Parámetros Iniciales y Límites
La Tabla A1 conserva los valores iniciales y rangos de configuración del PRD para evitar
que las migraciones asignen políticas distintas por equipo (Cuadra et al., 2026c). Los cambios
administrativos conservarán el historial, actor y vigencia definidos en el Esquema (Cuadra et al.,
2026e) y afectarán operaciones futuras, con acceso exclusivo del Propietario. El algoritmo de
hash y los límites fijos de archivos o cierre permanecerán bajo control de versión del programa.
Las preferencias visuales corresponden a cada usuario y se guardarán fuera del dominio. La mora
de demostración inicia en cero, por lo que generar un recargo en una prueba requiere configurar
antes una política sintética de valor positivo.
## Tabla A1
Parámetros Iniciales Conservados del PRD
ParámetroValor Inicial y RangoAplicación Técnica
Intentos fallidos5; entero de 3 a 10.Contador persistente por cuenta.
Bloqueo15 min; rango de 5 a 60 min.Cambios afectan nuevos bloqueos.
Inactividad15 min; rango de 5 a 60 min.Aviso 60 s antes y borrador local sin
secretos.
Gracia3 días; rango de 0 a 30 días
calendario.
Mora a partir del día posterior al
vencimiento más gracia.
Modalidad de moraPorcentaje; permite
Porcentaje o Monto fijo.
Una modalidad por versión y una
aplicación por renta.
Porcentaje de mora0 %; rango de 0 a 10 %
inclusive.
Principal pendiente al generar;
excluye Mora.
Monto fijo de mora0; rango de 0 a 10000 en
moneda del contrato.
Cero no genera recargo; límite
técnico de demo, no legal.
Por vencer30 días; entero de 1 a 90.Mismo umbral en contratos, panel y
reporte.
H25 min; rango de 1 a 60 min.Solo con aplicación abierta y sin
superposición local.
H324 h; rango de 24 a 168 h.Respeta próxima actualización y
caché compartida.

## 84
ParámetroValor Inicial y RangoAplicación Técnica
HTTPConexión 5 s; operación 10 s;
un reintento a 60 s.
Solo red o 5xx; 429 respeta Retry-
After o espera 24 h.
H610 s, fijo.Mostrar bloqueo y mantener proceso
si quedan transacciones.
Adjuntos5 MiB, fijo; PDF, PNG o
## JPEG.
Foto y logotipo solo PNG o JPEG.
RespaldoAlerta 24 h; siete diarios y
cuatro semanales.
AES-256-GCM y contraseña de
respaldo; comprobar autenticidad y
manifiesto.
Recibo000001 en base nueva;
creciente global.
Contador no editable y sin reinicio
anual.
PrecisiónDinero 2 decimales, tasa 8;
## HALF_EVEN.
Calendario local America/Managua e
instantes UTC.
TemaClaro; exactamente Claro u
## Oscuro.
Preferencia local por UUID; ambos
roles.
Sin efectosDesactivada al inicio.Opción personal independiente en
ambos temas.
Escritura ordinarialock_timeout=2000 ms;
statement_timeout=5000 ms.
Tres intentos totales; esperas
100/250 ms + jitter 0–50 ms fuera de
transacción y Session nueva.
Puerta de respaldorespaldo_lock_timeout_ms=3
000 ms.
Parámetro exclusivo de respaldo,
separado de escritura ordinaria.
Banda de referencia FX1–100 NIO/USD, inclusive.Fuera de banda: no reemplazar
vigente y registrar M09
## REFERENCIA_EN_CUARENTEN
A con valor y huella.
Reloj financieronow() de BD; diferencia local
máxima 5 min.
Superar 5 min bloquea nuevas
finanzas con ERR-057 / MSG-080.
POM Ilustrativo
El POM fija dependencias y plugins de construcción para Java 21. Compiler Plugin
3.14.0 y Surefire 3.5.3 existen en las etiquetas oficiales consultadas (Apache Software
Foundation, s. f.-d; Apache Software Foundation, s. f.-e). JasperReports incorpora extensiones
separadas de PDF y Excel POI, ambas fijadas al motor 7.0.8 (Jaspersoft, s. f.-d; Jaspersoft, s. f.-
b). Este fragmento representa configuración técnica y no una aplicación terminada ni una
compilación ejecutada. La distribución usará los artefactos resueltos de la implementación, con
clases y recursos organizados según el árbol del Apéndice B.

## 85
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
https://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion>
<groupId>ni.edu.uam</groupId>
<artifactId>latch</artifactId>
## <version>0.1.0</version>
## <properties>
## <maven.compiler.release>21</maven.compiler.release>
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
## <javafx.version>21.0.13</javafx.version>
## <jasper.version>7.0.8</jasper.version>
## </properties>
## <dependencies>
## <dependency>
<groupId>org.openjfx</groupId>
<artifactId>javafx-controls</artifactId>
## <version>${javafx.version}</version>
## </dependency>
## <dependency>
<groupId>org.openjfx</groupId>
<artifactId>javafx-fxml</artifactId>
## <version>${javafx.version}</version>
## </dependency>
## <dependency>
<groupId>org.postgresql</groupId>
<artifactId>postgresql</artifactId>
## <version>42.7.14</version>
## </dependency>
## <dependency>
<groupId>org.hibernate.orm</groupId>
<artifactId>hibernate-core</artifactId>
<version>7.4.6.Final</version>
## </dependency>
## <dependency>
<groupId>jakarta.persistence</groupId>
<artifactId>jakarta.persistence-api</artifactId>
## <version>3.2.0</version>
## </dependency>
## <dependency>
<groupId>com.zaxxer</groupId>
<artifactId>HikariCP</artifactId>
## <version>6.3.0</version>
## </dependency>
## <dependency>
<groupId>com.google.code.gson</groupId>
<artifactId>gson</artifactId>
## <version>2.13.2</version>
## </dependency>
## <dependency>
<groupId>net.sf.jasperreports</groupId>
<artifactId>jasperreports</artifactId>
## <version>${jasper.version}</version>
## </dependency>
## <dependency>
<groupId>net.sf.jasperreports</groupId>

## 86
<artifactId>jasperreports-pdf</artifactId>
## <version>${jasper.version}</version>
## </dependency>
## <dependency>
<groupId>net.sf.jasperreports</groupId>
<artifactId>jasperreports-excel-poi</artifactId>
## <version>${jasper.version}</version>
## </dependency>
## <dependency>
<groupId>net.sf.jasperreports</groupId>
<artifactId>jasperreports-fonts</artifactId>
## <version>${jasper.version}</version>
## </dependency>
## <dependency>
<groupId>org.bouncycastle</groupId>
<artifactId>bcprov-jdk18on</artifactId>
## <version>1.86</version>
## </dependency>
## <dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-api</artifactId>
## <version>2.0.20</version>
## </dependency>
## <dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-jdk14</artifactId>
## <version>2.0.20</version>
## </dependency>
## <dependency>
<groupId>org.junit.jupiter</groupId>
<artifactId>junit-jupiter</artifactId>
## <version>5.12.2</version>
## <scope>test</scope>
## </dependency>
## <dependency>
<groupId>org.apache.logging.log4j</groupId>
<artifactId>log4j-to-jul</artifactId>
## <version>2.25.4</version>
## <scope>runtime</scope>
## </dependency>
## </dependencies>
## <build>
## <plugins>
## <plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-compiler-plugin</artifactId>
## <version>3.14.0</version>
## </plugin>
## <plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-surefire-plugin</artifactId>
## <version>3.5.3</version>
## <configuration>
<useModulePath>false</useModulePath>
## </configuration>
## </plugin>
## <plugin>

## 87
<groupId>org.jacoco</groupId>
<artifactId>jacoco-maven-plugin</artifactId>
## <version>0.8.13</version>
## <executions>
## <execution>
## <goals>
## <goal>prepare-agent</goal>
## </goals>
## </execution>
## <execution>
## <id>cobertura</id>
## <phase>verify</phase>
## <goals>
## <goal>report</goal>
## </goals>
## </execution>
## </executions>
## </plugin>
## </plugins>
## </build>
## </project>
Configuración de Hibernate y Pool Compartido
El arranque programático recibirá URL, usuario y contraseña desde configuración
externa validada y creará un único HikariDataSource. Hibernate usará ese objeto como
DataSource y validará el esquema existente, sin ejecutar migraciones implícitas ni crear un pool
adicional. El fragmento ilustra la inyección y el registro de una entidad; el árbol del proyecto
define las demás entidades y sus repositorios. El cierre pertenece a H6 y ocurrirá después de
terminar los trabajos que usan SessionFactory, seguido del pool. La configuración no contiene
credenciales válidas y requiere valores de entorno presentes, de modo que un secreto faltante
produzca un error de configuración identificable.
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import ni.edu.uam.latch.dominio.Contrato;
public final class PersistenciaEjemplo {
public record Recursos(HikariDataSource pool,
SessionFactory sesiones) {
## }
public static Recursos crear() {
HikariConfig cfg = new HikariConfig();
cfg.setJdbcUrl(exigir("LATCH_DB_URL"));

## 88
cfg.setUsername(exigir("LATCH_DB_USER"));
cfg.setPassword(exigir("LATCH_DB_PASSWORD"));
cfg.setMaximumPoolSize(6);
cfg.setMinimumIdle(1);
cfg.setConnectionTimeout(3000);
cfg.setValidationTimeout(1000);
cfg.setMaxLifetime(1800000);
cfg.addDataSourceProperty("tcpKeepAlive", true);
HikariDataSource pool = new HikariDataSource(cfg);
try {
Configuration orm = new Configuration();
orm.getProperties().put(
"hibernate.connection.datasource", pool);
orm.setProperty("hibernate.hbm2ddl.auto", "validate");
orm.setProperty("hibernate.jdbc.time_zone", "UTC");
orm.setProperty("hibernate.show_sql", "false");
orm.addAnnotatedClass(Contrato.class);
return new Recursos(pool, orm.buildSessionFactory());
} catch (RuntimeException fallo) {
pool.close();
throw fallo;
## }
## }
private static String exigir(String clave) {
String valor = System.getenv(clave);
if (valor == null || valor.isBlank()) {
throw new IllegalStateException("Falta " + clave);
## }
return valor;
## }
## }
Consumo Mínimo con HttpClient
El ejemplo mínimo usa un cliente inyectado, Bearer, límite de conexión y espera total
explícita, con deserialización mediante Gson. Su propósito es ilustrar el contrato HTTP del
servicio y la validación de base y tasa, no implementar la concesión, la caché ni la autorización
financiera. Esas responsabilidades corresponden al servicio descrito en el cuerpo y conservan sus
criterios de aceptación. El cliente reutilizable se cierra desde H6 después de cancelar trabajos,
mientras el timeout cancela el futuro en curso y conserva la interrupción del trabajador. El
ejemplo no imprime llave ni cuerpo, y el fixture de la integración sigue siendo sintético. La
validación aceptará 1–100 NIO/USD inclusive. Si falla esa banda, TipoCambioService, fuera del

## 89
adaptador ilustrativo, conservará la referencia vigente y registrará la cuarentena en bitacora con
valor y huella; no habrá tabla adicional.
import ni.edu.uam.latch.aplicacion.excepcion.RespuestaFxInvalidaException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
public final class ClienteTasaEjemplo {
public record ReferenciaTasaDto(BigDecimal nioPorUsd,
Instant actualizada) {
## }
private final HttpClient cliente = HttpClient.newBuilder()
.connectTimeout(Duration.ofSeconds(5))
.followRedirects(HttpClient.Redirect.NEVER).build();
public ReferenciaTasaDto consultar(String llave) throws Exception {
if (llave == null || llave.isBlank()) {
throw new IllegalArgumentException("Falta llave HTTP");
## }
HttpRequest solicitud = HttpRequest.newBuilder(URI.create(
"https://v6.exchangerate-api.com/v6/latest/USD"))
.timeout(Duration.ofSeconds(10))
.header("Accept", "application/json")
.header("Authorization", "Bearer " + llave).GET().build();
var futuro = cliente.sendAsync(solicitud,
HttpResponse.BodyHandlers.ofString());
try {
var respuesta = futuro.get(10, TimeUnit.SECONDS);
if (respuesta.statusCode() != 200) {
throw new IllegalStateException(
"Estado HTTP " + respuesta.statusCode());
## }
JsonObject json = JsonParser.parseString(
respuesta.body()).getAsJsonObject();
if (!"success".equals(json.get("result").getAsString())
|| !"USD".equals(json.get("base_code").getAsString())) {
throw new IllegalStateException("Contrato inválido");
## }
BigDecimal tasa = json.getAsJsonObject("conversion_rates")
.get("NIO").getAsBigDecimal();
long marca = json.get("time_last_update_unix").getAsLong();
if (tasa.compareTo(BigDecimal.ONE) < 0
|| tasa.compareTo(BigDecimal.valueOf(100)) > 0 || marca <= 0)
## {
throw new RespuestaFxInvalidaException("Tasa fuera de banda
1-100 o fecha inválida");
## }
return new ReferenciaTasaDto(tasa, Instant.ofEpochSecond(marca));
} catch (InterruptedException fallo) {

## 90
futuro.cancel(true);
Thread.currentThread().interrupt();
throw fallo;
} catch (TimeoutException fallo) {
futuro.cancel(true);
throw fallo;
## }
## }
public void cerrarTrasDetenerTareas() {
cliente.close();
## }
## }

## 91
## Apéndice B
Estructura Completa de Carpetas
El árbol representa los paquetes y recursos previstos para Latch, separados del repositorio
del laboratorio usado como espacio de trabajo. Los nombres enumeran las clases principales que
implementarán los contratos del TRD, con pruebas agrupadas por nivel y recursos de
demostración separados. El esquema mantiene FXML, CSS, imágenes y reportes fuera de clases
Java y reserva infraestructura para adaptadores concretos. Los directorios operativos se ubicarán
fuera de Git y tendrán permisos propios de la instalación. Esta estructura permite revisar
dependencias por paquete y conservar evidencia de cada construcción sin confundir datos
sintéticos, configuración externa y archivos distribuidos al usuario. Los doce servicios públicos
serán exactamente los del Esquema; RespaldoService será un colaborador interno de
ConfiguracionAuditoriaService. Las subclases JavaFX Task o Service usarán sufijo Task;
EmisorRecibos representará H4 dentro de PagoService. El árbol remite al Brief Tabla C1 para
los 40 PAN y aplica los nombres españoles de D07; la tabla de renombrado entregada con esta
revisión desarrolla las 40 correspondencias.
latch/
pom.xml
mvnw
mvnw.cmd
## .mvn/wrapper/maven-wrapper.properties
## .gitignore
README.md
## LICENSES/
THIRD-PARTY-NOTICES.txt
db/migrations/
## V001__modelo.sql
## V002__objetos.sql
## V003__catalogos.sql
## V004__privilegios.sql
## T001__demostracion.sql
## R001__reportes.sql
docs/
adr/ADR-01.md a ADR-22.md
contratos/proveedor-tasas.json
evidencia/revision-fecha/

## 92
packaging/
crear-runtime.ps1
crear-app-image.ps1
instalacion.md
src/main/java/ni/edu/uam/latch/
arranque/
## Launcher.java
LatchApplication.java
ComposicionAplicacion.java
presentacion/
controlador/
InicioSesionController.java
PanelPrincipalController.java
RegistroPagoController.java
(40 Controller: inventario del Brief Tabla C1, con D07)
navegacion/NavegadorVistas.java
validacion/ValidadorFormulario.java
tema/
## Tema.java
ThemeService.java
PreferenciasVisuales.java
AuroraBackground.java
UiMetrics.java
aplicacion/
servicio/
SeguridadService.java
PropiedadUnidadService.java
InquilinoService.java
ContratoService.java
CobranzaService.java
PagoService.java
DepositoService.java
MantenimientoService.java
TipoCambioService.java
ReporteService.java
PanelService.java
ConfiguracionAuditoriaService.java
servicio/interno/RespaldoService.java
puerto/
ContratoRepository.java
CargoRepository.java
PagoRepository.java
AuditoriaRepository.java
ProveedorTasa.java
AlmacenAdjuntos.java
command/PagoCommand.java
dto/
PagoConfirmadoDto.java
ProgresoLoteDto.java
ReferenciaTasaDto.java
ReporteDto.java
ContextoOperacion.java
## Pagina.java
concurrencia/
GestorTareas.java
GeneracionCargosTask.java
RevisionVencimientosTask.java

## 93
ActualizacionTasaTask.java
ExportacionReporteTask.java
CoordinadorCierre.java
(H4 EmisorRecibos: sección crítica en PagoService)
excepcion/
LatchException.java
AccesoDenegadoException.java
ArchivoException.java
CargoAplicadoException.java
CargoIncompatibleException.java
ConflictoConcurrenciaException.java
ConflictoIdempotenciaException.java
ConsultaReporteException.java
CuentaBloqueadaException.java
CuotaFxException.java
DependenciaActivaException.java
DependenciaCreditoException.java
DuplicadoException.java
ExportacionException.java
FalloAccesoDatosException.java
GarantiaInsuficienteException.java
GarantiaPendienteException.java
IntegridadException.java
OperacionCanceladaException.java
ParametroInvalidoException.java
PeriodoDuplicadoException.java
PlantillaException.java
PropiedadIncompatibleException.java
RelojDesfasadoException.java
ReporteException.java
RespaldoException.java
RespuestaFxInvalidaException.java
RestauracionException.java
ResultadoInciertoException.java
SaldoInsuficienteException.java
ServicioExternoException.java
TareasActivasException.java
TasaNoAutorizadaException.java
TransicionInvalidaException.java
UnidadReservadaException.java
ValidacionException.java
dominio/
## Propiedad.java
## Unidad.java
## Inquilino.java
## Contrato.java
## Cargo.java
## Pago.java
AplicacionPago.java
## Usuario.java
## Recibo.java
DepositoGarantia.java
MovimientoDeposito.java
SolicitudMantenimiento.java
GastoPropiedad.java
TipoCambio.java
AutorizacionTasa.java

## 94
## Moneda.java
EstadoContrato.java
DocumentoAdjunto.java
regla/PoliticaMora.java
regla/DistribucionPago.java
valor/Importe.java
infraestructura/
persistencia/
HibernateBootstrap.java
UnidadTrabajoHibernate.java
jdbc/UsuarioJdbcRepository.java
jdbc/AuditoriaJdbcRepository.java
jdbc/IndicadoresJdbcRepository.java
orm/ContratoHibernateRepository.java
orm/CargoHibernateRepository.java
orm/PagoHibernateRepository.java
http/ExchangeRateApiCliente.java
archivos/AdjuntosDisco.java
archivos/RespaldoPostgres.java
preferencias/PreferencesAdaptador.java
seguridad/Argon2idHash.java
reportes/JasperGenerador.java
herramientas/Aprovisionar.java
src/main/resources/
META-INF/orm.xml
ui/
fxml/
inicio-sesion.fxml
panel-principal.fxml
registro-pago.fxml
(40 FXML planos: Brief Tabla C1, con D07)
css/
components.css
claro.css
oscuro.css
print.css
imagenes/
i18n/mensajes_es_NI.properties
reportes/
recibo.jrxml
estado-cuenta.jrxml
cartera-vencida.jrxml
ingresos-gastos.jrxml
ocupacion.jrxml
contratos-por-vencer.jrxml
historial-pagos.jrxml
garantias.jrxml
mantenimiento.jrxml
tasas-utilizadas.jrxml
subreportes/aplicaciones.jrxml
fuentes/fonts.xml
jasperreports_extension.properties
src/test/java/ni/edu/uam/latch/
unitarias/
integracion/
concurrencia/
presentacion/

## 95
reportes/
src/test/resources/fixtures/
base-v1.sql
tasa-valida-sintetica.json
tasa-error-sintetica.json
adjuntos/
target/  (generado; fuera de Git)
Directorios externos de operación:
%LOCALAPPDATA%/Latch/config/
%LOCALAPPDATA%/Latch/logs/
%LOCALAPPDATA%/Latch/temporales/
%LOCALAPPDATA%/Latch/adjuntos/  (individual)
recurso SMB restringido/adjuntos/  (LAN)
directorio autorizado/respaldos/
Preferences: ni/edu/uam/latch/presentation; usuarios por UUID

## 96
## Apéndice C
## Glosario Técnico
El glosario unifica vocabulario del negocio y mecanismos técnicos para que código,
pruebas y documentos mantengan el mismo concepto. Los valores visibles de estados y catálogos
conservan la ortografía del PRD, aunque los identificadores Java usen constantes sin espacios ni
tildes. La Tabla C1 define los términos necesarios para interpretar capas, transacciones,
concurrencia, tasas y presentación. Las equivalencias técnicas no modifican reglas financieras y
una etiqueta de pantalla no reemplaza un estado persistente. El equipo usará estas definiciones al
escribir Javadoc, mensajes y casos, con unidades de moneda y tiempo expresadas cuando el valor
pueda resultar ambiguo.
## Tabla C1
Glosario de Negocio y Construcción
TérminoDefinición Adoptada
UnidadEspacio alquilable asociado a una propiedad; su estado respeta las
reservas contractuales.
CanonMonto pactado de renta, expresado en la moneda del contrato.
CargoObligación de pago con tipo, monto, período y vencimiento.
MoraRecargo por atraso después de gracia; no constituye base para otra
mora.
Depósito de garantíaMonto separado de renta para aplicación o devolución con conceptos y
saldo trazables.
ReciboComprobante administrativo de pago confirmado, con número
consecutivo conservado incluso al anular.
Estado de cuentaReporte de cargos, aplicaciones, crédito y saldo de un contrato a fecha
de corte.
Tasa de cambioCantidad de NIO por un USD; distingue referencia informativa y tasa
autorizada para una fecha.
Período de facturaciónMes de renta identificado por año y mes, único por contrato para
cargos Renta.
DTOObjeto de transferencia de datos que no expone sesión, conexión ni
asociaciones diferidas.
JDBCAPI de acceso SQL; Latch utiliza PreparedStatement y pgJDBC para
PostgreSQL.

## 97
TérminoDefinición Adoptada
ORMMapeo entre entidades y tablas; Hibernate administra el modelo de
negocio y sus versiones.
SessionFactoryObjeto compartido que crea sesiones; una instancia por proceso y
cierre posterior a las tareas.
SessionContexto de persistencia por caso de uso y por hilo, con transacción de
alcance definido.
TransacciónUnidad de confirmación o reversión que conserva invariantes entre
todas sus escrituras.
@VersionControl optimista mediante columna BIGINT que detecta edición
concurrente de una entidad.
IdempotenciaUna solicitud UUID repetida con el mismo contenido recupera el
mismo resultado confirmado.
Sección críticaBloque que protege un recurso compartido; H4 abarca emisión y final
de transacción del recibo.
Lock pesimistaBloqueo de registro antes de calcular y escribir una operación
competidora.
InterbloqueoEspera circular entre transacciones; orden de filas y reintentos
acotados mitigan el riesgo.
InaniciónEspera prolongada de una tarea por competencia; se observa y limita
con colas y tiempos máximos.
Task y ServiceWorker de JavaFX para trabajo observable y su coordinación
reutilizable, con cancelación cooperativa.
PoolConjunto limitado de conexiones reutilizables; Latch comparte
HikariDataSource entre sus rutas.
CachéReferencia conservada localmente para reducir consultas, con origen y
antigüedad visibles.
Snapshot financieroCopia inmutable de tasa, aplicaciones y datos de emisión usada para
conservar historia.
Looked-up colorColor semántico JavaFX resuelto desde un ancestro, compartido por
estilos de componentes.
PseudoestadoSelector de estado visual, como :focused o :invalid propio de la
aplicación.
Aurora UIDirección visual de luces y degradados orgánicos; no designa una
biblioteca de Latch.
Claro y OscuroLos dos únicos temas de presentación; sin efectos es una opción
independiente en cada uno.
Monedas y RolesNIO (C$), USD (US$); Propietario y Asistente.
Tipos de UnidadCasa, Apartamento, Cuarto, Local comercial.
Estados de ContratoBorrador, Vigente, Por vencer, Vencido, Terminado, Cancelado.
Estados de UnidadDisponible, Ocupada, En mantenimiento, Inactiva.
Estados de CargoPendiente, Parcial, Pagado, Vencido, Anulado.
Estados de PagoRegistrado, Anulado.

## 98
TérminoDefinición Adoptada
Estados de
## Mantenimiento
Abierta, En proceso, Resuelta, Cancelada.
Estados de DepósitoRetenido, Aplicado, Devuelto parcial, Devuelto.
Tipos de CargoRenta, Mora, Reparación, Servicio, Otro.
Métodos de PagoEfectivo, Transferencia, Depósito bancario, Otro.

## 99
## Apéndice D
Fuentes y Trazabilidad Documental
La Tabla D1 relaciona cada obra consultada con el dato o decisión que respalda, su
organización responsable y el alcance de lectura. La consulta documental corresponde al 23 de
octubre de 2026 y no confirma condiciones comerciales para fechas posteriores. Las fuentes
técnicas son documentos oficiales, código etiquetado o licencias del responsable; el antecedente
científico conserva autor, método y localización. La revisión del artículo Aurora comprende
únicamente contenido público y no atribuye hallazgos a las partes restringidas. Los valores de
prueba, presupuestos, responsables y calendario de Latch son decisiones o supuestos del equipo,
y no resultados derivados de esas publicaciones.

## 100
## Tabla D1
Fuentes Consultadas y Alcance de Evidencia
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Formato estudiantil;
prevalecen las reglas
particulares del encargo.
## American Psychological
Association (s. f.). Ejemplo
editorial. Sample student
paper annotated. Publicación:
s. f..
https://apastyle.apa.org/style-
grammar-guidelines/paper-
format/student-annotated.pdf
23/10/2026Portada, encabezados
y referencias; pp. 2 a
- Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Publicación verificable; se
conserva la versión del
PRD aunque existe una
posterior.
## Apache Software Foundation
(s. f.-a). Página oficial de
distribución. Downloading
Apache Maven and Maven
Daemon. Publicación: s. f..
https://maven.apache.org/
download.cgi
23/10/2026Previous stable Maven
## 3.9.16. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Artefacto log4j-to-jul
publicado en la etiqueta
## 2.25.4; Apache 2.0.
## Apache Software Foundation
(s. f.-b). Código oficial
etiquetado. Log4j to JUL
Bridge 2.25.4 POM.
Publicación: s. f..
https://
raw.githubusercontent.com/
apache/logging-log4j2/rel/
## 2.25.4/log4j-to-jul/pom.xml
23/10/2026ArtifactId y
dependencias. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Envía API Log4j a JUL, sin
instalar un puente inverso
que forme un ciclo.
## Apache Software Foundation
(s. f.-c). Manual oficial.
Log4j-to-JUL bridge.
Publicación: s. f..
https://logging.apache.org/
log4j/2.x/log4j-to-jul.html
23/10/2026Descripción y
conversión de niveles.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 101
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Versión del compilador
fijada y publicada; Java
release 21 se configura en
## Latch.
## Apache Software Foundation
(s. f.-d). Código oficial
etiquetado. Maven Compiler
Plugin 3.14.0 POM.
Publicación: s. f..
https://
raw.githubusercontent.com/
apache/maven-compiler-
plugin/maven-compiler-
plugin-3.14.0/pom.xml
23/10/2026Version y
prerequisites. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Plugin de pruebas fijado
con proveedor JUnit
## Jupiter.
## Apache Software Foundation
(s. f.-e). Código oficial
etiquetado. Maven Surefire
3.5.3 POM. Publicación: s. f..
https://
raw.githubusercontent.com/
apache/maven-surefire/
surefire-3.5.3/pom.xml
23/10/2026Version y módulos.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
GET sin llave en URL y
cabecera Authorization;
organización identificada en
pie y términos.
AYR Tech (Pty) Ltd. (s. f.-a).
Documentación oficial del
proveedor. API authentication
notes. Publicación: s. f..
https://www.exchangerate-
api.com/docs/authentication
23/10/2026Token Bearer
authentication. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
USD 0, 1500 consultas por
mes y actualización diaria,
condiciones consultadas el
23 de octubre de 2026.
AYR Tech (Pty) Ltd. (s. f.-b).
Condiciones comerciales
oficiales. ExchangeRate-API
Free and Pro currency
converter API. Publicación: s.
f..
https://www.exchangerate-
api.com/
23/10/2026Plans and pricing,
Free. Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
result, base_code,
conversion_rates y
timestamps; códigos
semánticos de error.
AYR Tech (Pty) Ltd. (s. f.-c).
Contrato técnico oficial.
Standard requests.
Publicación: s. f..
https://www.exchangerate-
api.com/docs/standard-requests
23/10/2026Usage guide y Error
responses. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 102
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Cobertura NIO
comprobada; no supone
equivalencia con tasa BCN.
AYR Tech (Pty) Ltd. (s. f.-d).
Catálogo oficial. Supported
currencies. Publicación: s. f..
https://www.exchangerate-
api.com/docs/supported-
currencies
23/10/2026All supported
currencies, NIO y
USD. Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Uso informativo, caché
interna permitida,
redistribución restringida y
ausencia de garantía de
continuidad.
AYR Tech (Pty) Ltd. (s. f.-e).
Condiciones de uso oficiales.
Terms of service. Publicación:
s. f..
https://www.exchangerate-
api.com/terms
23/10/2026License, disclaimer y
data caching policy.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Artículo original de
## Housing Policy Debate,
## DOI
## 10.1080/10511482.2020.18
00779; estudio situado en
## Filadelfia.
Balzarini y Boyd (2021).
Artículo científico. Working
with them: Small-scale
landlord strategies for
avoiding evictions. Housing
## Policy Debate, 31(3–5), 425–
## 445.
https://nlihc.org/sites/default/
files/Working-With-Them-
Small-Scale-Landlord-
## Strategies.pdf
23/10/2026Resumen y método;
pp. 1 a 2 y 6 a 8 del
artículo. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Nota bibliográfica:
versión publicada en
línea en 2020; la
referencia corresponde
al volumen de 2021.

## 103
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Deslizamiento 0 % y
referencia oficial 36.6243
NIO por USD para 2026; no
es un contrato API.
Banco Central de Nicaragua
## (2025). Comunicado
institucional. BCN informa
que la tasa de deslizamiento
del tipo de cambio del
córdoba respecto al dólar de
los Estados Unidos de
América continuará en cero
por ciento (0%) anual en el
año 2026. Publicación: 2025.
https://bcn.gob.ni/divulgacion-
prensa/bcn-informa-que-la-
tasa-de-deslizamiento-del-tipo-
de-cambio-del-cordoba-
respecto
23/10/2026Párrafo inicial, 20 de
octubre de 2025.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
JUnit 5.12.2 sobre Java 8 o
posterior; compatible con
## Java 21.
Bechtold et al. (s. f.). Manual
oficial versionado. JUnit 5
user guide versión 5.12.2.
Publicación: s. f..
https://docs.junit.org/5.12.2/
user-guide/
23/10/2026Supported Java
versions y
parameterized tests.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Base de RF, 60 RNF, 38
RN y volúmenes; fuente
suministrada, sin validación
empírica.
Cuadra et al. (2026c).
Documento del equipo.
Documento de requisitos del
producto de Latch.
## Publicación: 2026c.
Archivo local:
Latch_PRD_APA7_v3.pdf
23/10/2026pp. 12, 23 a 27, 81 a
- Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Fuente redistribuible;
conservar licencia y avisos.
DejaVu Fonts (s. f.). Licencia
del proyecto. License.
Publicación: s. f..
https://dejavu-fonts.github.io/
## License.html
23/10/2026Bitstream Vera y
modificaciones
DejaVu. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 104
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Rama LTS y versión
publicada 21.0.12.1+1; el
calendario de 21.0.13 no
acredita un binario.
Eclipse Adoptium (s. f.).
Documentación del
distribuidor. Temurin support.
Publicación: s. f..
https://adoptium.net/support/23/10/2026Release roadmap, fila
## Java 21. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
API 3.2.0 y licencia dual
EPL/GPL con excepción
## Classpath.
## Eclipse Foundation (2024).
Especificación oficial. Jakarta
## Persistence 3.2. Publicación:
## 2024.
https://jakarta.ee/
specifications/persistence/3.2/
23/10/2026Release y API. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
LGPL 3.0; conservar avisos
y posibilidad de relink.
## Free Software Foundation
(2007). Licencia del
distribuidor. GNU Lesser
## General Public License
versión 3. Publicación: 2007.
https://
jasperreports.sourceforge.net/
license.html
23/10/2026Secciones 0 a 4.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Versión de herramienta
fijada; no se afirma
instalada.
Git for Windows (s. f.).
Publicación oficial. Git for
Windows v2.51.0.windows.1.
Publicación: s. f..
https://github.com/git-for-
windows/git/releases/tag/
v2.51.0.windows.1
23/10/2026Etiqueta y assets.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
JavaFX 21.0.13, JDK
mínimo 17; compatible
documentalmente con Java
## 21.
Gluon (s. f.-a).
Documentación del
distribuidor. JavaFX.
Publicación: s. f..
https://gluonhq.com/products/
javafx/
23/10/2026Roadmap, fila 21 LTS.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Licencia BSD e historial;
no se adopta la versión 26
mostrada.
Gluon (s. f.-b).
Documentación oficial. Scene
Builder. Publicación: s. f..
https://gluonhq.com/products/
scene-builder/
23/10/2026License y enlace BSD.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 105
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Versión publicada y
alineación con JDK y
JavaFX 21.
Gluon (s. f.-c). Publicación
oficial. Scene Builder release
21.0.0. Publicación: s. f..
https://github.com/gluonhq/
scenebuilder/releases/tag/21.0.0
23/10/2026Changelog, Use JDK
and JavaFX 21. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Versión fijada del
deserializador; publicación
del 10 de septiembre de
## 2025.
## Google (2025). Publicación
oficial de software. Gson
## 2.13.2. Publicación: 2025.
https://github.com/google/
gson/releases/tag/gson-
parent-2.13.2
23/10/2026Etiqueta gson-
parent-2.13.2. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
7.4.6.Final publicado el 23
de agosto de 2026; Java 21
y Jakarta Persistence 3.2.
Hibernate (s. f.-a).
Publicación oficial. 7.4 series.
Publicación: s. f..
https://hibernate.org/orm/
releases/7.4/
23/10/2026Compatibility y
Releases in this series.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Sustenta @Version,
sesiones y carga; manual de
serie que muestra
7.4.13.Final, sin atribuirlo
al parche fijado.
Hibernate (s. f.-b). Manual
oficial de la serie. Hibernate
ORM user guide. Publicación:
s. f..
https://docs.hibernate.org/orm/
## 7.4/userguide/html_single/
23/10/2026Domain model,
transactions, locking y
fetching. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Versión 0.8.13 publicada y
soporte de bytecode de Java
21; sin adoptar snapshot.
JaCoCo (s. f.). Historial
oficial. Change history.
Publicación: s. f..
https://www.jacoco.org/jacoco/
trunk/doc/changes.html
23/10/2026Release 0.8.13. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 106
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Ruptura de plantillas
compiladas y módulos
opcionales de versión 7.
Jaspersoft (s. f.-a). Manual
oficial. JasperReports
documentation. Publicación:
s. f..
https://
jasperreports.sourceforge.net/
README.html
23/10/2026Library 7 y Building
the library. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Extensión jasperreports-
excel-poi para XLSX y sus
dependencias controladas.
Jaspersoft (s. f.-b). Código
oficial etiquetado.
JasperReports Excel POI 7.0.8
POM. Publicación: s. f..
https://
raw.githubusercontent.com/
## Jaspersoft/jasperreports/7.0.8/
ext/excel-poi/pom.xml
23/10/2026ArtifactId y
dependencias. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Versión 7.0.8, objetivo Java
8 y extensión Hibernate
6.3.1.Final; no es
certificación del conjunto.
Jaspersoft (s. f.-c). Código
oficial etiquetado.
JasperReports Library
artifacts de la etiqueta 7.0.8.
Publicación: s. f..
https://
raw.githubusercontent.com/
## Jaspersoft/jasperreports/7.0.8/
pom-parent.xml
## 23/10/2026revision,
source.version,
hibernate.version y
licenses. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Extensión jasperreports-pdf
de la misma etiqueta que el
motor.
Jaspersoft (s. f.-d). Código
oficial etiquetado.
JasperReports PDF 7.0.8
POM. Publicación: s. f..
https://
raw.githubusercontent.com/
## Jaspersoft/jasperreports/7.0.8/
ext/pdf/pom.xml
23/10/2026ArtifactId y
dependencias. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Soporte explícito de Library
7.0.8; licencia de tipo BSD
del diseñador.
## Jaspersoft (2026). Publicación
oficial de software. Jaspersoft
Studio community edition
## 7.0.8. Publicación: 2026.
https://
community.jaspersoft.com/
download-jaspersoft/
community-edition/jaspersoft-
studio_7.0.8/
23/10/2026Change log 2026-08-
14 y licencia. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 107
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
## Código Community;
terceros conservan sus
avisos.
JetBrains (s. f.). Licencia
oficial. IntelliJ Community
license. Publicación: s. f..
https://github.com/JetBrains/
intellij-community/blob/
master/LICENSE.txt
23/10/2026Apache License 2.0.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Versión publicada; IDE no
distribuido con Latch.
JetBrains (2025). Publicación
oficial. What is new in IntelliJ
IDEA 2025.2. Publicación:
## 2025.
https://www.jetbrains.com/
idea/whatsnew/2025-2/
23/10/2026Release overview.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
bcprov-jdk18on 1.86 y
licencia Bouncy Castle;
implementación Java de
## Argon2id.
Legion of the Bouncy Castle
(s. f.). Distribución oficial.
Download Bouncy Castle for
Java. Publicación: s. f..
https://www.bouncycastle.org/
download/bouncy-castle-java/
23/10/2026JDK 1.8 and later,
release 1.86 y licencia.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Atribución del estilo;
consulta limitada al acceso
público, sin evidencia de
usabilidad.
Malewicz (s. f.). Publicación
primaria de autor. Aurora UI:
New visual trend for 2021.
Publicación: s. f..
https://hype4.academy/
articles/design/aurora-ui-new-
visual-trend-for-2021
23/10/2026Título, autor,
introducción y
descripción pública.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Cobertura NIO de
alternativa comprobada.
Open Exchange Rates (s. f.-a).
Catálogo oficial JSON.
Currencies JSON.
Publicación: s. f..
https://openexchangerates.org/
api/currencies.json
23/10/2026Entrada NIO. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 108
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Alternativa con base USD
en Free y autenticación
app_id.
Open Exchange Rates (s. f.-
b). Contrato técnico oficial.
Latest JSON. Publicación: s.
f..
https://
docs.openexchangerates.org/
reference/latest-json
23/10/2026base, rates, timestamp
y app_id. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Vigencia publicada del 14
de junio de 2022;
condiciones adicionales
para servicios.
## Open Exchange Rates (2022).
Condiciones de uso oficiales.
Terms and conditions of
website use. Publicación:
## 2022.
https://openexchangerates.org/
terms
23/10/2026Other applicable terms
y limitation. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
1000 solicitudes por mes y
actualizaciones horarias; sin
cuenta creada.
Open Exchange Rates (s. f.-c).
Condiciones comerciales
oficiales. The forever free
plan. Publicación: s. f..
https://openexchangerates.org/
signup/free
23/10/2026Beneficios del plan.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Radio admitido de 0 a 63;
límite relevante aunque el
diseño inicial usa
degradados.
OpenJFX (s. f.-a). API oficial.
GaussianBlur JavaFX 21.
Publicación: s. f..
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/
effect/GaussianBlur.html
23/10/2026radiusProperty. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Sintaxis CSS de JavaFX 21,
distinta de CSS web.
OpenJFX (s. f.-b). Referencia
técnica oficial. JavaFX CSS
reference guide. Publicación:
s. f..
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/
doc-files/cssref.html
23/10/2026Looked-up colors,
selectors, controls y
effects. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Pintura radial nativa para
capas estáticas.
OpenJFX (s. f.-c). API oficial.
RadialGradient JavaFX 21.
Publicación: s. f..
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/
paint/RadialGradient.html
23/10/2026Constructors y stops.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 109
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
call se ejecuta en segundo
plano; cancelación
cooperativa y
actualizaciones observables.
OpenJFX (s. f.-d). API oficial.
Task JavaFX 21. Publicación:
s. f..
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/
concurrent/Task.html
23/10/2026Descripción, cancel y
updateProgress.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Cifrado autenticado de
respaldo; parámetros
concretos de Latch son
decisiones del proyecto.
Oracle (s. f.-a). API oficial.
Cipher Java SE 21.
Publicación: s. f..
https://docs.oracle.com/en/
java/javase/21/docs/api/
java.base/javax/crypto/
## Cipher.html
23/10/2026GCM, autenticación e
IV único. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Reutilización de cliente,
envío síncrono o asíncrono
y ciclo de recursos.
Oracle (s. f.-b). API oficial.
HttpClient Java SE 21.
Publicación: s. f..
https://docs.oracle.com/en/
java/javase/21/docs/api/
java.net.http/java/net/http/
HttpClient.html
23/10/2026send, sendAsync,
builder y cierre.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
jpackage en plataforma
destino; app-image y
runtime propio, sin
prometer instalador MSI.
Oracle (s. f.-c). Manual
oficial. Packaging overview
Java 21. Publicación: s. f..
https://docs.oracle.com/en/
java/javase/21/jpackage/
packaging-overview.html
23/10/2026Packaging pre-reqs y
Java runtime
requirements. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Mínimo 19 MiB, dos
iteraciones y paralelismo
uno; no acredita desempeño
local.
OWASP Foundation (s. f.).
Guía especializada
institucional. Password
storage cheat sheet.
Publicación: s. f..
https://
cheatsheetseries.owasp.org/
cheatsheets/
Password_Storage_Cheat_Shee
t.html
23/10/2026Argon2id y salting.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 110
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
pgJDBC 42.7.14 para Java
8 o posterior, conservado
con PostgreSQL 18.
PostgreSQL JDBC Driver (s.
f.-a). Distribución oficial.
Download. Publicación: s. f..
https://jdbc.postgresql.org/
download/
23/10/2026Latest versions, Java
- Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Redistribución BSD de dos
cláusulas.
PostgreSQL JDBC Driver (s.
f.-b). Licencia oficial
etiquetada. License de
pgJDBC 42.7.14. Publicación:
s. f..
https://
raw.githubusercontent.com/
pgjdbc/pgjdbc/REL42.7.14/
## LICENSE
23/10/2026Cláusulas 1 y 2.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
sslmode=verify-full valida
identidad y cadena; require
por sí solo no valida
identidad.
PostgreSQL JDBC Driver (s.
f.-c). Manual oficial del
controlador. Using SSL.
Publicación: s. f..
https://jdbc.postgresql.org/
documentation/ssl/
23/10/2026Configuring the client.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
MIT para API y binarios.QOS.ch (s. f.-a). Licencia
oficial. SLF4J license.
Publicación: s. f..
https://www.slf4j.org/
license.html
23/10/2026Licensing terms.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
SLF4J 2.0.20 y proveedor
JUL; un solo destino de
diagnóstico.
QOS.ch (s. f.-b). Manual
oficial. SLF4J user manual.
Publicación: s. f..
https://www.slf4j.org/
manual.html
23/10/2026Providers y logging
framework selection.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 111
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
Bloqueos por fila, orden de
adquisición e interbloqueos.
The PostgreSQL Global
Development Group (s. f.-a).
Manual oficial de PostgreSQL
- 13.3. Explicit locking.
Publicación: s. f..
https://www.postgresql.org/
docs/18/explicit-locking.html
23/10/202613.3.2, 13.3.4 y 13.3.5.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Respaldo lógico; Latch
añade exclusión para
coordinar archivos adjuntos.
The PostgreSQL Global
Development Group (s. f.-b).
Manual oficial de PostgreSQL
- 25.1. SQL dump.
Publicación: s. f..
https://www.postgresql.org/
docs/18/backup-dump.html
23/10/2026Restoring the dump y
formatos. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Integridad declarativa; las
sumas entre filas requieren
servicio y bloqueo.
The PostgreSQL Global
Development Group (s. f.-c).
Manual oficial de PostgreSQL
## 18. 5.5. Constraints.
Publicación: s. f..
https://www.postgresql.org/
docs/18/ddl-constraints.html
23/10/2026Check, unique y
foreign keys. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Una secuencia no garantiza
numeración sin huecos.
The PostgreSQL Global
Development Group (s. f.-d).
Manual oficial de PostgreSQL
## 18. 9.17. Sequence
manipulation functions.
Publicación: s. f..
https://www.postgresql.org/
docs/18/functions-
sequence.html
23/10/2026Advertencia sobre
nextval y rollback.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Licencia PostgreSQL del
motor.
The PostgreSQL Global
Development Group (s. f.-e).
Licencia oficial. License.
Publicación: s. f..
https://www.postgresql.org/
about/licence/
23/10/2026Texto completo.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.

## 112
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
PostgreSQL 18.6 soportado,
fin previsto 14 de
noviembre de 2030.
The PostgreSQL Global
Development Group (s. f.-f).
Política oficial. Versioning
policy. Publicación: s. f..
https://www.postgresql.org/
support/versioning/
23/10/2026Releases, fila
PostgreSQL 18.
Fuente primaria
identificada; alcance
limitado a la
afirmación indicada.
Coordenadas 6.3.0 y
licencia Apache 2.0 del
proyecto original.
Wooldridge (s. f.-a). Código
oficial etiquetado. HikariCP
6.3.0 POM. Publicación: s. f..
https://
raw.githubusercontent.com/
brettwooldridge/HikariCP/
HikariCP-6.3.0/pom.xml
23/10/2026Version, licenses y
maven-compiler-
plugin. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Java 11 o posterior;
propiedades del pool y
keepalive; no se usan
comparaciones
publicitarias.
Wooldridge (s. f.-b).
Documentación oficial
etiquetada. HikariCP
README de la etiqueta
6.3.0. Publicación: s. f..
https://
raw.githubusercontent.com/
brettwooldridge/HikariCP/
HikariCP-6.3.0/README.md
23/10/2026Artifacts y
## Configuration. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Guía del 11 de diciembre de
2025; no certifica
conformance de JavaFX.
## World Wide Web Consortium
(2025). Nota informativa
W3C. Guidance on applying
WCAG 2 to non-web
information and
communications technologies
WCAG2ICT. Publicación:
## 2025.
https://www.w3.org/TR/
wcag2ict-22/
23/10/2026Abstract y aplicación a
software. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.
Criterios de contraste y
foco; 2.4.13 es AAA y
Latch lo usa como meta
propia.
## World Wide Web Consortium
(2024). Recomendación W3C.
## Web Content Accessibility
Guidelines WCAG 2.2.
## Publicación: 2024.
https://www.w3.org/TR/
## WCAG22/
## 23/10/20261.4.3, 1.4.11, 2.1.1,
2.4.7 y 2.4.13. Fuente
primaria identificada;
alcance limitado a la
afirmación indicada.

## 113
Afirmación o DecisiónAutor, Tipo, Obra y
## Publicación
URL Directa o DOIConsultaSección y
## Confiabilidad
## Tablas 3, 10, 11, 13, 20, C1,
C2 y C3; tokens y pantallas.
Cuadra et al. (2026a).
Documento académico del
proyecto. Brief de diseño de
interfaz y experiencia de
usuario para Latch.
Archivo local:
Latch_Brief_UIUX_APA7_v3.
pdf
23/10/2026Documento del equipo
consultado para esta
revisión; no acredita
ejecución de Latch.
Tabla 80 y flujos; ERR y
recuperación.
Cuadra et al. (2026b).
Documento académico del
proyecto. Documento de flujo
de la aplicación de Latch.
Archivo local:
Latch_App_Flow_APA7_v3.pd
f
23/10/2026Documento del equipo
consultado para esta
revisión; no acredita
ejecución de Latch.
Doce servicios,
excepciones, DDL, puerta y
recibo.
Cuadra et al. (2026e).
Documento académico del
proyecto. Esquema de base de
datos y servicios de Latch.
Archivo local:
Latch_Backend_Schema_APA
## 7_v3.pdf
23/10/2026Documento del equipo
consultado para esta
revisión; no acredita
ejecución de Latch.
Tablas 4–5 y 8; Apéndices
A–B; calendario, TAR y
## CP.
Cuadra et al. (2026f).
Documento académico del
proyecto. Plan de
implementación de Latch.
Archivo local:
Latch_Plan_Implementacion_A
PA7_v3.pdf
23/10/2026Documento del equipo
consultado para esta
revisión; no acredita
ejecución de Latch.