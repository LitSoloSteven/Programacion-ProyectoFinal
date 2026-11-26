package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Coordina recargas en segundo plano por vista: agrupa ráfagas, cancela tareas
 * previas, descarta respuestas obsoletas al cambiar filtros/panel/sesión y
 * ofrece consulta periódica acotada sin bloquear el hilo de JavaFX.
 */
public final class RecargadorVista {
    private final TareasSegundoPlano ejecutor;
    private final BooleanSupplier vistaActiva;
    private final BooleanSupplier sesionActiva;
    private final LongSupplier generacionSesion;
    private final BooleanProperty cargando = new SimpleBooleanProperty(false);

    private long versionActual;
    private Task<?> tareaActiva;
    private PauseTransition esperaPendiente;
    private Timeline consultaPeriodica;
    private boolean desactivado;
    private boolean cargadoUnaVez;

    public RecargadorVista(TareasSegundoPlano ejecutor,
                           BooleanSupplier vistaActiva,
                           BooleanSupplier sesionActiva,
                           LongSupplier generacionSesion) {
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
        this.vistaActiva = Objects.requireNonNull(vistaActiva, "vistaActiva");
        this.sesionActiva = Objects.requireNonNull(sesionActiva, "sesionActiva");
        this.generacionSesion = Objects.requireNonNull(generacionSesion, "generacionSesion");
    }

