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
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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
