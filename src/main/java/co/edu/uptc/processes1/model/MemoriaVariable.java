package co.edu.uptc.processes1.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestiona una memoria variable usando una lista única de particiones.
 * <p>
 * Cada partición representa tanto bloques ocupados como huecos libres,
 * mantenidos en orden ascendente de dirección de memoria.
 * El contador global de IDs garantiza identidad única y persistencia
 * en los registros del simulador.
 */
public class MemoriaVariable {

    /**
     * Resultado de una operación de asignación de memoria.
     */
    public record ResultadoAsignacion(BigInteger direccion) {}

    /**
     * Representa un movimiento de proceso durante la compactación post-liberación.
     */
    public record EventoMovimiento(
        String particionAnterior,
        String particionNueva,
        String nombreProceso,
        BigInteger direccionAnterior,
        BigInteger direccionNueva,
        BigInteger tamanio
    ) {}

    /**
     * Resultado de una operación de liberación con movimientos.
     */
    public record EventosLiberacion(
        List<EventoMovimiento> movimientos
    ) {}

    private final BigInteger tamanioTotal;
    private final List<Particion> particiones;
    private int contadorId;

    /**
     * Crea una instancia con una partición inicial LIBRE.
     *
     * @param tamanioTotal tamaño total de la memoria.
     */
    public MemoriaVariable(BigInteger tamanioTotal) {
        this.tamanioTotal = tamanioTotal;
        this.particiones = new ArrayList<>();
        this.contadorId = 1;

        // Crea la partición inicial: PAR1, libre, toda la memoria.
        Particion inicial = new Particion(contadorId++, BigInteger.ZERO, tamanioTotal);
        particiones.add(inicial);
    }

    /**
     * Asigna memoria a un proceso usando First Fit.
     * <p>
     * Si no hay hueco suficiente, retorna null.
     *
     * @param idProceso identificador del proceso.
     * @param nombreProceso nombre del proceso.
     * @param tamanio tamaño requerido.
     * @return {@code ResultadoAsignacion} con dirección asignada o null si falla.
     */
    public ResultadoAsignacion asignar(int idProceso, String nombreProceso, BigInteger tamanio) {
        // Intento: First Fit sin compactación
        ResultadoAsignacion resultado = intentarAsignarFirstFit(idProceso, nombreProceso, tamanio);
        if (resultado.direccion() != null) {
            return resultado;
        }

        return new ResultadoAsignacion(null);
    }

    /**
     * Intenta asignar memoria usando First Fit en la lista actual.
     */
    private ResultadoAsignacion intentarAsignarFirstFit(int idProceso, String nombreProceso, BigInteger tamanio) {
        for (int i = 0; i < particiones.size(); i++) {
            Particion p = particiones.get(i);
            if (!p.estaLibre() || p.getTamanio().compareTo(tamanio) < 0) {
                continue;
            }

            BigInteger direccionAsignada = p.getDireccionInicio();

            if (p.getTamanio().compareTo(tamanio) == 0) {
                // Caso exacto: ocupar la partición sin crear nueva
                p.ocupar(idProceso, nombreProceso);
            } else {
                // Caso con sobra: ocupar, crear nueva partición LIBRE
                BigInteger tamanioSobrante = p.getTamanio().subtract(tamanio);
                p.ocupar(idProceso, nombreProceso);
                
                // Ajustar tamaño de la partición existente
                // NOTA: Particion es inmutable en algunos campos, así que creamos nueva
                Particion ocupada = new Particion(p.getId(), p.getDireccionInicio(), tamanio, idProceso, nombreProceso);
                particiones.set(i, ocupada);

                // Crear nueva partición LIBRE para el sobrante
                BigInteger dirIniciaNueva = direccionAsignada.add(tamanio);
                Particion libre = new Particion(contadorId++, dirIniciaNueva, tamanioSobrante);
                particiones.add(i + 1, libre);
            }

            return new ResultadoAsignacion(direccionAsignada);
        }

        return new ResultadoAsignacion(null);
    }

