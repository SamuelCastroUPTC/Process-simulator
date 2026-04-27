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

import java.util.List;

public class HistorialCondensacionView {

    private Stage stage;
    private Label lblContador;
    private TableView<RegistroSimulacion.SnapshotCondensacion> tablaEventos;

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

        Label lblTitulo = new Label("Historial - Condensación de Particiones");
        lblTitulo.getStyleClass().add("historial-titulo");

        Label lblSub = new Label("Particiones fusionadas durante la liberación de memoria");
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
        tablaEventos.setPlaceholder(new Label("No hay eventos de condensación registrados."));

        TableColumn<RegistroSimulacion.SnapshotCondensacion, String> colPartRes = new TableColumn<>("Partición Resultante");
        colPartRes.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().particionResultante() != null ? c.getValue().particionResultante() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotCondensacion, String> colCond = new TableColumn<>("Particiones Condensadas");
        colCond.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().particionesCondensadas() != null ? c.getValue().particionesCondensadas() : "-"
        ));

        TableColumn<RegistroSimulacion.SnapshotCondensacion, String> colTamanio = new TableColumn<>("Tamaño Resultante");
        colTamanio.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().tamanioResultante() != null ? c.getValue().tamanioResultante().toString() : "-"
        ));

        tablaEventos.getColumns().addAll(colPartRes, colCond, colTamanio);
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

    public void mostrarConDatos(List<RegistroSimulacion.SnapshotCondensacion> datos) {
        tablaEventos.setItems(FXCollections.observableArrayList(datos));
        int n = datos.size();
        lblContador.setText(n + (n == 1 ? " evento" : " eventos"));
        stage.show();
        stage.toFront();
    }

    public void cerrar() {
        stage.close();
    }
}
