

## 1
Documento de Flujo de la Aplicación de Latch
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
Latch organiza los recorridos de una aplicación de escritorio para administrar propiedades,
unidades, inquilinos, contratos, cargos, pagos, garantías, mantenimiento y reportes. Este
documento especifica cuarenta y nueve flujos con precondiciones, pasos, alternos, excepciones y
trazabilidad hacia los requisitos y pantallas de los documentos hermanos recibidos. Ocho
recorridos de extremo a extremo relacionan las acciones del Propietario y del Asistente con los
servicios, la consulta cambiaria y la persistencia. El diseño conserva Java 21, JavaFX y
PostgreSQL, con una transacción única para pago, aplicaciones, recibo, contador y auditoría. Los
flujos de concurrencia precisan cancelación por puntos seguros, coordinación entre clientes y
cierre sin interrumpir una escritura confirmada. La integración distingue la referencia comercial
informativa de la tasa autorizada para la fecha del pago. Las máquinas de estado definen guardas
y roles, mientras el catálogo de errores y los escenarios límite fijan recuperaciones observables.
La navegación mantiene una ventana principal y dos temas Aurora UI con preferencias locales y
modalidad sin efectos. La investigación utiliza documentación oficial y un antecedente científico
acotado a su contexto. Los datos de demostración son sintéticos y los criterios de prueba
describen aceptación futura; la revisión documental y de archivos no acredita una aplicación
implementada ni participación de propietarios externos.
Palabras clave: JavaFX, flujos de aplicación, PostgreSQL, concurrencia, arrendamiento,
Aurora UI, trazabilidad

## 3
Documento de Flujo de la Aplicación de Latch
## Introducción
Latch reunirá los registros necesarios para explicar la ocupación de una unidad y el saldo
de un contrato de alquiler. El documento fija la secuencia de acciones que ejecutan el
Propietario, el Asistente y los servicios internos desde el arranque hasta el cierre. El equipo lo
utilizará para programar transiciones y definir pruebas; el docente podrá revisar su
correspondencia con los resultados de aprendizaje aportados. El alcance comprende los doce
módulos, con operación local y una variante de varios clientes conectados a PostgreSQL en red
local.
El Brief define pantallas, mensajes y tokens visuales que este flujo conserva (Cuadra et
al., 2026a). El PRD fija requisitos y reglas de negocio, incluidos los contratos activos y la
separación de tasas informativas y autorizadas (Cuadra et al., 2026c). El TRD especifica
transacciones, hilos y adaptadores compatibles con estas rutas (Cuadra et al., 2026d). El
Esquema de base de datos y servicios establece nombres físicos, restricciones y contratos de
persistencia (Cuadra et al., 2026e). El Plan de Implementación fija calendario, RACI, revisión
cruzada y casos CP-001 a CP-100 (Cuadra et al., 2026f). Los cinco documentos hermanos
sustentan esta revisión.
Base Documental y Alcance de la Evidencia
Balzarini y Boyd (2021) analizaron estrategias de arrendadores mediante setenta y una
entrevistas en Filadelfia y describieron arreglos de pago frente a atrasos. La versión editorial
publicada en línea permite considerar ese antecedente para construir escenarios de pagos
parciales. Su ámbito no describe prácticas de Managua ni demuestra beneficios de Latch. La
pertinencia local permanece como hipótesis de diseño académico. El equipo no recibió registros

## 4
privados, entrevistas, observaciones ni un propietario participante; los dos roles funcionales y los
ejemplos financieros representan actores y datos sintéticos.
La investigación de esta edición abrió fuentes originales el 23 de octubre de 2026 y
contrastó versiones, contratos HTTP, bloqueos y presentación. El Apéndice D identifica cada
obra, su sección utilizada y el límite de la evidencia disponible. La consulta del artículo Aurora
se restringió al contenido público, sin atribuir lectura de material bajo suscripción. Las pruebas
de teclado, SQL, concurrencia y exportación corresponden a un plan de aceptación. La
producción y revisión de este documento solo permiten informar verificaciones documentales y
de los archivos entregados.
Supuestos y Decisiones de Coherencia
La fecha base de planificación es el 22 de octubre de 2026 y la entrega documental
interna se fija para el 26 de noviembre. Esa fecha de portada es un supuesto de trabajo, sin
confirmar el calendario institucional. El Plan sitúa la presentación y evaluación entre el 26 de
noviembre y el 1 de diciembre de 2026 (Cuadra et al., 2026f). Se adoptan sus seis períodos, 228
h programadas, 281,14 h de capacidad y 53,14 h de reserva. La capacidad propuesta de doce
horas semanales por integrante incluye revisión y correcciones. El Apéndice E remite a los
responsables, casos CP y tareas TAR del Plan sin afirmar horas ejecutadas.
La Tabla 1 presenta decisiones de Coherencia Adoptadas.
## Tabla 1
Decisiones de Coherencia Adoptadas
TemaDecisión y Naturaleza
## Identificadore
s
Conservar PAN-01 a PAN-40 y MSG-001 a MSG-089 del Brief; RF y RN-01
a RN-38 del PRD. FLU y ERR pertenecen a este documento. Nombres
físicos, roles de BD y migraciones corresponden al Esquema (Cuadra et al.,
## 2026e).

## 5
TemaDecisión y Naturaleza
Paleta AuroraEl contrato visual del proyecto sustituyó la paleta histórica por los valores del
brief: fondo Claro #F5F7FC y Oscuro #0F1424; superficie #FFFFFF y
#182136; acento #4F46E5 y #A5B4FC. Decisión de actualización visual.
OcupaciónVigente, Por vencer y Vencido pendiente de cierre reservan unidad. Se
conserva ampliación RN-01 del PRD.
## Permisos
ampliados
Propietario confirma liquidación de garantía, anulación de cargos y
restauración. Asistente prepara y consulta según PAN del brief; no se le
concede configuración administrativa.
ReciboUsar estados Registrado y Anulado, sincronizados con Pago. No introducir
Emitido como estado adicional. Conservación del catálogo compartido.
## Activación
futura
Sucesor futuro permanece Borrador; la activación requiere fecha de inicio
alcanzada y cierre explícito del anterior. Decisión complementaria que evita
inventar un estado contractual.
## Garantía
inicial cero
Registrar acuerdo por importe cero como Retenido sin movimientos; no
genera recibo ni caja. Si hay fondos, el cierre exige remanente cero. Decisión
complementaria. El Asistente puede confirmar el cierre únicamente cuando la
garantía inicial sea cero y no haya movimientos; en los demás cierres con
garantía/liquidación confirma el Propietario.
Reloj y banda
cambiaria
El instante financiero y la fecha operativa proceden de now() de la BD; más
de cinco minutos de diferencia absoluta con el reloj local bloquea nuevas
finanzas. Banda técnica comercial de 1 a 100 NIO/USD; fuera de banda no se
guarda referencia vigente y se audita M09
REFERENCIA_EN_CUARENTENA con valor y huella, sin tabla adicional.
Convenciones y Notación
Cada identificador FLU conserva una secuencia estable y representa una especificación
que puede invocarse desde varios recorridos. Las tablas separan entradas, pasos, caminos
alternos, excepciones y postcondiciones para evitar interpretar una falla como éxito parcial. La
Figura 1 reúne la única leyenda de notación utilizada en el documento. Las flechas expresan
orden o mensajes, y los carriles asignan responsabilidad a cada paso. El Apéndice A amplía
diagramas de coordinación, el Apéndice B reproduce una especificación desarrollada y el
Apéndice C fija el vocabulario compartido.
La Figura 1 muestra leyenda Común de Diagramas.

## 6
## Figura 1
Leyenda Común de Diagramas
La Tabla 2 presenta contrato de Lectura de los Flujos.
## Tabla 2
Contrato de Lectura de los Flujos
ElementoInterpretación
Actor y permisoAmbos significa Propietario y Asistente. Sistema ejecuta tarea bajo política
autorizada. P abrevia Propietario únicamente dentro de matrices.
## Pasos
numerados
Secuencia normal dentro de celda; números locales a cada FLU. Confirmar
significa commit conocido cuando la operación escribe.
## Alterno /
excepción
Alterno conserva intención válida con otra ruta; excepción interrumpe o
deriva a recuperación. Ambos definen resultado observable.
Referencias por
rango
RF-M01-12 a RF-M01-15 incluye los cuatro identificadores existentes. Un
rango RN se interpreta inclusivo.
Pantallas y
datos
PAN designa vista o diálogo del Brief. Los nombres físicos siguen el
Esquema: consecutivo_recibo, esquema_migracion, latch_runtime y
latch_owner; fila fija UUID 00000000-0000-0000-0000-000000000001.
Migraciones V001__modelo, V002__objetos, V003__catalogos y
V004__privilegios; scripts T001 y R001. Las entidades conceptuales no
añaden tablas (Cuadra et al., 2026e).
Estados gráficosÓvalos nombrados representan estados o inicio/fin según contexto. En
secuencias, línea discontinua expresa vida del participante; en tareas, borde
discontinuo expresa segundo plano.
La Tabla 3 presenta fLU-01 CRUD Genérico.

## 7
## Tabla 3
FLU-01 CRUD Genérico
Especificación del Flujo
Identidad: FLU-01. CRUD Genérico. Actor: Rol autorizado.
Entrada: Precondiciones: Sesión válida; registro accesible y versión conocida. Disparador:
Nuevo, Buscar, Guardar o Dar de baja.
Pasos: 1) Capturar campos o filtros y conservar una copia editable. 2) Validar en interfaz y
repetir reglas y permiso en el servicio. 3) Consultar con parámetros; para escritura abrir
transacción, verificar versión y persistir con auditoría exigible. 4) Confirmar, cerrar recursos y
actualizar selección y foco solo tras conocer el resultado.
Alternos y Excepciones: Alternos: Cancelar conserva el registro anterior; baja exige PAN-23 y
Propietario. Guardar antes de navegar continúa solo tras confirmación. Excepciones:
Validación conserva campos; conflicto de versión exige comparar y recargar; fallo previo a
commit revierte. Resultado incierto conserva UUID.
Salida: Postcondiciones: Alta, consulta, edición o baja lógica confirmada; sin borrado físico de
finanzas.
Datos y Pantallas: Lee: Entidad, versión y permisos. Escribe: Entidad y evento; ninguna
escritura al consultar. Pantallas: PAN-23, PAN-39.
Reglas y Ejecución: RN-11, RN-13, RN-14. RF-M02-01, RF-M03-09. Servicio de aplicación
fuera del hilo JavaFX; UI recibe DTO.
Mapa General de Navegación
La ventana principal conserva una barra lateral por módulos y una barra superior con
contexto y acciones pertinentes. Elegir un destino actualiza de forma conjunta el contenido, la
selección lateral, el título y el foco visible. Los formularios modificados abren PAN-39 antes de
cambiar de vista; Guardar continúa únicamente cuando el servicio confirma la escritura. El
selector Claro/Oscuro y la modalidad sin efectos actúan sobre la presentación actual. Ninguna de
esas acciones cambia filtros, datos capturados, UUID de pago, permisos o ubicación dentro de la
aplicación.
La Figura 2 muestra navegación en Ventana Única.

## 8
## Figura 2
Navegación en Ventana Única
La Tabla 4 presenta transiciones de Navegación.
## Tabla 4
Transiciones de Navegación
OrigenDestinoDisparadorCondición
## PAN-01PAN-34 /
## PAN-02
IngresarCredenciales válidas; cambio
obligatorio antes del panel
## PAN-02PAN-03 /
## PAN-05
## Propiedades /
## Unidades
Propietario: PAN-03 y PAN-05, todas
las acciones. Asistente: PAN-05,
consulta y edición de
condiciones/foto en PAN-21; sin
altas, bajas ni restricción En
mantenimiento. Resolver edición
actual.
## PAN-02PAN-06 /
## PAN-24
## Inquilinos /
## Historial
Ambos; expediente seleccionado para
historial
## PAN-02PAN-07 /
## PAN-08
Contratos / NuevoAmbos; Guardar Borrador sin activar
PAN-07PAN-25 / 26 / 27Activar / Renovar /
## Terminar
Estado y fechas válidos; garantía
confirmada por P al liquidar

## 9
OrigenDestinoDisparadorCondición
## PAN-02PAN-09 /
## PAN-20
## Cobranza / Generar
cargos
Ambos; período y contrato según
acción
## PAN-09PAN-10 /
## PAN-11
Registrar pago /
Ver recibo
Ambos; recibo solo después de
commit conocido
PAN-11PAN-29Anular reciboSolo Propietario; motivo y RN-31
## PAN-02PAN-12 /
## PAN-31
Garantías / LiquidarAmbos consultan; solo P confirma
liquidación
## PAN-02PAN-13 /
## PAN-30
## Mantenimiento /
## Solicitud
Ambos; cancelación con motivo por
ambos roles
## PAN-02PAN-14 /
## PAN-32
## Tasas / Autorizar
manual
Ambos consultan; solo P autoriza
## PAN-02PAN-15 /
## PAN-16
Reportes / GenerarAmbos; parámetros válidos y H5
PAN-02PAN-17 / 18 / 19Configuración /
## Usuarios /
## Auditoría
## Solo Propietario
## PAN-17PAN-36 /
## PAN-37
## Respaldar /
## Restaurar
Solo P; exclusión y mantenimiento
## Cualquier
edición
## PAN-39 /
destino
Navegar o cerrarGuardar confirmado, Descartar o
Continuar editando
Acceso o
ventana
Misma vistaClaro/Oscuro; Sin
efectos
Ambos; datos y foco conservados
## Ventana
principal
PAN-01 / salidaCerrar sesión / SalirResolver cambios y tareas; FLU-05 o
## FLU-48
Flujo de Arranque y Cierre
El inicializador carga preferencias, CSS y recursos locales antes de mostrar el acceso. La
ausencia de luces decorativas produce un fondo simplificado y permite continuar con superficies
legibles; la ausencia de conexión a PostgreSQL impide autenticar. El pool único tendrá seis
conexiones máximas, una mínima y tres segundos de espera por conexión, según el TRD
recibido. La SessionFactory reutiliza ese DataSource y valida el esquema existente. La
instalación no ejecuta migraciones implícitas al abrir una sesión ni crea conexiones de negocio
independientes para cada controlador.
La Tabla 5 presenta fLU-49 Arrancar y Clasificar Fallas Iniciales.

## 10
## Tabla 5
FLU-49 Arrancar y Clasificar Fallas Iniciales
Especificación del Flujo
Identidad: FLU-49. Arrancar y Clasificar Fallas Iniciales. Actor: Sistema.
Entrada: Precondiciones: Paquete instalado; configuración externa esperada. Disparador:
## Ejecutar Latch.
Pasos: 1) Leer preferencias y cargar CSS del brief antes de mostrar acceso; decoración fallida
usa fondo simplificado. 2) Validar configuración obligatoria; abrir pool único y comprobar
conexión con plazo acotado. 3) Construir SessionFactory con ese DataSource y validar versión
y estructura del esquema, sin migración implícita. 4) Registrar instalación, iniciar H2 con
indicador atómico local y bloqueo de fila por contrato, y H3 con concesión en control_fx,
consultar caché sin esperar Internet y habilitar PAN-01.
Alternos y Excepciones: Alternos: Sin llave HTTP inicia con consulta externa deshabilitada;
sin preferencia válida usa Claro; sin decoraciones mantiene acceso. Excepciones:
Configuración de BD ausente o BD caída bloquea autenticación y ofrece Reintentar o Salir;
esquema incompatible cierra recursos y exige paquete/migración compatible.
Salida: Postcondiciones: Recursos listos antes de acceso, o fallo clasificado sin pool huérfano.
Datos y Pantallas: Lee: Configuración, recursos, esquema, preferencias y caché. Escribe:
Registro técnico de arranque sin secretos; programación compartida. Pantallas: PAN-01,
## PAN-40.
Reglas y Ejecución: RN-22, RN-30, RN-35, RN-36, RN-38. RF-M01-13, RF-M09-04, RF-
M12-08. Inicializador fuera de JavaFX; H2/H3 solo tras esquema válido.
La Figura 3 muestra clasificación del Arranque y Recuperación.
## Figura 3
Clasificación del Arranque y Recuperación
Preferences permite almacenar elecciones de presentación de forma persistente mediante
un almacén dependiente de la plataforma (Oracle, s. f.-b). Latch usará un nodo local de
instalación para lastTheme y un subnodo por id opaco de usuario para su tema y modalidad. Un
valor ausente, ilegible o distinto de Claro y Oscuro restaura Claro. El cambio de presentación

## 11
conserva los controles existentes; las capas decorativas serán transparentes al ratón y ajenas al
foco, conforme a las propiedades de Node documentadas por OpenJFX (s. f.-b).
La salida coordina recursos mediante FLU-48, que también rige las tareas de cierre de
sesión cuando afectan una operación activa. El coordinador detiene nuevas admisiones y
disparos programados antes de pedir cancelación. H1 y H2 concluyen la transacción actual; H3
libera su solicitud y H5 limpia temporales cuando finaliza el exportador. H4 alcanza commit o
rollback sin interrupción forzada. Solo entonces cierran HttpClient, sesiones, SessionFactory y
pool; superar diez segundos mantiene la aplicación visible con el detalle de la tarea pendiente en
## PAN-40.
Flujo de Autenticación y Autorización
El acceso verifica cuentas activas mediante consultas JDBC parametrizadas y contraseñas
Argon2id con sal por cuenta, decisión del PRD recibido. Los mensajes de rechazo conservan el
mismo texto para credenciales inexistentes e incorrectas. El quinto fallo consecutivo bloquea
durante quince minutos y un ingreso correcto reinicia el contador. Otro Propietario activo puede
restablecer la cuenta sin consultar secretos anteriores. El servicio revalida el rol en cada comando
de escritura para impedir que un diálogo abierto conserve permisos que otra sesión ya retiró
durante la operación.
La Figura 4 muestra acceso y Aplicación de Permisos.

## 12
## Figura 4
Acceso y Aplicación de Permisos
La Tabla 6 presenta pantalla Rol y Acción Permitida.
## Tabla 6
Pantalla Rol y Acción Permitida
PantallaPropietarioAsistente
PAN-01 / PAN-34Acceder, cambiar tema y
contraseña propia
Las mismas acciones
personales
PAN-02Indicadores, navegación y
preferencias
Indicadores, destinos
autorizados y preferencias
PAN-03 / 04 / 05 / 21 / 23Crear, editar, consultar y baja
lógica
PAN-05: consultar unidades;
PAN-21: editar
condiciones/foto. Sin alta de
propiedad o unidad, bajas ni
restricción En mantenimiento.
PAN-06 / 22 / 24Crear, editar, adjuntar y
consultar historial
Crear, editar, adjuntar y
consultar; sin baja
PAN-07 / 08 / 25 / 26 / 27Borrador, activar, renovar y
preparar término
Las mismas acciones
contractuales; liquidación
exige P
PAN-09 / 20 / 28Consultar, generar, crear y
anular cargos
Consultar, generar y crear; sin
anular
PAN-10 / 11 / 29Cobrar, reimprimir y anular
con motivo
Cobrar y reimprimir; sin
## PAN-29
PAN-12 / PAN-31Consultar, aplicar y devolver
garantía
Consultar; preparar datos fuera
del diálogo de confirmación
PAN-13 / PAN-30Registrar, atender, resolver y
cancelar
Registrar, atender, resolver y
cancelar
PAN-14 / PAN-32Consultar y autorizar tasa por
fecha
Consultar referencias e
historial; sin autorización
PAN-15 / PAN-16Generar, previsualizar y
exportar
Las mismas acciones de
reportes

## 13
PantallaPropietarioAsistente
## PAN-17 / 18 / 19 / 33 / 35 /
## 36 / 37 / 38
Parámetros, cuentas, auditoría
y mantenimiento
Sin acceso
PAN-39 / PAN-40Resolver cambios y coordinar
tareas propias
Las mismas acciones de
sesión, sin más permisos
Flujos por Módulo
M01 Seguridad y Usuarios
Las operaciones de seguridad distinguen la identidad de la cuenta, los permisos vigentes
y las preferencias de presentación. La administración de usuarios conserva un Propietario activo
y registra cambios sin incluir contraseñas en la bitácora. El cambio de tema está disponible desde
el acceso y la ventana principal para ambos roles, con almacenamiento local separado por id
opaco. La inactividad protege la vista y exige recuperar la sesión antes de continuar un pago
pendiente. Los flujos conservan el mismo UUID cuando la respuesta financiera todavía necesita
confirmación.
La Figura 5 muestra recorridos Principales de M01.
## Figura 5
Recorridos Principales de M01
La Tabla 7 presenta fLU-02 Acceder y Gestionar el Bloqueo.
## Tabla 7
FLU-02 Acceder y Gestionar el Bloqueo
Especificación del Flujo
Identidad: FLU-02. Acceder y Gestionar el Bloqueo. Actor: Propietario o Asistente.

## 14
Especificación del Flujo
Entrada: Precondiciones: Configuración y esquema válidos; acceso visible. Disparador:
## Ingresar.
Pasos: 1) Recibir usuario y contraseña sin registrarlos en logs. 2) Consultar cuenta con JDBC y
verificar Argon2id fuera del hilo gráfico. 3) Bajo bloqueo de cuenta, incrementar fallos o
reiniciar contador; al quinto fallo fijar bloqueo de quince minutos. 4) Auditar sin secretos;
actor NULL solo en M01 para ACCESO_FALLIDO o ACCESO_BLOQUEADO, con
SHA-256 truncado del nombre normalizado y nunca el texto ingresado. Con credenciales
válidas cargar rol, exigir cambio temporal si corresponde y restaurar preferencias por id opaco
antes del panel.
Alternos y Excepciones: Alternos: Cuenta bloqueada espera vencimiento o desbloqueo por
otro Propietario; cuenta desconocida usa verificación de costo comparable. Excepciones:
MSG-001 o MSG-002 mantiene acceso; caída de BD muestra MSG-006 y no crea sesión.
Salida: Postcondiciones: Sesión del rol vigente y evento de acceso; contraseña temporal
conduce a PAN-34.
Datos y Pantallas: Lee: Cuenta, hash, sal, bloqueo y preferencias. Escribe: Fallos, bloqueo y
bitácora de seguridad. El último acceso se deriva de bitacora; no es una columna de usuario.
Actor NULL solo en M01 / ACCESO_FALLIDO o ACCESO_BLOQUEADO; detalle con
SHA-256 truncado del nombre normalizado. Pantallas: PAN-01, PAN-02, PAN-34.
Reglas y Ejecución: RN-13, RN-15, RN-36. RF-M01-01 a RF-M01-05, RF-M01-14. Servicio
de aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 8 presenta fLU-03 Administrar Cuentas y Roles.
## Tabla 8
FLU-03 Administrar Cuentas y Roles
Especificación del Flujo
Identidad: FLU-03. Administrar Cuentas y Roles. Actor: Propietario.
Entrada: Precondiciones: Sesión administrativa; otra cuenta seleccionada para cambios.
Disparador: Nuevo usuario, Cambiar rol, Desactivar o Desbloquear.
Pasos: 1) Abrir PAN-33 con nombre único y uno de los dos roles. 2) Aplicar FLU-01; generar
hash salado para contraseña temporal. 3) Bloquear conjunto de cuentas administrativas y
conservar al menos un Propietario activo; impedir desactivar la propia sesión. 4) Confirmar
cambio, invalidar permisos de sesiones afectadas antes de su siguiente comando y mostrar
## MSG-055.
Alternos y Excepciones: Alternos: Desbloquear limpia contador y vencimiento; cancelar no
modifica la cuenta. Rechazar desactivación del último Propietario. Excepciones: Duplicado
usa MSG-053; último administrador MSG-054; permiso obsoleto MSG-062.
Salida: Postcondiciones: Cuenta creada o actualizada sin perder actor histórico.
Datos y Pantallas: Lee: Cuentas activas, roles y versión. Escribe: Cuenta, hash y evento de
seguridad. Pantallas: PAN-18, PAN-33.
Reglas y Ejecución: RN-13, RN-15, RN-16. RF-M01-05, RF-M01-06, RF-M01-09, RF-M01-
- Servicio de aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 9 presenta fLU-04 Cambiar o Restablecer Contraseña.

## 15
## Tabla 9
FLU-04 Cambiar o Restablecer Contraseña
Especificación del Flujo
Identidad: FLU-04. Cambiar o Restablecer Contraseña. Actor: Ambos para la propia;
Propietario para otra.
Entrada: Precondiciones: Cuenta activa; autorización de restablecimiento si es otra cuenta.
Disparador: Cambiar contraseña o Restablecer.
Pasos: 1) Abrir PAN-34 o PAN-35 sin mostrar hash ni sal. 2) Para cambio propio verificar
contraseña actual; exigir nueva de 12 a 128 caracteres y confirmación coincidente. 3) Calcular
Argon2id con sal nueva; en restablecimiento marcar cambio obligatorio. 4) Confirmar por
JDBC, auditar sin secreto e invalidar sesiones previas de la cuenta.
Alternos y Excepciones: Alternos: Cancelar conserva hash; acceso con temporal solo habilita
cambio y cierre de sesión. Excepciones: Confirmación desigual conserva las entradas no
secretas; fallo de persistencia revierte y exige reingresar contraseñas.
Salida: Postcondiciones: Hash nuevo confirmado; usuario vuelve al acceso y autentica con su
nueva contraseña.
Datos y Pantallas: Lee: Cuenta, hash actual y versión. Escribe: Hash, sal, cambio obligatorio
cuando corresponda y evento sin secretos. La marca temporal del cambio de clave se deriva de
bitacora; no es una columna de usuario. Pantallas: PAN-34, PAN-35, PAN-01.
Reglas y Ejecución: RN-13, RN-15. RF-M01-03, RF-M01-11. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 10 presenta fLU-05 Cerrar Sesión y Atender Inactividad.
## Tabla 10
FLU-05 Cerrar Sesión y Atender Inactividad
Especificación del Flujo
Identidad: FLU-05. Cerrar Sesión y Atender Inactividad. Actor: Propietario o Asistente.
Entrada: Precondiciones: Sesión abierta. Disparador: Cerrar sesión o quince minutos sin
interacción.
Pasos: 1) Avisar sesenta segundos antes del vencimiento por inactividad. 2) Resolver cambios
con PAN-39 y bloquear nuevos comandos mientras termina una transacción financiera activa.
3) Cancelar tareas ligadas a la sesión en puntos seguros; limpiar DTO con datos privados y
credenciales. 4) Mostrar PAN-01 con la última preferencia local y conservar solo las tareas
programadas de instalación autorizadas.
Alternos y Excepciones: Alternos: Continuar editando detiene la salida voluntaria; en
inactividad bloquea la vista y exige reautenticación antes de recuperar el borrador en memoria.
Excepciones: Commit incierto exige consultar UUID tras reautenticar; no ofrecer reenviar
como pago nuevo.
Salida: Postcondiciones: Sesión cerrada o vista bloqueada; ningún dato privado visible en
acceso.
Datos y Pantallas: Lee: Última actividad, tareas y cambios pendientes. Escribe: Evento de
cierre; borrador solo en memoria hasta salida segura. Pantallas: PAN-01, PAN-39, PAN-40.

