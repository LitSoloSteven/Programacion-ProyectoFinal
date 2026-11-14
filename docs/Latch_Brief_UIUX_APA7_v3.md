

## 1
Brief de Diseño de Interfaz y Experiencia de Usuario para Latch
## Steven Cuadra
## Alfredo Montalvan
## Caleb Tardencilla
## Aris Gavarrete
Facultad de Ingeniería y Arquitectura, Universidad Americana (UAM)
Ingeniería de Sistemas, Sede Managua
CNU0525: Programación de Aplicaciones de Escritorio, Grupo 2
II semestre del II año
## José A. Durán García
26 de noviembre de 2026

## 2
## Resumen
Latch adopta una interfaz de escritorio con dos temas Aurora UI, Claro y Oscuro, para
administrar propiedades, unidades, inquilinos y operaciones de alquiler. Este brief transforma los
requisitos del producto y la arquitectura técnica recibidos en un contrato de presentación para
JavaFX 21 y Scene Builder. El diseño conserva superficies opacas bajo formularios, tablas e
importes, y limita los campos de luz a la identidad y la periferia de la ventana. La especificación
contiene ochenta y nueve mensajes, cuarenta pantallas y diálogos, veinte vistas principales
desarrolladas y dos especificaciones complementarias de respaldo y restauración, veintidós
wireframes, tres diagramas y ocho composiciones a color con datos sintéticos. Los tokens
distinguen texto, acciones, foco, contornos y estados financieros en ambos temas, con razones de
contraste calculadas sobre pares sólidos. Los patrones precisan pagos parciales, anulación,
garantías, progreso y cancelación de tareas concurrentes. La referencia comercial USD/NIO
permanece separada de la tasa autorizada para el pago. La investigación consulta publicaciones
originales y documentación institucional; los arquetipos y las frecuencias son supuestos de
diseño. El plan de evaluación propone revisión heurística y recorridos del equipo con metas
verificables. La revisión del documento y los cálculos de color no acreditan pruebas con
usuarios, ejecución de JavaFX ni aprobación del docente.
Palabras clave: JavaFX, Aurora UI, alquiler, interfaz, accesibilidad, usabilidad,
## Nicaragua

## 3
Brief de Diseño de Interfaz y Experiencia de Usuario para Latch
## Introducción
Latch centralizará contratos, cargos, pagos parciales, depósitos y documentos de alquiler
en una aplicación de escritorio. Este brief fija controles y comportamientos para construir las
vistas con una apariencia compartida. Sus destinatarios son los cuatro autores y el docente de
Programación de Aplicaciones de Escritorio. El PRD aporta requisitos y el TRD, restricciones
técnicas; ambos se consultaron como documentos internos suministrados (Cuadra et al., 2026c,
2026d). Las pantallas conservan los identificadores RF y los módulos M01 a M12 que permiten
relacionar controles, operaciones y pruebas.
El documento de flujo especifica los recorridos entre vistas (Cuadra et al., 2026b); el
esquema de base de datos y servicios precisa la persistencia (Cuadra et al., 2026e), y el plan de
implementación organiza tareas, dependencias y capacidad (Cuadra et al., 2026f). Estas
referencias complementan los requisitos del PRD y la arquitectura del TRD. La modalidad
académica utiliza investigación documental y datos sintéticos; no incluye entrevistas,
observación de propietarios ni registros privados. El resumen académico aportado define RA1 a
RA5 y sus indicadores, sin que esta consulta equivalga a haber leído el syllabus original de la
asignatura.

## 4
Wang (2023) revisa veintidós documentos de 2012 a 2022 sobre transformación digital
inmobiliaria con un análisis bibliométrico. La consulta alcanzó el resumen editorial, que
identifica la gestión de información como tema de investigación. Ese antecedente internacional
orienta los registros y reportes de Latch, sin atribuir resultados al artículo completo. La
pertinencia para pequeños propietarios de Managua permanece como hipótesis del escenario
académico. El proyecto no midió demanda local, productividad ni preferencias de participantes
reales, por lo que conserva metas de evaluación propuestas.
Supuestos y Límites de la Especificación
La planificación toma el 22 de octubre de 2026 como fecha base del proyecto y propone
entregar documentos y versión demostrable el 26 de noviembre. Esa entrega es una fecha interna
de trabajo, no una confirmación del calendario institucional. El contexto aportado sitúa
presentación y evaluación el 26 de noviembre y el 1 de diciembre de 2026. La consulta de
fuentes de este brief corresponde al 23 de octubre de 2026. Las responsabilidades y la capacidad
de doce horas semanales por integrante son estimaciones propuestas, sin representar acuerdos,
disponibilidad confirmada ni horas trabajadas.
La Tabla 1 presenta alcance y Categoría de la Evidencia.
## Tabla 1
Alcance y Categoría de la Evidencia
ElementoValor AdoptadoNaturaleza
Alcance funcionalM01 a M12; propietario y asistente; inquilino recibe
documentos
Decisión del
proyecto
ExclusionesContabilidad completa, fiscalidad, pasarelas,
móvil/web, portal, mensajería, firma electrónica y
## OCR
Límite del alcance
Datos de figurasPropietario Demo; Inquilino Demo A; CT-001;
A-01; montos de demostración
## Sintéticos

## 5
ElementoValor AdoptadoNaturaleza
Entorno de
evaluación
Windows 11 x64; 8 GiB RAM; SSD; PostgreSQL
local; hasta tres clientes LAN
Supuesto de prueba
## V1
## Evidencia
disponible
Revisión documental, composición de figuras y
cálculo de contraste
No equivale a
ejecución de Latch
Compatibilidad con los Documentos Recibidos
El PRD y el TRD conservan PostgreSQL y la dirección Aurora con preferencias locales
para los dos roles. El brief adopta sus restricciones transaccionales, la paginación de cincuenta
filas y las tasas autorizadas por fecha. La Tabla 2 registra la base técnica consultada y diferencia
existencia publicada de integración ejecutada. Durante el diseño se sustituyeron los colores y
radios iniciales por los tokens del contrato visual del proyecto. La Tabla 33 documenta esa
decisión histórica, sin modificar entidades, permisos ni requisitos funcionales compatibles.
La Tabla 2 presenta base Técnica Conservada y Verificación Documental.
## Tabla 2
Base Técnica Conservada y Verificación Documental
ComponenteVersión AdoptadaSustento y Alcance
Java / JavaFXJDK 21; OpenJFX 21.0.13JavaFX 21.0.13 figura para Windows x64;
misma familia del TRD (Gluon, s. f.).
Base de datosPostgreSQL 18.6; pgJDBC
## 42.7.14
18.6 soportada hasta 2030; controlador Java 8
compatible con JDK 21 (The PostgreSQL
Global Development Group, s. f.; PostgreSQL
JDBC Driver, s. f.).
ORMHibernate 7.4.6.FinalPublicado el 23/08/2026; rama admite Java 21.
Se conserva versión del proyecto aunque la
página enumera revisiones posteriores
(Hibernate, s. f.).
ReportesJasperReports 7.0.8; Studio
7.0.8 según TRD
La biblioteca y POM están publicados; bytecode
Java 8. DTO, sin adaptador Hibernate
6.3.1.Final (Jaspersoft, s. f.-a, s. f.-b).
HerramientasFXML; Scene Builder 21;
Maven; IntelliJ IDEA;
Git/GitHub
Distribución del TRD. No se ejecutó
construcción ni se validó integración binaria de
## Latch.

## 6
Dirección Visual Aurora UI de Latch
Malewicz (s. f.) presenta Aurora UI como fondos orgánicos de degradados difuminados
en la parte pública de su artículo. La consulta comprende autor, título, introducción y descripción
breve disponibles en HYPE4 Academy/SquarePlanet. El contenido restringido por suscripción
no se consultó y la publicación no se utiliza como evidencia empírica de usabilidad. Latch adopta
la referencia por decisión del proyecto y define su propia paleta, geometría y organización
administrativa. La aplicación mantiene JavaFX, FXML y Scene Builder, sin incorporar
bibliotecas homónimas ni trasladar la interfaz a tecnologías web.
La identidad ocupa el acceso, el marco de la ventana y las zonas periféricas del
encabezado. Los importes, errores y entradas se leen sobre superficies uniformes cuya opacidad
es uno en ambos temas. Los campos violeta, turquesa y lavanda no representan cobros, deuda o
mora y permanecen estáticos durante las operaciones. El modo sin efectos elimina esas luces y
conserva el fondo sólido del tema seleccionado. La Figura 1 separa las capas decorativas, de
lectura e interacción para que la implementación no aplique transparencias a los datos
financieros ni capture el ratón con nodos de fondo.
La Figura 1 muestra capas de Composición Aurora en JavaFX.

## 7
## Figura 1
Capas de Composición Aurora en JavaFX
La Tabla 3 presenta geometría de las Luces Estáticas.
## Tabla 3
Geometría de las Luces Estáticas
CampoCentro y Radio ProporcionalesColor y Alfa Claro / Oscuro
Violetax 0.10; y 0.08; radio 0.48Color #6D5EF5; 0.20 / 0.16
Turquesax 0.84; y 0.02; radio 0.42Color #4CC9C0; 0.18 / 0.14
Lavandax 0.94; y 0.88; radio 0.40Color #B9AEFF; 0.14 / 0.12
InterpolaciónStops 0, 0.5 y 1; NO_CYCLEAlfa a, a × 0.25 y 0; mismo RGB para
evitar halos oscuros
ContenedorRectangle por luz, ancho/alto enlazados
al Pane
mouseTransparent true;
focusTraversable false; sin hilos
decorativos

## 8
La API RadialGradient permite definir centro, radio y paradas de color para dibujar las
luces con nodos independientes (OpenJFX, s. f.-e). El diseño inicial prescinde de desenfoque
adicional porque la caída del degradado suaviza el borde. GaussianBlur admite radios de cero a
sesenta y tres en JavaFX 21, límite que restringe cualquier ajuste posterior del fondo (OpenJFX,
s. f.-a). Las coordenadas de la Tabla 3 son proporcionales al contenedor y no a una pantalla
física. Las composiciones editoriales ilustran el resultado esperado sin afirmar que proceden de
un renderizado de JavaFX.
Objetivos y Principios de Diseño
La experiencia debe permitir identificar la siguiente acción y explicar el saldo antes de
confirmar un movimiento. Las metas de la Tabla 4 convierten ese propósito en criterios de
evaluación académica con escenarios sintéticos. Los tiempos incluyen lectura y corrección del
recorrido, y no equivalen a rendimiento medido de la aplicación. El equipo revisará el resultado
en Claro y Oscuro con las luces activas y desactivadas. Cada recorrido conservará el mismo
contrato y los mismos importes para comparar la presentación sin introducir cambios de
dificultad entre las condiciones de evaluación propuestas.
La Tabla 4 presenta objetivos Medibles de Experiencia.
## Tabla 4
Objetivos Medibles de Experiencia
ObjetivoMeta de AceptaciónEvidencia Prevista
Localizar acción
principal
4 de 4 revisores la identifican en 10 s
por vista
Recorrido cognitivo con captura
anotada
Leer carteraIdentificar saldo y vencimiento sin
error en 30 s
CT-001 sintético, dos monedas
separadas
Pago parcialCompletar en 120 s, cero duplicados
y saldo correcto
Pago C$ 3,662.43 sobre US$
## 200.00
## Recuperar
validación
Corregir primer error en 45 s sin
perder campos válidos
Fecha o monto inválido

## 9
ObjetivoMeta de AceptaciónEvidencia Prevista
Operar por teclado10 recorridos críticos; cero bloqueos
de foco
RNF-009; Tab, Shift+Tab, Enter y
## Escape
## Cambiar
presentación
≤ 500 ms; conservar 20
campos/filtros y tareas
RNF-049; comparación de estado
antes/después
La Tabla 5 presenta principios Aplicados en Latch.
## Tabla 5
Principios Aplicados en Latch
PrincipioAplicación Concreta
Jerarquía de tareaUn primario por formulario: Guardar, Confirmar pago o Generar;
secundarios separados.
Lectura financieraImportes a la derecha, moneda visible y resumen de aplicación antes de
cobrar.
Historia verificableAnular conserva número; reimprimir no registra otro pago; garantía
separada de renta.
## Corrección
localizada
Mensaje junto al campo, resumen enlazado y foco al primer error.
Control de la
persona
Cancelar entre contratos; Escape no confirma operaciones financieras.
Continuidad visualTema cambia colores sin recargar FXML ni perder selección o filtros.
ReconocimientoRótulos del catálogo y nombres de contrato/unidad visibles en cada
operación.
Permiso coherenteAcciones administrativas restringidas en pantalla y servicio; motivo de
bloqueo legible.
Acceso equivalenteTexto e icono, teclado completo y foco visible con o sin efectos.
Densidad ajustableColumnas flexibles, formularios desplazables y filas ampliables para
contenido largo.

## 10
Nielsen (1994) reúne diez heurísticas generales para revisar interfaces y describe su
origen en problemas de usabilidad. El equipo las utiliza como una guía de inspección, sin
convertirlas en una validación realizada con propietarios. La Tabla 6 vincula cada heurística con
una decisión verificable en Latch y limita la paráfrasis a sus principios generales. Las soluciones
concretas, los atajos y los umbrales pertenecen al diseño del proyecto. La revisión registrará
severidad, pantalla, reproducción y corrección propuesta para que una observación visual pueda
convertirse en una tarea de implementación con responsable nominal.
La Tabla 6 presenta heurísticas de Nielsen y Decisiones de Interfaz.
## Tabla 6
Heurísticas de Nielsen y Decisiones de Interfaz
HeurísticaDecisión de Latch
Visibilidad del estadoH1 muestra procesados, total y Cancelar; tasas indican fecha.
Correspondencia con el mundo
real
Canon, cargo, recibo y garantía usan el glosario del proyecto.
Control y libertadVolver conserva filtros; cancelar lote respeta contratos
confirmados.
Consistencia y estándaresMismos rótulos y estados en ambos temas y documentos.
Prevención de erroresResumen de pago y validación antes de activar contrato.
Reconocer antes que recordarContrato, unidad y saldo permanecen visibles al cobrar.
Flexibilidad y eficienciaAtajos por contexto y búsqueda paginada.
Estética y diseño mínimoAurora en periferia; contenido opaco sin adornos sobre cifras.
Reconocer y corregir erroresMensaje identifica causa, campo y acción de recuperación.
Ayuda y documentaciónAyuda contextual explica tasa, saldo a favor y anulación.

## 11
Usuarios, Contextos y Tareas
Los arquetipos de la Tabla 7 son construcciones sintéticas para revisar tareas y permisos
del producto. Sus frustraciones son hipótesis que organizan el recorrido y no testimonios
obtenidos mediante entrevistas. El Propietario administra parámetros y operaciones que alteran la
historia financiera, mientras el Asistente realiza la operación diaria dentro de sus permisos. El
inquilino recibe recibos y estados de cuenta sin iniciar sesión en Latch. El equipo utilizará estas
diferencias para comprobar visibilidad de acciones y recuperación de errores con escenarios
reproducibles, sin atribuir al diseño una representación estadística de propietarios de Managua.
La Tabla 7 presenta arquetipos Sintéticos de Operación.
## Tabla 7
Arquetipos Sintéticos de Operación
ArquetipoObjetivos y Frustraciones HipotéticasUso y Entorno
Propietario DemoRevisar cartera y justificar una deducción;
dificultad hipotética para reconstruir pagos
parciales. Nivel técnico básico.
Diario 20 a 40 min;
escritorio con teclado y
ratón; rol Propietario.
Asistente DemoRegistrar cobros y encontrar recibos; dificultad
hipotética al distinguir tasa referencial de
autorizada. Nivel técnico intermedio.
4 días por semana, 1 a 2 h;
equipo compartido con
sesión individual; rol
## Asistente.
La Tabla 8 presenta frecuencia e Importancia de las Tareas Como Supuestos.
## Tabla 8
Frecuencia e Importancia de las Tareas Como Supuestos
TareaFrecuencia PrevistaImportancia y Justificación
Cobrar y emitir reciboDiariaAlta; modifica deuda y requiere
evitar repetición.
Revisar cartera y
vencimientos
DiariaAlta; identifica cargos exigibles y
próximas renovaciones.
Mantener contactos y
contratos
SemanalAlta; respalda unidad, fechas y
montos del cobro.

## 12
TareaFrecuencia PrevistaImportancia y Justificación
Generar cargos mensualesMensual; reintento
controlado
Alta; afecta múltiples contratos.
Atender mantenimientoSemanal o por eventoMedia; separa gasto del propietario y
cargo al inquilino.
Liquidar garantíaAl terminar un contratoAlta; exige conceptos y control del
remanente.
Consultar tasa y reportesDiaria para referencia;
semanal para reportes
Media en consulta; alta cuando
respalda una operación.
Administrar usuarios y
respaldo
Por cambio de acceso;
respaldo manual diario
con alerta a las 24 h
Alta por integridad y continuidad de
datos.
El entorno objetivo adopta Windows 11 x64, teclado completo y ratón, con una
referencia de 1440 por 900 unidades lógicas. La ventana tiene una meta mínima de 1366 por 768
unidades lógicas para conservar tabla, filtros y acción principal. Estas cifras no describen un
equipo de participantes y se verificarán como condiciones de diseño. La iluminación supuesta es
interior uniforme, entre 300 y 500 lux, sin que el proyecto haya realizado mediciones
ambientales. La elección de Claro u Oscuro queda en manos de cada usuario y no depende de
una detección automática de Windows.
La matriz de escalado de 100 %, 125 %, 150 % y 200 % exige distinguir píxeles físicos
de unidades lógicas de JavaFX. Una ventana de 1366 por 768 unidades lógicas requiere
aproximadamente 2049 por 1152 píxeles físicos al 150 %, antes de considerar bordes del
sistema. Si el escritorio disponible es menor, la presentación compacta reduce el lateral a 192 y
los márgenes a 16, usa formularios de una columna y permite desplazamiento central. La meta
Should del PRD al 200 % en 1920 por 1080 se evalúa en esta misma matriz mediante la
adaptación, sin prometer que la ventana mínima cabe físicamente.

## 13
Arquitectura de Información y Navegación
La aplicación utiliza una ventana principal con barra lateral de módulos, barra superior de
contexto y contenido central reemplazable. Los formularios acotados y confirmaciones utilizan
diálogos modales vinculados a la ventana de origen. El mapa de la Figura 2 agrupa los doce
módulos sin duplicar su navegación en MenuBar. El rol determina las acciones disponibles
dentro de cada vista y conserva rótulos reconocibles para las tareas permitidas. Al cerrar un
diálogo, el foco regresa al control que lo abrió y la lista mantiene búsqueda, página y selección,
salvo que el registro haya dejado de cumplir el filtro.
La Figura 2 muestra mapa de Navegación por Módulos.
## Figura 2
Mapa de Navegación por Módulos
La Tabla 9 presenta menús y Comandos por Contexto.

## 14
## Tabla 9
Menús y Comandos por Contexto
SuperficieEstructura AdoptadaPropósito
MenuBar ArchivoCerrar sesión; Cerrar aplicaciónSesión y H6; Alt+A abre
menú.
MenuBar VerActualizar; Ir a búsqueda; Tema
Claro/Oscuro; Sin efectos decorativos
Acceso por teclado a
presentación y consulta;
selección sincronizada con
barra superior.
MenuBar
## Operaciones
Acción principal de la vista; acciones
autorizadas del registro
Equivalente accesible al
contexto de tabla, sin
repetir módulos.
MenuBar AyudaAyuda de la vista; Glosario; Acerca de LatchExplicación de términos y
versión académica.
ToolBar de
reportes
Anterior; Siguiente; Página n de total; PDF;
XLSX solo para reportes tabulares
Visor integrado
PAN-11/PAN-16; imprimir
desde el PDF exportado.
ContextMenu de
tablas
Abrir; Editar si procede; Ver historial; Anular
o Dar de baja según entidad y rol
Actúa sobre fila
seleccionada; tecla Menú o
Shift+F10.
La Tabla 10 presenta tabulación y Atajos de Teclado.
## Tabla 10
Tabulación y Atajos de Teclado
ÁmbitoOrden o AtajoComportamiento
AccesoUsuario; contraseña; Ingresar;
Claro/Oscuro; Sin efectos
Tab y Shift+Tab; flechas dentro del
ToggleGroup.
Ventana principalMenú; lateral; contexto; filtros;
tabla; paginación; acciones
F6 cambia región; no atrapar Tab en la
tabla.
FormularioCampos de arriba hacia abajo y de
izquierda a derecha; ayuda;
## Guardar; Cancelar
Label.labelFor enlaza campo;
validación lleva al primer error.
Global de
aplicación
Alt+T; F5; Ctrl+F; F1Enfocar tema; actualizar; buscar;
ayuda. No consumir atajos del editor.
CRUD contextualCtrl+N; Ctrl+S; Enter; EscapeNuevo; guardar; abrir seleccionado;
volver o cancelar. Enter no cobra por
defecto.

## 15
ÁmbitoOrden o AtajoComportamiento
FinancieroCtrl+P en contrato; Ctrl+Enter en
pago
Abrir pago; abrir resumen. Confirmar
exige botón explícito.
## Visor
## PAN-11/PAN-16
Ctrl+S; PageUp/PageDownExportar PDF; páginas cuando el foco
no está en entrada. Ctrl+P no tiene
acción en el visor.
Mnemónicos de
MenuBar
Alt+A; Alt+V; Alt+O; Alt+YArchivo; Ver; Operaciones; Ayuda.
Reservados globalmente; Ayuda usa Y.
Formulario de
contrato PAN-08
Ctrl+Shift+AAbrir confirmación de activación; no
confirma la operación. Alt+A abre
## Archivo.
## Garantías
## PAN-12
Alt+LAbrir liquidación si el rol lo permite.
## Mantenimiento
## PAN-13
Alt+ECambiar estado con confirmación.
Tasas PAN-14Alt+MAbrir tasa manual para el Propietario.
Reportes PAN-15
y lote PAN-20
Alt+GGenerar tras validar parámetros o
revisar resumen.
## Configuración
## PAN-17
Alt+RAbrir respaldo manual.
Tabla y contextoMenú; Shift+F10Abrir ContextMenu de la fila
seleccionada.
Selector y
controles nativos
Flechas; Space; Tab; Shift+TabCambiar opción, activar selección y
recorrer controles según foco.
La geometría de navegación permanece igual entre Claro y Oscuro para que cambiar la
presentación no cambie la tarea. La Tabla 10 es el registro único de atajos y mnemónicos. Alt+A
queda reservado para Archivo; Ctrl+Shift+A abre la confirmación de activación en PAN-08. El
visor permite exportar y recorrer páginas; la impresión se realiza desde el PDF exportado. La
barra lateral permite recorrer módulos con Tab y activar con Enter, mientras cada tabla conserva
sus teclas nativas de selección. F6 visita regiones sin cerrar controles desplegables de forma
inesperada. Los formularios que sobrepasan el alto visible desplazan el contenido hasta mostrar
el foco, y los diálogos restauran la posición de lectura al regresar a la ventana principal.

## 16
Sistema de Diseño
El sistema de diseño separa colores de referencia, tokens semánticos y reglas de
componente para mantener los mismos nombres en los dos temas. La Tabla 11 presenta la paleta
de uso cotidiano y el Apéndice C contiene la matriz completa con valor Claro, valor Oscuro, uso,
estado y propiedad JavaFX. Los colores de la aurora permanecen en la capa decorativa y los
estados financieros utilizan su propia pareja de texto y fondo. Ningún panel de contenido reduce
su opacidad para dejar pasar luces, por lo que el contraste de lectura depende de pares sólidos
reproducibles.
La Tabla 11 presenta paleta Semántica de los Dos Temas.
## Tabla 11
Paleta Semántica de los Dos Temas
RolClaroOscuro
background#F5F7FC#0F1424
surface#FFFFFF#182136
elevated#FFFFFF#202C44
text#17213A#F1F5F9
muted#4B5568#B7C3D8
accent#4F46E5#A5B4FC
on-accent#FFFFFF#111827
focus#4338CA#C4B5FD
border#64748B#94A3B8
decorative#D5DCE8#334155
success#166534#86EFAC
warning#854D0E#FDE68A
error#B42318#FCA5A5
info#1E40AF#93C5FD

