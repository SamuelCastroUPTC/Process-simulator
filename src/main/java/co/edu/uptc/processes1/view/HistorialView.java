package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HistorialView — Ventana UNDECORATED que muestra el historial de un estado.
 *
 * Estructura con TabPane:
 *   - Pestaña "Todos"              : todos los procesos del estado.
 *   - Pestañas dinámicas           : una por partición, inyectadas desde fuera.
 *
 * Cambios respecto a la versión anterior:
 *   1. La variable global `TableView tabla` se reemplazó por `TabPane tabPane`.
 *   2. Se creó el método fábrica `crearTablaParaEstado()` que encapsula
 *      toda la lógica de columnas (reutilizable para cada pestaña).
 *   3. `mostrarConDatos()` recibe la lista limpiamente y delega a la pestaña
 *      "Todos"; las pestañas de particiones se inyectan con `agregarTabParticion()`.
 */
public class HistorialView {

    // ── Metadatos por estado ──────────────────────────────────────────────────

    private record MetaEstado(String color, String descripcion) {}

    private static final String ESTADO_FINALIZADO = RegistroSimulacion.FINALIZADO;

    private static final Map<String, MetaEstado> META_ESTADO = Map.ofEntries(
        Map.entry(RegistroSimulacion.INICIO, new MetaEstado("#A8C5A0", "Procesos que iniciaron la simulacion")),
        Map.entry("Despachar",            new MetaEstado("#7B9EA6", "Procesos despachados al procesador")),
        Map.entry("Procesador",           new MetaEstado("#D4B896", "Procesos en ejecucion en el CPU")),
        Map.entry("Expiracion de tiempo", new MetaEstado("#D4A06A", "Procesos que expiraron su quantum")),
        Map.entry("Bloquear",             new MetaEstado("#E8A598", "Procesos enviados al estado bloqueado")),
        Map.entry("Bloqueado",            new MetaEstado("#B8A8C8", "Procesos en estado bloqueado")),
        Map.entry("Despertar",            new MetaEstado("#98C8D4", "Procesos despertados desde el bloqueo")),
        Map.entry(RegistroSimulacion.NO_EJECUTADO, new MetaEstado("#CCCCCC", "Procesos que superan el tamano de su particion o sin particion asignada")),
        Map.entry(ESTADO_FINALIZADO,      new MetaEstado("#AAAAAA", "Procesos que finalizaron su ejecucion")),
        Map.entry(RegistroSimulacion.FINALIZACION_PARTICIONES, new MetaEstado("#AAAAAA", "Procesos finalizados agrupados por particion"))
    );

    // ── Controles principales ─────────────────────────────────────────────────

    private Stage stage;
    private Label lblContador;

    /**
     * CAMBIO 1: reemplaza la antigua variable `TableView<Proceso> tabla`
     * por un TabPane que aloja una pestaña por agrupación.
     */
    private TabPane tabPane;

    /** Referencia a la tabla de la pestaña "Todos", para actualizar sus datos. */
    private TableView<RegistroSimulacion.SnapshotProceso> tablaTodos;

    private final String estado;

    // Para drag sin bordes
    private double dragOffsetX, dragOffsetY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public HistorialView(String estado) {
        this.estado = estado;
        buildUI();
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private void buildUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);

        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        MetaEstado meta = META_ESTADO.getOrDefault(
            estado,
            new MetaEstado("#7B9EA6", "Historial de procesos")
        );

        // ── Barra superior ────────────────────────────────────────────────────
        Label lblTitulo = new Label("Historial - " + estado);
        lblTitulo.getStyleClass().add("historial-titulo");

        Label lblSub = new Label(meta.descripcion());
        lblSub.getStyleClass().add("historial-subtitulo");

        VBox infoTitulo = new VBox(4, lblTitulo, lblSub);
        infoTitulo.setAlignment(Pos.CENTER_LEFT);

        lblContador = new Label("0 procesos");
        lblContador.getStyleClass().add("historial-contador");

        HBox barraTitulo = new HBox(16, infoTitulo);
        barraTitulo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(barraTitulo, Priority.ALWAYS);

