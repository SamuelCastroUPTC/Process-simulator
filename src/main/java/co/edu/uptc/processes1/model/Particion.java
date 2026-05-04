package co.edu.uptc.processes1.model;

import java.math.BigInteger;

/**
 * Representa una partición de memoria con identidad propia.
 * <p>
 * En este modelo tanto los bloques ocupados como los huecos libres son
 * particiones con un identificador global y un único estado de ocupación.
 */
public class Particion {

    /**
     * Estado funcional de la partición.
     */
    public enum EstadoParticion {
        LIBRE,
        OCUPADA
    }

    private final int id;
    private final String nombre;
    private final BigInteger direccionInicio;
    private final BigInteger tamanio;
    private EstadoParticion estado;
    private Integer idProceso;
    private String nombreProceso;

    /**
     * Crea una partición libre.
     *
     * @param id identificador secuencial global de la partición.
     * @param inicio dirección inicial de la partición.
     * @param tamanio tamaño de la partición.
     */
    public Particion(int id, BigInteger inicio, BigInteger tamanio) {
        this.id = id;
        this.nombre = "PAR" + id;
        this.direccionInicio = inicio;
        this.tamanio = tamanio;
        this.estado = EstadoParticion.LIBRE;
        this.idProceso = null;
        this.nombreProceso = null;
    }

    /**
     * Crea una partición ocupada por un proceso.
     *
     * @param id identificador secuencial global de la partición.
     * @param inicio dirección inicial de la partición.
     * @param tamanio tamaño de la partición.
     * @param idProceso identificador del proceso alojado.
     * @param nombreProceso nombre del proceso alojado.
     */
    public Particion(int id, BigInteger inicio, BigInteger tamanio, int idProceso, String nombreProceso) {
        this(id, inicio, tamanio);
        ocupar(idProceso, nombreProceso);
    }

    /**
     * Devuelve el identificador global de la partición.
     *
     * @return identificador secuencial de la partición.
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el nombre de la partición.
     *
     * @return nombre derivado del identificador, con formato {@code PAR{id}}.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la dirección inicial de la partición.
     *
     * @return dirección de inicio.
     */
    public BigInteger getDireccionInicio() {
        return direccionInicio;
    }

    /**
     * Devuelve el tamaño de la partición.
     *
     * @return tamaño de la partición.
     */
    public BigInteger getTamanio() {
        return tamanio;
    }

    /**
     * Devuelve el estado actual de la partición.
     *
     * @return estado funcional de la partición.
     */
    public EstadoParticion getEstado() {
        return estado;
    }

    /**
     * Devuelve el identificador del proceso alojado, si existe.
     *
     * @return identificador del proceso o {@code null} si la partición está libre.
     */
    public Integer getIdProceso() {
        return idProceso;
    }

    /**
     * Devuelve el nombre del proceso alojado, si existe.
     *
     * @return nombre del proceso o {@code null} si la partición está libre.
     */
    public String getNombreProceso() {
        return nombreProceso;
    }

    /**
     * Calcula la dirección final de la partición.
     *
     * @return dirección inicial más el tamaño de la partición.
     */
    public BigInteger getDireccionFin() {
        return direccionInicio.add(tamanio);
    }

    /**
     * Indica si la partición está libre.
     *
     * @return {@code true} cuando el estado es {@link EstadoParticion#LIBRE}.
     */
    public boolean estaLibre() {
        return estado == EstadoParticion.LIBRE;
    }

    /**
     * Ocupa la partición con un proceso.
     *
     * @param idProceso identificador del proceso a alojar.
     * @param nombreProceso nombre del proceso a alojar.
     */
    public void ocupar(int idProceso, String nombreProceso) {
        this.estado = EstadoParticion.OCUPADA;
        this.idProceso = idProceso;
        this.nombreProceso = nombreProceso;
    }

    /**
     * Libera la partición y elimina la referencia al proceso alojado.
     */
    public void liberar() {
        this.estado = EstadoParticion.LIBRE;
        this.idProceso = null;
        this.nombreProceso = null;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