## 16
Especificación del Flujo
Reglas y Ejecución: RN-15, RN-21, RN-30. RF-M01-07, RF-M01-08. Servicio de aplicación
fuera del hilo JavaFX; UI recibe DTO.
La Tabla 11 presenta fLU-06 Elegir Tema Antes del Acceso.
## Tabla 11
FLU-06 Elegir Tema Antes del Acceso
Especificación del Flujo
Identidad: FLU-06. Elegir Tema Antes del Acceso. Actor: Persona en acceso; ambos roles.
Entrada: Precondiciones: Recursos locales cargados; todavía sin sesión. Disparador: Claro,
Oscuro o Sin efectos decorativos.
Pasos: 1) Leer lastTheme y modalidad local antes de mostrar PAN-01. 2) Aceptar solo Claro u
Oscuro; ausencia, error de lectura o valor inválido usa Claro y efectos normales si falla la
lectura de simple, según el brief. 3) Aplicar CSS y decoración sin reconstruir controles;
conservar texto y foco. 4) Guardar elección local; después de autenticar ceder a la preferencia
individual por UUID.
Alternos y Excepciones: Alternos: Sin efectos conserva la paleta y elimina luces; recurso
decorativo ausente usa fondo uniforme del tema. Excepciones: Fallo de escritura conserva
elección de sesión y muestra MSG-003 una vez; no impide autenticar.
Salida: Postcondiciones: Acceso legible con dos temas y modalidad independiente.
Datos y Pantallas: Lee: Preferencias locales y recursos de presentación. Escribe: lastTheme y
simple en Preferences; ninguna tabla del dominio. Pantallas: PAN-01.
Reglas y Ejecución: RN-35, RN-36, RN-37, RN-38. RF-M01-12 a RF-M01-15. Hilo JavaFX
para CSS y foco; sin hilo decorativo.
M02 Propiedades y Unidades
La propiedad agrupa unidades y conserva sus datos de identificación sin alterar copias
históricas de recibos. Cada unidad mantiene un código único dentro de su propiedad, un tipo del
catálogo y un canon de referencia que no cambia contratos firmados. La ocupación se deriva de
v_ocupacion y contratos activos, incluidos los Vencido pendientes de cierre; unidad.estado solo
persiste Disponible, En mantenimiento e Inactiva. Una solicitud de mantenimiento sobre una
unidad Ocupada conserva esa ocupación; la restricción En mantenimiento corresponde a una
unidad libre. Las bajas lógicas verifican dependencias y quedan reservadas al Propietario antes
de confirmar cualquier cambio.
La Figura 6 muestra recorridos Principales de M02.

## 17
## Figura 6
Recorridos Principales de M02
La Tabla 12 presenta fLU-07 Crear o Editar Propiedad.
## Tabla 12
FLU-07 Crear o Editar Propiedad
Especificación del Flujo
Identidad: FLU-07. Crear o Editar Propiedad. Actor: Propietario.
Entrada: Precondiciones: Propiedad nueva o activa accesible. Disparador: Nueva propiedad o
## Editar.
Pasos: 1) Abrir PAN-04 y capturar nombre y dirección. 2) Aplicar FLU-01 con validación de
obligatorios. 3) Confirmar por Hibernate y conservar copias históricas de recibos. 4) Mostrar
MSG-009 y seleccionar la propiedad en PAN-03.
Alternos y Excepciones: Alternos: Cancelar descarta solo la edición confirmada en PAN-39;
consultar conserva filtros. Excepciones: Nombre o dirección vacíos usa MSG-004; conflicto
de versión requiere comparar cambios.
Salida: Postcondiciones: Propiedad guardada, disponible para unidades.
Datos y Pantallas: Lee: Propiedad y versión. Escribe: Propiedad; auditoría de cambio.
Pantallas: PAN-03, PAN-04.
Reglas y Ejecución: RN-11, RN-14. RF-M02-01, RF-M02-09. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 13 presenta fLU-08 Crear o Editar Unidad y Fotografía.
## Tabla 13
FLU-08 Crear o Editar Unidad y Fotografía
Especificación del Flujo
Identidad: FLU-08. Crear o Editar Unidad y Fotografía. Actor: Propietario para alta y edición;
Asistente solo para edición de condiciones y fotografía.
Entrada: Precondiciones: Propiedad activa; código disponible para alta, reservada al
Propietario. Asistente solo edita condiciones/foto de una unidad existente. Disparador: Nueva
unidad o Editar unidad.

## 18
Especificación del Flujo
Pasos: 1) Elegir Casa, Apartamento, Cuarto o Local comercial; capturar código y canon de
referencia positivo en NIO o USD. 2) Revalidar rol: Asistente solo edita condiciones/foto, sin
alta, baja ni restricción En mantenimiento. Aplicar FLU-01; validar unicidad por propiedad en
servicio y BD. 3) Si hay foto PNG o JPEG de hasta cinco MiB, validar firma, copiar con
nombre aleatorio y calcular huella. 4) Confirmar unidad y referencia de archivo; limpiar copia
temporal si revierte.
Alternos y Excepciones: Alternos: Foto ausente conserva funcionamiento; cambiar canon de
referencia no modifica contratos existentes. Excepciones: Duplicado muestra MSG-010;
archivo inválido se rechaza sin perder formulario.
Salida: Postcondiciones: Unidad y foto opcional vinculadas; contratos históricos intactos.
Datos y Pantallas: Lee: Propiedad, unidad, código y versión. Escribe: Unidad, metadatos y
archivo controlado. Pantallas: PAN-05, PAN-21.
Reglas y Ejecución: RN-03, RN-16, RN-25. RF-M02-02 a RF-M02-05. Servicio de aplicación
fuera del hilo JavaFX; UI recibe DTO.
La Tabla 14 presenta fLU-09 Consultar Disponibilidad de Unidades.
## Tabla 14
FLU-09 Consultar Disponibilidad de Unidades
Especificación del Flujo
Identidad: FLU-09. Consultar Disponibilidad de Unidades. Actor: Propietario o Asistente.
Entrada: Precondiciones: Catálogo accesible. Disparador: Buscar o cambiar filtros.
Pasos: 1) Seleccionar propiedad, tipo y estado. 2) Consultar página con filtros parametrizados
y ocupación derivada de contratos activos. 3) Mostrar código, canon de referencia, moneda y
estado exacto. 4) Abrir detalle conservando filtros y selección al regresar.
Alternos y Excepciones: Alternos: Sin coincidencias muestra MSG-012; cero unidades no
altera el catálogo. Excepciones: BD no disponible usa MSG-006 y conserva última página con
indicación de antigüedad.
Salida: Postcondiciones: Vista filtrada, sin escrituras.
Datos y Pantallas: Lee: Unidades y contratos Vigente, Por vencer o Vencido sin cierre.
Escribe: Ninguno. Pantallas: PAN-05.
Reglas y Ejecución: RN-01, RN-16. RF-M02-06, RF-M02-07. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 15 presenta fLU-10 Restringir Disponibilidad o Dar de Baja.
## Tabla 15
FLU-10 Restringir Disponibilidad o Dar de Baja
Especificación del Flujo
Identidad: FLU-10. Restringir Disponibilidad o Dar de Baja. Actor: Propietario.
Entrada: Precondiciones: Unidad sin contrato activo o propiedad sin unidades activas.
Disparador: En mantenimiento, Disponible o Dar de baja.

## 19
Especificación del Flujo
Pasos: 1) Consultar dependencias y abrir PAN-23 si corresponde baja. 2) Bloquear unidad y
revalidar ausencia de contrato activo. 3) Cambiar Disponible a En mantenimiento, restablecer
Disponible o marcar Inactiva; propiedad solo si sus unidades están Inactivas. 4) Confirmar con
auditoría y actualizar listas.
Alternos y Excepciones: Alternos: Cancelar conserva estado; reactivar una baja requiere
revisión administrativa y solo pasa a Disponible si no hay conflicto. Excepciones: Contrato
activo usa MSG-011; propiedad con unidades activas MSG-008.
Salida: Postcondiciones: Disponibilidad coherente; historia conservada por baja lógica.
Datos y Pantallas: Lee: Unidad, propiedad, contratos y versión. Escribe: Estado o baja lógica;
evento. Pantallas: PAN-03, PAN-05, PAN-23.
Reglas y Ejecución: RN-01, RN-11, RN-13. RF-M02-08, RF-M02-10, RF-M02-11. Servicio
de aplicación fuera del hilo JavaFX; UI recibe DTO.
## M03 Inquilinos
El expediente identifica al titular contractual mediante nombre y un medio de contacto,
con identificación y documentos opcionales. La coincidencia de una identificación advierte sobre
otro registro y permite abrirlo antes de crear uno nuevo. Los adjuntos pasan por validación de
firma, tamaño y copia controlada para evitar vínculos a archivos que desaparezcan fuera de la
aplicación. El historial consulta contratos y pagos relacionados sin duplicar sus saldos. El
Asistente puede corregir contacto y consultar documentos, mientras la baja lógica exige
Propietario y ausencia de contratos activos.
La Figura 7 muestra recorridos Principales de M03.
## Figura 7
Recorridos Principales de M03
La Tabla 16 presenta fLU-11 Registrar o Actualizar Inquilino.

## 20
## Tabla 16
FLU-11 Registrar o Actualizar Inquilino
Especificación del Flujo
Identidad: FLU-11. Registrar o Actualizar Inquilino. Actor: Propietario o Asistente.
Entrada: Precondiciones: Sesión con permiso de expediente. Disparador: Nuevo inquilino o
Editar contacto.
Pasos: 1) Capturar nombre y al menos un medio de contacto. 2) Validar correo si existe y
advertir coincidencia de identificación opcional. 3) Aplicar FLU-01 y guardar contacto de
emergencia opcional separado. 4) Mostrar MSG-016 sin cambiar datos históricos de recibos.
Alternos y Excepciones: Alternos: Usar coincidencia existente evita alta duplicada; sin
identificación ni emergencia se admiten datos mínimos. Excepciones: Datos mínimos usa
MSG-013; correo inválido MSG-014; fallo revierte edición.
Salida: Postcondiciones: Expediente guardado con datos mínimos y actor.
Datos y Pantallas: Lee: Expedientes coincidentes y versión. Escribe: Inquilino, contacto
opcional y evento. Pantallas: PAN-06, PAN-22.
Reglas y Ejecución: RN-14, RN-18. RF-M03-01 a RF-M03-04, RF-M03-09. Servicio de
aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 17 presenta fLU-12 Incorporar Adjunto al Expediente.
## Tabla 17
FLU-12 Incorporar Adjunto al Expediente
Especificación del Flujo
Identidad: FLU-12. Incorporar Adjunto al Expediente. Actor: Propietario o Asistente.
Entrada: Precondiciones: Inquilino existente; finalidad del adjunto documentada. Disparador:
Adjuntar documento.
Pasos: 1) Abrir FileChooser para PDF, PNG o JPEG. 2) Validar firma y tamaño hasta cinco
MiB; excluir ejecutables aunque cambie extensión. 3) Copiar a temporal controlado, calcular
huella y vincular nombre interno a expediente. 4) Confirmar metadatos y publicar copia;
eliminar huérfanos ante reversión.
Alternos y Excepciones: Alternos: Cancelar FileChooser no modifica expediente; sin adjuntos
el contrato sigue disponible. Excepciones: MSG-063 explica tipo o tamaño; falla de disco
conserva referencia anterior y permite elegir otro archivo.
Salida: Postcondiciones: Adjunto accesible por la aplicación con nombre y huella.
Datos y Pantallas: Lee: Inquilino y archivo elegido. Escribe: Metadatos, huella y copia local
controlada. Pantallas: PAN-22.
Reglas y Ejecución: RN-18, RN-25. RF-M03-05. Servicio de aplicación fuera del hilo JavaFX;
UI recibe DTO.
La Tabla 18 presenta fLU-13 Buscar Expediente y Revisar Historial.

## 21
## Tabla 18
FLU-13 Buscar Expediente y Revisar Historial
Especificación del Flujo
Identidad: FLU-13. Buscar Expediente y Revisar Historial. Actor: Propietario o Asistente.
Entrada: Precondiciones: Sesión válida. Disparador: Buscar por nombre o identificación;
## Historial.
Pasos: 1) Consultar coincidencias paginadas sin exponer campos opcionales en la lista. 2)
Seleccionar expediente y abrir PAN-24. 3) Leer contratos y pagos con enlaces y estados,
incluidos anulados. 4) Abrir detalle autorizado y volver al mismo criterio de búsqueda.
Alternos y Excepciones: Alternos: MSG-015 indica búsqueda vacía; un inquilino nuevo tiene
historial de cero movimientos. Excepciones: Falla de consulta usa MSG-006; no mostrar
historial de la selección anterior como si fuera el actual.
Salida: Postcondiciones: Historial consultado sin duplicar saldos.
Datos y Pantallas: Lee: Inquilino, contratos, pagos y recibos. Escribe: Ninguno. Pantallas:
## PAN-06, PAN-24, PAN-07, PAN-11.
Reglas y Ejecución: RN-08, RN-18. RF-M03-06, RF-M03-07. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 19 presenta fLU-14 Dar de Baja Lógica al Inquilino.
## Tabla 19
FLU-14 Dar de Baja Lógica al Inquilino
Especificación del Flujo
Identidad: FLU-14. Dar de Baja Lógica al Inquilino. Actor: Propietario.
Entrada: Precondiciones: Expediente sin contratos activos. Disparador: Dar de baja.
Pasos: 1) Mostrar PAN-23 con advertencia MSG-066. 2) Bloquear expediente y comprobar
contratos Vigente, Por vencer o Vencido pendiente de cierre. 3) Aplicar FLU-01 sin borrar
referencias financieras ni adjuntos históricos. 4) Excluir de nuevas selecciones y mantener
consulta de historial.
Alternos y Excepciones: Alternos: Cancelar conserva actividad; contrato activo exige cerrarlo
antes de solicitar baja. Excepciones: Cambio concurrente obliga a recargar; permiso de
Asistente produce MSG-062.
Salida: Postcondiciones: Inquilino inactivo con historia íntegra.
Datos y Pantallas: Lee: Inquilino, contratos y versión. Escribe: Baja lógica y evento. Pantallas:
## PAN-06, PAN-23.
Reglas y Ejecución: RN-11, RN-13. RF-M03-08. Servicio de aplicación fuera del hilo JavaFX;
UI recibe DTO.
## M04 Contratos
El contrato concentra condiciones, fechas y moneda que originan las obligaciones
mensuales. Guardar Borrador mantiene una propuesta editable sin reservar la unidad ni crear una

## 22
garantía financiera. La activación revalida la ocupación bajo bloqueo y registra el depósito
dentro de la misma transacción. Renovar prepara un sucesor enlazado y conserva los importes
confirmados del contrato anterior. La terminación requiere liquidación autorizada de la garantía
y mantiene cualquier deuda residual. H2 actualiza Por vencer y Vencido según la fecha
operativa, sin liberar la unidad antes del cierre explícito.
La Figura 8 muestra recorridos Principales de M04.
## Figura 8
Recorridos Principales de M04
La Tabla 20 presenta fLU-15 Preparar o Editar Borrador de Contrato.
## Tabla 20
FLU-15 Preparar o Editar Borrador de Contrato
Especificación del Flujo
Identidad: FLU-15. Preparar o Editar Borrador de Contrato. Actor: Propietario o Asistente.
Entrada: Precondiciones: Unidad e inquilino activos. Disparador: Nuevo contrato o Editar
## Borrador.
Pasos: 1) Capturar fechas, canon, moneda, día de pago y garantía; elegir unidad e inquilino. 2)
Exigir fin igual o posterior a inicio.plusMonths(1), canon positivo y día uno a treinta y uno. 3)
Aceptar PDF opcional según FLU-12 con contrato como destino. 4) Guardar Borrador con
FLU-01 y mostrar MSG-023.
Alternos y Excepciones: Alternos: Puede existir otro Borrador de la unidad porque aún no
reserva ocupación; activar revalida el intervalo. Excepciones: MSG-019, MSG-020 o
MSG-021 conserva entradas; PDF inválido no se publica.
Salida: Postcondiciones: Borrador editable sin cargos ni depósito financiero confirmado.
Datos y Pantallas: Lee: Unidad, inquilino y versión de Borrador. Escribe: Contrato Borrador y
adjunto opcional. Pantallas: PAN-07, PAN-08.
Reglas y Ejecución: RN-02, RN-03, RN-17, RN-19, RN-25. RF-M04-01 a RF-M04-04, RF-
M04-07, RF-M04-13. Servicio de aplicación fuera del hilo JavaFX; UI recibe DTO.

## 23
La Tabla 21 presenta fLU-16 Activar Contrato y Registrar Garantía.
## Tabla 21
FLU-16 Activar Contrato y Registrar Garantía
Especificación del Flujo
Identidad: FLU-16. Activar Contrato y Registrar Garantía. Actor: Propietario o Asistente.
Entrada: Precondiciones: Borrador completo; inicio no posterior a fecha operativa; garantía
pactada registrada. Disparador: Activar contrato.
Pasos: 1) Abrir PAN-25 con resumen de condiciones y depósito. 2) Bloquear unidad y
contratos; revalidar intervalos y disponibilidad. 3) En una transacción cambiar contrato a
Vigente, registrar depósito Retenido y ocupación derivada. 4) Auditar, confirmar y refrescar
PAN-07 y PAN-12; H2 reclasifica Por vencer si cumple umbral.
Alternos y Excepciones: Alternos: Inicio futuro conserva Borrador hasta su fecha; garantía
cero conserva registro de importe cero sin movimiento de caja. Excepciones: Conflicto usa
MSG-022 y revierte también depósito; fallo de BD no deja activación parcial.
Salida: Postcondiciones: Contrato activo y depósito vinculados; unidad Ocupada.
Datos y Pantallas: Lee: Borrador, unidad, contratos y condiciones. Escribe: Contrato, depósito
y evento en una transacción. Pantallas: PAN-08, PAN-25, PAN-12.
Reglas y Ejecución: RN-01, RN-10, RN-14, RN-19. RF-M04-05, RF-M04-06, RF-M07-01.
Servicio de aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 22 presenta fLU-17 Renovar Mediante Contrato Sucesor.
## Tabla 22
FLU-17 Renovar Mediante Contrato Sucesor
Especificación del Flujo
Identidad: FLU-17. Renovar Mediante Contrato Sucesor. Actor: Propietario o Asistente;
Propietario liquida garantía.
Entrada: Precondiciones: Contrato Vigente o Por vencer; sucesor sin solapamiento.
Disparador: Renovar. Renovar crea únicamente el sucesor Borrador desde Vigente o Por
vencer. El cierre atómico posterior cuenta como Terminar y admite anterior Vigente, Por
vencer o Vencido.
Pasos: 1) Abrir PAN-26 con nuevas fechas y canon sin editar rentas confirmadas. 2) Guardar
sucesor Borrador enlazado; mantener contrato actual y su garantía. 3) Al cierre pactado, que
cuenta como Terminar desde Vigente, Por vencer o Vencido, Propietario liquida la garantía
anterior mediante FLU-30 y FLU-31; conservar deuda. 4) Bloquear unidad y ambos contratos,
terminar anterior y activar sucesor en fecha admisible con nueva garantía. Si la garantía inicial
es cero y no existen movimientos, el Asistente puede confirmar el cierre; cualquier cierre con
garantía o liquidación exige confirmación del Propietario.

## 24
Especificación del Flujo
Alternos y Excepciones: Alternos: Si aún no llega la fecha, sucesor permanece Borrador; el
usuario vuelve para activar. No se transfiere garantía sin movimientos explícitos. Excepciones:
Solapamiento usa MSG-022; liquidación incompleta impide cierre; reversión conserva
contrato anterior. Liquidación incompleta: ERR-062 / MSG-085; conservar bloqueo y
propuesta hasta completar decisiones, movimientos y remanente.
Salida: Postcondiciones: Sucesor enlazado sin reescribir historia; una ocupación activa.
Datos y Pantallas: Lee: Contrato origen, sucesor, cargos y garantía. Escribe: Borrador sucesor;
luego cierre y activación atómicos. Pantallas: PAN-07, PAN-26, PAN-31, PAN-25.
Reglas y Ejecución: RN-01, RN-10, RN-19, RN-33. RF-M04-08. Servicio de aplicación fuera
del hilo JavaFX; UI recibe DTO.
La Tabla 23 presenta fLU-18 Terminar Contrato con Liquidación.
## Tabla 23
FLU-18 Terminar Contrato con Liquidación
Especificación del Flujo
Identidad: FLU-18. Terminar Contrato con Liquidación. Actor: Ambos preparan; Propietario
confirma liquidación. Excepción: garantía inicial cero sin movimientos permite confirmar al
## Asistente.
Entrada: Precondiciones: Contrato activo; fecha de término y motivo definidos. Disparador:
Terminar contrato.
Pasos: 1) Abrir PAN-27 y calcular deuda y depósito al corte sin prorrateo automático. 2)
Proponer aplicaciones y devolución en PAN-31 con concepto por deducción. 3) Propietario
confirma paquete transaccional de liquidación y terminación; bloquear contrato, cargos y
depósito. 4) Marcar Terminado, liberar unidad y conservar deuda pendiente exigible e historia.
El paquete usa UUID de liquidación (liquidacion_id) y liquidacion_hash de 64 caracteres
hexadecimales minúsculos compartidos por todos sus movimientos, con orden por movimiento
y UNIQUE(liquidacion_id, orden). Los reintentos consultan por liquidacion_id. Si la garantía
inicial es cero y no hay movimientos, el Asistente puede confirmar el cierre sin liquidación de
fondos.
Alternos y Excepciones: Alternos: Asistente prepara y conserva propuesta en memoria hasta
intervención del Propietario, salvo garantía inicial cero sin movimientos, que puede cerrar;
deuda residual no desaparece al terminar. Excepciones: Conceptos vacíos usa MSG-034;
exceso MSG-035; fallo revierte cierre y liquidación juntos. Liquidación pendiente: ERR-062 /
MSG-085, con cierre bloqueado; se conserva la excepción de garantía inicial cero sin
movimientos (DC-08).
Salida: Postcondiciones: Contrato Terminado; garantía agotada y movimientos conciliados.
Datos y Pantallas: Lee: Contrato, cargos, depósito y unidad. Escribe: Terminación,
aplicaciones, devolución y bitácora. Pantallas: PAN-27, PAN-31.
Reglas y Ejecución: RN-10, RN-13, RN-19, RN-26, RN-34. RF-M04-09, RF-M07-03 a RF-
M07-06. Servicio de aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 24 presenta fLU-19 Cancelar Borrador o Consultar Vencimientos.

## 25
## Tabla 24
FLU-19 Cancelar Borrador o Consultar Vencimientos
Especificación del Flujo
Identidad: FLU-19. Cancelar Borrador o Consultar Vencimientos. Actor: Propietario o
## Asistente; Sistema H2.
Entrada: Precondiciones: Borrador para cancelar; contratos accesibles para consulta.
Disparador: Cancelar Borrador o filtrar contratos.
Pasos: 1) Para cancelación abrir PAN-27, capturar motivo y comprobar ausencia de
movimientos. 2) Confirmar Borrador a Cancelado con auditoría. 3) Para consulta filtrar
unidad, inquilino o estado y recuperar página. 4) H2 marca Por vencer entre cero y treinta días
al fin, y Vencido desde el día posterior; mantiene la unidad reservada.
Alternos y Excepciones: Alternos: Cancelado es terminal; contratos activos se terminan, no se
cancelan. Excepciones: Estado cambiado rechaza cancelación y conserva motivo; H2 fallido
deja corte previo visible.
Salida: Postcondiciones: Borrador cancelado o lista actualizada; fechas determinan estados
activos.
Datos y Pantallas: Lee: Contratos, fecha operativa y política. Escribe: Estado Cancelado o
reclasificaciones de H2. Pantallas: PAN-07, PAN-27.
Reglas y Ejecución: RN-12, RN-19, RN-34. RF-M04-10, RF-M04-11, RF-M04-12. H2 para
revisión; consultas y cancelación en servicio de aplicación.
M05 Cargos y Cobranza
La cobranza parte de una renta única por contrato y período de facturación, aun cuando el
usuario repite una generación o cancela el lote. H1 confirma por contrato y aplica el crédito
disponible al crear el siguiente cargo. La mora depende de una política versionada y del principal
pendiente después de los días de gracia. H2 excluye Mora de la base y comparte el bloqueo
contractual con pagos y generación mensual. El estado del cargo deriva del saldo y vencimiento;
la anulación conserva la clave de renta y exige ausencia de aplicaciones activas.
La Figura 9 muestra recorridos Principales de M05.

## 26
## Figura 9
Recorridos Principales de M05
La Tabla 25 presenta fLU-20 Previsualizar y Generar Renta Mensual.
## Tabla 25
FLU-20 Previsualizar y Generar Renta Mensual
Especificación del Flujo
Identidad: FLU-20. Previsualizar y Generar Renta Mensual. Actor: Propietario o Asistente.
Entrada: Precondiciones: Período AAAA-MM válido; contratos con ocupación en ese mes.
Disparador: Generar cargos en PAN-20.
Pasos: 1) Consultar elegibles y mostrar canon completo, fecha ajustada al último día del mes y
omisiones. 2) Confirmar MSG-058 e iniciar H1 con instantánea de ids y política. 3) Por
contrato bloquear, comprobar renta única, crear cargo y aplicar saldo a favor disponible sin
nuevo recibo. 4) Confirmar por contrato; publicar progreso y conteos de generados, omitidos,
fallidos y pendientes; registrar en bitacora el evento M05 LOTE_RENTAS con correlación y
esos conteos, y finalizar con MSG-060.
Alternos y Excepciones: Alternos: Cancelar es un resultado normal sin ERR: usa MSG-059,
espera que el contrato en curso termine con commit o rollback y detiene antes del siguiente;
conserva contratos confirmados e identifica pendientes en el resumen; reanudar omite rentas
ya existentes, incluso anuladas. Excepciones: Fallo de un contrato revierte solo ese contrato y
continúa con los demás; caída de BD detiene lote y conserva como resumen durable el evento
M05 LOTE_RENTAS en bitacora, con correlación y conteos confirmados de generados,
omitidos, fallidos y pendientes; sin tabla de lotes.
Salida: Postcondiciones: Un cargo Renta por contrato y período; totales de lote conciliados.
Datos y Pantallas: Lee: Contratos, cargos previos, crédito y política. Escribe: Renta,
aplicaciones de crédito, auditoría y evento M05 LOTE_RENTAS en bitacora con correlación y
conteos generados/omitidos/fallidos/pendientes. Pantallas: PAN-20, PAN-09.
Reglas y Ejecución: RN-04, RN-06, RN-17, RN-20. RF-M05-01 a RF-M05-04, RF-M05-11,
RF-M05-12. H1; sección por contrato coordinada con H2 y pagos.
La Tabla 26 presenta fLU-21 Registrar Cargo Manual.

