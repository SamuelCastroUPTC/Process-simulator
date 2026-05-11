package co.edu.uptc.processes1.view;

import java.util.List;
import java.util.Map;

import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.presenter.RegistroSimulacion;

public interface IView {

    // ── Errores y Mensajes ────────────────────────────────────────────────────
    void mostrarError(String mensaje);
    void mostrarAviso(String mensaje);
    void mostrarExito(String mensaje);

    // ── Tabla de Procesos Cargados ────────────────────────────────────────────
    void actualizarTablaCargados(List<Proceso> procesos);
    void limpiarFormularioCarga();
    void actualizarEstadoMemoria(MemoriaVariable memoria);

    // ── Simulación ────────────────────────────────────────────────────────────
    void setBtnIniciarHabilitado(boolean habilitado);
    void actualizarEstadoSimulacion(String estado);

    void mostrarHistorialPorProceso(Map<Integer, List<RegistroSimulacion.CambioParticionInfo>> cambios);


    // ── Historial por Estado ──────────────────────────────────────────────────
    
    /**
     * Muestra el historial para un estado específico.
     * 
     * @param estado Nombre del estado (ej. "Procesador", "Salida", etc.)
     * @param datos Lista de snapshots de procesos para ese estado
     * @param usosParticion Lista de usos de partición (puede ser vacía)
     * @param finalizacionParticiones Lista de finalizaciones de particiones (puede ser vacía)
     */
    void mostrarHistorial(
        String estado, 
        List<RegistroSimulacion.SnapshotProceso> datos,
        List<RegistroSimulacion.UsoParticion> usosParticion,
        List<RegistroSimulacion.FinalizacionParticionInfo> finalizacionParticiones
    );

    // ── Datos del Formulario (delegados al modal FormProcces) ─────────────────
    String getNombreProceso();
    String getTiempoProceso();
    String getTamanioMemoria();

    // ── Control del modal ─────────────────────────────────────────────────────
    void cerrarModalFormulario();

    // En IView.java
void mostrarCondensaciones(List<RegistroSimulacion.CondensacionInfo> condensaciones);
    // En IView.java
void mostrarCompactaciones(List<RegistroSimulacion.CompactacionInfo> compactaciones);
}

