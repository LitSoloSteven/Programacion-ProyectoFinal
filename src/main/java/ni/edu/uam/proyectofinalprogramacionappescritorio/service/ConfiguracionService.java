package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ParametroHistorialDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PerfilEmisorDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Configuracion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ConfiguracionService {
    public static final String CLAVE_EMISOR = "emisor";
    private static final Gson GSON = new Gson();

    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public ConfiguracionService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    public Configuracion consultar(SesionUsuario identidad, String clave) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            return session.find(Configuracion.class, clave);
        });
    }

    public void guardar(SesionUsuario identidad, String clave, String valor, long versionEsperada) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            Configuracion config = session.find(Configuracion.class, clave);
            if (config == null) {
                if (versionEsperada != -1) {
                    throw new ConflictoDatosException(
                            "La configuración cambió o ya no existe. Actualiza los datos antes de guardar.");
                }
                session.persist(new Configuracion(clave, valor));
            } else {
                if (config.getVersion() != versionEsperada) {
                    throw new ConflictoDatosException(
                            "La configuración cambió. Actualiza los datos antes de guardar.");
                }
                config.cambiarValor(valor);
            }
            auditoria.registrar(session, actor, UUID.randomUUID(), "GUARDAR_CONFIGURACION", "configuracion:" + clave);
            return null;
        });
    }

    public ParametroHistorialDTO registrarParametro(SesionUsuario identidad, String clave, String valor,
                                                    Instant vigenteDesde, String motivo) {
        String claveNorm = textoRequerido(clave, 80, "La clave del parámetro es inválida.");
        String valorNorm = textoSinMaximo(valor, "El valor del parámetro es obligatorio.");
        String motivoNorm = textoRequerido(motivo, 300, "El motivo del cambio es obligatorio (máximo 300 caracteres).");
        validarParametroNoNegativo(claveNorm, valorNorm);

        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            Instant ahora = reloj.leer(session).instante();
            Instant vigenteEfectivo = vigenteDesde == null ? ahora : vigenteDesde;

            Long duplicados = session.createQuery("""
                            select count(p) from ParametroSistema p
                            where p.clave = :clave and p.vigenteDesde = :vigenteDesde
                            """, Long.class)
                    .setParameter("clave", claveNorm)
                    .setParameter("vigenteDesde", vigenteEfectivo)
                    .getSingleResult();
            if (duplicados > 0) {
                throw new ConflictoDatosException(
                        "Ya existe un registro de ese parámetro para la misma fecha de vigencia.");
            }

            ParametroSistema parametro = new ParametroSistema(
                    claveNorm, valorNorm, vigenteEfectivo, motivoNorm, ahora, actor);
            session.persist(parametro);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "REGISTRAR_PARAMETRO", "parametro:" + claveNorm);
            return aDto(parametro);
        });
    }

    public Optional<ParametroHistorialDTO> consultarParametroVigente(SesionUsuario identidad, String clave) {
        String claveNorm = textoRequerido(clave, 80, "La clave del parámetro es inválida.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            Instant ahora = reloj.leer(session).instante();
            return buscarVigente(session, claveNorm, ahora).map(ConfiguracionService::aDto);
        });
    }

    public List<ParametroHistorialDTO> listarHistorialParametros(SesionUsuario identidad, String claveOpcional) {
        String filtro = claveOpcional == null || claveOpcional.isBlank() ? null : claveOpcional.strip();
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            String hql = filtro == null
                    ? "select p from ParametroSistema p join fetch p.creadoPor order by p.vigenteDesde desc, p.creadoEn desc"
                    : "select p from ParametroSistema p join fetch p.creadoPor where p.clave = :clave order by p.vigenteDesde desc, p.creadoEn desc";
            var consulta = session.createQuery(hql, ParametroSistema.class);
            if (filtro != null) consulta.setParameter("clave", filtro);
            return consulta.getResultList().stream().map(ConfiguracionService::aDto).toList();
        });
    }

    public ParametroHistorialDTO guardarPerfilEmisor(SesionUsuario identidad, PerfilEmisorDTO perfil, String motivo) {
        if (perfil == null) throw new IllegalArgumentException("Faltan los datos del perfil emisor.");
        String nombre = textoRequerido(perfil.nombre(), 120, "El nombre del emisor es obligatorio (máximo 120 caracteres).");
        String identificacion = textoOpcional(perfil.identificacion(), 40, "La identificación admite hasta 40 caracteres.");
        String direccion = textoOpcional(perfil.direccion(), 300, "La dirección admite hasta 300 caracteres.");
        String telefono = textoOpcional(perfil.telefono(), 30, "El teléfono admite hasta 30 caracteres.");
        if (telefono != null && !ValidadorEntrada.esTelefonoValido(telefono)) {
            throw new IllegalArgumentException("El teléfono del emisor no es válido.");
        }
        String correo = textoOpcional(perfil.correo(), 254, "El correo admite hasta 254 caracteres.");
        if (correo != null && !ValidadorEntrada.esCorreoValido(correo)) {
            throw new IllegalArgumentException("El correo del emisor no es válido.");
        }
        PerfilEmisorDTO limpio = new PerfilEmisorDTO(nombre, identificacion, direccion, telefono, correo);
        return registrarParametro(identidad, CLAVE_EMISOR, GSON.toJson(limpio), null, motivo);
    }

    public Optional<PerfilEmisorDTO> consultarPerfilEmisorVigente(SesionUsuario identidad) {
        return consultarParametroVigente(identidad, CLAVE_EMISOR).map(dto -> parsearEmisor(dto.valor()));
    }

    public int leerMinutosInactividad(SesionUsuario identidad) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CONSULTAR_REPORTES);
            Instant ahora = reloj.leer(session).instante();
            return buscarVigente(session, "minutos_inactividad", ahora)
                    .map(ParametroSistema::getValor)
                    .map(ConfiguracionService::parsearMinutosInactividad)
                    .orElse(15);
        });
    }

    public static int parsearMinutosInactividad(String valor) {
        if (valor == null || valor.isBlank()) return 15;
        try {
            int minutos = Integer.parseInt(valor.strip());
            return (minutos >= 5 && minutos <= 60) ? minutos : 15;
        } catch (NumberFormatException error) {
            return 15;
        }
    }

    public static String buscarResumenEmisorVigente(Session session, Instant ahora) {
        return buscarVigente(session, CLAVE_EMISOR, ahora)
                .map(ParametroSistema::getValor)
                .map(ConfiguracionService::parsearEmisor)
                .map(PerfilEmisorDTO::resumenVisible)
                .filter(texto -> !texto.isBlank())
                .orElse(null);
    }

    public static Optional<ParametroSistema> buscarParametroVigente(Session session, String clave, Instant ahora) {
        return buscarVigente(session, clave, ahora);
    }

    private static Optional<ParametroSistema> buscarVigente(Session session, String clave, Instant ahora) {
        return session.createQuery("""
                        select p from ParametroSistema p
                        join fetch p.creadoPor
                        where p.clave = :clave and p.vigenteDesde <= :ahora
                        order by p.vigenteDesde desc, p.creadoEn desc
                        """, ParametroSistema.class)
                .setParameter("clave", clave)
                .setParameter("ahora", ahora)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    private static PerfilEmisorDTO parsearEmisor(String json) {
        try {
            PerfilEmisorDTO perfil = GSON.fromJson(json, PerfilEmisorDTO.class);
            if (perfil == null) throw new IllegalStateException("Perfil emisor vacío.");
            return perfil;
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("El perfil emisor configurado es inválido.", e);
        }
    }

    private static void validarParametroNoNegativo(String clave, String valor) {
        String normalizada = clave.toLowerCase(Locale.ROOT);
        boolean requiereNoNegativo = normalizada.startsWith("dias.")
                || normalizada.startsWith("mora.")
                || normalizada.startsWith("monto.")
                || normalizada.startsWith("gracia.")
                || normalizada.equals("mora_valor")
                || normalizada.contains("dias")
                || normalizada.contains("monto");
        if (!requiereNoNegativo) return;
        try {
            BigDecimal numero = new BigDecimal(valor);
            if (numero.signum() < 0) {
                throw new IllegalArgumentException(
                        "ERR-064: Los días o montos configurables no pueden ser negativos.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "ERR-064: El parámetro numérico debe contener un valor válido no negativo.");
        }
    }

    private static ParametroHistorialDTO aDto(ParametroSistema p) {
        return new ParametroHistorialDTO(
                p.getId(), p.getClave(), p.getValor(), p.getVigenteDesde(),
                p.getMotivo(), p.getCreadoEn(), p.getCreadoPor().getNombre());
    }

    private static String textoRequerido(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }

    private static String textoSinMaximo(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }

    private static String textoOpcional(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.strip();
        if (limpio.length() > maximo) throw new IllegalArgumentException(mensaje);
        return limpio;
    }
}
