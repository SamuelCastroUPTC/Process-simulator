package co.edu.uptc.processes1.view;

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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class HistorialCondensacionView {

    private Stage stage;
    private Label lblContador;
    private TableView<RegistroSimulacion.SnapshotParticion> tablaEventos;
    private TableView<RegistroSimulacion.SnapshotMemoria> tablaCompactacion;

    private double dragOffsetX;
    private double dragOffsetY;

    public HistorialCondensacionView() {
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

        Label lblTitulo = new Label("Historial - Particiones");
        lblTitulo.getStyleClass().add("historial-titulo");

        Label lblSub = new Label("Evolución de asignaciones y condensaciones de particiones");
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

        tablaEventos = new TableView<>();
        tablaEventos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaEventos.setPlaceholder(new Label("No hay eventos de particiones registrados."));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colPartRes = new TableColumn<>("Partición");
        colPartRes.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().nombreParticion() != null ? c.getValue().nombreParticion() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colCond = new TableColumn<>("Descripción");
        colCond.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().descripcion() != null ? c.getValue().descripcion() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colTamanio = new TableColumn<>("Tamaño");
        colTamanio.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().tamanio() != null ? c.getValue().tamanio().toString() : "-"
        ));

        tablaEventos.getColumns().addAll(colPartRes, colCond, colTamanio);
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        tablaCompactacion = new TableView<>();
        tablaCompactacion.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaCompactacion.setPlaceholder(new Label("No hay compactaciones registradas."));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colResultante =
            new TableColumn<>("Partición Resultante");
        colResultante.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().nombreProceso() != null ? c.getValue().nombreProceso() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDireccion =
            new TableColumn<>("Dirección Inicio");
        colDireccion.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().direccionInicio() != null ? c.getValue().direccionInicio().toString() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colTamResultante =
            new TableColumn<>("Tamaño");
        colTamResultante.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().tamanio() != null ? c.getValue().tamanio().toString() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDetalle =
            new TableColumn<>("Detalle");
        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().detalle() != null ? c.getValue().detalle() : "-"
        ));

        tablaCompactacion.getColumns().addAll(colResultante, colDireccion, colTamResultante, colDetalle);
        VBox.setVgrow(tablaCompactacion, Priority.ALWAYS);

        Tab tabParticiones = new Tab("Particiones", tablaEventos);
        tabParticiones.setClosable(false);
        Tab tabCompactacion = new Tab("Compactación", tablaCompactacion);
        tabCompactacion.setClosable(false);

        TabPane tabs = new TabPane(tabParticiones, tabCompactacion);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox contenido = new VBox(12, tabs);
        contenido.setPadding(new Insets(16, 36, 0, 36));
        contenido.setStyle("-fx-background-color: #F0F7F9;");
        VBox.setVgrow(contenido, Priority.ALWAYS);

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

    public void mostrarConDatos(List<RegistroSimulacion.SnapshotParticion> datos) {
        tablaEventos.setItems(FXCollections.observableArrayList(datos));
        int n = datos.size();
        lblContador.setText(n + (n == 1 ? " registro" : " registros"));
        stage.show();
        stage.toFront();
    }

    public void mostrarCompactacion(List<RegistroSimulacion.SnapshotMemoria> datos) {
        tablaCompactacion.setItems(FXCollections.observableArrayList(datos));
        stage.show();
        stage.toFront();
    }

    public void cerrar() {
        stage.close();
    }
}
