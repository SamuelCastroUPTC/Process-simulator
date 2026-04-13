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
        List<ProcesoRuntime> colaListos = new ArrayList<>();

        // CAMBIO: ya NO registramos "Listo" aquí, solo encolamos
        for (Proceso proceso : procesosIniciales) {
            colaListos.add(ProcesoRuntime.desde(proceso));
        }

        while (!colaListos.isEmpty()) {
            ProcesoRuntime actual = colaListos.remove(0);

            Particion particionAsignada = buscarParticionNextFit(particiones, actual.tamanioMemoria, ultimaParticion);

            if (particionAsignada == null) {
                registrarEstado(registro, RegistroSimulacion.NO_EJECUTADO, actual);
                continue;
            }

            particionAsignada.ocupar();
            actual.particion = particionAsignada;

            // NUEVO: registrar "Listo" aquí, con partición ya asignada
            registrarEstado(registro, RegistroSimulacion.INICIO, actual);

            try {
                registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                long rafaga = Math.min(QUANTUM, actual.tiempoRestante);
                long tiempoTrasRafaga = Math.max(0L, actual.tiempoRestante - rafaga);
                registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);
                actual.tiempoRestante = tiempoTrasRafaga;

                if (actual.tiempoRestante <= 0L) {
                    registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual, 0L);
                    continue;
                }

                if (actual.pasaPorBloqueado) {
                    registrarEstado(registro, RegistroSimulacion.BLOQUEAR, actual);
                    registrarEstado(registro, RegistroSimulacion.BLOQUEADO, actual);
                    registrarEstado(registro, RegistroSimulacion.DESPERTAR, actual);
                } else {
                    registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                }

                colaListos.add(actual);

            } finally {
                particionAsignada.liberar();
                actual.particion = null;
            }
        }

        return registro;
    }

    private Particion buscarParticionNextFit(List<Particion> particiones, int tamanio, int[] ultimaUsada) {
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
        private final int tamanioMemoria;
        private final boolean pasaPorBloqueado;
        private Particion particion;
        private long tiempoRestante;

        private ProcesoRuntime(
            int id,
            String nombre,
            long tiempoRestante,
            int tamanioMemoria,
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
