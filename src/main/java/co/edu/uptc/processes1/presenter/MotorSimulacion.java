package co.edu.uptc.processes1.presenter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;

public class MotorSimulacion {

    private static final BigInteger QUANTUM = BigInteger.valueOf(1000L);
    private static final BigInteger DIRECCION_BASE = BigInteger.TEN; // Empieza en 10

    public RegistroSimulacion ejecutar(List<Proceso> procesosIniciales) {
    RegistroSimulacion registro = new RegistroSimulacion();
    
    List<ProcesoRuntime> procesosOrdenados = procesosIniciales.stream()
        .map(ProcesoRuntime::desde)
        .sorted((a, b) -> Integer.compare(a.id, b.id))
        .toList();
    
    List<ParticionInfo> particiones = crearParticionesFijas(new ArrayList<>(procesosOrdenados));
    
    int maxIteraciones = procesosOrdenados.size() * 1000;
    int iteracionesGlobales = 0;
    int contadorParticiones = procesosOrdenados.size();
    
    List<ProcesoRuntime> colaListos = new ArrayList<>(procesosOrdenados);
    
    for (ProcesoRuntime runtime : colaListos) {
        runtime.particionAsignada = buscarParticionPorProceso(runtime, particiones);
        registrarEstado(registro, RegistroSimulacion.INICIO, runtime);
    }
    
    while (!colaListos.isEmpty()) {
        if (iteracionesGlobales > maxIteraciones) {
            for (ProcesoRuntime restante : new ArrayList<>(colaListos)) {
                registrarEstadoNoEjecutado(registro,
                    "La simulación superó el límite de iteraciones seguras", restante);
            }
            colaListos.clear();
            break;
        }
        
        boolean huboTerminados = false;
        
        for (int i = 0; i < colaListos.size(); ) {
            iteracionesGlobales++;
            ProcesoRuntime actual = colaListos.get(i);
            
            registrarEstado(registro, RegistroSimulacion.DESPACHAR, actual);
            registrarEstado(registro, RegistroSimulacion.PROCESADOR, actual, actual.tiempoRestante);
            
            BigInteger tiempoEjecutado = actual.tiempoRestante.min(QUANTUM);
            actual.tiempoRestante = actual.tiempoRestante.subtract(tiempoEjecutado);
            
            if (actual.tiempoRestante.compareTo(BigInteger.ZERO) <= 0) {
                registrarEstado(registro, RegistroSimulacion.FINALIZADO, actual);
                
                if (actual.particionAsignada != null) {
                    registro.registrarUsoParticion(actual.id, actual.nombre,
                        actual.particionAsignada.nombre, tiempoEjecutado);
                    
                    System.out.println("PROCESO TERMINADO: " + actual.nombre + " en " + actual.particionAsignada.nombre);
                    System.out.println("Particiones antes: " + particiones.stream().map(p -> p.nombre + (p.libre ? "(L)" : "(O)")).toList());
                    
                    actual.particionAsignada.libre = true;
                    actual.particionAsignada.idProceso = -1;
                    actual.particionAsignada.nombreProceso = null;
                    
                    System.out.println("Particiones después de liberar: " + particiones.stream().map(p -> p.nombre + (p.libre ? "(L)" : "(O)")).toList());
                    
                    registro.registrarFinalizacionParticion(
                        actual.particionAsignada.nombre,
                        actual.particionAsignada.tamanio
                    );
                    
                    huboTerminados = true;
                }
                
                colaListos.remove(i);
            } else {
                registrarEstado(registro, RegistroSimulacion.EXPIRACION_TIEMPO, actual, actual.tiempoRestante);
                
                if (actual.particionAsignada != null) {
                    registro.registrarUsoParticion(actual.id, actual.nombre,
                        actual.particionAsignada.nombre, tiempoEjecutado);
                }
                
                i++;
            }
        }
        
        // AL FINAL DEL CICLO: verificar condensaciones y compactaciones
        if (huboTerminados) {
            contadorParticiones = verificarYCondensar(particiones, registro, contadorParticiones);
            System.out.println("Particiones después de verificarYCondensar: " + particiones.stream().map(p -> p.nombre + (p.libre ? "(L)" : "(O)")).toList());
            System.out.println("---");
            
            // Actualizar referencias de procesos después de compactación
            for (ProcesoRuntime proceso : colaListos) {
                if (proceso.particionAsignada != null && !proceso.particionAsignada.libre) {
                    for (ParticionInfo p : particiones) {
                        if (!p.libre && p.idProceso == proceso.id) {
                            proceso.particionAsignada = p;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    System.out.println("========================================");
    System.out.println("TOTAL DE PARTICIONES CREADAS: " + contadorParticiones);
    System.out.println("Particiones finales: " + particiones.stream().map(p -> p.nombre + (p.libre ? "(L)" : "(O)")).toList());
    System.out.println("Compactaciones registradas: " + registro.getHistorialCompactaciones().size());
    System.out.println("Condensaciones registradas: " + registro.getHistorialCondensaciones().size());
    System.out.println("========================================");
    
    return registro;
}

    // Métodos de registro
    private void registrarEstado(RegistroSimulacion registro, String estado, ProcesoRuntime runtime) {
        registrarEstado(registro, estado, runtime, runtime.tiempoRestante);
    }

    private void registrarEstado(RegistroSimulacion registro, String estado, 
                                 ProcesoRuntime runtime, BigInteger tiempoSnapshot) {
        Proceso snapshot = new Proceso(
            runtime.id,
            runtime.nombre,
            runtime.tiempoRestante,
            runtime.tamanioMemoria
        );
        snapshot.setTiempoRestante(tiempoSnapshot);
        snapshot.setEstadoActual(estado);
        
        if (runtime.particionAsignada != null) {
            snapshot.setParticion(new Particion(
                extraerIdParticion(runtime.particionAsignada.nombre),
                runtime.particionAsignada.direccionInicio,
                runtime.tamanioMemoria,
                runtime.id,
                runtime.nombre
            ));
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
        registro.registrarNoEjecutado(snapshot, motivo);
    }

    private int extraerIdParticion(String nombreParticion) {
        String digitos = nombreParticion.replaceAll("\\D+", "");
        return digitos.isEmpty() ? 0 : Integer.parseInt(digitos);
    }

    // Métodos de búsqueda y creación
    private ParticionInfo buscarParticionPorProceso(ProcesoRuntime proceso, List<ParticionInfo> particiones) {
        return particiones.stream()
            .filter(p -> p.idProceso == proceso.id)
            .findFirst()
            .orElse(null);
    }

    private List<ParticionInfo> crearParticionesFijas(List<ProcesoRuntime> procesos) {
        List<ParticionInfo> particiones = new ArrayList<>();
        int idParticion = 1;
        BigInteger direccionActual = DIRECCION_BASE;
        
        for (ProcesoRuntime proceso : procesos) {
            BigInteger direccionFin = direccionActual.add(proceso.tamanioMemoria);
            
            particiones.add(new ParticionInfo(
                "PAR" + idParticion,
                proceso.tamanioMemoria,
                proceso.id,
                proceso.nombre,
                direccionActual,
                direccionFin
            ));
            
            direccionActual = direccionFin;
            idParticion++;
        }
        
        return particiones;
    }

    // Métodos de condensación y compactación
    private int verificarYCondensar(List<ParticionInfo> particiones, 
                                 RegistroSimulacion registro, 
                                 int contadorParticiones) {
    boolean huboCambio = true;
    
    while (huboCambio) {
        huboCambio = false;
        
        System.out.println("  [VERIFICAR] Intentando condensar: " + particiones.stream().map(p -> p.nombre + (p.libre ? "(L)" : "(O)")).toList());
        // PASO 1: Intentar condensar PRIMERO
        boolean condensado = false;
        for (int i = 0; i < particiones.size() - 1; i++) {
            ParticionInfo actual = particiones.get(i);
            ParticionInfo siguiente = particiones.get(i + 1);
            
            if (actual.libre && siguiente.libre) {
                System.out.println("  [CONDENSAR] " + actual.nombre + "(L) + " + siguiente.nombre + "(L)");
                BigInteger nuevoTamanio = actual.tamanio.add(siguiente.tamanio);
                contadorParticiones++;
                String nuevoNombre = "PAR" + contadorParticiones;
                
                BigInteger direccionInicio = actual.direccionInicio;
                BigInteger direccionFin = direccionInicio.add(nuevoTamanio);
                
                ParticionInfo condensada = new ParticionInfo(
                    nuevoNombre, nuevoTamanio, -1, null,
                    direccionInicio, direccionFin);
                condensada.libre = true;
                
                String descripcion = "Condensación entre " + actual.nombre + 
                                    " y " + siguiente.nombre;
                registro.registrarCondensacion(nuevoNombre, descripcion, nuevoTamanio);
                registro.registrarFinalizacionParticion(nuevoNombre, nuevoTamanio);
                
                particiones.remove(i + 1);
                particiones.remove(i);
                particiones.add(i, condensada);
                
                condensado = true;
                huboCambio = true;
                break;
            }
        }
        
        // PASO 2: SOLO si NO se pudo condensar, verificar fragmentación y compactar
        if (!condensado && hayFragmentacion(particiones)) {
            System.out.println("  [VERIFICAR] Hay fragmentación, compactando...");
            contadorParticiones = compactar(particiones, registro, contadorParticiones);
            huboCambio = true; // Después de compactar, volver a intentar condensar
        }
    }
    
    return contadorParticiones;
}

    private boolean hayFragmentacion(List<ParticionInfo> particiones) {
    for (int i = 0; i < particiones.size() - 1; i++) {
        // Si hay un hueco seguido de una ocupada, hay fragmentación
        if (particiones.get(i).libre && !particiones.get(i + 1).libre) {
            return true;
        }
    }
    return false;
}    

    private int compactar(List<ParticionInfo> particiones, 
                      RegistroSimulacion registro, 
                      int contadorParticiones) {
    
    int primerHueco = -1;
    for (int i = 0; i < particiones.size(); i++) {
        if (particiones.get(i).libre) {
            primerHueco = i;
            break;
        }
    }
    
    if (primerHueco == -1) {
        return contadorParticiones;
    }
    
    List<ParticionInfo> antesDelHueco = new ArrayList<>();
    for (int i = 0; i < primerHueco; i++) {
        antesDelHueco.add(particiones.get(i));
    }
    
    List<ParticionInfo> desdeElHueco = new ArrayList<>();
    for (int i = primerHueco; i < particiones.size(); i++) {
        desdeElHueco.add(particiones.get(i));
    }
    
    // El primer elemento es el hueco a mover
    ParticionInfo huecoAMover = desdeElHueco.get(0);
    
    // Las ocupadas (elementos entre el primer hueco y los huecos del final)
    List<ParticionInfo> ocupadas = new ArrayList<>();
    // Los huecos que ya están al final (se quedan como están)
    List<ParticionInfo> huecosFinal = new ArrayList<>();
    
    boolean encontroOcupada = false;
    for (int i = 1; i < desdeElHueco.size(); i++) {
        ParticionInfo p = desdeElHueco.get(i);
        if (!p.libre) {
            ocupadas.add(p);
            encontroOcupada = true;
        } else if (encontroOcupada) {
            // Hueco después de ocupadas = hueco del final
            huecosFinal.add(p);
        } else {
            // Hueco antes de cualquier ocupada (no debería pasar)
            ocupadas.add(p); // lo tratamos como ocupada
        }
    }
    
    particiones.clear();
    particiones.addAll(antesDelHueco);
    
    BigInteger direccionActual;
    if (antesDelHueco.isEmpty()) {
        direccionActual = DIRECCION_BASE;
    } else {
        direccionActual = antesDelHueco.get(antesDelHueco.size() - 1).direccionFin;
    }
    
    // 1. Asignar nombre al hueco que se mueve
    contadorParticiones++;
    String nombreHuecoMovido = "PAR" + contadorParticiones;
    
    // 2. Asignar nombres a las ocupadas
    List<String> nombresOcupadas = new ArrayList<>();
    for (int i = 0; i < ocupadas.size(); i++) {
        contadorParticiones++;
        nombresOcupadas.add("PAR" + contadorParticiones);
    }
    
    // 3. Procesar ocupadas primero
    for (int i = 0; i < ocupadas.size(); i++) {
        ParticionInfo ocupada = ocupadas.get(i);
        String nuevoNombre = nombresOcupadas.get(i);
        BigInteger direccionFin = direccionActual.add(ocupada.tamanio);
        
        ParticionInfo nuevaParticion = new ParticionInfo(
            nuevoNombre, ocupada.tamanio, ocupada.idProceso, ocupada.nombreProceso,
            direccionActual, direccionFin);
        nuevaParticion.libre = false;
        
        String descripcion = "Compactación de la partición " + ocupada.nombre;
        registro.registrarCompactacion(nuevoNombre, ocupada.nombre, descripcion, 
            ocupada.tamanio, direccionActual, direccionFin);
        registro.registrarFinalizacionParticion(nuevoNombre, ocupada.tamanio);
        
        particiones.add(nuevaParticion);
        direccionActual = direccionFin;
    }
    
    // 4. Agregar el hueco movido
    BigInteger direccionFinHueco = direccionActual.add(huecoAMover.tamanio);
    ParticionInfo nuevoHueco = new ParticionInfo(
        nombreHuecoMovido, huecoAMover.tamanio, -1, null,
        direccionActual, direccionFinHueco);
    nuevoHueco.libre = true;
    
    String descripcionHueco = "Compactación de la partición " + huecoAMover.nombre;
    registro.registrarCompactacion(nombreHuecoMovido, huecoAMover.nombre, descripcionHueco, 
        huecoAMover.tamanio, direccionActual, direccionFinHueco);
    registro.registrarFinalizacionParticion(nombreHuecoMovido, huecoAMover.tamanio);
    
    particiones.add(nuevoHueco);
    direccionActual = direccionFinHueco;
    
    // 5. Los huecos del final se quedan con su nombre original
    for (ParticionInfo hf : huecosFinal) {
        particiones.add(hf);
    }
    
    return contadorParticiones;
}    

    // Clases internas
    private static class ParticionInfo {
        String nombre;
        BigInteger tamanio;
        int idProceso;
        String nombreProceso;
        boolean libre;
        BigInteger direccionInicio;
        BigInteger direccionFin;
        
        ParticionInfo(String nombre, BigInteger tamanio, int idProceso, String nombreProceso,
                      BigInteger direccionInicio, BigInteger direccionFin) {
            this.nombre = nombre;
            this.tamanio = tamanio;
            this.idProceso = idProceso;
            this.nombreProceso = nombreProceso;
            this.libre = false;
            this.direccionInicio = direccionInicio;
            this.direccionFin = direccionFin;
        }
    }

    private static final class ProcesoRuntime {
        final int id;
        final String nombre;
        final BigInteger tamanioMemoria;
        BigInteger tiempoRestante;
        ParticionInfo particionAsignada;

        ProcesoRuntime(int id, String nombre, BigInteger tiempoRestante, BigInteger tamanioMemoria) {
            this.id = id;
            this.nombre = nombre;
            this.tiempoRestante = tiempoRestante;
            this.tamanioMemoria = tamanioMemoria;
            this.particionAsignada = null;
        }

        static ProcesoRuntime desde(Proceso p) {
            return new ProcesoRuntime(
                p.getId(),
                p.getNombre(),
                p.getTiempoRestante(),
                p.getTamanioMemoria()
            );
        }
    }
}