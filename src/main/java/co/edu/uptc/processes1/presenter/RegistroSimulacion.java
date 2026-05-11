package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.uptc.processes1.model.Proceso;

public class RegistroSimulacion {

    // ==========================================
    // 1. RECORDS (Estructuras de datos)
    // ==========================================
    public record UsoParticion(
            int idProceso,
            String nombreProceso,
            String nombreParticion,
            long veces,
            BigInteger tiempoCpu
    ) {}

    public record SnapshotMemoria(
            String evento,
            String nombreProceso,
            BigInteger direccionInicio,
            BigInteger tamanio,
            String detalle,
            String metadatoExtra,
            List<String> estadoHuecos,
            List<String> estadoBloques
    ) {}

    public record SnapshotProceso(
            int id,
            String nombre,
            BigInteger tiempoRestante,
            BigInteger tamanioMemoria,
            String estadoActual,
            String nombreParticion,
            String motivoNoEjecucion
    ) {}

    public record CondensacionInfo(
        String nombreNuevaParticion,
        String descripcion,
        BigInteger tamanio
    ) {}

    public record CompactacionInfo(
        String nombreNuevaParticion,
        String nombreParticionOriginal,
        String descripcion,
        BigInteger tamanio,
        BigInteger direccionInicio,
        BigInteger direccionFin
    ) {}

    public record FinalizacionParticionInfo(
        String nombreParticion,
        BigInteger tamanio
    ) {}

    // NUEVO RECORD para cambios de partición
    public record CambioParticionInfo(
        int idProceso,
        String nombreProceso,
        String nombreParticion,
        BigInteger direccionInicio,
        BigInteger direccionFin,
        String momento
    ) {}

    // ==========================================
    // 2. CONSTANTES DE ESTADOS
    // ==========================================
    public static final String INICIO = "Listo";
    public static final String DESPACHAR = "Despachar";
    public static final String PROCESADOR = "Procesador";
    public static final String EXPIRACION_TIEMPO = "Expiracion de tiempo";
    public static final String NO_EJECUTADO = "No Ejecutado";
    public static final String FINALIZADO = "Salida";
    public static final String FINALIZACIONDEPARTICION = "Finalizacion de Particion";
    public static final String CONDENSACION = "Condensacion";
    public static final String COMPACTACION = "Compactacion";
    public static final String HISTORIALPROCESO = "Historial de Procesos";

    // ==========================================
    // 3. MAPAS DE HISTORIAL (Variables de clase)
    // ==========================================
    private final Map<String, List<String>> historialTexto;
    private final Map<String, List<SnapshotProceso>> historialProcesos;
    private final Map<String, List<UsoParticion>> usoParticiones;
    private final List<FinalizacionParticionInfo> historialFinalizacionParticiones;
    private final List<CondensacionInfo> historialCondensaciones;
    private final List<CompactacionInfo> historialCompactaciones;
    private final Map<String, List<SnapshotMemoria>> historialMemoria;
    private final Map<Integer, List<CambioParticionInfo>> historialCambiosParticion;

    // ==========================================
    // 4. CONSTRUCTOR
    // ==========================================
    public RegistroSimulacion() {
        this.historialTexto = new LinkedHashMap<>();
        this.historialProcesos = new LinkedHashMap<>();
        this.usoParticiones = new LinkedHashMap<>();
        this.historialMemoria = new LinkedHashMap<>();
        this.historialFinalizacionParticiones = new ArrayList<>();
        this.historialCondensaciones = new ArrayList<>();
        this.historialCompactaciones = new ArrayList<>();
        this.historialCambiosParticion = new LinkedHashMap<>();
    }

