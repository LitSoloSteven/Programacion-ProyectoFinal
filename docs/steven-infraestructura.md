# Entrega de Steven: infraestructura y seguridad

## Qué está implementado

Se añadieron las tres entidades asignadas a Steven (Usuario, Auditoria y Configuracion), la conexión Hibernate/PostgreSQL, transacciones, autenticación, autorización base y ejecución de tareas JavaFX. Se mantuvieron las clases y el FXML de la plantilla para que Caleb continúe la interfaz.

Los tipos propios son clases; Rol y Permiso agrupan constantes String. No se crearon enumeraciones ni excepciones personalizadas. Las anotaciones JPA describen cómo Hibernate guarda las clases en tablas.

## Cómo conectar los servicios

La inicialización y las llamadas a base de datos deben ejecutarse en segundo plano. Este ejemplo muestra la composición de servicios; los Controllers reciben los servicios ya construidos:

```java
HibernateTransactions transacciones = new HibernateTransactions(HibernateUtil.getSessionFactory());
AutenticacionService autenticacion = new AutenticacionService(transacciones);
UsuarioService usuarios = new UsuarioService(transacciones);
ConfiguracionService configuracion = new ConfiguracionService(transacciones);

SesionUsuario sesion = autenticacion.autenticar(nombre, password);
Long id = usuarios.crear(sesion, "asistente", nuevaPassword, Rol.ASISTENTE);
configuracion.guardar(sesion, "moneda", "NIO", -1);
Configuracion actual = configuracion.consultar(sesion, "moneda");
configuracion.guardar(sesion, "moneda", "USD", actual.getVersion());
sesion.close();
```

`-1` significa crear una configuración nueva. Para editar hay que enviar la versión leída; así una pantalla con datos obsoletos no sobrescribe cambios ajenos. No guardar credenciales en Configuracion ni incluir contraseñas, hashes o datos sensibles en referencias de auditoría.

La contraseña se recibe como `char[]`. El Controller debe limpiar el arreglo con `Arrays.fill(password, '\0')` después del login y vaciar el campo visual. La contraseña nunca se almacena en texto plano.

## Integración del modelo de propietario

Los datos de contacto de `Propietario` viven en su propia entidad y se identifican con el mismo ID del usuario autenticado. Cada propiedad puede enlazar ese perfil. El formulario de propiedades crea o actualiza el perfil del usuario activo; un correo por perfil debe ser único. Para bases existentes, aplicar una vez `docs/migrations/V1__propietario_perfil.sql` antes de iniciar con `LATCH_DB_SCHEMA_MODE=validate`. Las propiedades anteriores conservan `propietario_id` nulo hasta que se editen.

El depósito pactado continúa en el contrato. `Garantia` ahora mantiene por separado el monto inicial y el saldo retenido, que puede reducirse sin alterar el contrato. Los contratos previos se inicializan con el depósito pactado al aplicar `docs/migrations/V2__seguimiento_garantias.sql`; aplícala después de V1. Los nuevos contratos crean automáticamente su seguimiento con el saldo inicial igual al depósito pactado. `docs/migrations/V3__descripcion_unidad.sql` añade el campo de descripción de Alfredo. Aplicar V1, V2 y V3 en ese orden antes de iniciar una base existente con modo `validate`.

## Contrato de integración para Alfredo

- Registrar cada nueva entidad en el método `HibernateUtil.construir`, junto con las tres existentes. Al construir una factory aislada también se pueden pasar entidades adicionales como argumentos.
- El DAO recibe una `Session`; no la abre ni la cierra y no hace commit.
- El Service envuelve toda la operación en `HibernateTransactions.ejecutar`.
- Dentro de esa operación, obtener al actor con `PermisoService.exigir(session, identidad, Permiso.OPERAR_ALQUILERES)`.
- Persistir cambios y registrar auditoría con esa misma sesión y una correlación UUID para la operación.
- Si algo falla, lanzar el error. La infraestructura revierte la transacción y cierra la sesión. No capturar un error dentro de la operación para continuar y confirmar datos parciales.
- Cada hilo tiene su propia sesión. Compartir SessionFactory es correcto; compartir Session o una entidad administrada entre hilos no lo es.
- Los DTOs o datos para la pantalla deben construirse dentro de la transacción; evitar acceder a relaciones lazy después del cierre.

