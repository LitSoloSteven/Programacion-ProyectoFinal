# Informe de implementación UI/UX de Latch

## Alcance y verificación

La implementación se hizo en la rama `feature/uiux-latch`. Se conservaron los servicios, modelos, API, DTOs, migraciones y reglas financieras. Los controladores mantienen las mismas operaciones de negocio y argumentos; las consultas y escrituras se ejecutan mediante tareas en segundo plano.

La suite completa `mvnw.cmd test` pasó dos veces antes del ajuste final de altura reservada: **187 pruebas, 0 fallos, 0 errores y 0 omitidas**. Después del ajuste se ejecutaron el empaquetado y las pruebas dirigidas `FxmlControladoresTest,EstilosCssTest,ContrasteTest,InsigniaEstadoTest`, todos con éxito. También se intentó de nuevo la suite completa sin elevación: Surefire ejecutó 121 pruebas y registró 8 errores de inicio por `Unable to establish loopback connection` / `Permission denied` en pruebas API que levantan servidor local; las pruebas restantes terminaron sin fallos. No hay pruebas PostgreSQL configuradas en el entorno (`LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD` ausentes), por lo que ese perfil no se ejecutó.

La suite no sustituye la revisión visual manual. No se probó la aplicación en ventanas reales, con escalas de Windows ni con lectores de pantalla. JavaFX aplica los estilos de la escena propietaria a los controles popup; las hojas también definen estilos para menús, tooltips, calendarios, listas desplegables y diálogos. Su apariencia final y cualquier tinte residual de Modena quedan **sin verificación visual**.

## Matriz de aceptación (§14 del manual)

| Criterio | Estado | Evidencia |
|---|---|---|
| Solo aparecen los cinco colores base; las variantes alfa derivan de ellos. | Cumple | `estilos.css` declara la paleta y los velos dentro de `.root`; `EstilosCssTest` valida hex y canales RGB. |
| Texto normal ≥ 4.5:1; componentes y foco ≥ 3:1. | Cumple | `ContrasteTest` reproduce 11.05, 13.58, 9.39, 1.78, 7.61 y 5.26; comprueba los umbrales aplicables. |
| `#A7B1BE` no se usa en texto pequeño sobre `#E6E9EF`. | Cumple | Tokens de texto claro usan azul marino; `ContrasteTest` comprueba que el par 1.78:1 no se acepta como texto pequeño. |
| Tablas e importes conservan superficies opacas. | Cumple | Reglas `.data-table`, `.table-view` y filas usan superficies opacas de la paleta en `estilos.css`. |
| Las vistas comparten navegación, título, búsqueda, tabla y paginación. | Parcial | Las siete listas conservan navegación y patrón de encabezado, filtros, contador, `TableView` y acciones. Solo Pagos y Cargos tienen paginación de servidor, conforme a los servicios existentes; las demás listas filtran localmente. |
| Las tablas permanecen como `TableView`. | Cumple | Los siete FXML de lista declaran `TableView`; columnas de acciones tienen 48 px y clases de alineación/fecha se asignan por `FormatoTabla`. |
| Formularios con etiquetas, ayuda, errores junto al campo y resumen para varios errores. | Cumple | Siete FXML de formulario; controladores `XxxFormController` validan y muestran resumen cuando hay varios errores. Las etiquetas de error se mantienen gestionadas y reservan altura. |
| Guardar no duplica escrituras durante el procesamiento. | Cumple | Los controladores deshabilitan la acción primaria durante el `Task` y muestran el estado de guardado/autorización. |
| Los errores preservan datos y no muestran SQL/stack trace. | Cumple | Fallos mantienen abierto el diálogo y el borrador; los mensajes visibles son neutros. Búsqueda estática de texto de interfaz y controladores sin menciones visibles de PostgreSQL/SQL. |
| Anular requiere una fila seleccionada y confirmación específica. | Cumple | Cargos y Pagos deshabilitan la acción sin selección y muestran una confirmación con el registro y la consecuencia. |
| Autorización de tasas y anulación de pagos reflejan permisos. | Cumple | Controles de autorización/anulación quedan deshabilitados según permiso; el tooltip de tasa se instala en el contenedor envolvente. |
| El asistente registra tasa informativa, pero no se presenta como autorizador. | Cumple | Registro y autorización son acciones/formularios distintos; la autorización solo se habilita para propietario. |
| Rutas recorribles con teclado y foco visible. | Parcial | Hay estilos de foco y atajos Ctrl+F/Ctrl+N; Delete no ejecuta acciones. Recorrido integral con teclado y tecnología asistiva pendiente de revisión manual. |
| Iconos de un solo conjunto, sin emojis en navegación o controles. | Cumple | `Iconos` centraliza trazos `SVGPath`; títulos, navegación y notificaciones usan ese catálogo. |
| Estados financieros no dependen solo del color. | Cumple | `InsigniaEstado` usa icono y texto; `InsigniaEstadoTest` comprueba texto accesible y visible para los estados requeridos. |
| No hay movimiento continuo ni animaciones que retrasen una operación. | Cumple | `TransicionesUI` limita entrada/salida a 170/110 ms; notificaciones se pueden cerrar y ninguna escritura espera a una transición. |
| Transiciones interrumpibles; la interfaz responde durante cargas. | Parcial | `TransicionesUI` cancela/termina animaciones y respeta movimiento reducido; tareas de servicios vuelven al hilo FX. La respuesta visual integrada no se comprobó en ejecución manual. |
| Legibilidad de 1100×700 a ≥1440 px y escalas de Windows. | Parcial | Tamaños y mínimos de login/dashboard están configurados; sidebar cambia de 224 a 72 px bajo 1280 px. Revisión visual de tamaños, escalas 100/125/150 % y Windows pendiente. |
| No se muestran métricas, “tiempo real”, recibos o estados sin respaldo. | Cumple | Dashboard usa seis métricas existentes y hora de actualización; no se añadió impresión de recibos ni destinos nuevos. Búsqueda estática sin “tiempo real” ni texto visible de PostgreSQL. |