## 27
## Tabla 26
FLU-21 Registrar Cargo Manual
Especificación del Flujo
Identidad: FLU-21. Registrar Cargo Manual. Actor: Propietario o Asistente.
Entrada: Precondiciones: Contrato seleccionado; tipo Reparación, Servicio u Otro. Disparador:
Nuevo cargo.
Pasos: 1) Abrir PAN-28 y capturar concepto, importe positivo y vencimiento. 2) Verificar
moneda contractual y vínculo de reparación si existe. 3) Aplicar FLU-01 y auditar; un gasto de
mantenimiento no crea este cargo por sí mismo. 4) Confirmar cargo Pendiente o Vencido
según fecha y saldo.
Alternos y Excepciones: Alternos: Contrato Terminado admite obligación correctiva
documentada por Propietario; no crea renta futura. Excepciones: Concepto vacío usa
MSG-004; monto inválido conserva datos y bloquea confirmación.
Salida: Postcondiciones: Cargo manual trazable sin duplicar gasto del propietario.
Datos y Pantallas: Lee: Contrato y solicitud de mantenimiento opcional. Escribe: Cargo y
bitácora. Pantallas: PAN-09, PAN-28.
Reglas y Ejecución: RN-14, RN-23, RN-24, RN-34. RF-M05-05. Servicio de aplicación fuera
del hilo JavaFX; UI recibe DTO.
La Tabla 27 presenta fLU-22 Revisar Estados y Generar Mora.
## Tabla 27
FLU-22 Revisar Estados y Generar Mora
Especificación del Flujo
Identidad: FLU-22. Revisar Estados y Generar Mora. Actor: Sistema bajo política del
## Propietario.
Entrada: Precondiciones: Instalación operativa; H2 habilitado cada cinco minutos. Disparador:
Vencimiento del temporizador o recuperación de suspensión.
Pasos: 1) Evitar solapamiento con indicador atómico local; obtener instante y fecha operativa
de now() de la BD y seleccionar contratos y rentas con saldo. 2) Bloquear un contrato a la vez;
releer saldos frente a H1 y pagos. 3) Después de vencimiento más tres días de gracia, crear una
Mora por renta según versión: porcentaje del principal pendiente o fijo; excluir Mora de la
base. 4) Actualizar estados de cargos y contratos, confirmar y avisar al panel con corte. H2 usa
ContextoOperacion.sistema('H2') y auditoría con actor NULL solo para M05
MORA_AUTOMATICA, REVISION_VENCIMIENTO e INCIDENTE_H2, con
detalle.origen='H2'.
Alternos y Excepciones: Alternos: Política inicial de mora cero no genera cargo de valor cero;
una revisión posterior vuelve a evaluar rentas sin recargo. Excepciones: Conflicto o timeout
revierte contrato afectado y registra incidente; siguiente ciclo reevalúa sin duplicar recargo. El
incidente se audita como M05 INCIDENTE_H2 con actor NULL y detalle.origen='H2'.
Salida: Postcondiciones: Mora única y estados derivados con prioridad Anulado, Pagado,
## Vencido, Parcial, Pendiente.

## 28
Especificación del Flujo
Datos y Pantallas: Lee: Rentas, aplicaciones, política y fecha. Escribe: Mora, reclasificaciones
y bitácora. Pantallas: PAN-09, PAN-02.
Reglas y Ejecución: RN-05, RN-12, RN-20, RN-24. RF-M05-06 a RF-M05-08, RF-M04-10.
H2: indicador atómico local y bloqueo de fila por contrato entre clientes LAN; sin concesión
de instalación, escena gráfica ni Session compartida.
La Tabla 28 presenta fLU-23 Consultar Cartera o Anular Cargo.
## Tabla 28
FLU-23 Consultar Cartera o Anular Cargo
Especificación del Flujo
Identidad: FLU-23. Consultar Cartera o Anular Cargo. Actor: Ambos consultan; Propietario
anula.
Entrada: Precondiciones: Corte válido; cargo sin aplicaciones activas para anular. Disparador:
Filtrar cartera o Anular cargo.
Pasos: 1) Agrupar saldo por contrato, período y antigüedad con moneda separada. 2) Mostrar
principal, mora, abonos y saldo a favor sin compensación oculta. 3) Para anular, exigir motivo,
bloquear contrato y cargo y releer aplicaciones. 4) Marcar Anulado con auditoría y conservar
clave de renta para impedir duplicación al reintentar H1.
Alternos y Excepciones: Alternos: Cargo con aplicaciones exige revertirlas antes; cancelar
anulación conserva cartera. Excepciones: MSG-024 impide anular con abonos; caída de
consulta conserva filtros.
Salida: Postcondiciones: Cartera consultada o cargo anulado con historia y motivo.
Datos y Pantallas: Lee: Cargos, aplicaciones, créditos y recibos. Escribe: Estado Anulado y
auditoría solo al confirmar. Pantallas: PAN-09, PAN-28.
Reglas y Ejecución: RN-04, RN-08, RN-13, RN-24. RF-M05-09, RF-M05-10. Servicio de
aplicación fuera del hilo JavaFX; UI recibe DTO.
M06 Pagos y Recibos
El pago conserva el dinero recibido, su moneda y el equivalente contractual usado para
distribuir aplicaciones. La asignación automática comienza por el cargo vencido más antiguo y la
selección manual exige releer los saldos antes de confirmar. H4 protege el consecutivo junto con
pago, recibo y auditoría en una transacción única. El UUID identifica una intención y permite
recuperar el resultado después de una respuesta perdida. La anulación revierte efectos
autorizados, conserva el número y bloquea operaciones con créditos consumidos posteriormente
hasta resolver sus dependencias documentadas.
La Figura 10 muestra recorridos Principales de M06.

## 29
## Figura 10
Recorridos Principales de M06
La Tabla 29 presenta fLU-24 Cobrar con Distribución Automática.
## Tabla 29
FLU-24 Cobrar con Distribución Automática
Especificación del Flujo
Identidad: FLU-24. Cobrar con Distribución Automática. Actor: Propietario o Asistente.
Entrada: Precondiciones: Contrato seleccionado; importe positivo; UUID nuevo conservado.
Disparador: Confirmar pago.
Pasos: 1) Obtener instante y fecha operativa de now() de la BD, presentar la fecha operativa en
America/Managua y capturar fecha efectiva ingresada por el usuario, no futura respecto a la
operativa, método y moneda; guardar la fecha efectiva en pago.fecha_pago y el instante de BD
en pago.creado_en; bloquear nuevas finanzas si |reloj local − BD| > 5 min (ERR-057 /
MSG-080); mostrar MSG-029 con distribución prevista. 2) Tomar puerta compartida y
bloquear contrato y cargos por vencimiento e id; releer saldos y aplicar al vencido más
antiguo. 3) Guardar pago y aplicaciones, derivar excedente en moneda contractual y ejecutar
flush; H4 invoca fn_reservar_numero en la misma conexión JDBC, que reserva el número y
avanza el contador; persistir recibo con copia histórica del emisor y ejecutar flush. 4) Insertar
auditoría JDBC en la misma conexión Hibernate, ejecutar CALL sp_verificar_integridad y
commit; mostrar MSG-030 antes de H5. En misma moneda la conversión es la identidad y
tasa_usada y autorizacion_id son NULL. Los bloqueos de recursos presentes siguen puerta,
unidad, contratos, cargos, pagos de origen, depósitos y contador; se omiten recursos ajenos al
pago.
Alternos y Excepciones: Alternos: Sin vencidos todo queda a favor; duplicado del mismo
UUID recupera el recibo existente. Excepciones: Antes de commit revierte contador y
finanzas; confirmación incierta usa MSG-064 y consulta UUID antes de reenvío.
Inconsistencia detectada antes del commit entre pago, aplicaciones, movimientos de garantía o
recibo: ERR-063 / MSG-086 solo tras rollback confirmado de toda la operación, conservando
causa técnica. Commit incierto mantiene ERR-031 o ERR-054 / MSG-064, sin afirmar
ausencia de cambios.
Salida: Postcondiciones: Pago Registrado y un recibo Registrado; importe convertido igual a
aplicaciones más crédito.

## 30
Especificación del Flujo
Datos y Pantallas: Lee: Contrato, cargos, crédito, permisos y contador. Escribe: Pago, recibo,
aplicaciones, consecutivo y evento atómicos. El crédito se deriva del equivalente menos
aplicaciones; no se escribe una entidad o columna de crédito. Pantallas: PAN-09, PAN-10,
## PAN-11.
Reglas y Ejecución: RN-06, RN-07, RN-14, RN-21, RN-32. RF-M06-01, RF-M06-02, RF-
M06-04, RF-M06-07, RF-M06-08, RF-M06-11, RF-M06-12. H4 dentro del servicio
financiero; H5 después del commit.
La Tabla 30 presenta fLU-25 Cobrar con Aplicación Manual.
## Tabla 30
FLU-25 Cobrar con Aplicación Manual
Especificación del Flujo
Identidad: FLU-25. Cobrar con Aplicación Manual. Actor: Propietario o Asistente.
Entrada: Precondiciones: Cargos abiertos del mismo contrato; resumen de pago preparado.
Disparador: Elegir distribución manual.
Pasos: 1) Seleccionar cargos y montos sin mezclar contratos ni monedas contractuales. 2)
Calcular total convertido con HALF_EVEN y reservar excedente como crédito. 3) Al
confirmar, bloquear y releer saldos; rechazar cualquier aplicación que supere el saldo actual. 4)
Ejecutar la transacción de FLU-24 con H4 y conservar distribución aceptada en recibo.
Alternos y Excepciones: Alternos: Volver a automático recalcula propuesta sin persistir;
aplicación cero excluye el cargo. Excepciones: MSG-028 exige revisar distribución si otro
usuario abonó; no ajustar importes del usuario en silencio.
Salida: Postcondiciones: Distribución manual confirmada o formulario conservado para
corregir.
Datos y Pantallas: Lee: Cargos y saldos actuales. Escribe: Mismos registros atómicos de
FLU-24. Pantallas: PAN-10, PAN-11.
Reglas y Ejecución: RN-06, RN-21, RN-32. RF-M06-03, RF-M06-12. H4; H5 para recibo
después de confirmar.
La Tabla 31 presenta fLU-26 Cobrar en Moneda Distinta.
## Tabla 31
FLU-26 Cobrar en Moneda Distinta
Especificación del Flujo
Identidad: FLU-26. Cobrar en Moneda Distinta. Actor: Propietario o Asistente.
Entrada: Precondiciones: Tasa autorizada por Propietario para la fecha efectiva ingresada por
el usuario, no futura respecto a la fecha operativa obtenida de now() de la BD en
America/Managua. Disparador: Elegir moneda distinta en PAN-10.

## 31
Especificación del Flujo
Pasos: 1) Mostrar por separado referencia comercial y tasa autorizada correspondiente a la
fecha efectiva con fuente y fecha. 2) Para NIO recibido y contrato USD dividir por NIO/USD;
en sentido inverso multiplicar. 3) Redondear total contractual a dos decimales, distribuir y
asignar remanente decimal al último cargo sin superar saldo. 4) Ejecutar FLU-24 y congelar
tasa, fecha, fuente y autorizador en pago y recibo.
Alternos y Excepciones: Alternos: Misma moneda usa la conversión identidad sin Internet, con
tasa_usada=NULL y autorizacion_id=NULL; nueva referencia de H3 no modifica la
autorización inmutable de la fecha. Si cambia la fecha efectiva al confirmar, verificar la
autorización de esa fecha y recalcular/reconfirmar antes de adquirir bloqueos financieros. Si
cambia la fecha operativa, revalidar que la efectiva no sea futura y mantener el control de
desfase del reloj ligado a la operativa. Excepciones: Sin autorización usa MSG-027; una
segunda autorización para la misma fecha se rechaza con ERR-060 / MSG-083. La corrección
de un registro autorizado requiere procedimiento excepcional y no modifica pagos históricos.
Salida: Postcondiciones: Original y equivalente conservados; cero diferencia entre equivalente,
aplicaciones y crédito.
Datos y Pantallas: Lee: Tasa autorizada, contrato y cargos. Escribe: Pago y procedencia
inmutable junto con FLU-24. Pantallas: PAN-10, PAN-14, PAN-32.
Reglas y Ejecución: RN-09, RN-21, RN-22, RN-32. RF-M06-05, RF-M06-06, RF-M09-09.
H3 solo aporta referencia; H4 confirma; ninguna llamada HTTP dentro de la transacción.
La Tabla 32 presenta fLU-27 Anular Pago y Recibo.
## Tabla 32
FLU-27 Anular Pago y Recibo
Especificación del Flujo
Identidad: FLU-27. Anular Pago y Recibo. Actor: Propietario.
Entrada: Precondiciones: Pago Registrado; ningún crédito suyo tiene aplicación posterior
activa. Disparador: Anular recibo.
Pasos: 1) Abrir PAN-29, exigir motivo y mostrar MSG-067. 2) Tomar puerta compartida y,
cuando corresponda, bloquear unidad; después contratos, cargos, pagos de origen y depósitos,
en ese orden, con contador al final si interviene; verificar RN-31. 3) Revertir aplicaciones y
crédito, recalcular saldos y marcar pago y recibo Anulado en una transacción. 4) Conservar
número, importes originales, actor e instante; regenerar copia marcada Anulado mediante H5.
Alternos y Excepciones: Alternos: Dependencias posteriores deben revertirse por operaciones
autorizadas antes de volver; recibo ya Anulado devuelve su estado sin otra reversión.
Excepciones: MSG-031 por motivo vacío; MSG-032 por dependencias; fallo de PDF usa
MSG-033 sin deshacer anulación confirmada.
Salida: Postcondiciones: Saldos restaurados y número ocupado por el recibo anulado.
Datos y Pantallas: Lee: Pago, recibo, crédito y aplicaciones descendientes. Escribe:
Reversiones, estados y auditoría; contador sin cambio. Pantallas: PAN-11, PAN-29.
Reglas y Ejecución: RN-08, RN-13, RN-14, RN-31. RF-M06-09. Servicio transaccional de
anulación; H5 para copia.
La Tabla 33 presenta fLU-28 Recuperar Resultado y Reimprimir Recibo.

## 32
## Tabla 33
FLU-28 Recuperar Resultado y Reimprimir Recibo
Especificación del Flujo
Identidad: FLU-28. Recuperar Resultado y Reimprimir Recibo. Actor: Propietario o Asistente.
Entrada: Precondiciones: Número de recibo o UUID del pago disponible. Disparador:
Consultar estado o Reimprimir.
Pasos: 1) Si el envío quedó incierto, consultar UUID con una sesión nueva. 2) Si existe pago
confirmado, recuperar su recibo; si no existe, comprobar transacción anterior terminada antes
de reintentar el mismo UUID. 3) Cargar instantánea histórica y estado actual del recibo sin
crear pago. 4) H5 genera vista previa y permite exportar conservando número y marca de
anulación.
Alternos y Excepciones: Alternos: Fallo solo de exportación permite repetir H5; una
reimpresión nunca toma contador H4. Excepciones: BD inaccesible mantiene bloqueo de
reenvío y MSG-064; archivo fallido usa MSG-033.
Salida: Postcondiciones: Resultado financiero conocido o pendiente explícito; misma
identificación de recibo.
Datos y Pantallas: Lee: Idempotencia, pago, recibo e instantánea del emisor. Escribe: Ninguno
al consultar o reimprimir; reintento usa FLU-24. Pantallas: PAN-10, PAN-11, PAN-16.
Reglas y Ejecución: RN-07, RN-08, RN-21. RF-M06-10, RF-M06-11. H5; H4 solo si se
confirma que no hubo pago y se reintenta el mismo UUID.
M07 Depósitos de Garantía
La garantía mantiene sus fondos separados de los cobros de renta y sus movimientos
explican cualquier aplicación o devolución. El registro nace con la activación del contrato y
conserva la moneda contractual. Al terminar, el Propietario confirma deducciones con concepto
y concilia el remanente contra el importe inicial. Una aplicación parcial sin devolución mantiene
Retenido, mientras una devolución parcial con fondos pendientes cambia a Devuelto parcial. El
estado final depende de si hubo devoluciones o solo aplicaciones, conforme a RN-26, y la
liquidación genera un documento distinto del recibo de pago.
La Figura 11 muestra recorridos Principales de M07.

## 33
## Figura 11
Recorridos Principales de M07
La Tabla 34 presenta fLU-29 Registrar y Consultar Depósito Retenido.
## Tabla 34
FLU-29 Registrar y Consultar Depósito Retenido
Especificación del Flujo
Identidad: FLU-29. Registrar y Consultar Depósito Retenido. Actor: Propietario o Asistente.
Entrada: Precondiciones: Contrato activado mediante FLU-16. Disparador: Abrir depósitos o
activar contrato.
Pasos: 1) Registrar garantía pactada dentro de la activación, en moneda contractual. 2) Separar
garantía de renta y del ingreso operativo. 3) Leer movimientos al corte y calcular remanente
igual a inicial menos aplicaciones menos devoluciones. 4) Mostrar Retenido y vínculos a su
contrato; permitir reporte de liquidación.
Alternos y Excepciones: Alternos: Garantía cero se identifica como sin fondos retenidos y
estado Retenido sin movimientos; no genera pago de renta ni recibo M06. Excepciones:
Descuadre bloquea liquidación y registra incidente; MSG-036 cuando no existe depósito.
Salida: Postcondiciones: Garantía trazable y saldo consultado.
Datos y Pantallas: Lee: Contrato y movimientos de depósito. Escribe: Depósito solo en
activación; consulta sin escrituras. Pantallas: PAN-12, PAN-25.
Reglas y Ejecución: RN-10, RN-26. RF-M07-01, RF-M07-02. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 35 presenta fLU-30 Aplicar Garantía a Deuda.
## Tabla 35
FLU-30 Aplicar Garantía a Deuda
Especificación del Flujo
Identidad: FLU-30. Aplicar Garantía a Deuda. Actor: Propietario.
Entrada: Precondiciones: Propuesta de cierre; depósito con remanente y deuda del contrato.
Disparador: Aplicar depósito.

## 34
Especificación del Flujo
Pasos: 1) Abrir PAN-31 con cargos y concepto por deducción. 2) Asignar montos sin superar
saldo de cargo ni remanente de garantía. 3) Con UUID de liquidación (liquidacion_id),
bloquear contrato, cargos y depósito; confirmar aplicaciones y auditoría con terminación si
integra FLU-18. 4) Recalcular estado: agotado solo con aplicaciones queda Aplicado; con
remanente sin devolución conserva Retenido. Cada movimiento conserva el mismo
liquidacion_id y liquidacion_hash del paquete, un orden SMALLINT y unicidad
(liquidacion_id, orden).
Alternos y Excepciones: Alternos: Asistente consulta y prepara sin confirmar; parte restante
podrá devolverse en el mismo paquete de liquidación. Excepciones: MSG-034 por deducción
sin concepto; MSG-035 por exceso; reintento consulta por liquidacion_id. Cargo inexistente,
anulado o de otro contrato: ERR-061 / MSG-084; rechazar y conservar la propuesta. Un
exceso de saldo mantiene su error específico.
Salida: Postcondiciones: Deuda reducida por garantía, sin recibo de cobro nuevo ni aumento
de ingresos de caja.
Datos y Pantallas: Lee: Depósito, deuda, aplicaciones y versión. Escribe: Movimientos de
garantía, aplicaciones y evento. Pantallas: PAN-12, PAN-31.
Reglas y Ejecución: RN-10, RN-13, RN-21, RN-26. RF-M07-03, RF-M07-04. Servicio de
aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 36 presenta fLU-31 Devolver Garantía Total o Parcial.
## Tabla 36
FLU-31 Devolver Garantía Total o Parcial
Especificación del Flujo
Identidad: FLU-31. Devolver Garantía Total o Parcial. Actor: Propietario.
Entrada: Precondiciones: Remanente calculado y deducciones justificadas. Disparador:
Confirmar devolución.
Pasos: 1) Capturar devolución, fecha, método y conceptos de deducciones. 2) Exigir que
aplicaciones más devoluciones acumuladas no superen depósito inicial. 3) Confirmar con
UUID de liquidación (liquidacion_id), hash compartido del paquete, orden por movimiento y
auditoría; si queda remanente con devolución, estado Devuelto parcial; agotado con
devolución, Devuelto. 4) Mostrar MSG-068 y emitir liquidación mediante H5 sin consumir
consecutivo de recibos de pago.
Alternos y Excepciones: Alternos: Devolución cero con aplicación total queda Aplicado; cierre
contractual exige remanente cero, salvo garantía inicial cero. Excepciones: Exceso usa
MSG-035; fallo después de commit exige consultar por liquidacion_id, no duplicar
devolución.
Salida: Postcondiciones: Fondos conciliados y comprobante de liquidación separado de renta.
Datos y Pantallas: Lee: Depósito, movimientos y propuesta de cierre. Escribe: Devolución,
estado y bitácora. Pantallas: PAN-12, PAN-31, PAN-16.
Reglas y Ejecución: RN-10, RN-21, RN-26. RF-M07-05, RF-M07-06, RF-M07-07. Servicio
financiero transaccional; H5 genera liquidación.

## 35
## M08 Mantenimiento
La solicitud registra una necesidad de trabajo vinculada a una unidad, con prioridad y
responsable. El equipo distingue el estado del trabajo del estado de ocupación para admitir
reparaciones mientras un contrato sigue activo. Resolver registra el costo efectivo como gasto del
propietario y conserva los cambios del expediente. Repercutir ese costo al inquilino requiere otro
cargo con concepto, sin una creación automática que confunda ambos movimientos. Las
consultas agrupan gastos por propiedad y moneda; la cancelación conserva costos ya
confirmados y exige un motivo por cualquiera de los dos roles.
La Figura 12 muestra recorridos Principales de M08.
## Figura 12
Recorridos Principales de M08
La Tabla 37 presenta fLU-32 Crear o Asignar Solicitud de Mantenimiento.
## Tabla 37
FLU-32 Crear o Asignar Solicitud de Mantenimiento
Especificación del Flujo
Identidad: FLU-32. Crear o Asignar Solicitud de Mantenimiento. Actor: Propietario o
## Asistente.
Entrada: Precondiciones: Unidad registrada; no exige desocupar unidad. Disparador: Nueva
solicitud.
Pasos: 1) Abrir PAN-30 y capturar unidad, descripción y prioridad Baja, Media o Alta. 2)
Elegir responsable textual y fecha; aplicar FLU-01. 3) Guardar Abierta y permitir asignar o
corregir antes de resolver. 4) Mostrar MSG-039 y conservar vínculo a unidad y propiedad.

## 36
Especificación del Flujo
Alternos y Excepciones: Alternos: Solicitud en unidad Ocupada no cambia su estado;
restricción de disponibilidad usa FLU-10 solo sin contrato activo. Excepciones: MSG-037 por
descripción o unidad ausente; fallo revierte solicitud.
Salida: Postcondiciones: Solicitud Abierta con responsable y prioridad.
Datos y Pantallas: Lee: Unidad, propiedad y solicitud. Escribe: Solicitud, asignación y evento.
Pantallas: PAN-13, PAN-30.
Reglas y Ejecución: RN-23, RN-27. RF-M08-01, RF-M08-02. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 38 presenta fLU-33 Atender y Resolver Mantenimiento.
## Tabla 38
FLU-33 Atender y Resolver Mantenimiento
Especificación del Flujo
Identidad: FLU-33. Atender y Resolver Mantenimiento. Actor: Propietario o Asistente.
Entrada: Precondiciones: Solicitud Abierta o En proceso. Disparador: Iniciar atención o
## Resolver.
Pasos: 1) Cambiar Abierta a En proceso con responsable y fecha. 2) Registrar costo efectivo y
moneda con concepto de gasto. 3) Para resolver exigir descripción del trabajo y fecha;
confirmar costo y estado Resuelta. 4) Actualizar gastos por propiedad y conservar historial de
estados.
Alternos y Excepciones: Alternos: Sin costo se admite cero con explicación; costos
repercutidos exigen cargo independiente FLU-21. Excepciones: Solicitud terminal no se
reabre; conflicto de versión conserva edición hasta comparación. Gasto ligado a solicitud cuya
unidad pertenece a otra propiedad: ERR-065 / MSG-087, sin registro y con selección
corregible. Solo se permiten Abierta a En proceso o Cancelada y En proceso a Resuelta o
Cancelada; repetir el estado actual lo devuelve sin duplicar cambios ni eventos. Otra transición
usa ERR-066 / MSG-088 y conserva el estado anterior.
Salida: Postcondiciones: Resuelta y gasto trazable sin cobro automático al inquilino.
Datos y Pantallas: Lee: Solicitud, unidad y costos previos. Escribe: Estado, costo y gasto de
propiedad. Pantallas: PAN-13, PAN-30.
Reglas y Ejecución: RN-23, RN-27. RF-M08-03, RF-M08-04, RF-M08-05. Servicio de
aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 39 presenta fLU-34 Consultar Gastos o Cancelar Solicitud.
## Tabla 39
FLU-34 Consultar Gastos o Cancelar Solicitud
Especificación del Flujo
Identidad: FLU-34. Consultar Gastos o Cancelar Solicitud. Actor: Propietario o Asistente.
Entrada: Precondiciones: Solicitud no terminal para cancelar; período válido para consulta.
Disparador: Filtrar gastos o Cancelar solicitud.

## 37
Especificación del Flujo
Pasos: 1) Consultar por propiedad, período, prioridad y estado. 2) Agrupar costo por moneda y
enlazar solicitud y gasto. 3) Para cancelar Abierta o En proceso exigir motivo y conservar
costos ya confirmados. 4) Confirmar Cancelada y actualizar lista sin borrar gasto efectivo.
Alternos y Excepciones: Alternos: Sin coincidencias usa MSG-038; Resuelta y Cancelada son
terminales. Excepciones: Conflicto de versión impide cancelar hasta releer la solicitud. Error
de consulta mantiene filtros. Repetir el estado actual es idempotente, sin nuevos cambios ni
eventos; otra transición no permitida usa ERR-066 / MSG-088 conservando el estado anterior.
Salida: Postcondiciones: Gastos explicables o solicitud Cancelada con historia.
Datos y Pantallas: Lee: Solicitudes, gastos y versiones. Escribe: Cancelación y auditoría solo
al confirmar. Pantallas: PAN-13, PAN-30.
Reglas y Ejecución: RN-23, RN-27. RF-M08-06, RF-M08-07. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
M09 Tipo de Cambio
La integración consulta una referencia comercial USD/NIO para cumplir el consumo de
servicio web y comunicar procedencia y antigüedad. Una respuesta aceptada crea una versión
informativa; el Propietario autoriza por separado la tasa de la fecha del pago. Esa separación
conserva el uso orientativo declarado por el proveedor y evita rotular su respuesta como
referencia oficial del BCN. La caché permite consultar información histórica sin Internet,
mientras la autorización local permite resolver una conversión documentada. H3 comparte cuota
y programación entre clientes de una misma instalación para evitar consultas multiplicadas.
La Figura 13 muestra recorridos Principales de M09.
## Figura 13
Recorridos Principales de M09
La Tabla 40 presenta fLU-35 Consultar Referencia Comercial USD/NIO.

