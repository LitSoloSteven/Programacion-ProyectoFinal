# Manifiesto de Entrega y Huellas SHA-256 — Latch (`1b8de3a`)

- **Producto:** Latch `1.0.0`
- **Rama:** `feature/reportes-vista-previa`
- **SHA Git candidato:** `1b8de3adb6d87d87bd05655ccf7d523ed1bb936d` (`1b8de3a`)
- **Respaldo estable:** `main` en `c61187e0c2daf4e567db0cb728b3a5babb529085` (`c61187e`)
- **Runtime embebido:** Eclipse Temurin OpenJDK `21.0.12.1+1` (`Latch\runtime\bin\java.exe`)

## Copias del paquete ZIP verificadas (idénticas byte a byte)

| Copia | Ruta | Tamaño (bytes) | SHA-256 |
| --- | --- | ---: | --- |
| **Copia 1 (principal)** | `target/entrega-latch-1b8de3a/Latch-1.0.0-win64-1b8de3a.zip` | `110.065.894` | `7BFE6519726C8F1CD95020033E56055BD157D890F79EBB230D9E95B4DD884B6F` |
| **Copia 2 (respaldo externo)** | `C:\Users\stive\.gemini\antigravity\brain\0ac85865-a564-433f-a3fa-fb030058880b\entrega-copia-2\Latch-1.0.0-win64-1b8de3a.zip` | `110.065.894` | `7BFE6519726C8F1CD95020033E56055BD157D890F79EBB230D9E95B4DD884B6F` |

## Binarios internos del paquete (`Latch-1.0.0-win64-1b8de3a.zip`)

| Artefacto | Tamaño (bytes) | SHA-256 |
| --- | ---: | --- |
| `Latch/Latch.exe` | `453.632` | `F17AF7804676C3414C43B37F3275FD9E06029DBDA758456D532CD13EAA33D91B` |
| `Latch/app/ProyectoFInal-ProgramacionAppEscritorio-1.0-SNAPSHOT.jar` | `648.201` | `F805134E61278DE5519C8FABC13A23D494E7985E045233696C4D48BE45C075D1` |

## Contenido del paquete ZIP

- `Latch/` — `app-image` autocontenida con `Latch.exe`, `runtime/` (Java `21.0.12.1+1`) y `app/` (`95` JARs: aplicación + `94` dependencias de runtime).
- `migrations/` — `V1__propietario_perfil.sql` a `V10__instante_operativo_pago.sql`.
- `docs/` — `README.md`, `demo-docente.md`, `SEGURIDAD.md`, `API_Latch.md`, `LICENCIAS_TERCEROS.md`, `GUION_DEMO_20MIN_1b8de3a.md`.
- `.env.example`, `VERSION.txt` y lanzadores `.cmd` (`iniciar-latch.cmd`, `iniciar-latch-sw.cmd`, `iniciar-api.cmd`, `verificar-bd.cmd`, `crear-propietario-inicial.cmd`).

## Documentos de evidencia en el repositorio (`docs/evidencia-cierre-2026-11-25/`)

- [E1_linea_base_y_bloqueos.md](E1_linea_base_y_bloqueos.md)
- [E2_estabilizacion_y_congelamiento.md](E2_estabilizacion_y_congelamiento.md)
- [E3_verificacion_candidato.md](E3_verificacion_candidato.md)
- [E4_empaquetado_y_entrega.md](E4_empaquetado_y_entrega.md)
- [registro_aceptacion.md](registro_aceptacion.md)
- [LICENCIAS_TERCEROS.md](LICENCIAS_TERCEROS.md)
- [GUION_DEMO_20MIN_1b8de3a.md](GUION_DEMO_20MIN_1b8de3a.md)
