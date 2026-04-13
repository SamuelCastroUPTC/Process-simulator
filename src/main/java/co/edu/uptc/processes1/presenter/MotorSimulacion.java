package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de simulacion Round Robin con flujo secuencial determinista.
 *
 * No usa listas paralelas de bloqueo/suspension: solo registra transiciones
 * de historial y reencola el proceso cuando aplique.
 */
public class MotorSimulacion {

    private static final long QUANTUM = 5000L;

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, List<Particion> particiones) {
        int[] ultimaParticion = {0};
        RegistroSimulacion registro = new RegistroSimulacion();

        // Calcular tamaño de partición más grande
        long particionMasGrande = particiones == null || particiones.isEmpty()
            ? 0L
            : particiones.stream().mapToLong(Particion::getTamanoTotal).max().orElse(0L);

        // === INICIALIZACIÓN ===
        // 1. Ordenar procesos por tiempo de ejecución ascendente
        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> Long.compare(a.tiempoRestante, b.tiempoRestante))
            .toList();

        List<ProcesoRuntime> colaListos = new ArrayList<>();

        // 2. Marcar como NO_EJECUTADO los que superan partición más grande
        for (ProcesoRuntime runtime : procesosOrdenados) {
            if (runtime.tamanioMemoria > particionMasGrande) {
                registrarEstado(registro, RegistroSimulacion.NO_EJECUTADO, runtime);
            } else {
                colaListos.add(runtime);
            }
        }

        // === CICLO PRINCIPAL ===
        while (!colaListos.isEmpty()) {
            List<ProcesoRuntime> pendientes = new ArrayList<>();
            List<ProcesoRuntime> siguienteCiclo = new ArrayList<>();
            List<Particion> particionesALiberar = new ArrayList<>();

            // Procesar todos los procesos actualmente en colaListos en orden
            for (ProcesoRuntime actual : colaListos) {
                // Intentar asignar partición con Next Fit
                Particion particionAsignada = buscarParticionNextFit(particiones, actual.tamanioMemoria, ultimaParticion);

                if (particionAsignada == null) {
                    // Si no se encuentra: reencolar en pendientes
                    pendientes.add(actual);
                    continue;
                }

                // Si se encuentra partición:
                particionAsignada.ocupar();
                actual.particion = particionAsignada;

                try {
                    // a. Registrar snapshot INICIO (Listo) con partición asignada
                    registrarEstado(registro, RegistroSimulacion.INICIO, actual);

                    // b. Registrar snapshot DESPACHAR
                    registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                    // c. Calcular ráfaga = min(QUANTUM, tiempoRestante)
                    long rafaga = Math.min(QUANTUM, actual.tiempoRestante);
                    long tiempoTrasRafaga = Math.max(0L, actual.tiempoRestante - rafaga);

                    // d. Registrar snapshot PROCESADOR con tiempoRestante - rafaga como tiempo snapshot
                    registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);

                    // e. Llamar registro.registrarUsoParticion(..., rafaga)
                    registro.registrarUsoParticion(
                        actual.id,
                        actual.nombre,
                        particionAsignada.getNombre(),
                        rafaga
                    );

                    // f. Actualizar actual.tiempoRestante -= rafaga
                    actual.tiempoRestante = tiempoTrasRafaga;

                    // g. Si tiempoRestante <= 0: registrar FINALIZADO y no reencolar
                    if (actual.tiempoRestante <= 0L) {
                        registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual, 0L);
                    } else if (actual.pasaPorBloqueado) {
                        // h. Si tiempoRestante > 0 y pasaPorBloqueado: registrar BLOQUEAR, BLOQUEADO, DESPERTAR, reencolar
                        registrarEstado(registro, RegistroSimulacion.BLOQUEAR, actual);
                        registrarEstado(registro, RegistroSimulacion.BLOQUEADO, actual);
                        registrarEstado(registro, RegistroSimulacion.DESPERTAR, actual);
                        siguienteCiclo.add(actual);
                    } else {
                        // i. Si tiempoRestante > 0 y no pasaPorBloqueado: registrar EXPIRACION_TIEMPO, reencolar
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                        siguienteCiclo.add(actual);
                    }

                } finally {
                    if (actual.tiempoRestante <= 0L) {
                        // Terminó: liberar inmediatamente
                        particionAsignada.liberar();
                    } else {
                        // Sigue vivo: liberar al final del ciclo
                        particionesALiberar.add(particionAsignada);
                    }
                    actual.particion = null;
                }
            }

            // Liberar todas las particiones al terminar el ciclo
            for (Particion p : particionesALiberar) {
                p.liberar();
            }
            particionesALiberar.clear();

            // Al terminar de procesar todos los procesos del ciclo actual:
            // colaListos = pendientes + siguienteCiclo (primero no encontraron, luego ejecutaron)
            colaListos.clear();
            colaListos.addAll(pendientes);
            colaListos.addAll(siguienteCiclo);
        }

        // === FINALIZACIÓN ===
        // Llamar registro.copiarEstado(FINALIZADO, FINALIZACION_PARTICIONES)
        registro.copiarEstado(
            RegistroSimulacion.FINALIZADO,
            RegistroSimulacion.FINALIZACION_PARTICIONES
        );

        return registro;
    }

    private Particion buscarParticionNextFit(List<Particion> particiones, long tamanio, int[] ultimaUsada) {
        if (particiones == null || particiones.isEmpty()) {
            return null;
        }
        int n = particiones.size();
        for (int i = 0; i < n; i++) {
            int idx = (ultimaUsada[0] + i) % n;
            Particion p = particiones.get(idx);
            if (p.estaDisponible(tamanio)) {
                ultimaUsada[0] = (idx + 1) % n;
                return p;
            }
        }
        return null;
    }

    private ProcesoRuntime extraerSiguiente(List<ProcesoRuntime> colaListos) {
        if (colaListos.isEmpty()) {
            return null;
        }
        return colaListos.remove(0);
    }

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime) {
        registrarEstado(registro, estado, runtime, runtime.tiempoRestante);
    }

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime, long tiempoSnapshot) {
        Proceso snapshot = new Proceso(
            runtime.id,
            runtime.nombre,
            runtime.tiempoRestante,
            runtime.tamanioMemoria,
            runtime.pasaPorBloqueado
        );
        snapshot.setTiempoRestante(tiempoSnapshot);
        snapshot.setEstadoActual(estado);
        snapshot.setParticion(runtime.particion);
        registro.registrar(estado, snapshot);
    }

    private static final class ProcesoRuntime {
        private final int id;
        private final String nombre;
        private final long tamanioMemoria;
        private final boolean pasaPorBloqueado;
        private Particion particion;
        private long tiempoRestante;

        private ProcesoRuntime(
            int id,
            String nombre,
            long tiempoRestante,
            long tamanioMemoria,
            boolean pasaPorBloqueado,
            Particion particion
        ) {
            this.id = id;
            this.nombre = nombre;
            this.tiempoRestante = tiempoRestante;
            this.tamanioMemoria = tamanioMemoria;
            this.pasaPorBloqueado = pasaPorBloqueado;
            this.particion = particion;
        }

        private static ProcesoRuntime desde(Proceso p) {
            return new ProcesoRuntime(
                p.getId(),
                p.getNombre(),
                p.getTiempoRestante(),
                p.getTamanioMemoria(),
                p.isPasaPorBloqueado(),
                p.getParticion()
            );
        }
    }
}
