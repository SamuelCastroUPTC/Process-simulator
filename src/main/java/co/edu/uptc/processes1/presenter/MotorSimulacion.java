package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de simulacion Round Robin con flujo secuencial determinista.
 */
public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(5000L);

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales, MemoriaVariable memoria) {
        RegistroSimulacion registro = new RegistroSimulacion();

        BigInteger tamanioTotalMemoria = memoria.getTamanioTotal();

        List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
            .map(ProcesoRuntime::desde)
            .sorted((a, b) -> a.tiempoRestante.compareTo(b.tiempoRestante))
            .toList();

        List<ProcesoRuntime> colaListos = new ArrayList<>();

        for (ProcesoRuntime runtime : procesosOrdenados) {
            if (runtime.tamanioMemoria.compareTo(tamanioTotalMemoria) > 0) {
                registrarEstado(registro, RegistroSimulacion.NO_EJECUTADO, runtime);
            } else {
                colaListos.add(runtime);
            }
        }

        for (ProcesoRuntime runtime : colaListos) {
            registrarEstado(registro, RegistroSimulacion.LISTO, runtime);
        }

        while (!colaListos.isEmpty()) {
            for (int i = 0; i < colaListos.size(); ) {
                ProcesoRuntime actual = colaListos.get(i);

                BigInteger direccionInicio = memoria.asignar(actual.id, actual.nombre, actual.tamanioMemoria);
                if (direccionInicio == null) {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                    continue;
                }

                actual.referenciaMemoria = direccionInicio.toString();
                boolean termino = false;

                try {
                    registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);

                    BigInteger rafaga = QUANTUM.min(actual.tiempoRestante);
                    BigInteger tiempoTrasRafaga = actual.tiempoRestante.subtract(rafaga).max(BigInteger.ZERO);

                    registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, tiempoTrasRafaga);

                    registro.registrarUsoParticion(
                        actual.id,
                        actual.nombre,
                        actual.referenciaMemoria,
                        rafaga
                    );

                    actual.tiempoRestante = tiempoTrasRafaga;

                    if (actual.tiempoRestante.compareTo(BigInteger.ZERO) <= 0) {
                        registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual, BigInteger.ZERO);
                        termino = true;
                    } else if (actual.pasaPorBloqueado) {
                        registrarEstado(registro, RegistroSimulacion.BLOQUEAR, actual);
                        registrarEstado(registro, RegistroSimulacion.BLOQUEADO, actual);
                        registrarEstado(registro, RegistroSimulacion.DESPERTAR, actual);
                    } else {
                        registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual);
                    }

                } finally {
                    memoria.liberar(actual.id);
                    actual.referenciaMemoria = null;
                }

                if (termino) {
                    colaListos.remove(i);
                } else {
                    registrarEstado(registro, RegistroSimulacion.LISTO, actual);
                    i++;
                }
            }
        }

        registro.copiarEstado(
            RegistroSimulacion.FINALIZADO,
            RegistroSimulacion.FINALIZACION_PARTICIONES
        );

        return registro;
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
        if (runtime.referenciaMemoria != null) {
            snapshot.setParticion(new Particion(-1, runtime.referenciaMemoria, runtime.tamanioMemoria));
        } else {
            snapshot.setParticion(null);
        }
        registro.registrar(estado, snapshot);
    }

    private static final class ProcesoRuntime {
        private final int id;
        private final String nombre;
        private final BigInteger tamanioMemoria;
        private final boolean pasaPorBloqueado;
        private BigInteger tiempoRestante;
        private String referenciaMemoria;

        private ProcesoRuntime(
            int id,
            String nombre,
            BigInteger tiempoRestante,
            BigInteger tamanioMemoria,
            boolean pasaPorBloqueado
        ) {
            this.id = id;
            this.nombre = nombre;
            this.tiempoRestante = tiempoRestante;
            this.tamanioMemoria = tamanioMemoria;
            this.pasaPorBloqueado = pasaPorBloqueado;
            this.referenciaMemoria = null;
        }

        private static ProcesoRuntime desde(Proceso p) {
            return new ProcesoRuntime(
                p.getId(),
                p.getNombre(),
                p.getTiempoRestante(),
                p.getTamanioMemoria(),
                p.isPasaPorBloqueado()
            );
        }
    }
}
