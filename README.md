# Latch — Proyecto de Programación de Aplicaciones de Escritorio

Aplicación académica de administración de alquileres. Esta entrega implementa la parte de Steven Cuadra: infraestructura Hibernate, usuarios, roles, auditoría, configuración y base de tareas en segundo plano. La pantalla inicial sigue siendo la plantilla JavaFX; los formularios y las entidades de alquileres corresponden a las siguientes integraciones del equipo.

## Requisitos

- JDK 21 (configurar `JAVA_HOME`).
- PostgreSQL 18 para ejecutar la persistencia.
- Maven Wrapper incluido: `./mvnw.cmd` en Windows y `./mvnw` en Linux/macOS.
- JavaFX 21.0.6, Hibernate 6.6.59.Final y controlador PostgreSQL 42.7.14, declarados en Maven.

## Base de datos y configuración

Desde pgAdmin o psql, crear un usuario local con contraseña propia y una base llamada `latch_db` de su propiedad. No utilizar el superusuario de PostgreSQL para la aplicación.

La aplicación carga automáticamente `.env` desde la raíz del proyecto. Copiar `.env.example` a `.env` si no existe y completar:

```dotenv
LATCH_DB_URL=jdbc:postgresql://localhost:5432/latch_db
LATCH_DB_USER=tu_usuario_postgresql
LATCH_DB_PASSWORD=tu_contraseña_postgresql
LATCH_DB_SCHEMA_MODE=update
```

Ejecutar los comandos Maven desde la raíz del proyecto. En IntelliJ, establecer esa carpeta como directorio de trabajo. Usar `update` en la primera ejecución y cambiarlo a `validate` después de crear las tablas.

Para comprobar la conexión y crear las tablas con `update`, ejecutar:

```powershell
./mvnw.cmd compile exec:java
```

Debe aparecer `Conexión con PostgreSQL verificada correctamente.`. Esta comprobación no crea una cuenta de la aplicación; para eso se utiliza el paso «Primer propietario» de abajo. La pantalla JavaFX sigue siendo la plantilla inicial hasta integrar los formularios.

También se pueden usar variables de entorno para PowerShell; tienen prioridad sobre los valores del archivo:

```powershell
$env:JAVA_HOME = 'C:/ruta/al/jdk-21'
$env:LATCH_DB_URL = 'jdbc:postgresql://localhost:5432/latch_db'
$env:LATCH_DB_USER = 'latch_local'
$clave = Read-Host 'Contraseña PostgreSQL' -AsSecureString
$env:LATCH_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $clave).Password
$env:LATCH_DB_SCHEMA_MODE = 'update'
```

`update` crea las tablas de esta entrega en una base de desarrollo. Después del primer inicio, usar `validate`, el valor predeterminado: verifica el esquema sin modificarlo. `none` desactiva esa verificación. No se aceptan `create` ni `create-drop` en la configuración de ejecución.

El archivo `.env` está ignorado por Git. Admite líneas `NOMBRE=valor`, líneas vacías, comentarios que empiezan con `#` y valores entre comillas simples o dobles. Los valores son literales: no se expanden variables ni secuencias de escape. No añadir comentarios al final de una credencial. No introducir credenciales ni parámetros en la URL JDBC; usuario y contraseña tienen sus propias variables.

## Primer propietario

Ejecutar una sola vez, con la aplicación detenida y la conexión configurada:

```powershell
$env:LATCH_INITIAL_USER = 'steven'
$claveInicial = Read-Host 'Contraseña inicial (12 a 128 caracteres)' -AsSecureString
$env:LATCH_INITIAL_PASSWORD = [System.Net.NetworkCredential]::new('', $claveInicial).Password
./mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.util.CrearPropietarioInicial'
Remove-Item Env:LATCH_INITIAL_PASSWORD
Remove-Item Env:LATCH_INITIAL_USER
$env:LATCH_DB_SCHEMA_MODE = 'validate'
```

La herramienta inicializa Hibernate, crea el propietario y su auditoría en una transacción, y cierra los recursos. Rechaza la operación si ya existen usuarios; serializa los instaladores mediante un bloqueo transaccional de PostgreSQL. Las cuentas posteriores se crean con `UsuarioService` y requieren una sesión de propietario.

