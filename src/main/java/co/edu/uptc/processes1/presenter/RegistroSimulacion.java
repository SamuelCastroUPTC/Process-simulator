package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Proceso;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historial organizado por estado para la simulacion.
 *
 * historialTexto: columna -> entradas formateadas (ej. "P1 (10) B").
 * historialProcesos: columna -> snapshots de proceso para mostrar en tablas.
 */
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

    // ¡NUEVO RECORD! Para los eventos de memoria
    public record SnapshotMemoria(
            String evento,
            String nombreProceso,
            BigInteger direccionInicio,
            BigInteger tamanio,
            String detalle,
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

    // ==========================================
    // 2. CONSTANTES DE ESTADOS
    // ==========================================
    public static final String LISTO = "Listo";
    public static final String INICIO = LISTO;
    public static final String DESPACHAR = "Despachar";
    public static final String PROCESADOR = "Procesador";
    public static final String EXPIRACION_TIEMPO = "Expiracion de tiempo";
    public static final String NO_EJECUTADO = "No Ejecutado";
    public static final String FINALIZADO = "Salida";
    public static final String FINALIZACION_PARTICIONES = "Finalizacion de particiones";

    // ¡NUEVAS CONSTANTES! Para memoria
    public static final String ASIGNACION   = "Asignación";
    public static final String LIBERACION   = "Liberación";
    public static final String CONDENSACION = "Condensación";

    private static final List<String> ESTADOS = List.of(
            INICIO, DESPACHAR, PROCESADOR, EXPIRACION_TIEMPO,
            NO_EJECUTADO, FINALIZADO, FINALIZACION_PARTICIONES
    );

    // ¡NUEVA LISTA! Separada para los estados de memoria
    private static final List<String> ESTADOS_MEMORIA = List.of(
            ASIGNACION, LIBERACION, CONDENSACION
    );

    // ==========================================
    // 3. MAPAS DE HISTORIAL (Variables de clase)
    // ==========================================
    private final Map<String, List<String>> historialTexto;
    private final Map<String, List<SnapshotProceso>> historialProcesos;
    private final Map<String, List<UsoParticion>> usoParticiones;
    
    // ¡NUEVO MAPA! Para el historial de memoria
    private final Map<String, List<SnapshotMemoria>> historialMemoria;

    // ==========================================
    // 4. CONSTRUCTOR
    // ==========================================
    public RegistroSimulacion() {
        this.historialTexto = new LinkedHashMap<>();
        this.historialProcesos = new LinkedHashMap<>();
        this.usoParticiones = new LinkedHashMap<>();
        this.historialMemoria = new LinkedHashMap<>(); // Inicializamos el mapa nuevo
        
        // ¡NUEVO CICLO! Inicializamos las listas vacías para los eventos de memoria
        for (String estado : ESTADOS_MEMORIA) {
            historialMemoria.put(estado, new ArrayList<>());
        }
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

    // ¡NUEVO MÉTODO! Para registrar eventos de memoria
    public void registrarMemoria(String evento, SnapshotMemoria snapshot) {
        historialMemoria.computeIfAbsent(evento, k -> new ArrayList<>()).add(snapshot);
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

    // ¡NUEVO MÉTODO! Para obtener el historial de memoria
    public List<SnapshotMemoria> getHistorialMemoria(String evento) {
        return Collections.unmodifiableList(
                historialMemoria.getOrDefault(evento, List.of())
        );
    }
}