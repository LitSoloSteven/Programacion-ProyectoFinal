# Demo de Latch para el docente

## Preparación

Usa PostgreSQL 18 con una base vacía llamada `latch_demo` y un rol local que
sea su propietario. Reserva otro nombre de base para `verify -Ppostgres`.
Ejecuta los comandos desde PowerShell, en la raíz del repositorio, con JDK 21
y `psql` disponibles. Prepara `$cp` y `$paquete` siguiendo el
[README](../README.md#jar-y-demostración-en-windows).

```powershell
$env:LATCH_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/latch_demo'
$env:LATCH_DB_USER = 'latch_demo'
$claveBd = Read-Host 'Contraseña del rol de demo' -AsSecureString
$env:LATCH_DB_PASSWORD = [Net.NetworkCredential]::new('', $claveBd).Password
$env:LATCH_API_PORT = '48157'
$env:LATCH_DB_SCHEMA_MODE = 'update'
java -cp $cp "$paquete.config.HibernateUtil"
if ($LASTEXITCODE -ne 0) { throw 'Falló la creación del esquema de demo' }
```

Cuando Hibernate haya creado el esquema, aplica V1 a V10 en orden y vuelve a
`validate`. Ejecuta la creación del usuario una sola vez. Usa una contraseña
exclusiva de la demo de 12 a 128 caracteres y consérvala para el ingreso.

```powershell
$migraciones = Get-ChildItem docs/migrations -Filter 'V*.sql' -File |
    Sort-Object { [int]($_.BaseName -replace '^V(\d+)__.*$', '$1') }
$argumentos = @('-X', '-h', '127.0.0.1', '-U', $env:LATCH_DB_USER,
    '-d', 'latch_demo', '-v', 'ON_ERROR_STOP=1')
foreach ($archivo in $migraciones) { $argumentos += @('-f', $archivo.FullName) }
psql @argumentos
if ($LASTEXITCODE -ne 0) { throw 'Falló una migración de la base de demo' }
$env:LATCH_DB_SCHEMA_MODE = 'validate'
$env:LATCH_INITIAL_USER = 'fase7.demo'
$claveDemo = Read-Host 'Contraseña de fase7.demo' -AsSecureString
$env:LATCH_INITIAL_PASSWORD = [Net.NetworkCredential]::new('', $claveDemo).Password
try {
    java -cp $cp "$paquete.herramientas.CrearPropietarioInicial"
    if ($LASTEXITCODE -ne 0) { throw 'Falló la creación del usuario de demo' }
} finally {
    Remove-Item Env:LATCH_INITIAL_PASSWORD, Env:LATCH_INITIAL_USER
}
```

En el equipo donde falló `glass.dll`, arranca con
`java '-Dprism.order=sw' -cp $cp "$paquete.Launcher"`. Si el puerto 48157 está
ocupado, ajusta `LATCH_API_PORT` antes de iniciar.

## Recorrido

| Paso | Acción y resultado visible |
| --- | --- |
| 1. Login | Ingresa como `fase7.demo`. Abre el dashboard y comprueba que se actualizan las métricas. |
| 2. Propiedad | Crea **Propiedad Fase 7** en **Avenida sintética 51**, con **Propietario sintético**, `propietario@example.test` y **8888-1234**. Comprueba que aparece en la tabla. |
| 3. Unidad e inquilino | Crea la unidad **F7-101** en esa propiedad, con canon **100.00**, y el inquilino **Inquilino Fase 7**, con `inquilino@example.test`. |
| 4. Contrato | Selecciona la unidad y el inquilino. Usa canon **100.00**, día de pago **10**, depósito **0.00** y vigencia desde hoy hasta dentro de un año. Comprueba que se lista el contrato. |
| 5. Cobro | En **Pagos**, elige el contrato, importe **25.00** en la moneda del contrato y método **Efectivo**. Pulsa **Revisar pago** y **Confirmar pago**. |
| 6. Recibo | Abre **Ver pago** y muestra el número de recibo, el importe y la moneda. |
| 7. Logout | Cierra el recibo, selecciona **Cerrar sesión** y confirma. Debe volver la pantalla de login. |

Para repetir el recorrido en la misma base, usa un código de unidad y nombres
con un sufijo nuevo. Cierra la ventana antes de iniciar la API independiente
en el mismo puerto.
