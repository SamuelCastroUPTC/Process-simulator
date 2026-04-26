package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.model.Particion;

import java.math.BigInteger;
import java.util.List;

/**
 * Interfaz IPresenter â€” Contrato MVP entre la Vista y el Presentador.
 * La Vista solo conoce esta interfaz, nunca la implementaciÃ³n concreta.
 *
 * Cada mÃ©todo representa un evento de usuario que la Vista delega al Presentador.
 */
public interface IPresenter {

    /**
     * Agrega un proceso validando reglas de negocio.
     * Debe rechazar nombres duplicados.
     */
    void agregarProceso(
        String nombre,
        BigInteger tiempo,
        BigInteger tamanioMemoria,
        boolean pasaPorBloqueado
    );

    void agregarParticion(String nombre, BigInteger tamano);

    /** Inicia el calculo completo de la simulacion en bucle cerrado. */
    RegistroSimulacion iniciarSimulacion();

    /** Devuelve una vista de solo lectura de procesos cargados. */
    List<Proceso> getProcesosCargados();

    /** Devuelve las particiones de memoria disponibles. */
    List<Particion> getParticionesMemoria();

    /** Devuelve el uso acumulado de particiones de la ultima simulacion. */
    List<RegistroSimulacion.UsoParticion> getUsoParticiones();

    /** El usuario presionÃ³ "Cargar Proceso". El Presentador leerÃ¡ los datos de la Vista. */
    void onCargarProceso();

    /** El usuario presionÃ³ "Iniciar SimulaciÃ³n". */
    void onIniciarSimulacion();

    /**
     * El usuario presionÃ³ uno de los 8 botones de historial.
     * @param estado Nombre del estado (ej. "Procesador", "Bloqueado", ...)
     */
    void onVerHistorial(String estado);
    /**
     * El usuario presionó "Eliminar" en la tabla de procesos cargados.
     * Debe quitar el proceso de la lista antes de iniciar la simulación.
     */
    void onEliminarProceso(co.edu.uptc.processes1.model.Proceso proceso);
    
    /** El usuario presionó "Eliminar" en la tabla de particiones. */
    void onEliminarParticion(Particion particion);

    /** El usuario seleccionó una partición para editar. */
    void onEditarParticion(Particion particion);
}
