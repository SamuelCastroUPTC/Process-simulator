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
import java.util.stream.Collectors;

public class SimuladorPresenter implements IPresenter {

    private final IView view;
    private final MotorSimulacion motorSimulacion;
    private final List<Particion> particionesMemoria;

    private final List<Proceso> procesosCargados = new ArrayList<>();
    private final Map<String, List<Proceso>> historialesPorEstado = new LinkedHashMap<>();
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
        RegistroSimulacion.FINALIZADO
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
        int tamanioMemoria,
        boolean pasaPorBloqueado,
        Particion particionSeleccionada
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
        nuevo.setParticion(particionSeleccionada);
        if (particionSeleccionada != null) {
            particionSeleccionada.agregarProceso(nuevo);
        }
        procesosCargados.add(nuevo);

        // Al cargar exitosamente, se registra inmediatamente en Listo.
        historialesPorEstado.computeIfAbsent(RegistroSimulacion.INICIO, key -> new ArrayList<>())
            .add(nuevo.copiar());

        view.actualizarTablaCargados(new ArrayList<>(procesosCargados));
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
        view.limpiarFormularioCarga();
        view.mostrarExito("Proceso '" + nombre + "' cargado correctamente.");
    }

    @Override
    public void agregarParticion(String nombre, int tamano) {
        if (nombre == null || nombre.isBlank()) {
            view.mostrarError("El nombre de la partición es obligatorio.");
            return;
        }
        if (tamano <= 0) {
            view.mostrarError("El tamaño de la partición debe ser mayor a 0.");
            return;
        }
        boolean nombreDuplicado = particionesMemoria.stream()
            .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre.trim()));
        if (nombreDuplicado) {
            view.mostrarError("Ya existe una partición con ese nombre.");
            return;
        }

        int idSecuencial = particionesMemoria.size() + 1;
        particionesMemoria.add(new Particion(idSecuencial, nombre.trim(), tamano));
        view.actualizarTablaParticiones(new ArrayList<>(particionesMemoria));
        view.mostrarExito("Partición '" + nombre.trim() + "' creada correctamente.");
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

        ultimoRegistro = motorSimulacion.ejecutar(procesosOrdenados);
        sincronizarHistorialesConRegistro(ultimoRegistro);

        procesosCargados.clear();
        particionesMemoria.forEach(Particion::liberarProceso);
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
        String tamanioMemoriaStr = view.getTamanioMemoria();
        Particion particionSeleccionada = view.getParticionSeleccionada();
        boolean pasaPorBloqueado = view.isPasaPorBloqueado();

        if (nombre.isBlank() || tiempoStr.isBlank() || tamanioMemoriaStr.isBlank()) {
            view.mostrarError("Por favor, complete todos los campos obligatorios");
            return;
        }

        if (!tiempoStr.matches("-?\\d+")) {
            view.mostrarError("El tiempo debe ser un numero entero valido");
            return;
        }

        long tiempoSegundos;
        try {
            tiempoSegundos = Long.parseLong(tiempoStr);
        } catch (NumberFormatException ex) {
            view.mostrarError("El tiempo ingresado no es valido o es demasiado largo.");
            return;
        }

        if (tiempoSegundos <= 0) {
            view.mostrarError("El tiempo de ejecucion debe ser mayor a 0");
            return;
        }

        long tiempo;
        try {
            tiempo = Math.multiplyExact(tiempoSegundos, 1000L);
        } catch (ArithmeticException ex) {
            view.mostrarError("El tiempo ingresado no es valido o es demasiado largo.");
            return;
        }

        if (!tamanioMemoriaStr.matches("\\d+")) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        int tamanioMemoria;
        try {
            tamanioMemoria = Integer.parseInt(tamanioMemoriaStr);
        } catch (NumberFormatException ex) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        if (tamanioMemoria <= 0) {
            view.mostrarError("El tamano de memoria debe ser un numero entero mayor a 0");
            return;
        }

        if (particionSeleccionada != null && tamanioMemoria > particionSeleccionada.getTamanoTotal()) {
            view.mostrarError("El tamaño de memoria del proceso no puede superar el tamaño total de la partición seleccionada.");
            return;
        }

        agregarProceso(
            nombre,
            tiempo,
            tamanioMemoria,
            pasaPorBloqueado,
            particionSeleccionada
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
    public void onVerHistorial(String estado) {
        String estadoCanonico = normalizarEstado(estado);
        List<Proceso> datos = historialesPorEstado.getOrDefault(estadoCanonico, List.of());
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
            List<Proceso> origen = registro.getHistorialProcesos(estado);
            List<Proceso> copias = new ArrayList<>(origen.size());
            for (Proceso proceso : origen) {
                copias.add(proceso.copiar());
            }
            historialesPorEstado.put(estado, copias);
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
            default -> estado;
        };
    }
}
