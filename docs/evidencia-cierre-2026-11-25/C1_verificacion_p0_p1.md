# Verificación de C1 (P0 y P1)

Revisión de la rama de trabajo `feature/p1-base-seguridad-admin`, con base `1b8de3a` y cambios locales sin commit. El ZIP congelado de E4 corresponde a otra revisión.

| Criterio | Resultado |
| --- | --- |
| P0, decisiones y línea base | **Verificado por inspección documental.** Las decisiones D-P0-1 a D-P0-4 están registradas en el [plan](../LATCH_PLAN_DE_CIERRE_2026-11-25.md). |
| Instalación y actualización | **Verificado.** `psql.exe -X -v ON_ERROR_STOP=1 --single-transaction` con V0–V12 en base vacía. El perfil Maven también comprobó instalación limpia con Hibernate en `validate` y actualización de datos previos. |
| Usuarios y credenciales | **Verificado en servicio y HTTP.** Alta, roles, estado y restablecimiento, Argon2id con transición PBKDF2, revocación y cambio de clave. La clave temporal correcta devuelve 409 sin token hasta registrar una contraseña distinta. |
| Catálogos e historial | **Verificado en servicio y HTTP.** Permisos de la Tabla 7, bajas lógicas, historial y coincidencia de identificación. La edición permitida conserva el código histórico de la unidad, incluso si el formulario envía el código sin espacios exteriores. |
| Parámetros, emisor y auditoría | **Verificado en servicio.** Historial de vigencia, emisor conservado por recibo y consulta filtrada de auditoría restringida al Propietario. |
| JavaFX | **Verificado por pruebas de FXML, estilos y contraste.** **No probado en monitor:** ejecutar `./mvnw.cmd javafx:run` contra datos de prueba y recorrer Administración con Propietario, cambio de clave temporal desde login y edición de unidad histórica como Asistente. |

`./mvnw.cmd -B verify -Ppostgres` en PostgreSQL 18.6 desechable terminó con **253 pruebas H2 y 39 PostgreSQL**, sin fallos, errores ni omisiones. También se ejecutó `git diff --check`.

Invalidaría el resultado un token emitido con la clave temporal, cambios en un campo restringido tras la edición de Asistente o que Hibernate rechace el esquema final en `validate`. La aprobación humana de cambios críticos (G6) sigue pendiente. La retirada de PBKDF2 depende de verificar que ya no queden hashes legados en las bases mantenidas. La modalidad de inactividad de sesión corresponde a P8.
