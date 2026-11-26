package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinador H6 de cierre controlado (RF-M12-08 / RN-30 / RNF-022):
 * impide nuevas ejecuciones, solicita cancelación cooperativa de H1, H2, H3 y H5,
 * permite finalizar trabajos en curso (incluido H4) hasta 10 segundos fuera del hilo
 * JavaFX y cierra recursos únicamente cuando todas las tareas liberaron.
 */
public final class CoordinadorCierreH6 {
    public static final Duration TIEMPO_LIMITE_DEFECTO = Duration.ofSeconds(10);

    public interface ParticipanteCierre {
        String nombre();

        boolean hayActividad();

        void solicitarCancelacion();

        boolean esperarTerminacion(Duration limite) throws InterruptedException;
    }

    public record ResultadoCierre(
            boolean completado,
            String codigoError,
            String mensaje,
            List<String> tareasPendientes) {

        public ResultadoCierre {
            tareasPendientes = tareasPendientes == null ? List.of() : List.copyOf(tareasPendientes);
        }

        public static ResultadoCierre ok() {
            return new ResultadoCierre(true, null, "Recursos cerrados correctamente.", List.of());
        }

        public static ResultadoCierre bloqueado(List<String> pendientes) {
            return new ResultadoCierre(
                    false,
                    TareasActivasException.CODIGO,
                    TareasActivasException.MENSAJE_MSG_061,
                    pendientes);
        }
    }

    private final List<ParticipanteCierre> participantes;
    private final Runnable bloquearNuevas;
    private final Runnable liberarBloqueoSiFalla;
    private final Runnable cerrarRecursos;
    private final AtomicBoolean cerrado = new AtomicBoolean(false);

    public CoordinadorCierreH6(List<ParticipanteCierre> participantes,
                                Runnable bloquearNuevas,
                                Runnable liberarBloqueoSiFalla,
                                Runnable cerrarRecursos) {
        this.participantes = List.copyOf(Objects.requireNonNull(participantes, "Faltan participantes."));
        this.bloquearNuevas = bloquearNuevas == null ? () -> { } : bloquearNuevas;
        this.liberarBloqueoSiFalla = liberarBloqueoSiFalla == null ? () -> { } : liberarBloqueoSiFalla;
        this.cerrarRecursos = Objects.requireNonNull(cerrarRecursos, "Falta la acción de cierre de recursos.");
    }

    public static CoordinadorCierreH6 paraAplicacion(TareasSegundoPlano tareas, Runnable cerrarRecursos) {
        return paraAplicacion(tareas, null, cerrarRecursos);
    }

    public static CoordinadorCierreH6 paraAplicacion(TareasSegundoPlano tareas,
                                                     ParticipanteCierre participanteAdicional,
                                                     Runnable cerrarRecursos) {
        List<ParticipanteCierre> lista = new ArrayList<>();
        if (participanteAdicional != null) {
            lista.add(participanteAdicional);
        }
        lista.add(new ParticipanteCierre() {
            @Override public String nombre() { return "H5: Reportes y exportación"; }
            @Override public boolean hayActividad() { return GestorH5Reportes.hayActividad(); }
            @Override public void solicitarCancelacion() { GestorH5Reportes.solicitarCancelacionActiva(); }
            @Override public boolean esperarTerminacion(Duration limite) throws InterruptedException {
                return GestorH5Reportes.esperarTerminacion(limite);
            }
        });
        if (tareas != null) {
            lista.add(new ParticipanteCierre() {
                @Override public String nombre() { return "H4/Tareas de interfaz en curso"; }
                @Override public boolean hayActividad() { return tareas.tieneActivas(); }
                @Override public void solicitarCancelacion() { }
                @Override public boolean esperarTerminacion(Duration limite) throws InterruptedException {
                    return tareas.esperarTareas(limite);
                }
            });
        }
        return new CoordinadorCierreH6(
                lista,
                () -> {
                    GestorH5Reportes.bloquearNuevas(true);
                    if (tareas != null) tareas.bloquearNuevas(true);
                },
                () -> {
                    GestorH5Reportes.bloquearNuevas(false);
                    if (tareas != null) tareas.bloquearNuevas(false);
                },
                () -> {
                    if (tareas != null) tareas.cerrar();
                    cerrarRecursos.run();
                });
    }

    public ResultadoCierre ejecutarCierre(Duration tiempoLimite) {
        if (cerrado.get()) {
            return ResultadoCierre.ok();
        }
        Duration presupuesto = tiempoLimite == null || tiempoLimite.isNegative()
                ? Duration.ZERO : tiempoLimite;
        bloquearNuevas.run();

        for (ParticipanteCierre participante : participantes) {
            participante.solicitarCancelacion();
        }

        long fechaLimiteNanos = System.nanoTime() + presupuesto.toNanos();
        List<String> pendientes = new ArrayList<>();
        boolean interrumpido = false;

        for (ParticipanteCierre participante : participantes) {
            long restanteNanos = Math.max(0L, fechaLimiteNanos - System.nanoTime());
            boolean termino;
            try {
                termino = participante.esperarTerminacion(Duration.ofNanos(restanteNanos));
            } catch (InterruptedException error) {
                interrumpido = true;
                termino = false;
            }
            if (!termino || participante.hayActividad()) {
                pendientes.add(participante.nombre());
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }

        if (!pendientes.isEmpty()) {
            liberarBloqueoSiFalla.run();
            return ResultadoCierre.bloqueado(pendientes);
        }

        if (cerrado.compareAndSet(false, true)) {
            cerrarRecursos.run();
        }
        return ResultadoCierre.ok();
    }
}