    /**
     * Libera un proceso y realiza shifting automático de memoria.
     * <p>
     * Realiza movimientos de procesos para tapar huecos generados por la liberación.
     *
     * @param idProceso identificador del proceso a liberar.
     * @return {@code EventosLiberacion} con movimientos, o null si no existe.
     */
    public EventosLiberacion liberar(int idProceso) {
        // Paso 1: Encontrar la partición ocupada
        int indiceOcupada = -1;
        Particion ocupada = null;
        for (int i = 0; i < particiones.size(); i++) {
            Particion p = particiones.get(i);
            if (!p.estaLibre() && p.getIdProceso() == idProceso) {
                indiceOcupada = i;
                ocupada = p;
                break;
            }
        }

        if (ocupada == null) {
            return null; // Proceso no encontrado
        }

        String nombreParticionAnterior = ocupada.getNombre();
        List<EventoMovimiento> movimientos = new ArrayList<>();

        // Paso 2: Marcar como LIBRE con nuevo ID
        Particion hueco = new Particion(contadorId++, ocupada.getDireccionInicio(), ocupada.getTamanio());
        particiones.set(indiceOcupada, hueco);

        // Paso 4: Mover procesos hacia arriba
        // El hueco actual empieza en la dirección de la partición liberada
        BigInteger dirHuecoActual = hueco.getDireccionInicio();
        int indiceHueco = indiceOcupada; // rastrea dónde está el hueco en la lista

        for (int i = indiceOcupada + 1; i < particiones.size(); i++) {
            Particion aMover = particiones.get(i);
            if (aMover.estaLibre()) continue; // solo mueve OCUPADAS

            BigInteger dirAnterior = aMover.getDireccionInicio();
            String nombreAnterior = aMover.getNombre();

            // Nuevo ID para la partición desplazada
            Particion movida = new Particion(
                contadorId++,
                dirHuecoActual,
                aMover.getTamanio(),
                aMover.getIdProceso(),
                aMover.getNombreProceso()
            );

            // El hueco se mueve a donde estaba la partición que subió
            Particion nuevoHueco = new Particion(
                contadorId++,
                dirAnterior,
                aMover.getTamanio()
            );

            movimientos.add(new EventoMovimiento(
                nombreAnterior,          // partición anterior (PAR_VIEJA)
                movida.getNombre(),      // partición nueva   (PAR_NUEVA con nuevo ID)
                aMover.getNombreProceso(),
                dirAnterior,             // dirección anterior
                dirHuecoActual,          // dirección nueva
                aMover.getTamanio()
            ));

            particiones.set(i, movida);
            particiones.set(indiceHueco, nuevoHueco);

            // El hueco ahora ocupa el lugar donde estaba aMover
            dirHuecoActual = dirAnterior;
            indiceHueco = i;
        }

        return new EventosLiberacion(movimientos);
    }

    /**
     * Libera un proceso sin hacer shifting ni condensación.
     * <p>
     * Usado cuando expira el quantum (Round Robin) y el proceso vuelve a la cola.
     * Solo marca la partición como LIBRE reutilizando el mismo ID.
     *
     * @param idProceso identificador del proceso a liberar.
     * @return {@code true} si el proceso fue encontrado y liberado, {@code false} en caso contrario.
     */
    public boolean liberarSinDesplazar(int idProceso) {
        for (int i = 0; i < particiones.size(); i++) {
            Particion p = particiones.get(i);
            if (!p.estaLibre() && p.getIdProceso() == idProceso) {
                // Reutiliza el MISMO ID: solo marca libre, no genera nuevo ID
                Particion libre = new Particion(p.getId(),
                    p.getDireccionInicio(), p.getTamanio());
                particiones.set(i, libre);
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve una copia inmutable de la lista de particiones.
     *
     * @return lista inmutable de todas las particiones.
     */
    public List<Particion> getParticiones() {
        return Collections.unmodifiableList(new ArrayList<>(particiones));
    }

    /**
     * Devuelve todas las particiones ocupadas.
     *
     * @return lista inmutable de particiones con estado OCUPADA.
     */
    public List<Particion> getParticionesOcupadas() {
        return particiones.stream()
            .filter(p -> !p.estaLibre())
            .toList();
    }

    /**
     * Devuelve todas las particiones libres.
     *
     * @return lista inmutable de particiones con estado LIBRE.
     */
    public List<Particion> getParticionesLibres() {
        return particiones.stream()
            .filter(Particion::estaLibre)
            .toList();
    }

    /**
     * Calcula el espacio libre total en memoria.
     *
     * @return suma de tamaños de todas las particiones LIBRES.
     */
    public BigInteger getEspacioLibreTotal() {
        return particiones.stream()
            .filter(Particion::estaLibre)
            .map(Particion::getTamanio)
            .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * Calcula el espacio ocupado total en memoria.
     *
     * @return suma de tamaños de todas las particiones OCUPADAS.
     */
    public BigInteger getEspacioOcupado() {
        return particiones.stream()
            .filter(p -> !p.estaLibre())
            .map(Particion::getTamanio)
            .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * Devuelve el tamaño total de la memoria.
     *
     * @return tamaño total.
     */
    public BigInteger getTamanioTotal() {
        return tamanioTotal;
    }

    /**
     * Devuelve el contador actual de IDs de partición.
     * <p>
     * Útil para serialización y debugging.
     *
     * @return contador actual.
     */
    public int getContadorIdActual() {
        return contadorId;
    }

    /**
     * Devuelve una lista de huecos (particiones LIBRES) para compatibilidad.
     * <p>
     * Usado por visualización y otros componentes que aún esperan el modelo antiguo.
     *
     * @return lista inmutable de particiones LIBRES.
     */
    public List<Particion> getHuecos() {
        return getParticionesLibres();
    }

    /**
     * Devuelve una lista de bloques (particiones OCUPADAS) para compatibilidad.
     * <p>
     * Usado por visualización y otros componentes que aún esperan el modelo antiguo.
     *
     * @return lista inmutable de particiones OCUPADAS.
     */
    public List<Particion> getBloquesOcupados() {
        return getParticionesOcupadas();
    }
}
