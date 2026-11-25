# Auditoría integrada de C2 (P2–P5)

Revisión del 25 de noviembre de 2026 sobre la rama de trabajo `feature/p1-base-seguridad-admin`, con base `1b8de3a` y cambios locales sin commit. La auditoría no reemplaza la evidencia histórica de los paquetes ni corresponde al ZIP de E4.

| Área | Resultado |
| --- | --- |
| P2 · Contratos y garantías | **Verificado en servicio y HTTP con H2.** Se conservó la cobertura del ciclo de contratos, la liquidación y el rollback. El perfil PostgreSQL validó el esquema y las restricciones de migración. [Evidencia de P2](C2_P2_verificacion_contratos_garantias.md) |
| P3 · Cobranza y pagos | **Verificado en H2 y PostgreSQL**, con la cobertura del perfil y dos escenarios adicionales con dos JVM reales sobre PostgreSQL. Pasaron 100 recibos contiguos, diez rollbacks, la anulación posterior y la colisión de dos pagos sobre el mismo saldo. Se corrigió el tiempo límite de los procesos de prueba. Cerrar el diálogo H1 durante un lote ahora solicita cancelar y conserva el diálogo hasta que termine. [Evidencia de P3](C2_P3_verificacion_cobranza_pagos.md) |
| P4 · Tasas | **Verificado con S1 en Spring Boot** y el cliente HTTP saliente de Latch. El proveedor real con credencial y la variante LAN entre equipos siguen **No probados** en esta auditoría. [Evidencia de P4](C2_P4_cliente_https_y_S1.md) |
| P5 · Mantenimiento y adjuntos | **Verificado en servicio y HTTP con H2** para solicitudes, gastos y adjuntos. V16 pasó en la instalación directa y la validación del esquema en PostgreSQL. [Evidencia de P5](C2_P5_mantenimiento_y_adjuntos.md) |

- Instalación limpia: `psql.exe -X -v ON_ERROR_STOP=1 --single-transaction`, aplicando V0 a V16 en orden sobre una base vacía desechable. **Aprobada**.
- Suite integrada: `./mvnw.cmd -o -B verify -Ppostgres` en PostgreSQL 18.6 desechable. **292 pruebas de la suite normal y 40 de PostgreSQL**, sin fallos, errores ni omisiones.
- Concurrencia: `./mvnw.cmd -o -B -Dtest=PagosConcurrenciaDosJvmTest -Dlatch.test.postgres=true test`, con `LATCH_TEST_DB_*` de una base nueva y exclusiva. **2 pruebas aprobadas**. Las JVM hijas recibieron las credenciales por el entorno.
- Ajuste final del diálogo: después de la suite integrada se recompiló el controlador y se ejecutó `./mvnw.cmd -o -B -Dtest=FxmlControladoresTest,EstilosCssTest,ContrasteTest,CobranzaH1H2ServiceTest test`. **10 pruebas aprobadas**.
- Humo interactivo: **No probado** en esta auditoría. Ejecutar `./mvnw.cmd javafx:run` contra datos sintéticos y recorrer contratos/garantías, H1/H2/pagos, tasas y mantenimiento/adjuntos. En H1, cerrar mientras se procesa un lote y comprobar que se detiene entre contratos y muestra el resultado final.

El cierre formal requiere además la revisión humana G6 de dinero, permisos y migraciones. Invalidaría la evidencia técnica un hueco tras rollback en PostgreSQL, saldo negativo bajo concurrencia, una migración fallida o que el lote siga procesando nuevos contratos tras aceptar la cancelación.
