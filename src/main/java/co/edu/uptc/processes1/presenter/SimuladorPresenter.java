package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.view.IView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SimuladorPresenter implements IPresenter {

    private final IView view;
    private final MotorSimulacion motorSimulacion;

    private MemoriaVariable memoriaVariable;
    private final List<Proceso> procesosCargados = new ArrayList<>();
    private final Map<String, List<RegistroSimulacion.SnapshotProceso>> historialesPorEstado = new LinkedHashMap<>();
    private int contadorId = 1;
    private RegistroSimulacion ultimoRegistro = new RegistroSimulacion();

    private static final List<String> ESTADOS = List.of(
    RegistroSimulacion.INICIO,
    RegistroSimulacion.DESPACHAR,
    RegistroSimulacion.PROCESADOR,
    RegistroSimulacion.EXPIRACION_TIEMPO,
    RegistroSimulacion.NO_EJECUTADO,
    RegistroSimulacion.FINALIZADO,
    RegistroSimulacion.HISTORIAL_PARTICIONES,
    RegistroSimulacion.ASIGNACION,
    RegistroSimulacion.LIBERACION,
    RegistroSimulacion.CONDENSACION
);

    public SimuladorPresenter(IView view) {
        this(view, BigInteger.ZERO, new MotorSimulacion());
    }

    public SimuladorPresenter(IView view, BigInteger tamanioTotalMemoria) {
        this(view, tamanioTotalMemoria, new MotorSimulacion());
    }

    public SimuladorPresenter(IView view, BigInteger tamanioTotalMemoria, MotorSimulacion motorSimulacion) {
        this.view = view;
        this.motorSimulacion = motorSimulacion;
        this.memoriaVariable = new MemoriaVariable(tamanioTotalMemoria);
        ESTADOS.forEach(estado -> historialesPorEstado.put(estado, new ArrayList<>()));
    }

    @Override
    public void agregarProceso(String nombre, BigInteger tiempo, BigInteger tamanioMemoria) {
        if (existeNombre(nombre)) {
            view.mostrarError("El nombre del proceso ya existe");
            return;
        }

        Proceso nuevo = new Proceso(
            contadorId++,
            nombre,
            tiempo,
            tamanioMemoria
        );

        procesosCargados.add(nuevo);
    this.memoriaVariable = recalcularMemoria();

        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarEstadoMemoria(memoriaVariable);
        view.limpiarFormularioCarga();
        view.mostrarExito("Proceso '" + nombre + "', cargado correctamente.");
    }

    @Override
    public RegistroSimulacion iniciarSimulacion() {
        if (procesosCargados.isEmpty()) {
            view.mostrarError("No hay procesos en la cola. Cargue al menos uno antes de simular.");
            return ultimoRegistro;
        }

        view.setBtnIniciarHabilitado(false);
        view.actualizarEstadoSimulacion("Simulacion en progreso...");

        List<Proceso> procesosOrdenados = new ArrayList<>(procesosCargados);

        ultimoRegistro = motorSimulacion.ejecutar(procesosOrdenados, memoriaVariable);
        sincronizarHistorialesConRegistro(ultimoRegistro);

        procesosCargados.clear();
    this.memoriaVariable = recalcularMemoria();

        view.actualizarTablaCargados(new ArrayList<>());
        view.actualizarEstadoMemoria(memoriaVariable);
        view.actualizarEstadoSimulacion("Simulacion finalizada.");
        view.mostrarExito("Simulacion completada. Puede revisar el historial por estado.");
        return ultimoRegistro;
    }

    @Override
    public List<Proceso> getProcesosCargados() {
        return Collections.unmodifiableList(procesosCargados);
    }

    @Override
    public MemoriaVariable getMemoriaVariable() {
        return memoriaVariable;
    }

    @Override
    public List<RegistroSimulacion.UsoParticion> getUsoParticiones() {
        return ultimoRegistro.getUsoParticiones();
    }

    @Override
    public void onCargarProceso() {
        String nombre = view.getNombreProceso();
        String tiempoStr = limpiarNumero(view.getTiempoProceso());
        String tamanioMemoriaStr = limpiarNumero(view.getTamanioMemoria());

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
        BigInteger tiempo = tiempoSegundos.multiply(BigInteger.valueOf(1000L));

        if (!tamanioMemoriaStr.matches("\\d+")) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        BigInteger tamanioMemoria = new BigInteger(tamanioMemoriaStr);
        if (tamanioMemoria.signum() <= 0) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        agregarProceso(nombre, tiempo, tamanioMemoria);
    }

    @Override
    public void onIniciarSimulacion() {
        iniciarSimulacion();
    }

    @Override
    public void onEliminarProceso(Proceso proceso) {
        procesosCargados.removeIf(p -> p.getId() == proceso.getId());
        this.memoriaVariable = recalcularMemoria();
        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarEstadoMemoria(memoriaVariable);
    }

    @Override
    public boolean onEditarProceso(Proceso proceso, String tiempoSegundos, String tamanioMemoria) {
        if (proceso == null) {
            view.mostrarError("No se encontró el proceso a editar");
            return false;
        }

        if (tiempoSegundos == null || tiempoSegundos.isBlank() || tamanioMemoria == null || tamanioMemoria.isBlank()) {
            view.mostrarError("Por favor, complete todos los campos obligatorios");
            return false;
        }

        String tiempoLimpio = limpiarNumero(tiempoSegundos);

        if (!tiempoLimpio.matches("-?\\d+")) {
            view.mostrarError("El tiempo debe ser un numero entero valido");
            return false;
        }

        BigInteger tiempo = new BigInteger(tiempoLimpio);
        if (tiempo.signum() <= 0) {
            view.mostrarError("El tiempo de ejecucion debe ser mayor a 0");
            return false;
        }

        String tamanioLimpio = limpiarNumero(tamanioMemoria);
        if (!tamanioLimpio.matches("\\d+")) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return false;
        }

        BigInteger tamanio = new BigInteger(tamanioLimpio);
        if (tamanio.signum() <= 0) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return false;
        }

        proceso.setTiempoRestante(tiempo.multiply(BigInteger.valueOf(1000L)));
        proceso.setTamanioMemoria(tamanio);

        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarEstadoMemoria(memoriaVariable);
        return true;
    }

    @Override
    public void onVerHistorial(String estado) {
        String estadoCanonico = normalizarEstado(estado);
        List<RegistroSimulacion.SnapshotProceso> datos = historialesPorEstado.getOrDefault(estadoCanonico, List.of());
        view.mostrarHistorial(estado, datos);
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

    private MemoriaVariable recalcularMemoria() {
        BigInteger tamanioTotal = procesosCargados.stream()
            .map(Proceso::getTamanioMemoria)
            .reduce(BigInteger.ZERO, BigInteger::add);

        return new MemoriaVariable(tamanioTotal.compareTo(BigInteger.ZERO) == 0
            ? BigInteger.ZERO : tamanioTotal);
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
            case "no ejecutado" -> RegistroSimulacion.NO_EJECUTADO;
            case "salida", "finalizado" -> RegistroSimulacion.FINALIZADO;
            case "finalizacion de particiones", "finalización de particiones", "particiones" -> RegistroSimulacion.HISTORIAL_PARTICIONES;
            default -> estado;
        };
    }

    private String limpiarNumero(String s) {
        return (s == null) ? "" : s.replaceAll("[^\\d]", "").trim();
    }


@Override
public void onVerHistorialMemoria(String evento) {
    List<RegistroSimulacion.SnapshotMemoria> datos = ultimoRegistro.getHistorialMemoria(evento);
    view.mostrarHistorialMemoria(evento, datos);
}

    @Override
    public void onVerHistorialParticiones() {
        view.mostrarHistorialCondensacion(ultimoRegistro.getHistorialParticiones());
    }

    @Override
    public void onVerHistorialCompactacion() {
        view.mostrarHistorialCompactacion(ultimoRegistro.getHistorialMemoria(RegistroSimulacion.CONDENSACION));
    }
}
