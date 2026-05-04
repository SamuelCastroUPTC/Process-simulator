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
    public record ResultadoAsignacion(BigInteger direccion, boolean compactoAntes) {}

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
     * Representa una condensación de dos huecos libres contiguos.
     */
    public record EventoCondensacion(
        String particion1,
        String particion2,
        String particionResultante,
        BigInteger tamanioResultante
    ) {}

    /**
     * Resultado de una operación de liberación con movimientos y condensación.
     */
    public record EventosLiberacion(
        List<EventoMovimiento> movimientos,
        EventoCondensacion condensacion
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
     * Si no hay hueco suficiente pero hay espacio libre total, compacta
     * la memoria e intenta de nuevo.
     *
     * @param idProceso identificador del proceso.
     * @param nombreProceso nombre del proceso.
     * @param tamanio tamaño requerido.
     * @return {@code ResultadoAsignacion} con dirección asignada o null si falla.
     */
    public ResultadoAsignacion asignar(int idProceso, String nombreProceso, BigInteger tamanio) {
        boolean compactoAntes = false;

        // Intento 1: First Fit sin compactación
        ResultadoAsignacion resultado = intentarAsignarFirstFit(idProceso, nombreProceso, tamanio);
        if (resultado.direccion() != null) {
            return resultado;
        }

        // Intento 2: Compactar y reintentar si hay espacio libre total
        if (getEspacioLibreTotal().compareTo(tamanio) >= 0) {
            compactarInmediato();
            resultado = intentarAsignarFirstFit(idProceso, nombreProceso, tamanio);
            if (resultado.direccion() != null) {
                return new ResultadoAsignacion(resultado.direccion(), true);
            }
        }

        return new ResultadoAsignacion(null, false);
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

            return new ResultadoAsignacion(direccionAsignada, false);
        }

        return new ResultadoAsignacion(null, false);
    }

    /**
     * Compacta la memoria moviendo todos los procesos OCUPADOS al inicio.
     * <p>
     * Este método solo se invoca cuando falla una asignación. Se reasignan
     * todos los IDs de partición para mantener el contador global.
     */
    private void compactarInmediato() {
        // Extrae procesos ocupados en orden físico
        List<Particion> ocupadas = particiones.stream()
            .filter(p -> !p.estaLibre())
            .sorted(Comparator.comparing(Particion::getDireccionInicio))
            .toList();

        // Reconstruye lista: ocupadas contiguamente, luego un hueco libre
        List<Particion> nuevaLista = new ArrayList<>();
        BigInteger cursor = BigInteger.ZERO;

        for (Particion ocupada : ocupadas) {
            Particion reubicada = new Particion(
                contadorId++,
                cursor,
                ocupada.getTamanio(),
                ocupada.getIdProceso(),
                ocupada.getNombreProceso()
            );
            nuevaLista.add(reubicada);
            cursor = cursor.add(ocupada.getTamanio());
        }

        // Crear hueco libre al final si hay espacio
        if (cursor.compareTo(tamanioTotal) < 0) {
            BigInteger tamanioLibre = tamanioTotal.subtract(cursor);
            Particion hueco = new Particion(contadorId++, cursor, tamanioLibre);
            nuevaLista.add(hueco);
        }

        // Reemplazar lista
        particiones.clear();
        particiones.addAll(nuevaLista);
    }

    /**
     * Libera un proceso y compacta inmediatamente la memoria.
     * <p>
     * Este es el método más complejo: realiza movimientos de procesos para
     * tapar huecos, registra todos los movimientos con sus IDs reasignados,
     * y condensa huecos adyacentes.
     *
     * @param idProceso identificador del proceso a liberar.
     * @return {@code EventosLiberacion} con movimientos y condensación, o null si no existe.
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
        EventoCondensacion condensacion = null;

        // Paso 2: Marcar como LIBRE con nuevo ID
        Particion hueco = new Particion(contadorId++, ocupada.getDireccionInicio(), ocupada.getTamanio());
        particiones.set(indiceOcupada, hueco);

        // Paso 3: Buscar procesos abajo del hueco
        List<Integer> indicesAbajo = new ArrayList<>();
        for (int i = indiceOcupada + 1; i < particiones.size(); i++) {
            Particion p = particiones.get(i);
            if (!p.estaLibre()) {
                indicesAbajo.add(i);
            }
        }

        // Paso 4: Mover procesos hacia arriba
        if (!indicesAbajo.isEmpty()) {
            BigInteger dirHuecoActual = hueco.getDireccionInicio();

            for (int indiceAbajoIdx = 0; indiceAbajoIdx < indicesAbajo.size(); indiceAbajoIdx++) {
                int indiceAParamover = indicesAbajo.get(indiceAbajoIdx);
                Particion aMover = particiones.get(indiceAParamover);

                // Registrar movimiento
                BigInteger dirAnterior = aMover.getDireccionInicio();
                movimientos.add(new EventoMovimiento(
                    aMover.getNombre(),
                    "PAR" + contadorId,
                    aMover.getNombreProceso(),
                    dirAnterior,
                    dirHuecoActual,
                    aMover.getTamanio()
                ));

                // Crear nueva partición ocupada con nuevo ID
                Particion movida = new Particion(
                    contadorId++,
                    dirHuecoActual,
                    aMover.getTamanio(),
                    aMover.getIdProceso(),
                    aMover.getNombreProceso()
                );

                // El hueco se desplaza a donde estaba la partición que se movió
                hueco = new Particion(
                    contadorId++,
                    dirAnterior,
                    aMover.getTamanio()
                );

                // Actualizar lista
                particiones.set(indiceAParamover, movida);
                particiones.set(indiceOcupada, hueco);

                dirHuecoActual = aMover.getDireccionInicio();
                indiceOcupada = indiceAParamover;
            }
        }

        // Paso 5: Intentar condensar si hay otro hueco adyacente abajo
        if (indiceOcupada + 1 < particiones.size()) {
            Particion siguiente = particiones.get(indiceOcupada + 1);
            if (siguiente.estaLibre()) {
                // Crear nuevo hueco condensado
                String nombrePart1 = hueco.getNombre();
                String nombrePart2 = siguiente.getNombre();
                BigInteger tamanioResultante = hueco.getTamanio().add(siguiente.getTamanio());

                Particion condensada = new Particion(contadorId++, hueco.getDireccionInicio(), tamanioResultante);
                particiones.set(indiceOcupada, condensada);
                particiones.remove(indiceOcupada + 1);

                condensacion = new EventoCondensacion(
                    nombrePart1,
                    nombrePart2,
                    condensada.getNombre(),
                    tamanioResultante
                );
            }
        }

        return new EventosLiberacion(movimientos, condensacion);
    }

    /**
     * Libera un proceso sin hacer shifting ni condensación.
     * <p>
     * Usado cuando expira el quantum (Round Robin) y el proceso vuelve a la cola.
     * Solo marca la partición como LIBRE con un nuevo ID.
     *
     * @param idProceso identificador del proceso a liberar.
     * @return {@code true} si el proceso fue encontrado y liberado, {@code false} en caso contrario.
     */
    public boolean liberarSinDesplazar(int idProceso) {
        for (int i = 0; i < particiones.size(); i++) {
            Particion p = particiones.get(i);
            if (!p.estaLibre() && p.getIdProceso() == idProceso) {
                // Crear nueva partición LIBRE con nuevo ID
                Particion libre = new Particion(contadorId++, p.getDireccionInicio(), p.getTamanio());
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
