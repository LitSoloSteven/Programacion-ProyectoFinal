package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.coyote.Processor;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.Http11Processor;
import org.apache.tomcat.util.net.AbstractEndpoint.Handler.SocketState;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SocketEvent;
import org.apache.tomcat.util.net.SocketWrapperBase;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Plazo de recepción desde el primer evento de lectura, incluso antes de MVC. */
public final class ProtocoloApi extends Http11NioProtocol {
    static final String RECEPCION = ProtocoloApi.class.getName() + ".recepcion";
    private static final int PLAZO_MS = 30_000;

    private final Entrada entrada;
    private Connector conector;

    public ProtocoloApi() {
        this(new Entrada());
    }

    private ProtocoloApi(Entrada entrada) {
        super(entrada);
        this.entrada = entrada;
    }

    void configurar(Connector conector) {
        this.conector = conector;
        // Un solo intercambio por socket mantiene el plazo ligado al primer evento de lectura.
        setMaxKeepAliveRequests(1);
        setConnectionTimeout(PLAZO_MS);
        setDisableUploadTimeout(false);
        setConnectionUploadTimeout(PLAZO_MS);
    }

    @Override
    protected Processor createProcessor() {
        return new Http11Processor(this, new Adaptador(conector)) {
            @Override
            public SocketState service(SocketWrapperBase<?> socket) throws IOException {
                Recepcion recepcion = entrada.pendientes.get(socket);
                getRequest().setAttribute(RECEPCION, recepcion);
                SocketState estado = SocketState.CLOSED;
                try {
                    estado = super.service(socket);
                    return estado;
                } finally {
                    if (estado != SocketState.LONG) {
                        var pendiente = entrada.pendientes.remove(socket);
                        if (pendiente != null) pendiente.cancelar();
                    }
                }
            }
        };
    }

    private static final class Entrada extends NioEndpoint {
        private final ConcurrentHashMap<SocketWrapperBase<?>, Recepcion> pendientes =
                new ConcurrentHashMap<>();

        @Override
        public boolean processSocket(SocketWrapperBase<NioChannel> socket, SocketEvent evento,
                                     boolean delegar) {
            if (socket != null && evento == SocketEvent.OPEN_READ && !socket.isClosed()) {
                pendientes.computeIfAbsent(socket, conexion -> {
                    var recepcion = new Recepcion(conexion);
                    recepcion.tarea = getUtilityExecutor().schedule(() -> {
                        try {
                            recepcion.expirar();
                        } finally {
                            pendientes.remove(conexion, recepcion);
                        }
                    }, PLAZO_MS, TimeUnit.MILLISECONDS);
                    return recepcion;
                });
            }
            return super.processSocket(socket, evento, delegar);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    static final class Recepcion {
        private final SocketWrapperBase<?> socket;
        private final long inicio = System.nanoTime();
        private boolean completa;
        private ScheduledFuture<?> tarea;

        synchronized void completar() throws IOException {
            if (completa) return;
            comprobarPlazo();
            completa = true;
            cancelar();
        }

        synchronized void antesDeLeer() throws IOException {
            if (!completa) {
                socket.setReadTimeout(1 + TimeUnit.NANOSECONDS.toMillis(comprobarPlazo()));
            }
        }

        private long comprobarPlazo() throws IOException {
            long restante = TimeUnit.MILLISECONDS.toNanos(PLAZO_MS) - (System.nanoTime() - inicio);
            if (socket.isClosed() || restante <= 0) {
                expirar();
                throw new SocketTimeoutException("Venció el plazo de recepción HTTP.");
            }
            return restante;
        }

        private void cancelar() {
            tarea.cancel(false);
        }

        synchronized void expirar() {
            if (!completa) socket.close();
        }
    }

    private static final class Adaptador extends CoyoteAdapter {
        Adaptador(Connector conector) {
            super(conector);
        }

        @Override
        public void service(Request peticion, Response respuesta) throws Exception {
            respuesta.setHeader("Cache-Control", "no-store");
            respuesta.setHeader("X-Content-Type-Options", "nosniff");
            if (respuesta.getStatus() < 400 && peticion.protocol().equals("")) {
                throw new InterruptedIOException("La API requiere cabeceras HTTP.");
            }
            // Tomcat sintetiza Host a partir del destino absoluto de una petición HTTP/1.0.
            if (respuesta.getStatus() < 400
                    && (!peticion.protocol().equals("HTTP/1.1") || peticion.requestURI().equals("*"))) {
                respuesta.setStatus(400);
                respuesta.setHeader("Connection", "close");
            }
            if (respuesta.getStatus() >= 400) {
                escribirError(respuesta);
                return;
            }
            if (peticion.isFinished() && peticion.getAttribute(RECEPCION) instanceof Recepcion recepcion) {
                recepcion.completar();
            }
            super.service(peticion, respuesta);
        }

        @Override
        protected boolean postParseRequest(Request peticion,
                org.apache.catalina.connector.Request servlet,
                Response respuesta, org.apache.catalina.connector.Response salida)
                throws IOException, jakarta.servlet.ServletException {
            boolean continuar = super.postParseRequest(peticion, servlet, respuesta, salida);
            if (respuesta.getStatus() >= 400) {
                escribirError(respuesta);
                return false;
            }
            return continuar;
        }

        private static void escribirError(Response respuesta) throws IOException {
            var error = RespuestasApi.porEstado(respuesta.getStatus());
            byte[] json = RespuestasApi.cuerpo(error).getBytes(StandardCharsets.UTF_8);
            respuesta.setContentType(RespuestasApi.JSON_UTF8);
            respuesta.setContentLength(json.length);
            respuesta.doWrite(ByteBuffer.wrap(json));
        }
    }
}