## Ejecución y pruebas

```powershell
./mvnw.cmd test
./mvnw.cmd javafx:run
```

Las pruebas normales usan H2 en memoria y datos sintéticos. Comprueban autenticación, permisos, auditoría, rollback, restricciones y versiones concurrentes.

Para repetir la suite contra PostgreSQL, configurar las mismas variables `LATCH_DB_*` apuntando a **una base desechable exclusiva para pruebas**, y ejecutar:

```powershell
./mvnw.cmd test '-Dlatch.test.postgres=true'
```

Esta modalidad de pruebas crea y elimina las tablas de la suite; no debe apuntar a `latch_db` ni a una base con datos que se quieran conservar.

## Arquitectura y uso por el equipo

`JavaFX/FXML → Controller → Service → DAO → Hibernate → PostgreSQL`.

Se conserva el paquete existente `ni.edu.uam.proyectofinalprogramacionappescritorio`.

| Clase/paquete | Responsabilidad |
| --- | --- |
| `config/DatabaseConfig` | Configuración sin credenciales en código |
| `config/HibernateUtil` | Una SessionFactory compartida y registro de entidades |
| `config/HibernateTransactions` | Sesión por operación, commit, rollback y cierre |
| `entity/Usuario` | Cuenta, contraseña derivada, rol, estado y versión |
| `entity/Auditoria` | Actor relacionado, instante UTC, correlación, acción y referencia |
| `entity/Configuracion` | Parámetro de aplicación con control de versión; no guardar secretos aquí |
| `service/*` | Autenticación, permisos y operaciones auditadas |
| `util/PasswordHasher` | PBKDF2-HMAC-SHA256, sal aleatoria y comparación segura |
| `util/TareasSegundoPlano` | Cola acotada, ejecución de Task y cancelación al cerrar |

Los roles y permisos son **clases con constantes String**. No hay `enum` ni clases de excepciones personalizadas. Los errores de validación usan `IllegalArgumentException`, los de permisos `SecurityException` y los técnicos de persistencia `IllegalStateException`. El Controller muestra `getMessage()`; no muestra la causa técnica ni un stack trace al usuario.

Matriz inicial de permisos (propuesta de esta entrega, pendiente de contrastar con el PRD completo):

| Operación | Propietario | Asistente |
| --- | --- | --- |
| Gestionar usuarios y configuración | Sí | No |
| Consultar auditoría | Sí | No |
| Operar alquileres y consultar reportes | Sí | Sí |

Cada Service de los módulos futuros debe llamar a `PermisoService.exigir` dentro de su transacción. Deshabilitar un botón no sustituye esa comprobación. Se revisa el usuario activo y su rol actual en la base; cerrar `SesionUsuario` invalida esa sesión local.

Consultar [la guía de integración](docs/steven-infraestructura.md) para ejemplos y el recorrido de demostración.

## Problemas frecuentes

- `JAVA_HOME` inválido: apuntar a la carpeta del JDK 21, no a `bin`.
- Falta una variable: configurarla en la terminal o en Run Configuration de IntelliJ; reiniciar la ejecución después de cambiarla.
- Error de conexión: comprobar el servicio PostgreSQL, puerto, base, usuario y contraseña.
- Tabla ausente con `validate`: usar `update` sólo para inicializar la base de desarrollo y luego volver a `validate`.
- Datos duplicados o relaciones inválidas: se revierte la operación completa; corregir la entrada antes de reintentar.
- Registro modificado por otro usuario: recargar los datos y volver a aplicar el cambio.

## Alcance del equipo

- **Steven Cuadra:** infraestructura, seguridad, auditoría y concurrencia base (esta entrega).
- **Alfredo Montalvan:** entidades de alquileres, PostgreSQL, DAOs, CRUD y HQL relacionadas.
- **Caleb Tardencilla:** FXML, Controllers, validaciones y flujos JavaFX.
- **Aris Gavarrete:** recibos, tipo de cambio, reportes y documentación final.

Pendientes del equipo: integrar sus entidades en `HibernateUtil`, servicios de negocio, login y formularios, tareas H1–H5, reportes y evidencias de GitHub. El pool básico de Hibernate es suficiente para la demo académica; una distribución de producción requeriría revisar su configuración de conexiones y seguridad.