## 38
## Tabla 40
FLU-35 Consultar Referencia Comercial USD/NIO
Especificación del Flujo
Identidad: FLU-35. Consultar Referencia Comercial USD/NIO. Actor: Sistema H3; ambos
solicitan consulta.
Entrada: Precondiciones: Instancia designada; llave externa; ventana y presupuesto
disponibles. Disparador: Programación diaria o consulta autorizada.
Pasos: 1) Adquirir exclusión compartida de instalación; reservar intento de cuota. 2)
HttpClient envía GET estándar base USD con Bearer y plazos de cinco segundos de conexión
y diez totales. 3) Gson valida result, base_code, conversion_rates.NIO, fechas y cuerpo hasta
256 KiB. 4) Guardar versión inmutable de referencia aceptada y publicar MSG-040 y
MSG-069 con origen y antigüedad. H3 usa ContextoOperacion.sistema('H3') y auditoría con
actor NULL solo para M09 REFERENCIA_ACTUALIZADA,
REFERENCIA_EN_CUARENTENA e INCIDENTE_H3, con detalle.origen='H3'. La
autorización de tasa conserva actor humano obligatorio. La respuesta exige JSON válido, base
USD, NIO decimal válido, fechas coherentes y banda inclusiva 1–100 NIO/USD. Una
respuesta inválida conserva la referencia anterior y registra rechazo; fuera de banda conserva
la anterior y registra cuarentena. Solo una consulta diaria y un único reintento para fallos
transitorios conforme al protocolo.
Alternos y Excepciones: Alternos: Refresco dentro de ventana devuelve caché; red, HTTP 408
o 5xx admiten un único reintento tras sesenta segundos. Excepciones: los demás 4xx
suspenden según causa; 429 respeta Retry-After o veinticuatro horas; JSON inválido conserva
historial anterior; fuera de banda 1–100 NIO/USD no guarda referencia vigente y registra en
bitacora M09 REFERENCIA_EN_CUARENTENA con valor y huella, sin tabla adicional.
Los incidentes de H3 se auditan como M09 INCIDENTE_H3 con actor NULL y
detalle.origen='H3'.
Salida: Postcondiciones: Referencia informativa almacenada; ninguna autorización financiera
implícita.
Datos y Pantallas: Lee: Cuota, programación, llave externa y respuesta JSON. Escribe: Intento,
referencia, metadatos y diagnóstico sin llave. Pantallas: PAN-14, PAN-02.
Reglas y Ejecución: RN-09, RN-22. RF-M09-01 a RF-M09-03, RF-M09-05. H3; no bloquea
hilo JavaFX ni transacciones de pago.
La Tabla 41 presenta fLU-36 Usar Caché e Inspeccionar Historial.
## Tabla 41
FLU-36 Usar Caché e Inspeccionar Historial
Especificación del Flujo
Identidad: FLU-36. Usar Caché e Inspeccionar Historial. Actor: Propietario o Asistente.
Entrada: Precondiciones: Historial local disponible o instalación sin primera consulta.
Disparador: Abrir tipo de cambio.

## 39
Especificación del Flujo
Pasos: 1) Leer última referencia con fetched_at, last_update y next_update. 2) Rotular vigencia
informativa hasta next_update; fecha local distinta de la del pago se identifica histórica. 3)
Mostrar registros de autorización usados en pagos sin cambiar sus valores. 4) Mantener
consulta local aun sin Internet y mostrar el estado "Sin tasa disponible" cuando no haya caché.
MSG-065 se muestra solo al intentar una conversión sin tasa autorizada para la fecha efectiva.
Alternos y Excepciones: Alternos: Caché vencida usa MSG-041; misma moneda permite pagar
sin referencia externa. Excepciones: BD local caída usa MSG-006; caché no autoriza pago
entre monedas.
Salida: Postcondiciones: Origen, antigüedad y distinción comercial/autorizada visibles.
Datos y Pantallas: Lee: Historial de referencias y tasas utilizadas. Escribe: Ninguno. Pantallas:
## PAN-14, PAN-02.
Reglas y Ejecución: RN-09, RN-22. RF-M09-04, RF-M09-06, RF-M09-08. Servicio de
aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 42 presenta fLU-37 Autorizar Tasa Manual por Fecha.
## Tabla 42
FLU-37 Autorizar Tasa Manual por Fecha
Especificación del Flujo
Identidad: FLU-37. Autorizar Tasa Manual por Fecha. Actor: Propietario.
Entrada: Precondiciones: Fecha efectiva del pago, fuente verificable y motivo disponibles; no
existe autorización para esa fecha. Disparador: Registrar tasa manual.
Pasos: 1) Abrir PAN-32 con fecha efectiva y tasa positiva NIO por USD. 2) Registrar fuente y
motivo; rango técnico de una a cien NIO/USD requiere revisión explícita fuera de banda, sin
presentarlo como ley económica. 3) Confirmar un único registro autorizado e inmutable por
fecha efectiva, protegido por UNIQUE, con id, actor e instante de BD; mostrar MSG-070. 4)
Habilitar FLU-26 para esa fecha y conservar los registros vinculados a pagos.
Alternos y Excepciones: Alternos: Referencia comercial puede informar revisión, pero el
Propietario registra fundamento propio; fecha histórica usa fuente de esa fecha. Excepciones:
Asistente usa MSG-062; fecha efectiva ya autorizada produce DuplicadoException, ERR-060 /
MSG-083 y no inserta otro registro; dato no positivo se rechaza; fuera de banda exige
reingreso y confirmación de fuente por Propietario.
Salida: Postcondiciones: Una autorización inmutable por fecha efectiva, sin modificar pagos
previos. La corrección requiere procedimiento excepcional; este flujo no crea versiones ni
invalida registros.
Datos y Pantallas: Lee: Fuentes y autorizaciones existentes. Escribe: Registro único e
inmutable de tasa autorizada y auditoría. Pantallas: PAN-14, PAN-32.
Reglas y Ejecución: RN-09, RN-13, RN-22. RF-M09-07, RF-M09-08, RF-M09-09. Servicio
de aplicación fuera del hilo JavaFX; UI recibe DTO.
## M10 Reportes
Los reportes fijan parámetros y corte antes de leer movimientos para que sus totales
puedan reconciliarse. H5 recibe objetos de transferencia y cierra la sesión de datos antes de llenar

## 40
una plantilla Jasper, sin convertir la plantilla en un servicio financiero. La vista previa conserva
filtros y permite elegir PDF o XLSX mediante diálogos nativos. La publicación usa un archivo
temporal y protege cualquier destino anterior hasta completar la escritura. Cancelar mantiene el
trabajo visible mientras libera recursos reales; un recibo confirmado conserva su número aunque
falle la generación de su archivo.
La Figura 14 muestra recorridos Principales de M10.
## Figura 14
Recorridos Principales de M10
La Tabla 43 presenta fLU-38 Parametrizar y Generar Reporte.
## Tabla 43
FLU-38 Parametrizar y Generar Reporte
Especificación del Flujo
Identidad: FLU-38. Parametrizar y Generar Reporte. Actor: Propietario o Asistente.
Entrada: Precondiciones: Reporte elegido y acceso a sus datos. Disparador: Generar reporte.
Pasos: 1) Elegir salida del catálogo de diez, corte, fechas, propiedad, contrato y moneda
pertinentes. REP-05 toma el instante de generación y no ofrece fecha histórica. 2) Validar
rango y parámetros obligatorios; fijar instantánea de lectura consistente y filtros. 3) H5
consulta DTO, cierra Session y llena plantilla Jasper compilada con la versión fijada. 4)
Mostrar PAN-16 con parámetros y totales separados por moneda.
Alternos y Excepciones: Alternos: Cero filas usa MSG-049 y permite salida con encabezados
y total cero; recibo usa instantánea histórica. Excepciones: MSG-045 por contrato ausente;
MSG-046 por generación fallida conserva parámetros y libera memoria.
Salida: Postcondiciones: Vista previa coherente con corte y filtros.
Datos y Pantallas: Lee: Movimientos, catálogos e instantáneas. Escribe: Ninguno en dominio.
Pantallas: PAN-15, PAN-16.
Reglas y Ejecución: RN-08, RN-24, RN-28. RF-M10-01 a RF-M10-08, RF-M10-11, RF-M10-
- H5; escena gráfica solo recibe progreso y resultado.

## 41
La Tabla 44 presenta fLU-39 Exportar PDF o XLSX.
## Tabla 44
FLU-39 Exportar PDF o XLSX
Especificación del Flujo
Identidad: FLU-39. Exportar PDF o XLSX. Actor: Propietario o Asistente.
Entrada: Precondiciones: Vista previa generada; destino elegido. Disparador: Exportar PDF,
Exportar XLSX o Exportar lote.
Pasos: 1) FileChooser elige un archivo; DirectoryChooser selecciona carpeta para lote de
reportes. 2) Comprobar extensión, permiso y colisión; MSG-048 exige Reemplazar o
Cancelar. 3) H5 escribe temporal en mismo volumen y valida finalización; XLSX conserva
importes numéricos y monedas explícitas. 4) Publicar por cambio de nombre y mostrar
MSG-050 solo cuando el archivo final exista.
Alternos y Excepciones: Alternos: Cancelar selector no crea archivo; si cambio atómico no
está soportado, publicar con copia verificada y conservar original hasta éxito. Excepciones:
MSG-047 ofrece otra ubicación; fallo conserva archivo final previo y elimina temporal
incompleto.
Salida: Postcondiciones: Archivo íntegro con nombre y formato elegidos.
Datos y Pantallas: Lee: JasperPrint, DTO y destino. Escribe: Archivo externo y temporal;
ninguna alteración financiera. Pantallas: PAN-16.
Reglas y Ejecución: RN-28. RF-M10-09, RF-M10-10. H5; diálogos de archivo en hilo
JavaFX.
La Tabla 45 presenta fLU-40 Cancelar Generación o Exportación.
## Tabla 45
FLU-40 Cancelar Generación o Exportación
Especificación del Flujo
Identidad: FLU-40. Cancelar Generación o Exportación. Actor: Propietario o Asistente.
Entrada: Precondiciones: H5 activa y asociada a la sesión. Disparador: Cancelar o solicitud de
## H6.
Pasos: 1) Marcar cancelación y deshabilitar nuevo envío de la misma tarea. 2) H5 consulta
bandera entre fases y usa interrupción cooperativa cuando el exportador lo admite. 3) Cerrar
recursos y borrar temporal sin sustituir destino previo. 4) Mostrar MSG-089, sin ERR, solo al
terminar el trabajo real y limpiar temporales antes de publicar; liberar DTO y JasperPrint. La
cancelación es cooperativa durante consulta, llenado y exportación.
Alternos y Excepciones: Alternos: Si publicación ya terminó, devolver éxito con ruta; si
exportador no responde, mantener tarea visible y H6 abierto. Excepciones: No confundir Task
cancelada con recurso ya liberado; fallo de borrado registra ruta para limpieza posterior
protegida. La publicación final resuelve la carrera: si ya se publicó, informar el resultado real;
conservar destino previo mientras no exista publicación exitosa y mantener todo pago o
anulación ya confirmados.
Salida: Postcondiciones: Sin archivo incompleto publicado ni sesión de datos retenida.

## 42
Especificación del Flujo
Datos y Pantallas: Lee: Registro de tareas y temporales. Escribe: Limpieza de temporal; estado
técnico. Pantallas: PAN-16, PAN-40.
Reglas y Ejecución: RN-28, RN-30. RF-M10-12. H5 con H6; confirmación de terminación
real.
## M11 Panel Principal
El panel mantiene un corte explícito para ocupación, cobrado, por cobrar y mora, con
denominadores y monedas visibles. Sus indicadores abren listas filtradas que permiten explicar
cada total mediante los registros que lo componen. Si falla un refresco, la vista conserva el
conjunto anterior con su fecha para evitar mezclar cifras de momentos distintos. La navegación
lateral protege formularios modificados antes de cambiar el contenido de la ventana única. El
selector personal aplica los tokens del brief a escenas y diálogos existentes, sin reconstruir
controles ni alterar una operación financiera pendiente.
La Figura 15 muestra recorridos Principales de M11.
## Figura 15
Recorridos Principales de M11
La Tabla 46 presenta fLU-41 Consultar Indicadores con Corte.
## Tabla 46
FLU-41 Consultar Indicadores con Corte
Especificación del Flujo
Identidad: FLU-41. Consultar Indicadores con Corte. Actor: Propietario o Asistente.
Entrada: Precondiciones: Sesión válida; filtros iniciales del período. Disparador: Abrir panel o
## Actualizar.

## 43
Especificación del Flujo
Pasos: 1) Fijar el instante de generación para ocupación, sin selector histórico, y consultar
ocupadas sobre activas, cobrado y por cobrar por moneda. 2) Separar Mora del principal y
excluir anulaciones de cobro neto. 3) Leer contratos Por vencer revisados por H2 y referencia
comercial de H3. 4) Actualizar conjunto de indicadores de un mismo corte y anunciar
finalización sin mover foco.
Alternos y Excepciones: Alternos: Cero unidades muestra cero con denominador cero; sin tasa
muestra ausencia explícita. Excepciones: MSG-044 conserva conjunto anterior con su corte si
falla refresco; no mezclar consultas parciales nuevas y antiguas.
Salida: Postcondiciones: Panel con denominadores, origen y fechas visibles.
Datos y Pantallas: Lee: Consultas agregadas JDBC, contratos y tasas. Escribe: Ninguno.
Pantallas: PAN-02.
Reglas y Ejecución: RN-12, RN-22, RN-24. RF-M11-01 a RF-M11-05. Consulta de
aplicación; H2 y H3 publican avisos de nuevo corte.
La Tabla 47 presenta fLU-42 Navegar desde Indicador o Barra Lateral.
## Tabla 47
FLU-42 Navegar desde Indicador o Barra Lateral
Especificación del Flujo
Identidad: FLU-42. Navegar desde Indicador o Barra Lateral. Actor: Propietario o Asistente.
Entrada: Precondiciones: Destino autorizado para el rol. Disparador: Indicador, menú,
contexto o módulo lateral.
Pasos: 1) Guardar id seleccionado, filtros y foco de la vista origen. 2) Si hay edición abrir
PAN-39: Guardar, Descartar o Continuar editando. 3) Tras resolución cargar destino dentro de
la ventana única; propagar filtros del indicador. 4) Actualizar selección lateral, título superior y
foco al encabezado o primer control de destino.
Alternos y Excepciones: Alternos: Continuar editando conserva vista; volver restaura selección
y posición; módulos administrativos no aparecen para Asistente. Excepciones: Falla de carga
conserva vista origen y MSG-006; servicio vuelve a validar rol aunque comando se fuerce.
Salida: Postcondiciones: Ruta y contenido coinciden, sin pérdida silenciosa de datos.
Datos y Pantallas: Lee: Permisos, filtros y cambios pendientes. Escribe: Estado de navegación
en memoria. Pantallas: PAN-02, PAN-07, PAN-09, PAN-13, PAN-39.
Reglas y Ejecución: RN-13, RN-37. RF-M11-06, RF-M11-10. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 48 presenta fLU-43 Cambiar Tema y Modalidad en Sesión.
## Tabla 48
FLU-43 Cambiar Tema y Modalidad en Sesión
Especificación del Flujo
Identidad: FLU-43. Cambiar Tema y Modalidad en Sesión. Actor: Propietario o Asistente.
Entrada: Precondiciones: Ventana principal o diálogo abierto; usuario identificado.
Disparador: Selector Claro/Oscuro o Sin efectos decorativos.

## 44
Especificación del Flujo
Pasos: 1) Conservar nodo con foco, selección, filtros, UUID y texto de formularios. 2) Aplicar
tokens del brief a escena y ventanas auxiliares registradas sin recrear controladores. 3) Activar
o retirar luces estáticas con mouseTransparent y focusTraversable=false. 4) Persistir tema y
modalidad por id opaco, además de última elección local; restaurar foco si el nodo sigue
disponible.
Alternos y Excepciones: Alternos: Sin preferencia individual usa Claro; cambio de cuenta
carga la de esa cuenta. Reportes conservan papel blanco. Excepciones: MSG-003 mantiene
cambio de sesión ante fallo de guardado; decoración ausente usa superficie uniforme sin
bloquear navegación.
Salida: Postcondiciones: Dos temas completos con mismos datos, tarea y permisos.
Datos y Pantallas: Lee: Preferences local y recursos CSS. Escribe: Preferencia local; ninguna
entidad de negocio. Pantallas: PAN-02 y diálogos abiertos.
Reglas y Ejecución: RN-35 a RN-38. RF-M11-07 a RF-M11-09. Hilo JavaFX; sin tarea de
animación ni cambios en H1 a H6.
M12 Configuración y Auditoría
Los parámetros administrativos tienen versión y fecha de vigencia para explicar qué
política originó cada movimiento. Los nuevos datos del emisor afectan recibos posteriores,
mientras las copias ya emitidas mantienen su identificación histórica. La bitácora ofrece consulta
de solo lectura por actor, acción, período y correlación. El respaldo suspende escrituras y reúne
base de datos y adjuntos bajo un corte verificable. Restaurar exige mantenimiento, copia previa,
contraseña de respaldo, autenticidad AES-256-GCM, descifrado e integridad del paquete; el
cierre utiliza H6 para esperar transacciones y recursos antes de liberar el pool compartido por la
instalación.
La Figura 16 muestra recorridos Principales de M12.

## 45
## Figura 16
Recorridos Principales de M12
La Tabla 49 presenta fLU-44 Versionar Parámetros y Datos del Emisor.
## Tabla 49
FLU-44 Versionar Parámetros y Datos del Emisor
Especificación del Flujo
Identidad: FLU-44. Versionar Parámetros y Datos del Emisor. Actor: Propietario.
Entrada: Precondiciones: Sesión administrativa y versión actual. Disparador: Guardar
parámetros.
Pasos: 1) Abrir PAN-17 con gracia, mora, umbral de vencimiento, bloqueo, inactividad e
intervalos de tareas. 2) Validar límites y vigencia; capturar identificación, contacto y logotipo
opcional PNG o JPEG. 3) Guardar versión por FLU-01 y anunciar programación nueva a
tareas sin modificar transacción en curso. 4) Mostrar MSG-052; nuevos recibos usan emisor
actualizado y los anteriores conservan copia.
Alternos y Excepciones: Alternos: Tema personal se cambia por FLU-43 sin entrar a
configuración; logotipo ausente conserva recibos textuales. Excepciones: ERR-064 / MSG-051
por días o montos configurables negativos, conservando el formulario para corregir el rango;
las demás validaciones mantienen sus mensajes específicos; conflicto de versión obliga a
recargar antes de guardar.
Salida: Postcondiciones: Política nueva auditable con fecha de vigencia.
Datos y Pantallas: Lee: Parámetros, emisor y versión. Escribe: Versión de política, emisor y
archivo opcional. Pantallas: PAN-17.
Reglas y Ejecución: RN-05, RN-13, RN-25, RN-28. RF-M12-01, RF-M12-02, RF-M12-03.
Servicio de aplicación fuera del hilo JavaFX; UI recibe DTO.
La Tabla 50 presenta fLU-45 Consultar Auditoría y Detalle de Evento.
## Tabla 50
FLU-45 Consultar Auditoría y Detalle de Evento
Especificación del Flujo
Identidad: FLU-45. Consultar Auditoría y Detalle de Evento. Actor: Propietario.
Entrada: Precondiciones: Sesión administrativa. Disparador: Filtrar bitácora o abrir evento.

## 46
Especificación del Flujo
Pasos: 1) Elegir usuario, acción y rango de fechas. 2) Consultar mediante PreparedStatement
paginado. 3) Abrir PAN-38 con instante UTC presentado en America/Managua, correlación y
referencias. 4) Permitir volver a lista con filtros; MSG-056 indica solo lectura.
Alternos y Excepciones: Alternos: Sin eventos usa MSG-057; operación financiera se busca
por UUID o número de recibo. Excepciones: Falla de consulta conserva filtro y MSG-006;
ningún rol puede editar eventos desde Latch.
Salida: Postcondiciones: Evento recuperado sin secretos ni alteraciones.
Datos y Pantallas: Lee: Bitácora y referencias financieras. Escribe: Ninguno; la escritura del
evento ocurre en la transacción originaria. Pantallas: PAN-19, PAN-38.
Reglas y Ejecución: RN-13, RN-14. RF-M12-04, RF-M12-05. Servicio de aplicación fuera del
hilo JavaFX; UI recibe DTO.
La Tabla 51 presenta fLU-46 Crear Respaldo Consistente.
## Tabla 51
FLU-46 Crear Respaldo Consistente
Especificación del Flujo
Identidad: FLU-46. Crear Respaldo Consistente. Actor: Propietario.
Entrada: Precondiciones: Destino con espacio; autoridad de mantenimiento y contraseña de
respaldo disponibles, capturada en pwdClaveRespaldo sin registrarla ni guardarla en
## Preferences. Disparador: Respaldar.
Pasos: 1) Elegir carpeta con DirectoryChooser en PAN-36 y anunciar suspensión de nuevas
escrituras a clientes. 2) Tomar puerta exclusiva de mantenimiento tras drenar transacciones y
pausar H1, H2 y escrituras de H3. 3) Generar pg_dump y copiar adjuntos bajo el mismo corte;
crear manifiesto con hashes y versión de esquema. 4) Cifrar el paquete con AES-256-GCM y
la contraseña de respaldo; verificar autenticidad e integridad antes de publicarlo y liberar
puerta; reanudar tareas con nuevo estado de instalación.
Alternos y Excepciones: Alternos: Cancelar antes de exclusión no cambia operación; cancelar
durante copia elimina temporal y reabre escrituras al liberar recursos. Excepciones: Espacio o
permiso insuficiente conserva respaldo previo; timeout no fuerza cierre de transacciones de
pago; fallo de autenticidad o clave usa ERR-059 / MSG-082 y no publica el paquete.
Salida: Postcondiciones: Paquete de BD y adjuntos cifrado con AES-256-GCM, auténtico e
íntegro sin credenciales de conexión.
Datos y Pantallas: Lee: BD, adjuntos, versiones y tareas. Escribe: Paquete cifrado y manifiesto
protegido; evento del respaldo. Pantallas: PAN-36, PAN-40.
Reglas y Ejecución: RN-13, RN-29. RF-M12-06. Tarea de respaldo supervisada por H6;
puerta de escritura compartida con H1 a H4.
La Tabla 52 presenta fLU-47 Restaurar en Mantenimiento.

## 47
## Tabla 52
FLU-47 Restaurar en Mantenimiento
Especificación del Flujo
Identidad: FLU-47. Restaurar en Mantenimiento. Actor: Propietario.
Entrada: Precondiciones: Paquete disponible y contraseña de respaldo capturada en
pwdClaveRespaldo; ninguna sesión operativa en clientes; cuenta administrativa del clúster con
CREATEDB, externa a latch_runtime y latch_owner. Disparador: Restaurar respaldo.
Pasos: 1) Abrir PAN-37, verificar autenticidad AES-256-GCM con la contraseña de respaldo y
descifrar en área aislada; después validar manifiesto, hashes y compatibilidad de esquema. 2)
Cerrar sesiones, detener tareas con H6 y adquirir exclusión externa mientras no haya
conexiones operativas. 3) Crear copia previa, restaurar a base de reemplazo y directorio
aislado; validar recuentos y referencias de archivos. 4) Cambiar configuración de destino solo
tras éxito, reiniciar arranque y autenticar; preservar copia anterior para recuperación.
Alternos y Excepciones: Alternos: Paquete incompatible se rechaza antes de reemplazo; error
en restauración conserva instalación previa y permite reinicio con ella. Excepciones: Sesiones
LAN activas bloquean proceso; fallo de copia previa impide restaurar; clave o autenticidad
inválida produce ERR-059 / MSG-082 antes del reemplazo y conserva instalación y copia
previa.
Salida: Postcondiciones: Instalación restaurada o anterior conservada sin mezcla de archivos.
Datos y Pantallas: Lee: Paquete, esquema y estado de sesiones. Escribe: Base de reemplazo,
adjuntos y registro externo de restauración. Pantallas: PAN-37, PAN-01.
Reglas y Ejecución: RN-13, RN-29, RN-30. RF-M12-07. H6 coordina detención; tarea
exclusiva de mantenimiento externa al pool cerrado.
La Tabla 53 presenta fLU-48 Cerrar Aplicación con Recursos Ordenados.
## Tabla 53
FLU-48 Cerrar Aplicación con Recursos Ordenados
Especificación del Flujo
Identidad: FLU-48. Cerrar Aplicación con Recursos Ordenados. Actor: Propietario o
## Asistente.
Entrada: Precondiciones: Aplicación abierta; inventario de tareas disponible. Disparador: Salir,
cierre de ventana o restauración autorizada.
Pasos: 1) Resolver PAN-39 y mostrar PAN-40 si hay trabajos; impedir nuevas tareas y
disparos programados. 2) H6 solicita cancelación segura de H1, H2, H3 y H5; H4 finaliza
commit o rollback. 3) Esperar confirmación de terminación real, cerrar HttpClient, sesiones,
SessionFactory y pool en ese orden. 4) Persistir preferencias sin bloquear cierre; solicitar
Platform.exit desde hilo JavaFX cuando no queden recursos activos.
Alternos y Excepciones: Alternos: Usuario puede continuar trabajando antes de iniciar drenaje;
superados diez segundos mostrar MSG-061 y conservar proceso abierto. Excepciones: Nunca
cerrar pool bajo H4 ni terminar proceso por fuerza; resultado incierto conserva UUID en
registro local protegido sin datos privados.

