package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

/**
 * Motor de simulacion Round Robin con flujo secuencial determinista.
 */
public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(1000L);

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, MemoriaVariable memoria) {
        RegistroSimulacion registro = new RegistroSimulacion();

        BigInteger tamanioTotalMemoria = memoria.getTamanioTotal();

        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> Integer.compare(a.id, b.id))
            .toList();

        int maxIteraciones = procesosOrdenados.size() * 1000;
        int iteracionesGlobales = 0;
        int contadorParticion = 1;
        Map<Integer, String> nombreParticionPorProceso = new HashMap<>();
        Map<BigInteger, String> nombrePorDireccion = new HashMap<>();
        Map<String, String> nombreFinal = new HashMap<>();

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

                BigInteger direccionInicio = memoria.asignar(actual.id, actual.nombre, actual.tamanioMemoria);
                if (direccionInicio == null) {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                    continue;
                }

                if (!nombreParticionPorProceso.containsKey(actual.id)) {
                    nombreParticionPorProceso.put(actual.id, "PAR" + contadorParticion++);
                }
                actual.referenciaMemoria = nombreParticionPorProceso.get(actual.id);
                nombrePorDireccion.put(direccionInicio, actual.referenciaMemoria);

                BigInteger direccionMinimaFusion = direccionInicio;
                List<String> nombresAdyacentes = new ArrayList<>();
                nombresAdyacentes.add(actual.referenciaMemoria);
                List<BigInteger> direccionesFusionadas = new ArrayList<>();
                for (var hueco : memoria.getHuecos()) {
                    boolean adyacenteIzquierda = hueco.getDireccionFin().equals(direccionInicio);
                    boolean adyacenteDerecha = hueco.getDireccionInicio().equals(direccionInicio.add(actual.tamanioMemoria));
                    if (adyacenteIzquierda || adyacenteDerecha) {
                        direccionesFusionadas.add(hueco.getDireccionInicio());
                        nombresAdyacentes.add(nombrePorDireccion.getOrDefault(
                            hueco.getDireccionInicio(),
                            "H[" + hueco.getDireccionInicio() + "]"
                        ));
                        if (hueco.getDireccionInicio().compareTo(direccionMinimaFusion) < 0) {
                            direccionMinimaFusion = hueco.getDireccionInicio();
                        }
                    }
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
                        String particionAlTerminar = nombreFinal.getOrDefault(
                            actual.referenciaMemoria,
                            actual.referenciaMemoria
                        );
                        Proceso snapshotFinal = new Proceso(
                            actual.id,
                            actual.nombre,
                            BigInteger.ZERO,
                            actual.tamanioMemoria
                        );
                        snapshotFinal.setTiempoRestante(BigInteger.ZERO);
                        snapshotFinal.setEstadoActual(RegistroSimulacion.FINALIZADO);
                        snapshotFinal.setParticion(new Particion(-1, particionAlTerminar, actual.tamanioMemoria));
                        registro.registrar(RegistroSimulacion.FINALIZADO, snapshotFinal);
                        termino = true;
                    } else {
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                    }

                } finally {
                    if (termino) {
                        BigInteger dirProceso = obtenerDireccionProceso(memoria, actual.id);
                        BigInteger dirFinProceso = dirProceso != null
                            ? dirProceso.add(actual.tamanioMemoria)
                            : null;

                        List<String> nombresAFusionar = new ArrayList<>();
                        nombresAFusionar.add(actual.referenciaMemoria);

                        for (var hueco : memoria.getHuecos()) {
                            boolean antesDelProceso = dirProceso != null && hueco.getDireccionFin().equals(dirProceso);
                            boolean despuesDelProceso = dirFinProceso != null && hueco.getDireccionInicio().equals(dirFinProceso);
                            if (antesDelProceso || despuesDelProceso) {
                                String nombreHueco = nombrePorDireccion.getOrDefault(hueco.getDireccionInicio(), null);
                                if (nombreHueco != null && !nombresAFusionar.contains(nombreHueco)) {
                                    nombresAFusionar.add(nombreHueco);
                                }
                            }
                        }

                        boolean huboCondensacion = memoria.liberar(actual.id);

                        registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                            actual.nombre, null, actual.tamanioMemoria,
                            "Proceso '" + actual.nombre + "' terminó. Partición "
                                + actual.referenciaMemoria + " libre", memoria);

                        if (huboCondensacion && nombresAFusionar.size() > 1) {
                            String particionResultante = "PAR" + contadorParticion++;
                            String condensadas = String.join("+", nombresAFusionar);
                            BigInteger tamanioResultante = memoria.getEspacioLibreTotal();

                            registro.registrarCondensacion(new RegistroSimulacion.SnapshotCondensacion(
                                particionResultante,
                                condensadas,
                                tamanioResultante
                            ));

                            for (String nombre : nombresAFusionar) {
                                nombreFinal.put(nombre, particionResultante);
                            }

                            nombrePorDireccion.remove(dirProceso);
                            for (BigInteger inicioHueco : direccionesFusionadas) {
                                nombrePorDireccion.remove(inicioHueco);
                            }
                            nombrePorDireccion.put(direccionMinimaFusion, particionResultante);

                            registro.actualizarParticionesFinalizadas(nombreFinal);

                            registrarEventoMemoria(registro, RegistroSimulacion.CONDENSACION,
                                actual.nombre, null, null,
                                "Condensación: " + condensadas + " → " + particionResultante, memoria);
                        } else if (dirProceso != null) {
                            nombrePorDireccion.put(dirProceso, actual.referenciaMemoria);
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

        registro.copiarEstado(
            RegistroSimulacion.FINALIZADO,
            RegistroSimulacion.FINALIZACION_PARTICIONES
        );

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

    private BigInteger obtenerDireccionProceso(MemoriaVariable memoria, int idProceso) {
        return memoria.getBloquesOcupados().stream()
            .filter(b -> b.getIdProceso() == idProceso)
            .map(b -> b.getDireccionInicio())
            .findFirst()
            .orElse(null);
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