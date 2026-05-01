package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.presenter.RegistroSimulacion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.math.BigInteger;
import java.util.List;

public class HistorialParticionesView {

    private Stage stage;
    private Label lblContador;
    private TableView<RegistroSimulacion.SnapshotParticion> tablaEventos;

    private double dragOffsetX;
    private double dragOffsetY;

    public HistorialParticionesView() {
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

        Label lblTitulo = new Label("Historial de Particiones");
        lblTitulo.getStyleClass().add("historial-titulo");

        Label lblSub = new Label("Evolución de particiones durante la simulación (asignaciones y condensaciones)");
        lblSub.getStyleClass().add("historial-subtitulo");

        VBox infoTitulo = new VBox(4, lblTitulo, lblSub);
        infoTitulo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoTitulo, Priority.ALWAYS);

        lblContador = new Label("0 registros");
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
        tablaEventos.setPlaceholder(new Label("No hay registros de particiones."));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colNombre = new TableColumn<>("Nombre de Partición");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().nombreParticion() != null ? c.getValue().nombreParticion() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().descripcion() != null ? c.getValue().descripcion() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotParticion, String> colTamanio = new TableColumn<>("Tamaño");
        colTamanio.setCellValueFactory(c -> new SimpleStringProperty(formatearTamanio(c.getValue().tamanio())));

        tablaEventos.getColumns().addAll(colNombre, colDescripcion, colTamanio);
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        VBox contenido = new VBox(12, tablaEventos);
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

    public void cerrar() {
        stage.close();
    }

    private String formatearTamanio(BigInteger tamanio) {
        return tamanio != null ? tamanio.toString() : "-";
    }
}
