package co.edu.uptc.processes1.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class FormParticion {

    private final Stage modalStage;
    private final TextField txtNombre;
    private final TextField txtTamano;
    private Runnable onGuardar;

    public FormParticion(Window owner) {
        modalStage = new Stage();
        modalStage.initOwner(owner);
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.UNDECORATED);
        modalStage.setResizable(false);

        Label lblTitulo = new Label("Crear Partición");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;");

        Label lblSub = new Label("Complete el nombre y el tamaño de la partición");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #9A9A9A;");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #AAAAAA; -fx-font-size: 13px;" +
            "-fx-cursor: hand; -fx-padding: 4 8 4 8;" +
            "-fx-border-color: #DDDDDD; -fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        btnCerrar.setOnAction(e -> modalStage.close());

        VBox infoHeader = new VBox(3, lblTitulo, lblSub);
        infoHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoHeader, Priority.ALWAYS);

        HBox header = new HBox(infoHeader, btnCerrar);
        header.setAlignment(Pos.CENTER_LEFT);

        txtNombre = campo("Ej: Partición A");
        txtTamano = campo("Ej: 256");
        soloNumeros(txtTamano);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label lblNombre = etiqueta("Nombre de la Partición:");
        Label lblTamano = etiqueta("Tamaño (u):");

        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(lblTamano, 0, 1);
        grid.add(txtTamano, 1, 1);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPrefWidth(160);
        c0.setHgrow(Priority.NEVER);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);
        grid.getColumnConstraints().addAll(c0, c1);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #888888; -fx-font-size: 14px;" +
            "-fx-border-color: #DDD8D3; -fx-border-radius: 10;" +
            "-fx-background-radius: 10; -fx-padding: 12 28 12 28; -fx-cursor: hand;"
        );
        btnCancelar.setOnAction(e -> modalStage.close());

        Button btnGuardar = new Button("Guardar Partición");
        btnGuardar.setStyle(
            "-fx-background-color: #7B9EA6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 12 32 12 32; -fx-cursor: hand;"
        );
        btnGuardar.setOnAction(e -> {
            if (getNombre().isBlank() || getTamano().isBlank()) {
                mostrarError("Complete el nombre y el tamaño de la partición.");
                return;
            }
            if (onGuardar != null) {
                onGuardar.run();
            }
            modalStage.close();
        });

        HBox filaBotones = new HBox(16, btnCancelar, btnGuardar);
        filaBotones.setAlignment(Pos.CENTER);

        VBox topSection = new VBox(16, header, new Separator());
        VBox centerSection = new VBox(14, grid);
        VBox bottomSection = new VBox(14, new Separator(), filaBotones);

        BorderPane card = new BorderPane();
        card.setTop(topSection);
        card.setCenter(centerSection);
        card.setBottom(bottomSection);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #DDD8D3; -fx-border-width: 1;"
        );
        card.setPrefWidth(620);
        card.setMinWidth(560);
        card.setMaxWidth(720);

        Scene scene = new Scene(card);
        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        modalStage.setScene(scene);
    }

    public void setOnGuardar(Runnable onGuardar) {
        this.onGuardar = onGuardar;
    }

    public void mostrar() {
        limpiar();
        modalStage.showAndWait();
    }

    public void cerrar() {
        if (modalStage.isShowing()) {
            modalStage.close();
        }
    }

    public void limpiar() {
        txtNombre.clear();
        txtTamano.clear();
        txtNombre.requestFocus();
    }

    public String getNombre() {
        return txtNombre.getText().trim();
    }

    public String getTamano() {
        return txtTamano.getText().trim();
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #7A7A7A; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private void soloNumeros(TextField tf) {
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                tf.setText(newV.replaceAll("\\D", ""));
            }
        });
    }

    private void mostrarError(String mensaje) {
        Stage err = new Stage();
        err.initOwner(modalStage);
        err.initModality(Modality.APPLICATION_MODAL);
        err.initStyle(StageStyle.UNDECORATED);

        Label lbl = new Label(mensaje);
        lbl.setWrapText(true);

        Button ok = new Button("Aceptar");
        ok.setOnAction(e -> err.close());

        VBox box = new VBox(12, lbl, ok);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));

        err.setScene(new Scene(box));
        err.showAndWait();
    }
}