package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            java.util.Set<String> particionesYaRegistradas = new java.util.HashSet<>();
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
                        List<HuecoMemoria> huecosPrevios = new ArrayList<>(memoria.getHuecos());
                        BigInteger dirProceso = obtenerDireccionProceso(memoria, actual.id);
                        BigInteger finProceso = dirProceso != null
                            ? dirProceso.add(actual.tamanioMemoria)
                            : null;

                        boolean huboCondensacion = memoria.liberar(actual.id);
                        List<HuecoMemoria> huecosNuevos = new ArrayList<>(memoria.getHuecos());

                        registrarEventoMemoria(registro, RegistroSimulacion.LIBERACION,
                            actual.nombre, null, actual.tamanioMemoria,
                            "Proceso '" + actual.nombre + "' terminó. Partición "
                                + actual.referenciaMemoria + " libre", memoria);

                        boolean huboFusiones = huboCondensacion && huecosNuevos.size() < huecosPrevios.size() + 1;
                        if (huboFusiones && dirProceso != null) {
                            List<HuecoMemoria> huecosFusionados = new ArrayList<>();
                            List<String> nombresUnidos = new ArrayList<>();
                            nombresUnidos.add(actual.referenciaMemoria);

                            BigInteger direccionResultado = dirProceso;
                            int contadorHuecosConNombre = 0;

                            for (HuecoMemoria hueco : huecosPrevios) {
                                boolean adyacenteIzquierda = hueco.getDireccionFin().equals(dirProceso);
                                boolean adyacenteDerecha = finProceso != null && hueco.getDireccionInicio().equals(finProceso);
                                if (adyacenteIzquierda || adyacenteDerecha) {
                                    huecosFusionados.add(hueco);
                                    String nombreHueco = nombrePorDireccion.get(hueco.getDireccionInicio());
                                    if (nombreHueco != null && !nombreHueco.isBlank()) {
                                        contadorHuecosConNombre++;
                                        if (!nombresUnidos.contains(nombreHueco)) {
                                        nombresUnidos.add(nombreHueco);
                                        }
                                    }
                                    if (hueco.getDireccionInicio().compareTo(direccionResultado) < 0) {
                                        direccionResultado = hueco.getDireccionInicio();
                                    }
                                }
                            }

                            // Solo registrar condensación si hay al menos un hueco adyacente con nombre PARx
                            if (contadorHuecosConNombre > 0) {
                                String nombreNuevo = "PAR" + contadorParticion++;
                                BigInteger direccionFinalFusion = direccionResultado;
                                BigInteger tamanioResultante = actual.tamanioMemoria;
                                for (HuecoMemoria huecoFusionado : huecosFusionados) {
                                    tamanioResultante = tamanioResultante.add(huecoFusionado.getTamanio());
                                }
                                String nombresUnidosTexto = String.join("+", nombresUnidos);

                                registro.registrarCondensacion(new RegistroSimulacion.SnapshotCondensacion(
                                    nombreNuevo,
                                    nombresUnidosTexto,
                                    tamanioResultante
                                ));
                                registro.registrarParticion(new RegistroSimulacion.SnapshotParticion(
                                    nombreNuevo,
                                    "Condensación de " + nombresUnidosTexto,
                                    tamanioResultante
                                ));

                                for (String nombre : nombresUnidos) {
                                    nombreFinal.put(nombre, nombreNuevo);
                                }

                                nombrePorDireccion.remove(dirProceso);
                                for (HuecoMemoria huecoFusionado : huecosFusionados) {
                                    nombrePorDireccion.remove(huecoFusionado.getDireccionInicio());
                                }

                                nombrePorDireccion.put(direccionFinalFusion, nombreNuevo);

                                registrarEventoMemoria(registro, RegistroSimulacion.CONDENSACION,
                                    nombreNuevo, null, null,
                                    "Condensación: " + nombresUnidosTexto + " → " + nombreNuevo, memoria);
                            } else {
                                // Sin condensación real, solo limpiar nombrePorDireccion
                                nombrePorDireccion.remove(dirProceso);
                                for (HuecoMemoria huecoFusionado : huecosFusionados) {
                                    nombrePorDireccion.remove(huecoFusionado.getDireccionInicio());
                                }
                            }
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