# Latch

Aplicación de escritorio para gestionar alquileres, propiedades, contratos, cobros y mantenimiento. Los recibos, estados de cuenta y demás documentos se consultan y exportan desde **Reportes**.

## Repositorios

| Repositorio | Contenido |
| --- | --- |
| [Latch API](https://github.com/LitSoloSteven/latch-api) | Servidor Spring Boot. Genera `target/ROOT.war` para Apache Tomcat 11 y contiene las migraciones de PostgreSQL. |

Este repositorio contiene la aplicación JavaFX y sus DTO HTTP en un solo proyecto Maven. El escritorio se comunica con la API por HTTP. Cada proyecto compila con su propio código del contrato.

## Instalación y actualización

Si ya usas Latch desde `main`, sigue la [guía de migración para el equipo](docs/ACTUALIZAR_DESDE_MAIN.md). Incluye instalación nueva, actualización con datos existentes, despliegue del WAR, arranque del escritorio y recuperación.

Para preparar los componentes desde el código fuente, usa JDK 21 y ejecuta los comandos en este orden, en la raíz de cada repositorio.

| Carpeta | Comando | Resultado |
| --- | --- | --- |
| `latch-api` | `./mvnw.cmd -B clean verify` | Ejecuta las pruebas y genera `target/ROOT.war`. |
| Este repositorio | `./mvnw.cmd -B clean test javafx:jlink` | Prueba el escritorio y genera `target/app.zip`. |

La instalación se ejecuta con PostgreSQL 18 y Tomcat 11 para la API. Despliega el WAR en un conector local `127.0.0.1`. Configura PostgreSQL en el servidor y define `LATCH_API_BASE_URL` en el escritorio con la dirección de la API, por ejemplo `http://127.0.0.1:8090`. El cliente empaquetado incluye su propia JVM.

Para probar ambos procesos juntos, coloca `latch-api` junto a este repositorio, compila la API primero y ejecuta `./mvnw.cmd -B verify` aquí.

## Referencias

- [Contrato HTTP de Latch](docs/API_Latch.md)
- [Controles de seguridad](docs/SEGURIDAD.md)
- [Migraciones oficiales de PostgreSQL](https://github.com/LitSoloSteven/latch-api/tree/main/docs/migrations)