## 17
Contraste Calculado y Superficies
World Wide Web Consortium (W3C, 2024) define luminancia relativa a partir de los
canales sRGB y la relación entre el color más claro y el más oscuro. El cálculo usa cada canal
dividido entre 255, linealiza con el umbral 0.04045 y obtiene L mediante 0.2126R + 0.7152G +
0.0722B. La razón es (L mayor + 0.05) / (L menor + 0.05). El algoritmo compara el resultado de
precisión doble con el umbral antes de formatear decimales para presentación; las razones de las
tablas no son estimaciones visuales de contraste.
La Tabla 12 presenta contraste de Pares Funcionales Principales.
## Tabla 12
Contraste de Pares Funcionales Principales
TemaTexto o Señal / FondoRazónUmbral y Dictamen
Claro#17213A / #FFFFFF15.96870818:14.5:1; Cumple
Claro#4B5568 / #FFFFFF7.50411060:14.5:1; Cumple
Claro#FFFFFF / #4F46E56.28752860:14.5:1; Cumple
Claro#64748B / #FFFFFF4.75884279:13:1; Cumple
Claro#4338CA / #FFFFFF7.90311148:13:1; Cumple
Claro#166534 / #F0FDF46.81165163:14.5:1; Cumple
Claro#854D0E / #FFFBEB6.60658223:14.5:1; Cumple
Claro#B42318 / #FEF2F26.00989513:14.5:1; Cumple
Claro#1E40AF / #EFF6FF8.01483692:14.5:1; Cumple
Claro#4F46E5 / #FFFFFF6.28752860:14.5:1; Cumple
Oscuro#F1F5F9 / #18213614.63676799:14.5:1; Cumple
Oscuro#B7C3D8 / #1821369.01850224:14.5:1; Cumple
Oscuro#111827 / #A5B4FC8.89860092:14.5:1; Cumple
Oscuro#94A3B8 / #1821366.25373330:13:1; Cumple
Oscuro#C4B5FD / #1821368.68558820:13:1; Cumple
Oscuro#86EFAC / #12352A9.53585379:14.5:1; Cumple
Oscuro#FDE68A / #3A2E1610.65685380:14.5:1; Cumple
Oscuro#FCA5A5 / #3B20287.77432268:14.5:1; Cumple

## 18
TemaTexto o Señal / FondoRazónUmbral y Dictamen
Oscuro#93C5FD / #1A30487.46371476:14.5:1; Cumple
Oscuro#A5B4FC / #1821368.04339562:14.5:1; Cumple
Nota. Cálculo realizado para este brief con sRGB de 8 bits. La decisión usa la razón sin
redondear; la tabla muestra ocho decimales. Los pares completos, incluida selección, hover,
presión y controles inactivos, están en el Apéndice C. Las luces no quedan debajo del contenido.
El texto normal utiliza una meta mínima de 4.5:1 y el texto grande de 3:1 conforme a la
explicación de contraste de W3C (s. f.-b). La definición de texto grande corresponde al menos a
18 puntos o 14 puntos en negrita; Latch exige 4.5:1 también a sus rótulos de 18 unidades lógicas.
Las señales necesarias de controles y foco deben alcanzar 3:1 frente a sus colores adyacentes
(W3C, s. f.-a). El borde decorativo tenue no identifica campos y se acompaña de un contorno
funcional propio con contraste suficiente.
Tipografía, Espaciado e Iconografía
La Tabla 13 presenta escala y Geometría Compartidas.
## Tabla 13
Escala y Geometría Compartidas
RecursoValor AdoptadoAplicación
Familia de
aplicación
Segoe UI; alternativa Arial;
última opción System
Resolver con Font.getFamilies; no
incluir archivos de fuentes de Windows
en el paquete.
Escala lógica12 metadatos; 14 cuerpo; 18
sección; 28 título
JavaFX -fx-font-size; no confundir
unidades lógicas con puntos del brief.
Espaciado4, 8, 12, 16, 24 y 32UiMetrics; padding y gap de
GridPane/HBox/VBox.
AlturasControles mínimo 36; fila
inicial 40
Ampliar con contenido multilínea;
minHeight, sin maxHeight que recorte.
Radios8 controles; 12 paneles y
diálogos
-fx-background-radius y -fx-border-
radius compatibles.
EstructuraLateral 224; superior ≥ 64;
margen central 24
Compacta: 192 y 16; ScrollPane central;
dos columnas a una si hace falta.

## 19
RecursoValor AdoptadoAplicación
Iconos propiosSVGPath geométrico de 16 o
20; trazo 2
Más para crear, documento para recibo,
advertencia con signo, marca para
confirmado.
ElevaciónSolo diálogo: dropshadow
gaussiano, radio 12, alfa 0.14, y
## 4
No aplicar resplandor por control;
paneles se separan con espacio y borde
decorativo.
Las cifras monetarias se alinean a la derecha y conservan siempre dos decimales con la
moneda visible. La aplicación evita cambiar de familia dentro de una columna, para que el ancho
de los importes sea estable durante la comparación. Los iconos propios tienen una función
reconocible y acompañan un rótulo o un nombre accesible. El indicador de módulo seleccionado
añade una banda de cuatro unidades y un cambio de fondo que no reemplaza el texto. Los rótulos
largos pueden ocupar dos líneas y la acción principal conserva una altura mínima que aumenta
cuando el contenido lo requiere.
Estados de Componentes
La Tabla 14 presenta contrato de Estados de Controles.
## Tabla 14
Contrato de Estados de Controles
ComponenteEstados y Comportamiento Compartido
BotónNormal: contorno border; hover: hover; presionado: pressed. Primario usa
accent, accent-hover y accent-pressed con on-accent. Foco externo con
separación de 2 y anillo de 2. Deshabilitado conserva texto legible y causa
asociada.
FormularioEntrada sólida; contorno funcional; hover cambia contorno a focus. Error
añade texto e icono y borde error. Error con foco muestra ambos anillos
separados. Deshabilitado usa disabled-bg/text sin opacidad global.
TableView /
ListView
Normal y fila alterna sobre surface; hover propio; seleccionado usa
selection, banda y texto text; foco propio. Vacío con mensaje y acción;
carga con indicador externo; error mantiene filtros.
Menús y contextoFondo elevated; texto text; hover y foco con selection y contorno focus;
seleccionado incluye marca. Deshabilitado conserva muted y explicación
en ayuda. No hay estado error en comando sin entrada.

## 20
ComponenteEstados y Comportamiento Compartido
Diálogo / AlertPanel sólido; encabezado y botones heredan tema; foco inicial en opción
segura. Error en contenido persistente; carga no bloquea Cancelar; éxito
cierra y devuelve foco tras commit.
ComboBox /
DatePicker
Popup elevated; opciones hover/selección/foco; fecha elegida rotulada y
contorneada. Estado inválido en editor; deshabilitado sin transparencia.
Calendario y lista reciben el tema al abrirse.
CheckBox /
RadioButton
Normal con borde border; hover de apoyo; seleccionado con marca o
punto, además de etiqueta. Foco visible del control; deshabilitado con
explicación legible.
## Progreso /
pestañas
ProgressBar usa accent y texto n de total; cancelación explícita. Pestaña
seleccionada con fondo y rótulo; foco y navegación por flechas nativos. Sin
animación decorativa.
OpenJFX (s. f.-b) documenta colores consultados desde el árbol de estilos y propiedades
específicas del motor CSS de JavaFX. Latch usa nombres como -latch-surface para color y clases
de componente para aplicar las propiedades compatibles. La geometría utiliza constantes
UiMetrics o propiedades del control, sin variables web var(--token). El pseudoestado invalid es
propio y el controlador lo activa con pseudoClassStateChanged cuando falla la validación. Los
estilos completos del Apéndice C incluyen tablas, formularios, menús, diálogos, calendarios y
listas desplegables, con el mismo contrato semántico para Claro y Oscuro.
La Tabla 15 presenta controles del Curso y Uso en Latch.
## Tabla 15
Controles del Curso y Uso en Latch
Control JavaFXUso Concreto y Condición
Label, ButtonRótulos labelFor, mensajes y acciones específicas;
accessibleText cuando el texto no basta.
TextField, PasswordFieldBúsqueda, contactos y montos; contraseña oculta, sin copiarla
a logs.
ComboBox, DatePickerCatálogos cerrados y fechas; editor valida formato
dd/MM/aaaa.
ListView, TableViewCatálogo de reportes y registros paginados; navegación y
selección nativas.

## 21
Control JavaFXUso Concreto y Condición
CheckBox, RadioButtonSin efectos, filtros booleanos, Claro/Oscuro y método de
distribución.
ImageViewFoto opcional y página renderizada de reporte; texto
alternativo; no como botón.
MenuBar, ToolBar,
ContextMenu
Sesión y ayuda; visor; acciones de fila autorizadas.
Alert, DialogConfirmación, errores de operación y formularios acotados;
propietario de ventana explícito.
FileChooser, DirectoryChooserAdjuntos y exportación; carpeta de respaldo. Aspecto nativo
depende de Windows.
ProgressBar, ProgressIndicatorJustificados por H1 y H5 y consultas sin duración conocida;
acompañados por texto.
TabPane, ScrollPane,
## Pagination
Justificados por expediente, alto variable y páginas de 50 filas;
sin dependencia externa.
Tooltip, TextArea, SVGPathAyuda complementaria, conceptos/motivos multilínea e iconos
propios; no sustituyen rótulos.
Patrones de Interacción
Cada formulario mantiene valores válidos cuando aparece un error y explica la causa
junto al campo afectado. Guardar ejecuta la validación local antes de invocar el servicio,
mientras la restricción de dominio vuelve a comprobarse en la transacción. El primer error recibe
foco y el resumen superior contiene vínculos a los campos que deben corregirse. Los mensajes se
mantienen hasta una corrección o una acción explícita, sin desaparecer por un temporizador. La
Tabla 16 define comportamientos comunes y ejemplos; el número definitivo del recibo solo
aparece después de confirmar el pago en la base activa.
La Tabla 16 presenta patrones de Operación y Recuperación.
## Tabla 16
Patrones de Operación y Recuperación
PatrónComportamiento Esperado y Ejemplo
Alta y ediciónTítulo indica Nuevo o Editar; Ctrl+S valida; Cancelar revisa cambios.
Ejemplo: dirección vacía produce MSG-004 sin borrar nombre.

## 22
PatrónComportamiento Esperado y Ejemplo
Búsqueda y filtrosEspera 300 ms tras última tecla; Enter ejecuta inmediato; restablecer
elimina solo filtros. Tabla de 50 filas, conteo y orden estable por campo
más id.
Ordenamiento y
paginación
Encabezado clic o teclado cambia ascendente/descendente, con texto
accesible. Filtrar vuelve a página 1; editar conserva página si la fila sigue
visible.
## Destructiva
autorizada
Mostrar entidad, consecuencia y motivo si aplica; foco inicial en Cancelar.
Baja lógica de unidad no elimina historia y exige no tener contrato activo.
## Anulación
financiera
El rótulo es Anular recibo; conserva número, usuario, fecha y motivo.
Nunca sustituir por Eliminar ni habilitar al Asistente.
Éxito y errorConfirmación posterior a commit; error identifica recuperación. Pago
confirmado con PDF fallido produce MSG-033 y permite exportar sin
repetir pago.
Vacío y sin
coincidencias
Catálogo inicial ofrece crear según rol; filtro sin coincidencias ofrece
cambiar filtros. No confundir cero filas con error o carga.
Carga y tareasConsulta muestra indicador y mensaje; H1 muestra n de total y Cancelar;
H5 cancela sin publicar temporal. Ningún controlador espera Future.get o
join.
Pago parcial y
crédito
Resumen muestra recibido, equivalente, aplicaciones y remanente. US$
200.00 menos US$ 100.00 deja US$ 100.00; excedente es saldo a favor.
Resultado inciertoConservar UUID y bloquear nuevo envío. Consultar estado por
idempotencia antes de reintentar; MSG-064 explica la situación.
ConcurrenciaSi otra sesión cambia saldo, recargar resumen y solicitar confirmación
nueva. H4 bloquea contador transaccional; UI no reserva consecutivos.
Cierre y sesiónH6 espera tareas; a 10 s usa MSG-061 y mantiene proceso. Cambios sin
guardar abren PAN-39; inactividad de 15 min requiere gestionar tareas sin
cerrar a la fuerza.

## 23
Selector y Restauración de Preferencias
El selector de tema presenta dos RadioButton rotulados Claro y Oscuro dentro de un
ToggleGroup. Alt+T lleva el foco al grupo y las flechas cambian la opción con el punto de
selección visible, sin depender de un sol o una luna. La opción Sin efectos decorativos utiliza un
CheckBox independiente y funciona con los dos temas. El cambio actualiza las escenas
existentes y sus popups sin recargar formularios ni detener tareas. La Figura 3 muestra la
restauración de preferencias antes y después del acceso, con valores ausentes o ilegibles resueltos
mediante Claro.
La Figura 3 muestra restauración y Persistencia Local del Tema.
## Figura 3
Restauración y Persistencia Local del Tema

## 24
Oracle (s. f.) describe Preferences como un árbol persistente de preferencias con un
almacén dependiente de la implementación. Latch utiliza el nodo local
ni/edu/uam/latch/presentation, con lastTheme antes de autenticar y users/UUID/theme después
del acceso. El UUID identifica al usuario de la aplicación sin usar su nombre como clave. El
guardado copia la elección personal y la última elección local, mientras un fallo conserva el
cambio en memoria y muestra MSG-003 una vez por sesión. La restauración de otra cuenta carga
su propia elección y deja intactos permisos y datos de negocio.
La Tabla 17 presenta contrato del Cambio de Presentación.
## Tabla 17
Contrato del Cambio de Presentación
MomentoAcción y Conservación de Estado
Inicio sin sesiónLeer lastTheme y simple; inválido o lectura fallida: Claro y efectos
normales. No inferir tema de Windows.
AutenticaciónLeer users/UUID/theme y simple; ausente: Claro y false. Aplicar a ventana
principal y nuevos diálogos.
Cambio explícitoSustituir hoja de paleta y actualizar fondo en hilo JavaFX; no reemplazar
Scene ni recargar FXML. Conservar nodo enfocado, texto, selección,
errores y scroll.
Popups ya abiertosRegistro de escenas de Window; aplicar hojas a ContextMenu, Tooltip,
DatePicker y ComboBox. No cerrar popup para aplicar tema.
Cerrar sesiónConservar último tema local; antes de autenticar otra cuenta usar
lastTheme; al identificarla, preferencia propia o Claro.
Fallo de escrituraSeguir usando tema en sesión; MSG-003; guardar fuera del hilo gráfico.
No reintentar por cada pulsación.
Límite nativoFileChooser y DirectoryChooser conservan apariencia de Windows; la
aplicación no simula control de su tema.

## 25
Tasas, Caché y Estado de Conexión
AYR Tech (Pty) Ltd. documenta solicitudes estándar con base seleccionable y NIO en su
catálogo de monedas (s. f.-c, s. f.-d). Latch conserva ExchangeRate-API Free autenticado para la
consulta informativa prevista en el PRD. El plan publica actualización diaria y mil quinientas
solicitudes mensuales en su comparación oficial (AYR Tech (Pty) Ltd., s. f.-b). La autenticación
Bearer permite mantener la clave fuera de la URL (AYR Tech (Pty) Ltd., s. f.-a). Esta elección
mantiene una frecuencia compatible con el escenario académico y no implica contratación, una
cuota garantizada ni una consulta autenticada ejecutada.
La Tabla 18 presenta contrato de Integración y Presentación USD NIO.
## Tabla 18
Contrato de Integración y Presentación USD NIO
AspectoDecisión Cerrada
Endpoint y secretoGET https://v6.exchangerate-api.com/v6/latest/USD; Authorization
Bearer. Leer LATCH_FX_API_KEY o archivo externo con ACL; nunca
incorporar la clave en FXML, Preferences, logs ni repositorio.
JSON aceptadoresult success; base_code USD; conversion_rates.NIO decimal finito en
banda técnica 1–100 NIO/USD inclusive; time_last_update_unix ≤ ahora
< time_next_update_unix para rotular vigente. Fechas futuras
incoherentes se rechazan. Fuera de banda: no guardar como referencia
vigente; registrar en bitacora M09, REFERENCIA_EN_CUARENTENA,
con valor y huella. No crear tabla de cuarentena.
Actualización y
caché
H3 cada 24 h; servidor LAN comparte caché y control de última consulta.
Conservar origen, tasa, base, destino, cotización, descarga y próxima
actualización.
Límites y fallas1 consulta diaria planificada; máximo 31 ordinarias por mes por
instalación. Conexión 5 s, petición 10 s; un reintento tras 60 s para
red/5xx. 429 respeta Retry-After, sin cabecera espera 24 h.
Fallas de contratoinvalid-key, inactive-account, quota-reached o NIO ausente: no reintentar
hasta corrección; avisar sin detener operaciones locales en la misma
moneda.
LicenciaUso final y caché interna; sin redistribuir catálogo o API. Datos
informativos; no usarlos como tasa financiera de pago (AYR Tech (Pty)
Ltd., s. f.-e).

## 26
AspectoDecisión Cerrada
## Alternativas
iniciales
FXRatesAPI y CurrencyFreaks no se adoptan porque no se completó su
verificación de todos los criterios. No se atribuyen defectos, cuotas o
cobertura sin consulta.
## Autorización
financiera
Propietario registra valor por fecha local, fuente y motivo. No copiar
automáticamente el valor de la API ni convertir su caché en autorización.
El Banco Central de Nicaragua (2025) anunció un deslizamiento anual de cero por ciento
y un tipo de cambio oficial de 36.6243 córdobas por dólar para 2026. Esa referencia institucional
se distingue de los valores comerciales y no se extiende a 2027. El escenario sintético de pago
divide C$ 3,662.43 entre 36.62430000 para obtener US$ 100.00 y conserva ambos importes
junto a la fuente autorizada. Los términos del proveedor comercial desaconsejan usar sus datos
para transacciones reales; el flujo de Latch mantiene esa consulta como información
independiente de la autorización financiera del Propietario.
La Tabla 19 presenta estados de Tasa y Señales de Interfaz.
## Tabla 19
Estados de Tasa y Señales de Interfaz
CondiciónRótulo y Comportamiento
VigenteReferencia vigente; icono i; fecha y próxima actualización. Describe frescura
informativa, no autorización de pago.
DesactualizadoReferencia desactualizada; signo de aviso y fecha. Conserva caché; evita
aparentar una consulta actual.
ManualTasa manual autorizada; fuente, motivo, actor y fecha efectiva. Solo aplica a
la fecha del pago indicada.
Sin conexiónSin conexión con proveedor; marca de conexión y último dato si existe. Es
estado de transporte y puede coexistir con caché vigente o desactualizada.
Sin tasa
disponible
Sin referencia comercial en caché. Permite pagos en moneda contractual;
MSG-065 solo se muestra al intentar una conversión sin tasa autorizada y
enlaza la solicitud al Propietario.

## 27
Catálogo de Pantallas y Diálogos
El catálogo identifica cuarenta elementos para vincular requisitos, vistas FXML y
pruebas de aceptación. La denominación Ventana representa una vista dentro del único marco
principal, salvo el acceso inicial; no crea cuarenta ventanas independientes del sistema operativo.
Los diálogos pertenecen a la ventana que los abre y regresan al punto de origen. La tabla
conserva el módulo y el rol permitido en cada elemento para que el controlador oculte acciones
sin autorización. Las veinte especificaciones principales desarrollan la composición y los
mensajes; el Apéndice A amplía los formularios de unidad e inquilino.

## 28
La Tabla 20 presenta catálogo de Pantallas y Diálogos.
## Tabla 20
Catálogo de Pantallas y Diálogos
CódigoNombreMóduloRolTipoPropósitoRequisitos del PRD
PAN-01Inicio de SesiónM01AmbosVentanaIngresarRF-M01-01 a 05, 12 a 15
PAN-02Panel PrincipalM11AmbosVentanaVer CobranzaRF-M11-01 a 10
PAN-03Lista de PropiedadesM02PropietarioVentanaNueva PropiedadRF-M02-01, 09, 10
PAN-04Formulario de
## Propiedad
M02PropietarioDiálogoGuardar PropiedadRF-M02-01, 09
PAN-05UnidadesM02Ambos
consultan;
Asistente edita
condiciones/fo
to; Propietario
administra
VentanaConsultar unidades;
nueva unidad solo
## Propietario
RF-M02-02 a 08, 11
PAN-06InquilinosM03AmbosVentanaNuevo InquilinoRF-M03-01 a 09
PAN-07Listado de ContratosM04AmbosVentanaNuevo ContratoRF-M04-10, 12; RF-M11-06
PAN-08Formulario de
## Contrato
M04AmbosDiálogoGuardar BorradorRF-M04-01 a 09, 11, 13
PAN-09Cargos y Estado de
## Cuenta
M05AmbosVentanaRegistrar PagoRF-M05-05 a 11; RF-M10-05
PAN-10Registro de PagoM06AmbosDiálogoConfirmar PagoRF-M06-01 a 08, 11, 12; RF-
## M09-09
PAN-11Vista Previa de ReciboM06AmbosVentanaExportar PDFRF-M06-08 a 10; RF-M10-01,
## 10

## 29
CódigoNombreMóduloRolTipoPropósitoRequisitos del PRD
PAN-12Depósitos de GarantíaM07Ambos
consulta;
## Propietario
liquida
VentanaLiquidar DepósitoRF-M07-01 a 07
PAN-13MantenimientoM08AmbosVentanaNueva SolicitudRF-M08-01 a 07; RF-M05-05
PAN-14Tipo de CambioM09Ambos
consulta;
## Propietario
autoriza
VentanaConsultar ReferenciaRF-M09-01 a 09
PAN-15Parámetros de
## Reportes
M10AmbosVentanaGenerar ReporteRF-M10-01 a 13
PAN-16Vista Previa de
## Reporte
M10AmbosVentanaExportar PDFRF-M10-09, 10, 12; RN-28
PAN-17ConfiguraciónM12PropietarioVentanaGuardar ParámetrosRF-M12-01 a 03, 06, 07
PAN-18UsuariosM01PropietarioVentanaNuevo UsuarioRF-M01-03 a 06, 09 a 11
PAN-19Bitácora de AuditoríaM12PropietarioVentanaConsultar BitácoraRF-M12-04, 05; RN-14
PAN-20Generación Masiva de
## Cargos
M05AmbosDiálogoGenerar CargosRF-M05-01 a 04, 12; RN-20
PAN-21Formulario de UnidadM02Propietario;
Asistente solo
en edición de
condiciones/fo
to
DiálogoCódigo, tipo, canon y fotoRF-M02-02 a 05
PAN-22Formulario de
## Inquilino
M03AmbosDiálogoDatos mínimos y adjuntosRF-M03-01 a 05, 09
PAN-23Confirmación de Baja
## Lógica
## M02/
## M03
PropietarioDiálogoRevisar dependencias y
conservar historia
## RF-M03-08

## 30
CódigoNombreMóduloRolTipoPropósitoRequisitos del PRD
PAN-24Historial del InquilinoM03AmbosDiálogoContratos y pagos
enlazados
## RF-M03-07
PAN-25Activación de ContratoM04AmbosDiálogoRevisar garantía y
ocupación
## RF-M04-05, 06
PAN-26Renovación de
## Contrato
M04AmbosDiálogoCrear sucesor sin
sobrescribir
## RF-M04-08
PAN-27Terminación o
## Cancelación
M04AmbosDiálogoFecha, motivo y
transición
## RF-M04-09, 11
PAN-28Cargo ManualM05AmbosDiálogoReparación, Servicio u
## Otro
## RF-M05-05
PAN-29Anulación de ReciboM06PropietarioDiálogoMotivo y control de
dependencias
## RF-M06-09
PAN-30Solicitud de
## Mantenimiento
M08AmbosDiálogoUnidad, prioridad,
responsable y costo
RF-M08-01 a 07
PAN-31Liquidación de
## Garantía
M07PropietarioDiálogoDeducciones y
devolución
RF-M07-03 a 07
PAN-32Tasa Manual
## Autorizada
M09PropietarioDiálogoValor, fuente, fecha y
motivo
## RF-M09-07, 09
PAN-33Alta y Edición de
## Usuario
M01PropietarioDiálogoCuenta y rolRF-M01-06, 09
PAN-34Cambio de ContraseñaM01AmbosDiálogoCambio obligatorio de
temporal
## RF-M01-03, 11
PAN-35Restablecimiento de
## Cuenta
M01PropietarioDiálogoContraseña temporal y
desbloqueo
## RF-M01-04, 11