    public static RecargadorVista paraNodo(Node raiz, TareasSegundoPlano ejecutor) {
        Objects.requireNonNull(raiz, "raiz");
        AtomicBoolean montada = new AtomicBoolean(raiz.getScene() != null);
        RecargadorVista recargador = new RecargadorVista(
                ejecutor,
                () -> !montada.get() || raiz.getScene() != null,
                SesionCliente::haySesionActiva,
                SesionCliente::generacionActual);
        raiz.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                montada.set(true);
            } else if (anterior != null) {
                recargador.desactivar();
            }
        });
        return recargador;
    }

    public static RecargadorVista paraVistaReutilizable(Node raiz,
                                                        BooleanSupplier vistaVisible,
                                                        TareasSegundoPlano ejecutor) {
        Objects.requireNonNull(raiz, "raiz");
        Objects.requireNonNull(vistaVisible, "vistaVisible");
        AtomicBoolean montada = new AtomicBoolean(raiz.getScene() != null);
        RecargadorVista recargador = new RecargadorVista(
                ejecutor,
                () -> (!montada.get() || raiz.getScene() != null) && vistaVisible.getAsBoolean(),
                SesionCliente::haySesionActiva,
                SesionCliente::generacionActual);
        raiz.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                montada.set(true);
            } else if (anterior != null) {
                recargador.desactivar();
            }
        });
        return recargador;
    }

    public ReadOnlyBooleanProperty cargandoProperty() {
        return cargando;
    }

    public boolean isCargando() {
        return cargando.get();
    }

    public boolean haCargado() {
        return cargadoUnaVez;
    }

    public <T> void recargar(Supplier<T> consulta,
                             Consumer<T> alCompletar,
                             Consumer<Throwable> alFallar) {
        Objects.requireNonNull(consulta, "consulta");
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        if (desactivado || !vistaActiva.getAsBoolean() || !sesionActiva.getAsBoolean()) {
            return;
        }
        detenerEsperaPendiente();
        cancelarTareaEnVuelo();

        long version = ++versionActual;
        long generacion = generacionSesion.getAsLong();
        cargando.set(true);

        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return consulta.get();
            }
        };
        tareaActiva = tarea;
        tarea.setOnCancelled(evento -> {
            if (version == versionActual) {
                tareaActiva = null;
                cargando.set(false);
            }
        });

        TareasJavaFx.ejecutar(ejecutor, tarea, resultado -> {
            boolean vigente = esVigente(version, generacion);
            if (version == versionActual) {
                tareaActiva = null;
                cargando.set(false);
            }
            if (!vigente) {
                return;
            }
            cargadoUnaVez = true;
            alCompletar.accept(resultado);
        }, error -> {
            boolean vigente = esVigente(version, generacion);
            if (version == versionActual) {
                tareaActiva = null;
                cargando.set(false);
            }
            if (!vigente || tarea.isCancelled() || esInterrupcion(error)) {
                return;
            }
            alFallar.accept(error);
        });
    }

    public <T> void recargarAgrupado(Duration espera,
                                     Supplier<T> consulta,
                                     Consumer<T> alCompletar,
                                     Consumer<Throwable> alFallar) {
        Objects.requireNonNull(consulta, "consulta");
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        if (desactivado || !vistaActiva.getAsBoolean() || !sesionActiva.getAsBoolean()) {
            return;
        }
        if (espera == null || espera.lessThanOrEqualTo(Duration.ZERO)) {
            recargar(consulta, alCompletar, alFallar);
            return;
        }
        detenerEsperaPendiente();
        PauseTransition pausa = new PauseTransition(espera);
        esperaPendiente = pausa;
        pausa.setOnFinished(evento -> {
            if (esperaPendiente == pausa) {
                esperaPendiente = null;
            }
            recargar(consulta, alCompletar, alFallar);
        });
        pausa.play();
    }

    public <T> void recargarSiInactivo(Supplier<T> consulta,
                                       Consumer<T> alCompletar,
                                       Consumer<Throwable> alFallar) {
        if (desactivado || isCargando() || tieneEsperaPendiente()
                || !vistaActiva.getAsBoolean() || !sesionActiva.getAsBoolean()) {
            return;
        }
        recargar(consulta, alCompletar, alFallar);
    }

    public void activarConsultaPeriodica(Duration intervalo, Runnable disparador) {
        Objects.requireNonNull(intervalo, "intervalo");
        Objects.requireNonNull(disparador, "disparador");
        detenerConsultaPeriodica();
        Timeline timeline = new Timeline(new KeyFrame(intervalo, evento -> {
            if (!sesionActiva.getAsBoolean()) {
                detenerConsultaPeriodica();
                return;
            }
            if (!desactivado && vistaActiva.getAsBoolean()) {
                disparador.run();
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        consultaPeriodica = timeline;
        timeline.play();
    }

    public void detenerConsultaPeriodica() {
        if (consultaPeriodica != null) {
            consultaPeriodica.stop();
            consultaPeriodica = null;
        }
    }

    public void pausar() {
        detenerConsultaPeriodica();
        detenerEsperaPendiente();
        versionActual++;
        cancelarTareaEnVuelo();
        cargando.set(false);
    }

    public void desactivar() {
        desactivado = true;
        pausar();
    }

    public void reactivar() {
        desactivado = false;
    }

    public static <T, K> void conservarSeleccionPorId(TableView<T> tabla,
                                                      Function<T, K> extractorId,
                                                      Runnable actualizarContenido) {
        Objects.requireNonNull(tabla, "tabla");
        Objects.requireNonNull(extractorId, "extractorId");
        Objects.requireNonNull(actualizarContenido, "actualizarContenido");
        T previo = tabla.getSelectionModel().getSelectedItem();
        K idPrevio = previo == null ? null : extractorId.apply(previo);
        actualizarContenido.run();
        if (idPrevio == null) {
            return;
        }
        for (T item : tabla.getItems()) {
            if (Objects.equals(extractorId.apply(item), idPrevio)) {
                tabla.getSelectionModel().select(item);
                return;
            }
        }
        tabla.getSelectionModel().clearSelection();
    }

    public static <T, K> void conservarComboPorId(ComboBox<T> combo,
                                                  List<T> elementos,
                                                  boolean incluirNulo,
                                                  Function<T, K> extractorId) {
        Objects.requireNonNull(combo, "combo");
        Objects.requireNonNull(elementos, "elementos");
        Objects.requireNonNull(extractorId, "extractorId");
        T previo = combo.getValue();
        K idPrevio = previo == null ? null : extractorId.apply(previo);
        List<T> nuevos = new ArrayList<>(elementos.size() + (incluirNulo ? 1 : 0));
        if (incluirNulo) {
            nuevos.add(null);
        }
        nuevos.addAll(elementos);
        combo.getItems().setAll(nuevos);
        if (idPrevio == null) {
            combo.setValue(null);
            return;
        }
        for (T item : nuevos) {
            if (item != null && Objects.equals(extractorId.apply(item), idPrevio)) {
                combo.setValue(item);
                return;
            }
        }
        combo.setValue(null);
    }

    private boolean tieneEsperaPendiente() {
        return esperaPendiente != null;
    }

    private void detenerEsperaPendiente() {
        if (esperaPendiente != null) {
            esperaPendiente.stop();
            esperaPendiente = null;
        }
    }

    private void cancelarTareaEnVuelo() {
        if (tareaActiva != null && tareaActiva.isRunning()) {
            tareaActiva.cancel(true);
        }
        tareaActiva = null;
    }

    private boolean esVigente(long version, long generacion) {
        return !desactivado
                && version == versionActual
                && vistaActiva.getAsBoolean()
                && sesionActiva.getAsBoolean()
                && generacionSesion.getAsLong() == generacion;
    }

    private static boolean esInterrupcion(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof ErrorClienteException api && "INTERRUMPIDO".equals(api.getCodigo())) {
                return true;
            }
            if (actual instanceof InterruptedException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
