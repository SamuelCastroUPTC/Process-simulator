package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.view.IView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigInteger;
import java.util.stream.Collectors;

public class SimuladorPresenter implements IPresenter {

    private final IView view;
    private final MotorSimulacion motorSimulacion;
    private final List<Particion> particionesMemoria;

    private final List<Proceso> procesosCargados = new ArrayList<>();
    private final Map<String, List<RegistroSimulacion.SnapshotProceso>> historialesPorEstado = new LinkedHashMap<>();
    private Particion particionEnEdicion;
    private int contadorId = 1;
    private RegistroSimulacion ultimoRegistro = new RegistroSimulacion();

    private static final List<String> ESTADOS = List.of(
        RegistroSimulacion.INICIO,
        RegistroSimulacion.DESPACHAR,
        RegistroSimulacion.PROCESADOR,
        RegistroSimulacion.EXPIRACION_TIEMPO,
        RegistroSimulacion.BLOQUEAR,
        RegistroSimulacion.BLOQUEADO,
        RegistroSimulacion.DESPERTAR,
        RegistroSimulacion.NO_EJECUTADO,
        RegistroSimulacion.FINALIZADO,
        RegistroSimulacion.FINALIZACION_PARTICIONES
    );

    public SimuladorPresenter(IView view) {
        this(view, List.of(), new MotorSimulacion());
    }

    public SimuladorPresenter(IView view, List<Integer> tamaniosParticiones) {
        this(view, tamaniosParticiones, new MotorSimulacion());
    }

    public SimuladorPresenter(IView view, MotorSimulacion motorSimulacion) {
        this(view, List.of(), motorSimulacion);
    }

    public SimuladorPresenter(IView view, List<Integer> tamaniosParticiones, MotorSimulacion motorSimulacion) {
        this.view = view;
        this.motorSimulacion = motorSimulacion;
        this.particionesMemoria = new ArrayList<>();
        int idSecuencial = 1;
        for (Integer tamano : tamaniosParticiones) {
            if (tamano != null && tamano > 0) {
                this.particionesMemoria.add(new Particion(idSecuencial, "Partición " + idSecuencial, tamano));
                idSecuencial++;
            }
        }
        ESTADOS.forEach(estado -> historialesPorEstado.put(estado, new ArrayList<>()));
    }

    @Override
    public void agregarProceso(
        String nombre,
        long tiempo,
        long tamanioMemoria,
        boolean pasaPorBloqueado
    ) {
        if (existeNombre(nombre)) {
            view.mostrarError("El nombre ya existe");
            return;
        }

        Proceso nuevo = new Proceso(
            contadorId++,
            nombre,
            tiempo,
            tamanioMemoria,
            pasaPorBloqueado
        );

        procesosCargados.add(nuevo);

        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
        view.limpiarFormularioCarga();
        view.mostrarExito("Proceso '" + nombre + "' cargado correctamente.");
    }

    @Override
    public void agregarParticion(String nombre, long tamano) {
        if (nombre == null || nombre.isBlank()) {
            view.mostrarError("El nombre de la partición es obligatorio.");
            return;
        }
        if (tamano <= 0) {
            view.mostrarError("El tamaño de la partición debe ser mayor a 0.");
            return;
        }
        String nombreNormalizado = nombre.trim();
        int idParticionEditada = particionEnEdicion != null ? particionEnEdicion.getId() : -1;
        boolean nombreDuplicado = particionesMemoria.stream()
            .anyMatch(p -> p.getId() != idParticionEditada
                && p.getNombre().equalsIgnoreCase(nombreNormalizado));
        if (nombreDuplicado) {
            view.mostrarError("Ya existe una partición con ese nombre.");
            return;
        }

        if (particionEnEdicion != null) {
            particionEnEdicion.setNombre(nombreNormalizado);
            particionEnEdicion.setTamanoTotal(tamano);
            particionEnEdicion = null;
            view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
            view.mostrarExito("Partición actualizada correctamente.");
            return;
        }

        int idSecuencial = particionesMemoria.size() + 1;
        particionesMemoria.add(new Particion(idSecuencial, nombreNormalizado, tamano));
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
        view.mostrarExito("Partición '" + nombreNormalizado + "' creada correctamente.");
    }

    @Override
    public RegistroSimulacion iniciarSimulacion() {
        if (procesosCargados.isEmpty()) {
            view.mostrarError("No hay procesos en la cola. Cargue al menos uno antes de simular.");
            return ultimoRegistro;
        }

        view.setBtnIniciarHabilitado(false);
        view.actualizarEstadoSimulacion("Simulacion en progreso...");

        List<Proceso> procesosOrdenados = procesosCargados.stream()
            .sorted(Comparator.comparingLong(Proceso::getTiempoRestante))
            .collect(Collectors.toList());

        ultimoRegistro = motorSimulacion.ejecutar(procesosOrdenados, particionesMemoria);
        sincronizarHistorialesConRegistro(ultimoRegistro);

        procesosCargados.clear();
        particionesMemoria.forEach(Particion::liberar);
        view.actualizarTablaCargados(new ArrayList<>());
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
        view.actualizarEstadoSimulacion("Simulacion finalizada.");
        view.mostrarExito("Simulacion completada. Puede revisar el historial por estado.");
        return ultimoRegistro;
    }