## 48
Especificación del Flujo
Salida: Postcondiciones: Proceso cerrado con tareas terminadas o espera visible sin pérdida
silenciosa.
Datos y Pantallas: Lee: Tareas, transacciones, formularios y preferencias. Escribe: Bitácora de
cierre y diario de UUID pendientes. Pantallas: PAN-39, PAN-40.
Reglas y Ejecución: RN-21, RN-29, RN-30. RF-M12-08. H6 coordina H1 a H5; no espera en
hilo JavaFX.
Flujos de Negocio de Extremo a Extremo
Los recorridos reúnen acciones de distintos módulos y mantienen una responsabilidad
identificable en cada paso. Cada diagrama conserva cinco carriles: Propietario, Asistente,
Sistema, Servicio Web y Base de Datos. Un carril sin actividad indica ausencia de participación
y no una llamada implícita. Las especificaciones usan importes sintéticos para calcular resultados
esperados y remiten a los FLU que definen excepciones particulares. La garantía, el crédito y los
cobros permanecen separados en cada cierre. Los diagramas representan diseño de
funcionamiento, sin acreditar operaciones realizadas en una aplicación implementada.
De Propiedad a Contrato Activo
La Tabla 54 presenta especificación de E2E-01.
## Tabla 54
Especificación de E2E-01
CampoRecorrido
Entrada y
flujos
Catálogos sin la unidad sintética U-01; dos roles habilitados. FLU-07,
## FLU-08, FLU-11, FLU-15, FLU-16.
Alternos y
excepciones
Si inicio es futuro conserva Borrador. Solapamiento revierte activación y
garantía. El Servicio Web no participa.
## Postcondición
medible
Una ocupación activa y una garantía vinculada; cero rentas creadas por
activar.
La Figura A1 del Apéndice A muestra carriles de E2E-01 De Propiedad a Contrato
## Activo.
Ciclo Mensual de Cobranza
La Tabla 55 presenta especificación de E2E-02.

## 49
## Tabla 55
Especificación de E2E-02
CampoRecorrido
Entrada y
flujos
CT-01 activo en noviembre; canon sintético C$ 1,000.00 y mora fija sintética
## C$ 50.00. FLU-20, FLU-22, FLU-24, FLU-38.
Alternos y
excepciones
Sin saldo no genera mora; sin política positiva no crea recargo. Lote
cancelado reanuda por contrato. Servicio Web sin llamada.
## Postcondición
medible
Renta y Mora Pagado; recibo único; estado de cuenta saldo cero si no existen
otros movimientos.
La Figura A2 del Apéndice A muestra carriles de E2E-02 Ciclo Mensual de Cobranza.
Pago Parcial y Aplicación a Cargos
La Tabla 56 presenta especificación de E2E-03.
## Tabla 56
Especificación de E2E-03
CampoRecorrido
Entrada y
flujos
Dos cargos vencidos sintéticos: C$ 600.00 y C$ 400.00; pago C$ 800.00.
## FLU-24, FLU-25, FLU-23.
Alternos y
excepciones
Si otro usuario pagó, rechaza distribución excesiva y conserva formulario. El
primer cargo queda Pagado; el segundo Vencido con abono. Servicio Web
sin llamada.
## Postcondición
medible
C$ 800.00 aplicado, C$ 200.00 pendiente, cero crédito y un recibo.
La Figura A3 del Apéndice A muestra carriles de E2E-03 Pago Parcial y Aplicación a
## Cargos.
Pago entre Monedas con Tasa Autorizada
La Tabla 57 presenta especificación de E2E-04.
## Tabla 57
Especificación de E2E-04
CampoRecorrido
Entrada y
flujos
Cargo sintético US$ 100.00; recibido C$ 3,662.43. Tasa autorizada del
ejemplo: 36.6243 NIO/USD, referencia BCN de 2026; no confundir con la
tasa ficticia 36.5 NIO/USD usada en fixtures de prueba. FLU-35, FLU-37,
## FLU-26.

## 50
CampoRecorrido
Alternos y
excepciones
Sin red consulta caché; sin tasa autorizada bloquea pago antes de transacción.
Misma moneda no requiere H3. No se interpreta la respuesta comercial como
tasa oficial.
## Postcondición
medible
US$ 100.00 aplicado y copia inmutable de la tasa autorizada; no se ejecutó
esta operación sobre Latch.
La Figura A4 del Apéndice A muestra carriles de E2E-04 Pago entre Monedas con Tasa
## Autorizada.
Anulación de Recibo con Dependencias
La Tabla 58 presenta especificación de E2E-05.
## Tabla 58
Especificación de E2E-05
CampoRecorrido
Entrada y
flujos
Pago sintético Registrado por C$ 500.00; crédito sin consumo posterior.
## FLU-27, FLU-28, FLU-45.
Alternos y
excepciones
Crédito consumido por otro cargo bloquea anulación hasta revertir
dependencias autorizadas. Repetir sobre Anulado devuelve estado sin nueva
reversión. Servicio Web sin llamada.
## Postcondición
medible
Saldos recalculados, número no reutilizable y evento consultable por
## Propietario.
La Figura A5 del Apéndice A muestra carriles de E2E-05 Anulación de Recibo con
## Dependencias.
Renovación sin Solapamiento
La Tabla 59 presenta especificación de E2E-06.
## Tabla 59
Especificación de E2E-06
CampoRecorrido
Entrada y
flujos
CT-01 Por vencer; sucesor CT-02 empieza el día posterior al fin anterior.
## FLU-17, FLU-30, FLU-31, FLU-16.
Alternos y
excepciones
Antes de fecha conserva sucesor Borrador; si el anterior no cerró, la nueva
ocupación no inicia. El cierre mantiene deuda anterior y no la traslada al
canon sucesor. Servicio Web sin llamada. Preparar la renovación solo se
inicia desde Vigente o Por vencer; el cierre atómico cuenta como Terminar y
admite Vigente, Por vencer o Vencido.

## 51
CampoRecorrido
## Postcondición
medible
Historia contractual enlazada, nueva garantía explícita y una ocupación
activa.
La Figura A6 del Apéndice A muestra carriles de E2E-06 Renovación sin Solapamiento.
Terminación Anticipada y Garantía
La Tabla 60 presenta especificación de E2E-07.
## Tabla 60
Especificación de E2E-07
CampoRecorrido
Entrada y
flujos
Garantía sintética C$ 1,000.00; deuda C$ 300.00 y devolución prevista C$
## 700.00. FLU-18, FLU-30, FLU-31.
Alternos y
excepciones
Deducción sin concepto o suma superior a garantía rechaza paquete. Si
garantía no cubre toda deuda, contrato Terminado conserva saldo. Servicio
Web sin llamada.
## Postcondición
medible
Remanente cero, deuda reducida, devolución trazable y unidad Disponible si
no hay restricción administrativa.
La Figura A7 del Apéndice A muestra carriles de E2E-07 Terminación Anticipada y
## Garantía.
Cierre Mensual con Reportes
La Tabla 61 presenta especificación de E2E-08.
## Tabla 61
Especificación de E2E-08
CampoRecorrido
Entrada y
flujos
Corte sintético 30/11/2026; movimientos del período confirmados. FLU-23,
## FLU-38, FLU-39, FLU-46.
Alternos y
excepciones
Cierre es revisión y exportación, no bloqueo contable irreversible ni
alteración de saldos. Archivo fallido se reexporta; respaldo fallido conserva
anterior. Servicio Web sin llamada.
## Postcondición
medible
Cortes y totales identificados por moneda; garantía fuera de renta y
documentos administrativos sin carácter fiscal.
La Figura A8 del Apéndice A muestra carriles de E2E-08 Cierre Mensual con Reportes.

## 52
Flujos de Concurrencia
Las tareas de JavaFX ejecutan call en segundo plano y comunican progreso mediante
propiedades observables, sin manipular el grafo de escena desde el trabajador (OpenJFX, s. f.-c).
Latch distinguirá el estado de Worker del estado real del hilo y de la transacción para evitar
cerrar recursos todavía ocupados. Cada caso de uso abre su propia Session y entrega DTO a la
interfaz, según el contrato de Hibernate (s. f.-b). Las banderas de cancelación se revisan en
puntos seguros, con confirmación explícita de que la tarea liberó sus recursos antes de completar
## H6.
La Tabla 62 presenta recursos Compartidos y Orden de Coordinación.
## Tabla 62
Recursos Compartidos y Orden de Coordinación
RecursoProtección y Frontera
Instalación y
mantenimiento
Puerta compartida para toda escritura; exclusiva para respaldo.
Restauración usa exclusión externa con clientes detenidos.
Unidad y
contratos
Tomar puerta compartida; bloquear unidad si cambia ocupación, contratos
por UUID, cargos por vencimiento e id, pagos de origen, depósitos y
contador al final, según los recursos de la operación.
## Contrato
financiero
H1, H2 y pago serializan lecturas de saldo y aplicaciones sobre el mismo
contrato. Ningún bloqueo espera interacción, HTTP ni exportación.
JDBC y ORMSeguridad y agregados por JDBC; entidades de negocio por Hibernate.
Auditoría financiera usa PreparedStatement en conexión de Session
mediante doWork, sin commit independiente (Hibernate, s. f.-c). Orden
financiero: puerta compartida, bloqueos, flush de pago/aplicaciones,
fn_reservar_numero (avanza contador), persistir recibo y flush, auditoría
JDBC, CALL sp_verificar_integridad y commit.
Contador y
## UUID
ReentrantLock local justo hasta commit/rollback; FOR UPDATE en BD
para clientes diferentes; claves únicas por recibo, solicitud de pago y
(liquidacion_id, orden) para los movimientos de liquidación.
Esperas y
reintentos
lock_timeout 2000 ms y statement_timeout 5000 ms para escrituras
ordinarias. SQLSTATE 40P01/40001: tres intentos totales (uno inicial y
dos reintentos), con esperas base de 100 y 250 ms más jitter de 0–50 ms
por espera, fuera de transacción. Cada intento abre Session y transacción
nuevas y conserva el mismo UUID; al agotar intentos, intervención. El
timeout propio de respaldo se configura por separado.

## 53
RecursoProtección y Frontera
H3 en LANConcesión en control_fx con fila UUID 00000000-0000-0000-0000-
000000000001; presupuesto compartido y presupuesto durable de cien
intentos/mes por instalación. Refresco manual usa caché mientras no
corresponda nueva consulta.
UI y cancelaciónProgreso por contrato o fase; Cancelar no bloquea JavaFX. Worker
CANCELLED no acredita rollback ni hilo TERMINATED; registro de
tareas confirma ambos recursos.
## H1 Generación Masiva
La Tabla 63 presenta contrato de Ejecución de H1.
## Tabla 63
Contrato de Ejecución de H1
CampoComportamiento
Interacción y
reanudación
Una transacción por contrato conserva el avance. El resumen durable es el
evento de bitacora M05 LOTE_RENTAS, con correlación y conteos de
generados, omitidos, fallidos y pendientes. Cancelar espera commit o
rollback del contrato en curso. Reanudar vuelve a consultar elegibles, omite
cualquier renta existente y conserva la política del nuevo lote identificada. La
cancelación voluntaria no genera ERR; MSG-059 mantiene su texto literal
del Brief y el resumen identifica los pendientes.
Fallo y
detención
Falla parcial revierte un contrato; pérdida de BD detiene el lote. No reanuda
sobre una Session fallida.
TrazabilidadFLU-20; RN-04, RN-20. Secuencia: UI, tarea, persistencia y servicio web si
interviene.
La Figura A9 del Apéndice A muestra secuencia y Estados de Ejecución de H1.
H2 Vencimientos y Mora
La Tabla 64 presenta contrato de Ejecución de H2.

## 54
## Tabla 64
Contrato de Ejecución de H2
CampoComportamiento
Interacción y
reanudación
El temporizador usa demora fija tras finalizar cada ciclo e indicador atómico
local para impedir solapamiento en la JVM. En LAN, H2 comparte bloqueo
de fila por contrato con H1 y pagos; no usa concesión de instalación. Solo H3
usa concesión mediante control_fx. Omite Mora de la base y crea recargo
único por renta. Tras suspensión procesa fecha operativa de now() de la BD
sin inventar ciclos pasados. H2 arranca tras validar el esquema en FLU-49; la
fábrica sistema('H2') de ContextoOperacion limita actor NULL a M05
MORA_AUTOMATICA, REVISION_VENCIMIENTO e INCIDENTE_H2,
con detalle.origen='H2'.
Fallo y
detención
Error por contrato deja evento y continúa; error global conserva corte previo,
termina ciclo y espera nuevo disparo.
TrazabilidadFLU-19, FLU-22; RN-05, RN-12, RN-20. Secuencia: UI, tarea, persistencia y
servicio web si interviene.
La Figura A10 del Apéndice A muestra secuencia y Estados de Ejecución de H2.
H3 Actualización de Referencias
La Tabla 65 presenta contrato de Ejecución de H3.
## Tabla 65
Contrato de Ejecución de H3
CampoComportamiento
Interacción y
reanudación
Un único consultor LAN adquiere concesión de instalación en control_fx
antes de enviar. La ventana será el mayor instante entre veinticuatro horas y
next_update. Cada intento reserva cuota compartida y se conserva tras
reinicio. La referencia aceptada no cambia la autorización por fecha ni
operaciones históricas. H3 arranca tras validar el esquema en FLU-49; la
fábrica sistema('H3') de ContextoOperacion limita actor NULL a M09
REFERENCIA_ACTUALIZADA, REFERENCIA_EN_CUARENTENA e
INCIDENTE_H3, con detalle.origen='H3'. autorizacion_tasa requiere actor
humano.
Fallo y
detención
Red, HTTP 408 o 5xx: un único reintento después de sesenta segundos; 429:
Retry-After o veinticuatro horas; 401/403 o cuota suspenden nuevas
consultas.
TrazabilidadFLU-35, FLU-36; RN-09, RN-22. Secuencia: UI, tarea, persistencia y
servicio web si interviene.
La Figura A11 del Apéndice A muestra secuencia y Estados de Ejecución de H3.

## 55
H4 Sección Crítica de Recibos
La Tabla 66 presenta contrato de Ejecución de H4.
## Tabla 66
Contrato de Ejecución de H4
CampoComportamiento
Interacción y
reanudación
H4 es una sección crítica de una tarea financiera, no un hilo permanente.
ReentrantLock justo protege el proceso local; la fila de consecutivo_recibo
con UUID 00000000-0000-0000-0000-000000000001 coordina clientes
distintos. El coordinador mantiene conexión y transacción únicas hasta
commit o rollback. El contador comienza en 000001 y no se reinicia cada
año. Tras puerta compartida y bloqueos, flush de pago/aplicaciones precede a
fn_reservar_numero, que avanza contador; después se persiste recibo y se
hace flush, auditoría JDBC, CALL sp_verificar_integridad y commit. El
rollback anterior al commit no consume número; la anulación posterior
conserva número e historial. nextval no sustituye el contador transaccional;
las variantes de aceptación D22 se separan en PV-09, sin nuevos CP.
Fallo y
detención
Fallo anterior al commit revierte número; estado incierto obliga a consultar
UUID. H6 espera la transacción y no interrumpe una escritura ya iniciada.
TrazabilidadFLU-24 a FLU-28; RN-07, RN-21. Secuencia: UI, tarea, persistencia y
servicio web si interviene.
La Figura A12 del Apéndice A muestra secuencia y Estados de Ejecución de H4.
H5 Generación y Exportación
La Tabla 67 presenta contrato de Ejecución de H5.
## Tabla 67
Contrato de Ejecución de H5
CampoComportamiento
Interacción y
reanudación
El registro de tareas conserva el recurso real además del estado Worker. La
consulta concluye antes de llenar la plantilla. La exportación escribe en un
temporal del volumen destino y publica al completar. Cancelar conserva el
archivo anterior y permite repetir con los mismos filtros cuando no queden
recursos activos. Durante consulta, llenado y exportación, la cancelación
cooperativa no genera ERR: MSG-089 solo tras terminar y limpiar
temporales antes de publicar. Si ya publicó, informa resultado real; conserva
destino previo hasta publicación exitosa y finanzas confirmadas.
Fallo y
detención
Generación fallida conserva parámetros; destino sin permiso permite otra
carpeta; exportador no interrumpible permanece visible hasta terminar.

## 56
CampoComportamiento
TrazabilidadFLU-28, FLU-31, FLU-38 a FLU-40; RN-28. Secuencia: UI, tarea,
persistencia y servicio web si interviene.
La Figura A13 del Apéndice A muestra secuencia y Estados de Ejecución de H5.
## H6 Detención Controlada
La Tabla 68 presenta contrato de Ejecución de H6.
## Tabla 68
Contrato de Ejecución de H6
CampoComportamiento
Interacción y
reanudación
El coordinador consume el cierre de ventana y trabaja fuera de JavaFX.
Detiene productores antes de esperar consumidores. Después de diez
segundos muestra PAN-40 y MSG-061 con tarea pendiente; conserva el
proceso hasta liberar recursos. Una bandera de cancelación no equivale a
cierre de conexiones ni a terminación del hilo.
Fallo y
detención
Fallo de cierre identifica recurso y mantiene proceso. La limpieza es
idempotente, registra causas y no utiliza Thread.stop ni salida forzada durante
escritura.
TrazabilidadFLU-48, FLU-49; RN-29, RN-30. Secuencia: UI, tarea, persistencia y
servicio web si interviene.
La Figura A14 del Apéndice A muestra secuencia y Estados de Ejecución de H6.
PostgreSQL conserva los bloqueos de fila hasta que termina la transacción que los
adquirió, por lo que FOR UPDATE permite coordinar contadores entre clientes (The
PostgreSQL Global Development Group, s. f.-a). Una secuencia basada en nextval puede dejar
huecos porque sus avances no se revierten, advertencia que descarta esa herramienta para RN-07
(The PostgreSQL Global Development Group, s. f.-b). El número solo será visible tras confirmar
todo el pago. El diagrama independiente compara dos usuarios y el Apéndice A amplía la rama
de rollback y recuperación por UUID.
La Figura 17 muestra dos Usuarios y un Consecutivo Transaccional.

## 57
## Figura 17
Dos Usuarios y un Consecutivo Transaccional
Flujos de Integración con el Servicio Web
ExchangeRate-API de AYR Tech admite el código NIO y solicitudes estándar con base
USD (AYR Tech (Pty) Ltd., s. f.-c, s. f.-d). El plan Free publica actualización diaria y mil
quinientas solicitudes mensuales, condiciones consultadas sin contratar una cuenta (AYR Tech
(Pty) Ltd., s. f.-b). Los términos permiten caché interna, restringen redistribución y desaconsejan
usar la referencia informativa para transacciones (AYR Tech (Pty) Ltd., s. f.-e). Latch conserva
el proveedor del PRD para RA5 y exige una tasa autorizada por fecha para registrar pagos entre
monedas.
La Tabla 69 presenta selección del Proveedor y Referencia Institucional.

## 58
## Tabla 69
Selección del Proveedor y Referencia Institucional
OpciónEvidencia y Decisión
ExchangeRate-
API Free
Seleccionado por continuidad con PRD/TRD, catálogo NIO y contrato
explícito. Llave requerida; 1500 solicitudes/mes; actualización diaria.
Presupuesto interno de cien intentos compartidos, sin prometer SLA.
## Open Exchange
## Rates Free
Catálogo público contiene NIO y plan anuncia mil consultas mensuales
con actualización horaria (Open Exchange Rates, s. f.-a, s. f.-b).
Alternativa reconocida; no se adopta porque no se necesita cambiar el
proveedor compatible recibido.
FXRatesAPI /
CurrencyFreaks
Candidatos iniciales no adoptados. No se les atribuyen condiciones,
cobertura o reputación sin comprobación; la comparación utiliza dos
proveedores con documentación original accesible.
Banco Central de
## Nicaragua
El comunicado del 20 de octubre de 2025 fija deslizamiento cero y
referencia oficial 36.6243 NIO/USD para 2026 (Banco Central de
Nicaragua, 2025). Fuente para autorización fundamentada; no se inventa
API JSON del BCN.
El cliente reutilizable de Java 21 permite enviar solicitudes y procesar respuestas
mediante futuros, según la API oficial de Oracle (s. f.-a). La solicitud será GET a
https://v6.exchangerate-api.com/v6/latest/USD, con Accept application/json y Authorization
Bearer desde una llave externa, método documentado por AYR Tech (Pty) Ltd. (s. f.-a). H3
valida result, base_code y conversion_rates.NIO antes de guardar una versión. El adaptador
conserva metadatos de fecha y descarta cuerpos mayores de 256 KiB, marcas incoherentes o
valores comerciales fuera de la banda técnica definida.
La Figura 18 muestra respuesta Externa Caché y Autorización.

## 59
## Figura 18
Respuesta Externa Caché y Autorización
La Tabla 70 presenta contrato JSON y Política de Vigencia.
## Tabla 70
Contrato JSON y Política de Vigencia
Campo o ReglaValidación Adoptada
result / base_codesuccess y USD; result=error se procesa aun con HTTP 200, sin asumir
éxito de negocio.
conversion_rates.
## NIO
Número decimal positivo; banda técnica informativa de 1 a 100
NIO/USD, inclusive. Fuera de banda no se guarda como referencia
vigente: se registra en bitacora el evento M09
REFERENCIA_EN_CUARENTENA con valor y huella; no se crea tabla
de cuarentena.
last_update /
next_update
Instantes interpretables; next_update > last_update; no más de cinco
minutos de futuro frente a reloj contrastado con BD.
CachéGuardar fetched_at, actualizaciones del proveedor, base, destino, valor y
procedencia. Identificar histórica si fecha local difiere de la del pago; no
modifica autorizaciones.
Programación y
presupuesto
Máximo entre ventana de 24 h y next_update. Mes de 31 días: hasta 31
intentos ordinarios y 31 reintentos; 62 < presupuesto interno 100 < cuota
publicada 1500.
Tasa autorizadaRegistro único e inmutable por fecha efectiva (UNIQUE), fuente, motivo,
valor NIO/USD, UUID y actor Propietario; copia inmutable en cada
operación. El sistema no precarga tasa inventada ni promueve JSON
comercial a oficial. Una tasa manual fuera de la banda comercial requiere
revisión y confirmación del Propietario, sin CHECK de banda en la BD.
Secretos y
privacidad
Llave externa LATCH_FX_API_KEY solo en consultor designado;
redactar Authorization. El servicio recibe códigos de moneda, sin
nombres, contratos ni importes de personas.
La Tabla 71 presenta respuestas Visibles y Recuperación de la Integración.

## 60
## Tabla 71
Respuestas Visibles y Recuperación de la Integración
CasoQué Ve el UsuarioQué Hace el Sistema
ÉxitoMSG-040 y MSG-069; fuente
y próxima actualización
Guardar referencia inmutable; autorización
sigue separada.
Sin conexiónMSG-042; última consulta o
ausencia explícita
Conservar caché; un reintento después de 60
s.
## Tiempo
agotado
MSG-042 con contexto de
espera agotada
Conexión 5 s; total 10 s; cancelar y un
reintento de red a 60 s.
HTTP 4xxMSG-043 o texto de
autorización de ERR-040
401/403 y llave inválida suspenden; otros
4xx sin reintento automático, excepto HTTP
408: timeout transitorio, conserva caché y
admite un único reintento tras 60 s. HTTP
429 sigue su política de cuota y Retry-After.
HTTP 5xxMSG-042; proveedor no
disponible
Un reintento a 60 s; luego esperar ventana
siguiente.
429 / cuotaAviso ERR-043 de límite
alcanzado
429 respeta Retry-After, sin cabecera espera
24 h; quota-reached suspende hasta cuota
disponible.
## JSON
inválido
MSG-043; historial
conservado
Rechazar respuesta completa; sin sustituir
caché.
Tasa fuera de
banda
MSG-043 con contexto de
revisión
No guardar referencia vigente; registrar
bitacora M09
REFERENCIA_EN_CUARENTENA con
valor y huella, sin nueva tabla. Propietario
revisa fuente.
## Manual
autorizada
MSG-070 con fuente y fecha
efectiva
Guardar registro único e inmutable por fecha
efectiva mediante FLU-37 y permitir
conversión de esa fecha.
## Conversión
sin
autorización
MSG-027 o MSG-065Bloquear conversión antes de H4; permitir
operaciones de misma moneda.
Flujo de Reportes y Exportación
JasperReports 7.0.8 está publicado en el repositorio oficial y declara bytecode objetivo
Java 8, compatible por diseño con el runtime Java 21 elegido, sujeto a prueba de integración del
paquete (Jaspersoft, s. f.-a). Su API incluye JRXlsxExporter para producir XLSX (Jaspersoft, s.
f.-b). Latch conserva las plantillas de la misma versión y transmite DTO sin entidades abiertas.

## 61
La salida fija corte, filtros y moneda; H5 genera el documento después de cerrar la consulta. El
archivo de un recibo puede regenerarse sin repetir la transacción que registró el pago.
La Figura 19 muestra parámetros Vista Previa y Archivo Final.
## Figura 19
Parámetros Vista Previa y Archivo Final
La Tabla 72 presenta catálogo y Parámetros de Reportes.
## Tabla 72
Catálogo y Parámetros de Reportes
Reporte del PRDParámetros y Contenido Obligatorio
REP-01 ReciboPago confirmado; consecutivo, emisor histórico, contrato, original,
equivalente, tasa, aplicaciones y crédito. Marca Anulado si
corresponde.
REP-02 Estado de
cuenta
Contrato y corte; cargos, abonos, anulaciones, saldo, crédito y
moneda; no sustituir movimientos históricos por saldo actual.
REP-03 Cartera
vencida
Corte y propiedad; principal, mora y antigüedad por contrato. Tramos
1 a 30, 31 a 60, 61 a 90 y más de 90 días.
REP-04 Ingresos y
gastos
Período, propiedad y moneda; cobros netos y gastos; garantía
separada y anulados identificados.
REP-05 OcupaciónPropiedad y tipo; ocupadas sobre activas al instante de generación, sin
selector de fecha histórica. Inactivas fuera del denominador; En
mantenimiento sigue unidad activa. El modelo no conserva historia
del estado de unidad.
REP-06 Contratos por
vencer
Corte y umbral; activos con cero a treinta días por defecto, unidad e
inquilino.
REP-07 Historial de
pagos
Inquilino, contrato y fechas; recibos Registrado y Anulado; netos sin
pagos anulados.
REP-08 GarantíasContrato y corte; inicial, aplicaciones, devoluciones, conceptos y
remanente.
## REP-09
## Mantenimiento
Propiedad, unidad, período, estado y prioridad; responsable, costo y
moneda.

## 62
Reporte del PRDParámetros y Contenido Obligatorio
REP-10 Tasas
utilizadas
Fechas y origen; pagos, tasa congelada, fecha efectiva y autorizador.
Sin redistribución del catálogo de la API.
FileChooser selecciona el archivo individual y DirectoryChooser la carpeta de un lote,
con nombre y formato confirmados antes de iniciar escritura. El diálogo de colisión exige
Reemplazar o Cancelar y conserva el destino previo durante la generación. H5 escribe en un
temporal del mismo volumen y publica después de completar el exportador; si no hay cambio
atómico disponible, conserva el original hasta verificar la copia final. Los errores de permiso o
espacio mantienen parámetros y permiten otra ubicación, mientras cancelar elimina únicamente
el temporal incompleto de esa tarea.
Máquinas de Estado
Las máquinas conservan exactamente los valores del catálogo compartido y distinguen
estado persistido de información derivada por saldo o fecha. El contrato activo incluye Vigente,
Por vencer y Vencido pendiente de cierre, por lo que una transición temporal no libera la unidad.
Pago y Recibo comparten Registrado y Anulado y cambian juntos dentro de una transacción
autorizada. En las tablas, Ambos designa los dos roles y Sistema identifica H2. Los estados
terminales conservan consultas históricas y carecen de una transición de reactivación financiera
que modifique documentos ya confirmados.
## Contrato
La Figura 20 muestra máquina de Estado de Contrato.

