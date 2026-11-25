package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.GastoPropiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private ReporteService reporteService;
    private long propietarioId;
    private long asistenteId;
    private SesionUsuario propietario;
    private SesionUsuario asistente;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:reportes_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");

        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        reporteService = new ReporteService(tx);

        tx.ejecutar(session -> {
            Usuario usuarioPropietario = new Usuario("rep-owner-" + token(), "hash", Rol.PROPIETARIO);
            Usuario usuarioAsistente = new Usuario("rep-asist-" + token(), "hash", Rol.ASISTENTE);
            session.persist(usuarioPropietario);
            session.persist(usuarioAsistente);
            session.flush();
            propietarioId = usuarioPropietario.getId();
            asistenteId = usuarioAsistente.getId();

            session.persist(new Propietario(usuarioPropietario,
                    "Propietario Reportes",
                    "rep-owner-" + token() + "@latch.test",
                    "+505 8888-0000"));
            return null;
        });

        propietario = SesionUsuario.paraSesionAutenticada(propietarioId, "Propietario Reportes", Rol.PROPIETARIO);
        asistente = SesionUsuario.paraSesionAutenticada(asistenteId, "Asistente Reportes", Rol.ASISTENTE);
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void pagosReporteUsaEncabezadosEstaticosYExcluyeAnuladosDelNetoPorMoneda() throws IOException {
        Pago pagoUsdVigente = crearPago("Contrato A-101", LocalDate.of(2026, 11, 5),
                "USD", new BigDecimal("500.00"), "Transferencia", "REF-USD-01", "Registrado");
        Pago pagoUsdAnulado = crearPago("Contrato A-101", LocalDate.of(2026, 11, 6),
                "USD", new BigDecimal("999.00"), "Efectivo", "REF-ANULADO-01", "Anulado");
        Pago pagoNioVigente = crearPago("Contrato B-202", LocalDate.of(2026, 11, 7),
                "NIO", new BigDecimal("12500.50"), "Efectivo", "REF-NIO-01", "Registrado");

        byte[] pdf = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarReportePagosPdf(List.of(pagoUsdVigente, pagoUsdAnulado, pagoNioVigente)));

        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Monto original"), texto);
        assertTrue(texto.contains("Monto contrato"), texto);
        assertTrue(texto.contains("Anulados: 1"), texto);
        assertTrue(texto.contains("Neto USD: 500.00 USD"), texto);
        assertTrue(texto.contains("Neto NIO: 12,500.50 NIO"), texto);
        assertTrue(texto.contains("REF-ANULADO-01"), texto);
        assertTrue(texto.contains("Anulado"), texto);
        assertFalse(texto.contains("1,499.00"), texto);
        assertFalse(texto.contains("13,000.50"), texto);
    }

    @Test
    void cargosCarteraSeparaSaldoYMoraEnUsdYNio() throws IOException {
        LocalDate hoy = LocalDate.now();
        List<CargoListaResponse> cargos = List.of(
                new CargoListaResponse("1", "c1", "Contrato C-01", "Renta", "Mes vencido USD",
                        hoy.minusDays(20).toString(), hoy.minusDays(20).toString(), hoy.minusDays(5).toString(),
                        "300.00", "300.00 USD", "Pendiente"),
                new CargoListaResponse("2", "c2", "Contrato C-02", "Renta", "Mes vigente USD",
                        hoy.toString(), hoy.toString(), hoy.plusDays(10).toString(),
                        "150.00", "150.00 USD", "Pendiente"),
                new CargoListaResponse("3", "c3", "Contrato C-03", "Renta", "Mes vencido NIO",
                        hoy.minusDays(15).toString(), hoy.minusDays(15).toString(), hoy.minusDays(3).toString(),
                        "10000.00", "10,000.00 NIO", "Pendiente"),
                new CargoListaResponse("4", "c4", "Contrato C-04", "Renta", "Mes pagado NIO",
                        hoy.minusDays(30).toString(), hoy.minusDays(30).toString(), hoy.minusDays(15).toString(),
                        "5000.00", "5,000.00 NIO", "Pagado")
        );

        byte[] pdf = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarReporteCargosCarteraDesdeListaPdf(cargos));

        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Saldo USD: 450.00"), texto);
        assertTrue(texto.contains("Saldo NIO: 10,000.00"), texto);
        assertTrue(texto.contains("Mora USD: 300.00"), texto);
        assertTrue(texto.contains("Mora NIO: 10,000.00"), texto);
        assertFalse(texto.contains("10,450.00"), texto);
        assertFalse(texto.contains("10,300.00"), texto);
    }

    @Test
    void unidadesSeparaCanonReferenciaEnUsdYNio() throws IOException {
        Unidad u1 = new Unidad();
        u1.setPropiedadNombre("Residencial Norte");
        u1.setCodigo("U-101");
        u1.setTipo("Apartamento");
        u1.setEstado("Disponible");
        u1.setCanonReferencia(new BigDecimal("450.00"));
        u1.setMoneda("USD");

        Unidad u2 = new Unidad();
        u2.setPropiedadNombre("Residencial Norte");
        u2.setCodigo("U-102");
        u2.setTipo("Apartamento");
        u2.setEstado("Ocupada");
        u2.setCanonReferencia(new BigDecimal("550.25"));
        u2.setMoneda("USD");

        Unidad u3 = new Unidad();
        u3.setPropiedadNombre("Edificio Central");
        u3.setCodigo("U-201");
        u3.setTipo("Local");
        u3.setEstado("Disponible");
        u3.setCanonReferencia(new BigDecimal("18500.75"));
        u3.setMoneda("NIO");

        byte[] pdf = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarReporteUnidadesPdf(List.of(u1, u2, u3)));

        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Canon USD: 1,000.25"), texto);
        assertTrue(texto.contains("Canon NIO: 18,500.75"), texto);
        assertFalse(texto.contains("19,501.00"), texto);
    }

    @Test
    void contratosPropiedadesEInquilinosCompilanYGeneranPdfValido() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();

        byte[] pdfPropiedades = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarReportePropiedadesPdf());
        assertPdfValido(pdfPropiedades);
        String textoPropiedades = extraerTextoPdf(pdfPropiedades);
        assertTrue(textoPropiedades.contains("Torre Latch"), textoPropiedades);
        assertTrue(textoPropiedades.contains("A-01"), textoPropiedades);
        assertTrue(textoPropiedades.contains("A-02"), textoPropiedades);

        byte[] pdfContratos = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarReporteContratosOcupacionPdf(List.of(datos.contrato())));
        assertPdfValido(pdfContratos);
        String textoContratos = extraerTextoPdf(pdfContratos);
        assertTrue(textoContratos.contains("Sofía Ramírez"), textoContratos);
        assertTrue(textoContratos.contains("1 / 2"), textoContratos);

        Inquilino inquilinoInactivo = new Inquilino();
        inquilinoInactivo.setNombre("Carlos Méndez");
        inquilinoInactivo.setIdentificacion("001-010190-0002B");
        inquilinoInactivo.setActivo(false);

        byte[] pdfInquilinos = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarReporteInquilinosPdf(List.of(datos.inquilino(), inquilinoInactivo)));
        assertPdfValido(pdfInquilinos);
        String textoInquilinos = extraerTextoPdf(pdfInquilinos);
        assertTrue(textoInquilinos.contains("Sofía Ramírez"), textoInquilinos);
        assertTrue(textoInquilinos.contains("Carlos Méndez"), textoInquilinos);
        assertTrue(textoInquilinos.contains("Inquilinos activos: 1"), textoInquilinos);
        assertTrue(textoInquilinos.contains("Inquilinos inactivos: 1"), textoInquilinos);
    }

    @Test
    void exigePermisoConsultarReportesDentroDeTransaccion() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        Pago pago = crearPago("Contrato A-01", LocalDate.of(2026, 11, 5),
                "USD", new BigDecimal("250.00"), "Efectivo", "REF-01", "Registrado");
        CargoListaResponse cargo = new CargoListaResponse("1", "c1", "Contrato A-01", "Renta", "Canon",
                "2026-11-01", "2026-11-01", "2026-11-10", "250.00", "250.00 USD", "Pendiente");

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () -> reporteService.generarReportePagosPdf(List.of(pago)));
        assertThrows(SecurityException.class,
                () -> reporteService.generarReporteContratosOcupacionPdf(List.of(datos.contrato())));
        assertThrows(SecurityException.class, () -> reporteService.generarReportePropiedadesPdf());
        assertThrows(SecurityException.class,
                () -> reporteService.generarReporteCargosCarteraDesdeListaPdf(List.of(cargo)));
        assertThrows(SecurityException.class,
                () -> reporteService.generarReporteInquilinosPdf(List.of(datos.inquilino())));
        assertThrows(SecurityException.class,
                () -> reporteService.generarReporteUnidadesPdf(List.of(datos.unidadOcupada())));

        byte[] pdfAsistente = SesionUsuario.ejecutarComo(asistente, () ->
                reporteService.generarReporteUnidadesPdf(List.of(datos.unidadOcupada())));
        assertPdfValido(pdfAsistente);

        tx.ejecutar(session -> {
            session.find(Usuario.class, asistenteId).desactivar();
            return null;
        });

        assertThrows(SecurityException.class, () -> SesionUsuario.ejecutarComo(asistente, () ->
                reporteService.generarReporteUnidadesPdf(List.of(datos.unidadOcupada()))));
    }

    @Test
    void generarDocumentoYExportarPdfSinReconsultarBase() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();

        DocumentoReporte documento = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoPropiedades());

        assertEquals("LATCH — Inventario de Propiedades y Unidades", documento.titulo());
        assertEquals("Reporte-Propiedades-Unidades.pdf", documento.nombreArchivoSugerido());
        assertFalse(documento.jasperPrint().getPages().isEmpty());

        SesionUsuario.cerrarSesion();
        byte[] pdf = reporteService.exportarPdf(documento.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Torre Latch"), texto);

        DocumentoReporte docUnidades = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarDocumentoUnidades(List.of(datos.unidadOcupada())));
        assertEquals("Reporte-Unidades.pdf", docUnidades.nombreArchivoSugerido());
        assertFalse(docUnidades.jasperPrint().getPages().isEmpty());
    }

    @Test
    void reciboVigenteContieneNumeroMontosYSinMarcaAnulado() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        UUID pagoId = sembrarPagoConRecibo(datos.contrato().getId(), 101L,
                "USD", new BigDecimal("320.00"), "Transferencia", "REF-REC-101", "Registrado");

        DocumentoReporte documento = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoRecibo(pagoId));

        assertEquals("Recibo-101.pdf", documento.nombreArchivoSugerido());
        byte[] pdf = reporteService.exportarPdf(documento.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("101"), texto);
        assertTrue(texto.contains("320.00 USD"), texto);
        assertTrue(texto.contains("VIGENTE"), texto);
        assertTrue(texto.contains("REF-REC-101"), texto);
        assertFalse(texto.contains("ANULADO"), texto);
    }

    @Test
    void reciboAnuladoIncluyeMarcaVisibleAnuladoYNoAlteraConsecutivo() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        UUID pagoId = sembrarPagoConRecibo(datos.contrato().getId(), 102L,
                "USD", new BigDecimal("410.00"), "Efectivo", "REF-REC-102", "Anulado");

        byte[] pdf = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarPdfPago(pagoId));

        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("102"), texto);
        assertTrue(texto.contains("ANULADO"), texto);
        assertTrue(texto.contains("RECIBO ANULADO"), texto);
        long cantidadRecibos = tx.ejecutar(session ->
                session.createQuery("select count(r) from Recibo r", Long.class).getSingleResult());
        assertEquals(1L, cantidadRecibos);
    }

    @Test
    void reciboInexistenteOPagoSinReciboLanzaIllegalArgumentException() {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        UUID pagoSinReciboId = tx.ejecutar(session -> {
            Pago pago = new Pago();
            pago.setContratoId(datos.contrato().getId());
            pago.setFechaPago(LocalDate.of(2026, 11, 10));
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("150.00"));
            pago.setMetodo("Efectivo");
            pago.setReferencia("REF-SIN-RECIBO");
            pago.setEstado("Registrado");
            pago.aplicarConversion("USD", new BigDecimal("150.00"), null);
            session.persist(pago);
            session.flush();
            return pago.getId();
        });

        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoRecibo(UUID.randomUUID())));
        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoRecibo(pagoSinReciboId)));
    }

    @Test
    void reciboExigePermisoConsultarReportes() {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        UUID pagoId = sembrarPagoConRecibo(datos.contrato().getId(), 103L,
                "USD", new BigDecimal("200.00"), "Efectivo", "REF-REC-103", "Registrado");

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoRecibo(pagoId));

        tx.ejecutar(session -> {
            session.find(Usuario.class, asistenteId).desactivar();
            return null;
        });

        assertThrows(SecurityException.class, () -> SesionUsuario.ejecutarComo(asistente,
                () -> reporteService.generarDocumentoRecibo(pagoId)));
    }

    @Test
    void rep01ReciboSoloAdmitePdfYMuestraAvisoNoFiscalYTasaAplicada() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        UUID pagoId = tx.ejecutar(session -> {
            Usuario owner = session.find(Usuario.class, propietarioId);
            TipoCambio tc = TipoCambio.autorizada(LocalDate.of(2026, 11, 8),
                    new BigDecimal("36.62430000"), "BCN", Instant.now(), owner, "Tasa oficial");
            session.persist(tc);

            Cargo cargo = new Cargo();
            cargo.setContratoId(datos.contrato().getId());
            cargo.setTipo("Renta");
            cargo.setPeriodo(LocalDate.of(2026, 11, 1));
            cargo.setFechaEmision(LocalDate.of(2026, 11, 1));
            cargo.setVenceEl(LocalDate.of(2026, 11, 5));
            cargo.setImporte(new BigDecimal("100.00"));
            cargo.setConcepto("Renta noviembre");
            cargo.setEstado("Pendiente");
            session.persist(cargo);

            Pago pago = new Pago();
            pago.setContratoId(datos.contrato().getId());
            pago.setFechaPago(LocalDate.of(2026, 11, 8));
            pago.setMonedaOriginal("NIO");
            pago.setMontoOriginal(new BigDecimal("7324.86"));
            pago.setMetodo("Transferencia");
            pago.setReferencia("REF-FX-104");
            pago.setEstado("Registrado");
            pago.aplicarConversion("USD", new BigDecimal("200.00"), tc);
            session.persist(pago);
            session.flush();

            session.persist(AplicacionPago.inicial(
                    pago.getId(), cargo.getId(), new BigDecimal("100.00"),
                    LocalDate.of(2026, 11, 8), Instant.now(), propietarioId));
            session.persist(new Recibo(104L, pago, "Sofía Ramírez — Torre Latch A-01"));
            return pago.getId();
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoRecibo(pagoId));
        assertFalse(doc.admiteXlsx());
        assertThrows(IllegalArgumentException.class, () -> reporteService.exportarXlsx(doc));

        String texto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(texto.contains("36.62430000"), texto);
        assertTrue(texto.contains("Renta noviembre"), texto);
        assertTrue(texto.contains("100.00 USD"), texto);
        assertTrue(texto.contains("No es factura fiscal"), texto);
    }

    @Test
    void rep02EstadoCuentaDesdeBasePorCorteGeneraPdfYXlsxNumerico() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        tx.ejecutar(session -> {
            Cargo c = new Cargo();
            c.setContratoId(datos.contrato().getId());
            c.setTipo("Renta");
            c.setPeriodo(LocalDate.of(2026, 11, 1));
            c.setFechaEmision(LocalDate.of(2026, 11, 1));
            c.setVenceEl(LocalDate.of(2026, 11, 5));
            c.setImporte(new BigDecimal("500.00"));
            c.setConcepto("Renta nov");
            c.setEstado("Pendiente");
            session.persist(c);

            Pago p = new Pago();
            p.setContratoId(datos.contrato().getId());
            p.setFechaPago(LocalDate.of(2026, 11, 4));
            p.setMonedaOriginal("USD");
            p.setMontoOriginal(new BigDecimal("200.00"));
            p.setMetodo("Efectivo");
            p.setReferencia("EC-01");
            p.setEstado("Registrado");
            p.aplicarConversion("USD", new BigDecimal("200.00"), null);
            session.persist(p);
            session.flush();
            session.persist(AplicacionPago.inicial(
                    p.getId(), c.getId(), new BigDecimal("200.00"),
                    LocalDate.of(2026, 11, 4), Instant.now(), propietarioId));
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoEstadoCuenta(datos.contrato().getId(), LocalDate.of(2026, 11, 10)));
        assertTrue(doc.admiteXlsx());
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("500.00 USD"), pdfTexto);
        assertTrue(pdfTexto.contains("300.00 USD"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>500.00</v>"), hoja);
        assertTrue(hoja.contains("<v>200.00</v>"), hoja);
        assertTrue(hoja.contains("<v>300.00</v>"), hoja);
    }

    @Test
    void rep03CarteraVencidaClasificaTramosYExcluyeAnuladosEnPdfYXlsx() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        LocalDate corte = LocalDate.of(2026, 11, 25);
        tx.ejecutar(session -> {
            Contrato cNio = crearContratoSecundario(session, datos.unidadDisponible(), datos.inquilino(),
                    "NIO", new BigDecimal("10000.00"), new BigDecimal("0.00"), "Vigente");

            Cargo c1a30 = crearCargo(datos.contrato().getId(), "Renta", "Renta 15d",
                    LocalDate.of(2026, 11, 1), corte.minusDays(15), new BigDecimal("300.00"));
            Cargo cMora = crearCargo(datos.contrato().getId(), "Mora", "Mora 45d",
                    LocalDate.of(2026, 10, 1), corte.minusDays(45), new BigDecimal("30.00"));
            Cargo c61a90 = crearCargo(cNio.getId(), "Renta", "Renta 70d",
                    LocalDate.of(2026, 9, 1), corte.minusDays(70), new BigDecimal("4000.00"));
            Cargo cAnulado = crearCargo(cNio.getId(), "Renta", "Renta anulada",
                    LocalDate.of(2026, 8, 1), corte.minusDays(100), new BigDecimal("9999.00"));
            cAnulado.anular("Duplicado", propietarioId, Instant.now());

            session.persist(c1a30);
            session.persist(cMora);
            session.persist(c61a90);
            session.persist(cAnulado);
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoCarteraVencida(corte, null, null, null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("1-30d 300.00"), pdfTexto);
        assertTrue(pdfTexto.contains("31-60d 30.00"), pdfTexto);
        assertTrue(pdfTexto.contains("61-90d 4,000.00"), pdfTexto);
        assertFalse(pdfTexto.contains("9,999.00"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>300.00</v>"), hoja);
        assertTrue(hoja.contains("<v>30.00</v>"), hoja);
        assertTrue(hoja.contains("<v>4000.00</v>"), hoja);
        assertFalse(hoja.contains("9999.00"), hoja);
    }

    @Test
    void rep04IngresosYGastosSeparaRentasOtrosGastosYGarantiasYExcluyeAnulados() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);

        tx.ejecutar(session -> {
            Contrato c = session.find(Contrato.class, datos.contrato().getId());
            c.setDepositoPactado(new BigDecimal("500.00"));
            c.inicializarGarantiaSeguimiento();
            session.flush();

            Cargo renta = crearCargo(c.getId(), "Renta", "Renta nov",
                    desde, LocalDate.of(2026, 11, 5), new BigDecimal("400.00"));
            session.persist(renta);

            Pago pagoVigente = new Pago();
            pagoVigente.setContratoId(c.getId());
            pagoVigente.setFechaPago(LocalDate.of(2026, 11, 10));
            pagoVigente.setMonedaOriginal("USD");
            pagoVigente.setMontoOriginal(new BigDecimal("450.00"));
            pagoVigente.setMetodo("Transferencia");
            pagoVigente.setReferencia("ING-01");
            pagoVigente.setEstado("Registrado");
            pagoVigente.aplicarConversion("USD", new BigDecimal("450.00"), null);
            session.persist(pagoVigente);

            Pago pagoAnulado = new Pago();
            pagoAnulado.setContratoId(c.getId());
            pagoAnulado.setFechaPago(LocalDate.of(2026, 11, 11));
            pagoAnulado.setMonedaOriginal("USD");
            pagoAnulado.setMontoOriginal(new BigDecimal("777.00"));
            pagoAnulado.setMetodo("Efectivo");
            pagoAnulado.setReferencia("ING-ANUL");
            pagoAnulado.setEstado("Anulado");
            pagoAnulado.aplicarConversion("USD", new BigDecimal("777.00"), null);
            session.persist(pagoAnulado);
            session.flush();

            session.persist(AplicacionPago.inicial(
                    pagoVigente.getId(), renta.getId(), new BigDecimal("400.00"),
                    LocalDate.of(2026, 11, 10), Instant.now(), propietarioId));

            Usuario owner = session.find(Usuario.class, propietarioId);
            session.persist(new GastoPropiedad(datos.propiedad().getId(), null,
                    LocalDate.of(2026, 11, 12), new BigDecimal("75.00"), "USD", "Pintura pasillo",
                    Instant.now(), owner));

            session.persist(MovimientoGarantia.deduccion(
                    c.getGarantiaSeguimiento().getId(), UUID.randomUUID(),
                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    (short) 1, new BigDecimal("100.00"), LocalDate.of(2026, 11, 15),
                    "Reparación puerta", Instant.now(), propietarioId));
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoIngresosYGastos(desde, hasta, null, null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("Rentas 400.00 | Otros 50.00 | Gastos 75.00 | Neto 375.00 | Garantías (sep.) 100.00"),
                pdfTexto);
        assertTrue(pdfTexto.contains("Pago (Anulado)"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>400.00</v>"), hoja);
        assertTrue(hoja.contains("<v>50.00</v>"), hoja);
        assertTrue(hoja.contains("<v>75.00</v>"), hoja);
        assertTrue(hoja.contains("<v>375.00</v>"), hoja);
        assertTrue(hoja.contains("<v>100.00</v>"), hoja);
    }

    @Test
    void rep05OcupacionIncluyeEnMantenimientoYExcluyeInactivaDelDenominador() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        tx.ejecutar(session -> {
            Unidad mant = new Unidad();
            mant.setPropiedad(datos.propiedad());
            mant.setCodigo("A-03");
            mant.setTipo("Apartamento");
            mant.setEstado("En mantenimiento");
            mant.setCanonReferencia(new BigDecimal("400.00"));
            mant.setMoneda("USD");
            session.persist(mant);

            Unidad inactiva = new Unidad();
            inactiva.setPropiedad(datos.propiedad());
            inactiva.setCodigo("A-04");
            inactiva.setTipo("Apartamento");
            inactiva.setEstado("Inactiva");
            inactiva.setCanonReferencia(new BigDecimal("400.00"));
            inactiva.setMoneda("USD");
            session.persist(inactiva);
            return null;
        });

        // Ocupadas = 1 (A-01), Activas = 3 (A-01, A-02, A-03; A-04 excluida) -> 33.3%
        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoOcupacion(datos.propiedad().getId(), null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("Ocupación: 1 / 3 (33.3%)"), pdfTexto);
        assertTrue(pdfTexto.contains("En mantenimiento: 1"), pdfTexto);
        assertTrue(pdfTexto.contains("Inactivas (fuera): 1"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("1 / 3"), hoja);
    }

    @Test
    void rep06ContratosPorVencerFiltraUmbralYExcluyeTerminados() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        LocalDate base = LocalDate.of(2026, 11, 25);
        tx.ejecutar(session -> {
            Contrato vigente = session.find(Contrato.class, datos.contrato().getId());
            vigente.setFin(base.plusDays(12));

            Contrato terminado = crearContratoSecundario(session, datos.unidadDisponible(), datos.inquilino(),
                    "USD", new BigDecimal("600.00"), BigDecimal.ZERO, "Vigente");
            terminado.setFin(base.plusDays(5));
            terminado.terminar(base.minusDays(1), "Entrega anticipada");
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoContratosPorVencer(base, 30, null, null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("Contratos por vencer: 1"), pdfTexto);
        assertTrue(pdfTexto.contains("500.00"), pdfTexto);
        assertFalse(pdfTexto.contains("600.00"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>12</v>"), hoja);
        assertTrue(hoja.contains("<v>500.00</v>"), hoja);
    }

    @Test
    void rep07HistorialPagosFiltraInquilinoYExcluyeAnuladosDelNeto() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        tx.ejecutar(session -> {
            Pago p1 = new Pago();
            p1.setContratoId(datos.contrato().getId());
            p1.setFechaPago(LocalDate.of(2026, 11, 5));
            p1.setMonedaOriginal("USD");
            p1.setMontoOriginal(new BigDecimal("300.00"));
            p1.setMetodo("Transferencia");
            p1.setReferencia("HP-01");
            p1.setEstado("Registrado");
            p1.aplicarConversion("USD", new BigDecimal("300.00"), null);
            session.persist(p1);

            Pago p2 = new Pago();
            p2.setContratoId(datos.contrato().getId());
            p2.setFechaPago(LocalDate.of(2026, 11, 6));
            p2.setMonedaOriginal("USD");
            p2.setMontoOriginal(new BigDecimal("250.00"));
            p2.setMetodo("Efectivo");
            p2.setReferencia("HP-02");
            p2.anular("Error de monto", propietarioId, Instant.now());
            p2.aplicarConversion("USD", new BigDecimal("250.00"), null);
            session.persist(p2);
            session.flush();

            session.persist(new Recibo(201L, p1, "Sofía Ramírez"));
            session.persist(new Recibo(202L, p2, "Sofía Ramírez"));
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoHistorialPagos(
                        datos.inquilino().getId(), null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("Neto USD (excluye anulados): 300.00 USD"), pdfTexto);
        assertTrue(pdfTexto.contains("Error de monto"), pdfTexto);
        assertTrue(pdfTexto.contains("#202"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>300.00</v>"), hoja);
        assertTrue(hoja.contains("Error de monto"), hoja);
    }

    @Test
    void rep08LiquidacionGarantiasConciliaInicialMovimientosYRemanente() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        LocalDate corte = LocalDate.of(2026, 11, 25);
        tx.ejecutar(session -> {
            Contrato c = session.find(Contrato.class, datos.contrato().getId());
            c.setDepositoPactado(new BigDecimal("500.00"));
            c.inicializarGarantiaSeguimiento();
            session.flush();

            Cargo cargo = crearCargo(c.getId(), "Renta", "Saldo renta",
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), new BigDecimal("150.00"));
            session.persist(cargo);
            session.flush();

            UUID liqId = UUID.randomUUID();
            String hash = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
            UUID gId = c.getGarantiaSeguimiento().getId();
            session.persist(MovimientoGarantia.aplicacion(
                    gId, cargo.getId(), liqId, hash, (short) 1,
                    new BigDecimal("150.00"), LocalDate.of(2026, 11, 20), "Aplicación a renta", Instant.now(), propietarioId));
            session.persist(MovimientoGarantia.deduccion(
                    gId, liqId, hash, (short) 2,
                    new BigDecimal("100.00"), LocalDate.of(2026, 11, 21), "Daño pintura", Instant.now(), propietarioId));
            session.persist(MovimientoGarantia.devolucion(
                    gId, liqId, hash, (short) 3,
                    new BigDecimal("250.00"), LocalDate.of(2026, 11, 22), "Transferencia", "Devolución final",
                    Instant.now(), propietarioId));
            c.getGarantiaSeguimiento().ajustarSaldo(BigDecimal.ZERO.setScale(2));
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoLiquidacionGarantias(datos.contrato().getId(), corte, null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("USD: Inicial 500.00 | Aplicado 150.00 | Deducido 100.00 | Devuelto 250.00 | Remanente 0.00"),
                pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>150.00</v>"), hoja);
        assertTrue(hoja.contains("<v>100.00</v>"), hoja);
        assertTrue(hoja.contains("<v>250.00</v>"), hoja);
        assertTrue(hoja.contains("Daño pintura"), hoja);
    }

    @Test
    void rep09MantenimientoConciliaCostosConMantenimientoService() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        MantenimientoService mantService = new MantenimientoService(tx);

        SesionUsuario.ejecutarComo(propietario, () -> {
            var sol = mantService.abrir(propietario, datos.unidadOcupada().getId(),
                    "Fuga en baño", "Alta", "Juan Técnico", LocalDate.of(2026, 11, 10));
            mantService.registrarGasto(propietario, datos.propiedad().getId(), sol.id(),
                    LocalDate.of(2026, 11, 11), new BigDecimal("85.50"), "USD", "Repuestos PVC");
            mantService.registrarGasto(propietario, datos.propiedad().getId(), null,
                    LocalDate.of(2026, 11, 12), new BigDecimal("1500.00"), "NIO", "Limpieza general");
            return null;
        });

        var resumenService = SesionUsuario.ejecutarComo(propietario, () ->
                mantService.consultarGastos(propietario, datos.propiedad().getId(),
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null));

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarDocumentoMantenimiento(
                        datos.propiedad().getId(), null,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                        null, null, null));

        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("Costo total USD: 85.50 USD"), pdfTexto);
        assertTrue(pdfTexto.contains("Costo total NIO: 1,500.00 NIO"), pdfTexto);
        assertEquals(new BigDecimal("85.50"), resumenService.totalUsd());
        assertEquals(new BigDecimal("1500.00"), resumenService.totalNio());

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>85.50</v>"), hoja);
        assertTrue(hoja.contains("<v>1500.00</v>"), hoja);
    }

    @Test
    void rep10TasasUtilizadasMuestraSoloTasasAplicadasEnPagosYNoRedistribuyeCatalogo() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        tx.ejecutar(session -> {
            Usuario owner = session.find(Usuario.class, propietarioId);
            TipoCambio usada = TipoCambio.autorizada(LocalDate.of(2026, 11, 20),
                    new BigDecimal("36.62430000"), "BCN-Oficial", Instant.now(), owner, "Pago multimoneda");
            TipoCambio noUsada = TipoCambio.informativa(LocalDate.of(2026, 11, 21),
                    new BigDecimal("37.12345678"), "CatalogoNoUsado", Instant.now());
            session.persist(usada);
            session.persist(noUsada);

            Pago pago = new Pago();
            pago.setContratoId(datos.contrato().getId());
            pago.setFechaPago(LocalDate.of(2026, 11, 20));
            pago.setMonedaOriginal("NIO");
            pago.setMontoOriginal(new BigDecimal("3662.43"));
            pago.setMetodo("Transferencia");
            pago.setReferencia("TC-USADA");
            pago.setEstado("Registrado");
            pago.aplicarConversion("USD", new BigDecimal("100.00"), usada);
            session.persist(pago);
            session.flush();
            session.persist(new Recibo(301L, pago, "Sofía Ramírez"));
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoTasasUtilizadas(
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null));
        String pdfTexto = extraerTextoPdf(reporteService.exportarPdf(doc.jasperPrint()));
        assertTrue(pdfTexto.contains("36.62430000"), pdfTexto);
        assertTrue(pdfTexto.contains("BCN-Oficial"), pdfTexto);
        assertFalse(pdfTexto.contains("37.12345678"), pdfTexto);
        assertFalse(pdfTexto.contains("CatalogoNoUsado"), pdfTexto);

        String hoja = leerHojaXlsx(reporteService.exportarXlsx(doc));
        assertTrue(hoja.contains("<v>36.62430000</v>"), hoja);
        assertFalse(hoja.contains("37.12345678"), hoja);
    }

    @Test
    void rep02ARep10ExigenPermisoConsultarReportesYRespetanCancelacionCooperativa() {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        SesionUsuario.cerrarSesion();

        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoCarteraVencida(null, null, null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoIngresosYGastos(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoOcupacion(null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoContratosPorVencer(null, 30, null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoHistorialPagos(null, null, null, null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoLiquidacionGarantias(null, null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoMantenimiento(
                null, null, null, null, null, null, null));
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoTasasUtilizadas(null, null, null));

        assertThrows(OperacionCanceladaException.class, () -> SesionUsuario.ejecutarComo(asistente, () ->
                reporteService.generarDocumentoOcupacion(datos.propiedad().getId(), null, () -> true)));
    }

    private static Contrato crearContratoSecundario(org.hibernate.Session session,
                                                    Unidad unidad,
                                                    Inquilino inquilino,
                                                    String moneda,
                                                    BigDecimal canon,
                                                    BigDecimal deposito,
                                                    String estado) {
        Contrato c = new Contrato();
        c.setUnidad(unidad);
        c.setInquilino(inquilino);
        c.setInicio(LocalDate.of(2026, 1, 1));
        c.setFin(LocalDate.of(2026, 12, 31));
        c.setCanon(canon);
        c.setMoneda(moneda);
        c.setEstado(estado);
        c.setDepositoPactado(deposito);
        session.persist(c);
        session.flush();
        return c;
    }

    private static Cargo crearCargo(UUID contratoId, String tipo, String concepto,
                                    LocalDate emision, LocalDate vence, BigDecimal importe) {
        Cargo c = new Cargo();
        c.setContratoId(contratoId);
        c.setTipo(tipo);
        c.setPeriodo(emision.withDayOfMonth(1));
        c.setFechaEmision(emision);
        c.setVenceEl(vence);
        c.setImporte(importe);
        c.setConcepto(concepto);
        c.setEstado("Pendiente");
        return c;
    }

    private static String leerHojaXlsx(byte[] xlsx) throws IOException {
        Map<String, String> entradas = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                zis.transferTo(buf);
                entradas.put(entry.getName(), buf.toString(StandardCharsets.UTF_8));
                zis.closeEntry();
            }
        }
        String hoja = entradas.get("xl/worksheets/sheet1.xml");
        assertNotNull(hoja, "Falta sheet1.xml en XLSX");
        return hoja;
    }

    private UUID sembrarPagoConRecibo(UUID contratoId,
                                      long numeroRecibo,
                                      String moneda,
                                      BigDecimal monto,
                                      String metodo,
                                      String referencia,
                                      String estado) {
        return tx.ejecutar(session -> {
            Pago pago = new Pago();
            pago.setContratoId(contratoId);
            pago.setFechaPago(LocalDate.of(2026, 11, 8));
            pago.setMonedaOriginal(moneda);
            pago.setMontoOriginal(monto);
            pago.setMetodo(metodo);
            pago.setReferencia(referencia);
            pago.setEstado(estado);
            pago.aplicarConversion(moneda, monto, null);
            session.persist(pago);
            session.flush();

            Recibo recibo = new Recibo(numeroRecibo, pago, "Sofía Ramírez — Torre Latch A-01");
            session.persist(recibo);
            session.flush();
            return pago.getId();
        });
    }

    @Test
    void estadoCuentaAgrupaCargosYPagosYMuestraTotalesYGarantiaEnMonedaUnica() throws IOException {
        EstadoCuentaResponse respuesta = new EstadoCuentaResponse(
                "contrato-10",
                "2026-11-24",
                "USD",
                "450.00",
                List.of(
                        new EstadoCuentaResponse.CargoEstadoCuenta(
                                "cargo-1", "Renta", "2026-10-01", "2026-10-01", "2026-10-05",
                                "Renta octubre", "450.00", "100.00", "350.00", "Parcial"),
                        new EstadoCuentaResponse.CargoEstadoCuenta(
                                "cargo-2", "Renta", "2026-11-01", "2026-11-01", "2026-11-05",
                                "Renta noviembre", "450.00", "0.00", "450.00", "Pendiente")
                ),
                List.of(
                        new EstadoCuentaResponse.PagoEstadoCuenta(
                                "pago-1", 77L, "2026-10-04", "100.00", "USD", "100.00",
                                "Transferencia", "REF-77", "Registrado", "100.00", "0.00")
                ),
                new EstadoCuentaResponse.TotalesEstadoCuenta("900.00", "100.00", "800.00", "0.00"),
                new EstadoCuentaResponse.GarantiaEstadoCuenta("450.00", "450.00")
        );

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarDocumentoEstadoCuenta(respuesta, "Sofía Ramírez — A-01"));

        assertEquals("Estado-Cuenta-2026-11-24.pdf", doc.nombreArchivoSugerido());
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Sección: Cargos"), texto);
        assertTrue(texto.contains("Sección: Pagos"), texto);
        assertTrue(texto.contains("Recibo #77"), texto);
        assertTrue(texto.contains("Cargos: 900.00 USD"), texto);
        assertTrue(texto.contains("Pagado: 100.00 USD"), texto);
        assertTrue(texto.contains("Saldo: 800.00 USD"), texto);
        assertTrue(texto.contains("Depósito en garantía"), texto);
        assertTrue(texto.contains("Pactada: 450.00 USD"), texto);
        assertTrue(texto.contains("Saldo actual: 450.00 USD"), texto);
    }

    @Test
    void estadoCuentaSinMovimientosYExigePermisoConsultarReportes() throws IOException {
        EstadoCuentaResponse vacio = new EstadoCuentaResponse(
                "contrato-11",
                "2026-11-24",
                "NIO",
                "12000.00",
                List.of(),
                List.of(),
                new EstadoCuentaResponse.TotalesEstadoCuenta("0.00", "0.00", "0.00", "0.00"),
                null
        );

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario, () ->
                reporteService.generarDocumentoEstadoCuenta(vacio, "Contrato NIO"));
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Sin movimientos registrados"), texto);
        assertTrue(texto.contains("Sin garantía registrada"), texto);

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () ->
                reporteService.generarDocumentoEstadoCuenta(vacio, "Contrato NIO"));
    }

    @Test
    void tasasCambioFormateaOchoDecimalesYMuestraEstadosYPermisos() throws IOException {
        List<TasaResponse> tasas = List.of(
                new TasaResponse("t-1", "2026-11-24", "36.62430000", "BCN",
                        "2026-11-24T08:00:00Z", true, "1", "Ana Propietaria", "Tasa oficial"),
                new TasaResponse("t-2", "2026-11-25", "36.70000000", "Mesa",
                        "2026-11-24T09:15:00Z", false, null, null, "Pendiente revisión")
        );

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoTasasCambio(tasas));

        assertEquals("Reporte-Tasas-Cambio.pdf", doc.nombreArchivoSugerido());
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("36.62430000"), texto);
        assertTrue(texto.contains("36.70000000"), texto);
        assertTrue(texto.contains("Autorizada"), texto);
        assertTrue(texto.contains("Pendiente"), texto);
        assertTrue(texto.contains("Autorizadas: 1"), texto);
        assertTrue(texto.contains("Pendientes: 1"), texto);

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoTasasCambio(tasas));
    }

    @Test
    void resumenDashboardMuestraInstanteYTotalesSeparadosPorMoneda() throws IOException {
        MetricasResponse metricas = new MetricasResponse(
                3, 12, 8, 7,
                List.of(
                        new TotalMonedaResponse("USD", 2, "400.25"),
                        new TotalMonedaResponse("NIO", 1, "35000.50")
                ),
                List.of(
                        new TotalMonedaResponse("USD", 1, "300.10"),
                        new TotalMonedaResponse("NIO", 2, "20000.75")
                )
        );

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoResumenDashboard(metricas));

        assertEquals("Resumen-Panel-Latch.pdf", doc.nombreArchivoSugerido());
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("Generado:"), texto);
        assertTrue(texto.contains("Propiedades activas: 3"), texto);
        assertTrue(texto.contains("Contratos vigentes: 7"), texto);
        assertTrue(texto.contains("400.25 USD"), texto);
        assertTrue(texto.contains("35,000.50 NIO"), texto);
        assertTrue(texto.contains("300.10 USD"), texto);
        assertTrue(texto.contains("20,000.75 NIO"), texto);
        assertFalse(texto.contains("35,400.75"), texto);

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoResumenDashboard(metricas));
    }

    @Test
    void garantiasDepositosSeparaUsdYNioYExcluyeCanonDeRenta() throws IOException {
        DatosSembrados datos = sembrarPropiedadYUnidades();
        tx.ejecutar(session -> {
            Contrato cUsd = session.find(Contrato.class, datos.contrato().getId());
            cUsd.setCanon(new BigDecimal("999.00"));
            cUsd.setDepositoPactado(new BigDecimal("500.00"));
            cUsd.inicializarGarantiaSeguimiento();
            cUsd.getGarantiaSeguimiento().ajustarSaldo(new BigDecimal("350.00"));

            Contrato cNio = new Contrato();
            cNio.setUnidad(session.find(Unidad.class, datos.unidadDisponible().getId()));
            cNio.setInquilino(session.find(Inquilino.class, datos.inquilino().getId()));
            cNio.setInicio(LocalDate.of(2026, 2, 1));
            cNio.setFin(LocalDate.of(2027, 2, 1));
            cNio.setCanon(new BigDecimal("25000.00"));
            cNio.setMoneda("NIO");
            cNio.setEstado("Vigente");
            cNio.setDepositoPactado(new BigDecimal("12000.00"));
            cNio.inicializarGarantiaSeguimiento();
            session.persist(cNio);
            session.flush();
            return null;
        });

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoGarantias());

        assertEquals("Reporte-Garantias-Depositos.pdf", doc.nombreArchivoSugerido());
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertPdfValido(pdf);
        String texto = extraerTextoPdf(pdf);
        assertTrue(texto.contains("USD pactado: 500.00 USD / saldo: 350.00 USD"), texto);
        assertTrue(texto.contains("NIO pactado: 12,000.00 NIO / saldo: 12,000.00 NIO"), texto);
        assertFalse(texto.contains("12,500.00"), texto);
        assertFalse(texto.contains("999.00"), texto);
        assertFalse(texto.contains("25,000.00"), texto);

        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, () -> reporteService.generarDocumentoGarantias());
    }

    private DatosSembrados sembrarPropiedadYUnidades() {
        return tx.ejecutar(session -> {
            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Torre Latch");
            propiedad.setDireccion("Avenida Central 123");
            propiedad.setPropietario(session.find(Propietario.class, propietarioId));
            session.persist(propiedad);
            session.flush();

            Unidad ocupada = new Unidad();
            ocupada.setPropiedad(propiedad);
            ocupada.setCodigo("A-01");
            ocupada.setTipo("Apartamento");
            ocupada.setEstado("Ocupada");
            ocupada.setCanonReferencia(new BigDecimal("500.00"));
            ocupada.setMoneda("USD");
            session.persist(ocupada);

            Unidad disponible = new Unidad();
            disponible.setPropiedad(propiedad);
            disponible.setCodigo("A-02");
            disponible.setTipo("Apartamento");
            disponible.setEstado("Disponible");
            disponible.setCanonReferencia(new BigDecimal("450.00"));
            disponible.setMoneda("USD");
            session.persist(disponible);

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Sofía Ramírez");
            inquilino.setIdentificacion("001-010190-0001A");
            inquilino.setTelefono("8888-9999");
            inquilino.setCorreo("sofia@latch.test");
            inquilino.setContactoEmergencia("María Ramírez");
            inquilino.setActivo(true);
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidad(ocupada);
            contrato.setInquilino(inquilino);
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("500.00"));
            contrato.setMoneda("USD");
            contrato.setEstado("Vigente");
            session.persist(contrato);
            session.flush();

            return new DatosSembrados(propiedad, ocupada, disponible, inquilino, contrato);
        });
    }

    private static Pago crearPago(String contratoInfo,
                                  LocalDate fechaPago,
                                  String moneda,
                                  BigDecimal monto,
                                  String metodo,
                                  String referencia,
                                  String estado) {
        Pago pago = new Pago();
        pago.setContratoInfo(contratoInfo);
        pago.setFechaPago(fechaPago);
        pago.setMonedaOriginal(moneda);
        pago.setMontoOriginal(monto);
        pago.setMetodo(metodo);
        pago.setReferencia(referencia);
        pago.setEstado(estado);
        pago.aplicarConversion(moneda, monto, null);
        return pago;
    }

    private static void assertPdfValido(byte[] pdf) {
        assertNotNull(pdf);
        assertTrue(pdf.length > 5, "El PDF debe tener contenido.");
        String cabecera = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertTrue("%PDF-".equals(cabecera), "Cabecera PDF inválida: " + cabecera);
    }

    private static String extraerTextoPdf(byte[] pdf) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                sb.append(extractor.getTextFromPage(pagina)).append(' ');
            }
        }
        return sb.toString()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record DatosSembrados(
            Propiedad propiedad,
            Unidad unidadOcupada,
            Unidad unidadDisponible,
            Inquilino inquilino,
            Contrato contrato) {}
}
