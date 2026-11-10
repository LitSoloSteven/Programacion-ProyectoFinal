# Latch — Sistema de Gestión Inmobiliaria y Arrendamiento
## Entrega del Integrante 3: Caleb Tardencilla (@CTardencilla)
**Carrera:** Ingeniería de Sistemas — Universidad Americana (UAM)  
**Asignatura:** Programación de Aplicaciones de Escritorio  
**Rama de Trabajo:** `Caleb`  
**Tecnologías:** Java 21 (Temurin 21), JavaFX 21, FXML, PostgreSQL 18, JDBC, JUnit 5, Maven  

---

## 1. Resumen Ejecutivo del Alcance

En estricto cumplimiento del **Prompt Maestro de Rol y Responsabilidades**, el Integrante 3 — Caleb Tardencilla ha desarrollado la capa de presentación completa de la aplicación de escritorio **Latch**, asegurando que todas las vistas interactúen con **datos reales persistidos en PostgreSQL**, erradicando listas estáticas, mocks o simulaciones en memoria.

### Componentes Clave Desarrollados:
1. **Infraestructura Base y Configuración Maven:**
   - Actualización del compilador a Java 21 (`<release>21</release>`).
   - Integración del controlador `postgresql-42.7.2.jar`.
   - Fábrica de conexiones segura (`ConexionBD`) con soporte para variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`) y parámetros locales por defecto.
   - Inicializador automático de esquema (`DatabaseInitializer`) que crea el esquema `latch`, las tablas correspondientes y carga datos semilla de prueba.
2. **Autenticación y Seguridad Visual:**
   - Pantalla de inicio de sesión (`login-view.fxml` y `LoginController`).
   - Autenticación contra la tabla `latch.usuario` vinculada a `latch.rol`.
   - Control de sesión activa en memoria (`SesionUsuario`).
   - Control de permisos visuales: el rol *Asistente* tiene restringidas las operaciones destructivas de eliminación física en comparación con el *Propietario*.
3. **Dashboard Principal (`dashboard-view.fxml` y `DashboardController`):**
   - Menú de navegación lateral (Sidebar) persistente que previene duplicidad de ventanas.
   - Tarjetas métricas (KPIs) calculadas en tiempo real directamente desde PostgreSQL: propiedades activas, unidades en inventario, inquilinos activos, contratos vigentes, cargos pendientes e importes de pagos recibidos.
4. **Módulos CRUD Completos:**
   - **🏢 Propiedades (`PropiedadController`):** Registro, consulta, edición, baja/eliminación con confirmación y sincronización de `TableView`.
   - **🚪 Unidades (`UnidadController`):** Selección de propiedad mediante `ComboBox` con datos reales de PostgreSQL, validación de código único, canon mensual y tipo de unidad.
   - **👥 Inquilinos (`InquilinoController`):** Captura de arrendatarios con validación de correo RFC 5322, teléfono, cédula y baja lógica.
   - **📄 Contratos (`ContratoController`):** Vinculación estricta Unidad ↔ Inquilino, validación de coherencia temporal (inicio <= fin, duración mínima de 1 mes), día límite de cobro y depósito en garantía.
   - **📑 Cargos (`CargoController`):** Emisión de obligaciones financieras asociadas a contratos vigentes con validación de vencimiento e importes positivos.
   - **💵 Pagos (`PagoController`):** Cobro y conciliación de ingresos con métodos de pago (Efectivo, Transferencia, Depósito), referencia de comprobante y anulación controlada.
5. **Validaciones y Experiencia de Usuario:**
   - Clase utilitaria `ValidadorEntrada` para verificación preventiva de cadenas, números decimales, enteros y rangos de fechas.
   - Alertas uniformes (`AlertaUtil`) protegidas contra excepciones en hilos externos y con mensajes comprensibles al usuario.
   - Diálogos de confirmación antes de cualquier operación destructiva.
   - Diseño visual profesional con CSS bajo el lenguaje de diseño **Aurora UI**.

---

## 2. Credenciales de Acceso Inicial al Sistema

| Usuario | Contraseña | Rol / Perfil | Permisos |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | **Propietario** | Acceso total: creación, edición, baja lógica y eliminación física. |
| `asistente` | `asistente123` | **Asistente** | Operativo: creación, edición y cobranza. Eliminación física protegida. |

---

## 3. Instrucciones de Compilación y Ejecución

### 3.1. Requisitos
- JDK 21 (Eclipse Temurin 21 recomendado).
- PostgreSQL 18 en ejecución en `localhost:5432` con la base de datos `latch`.

### 3.2. Ejecución de Pruebas Unitarias y Funcionales
Desde la consola PowerShell en el directorio raíz del proyecto:

```powershell
$env:JAVA_HOME = "C:\Users\caleb\.jdks\temurin-21.0.12.1"
.\mvnw.cmd clean test
```

**Resultado:** 35 pruebas ejecutadas, 0 fallos, 0 errores, BUILD SUCCESS.

### 3.3. Ejecución de la Aplicación Gráfica JavaFX

```powershell
$env:JAVA_HOME = "C:\Users\caleb\.jdks\temurin-21.0.12.1"
.\mvnw.cmd javafx:run
```

---

## 4. Estructura Arquitectónica Implementada

```text
src/main/java/ni/edu/uam/proyectofinalprogramacionappescritorio/
├── controller/
│   ├── AlertaUtil.java
│   ├── CargoController.java
│   ├── ContratoController.java
│   ├── DashboardController.java
│   ├── InquilinoController.java
│   ├── LoginController.java
│   ├── PagoController.java
│   ├── PropiedadController.java
│   ├── UnidadController.java
│   └── ValidadorEntrada.java
├── dao/
│   ├── CargoDao.java
│   ├── ConexionBD.java
│   ├── ContratoDao.java
│   ├── DashboardDao.java
│   ├── DatabaseInitializer.java
│   ├── InquilinoDao.java
│   ├── PagoDao.java
│   ├── PropiedadDao.java
│   ├── UnidadDao.java
│   └── UsuarioDao.java
├── modelo/
│   ├── Cargo.java
│   ├── Contrato.java
│   ├── Inquilino.java
│   ├── Pago.java
│   ├── Propiedad.java
│   ├── SesionUsuario.java
│   ├── Unidad.java
│   └── Usuario.java
├── service/
│   ├── AuthService.java
│   ├── CargoService.java
│   ├── ContratoService.java
│   ├── DashboardService.java
│   ├── InquilinoService.java
│   ├── PagoService.java
│   ├── PropiedadService.java
│   └── UnidadService.java
├── HelloApplication.java
└── Launcher.java
```