## 63
## Figura 20
Máquina de Estado de Contrato
La Tabla 73 presenta transiciones de Contrato.
## Tabla 73
Transiciones de Contrato
OrigenEventoCondiciónDestinoAcciónRol
BorradorActivarCompleto; sin
ocupación
superpuesta
VigenteCrear garantía y
ocupar unidad
## Ambos
BorradorCancelarMotivo; sin
movimientos
CanceladoConservar
borrador histórico
## Ambos
VigenteRevisión
## H2
0 a 30 días
hasta fin
Por vencerActualizar estado
y alerta
## Sistema
Por vencerRevisión
## H2
## Umbral
reducido; días
mayores al
umbral
VigenteRetirar alerta;
conservar fechas
## Sistema
## Vigente /
Por vencer
## Revisión
## H2
Fecha operativa
posterior al fin
VencidoReservar unidad;
conservar deuda
## Sistema
## Vigente /
Por vencer /
## Vencido
TerminarFecha, motivo y
garantía
liquidada
TerminadoLiberar unidad;
conservar deuda
## Ambos;
garantía P
## Vigente /
Por vencer /
## Vencido
## Terminar
anterior y
activar
sucesor
Sucesor listo y
fecha no
superpuesta
TerminadoActivar sucesor en
## Vigente
## Ambos;
garantía P
## Terminado /
## Cancelado
ConsultarSin reactivación
ni edición
financiera
## Mismo
estado
Leer historiaAmbos

## 64
## Unidad
La Figura 21 muestra máquina de Estado de Unidad.
## Figura 21
Máquina de Estado de Unidad
La Tabla 74 presenta transiciones de Unidad.
## Tabla 74
Transiciones de Unidad
OrigenEventoCondiciónDestinoAcciónRol
DisponibleActivar
contrato
## Unidad
bloqueada y sin
conflicto
OcupadaDerivar ocupaciónAmbos por
## M04
OcupadaTerminar
contrato
Sin otro
contrato activo
DisponibleLiberar ocupaciónAmbos por
## M04
DisponibleRestringirSin contrato
activo
## En
mantenimie
nto
Excluir nuevas
activaciones
## Propietario
## En
mantenimie
nto
## Restablec
er
## Trabajo
concluido y
revisión
DisponibleHabilitar
selección
## Propietario
## Disponible /
## En
mantenimie
nto
Dar de
baja
Sin contrato
activo
InactivaConservar
historial
## Propietario
InactivaReactivarRevisión; sin
contrato activo
DisponibleAuditar
reactivación
## Propietario
OcupadaSolicitud
de trabajo
Contrato sigue
activo
OcupadaCrear solicitud sin
desocupar
## Ambos

## 65
## Cargo
La Figura 22 muestra máquina de Estado de Cargo.
## Figura 22
Máquina de Estado de Cargo
La Tabla 75 presenta transiciones de Cargo.
## Tabla 75
Transiciones de Cargo
OrigenEventoCondiciónDestinoAcciónRol
PendienteAbono0 < aplicado <
importe; no
vencido
ParcialReducir saldoAmbos
## Pendiente /
## Parcial
## Pasar
vencimien
to
Fecha posterior;
saldo > 0
VencidoActualizar estado
por H2
## Sistema
## Pendiente /
## Parcial /
## Vencido
## Pagar
saldo
Saldo resultante
## = 0
PagadoRegistrar
aplicación
## Ambos
VencidoAbono
parcial
Saldo resultante
## > 0
VencidoMostrar abono y
saldo
## Ambos
## Pendiente /
## Parcial /
## Vencido /
## Pagado
## Anular
cargo
Sin aplicaciones
activas; motivo
AnuladoConservar clave
de renta
## Propietario
## Pagado /
## Parcial /
## Vencido
## Anular
pago
## Reversión
autorizada de
aplicaciones
## Estado
derivado
Aplicar prioridad
## RN-24
## Propietario
AnuladoConsultarEstado terminalAnuladoConservar importe
histórico
## Ambos

## 66
## Pago
La Figura 23 muestra máquina de Estado de Pago.
## Figura 23
Máquina de Estado de Pago
La Tabla 76 presenta transiciones de Pago.
## Tabla 76
Transiciones de Pago
OrigenEventoCondiciónDestinoAcciónRol
RegistradoAnular
recibo
Motivo; sin
dependencias
activas
AnuladoRevertir
aplicaciones y
crédito
## Propietario
RegistradoReimprim
ir /
consultar
## UUID
## Pago
confirmado
RegistradoRecuperar mismo
recibo
## Ambos
AnuladoReimprim
ir / repetir
anulación
## Historia
inmutable
AnuladoMostrar motivo
sin revertir otra
vez
## Ambos
consultan
## Recibo
La Figura 24 muestra máquina de Estado de Recibo.
## Figura 24
Máquina de Estado de Recibo

## 67
La Tabla 77 presenta transiciones de Recibo.
## Tabla 77
Transiciones de Recibo
OrigenEventoCondiciónDestinoAcciónRol
RegistradoAnular
pago
## Misma
transacción del
pago
AnuladoConservar
número, motivo y
actor
## Propietario
RegistradoFallar
## PDF
Pago sigue
confirmado
RegistradoReintentar H5 sin
tomar H4
## Ambos
## Registrado /
## Anulado
## Reimprim
ir
## Usar
instantánea y
estado vigente
## Mismo
estado
Generar copia
identificable
## Ambos
## Depósito
La Figura 25 muestra máquina de Estado de Depósito.
## Figura 25
Máquina de Estado de Depósito
La Tabla 78 presenta transiciones de Depósito.
## Tabla 78
Transiciones de Depósito
OrigenEventoCondiciónDestinoAcciónRol
RetenidoAplicar
parte
## Remanente > 0;
sin devolución
RetenidoCrear aplicación
con concepto
## Propietario
RetenidoAplicar
todo
## Remanente = 0;
sin devolución
AplicadoCerrar fondos por
deuda
## Propietario

## 68
OrigenEventoCondiciónDestinoAcciónRol
RetenidoDevolver
parte
## Devolución > 0
y remanente > 0
## Devuelto
parcial
## Registrar
devolución
## Propietario
## Retenido /
## Devuelto
parcial
## Agotar
con
devolució
n
## Remanente = 0;
devolución
acumulada > 0
DevueltoRegistrar último
movimiento
## Propietario
## Devuelto
parcial
Aplicar o
devolver
parte
Remanente > 0Devuelto
parcial
## Conservar
identidad
financiera
## Propietario
## Aplicado /
## Devuelto
ConsultarSin fondos
remanentes
## Mismo
estado
## Mostrar
liquidación
## Ambos
Solicitud de Mantenimiento
La Figura 26 muestra máquina de Estado de Solicitud de Mantenimiento.
## Figura 26
Máquina de Estado de Solicitud de Mantenimiento
La Tabla 79 presenta transiciones de Solicitud de Mantenimiento.
## Tabla 79
Transiciones de Solicitud de Mantenimiento
OrigenEventoCondiciónDestinoAcciónRol
AbiertaIniciar
atención
## Responsable
asignado
En procesoRegistrar fecha de
inicio
## Ambos
AbiertaCancelarMotivo
confirmado
CanceladaConservar
solicitud
## Ambos
En procesoResolverTrabajo
descrito; costo
válido
ResueltaRegistrar gasto
efectivo
## Ambos

## 69
OrigenEventoCondiciónDestinoAcciónRol
En procesoCancelarMotivo; gasto
previo
conservado
CanceladaConservar gasto e
historia
## Ambos
## Resuelta /
## Cancelada
ConsultarEstados
terminales
## Mismo
estado
Leer detalle sin
reabrir
## Ambos
Nota. Mismo estado indica autorrecorrido en el estado origen, sin añadir un valor al catálogo.
Estado derivado en Cargo aplica RN-24: Anulado prevalece, luego Pagado si saldo cero,
Vencido si saldo positivo y fecha pasada, Parcial si hay abono sin vencimiento y Pendiente en el
resto. Las barras separan varios estados de origen permitidos.
Catálogo de Errores y Excepciones
El catálogo identifica situación, capa de origen, mensaje y recuperación para que los
controladores no traduzcan una excepción técnica en una respuesta ambigua. Los códigos MSG
remiten al texto exacto del Apéndice B del Brief corregido, cuyo catálogo comprende 89
mensajes (MSG-001 a MSG-089); no se mantienen textos propuestos fuera del catálogo. Un
contexto adicional puede identificar timeout o cuota sin revelar SQLSTATE, credenciales o rutas
privadas. El usuario conserva los datos válidos y recibe una acción disponible. Los fallos
posteriores al commit mantienen la operación financiera y habilitan solo recuperación o
exportación del resultado confirmado.
La Tabla 80 presenta errores y Recuperaciones Operativas.
## Tabla 80
Errores y Recuperaciones Operativas
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-001
Configuración de
BD ausente
servicioMSG-071: Falta la
configuración de
conexión. Revise la
instalación y vuelva a
intentar.
No crear pool; conservar
diagnóstico sin secreto. Corregir
configuración y reintentar arranque.

## 70
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-002
Base no
disponible al
arrancar
base de
datos
MSG-006Cerrar recursos parciales;
Reintentar o Salir. Sin acceso hasta
conexión válida.
## ERR-003
## Esquema
incompatible
base de
datos
MSG-072: La base de
datos requiere una
versión compatible del
esquema. Cierre y
revise la instalación.
No migrar implícitamente; cerrar
fábrica y pool. Aplicar paquete
compatible bajo mantenimiento.
## ERR-004
## Recurso Aurora
ausente
interfazMSG-073: Se usará el
fondo simplificado
porque no se pudo
cargar la decoración.
Continuar con CSS funcional local
y superficies uniformes; conservar
foco.
## ERR-005
## Preferencia
ausente o
inválida
interfazSin alerta intrusiva;
mostrar Claro en
selector.
Restaurar Claro y efectos normales
si falla simple, conforme al brief;
permitir cambio personal.
## ERR-006
## Guardar
preferencia falla
interfazMSG-003Conservar sesión, avisar una vez;
intentar de nuevo en próximo
cambio.
## ERR-007
## Credencial
incorrecta
servicioMSG-001Actualizar contador de forma
atómica; permitir corregir sin
revelar cuenta.
## ERR-008
Quinto fallo o
bloqueo activo
servicioMSG-002Bloquear quince minutos; esperar o
revisión de otro Propietario.
## ERR-009
Acción fuera de
rol
servicioMSG-062Rechazar antes de escribir;
refrescar permisos y navegación.
## ERR-010
Nombre de
usuario duplicado
base de
datos
MSG-053Capturar restricción única
normalizada; conservar formulario
y cambiar nombre.
## ERR-011
## Último
Propietario activo
servicioMSG-054Bloquear conjunto administrativo y
revertir cambio. Mantener al menos
uno activo.
## ERR-012
Contraseña no
coincide o es
insuficiente
interfazMSG-074: Use de 12 a
128 caracteres y repita
la misma contraseña.
No guardar hash; conservar solo
campos no secretos y reingresar
contraseña.
## ERR-013
Cambio sin
guardar al
navegar
interfazMSG-005PAN-39 ofrece Guardar, Descartar
o Continuar editando; no cambiar
destino antes de resolver.

## 71
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-014
Propiedad con
unidades activas
servicioMSG-008Denegar baja; abrir lista de
unidades dependientes.
## ERR-015
Código de unidad
duplicado
base de
datos
MSG-010Rollback y conservación de
campos; corregir código dentro de
propiedad.
## ERR-016
Baja de unidad
con contrato
activo
servicioMSG-011Denegar y mostrar contrato
asociado; cerrar contrato antes de
baja.
## ERR-017
Datos mínimos
de inquilino
interfazMSG-013Foco al campo ausente; conservar
datos válidos.
## ERR-018
Correo inválido
proporcionado
interfazMSG-014Corregir formato o retirar correo
opcional; no borrar nombre.
## ERR-019
Adjunto inválido
o mayor a cinco
MiB
servicioMSG-063Validar firma y tamaño; eliminar
temporal rechazado y elegir archivo
permitido.
## ERR-020
## Duración
contractual
menor a un mes
servicioMSG-019Calcular inicio.plusMonths(1);
conservar Borrador y corregir fecha
final.
## ERR-021
Canon no
positivo
interfazMSG-020Rechazar importe; foco en canon
sin borrar moneda.
## ERR-022
Día de pago fuera
de uno a treinta y
uno
interfazMSG-021Corregir día; ajuste al último día
del mes ocurre solo para valores
válidos.
## ERR-023
## Ocupación
superpuesta al
activar
base de
datos
MSG-022Revertir contrato y depósito; releer
unidad y elegir intervalo libre.
## ERR-024
Renta del período
ya existente
base de
datos
MSG-060; detalle del
resumen: omitido.
Tratar unicidad como omisión
idempotente, incluso si renta
Anulado; continuar H1.
## ERR-025
Fallo parcial de
## H1
base de
datos
MSG-060; detalle del
resumen: fallido.
Rollback del contrato, registrar
correlación; reintentar solo
pendientes/fallidos tras revisar
causa.

## 72
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-026
Cargo con
aplicaciones
activas
servicioMSG-024Rechazar anulación; revisar y
revertir dependencias autorizadas.
## ERR-027
Monto recibido
no positivo
interfazMSG-026Rechazar antes de H4; conservar
contrato y método.
## ERR-028
Falta tasa
autorizada para
fecha
servicioMSG-027Bloquear pago entre monedas;
Propietario autoriza por PAN-32.
## ERR-029
## Aplicación
supera saldo
actual
servicioMSG-028Releer bajo bloqueo; mostrar saldo
nuevo y exigir reconfirmar
distribución.
## ERR-030
UUID repetido
con otra huella
servicioMSG-075: Esta
solicitud ya
corresponde a otros
datos. Consulte el
pago antes de
continuar.
No reutilizar ni generar pago;
recuperar original y exigir nueva
intención explícita con UUID
nuevo.
## ERR-031
Commit de pago
con respuesta
perdida
redMSG-064Conservar UUID; nueva conexión
consulta estado y espera resolución
antes de reintentar.
## ERR-032
Motivo de
anulación vacío
interfazMSG-031Mantener PAN-29 y foco en
motivo; sin cambios financieros.
## ERR-033
## Crédito
consumido
posteriormente
servicioMSG-032Bloquear anulación; identificar
dependencias y revertir por orden
inverso autorizado.
## ERR-034
## Recibo
confirmado sin
## PDF
servicioMSG-033Conservar pago y número; repetir
solo H5 desde PAN-11.
## ERR-035
Deducción de
depósito sin
concepto
interfazMSG-034Conservar propuesta; corregir cada
concepto antes de liquidar.
## ERR-036
## Liquidación
excede remanente
servicioMSG-035Rollback de paquete; releer
depósito y mostrar remanente
vigente.

## 73
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-037
Solicitud sin
descripción o
unidad
interfazMSG-037Foco en dato ausente; conservar
costo y responsable aún no
persistidos.
## ERR-038
Proveedor sin
conexión
redMSG-042Conservar caché; un reintento a
sesenta segundos si ventana
permite; funciones locales
continúan.
## ERR-039
Tiempo HTTP
agotado
redMSG-042; contexto:
tiempo de espera
agotado.
Cancelar solicitud a diez segundos;
un reintento de red tras sesenta
segundos y conservar caché.
## ERR-040
HTTP 401, 403 o
llave inválida
servicioMSG-076: No se pudo
autorizar la consulta.
Solicite revisión de la
configuración.
Suspender H3; redactar
Authorization en log y reanudar
solo tras corrección.
## ERR-041
HTTP 400, 404 u
otro 4xx no
transitorio
(excepto 408 y
## 429)
servicioMSG-043; contexto:
solicitud rechazada.
Sin reintento automático; revisar
contrato de endpoint y conservar
referencia anterior.
## ERR-042
HTTP 5xx  o
## HTTP 408
transitorio
servicioMSG-042; contexto:
proveedor no
disponible.
Un reintento después de sesenta
segundos; luego esperar próxima
ventana.
## ERR-043
HTTP 429 o
quota-reached
servicioMSG-077: La consulta
alcanzó su límite de
uso. Se conserva la
última referencia
disponible.
Respetar Retry-After para 429 o
esperar veinticuatro horas si falta;
quota-reached suspende hasta
nuevo período/corrección.
## ERR-044
JSON inválido,
base distinta o sin
## NIO
servicioMSG-043Rechazar versión completa; no
reemplazar caché ni crear tasa
autorizada.
## ERR-045
Tasa comercial
fuera de banda
técnica
servicioMSG-043; contexto:
valor requiere
revisión.
Si valor queda fuera de 1–100
NIO/USD, inclusive: no guardar
referencia vigente; evento bitacora
## M09
## REFERENCIA_EN_CUARENTE
NA con valor y huella, sin tabla
nueva; conservar historial y
solicitar revisión del Propietario.

## 74
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-046
## Referencia
comercial
vencida
servicioMSG-041Mostrar fetched_at y next_update;
no promover referencia histórica a
autorización de pago.
## ERR-047
Filtro de fechas
invertido
interfazMSG-017Conservar selección; corregir fin
antes de ejecutar consulta o H5.
## ERR-048
Reporte sin
contrato
obligatorio
interfazMSG-045Foco en contrato; ningún trabajo de
generación iniciado.
## ERR-049
Error al llenar
plantilla Jasper
servicioMSG-046Cerrar recursos, conservar
parámetros y permitir repetir H5;
pago previo intacto.
## ERR-050
Carpeta sin
permiso o disco
lleno
servicioMSG-047No publicar temporal incompleto;
elegir otra ubicación sin registrar
otro pago.
## ERR-051
Archivo de
destino existente
interfazMSG-048Esperar Reemplazar o Cancelar;
conservar destino original hasta
publicación exitosa.
## ERR-052
Conflicto de
versión de
entidad
base de
datos
MSG-078: Otro
usuario cambió este
registro. Compare los
datos actuales antes de
guardar.
Rollback; conservar edición local
en memoria y presentar versión
actual para recarga.
## ERR-053
Interbloqueo o
serialización
abortada
base de
datos
MSG-006; contexto:
operación debe
reintentarse.
SQLSTATE 40P01/40001: tres
intentos totales (1 + 2 reintentos),
esperas base 100 y 250 ms más
jitter 0–50 ms fuera de transacción;
Session y transacción nuevas en
cada intento, mismo UUID. Tras
agotarlos, intervención.
## ERR-054
## Desconexión
durante escritura
redMSG-064 para
finanzas; MSG-006
para consulta.
Si commit incierto consultar UUID;
si rollback confirmado permitir
reintento controlado.
## ERR-055
Respaldo con
hash incorrecto
servicioMSG-079: El respaldo
no superó la
comprobación de
integridad. Seleccione
otro paquete.
Rechazar antes de restaurar;
conservar instalación y copia
previa.
## ERR-056
Cierre supera
diez segundos
servicioMSG-061Mostrar tarea pendiente en
PAN-40, mantener proceso y
esperar liberación real.

## 75
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-057
Reloj local
difiere de BD
servicioMSG-080: La fecha
del equipo difiere de la
base de datos. Revise
el reloj antes de
registrar operaciones.
Obtener instante y fecha operativa
financiera de now() de la BD (fecha
en America/Managua). Si |reloj
local − BD| > 5 min, bloquear
nuevas operaciones financieras con
ERR-057 / MSG-080 hasta
corregir.
## ERR-058
Restauración con
recibos
posteriores al
respaldo
servicioMSG-081: La
restauración requiere
conciliar los recibos
posteriores al respaldo
antes de volver a
operar.
Mantener mantenimiento; recuperar
respaldo más reciente o
reconstrucción comprobada sin
renumerar ni reutilizar
consecutivos.
## ERR-059 /
Autenticidad o
contraseña del
paquete inválida
servicioMSG-082: No se pudo
verificar la
autenticidad del
respaldo. Revise la
contraseña o
seleccione otro
paquete.
Rechazar cifrado o autenticidad
AES-256-GCM inválida; no
publicar el paquete ni reemplazar la
instalación. Conservar copia previa
y permitir corregir contraseña o
seleccionar otro paquete.
ERR-060 / Tasa
duplicada por
fecha efectiva
servicio /
base de
datos
MSG-083: Ya existe
una tasa autorizada
para esa fecha
DuplicadoException ante UNIQUE
de autorizacion_tasa por fecha
efectiva; rollback sin otro registro.
Consultar la autorización inmutable
existente; corrección solo por
procedimiento excepcional.
ERR-061 / Cargo
incompatible para
aplicación de
garantía
servicioMSG-084CargoIncompatibleException:
cargo inexistente, anulado o de otro
contrato. Rechazar aplicación y
conservar propuesta; exceso de
saldo mantiene su error específico.
## ERR-062 /
Liquidación de
garantía
pendiente
servicioMSG-085GarantiaPendienteException: cierre
bloqueado mientras falten
decisiones, movimientos o
remanente por resolver. Conservar
propuesta; permitir deuda residual
después de terminar y respetar
garantía inicial cero sin
movimientos (DC-08).

## 76
Identificador y
## Situación
OrigenMensaje al UsuarioAcción Técnica y Recuperación
## ERR-063 /
## Inconsistencia
financiera
anterior al
commit
servicio /
base de
datos
MSG-086 solo con
rollback confirmado
IntegridadException: revertir toda
la operación y conservar causa
técnica. Commit incierto: ERR-031
o ERR-054 / MSG-064, sin afirmar
ausencia de cambios. Hash de
respaldo: ERR-055 / MSG-079;
autenticidad o contraseña:
## ERR-059 / MSG-082.
ERR-064 / Días o
montos
configurables
negativos
servicioMSG-051ParametroInvalidoException:
rechazar el rango negativo cubierto
por MSG-051 y conservar
formulario para corregir. Las
demás validaciones mantienen sus
mensajes específicos.
## ERR-065 /
## Propiedad
incompatible con
solicitud
servicioMSG-087PropiedadIncompatibleException:
al registrar gasto, la unidad de la
solicitud pertenece a otra
propiedad. Rechazar registro y
permitir corregir propiedad o
solicitud.
## ERR-066 /
Transición de
mantenimiento
no permitida
servicioMSG-088TransicionInvalidaException: solo
Abierta a En proceso o Cancelada,
y En proceso a Resuelta o
Cancelada. Estados Resuelta y
Cancelada terminales; repetir
estado actual es idempotente, sin
cambios ni eventos nuevos.
Rechazar otras transiciones
conservando estado anterior.
Escenarios Límite y Casos Especiales
Los escenarios límite precisan qué debe permanecer consistente cuando cambian fechas,
recursos o acciones de dos usuarios. La aceptación comprobará importes, estados, consecutivos y
recursos liberados mediante datos sintéticos, sin interpretar el texto de un mensaje como prueba
suficiente de persistencia. Una revisión del contador debe consultar la base y el recibo
confirmado, mientras una cancelación debe observar el trabajo real después de terminar. Los
casos de reloj y restauración limitan la operación cuando falta confianza en el corte temporal o
en la continuidad de la historia financiera disponible.

## 77
La Tabla 81 presenta casos Especiales y Comportamiento Esperado.
## Tabla 81
Casos Especiales y Comportamiento Esperado
CasoEscenarioResultado Esperado
EL-01Contrato vence el
día de pago
El día final sigue Por vencer si está dentro del umbral; recibe
pago. Desde el siguiente día pasa a Vencido y reserva unidad
hasta cierre. La fecha final inclusiva no altera la duración
mínima fin >= inicio + 1 mes: un mes natural completo (del
día 1 al 31) exige fin >= día 1 del mes siguiente.
EL-02Pago exacto del
saldo
Aplicaciones igualan equivalente; crédito cero, cargo Pagado
y un recibo confirmado.
EL-03Pago mayor al saldoAplicar deuda vencida y guardar excedente en moneda
contractual; siguiente cargo usa crédito sin otro recibo.
EL-04Cambio de tasa
entre cargo y pago
Usar tasa autorizada de fecha del pago, guardar copia y
mantener canon del cargo. Nueva referencia no reexpresa
movimientos históricos.
EL-05Dos usuarios pagan
simultáneamente
Contrato y cargos bloqueados serializan distribución;
propuesta manual obsoleta exige reconfirmación; contador
entrega números contiguos confirmados.
EL-06Cerrar durante H1Cancelar entre contratos; el contrato en curso termina con
commit o rollback y H6 espera antes de cerrar pool.
EL-07Reloj del sistema
incorrecto
Comparar reloj con BD; más de cinco minutos bloquea
finanzas y señala revisión. Instante y fecha operativa
proceden de now() de la BD; fecha en America/Managua e
instante UTC en auditoría.
EL-08BD desconectada a
mitad de operación
Revertir si resultado conocido; ante commit incierto
conservar UUID, consultar con sesión nueva y no duplicar
intención.
EL-09Día treinta y uno en
febrero
Vencimiento usa último día de febrero. Duración mínima se
valida con mes calendario y no con treinta días fijos. Se
conserva fin >= inicio + 1 mes; para un mes natural
completo, fin debe alcanzar al menos el día 1 del mes
siguiente.
EL-10Excedente ya
aplicado al siguiente
cargo
RN-31 impide anular pago fuente hasta revertir esa
aplicación autorizada; conserva número e historia.
EL-11Unidad con contrato
## Vencido
Permanece Ocupada mientras no cierre explícito; no se
activa otro contrato sobre ese espacio reservado.
EL-12Depósito con
devolución y
aplicación final
Si remanente cero y existe cualquier devolución, Devuelto; si
solo aplicaciones, Aplicado. Depósito no se suma a renta
cobrada.

## 78
CasoEscenarioResultado Esperado
EL-13Cuenta cambia de
rol con diálogo
abierto
Servicio relee permiso antes de escribir y deniega comando
administrativo ahora inválido; conserva formulario sin
confirmar.
EL-14Cambio de tema en
pago no confirmado
Conservar monto, moneda, tasa propuesta, UUID, selección
y foco. Sin efectos solo retira decoración.
EL-15Sin Internet en
primer arranque
Acceso y operaciones de misma moneda disponibles con
BD; MSG-065 impide conversión sin tasa autorizada por
fecha.
EL-16Repetición de H1
sobre renta Anulado
Omitir clave contrato/período ya existente; corrección
requiere movimiento autorizado con referencia, no segunda
renta.
EL-17Fallo PDF después
del commit
Pago y número siguen confirmados. PAN-11 permite generar
otra vez sin repetir H4.
EL-18Restaurar copia
anterior a recibos
emitidos
Mantenimiento continúa hasta conciliar evidencia posterior;
garantía de consecutivos se limita a base activa y ruta
controlada de escritura.
EL-19BD se recupera
después de
suspensión
Revalidar pool, fecha operativa de BD, indicador atómico
local y bloqueos por contrato de H2, y concesión de H3 en
control_fx; H2 revisa estado vigente y H3 respeta cuota y
próxima actualización persistidas.
EL-20Cero unidades o
reporte sin filas
Ocupación cero con denominador visible; reporte conserva
filtros y encabezados con MSG-049; no divide entre cero.
## Trazabilidad
La matriz revisada vincula los 49 flujos con los requisitos funcionales del PRD y las
pantallas del Brief (Cuadra et al., 2026a, 2026c). Cada fila coincide con los campos Reglas y
Ejecución y Datos y Pantallas de su FLU tras las correcciones. M02 conserva sus RF y PAN con
el alcance de Asistente limitado a consulta y edición de condiciones/foto; FLU-26/37 preservan
autorización única e inmutable y FLU-46/47 incorporan cifrado y autenticidad sin crear RF ni
PAN. Los rangos son inclusivos. Los PV remiten a CP en la Tabla E1. La cobertura documental
identifica comportamientos especificados y no equivale a pruebas ejecutadas.
La Tabla 82 presenta flujo Requisito Funcional y Pantalla.

## 79
## Tabla 82
Flujo Requisito Funcional y Pantalla
FlujoRequisitos Funcionales del PRDPantallas del Brief
## FLU-01RF-M02-01, RF-M03-09PAN-23, PAN-39
FLU-02RF-M01-01 a RF-M01-05, RF-M01-14PAN-01, PAN-02, PAN-34
## FLU-03RF-M01-05, RF-M01-06, RF-M01-09, RF-
## M01-10
## PAN-18, PAN-33
## FLU-04RF-M01-03, RF-M01-11PAN-34, PAN-35, PAN-01
## FLU-05RF-M01-07, RF-M01-08PAN-01, PAN-39, PAN-40
FLU-06RF-M01-12 a RF-M01-15PAN-01
## FLU-07RF-M02-01, RF-M02-09PAN-03, PAN-04
FLU-08RF-M02-02 a RF-M02-05PAN-05, PAN-21
## FLU-09RF-M02-06, RF-M02-07PAN-05
## FLU-10RF-M02-08, RF-M02-10, RF-M02-11PAN-03, PAN-05, PAN-23
FLU-11RF-M03-01 a RF-M03-04, RF-M03-09PAN-06, PAN-22
## FLU-12RF-M03-05PAN-22
## FLU-13RF-M03-06, RF-M03-07PAN-06, PAN-24, PAN-07,
## PAN-11
## FLU-14RF-M03-08PAN-06, PAN-23
FLU-15RF-M04-01 a RF-M04-04, RF-M04-07, RF-
## M04-13
## PAN-07, PAN-08
## FLU-16RF-M04-05, RF-M04-06, RF-M07-01PAN-08, PAN-25, PAN-12
## FLU-17RF-M04-08PAN-07, PAN-26, PAN-31,
## PAN-25
FLU-18RF-M04-09, RF-M07-03 a RF-M07-06PAN-27, PAN-31
## FLU-19RF-M04-10, RF-M04-11, RF-M04-12PAN-07, PAN-27
FLU-20RF-M05-01 a RF-M05-04, RF-M05-11, RF-
## M05-12
## PAN-20, PAN-09
## FLU-21RF-M05-05PAN-09, PAN-28
FLU-22RF-M05-06 a RF-M05-08, RF-M04-10PAN-09, PAN-02
## FLU-23RF-M05-09, RF-M05-10PAN-09, PAN-28
## FLU-24RF-M06-01, RF-M06-02, RF-M06-04, RF-
## M06-07, RF-M06-08, RF-M06-11, RF-M06-
## 12
## PAN-09, PAN-10, PAN-11
## FLU-25RF-M06-03, RF-M06-12PAN-10, PAN-11
## FLU-26RF-M06-05, RF-M06-06, RF-M09-09PAN-10, PAN-14, PAN-32
## FLU-27RF-M06-09PAN-11, PAN-29
## FLU-28RF-M06-10, RF-M06-11PAN-10, PAN-11, PAN-16
## FLU-29RF-M07-01, RF-M07-02PAN-12, PAN-25
## FLU-30RF-M07-03, RF-M07-04PAN-12, PAN-31
## FLU-31RF-M07-05, RF-M07-06, RF-M07-07PAN-12, PAN-31, PAN-16
## FLU-32RF-M08-01, RF-M08-02PAN-13, PAN-30
## FLU-33RF-M08-03, RF-M08-04, RF-M08-05PAN-13, PAN-30
## FLU-34RF-M08-06, RF-M08-07PAN-13, PAN-30

## 80
FlujoRequisitos Funcionales del PRDPantallas del Brief
FLU-35RF-M09-01 a RF-M09-03, RF-M09-05PAN-14, PAN-02
## FLU-36RF-M09-04, RF-M09-06, RF-M09-08PAN-14, PAN-02
## FLU-37RF-M09-07, RF-M09-08, RF-M09-09PAN-14, PAN-32
FLU-38RF-M10-01 a RF-M10-08, RF-M10-11, RF-
## M10-13
## PAN-15, PAN-16
## FLU-39RF-M10-09, RF-M10-10PAN-16
## FLU-40RF-M10-12PAN-16, PAN-40
FLU-41RF-M11-01 a RF-M11-05PAN-02
## FLU-42RF-M11-06, RF-M11-10PAN-02, PAN-07, PAN-09,
## PAN-13, PAN-39
FLU-43RF-M11-07 a RF-M11-09PAN-02 y diálogos abiertos
## FLU-44RF-M12-01, RF-M12-02, RF-M12-03PAN-17
## FLU-45RF-M12-04, RF-M12-05PAN-19, PAN-38
## FLU-46RF-M12-06PAN-36, PAN-40
## FLU-47RF-M12-07PAN-37, PAN-01
## FLU-48RF-M12-08PAN-39, PAN-40
## FLU-49RF-M01-13, RF-M09-04, RF-M12-08PAN-01, PAN-40
La Tabla 83 presenta resultados Académicos y Evidencia Propuesta.
## Tabla 83
Resultados Académicos y Evidencia Propuesta
ResultadoFlujos y Evidencia de Aceptación
RA1; IL1.1 a
## IL1.3
FLU-06 a FLU-19 y FLU-42/43; controles JavaFX, FXML, barra lateral,
MenuBar, ToolBar, ContextMenu, Alert, Dialog y selectores de archivos con
recorrido por teclado.
RA2; IL2.1 a
## IL2.3
FLU-02/03/04, FLU-24 y FLU-45; JDBC y PreparedStatement para acceso,
auditoría atómica y agregados. Verificar rollback y consistencia SQL.
RA3; IL3.1 a
## IL3.3
FLU-20/22/24/35/38/48; H1 a H6, progreso, bloqueos entre clientes y
detención controlada con recursos realmente liberados.
RA4; IL4.1 a
## IL4.3
CRUD de negocio por Hibernate; FLU-38/39 y diez reportes Jasper en PDF
y XLSX con lectura conciliada y sin sesiones abiertas en plantilla.
RA5; IL5.1 a
## IL5.3
FLU-35 a FLU-37 y FLU-26; HTTP, JSON, fallos, caché y procedencia
visible, separados de la autorización de tasa por fecha.
Nota. Los RA e IL proceden del resumen académico proporcionado. No se recibió ni se consultó
el syllabus original del docente.

## 81
## Referencias
AYR Tech (Pty) Ltd. (s. f.-a). API authentication notes.
https://www.exchangerate-api.com/docs/authentication
AYR Tech (Pty) Ltd. (s. f.-b). ExchangeRate-API: Free & Pro currency converter API.
https://www.exchangerate-api.com/
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
continuará en cero por ciento (0%) anual en el año 2026. https://bcn.gob.ni/divulgacion-
prensa/bcn-informa-que-la-tasa-de-deslizamiento-del-tipo-de-cambio-del-cordoba-
respecto
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026a). Brief de diseño de interfaz y
experiencia de usuario para Latch. [Documento académico no publicado]. Universidad
Americana (UAM).

