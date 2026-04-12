package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainView implements IView {

    private final Stage stage;
    private FormProcces   formularioModal;
    private FormParticion formularioParticion;

    // ── CAMBIO 1: nueva variable de clase para la tabla de particiones ────────
    private TableView<Proceso>    tablaCargados;
    private TableView<Particion>  tablaParticiones;   // <-- NUEVO

    private Button btnIniciar;
    private Label  lblEstadoSim;

    private final Map<String, HistorialView> ventanasHistorial = new HashMap<>();
    private Object presenter;

    private double dragOffsetX, dragOffsetY;

    private static final String[] ESTADOS_HISTORIAL = {
        RegistroSimulacion.INICIO, "Despachar", "Procesador",
        "Expiracion de tiempo", "Bloquear", "Bloqueado",
        "Despertar", RegistroSimulacion.NO_EJECUTADO, "Salida"
    };

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainView(Stage stage) {
        this.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Simulador de Procesos");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
    }

    public void setPresenter(Object presenter) {
        this.presenter = presenter;
        asegurarFormularioModal();
        asegurarFormularioParticion();
    }

    public void mostrar() {
        VBox root = new VBox();
        root.getStyleClass().add("ventana-principal");

        HBox     barra  = construirBarraTitulo();
        GridPane cuerpo = construirCuerpo();
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        root.getChildren().addAll(barra, cuerpo);

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#F7F3EF"));

        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaxHeight(bounds.getHeight());

        stage.setScene(scene);
        stage.show();
    }

    // ══════════════════ BARRA DE TITULO ══════════════════════════════════════

    private HBox construirBarraTitulo() {
        Label lblTitulo = new Label("Simulador de Procesos");
        lblTitulo.getStyleClass().add("titulo-app");

        Label lblSub = new Label("Sistema Operativo - Arquitectura MVP");
        lblSub.getStyleClass().add("subtitulo-app");

        VBox titulos = new VBox(2, lblTitulo, lblSub);
        titulos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titulos, Priority.ALWAYS);

        Button btnManual = new Button("Manual de Usuario");
        btnManual.getStyleClass().add("btn-salir");
        btnManual.setOnAction(e -> abrirManualUsuario());

        Button btnSalir = new Button("Salir del Programa");
        btnSalir.getStyleClass().add("btn-salir");
        btnSalir.setOnAction(e -> {
            ventanasHistorial.values().forEach(HistorialView::cerrar);
            stage.close();
        });

        HBox acciones = new HBox(10, btnManual, btnSalir);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        HBox barra = new HBox(titulos, acciones);
        barra.getStyleClass().add("barra-titulo");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(14, 24, 14, 28));

        barra.setOnMousePressed(e -> { dragOffsetX = e.getSceneX(); dragOffsetY = e.getSceneY(); });
        barra.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        return barra;
    }

    // ══════════════════ CUERPO — 4 CUADRANTES ════════════════════════════════

    private GridPane construirCuerpo() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(24, 28, 28, 28));
        grid.setStyle("-fx-background-color: #F7F3EF;");

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setPercentWidth(25);
        cc0.setHgrow(Priority.ALWAYS);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(75);
        cc1.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(cc0, cc1);

        RowConstraints rc0 = new RowConstraints();
        rc0.setPercentHeight(50);
        rc0.setVgrow(Priority.ALWAYS);

        RowConstraints rc1 = new RowConstraints();
        rc1.setPercentHeight(50);
        rc1.setVgrow(Priority.ALWAYS);

        grid.getRowConstraints().addAll(rc0, rc1);

        VBox supIzq = construirCuadranteCrear();
        grid.add(supIzq, 0, 0);
        GridPane.setFillWidth(supIzq,  true);
        GridPane.setFillHeight(supIzq, true);

        // CAMBIO 2: ahora devuelve el card con TabPane dentro
        VBox supDer = construirCuadranteTabla();
        grid.add(supDer, 1, 0);
        GridPane.setFillWidth(supDer,  true);
        GridPane.setFillHeight(supDer, true);

        VBox infIzq = construirCuadranteIniciar();
        grid.add(infIzq, 0, 1);
        GridPane.setFillWidth(infIzq,  true);
        GridPane.setFillHeight(infIzq, true);

        VBox infDer = construirCuadranteHistoriales();
        grid.add(infDer, 1, 1);
        GridPane.setFillWidth(infDer,  true);
        GridPane.setFillHeight(infDer, true);

        return grid;
    }

    // ══════════════════ SUP-IZQ: Crear proceso ════════════════════════════════

    private VBox construirCuadranteCrear() {
        Label lblTitulo = new Label("Gestion de Procesos");
        lblTitulo.getStyleClass().add("card-titulo");

        Label lblDesc = new Label(
            "Haga clic en el boton para abrir el formulario y agregar " +
            "un nuevo proceso a la cola de simulacion."
        );
        lblDesc.getStyleClass().add("card-subtitulo");
        lblDesc.setWrapText(true);

        Button btnCrear = new Button("Crear nuevo proceso");
        aplicarEstiloBtnCrear(btnCrear, false);
        btnCrear.setMaxWidth(Double.MAX_VALUE);
        btnCrear.setOnAction(e -> abrirFormularioSeguro());
        btnCrear.setOnMouseEntered(e -> aplicarEstiloBtnCrear(btnCrear, true));
        btnCrear.setOnMouseExited (e -> aplicarEstiloBtnCrear(btnCrear, false));

        Button btnCrearParticiones = new Button("Crear Particiones");
        aplicarEstiloBtnCrear(btnCrearParticiones, false);
        btnCrearParticiones.setMaxWidth(Double.MAX_VALUE);
        btnCrearParticiones.setOnAction(e -> abrirFormularioParticionSeguro());
        btnCrearParticiones.setOnMouseEntered(e -> aplicarEstiloBtnCrear(btnCrearParticiones, true));
        btnCrearParticiones.setOnMouseExited (e -> aplicarEstiloBtnCrear(btnCrearParticiones, false));

        Label lblHint = new Label(
            "Los procesos se agregan a la tabla de la derecha antes de iniciar la simulacion."
        );
        lblHint.setWrapText(true);
        lblHint.setStyle("-fx-text-fill: #9AAFB5; -fx-font-size: 12px; -fx-font-style: italic;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox contenido = new VBox(12, lblDesc, spacer, btnCrear, btnCrearParticiones,
                                  new Separator(), lblHint);
        VBox card = new VBox(10, lblTitulo, contenido);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        return card;
    }

    private void aplicarEstiloBtnCrear(Button btn, boolean hover) {
        String bg     = hover ? "#6A8F98" : "#7B9EA6";
        String shadow = hover
            ? "dropshadow(gaussian, rgba(123,158,166,0.55), 16, 0, 0, 5)"
            : "dropshadow(gaussian, rgba(123,158,166,0.40), 12, 0, 0, 4)";
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 14 24 14 24;" +
            "-fx-cursor: hand;" +
            "-fx-effect: " + shadow + ";"
        );
    }

    // ══════════════════ SUP-DER: TabPane con dos pestañas ════════════════════
    // CAMBIO PRINCIPAL: reemplaza la tabla directa por un TabPane

    private VBox construirCuadranteTabla() {
        Label lblTitulo = new Label("Cola de Procesos y Particiones");
        lblTitulo.getStyleClass().add("card-titulo");

        Label lblSub = new Label("Consulte los procesos cargados y el estado actual de la memoria");
        lblSub.getStyleClass().add("card-subtitulo");

        // ── Pestaña 1: Procesos Cargados ──────────────────────────────────────
        tablaCargados = construirTablaProcesos();
        tablaCargados.setMaxHeight(Double.MAX_VALUE);

        // Envuelve la tabla en un VBox para que el padding interno quede limpio
        VBox contenedorProcesos = new VBox(tablaCargados);
        VBox.setVgrow(tablaCargados, Priority.ALWAYS);
        contenedorProcesos.setPadding(new Insets(10, 0, 0, 0));

        Tab tabProcesos = new Tab("Procesos Cargados", contenedorProcesos);
        tabProcesos.setClosable(false);

        // ── Pestaña 2: Estado de Particiones ──────────────────────────────────
        tablaParticiones = construirTablaParticiones();
        tablaParticiones.setMaxHeight(Double.MAX_VALUE);

        VBox contenedorParticiones = new VBox(tablaParticiones);
        VBox.setVgrow(tablaParticiones, Priority.ALWAYS);
        contenedorParticiones.setPadding(new Insets(10, 0, 0, 0));

        Tab tabParticiones = new Tab("Estado de Particiones", contenedorParticiones);
        tabParticiones.setClosable(false);

        // ── TabPane ───────────────────────────────────────────────────────────
        TabPane tabPane = new TabPane(tabProcesos, tabParticiones);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setMaxHeight(Double.MAX_VALUE);
        tabPane.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Ocultar el fondo del TabPane para respetar el color de la tarjeta (#F7F3EF)
        tabPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-tab-min-height: 32px;"
        );

        // ── Card contenedor ───────────────────────────────────────────────────
        VBox card = new VBox(10, lblTitulo, lblSub, new Separator(), tabPane);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        return card;
    }

    // ══════════════════ TABLA DE PROCESOS ════════════════════════════════════

    @SuppressWarnings("unchecked")
    private TableView<Proceso> construirTablaProcesos() {
        TableView<Proceso> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Sin procesos. Presione 'Crear nuevo proceso'."));

        TableColumn<Proceso, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(120);
        colNombre.setMinWidth(120);
        colNombre.setMaxWidth(Double.MAX_VALUE);   // absorbe el espacio sobrante

        TableColumn<Proceso, Long> colTiempo = new TableColumn<>("Tiempo (s)");
        colTiempo.setCellValueFactory(new PropertyValueFactory<>("tiempo"));
        colTiempo.setPrefWidth(100);
        colTiempo.setMinWidth(100);
        colTiempo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null
                    : String.valueOf(item / 1000L));
            }
        });

        TableColumn<Proceso, Integer> colMemoria = new TableColumn<>("Memoria (u)");
        colMemoria.setCellValueFactory(new PropertyValueFactory<>("tamanioMemoria"));
        colMemoria.setPrefWidth(110);
        colMemoria.setMinWidth(110);

        TableColumn<Proceso, String> colParticion = new TableColumn<>("Particion");
        colParticion.setCellValueFactory(cell -> {
            Proceso p = cell.getValue();
            Particion part = (p != null) ? p.getParticion() : null;
            return new javafx.beans.property.SimpleStringProperty(
                part != null ? part.toString() : "Sin asignar"
            );
        });
        colParticion.setPrefWidth(160);
        colParticion.setMinWidth(160);

        TableColumn<Proceso, Boolean> colBloq = boolCol("Bloqueable", "pasaPorBloqueado", 110);

        TableColumn<Proceso, Void> colEditar = accionCol("Editar", "#7B9EA6", proceso ->
            mostrarAviso("Edicion disponible en la siguiente iteracion. Proceso: " + proceso.getNombre())
        );
        colEditar.setPrefWidth(90); colEditar.setMinWidth(90); colEditar.setMaxWidth(110);

        TableColumn<Proceso, Void> colEliminar = accionCol("Eliminar", "#E8A598",
            this::notificarEliminarProceso);
        colEliminar.setPrefWidth(95); colEliminar.setMinWidth(95); colEliminar.setMaxWidth(115);

        tv.getColumns().addAll(
            colNombre, colTiempo, colMemoria, colParticion,
            colBloq, colEditar, colEliminar
        );
        return tv;
    }

    // ══════════════════ TABLA DE PARTICIONES (NUEVA) ══════════════════════════
    // CAMBIO 3: nuevo método

    @SuppressWarnings("unchecked")
    private TableView<Particion> construirTablaParticiones() {
        TableView<Particion> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No hay particiones configuradas."));

        // Nombre — absorbe el espacio sobrante igual que en la tabla de procesos
        TableColumn<Particion, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(160);
        colNombre.setMinWidth(120);
        colNombre.setMaxWidth(Double.MAX_VALUE);

        // Espacio Total
        TableColumn<Particion, Integer> colTotal = new TableColumn<>("Espacio Total (u)");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("tamanoTotal"));
        colTotal.setPrefWidth(160);
        colTotal.setMinWidth(140);

        // Espacio Disponible — resaltado en verde/rojo segun ocupacion
        TableColumn<Particion, Integer> colDisponible = new TableColumn<>("Espacio Disponible (u)");
        colDisponible.setCellValueFactory(new PropertyValueFactory<>("espacioDisponible"));
        colDisponible.setPrefWidth(175);
        colDisponible.setMinWidth(155);
        colDisponible.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(item));
                // Verde si hay espacio, coral si esta llena
                setStyle(item > 0
                    ? "-fx-text-fill: #5A8550; -fx-font-weight: bold;"
                    : "-fx-text-fill: #C0504D; -fx-font-weight: bold;"
                );
            }
        });

        tv.getColumns().addAll(colNombre, colTotal, colDisponible);
        return tv;
    }

    // ══════════════════ INF-IZQ: Control de simulacion ════════════════════════

    private VBox construirCuadranteIniciar() {
        Label lblTitulo = new Label("Control de Simulacion");
        lblTitulo.getStyleClass().add("card-titulo");

        lblEstadoSim = new Label("En espera - Cargue procesos para comenzar");
        lblEstadoSim.getStyleClass().add("label-estado");
        lblEstadoSim.setWrapText(true);
        lblEstadoSim.setMaxWidth(Double.MAX_VALUE);

        HBox panelEstado = new HBox(lblEstadoSim);
        panelEstado.getStyleClass().add("estado-simulacion-panel");
        panelEstado.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblEstadoSim, Priority.ALWAYS);

        Label lblDesc = new Label(
            "Una vez que haya cargado todos los procesos necesarios, " +
            "inicie la simulacion para ejecutar el ciclo de vida."
        );
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #7A7A7A; -fx-font-size: 13px;");

        btnIniciar = new Button("Iniciar Simulacion");
        btnIniciar.getStyleClass().add("btn-iniciar");
        btnIniciar.setMaxWidth(Double.MAX_VALUE);
        btnIniciar.setDisable(true);
        btnIniciar.setOnAction(e -> notificarIniciarSimulacion());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(14, lblTitulo, new Separator(), panelEstado, lblDesc, spacer, btnIniciar);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    // ══════════════════ INF-DER: 9 botones de historial ══════════════════════

    private VBox construirCuadranteHistoriales() {
        Label lblTitulo = new Label("Historial de Estados");
        lblTitulo.getStyleClass().add("card-titulo");

        Label lblSub = new Label("Consulte el historial de cada estado de la simulacion");
        lblSub.getStyleClass().add("card-subtitulo");

        GridPane grid = construirGridHistoriales();
        VBox.setVgrow(grid, Priority.ALWAYS);

        VBox card = new VBox(12, lblTitulo, lblSub, new Separator(), grid);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(grid, Priority.ALWAYS);

        return card;
    }

    private GridPane construirGridHistoriales() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setMaxHeight(Double.MAX_VALUE);

        final int COLS = 3;
        for (int c = 0; c < COLS; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLS);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 3; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 3.0);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        for (int i = 0; i < ESTADOS_HISTORIAL.length; i++) {
            grid.add(crearBtnHistorial(ESTADOS_HISTORIAL[i]), i % COLS, i / COLS);
        }
        return grid;
    }

    private Button crearBtnHistorial(String nombre) {
        Button btn = new Button(nombre);
        btn.getStyleClass().add("btn-historial");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMaxHeight(Double.MAX_VALUE);
        btn.setWrapText(true);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        btn.setOnAction(e -> notificarVerHistorial(nombre));
        GridPane.setHgrow(btn, Priority.ALWAYS);
        GridPane.setFillWidth(btn,  true);
        GridPane.setFillHeight(btn, true);
        return btn;
    }

    // ══════════════════ HELPERS DE COLUMNAS ══════════════════════════════════

    /** Columna booleana que muestra "Si" (verde) / "No" (gris). */
    @SuppressWarnings("unchecked")
    private TableColumn<Proceso, Boolean> boolCol(String header, String property, double width) {
        TableColumn<Proceso, Boolean> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        col.setMinWidth(width);
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item ? "Si" : "No");
                b.setStyle(item
                    ? "-fx-text-fill: #5A8550; -fx-font-weight: bold;"
                    : "-fx-text-fill: #BBBBBB; -fx-font-weight: bold;");
                setGraphic(b); setText(null);
            }
        });
        return col;
    }

    /**
     * Columna de accion con un boton de texto (sin iconos).
     * El color alterna entre relleno y borde segun hover.
     */
    @SuppressWarnings("unchecked")
    private TableColumn<Proceso, Void> accionCol(String etiqueta, String colorHex,
                                                  java.util.function.Consumer<Proceso> accion) {
        TableColumn<Proceso, Void> col = new TableColumn<>(etiqueta);
        col.setResizable(false);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button(etiqueta);
            {
                estiloAccion(btn, colorHex, false);
                btn.setOnMouseEntered(e -> estiloAccion(btn, colorHex, true));
                btn.setOnMouseExited (e -> estiloAccion(btn, colorHex, false));
                btn.setOnAction(e -> {
                    if (!isEmpty()) accion.accept(getTableView().getItems().get(getIndex()));
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        return col;
    }

    private void estiloAccion(Button btn, String colorHex, boolean hover) {
        btn.setStyle(
            "-fx-background-color: " + (hover ? colorHex : "transparent") + ";" +
            "-fx-text-fill: "        + (hover ? "white"  : colorHex)      + ";" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;" +
            "-fx-padding: 3 8 3 8;" +
            "-fx-border-color: " + colorHex + "; -fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
    }

    // ══════════════════ NOTIFICACIONES AL PRESENTADOR ═════════════════════════

    private void notificarCargarProceso() {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p)
            p.onCargarProceso();
    }

    private void notificarIniciarSimulacion() {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p)
            p.onIniciarSimulacion();
    }

    private void notificarVerHistorial(String estado) {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p)
            p.onVerHistorial(estado);
    }

    private void notificarEliminarProceso(Proceso proceso) {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p)
            p.onEliminarProceso(proceso);
    }

    private void notificarAgregarParticion() {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p
                && formularioParticion != null) {
            p.agregarParticion(
                formularioParticion.getNombre(),
                Integer.parseInt(formularioParticion.getTamano())
            );
            cargarParticionesEnFormulario();
        }
    }

    // ══════════════════ IMPLEMENTACION DE IView ═══════════════════════════════

    @Override public void mostrarError(String msg)  { ModalUtil.error(stage, msg); }
    @Override public void mostrarAviso(String msg)  { ModalUtil.info(stage, "Aviso", msg); }
    @Override public void mostrarExito(String msg)  { ModalUtil.exito(stage, msg); }

    @Override
    public void actualizarTablaCargados(List<Proceso> procesos) {
        tablaCargados.setItems(FXCollections.observableArrayList(procesos));
        btnIniciar.setDisable(procesos.isEmpty());
        lblEstadoSim.setText(procesos.isEmpty()
            ? "En espera - Cargue procesos para comenzar"
            : procesos.size() + " proceso(s) en cola - Listo para simular"
        );
    }

    /**
     * Actualiza la tabla de particiones en la segunda pestana.
     * Llama este metodo desde el Presentador cada vez que cambie
     * el estado de la memoria (alta, baja, compactacion, etc.).
     */
    @Override
    public void actualizarTablaParticiones(List<Particion> particiones) {
        if (tablaParticiones != null) {
            tablaParticiones.setItems(FXCollections.observableArrayList(particiones));
            tablaParticiones.refresh(); // <-- FUERZA EL REPINTADO DE LA TABLA
        }
    }

    @Override public void limpiarFormularioCarga()             { if (formularioModal != null) formularioModal.limpiar(); }
    @Override public void setBtnIniciarHabilitado(boolean h)   { btnIniciar.setDisable(!h); }
    @Override public void actualizarEstadoSimulacion(String e) { lblEstadoSim.setText(e); }

    @Override
    public void mostrarHistorial(String estado, List<RegistroSimulacion.SnapshotProceso> datos) {
        ventanasHistorial.computeIfAbsent(estado, HistorialView::new).mostrarConDatos(datos);
    }

    @Override public String   getNombreProceso()           { return formularioModal != null ? formularioModal.getNombre()         : ""; }
    @Override public String   getTiempoProceso()           { return formularioModal != null ? formularioModal.getTiempo()         : ""; }
    @Override public String   getTamanioMemoria()          { return formularioModal != null ? formularioModal.getTamanioMemoria() : ""; }
    @Override public Particion getParticionSeleccionada()  { return formularioModal != null ? formularioModal.getParticionSeleccionada() : null; }
    @Override public boolean  isPasaPorBloqueado()         { return formularioModal != null && formularioModal.isPasaPorBloqueado(); }

    @Override
    public void cerrarModalFormulario() {
        if (formularioModal != null) formularioModal.cerrar();
    }

    // ══════════════════ UTILIDADES INTERNAS ══════════════════════════════════

    private void asegurarFormularioModal() {
        if (formularioModal != null) return;
        formularioModal = new FormProcces(stage);
        formularioModal.setOnCargar(this::notificarCargarProceso);
    }

    private void asegurarFormularioParticion() {
        if (formularioParticion != null) return;
        formularioParticion = new FormParticion(stage);
        formularioParticion.setOnGuardar(this::notificarAgregarParticion);
    }

    private void abrirFormularioSeguro() {
        try {
            asegurarFormularioModal();
            cargarParticionesEnFormulario();
            formularioModal.mostrar();
        } catch (Exception ex) {
            mostrarError("No fue posible abrir el formulario de creacion.");
        }
    }

    private void abrirFormularioParticionSeguro() {
        try {
            asegurarFormularioParticion();
            formularioParticion.mostrar();
        } catch (Exception ex) {
            mostrarError("No fue posible abrir el formulario de particiones.");
        }
    }

    private void cargarParticionesEnFormulario() {
        if (formularioModal == null
                || !(presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p)) return;
        formularioModal.cargarParticiones(p.getParticionesMemoria());
    }

    private void abrirManualUsuario() {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            ModalUtil.error(stage, "No se puede abrir el manual en este sistema.");
            return;
        }
        try (InputStream input = getClass().getResourceAsStream("/Manual de Usuario Procesos.pdf")) {
            if (input == null) {
                ModalUtil.error(stage, "No se encontro el archivo 'Manual de Usuario Procesos.pdf'.");
                return;
            }
            File tmp = File.createTempFile("manual-usuario-procesos-", ".pdf");
            tmp.deleteOnExit();
            Files.copy(input, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Desktop.getDesktop().open(tmp);
        } catch (IOException ex) {
            ModalUtil.error(stage, "No fue posible abrir el manual de usuario.");
        } catch (Exception ex) {
            ModalUtil.error(stage, "Ocurrio un error inesperado al abrir el manual.");
        }
    }

    public Stage getStage() { return stage; }
}