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

    private static final int QUANTUM = 5000;

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales) {
        RegistroSimulacion registro = new RegistroSimulacion();
        List<ProcesoRuntime> colaListos = new ArrayList<>();

        for (Proceso proceso : procesosIniciales) {
            ProcesoRuntime runtime = ProcesoRuntime.desde(proceso);
            colaListos.add(runtime);
            registrarEstado(registro, RegistroSimulacion.INICIO, runtime);
        }

        while (!colaListos.isEmpty()) {
            ProcesoRuntime actual = extraerSiguiente(colaListos);
            if (actual == null) {
                continue;
            }

            registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

            int rafaga = Math.min(QUANTUM, actual.tiempoRestante);
            int tiempoRestante = Math.max(0, actual.tiempoRestante - rafaga);
            registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoRestante);
            actual.tiempoRestante = tiempoRestante;

            if (actual.tiempoRestante <= 0) {
                registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual, 0);
                continue;
            }

            if (actual.pasaPorBloqueado) {
                registrarEstado(registro, RegistroSimulacion.BLOQUEAR, actual);
                registrarEstado(registro, RegistroSimulacion.BLOQUEADO, actual);
                registrarEstado(registro, RegistroSimulacion.DESPERTAR, actual);
                registrarEstado(registro, RegistroSimulacion.INICIO, actual);
                colaListos.add(actual);
                continue;
            }

            registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
            registrarEstado(registro, RegistroSimulacion.INICIO, actual);
            colaListos.add(actual);
        }

        return registro;
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

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime, int tiempoSnapshot) {
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
        private final Particion particion;
        private int tiempoRestante;

        private ProcesoRuntime(
            int id,
            String nombre,
            int tiempoRestante,
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
