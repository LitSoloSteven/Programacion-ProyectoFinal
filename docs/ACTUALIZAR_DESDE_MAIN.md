# Actualizar Latch desde main

La versión anterior ejecuta JavaFX y su API en la misma aplicación. La versión nueva se instala con un servidor Spring Boot y un escritorio JavaFX que arrancan por separado. La configuración de PostgreSQL y los adjuntos pasan al servidor. Conserva tu copia actual de `main` para poder volver a ella.

## 1. Prepara tu equipo y clona los proyectos

Para compilar en Windows necesitas Git, JDK 21 y los clientes de PostgreSQL 18 (`psql`, `pg_dump`, `pg_restore`). Para desplegar el servidor necesitas Apache Tomcat 11 y PostgreSQL 18 accesible desde la API. Comprueba que `JAVA_HOME` apunta al JDK y que `java -version` y `psql --version` muestran las versiones esperadas.

Abre PowerShell en una carpeta que pueda contener los tres proyectos hermanos. Si ya tienes alguno clonado, comprueba su rama y su estado antes de actualizarlo.

```powershell
git clone https://github.com/LitSoloSteven/latch-contrato.git
git clone https://github.com/LitSoloSteven/latch-api.git
git clone https://github.com/LitSoloSteven/latch-cliente.git
```

El proyecto principal sirve como entrada a esta documentación. Puedes conservar tu checkout de `main` y abrir los tres proyectos nuevos desde sus propias carpetas. En IntelliJ IDEA, abre el `pom.xml` de cada proyecto por separado y selecciona JDK 21. Usa PowerShell para la primera compilación y para verificar la configuración de Tomcat.

## 2. Compila los componentes

Usa el mismo usuario de Windows en los tres comandos para que Maven encuentre la biblioteca instalada en su caché local. Desde la carpeta que contiene los tres repositorios, ejecuta este bloque.

```powershell
$compilaciones = @(
    @{ Carpeta = 'latch-contrato'; Objetivos = @('clean', 'install') },
    @{ Carpeta = 'latch-api'; Objetivos = @('clean', 'verify') },
    @{ Carpeta = 'latch-cliente'; Objetivos = @('clean', 'verify', 'javafx:jlink') }
)
foreach ($proyecto in $compilaciones) {
    Push-Location -LiteralPath $proyecto.Carpeta
    try {
        $objetivos = $proyecto.Objetivos
        .\mvnw.cmd -B @objetivos
        if ($LASTEXITCODE -ne 0) { throw "Falló la compilación de $($proyecto.Carpeta)" }
    } finally {
        Pop-Location
    }
}
```

El servidor queda en `latch-api\target\ROOT.war`. El escritorio queda en `latch-cliente\target\app.zip`. La comprobación de dos JVM del cliente utiliza el WAR y las clases de prueba de `latch-api`, por eso se compila primero la API y se mantienen las carpetas hermanas.

## 3. Respalda y migra los datos

### Si ya tienes datos en main

1. Cierra la aplicación anterior y confirma que dejó de escribir. Conserva su configuración local, y copia la carpeta de adjuntos si la versión anterior ya la usaba. Mantén las credenciales y los respaldos fuera de Git.
2. Haz un respaldo de la base actual en formato personalizado y comprueba que el catálogo del respaldo sea legible. Con estos valores de ejemplo, `latch_db` es la fuente y `latch_migracion` es una base nueva para el ensayo. Usa tus datos de conexión reales si los nombres, el puerto o el rol son distintos.

```powershell
New-Item -ItemType Directory -Force -Path 'C:\Latch\respaldos' | Out-Null
pg_dump -h 127.0.0.1 -p 5432 -U tu_usuario -Fc -f C:\Latch\respaldos\latch-main.dump latch_db
if ($LASTEXITCODE -ne 0) { throw 'Falló el respaldo' }
pg_restore -l C:\Latch\respaldos\latch-main.dump
if ($LASTEXITCODE -ne 0) { throw 'El respaldo no se puede leer' }
```

3. Crea una base separada llamada `latch_migracion`, con permisos para restaurar y aplicar DDL. Restaura allí la copia y, si existen adjuntos, usa también una copia de su carpeta. La base de `main` queda disponible para volver al estado anterior.