        HBox barra = new HBox(barraTitulo, lblContador);
        barra.getStyleClass().add("historial-barra");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(24, 36, 24, 36));

        barra.setOnMousePressed(e -> { dragOffsetX = e.getSceneX(); dragOffsetY = e.getSceneY(); });
        barra.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // ── CAMBIO 1: TabPane en lugar de una sola tabla ──────────────────────
        tablaTodos = crearTablaParaEstado();

        Tab tabTodos = new Tab("Todos", envolverTabla(tablaTodos));
        tabTodos.setClosable(false);

        tabPane = new TabPane(tabTodos);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);   // ocupa todo el espacio disponible

        // ── Footer ────────────────────────────────────────────────────────────
        Button btnVolver = new Button("Volver al Menu Principal");
        btnVolver.getStyleClass().add("btn-volver");
        btnVolver.setOnAction(e -> stage.close());

        HBox footer = new HBox(btnVolver);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(30, 36, 40, 36));
        footer.setStyle("-fx-background-color: #F0F7F9;");

        // ── Layout raiz ───────────────────────────────────────────────────────
        VBox contenidoTabPane = new VBox(tabPane);
        contenidoTabPane.setPadding(new Insets(16, 36, 0, 36));
        contenidoTabPane.setStyle("-fx-background-color: #F0F7F9;");
        VBox.setVgrow(contenidoTabPane, Priority.ALWAYS);

        VBox root = new VBox(barra, contenidoTabPane, footer);
        root.getStyleClass().add("historial-root");
        VBox.setVgrow(contenidoTabPane, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.setFill(Color.WHITE);

        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
    }

    // ── CAMBIO 2: método fábrica de tablas ────────────────────────────────────

    /**
     * Crea y devuelve una instancia nueva de {@code TableView<Proceso>}
     * completamente configurada con las columnas apropiadas para el estado actual.
     *
     * JavaFX exige un objeto TableView distinto por cada Tab, por eso este
     * método es una fábrica: cada invocación produce una tabla independiente.
     *
     * Lógica de columnas:
     *   - Estado "Salida" (finalizado): solo muestra "Nombre".
     *   - Resto de estados:             muestra "Nombre" y "Tiempo (s)".
     */
    private TableView<RegistroSimulacion.SnapshotProceso> crearTablaParaEstado() {
        TableView<RegistroSimulacion.SnapshotProceso> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No hay procesos registrados en este estado."));
        TableColumn<RegistroSimulacion.SnapshotProceso, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombre()));
        tv.getColumns().add(colNombre);

        // Agregar columna "Partición" si es FINALIZACION_PARTICIONES
        if (RegistroSimulacion.FINALIZACION_PARTICIONES.equalsIgnoreCase(estado)) {
            TableColumn<RegistroSimulacion.SnapshotProceso, String> colParticion = new TableColumn<>("Partición");
            colParticion.setCellValueFactory(cell -> {
                String nombreParticion = cell.getValue().nombreParticion();
                String valor = (nombreParticion == null || nombreParticion.isBlank()) ? "Sin asignar" : nombreParticion;
                return new SimpleStringProperty(valor);
            });
            colParticion.setPrefWidth(140);
            tv.getColumns().add(colParticion);
        }

        boolean esFinalizado = ESTADO_FINALIZADO.equalsIgnoreCase(estado)
            || RegistroSimulacion.FINALIZACION_PARTICIONES.equalsIgnoreCase(estado);
        if (!esFinalizado) {
            TableColumn<RegistroSimulacion.SnapshotProceso, BigInteger> colTiempo = new TableColumn<>("Tiempo (s)");
            colTiempo.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().tiempoRestante()));
            colTiempo.setPrefWidth(140);
            colTiempo.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(BigInteger item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item.divide(BigInteger.valueOf(1000L)).toString());
                }
            });
            tv.getColumns().add(colTiempo);
        }
        return tv;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * CAMBIO 3: actualiza la pestaña "Todos" con la lista recibida
     * y refresca el contador de la barra superior.
     *
     * La lógica de las pestañas por partición se maneja por separado
     * mediante {@link #agregarTabParticion(String, List)}.
     *
     * @param datos Lista completa de procesos que pasaron por este estado.
     */
    public void mostrarConDatos(List<RegistroSimulacion.SnapshotProceso> datos) {
        mostrarConDatos(datos, List.of());
    }

    public void mostrarConDatos(List<RegistroSimulacion.SnapshotProceso> datos, List<RegistroSimulacion.UsoParticion> usosParticion) {
        tabPane.getTabs().clear();

        Tab tabTodos = new Tab("Todos");
        TableView<RegistroSimulacion.SnapshotProceso> tablaTodosNueva = crearTablaParaEstado();
        tablaTodosNueva.setItems(FXCollections.observableArrayList(datos));
        tabTodos.setContent(envolverTabla(tablaTodosNueva));
        tabTodos.setClosable(false);
        tabPane.getTabs().add(tabTodos);
        this.tablaTodos = tablaTodosNueva;

        boolean esFinalizacion = ESTADO_FINALIZADO.equalsIgnoreCase(estado)
            || RegistroSimulacion.FINALIZACION_PARTICIONES.equalsIgnoreCase(estado);

        if (esFinalizacion && usosParticion != null && !usosParticion.isEmpty()) {
            Map<String, List<RegistroSimulacion.UsoParticion>> porParticion = usosParticion.stream()
                .collect(Collectors.groupingBy(
                    RegistroSimulacion.UsoParticion::nombreParticion,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

            porParticion.forEach((nombreParticion, usos) -> {
                Tab tabParticion = new Tab(nombreParticion);
                VBox contenidoParticion = construirContenidoParticionFinal(nombreParticion, usos);
                tabParticion.setContent(contenidoParticion);
                tabParticion.setClosable(false);
                tabPane.getTabs().add(tabParticion);
            });
        } else {
            Map<String, List<RegistroSimulacion.SnapshotProceso>> procesosPorParticion = datos.stream()
                .collect(Collectors.groupingBy(
                    proceso -> (proceso == null || proceso.nombreParticion() == null || proceso.nombreParticion().isBlank())
                        ? "Sin asignar"
                        : proceso.nombreParticion(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

            procesosPorParticion.entrySet().stream()
                .filter(entry -> !"Sin asignar".equals(entry.getKey()))
                .forEach(entry -> {
                    String nombreTab = entry.getKey();
                    List<RegistroSimulacion.SnapshotProceso> procesosParticion = entry.getValue();
                    Tab tabParticion = new Tab(nombreTab);
                    TableView<RegistroSimulacion.SnapshotProceso> tablaParticion = crearTablaParaEstado();
                    tablaParticion.setItems(FXCollections.observableArrayList(procesosParticion));
                    tabParticion.setContent(envolverTabla(tablaParticion));
                    tabParticion.setClosable(false);
                    tabPane.getTabs().add(tabParticion);
                });
        }

        int n = datos.size();
        lblContador.setText(n + (n == 1 ? " proceso" : " procesos"));

        stage.show();
        stage.toFront();
    }

    /**
     * Agrega (o actualiza si ya existe) una pestaña dinámica para la partición
     * indicada, mostrando solo los procesos de esa partición.
     *
     * Este método está preparado para que el Presentador lo llame después de
     * {@code mostrarConDatos()}, una vez que haya filtrado los datos por partición.
     *
     * @param nombreParticion Nombre de la partición (se usa como título de la pestaña).
     * @param datos           Procesos de esa partición que pasaron por este estado.
     */
    public void agregarTabParticion(String nombreParticion, List<RegistroSimulacion.SnapshotProceso> datos) {
        // Buscar si ya existe una pestaña con ese nombre
        Tab tabExistente = tabPane.getTabs().stream()
            .filter(t -> nombreParticion.equals(t.getText()))
            .findFirst()
            .orElse(null);

        if (tabExistente != null) {
            // Actualizar la tabla ya existente
            @SuppressWarnings("unchecked")
            TableView<RegistroSimulacion.SnapshotProceso> tablaExistente =
                (TableView<RegistroSimulacion.SnapshotProceso>) ((VBox) tabExistente.getContent()).getChildren().get(0);
            tablaExistente.setItems(FXCollections.observableArrayList(datos));
        } else {
            // Crear nueva pestaña con su propia tabla (fábrica)
            TableView<RegistroSimulacion.SnapshotProceso> nuevaTabla = crearTablaParaEstado();
            nuevaTabla.setItems(FXCollections.observableArrayList(datos));

            Tab nuevaTab = new Tab(nombreParticion, envolverTabla(nuevaTabla));
            nuevaTab.setClosable(false);
            tabPane.getTabs().add(nuevaTab);
        }
    }

    private VBox construirContenidoParticionFinal(String nombreParticion, List<RegistroSimulacion.UsoParticion> usos) {
        Label lblTituloParticion = new Label("Partición: " + nombreParticion);
        lblTituloParticion.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;");

        BigInteger tiempoTotal = BigInteger.ZERO;
        for (RegistroSimulacion.UsoParticion uso : usos) {
            if (uso != null && uso.tiempoCpu() != null) {
                tiempoTotal = tiempoTotal.add(uso.tiempoCpu());
            }
        }
        Label lblTotal = new Label("Tiempo total de ejecución: " + tiempoTotal.divide(BigInteger.valueOf(1000L)) + " s");
        lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #7B9EA6;");

        List<String> pasosExpandidos = new ArrayList<>();
        for (RegistroSimulacion.UsoParticion uso : usos) {
            if (uso == null || uso.nombreProceso() == null || uso.nombreProceso().isBlank()) {
                continue;
            }
            pasosExpandidos.add(uso.nombreProceso());
        }

        ListView<String> lista = new ListView<>();
        lista.setPlaceholder(new Label("No hay procesos registrados para esta partición."));
        lista.setItems(FXCollections.observableArrayList(pasosExpandidos));

        VBox contenedor = new VBox(10, lblTituloParticion, lblTotal, lista);
        contenedor.setPadding(new Insets(12, 0, 0, 0));
        VBox.setVgrow(lista, Priority.ALWAYS);
        return contenedor;
    }

    /**
     * Elimina todas las pestañas de particiones, dejando solo "Todos".
     * Útil para limpiar el estado antes de una nueva simulación.
     */
    public void limpiarTabsParticiones() {
        tabPane.getTabs().removeIf(t -> !"Todos".equals(t.getText()));
    }

    public void cerrar() {
        stage.close();
    }

    public Stage getStage() { return stage; }

    // ── Utilidad interna ──────────────────────────────────────────────────────

    /**
     * Envuelve una tabla en un VBox con el padding y el grow correctos,
     * listo para insertar como contenido de un Tab.
     */
    private VBox envolverTabla(TableView<RegistroSimulacion.SnapshotProceso> tabla) {
        VBox wrapper = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setMaxHeight(Double.MAX_VALUE);
        wrapper.setStyle("-fx-background-color: transparent;");
        return wrapper;
    }
}