## 31
CódigoNombreMóduloRolTipoPropósitoRequisitos del PRD
PAN-36Respaldo ManualM12PropietarioDiálogoDestino,
pwdClaveRespaldo y
progreso de paquete
## AES-256-GCM;
## MSG-082
## RF-M12-06
PAN-37Restauración en
## Mantenimiento
M12PropietarioDiálogoPaquete,
pwdClaveRespaldo,
autenticidad y copia
previa; MSG-082
## RF-M12-07
PAN-38Detalle de EventoM12PropietarioDiálogoOperación y correlación,
solo lectura
## RF-M12-05
PAN-39Cambios Sin GuardarM01/
## M12
AmbosDiálogoGuardar, descartar o
continuar editando
## RNF-009
PAN-40Cierre con Tareas
## Activas
M12AmbosDiálogoH6 coordina cancelación
y espera
## RF-M12-08

## 32
Especificación por Pantalla
Cada pantalla principal conserva el código del catálogo y una relación con sus requisitos
del PRD. El Apéndice A reúne su wireframe propio y su tabla de controles, validación, estados y
acciones. Las letras A, B y C distinguen identidad Aurora, lectura e interacción dentro de cada
dibujo. Las figuras son esquemas del proyecto con datos sintéticos y texto ampliado para su
lectura editorial. La aplicación conserva las medidas lógicas del sistema de diseño; las
ilustraciones utilizan una anchura de dieciséis centímetros y medio y no son capturas de JavaFX.
PAN-01 Inicio de Sesión
El acceso presenta el nombre de Latch junto a un formulario opaco y el selector de tema.
Cualquier persona que llegue a esta ventana puede elegir Claro u Oscuro antes de autenticar. La
aplicación conserva el usuario escrito y el campo de contraseña durante el cambio visual. Un
intento fallido devuelve un mensaje genérico y mantiene el foco en la contraseña para corregirla.
El quinto fallo activa el bloqueo de quince minutos definido en el PRD, sin revelar la existencia
de una cuenta ni registrar la clave ingresada.
El wireframe y el contrato detallado de PAN-01 corresponden a la Figura A1 y la Tabla A1 del
## Apéndice A.

## 33
PAN-02 Panel Principal
El panel reúne ocupación, cobrado, por cobrar y mora para un período seleccionado.
Cada total financiero identifica su moneda y la fecha del último corte. La tabla de contratos por
vencer tiene más espacio que el resumen de ocupación porque requiere comparar varias filas. Al
activar un indicador, la persona abre la lista correspondiente con el mismo filtro aplicado. El
valor de cambio muestra su procedencia y antigüedad como información separada de la tasa
autorizada para pagos, sin sugerir que una consulta comercial establece el monto exigible al
inquilino.
El wireframe y el contrato detallado de PAN-02 corresponden a la Figura A2 y la Tabla A2 del
## Apéndice A.
PAN-03 Lista de Propiedades
La lista de propiedades permite localizar el conjunto al que pertenecen las unidades
alquilables. El nombre y la dirección ocupan columnas amplias, mientras la cantidad de unidades
mantiene un ancho menor. La acción Nueva propiedad abre el formulario sin reemplazar la lista
ni perder la búsqueda. Una baja requiere revisar las unidades activas y confirmar el nombre del
registro afectado. El Asistente no dispone de esta operación de mantenimiento del catálogo, de
acuerdo con la matriz de permisos del PRD, aunque puede reconocer propiedad y unidad desde
los contratos autorizados.
El wireframe y el contrato detallado de PAN-03 corresponden a la Figura A3 y la Tabla A3 del
## Apéndice A.

## 34
PAN-04 Formulario de Propiedad
El formulario de propiedad solicita nombre y dirección antes de guardar el catálogo. Las
notas opcionales quedan después de los datos obligatorios para mantener un recorrido de lectura
estable. La edición conserva el identificador interno y presenta los valores vigentes al abrir el
diálogo. Cuando la persona cancela una modificación pendiente, una confirmación evita
descartar lo escrito por accidente. Guardar valida los campos, deshabilita el envío repetido y
devuelve el foco a la fila creada o editada, con un mensaje de confirmación que identifica la
propiedad sin exponer información ajena al registro.
El wireframe y el contrato detallado de PAN-04 corresponden a la Figura A4 y la Tabla A4 del
## Apéndice A.
PAN-05 Unidades
La vista de unidades relaciona cada código con una propiedad, un tipo y un estado del
catálogo compartido. El canon de referencia muestra monto y moneda sin modificar contratos
existentes. La fotografía ocupa una región opcional y su ausencia no bloquea el alta ni cambia el
orden del formulario. Una unidad con contrato activo muestra Ocupada y conserva la restricción
de baja. Los filtros permiten distinguir disponibilidad de mantenimiento; el estado En
mantenimiento solo puede asignarse por el Propietario a una unidad sin contrato activo. El
Asistente consulta unidades y edita condiciones o fotografía desde PAN-21, sin altas, bajas ni
cambios a En mantenimiento.
El wireframe y el contrato detallado de PAN-05 corresponden a la Figura A5 y la Tabla A5 del
## Apéndice A.

## 35
PAN-06 Inquilinos
El expediente del inquilino reúne los datos mínimos de contacto y su historial contractual.
La búsqueda acepta nombre o identificación cuando esta última exista en el registro. El detalle
presenta vínculos a contratos y recibos para evitar copiar saldos entre formularios. Los
documentos y el contacto de emergencia son opcionales; la interfaz explica su finalidad antes de
adjuntar archivos. Ambos roles pueden crear y corregir contactos, mientras la baja lógica queda
reservada al Propietario y exige ausencia de contrato activo, sin borrar movimientos que
respaldan un recibo o un estado de cuenta.
El wireframe y el contrato detallado de PAN-06 corresponden a la Figura A6 y la Tabla A6 del
## Apéndice A.
PAN-07 Listado de Contratos
El listado de contratos prioriza unidad, inquilino, fechas y saldo para facilitar la decisión
de cobro. Cada estado aparece como texto acompañado por un símbolo, de modo que el color no
sea el único indicador disponible. El filtro Por vencer conserva el umbral configurado y muestra
la fecha de corte usada para calcularlo. Abrir un contrato mantiene la página y los criterios de
búsqueda al regresar. La acción de pago exige una fila seleccionada y vuelve a consultar su saldo
antes de confirmar, porque otra sesión puede haber registrado un abono sobre el mismo contrato.
El wireframe y el contrato detallado de PAN-07 corresponden a la Figura A7 y la Tabla A7 del
## Apéndice A.

## 36
PAN-08 Formulario de Contrato
El contrato inicia como Borrador y separa la edición de condiciones de la activación
financiera. El formulario solicita unidad, inquilino, vigencia, canon, moneda, día de pago y
depósito pactado mayor o igual a cero. Con importe cero, la activación registra la garantía como
Retenido, sin movimientos ni recibo. La fecha final debe permitir al menos un mes calendario
desde el inicio, incluso cuando los meses tienen distinta cantidad de días. Activar muestra un
resumen de ocupación y garantía antes de confirmar la operación. Una renovación crea un
contrato sucesor enlazado y conserva las condiciones anteriores; terminar o cancelar requiere
fecha y motivo según el estado permitido en las reglas del producto.
El wireframe y el contrato detallado de PAN-08 corresponden a la Figura A8 y la Tabla A8 del
## Apéndice A.
PAN-09 Cargos y Estado de Cuenta
El estado de cuenta del contrato explica cada saldo mediante cargos y aplicaciones de
pagos. La vista separa principal, mora y saldo a favor con rótulos que incluyen la moneda
contractual. Un cargo vencido con abono conserva el estado Vencido, aunque la columna
Abonado permita reconocer el pago parcial. El botón Registrar pago toma el contrato visible y
abre una distribución revisable. Los cargos manuales exigen concepto y vencimiento; cualquier
repercusión de mantenimiento requiere una operación separada del gasto del propietario para
conservar el sentido financiero de ambos registros.
El wireframe y el contrato detallado de PAN-09 corresponden a la Figura A9 y la Tabla A9 del
## Apéndice A.

## 37
PAN-10 Registro de Pago
El registro de pago muestra el importe recibido y su aplicación antes de emitir el recibo.
La distribución inicial cubre el cargo vencido más antiguo y admite selección manual dentro del
mismo contrato. Cuando la moneda del pago difiere de la contractual, la vista exige una tasa
autorizada para esa fecha y presenta el cálculo equivalente. El excedente aparece como saldo a
favor con su moneda, sin perderlo en el redondeo. La confirmación deshabilita un segundo envío
y conserva la clave de idempotencia si el resultado queda incierto por una interrupción de
conexión.
El wireframe y el contrato detallado de PAN-10 corresponden a la Figura A10 y la Tabla A10
del Apéndice A.
PAN-11 Vista Previa de Recibo
La vista previa de recibo presenta una página blanca dentro de un marco que sigue el
tema seleccionado. El número identifica el pago confirmado y permanece igual en cada
reimpresión. El encabezado conserva los datos del propietario guardados al emitir el
comprobante, de modo que una edición posterior no cambie su historia. La anulación solicita
motivo y una confirmación específica del Propietario antes de revertir las aplicaciones
autorizadas. Si la exportación falla después del pago, la vista mantiene el recibo disponible para
reintentar la generación sin registrar nuevamente el dinero recibido.
El wireframe y el contrato detallado de PAN-11 corresponden a la Figura A11 y la Tabla A11
del Apéndice A.

## 38
PAN-12 Depósitos de Garantía
La garantía se consulta como un fondo separado de los ingresos por renta del contrato. La
vista muestra el importe inicial, las aplicaciones a deuda, las devoluciones y el remanente con la
misma moneda. El Asistente puede leer los movimientos, mientras el Propietario dispone de la
liquidación al terminar el contrato. Cada deducción debe identificar su concepto y el cargo al que
corresponde cuando paga deuda. La confirmación verifica que la suma aplicada y devuelta no
exceda el remanente, y deja un documento que permite reconstruir el estado Retenido, Aplicado,
Devuelto parcial o Devuelto.
El wireframe y el contrato detallado de PAN-12 corresponden a la Figura A12 y la Tabla A12
del Apéndice A.
PAN-13 Mantenimiento
Mantenimiento organiza las solicitudes por unidad, prioridad y estado de atención. El
formulario conserva descripción, responsable propuesto y costo del trabajo para el resumen de
gastos por propiedad. Una solicitud Abierta puede pasar a En proceso o Cancelada, y una
solicitud En proceso puede terminar como Resuelta o Cancelada. Los estados terminales
preservan el historial confirmado. Registrar un costo añade gasto del propietario; cobrar una
reparación al inquilino exige crear un cargo con concepto en cobranza, sin copiar el gasto de
forma automática ni alterar el depósito de garantía sin liquidación autorizada.
El wireframe y el contrato detallado de PAN-13 corresponden a la Figura A13 y la Tabla A13
del Apéndice A.

## 39
PAN-14 Tipo de Cambio
La pantalla de tipo de cambio distingue una referencia comercial informativa de la tasa
autorizada para registrar pagos. Cada región identifica origen, fecha efectiva y momento de
descarga o registro. La referencia API puede figurar vigente o desactualizada y conservarse en
caché cuando no existe conexión. El Propietario registra una tasa manual con fuente y motivo
para una fecha concreta del pago, sin copiar de manera automática la referencia comercial. El
historial mantiene los valores utilizados en operaciones anteriores y no permite sustituirlos
cuando una consulta posterior devuelve una cotización diferente.
El wireframe y el contrato detallado de PAN-14 corresponden a la Figura A14 y la Tabla A14
del Apéndice A.
PAN-15 Parámetros de Reportes
El catálogo de reportes presenta diez salidas del producto y adapta los parámetros al
documento seleccionado. El estado de cuenta exige contrato y fecha de corte, mientras ingresos y
gastos utiliza un período y una moneda. La persona revisa los filtros antes de iniciar la
generación para reconocer qué datos explicarán los totales. H5 realiza la consulta y el armado en
segundo plano con progreso visible y una acción de cancelación. La vista conserva los
parámetros si la tarea falla, de modo que corregir una fecha o reintentar no requiera reconstruir la
selección anterior.
El wireframe y el contrato detallado de PAN-15 corresponden a la Figura A15 y la Tabla A15
del Apéndice A.

## 40
PAN-16 Vista Previa de Reporte
La vista previa mantiene las páginas del reporte en blanco y conserva los filtros en un
encabezado de contexto. El marco cambia entre Claro y Oscuro sin modificar la imagen del
documento ni sus importes. Los controles de página anuncian la posición actual y no dependen
de botones con iconos sin nombre. Exportar solicita destino, revisa la existencia del archivo y
ejecuta la escritura en segundo plano. Una cancelación conserva cualquier archivo anterior y
elimina el temporal; la confirmación final muestra la ruta del archivo publicado y permite volver
a los parámetros usados en la consulta.
El wireframe y el contrato detallado de PAN-16 corresponden a la Figura A16 y la Tabla A16
del Apéndice A.
PAN-17 Configuración
Configuración reúne parámetros operativos, datos del propietario y acciones de respaldo
autorizadas. Una nueva versión registra fecha y motivo para conservar el contexto de los cálculos
confirmados. Los cambios de datos para recibos afectan emisiones posteriores y preservan las
copias históricas. El tema permanece en la barra superior para ambos roles como preferencia
personal, con el mismo acceso descrito para las otras vistas. Crear un respaldo exige destino y
detención coordinada de escrituras; restaurar requiere mantenimiento, revisión del paquete y
confirmación antes de reemplazar la base.
El wireframe y el contrato detallado de PAN-17 corresponden a la Figura A17 y la Tabla A17
del Apéndice A.

## 41
PAN-36 Respaldo Manual
El diálogo solicita directorio de destino y PasswordField pwdClaveRespaldo con rótulo
Contraseña de respaldo. La clave permanece oculta, no se registra en logs ni se guarda en
Preferences. Confirmar crea el paquete cifrado AES-256-GCM según el TRD (Cuadra et al.,
2026d), con manifiesto y hashes. El progreso distingue preparación, cifrado y publicación;
Cancelar respeta la frontera segura. Una falla de integridad muestra MSG-079; una falla de
autenticidad o clave del paquete muestra MSG-082.
PAN-37 Restauración en Mantenimiento
El diálogo solicita archivo de respaldo y PasswordField pwdClaveRespaldo con rótulo
Contraseña de respaldo, además de la confirmación del Propietario. Antes de restaurar exige
mantenimiento, copia previa y validación de autenticidad AES-256-GCM; solo publica archivos
después de verificar autenticidad, descifrar y comprobar hashes. Una clave o autenticidad
inválida produce ERR-059 / MSG-082: No se pudo verificar la autenticidad del respaldo. Revise
la contraseña o seleccione otro paquete. Conserva la instalación y la copia previa; no publica
archivos restaurados. La conciliación de recibos posteriores utiliza MSG-081.
PAN-18 Usuarios
Usuarios permite al Propietario crear cuentas y mantener el acceso de los dos roles
definidos. La tabla muestra nombre de cuenta, rol y estado sin exponer contraseñas, hashes ni
secretos del servicio web. Cambiar el rol requiere una nueva sesión de la cuenta afectada para
aplicar los permisos vigentes. La desactivación conserva la historia y protege la existencia de al
menos un Propietario activo. El restablecimiento entrega una contraseña temporal por el
procedimiento local de demostración y exige su cambio al siguiente acceso, sin añadir envíos
automáticos por correo o mensajería fuera del alcance académico.

## 42
El wireframe y el contrato detallado de PAN-18 corresponden a la Figura A18 y la Tabla A18
del Apéndice A.
PAN-19 Bitácora de Auditoría
La bitácora explica quién ejecutó una operación y qué registro quedó afectado en un
momento determinado. Los filtros por período, usuario, operación y correlación permiten
localizar el evento asociado con un recibo. La vista convierte el instante persistido en UTC a la
zona America/Managua para su lectura, sin modificar el valor almacenado. El detalle muestra las
referencias necesarias para reconstruir el movimiento y excluye contraseñas o contenido de
adjuntos. El Propietario consulta esta información en modo de lectura; la interfaz no proporciona
edición ni baja de eventos financieros confirmados.
El wireframe y el contrato detallado de PAN-19 corresponden a la Figura A19 y la Tabla A19
del Apéndice A.
PAN-20 Generación Masiva de Cargos
La generación masiva comienza con una previsualización de contratos elegibles para el
período mensual seleccionado. La tabla identifica el canon que se generará y las rentas que el
sistema omitirá porque ya existen. Al confirmar, H1 procesa cada contrato con su propia
transacción y actualiza una barra de progreso acompañada por cantidades. Cancelar solicita la
detención entre contratos, conserva los cargos confirmados y evita movimientos parciales. El
resumen diferencia generados, omitidos, fallidos y pendientes; un nuevo intento vuelve a revisar
el período para continuar sin duplicar cargos ni congelar la ventana principal.
El wireframe y el contrato detallado de PAN-20 corresponden a la Figura A20 y la Tabla A20
del Apéndice A.

## 43
Composiciones Aurora de Alta Fidelidad
Las ocho composiciones a color representan acceso, panel, contratos y pago con los
mismos datos en Claro y Oscuro. La lectura financiera se mantiene sobre paneles opacos y los
campos de luz quedan en el marco o la zona de identidad. Las etiquetas de la figura se amplían
hasta un mínimo editorial cercano a ocho puntos al insertarlas en el documento; los tamaños de
la aplicación siguen la escala lógica especificada. Estas imágenes son propuestas propias y no
evidencias de ejecución, rendimiento o soporte de accesibilidad de una aplicación JavaFX
implementada.
La Figura 4 muestra composición de Acceso en Tema Claro.
## Figura 4
Composición de Acceso en Tema Claro

## 44
La Figura 5 muestra composición de Acceso en Tema Oscuro.
## Figura 5
Composición de Acceso en Tema Oscuro

## 45
La Figura 6 muestra composición de Panel Principal en Tema Claro.
## Figura 6
Composición de Panel Principal en Tema Claro

## 46
La Figura 7 muestra composición de Panel Principal en Tema Oscuro.
## Figura 7
Composición de Panel Principal en Tema Oscuro

## 47
La Figura 8 muestra composición de Listado de Contratos en Tema Claro.
## Figura 8
Composición de Listado de Contratos en Tema Claro

## 48
La Figura 9 muestra composición de Listado de Contratos en Tema Oscuro.
## Figura 9
Composición de Listado de Contratos en Tema Oscuro

## 49
La Figura 10 muestra composición de Registro de Pago en Tema Claro.
## Figura 10
Composición de Registro de Pago en Tema Claro

## 50
La Figura 11 muestra composición de Registro de Pago en Tema Oscuro.
## Figura 11
Composición de Registro de Pago en Tema Oscuro

## 51
La Tabla 21 presenta conformidad Visual Aurora de las Composiciones.
## Tabla 21
Conformidad Visual Aurora de las Composiciones
Vista y TemasPaleta, Luz y SuperficieFoco, Ratón, Escala y Sin Efectos
## Acceso Claro /
## Oscuro
Tres luces periféricas; formulario y
zona de texto opacos; paletas del
tema.
Foco dibujado en Usuario; fondo sin
interacción. Escala y selector en
ejecución: plan de evaluación; sin
efectos conserva panel y fondo sólido.
## Panel Claro /
## Oscuro
Marco Aurora; indicadores y tabla
sobre surface. Totales USD sin
mezcla de NIO.
Selección legible y rótulos; nodos
decorativos mouseTransparent en
contrato. Ajuste 100/125/150/200 %
requiere ejecutar; sin efectos conserva
distribución.
## Contratos Claro /
## Oscuro
Luces fuera de filtros y tabla;
selección por banda y texto; estados
rotulados.
Buscar y acciones alcanzables por
teclado según contrato. Cadenas
largas y foco real se probarán; sin
efectos mantiene columnas y
permisos.
## Pago Claro /
## Oscuro
Entradas sólidas; tasa autorizada
separada de referencia API; mismo
cálculo.
Anillo dibujado en monto; Confirmar
rotulado. Prueba de Tab, escalado y
cancelación prevista; sin efectos
conserva valores y UUID.
Nota. Comprobación documental: paletas, composición y existencia de ocho imágenes. El
comportamiento del ratón, foco real, escalado y cambio de tema forma parte del plan de
ejecución; no se informa como prueba realizada.

## 52
Mensajes y Contenido
La interfaz utiliza español de Nicaragua con instrucciones breves y verbos que describen
la acción disponible. Los errores indican el dato que debe corregirse y conservan el resto del
formulario para evitar un nuevo ingreso. Un mensaje financiero diferencia confirmación,
resultado incierto y falla de exportación posterior al pago. Los códigos MSG identifican
especificaciones y diagnósticos del equipo, mientras la persona ve el texto exacto del catálogo.
El Apéndice B contiene ochenta y nueve mensajes (MSG-001 a MSG-089) y sus condiciones de
aparición; la Tabla 22 resume las convenciones que mantienen consistencia entre pantallas,
recibos y reportes.
La Tabla 22 presenta formatos y Rótulos de la Interfaz.
## Tabla 22
Formatos y Rótulos de la Interfaz
ElementoConvención Adoptada
Fechadd/MM/aaaa; ejemplo sintético 23/10/2026. DatePicker valida editor y
explica errores.
ImportesPunto decimal y coma de miles: C$ 3,662.43; US$ 100.00. Dos decimales
con HALF_EVEN al convertir total.
Tasa36.62430000 NIO/USD corresponde a la referencia del BCN para 2026;
36.50000000 es ficticia y solo se usa en pruebas declaradas. Ocho decimales
almacenables; fecha y fuente junto al valor.
LocalizaciónDecisión explícita del proyecto para pantalla y exportación; no afirma una
convención universal de Nicaragua. Parsear sin ambigüedad ni depender de
locale de Windows.
## Entrada
monetaria
Aceptar dígitos, una marca decimal punto y agrupación con coma válida;
rechazar 1,25 como decimal. Ayuda: Use punto decimal, por ejemplo
## 1,250.00.
BotonesGuardar propiedad; Guardar Borrador; Activar contrato; Confirmar pago;
Anular recibo; Dar de baja; Exportar PDF; Exportar XLSX; Cancelar.
ConfirmacionesConservar; Cancelar; Continuar editando; Descartar; Reemplazar. Evitar
Sí/No sin acción descrita.
La Tabla 23 presenta glosario de la Interfaz.

## 53
## Tabla 23
Glosario de la Interfaz
TérminoDefinición Operativa
UnidadEspacio alquilable de tipo Casa, Apartamento, Cuarto o Local
comercial.
CanonMonto pactado de renta del contrato, en NIO o USD.
CargoObligación de pago con concepto y fecha de vencimiento.
MoraRecargo de pago tardío después de la gracia; no se aplica sobre mora.
Depósito de garantíaFondo separado de renta que se aplica o devuelve con conceptos al
cierre.
ReciboComprobante de un pago confirmado, con consecutivo único que se
conserva al anular.
Estado de cuentaMovimientos y saldos del contrato a una fecha de corte.
Tasa de cambioCantidad de NIO por USD, con origen, fecha efectiva y valor
congelado por operación.
Período de facturaciónMes al que corresponde el cargo único de renta por contrato.
Saldo a favorExcedente del pago en moneda contractual, aplicable al siguiente cargo.
La Tabla 24 presenta catálogos Textuales Compartidos.
## Tabla 24
## Catálogos Textuales Compartidos
CatálogoValores Exactos
Monedas / rolesNIO (C$), USD (US$); Propietario, Asistente.
ContratoBorrador, Vigente, Por vencer, Vencido, Terminado, Cancelado.
UnidadDisponible, Ocupada, En mantenimiento, Inactiva.
CargoPendiente, Parcial, Pagado, Vencido, Anulado.
PagoRegistrado, Anulado.
MantenimientoAbierta, En proceso, Resuelta, Cancelada.
DepósitoRetenido, Aplicado, Devuelto parcial, Devuelto.
Tipo de cargoRenta, Mora, Reparación, Servicio, Otro.
Método de pagoEfectivo, Transferencia, Depósito bancario, Otro.