## 82
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026c). Documento de requisitos del
producto de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026d). Documento de requisitos
técnicos de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026e). Esquema de Base de Datos
y Servicios de Latch. [Documento académico no publicado]. Universidad Americana
## (UAM).
Cuadra, S., Montalvan, A., Tardencilla, C., y Gavarrete, A. (2026f). Plan de Implementación de
Latch. [Documento académico no publicado]. Universidad Americana (UAM).
Gluon (s. f.). JavaFX. https://gluonhq.com/products/javafx/
Hibernate (s. f.-a). 7.4 series. https://hibernate.org/orm/releases/7.4/
Hibernate (s. f.-b). Session.
https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/Session.html
Hibernate (s. f.-c). SharedSessionContract.
https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/SharedSessionContract.html
Jaspersoft (s. f.-a). JasperReports Library artifacts: pom-parent.xml, versión 7.0.8.
https://raw.githubusercontent.com/Jaspersoft/jasperreports/7.0.8/pom-parent.xml
Jaspersoft (s. f.-b). JRXlsxExporter.
https://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/export/ooxml/
JRXlsxExporter.html

## 83
Malewicz, M. (s. f.). Aurora UI: New visual trend for 2021.
https://hype4.academy/articles/design/aurora-ui-new-visual-trend-for-2021
Open Exchange Rates (s. f.-a). Currencies. https://openexchangerates.org/api/currencies.json
Open Exchange Rates (s. f.-b). The forever free plan. https://openexchangerates.org/signup/free
OpenJFX (s. f.-a). JavaFX CSS reference guide.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html
OpenJFX (s. f.-b). Node. https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/Node.html
OpenJFX (s. f.-c). Task.
https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html
Oracle (s. f.-a). HttpClient: Java SE 21 & JDK 21.
https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/
HttpClient.html
Oracle (s. f.-b). Preferences: Java SE 21 & JDK 21.
https://docs.oracle.com/en/java/javase/21/docs/api/java.prefs/java/util/prefs/
## Preferences.html
PostgreSQL JDBC Driver (s. f.). Download. https://jdbc.postgresql.org/download/
The PostgreSQL Global Development Group (2026). PostgreSQL 18.6 release notes.
https://www.postgresql.org/docs/release/18.6/
The PostgreSQL Global Development Group (s. f.-a). Explicit locking.
https://www.postgresql.org/docs/18/explicit-locking.html
The PostgreSQL Global Development Group (s. f.-b). Sequence manipulation functions.
https://www.postgresql.org/docs/18/functions-sequence.html

## 84
The PostgreSQL Global Development Group (s. f.-c). Versioning policy.
https://www.postgresql.org/support/versioning/
World Wide Web Consortium (2024). Web content accessibility guidelines (WCAG) 2.2.
https://www.w3.org/TR/WCAG22/

## 85
## Apéndice A
Diagramas de Gran Tamaño
Las láminas amplían los ocho recorridos de negocio y las secuencias con estados de H1 a
H6 para conservar rótulos legibles. Las Figuras A1 a A8 contienen los cinco carriles solicitados y
las Figuras A9 a A14 reúnen cada secuencia y sus estados de ejecución. La Figura A15
representa rollback antes del commit y la Figura A16 desarrolla recuperación por UUID. Las
flechas mantienen la leyenda común del cuerpo. Los importes de demostración son sintéticos y
no documentan transacciones ejecutadas. Para A4, el ejemplo textual de la Tabla 57 usa C$
3,662.43 / 36.6243 NIO/USD (referencia BCN para 2026) = US$ 100.00, con HALF_EVEN;
36.5 NIO/USD es una tasa ficticia de pruebas. Las imágenes se conservan y sus ajustes
pendientes se detallan en la lista de figuras a redibujar.
La Figura A1 muestra carriles de E2E-01 De Propiedad a Contrato Activo.

## 86
## Figura A1
Carriles de E2E-01 De Propiedad a Contrato Activo
La Figura A2 muestra carriles de E2E-02 Ciclo Mensual de Cobranza.

## 87
## Figura A2
Carriles de E2E-02 Ciclo Mensual de Cobranza
La Figura A3 muestra carriles de E2E-03 Pago Parcial y Aplicación a Cargos.

## 88
## Figura A3
Carriles de E2E-03 Pago Parcial y Aplicación a Cargos
La Figura A4 muestra carriles de E2E-04 Pago entre Monedas con Tasa Autorizada.

## 89
## Figura A4
Carriles de E2E-04 Pago entre Monedas con Tasa Autorizada
La Figura A5 muestra carriles de E2E-05 Anulación de Recibo con Dependencias.

## 90
## Figura A5
Carriles de E2E-05 Anulación de Recibo con Dependencias
La Figura A6 muestra carriles de E2E-06 Renovación sin Solapamiento.

## 91
## Figura A6
Carriles de E2E-06 Renovación sin Solapamiento
La Figura A7 muestra carriles de E2E-07 Terminación Anticipada y Garantía.

## 92
## Figura A7
Carriles de E2E-07 Terminación Anticipada y Garantía
La Figura A8 muestra carriles de E2E-08 Cierre Mensual con Reportes.

## 93
## Figura A8
Carriles de E2E-08 Cierre Mensual con Reportes
La Figura A9 muestra secuencia y Estados de Ejecución de H1.

## 94
## Figura A9
Secuencia y Estados de Ejecución de H1
La Figura A10 muestra secuencia y Estados de Ejecución de H2.

## 95
## Figura A10
Secuencia y Estados de Ejecución de H2
La Figura A11 muestra secuencia y Estados de Ejecución de H3.

## 96
## Figura A11
Secuencia y Estados de Ejecución de H3
La Figura A12 muestra secuencia y Estados de Ejecución de H4.

## 97
## Figura A12
Secuencia y Estados de Ejecución de H4
La Figura A13 muestra secuencia y Estados de Ejecución de H5.

## 98
## Figura A13
Secuencia y Estados de Ejecución de H5
La Figura A14 muestra secuencia y Estados de Ejecución de H6.

## 99
## Figura A14
Secuencia y Estados de Ejecución de H6
La Figura A15 muestra rollback antes del Commit con Dos Usuarios.

## 100
## Figura A15
Rollback antes del Commit con Dos Usuarios
La Figura A16 muestra recuperación por Idempotencia Tras Respuesta Perdida.

## 101
## Figura A16
Recuperación por Idempotencia Tras Respuesta Perdida
Nota. Si no existe el UUID, el servicio comprueba primero que la transacción original terminó y
solo entonces admite reintentar el mismo UUID y contenido. Si la consulta no puede confirmar
ese hecho, mantiene resultado incierto y bloquea un nuevo envío. Una huella distinta produce
conflicto de idempotencia.

## 102
## Apéndice B
Formato de Especificación de Flujo con un Ejemplo Completo
El formato admite siete grupos de campos sin omitir actor, precondiciones, disparador,
pasos, alternos, excepciones, postcondiciones, datos, pantallas y reglas. Los pasos se numeran
dentro de la celda para mantener la secuencia sin listas en el cuerpo. Cada variante conserva el
identificador del flujo y documenta una condición observable de salida. El ejemplo desarrolla
FLU-24 con datos sintéticos y conversión identidad por ser la misma moneda, con
tasa_usada=NULL y autorizacion_id=NULL, para aislar el efecto del pago parcial; no representa
una autorización NIO/USD. Como variante de FLU-26 / CP-029, C$ 3,662.43 / 36.6243
NIO/USD (referencia BCN de 2026) redondea a US$ 100.00 con HALF_EVEN; la tasa 36.5
NIO/USD de los fixtures del Plan es ficticia. La asignación del número ocurre dentro de la
transacción y la generación del archivo sucede después del commit conocido. Nota de
equivalencia: CT-001 (BRI/FLU, Apéndice B) = CT-01 (FLU, escenarios E2E) = C1 (PLA,
contrato). Los identificadores C1 y C2 de cargos en la Tabla B1 son locales a ese ejemplo y no
designan el contrato C1 del PLA.
La Tabla B1 presenta fLU-24 Cobrar con Distribución Automática.
## Tabla B1
FLU-24 Cobrar con Distribución Automática
Especificación del Flujo
Identidad: FLU-24. Cobrar con Distribución Automática. Actor: Propietario o Asistente.
Entrada: Precondiciones: Sesión Asistente activa; contrato sintético CT-001; cargos vencidos
C1 C$ 600.00 y C2 C$ 400.00; UUID nuevo y huella del formulario. Disparador: Confirmar
C$ 800.00, Efectivo, fecha efectiva sintética 23/10/2026 ingresada por el usuario y fecha
operativa sintética 23/10/2026 obtenida de now() de la BD en America/Managua; efectiva no
futura respecto a operativa, con instante financiero guardado en pago.creado_en.

## 103
Especificación del Flujo
Pasos: 1) La interfaz muestra C1 C$ 600.00, C2 C$ 200.00 y crédito C$ 0.00; el usuario
confirma MSG-029. 2) El servicio revalida permiso y UUID, toma puerta compartida y
bloqueo de contrato, luego C1 y C2 por vencimiento e id; relee C$ 1,000.00 pendientes. 3)
Hibernate guarda pago y aplicaciones por C$ 800.00 con conversión identidad,
tasa_usada=NULL y autorizacion_id=NULL; flush precede al acceso al contador mediante
JDBC de la misma conexión. 4) H4 invoca fn_reservar_numero con siguiente_numero
sintético 41: reserva 000041 y avanza el contador a 42; persistir el recibo y ejecutar flush,
insertar auditoría JDBC en la misma conexión y ejecutar CALL sp_verificar_integridad; un
único coordinador ejecuta commit. 5) El servicio devuelve recibo y saldos conocidos; la
interfaz muestra MSG-030. H5 genera PDF sin retener bloqueo ni Session. Para recursos
presentes, el orden general es puerta, unidad, contratos, cargos, pagos de origen, depósitos y
contador; este ejemplo no bloquea recursos que no intervienen.
Alternos y Excepciones: Alternos: Selección manual puede cambiar destinos del mismo
contrato si total y saldos lo permiten. Un UUID repetido con misma huella recupera 000041
sin volver a aplicar C$ 800.00. Excepciones: Si falla auditoría antes de commit, rollback
conserva contador en 41 y no hay pago. Si respuesta se pierde después del commit, MSG-064
conserva UUID y consulta 000041 con Session nueva. Si solo falla PDF, MSG-033 permite
repetir H5.
Salida: Postcondiciones: C1 Pagado con saldo cero; C2 Vencido con saldo C$ 200.00; un pago
y un recibo Registrado; crédito cero y contador siguiente 42.
Datos y Pantallas: Lee: Contrato, cargos, crédito, permisos y contador. Escribe: Pago, recibo,
aplicaciones, consecutivo y evento atómicos; crédito derivado, sin escritura separada.
Pantallas: PAN-09, PAN-10, PAN-11.
Reglas y Ejecución: RN-06, RN-07, RN-14, RN-21, RN-32. RF-M06-01, RF-M06-02, RF-
M06-04, RF-M06-07, RF-M06-08, RF-M06-11, RF-M06-12. H4 dentro del servicio
financiero; H5 después del commit.
La Tabla B2 presenta comprobación del Ejemplo Sintético.
## Tabla B2
Comprobación del Ejemplo Sintético
InvarianteResultado Esperado
## Conservación
del pago
C$ 800.00 = C$ 600.00 + C$ 200.00 + C$ 0.00 de crédito. Misma moneda:
conversión identidad; tasa_usada=NULL y autorizacion_id=NULL.
Saldo de deudaC$ 1,000.00 iniciales menos C$ 800.00 de aplicaciones = C$ 200.00
pendientes.
IdempotenciaUn UUID y una huella producen un pago y un recibo; reintento recupera la
misma identificación.
ContadorSolo commit consume 41; rollback lo deja disponible. Anular conserva
000041 y no retrocede contador. fn_reservar_numero reserva 41 y avanza a
42 antes de persistir el recibo; después siguen flush, auditoría JDBC, CALL
sp_verificar_integridad y commit.
EvidenciaCálculo aritmético de escenario documental; no representa una ejecución de
Latch ni de PostgreSQL.

## 104
## Apéndice C
## Glosario
La Tabla C1 presenta términos Operativos Compartidos.
## Tabla C1
## Términos Operativos Compartidos
TérminoDefinición
UnidadEspacio alquilable de tipo Casa, Apartamento, Cuarto o Local comercial,
vinculado a propiedad.
CanonMonto de renta pactado en el contrato; el canon de referencia de la unidad
no modifica ese acuerdo.
CargoObligación con importe, moneda, concepto y vencimiento; su saldo
cambia mediante aplicaciones trazables.
MoraRecargo posterior a la gracia, fijo o porcentual sobre principal pendiente
elegible; excluye Mora de su base.
Depósito de
garantía
Fondos separados de renta que se retienen al activar y se aplican o
devuelven con conceptos al cierre.
ReciboComprobante administrativo de un pago confirmado con número
consecutivo único conservado incluso al anular.
Estado de cuentaRelación de cargos, abonos, anulaciones, saldo y crédito del contrato a
una fecha de corte.
Tasa de cambioCantidad NIO por USD con fecha y fuente; autorización única e
inmutable por fecha efectiva, distinguir referencia comercial de
autorización para pago.
Período de
facturación
Mes AAAA-MM del cargo de Renta único por contrato; canon completo
sin prorrateo automático. La duración contractual exige fin >= inicio + 1
mes; un mes natural completo (1–31) requiere fin >= día 1 del mes
siguiente, aun cuando la fecha final sea inclusiva.
Saldo a favorExcedente del equivalente contractual de un pago; se aplica al siguiente
cargo sin emitir otro recibo.
Contrato activoContrato Vigente, Por vencer o Vencido sin cierre; reserva unidad y
conserva obligaciones.
IdempotenciaUna intención con UUID y huella produce un solo resultado; repetirla
recupera su recibo confirmado.
CommitConfirmación transaccional conocida que hace visibles pago,
aplicaciones, recibo, contador y auditoría juntos.
RollbackReversión de una transacción fallida, incluido el avance del contador de
recibo aún no confirmado.
Resultado inciertoLa comunicación falló y el cliente desconoce el commit; requiere
consulta por UUID antes de reenviar.
CorteInstante o fecha que delimita movimientos incluidos en un indicador o
reporte; se conserva junto a los filtros.

## 105
TérminoDefinición
Tarea y trabajadorTask es una unidad observable; el hilo del pool que la ejecuta puede
seguir vivo al finalizarla.
Aurora UIDirección visual de luces difuminadas estáticas y superficies opacas;
Claro y Oscuro son sus dos temas en Latch.
Modalidad sin
efectos
Preferencia independiente del tema que elimina decoración y mantiene
foco, datos, contraste y operación.
Baja lógicaDesactivación de selección futura sin borrar historial ni referencias a
movimientos confirmados.
Lease de
instalación
Permiso temporal compartido en BD que designa un único ejecutor LAN
exclusivamente para H3 mediante control_fx; H2 usa indicador atómico
local y bloqueo de fila por contrato.
DTOObjeto de transferencia con datos preparados antes de cerrar la consulta;
evita llevar entidades abiertas a UI o reportes.
La Tabla C2 presenta catálogos Textuales Exactos.
## Tabla C2
## Catálogos Textuales Exactos
CatálogoValores
Monedas y rolesNIO (C$), USD (US$); Propietario, Asistente.
ContratoBorrador, Vigente, Por vencer, Vencido, Terminado, Cancelado.
UnidadCatálogo visible: Disponible, Ocupada, En mantenimiento, Inactiva.
unidad.estado persiste solo Disponible, En mantenimiento e Inactiva;
Ocupada se deriva de v_ocupacion.
CargoPendiente, Parcial, Pagado, Vencido, Anulado.
Pago y ReciboRegistrado, Anulado.
DepósitoRetenido, Aplicado, Devuelto parcial, Devuelto.
MantenimientoAbierta, En proceso, Resuelta, Cancelada.
Tipos de cargoRenta, Mora, Reparación, Servicio, Otro.
Métodos de pagoEfectivo, Transferencia, Depósito bancario, Otro.

## 106
## Apéndice D
Fuentes y Trazabilidad Documental
La tabla registra afirmaciones externas y decisiones que toman información de obras
consultadas en esta edición. La fecha de consulta corresponde al 23 de octubre de 2026 y no
reemplaza el año de publicación ni convierte una página sin fecha en una obra de ese año. El
artículo científico se cita como edición de volumen de 2021, Housing Policy Debate, 31(3–5),
425–445; su versión publicada en línea en 2020 se identifica en la nota de la Tabla D1. Los cinco
documentos hermanos se citan como Brief (2026a), PRD (2026c), TRD (2026d), Esquema
(2026e) y Plan (2026f).
La Tabla D1 presenta registro de Fuentes Consultadas.
## Tabla D1
Registro de Fuentes Consultadas
Afirmación o
## Decisión
Fuente Fecha y LocalizadorSección y Confiabilidad
URL sin llave;
cabecera
## Authorization
AYR Tech (Pty) Ltd. (s. f.-a). API
authentication notes. Documentación
oficial. https://www.exchangerate-
api.com/docs/authentication Consulta:
## 23/10/2026.
## Token Bearer. Proveedor
identifica ambos métodos de
autenticación.
## Actualización
diaria y 1500
solicitudes
mensuales
AYR Tech (Pty) Ltd. (s. f.-b).
ExchangeRate-API: Free & Pro currency
converter API. Página oficial de planes.
https://www.exchangerate-api.com/
## Consulta: 23/10/2026.
Free. La empresa respalda
sus propias condiciones
comerciales, sin garantía de
permanencia.
base_code,
conversion_rates
y result
AYR Tech (Pty) Ltd. (s. f.-c). Standard
requests. Documentación oficial.
https://www.exchangerate-api.com/docs/st
andard-requests Consulta: 23/10/2026.
Usage guide y Error
responses. Contrato primario
del endpoint seleccionado.
Cobertura del
córdoba y USD
como base
admitida
AYR Tech (Pty) Ltd. (s. f.-d). Supported
currency codes and countries. Catálogo
oficial. https://www.exchangerate-
api.com/docs/supported-currencies
## Consulta: 23/10/2026.
Fila NIO. Catálogo del
fabricante; no prueba
disponibilidad autenticada.

