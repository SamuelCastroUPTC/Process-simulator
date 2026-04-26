package co.edu.uptc.processes1.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.math.BigInteger;
import java.util.function.Consumer;

public class ConfigMemoriaview {

    private Stage stage;
    private TextField txtTamanoTotal;
    private Consumer<BigInteger> onConfigurado;

    private double dragOffsetX;
    private double dragOffsetY;

    public ConfigMemoriaview() {
        buildUI();
    }

    private void buildUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        Label lblTitulo = new Label("Configuracion de Memoria");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;");

        Label lblSub = new Label("Ingrese el tamano total de la memoria para iniciar la simulacion.");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #7A7A7A;");
        lblSub.setWrapText(true);

        VBox encabezado = new VBox(6, lblTitulo, lblSub);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Label lblPaso = new Label("TAMANO TOTAL DE MEMORIA");
        lblPaso.setStyle("-fx-text-fill: #7B9EA6; -fx-font-size: 11px; -fx-font-weight: bold;");

        txtTamanoTotal = new TextField();
        txtTamanoTotal.setPromptText("Ej: 1024");
        txtTamanoTotal.setMaxWidth(Double.MAX_VALUE);
        soloNumeros(txtTamanoTotal);

        Button btnGuardar = new Button("Guardar y Continuar");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        estiloBotonGuardar(btnGuardar, false);
        btnGuardar.setOnMouseEntered(e -> estiloBotonGuardar(btnGuardar, true));
        btnGuardar.setOnMouseExited(e -> estiloBotonGuardar(btnGuardar, false));
        btnGuardar.setOnAction(e -> guardarYContinuar());

        VBox contenido = new VBox(16,
            encabezado,
            new Separator(),
            lblPaso,
            txtTamanoTotal,
            btnGuardar
        );
        contenido.setPadding(new Insets(32, 36, 32, 36));
        contenido.setStyle("-fx-background-color: #FFFFFF;");
        contenido.setPrefWidth(480);
        contenido.setMinWidth(440);

        encabezado.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        encabezado.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        Scene scene = new Scene(contenido);
        scene.setFill(Color.WHITE);

        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);

        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + (bounds.getWidth() - 480) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - 300) / 2);
    }

    private void guardarYContinuar() {
        String valorLimpio = txtTamanoTotal.getText().replaceAll("\\.", "").trim();
        if (valorLimpio.isEmpty()) {
            mostrarError("El tamano total de memoria no puede estar vacio.");
            txtTamanoTotal.requestFocus();
            return;
        }

        BigInteger tamanioTotal;
        try {
            tamanioTotal = new BigInteger(valorLimpio);
        } catch (NumberFormatException ex) {
            mostrarError("Ingrese un numero entero positivo valido.");
            txtTamanoTotal.requestFocus();
            return;
        }

        if (tamanioTotal.compareTo(BigInteger.ZERO) <= 0) {
            mostrarError("El tamano total de memoria debe ser mayor a 0.");
            txtTamanoTotal.requestFocus();
            return;
        }

        stage.close();
        if (onConfigurado != null) {
            onConfigurado.accept(tamanioTotal);
        }
    }

    public void setOnConfigurado(Consumer<BigInteger> callback) {
        this.onConfigurado = callback;
    }

    public void mostrarYEsperar() {
        stage.showAndWait();
    }

    public Stage getStage() {
        return stage;
    }

    private void mostrarError(String mensaje) {
        Stage err = new Stage();
        err.initOwner(stage);
        err.initModality(Modality.APPLICATION_MODAL);
        err.initStyle(StageStyle.UNDECORATED);
        err.setResizable(false);

        Label lblMsg = new Label(mensaje);
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-text-fill: #5A3030; -fx-font-size: 13px;");
        lblMsg.setMaxWidth(320);

        Button btnOk = new Button("Aceptar");
        btnOk.setStyle(
            "-fx-background-color: #E8A598; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 10 28 10 28; -fx-cursor: hand;"
        );
        btnOk.setOnAction(e -> err.close());

        Label lblTitErr = new Label("Error de validacion");
        lblTitErr.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #C0504D;");

        VBox box = new VBox(14, lblTitErr, new Separator(), lblMsg, btnOk);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 32, 28, 32));
        box.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #E8A598; -fx-border-width: 2;"
        );
        box.setPrefWidth(380);

        Scene s = new Scene(box);
        s.setFill(Color.WHITE);
        s.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                err.close();
            }
        });
        err.setScene(s);
        err.showAndWait();
    }

    private void soloNumeros(TextField tf) {
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("\\D", ""));
        });
    }

    private void estiloBotonGuardar(Button btn, boolean hover) {
        String bg = hover ? "#85AD7D" : "#A8C5A0";
        btn.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
            "-fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 14 32 14 32; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(168,197,160,0.45), 10, 0, 0, 3);"
        );
    }
}