## 54
Accesibilidad y Usabilidad
Latch adopta criterios de WCAG como metas de diseño transferidas a controles de
escritorio, sin declarar una certificación web de la aplicación. El foco permanece visible mientras
el control lo conserva, siguiendo la explicación de W3C (s. f.-c). Los botones tienen al menos
treinta y seis unidades de altura y un área operable suficiente para su rótulo. La referencia de
veinticuatro píxeles CSS del criterio de tamaño de objetivo informa esta decisión, pero no
demuestra equivalencia automática con unidades JavaFX (W3C, s. f.-d). Los textos de error y
estado acompañan cualquier diferencia de color.
OpenJFX (s. f.-c) proporciona accessibleText, accessibleHelp y accessibleRole para
describir los nodos de la interfaz. Platform informa si una tecnología de asistencia activó el
soporte, sin probar la calidad del recorrido con un lector específico (OpenJFX, s. f.-d). El equipo
enlazará Label con su entrada y priorizará controles nativos. Las figuras no comprueban
anuncios, orden de lectura ni pronunciación de importes. El plan propone probar Narrador en
Windows 11 y registrar versión, pasos y evidencia antes de afirmar compatibilidad con ese
entorno.
La Tabla 25 presenta criterios de Aprobación de Accesibilidad.
## Tabla 25
Criterios de Aprobación de Accesibilidad
CondiciónCriterio Medible y Procedimiento
Texto y señalesTodos los pares pertinentes ≥ 4.5:1 para texto normal; ≥ 3:1 para grande
según W3C y señales necesarias. Comparación sin redondear; revisar cada
estado con sus fondos reales.
FocoAnillo de 2 unidades con separación 2; contorno ≥ 3:1 frente a superficie y
sin depender de glow. Recorrer diez tareas y verificar que no quede oculto
bajo pie fijo.
TecladoCero bloqueos en Tab/Shift+Tab, Enter, Escape, flechas y F6. Guardar y
confirmar disponibles sin ratón. Abrir contexto mediante Menú/Shift+F10.

## 55
CondiciónCriterio Medible y Procedimiento
MensajesVeinte entradas inválidas identifican campo y causa; resumen lleva al
error; el mensaje persiste hasta corrección. Estado comunicado con texto y
símbolo.
Escalado100 %, 125 %, 150 % y 200 %; cadenas de 120 caracteres, concepto de
500 e importe C$ 9,999,999,999,999.99. Cero recortes de controles o
importes; permitir wrap/scroll.
Matriz visualClaro normal, Claro sin efectos, Oscuro normal y Oscuro sin efectos para
acceso, panel, tabla, formulario, calendario, ComboBox, menú, tooltip,
diálogo, carga, error y selección.
## Accesibilidad
asistida
Probar Narrador y lectura de monto, tasa y error; registrar entorno y
anuncios. Cero promesas de compatibilidad basadas solo en la existencia
de propiedades.
Visor y
exportación
La página blanca conserva contraste; ImageView requiere descripción y
resumen textual. No atribuir PDF/UA a JasperReports Community sin
evidencia.
La inspección distingue las verificaciones posibles sobre el documento de aquellas que
requieren ejecutar Latch. El cálculo comprueba colores sólidos y la revisión visual comprueba
que las composiciones mantengan lectura opaca y rótulos identificables. El teclado real, la
respuesta de menús y los anuncios del lector requieren un prototipo con las versiones
seleccionadas. Los ocho ejemplos a color mantienen los mismos datos para revisar diferencias de
tema sin cambiar la tarea. La Lista de Verificación del Apéndice D registra esta separación y
evita presentar una intención de accesibilidad como un resultado de aceptación ejecutado.

## 56
Diseño de Documentos Impresos y Reportes
Los comprobantes administrativos utilizan papel carta con fondo blanco y márgenes de
quince milímetros para el diseño del reporte. El recibo y el estado de cuenta conservan DejaVu
Sans de diez puntos como cuerpo, doce para encabezados y catorce para el título. La franja
índigo ocupa solo una línea de identidad y los importes utilizan texto oscuro de alto contraste.
Estas medidas pertenecen a los reportes de Latch y no sustituyen Times New Roman de doce
puntos ni los márgenes APA del brief. Las Figuras 12 y 13 muestran maquetaciones propias con
datos sintéticos para los dos documentos.
La Figura 12 muestra maquetación del Recibo Administrativo.

## 57
## Figura 12
Maquetación del Recibo Administrativo
Nota. Datos sintéticos. La anotación inferior explica la variante anulada; un recibo anulado
llevará marca ANULADO, motivo, actor e instante, manteniendo legibles los importes.
La Figura 13 muestra maquetación del Estado de Cuenta.

## 58
## Figura 13
Maquetación del Estado de Cuenta

## 59
El encabezado incluye nombre del propietario, dirección o contacto configurado y
logotipo opcional, guardados como copia histórica al emitir el recibo. El detalle muestra número,
contrato, unidad, inquilino, fecha, método, monedas y aplicaciones. El pie identifica emisor,
condición administrativa y página actual sobre total, con fecha de corte en reportes. La marca
ANULADO utiliza texto grande gris y una leyenda oscura con motivo y fecha, sin cubrir cifras.
Las deducciones de garantía llevan conceptos propios y los totales de reportes nunca mezclan
NIO con USD dentro de una misma suma.
La Tabla 26 presenta catálogo de Salidas de Reportes.
## Tabla 26
Catálogo de Salidas de Reportes
SalidaParámetros y Campos de Control
ReciboPago confirmado; número, monedas, tasa, aplicaciones y saldo a favor.
Estado de cuentaContrato y corte; cargos, abonos, mora y saldo.
Cartera vencidaCorte, propiedad y moneda; antigüedad y saldo por contrato.
Ingresos y gastosPeríodo, propiedad y moneda; renta separada de garantía y gasto de
mantenimiento.
OcupaciónPropiedad y tipo; instante de generación, sin selector de fecha
histórica; disponibles, ocupadas, mantenimiento e inactivas.
Contratos por vencerCorte y umbral; fin, unidad e inquilino.
Historial de pagosInquilino, contrato y período; recibos registrados y anulados.
GarantíasContrato y corte; inicial, conceptos, aplicado, devuelto y remanente.
MantenimientoPropiedad y período; prioridad, estado, responsable y costo.
Tasas utilizadasPeríodo y origen; fecha efectiva, valor y operación vinculada.

## 60
Plan de Evaluación de Usabilidad
La evaluación propuesta combina revisión heurística y recorrido cognitivo del equipo con
los roles sintéticos. Las tareas emplean datos de demostración y resultados esperados, porque no
se ejecutaron sesiones de evaluación de Latch. Cada revisor registrará pantalla, tiempos, errores,
ayuda y una captura identificable. El éxito exige completar la operación y explicar el saldo o
estado resultante sin ayuda del autor de la vista. Esta revisión académica permite encontrar
defectos internos, sin acreditar aceptación de propietarios ni satisfacción de personas externas al
proyecto.

## 61
La Tabla 27 presenta escenarios de Evaluación y Resultados Esperados.
## Tabla 27
Escenarios de Evaluación y Resultados Esperados
CasoFecha
## Propuesta
## Responsable /
## Revisor
Escenario y ProcedimientoCriterio de AprobaciónCP equivalente
EV-0119/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Abrir CT-001 y leer US$ 200.00
vencidos al 23/10/2026. Identificar
vencimiento y acción de cobro.
≤ 30 s; 0 lecturas
incorrectas; éxito 4/4
revisores.
CP-093 (variante de
lectura de cartera)
EV-0219/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Localizar Nuevo contrato desde lista
y Guardar Borrador desde
formulario; sin pulsar acciones
financieras.
≤ 10 s para acción
principal; 0 activaciones
accidentales.
CP-093 (variante de
navegación);
## CP-015, CP-017
EV-0320/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Pagar C$ 3,662.43 sobre US$ 200.00
a la referencia BCN 36.62430000,
autorizada para la fecha del pago;
revisar aplicación y confirmar una
vez.
≤ 120 s; saldo US$ 100.00;
1 pago y 1 recibo.
CP-029 (variante
con tasa BCN
## 36.62430000);
## CP-026
EV-0420/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Ingresar 0.00 como monto de pago y
una fecha final menor a un mes en
contrato; corregir errores sin perder
campos.
≤ 45 s por error; 0 pérdidas
de entradas válidas.
## CP-015; CP-026
(variante de monto
inválido)
EV-0521/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Completar acceso, búsqueda y pago
parcial con teclado; abrir calendario
y contexto; cerrar diálogo.
≤ 180 s; 0 bloqueos de
foco; ninguna confirmación
por Escape.
## CP-079, CP-080,
## CP-092, CP-093
EV-0621/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Cambiar Claro/Oscuro y Sin efectos
con 20 campos/filtros, selección y
una tarea activa.
≤ 500 ms cambio visual;
20/20 valores intactos;
misma tarea y foco.
## CP-084, CP-087,
## CP-088

## 62
CasoFecha
## Propuesta
## Responsable /
## Revisor
Escenario y ProcedimientoCriterio de AprobaciónCP equivalente
EV-0722/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Simular HTTP sin red, JSON sin
NIO y ausencia de tasa autorizada;
intentar pago entre monedas.
3/3 condiciones rotuladas;
0 conversiones con API
informativa.
## CP-054, CP-058,
## CP-066
EV-0822/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Generar 50 cargos y cancelar al 30;
repetir lote con mismo período
sintético.
Detención entre contratos;
0 duplicados; resumen
consistente; UI ≤ 200 ms.
## CP-021; CP-047
## (variante: 50
contratos, cancelar
al 30)
EV-0923/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Revisar ambas paletas en 1366×768
y 1920×1080, a 100/125/150/200 %,
nombre de 120 caracteres, error
multilínea y cifra extensa.
0 recortes; contraste
aprobado; 4 variantes
tema/efectos.
## CP-078, CP-082
EV-1023/11/2026Responsabilidad
y revisión según
## Plan, Tablas 4–5.
Asistente intenta anular; Propietario
anula recibo permitido con motivo.
Exportar y revisar página blanca.
Asistente denegado;
número conservado; 0
pagos nuevos al
reimprimir.
## CP-005, CP-032,
## CP-086

## 63
Las diez tareas se repetirán con cada tema y la modalidad sin efectos cuando el caso
involucre presentación. Los EV son escenarios de diseño; su ejecución se registra bajo los CP
equivalentes del Plan, sin crear nuevos CP. Los responsables y la revisión cruzada se rigen por el
Plan, Tablas 4–5 y sección Revisión Cruzada y Decisión Técnica (Cuadra et al., 2026f). La
revisión heurística utilizará severidad cero para ausencia de problema, uno para detalle menor,
dos para fricción, tres para bloqueo y cuatro para pérdida o riesgo de integridad. Los problemas
de severidad tres o cuatro impiden cerrar el recorrido como aprobado. El registro final
conservará versión, entorno, datos, pasos y evidencia antes de añadir resultados obtenidos o
afirmar cumplimiento del producto.
La Tabla 28 presenta dependencias y Capacidad del Proyecto Propuesto.
## Tabla 28
Dependencias y Capacidad del Proyecto Propuesto
PeríodoTrabajo y trazabilidad al PlanHoras asignadas /
capacidad
Sprint 0 22–27/10Fundamentos; sistema visual, JDBC y concurrencia.
TAR-001 a TAR-016; responsables según Plan,
## Tablas 4–5.
## 32 / 41.142857
## Sprint 1 28/10–
## 03/11
Dominio, ORM, catálogos y contratos. TAR-017 a
TAR-032; responsables según Plan, Tablas 4–5.
## 40 / 48.000000
Sprint 2 04–10/11Circuito financiero, recibos y garantía. TAR-033 a
TAR-048; responsables según Plan, Tablas 4–5.
## 40 / 48.000000
Sprint 3 11–17/11HTTP preparado, reportes y respaldo. TAR-049 a
TAR-064; responsables según Plan, Tablas 4–5.
## 40 / 48.000000
Sprint 4 18–25/11HTTP definitivo, evaluación y regresión. TAR-065 a
TAR-080; responsables según Plan, Tablas 4–5.
## 44 / 54.857143
## Cierre 26/11–
## 01/12
Entrega, ensayo, correcciones y defensa. TAR-081 a
TAR-096; responsables según Plan, Tablas 4–5.
## 32 / 41.142857
TotalPlan, Apéndice A, TAR-001 a TAR-096.228 / 281.142857
Nota. Capacidad = 48 × 41/7 = 281.142857 h; carga = 228 h; reserva = 53.142857 h (18.902439
% de la capacidad). Para lectura: 281,14 h y 53,14 h. Las sumas usan precisión completa. Sprint

## 64
0 y Cierre tienen seis días; Sprint 4, ocho. Fuente única: Plan, Apéndice A, TAR-001 a TAR-096
(Cuadra et al., 2026f). Son estimaciones, no horas ejecutadas.
La Tabla 29 presenta distribución Nominal de Horas de Trabajo.
## Tabla 29
Distribución Nominal de Horas de Trabajo
PeríodoStevenAlfredoCalebAris
## Sprint 0 22–27/108888
## Sprint 1 28/10–03/1110101010
## Sprint 2 04–10/1110101010
## Sprint 3 11–17/1110101010
## Sprint 4 18–25/1111111111
## Cierre 26/11–01/128888
## Total57575757
Nota. Carga por persona: 8, 10, 10, 10, 11 y 8 h; total 57 h. Capacidad individual: 70.285714 h;
reserva: 13.285714 h. Cada fila corresponde al rango TAR de la Tabla 28, sumado por
responsable desde el Plan, Apéndice A. El prorrateo usa 12/7 h por día calendario y no acredita
disponibilidad confirmada.
Entregables para Desarrollo
El paquete de presentación propuesto organiza las vistas en recursos FXML y
controladores con responsabilidad de interfaz. El controlador enlaza campos y eventos con casos
de uso, mientras los servicios conservan permisos y fronteras transaccionales. Los nombres de
archivo de la tabla fijan un inventario para Scene Builder y evitan que una vista se genere con
otra convención durante la implementación. El código del Apéndice C es una referencia técnica
incluida en el brief, sin construcción ni ejecución de JavaFX. Las figuras propias describen el
acabado esperado y no reemplazan controles funcionales del framework.
La Tabla 30 presenta grupos del Inventario de Desarrollo.

## 65
## Tabla 30
Grupos del Inventario de Desarrollo
PantallasRecursos y Ubicación de la Especificación
PAN-01 a PAN-10Acceso, shell y formularios de catálogo, contrato y pago; rutas y
controladores individuales en la Tabla C1 del Apéndice C.
PAN-11 a PAN-20Visores, depósitos, mantenimiento, tasas, reportes, configuración,
usuarios, bitácora y lote; Tabla C1.
PAN-21 a PAN-30Formularios complementarios, transiciones de contrato, cargo y
anulación; Tabla C1.
PAN-31 a PAN-40Liquidación, tasa manual, cuentas, respaldo, auditoría y cierre; Tabla C1.
La Tabla 31 presenta recursos Compartidos y Convenciones.
## Tabla 31
Recursos Compartidos y Convenciones
RecursoContrato de Uso
/ui/css/components.cssEstados de botones, formularios, tablas, menús, popups y diálogos.
Contenido íntegro en Apéndice C.
/ui/css/claro.css y
## /ui/css/oscuro.css;
## /ui/css/print.css
Paletas con los mismos looked-up colors; print.css define
superficie blanca para el visor, sin sustituir la tipografía DejaVu
Sans de las plantillas JasperReports.
AuroraBackground.javani.edu.uam.latch.presentacion.tema.AuroraBackground; Pane
reutilizable de tres RadialGradient; update aplica tema y opción
simplificada.
ThemeService.javani.edu.uam.latch.presentacion.tema.ThemeService; registro débil
de escenas y ventanas; CSS desde /ui/css/; Preferences
ni/edu/uam/latch/presentation.
UiMetrics.javani.edu.uam.latch.presentacion.tema.UiMetrics; constantes double:
## SPACE_4/8/12/16/24/32, CONTROL_MIN 36, ROW_BASE 40,
## SIDEBAR 224, HEADER_MIN 64, CONTROL_RADIUS 8,
## PANEL_RADIUS 12.
fx:idlowerCamelCase; prefijos lbl, txt, pwd, cbo, tbl, dp, btn, chk, rb,
prg, img, tab, lst, pag y grp para ToggleGroup. Único dentro del
FXML; no usar nombre o identificación real.

## 66
RecursoContrato de Uso
Recursos gráficosRecursos: /ui/fxml/ plano, /ui/css/, /ui/imagenes/,
/ui/i18n/mensajes_es_NI.properties y /reportes/. Iconos SVGPath
propios; luces generadas por nodos, sin imagen aurora-
simplificada.png; 22 wireframes, 3 diagramas, 8 composiciones a
color y 2 maquetaciones impresas incluidos como figuras.
Scene BuilderConfigurar fx:controller, fx:id y clases de estilo; previsualizar
ambas paletas cambiando CSS. La lógica de transacción no reside
en FXML.
El servicio de tema registra la escena principal y las escenas de ventanas secundarias
mediante el inventario de Window. Cuando aparece un popup, aplica las mismas hojas al nuevo
árbol y mantiene el selector sincronizado con la preferencia en memoria. La restauración se
invoca al iniciar y después de autenticar; el controlador bloquea temporalmente sus escuchas del
selector para evitar escrituras causadas por esa carga. Cada RadioButton expone nombre y estado
seleccionado nativos. El servicio cambia colores sin recrear la vista, mientras el fondo recibe una
actualización independiente y conserva las operaciones financieras fuera de su responsabilidad.
El acceso a datos conserva la distribución JDBC e Hibernate de los documentos recibidos
y utiliza una única transacción por operación financiera. El caso de uso de pago confirma
aplicaciones, crédito, recibo, contador y bitácora sobre la misma conexión controlada, sin
escrituras JDBC independientes después del commit. H1 y H2 se coordinan por contrato, H3
consulta referencias y H5 produce reportes mediante DTO. H6 solicita detención y espera las
fronteras seguras, mientras H4 completa la sección crítica en curso. La presentación solo recibe
resultados y progreso, de modo que cambiar el tema no reinicie transacciones.
La Tabla 32 presenta matriz RACI Propuesta del Proyecto.

## 67
## Tabla 32
Matriz RACI Propuesta del Proyecto
EntregableSteven CuadraAlfredo
## Montalvan
## Caleb
## Tardencilla
## Aris Gavarrete
Alcance y calendarioA/RCCC
Seguridad y rolesA/RCCR
Esquema y migracionesCA/RIC
Persistencia y
transacciones
## RA/RCC
FXML y temasCIA/RR
Concurrencia y cierreA/RRCC
HTTP y cachéRCCA/R
Reportes y oráculosCRCA/R
Pruebas y conformidad
visual
## RRA/RR
Manual y defensaA/RRRR
App-image ZIP y
respaldo de entrega
## CRCA/R
Nota. La Tabla 32 reproduce la matriz canónica del Plan, Tabla 5; la asignación nominal, el
relevo y el mapa de revisión cruzada se consultan en el Plan, Tabla 4 y sección Revisión Cruzada
y Decisión Técnica (Cuadra et al., 2026f). A coordina aceptación interna; R ejecuta; C consulta; I
recibe información. El autor no aprueba su trabajo como única revisión.

## 68
Decisiones de Diseño Adoptadas
Las decisiones de la tabla cierran alternativas del alcance académico y mantienen los
requisitos del PRD compatibles con el contrato visual del proyecto. Las fuentes externas
respaldan propiedades técnicas y criterios de revisión; los tokens y la composición son
decisiones de Latch. La sustitución histórica de los valores iniciales de presentación queda
registrada en la tabla. El brief conserva los identificadores funcionales y las reglas de pagos,
contratos y garantías. Los cálculos y las verificaciones de archivos se distinguen de las pruebas
del prototipo propuestas para noviembre.

## 69
La Tabla 33 presenta decisiones Cerradas y Efectos de Implementación.
## Tabla 33
Decisiones Cerradas y Efectos de Implementación
DecisiónAlternativas
## Consideradas
Justificación y BaseResponsable
## Propuesto
## Impacto
Dos temas Aurora
desde versión
inicial
Tema único; tema
automático de
## Windows
Contrato visual del proyecto y RN-35 a RN-37 del
PRD. Claro inicial si falta preferencia.
## Caleb
## Tardencilla
Dos paletas completas;
mismo FXML y
permisos.
Paneles opacos
bajo datos
Transparencia en
formularios y tablas
Contraste reproducible y contrato visual.Caleb
## Tardencilla
Aurora solo en fondo
independiente.
Ventana única con
lateral
## Ventanas
independientes;
navegación repetida
en menús
Contrato visual y RNF de navegación.Caleb
## Tardencilla
BorderPane y diálogos
propietarios; restaurar
foco.
## Contorno
funcional propio
Borde decorativo
#D5DCE8 como
campo
Contraste calculado; border Claro #64748B y
Oscuro #94A3B8.
## Caleb
## Tardencilla
Separar border de
decorative en CSS.
Adoptar el
contrato visual del
proyecto
Conservar los
valores anteriores
Decisión histórica del contrato visual del proyecto.
Antes / adoptado: background #F3F5FA /
#F5F7FC y #0B1220 / #0F1424; surface oscuro
#182235 / #182136; text #172033 / #17213A y
## #F3F5FA / #F1F5F9.
## Caleb
## Tardencilla
Sustituir solo tokens;
mantener arquitectura.
## Completar
actualización
cromática
Mantener acento y
muted preliminares
Antes / adoptado: muted #475569 / #4B5568 y
#CBD5E1 / #B7C3D8; accent Claro #4338CA /
#4F46E5; on-accent oscuro #0B1220 / #111827.
Foco separado #4338CA / #C4B5FD.
## Caleb
## Tardencilla
## Tokens
independientes;
contrastes calculados.

## 70
DecisiónAlternativas
## Consideradas
Justificación y BaseResponsable
## Propuesto
## Impacto
Radio de control 8
y escala completa
Radio preliminar 6;
espacios parciales
Contrato obligatorio; panel conserva 12. Agregar
12 y 32 a espaciado.
## Caleb
## Tardencilla
Actualizar UiMetrics y
estilos sin cambios de
datos.
Fondo sin
GaussianBlur
inicial
Blur adicional de
radio 63 o menor;
imagen fija
TRD compatible; RadialGradient reduce borde sin
efecto extra. Luces antes 0.12/0.10 uniformes,
ahora tres alfas de Tabla 3.
## Caleb
## Tardencilla
Máximo tres nodos
decorativos, sin hilos
ni animación.
Preferences por
UUID y último
tema local
Nueva tabla;
preferencia global
para todas las
cuentas
Contrato visual y API Preferences; no altera
PostgreSQL.
## Steven
## Cuadra
Manejo de fallo de
lectura y MSG-003 al
guardar.
Tasa API solo
informativa; pago
con autorización
por fecha
## Convertir
directamente con la
última caché
comercial
Continuidad con PRD y términos del proveedor;
BCN separado.
## Aris
## Gavarrete
Dos regiones de tasas
y bloqueo de
conversión sin
autorización.
Número de recibo
posterior al
commit
Reserva al abrir
formulario;
secuencia no
reversible
RN-07 y TRD: contador bloqueado con pago y
auditoría.
## Alfredo
## Montalvan
No mostrar número
definitivo durante
edición; reintento por
## UUID.
## Formato
monetario
explícito
Locale automático
de Windows; coma
decimal de
ejemplos previos
El contrato visual del proyecto fija punto decimal
y coma de miles. Es localización del proyecto.
## Caleb
## Tardencilla
Corregir ejemplos
tipográficos del PRD
sin modificar valores.
Reportes blancos
y DTO
Aurora en
impresión;
adaptador Hibernate
de Jasper
Contrato visual, continuidad con TRD y POM de
biblioteca.
## Aris
## Gavarrete
Página opaca blanca;
evitar dependencia
Hibernate 6.3.1.Final.

## 71
DecisiónAlternativas
## Consideradas
Justificación y BaseResponsable
## Propuesto
## Impacto
## Evaluación
académica interna
Entrevistas o SUS
sin participantes
reales
Modalidad aportada y ausencia de campo.Steven
## Cuadra
Resultados esperados;
evidencia identificable
para informar
ejecución.

