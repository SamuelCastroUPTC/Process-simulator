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

import java.util.List;

public class HistorialMemoriaView {

    private Stage stage;
    private Label lblContador;
    private TableView<RegistroSimulacion.SnapshotMemoria> tablaEventos;
    private ListView<String> listaHuecos;
    private ListView<String> listaBloques;
    private final String evento;

    private double dragOffsetX, dragOffsetY;

    public HistorialMemoriaView(String evento) {
        this.evento = evento;
        buildUI();
    }

    private void buildUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);

        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        // ── Barra superior ────────────────────────────────────────────────────
        Label lblTitulo = new Label("Historial - " + evento);
        lblTitulo.getStyleClass().add("historial-titulo");

        String descripcion = switch (evento) {
            case "Asignación"   -> "Memoria asignada a cada proceso durante la simulación";
            case "Liberación"   -> "Memoria liberada al terminar o expirar cada proceso";
            case "Condensación" -> "Huecos fusionados por adyacencia tras cada liberación";
            default             -> "Eventos de memoria variable";
        };

        Label lblSub = new Label(descripcion);
        lblSub.getStyleClass().add("historial-subtitulo");

        VBox infoTitulo = new VBox(4, lblTitulo, lblSub);
        infoTitulo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoTitulo, Priority.ALWAYS);

        lblContador = new Label("0 eventos");
        lblContador.getStyleClass().add("historial-contador");

        HBox barra = new HBox(infoTitulo, lblContador);
        barra.getStyleClass().add("historial-barra");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(24, 36, 24, 36));
        barra.setOnMousePressed(e -> { dragOffsetX = e.getSceneX(); dragOffsetY = e.getSceneY(); });
        barra.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // ── Tabla de eventos ──────────────────────────────────────────────────
        tablaEventos = new TableView<>();
        tablaEventos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaEventos.setPlaceholder(new Label("No hay eventos registrados."));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colProceso = new TableColumn<>("Proceso");
        colProceso.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().nombreProceso() != null ? c.getValue().nombreProceso() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDireccion = new TableColumn<>("Dirección Inicio");
        colDireccion.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().direccionInicio() != null ? c.getValue().direccionInicio().toString() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colTamanio = new TableColumn<>("Tamaño");
        colTamanio.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().tamanio() != null ? c.getValue().tamanio().toString() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDetalle = new TableColumn<>("Detalle");
        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detalle()));
        colDetalle.setPrefWidth(400);

        tablaEventos.getColumns().addAll(colProceso, colDireccion, colTamanio, colDetalle);

        // Al seleccionar una fila, actualizar los paneles laterales
        tablaEventos.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                listaHuecos.setItems(FXCollections.observableArrayList(sel.estadoHuecos()));
                listaBloques.setItems(FXCollections.observableArrayList(sel.estadoBloques()));
            }
        });
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        // ── Panel de estado de memoria ────────────────────────────────────────
        Label lblHuecos = new Label("Huecos libres en ese momento:");
        lblHuecos.setStyle("-fx-font-weight: bold; -fx-text-fill: #5A8550; -fx-font-size: 13px;");

        listaHuecos = new ListView<>();
        listaHuecos.setPlaceholder(new Label("Sin huecos."));
        listaHuecos.setPrefHeight(160);
        listaHuecos.setStyle("-fx-background-color: #D4EDD0; -fx-border-color: #A8C5A0;");

        Label lblBloques = new Label("Bloques ocupados en ese momento:");
        lblBloques.setStyle("-fx-font-weight: bold; -fx-text-fill: #5A7A85; -fx-font-size: 13px;");

        listaBloques = new ListView<>();
        listaBloques.setPlaceholder(new Label("Sin bloques."));
        listaBloques.setPrefHeight(160);
        listaBloques.setStyle("-fx-background-color: #D4EBF0; -fx-border-color: #7B9EA6;");

        Label lblHint = new Label("Seleccione un evento de la tabla para ver el estado de memoria en ese instante.");
        lblHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAAAAA; -fx-font-style: italic;");

        VBox panelEstado = new VBox(8, lblHuecos, listaHuecos, lblBloques, listaBloques, lblHint);
        panelEstado.setPadding(new Insets(12, 0, 0, 0));
        panelEstado.setStyle("-fx-background-color: #F0F7F9;");

        // ── Layout central ────────────────────────────────────────────────────
        VBox contenido = new VBox(12, tablaEventos, new Separator(), panelEstado);
        contenido.setPadding(new Insets(16, 36, 0, 36));
        contenido.setStyle("-fx-background-color: #F0F7F9;");
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        // ── Footer ────────────────────────────────────────────────────────────
        Button btnVolver = new Button("Volver al Menu Principal");
        btnVolver.getStyleClass().add("btn-volver");
        btnVolver.setOnAction(e -> stage.close());

        HBox footer = new HBox(btnVolver);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(30, 36, 40, 36));
        footer.setStyle("-fx-background-color: #F0F7F9;");

        VBox root = new VBox(barra, contenido, footer);
        root.getStyleClass().add("historial-root");
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.setFill(Color.WHITE);
        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
    }

    public void mostrarConDatos(List<RegistroSimulacion.SnapshotMemoria> datos) {
        tablaEventos.setItems(FXCollections.observableArrayList(datos));
        listaHuecos.setItems(FXCollections.observableArrayList());
        listaBloques.setItems(FXCollections.observableArrayList());
        int n = datos.size();
        lblContador.setText(n + (n == 1 ? " evento" : " eventos"));
        stage.show();
        stage.toFront();
    }

    public void cerrar() { stage.close(); }
    public Stage getStage() { return stage; }
}