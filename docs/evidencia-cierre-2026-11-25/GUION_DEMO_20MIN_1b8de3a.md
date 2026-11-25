# Guion de Demostración (20 minutos) — Latch (`1b8de3a`)

- **Expositor responsable:** Steven Cuadra
- **Candidato:** rama `feature/reportes-vista-previa`, SHA `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`)
- **Base de datos de demostración:** `latch_demo` aislada en PostgreSQL 18 (preparada según [demo-docente.md](../demo-docente.md); **nunca** usar `latch_test_e1` ni datos reales).
- **Comando de arranque recomendado (fuera del IDE, con runtime incluido):** `iniciar-latch-sw.cmd` (usa `-Dprism.order=sw` para evitar el fallo nativo en `glass.dll` al abrir vista previa/recibo) o `iniciar-latch.cmd`.

---

## Cronograma propuesto (20 minutos)

| Bloque | Tiempo | Tema | Qué mostrar y comprobar |
| --- | --- | --- | --- |
| **1. Contexto y arquitectura** | `00:00–02:30` | Propósito y separación de capas | Explicar arquitectura `JavaFX → ClienteApi → Spring Boot (127.0.0.1) → Services → Hibernate → PostgreSQL 18`, invariantes (`BigDecimal` 2/8 decimales, separación USD/NIO, permisos en transacción, hora PostgreSQL). |
| **2. Acceso y tema (`M01`/`M11`)** | `02:30–05:00` | Login + Dashboard | Iniciar sesión como `fase7.demo` en `PAN-01`. Mostrar navegación por teclado (`Tab`), alternar tema **Claro/Oscuro**, abrir el Dashboard y verificar actualización de métricas. |
| **3. Catálogos (`M02`/`M03`)** | `05:00–08:30` | Propiedad, unidad e inquilino | Alta sintética de **Propiedad Fase 7**, unidad **F7-101** (`100.00 USD`) e **Inquilino Fase 7** (`demo-docente.md` pasos 2–3). Mostrar validaciones de entrada y filtros. |
| **4. Contrato y garantía (`M04`/`M07`)** | `08:30–11:30` | Alta, activación y unicidad | Seleccionar unidad e inquilino, canon `100.00`, día de pago `10`, depósito `0.00`, vigencia 1 año (`Vigente`). Explicar que `V6` impide dos contratos activos en la misma unidad. |
| **5. Tasas y cobro (`M09`/`M06`)** | `11:30–15:30` | Tasa autorizada, pago, idempotencia y recibo | Mostrar registro/autorización de tasa (`8` decimales) en **Tasas**. En **Pagos**, cobrar `25.00 USD` en `Efectivo` → **Revisar pago** → **Confirmar pago**. Abrir **Ver pago** y mostrar el consecutivo transaccional (`V8`), importe y moneda. |
| **6. Reportes y vista previa PDF (`M10`)** | `15:30–18:00` | Vista previa reutilizable y guardado atómico | Abrir vista previa PDF desde Pagos/Contratos/Dashboard (`RenderizadorVistaPreviaReporte`), probar zoom/paginación y guardar PDF sin temporales residuales. Explicar anulación trazable por Propietario (`motivo`, recibo `ANULADO` sin alterar consecutivo). |
| **7. Seguridad, brechas y cierre** | `18:00–20:00` | Logout y transparencia de alcance | Pulsar **Cerrar sesión** → vuelve a `PAN-01` y revoca token (`401`). Declarar brechas abiertas de alcance completo (`P1`–`P9`: Argon2id gradual, `M08`, `H1`–`H6`, `S1`, respaldo AES-GCM, `XLSX`, `ENT-14` en equipo alterno). |

---

## Plan de contingencia durante la exposición

| Riesgo | Acción inmediata |
| --- | --- |
| Cierre nativo en `glass.dll` al abrir recibo o vista previa | Usar `iniciar-latch-sw.cmd` (`-Dprism.order=sw`). |
| Unidad/nombre duplicado al repetir la demo | Agregar un sufijo nuevo a la unidad e inquilino (`F7-102`, etc.) según [demo-docente.md](../demo-docente.md). |
| Error `ERR-057` por reloj local desfasado | Sincronizar la hora de Windows con el servidor PostgreSQL (`< 5` minutos). |
| Fallo en demo interactiva en vivo | Mostrar los PDFs ya verificados en E3, el recorrido automatizado `recorrido_e3_api.py` y la puerta `./mvnw.cmd verify -Ppostgres -B` verde (`234` H2 + `37` PostgreSQL 18.6). |