## 72
La investigación de este brief consulta el conjunto documental de Latch mediante
referencias cruzadas al Flujo, PRD, TRD, Esquema y Plan y fuentes originales de W3C,
OpenJFX, Oracle, fabricantes y Banco Central de Nicaragua. El antecedente científico se limita
al resumen editorial, como indica el texto. Sesenta pares de colores sólidos se comprobaron por
cálculo, sin inspeccionar píxeles de JavaFX. Las composiciones se elaboraron para el documento
y el plan conserva las pruebas funcionales futuras. El Apéndice E registra autoría, secciones
consultadas y motivo de confiabilidad de cada fuente, con fecha real de consulta.

## 73
## Referencias
AYR Tech (Pty) Ltd. (s. f.-a). API authentication notes.
https://www.exchangerate-api.com/docs/authentication
AYR Tech (Pty) Ltd. (s. f.-b). ExchangeRate-API - Free & Pro Currency Converter API.
https://www.exchangerate-api.com/
AYR Tech (Pty) Ltd. (s. f.-c). Standard requests.
https://www.exchangerate-api.com/docs/standard-requests
AYR Tech (Pty) Ltd. (s. f.-d). Supported currencies.
https://www.exchangerate-api.com/docs/supported-currencies
AYR Tech (Pty) Ltd. (s. f.-e). Terms of service. https://www.exchangerate-api.com/terms
Banco Central de Nicaragua. (20 de octubre de 2025). BCN informa que la tasa de deslizamiento
del tipo de cambio del córdoba respecto al dólar de los Estados Unidos de América
continuará en cero por ciento (0%) anual en el año 2026.
https://www.bcn.gob.ni/divulgacion-prensa/bcn-informa-que-la-tasa-de-deslizamiento-
del-tipo-de-cambio-del-cordoba-respecto
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026b). Documento de Flujo de
Latch. [Manuscrito no publicado]. Universidad Americana.
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026c). Documento de Requisitos del
Producto de Latch. [Manuscrito no publicado]. Universidad Americana.
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026d). Documento de Requisitos
Técnicos de Latch. [Manuscrito no publicado]. Universidad Americana.
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026e). Esquema de Base de Datos y
Servicios de Latch. [Manuscrito no publicado]. Universidad Americana.

## 74
Cuadra, S., Montalvan, A., Tardencilla, C. y Gavarrete, A. (2026f). Plan de Implementación de
Latch. [Manuscrito no publicado]. Universidad Americana.
Gluon. (s. f.). JavaFX. https://gluonhq.com/products/javafx/
Hibernate. (s. f.). 7.4 series. https://hibernate.org/orm/releases/7.4/
Jaspersoft. (s. f.-a). JasperReports 7.0.8.
https://github.com/Jaspersoft/jasperreports/releases/tag/7.0.8
Jaspersoft. (s. f.-b). JasperReports Library artifacts. [POM de la etiqueta 7.0.8].
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/pom-parent.xml
Malewicz, M. (s. f.). Aurora UI: New visual trend for 2021.
https://hype4.academy/articles/design/aurora-ui-new-visual-trend-for-2021
Nielsen, J. (24 de abril de 1994). 10 usability heuristics for user interface design.
https://www.nngroup.com/articles/ten-usability-heuristics/
OpenJFX. (s. f.-a). GaussianBlur JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/effect/GaussianBlur.html
OpenJFX. (s. f.-b). JavaFX CSS reference guide.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html
OpenJFX. (s. f.-c). Node JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/Node.html
OpenJFX. (s. f.-d). Platform JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/application/Platform.html
OpenJFX. (s. f.-e). RadialGradient JavaFX 21.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/paint/RadialGradient.html

## 75
Oracle. (s. f.). Preferences Java SE 21.
https://docs.oracle.com/en/java/javase/21/docs/api/java.prefs/java/util/prefs/
## Preferences.html
PostgreSQL JDBC Driver. (s. f.). Download. https://jdbc.postgresql.org/download/
The PostgreSQL Global Development Group. (s. f.). Versioning policy.
https://www.postgresql.org/support/versioning/
Wang, F. (2023). The present and future of the digital transformation of real estate: A systematic
review of smart real estate. Business Informatics, 17(2), 85–97.
https://doi.org/10.17323/2587-814X.2023.2.85.97
World Wide Web Consortium. (s. f.-a). Understanding success criterion 1.4.11: Non-text
contrast. https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html
World Wide Web Consortium. (s. f.-b). Understanding success criterion 1.4.3: Contrast
minimum. https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
World Wide Web Consortium. (s. f.-c). Understanding success criterion 2.4.7: Focus visible.
https://www.w3.org/WAI/WCAG22/Understanding/focus-visible.html
World Wide Web Consortium. (s. f.-d). Understanding success criterion 2.5.8: Target size
minimum. https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html
World Wide Web Consortium. (2024). Web content accessibility guidelines WCAG 2.2.
https://www.w3.org/TR/WCAG22/

## 76
## Apéndice A
## Wireframes Complementarios
Los formularios complementarios precisan campos del catálogo de unidades y del
expediente de inquilinos. Cada figura conserva el patrón de superficie opaca, rótulos persistentes
y acciones al pie del diálogo. El formulario de unidad solicita código único por propiedad y un
canon de referencia que no cambia contratos vigentes. El formulario de inquilino requiere
nombre y un medio de contacto, con los documentos como anexos opcionales. Los ejemplos son
sintéticos y no representan expedientes de personas reales; sus identificadores PAN-21 y
PAN-22 conservan la trazabilidad del catálogo principal y de sus requisitos relacionados.
PAN-01 Inicio de Sesión
La Figura A1 muestra wireframe de Inicio de Sesión.
## Figura A1
Wireframe de Inicio de Sesión
La Tabla A1 presenta contrato de PAN-01.

## 77
## Tabla A1
Contrato de PAN-01
AspectoEspecificación
Regiones y capasStackPane; zona de identidad y GridPane de acceso. A: periferia estática. B:
surface opaca. C: controles y foco.
Controles y
datos
txtUsuario, pwdClave, btnIngresar, rbClaro, rbOscuro, chkSinEfectos.
## Datos: Usuario, Contraseña.
Validación y
mensajes
Cuenta y clave requeridas; fallo genérico MSG-001; bloqueo MSG-002.
EstadosVacío: credenciales; carga: Ingresando; error: MSG-001; éxito: abre
## PAN-02.
Acciones y
acceso
Enter ingresa; Alt+T enfoca tema; Tab recorre credenciales y opciones.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
No guardar claves. Cambiar tema conserva credenciales y foco. RF-M01-01
a 05, 12 a 15.
PAN-02 Panel Principal
La Figura A2 muestra wireframe de Panel Principal.
## Figura A2
Wireframe de Panel Principal
La Tabla A2 presenta contrato de PAN-02.

## 78
## Tabla A2
Contrato de PAN-02
AspectoEspecificación
Regiones y capasBorderPane; lateral 224, contexto 64, centro con GridPane adaptable. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
lblOcupacion, lblCobrado, lblPorCobrar, tblVencimientos, cboMoneda,
btnActualizar. Datos: Contrato, Vence, Saldo.
Validación y
mensajes
Período y moneda válidos; mostrar fecha de corte; sin sumar NIO con USD.
EstadosVacío: Sin contratos; carga: conserva corte anterior; error: MSG-044; éxito:
corte actualizado.
Acciones y
acceso
F5 actualiza; Enter en indicador abre lista filtrada; Alt+T cambia tema.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Indicadores con DTO agregado; no confundir garantía con ingreso. RF-M11-
01 a 10.
PAN-03 Lista de Propiedades
La Figura A3 muestra wireframe de Lista de Propiedades.
## Figura A3
Wireframe de Lista de Propiedades
La Tabla A3 presenta contrato de PAN-03.

## 79
## Tabla A3
Contrato de PAN-03
AspectoEspecificación
Regiones y capasBorderPane central; filtros arriba, TableView al centro, paginación abajo. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
txtBuscar, tblPropiedades, btnNueva, btnEditar, btnBaja, pagPropiedades.
## Datos: Nombre, Dirección, Unidades.
Validación y
mensajes
Baja solo sin unidades activas; MSG-008 ante dependencia.
EstadosVacío: MSG-007; carga: Cargando propiedades; error: MSG-006; éxito: fila
guardada.
Acciones y
acceso
Ctrl+N abre PAN-04; Enter edita; Ctrl+F busca; Menú muestra contexto.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
La baja es lógica y exclusiva del Propietario; consulta paginada de 50 filas.
## RF-M02-01, 09, 10.
PAN-04 Formulario de Propiedad
La Figura A4 muestra wireframe de Formulario de Propiedad.
## Figura A4
Wireframe de Formulario de Propiedad
La Tabla A4 presenta contrato de PAN-04.

## 80
## Tabla A4
Contrato de PAN-04
AspectoEspecificación
Regiones y capasDialogPane con GridPane de dos columnas y acciones inferiores. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
txtNombre, txtDireccion, txtNotas, btnGuardar, btnCancelar. Datos:
Nombre, Dirección, Notas opcionales.
Validación y
mensajes
Nombre y dirección obligatorios; MSG-004 junto al primer campo inválido.
EstadosVacío: alta; carga: Guardando; error: conserva entradas; éxito: MSG-009 y
vuelve a lista.
Acciones y
acceso
Ctrl+S guarda; Escape solicita descartar si hubo cambios; Tab según orden
visual. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
No actualiza copias históricas de datos del propietario en recibos. RF-M02-
## 01, 09.
PAN-05 Unidades
La Figura A5 muestra wireframe de Unidades.
## Figura A5
Wireframe de Unidades
La Tabla A5 presenta contrato de PAN-05.

## 81
## Tabla A5
Contrato de PAN-05
AspectoEspecificación
Regiones y capasBorderPane con filtros y TableView; detalle lateral colapsable. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
cboPropiedad, cboTipo, cboEstado, tblUnidades, btnNueva (solo
Propietario), imgUnidad. Datos: Unidad, Tipo, Estado, Canon.
Validación y
mensajes
Código único por propiedad; canon positivo; ocupación derivada; MSG-010
y MSG-011.
EstadosVacío: MSG-012; carga: Cargando unidades; error: MSG-006; éxito: fila
actualizada.
Acciones y
acceso
Ctrl+N abre PAN-21 en alta solo para Propietario; el Asistente abre edición
de condiciones/foto desde la fila; Enter abre detalle; F5 actualiza; Tab visita
filtros. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Foto opcional PNG/JPEG hasta 5 MiB; estado Ocupada no se edita. RF-
M02-02 a 08, 11.
PAN-06 Inquilinos
La Figura A6 muestra wireframe de Inquilinos.
## Figura A6
Wireframe de Inquilinos
La Tabla A6 presenta contrato de PAN-06.

## 82
## Tabla A6
Contrato de PAN-06
AspectoEspecificación
Regiones y capasBorderPane con tabla e historial en TabPane de detalle. A: periferia estática.
B: surface opaca. C: controles y foco.
Controles y
datos
txtBuscar, tblInquilinos, tabHistorial, btnNuevo, btnEditar, btnAdjuntar.
## Datos: Inquilino, Contacto, Contrato.
Validación y
mensajes
Nombre y un contacto; correo válido si existe; MSG-013 y MSG-014.
EstadosVacío: MSG-015; carga: Cargando historial; error: MSG-006; éxito:
## MSG-016.
Acciones y
acceso
Ctrl+N abre PAN-22; Ctrl+F busca; Enter abre historial; Tab visita pestañas.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Adjuntos opcionales; nunca exigir identificación o emergencia para guardar.
RF-M03-01 a 09.
PAN-07 Listado de Contratos
La Figura A7 muestra wireframe de Listado de Contratos.
## Figura A7
Wireframe de Listado de Contratos
La Tabla A7 presenta contrato de PAN-07.

## 83
## Tabla A7
Contrato de PAN-07
AspectoEspecificación
Regiones y capasBorderPane central con encabezado, filtros y tabla. A: periferia estática. B:
surface opaca. C: controles y foco.
Controles y
datos
txtBuscar, cboEstado, dpDesde, dpHasta, tblContratos, btnNuevo,
btnCobrar. Datos: Contrato, Unidad, Estado, Saldo.
Validación y
mensajes
Fechas de filtro coherentes; estado usa catálogo exacto; MSG-017.
EstadosVacío: MSG-018; carga: Cargando contratos; error: MSG-006; éxito: lista
filtrada.
Acciones y
acceso
Ctrl+N abre PAN-08; Enter abre PAN-09; Ctrl+F busca; Ctrl+P abre pago
seleccionado. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Por vencer sigue siendo activo; Vencido reserva unidad hasta cierre
explícito. RF-M04-10, 12; RF-M11-06.
PAN-08 Formulario de Contrato
La Figura A8 muestra wireframe de Formulario de Contrato.
## Figura A8
Wireframe de Formulario de Contrato
La Tabla A8 presenta contrato de PAN-08.

## 84
## Tabla A8
Contrato de PAN-08
AspectoEspecificación
Regiones y capasDialogPane con ScrollPane y GridPane; bloques de partes, vigencia y
montos. A: periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
cboUnidad, cboInquilino, dpInicio, dpFin, txtCanon, cboMoneda,
txtDiaPago, txtDeposito. Datos: Unidad / Inquilino, Inicio / Fin, Canon /
Moneda, Día de pago / Depósito.
Validación y
mensajes
Un mes calendario; día 1 a 31; canon positivo; txtDeposito ≥ 0; sin
superposición; MSG-019 a MSG-022.
EstadosVacío: Borrador nuevo; carga: Guardando; error: campo identificado; éxito:
## MSG-023.
Acciones y
acceso
Ctrl+S guarda Borrador; Ctrl+Shift+A abre confirmación de activar; Escape
revisa cambios. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Activar exige inicio ≤ fecha de activación (ejemplo: inicio y activación
23/10/2026) y registra garantía en la misma operación; importe cero:
Retenido sin movimientos ni recibo; renovar crea sucesor histórico. RF-
M04-01 a 09, 11, 13.
PAN-09 Cargos y Estado de Cuenta
La Figura A9 muestra wireframe de Cargos y Estado de Cuenta.
## Figura A9
Wireframe de Cargos y Estado de Cuenta
La Tabla A9 presenta contrato de PAN-09.

## 85
## Tabla A9
Contrato de PAN-09
AspectoEspecificación
Regiones y capasBorderPane con resumen contractual y tabla; detalle de aplicaciones inferior.
A: periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
lblContrato, lblSaldo, dpCorte, tblCargos, btnCargo, btnPago,
btnEstadoCuenta. Datos: Vence, Concepto, Abonado, Saldo.
Validación y
mensajes
Cargo positivo y concepto; anular solo sin aplicaciones y con rol; MSG-024.
EstadosVacío: MSG-025; carga: Calculando saldo; error: MSG-006; éxito:
aplicaciones visibles.
Acciones y
acceso
Ctrl+P abre PAN-10; Ctrl+N abre cargo manual; Enter consulta aplicaciones.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Vencido prevalece sobre Parcial si queda deuda vencida; separar mora y
crédito. RF-M05-05 a 11; RF-M10-05.
PAN-10 Registro de Pago
La Figura A10 muestra wireframe de Registro de Pago.
## Figura A10
Wireframe de Registro de Pago
La Tabla A10 presenta contrato de PAN-10.

## 86
## Tabla A10
Contrato de PAN-10
AspectoEspecificación
Regiones y capasDialogPane con GridPane de entrada y TableView de aplicación. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
txtMonto, cboMoneda, dpPago, cboMetodo, lblTasa, rbAutomatico,
rbManual, tblAplicaciones, btnConfirmar. Datos: Monto / Moneda, Fecha /
Método, Tasa autorizada / Origen.
Validación y
mensajes
Monto positivo; tasa de fecha si cambia moneda; sumas no exceden saldo;
MSG-026 a MSG-029.
EstadosVacío: sin monto; carga: Confirmando; error: conserva UUID; éxito:
MSG-030 y PAN-11.
Acciones y
acceso
Ctrl+Enter abre resumen de confirmación; Escape vuelve sin pago; Tab
sigue entrada y distribución. labelFor y nombres accesibles; sin depender de
color.
Desarrollo y
trazas
H4 solo al confirmar; número definitivo después de commit; reintento
idempotente. RF-M06-01 a 08, 11, 12; RF-M09-09.
PAN-11 Vista Previa de Recibo
La Figura A11 muestra wireframe de Vista Previa de Recibo.
## Figura A11
Wireframe de Vista Previa de Recibo
La Tabla A11 presenta contrato de PAN-11.

## 87
## Tabla A11
Contrato de PAN-11
AspectoEspecificación
Regiones y capasBorderPane; ToolBar arriba, página blanca en ScrollPane. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
imgPagina, btnExportar, btnAnular, lblNumero, lblEstado. Datos: página,
referencia y totales.
Validación y
mensajes
Anular requiere Propietario, motivo y ausencia de dependencias; MSG-031 y
## MSG-032.
EstadosVacío: sin selección; carga: Generando página; error: MSG-033; éxito:
número inmutable visible.
Acciones y
acceso
Ctrl+S exporta; Escape regresa al contrato; foco retorna a acción de origen.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Visor integrado: JasperPrintManager renderiza cada página a PNG en
memoria mediante ImageIO y la muestra en ImageView; sin Swing UI ni
SwingFXUtils. HostServices abre solo el archivo exportado o su carpeta.
Imprimir desde el PDF exportado conserva número; anulado lleva marca
ANULADO y motivo. RF-M06-08 a 10; RF-M10-01, 10.
PAN-12 Depósitos de Garantía
La Figura A12 muestra wireframe de Depósitos de Garantía.
## Figura A12
Wireframe de Depósitos de Garantía
La Tabla A12 presenta contrato de PAN-12.

## 88
## Tabla A12
Contrato de PAN-12
AspectoEspecificación
Regiones y capasBorderPane con resumen de garantía, movimientos y acción de liquidar. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
tblDepositos, lblInicial, lblAplicado, lblDevuelto, lblRemanente,
btnLiquidar. Datos: Contrato, Estado, Remanente.
Validación y
mensajes
Concepto por deducción; suma limitada al remanente; MSG-034 y
## MSG-035.
EstadosVacío: MSG-036; carga: Cargando movimientos; error: MSG-006; éxito:
identidad de saldo.
Acciones y
acceso
Enter muestra movimientos; Alt+L abre liquidación si autorizado; Tab
recorre importes. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Inicial = aplicado + devuelto + remanente; depósito separado de renta. RF-
M07-01 a 07.
PAN-13 Mantenimiento
La Figura A13 muestra wireframe de Mantenimiento.
## Figura A13
Wireframe de Mantenimiento
La Tabla A13 presenta contrato de PAN-13.

## 89
## Tabla A13
Contrato de PAN-13
AspectoEspecificación
Regiones y capasBorderPane con filtros y TableView; formulario en diálogo PAN-30. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
cboUnidad, cboPrioridad, cboEstado, tblSolicitudes, btnNueva, btnAtender.
## Datos: Unidad, Prioridad, Estado, Costo.
Validación y
mensajes
Descripción y unidad; prioridad Baja/Media/Alta; costo no negativo;
## MSG-037.
EstadosVacío: MSG-038; carga: Cargando solicitudes; error: MSG-006; éxito:
## MSG-039.
Acciones y
acceso
Ctrl+N abre solicitud; Enter abre detalle; Alt+E cambia estado con
confirmación. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Resuelta y Cancelada son terminales; gasto no crea cargo automático. RF-
M08-01 a 07; RF-M05-05.
PAN-14 Tipo de Cambio
La Figura A14 muestra wireframe de Tipo de Cambio.
## Figura A14
Wireframe de Tipo de Cambio
La Tabla A14 presenta contrato de PAN-14.

## 90
## Tabla A14
Contrato de PAN-14
AspectoEspecificación
Regiones y capasBorderPane con referencia API, tasa autorizada e historial en regiones
separadas. A: periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
lblReferencia, lblOrigen, lblAntiguedad, tblTasas, btnConsultar,
btnTasaManual. Datos: Fecha, Origen, NIO por USD.
Validación y
mensajes
USD, NIO finito en banda 1–100 y fechas coherentes; fuera de banda:
cuarentena sin reemplazar vigente y evento M09
REFERENCIA_EN_CUARENTENA; manual con fuente y motivo.
MSG-040 a MSG-043; MSG-076/077 para autorización/cuota; MSG-083 si
la fecha ya tiene tasa autorizada.
EstadosVacío: Sin tasa; carga: Consultando; error: Sin conexión con historial; éxito:
referencia vigente.
Acciones y
acceso
F5 solicita consulta permitida; Alt+M abre tasa manual si Propietario; Tab
visita historial. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
No prellenar tasa de pago desde API; 24 h por instalación, caché compartida
en LAN. RF-M09-01 a 09.
PAN-15 Parámetros de Reportes
La Figura A15 muestra wireframe de Parámetros de Reportes.
## Figura A15
Wireframe de Parámetros de Reportes
La Tabla A15 presenta contrato de PAN-15.

## 91
## Tabla A15
Contrato de PAN-15
AspectoEspecificación
Regiones y capasBorderPane con ListView de reportes y GridPane de parámetros. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
lstReportes, dpDesde, dpHasta, cboPropiedad, cboMoneda, btnGenerar,
btnCancelar. Datos: Reporte, Desde / Hasta, Propiedad / Moneda.
Validación y
mensajes
Rango válido; contrato requerido en estado de cuenta; MSG-017 y
## MSG-045.
EstadosVacío: Elija un reporte; carga: Generando con H5; error: MSG-046; éxito:
## PAN-16.
Acciones y
acceso
Alt+G genera; Escape cancela H5 en frontera segura; Tab visita parámetros.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Diez reportes; exportaciones tabulares numéricas; no sumar monedas. RF-
M10-01 a 13.
PAN-16 Vista Previa de Reporte
La Figura A16 muestra wireframe de Vista Previa de Reporte.
## Figura A16
Wireframe de Vista Previa de Reporte
La Tabla A16 presenta contrato de PAN-16.

## 92
## Tabla A16
Contrato de PAN-16
AspectoEspecificación
Regiones y capasBorderPane; ToolBar de páginas y exportación; ScrollPane con página
blanca. A: periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
imgPagina, lblPagina, btnAnterior, btnSiguiente, btnPdf, btnXlsx,
prgExportacion. Datos: página, referencia y totales.
Validación y
mensajes
Destino escribible; confirmar reemplazo; XLSX solo tabulares; MSG-047 y
## MSG-048.
EstadosVacío: MSG-049; carga: Exportando; error: MSG-046; éxito: MSG-050.
Acciones y
acceso
Ctrl+S exporta PDF; PageDown cambia página fuera de campos; Escape
cancela exportación. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Visor integrado: páginas PNG en memoria con JasperPrintManager e
ImageIO, mostradas en ImageView, sin Swing UI ni SwingFXUtils;
HostServices abre archivo exportado o carpeta. Impresión desde el PDF
exportado. Archivo temporal y publicación final atómica; nunca anunciar
archivo incompleto. RF-M10-09, 10, 12; RN-28.
PAN-17 Configuración
La Figura A17 muestra wireframe de Configuración.
## Figura A17
Wireframe de Configuración
La Tabla A17 presenta contrato de PAN-17.

## 93
## Tabla A17
Contrato de PAN-17
AspectoEspecificación
Regiones y capasBorderPane con TabPane de parámetros, propietario y respaldo. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
txtGracia, txtMora, rbPorcentaje, rbFijo, txtUmbral, txtPropietario,
btnGuardar, btnRespaldo. Datos: Días de gracia / Mora, Aviso de
vencimiento, Datos del propietario.
Validación y
mensajes
Valores no negativos; umbral 30 por defecto; motivo por versión; MSG-051.
EstadosVacío: valores iniciales; carga: Guardando versión; error: MSG-006; éxito:
## MSG-052.
Acciones y
acceso
Ctrl+S guarda pestaña; Alt+R abre respaldo; Tab recorre la sección activa.
labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Preferencia Claro/Oscuro fuera de permisos administrativos; no altera
entidades. PAN-36/PAN-37 solicitan pwdClaveRespaldo y usan AES-256-
GCM; autenticidad o clave inválida: MSG-082. RF-M12-01 a 03, 06, 07.
PAN-18 Usuarios
La Figura A18 muestra wireframe de Usuarios.
## Figura A18
Wireframe de Usuarios
La Tabla A18 presenta contrato de PAN-18.

