package co.edu.uptc.processes1.view;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * HistorialView — Ventana UNDECORATED que muestra el historial de un estado.
 *
 * Estructura con TabPane:
 *   - Pestaña "Todos"              : todos los procesos del estado.
 *   - Pestañas dinámicas           : una por partición, inyectadas desde fuera.
 *   - Pestaña fija                 : finalización global de particiones.
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
        Map.entry(RegistroSimulacion.NO_EJECUTADO, new MetaEstado("#CCCCCC", "Procesos que superan el tamano de su particion o sin particion asignada")),
        Map.entry(ESTADO_FINALIZADO,      new MetaEstado("#AAAAAA", "Procesos que finalizaron su ejecucion"))
    );

    // ── Controles principales ─────────────────────────────────────────────────

    private Stage stage;
    private Label lblContador;

    /**
     * CAMBIO 1: reemplaza la antigua variable `TableView<Proceso> tabla`
     * por un TabPane que aloja una pestaña por agrupación.
     */
    private TabPane tabPane;

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
        tabPane = new TabPane();
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
     *   - Expiración de tiempo: muestra "Nombre", "Tiempo (s)" y "Partición".
    *   - Salida: muestra "Nombre", "Tamaño" y "Particiones Usadas".
    *   - Finalización de Particiones: muestra "Partición" y "Tamaño".
     *   - Resto de estados: muestra "Nombre" y "Tiempo (s)".
     */
    private TableView<RegistroSimulacion.SnapshotProceso> crearTablaParaEstado() {
        TableView<RegistroSimulacion.SnapshotProceso> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No hay procesos registrados en este estado."));
        TableColumn<RegistroSimulacion.SnapshotProceso, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombre()));
        tv.getColumns().add(colNombre);

        if (RegistroSimulacion.EXPIRACION_TIEMPO.equalsIgnoreCase(estado)) {
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

            TableColumn<RegistroSimulacion.SnapshotProceso, String> colParticion = new TableColumn<>("Partición");
            colParticion.setCellValueFactory(cell -> {
                String p = cell.getValue().nombreParticion();
                return new SimpleStringProperty((p == null || p.isBlank()) ? "Sin asignar" : p);
            });
            colParticion.setPrefWidth(140);
            tv.getColumns().add(colParticion);
        } else if (ESTADO_FINALIZADO.equalsIgnoreCase(estado)) {

            TableColumn<RegistroSimulacion.SnapshotProceso, String> colTamanio = new TableColumn<>("Tamaño");
            colTamanio.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().tamanioMemoria() != null ? cell.getValue().tamanioMemoria().toString() : "-"));
            tv.getColumns().add(colTamanio);

        } else {
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

        if (RegistroSimulacion.NO_EJECUTADO.equalsIgnoreCase(estado)) {
            TableColumn<RegistroSimulacion.SnapshotProceso, String> colMotivo = new TableColumn<>("Motivo");
            colMotivo.setCellValueFactory(cell -> {
                String motivo = cell.getValue().motivoNoEjecucion();
                return new SimpleStringProperty(motivo != null ? motivo : "Sin especificar");
            });
            colMotivo.setPrefWidth(380);
            colMotivo.setMinWidth(200);
            tv.getColumns().add(colMotivo);
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
        mostrarConDatos(datos, List.of(), List.of());
    }

    public void mostrarConDatos(
        List<RegistroSimulacion.SnapshotProceso> datos,
        List<RegistroSimulacion.UsoParticion> usosParticion) {
    
    tabPane.getTabs().clear();

    // Tab "Todos"
    Tab tabTodos = new Tab("Todos");
    TableView<RegistroSimulacion.SnapshotProceso> tablaTodos = crearTablaParaEstado();
    tablaTodos.setItems(FXCollections.observableArrayList(datos));
    tabTodos.setContent(envolverTabla(tablaTodos));
    tabPane.getTabs().add(tabTodos);

    // Pestañas por partición (solo para estado Finalizado)
    if (ESTADO_FINALIZADO.equalsIgnoreCase(estado)) {
        Map<String, List<RegistroSimulacion.SnapshotProceso>> procesosPorParticion = datos.stream()
            .filter(proceso -> proceso != null
                && proceso.nombreParticion() != null
                && !proceso.nombreParticion().isBlank())
            .collect(Collectors.groupingBy(
                RegistroSimulacion.SnapshotProceso::nombreParticion,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        procesosPorParticion.forEach((nombreParticion, procesosParticion) -> {
            Tab tabParticion = new Tab(nombreParticion);
            VBox contenidoParticion = construirContenidoParticionFinal(
                nombreParticion, procesosParticion, usosParticion);
            tabParticion.setContent(contenidoParticion);
            tabParticion.setClosable(false);
            tabPane.getTabs().add(tabParticion);
        });
    }

    int n = datos.size();
    lblContador.setText(n + (n == 1 ? " proceso" : " procesos"));

    stage.show();
    stage.toFront();
}

    public void mostrarConDatos(
        List<RegistroSimulacion.SnapshotProceso> datos,
        List<RegistroSimulacion.UsoParticion> usosParticion,
        List<RegistroSimulacion.FinalizacionParticionInfo> finalizacionParticiones) {
    
    // Ignoramos finalizacionParticiones aquí porque ahora se maneja en ventana aparte
    mostrarConDatos(datos, usosParticion);
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

    private String formatearTamanio(BigInteger tamanio) {
        return tamanio != null ? tamanio.toString() : "-";
    }

    private String formatearTiempo(BigInteger tiempo) {
        return tiempo != null ? tiempo.divide(BigInteger.valueOf(1000L)).toString() : "-";
    }

    private static final class FilaParticionFinal {
        private final String nombreProceso;
        private final String tamanio;
        private final String tiempoTotal;

        private FilaParticionFinal(String nombreProceso, String tamanio, String tiempoTotal) {
            this.nombreProceso = nombreProceso;
            this.tamanio = tamanio;
            this.tiempoTotal = tiempoTotal;
        }

        private String nombreProceso() { return nombreProceso; }
        private String tamanio() { return tamanio; }
        private String tiempoTotal() { return tiempoTotal; }
    }

    private VBox construirContenidoParticionFinal(
        String nombreParticion,
        List<RegistroSimulacion.SnapshotProceso> procesos,
        List<RegistroSimulacion.UsoParticion> usosParticion) {
    
    Label lblTituloParticion = new Label("Partición: " + nombreParticion);
    lblTituloParticion.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;");

    TableView<FilaParticionFinal> tabla = new TableView<>();
    tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    tabla.setPlaceholder(new Label("No hay procesos registrados para esta partición."));

    TableColumn<FilaParticionFinal, String> colProceso = new TableColumn<>("Proceso");
    colProceso.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombreProceso()));

    TableColumn<FilaParticionFinal, String> colTamanio = new TableColumn<>("Tamaño");
    colTamanio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().tamanio()));

    TableColumn<FilaParticionFinal, String> colTiempo = new TableColumn<>("Tiempo total");
    colTiempo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().tiempoTotal()));

    tabla.getColumns().addAll(colProceso, colTamanio, colTiempo);

    List<FilaParticionFinal> filas = new ArrayList<>();
    for (RegistroSimulacion.SnapshotProceso proceso : procesos) {
        if (proceso == null || proceso.nombreParticion() == null || proceso.nombreParticion().isBlank()) {
            continue;
        }
        
        BigInteger tiempoTotal = BigInteger.ZERO;
        if (usosParticion != null) {
            for (RegistroSimulacion.UsoParticion uso : usosParticion) {
                if (uso == null
                    || uso.nombreParticion() == null
                    || !nombreParticion.equals(uso.nombreParticion())
                    || uso.nombreProceso() == null
                    || !proceso.nombre().equals(uso.nombreProceso())
                    || uso.tiempoCpu() == null) {
                    continue;
                }
                tiempoTotal = tiempoTotal.add(uso.tiempoCpu());
            }
        }

        filas.add(new FilaParticionFinal(
            proceso.nombre(),
            proceso.tamanioMemoria() != null ? formatearTamanio(proceso.tamanioMemoria()) : "-",
            formatearTiempo(tiempoTotal)
        ));
    }

    tabla.setItems(FXCollections.observableArrayList(filas));

    VBox contenedor = new VBox(10, lblTituloParticion, tabla);
    contenedor.setPadding(new Insets(12, 0, 0, 0));
    VBox.setVgrow(tabla, Priority.ALWAYS);
    return contenedor;
}
    

}