    // ==========================================
    // 5. MÉTODOS DE REGISTRO
    // ==========================================
    public void registrar(String estado, Proceso snapshot) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshot.toString());
        String nombreParticion = snapshot.getParticion() != null ? snapshot.getParticion().getNombre() : null;
        SnapshotProceso snapshotLigero = new SnapshotProceso(
                snapshot.getId(),
                snapshot.getNombre(),
                snapshot.getTiempoRestante(),
                snapshot.getTamanioMemoria(),
                snapshot.getEstadoActual(),
                nombreParticion,
                null
        );
        historialProcesos.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshotLigero);
    }

    public void registrarNoEjecutado(Proceso snapshot, String motivo) {
        historialTexto.computeIfAbsent(NO_EJECUTADO, key -> new ArrayList<>()).add(snapshot.toString());
        String nombreParticion = snapshot.getParticion() != null ? snapshot.getParticion().getNombre() : null;
        SnapshotProceso snapshotLigero = new SnapshotProceso(
                snapshot.getId(),
                snapshot.getNombre(),
                snapshot.getTiempoRestante(),
                snapshot.getTamanioMemoria(),
                snapshot.getEstadoActual(),
                nombreParticion,
                motivo
        );
        historialProcesos.computeIfAbsent(NO_EJECUTADO, key -> new ArrayList<>()).add(snapshotLigero);
    }

    public void registrarTexto(String estado, String mensaje) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(mensaje);
    }

    public void registrarUsoParticion(int idProceso, String nombreProceso, String nombreParticion, BigInteger tiempoCpu) {
        if (nombreParticion == null || nombreParticion.isBlank()) {
            return;
        }
        List<UsoParticion> eventos = usoParticiones.computeIfAbsent(nombreParticion, key -> new ArrayList<>());
        eventos.add(new UsoParticion(idProceso, nombreProceso, nombreParticion, 1L, tiempoCpu));
    }

    public void registrarMemoria(String evento, SnapshotMemoria snapshot) {
        historialMemoria.computeIfAbsent(evento, k -> new ArrayList<>()).add(snapshot);
    }

    public void registrarFinalizacionParticion(String nombreParticion, BigInteger tamanio) {
        historialFinalizacionParticiones.add(
            new FinalizacionParticionInfo(nombreParticion, tamanio)
        );
    }

    // Método para registrar condensación
    public void registrarCondensacion(String nombreParticion, String descripcion, BigInteger tamanio) {
        historialCondensaciones.add(new CondensacionInfo(nombreParticion, descripcion, tamanio));
    }

    // Método para registrar compactación
    public void registrarCompactacion(String nombreNuevaParticion, String nombreOriginal, 
                                       String descripcion, BigInteger tamanio,
                                       BigInteger direccionInicio, BigInteger direccionFin) {
        historialCompactaciones.add(new CompactacionInfo(nombreNuevaParticion, nombreOriginal, 
            descripcion, tamanio, direccionInicio, direccionFin));
    }

    // NUEVO: Registrar cambio de partición de un proceso
    public void registrarCambioParticion(int idProceso, String nombreProceso, 
                                          String nombreParticion, BigInteger dirInicio, 
                                          BigInteger dirFin, String momento) {
        historialCambiosParticion
            .computeIfAbsent(idProceso, k -> new ArrayList<>())
            .add(new CambioParticionInfo(idProceso, nombreProceso, nombreParticion, 
                  dirInicio, dirFin, momento));
    }

    // ==========================================
    // 6. GETTERS Y UTILIDADES
    // ==========================================
    public Map<String, List<String>> getHistorialTexto() {
        return Collections.unmodifiableMap(historialTexto);
    }

    public Map<String, List<SnapshotProceso>> getHistorialProcesos() {
        return Collections.unmodifiableMap(historialProcesos);
    }

    public List<String> getHistorialTexto(String estado) {
        return historialTexto.getOrDefault(estado, List.of());
    }

    public List<SnapshotProceso> getHistorialProcesos(String estado) {
        return historialProcesos.getOrDefault(estado, List.of());
    }

    public void copiarEstado(String estadoOrigen, String estadoDestino) {
        List<String> origenTexto = historialTexto.getOrDefault(estadoOrigen, List.of());
        List<SnapshotProceso> origenProcesos = historialProcesos.getOrDefault(estadoOrigen, List.of());
        historialTexto.put(estadoDestino, new ArrayList<>(origenTexto));
        historialProcesos.put(estadoDestino, new ArrayList<>(origenProcesos));
    }

    public List<UsoParticion> getUsoParticiones() {
        List<UsoParticion> resultado = new ArrayList<>();
        for (List<UsoParticion> eventos : usoParticiones.values()) {
            resultado.addAll(eventos);
        }
        return Collections.unmodifiableList(resultado);
    }

    public List<FinalizacionParticionInfo> getHistorialFinalizacionParticiones() {
        return Collections.unmodifiableList(historialFinalizacionParticiones);
    }

    public List<SnapshotMemoria> getHistorialMemoria(String evento) {
        return Collections.unmodifiableList(
                historialMemoria.getOrDefault(evento, List.of())
        );
    }

    public List<CondensacionInfo> getHistorialCondensaciones() {
        return Collections.unmodifiableList(historialCondensaciones);
    }

    public List<CompactacionInfo> getHistorialCompactaciones() {
        return Collections.unmodifiableList(historialCompactaciones);
    }

    // NUEVOS GETTERS para cambios de partición
    public List<CambioParticionInfo> getHistorialCambiosParticion(int idProceso) {
        return Collections.unmodifiableList(
            historialCambiosParticion.getOrDefault(idProceso, List.of())
        );
    }

    public Map<Integer, List<CambioParticionInfo>> getTodosCambiosParticion() {
        return Collections.unmodifiableMap(historialCambiosParticion);
    }
}