## 94
## Tabla A18
Contrato de PAN-18
AspectoEspecificación
Regiones y capasBorderPane con TableView; alta y restablecimiento en diálogos. A: periferia
estática. B: surface opaca. C: controles y foco.
Controles y
datos
tblUsuarios, btnNuevo, btnRol, btnDesactivar, btnRestablecer, cboRol.
## Datos: Usuario, Rol, Estado.
Validación y
mensajes
Usuario único; dos roles; conservar último Propietario; MSG-053 y
## MSG-054.
EstadosVacío: sin filtro; carga: Cargando usuarios; error: MSG-006; éxito:
## MSG-055.
Acciones y
acceso
Ctrl+N crea cuenta; Enter abre edición permitida; Menú ofrece acciones de
cuenta. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
No mostrar hashes o claves; rol nuevo exige nueva sesión de cuenta afectada.
RF-M01-03 a 06, 09 a 11.
PAN-19 Bitácora de Auditoría
La Figura A19 muestra wireframe de Bitácora de Auditoría.
## Figura A19
Wireframe de Bitácora de Auditoría
La Tabla A19 presenta contrato de PAN-19.

## 95
## Tabla A19
Contrato de PAN-19
AspectoEspecificación
Regiones y capasBorderPane con filtros, TableView y diálogo de detalle de evento. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
dpDesde, dpHasta, cboUsuario, cboOperacion, tblEventos, txtCorrelacion.
Datos: Fecha y hora, Usuario, Operación, Referencia.
Validación y
mensajes
Rango coherente; auditoría de solo lectura; MSG-017 y MSG-056.
EstadosVacío: MSG-057; carga: Consultando eventos; error: MSG-006; éxito:
conteo del filtro.
Acciones y
acceso
Ctrl+F enfoca correlación; Enter abre detalle; F5 actualiza el corte. labelFor
y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Persistir UTC y mostrar America/Managua; excluir claves y contenido
sensible de adjuntos. RF-M12-04, 05; RN-14.
PAN-20 Generación Masiva de Cargos
La Figura A20 muestra wireframe de Generación Masiva de Cargos.
## Figura A20
Wireframe de Generación Masiva de Cargos
La Tabla A20 presenta contrato de PAN-20.

## 96
## Tabla A20
Contrato de PAN-20
AspectoEspecificación
Regiones y capasDialogPane con período, previsualización TableView y progreso inferior. A:
periferia estática. B: surface opaca. C: controles y foco.
Controles y
datos
dpPeriodo, tblElegibles, btnPrevisualizar, btnGenerar, prgLote,
lblProcesados, btnCancelar. Datos: Contrato, Período, Importe, Resultado.
Validación y
mensajes
Período mensual y elegibles; omitir duplicados; MSG-058 a MSG-060.
EstadosVacío: sin elegibles; carga: n de total; error: conserva confirmados; éxito:
resumen del lote.
Acciones y
acceso
Alt+G genera tras resumen; Escape solicita cancelar; Tab alcanza Cancelar
durante H1. labelFor y nombres accesibles; sin depender de color.
Desarrollo y
trazas
Confirmar por contrato; cancelar entre contratos; reintento omite períodos
existentes. RF-M05-01 a 04, 12; RN-20.
La Figura A21 muestra formulario Complementario de Unidad PAN-21.
## Figura A21
Formulario Complementario de Unidad PAN-21
La Tabla A21 presenta campos del Formulario de Unidad.

## 97
## Tabla A21
Campos del Formulario de Unidad
Campo y fx:idValidación y Mensaje
cboPropiedad; txtCodigoPropiedad activa; código único en ella. Duplicado:
MSG-010. El Asistente consulta estos datos; no realiza
altas de unidad.
cboTipo; txtCanon; cboMonedaCasa, Apartamento, Cuarto o Local comercial; canon
positivo y NIO/USD. El Asistente puede editar canon de
referencia y moneda según RF-M02-04, sin cambiar
contratos vigentes.
cboEstado; imgUnidad; btnFotoOcupada derivado; fotografía PNG/JPEG opcional hasta 5
MiB; no borra historia. El Asistente puede editar la foto
(RF-M02-05), sin asignar En mantenimiento ni realizar
bajas.
btnGuardar; btnCancelarCtrl+S valida y guarda; Escape revisa cambios; foco al
primer error. Propietario: alta y edición; Asistente: solo
edición de condiciones/foto según Tabla 20, con validación
de rol en servicio.
La Figura A22 muestra formulario Complementario de Inquilino PAN-22.
## Figura A22
Formulario Complementario de Inquilino PAN-22
La Tabla A22 presenta campos del Formulario de Inquilino.

## 98
## Tabla A22
Campos del Formulario de Inquilino
Campo y fx:idValidación y Mensaje
txtNombre; txtTelefono;
txtCorreo
Nombre y al menos un contacto; MSG-013. Correo
proporcionado: MSG-014.
txtIdentificacion; txtEmergenciaOpcionales; coincidencia de identificación advierte y abre
registro existente.
lstAdjuntos; btnAdjuntarPDF/PNG/JPEG hasta 5 MiB; firma y huella; MSG-063
ante rechazo.
btnGuardar; btnCancelarCtrl+S guarda y MSG-016; Tab sigue orden visual; no
exigir campos opcionales.

## 99
## Apéndice B
Catálogo Completo de Mensajes
Los ochenta y nueve textos del catálogo (MSG-001 a MSG-089) corresponden a
situaciones definidas de validación, consulta y confirmación del sistema. La interfaz presenta el
contenido literal y reserva MSG como identificador interno para especificación y diagnóstico.
Los rótulos de campo, el nombre del contrato y los importes aparecen en regiones vecinas, de
modo que el mensaje conserve un significado estable. Los textos no muestran SQLSTATE,
contraseñas ni detalles internos de conexión. Cuando una operación queda incierta, el
controlador conserva su identificador y recupera el estado antes de permitir otro envío, según el
patrón de reintento del módulo de pagos.
La Tabla B1 presenta mensajes Exactos y Condición de Aparición.
## Tabla B1
Mensajes Exactos y Condición de Aparición
CódigoTipoTexto ExactoCuándo Aparece
MSG-001ErrorEl usuario o la contraseña no son correctos.Acceso inválido;
foco en contraseña
MSG-002ErrorNo se pudo iniciar sesión. Espere 15 minutos o
solicite revisión al Propietario.
Quinto fallo; evita
confirmar existencia
de cuenta
MSG-003AvisoEl tema cambió para esta sesión, pero no se
pudo guardar la preferencia.
Falla de Preferences;
una vez por sesión
MSG-004ErrorComplete este campo.Campo obligatorio
vacío
MSG-005Confirmaci
ón
Hay cambios sin guardar. Elija Guardar,
Descartar o Continuar editando.
Salida de formulario
modificado
MSG-006ErrorNo se pudieron cargar los datos. Conserve esta
vista y vuelva a intentar.
Falla de consulta
MSG-007VacíoTodavía no hay propiedades. Cree una
propiedad para registrar sus unidades.
Catálogo inicial de
## Propietario

## 100
CódigoTipoTexto ExactoCuándo Aparece
MSG-008ErrorLa propiedad tiene unidades activas. Revise sus
unidades antes de darla de baja.
Baja con
dependencia
MSG-009ÉxitoLa propiedad se guardó.Commit de
propiedad
MSG-010ErrorEste código de unidad ya existe en la propiedad
seleccionada.
Código duplicado
MSG-011ErrorLa unidad tiene un contrato activo y no puede
darse de baja.
Unidad ocupada
MSG-012VacíoNo hay unidades con estos filtros. Cambie los
filtros o cree una unidad.
Tabla vacía de
unidades
MSG-013ErrorEscriba un nombre y al menos un medio de
contacto.
Alta de inquilino
incompleta
MSG-014ErrorRevise el correo. Use el formato
nombre@dominio.
## Correo
proporcionado
inválido
MSG-015VacíoNo hay inquilinos con esta búsqueda. Cambie
el criterio o registre un inquilino.
Búsqueda sin
coincidencias
MSG-016ÉxitoLos datos del inquilino se guardaron.Commit de contacto
MSG-017ErrorLa fecha final debe ser igual o posterior a la
fecha inicial del filtro.
Rango de consulta
invertido
MSG-018VacíoNo hay contratos con estos filtros. Revise el
estado o el período seleccionado.
Tabla de contratos
vacía
MSG-019ErrorEl contrato debe durar al menos un mes
calendario.
Fecha final menor a
inicio más un mes
MSG-020ErrorEl canon debe ser mayor que 0.00.Canon inválido
MSG-021ErrorEl día de pago debe estar entre 1 y 31.Día contractual
inválido
MSG-022ErrorLa unidad ya tiene un contrato activo en ese
período.
Conflicto al activar
MSG-023ÉxitoEl contrato se guardó como Borrador.Guardado sin
activación
MSG-024ErrorEl cargo tiene aplicaciones activas. Revise los
pagos antes de anularlo.
Anulación de cargo
bloqueada
MSG-025VacíoEste contrato no tiene cargos al corte
seleccionado.
Estado de cuenta
vacío
MSG-026ErrorEl monto recibido debe ser mayor que 0.00.Pago no positivo
MSG-027ErrorNo hay tasa autorizada para la fecha del pago.
Solicite su registro al Propietario.
Conversión sin
autorización

## 101
CódigoTipoTexto ExactoCuándo Aparece
MSG-028ErrorLa aplicación supera el saldo del cargo. Revise
la distribución.
Aplicación manual
excesiva
MSG-029Confirmaci
ón
Confirme el monto, la moneda y la distribución
antes de registrar el pago.
Resumen anterior a
## H4
MSG-030ÉxitoEl pago se registró. El recibo está disponible.Commit financiero
confirmado
MSG-031ErrorEscriba el motivo de la anulación.Motivo vacío de
recibo
MSG-032ErrorEl pago tiene aplicaciones posteriores activas.
Revise sus dependencias antes de anularlo.
## RN-31
MSG-033ErrorEl recibo existe, pero no se pudo generar su
archivo. Vuelva a exportarlo.
Falla de reporte
posterior a pago
MSG-034ErrorEscriba un concepto para cada deducción.Liquidación
incompleta
MSG-035ErrorLa aplicación y la devolución superan el
remanente del depósito.
Identidad de garantía
inválida
MSG-036VacíoNo hay depósitos para el contrato seleccionado.Vista de garantía sin
registros
MSG-037ErrorDescriba la solicitud y seleccione una unidad.Mantenimiento
incompleto
MSG-038VacíoNo hay solicitudes con estos filtros.Lista de
mantenimiento vacía
MSG-039ÉxitoLa solicitud de mantenimiento se guardó.Commit de solicitud
MSG-040Informació
n
Referencia comercial informativa. No está
autorizada para registrar pagos.
Región API
permanente
MSG-041AvisoLa referencia está desactualizada. Consulte su
fecha antes de usarla como información.
## Próxima
actualización
superada
MSG-042AvisoNo hay conexión con el proveedor. Se conserva
la última referencia disponible.
Falla HTTP con
caché
MSG-043ErrorLa respuesta no contiene una tasa USD/NIO
válida. Se conserva el historial.
JSON rechazado
MSG-044AvisoLos indicadores conservan el corte anterior
porque no se pudo actualizar la consulta.
Panel con falla de
refresco
MSG-045ErrorSeleccione un contrato para generar su estado
de cuenta.
Reporte sin
parámetro
obligatorio

## 102
CódigoTipoTexto ExactoCuándo Aparece
MSG-046ErrorNo se pudo generar el reporte. Se conservaron
los parámetros.
## Falla H5
MSG-047ErrorNo se puede escribir en la carpeta elegida.
Seleccione otra ubicación.
Destino no
escribible
MSG-048Confirmaci
ón
Ya existe un archivo con ese nombre. Elija
Reemplazar o Cancelar.
Exportación con
colisión
MSG-049VacíoEl reporte no tiene datos para los filtros
seleccionados.
Reporte de cero filas
MSG-050ÉxitoEl archivo se guardó en la ubicación
seleccionada.
Publicación final
confirmada
MSG-051ErrorRevise los valores. Los días y montos
configurados no pueden ser negativos.
Parámetro inválido
MSG-052ÉxitoLa nueva versión de parámetros se guardó.Commit de
configuración
MSG-053ErrorEse nombre de usuario ya existe. Escriba otro
nombre.
Cuenta duplicada
MSG-054ErrorDebe conservar al menos una cuenta
Propietario activa.
Protección de último
administrador
MSG-055ÉxitoLos cambios de la cuenta se guardaron.Cuenta actualizada
MSG-056Informació
n
La bitácora es de solo lectura.Detalle de auditoría
MSG-057VacíoNo hay eventos con estos filtros.Auditoría sin
coincidencias
MSG-058Confirmaci
ón
Revise el período y los contratos elegibles antes
de generar los cargos.
## Previsualización H1
MSG-059Informació
n
Se solicitó cancelar. El contrato en curso
terminará antes de detener el lote.
Cancelación entre
contratos
MSG-060ÉxitoLa generación terminó. Revise los generados,
omitidos y fallidos del resumen.
## Fin H1
MSG-061AvisoEl cierre sigue esperando tareas activas. La
aplicación permanecerá abierta.
H6 supera 10 s
MSG-062ErrorNo tiene permiso para realizar esta acción.Servicio rechaza rol
MSG-063ErrorEl archivo debe ser PDF, PNG o JPEG y no
superar 5 MiB.
Adjunto de inquilino
inválido
MSG-064AvisoEl resultado del pago aún no está confirmado.
Consulte el estado antes de reintentar.
## Conexión
interrumpida tras
enviar

## 103
CódigoTipoTexto ExactoCuándo Aparece
MSG-065Informació
n
No hay tasa autorizada. Puede consultar
referencias, pero no registrar pagos entre
monedas.
Sin tasa financiera
MSG-066Confirmaci
ón
Esta operación dará de baja el registro y
conservará su historial.
## PAN-23
MSG-067Confirmaci
ón
El recibo quedará Anulado y conservará su
número. Confirme el motivo registrado.
## PAN-29
MSG-068ÉxitoEl depósito se liquidó. Revise las aplicaciones,
devoluciones y el remanente.
Commit de
liquidación
MSG-069Informació
n
La referencia está vigente como información
hasta la próxima actualización indicada.
API dentro de
intervalo
MSG-070Informació
n
Tasa manual autorizada para la fecha indicada.
Consulte su fuente y motivo.
## Autorización
financiera visible
MSG-071ErrorFalta la configuración de conexión. Revise la
instalación y vuelva a intentar.
## ERR-001:
Configuración de
BD ausente
MSG-072ErrorLa base de datos requiere una versión
compatible del esquema. Cierre y revise la
instalación.
ERR-003: Esquema
incompatible
MSG-073AvisoSe usará el fondo simplificado porque no se
pudo cargar la decoración.
ERR-004: Recurso
Aurora ausente
MSG-074ErrorUse de 12 a 128 caracteres y repita la misma
contraseña.
## ERR-012:
contraseña de menos
de 12 o más de 128
caracteres, o
confirmación
distinta.
MSG-075ErrorEsta solicitud ya corresponde a otros datos.
Consulte el pago antes de continuar.
## ERR-030: UUID
repetido con otra
huella
MSG-076ErrorNo se pudo autorizar la consulta. Solicite
revisión de la configuración.
## ERR-040: HTTP
401, 403 o llave
inválida
MSG-077AvisoLa consulta alcanzó su límite de uso. Se
conserva la última referencia disponible.
## ERR-043: HTTP
429 o quota-reached
MSG-078ErrorOtro usuario cambió este registro. Compare los
datos actuales antes de guardar.
ERR-052: Conflicto
de versión de
entidad
MSG-079ErrorEl respaldo no superó la comprobación de
integridad. Seleccione otro paquete.
ERR-055: Respaldo
con hash incorrecto

## 104
CódigoTipoTexto ExactoCuándo Aparece
MSG-080ErrorLa fecha del equipo difiere de la base de datos.
Revise el reloj antes de registrar operaciones.
ERR-057: diferencia
absoluta entre reloj
local y BD mayor de
5 minutos; bloquear
nuevas operaciones
financieras.
MSG-081AvisoLa restauración requiere conciliar los recibos
posteriores al respaldo antes de volver a operar.
## ERR-058:
Restauración con
recibos posteriores
al respaldo
MSG-082ErrorNo se pudo verificar la autenticidad del
respaldo. Revise la contraseña o seleccione otro
paquete.
## ERR-059:
autenticidad
AES-256-GCM o
contraseña de
respaldo inválida en
## PAN-36/PAN-37;
no publicar
restauración.
MSG-083ErrorYa existe una tasa autorizada para esa fechaERR-060: intento de
autorizar una fecha
efectiva ya
registrada;
DuplicadoException
en PAN-32.
MSG-084ErrorEl cargo no es válido para esta aplicación de
garantía. Seleccione un cargo del mismo
contrato que no esté anulado.
## ERR-061 /
CargoIncompatibleE
xception: aplicación
de garantía a cargo
inexistente, anulado
o de otro contrato.
Rechazar y
conservar la
propuesta; exceso de
saldo mantiene su
error específico.

## 105
CódigoTipoTexto ExactoCuándo Aparece
MSG-085ErrorLa garantía tiene una liquidación pendiente.
Complete las aplicaciones y la devolución antes
de terminar el contrato.
## ERR-062 /
GarantiaPendienteE
xception: terminar
sin liquidación
completa y
autorizada, con
decisiones,
movimientos o
remanente
pendientes. Bloquear
el cierre; conservar
la excepción de
garantía inicial cero
sin movimientos
(DC-08) y la
posibilidad de deuda
residual después de
terminar.
MSG-086ErrorLa operación se revirtió por una inconsistencia
de datos. No se guardaron cambios. Solicite
revisión antes de reintentar.
## ERR-063 /
IntegridadException
financiera:
inconsistencia
detectada antes del
commit y rollback
confirmado.
Conservar causa
técnica. Commit
incierto: ERR-031 o
## ERR-054 /
MSG-064; hash de
respaldo: ERR-055 /
## MSG-079;
autenticidad o clave:
## ERR-059 /
## MSG-082.

## 106
CódigoTipoTexto ExactoCuándo Aparece
MSG-087ErrorLa solicitud de mantenimiento pertenece a otra
propiedad. Revise la propiedad o la solicitud
seleccionada.
## ERR-065 /
PropiedadIncompati
bleException: gasto
asociado a una
solicitud cuya
unidad pertenece a
otra propiedad.
Rechazar el registro
y permitir corregir la
selección.
MSG-088ErrorEl cambio de estado solicitado no está
permitido. Se conserva el estado actual.
## ERR-066 /
TransicionInvalidaE
xception: transición
de mantenimiento
fuera de Abierta a
En proceso o
Cancelada, y En
proceso a Resuelta o
## Cancelada. Resuelta
y Cancelada son
terminales; repetir el
estado actual lo
devuelve sin
duplicar cambios ni
eventos. Rechazar
otras transiciones sin
alterar el estado.

## 107
CódigoTipoTexto ExactoCuándo Aparece
MSG-089Informació
n
La operación se canceló antes de publicar el
archivo. Puede volver a generarlo con los
mismos parámetros.
## H5 /
OperacionCancelada
Exception, sin ERR:
cancelación
cooperativa durante
consulta, llenado o
exportación,
confirmada al
terminar el trabajo y
limpiar temporales
antes de publicar.
Conservar destino
previo y finanzas
confirmadas; si ya se
publicó, informar el
resultado real. H1
conserva MSG-059
y termina el contrato
en curso antes de
detener el lote.
Nota. En MSG-002 y los demás mensajes con valores configurables, el número mostrado
se sustituye por el parámetro vigente; el catálogo conserva el valor inicial. El límite de adjuntos
de 5 MiB es fijo.

## 108
## Apéndice C
Especificación de Tokens CSS
Los tokens conservan sus nombres semánticos y cambian únicamente de valor al
seleccionar Claro u Oscuro. La matriz asigna colores mediante looked-up colors y deja la
geometría en propiedades compatibles o constantes tipadas de Java. Las hojas incluidas son una
especificación de referencia nativa para JavaFX 21, sin dependencias web, var(--token) ni
WebView. El controlador debe activar invalid y enlazar eventos con el servicio de tema para
completar el comportamiento descrito. La presencia de código en el brief no acredita su
compilación ni sustituye las pruebas de escenas, calendarios y popups en la versión seleccionada.

## 109
La Tabla C1 presenta inventario Propuesto de FXML y Controladores.
## Tabla C1
Inventario Propuesto de FXML y Controladores
PantallaArchivo FXMLControladorResponsabilidad Propuesta
PAN-01/ui/fxml/inicio-sesion.fxmlInicioSesionControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-02/ui/fxml/panel-principal.fxmlPanelPrincipalControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-03/ui/fxml/lista-propiedades.fxmlListaPropiedadesControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-04/ui/fxml/formulario-propiedad.fxmlFormularioPropiedadControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-05/ui/fxml/unidades.fxmlUnidadesControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-06/ui/fxml/inquilinos.fxmlInquilinosControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-07/ui/fxml/listado-contratos.fxmlListadoContratosControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-08/ui/fxml/formulario-contrato.fxmlFormularioContratoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-09/ui/fxml/cargos-estado-cuenta.fxmlCargosEstadoCuentaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-10/ui/fxml/registro-pago.fxmlRegistroPagoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-11/ui/fxml/vista-previa-recibo.fxmlVistaPreviaReciboControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.

## 110
PantallaArchivo FXMLControladorResponsabilidad Propuesta
PAN-12/ui/fxml/depositos-garantia.fxmlDepositosGarantiaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-13/ui/fxml/mantenimiento.fxmlMantenimientoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-14/ui/fxml/tipo-cambio.fxmlTipoCambioControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-15/ui/fxml/parametros-reportes.fxmlParametrosReportesControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-16/ui/fxml/vista-previa-reporte.fxmlVistaPreviaReporteControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-17/ui/fxml/configuracion.fxmlConfiguracionControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-18/ui/fxml/usuarios.fxmlUsuariosControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-19/ui/fxml/bitacora-auditoria.fxmlBitacoraAuditoriaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-20/ui/fxml/generacion-masiva-cargos.fxmlGeneracionMasivaCargosControlle
r
Caleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-21/ui/fxml/formulario-unidad.fxmlFormularioUnidadControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-22/ui/fxml/formulario-inquilino.fxmlFormularioInquilinoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-23/ui/fxml/confirmacion-baja-logica.fxmlConfirmacionBajaLogicaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-24/ui/fxml/historial-inquilino.fxmlHistorialInquilinoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.

## 111
PantallaArchivo FXMLControladorResponsabilidad Propuesta
PAN-25/ui/fxml/activacion-contrato.fxmlActivacionContratoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-26/ui/fxml/renovacion-contrato.fxmlRenovacionContratoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-27/ui/fxml/terminacion-cancelacion.fxmlTerminacionCancelacionControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-28/ui/fxml/cargo-manual.fxmlCargoManualControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-29/ui/fxml/anulacion-recibo.fxmlAnulacionReciboControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-30/ui/fxml/solicitud-mantenimiento.fxmlSolicitudMantenimientoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-31/ui/fxml/liquidacion-garantia.fxmlLiquidacionGarantiaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-32/ui/fxml/tasa-manual-autorizada.fxmlTasaManualAutorizadaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-33/ui/fxml/alta-edicion-usuario.fxmlAltaEdicionUsuarioControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-34/ui/fxml/cambio-contrasena.fxmlCambioContrasenaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-35/ui/fxml/restablecimiento-cuenta.fxmlRestablecimientoCuentaControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-36/ui/fxml/respaldo-manual.fxmlRespaldoManualControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-37/ui/fxml/restauracion-mantenimiento.fxmlRestauracionMantenimientoControl
ler
Caleb Tardencilla; revisión: Plan,
## Tablas 4–5.