## Archivos creados y modificados

- `.gitignore`: excluye `.agents/` y `docs/dashboard-latch-mockup.svg`.
- `src/main/java/module-info.java`: habilita `java.util.prefs`.
- Arranque y shell: `HelloApplication.java`, `controller/LoginController.java`, `controller/DashboardController.java`.
- Controladores de lista reestructurados: `CargoController.java`, `PagoController.java`, `TasaController.java`, `PropiedadController.java`, `UnidadController.java`, `InquilinoController.java`, `ContratoController.java`.
- Controladores de formulario creados: `CargoFormController.java`, `PagoFormController.java`, `TasaFormController.java`, `PropiedadFormController.java`, `UnidadFormController.java`, `InquilinoFormController.java`, `ContratoFormController.java`.
- Utilidades nuevas: `AccionesTabla.java`, `AtajosLista.java`, `FormatoTabla.java`, `TemaAplicacion.java`, `Iconos.java`, `InsigniaEstado.java`, `Notificaciones.java`, `DialogoFormulario.java`, `PanelEstadoTabla.java`, `SesionExpiradaUtil.java`, `TransicionesUI.java`.
- Utilidades existentes extendidas: `AlertaUtil.java`.
- Recursos FXML modificados: `login-view.fxml`, `dashboard-view.fxml`, `cargo-view.fxml`, `pago-view.fxml`, `tasa-view.fxml`, `propiedad-view.fxml`, `unidad-view.fxml`, `inquilino-view.fxml`, `contrato-view.fxml`.
- Recursos FXML nuevos: `cargo-form.fxml`, `pago-form.fxml`, `tasa-form.fxml`, `propiedad-form.fxml`, `unidad-form.fxml`, `inquilino-form.fxml`, `contrato-form.fxml`.
- Estilos y pruebas nuevas: `estilos.css`, `EstilosCssTest.java`, `ContrasteTest.java`, `InsigniaEstadoTest.java`, `FxmlControladoresTest.java`.
- Este informe: `docs/INFORME_UIUX.md`.

Las fuentes Fira Sans/Fira Code no estaban empaquetadas en el repositorio. La interfaz conserva la escala y usa los fallbacks Segoe UI/Consolas; no se descargaron fuentes ni se agregó licencia de terceros.

## Decisiones y observaciones

- La paginación se conserva únicamente para Pagos y Cargos, que la soportan en servicio. Propiedades, Unidades, Inquilinos, Contratos y Tasas filtran el conjunto cargado y muestran contador; no se inventó paginación de servidor.
- No se agregó reautenticación. Si expira el token, el formulario puede permanecer abierto con sus valores y ofrece cerrar sesión; el usuario deberá iniciar sesión nuevamente.
- Se conservaron las operaciones de negocio, permisos y reglas existentes. Las confirmaciones destructivas identifican el registro y la consecuencia.
- `Pago`, `Unidad` y `Contrato` mantienen el uso de `String.format("%.2f")`, sensible a la `Locale` por defecto. Se registra el riesgo sin modificar la regla de formato solicitada.
- El token de API dura 15 minutos y no tiene renovación; los errores 401 lo comunican y no implementan renovación.
- Posible tintado residual de Modena: no detectado en código estático; queda sin verificar visualmente en popups y diálogos.
- No se agregaron fuentes externas ni dependencias.

## Fases y commits

| Fase | Commit |
|---|---|
| 1. Tokens y utilidades base | `be4e4eb` |
| 2. Shell, login y dashboard | `e6b618a` |
| 3. Lista compartida y atajos | `4c7ea24` |
| 4. Cargos, Pagos y Tasas | `08a2ff7` |
| 5. Propiedades, Unidades, Inquilinos y Contratos | `120e1f8` |
| 6. Movimiento | `f1b13aa` — transiciones interrumpibles y movimiento reducido. |
| 7. Tema central | `dae46ca` — preferencias persistentes de tema y movimiento reducido. |
| 8. Revisión e informe | El commit de cierre contiene este informe, la eliminación del plan efímero y la revisión automatizada. PostgreSQL queda pendiente por falta de variables. |

## Comprobaciones visuales manuales pendientes

1. Revisar login y dashboard en 1100×700, 1280×800 y ≥1440 px.
2. Revisar escalas de Windows 100 %, 125 % y 150 %, incluido scroll horizontal de tablas.
3. Inspeccionar ComboBox, DatePicker, ContextMenu, Tooltip, Alert/Dialog y barras de scroll en tema claro y oscuro; anotar cualquier tinte de Modena.
4. Recorrer las ocho rutas y todos los formularios con teclado, incluido foco al cerrar diálogos, Escape y confirmación de cambios.
5. Confirmar con propietario y asistente los estados habilitado/deshabilitado de autorización y anulación.
6. Revisar lectores de pantalla: nombres accesibles de navegación, botones de acción de fila, estados y notificaciones.
7. Probar reducción de movimiento y cambio de tema mientras una transición y una carga están activas.
8. Ejecutar el perfil PostgreSQL al disponer de `LATCH_TEST_DB_URL`, `LATCH_TEST_DB_USER` y `LATCH_TEST_DB_PASSWORD`.