## 107
Afirmación o
## Decisión
Fuente Fecha y LocalizadorSección y Confiabilidad
Caché de uso
interno;
información
orientativa; no
redistribución
AYR Tech (Pty) Ltd. (s. f.-e). Terms of
service. Condiciones del proveedor.
https://www.exchangerate-api.com/terms
## Consulta: 23/10/2026.
License, Disclaimer y Data
caching policy. Texto
primario que limita el uso
del dato externo.
71 entrevistas en
Filadelfia; planes
de pago como
antecedente
limitado
Balzarini, J., y Boyd, M. L. (2021).
Working with them: Small-scale landlord
strategies for avoiding evictions. Housing
## Policy Debate, 31(3–5), 425–445.
https://doi.org/10.1080/10511482.2020.18
## 00779 Consulta: 23/10/2026.
Resumen e introducción de
copia editorial en NLIHC.
Nota de versión: publicación
en línea en 2020; referencia
bibliográfica de la edición de
volumen de 2021. No se
generaliza a Managua.
## Deslizamiento
cero y 36.6243
NIO por USD
para 2026
Banco Central de Nicaragua (2025). BCN
informa que la tasa de deslizamiento del
tipo de cambio del córdoba respecto al
dólar de los Estados Unidos de América
continuará en cero por ciento (0%) anual
en el año 2026. Comunicado institucional.
https://bcn.gob.ni/divulgacion-prensa/bcn-
informa-que-la-tasa-de-deslizamiento-del-
tipo-de-cambio-del-cordoba-respecto
## Consulta: 23/10/2026.
20 de octubre de 2025;
párrafo inicial. Organismo
competente para la
referencia oficial; no
documenta endpoint JSON.
PAN-01 a
## PAN-40,
MSG-001 a
MSG-089 y
tokens Aurora
## Cuadra, S., Montalvan, A., Tardencilla,
C., y Gavarrete, A. (2026a). Brief de
diseño de interfaz y experiencia de
usuario para Latch. Documento
académico no publicado. Archivo adjunto
Latch_Brief_UIUX_APA7_v3.pdf
## Consulta: 23/10/2026.
Navegación, Tabla 20 y
Apéndices B y C.
Documento del equipo
suministrado; especificación
de diseño, sin evidencia de
campo.
RF-Mxx-nn y
RN-01 a RN-38
## Cuadra, S., Montalvan, A., Tardencilla,
C., y Gavarrete, A. (2026c). Documento
de requisitos del producto de Latch.
Documento académico no publicado.
Archivo adjunto
Latch_PRD_APA7_v3.pdf Consulta:
## 23/10/2026.
Reglas RN, requisitos M01 a
M12 y reportes. Fuente del
alcance recibida; conserva
códigos y datos sintéticos.
## Transacción
única, contador y
programación
compartida
## Cuadra, S., Montalvan, A., Tardencilla,
C., y Gavarrete, A. (2026d). Documento
de requisitos técnicos de Latch.
Documento académico no publicado.
Archivo adjunto
Latch_TRD_APA7_v3.pdf Consulta:
## 23/10/2026.
Persistencia, hilos, servicio
web y cierre. Decisiones
compatibles conservadas;
paleta sustituida por tokens
del brief recibido.

## 108
Afirmación o
## Decisión
Fuente Fecha y LocalizadorSección y Confiabilidad
JavaFX 21.0.13
admite JDK 17 o
superior
Gluon (s. f.). JavaFX. Distribuidor oficial
de OpenJFX.
https://gluonhq.com/products/javafx/
## Consulta: 23/10/2026.
Roadmap; fila 21 LTS.
Matriz explícita del
distribuidor; JDK 21
satisface el mínimo.
7.4.6.Final existe;
## Java 21
compatible
Hibernate (s. f.-a). 7.4 series. Publicación
oficial de versiones.
https://hibernate.org/orm/releases/7.4/
## Consulta: 23/10/2026.
Compatibility y Releases in
this series. Responsable del
ORM publica versión,
licencia Apache 2.0 y
compatibilidad.
Session por caso
de uso y
trabajador; sin
compartir
entidades de UI
Hibernate (s. f.-b). Session. Javadoc
oficial.
https://docs.hibernate.org/orm/7.4/javadoc
s/org/hibernate/Session.html Consulta:
## 23/10/2026.
Ciclo de vida y seguridad de
hilos. Contrato de la interfaz
del ORM.
JDBC participa en
conexión de la
transacción
## Hibernate
Hibernate (s. f.-c).
SharedSessionContract. Javadoc oficial.
https://docs.hibernate.org/orm/7.4/javadoc
s/org/hibernate/SharedSessionContract.ht
ml Consulta: 23/10/2026.
doWork y beginTransaction.
API primaria para integrar
bitácora parametrizada sin
commit independiente.
7.0.8 publicado
con bytecode
objetivo 8;
ejecución con
JDK 21 como
decisión
compatible a
comprobar en
integración
Jaspersoft (s. f.-a). JasperReports Library
artifacts: pom-parent.xml, versión 7.0.8.
Código oficial etiquetado.
https://raw.githubusercontent.com/Jaspers
oft/jasperreports/7.0.8/pom-parent.xml
## Consulta: 23/10/2026.
revision, source.version y
target.version. Repositorio
del fabricante; no equivale a
prueba de la aplicación.
## Exportación
XLSX mediante
biblioteca fijada
Jaspersoft (s. f.-b). JRXlsxExporter.
Javadoc oficial 7.0.8.
https://jasperreports.sourceforge.net/api/n
et/sf/jasperreports/engine/export/ooxml/
JRXlsxExporter.html Consulta:
## 23/10/2026.
Descripción de clase.
Contrato de exportación del
responsable del proyecto.
Procedencia del
nombre Aurora
UI; decisiones
particulares
proceden del brief
Malewicz, M. (s. f.). Aurora UI: New
visual trend for 2021. Propuesta visual
primaria; acceso parcial.
https://hype4.academy/articles/design/aur
ora-ui-new-visual-trend-for-2021
## Consulta: 23/10/2026.
Título, autor e introducción
pública. Autor identificado;
contenido restringido
excluido y sin prueba de
eficacia.
## Alternativa
también
contempla
córdoba
Open Exchange Rates (s. f.-a). Currencies.
Catálogo oficial JSON.
https://openexchangerates.org/api/currenci
es.json Consulta: 23/10/2026.
Clave NIO. Respuesta de
catálogo público leída; sin
consumo autenticado.

## 109
Afirmación o
## Decisión
Fuente Fecha y LocalizadorSección y Confiabilidad
1000 solicitudes
mensuales y
actualización
horaria
Open Exchange Rates (s. f.-b). The
forever free plan. Página oficial de plan.
https://openexchangerates.org/signup/free
## Consulta: 23/10/2026.
Condiciones de plan Free.
Fabricante acredita su oferta;
no se contrató ni se presume
## SLA.
Aplicar tokens
con hojas CSS y
superficies opacas
OpenJFX (s. f.-a). JavaFX CSS reference
guide. Documentación oficial.
https://openjfx.io/javadoc/21/javafx.graph
ics/javafx/scene/doc-files/cssref.html
## Consulta: 23/10/2026.
Scene stylesheets y Paint.
Referencia nativa de JavaFX
21, sin biblioteca web.
Decoración fuera
de selección y
foco
OpenJFX (s. f.-b). Node. Javadoc oficial.
https://openjfx.io/javadoc/21/javafx.graph
ics/javafx/scene/Node.html Consulta:
## 23/10/2026.
mouseTransparent y
focusTraversable. API
primaria del grafo de escena.
Trabajos en
segundo plano sin
manipular escena
desde call
OpenJFX (s. f.-c). Task. Javadoc oficial.
https://openjfx.io/javadoc/21/javafx.graph
ics/javafx/concurrent/Task.html Consulta:
## 23/10/2026.
call, updateProgress y
cancelación cooperativa.
Responsable del framework
define ciclo de Worker y
contrato de hilo gráfico.
## Cliente
reutilizable y
futuros para
consulta HTTP
Oracle (s. f.-a). HttpClient: Java SE 21 &
JDK 21. Javadoc oficial.
https://docs.oracle.com/en/java/javase/21/
docs/api/java.net.http/java/net/http/
HttpClient.html Consulta: 23/10/2026.
Descripción y sendAsync.
API oficial del JDK elegido.
## Preferencias
locales por id de
Latch y manejo de
fallos
Oracle (s. f.-b). Preferences: Java SE 21 &
JDK 21. Javadoc oficial.
https://docs.oracle.com/en/java/javase/21/
docs/api/java.prefs/java/util/prefs/
## Preferences.html Consulta: 23/10/2026.
Descripción y
almacenamiento persistente.
API del JDK; no implica
sincronización entre equipos.
pgJDBC 42.7.14 y
dependencia
org.postgresql:pos
tgresql
PostgreSQL JDBC Driver (s. f.).
Download. Distribución oficial.
https://jdbc.postgresql.org/download/
## Consulta: 23/10/2026.
## Java 8: 42.7.14. Publicación
mantenida por el proyecto
pgJDBC.
## Versión 18.6
publicada y
estable elegida por
continuidad
The PostgreSQL Global Development
Group (2026). PostgreSQL 18.6 release
notes. Notas oficiales de lanzamiento.
https://www.postgresql.org/docs/release/1
## 8.6/ Consulta: 23/10/2026.
Release date: 2026-08-13.
Responsable del motor
documenta lanzamiento.
## FOR UPDATE
hasta fin de
transacción y
tratamiento de
interbloqueos
The PostgreSQL Global Development
Group (s. f.-a). Explicit locking. Manual
oficial de PostgreSQL 18.
https://www.postgresql.org/docs/18/explic
it-locking.html Consulta: 23/10/2026.
13.3.2 y 13.3.4. Semántica
oficial de bloqueos; revisión
documental sin ejecución
## SQL.

## 110
Afirmación o
## Decisión
Fuente Fecha y LocalizadorSección y Confiabilidad
nextval no
garantiza ausencia
de huecos;
contador
transaccional
elegido
The PostgreSQL Global Development
Group (s. f.-b). Sequence manipulation
functions. Manual oficial.
https://www.postgresql.org/docs/18/functi
ons-sequence.html Consulta: 23/10/2026.
## 9.17; Caution. Advertencia
explícita del motor sobre
rollback y secuencias.
Soporte de rama
18 hasta
noviembre de
## 2030
The PostgreSQL Global Development
Group (s. f.-c). Versioning policy. Política
oficial de soporte.
https://www.postgresql.org/support/versio
ning/ Consulta: 23/10/2026.
Tabla de versiones; rama 18.
Política del responsable; 19
RC no se adopta como
estable.
Contraste y foco
como metas
transferidas al
escritorio
## World Wide Web Consortium (2024).
Web content accessibility guidelines
(WCAG) 2.2. Recomendación técnica.
https://www.w3.org/TR/WCAG22/
## Consulta: 23/10/2026.
1.4.3, 1.4.11, 2.1.1 y 2.4.7.
Organismo de
estandarización; no acredita
conformidad web de una app
JavaFX.
Nombres físicos,
restricciones,
autorizaciones y
contratos de
servicios
## Cuadra, S., Montalvan, A., Tardencilla,
C., y Gavarrete, A. (2026e). Esquema de
Base de Datos y Servicios de Latch.
Documento académico no publicado.
## Archivo
Latch_Backend_Schema_APA7_v3.pdf.
## Consulta: 23/10/2026.
Tablas 2–3, seguridad,
autorizacion_tasa,
control_fx, concurrencia y
respaldo. Fuente canónica de
persistencia; revisión
documental sin ejecución
## SQL.
## Calendario,
RACI, revisión
cruzada y
ejecución CP
## Cuadra, S., Montalvan, A., Tardencilla,
C., y Gavarrete, A. (2026f). Plan de
Implementación de Latch. Documento
académico no publicado. Archivo
Latch_Plan_Implementacion_APA7_v3.p
df. Consulta: 23/10/2026.
Tablas 4–5 y 8, Revisión
Cruzada y Decisión Técnica;
Apéndices A y B: TAR-001
a TAR-096 y CP-001 a
CP-100. Decisiones D22,
D27 y D15: consultar el
registro canónico del PLA,
Apéndice G, para su alcance
y la sincronización de los
casos del Plan.
Nota. La copia editorial del artículo se abrió en https://nlihc.org/sites/default/files/Working-
With-Them-Small-Scale-Landlord-Strategies.pdf. El DOI conserva la identificación recuperable
de la obra. Las versiones consultadas son Brief corregido (2026a, 140 páginas), PRD corregido
(2026c, 145 páginas), TRD corregido (2026d, 113 páginas), Esquema corregido (2026e, 204
páginas) y Plan (2026f, 130 páginas). Estos recuentos identifican los PDF de apoyo de esta
revisión; sus decisiones documentales no son resultados de campo.

## 111
## Apéndice E
Plan de Verificación y Responsabilidades
Línea Técnica y Alcance de Verificación
La línea técnica conserva Java 21 y JavaFX 21.0.13, cuya fila de soporte publica JDK
mínimo 17 (Gluon, s. f.). PostgreSQL 18.6 fue publicado el 13 de agosto de 2026 y su rama
recibe soporte hasta noviembre de 2030 (The PostgreSQL Global Development Group, 2026, s.
f.-c). pgJDBC publica 42.7.14 para Java 8 o superior (PostgreSQL JDBC Driver, s. f.). Hibernate
incluye 7.4.6.Final y compatibilidad con Java 21 (Hibernate, s. f.-a). Se preservan versiones
publicadas del TRD sin afirmar aprobación docente ni presentarlas como las últimas revisiones
de cada biblioteca.
La dirección Aurora procede del contenido público de Malewicz (s. f.), mientras los
tokens concretos pertenecen al brief de Latch. Las hojas de JavaFX admiten estilos de escena y
pinturas nativas, base técnica para aplicar esa presentación (OpenJFX, s. f.-a). El contraste
mínimo de texto normal de 4.5:1 y el foco visible se adoptan como metas transferidas desde
WCAG 2.2 (World Wide Web Consortium, 2024). Esta especificación de escritorio conserva
controles nativos, superficies opacas y decoración estática; el documento no declara certificación
web ni resultados de una evaluación con usuarios.
El plan verifica comportamientos sobre una futura implementación con Windows 11 x64,
JDK 21, JavaFX 21.0.13 y PostgreSQL 18.6 aislado. La carga sintética incluye cien propiedades,
quinientas unidades, mil inquilinos y diez mil cargos para comparar las metas del TRD. Los
casos HTTP utilizan un servidor simulado y no requieren contratar el proveedor. Los PV-01 a
PV-16 son escenarios de diseño; la columna CP equivalente remite a los identificadores de
ejecución CP-001 a CP-100 del Plan (Cuadra et al., 2026f). Cada caso nuevo se registra solo

## 112
como variante de un CP existente. Cada ejecución deberá registrar fecha, versión del paquete,
evidencia y resultado observado en su acta de prueba. En esta entrega los resultados son
esperados; la revisión documental de SQL y API no representa ejecución de la aplicación.
La Tabla E1 presenta escenarios Procedimientos y Criterios de Aceptación.
## Tabla E1
Escenarios Procedimientos y Criterios de Aceptación
PruebaDatos y Procedimiento
## Propuesto
Resultado Esperado y
## Criterio Medible
## CP
equivalente
PV-01 Acceso
y bloqueo
Cuenta sintética A1; cinco
contraseñas incorrectas,
luego correcta dentro y fuera
de quince minutos.
Consultar contador y evento.
Quinto fallo bloquea; texto
genérico sin revelar cuenta;
cero sesiones durante
bloqueo; acceso posterior
reinicia fallos.
CP-001 a
## CP-003
## PV-02
## Autorización
Asistente con PAN-10
abierto; cambiar rol o
desactivar desde otro cliente
y forzar comando de
anulación/configuración.
Cero escrituras prohibidas;
MSG-062 y permisos
renovados antes de siguiente
comando.
## CP-005
## (variantes)
PV-03 Temas y
preferencias
En pago preparado, alternar
Claro/Oscuro y modalidad;
reiniciar con dos cuentas,
valor inválido y almacén sin
escritura.
Veinte campos/filtros, foco y
UUID intactos; Claro por
defecto; MSG-003 una vez
por sesión; cambio visual
objetivo ≤ 500 ms.
## CP-084,
## CP-085,
## CP-087,
CP-089 a
## CP-091
## PV-04
## Ocupación
concurrente
Dos clientes activan
Borradores de misma unidad
e intervalo a la vez;
inspeccionar depósito y
contrato.
Solo una activación
confirmada y una garantía;
perdedor sin escrituras
parciales.
## CP-016,
## CP-017,
## CP-052
## PV-05 H1
idempotente
Lote de quinientos contratos,
veinte rentas previas;
cancelar tras cien
confirmaciones y repetir
período.
Una renta por
contrato/período; contadores
suman total; avance
confirmado conservado y
cancelación entre contratos.
## CP-021,
## CP-045,
## CP-047,
## CP-099
PV-06 H2 y
pago
Renta C$ 1,000.00 vencida,
tres días gracia y mora
sintética 5 %; coordinar
pago y H2 con barrera.
Base leída bajo bloqueo;
Mora única sin
capitalización; resultado
corresponde al orden serial
confirmado, no a saldo
obsoleto.
## CP-022,
## CP-024,
## CP-046,
## CP-048
## (variante
## H2/pago)

## 113
PruebaDatos y Procedimiento
## Propuesto
Resultado Esperado y
## Criterio Medible
## CP
equivalente
PV-07 Pago
parcial y
excedente
Cargos C$ 600.00 y C$
400.00; pagar C$ 800.00,
luego C$ 300.00 con UUID
distintos.
Primer saldo C$ 200.00;
segundo crédito C$ 100.00;
cada equivalente igual a
aplicaciones más crédito.
CP-026 a
## CP-028
## PV-08
## Conversión
Cargo US$ 100.00; pago C$
3,662.43 y tasa autorizada
## 36.62430000 NIO/USD
(referencia BCN para 2026);
distinguir de la tasa ficticia
36.5 del Plan. Repetir sin
autorización y con otra
referencia API; intentar
segunda autorización de la
misma fecha.
Equivalente US$ 100.00;
procedencia congelada; falta
de autorización bloquea;
nueva referencia no cambia
el pago; duplicado por fecha
produce ERR-060 /
MSG-083 sin otra
autorización.
CP-029 a
## CP-031,
## CP-066
## (variantes)
## PV-09
## Contador
simultáneo
Dos variantes separadas,
cada una con diez emisores
concurrentes en dos JVM y
contador inicial 1: a)
confirmar 100 pagos con
UUID distintos y otras diez
solicitudes con rollback
antes del commit; b)
confirmar 100 pagos y
anular diez después del
commit. Consultar recibos y
consecutivo. El máximo de
tres sesiones LAN limita
clientes de aplicación, no
hilos de prueba.
Variante a: 100 recibos y
siguiente_numero=101;
rollback no consume
número. Variante b: 100
recibos conservados, 90
pagos Registrado y 10
Anulado, con
siguiente_numero=101. Cero
duplicados o huecos entre
confirmados de la base
activa; la anulación conserva
número e historial y no
reinicia el contador.
## CP-041,
## CP-042,
## CP-032 (D22;
registro de
decisiones del
## PLA,
## Apéndice G)
## PV-10
## Respuesta
perdida
Cortar respuesta después del
commit y reenviar mismo
UUID y huella; luego mismo
UUID con monto distinto.
Un pago y un recibo;
recuperar original; huella
distinta rechazada sin nueva
escritura.
## CP-043,
## CP-044
## (variantes)
## PV-11
Anulación con
dependencias
Pago genera crédito C$
100.00, siguiente renta lo
consume; intentar anular y
luego revertir dependencia
autorizada.
Primer intento bloqueado
por RN-31; segundo restaura
saldos, conserva número y
auditoría.
## CP-032,
## CP-033
## PV-12
Garantía y
término
## Garantía C$ 1,000.00;
aplicar C$ 300.00 y devolver
C$ 700.00 con conceptos;
inyectar fallo en auditoría.
## Éxito: Terminado, Devuelto
y remanente cero. Fallo:
rollback de cierre y
liquidación completos.
CP-034 a
## CP-036,
## CP-040
## (variante
rollback)

## 114
PruebaDatos y Procedimiento
## Propuesto
Resultado Esperado y
## Criterio Medible
## CP
equivalente
PV-13 Matriz
## HTTP
Simular éxito, red caída,
timeout, 4xx, 5xx, 429,
JSON inválido y tasa
comercial fuera de banda.
Distinguir HTTP 408
transitorio de 4xx
permanentes y HTTP 429.
Comprobar JSON, base
USD, NIO válido y fechas
coherentes; conexión 5 s y
operación 10 s. Variante
CP-058: referencia
comercial sin autorización
automática; tasa manual
fuera de banda revisada y
confirmada por Propietario,
sin CHECK de banda en
## BD.
Ocho causas visibles; caché
anterior intacta al rechazar;
fuera de banda 1–100
registra M09
## REFERENCIA_EN_CUAR
ENTENA con valor y huella
sin tabla nueva; reintentos y
cuota según tabla; cero
llaves en evidencia. 408, red
o 5xx admiten un único
reintento tras 60 s; una
consulta diaria. 429 respeta
Retry-After y cuota;
respuestas inválidas
conservan referencia anterior
y registran rechazo o
cuarentena.
CP-053 a
## CP-066
## (variante
## CP-058:
banda)
## PV-14
Reportes y
archivos
Diez salidas, filtros válidos,
carpeta sin permiso, archivo
existente y cancelación;
XLSX de diez mil filas.
Variantes de cancelación H5
durante consulta, llenado y
exportación, incluida carrera
con publicación, destino
previo y finanzas ya
confirmadas.
Totales por moneda
conciliados; cero archivos
finales incompletos; destino
previo preservado; meta
XLSX ≤ 30 s. MSG-089 sin
ERR solo tras terminar y
limpiar temporales antes de
publicar; si ya publicó,
resultado real. Pagos y
anulaciones confirmados se
conservan.
CP-067 a
## CP-076,
## CP-086,
## CP-100
## (variantes)
PV-15 Cierre y
respaldo
Cerrar durante H1, HTTP,
exportación y H4; prolongar
trabajo más de diez
segundos; respaldo con hash
alterado, contraseña
incorrecta y etiqueta de
autenticidad AES-256-GCM
alterada como variantes de
## CP-095.
H6 conserva proceso a diez
segundos; pool cierra tras
tareas; hash inválido produce
MSG-079; clave o
autenticidad inválida
produce ERR-059 /
MSG-082, sin reemplazar
instalación.
CP-049 a
## CP-051,
## CP-095
## (variantes)
PV-16 Teclado
y consulta
Recorrer diez tareas con
Tab, Shift+Tab, Enter,
Escape y F6; abrir panel
treinta veces con carga
sintética.
Cero pérdidas de foco o
entradas; contraste según
tokens; meta panel p95 ≤ 2
s; medir en acta futura.
CP-078 a
## CP-080,
## CP-093,
## CP-098

## 115
Responsables y Capacidad Propuesta
La asignación y el relevo técnico se rigen por la Tabla 4 del Plan; la Tabla E2 reproduce
su Tabla 5 como matriz RACI de entregables (Cuadra et al., 2026f). La revisión cruzada remite
al apartado Revisión Cruzada y Decisión Técnica del mismo Plan. Las letras R, A, C e I
conservan el significado de esa fuente y una sola A por fila; la responsabilidad de cada TAR se
consulta en su Apéndice A. Son asignaciones académicas propuestas, sin afirmar acuerdos ni
horas ejecutadas.
La Tabla E2 reproduce la matriz RACI del Plan de Implementación (Tabla 5).
## Tabla E2
Matriz RACI de Entregables según el Plan de Implementación
EntregableSteven CuadraAlfredo
## Montalvan
## Caleb
## Tardencilla
## Aris
## Gavarrete
Alcance y calendarioA/RCCC
Seguridad y rolesA/RCCR
Esquema y
migraciones
## CA/RIC
Persistencia y
transacciones
## RA/RCC
FXML y temasCIA/RR
Concurrencia y cierreA/RRCC
HTTP y cachéRCCA/R
Reportes y oráculosCRCA/R
Pruebas y
conformidad visual
## RRA/RR
Manual y defensaA/RRRR
App-image ZIP y
respaldo de entrega
## CRCA/R
La Tabla E3 presenta fechas Dependencias y Capacidad Propuesta.

## 116
## Tabla E3
Fechas Dependencias y Capacidad Propuesta
Período y HitoDependencia y TrabajoCapacidad / Asignación
## Sprint 0; 22–
## 27/10
TAR-001 a TAR-016; fundamentos, JDBC,
dos temas y evidencia de concurrencia.
32 h programadas;
capacidad 41,142857 h;
reserva 9,142857 h.
## Sprint 1; 28/10
## –03/11
TAR-017 a TAR-032; dominio, ORM,
CRUD, contrato y garantía.
40 h programadas;
capacidad 48 h; reserva 8 h.
## Sprint 2; 04–
## 10/11
TAR-033 a TAR-048; renta, mora, pago,
consecutivo y liquidación.
40 h programadas;
capacidad 48 h; reserva 8 h.
## Sprint 3; 11–
## 17/11
TAR-049 a TAR-064; mantenimiento, panel,
respaldo y diez reportes.
40 h programadas;
capacidad 48 h; reserva 8 h.
## Sprint 4; 18–
## 25/11
TAR-065 a TAR-080; HTTP, regresión
visual, medidas y versión candidata.
44 h programadas;
capacidad 54,857143 h;
reserva 10,857143 h.
## Cierre; 26/11–
## 01/12
TAR-081 a TAR-096; entrega interna el
26/11, correcciones, ensayo y defensa.
32 h programadas;
capacidad 41,142857 h;
reserva 9,142857 h.
## Total; 22/10–
## 01/12
TAR-001 a TAR-096; 96 tareas en seis
períodos, según Plan, Apéndice A.
228 h programadas;
capacidad 281,142857 h;
reserva 53,142857 h (18,90
## %).
Nota. Fuente: Plan de Implementación, Tabla 8 y Apéndice A, TAR-001 a TAR-096 (Cuadra et
al., 2026f). Las sumas de tareas por período son 32 + 40 + 40 + 40 + 44 + 32 = 228 h. Capacidad
exacta: 48 × 41/7 = 281,142857 h; reserva: 281,142857 − 228 = 53,142857 h (18,902439 %).
Cada integrante tiene 57 h programadas y 70,285714 h de capacidad; reserva individual
13,285714 h. Para lectura se muestran 281,14 h y 53,14 h; no son horas trabajadas.
La revisión cruzada y el relevo remiten al Plan, Tablas 4–5 y apartado Revisión Cruzada
y Decisión Técnica (Cuadra et al., 2026f). El seguimiento es asíncrono diario y la revisión ocurre
al cierre de cada sprint. Los cuatro participarán en las pruebas de competencia entre clientes y
cierre de recursos. La reserva de 53,14 h queda disponible para coordinación, correcciones e
incertidumbre, sin comprometer nuevas funciones fuera del alcance. La planificación excluye

## 117
contabilidad completa, facturación fiscal, portal del inquilino, mensajería automática, firma
electrónica y OCR.