## 112
PantallaArchivo FXMLControladorResponsabilidad Propuesta
PAN-38/ui/fxml/detalle-evento.fxmlDetalleEventoControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-39/ui/fxml/cambios-sin-guardar.fxmlCambiosSinGuardarControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
PAN-40/ui/fxml/cierre-con-tareas-activas.fxmlCierreConTareasActivasControllerCaleb Tardencilla; revisión: Plan,
## Tablas 4–5.
Los controladores de la Tabla C1 pertenecen a ni.edu.uam.latch.presentacion. Los nombres se derivan del catálogo español sin
tildes ni conectores, siguiendo InicioSesionController y RegistroPagoController; CambiosSinGuardar conserva Sin por su significado.
La raíz classpath /ui/ contiene fxml/ plano, css/ (components.css, claro.css, oscuro.css y print.css), imagenes/ e
i18n/mensajes_es_NI.properties; /reportes/ contiene las plantillas. No se usa themes/ ni una imagen de fondo simplificado.
La Tabla C2 presenta matriz de Tokens Semánticos de Color.
## Tabla C2
Matriz de Tokens Semánticos de Color
TokenValor ClaroValor
## Oscuro
UsoEstadoPropiedad JavaFX o Constante
-latch-background#F5F7FC#0F1424Fondo generalNormal-fx-background-color
-latch-surface#FFFFFF#182136Lectura y formulariosNormal-fx-background-color
-latch-elevated#FFFFFF#202C44Diálogos y popupsNormal-fx-background-color
-latch-text#17213A#F1F5F9Texto y cifrasTodos-fx-text-fill
-latch-muted#4B5568#B7C3D8Metadatos y placeholderNormal-fx-text-fill; -fx-prompt-text-fill
-latch-accent#4F46E5#A5B4FCPrimario y enlacesNormal-fx-background-color; -fx-text-fill

## 113
TokenValor ClaroValor
## Oscuro
UsoEstadoPropiedad JavaFX o Constante
-latch-on-accent#FFFFFF#111827Texto en primarioNormal-fx-text-fill
-latch-focus#4338CA#C4B5FDAnillo externoFoco-fx-border-color
-latch-border#64748B#94A3B8Contorno funcionalNormal-fx-border-color
-latch-decorative#D5DCE8#334155Separador sin significadoNormal-fx-border-color
-latch-hover#EEF2FF#202C44Fila y acción secundariaHover-fx-background-color
-latch-pressed#E0E7FF#2C3B59Botón secundarioPresionado-fx-background-color
-latch-selection#E0E7FF#334155Fila seleccionadaSeleccionado-fx-background-color
-latch-accent-hover#4338CA#C7D2FEPrimarioHover-fx-background-color
## -latch-accent-
pressed
#3730A3#818CF8PrimarioPresionado-fx-background-color
-latch-disabled-bg#E5E7EB#28344BControl inactivoDeshabilitad
o
## -fx-background-color
-latch-disabled-text#4B5568#B7C3D8Explicación inactivaDeshabilitad
o
## -fx-text-fill
-latch-success#166534#86EFACPagado, Resuelta,
## Registrado
## Semántico-fx-text-fill; -fx-border-color
-latch-success-bg#F0FDF4#12352AApoyo de éxitoSemántico-fx-background-color
-latch-warning#854D0E#FDE68AParcial, Por vencerSemántico-fx-text-fill; -fx-border-color
-latch-warning-bg#FFFBEB#3A2E16Apoyo de advertenciaSemántico-fx-background-color
-latch-error#B42318#FCA5A5Vencido, error de entradaError-fx-text-fill; -fx-border-color
-latch-error-bg#FEF2F2#3B2028Apoyo de errorError-fx-background-color
-latch-info#1E40AF#93C5FDPendiente, Abierta,
## Retenido
## Semántico-fx-text-fill; -fx-border-color
-latch-info-bg#EFF6FF#1A3048Apoyo de informaciónSemántico-fx-background-color

## 114
La Tabla C3 presenta tokens de Referencia y Geometría.
## Tabla C3
Tokens de Referencia y Geometría
NombreValor ClaroValor OscuroUso y EstadoMecanismo
aurora-violet#6D5EF5#6D5EF5Solo decorativo, normalColor.web y Stop en
AuroraBackground
aurora-teal#4CC9C0#4CC9C0Solo decorativo, normalColor.web y Stop
aurora-lavender#B9AEFF#B9AEFFSolo decorativo, normalColor.web y Stop
space4/8/12/16/24/32IgualSeparación y márgenesdouble UiMetrics; Insets;
hgap/vgap
type-body/meta/
section/view
14/12/18/28IgualTexto por jerarquía-fx-font-size; Font.font
control/row36 mínimo / 40 inicialIgualCampo y tablasetMinHeight; -fx-cell-size; celda
adaptable
radius-control/panel8 / 12IgualContorno y superficie-fx-border-radius; -fx-background-
radius
sidebar/header/content224 / 64 mínimo / 24IgualVentana de referenciaBorderPane; prefWidth;
minHeight; padding
focus-ring2; separación 2IgualFoco externo-fx-border-width 2; insets -4
transitions120 ms, si aportan
continuidad
IgualSolo respuesta no financieraFadeTransition opcional;
desactivar en simple; sin CSS web
simplefalse inicial; true
elimina luces
IgualPreferencia independienteCheckBox y Preferences; sin tabla
nueva
La Tabla C4 presenta cálculo Completo de Pares de Contraste.

## 115
## Tabla C4
Cálculo Completo de Pares de Contraste
TemaPar FuncionalColoresRazón MostradaUmbralComparación sin
## Redondear
Clarotext / surface#17213A / #FFFFFF15.968708177288:14.5:1Aprobado
Claromuted / surface#4B5568 / #FFFFFF7.504110602305:14.5:1Aprobado
Clarotext / elevated#17213A / #FFFFFF15.968708177288:14.5:1Aprobado
Claromuted / elevated#4B5568 / #FFFFFF7.504110602305:14.5:1Aprobado
Clarotext / background#17213A / #F5F7FC14.898391204475:14.5:1Aprobado
Claromuted / background#4B5568 / #F5F7FC7.001140865847:14.5:1Aprobado
Clarotext / hover#17213A / #EEF2FF14.280801159432:14.5:1Aprobado
Claromuted / hover#4B5568 / #EEF2FF6.710919267867:14.5:1Aprobado
Clarotext / pressed#17213A / #E0E7FF12.960374089962:14.5:1Aprobado
Claromuted / pressed#4B5568 / #E0E7FF6.090416302845:14.5:1Aprobado
Clarotext / selection#17213A / #E0E7FF12.960374089962:14.5:1Aprobado
Claromuted / selection#4B5568 / #E0E7FF6.090416302845:14.5:1Aprobado
Claroon-accent / accent#FFFFFF / #4F46E56.287528596096:14.5:1Aprobado
Claroon-accent / accent-hover#FFFFFF / #4338CA7.903111480025:14.5:1Aprobado
Claroon-accent / accent-pressed#FFFFFF / #3730A39.933329874513:14.5:1Aprobado
Claroborder / surface#64748B / #FFFFFF4.758842787869:13:1Aprobado
Claroborder / elevated#64748B / #FFFFFF4.758842787869:13:1Aprobado
Claroborder / background#64748B / #F5F7FC4.439877086307:13:1Aprobado
Clarofocus / surface#4338CA / #FFFFFF7.903111480025:13:1Aprobado
Clarofocus / elevated#4338CA / #FFFFFF7.903111480025:13:1Aprobado

## 116
TemaPar FuncionalColoresRazón MostradaUmbralComparación sin
## Redondear
Clarofocus / background#4338CA / #F5F7FC7.373398352251:13:1Aprobado
Claroerror / surface#B42318 / #FFFFFF6.574288615577:13:1Aprobado
Claroerror / elevated#B42318 / #FFFFFF6.574288615577:13:1Aprobado
Claroerror / background#B42318 / #F5F7FC6.133641030857:13:1Aprobado
Clarosuccess / success-bg#166534 / #F0FDF46.811651629294:14.5:1Aprobado
Clarowarning / warning-bg#854D0E / #FFFBEB6.606582234644:14.5:1Aprobado
Claroerror / error-bg#B42318 / #FEF2F26.009895134838:14.5:1Aprobado
Claroinfo / info-bg#1E40AF / #EFF6FF8.014836919642:14.5:1Aprobado
Claroaccent / surface#4F46E5 / #FFFFFF6.287528596096:14.5:1Aprobado
Clarodisabled-text / disabled-bg#4B5568 / #E5E7EB6.061034383967:14.5:1Aprobado
Oscurotext / surface#F1F5F9 / #18213614.636767990681:14.5:1Aprobado
Oscuromuted / surface#B7C3D8 / #1821369.018502240001:14.5:1Aprobado
Oscurotext / elevated#F1F5F9 / #202C4412.735549438861:14.5:1Aprobado
Oscuromuted / elevated#B7C3D8 / #202C447.847058941916:14.5:1Aprobado
Oscurotext / background#F1F5F9 / #0F142416.729105894298:14.5:1Aprobado
Oscuromuted / background#B7C3D8 / #0F142410.307704479363:14.5:1Aprobado
Oscurotext / hover#F1F5F9 / #202C4412.735549438861:14.5:1Aprobado
Oscuromuted / hover#B7C3D8 / #202C447.847058941916:14.5:1Aprobado
Oscurotext / pressed#F1F5F9 / #2C3B5910.212946572403:14.5:1Aprobado
Oscuromuted / pressed#B7C3D8 / #2C3B596.292747251229:14.5:1Aprobado
Oscurotext / selection#F1F5F9 / #3341559.451924418632:14.5:1Aprobado
Oscuromuted / selection#B7C3D8 / #3341555.823840454124:14.5:1Aprobado
Oscuroon-accent / accent#111827 / #A5B4FC8.898600916847:14.5:1Aprobado

## 117
TemaPar FuncionalColoresRazón MostradaUmbralComparación sin
## Redondear
Oscuroon-accent / accent-hover#111827 / #C7D2FE11.892547106984:14.5:1Aprobado
Oscuroon-accent / accent-pressed#111827 / #818CF85.947131151756:14.5:1Aprobado
Oscuroborder / surface#94A3B8 / #1821366.253733298070:13:1Aprobado
Oscuroborder / elevated#94A3B8 / #202C445.441415047757:13:1Aprobado
Oscuroborder / background#94A3B8 / #0F14247.147709565713:13:1Aprobado
Oscurofocus / surface#C4B5FD / #1821368.685588199819:13:1Aprobado
Oscurofocus / elevated#C4B5FD / #202C447.557388215405:13:1Aprobado
Oscurofocus / background#C4B5FD / #0F14249.927200106031:13:1Aprobado
Oscuroerror / surface#FCA5A5 / #1821368.448420534719:13:1Aprobado
Oscuroerror / elevated#FCA5A5 / #202C447.351027048370:13:1Aprobado
Oscuroerror / background#FCA5A5 / #0F14249.656129130069:13:1Aprobado
Oscurosuccess / success-bg#86EFAC / #12352A9.535853793037:14.5:1Aprobado
Oscurowarning / warning-bg#FDE68A / #3A2E1610.656853798872:14.5:1Aprobado
Oscuroerror / error-bg#FCA5A5 / #3B20287.774322683853:14.5:1Aprobado
Oscuroinfo / info-bg#93C5FD / #1A30487.463714763941:14.5:1Aprobado
Oscuroaccent / surface#A5B4FC / #1821368.043395618742:14.5:1Aprobado
Oscurodisabled-text / disabled-bg#B7C3D8 / #28344B7.017785625470:14.5:1Aprobado
Nota. Sesenta pares; comparación en precisión doble antes del formato decimal. Los controles inactivos se incluyen con meta
voluntaria de 4.5:1, aunque la excepción de contraste permita tratarlos aparte. Selección y hover mantienen texto principal/secundario;
el foco usa superficie exterior uniforme y separación del primario.

## 118
Paletas Completas de Referencia
claro.css
## .root, .context-menu, .tooltip {
-latch-background: #F5F7FC;
-latch-surface: #FFFFFF;
-latch-elevated: #FFFFFF;
-latch-text: #17213A;
-latch-muted: #4B5568;
-latch-accent: #4F46E5;
-latch-on-accent: #FFFFFF;
-latch-focus: #4338CA;
-latch-border: #64748B;
-latch-decorative: #D5DCE8;
-latch-hover: #EEF2FF;
-latch-pressed: #E0E7FF;
-latch-selection: #E0E7FF;
-latch-accent-hover: #4338CA;
-latch-accent-pressed: #3730A3;
-latch-disabled-bg: #E5E7EB;
-latch-disabled-text: #4B5568;
## -latch-success: #166534;
-latch-success-bg: #F0FDF4;
-latch-warning: #854D0E;
-latch-warning-bg: #FFFBEB;
-latch-error: #B42318;
-latch-error-bg: #FEF2F2;
-latch-info: #1E40AF;
-latch-info-bg: #EFF6FF;
## }
oscuro.css
## .root, .context-menu, .tooltip {
-latch-background: #0F1424;
## -latch-surface: #182136;
-latch-elevated: #202C44;
-latch-text: #F1F5F9;
-latch-muted: #B7C3D8;
-latch-accent: #A5B4FC;
## -latch-on-accent: #111827;
-latch-focus: #C4B5FD;
-latch-border: #94A3B8;
## -latch-decorative: #334155;
-latch-hover: #202C44;
-latch-pressed: #2C3B59;
## -latch-selection: #334155;
-latch-accent-hover: #C7D2FE;
-latch-accent-pressed: #818CF8;
-latch-disabled-bg: #28344B;
-latch-disabled-text: #B7C3D8;
-latch-success: #86EFAC;
-latch-success-bg: #12352A;
-latch-warning: #FDE68A;
-latch-warning-bg: #3A2E16;

## 119
-latch-error: #FCA5A5;
-latch-error-bg: #3B2028;
-latch-info: #93C5FD;
-latch-info-bg: #1A3048;
## }
Estilos Compartidos de Componentes
La hoja compartida aplica la paleta semántica a los estados usados por los controles de
Latch. Las tablas conservan cuarenta unidades como altura inicial y el controlador ampliará las
celdas cuando una columna requiera texto multilínea. El anillo de foco queda fuera del fondo de
la acción primaria para comparar su color con la superficie uniforme. Los mensajes de error
utilizan rótulos con ajuste de línea y el borde propio del campo. El modo sin efectos elimina el
fondo decorativo; la geometría, los colores funcionales y la semántica del formulario siguen
disponibles en los dos temas.
/* Latch: recursos nativos JavaFX 21. Especificación de referencia. */
## .root {
-fx-font-family: "Segoe UI";
## -fx-font-size: 14px;
## -fx-background-color: -latch-background;
## -fx-base: -latch-surface;
## -fx-control-inner-background: -latch-surface;
## -fx-text-background-color: -latch-text;
## -fx-text-inner-color: -latch-text;
## -fx-accent: -latch-accent;
## -fx-focus-color: -latch-focus;
-fx-faint-focus-color: transparent;
## }
## .surface, .dialog-pane {
## -fx-background-color: -latch-surface;
## -fx-background-radius: 12;
## -fx-padding: 24;
## }
## .label {
## -fx-text-fill: -latch-text;
## }
## .text {
## -fx-fill: -latch-text;
## }
## .dialog-pane {
-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 12, 0, 0, 4);
## -fx-border-color: -latch-decorative;
## -fx-border-radius: 12;
## }
## .muted {
## -fx-text-fill: -latch-muted;
## -fx-font-size: 12px;

## 120
## }
## .view-title {
## -fx-font-size: 28px;
-fx-font-weight: bold;
## }
## .section-title {
## -fx-font-size: 18px;
-fx-font-weight: bold;
## }
## .error-message {
## -fx-text-fill: -latch-error;
-fx-wrap-text: true;
## }
## .success-message {
## -fx-text-fill: -latch-success;
-fx-wrap-text: true;
## }
## .info-message {
## -fx-text-fill: -latch-info;
-fx-wrap-text: true;
## }
## .warning-message {
## -fx-text-fill: -latch-warning;
-fx-wrap-text: true;
## }
## .button, .toggle-button {
## -fx-background-color: -latch-surface;
## -fx-text-fill: -latch-text;
## -fx-border-color: -latch-border;
## -fx-border-radius: 8;
## -fx-background-radius: 8;
## -fx-min-height: 36;
## -fx-padding: 7 16;
-fx-cursor: hand;
## }
## .button:hover, .toggle-button:hover {
## -fx-background-color: -latch-hover;
## }
## .button:pressed, .toggle-button:pressed {
## -fx-background-color: -latch-pressed;
## }
## .button.primary, .toggle-button:selected {
## -fx-background-color: -latch-accent;
## -fx-text-fill: -latch-on-accent;
## -fx-border-color: -latch-accent;
## }
## .button.primary:hover {
## -fx-background-color: -latch-accent-hover;
## }
## .button.primary:pressed {
## -fx-background-color: -latch-accent-pressed;
## }
## .button.danger {
## -fx-text-fill: -latch-error;
## }
## .button:disabled, .toggle-button:disabled, .text-input:disabled,
## .combo-box:disabled, .date-picker:disabled, .check-box:disabled,

## 121
## .radio-button:disabled {
## -fx-opacity: 1;
## -fx-background-color: -latch-disabled-bg;
## -fx-text-fill: -latch-disabled-text;
-fx-cursor: default;
## }
## .button:focused, .toggle-button:focused, .text-input:focused,
## .combo-box:focused, .date-picker:focused, .table-view:focused,
## .list-view:focused, .check-box:focused, .radio-button:focused,
## .pagination .button:focused, .tab:focused, .scroll-pane:focused {
## -fx-border-color: -latch-focus;
## -fx-border-width: 2;
## -fx-border-insets: -4;
## -fx-border-radius: 10;
## }
## .text-input, .combo-box, .date-picker {
## -fx-background-color: -latch-surface;
## -fx-text-fill: -latch-text;
## -fx-prompt-text-fill: -latch-muted;
## -fx-highlight-fill: -latch-selection;
## -fx-highlight-text-fill: -latch-text;
## -fx-border-color: -latch-border;
## -fx-border-width: 1;
## -fx-border-radius: 8;
## -fx-background-radius: 8;
## -fx-min-height: 36;
## -fx-padding: 7 12;
## }
## .text-input:hover, .combo-box:hover, .date-picker:hover {
## -fx-border-color: -latch-focus;
## }
## .text-input:invalid, .combo-box:invalid, .date-picker:invalid {
## -fx-border-color: -latch-error;
## -fx-border-width: 2;
## }
## .text-input:invalid:focused, .combo-box:invalid:focused,
## .date-picker:invalid:focused {
## -fx-border-color: -latch-focus, -latch-error;
## -fx-border-insets: -4, 0;
## -fx-border-width: 2, 2;
## }
## .text-area .content {
## -fx-background-color: -latch-surface;
## }
## .combo-box .list-cell, .date-picker .text-field {
## -fx-text-fill: -latch-text;
## -fx-background-color: -latch-surface;
## }
## .combo-box .arrow, .date-picker .arrow {
## -fx-background-color: -latch-text;
## }
## .combo-box-popup .list-view, .context-menu, .tooltip,
## .date-picker-popup {
## -fx-background-color: -latch-elevated;
## -fx-border-color: -latch-border;
## -fx-border-width: 1;
## -fx-background-radius: 8;

## 122
## -fx-padding: 8;
## }
## .list-view, .table-view, .scroll-pane,
## .scroll-pane > .viewport {
## -fx-background-color: -latch-surface;
## }
## .list-cell, .table-row-cell, .table-cell {
## -fx-background-color: -latch-surface;
## -fx-text-fill: -latch-text;
## }
## .table-view {
## -fx-border-color: -latch-border;
## -fx-border-radius: 8;
## -fx-background-radius: 8;
## -fx-padding: 1;
## }
## .table-row-cell {
## -fx-cell-size: 40px;
## }
## .table-cell {
## -fx-padding: 9 12;
-fx-border-color: transparent;
## }
## .table-view .column-header-background,
## .table-view .column-header, .table-view .filler {
## -fx-background-color: -latch-hover;
## }
## .table-view .column-header .label {
## -fx-text-fill: -latch-text;
## }
## .table-row-cell:hover, .table-row-cell:hover .table-cell,
## .list-cell:hover {
## -fx-background-color: -latch-hover;
## }
## .table-row-cell:selected, .table-row-cell:selected .table-cell,
## .list-cell:selected {
## -fx-background-color: -latch-selection;
## -fx-text-fill: -latch-text;
## }
## .table-row-cell:selected {
## -fx-border-color: -latch-border;
## -fx-border-width: 1 1 1 4;
## }
## .table-row-cell:focused {
## -fx-border-color: -latch-focus;
## -fx-border-width: 2;
## }
## .table-row-cell:empty, .table-row-cell:empty .table-cell {
## -fx-background-color: -latch-surface;
-fx-border-color: transparent;
## }
## .money {
-fx-alignment: CENTER-RIGHT;
## }
## .menu-bar, .tool-bar {
## -fx-background-color: -latch-surface;
## -fx-padding: 8 12;

## 123
## }
## .menu, .menu-item {
## -fx-background-color: -latch-elevated;
## }
## .menu .label, .menu-item .label, .tooltip {
## -fx-text-fill: -latch-text;
## }
## .menu:hover, .menu:showing, .menu-item:hover,
## .menu-item:focused {
## -fx-background-color: -latch-selection;
## -fx-border-color: -latch-focus;
## -fx-border-width: 2;
## }
## .menu-item:disabled {
## -fx-opacity: 1;
## }
## .menu-item:disabled .label {
## -fx-text-fill: -latch-muted;
## }
## .check-box .box, .radio-button .radio {
## -fx-background-color: -latch-surface;
## -fx-border-color: -latch-border;
## -fx-border-width: 2;
## }
## .check-box:selected .mark, .radio-button:selected .dot {
## -fx-background-color: -latch-text;
## }
## .check-box:hover .box, .radio-button:hover .radio {
## -fx-background-color: -latch-hover;
## }
## .date-picker-popup .month-year-pane,
## .date-picker-popup .calendar-grid {
## -fx-background-color: -latch-elevated;
## }
## .date-cell {
## -fx-background-color: -latch-elevated;
## -fx-text-fill: -latch-text;
## -fx-min-width: 36; -fx-min-height: 36;
## }
## .date-cell:hover, .date-cell:selected {
## -fx-background-color: -latch-selection;
## -fx-text-fill: -latch-text;
## }
## .date-cell:focused {
## -fx-border-color: -latch-focus;
## -fx-border-width: 2;
## }
## .date-cell:disabled {
## -fx-opacity: 1;
## -fx-text-fill: -latch-muted;
## }
## .dialog-pane > .header-panel,
## .dialog-pane > .button-bar, .tab-pane .tab-header-area {
## -fx-background-color: -latch-surface;
## }
## .dialog-pane .content.label {
-fx-wrap-text: true;

## 124
## }
## .tab {
## -fx-background-color: -latch-surface;
## }
## .tab:selected {
## -fx-background-color: -latch-selection;
## }
## .tab .tab-label {
## -fx-text-fill: -latch-text;
## }
## .progress-bar > .track {
## -fx-background-color: -latch-selection;
## }
## .progress-bar > .bar {
## -fx-background-color: -latch-accent;
## }
## .progress-indicator {
## -fx-progress-color: -latch-accent;
## }
## .hyperlink {
## -fx-text-fill: -latch-accent;
-fx-underline: true;
## }
## .hyperlink:focused {
## -fx-border-color: -latch-focus;
## -fx-border-width: 2;
## }
## .scroll-bar .thumb {
## -fx-background-color: -latch-border;
## }
## .scroll-bar .track {
## -fx-background-color: -latch-surface;
## }
## .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow {
## -fx-background-color: -latch-text;
## }
## .status-success {
## -fx-text-fill: -latch-success;
## -fx-background-color: -latch-success-bg;
## }
## .status-warning {
## -fx-text-fill: -latch-warning;
## -fx-background-color: -latch-warning-bg;
## }
## .status-error {
## -fx-text-fill: -latch-error;
## -fx-background-color: -latch-error-bg;
## }
## .status-info {
## -fx-text-fill: -latch-info;
## -fx-background-color: -latch-info-bg;
## }
## .paper {
-fx-background-color: white;
## }

