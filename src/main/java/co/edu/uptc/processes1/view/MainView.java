package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.model.BloqueMemoria;
import co.edu.uptc.processes1.model.HuecoMemoria;
import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Proceso;
import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainView implements IView {

    private final Stage stage;
    private FormProcces formularioModal;

    private TableView<Proceso> tablaCargados;
    private VBox panelMemoria;

    private Button btnIniciar;
    private Label lblEstadoSim;
    private Stage stageHistoriales;

    private final Map<String, HistorialView> ventanasHistorial = new HashMap<>();
    
    // --- CAMBIO A: Nuevo mapa de ventanas de memoria ---
    private final Map<String, HistorialMemoriaView> ventanasMemoria = new HashMap<>();
    private HistorialParticionesView ventanaParticiones;

    private Object presenter;

    private double dragOffsetX;
    private double dragOffsetY;

    // --- CAMBIO A: Array ESTADOS_HISTORIAL actualizado con los estados nuevos ---
    private static final String[] ESTADOS_HISTORIAL = {
        RegistroSimulacion.INICIO, "Despachar", "Procesador",
        "Expiracion de tiempo", RegistroSimulacion.NO_EJECUTADO, "Finalización",
        "Particiones",
        RegistroSimulacion.ASIGNACION,       // nuevo
        RegistroSimulacion.LIBERACION,       // nuevo
        RegistroSimulacion.CONDENSACION      // nuevo
    };

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
    }

    public void mostrar() {
        VBox root = new VBox();
        root.getStyleClass().add("ventana-principal");

        HBox barra = construirBarraTitulo();
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
            ventanasMemoria.values().forEach(HistorialMemoriaView::cerrar); // Opcional: para limpiar también estas ventanas
            stage.close();
        });

        HBox acciones = new HBox(10, btnManual, btnSalir);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        HBox barra = new HBox(titulos, acciones);
        barra.getStyleClass().add("barra-titulo");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(14, 24, 14, 28));

        barra.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        barra.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        return barra;
    }

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
        rc0.setPercentHeight(100);
        grid.getRowConstraints().add(rc0);

        VBox cardCrear = construirCuadranteCrear();
        VBox cardIniciar = construirCuadranteIniciar();
        VBox columnaIzq = new VBox(20, cardCrear, cardIniciar);
        VBox.setVgrow(cardCrear, Priority.ALWAYS);
        VBox.setVgrow(cardIniciar, Priority.ALWAYS);
        columnaIzq.setMaxHeight(Double.MAX_VALUE);
        grid.add(columnaIzq, 0, 0);
        GridPane.setFillWidth(columnaIzq, true);
        GridPane.setFillHeight(columnaIzq, true);

        VBox supDer = construirCuadranteTabla();
        grid.add(supDer, 1, 0);
        GridPane.setFillWidth(supDer, true);
        GridPane.setFillHeight(supDer, true);

        return grid;
    }

    private VBox construirCuadranteCrear() {
        Label lblTitulo = new Label("Gestion de Procesos");
        lblTitulo.getStyleClass().add("card-titulo");

        Label lblDesc = new Label(
            "Haga clic en el boton para agregar un nuevo proceso a la cola de simulacion."
        );
        lblDesc.getStyleClass().add("card-subtitulo");
        lblDesc.setWrapText(true);

        Button btnCrear = new Button("Crear nuevo proceso");
        aplicarEstiloBtnCrear(btnCrear, false);
        btnCrear.setMaxWidth(Double.MAX_VALUE);
        btnCrear.setOnAction(e -> abrirFormularioSeguro());
        btnCrear.setOnMouseEntered(e -> aplicarEstiloBtnCrear(btnCrear, true));
        btnCrear.setOnMouseExited(e -> aplicarEstiloBtnCrear(btnCrear, false));

        Label lblHint = new Label(
            "Los procesos se agregan a la tabla de la derecha antes de iniciar la simulacion."
        );
        lblHint.setWrapText(true);
        lblHint.setStyle("-fx-text-fill: #9AAFB5; -fx-font-size: 12px; -fx-font-style: italic;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox contenido = new VBox(12, lblDesc, spacer, btnCrear, new Separator(), lblHint);
        VBox card = new VBox(10, lblTitulo, contenido);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        return card;
    }

    private void aplicarEstiloBtnCrear(Button btn, boolean hover) {
        String bg = hover ? "#6A8F98" : "#7B9EA6";
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

    @SuppressWarnings("unchecked")
    private TableView<Proceso> construirTablaProcesos() {
        TableView<Proceso> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Sin procesos. Presione 'Crear nuevo proceso'."));

        TableColumn<Proceso, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(140);
        colNombre.setMinWidth(120);
        colNombre.setMaxWidth(Double.MAX_VALUE);

        TableColumn<Proceso, BigInteger> colTiempo = new TableColumn<>("Tiempo (s)");
        colTiempo.setCellValueFactory(new PropertyValueFactory<>("tiempo"));
        colTiempo.setPrefWidth(110);
        colTiempo.setMinWidth(100);
        colTiempo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigInteger item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.divide(BigInteger.valueOf(1000L)).toString());
            }
        });

        TableColumn<Proceso, BigInteger> colMemoria = new TableColumn<>("Memoria");
        colMemoria.setCellValueFactory(new PropertyValueFactory<>("tamanioMemoria"));
        colMemoria.setPrefWidth(130);
        colMemoria.setMinWidth(110);
        colMemoria.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigInteger item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
            }
        });

        TableColumn<Proceso, Void> colEditar = accionCol("Editar", "#7B9EA6", this::abrirFormularioEdicion);
        colEditar.setPrefWidth(90);
        colEditar.setMinWidth(90);

        TableColumn<Proceso, Void> colEliminar = accionCol("Eliminar", "#E8A598", this::notificarEliminarProceso);
        colEliminar.setPrefWidth(100);
        colEliminar.setMinWidth(95);

        tv.getColumns().addAll(colNombre, colTiempo, colMemoria, colEditar, colEliminar);
        return tv;
    }

    private VBox construirCuadranteTabla() {
        Label lblTitulo = new Label("Cola de Procesos");
        lblTitulo.getStyleClass().add("card-titulo");

        Label lblSub = new Label("Procesos cargados y estado actual de la memoria variable");
        lblSub.getStyleClass().add("card-subtitulo");

        tablaCargados = construirTablaProcesos();
        tablaCargados.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(tablaCargados, Priority.ALWAYS);

        Label lblMemoria = new Label("Estado de Memoria");
        lblMemoria.getStyleClass().add("card-titulo");

        panelMemoria = new VBox(6);
        panelMemoria.setPadding(new Insets(8, 10, 8, 10));
        panelMemoria.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DDD8D3; -fx-border-width: 1;");
        panelMemoria.setMinHeight(220);
        panelMemoria.setPrefHeight(260);
        VBox.setVgrow(panelMemoria, Priority.ALWAYS);

        VBox card = new VBox(10,
            lblTitulo, lblSub, new Separator(),
            tablaCargados,
            new Separator(), lblMemoria,
            panelMemoria
        );
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tablaCargados, Priority.ALWAYS);
        VBox.setVgrow(panelMemoria, Priority.ALWAYS);

        return card;
    }

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
            "Una vez que haya cargado todos los procesos, inicie la simulacion."
        );
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #7A7A7A; -fx-font-size: 13px;");

        btnIniciar = new Button("Iniciar Simulacion");
        btnIniciar.getStyleClass().add("btn-iniciar");
        btnIniciar.setMaxWidth(Double.MAX_VALUE);
        btnIniciar.setDisable(true);
        btnIniciar.setOnAction(e -> notificarIniciarSimulacion());

        Button btnHistoriales = new Button("Ver Historiales");
        btnHistoriales.setMaxWidth(Double.MAX_VALUE);
        btnHistoriales.setStyle(
            "-fx-background-color: #7B9EA6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 14 40 14 40;" +
            "-fx-cursor: hand;"
        );
        btnHistoriales.setOnAction(e -> abrirVentanaHistoriales());

        Region separadorBotones = new Region();
        separadorBotones.setMinHeight(10);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(14, lblTitulo, new Separator(), panelEstado, lblDesc, spacer,
            btnIniciar, separadorBotones, btnHistoriales);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

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
                btn.setOnMouseExited(e -> estiloAccion(btn, colorHex, false));
                btn.setOnAction(e -> {
                    if (!isEmpty()) accion.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        return col;
    }

    private void estiloAccion(Button btn, String colorHex, boolean hover) {
        btn.setStyle(
            "-fx-background-color: " + (hover ? colorHex : "transparent") + ";" +
            "-fx-text-fill: " + (hover ? "white" : colorHex) + ";" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;" +
            "-fx-padding: 3 8 3 8;" +
            "-fx-border-color: " + colorHex + "; -fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
    }

    private void notificarCargarProceso() {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) {
            p.onCargarProceso();
            if (formularioModal != null) {
                formularioModal.setProcesosCargados(p.getProcesosCargados());
            }
            actualizarEstadoMemoria(p.getMemoriaVariable());
        }
    }

    private void notificarIniciarSimulacion() {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) {
            p.onIniciarSimulacion();
            actualizarEstadoMemoria(p.getMemoriaVariable());
        }
    }

    // --- CAMBIO C: Redirigir los eventos de memoria al nuevo método del presenter ---
    private void notificarVerHistorial(String estado) {
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) {
            if ("Particiones".equals(estado)) {
                p.onVerHistorialParticiones();
                return;
            }
            if ("Finalización".equals(estado) || RegistroSimulacion.FINALIZADO.equalsIgnoreCase(estado)) {
                p.onVerHistorial(RegistroSimulacion.FINALIZADO);
                return;
            }
            if (RegistroSimulacion.ASIGNACION.equals(estado)
                    || RegistroSimulacion.LIBERACION.equals(estado)
                    || RegistroSimulacion.CONDENSACION.equals(estado)) {
                p.onVerHistorialMemoria(estado);
            } else {
                p.onVerHistorial(estado);
            }
        }
    }

    private void notificarEliminarProceso(Proceso proceso) {
        if (!(presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) || proceso == null) {
            return;
        }
        boolean primera = ModalUtil.confirmar(stage,
            "¿Seguro que quiere eliminar el proceso " + proceso.getNombre() + "?");
        if (!primera) {
            return;
        }

        boolean segunda = ModalUtil.confirmar(stage,
            "¿Realmente está seguro de eliminar el proceso " + proceso.getNombre() + "? Esta acción no se puede deshacer.");
        if (!segunda) {
            return;
        }

        p.onEliminarProceso(proceso);
        actualizarEstadoMemoria(p.getMemoriaVariable());
        mostrarExito("Proceso eliminado correctamente");
    }

    private void abrirFormularioEdicion(Proceso proceso) {
        if (proceso == null) {
            return;
        }
        try {
            asegurarFormularioModal();
            if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) {
                formularioModal.setProcesosCargados(p.getProcesosCargados());
            }
            formularioModal.setModoEdicion(true);
            formularioModal.cargarProcesoParaEdicion(proceso);
            formularioModal.setOnCargar(() -> notificarEditarProceso(proceso));
            formularioModal.mostrar(false);
        } catch (Exception ex) {
            mostrarError("No fue posible abrir el formulario de edicion.");
        } finally {
            if (formularioModal != null) {
                formularioModal.setModoEdicion(false);
                formularioModal.setOnCargar(this::notificarCargarProceso);
            }
        }
    }

    private void notificarEditarProceso(Proceso proceso) {
        if (!(presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) || proceso == null) {
            return;
        }

        boolean actualizado = p.onEditarProceso(
            proceso,
            formularioModal.getTiempo(),
            formularioModal.getTamanioMemoria()
        );

        if (actualizado) {
            mostrarExito("Proceso editado correctamente");
            formularioModal.cerrar();
        }
    }

    @Override
    public void mostrarError(String msg) {
        ModalUtil.error(stage, msg);
    }

    @Override
    public void mostrarAviso(String msg) {
        ModalUtil.info(stage, "Aviso", msg);
    }

    @Override
    public void mostrarExito(String msg) {
        ModalUtil.exito(stage, msg);
    }

    @Override
    public void actualizarTablaCargados(List<Proceso> procesos) {
        tablaCargados.setItems(FXCollections.observableArrayList(procesos));
        btnIniciar.setDisable(procesos.isEmpty());
        lblEstadoSim.setText(procesos.isEmpty()
            ? "En espera - Cargue procesos para comenzar"
            : procesos.size() + " proceso(s) en cola - Listo para simular"
        );
    }

    @Override
    public void actualizarEstadoMemoria(MemoriaVariable memoria) {
        panelMemoria.getChildren().clear();
        if (memoria == null) {
            panelMemoria.getChildren().add(new Label("Memoria no disponible."));
            return;
        }

        Label resumen = new Label(
            "Total: " + memoria.getTamanioTotal() +
            " | Libre: " + memoria.getEspacioLibreTotal() +
            " | Ocupado: " + memoria.getEspacioOcupado()
        );
        resumen.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A7A7A; -fx-font-weight: bold;");
        panelMemoria.getChildren().add(resumen);

        BigInteger total = memoria.getTamanioTotal();
        double anchoBase = 720.0;

        for (BloqueMemoria bloque : memoria.getBloquesOcupados()) {
            double proporcion = total.signum() == 0
                ? 0.0
                : bloque.getTamanio().doubleValue() / total.doubleValue();
            double ancho = Math.max(40.0, anchoBase * proporcion);

            Label l = new Label("Proceso " + bloque.getNombreProceso() +
                " | tam=" + bloque.getTamanio() +
                " | dir=" + bloque.getDireccionInicio() + "-" + bloque.getDireccionFin());
            l.setMinHeight(28);
            l.setPrefHeight(28);
            l.setPrefWidth(ancho);
            l.setStyle("-fx-background-color: #7B9EA6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8 4 8;");
            panelMemoria.getChildren().add(l);
        }

        for (HuecoMemoria hueco : memoria.getHuecos()) {
            double proporcion = total.signum() == 0
                ? 0.0
                : hueco.getTamanio().doubleValue() / total.doubleValue();
            double ancho = Math.max(40.0, anchoBase * proporcion);

            Label l = new Label("Hueco | tam=" + hueco.getTamanio() +
                " | dir=" + hueco.getDireccionInicio() + "-" + hueco.getDireccionFin());
            l.setMinHeight(24);
            l.setPrefHeight(24);
            l.setPrefWidth(ancho);
            l.setStyle("-fx-background-color: #A8C5A0; -fx-text-fill: #2E4A2A; -fx-font-size: 11px; -fx-padding: 3 8 3 8;");
            panelMemoria.getChildren().add(l);
        }

        if (memoria.getBloquesOcupados().isEmpty() && memoria.getHuecos().isEmpty()) {
            panelMemoria.getChildren().add(new Label("Sin bloques para mostrar."));
        }
    }

    @Override
    public void setBtnIniciarHabilitado(boolean habilitado) {
        btnIniciar.setDisable(!habilitado);
    }

    @Override
    public void actualizarEstadoSimulacion(String estado) {
        lblEstadoSim.setText(estado);
    }

    @Override
    public void mostrarHistorial(String estado, List<RegistroSimulacion.SnapshotProceso> datos) {
        HistorialView historialView = ventanasHistorial.computeIfAbsent(estado, HistorialView::new);
        if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p
                && esEstadoFinalizacion(estado)) {
            historialView.mostrarConDatos(datos, p.getUsoParticiones());
            return;
        }
        historialView.mostrarConDatos(datos);
    }

    // --- CAMBIO B: Implementación del método de IView ---
    @Override
    public void mostrarHistorialMemoria(String evento, List<RegistroSimulacion.SnapshotMemoria> datos) {
        HistorialMemoriaView ventana = ventanasMemoria.computeIfAbsent(evento, HistorialMemoriaView::new);
        ventana.mostrarConDatos(datos);
    }

    @Override
    public void mostrarHistorialParticiones(List<RegistroSimulacion.SnapshotParticion> datos) {
        if (ventanaParticiones == null) {
            ventanaParticiones = new HistorialParticionesView();
        }
        ventanaParticiones.mostrarConDatos(datos);
    }

    private boolean esEstadoFinalizacion(String estado) {
        return RegistroSimulacion.FINALIZADO.equalsIgnoreCase(estado)
            || RegistroSimulacion.HISTORIAL_PARTICIONES.equalsIgnoreCase(estado);
    }

    @Override
    public String getNombreProceso() {
        return formularioModal != null ? formularioModal.getNombre() : "";
    }

    @Override
    public String getTiempoProceso() {
        return formularioModal != null ? formularioModal.getTiempo().replace(".", "").trim() : "";
    }

    @Override
    public String getTamanioMemoria() {
        return formularioModal != null ? formularioModal.getTamanioMemoria() : "";
    }

    @Override
    public void limpiarFormularioCarga() {
        if (formularioModal != null) formularioModal.limpiar();
    }

    @Override
    public void cerrarModalFormulario() {
        if (formularioModal != null) formularioModal.cerrar();
    }

    private void asegurarFormularioModal() {
        if (formularioModal != null) return;
        formularioModal = new FormProcces(stage);
        formularioModal.setOnCargar(this::notificarCargarProceso);
    }

    private void abrirFormularioSeguro() {
        try {
            asegurarFormularioModal();
            if (presenter instanceof co.edu.uptc.processes1.presenter.IPresenter p) {
                formularioModal.setProcesosCargados(p.getProcesosCargados());
            }
            formularioModal.setModoEdicion(false);
            formularioModal.mostrar();
        } catch (Exception ex) {
            mostrarError("No fue posible abrir el formulario de creacion.");
        }
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

    private void abrirVentanaHistoriales() {
        if (stageHistoriales != null && stageHistoriales.isShowing()) {
            stageHistoriales.toFront();
            return;
        }
        stageHistoriales = new Stage();
        stageHistoriales.initOwner(stage);
        stageHistoriales.initStyle(StageStyle.UNDECORATED);

        Label lblTitulo = new Label("Historial de Estados");
        lblTitulo.setStyle(
            "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;"
        );
        Label lblSub = new Label("Seleccione un estado para consultar su historial");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox infoTitulo = new VBox(4, lblTitulo, lblSub);
        infoTitulo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoTitulo, Priority.ALWAYS);

        Button btnCerrarH = new Button("Cerrar");
        btnCerrarH.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #FFFFFF;" +
            "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8 4 8;" +
            "-fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        btnCerrarH.setOnAction(e -> stageHistoriales.close());

        HBox barraH = new HBox(infoTitulo, btnCerrarH);
        barraH.setStyle("-fx-background-color: #7B9EA6; -fx-padding: 20 28 20 28;");
        barraH.setAlignment(Pos.CENTER_LEFT);

        GridPane gridH = new GridPane();
        gridH.setHgap(12);
        gridH.setVgap(12);
        gridH.setPadding(new Insets(24, 28, 28, 28));
        gridH.setStyle("-fx-background-color: #F7F3EF;");
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            cc.setHgrow(Priority.ALWAYS);
            gridH.getColumnConstraints().add(cc);
        }
        int columnas = 3;
        int filas = (int) Math.ceil(ESTADOS_HISTORIAL.length / (double) columnas);
        for (int r = 0; r < filas; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(80);
            rc.setVgrow(Priority.ALWAYS);
            gridH.getRowConstraints().add(rc);
        }

        for (int i = 0; i < ESTADOS_HISTORIAL.length; i++) {
            final String estadoFinal = ESTADOS_HISTORIAL[i];
            Button btn = new Button(estadoFinal);
            btn.getStyleClass().add("btn-historial");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setMaxHeight(Double.MAX_VALUE);
            btn.setWrapText(true);
            btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            btn.setOnAction(e -> notificarVerHistorial(estadoFinal));
            GridPane.setFillWidth(btn, true);
            GridPane.setFillHeight(btn, true);
            gridH.add(btn, i % columnas, i / columnas);
        }

        VBox rootH = new VBox(barraH, gridH);
        VBox.setVgrow(gridH, Priority.ALWAYS);

        Scene sceneH = new Scene(rootH, 620, Math.max(420, 320 + filas * 90));
        sceneH.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                stageHistoriales.close();
            }
        });
        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) sceneH.getStylesheets().add(css.toExternalForm());

        stageHistoriales.setScene(sceneH);
        stageHistoriales.setOnShown(e -> {
            stageHistoriales.setX(stage.getX() + (stage.getWidth() - sceneH.getWidth()) / 2);
            stageHistoriales.setY(stage.getY() + (stage.getHeight() - sceneH.getHeight()) / 2);
        });
        stageHistoriales.show();
    }

    public Stage getStage() {
        return stage;
    }
}