```powershell
createdb -h 127.0.0.1 -p 5432 -U tu_usuario -O tu_usuario latch_migracion
if ($LASTEXITCODE -ne 0) { throw 'Pide al administrador que cree una base vacía de ensayo' }
pg_restore -h 127.0.0.1 -p 5432 -U tu_usuario -d latch_migracion --exit-on-error --single-transaction C:\Latch\respaldos\latch-main.dump
if ($LASTEXITCODE -ne 0) { throw 'Falló la restauración de prueba' }
```

4. Confirma con quien administra la base hasta qué migración está aplicada. El código de la versión anterior de `main` incluye V1 a V10. El estado de cada base depende de las migraciones que ejecutó su responsable. Si la copia está comprobada hasta V10, aplica V11 a V16. Si está en otro estado, selecciona únicamente la secuencia pendiente. Si no puedes identificarla, detente antes de aplicar DDL y revisa el esquema y los archivos SQL con el responsable.
5. Desde la raíz de `latch-api`, ejecuta el siguiente bloque **solo para una copia confirmada en V10**. El lote se ejecuta en una transacción y se revierte si un archivo falla.

```powershell
$primera = 11
$ultima = 16
$migraciones = Get-ChildItem -LiteralPath 'docs/migrations' -Filter 'V*.sql' -File |
    ForEach-Object {
        if ($_.Name -match '^V(\d+)__.*\.sql$') {
            [pscustomobject]@{ Version = [int]$Matches[1]; Ruta = $_.FullName }
        }
    } | Where-Object { $_.Version -ge $primera -and $_.Version -le $ultima } |
    Sort-Object Version
if (@($migraciones).Count -ne ($ultima - $primera + 1)) { throw 'Faltan migraciones' }
$argumentosPsql = @('-X', '-h', '127.0.0.1', '-p', '5432', '-U', 'tu_usuario',
    '-d', 'latch_migracion', '-v', 'ON_ERROR_STOP=1', '--single-transaction')
foreach ($archivo in $migraciones) { $argumentosPsql += @('-f', $archivo.Ruta) }
psql @argumentosPsql
if ($LASTEXITCODE -ne 0) { throw 'Falló la migración; revisa el primer error' }
```

