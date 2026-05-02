package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.edu.uptc.processes1.model.HuecoMemoria;
import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

/**
 * Motor de simulacion Round Robin con flujo secuencial determinista.
 */
public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(1000L);

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, MemoriaVariable memoria) {
        Set<String> particionesYaRegistradas = new HashSet<>();
        RegistroSimulacion registro = new RegistroSimulacion();

        BigInteger tamanioTotalMemoria = memoria.getTamanioTotal();

        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> Integer.compare(a.id, b.id))
            .toList();

        int maxIteraciones = procesosOrdenados.size() * 1000;
        int iteracionesGlobales = 0;
        int[] contadorParticion = {1};
        Map<Integer, String> nombreParticionPorProceso = new HashMap<>();
        Map<BigInteger, String> nombrePorDireccion = new HashMap<>();
        Map<String, String> nombreFinal = new HashMap<>();
        List<String> slotsParticion = new ArrayList<>();
        Set<String> slotsLibres = new HashSet<>();
        Map<String, BigInteger> tamaniosPorParticion = new HashMap<>();

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
            registrarEstado(registro, RegistroSimulacion.LISTO, runtime);
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

                BigInteger espacioLibreAntesDeAsignar = memoria.getEspacioLibreTotal();
                MemoriaVariable.ResultadoAsignacion resultado = memoria.asignar(actual.id, actual.nombre, actual.tamanioMemoria);
                if (resultado.direccion() == null) {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                    continue;
                }

                BigInteger direccionInicio = resultado.direccion();

                // Si ocurrió compactación antes de la asignación, registrarlo
                if (resultado.compactoAntes()) {
                    registrarEventoMemoria(registro, RegistroSimulacion.COMPACTACION,
                        actual.nombre,
                        BigInteger.ZERO,
                        espacioLibreAntesDeAsignar,
                        "Compactación ejecutada: " + espacioLibreAntesDeAsignar
                            + " bytes libres consolidados para alojar '" + actual.nombre + "'",
                        memoria);
                }

                if (!nombreParticionPorProceso.containsKey(actual.id)) {
                    String nombreParticion = "PAR" + contadorParticion[0]++;
                    nombreParticionPorProceso.put(actual.id, nombreParticion);
                    slotsParticion.add(nombreParticion);
                    tamaniosPorParticion.put(nombreParticion, actual.tamanioMemoria);
                }
                actual.referenciaMemoria = nombreParticionPorProceso.get(actual.id);
                nombrePorDireccion.put(direccionInicio, actual.referenciaMemoria);
                // Solo registrar la partición si no ha sido registrada antes
                if (!particionesYaRegistradas.contains(actual.referenciaMemoria)) {
                    registro.registrarParticion(new RegistroSimulacion.SnapshotParticion(
                        actual.referenciaMemoria,
                        "Asignada a proceso '" + actual.nombre + "'",
                        actual.tamanioMemoria
                    ));
                    particionesYaRegistradas.add(actual.referenciaMemoria);
                }

                // ── NUEVO: registrar asignación ──────────────────────────────────────────
                registrarEventoMemoria(registro, RegistroSimulacion.ASIGNACION,
                    actual.nombre, direccionInicio, actual.tamanioMemoria,
                    "Proceso '" + actual.nombre + "' asignado en dir=" + direccionInicio
                    + ", tamaño=" + actual.tamanioMemoria, memoria);

                boolean termino = false;

                try {
                    registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                    BigInteger rafaga = QUANTUM.min(actual.tiempoRestante);
                    BigInteger tiempoTrasRafaga = actual.tiempoRestante.subtract(rafaga).max(BigInteger.ZERO);

                    registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);

                    registro.registrarUsoParticion(actual.id, actual.nombre, actual.referenciaMemoria, rafaga);

                    actual.tiempoRestante = tiempoTrasRafaga;

                    if (actual.tiempoRestante.compareTo(BigInteger.ZERO) <= 0) {
                        Proceso snapshotFinal = new Proceso(
                            actual.id,
                            actual.nombre,
                            BigInteger.ZERO,
                            actual.tamanioMemoria
                        );
                        snapshotFinal.setTiempoRestante(BigInteger.ZERO);
                        snapshotFinal.setEstadoActual(RegistroSimulacion.FINALIZADO);
                        snapshotFinal.setParticion(new Particion(-1, actual.referenciaMemoria, actual.tamanioMemoria));
                        registro.registrar(RegistroSimulacion.FINALIZADO, snapshotFinal);
                        termino = true;
                    } else {
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                    }

                } finally {
                    if (termino) {
                        String nombreParticionLiberada = actual.referenciaMemoria;
                        boolean esUltimoProceso = colaListos.size() == 1;

                        memoria.liberar(actual.id);

                        registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                            actual.nombre, null, actual.tamanioMemoria,
                            "Proceso '" + actual.nombre + "' terminó. Partición "
                                + nombreParticionLiberada + " libre", memoria);

                        if (!esUltimoProceso) {
                            intentarCondensar(
                                nombreParticionLiberada,
                                slotsParticion,
                                slotsLibres,
                                tamaniosPorParticion,
                                registro,
                                memoria,
                                contadorParticion,
                                false
                            );
                        }
                    } else {
                        memoria.liberarSinCondensar(actual.id);
                        if (direccionInicio != null) {
                            nombrePorDireccion.put(direccionInicio, actual.referenciaMemoria);
                        }
                        registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                            actual.nombre, null, actual.tamanioMemoria,
                            "Proceso '" + actual.nombre + "' expiró quantum, liberó temporalmente", memoria);
                    }

                    actual.referenciaMemoria = null;
                }

                if (termino) {
                    colaListos.remove(i);
                } else {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                }
            }

            if (limiteSuperado) {
                break;
            }
        }

        return registro;
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
            snapshot.setParticion(new Particion(-1, nombreParticion, runtime.tamanioMemoria));
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
        return memoria.getHuecos().stream()
            .map(hueco -> hueco.getTamanio())
            .max(BigInteger::compareTo)
            .orElse(BigInteger.ZERO);
    }

    private void intentarCondensar(
        String nombreParticionLiberada,
        List<String> slotsParticion,
        Set<String> slotsLibres,
        Map<String, BigInteger> tamaniosPorParticion,
        RegistroSimulacion registro,
        MemoriaVariable memoria,
        int[] contadorParticion,
        boolean esUltimoProceso) {

        if (esUltimoProceso) {
            return;
        }

        if (nombreParticionLiberada == null || nombreParticionLiberada.isBlank()) {
            return;
        }

        slotsLibres.add(nombreParticionLiberada);

        int posicion = slotsParticion.indexOf(nombreParticionLiberada);
        if (posicion < 0) {
            return;
        }

        String particionIzquierda = null;
        String particionDerecha = null;

        if (posicion > 0) {
            String anterior = slotsParticion.get(posicion - 1);
            if (slotsLibres.contains(anterior)) {
                particionIzquierda = anterior;
                particionDerecha = nombreParticionLiberada;
            }
        }

        if (particionIzquierda == null && posicion < slotsParticion.size() - 1) {
            String siguiente = slotsParticion.get(posicion + 1);
            if (slotsLibres.contains(siguiente)) {
                particionIzquierda = nombreParticionLiberada;
                particionDerecha = siguiente;
            }
        }

        if (particionIzquierda == null || particionDerecha == null) {
            return;
        }

        BigInteger tamanioIzquierda = tamaniosPorParticion.getOrDefault(particionIzquierda, BigInteger.ZERO);
        BigInteger tamanioDerecha = tamaniosPorParticion.getOrDefault(particionDerecha, BigInteger.ZERO);
        BigInteger tamanioResultante = tamanioIzquierda.add(tamanioDerecha);
        String nombreNuevo = "PAR" + contadorParticion[0]++;
        String nombresUnidosTexto = particionIzquierda + "+" + particionDerecha;

        registro.registrarParticion(new RegistroSimulacion.SnapshotParticion(
            nombreNuevo,
            "Condensación de " + nombresUnidosTexto,
            tamanioResultante
        ));
        registro.registrarCondensacion(new RegistroSimulacion.SnapshotCondensacion(
            nombreNuevo,
            nombresUnidosTexto,
            tamanioResultante
        ));
        registrarEventoMemoria(registro, RegistroSimulacion.CONDENSACION,
            nombreNuevo, null, tamanioResultante,
            "Condensación: " + nombresUnidosTexto + " → " + nombreNuevo, memoria);

        int indiceIzquierda = slotsParticion.indexOf(particionIzquierda);
        int indiceDerecha = slotsParticion.indexOf(particionDerecha);
        int indiceReemplazo = Math.min(indiceIzquierda, indiceDerecha);
        int indiceEliminacion = Math.max(indiceIzquierda, indiceDerecha);

        slotsParticion.set(indiceReemplazo, nombreNuevo);
        slotsParticion.remove(indiceEliminacion);

        slotsLibres.remove(particionIzquierda);
        slotsLibres.remove(particionDerecha);
        slotsLibres.add(nombreNuevo);

        tamaniosPorParticion.put(nombreNuevo, tamanioResultante);

        if (!esUltimoProceso) {
            intentarCondensar(
                nombreNuevo,
                slotsParticion,
                slotsLibres,
                tamaniosPorParticion,
                registro,
                memoria,
                contadorParticion,
                false
            );
        }
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

        List<String> huecos = memoria.getHuecos().stream()
            .map(h -> "Hueco[" + h.getDireccionInicio() + " - " + h.getDireccionFin()
                      + ", tam=" + h.getTamanio() + "]")
            .toList();

        List<String> bloques = memoria.getBloquesOcupados().stream()
            .map(b -> b.getNombreProceso() + "[" + b.getDireccionInicio()
                      + " - " + b.getDireccionFin() + ", tam=" + b.getTamanio() + "]")
            .toList();

        registro.registrarMemoria(tipoEvento,
            new RegistroSimulacion.SnapshotMemoria(
                tipoEvento, nombreProceso, direccion, tamanio, detalle, huecos, bloques
            )
        );
    }

}