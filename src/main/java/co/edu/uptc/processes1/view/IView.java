package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import java.util.List;

/**
 * Interfaz IView — Contrato MVP entre la Vista y el Presentador.
 *
 * Versión simplificada para el modelo de particiones de memoria:
 *   - Se eliminaron banderas de ciclo de vida avanzadas.
 *   - Se agregó:     getTamanioMemoria  (campo nuevo en el formulario)
 */
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

    // ── Historial por Estado ──────────────────────────────────────────────────

    void mostrarHistorial(String estado, List<RegistroSimulacion.SnapshotProceso> datos);

    // ── Datos del Formulario (delegados al modal FormProcces) ─────────────────

    /** Nombre del proceso. */
    String getNombreProceso();

    /** Tiempo de ejecución en segundos (como String para validar en el Presentador). */
    String getTiempoProceso();

    /** Tamaño requerido en memoria en unidades (nuevo campo). */
    String getTamanioMemoria();

    // ── Control del modal ─────────────────────────────────────────────────────

    /**
     * Cierra el modal de creación de proceso.
     * El Presentador lo llama tras validar y registrar el proceso con éxito.
     */
    void cerrarModalFormulario();

    void mostrarHistorialMemoria(String evento, List<RegistroSimulacion.SnapshotMemoria> datos);

    void mostrarHistorialCondensacion(List<RegistroSimulacion.SnapshotCondensacion> datos);
}