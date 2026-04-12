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

    public static final String INICIO = "Listo";
    public static final String DESPACHAR = "Despachar";
    public static final String PROCESADOR = "Procesador";
    public static final String EXPIRACION_TIEMPO = "Expiracion de tiempo";
    public static final String BLOQUEAR = "Bloquear";
    public static final String BLOQUEADO = "Bloqueado";
    public static final String DESPERTAR = "Despertar";
    public static final String NO_EJECUTADO = "No Ejecutado";
    public static final String FINALIZADO = "Salida";

    private static final List<String> ESTADOS = List.of(
        INICIO,
        DESPACHAR,
        PROCESADOR,
        EXPIRACION_TIEMPO,
        BLOQUEAR,
        BLOQUEADO,
        DESPERTAR,
        NO_EJECUTADO,
        FINALIZADO
    );

    private final Map<String, List<String>> historialTexto;
    private final Map<String, List<Proceso>> historialProcesos;

    public RegistroSimulacion() {
        this.historialTexto = new LinkedHashMap<>();
        this.historialProcesos = new LinkedHashMap<>();
        for (String estado : ESTADOS) {
            historialTexto.put(estado, new ArrayList<>());
            historialProcesos.put(estado, new ArrayList<>());
        }
    }

    public void registrar(String estado, Proceso snapshot) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshot.toString());
        historialProcesos.computeIfAbsent(estado, key -> new ArrayList<>()).add(snapshot);
    }

    public void registrarTexto(String estado, String mensaje) {
        historialTexto.computeIfAbsent(estado, key -> new ArrayList<>()).add(mensaje);
    }

    public Map<String, List<String>> getHistorialTexto() {
        return Collections.unmodifiableMap(historialTexto);
    }

    public Map<String, List<Proceso>> getHistorialProcesos() {
        return Collections.unmodifiableMap(historialProcesos);
    }

    public List<String> getHistorialTexto(String estado) {
        return historialTexto.getOrDefault(estado, List.of());
    }

    public List<Proceso> getHistorialProcesos(String estado) {
        return historialProcesos.getOrDefault(estado, List.of());
    }
}