## Contrato para Caleb y Aris

Usar una Task para operaciones lentas y registrar los eventos antes de enviarla al ejecutor. Este ejemplo pertenece a un Controller que ya recibió `autenticacion` y el ejecutor `tareas` de la aplicación:

```java
Task<SesionUsuario> tarea = new Task<SesionUsuario>() {
    @Override
    protected SesionUsuario call() {
        try {
            return autenticacion.autenticar(nombre, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
};
tarea.setOnSucceeded(evento -> mostrarPanel(tarea.getValue()));
tarea.setOnFailed(evento -> mostrarError(tarea.getException().getMessage()));
tarea.setOnCancelled(evento -> restaurarFormulario());
tareas.ejecutar(tarea);
```

Los métodos `mostrarPanel`, `mostrarError` y `restaurarFormulario` son puntos de integración del Controller de Caleb. Los eventos de Task llegan al hilo JavaFX; `call()` trabaja en segundo plano y no debe tocar controles.

El ejecutor tiene dos hilos y una cola de 32 tareas. Manejar `RejectedExecutionException` para informar que está ocupado y restaurar la pantalla. Una tarea cancelada puede no entrar a `call()`; limpiar también los datos sensibles en su evento de cancelación.

`HelloApplication.stop()` cancela las tareas, espera hasta diez segundos desde un hilo de cierre y cierra Hibernate. Las tareas largas deben consultar `isCancelled()` e interrupción entre lotes y salir; cancelar una Task no garantiza que una operación ya confirmada en base de datos sea revertida. No implementar reintentos automáticos de pagos sin resolver antes su idempotencia.

## Demostración de esta entrega

1. Ejecutar `./mvnw.cmd test` y mostrar las pruebas sin fallos.
2. Configurar la base local y crear el propietario con el comando del README.
3. Mostrar Usuario, Auditoria y Configuracion en PostgreSQL y la FK de auditoría.
4. Mostrar `HibernateTransactions`: inicio, commit, rollback y cierre de sesión.
5. Mostrar la prueba que inserta configuración y auditoría, hace flush, provoca un error y comprueba que no quedó ninguno de los dos registros.
6. Mostrar que el asistente no puede crear cuentas ni modificar configuración.
7. Mostrar la prueba de versiones: dos sesiones leen el mismo registro y la segunda escritura se rechaza.
8. Explicar el cierre de tareas y el registro de contraseñas mediante hash con sal.

Las pruebas normales usan H2 como ayuda rápida. Para evidenciar el comportamiento en el motor de la entrega, repetir la suite en una base PostgreSQL desechable con `-Dlatch.test.postgres=true`, tal como indica el README.

Verificación realizada el 10 de noviembre de 2026: compilación con JDK 21, 11 pruebas correctas con H2 y las mismas 11 correctas con PostgreSQL 18. También se ejecutó correctamente la creación del propietario inicial contra una instancia PostgreSQL temporal. Los formularios y el cierre de tareas JavaFX requieren su prueba funcional cuando Caleb integre las pantallas.

## Límites e integración pendiente

Los permisos son una matriz inicial porque el PDF de planificación no incluye el detalle completo del PRD. Se deben confirmar antes de conectar operaciones financieras. Hay auditoría de instalaciones, inicios de sesión correctos, creación/desactivación de usuarios y cambios de configuración. Las acciones de alquileres deberán añadir sus propios registros; los intentos fallidos de login no se persisten como acciones exitosas.

La autenticación es una base local, sin recuperación de contraseña ni protección adicional contra intentos repetidos. La interfaz de login corresponde a Caleb. La rúbrica completa también depende de las entidades, CRUD, HQL y reportes de los demás integrantes; esta entrega no los declara terminados.