    @Override
    public List<Proceso> getProcesosCargados() {
        return Collections.unmodifiableList(procesosCargados);
    }

    @Override
    public void onCargarProceso() {
        String nombre = view.getNombreProceso();
        String tiempoStr = view.getTiempoProceso();
        String tamanioMemoriaStr = view.getTamanioMemoria().replace(".", "");
        boolean pasaPorBloqueado = view.isPasaPorBloqueado();

        if (nombre.isBlank() || tiempoStr.isBlank() || tamanioMemoriaStr.isBlank()) {
            view.mostrarError("Por favor, complete todos los campos obligatorios");
            return;
        }

        if (!tiempoStr.matches("-?\\d+")) {
            view.mostrarError("El tiempo debe ser un numero entero valido");
            return;
        }

        BigInteger tiempoSegundos = new BigInteger(tiempoStr);
        if (tiempoSegundos.signum() <= 0) {
            view.mostrarError("El tiempo de ejecucion debe ser mayor a 0");
            return;
        }

        // Si el tiempo en milisegundos desborda long, se satura en Long.MAX_VALUE.
        BigInteger tiempoMs = tiempoSegundos.multiply(BigInteger.valueOf(1000L));
        long tiempo = tiempoMs.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
            ? Long.MAX_VALUE
            : tiempoMs.longValue();

        if (!tamanioMemoriaStr.matches("\\d+")) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        BigInteger tamanioMemoriaBig = new BigInteger(tamanioMemoriaStr);
        if (tamanioMemoriaBig.signum() <= 0) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        // Si el tamaño desborda long, se satura en Long.MAX_VALUE.
        long tamanioMemoria = tamanioMemoriaBig.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
            ? Long.MAX_VALUE
            : tamanioMemoriaBig.longValue();

        agregarProceso(
            nombre,
            tiempo,
            tamanioMemoria,
            pasaPorBloqueado
        );
    }

    @Override
    public void onIniciarSimulacion() {
        iniciarSimulacion();
    }

    @Override
    public void onEliminarProceso(Proceso proceso) {
        procesosCargados.removeIf(p -> p.getId() == proceso.getId());
        Particion particionAsignada = proceso.getParticion();
        if (particionAsignada != null) {
            particionAsignada.removerProceso(proceso);
            proceso.setParticion(null);
        }
        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
    }

    @Override
    public void onEliminarParticion(Particion particion) {
        if (particion == null) {
            return;
        }
        if (particionEnEdicion != null && particionEnEdicion.getId() == particion.getId()) {
            particionEnEdicion = null;
        }
        particionesMemoria.removeIf(p -> p.getId() == particion.getId());
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
    }

    @Override
    public void onEditarParticion(Particion particion) {
        this.particionEnEdicion = particion;
    }

    public Particion getParticionEnEdicion() {
        return particionEnEdicion;
    }

    @Override
    public void onVerHistorial(String estado) {
        String estadoCanonico = normalizarEstado(estado);
        List<RegistroSimulacion.SnapshotProceso> datos = historialesPorEstado.getOrDefault(estadoCanonico, List.of());
        view.mostrarHistorial(estado, datos);
    }

    public List<Particion> getParticionesMemoria() {
        return Collections.unmodifiableList(particionesMemoria);
    }

    public Map<String, List<String>> getHistorialTexto() {
        return ultimoRegistro.getHistorialTexto();
    }

    public Optional<List<String>> getHistorialTexto(String estado) {
        String estadoCanonico = normalizarEstado(estado);
        List<String> datos = ultimoRegistro.getHistorialTexto(estadoCanonico);
        return datos.isEmpty() ? Optional.empty() : Optional.of(datos);
    }

    private boolean existeNombre(String nombre) {
        for (Proceso p : procesosCargados) {
            if (p.getNombre().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private void sincronizarHistorialesConRegistro(RegistroSimulacion registro) {
        historialesPorEstado.values().forEach(List::clear);
        for (String estado : ESTADOS) {
            historialesPorEstado.put(estado, registro.getHistorialProcesos(estado));
        }
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return RegistroSimulacion.INICIO;
        }

        String estadoNormalizado = estado.trim();
        return switch (estadoNormalizado.toLowerCase()) {
            case "inicio", "listo", "listos" -> RegistroSimulacion.INICIO;
            case "despachar", "despacho" -> RegistroSimulacion.DESPACHAR;
            case "procesador" -> RegistroSimulacion.PROCESADOR;
            case "expiracion de tiempo", "expiracion", "expiracion de", "expiración de tiempo", "expiración" -> RegistroSimulacion.EXPIRACION_TIEMPO;
            case "bloquear" -> RegistroSimulacion.BLOQUEAR;
            case "bloqueado", "bloqueo" -> RegistroSimulacion.BLOQUEADO;
            case "despertar" -> RegistroSimulacion.DESPERTAR;
            case "no ejecutado" -> RegistroSimulacion.NO_EJECUTADO;
            case "salida", "finalizado" -> RegistroSimulacion.FINALIZADO;
            case "finalizacion de particiones", "finalización de particiones" -> RegistroSimulacion.FINALIZACION_PARTICIONES;
            default -> estado;
        };
    }
}
