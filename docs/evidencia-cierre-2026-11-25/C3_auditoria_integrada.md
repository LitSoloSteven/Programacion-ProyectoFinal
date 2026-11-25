# Auditoría integrada de C3 (P6–P8)

Revisión del 25 de noviembre de 2026 sobre la rama `feature/p1-base-seguridad-admin`, con base `1b8de3a` y los cambios de la rama de trabajo. Las pruebas PostgreSQL se ejecutaron sobre bases desechables con datos sintéticos.

| Área | Evidencia de implementación |
| --- | --- |
| P6 · Reportes | `ReporteServiceTest` comprueba los diez reportes requeridos, incluido REP-01 solo en PDF, y los datos de REP-02 a REP-10 para XLSX. `ExportadorXlsxTest` cubre celdas numéricas, 10 000 filas, instancia única de H5 y conservación del archivo anterior al cancelar. [Acta de P6](C3_P6_reportes_y_exportacion.md) |
| P7 · Respaldo y restauración | `PostgresMigrationIT.RespaldoYRestauracionPostgres` ejecuta `pg_dump` y `pg_restore` nativos, verifica la clave, el recibo y la huella del adjunto restaurado. La regresión fuerza un error de PostgreSQL durante la restauración y comprueba que el recibo y los datos previos del destino se conserven. [Acta de P7](C3_P7_respaldo_y_restauracion.md) |
| P8 · Panel, temas y salida | Las pruebas de panel e inactividad verifican los indicadores y el aviso anterior al cierre de sesión. La prueba de temas abre un `Stage` y comprueba foco, persistencia de entradas y preferencias por cuenta. La regresión H6 agota el plazo con un trabajador activo, admite nuevas tareas después de abortar el cierre y vuelve a cerrar cuando las tareas terminan. [Acta de P8](C3_P8_panel_temas_y_cierre.md) |

## Correcciones de la auditoría

- El cargador FXML recibe el valor numérico de `Region.USE_PREF_SIZE` en el panel, el centro de reportes, la vista previa y las vistas relacionadas. La comprobación estática detecta el literal que impedía cargar las pantallas. Se reparó el trazado del icono de vista previa que JavaFX descarta cuando es inválido.
- H6 bloquea la admisión temporalmente y espera a que terminen los trabajadores. Después de un cierre bloqueado, la interfaz, la consulta H3 y el monitor de inactividad pueden reanudar su actividad. El cliente HTTP de H3 se cierra al finalizar el cierre. La cancelación de H2 usa el registro cooperativo.
- `pg_dump` y `pg_restore` se ejecutan sin solicitar contraseña interactivamente. El plazo también acota la lectura de salida. `pg_restore` usa una sola transacción.
- La restauración rechaza un paquete o una copia previa dentro de la carpeta de adjuntos que va a sustituir. En modo aislado también rechaza un destino que se superpone con el almacén operativo. La prueba de antigüedad usa el reloj de base que utiliza el servicio.

## Comandos y resultado

- `psql.exe -X -v ON_ERROR_STOP=1 --single-transaction` aplicó V0 a V16 en orden sobre una base vacía. **Aprobado**.
- `./mvnw.cmd -o -B verify -Ppostgres` ejecutó **335 pruebas normales y 41 de PostgreSQL 18.6** sin fallos, errores ni omisiones. La prueba de restauración incluida confirmó el rollback nativo.
- El humo de JavaFX con `-Dprism.order=sw` y una cuenta sintética abrió el panel, cambió el tema conservando foco y contenido, abrió la pestaña de respaldo, superó el caso ERR-056 de H6 al continuar y renderizó la primera página de REP-03 con las dos acciones de exportación visibles. La automatización de Windows detectó el selector nativo de PDF abierto por la JVM y lo cerró. El mismo pase ejecutó `FxmlControladoresTest`, `EstilosCssTest` y `ContrasteTest`. **4 pruebas aprobadas**. Las pruebas de exportación aparte comprueban la escritura de PDF y XLSX.
- La prueba de volumen usó PostgreSQL 18.6, **1 126 241 983 bytes de base y 1 073 741 824 bytes en 205 adjuntos sintéticos**, con destino en una segunda base desechable. El respaldo tardó **11 min 11 s** y la restauración **2 min 02 s**. Se recuperaron los 205 adjuntos con **0 huellas discordantes**. La prueba fue una variante temporal de `PostgresMigrationIT.RespaldoYRestauracionPostgres`, con datos sintéticos poco compresibles que acreditan el volumen. El equipo fue Windows 11 x64, SSD, 32 procesadores lógicos y 31,7 GiB de RAM.

## Estado de cierre

P6–P8 están implementados y pasaron la auditoría técnica en el equipo disponible. Para la aceptación formal quedan la revisión humana G6, el recorrido manual de aceptación del plan y la repetición de RNF-036/RNF-037 con el equipo y el conjunto de referencia V1. El benchmark de esta auditoría satisface los umbrales de 15 y 30 minutos en el equipo medido.

Invalidaría el resultado que una vista de C3 falle al cargar su FXML, que continuar tras ERR-056 impida nuevas tareas, que un error de `pg_restore` deje cambios parciales en el destino o que una restauración produzca una huella distinta a la registrada en el manifiesto.
