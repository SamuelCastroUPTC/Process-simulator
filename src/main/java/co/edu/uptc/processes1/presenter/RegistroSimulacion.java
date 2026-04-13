package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Proceso;

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

    public record UsoParticion(
        int idProceso,
        String nombreProceso,
        String nombreParticion,
        long veces,
        long tiempoCpu
    ) {}

    public record SnapshotProceso(
        int id,
        String nombre,
        long tiempoRestante,
        long tamanioMemoria,
        boolean pasaPorBloqueado,
        String estadoActual,
        String nombreParticion
    ) {}

    public static final String INICIO = "Listo";
    public static final String DESPACHAR = "Despachar";
    public static final String PROCESADOR = "Procesador";
    public static final String EXPIRACION_TIEMPO = "Expiracion de tiempo";
    public static final String BLOQUEAR = "Bloquear";
    public static final String BLOQUEADO = "Bloqueado";
    public static final String DESPERTAR = "Despertar";
    public static final String NO_EJECUTADO = "No Ejecutado";
    public static final String FINALIZADO = "Salida";
    public static final String FINALIZACION_PARTICIONES = "Finalizacion de particiones";

    private static final List<String> ESTADOS = List.of(
        INICIO,
        DESPACHAR,
        PROCESADOR,
        EXPIRACION_TIEMPO,
        BLOQUEAR,
        BLOQUEADO,
        DESPERTAR,
        NO_EJECUTADO,
        FINALIZADO,
        FINALIZACION_PARTICIONES
    );

    private final Map<String, List<String>> historialTexto;
    private final Map<String, List<SnapshotProceso>> historialProcesos;
    private final Map<String, Map<Integer, UsoParticionAcumulado>> usoParticiones;

    public RegistroSimulacion() {
        this.historialTexto = new LinkedHashMap<>();
        this.historialProcesos = new LinkedHashMap<>();
        this.usoParticiones = new LinkedHashMap<>();
        for (String estado : ESTADOS) {
            historialTexto.put(estado, new ArrayList<>());
            historialProcesos.put(estado, new ArrayList<>());
        }
    }

    public void registrar(String estado, Proceso snapshot) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshot.toString());
        String nombreParticion = snapshot.getParticion() != null ? snapshot.getParticion().getNombre() : null;
        SnapshotProceso snapshotLigero = new SnapshotProceso(
            snapshot.getId(),
            snapshot.getNombre(),
            snapshot.getTiempoRestante(),
            snapshot.getTamanioMemoria(),
            snapshot.isPasaPorBloqueado(),
            snapshot.getEstadoActual(),
            nombreParticion
        );
        historialProcesos.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshotLigero);
    }

    public void registrarTexto(String estado, String mensaje) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(mensaje);
    }

    public void registrarUsoParticion(int idProceso, String nombreProceso, String nombreParticion, long tiempoCpu) {
        if (nombreParticion == null || nombreParticion.isBlank()) {
            return;
        }
        Map<Integer, UsoParticionAcumulado> porProceso = usoParticiones.computeIfAbsent(nombreParticion, key -> new LinkedHashMap<>());
        UsoParticionAcumulado acumulado = porProceso.computeIfAbsent(
            idProceso,
            key -> new UsoParticionAcumulado(idProceso, nombreProceso, nombreParticion)
        );
        acumulado.incrementar(tiempoCpu);
    }

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
        for (Map<Integer, UsoParticionAcumulado> porProceso : usoParticiones.values()) {
            for (UsoParticionAcumulado acumulado : porProceso.values()) {
                resultado.add(acumulado.aUsoParticion());
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    private static final class UsoParticionAcumulado {
        private final int idProceso;
        private final String nombreProceso;
        private final String nombreParticion;
        private long veces;
        private long tiempoCpu;

        private UsoParticionAcumulado(int idProceso, String nombreProceso, String nombreParticion) {
            this.idProceso = idProceso;
            this.nombreProceso = nombreProceso;
            this.nombreParticion = nombreParticion;
        }

        private void incrementar(long tiempoCpu) {
            this.veces++;
            this.tiempoCpu += tiempoCpu;
        }

        private UsoParticion aUsoParticion() {
            return new UsoParticion(idProceso, nombreProceso, nombreParticion, veces, tiempoCpu);
        }
    }
}