Si V11 indica que el rol carece de acceso a un objeto grande de una columna histórica `respuesta_json` de tipo `oid`, el propietario de ese objeto debe concederle lectura sobre el OID indicado. Revisa la [migración V11](https://github.com/LitSoloSteven/latch-api/blob/main/docs/migrations/V11__idempotencia_respuesta_text.sql) con el responsable antes de reintentar.

### Si estás instalando en una base vacía

Crea una base vacía de tu propiedad. Ejecuta el bloque de migraciones con `$primera = 0`, `$ultima = 16` y el nombre de esa base. V0 instala el esquema inicial. Configura la API con `validate` y crea el usuario inicial solo si aún no hay usuarios. Ejecuta esto desde `latch-api`, con la API apagada y con el archivo `.env` del siguiente paso ya configurado.

```powershell
$env:LATCH_INITIAL_USER = Read-Host 'Usuario del primer propietario'
$claveInicial = Read-Host 'Contraseña inicial (12 a 128 caracteres)' -AsSecureString
$env:LATCH_INITIAL_PASSWORD = [Net.NetworkCredential]::new('', $claveInicial).Password
try {
    .\mvnw.cmd compile exec:java '-Dexec.mainClass=ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas.CrearPropietarioInicial'
    if ($LASTEXITCODE -ne 0) { throw 'Falló la creación del propietario' }
} finally {
    Remove-Item Env:LATCH_INITIAL_USER, Env:LATCH_INITIAL_PASSWORD
    Remove-Variable claveInicial
}
```

## 4. Configura y despliega la API

1. En `latch-api`, crea un archivo `.env` a partir de `.env.example`. Si ya existe, edítalo preservando los valores válidos. Completa las variables en el archivo local con el rol, la contraseña y la base que verificaste. Usa una carpeta de adjuntos persistente y con permiso de escritura para la cuenta que ejecutará Tomcat.

```dotenv
LATCH_DB_URL=jdbc:postgresql://127.0.0.1:5432/latch_migracion
LATCH_DB_USER=tu_usuario
LATCH_DB_PASSWORD=tu_clave
LATCH_DB_SCHEMA_MODE=validate
LATCH_API_PORT=8090
LATCH_ADJUNTOS_DIR=C:/Latch/datos/adjuntos
LATCH_PG_BIN_DIR=C:/Program Files/PostgreSQL/18/bin
```

2. Usa una instancia de Tomcat dedicada a Latch. En `conf/server.xml`, ajusta el conector HTTP existente para que tenga `address="127.0.0.1"` y `port="8090"`. El puerto de Tomcat se define en ese conector. `LATCH_API_PORT` se utiliza en el modo embebido.
3. Con Tomcat apagado, deja libre el contexto raíz de esa instancia. Si trae una aplicación de bienvenida `webapps/ROOT`, muévela fuera de `webapps`. Copia `latch-api\target\ROOT.war` a `webapps\ROOT.war`.
4. En `bin\setenv.bat` del Tomcat, apunta la propiedad del servidor a tu archivo `.env`. Sustituye la ruta de ejemplo por la absoluta de tu equipo.

```bat
set CATALINA_OPTS=%CATALINA_OPTS% -Dlatch.env.path="C:/Latch/latch-api/.env"
```

5. Ejecuta `bin\catalina.bat run` desde una terminal. Desde otra, comprueba que el servidor responde antes de abrir el escritorio.

```powershell
Invoke-RestMethod 'http://127.0.0.1:8090/api/v1/salud'
```

Si se detiene buscando tablas o columnas, revisa la base configurada y la última migración aplicada conservando `validate`. Si el puerto está ocupado, cierra la aplicación anterior o configura otro puerto de loopback en Tomcat y en el escritorio. Para desarrollo local hay un arranque alternativo con `./mvnw.cmd compile exec:java` desde `latch-api`. Ejecuta un modo de servidor a la vez.

## 5. Abre el escritorio y comprueba la instalación

Descomprime `latch-cliente\target\app.zip` en una carpeta permanente. En PowerShell, define la URL de la API y ejecuta `app.bat` desde la carpeta `bin` de la imagen. La dirección base termina en el puerto para `ROOT.war`. El escritorio agrega `/api/v1` a sus solicitudes.

```powershell
$env:LATCH_API_BASE_URL = 'http://127.0.0.1:8090'
.\app.bat
```

En IntelliJ puedes usar la configuración **Latch** del repositorio del cliente, con `LATCH_API_BASE_URL` ajustada a tu instalación. Desde la raíz de `latch-cliente`, el comando equivalente es `./mvnw.cmd javafx:run`.

Con la copia de ensayo, comprueba estos recorridos:

- Inicia sesión con tu usuario existente. Verifica que aparecen las propiedades, contratos activos y registros de cobro que esperabas.
- Como propietario, registra una operación sintética y verifica que el panel correspondiente se actualiza y muestra el estado correcto. Comprueba también usuarios y parámetros.
- Abre **Reportes**, localiza el recibo por su número, genera un PDF y ábrelo. Genera un XLSX de un reporte que admita ese formato. Compara recibo, moneda, fecha y saldos con la operación.
- Repite con una cuenta asistente. Comprueba que puede realizar sus operaciones habituales y que las secciones exclusivas del propietario quedan restringidas.
- Cierra el cliente y vuelve a abrirlo. Reinicia Tomcat y revisa que sigan visibles los datos y adjuntos. Haz un respaldo desde la API y ensaya su restauración sobre otra base y carpeta de adjuntos aisladas.

## 6. Cambia de versión y conserva un punto de retorno

Cuando el ensayo pase, acuerda una ventana con el equipo. Detén la versión anterior y toma un respaldo final. Si hubo operaciones después del ensayo, repite desde ese respaldo final en una base nueva antes de habilitar la versión nueva. Configura la API para usar la nueva base y su carpeta de adjuntos. Mantén la base y los archivos de la versión anterior bajo resguardo.

Si falla el arranque, detén el cliente y Tomcat, y vuelve a abrir la versión anterior con su configuración y base originales. Si la versión nueva ya registró operaciones reales, respalda esa base y concilia las operaciones con el equipo antes de volver a escribir en la anterior.
