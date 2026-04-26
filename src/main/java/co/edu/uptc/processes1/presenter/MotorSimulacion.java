package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de simulacion Round Robin con flujo secuencial determinista.
 *
 * No usa listas paralelas de bloqueo/suspension: solo registra transiciones
 * de historial y reencola el proceso cuando aplique.
 */
public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(5000L);

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, List<Particion> particiones) {
        int[] ultimaParticion = {0};
        RegistroSimulacion registro = new RegistroSimulacion();

        // Calcular tamaño de partición más grande
        BigInteger particionMasGrande = particiones == null || particiones.isEmpty()
            ? BigInteger.ZERO
            : particiones.stream().map(Particion::getTamanoTotal).max(BigInteger::compareTo).orElse(BigInteger.ZERO);

        // === INICIALIZACIÓN ===
        // 1. Ordenar procesos por tiempo de ejecución ascendente
        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> a.tiempoRestante.compareTo(b.tiempoRestante))
            .toList();

        List<ProcesoRuntime> colaListos = new ArrayList<>();

        // 2. Marcar como NO_EJECUTADO los que superan partición más grande
        for (ProcesoRuntime runtime : procesosOrdenados) {
            if (runtime.tamanioMemoria.compareTo(particionMasGrande) > 0) {
                registrarEstado(registro, RegistroSimulacion.NO_EJECUTADO, runtime);
            } else {
                colaListos.add(runtime);
            }
        }

        // Registrar INICIO una sola vez por proceso antes del ciclo de ráfagas.
        for (ProcesoRuntime runtime : colaListos) {
            registrarEstado(registro, RegistroSimulacion.LISTO, runtime);
        }

        // === CICLO PRINCIPAL ===
        while (!colaListos.isEmpty()) {
            List<Particion> particionesALiberar = new ArrayList<>();

            // Procesar todos los procesos actualmente en colaListos en orden
            for (int i = 0; i < colaListos.size(); ) {
                ProcesoRuntime actual = colaListos.get(i);

                // Intentar asignar partición con Next Fit
                Particion particionAsignada = buscarParticionNextFit(particiones, actual.tamanioMemoria, ultimaParticion);

                if (particionAsignada == null) {
                    // Si no se encuentra: permanece en cola, sin alterar orden
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                    continue;
                }

                // Si se encuentra partición:
                particionAsignada.ocupar();
                actual.particion = particionAsignada;
                boolean termino = false;

                try {
                    // a. Registrar snapshot DESPACHAR
                    registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                    // b. Calcular ráfaga = min(QUANTUM, tiempoRestante)
                    BigInteger rafaga = QUANTUM.min(actual.tiempoRestante);
                    BigInteger tiempoTrasRafaga = actual.tiempoRestante.subtract(rafaga).max(BigInteger.ZERO);

                    // c. Registrar snapshot PROCESADOR con tiempoRestante - rafaga como tiempo snapshot
                    registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);

                    // d. Llamar registro.registrarUsoParticion(..., rafaga)
                    registro.registrarUsoParticion(
                        actual.id,
                        actual.nombre,
                        particionAsignada.getNombre(),
                        rafaga
                    );

                    // e. Actualizar actual.tiempoRestante -= rafaga
                    actual.tiempoRestante = tiempoTrasRafaga;

                    // f. Si tiempoRestante <= 0: registrar FINALIZADO y no reencolar
                    if (actual.tiempoRestante.compareTo(BigInteger.ZERO) <= 0) {
                        registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual, BigInteger.ZERO);
                        termino = true;
                    } else if (actual.pasaPorBloqueado) {
                        // g. Si tiempoRestante > 0 y pasaPorBloqueado: registrar BLOQUEAR, BLOQUEADO, DESPERTAR
                        registrarEstado(registro, RegistroSimulacion.BLOQUEAR, actual);
                        registrarEstado(registro, RegistroSimulacion.BLOQUEADO, actual);
                        registrarEstado(registro, RegistroSimulacion.DESPERTAR, actual);
                    } else {
                        // h. Si tiempoRestante > 0 y no pasaPorBloqueado: registrar EXPIRACION_TIEMPO
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                    }

                } finally {
                    // Todas las particiones del ciclo se liberan al final de la vuelta
                    particionesALiberar.add(particionAsignada);
                    actual.particion = null;
                }

                // Mantener orden original: solo eliminar si terminó
                if (termino) {
                    colaListos.remove(i);
                } else {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                }
            }

            // Liberar todas las particiones al terminar el ciclo
            for (Particion p : particionesALiberar) {
                p.liberar();
            }
            particionesALiberar.clear();
        }

        // === FINALIZACIÓN ===
        // Llamar registro.copiarEstado(FINALIZADO, FINALIZACION_PARTICIONES)
        registro.copiarEstado(
            RegistroSimulacion.FINALIZADO,
            RegistroSimulacion.FINALIZACION_PARTICIONES
        );

        return registro;
    }

    private Particion buscarParticionNextFit(List<Particion> particiones, BigInteger tamanio, int[] ultimaUsada) {
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

    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime, BigInteger tiempoSnapshot) {
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
        private final BigInteger tamanioMemoria;
        private final boolean pasaPorBloqueado;
        private Particion particion;
        private BigInteger tiempoRestante;

        private ProcesoRuntime(
            int id,
            String nombre,
            BigInteger tiempoRestante,
            BigInteger tamanioMemoria,
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
