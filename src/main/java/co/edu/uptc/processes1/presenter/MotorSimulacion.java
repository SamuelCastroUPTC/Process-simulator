package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

/**
 * Motor de simulación Round Robin con flujo secuencial determinista.
 * <p>
 * Delega toda la lógica de compactación y condensación a MemoriaVariable.
 * El motor solo orquesta la simulación y registra los eventos resultantes.
 */
public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(1000L);

    /**
     * Ejecuta la simulación Round Robin sobre el conjunto de procesos.
     *
     * @param procesosIniciales lista inicial de procesos.
     * @param memoria instancia de MemoriaVariable que gestiona asignación y liberación.
     * @return registro de simulación con todos los eventos y estados.
     */
    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, MemoriaVariable memoria) {
        RegistroSimulacion registro = new RegistroSimulacion();

        BigInteger tamanioTotalMemoria = memoria.getTamanioTotal();

        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> Integer.compare(a.id, b.id))
            .toList();

        int maxIteraciones = procesosOrdenados.size() * 1000;
        int iteracionesGlobales = 0;

        List<ProcesoRuntime> colaListos = new ArrayList<>();

        for (ProcesoRuntime runtime : procesosOrdenados) {
            if (runtime.tamanioMemoria.compareTo(tamanioTotalMemoria) > 0) {
                BigInteger particionMasGrande = obtenerParticionMasGrandeDisponible(memoria);
                String motivo = "Su tamano de memoria (" + runtime.tamanioMemoria
                    + ") supera la particion mas grande disponible (" + particionMasGrande + ")";
                registrarEstadoNoEjecutado(registro, motivo, runtime);
            } else {
                colaListos.add(runtime);
            }
        }

        for (ProcesoRuntime runtime : colaListos) {
            registrarEstado(registro, RegistroSimulacion.INICIO, runtime);
        }

        while (!colaListos.isEmpty()) {
            boolean limiteSuperado = false;
            for (int i = 0; i < colaListos.size(); ) {
                iteracionesGlobales++;
                if (iteracionesGlobales > maxIteraciones) {
                    for (ProcesoRuntime restante : new ArrayList<>(colaListos)) {
                        registrarEstadoNoEjecutado(registro,
                            "La simulación superó el límite de iteraciones seguras",
                            restante);
                    }
                    colaListos.clear();
                    limiteSuperado = true;
                    break;
                }

                ProcesoRuntime actual = colaListos.get(i);

                // Actualizar nombre de partición si el proceso se movió en el ciclo anterior
                actualizarNombreParticionDelProceso(actual, memoria);

                // Intenta asignar memoria al proceso
                MemoriaVariable.ResultadoAsignacion resultado = memoria.asignar(actual.id, actual.nombre, actual.tamanioMemoria);
                if (resultado.direccion() == null) {
                    // No hay espacio disponible, salta este proceso en este ciclo
                    registrarEstado(registro, RegistroSimulacion.INICIO, actual);
                    i++;
                    continue;
                }

                // Registra compactación si ocurrió
                if (resultado.compactoAntes()) {
                    registrarEventoCompactacion(registro, actual.nombre, memoria);
                }

                // Obtén nombre de partición actual del proceso en memoria
                actual.referenciaMemoria = obtenerNombreParticionDelProceso(actual.id, memoria);

                // Registra asignación
                registrarEventoMemoria(registro, RegistroSimulacion.ASIGNACION,
                    actual.nombre, resultado.direccion(), actual.tamanioMemoria,
                    "Proceso '" + actual.nombre + "' asignado en partición " +
                    actual.referenciaMemoria + " (dir=" + resultado.direccion() + ")", memoria);

                boolean termino = false;

                try {
                    registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                    BigInteger rafaga = QUANTUM.min(actual.tiempoRestante);
                    BigInteger tiempoTrasRafaga = actual.tiempoRestante.subtract(rafaga).max(BigInteger.ZERO);

                    registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);

                    registro.registrarUsoParticion(actual.id, actual.nombre, actual.referenciaMemoria, rafaga);

                    actual.tiempoRestante = tiempoTrasRafaga;

                    if (actual.tiempoRestante.compareTo(BigInteger.ZERO) <= 0) {
                        termino = true;

                        // Registrar la partición usada por este proceso
                        if (actual.referenciaMemoria != null) {
                            registro.registrarParticion(new RegistroSimulacion.SnapshotParticion(
                                actual.referenciaMemoria,
                                "Finalizada por proceso '" + actual.nombre + "'",
                                actual.tamanioMemoria
                            ));
                        }

                        // Registra finalización
                        Proceso snapshotFinal = new Proceso(
                            actual.id,
                            actual.nombre,
                            BigInteger.ZERO,
                            actual.tamanioMemoria
                        );
                        snapshotFinal.setTiempoRestante(BigInteger.ZERO);
                        snapshotFinal.setEstadoActual(RegistroSimulacion.FINALIZADO);
                        snapshotFinal.setParticion(
                            crearParticionSnapshot(actual.referenciaMemoria, actual.tamanioMemoria, actual.id, actual.nombre)
                        );
                        registro.registrar(RegistroSimulacion.FINALIZADO, snapshotFinal);
                    } else {
                        // Quantum expirado, el proceso continuará
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                    }

                } finally {
                    if (termino) {
                        // Libera memoria y procesa eventos de desplazamiento y condensación
                        MemoriaVariable.EventosLiberacion eventos = memoria.liberar(actual.id);

                        if (eventos != null) {

                            // 1. Registrar cada movimiento de desplazamiento (proceso que se desplazó)
                            for (MemoriaVariable.EventoMovimiento mov : eventos.movimientos()) {
                                registro.registrarDesplazamiento(
                                    mov.nombreProceso(),
                                    mov.particionAnterior(),
                                    mov.particionNueva(),
                                    mov.direccionAnterior(),
                                    mov.direccionNueva(),
                                    mov.tamanio()
                                );
                            }

                            // 2. Registrar condensación si ocurrió (fusión de huecos libres)
                            if (eventos.condensacion() != null) {
                                MemoriaVariable.EventoCondensacion cond = eventos.condensacion();

                                List<String> huecos = memoria.getParticionesLibres().stream()
                                    .map(h -> "Hueco[" + h.getDireccionInicio() + " - "
                                        + h.getDireccionFin() + ", tam=" + h.getTamanio() + "]")
                                    .toList();
                                List<String> bloques = memoria.getParticionesOcupadas().stream()
                                    .map(b -> b.getNombreProceso() + "[" + b.getDireccionInicio()
                                        + " - " + b.getDireccionFin() + ", tam=" + b.getTamanio() + "]")
                                    .toList();

                                registro.registrarMemoria(RegistroSimulacion.CONDENSACION,
                                    new RegistroSimulacion.SnapshotMemoria(
                                        RegistroSimulacion.CONDENSACION,
                                        cond.particionResultante(),
                                        cond.tamanioResultante() != null
                                            ? memoria.getParticionesLibres().stream()
                                                .filter(p -> p.getNombre().equals(cond.particionResultante()))
                                                .map(p -> p.getDireccionInicio())
                                                .findFirst().orElse(null)
                                            : null,
                                        cond.tamanioResultante(),
                                        "Condensación: " + cond.particion1() + " + "
                                            + cond.particion2() + " → " + cond.particionResultante(),
                                        null,
                                        huecos,
                                        bloques
                                    )
                                );

                                registro.registrarParticion(new RegistroSimulacion.SnapshotParticion(
                                    cond.particionResultante(),
                                    "Resultado de condensación de "
                                        + cond.particion1() + " + " + cond.particion2(),
                                    cond.tamanioResultante()
                                ));
                            }

                            // 3. Registrar evento de liberación general
                            registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                                actual.nombre, null, actual.tamanioMemoria,
                                "Proceso '" + actual.nombre
                                    + "' terminó y fue liberado con desplazamiento.",
                                memoria);
                        }
                    } else {
                        // Quantum expirado: libera sin desplazar
                        memoria.liberarSinDesplazar(actual.id);
                        registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                            actual.nombre, null, actual.tamanioMemoria,
                            "Proceso '" + actual.nombre + "' expiró quantum, liberó temporalmente (sin desplazamiento)",
                            memoria);
                    }

                    actual.referenciaMemoria = null;
                }

                if (termino) {
                    colaListos.remove(i);
                } else {
                    registrarEstado(registro, RegistroSimulacion.INICIO, actual);
                    i++;
                }
            }

            if (limiteSuperado) {
                break;
            }
        }

        return registro;
    }

    /**
     * Actualiza el nombre de partición de un proceso consultando MemoriaVariable.
     */
    private void actualizarNombreParticionDelProceso(ProcesoRuntime actual, MemoriaVariable memoria) {
        actual.referenciaMemoria = obtenerNombreParticionDelProceso(actual.id, memoria);
    }

    /**
     * Obtiene el nombre de partición en el que reside un proceso.
     */
    private String obtenerNombreParticionDelProceso(int idProceso, MemoriaVariable memoria) {
        for (Particion p : memoria.getParticionesOcupadas()) {
            if (p.getIdProceso() == idProceso) {
                return p.getNombre();
            }
        }
        return null;
    }

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime) {
        registrarEstado(registro, estado, runtime, runtime.tiempoRestante, null);
    }

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime, BigInteger tiempoSnapshot) {
        registrarEstado(registro, estado, runtime, tiempoSnapshot, null);
    }

    private void registrarEstado(
        RegistroSimulacion registro,
        String estado,
        ProcesoRuntime runtime,
        BigInteger tiempoSnapshot,
        String nombreParticionOverride) {
        Proceso snapshot = new Proceso(
            runtime.id,
            runtime.nombre,
            runtime.tiempoRestante,
            runtime.tamanioMemoria
        );
        snapshot.setTiempoRestante(tiempoSnapshot);
        snapshot.setEstadoActual(estado);
        String nombreParticion = nombreParticionOverride != null ? nombreParticionOverride : runtime.referenciaMemoria;
        if (nombreParticion != null) {
            snapshot.setParticion(crearParticionSnapshot(nombreParticion, runtime.tamanioMemoria, runtime.id, runtime.nombre));
        } else {
            snapshot.setParticion(null);
        }
        registro.registrar(estado, snapshot);
    }

    private void registrarEstadoNoEjecutado(RegistroSimulacion registro, String motivo, ProcesoRuntime runtime) {
        Proceso snapshot = new Proceso(
            runtime.id,
            runtime.nombre,
            runtime.tiempoRestante,
            runtime.tamanioMemoria
        );
        snapshot.setEstadoActual(RegistroSimulacion.NO_EJECUTADO);
        snapshot.setParticion(null);
        registro.registrarNoEjecutado(snapshot, motivo);
    }

    private BigInteger obtenerParticionMasGrandeDisponible(MemoriaVariable memoria) {
        return memoria.getParticionesLibres().stream()
            .map(Particion::getTamanio)
            .max(BigInteger::compareTo)
            .orElse(BigInteger.ZERO);
    }

    /**
     * Registra un evento de compactación en el registro de simulación.
     */
    private void registrarEventoCompactacion(
        RegistroSimulacion registro,
        String nombreProcesoDesencadenador,
        MemoriaVariable memoria) {

        List<String> huecos = memoria.getParticionesLibres().stream()
            .map(h -> "Hueco[" + h.getDireccionInicio() + " - " + h.getDireccionFin()
                      + ", tam=" + h.getTamanio() + "]")
            .toList();

        List<String> bloques = memoria.getParticionesOcupadas().stream()
            .map(b -> b.getNombreProceso() + "[" + b.getDireccionInicio()
                      + " - " + b.getDireccionFin() + ", tam=" + b.getTamanio() + "]")
            .toList();

        String detalle = "Compactación ejecutada para alojar '" + nombreProcesoDesencadenador +
            "'. Estado tras compactación: " +
            (bloques.isEmpty() ? "sin procesos" : bloques.size() + " procesos");

        registro.registrarMemoria(RegistroSimulacion.COMPACTACION,
            new RegistroSimulacion.SnapshotMemoria(
                RegistroSimulacion.COMPACTACION,
                nombreProcesoDesencadenador,
                null,
                memoria.getEspacioOcupado(),
                detalle,
                null,
                huecos,
                bloques
            )
        );
    }

    private static final class ProcesoRuntime {
        private final int id;
        private final String nombre;
        private final BigInteger tamanioMemoria;
        private BigInteger tiempoRestante;
        private String referenciaMemoria;

        private ProcesoRuntime(
            int id,
            String nombre,
            BigInteger tiempoRestante,
            BigInteger tamanioMemoria
        ) {
            this.id = id;
            this.nombre = nombre;
            this.tiempoRestante = tiempoRestante;
            this.tamanioMemoria = tamanioMemoria;
            this.referenciaMemoria = null;
        }

        private static ProcesoRuntime desde(Proceso p) {
            return new ProcesoRuntime(
                p.getId(),
                p.getNombre(),
                p.getTiempoRestante(),
                p.getTamanioMemoria()
            );
        }
    }

    private void registrarEventoMemoria(
        RegistroSimulacion registro,
        String tipoEvento,
        String nombreProceso,
        BigInteger direccion,
        BigInteger tamanio,
        String detalle,
        MemoriaVariable memoria) {

        List<String> huecos = memoria.getParticionesLibres().stream()
            .map(h -> "Hueco[" + h.getDireccionInicio() + " - " + h.getDireccionFin()
                      + ", tam=" + h.getTamanio() + "]")
            .toList();

        List<String> bloques = memoria.getParticionesOcupadas().stream()
            .map(b -> b.getNombreProceso() + "[" + b.getDireccionInicio()
                      + " - " + b.getDireccionFin() + ", tam=" + b.getTamanio() + "]")
            .toList();

        registro.registrarMemoria(tipoEvento,
            new RegistroSimulacion.SnapshotMemoria(
                tipoEvento, nombreProceso, direccion, tamanio, detalle, null, huecos, bloques
            )
        );
    }

    private Particion crearParticionSnapshot(String nombreParticion, BigInteger tamanio, int idProceso, String nombreProceso) {
        if (nombreParticion == null || nombreParticion.isBlank()) {
            return null;
        }

        int idParticion = extraerIdParticion(nombreParticion);
        return new Particion(idParticion, BigInteger.ZERO, tamanio, idProceso, nombreProceso);
    }

    private int extraerIdParticion(String nombreParticion) {
        String digitos = nombreParticion.replaceAll("\\D+", "");
        if (digitos.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digitos);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}