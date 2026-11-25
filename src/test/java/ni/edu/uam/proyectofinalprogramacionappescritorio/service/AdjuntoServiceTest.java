package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoAdjuntoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PerfilEmisorDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.DocumentoAdjunto;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjuntoServiceTest {
    @TempDir
    Path tempDir;

    private SessionFactory factory;
    private HibernateTransactions tx;
    private AlmacenAdjuntos almacen;
    private AdjuntoService servicio;
    private ConfiguracionService configuracion;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID unidadId;
    private UUID inquilinoId;
    private UUID contratoId;

    @BeforeEach
    void iniciar() {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:adjuntos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(p);
        tx = new HibernateTransactions(factory);
        almacen = new AlmacenAdjuntos(tempDir);
        servicio = new AdjuntoService(tx, almacen);
        configuracion = new ConfiguracionService(tx);

        tx.ejecutar(session -> {
            Usuario uProp = new Usuario("prop-adj-" + UUID.randomUUID().toString().substring(0, 6), "hash", Rol.PROPIETARIO);
            Usuario uAsis = new Usuario("asis-adj-" + UUID.randomUUID().toString().substring(0, 6), "hash", Rol.ASISTENTE);
            session.persist(uProp);
            session.persist(uAsis);
            session.flush();

            propietario = SesionUsuario.paraSesionAutenticada(uProp.getId(), uProp.getNombre(), uProp.getRol(), uProp.getVersion());
            asistente = SesionUsuario.paraSesionAutenticada(uAsis.getId(), uAsis.getNombre(), uAsis.getRol(), uAsis.getVersion());

            Propiedad prop = new Propiedad();
            prop.setNombre("Condominio Lago");
            prop.setDireccion("Managua");
            session.persist(prop);
            session.flush();

            Unidad u = new Unidad();
            u.setPropiedad(prop);
            u.setCodigo("L-01");
            u.setCanonReferencia(new BigDecimal("300.00"));
            u.setMoneda("USD");
            session.persist(u);

            Inquilino inq = new Inquilino();
            inq.setNombre("Ana López");
            session.persist(inq);
            session.flush();

            unidadId = u.getId();
            inquilinoId = inq.getId();

            Contrato c = new Contrato(null, unidadId, null, inquilinoId, null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2027, 10, 31),
                    new BigDecimal("300.00"), "USD", 5, new BigDecimal("300.00"), "Vigente", 0);
            session.persist(c);
            session.flush();
            contratoId = c.getId();
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null && !factory.isClosed()) factory.close();
    }

    @Test
    void cp014AdjuntoPdfYHuellaSha256RechazaEjecutableYExcesoDeCincoMibSinDejarTemporales() throws Exception {
        byte[] pdfValido = ("%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n"
                + "A".repeat(1024)).getBytes(StandardCharsets.US_ASCII);
        String shaEsperado = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdfValido));

        DocumentoAdjuntoDTO guardado = servicio.adjuntarDocumentoInquilino(
                asistente, inquilinoId, "cedula_sintetica.pdf", pdfValido);
        assertEquals(shaEsperado, guardado.sha256());
        assertEquals(DocumentoAdjunto.MIME_PDF, guardado.mime());
        assertArrayEquals(pdfValido, servicio.leerContenido(asistente, guardado.id()));

        // Ejecutable MZ renombrado como .pdf -> ERR-019
        byte[] ejecutable = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00};
        IllegalArgumentException errMz = assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarDocumentoInquilino(asistente, inquilinoId, "falso.pdf", ejecutable));
        assertTrue(errMz.getMessage().contains("ERR-019"));

        // Archivo > 5 MiB -> ERR-019
        byte[] seisMib = new byte[(int) DocumentoAdjunto.MAX_TAMANO_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, seisMib, 0, 5);
        IllegalArgumentException errSize = assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarDocumentoInquilino(asistente, inquilinoId, "grande.pdf", seisMib));
        assertTrue(errSize.getMessage().contains("ERR-019"));

        // Traversal en nombre -> ERR-019
        assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarDocumentoInquilino(asistente, inquilinoId, "../escape.pdf", pdfValido));

        // Sin archivos temporales ni huérfanos extra en tempDir
        try (var flujo = Files.list(tempDir)) {
            List<Path> archivos = flujo.toList();
            assertEquals(1, archivos.size());
            assertFalse(archivos.getFirst().getFileName().toString().endsWith(".tmp"));
        }

        // Si se altera el archivo en disco, leerContenido falla la verificación SHA-256
        try (var flujo = Files.list(tempDir)) {
            Path enDisco = flujo.findFirst().orElseThrow();
            byte[] alterado = pdfValido.clone();
            alterado[alterado.length - 1] = 'Z';
            Files.write(enDisco, alterado);
        }
        assertThrows(IllegalStateException.class, () -> servicio.leerContenido(asistente, guardado.id()));
    }

    @Test
    void fotoUnidadContratoYLogotipoEmisorRespetanTiposYPermisos() throws Exception {
        byte[] png = generarImagen("png");
        byte[] jpg = generarImagen("jpg");
        byte[] pdf = "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

        // Foto de unidad rechaza PDF y permite PNG/JPEG por ambos roles; reemplaza foto activa previa
        assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarFotoUnidad(asistente, unidadId, "no_foto.pdf", pdf));
        DocumentoAdjuntoDTO foto1 = servicio.adjuntarFotoUnidad(asistente, unidadId, "fachada.png", png);
        DocumentoAdjuntoDTO foto2 = servicio.adjuntarFotoUnidad(propietario, unidadId, "sala.jpg", jpg);
        DocumentoAdjuntoDTO vigente = servicio.obtenerFotoUnidad(asistente, unidadId).orElseThrow();
        assertEquals(foto2.id(), vigente.id());
        boolean foto1Activa = tx.ejecutar(s -> s.find(DocumentoAdjunto.class, foto1.id()).isActivo());
        assertFalse(foto1Activa);

        // Contrato solo PDF
        assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarDocumentoContrato(propietario, contratoId, "imagen.png", png));
        DocumentoAdjuntoDTO docContrato = servicio.adjuntarDocumentoContrato(
                asistente, contratoId, "contrato_v1.pdf", pdf);
        assertEquals(1, servicio.listarPorContrato(propietario, contratoId).size());
        servicio.desactivar(propietario, docContrato.id());
        assertTrue(servicio.listarPorContrato(propietario, contratoId).isEmpty());

        // Logotipo del emisor: requiere perfil guardado y solo Propietario puede adjuntar/quitar
        assertThrows(IllegalArgumentException.class, () ->
                servicio.adjuntarLogotipoEmisor(propietario, "logo.png", png));
        configuracion.guardarPerfilEmisor(
                propietario,
                new PerfilEmisorDTO("Inmobiliaria Latch", "J031000000", "Managua", "+505 8888-0000", "info@latch.test"),
                "Alta inicial de emisor");
        assertThrows(SecurityException.class, () ->
                servicio.adjuntarLogotipoEmisor(asistente, "logo.png", png));
        DocumentoAdjuntoDTO logo = servicio.adjuntarLogotipoEmisor(propietario, "logo.png", png);
        assertEquals(logo.id(), servicio.obtenerLogotipoVigente(asistente).orElseThrow().id());
        assertThrows(SecurityException.class, () -> servicio.desactivar(asistente, logo.id()));
        servicio.desactivar(propietario, logo.id());
        assertTrue(servicio.obtenerLogotipoVigente(asistente).isEmpty());
    }

    private static byte[] generarImagen(String formato) throws IOException {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, formato, out));
        return out.toByteArray();
    }
}
