package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Política de retención de respaldos (TRD): conserva hasta 7 conjuntos diarios
 * y 4 conjuntos semanales, preservando siempre el paquete más reciente y sin tocar archivos ajenos.
 */
public final class PoliticaRetencionRespaldos {
    public static final int MAX_DIARIOS = 7;
    public static final int MAX_SEMANALES = 4;

    private static final Pattern PATRON_NOMBRE = Pattern.compile(
            "^latch-respaldo-(\\d{8}-\\d{6})(?:-[a-zA-Z0-9]+)?\\.lbackup$");
    private static final DateTimeFormatter FORMATO_MARCA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public record PlanRetencion(List<Path> conservar, List<Path> depurables) { }

    private record Candidato(Path ruta, LocalDateTime marca) { }

    public PlanRetencion evaluar(Path directorio) {
        Objects.requireNonNull(directorio, "Falta el directorio de respaldos.");
        if (!Files.isDirectory(directorio)) {
            return new PlanRetencion(List.of(), List.of());
        }
        List<Candidato> candidatos = new ArrayList<>();
        try (var flujo = Files.list(directorio)) {
            for (Path entrada : flujo.toList()) {
                if (!Files.isRegularFile(entrada)) continue;
                String nombre = entrada.getFileName().toString();
                Matcher m = PATRON_NOMBRE.matcher(nombre);
                if (!m.matches()) continue;
                try {
                    LocalDateTime marca = LocalDateTime.parse(m.group(1), FORMATO_MARCA);
                    candidatos.add(new Candidato(entrada, marca));
                } catch (DateTimeParseException ignored) {
                    // Archivos con marca inválida no se depuran automáticamente.
                }
            }
        } catch (IOException e) {
            throw RespaldoException.operativo("ERR-055: No se pudo evaluar la retención de respaldos.", e);
        }

        if (candidatos.isEmpty()) {
            return new PlanRetencion(List.of(), List.of());
        }

        candidatos.sort(Comparator.comparing(Candidato::marca).reversed()
                .thenComparing(c -> c.ruta().getFileName().toString(), Comparator.reverseOrder()));

        Set<Path> conservar = new LinkedHashSet<>();
        Set<LocalDate> diasCubiertos = new LinkedHashSet<>();
        Set<String> semanasCubiertas = new LinkedHashSet<>();

        // Preservar siempre el respaldo más reciente.
        Candidato masReciente = candidatos.getFirst();
        conservar.add(masReciente.ruta());

        for (Candidato c : candidatos) {
            LocalDate dia = c.marca().toLocalDate();
            if (!diasCubiertos.contains(dia) && diasCubiertos.size() < MAX_DIARIOS) {
                diasCubiertos.add(dia);
                conservar.add(c.ruta());
            }
        }

        WeekFields wf = WeekFields.ISO;
        for (Candidato c : candidatos) {
            LocalDate dia = c.marca().toLocalDate();
            String claveSemana = dia.get(wf.weekBasedYear()) + "-W" + dia.get(wf.weekOfWeekBasedYear());
            if (!semanasCubiertas.contains(claveSemana) && semanasCubiertas.size() < MAX_SEMANALES) {
                semanasCubiertas.add(claveSemana);
                conservar.add(c.ruta());
            }
        }

        List<Path> depurables = candidatos.stream()
                .map(Candidato::ruta)
                .filter(p -> !conservar.contains(p))
                .toList();
        return new PlanRetencion(List.copyOf(conservar), depurables);
    }
}