## 125
Componente Reutilizable de Fondo
package ni.edu.uam.latch.presentacion.tema;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
public final class AuroraBackground extends Pane {
public AuroraBackground() {
setMouseTransparent(true);
setFocusTraversable(false);
widthProperty().addListener(o -> requestLayout());
heightProperty().addListener(o -> requestLayout());
## }
public void update(boolean dark, boolean simple) {
getChildren().clear();
if (simple) {
return;
## }
double[] xs = {0.10, 0.84, 0.94};
double[] ys = {0.08, 0.02, 0.88};
double[] radii = {0.48, 0.42, 0.40};
String[] colors = {"#6D5EF5", "#4CC9C0", "#B9AEFF"};
double[] alphas = dark ? new double[] {0.16, 0.14, 0.12}
: new double[] {0.20, 0.18, 0.14};
for (int i = 0; i < 3; i++) {
Color c = Color.web(colors[i], alphas[i]);
Rectangle r = new Rectangle();
r.setFill(new RadialGradient(0, 0, xs[i], ys[i], radii[i],
true, CycleMethod.NO_CYCLE,
new Stop(0, c),
new Stop(0.5, Color.web(colors[i], alphas[i] * 0.25)),
new Stop(1, Color.web(colors[i], 0))));
r.widthProperty().bind(widthProperty());
r.heightProperty().bind(heightProperty());
r.setMouseTransparent(true);
getChildren().add(r);
## }
## }
## }

## 126
Servicio de Tema y Persistencia
El servicio supone que restore, register y select se invocan en el hilo JavaFX, con las
hojas bajo /ui/css/. La restauración lee preferencias al iniciar o autenticar, antes de mostrar la
operación. Window permite aplicar la paleta a las escenas emergentes, además de las ventanas
registradas de forma explícita. El controlador sincroniza los RadioButton con una guarda que
evita guardar durante la carga. Al cerrar, H6 coordina el ejecutor de preferencias y espera su fin
fuera del hilo gráfico, junto con las operaciones financieras pendientes de completar.
package ni.edu.uam.latch.presentacion.tema;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;
public final class ThemeService implements AutoCloseable {
public enum Theme {
## Claro, Oscuro
## }
private final Preferences prefs = Preferences.userRoot()
## .node("ni/edu/uam/latch/presentation");
private final Set<Scene> scenes =
Collections.newSetFromMap(new WeakHashMap<>());
private final Map<AuroraBackground, Boolean> backgrounds =
new WeakHashMap<>();
private final ExecutorService io = Executors.newSingleThreadExecutor(r ->
## {
Thread t = new Thread(r, "latch-ui-prefs");
t.setDaemon(true);
return t;
## });
private final Consumer<String> notices;
private Theme theme = Theme.Claro;
private boolean simple, warned;
private String userId;
public ThemeService(Consumer<String> notices) {
this.notices = notices;
Window.getWindows().addListener(
(ListChangeListener<Window>) change -> {
while (change.next()) {
for (Window w : change.getAddedSubList()) {
if (w.getScene() != null) {

## 127
register(w.getScene());
## }
## }
## }
## });
## }
private Theme readTheme(Preferences node, String key) {
try {
return Theme.valueOf(node.get(key, "Claro"));
} catch (RuntimeException ex) {
return Theme.Claro;
## }
## }
public void restore(String opaqueId) {
userId = opaqueId;
theme = Theme.Claro;
simple = false;
try {
Preferences n = opaqueId == null ? prefs
: prefs.node("users/" + opaqueId);
theme = readTheme(n, opaqueId == null ? "lastTheme" : "theme");
simple = n.getBoolean("simple", false);
} catch (RuntimeException ex) {
theme = Theme.Claro;
simple = false;
## }
repaint();
## }
public void register(Scene scene) {
scenes.add(scene);
apply(scene);
## }
public void register(AuroraBackground background) {
backgrounds.put(background, true);
background.update(theme == Theme.Oscuro, simple);
## }
public void select(Theme value, boolean noEffects) {
if (!Platform.isFxApplicationThread()) {
throw new IllegalStateException("Use el hilo JavaFX");
## }
theme = Objects.requireNonNull(value);
simple = noEffects;
repaint();
String id = userId;
Theme chosen = theme;
boolean reduced = simple;
io.execute(() -> {
try {
prefs.put("lastTheme", chosen.name());
prefs.putBoolean("simple", reduced);
if (id != null) {
Preferences n = prefs.node("users/" + id);

## 128
n.put("theme", chosen.name());
n.putBoolean("simple", reduced);
## }
prefs.flush();
} catch (RuntimeException | BackingStoreException ex) {
Platform.runLater(() -> {
if (!warned) {
warned = true;
notices.accept("MSG-003");
## }
## });
## }
## });
## }
private String resource(String file) {
return Objects.requireNonNull(getClass().getResource(
"/ui/css/" + file)).toExternalForm();
## }
private void apply(Scene s) {
String base = resource("components.css");
if (!s.getStylesheets().contains(base)) {
s.getStylesheets().add(base);
## }
s.getStylesheets().removeAll(resource("claro.css"),
resource("oscuro.css"));
s.getStylesheets().add(resource(
theme == Theme.Claro ? "claro.css" : "oscuro.css"));
s.getRoot().applyCss();
## }
private void repaint() {
for (Scene s : List.copyOf(scenes)) {
apply(s);
## }
backgrounds.keySet().forEach(b ->
b.update(theme == Theme.Oscuro, simple));
## }
public Theme theme() {
return theme;
## }
public boolean simple() {
return simple;
## }
public void close() {
io.shutdown();
## }
## }

## 129
Selector FXML e Integración del Controlador
<HBox spacing="12" xmlns:fx="http://javafx.com/fxml/1">
## <fx:define>
<ToggleGroup fx:id="grpTema"/>
## </fx:define>
<Label text="Tema"/>
<RadioButton fx:id="rbClaro" text="Claro"
toggleGroup="$grpTema" accessibleText="Tema Claro"/>
<RadioButton fx:id="rbOscuro" text="Oscuro"
toggleGroup="$grpTema" accessibleText="Tema Oscuro"/>
<CheckBox fx:id="chkSinEfectos" text="Sin efectos decorativos"/>
</HBox>
La Tabla C5 presenta enlace del Selector y Revisión de Integración.
## Tabla C5
Enlace del Selector y Revisión de Integración
ElementoRegla del Controlador
Carga FXMLImportar HBox, Label, RadioButton, CheckBox y ToggleGroup. El
fragmento es contenido para incluir en inicio-sesion.fxml y panel-
principal.fxml, con fx:id propio en cada controlador.
RestauraciónLlamar restore(null) antes del acceso y restore(UUID) tras autenticar;
actualizar selección y CheckBox sin emitir select. Claro cuando falta
preferencia.
EscuchaCuando cambia ToggleGroup o CheckBox por acción de usuario, llamar
select(Claro u Oscuro, sinEfectos). Ignorar toggle nulo; no aceptar otro
enum.
Acceso por tecladoAlt+T enfoca RadioButton seleccionado; flechas cambian selección;
Space activa CheckBox. RadioButton nativo anuncia nombre y estado.
Escenas y fondosRegistrar Scene al crear ventana y AuroraBackground al construir shell.
DialogPane hereda hoja en su Scene; popups reciben registro de Window
al mostrarse.
SincronizaciónAl cambiar desde MenuBar, actualizar también selector visible mediante
el estado theme/simple del servicio; mantener foco y contenido del nodo
actual.
Cierreclose detiene nuevas escrituras; H6 espera terminación segura del
ejecutor fuera del hilo gráfico. Registrar fin sin guardar contraseñas.

## 130
## Apéndice D
Lista de Verificación de Revisión de Diseño
La lista separa el control documental realizado al producir los archivos de las
verificaciones que dependen del prototipo. El conteo de figuras y elementos permite comprobar
la cobertura del brief, mientras las metas de teclado y escalado requieren ejecutar controles
reales. El equipo registrará evidencia por versión y repetirá el caso afectado después de una
corrección. Los resultados esperados de noviembre conservan su carácter de planificación hasta
que exista un registro verificable. Las categorías de esta tabla evitan convertir un dibujo, una
cifra calculada o una lectura de documentación en una prueba funcional realizada con Latch.
La Tabla D1 presenta control Documental y Verificación del Producto.
## Tabla D1
Control Documental y Verificación del Producto
ÍtemCriterio y Estado de la Evidencia
## Cobertura
documental
40 elementos PAN; 20 especificaciones principales y 2 complementarias
de respaldo/restauración; 22 wireframes; 3 diagramas; 8 composiciones a
color; 2 maquetaciones de reportes. Conteo verificable en el documento.
Mensajes89 mensajes MSG (MSG-001 a MSG-089) con texto exacto, tipo y
condición; catálogos del producto conservados.
PaletasDos temas completos y mismos nombres semánticos; 25 tokens de color
más referencias y geometría. Especificación documental.
Contraste60 pares calculados; comparación sin redondear y fondos sólidos. No es
inspección de píxeles de JavaFX.
APA del archivoCarta, márgenes 2.54 cm, Times New Roman 12, doble espacio, tablas
nativas y campos automáticos de página. Revisión estructural y visual
del PDF.
Coherencia técnicaPostgreSQL y JavaFX conservados; sustitución histórica de tokens por el
contrato visual del proyecto documentada; tasa informativa separada de
autorizada.
Teclado y foco realPlan EV-05; cero bloqueos y foco visible en las cuatro condiciones
tema/efectos. Requiere ejecutar Latch.

## 131
ÍtemCriterio y Estado de la Evidencia
Escalado y cadenas
largas
Plan EV-09 al 100/125/150/200 %, incluida la meta Should del PRD al
200 %. Requiere capturas del prototipo.
Tema y preferenciasPlan EV-06; veinte valores conservados, popups y error de
lectura/escritura. Requiere ejecución, no inferir desde las figuras.
Concurrencia y
finanzas
EV-03 y EV-08 más pruebas del TRD: contador, UUID, cancelación y
rollback. Requiere aplicación y PostgreSQL.
Lector de pantallaNarrador Windows 11: nombres, montos, errores y orden de lectura. Sin
declaración de compatibilidad hasta disponer de evidencia.
Límites de
investigación
Sin entrevistas, testimonios, propietario participante, SUS, satisfacción o
aprobación docente inventada. Fuente científica limitada al resumen
consultado.

## 132
## Apéndice E
Fuentes y Trazabilidad Documental
La trazabilidad identifica qué afirmación respalda cada obra y el alcance de la consulta
efectuada. La fecha real de revisión de fuentes es el 23 de octubre de 2026 y no la fecha base del
proyecto. Las obras internas integran el conjunto documental del proyecto y conservan el orden
de autoría y las letras asignadas al conjunto documental. Las páginas oficiales respaldan sus
especificaciones o condiciones publicadas, sin acreditar resultados de uso de Latch. La tabla
distingue el resumen científico de la lectura completa y relaciona los cinco documentos
hermanos mediante las referencias 2026b a 2026f.

## 133
La Tabla E1 presenta fuentes Consultadas y Límites de Atribución.
## Tabla E1
Fuentes Consultadas y Límites de Atribución
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Autenticación sin
clave en URL
AYR Tech (Pty) Ltd.
Tipo: Documentación original
Título: API authentication notes
Fecha: s. f.-a
https://www.exchangerate-api.com/
docs/authentication
## Consulta: 23/10/2026
Sección: Métodos In-URL y
## Bearer
## Confiabilidad:
Documentación técnica del
operador identificado
Free autenticado:
actualización diaria y
1500 solicitudes/mes
AYR Tech (Pty) Ltd.
Tipo: Documentación original
Título: ExchangeRate-API - Free &
Pro Currency Converter API
Fecha: s. f.-b
https://www.exchangerate-api.com/
## Consulta: 23/10/2026
## Sección: Plans & Pricing,
plan Free
## Confiabilidad: Condiciones
publicadas por proveedor; no
evidencia de disponibilidad
medida
Base USD,
conversion_rates y
errores
AYR Tech (Pty) Ltd.
Tipo: Documentación original
Título: Standard requests
Fecha: s. f.-c
https://www.exchangerate-api.com/
docs/standard-requests
## Consulta: 23/10/2026
Sección: Usage guide y
Error responses
## Confiabilidad: Contrato
JSON oficial; ejemplos
documentales, sin petición
ejecutada
Cobertura de NIOAYR Tech (Pty) Ltd.
Tipo: Documentación original
Título: Supported currencies
Fecha: s. f.-d
https://www.exchangerate-api.com/
docs/supported-currencies
## Consulta: 23/10/2026
Sección: All supported
currencies, fila NIO
Confiabilidad: Catálogo del
operador que presta el
servicio

## 134
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Operador, licencia, uso
final e información no
transaccional
AYR Tech (Pty) Ltd.
Tipo: Documentación original
Título: Terms of service
Fecha: s. f.-e
https://www.exchangerate-api.com/
terms
## Consulta: 23/10/2026
## Sección: Preamble, License
y Disclaimer
## Confiabilidad: Texto
contractual del proveedor;
condiciones observadas al
consultar
0 % anual y 36.6243
NIO/USD para 2026
Banco Central de Nicaragua
Tipo: Fuente institucional
Título: BCN informa que la tasa de
deslizamiento del tipo de cambio del
córdoba respecto al dólar de los
Estados Unidos de América continuará
en cero por ciento (0%) anual en el
año 2026
Fecha: 20 de octubre de 2025
https://www.bcn.gob.ni/divulgacion-
prensa/bcn-informa-que-la-tasa-de-
deslizamiento-del-tipo-de-cambio-
del-cordoba-respecto
## Consulta: 23/10/2026
Sección: Párrafo inicial del
comunicado
## Confiabilidad: Organismo
competente en política
cambiaria; no se extiende a
## 2027
## Identificadores,
permisos, reglas y
metas
## Cuadra, S., Montalvan, A.,
Tardencilla, C. y Gavarrete, A.
Tipo: Documento interno
Título: Documento de Requisitos del
Producto de Latch
## Fecha: 2026c
Latch_PRD_APA7_v3.pdf, archivo
suministrado
## Consulta: 23/10/2026
Sección: Requisitos M01 a
M12, RN, RNF y contrato de
tasas
## Confiabilidad: Documento
interno efectivamente
consultado; no valida
afirmaciones externas sin
revisar original
Fronteras técnicas y
actualización de tokens
preliminares
## Cuadra, S., Montalvan, A.,
Tardencilla, C. y Gavarrete, A.
Tipo: Documento interno
Título: Documento de Requisitos
Técnicos de Latch
## Fecha: 2026d
Latch_TRD_APA7_v3.pdf, archivo
suministrado
## Consulta: 23/10/2026
## Sección: Presentación,
transacciones, hilos y
preferencias
## Confiabilidad: Documento
interno efectivamente
consultado y compatible con
PostgreSQL

## 135
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Existencia de
distribución JavaFX
seleccionada
## Gluon
Tipo: Documentación original
Título: JavaFX
Fecha: s. f.
https://gluonhq.com/products/javafx/
## Consulta: 23/10/2026
## Sección: Descargas: 21.0.13
para Windows x64
## Confiabilidad: Publicación
del distribuidor; no implica
contrato LTS adquirido
7.4.6.Final publicada y
Java 21 admitido
## Hibernate
Tipo: Documentación original
Título: 7.4 series
Fecha: s. f.
https://hibernate.org/orm/releases/
## 7.4/
## Consulta: 23/10/2026
Sección: Compatibility y
Releases in this series
## Confiabilidad: Proyecto
responsable del ORM; se
conserva versión de los
documentos recibidos
Existencia de
biblioteca y límites de
etiquetado PDF
## Jaspersoft
Tipo: Documentación original
Título: JasperReports 7.0.8
Fecha: s. f.-a
https://github.com/Jaspersoft/
jasperreports/releases/tag/7.0.8
## Consulta: 23/10/2026
Sección: Lanzamiento y
notas de versión
## Confiabilidad: Repositorio
del fabricante; no se declara
## PDF/UA
Bytecode y separación
de adaptador Hibernate
## Jaspersoft
Tipo: Documentación original
Título: JasperReports Library artifacts
Fecha: s. f.-b
https://raw.githubusercontent.com/
## Jaspersoft/jasperreports/7.0.8/pom-
parent.xml
## Consulta: 23/10/2026
Sección: revision,
source.version,
target.version y
hibernate.version
Confiabilidad: POM de
etiqueta oficial verificable;
compatibilidad documental,
sin integración ejecutada

## 136
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Procedencia del estilo
de degradados
orgánicos
## Malewicz, M.
Tipo: Fuente primaria de estilo
Título: Aurora UI: New visual trend
for 2021
Fecha: s. f.
https://hype4.academy/articles/
design/aurora-ui-new-visual-trend-
for-2021
## Consulta: 23/10/2026
Sección: Título, autor,
introducción y descripción
pública
## Confiabilidad: Fuente
primaria admitida por
solicitud; parte restringida no
consultada; sin evidencia de
eficacia
Mapa de principios a
decisiones de Latch
## Nielsen, J.
Tipo: Documentación original
Título: 10 usability heuristics for user
interface design
Fecha: 24 de abril de 1994
https://www.nngroup.com/articles/
ten-usability-heuristics/
## Consulta: 23/10/2026
Sección: Diez heurísticas;
revisión visible de
## 30/01/2024
## Confiabilidad: Artículo
original de autor identificado
y entidad especializada
Rango permitido de 0
a 63
OpenJFX
Tipo: Documentación original
Título: GaussianBlur JavaFX 21
Fecha: s. f.-a
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/effect/
GaussianBlur.html
## Consulta: 23/10/2026
Sección: radiusProperty
Confiabilidad: API de la
versión seleccionada; no usar
un radio de blur web
Tokens y propiedades
CSS nativas
OpenJFX
Tipo: Documentación original
Título: JavaFX CSS reference guide
Fecha: s. f.-b
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/doc-
files/cssref.html
## Consulta: 23/10/2026
Sección: Looked-up colors y
controles
## Confiabilidad: Referencia
oficial de sintaxis; estilos del
equipo no ejecutados
Nombres accesibles y
fondo sin interacción
OpenJFX
Tipo: Documentación original
Título: Node JavaFX 21
Fecha: s. f.-c
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/
## Node.html
## Consulta: 23/10/2026
Sección: Accesibilidad, foco
y mouseTransparent
Confiabilidad: API oficial;
no prueba soporte de un
lector específico

## 137
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Detección de
accesibilidad activa y
cambio de estilos en
hilo gráfico
OpenJFX
Tipo: Documentación original
Título: Platform JavaFX 21
Fecha: s. f.-d
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/application/
## Platform.html
## Consulta: 23/10/2026
Sección: accessibilityActive
y hilo JavaFX
## Confiabilidad: Contrato
oficial; no equivale a
detectar preferencias
visuales de Windows
Luces estáticas con
paradas concretas
OpenJFX
Tipo: Documentación original
Título: RadialGradient JavaFX 21
Fecha: s. f.-e
https://openjfx.io/javadoc/21/
javafx.graphics/javafx/scene/paint/
RadialGradient.html
## Consulta: 23/10/2026
Sección: Constructor, centro,
radio y Stop
Confiabilidad: API oficial de
dibujo nativo
Persistencia local de
tema y manejo de
fallas
## Oracle
Tipo: Documentación original
Título: Preferences Java SE 21
Fecha: s. f.
https://docs.oracle.com/en/java/
javase/21/docs/api/java.prefs/java/
util/prefs/Preferences.html
## Consulta: 23/10/2026
Sección: Descripción de
clase, userRoot y flush
## Confiabilidad: Responsable
de Java; almacén
dependiente de
implementación
pgJDBC 42.7.14 en
## JDK 21
PostgreSQL JDBC Driver
Tipo: Documentación original
## Título: Download
Fecha: s. f.
https://jdbc.postgresql.org/download/
## Consulta: 23/10/2026
Sección: Latest versions,
## Java 8
## Confiabilidad: Proyecto
oficial del controlador; no
prueba conexión ejecutada
PostgreSQL 18.6
soportado hasta
noviembre de 2030
The PostgreSQL Global Development
## Group
Tipo: Documentación original
Título: Versioning policy
Fecha: s. f.
https://www.postgresql.org/support/
versioning/
## Consulta: 23/10/2026
## Sección: Tabla Releases
## Confiabilidad: Proyecto
responsable del motor y su
política de soporte

## 138
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
## Antecedente
internacional de
gestión de información
## Wang, F.
Tipo: Artículo científico, resumen
editorial
Título: The present and future of the
digital transformation of real estate: A
systematic review of smart real estate
## Fecha: 2023
https://doi.org/10.17323/2587-
## 814X.2023.2.85.97
## Consulta: 23/10/2026
Sección: Resumen de página
editorial; 22 documentos de
2012 a 2022
Confiabilidad: Artículo de
Business Informatics; autor,
DOI y método identificados;
consulta limitada al resumen
Fórmula sRGB y
umbral 0.04045
## World Wide Web Consortium
Tipo: Documentación original
Título: Web content accessibility
guidelines WCAG 2.2
## Fecha: 2024
https://www.w3.org/TR/WCAG22/
## Consulta: 23/10/2026
Sección: Definiciones de
luminancia relativa y
contraste; recomendación de
## 12/12/2024
## Confiabilidad: Organismo
que publica la
recomendación; adopción
como meta de escritorio
4.5:1 normal; 3:1
grande; definición de
texto grande
## World Wide Web Consortium
Tipo: Documentación original
Título: Understanding success
criterion 1.4.3: Contrast minimum
Fecha: s. f.-b
https://www.w3.org/WAI/
WCAG22/Understanding/contrast-
minimum.html
## Consulta: 23/10/2026
Sección: Success criterion e
intent
## Confiabilidad: Guía
explicativa oficial; no
certifica Latch
Foco visible sin
depender de glow
## World Wide Web Consortium
Tipo: Documentación original
Título: Understanding success
criterion 2.4.7: Focus visible
Fecha: s. f.-c
https://www.w3.org/WAI/
WCAG22/Understanding/focus-
visible.html
## Consulta: 23/10/2026
Sección: Success criterion e
intent
## Confiabilidad: Guía
explicativa oficial

## 139
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Señales funcionales ≥
## 3:1
## World Wide Web Consortium
Tipo: Documentación original
Título: Understanding success
criterion 1.4.11: Non-text contrast
Fecha: s. f.-a
https://www.w3.org/WAI/
WCAG22/Understanding/non-text-
contrast.html
## Consulta: 23/10/2026
Sección: Success criterion y
nota sobre umbral sin
redondeo
## Confiabilidad: Guía
explicativa oficial y
comparación exacta con
umbral
Referencia de 24
píxeles CSS y límite
de traslado a JavaFX
## World Wide Web Consortium
Tipo: Documentación original
Título: Understanding success
criterion 2.5.8: Target size minimum
Fecha: s. f.-d
https://www.w3.org/WAI/
WCAG22/Understanding/target-size-
minimum.html
## Consulta: 23/10/2026
Sección: Success criterion
Confiabilidad: Guía oficial;
el brief no equipara de forma
automática unidades lógicas
con CSS
Recorridos y ERR →
## MSG
Cuadra, Montalvan, Tardencilla y
## Gavarrete
Tipo: Documento interno
Título: Documento de Flujo de Latch
## Fecha: 2026b
Latch_App_Flow_APA7_v3.pdf,
archivo del proyecto
Consulta documental: 23/10/2026
Sección: Tabla 80, textos
Propuesto y recuperaciones
operativas
Confiabilidad: Fuente del
proyecto; no acredita
ejecución de Latch.
Persistencia y
contratos de servicios
Cuadra, Montalvan, Tardencilla y
## Gavarrete
Tipo: Documento interno
Título: Esquema de Base de Datos y
Servicios de Latch
## Fecha: 2026e
Latch_Backend_Schema_APA7_v3.
pdf, archivo del proyecto
Consulta documental: 23/10/2026
Sección: Referencia cruzada
a persistencia y servicios;
alcance técnico según TRD
Confiabilidad: Fuente del
proyecto; no acredita
ejecución de Latch.

## 140
Afirmación o
## Decisión
Autor, Tipo, Título y FechaURL o DOI y Consulta RealSección Utilizada y
## Confiabilidad
Calendario, RACI y
ejecución
Cuadra, Montalvan, Tardencilla y
## Gavarrete
Tipo: Documento interno
Título: Plan de Implementación de
## Latch
## Fecha: 2026f
Latch_Plan_Implementacion_APA7_
v3.pdf, archivo del proyecto
Consulta documental: 23/10/2026
Sección: Tablas 4–5 y 8;
Apéndices A–B, TAR y CP
Confiabilidad: Fuente del
proyecto; no acredita
ejecución de Latch.
Nota. Los archivos internos no tienen URL pública; sus nombres identifican las copias del proyecto. Las referencias
2026b/2026c/2026d/2026e/2026f corresponden respectivamente al Flujo, PRD, TRD, Esquema y Plan. El syllabus no se consultó. Los
tokens y escenarios de Latch son elaboración de diseño, no datos empíricos de las fuentes. Se conserva la fecha de consulta del Brief;
no se unifican fechas entre documentos.