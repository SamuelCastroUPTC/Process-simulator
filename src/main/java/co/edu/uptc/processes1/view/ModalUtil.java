package co.edu.uptc.processes1.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ModalUtil {

    public enum TipoModal { ERROR, EXITO, INFO }

    /**
     * Muestra un modal bloqueante personalizado.
     *
     * @param owner    Ventana propietaria (para centrarlo)
     * @param tipo     ERROR | EXITO | INFO
     * @param titulo   Ti­tulo del modal
     * @param mensaje  Cuerpo del mensaje
     */
    public static void mostrar(Window owner, TipoModal tipo, String titulo, String mensaje) {

        Stage modal = new Stage();
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);

        String colorIcono = switch (tipo) {
            case ERROR -> "#E8A598";
            case EXITO -> "#A8C5A0";
            case INFO  -> "#7B9EA6";
        };

        String estiloTitulo = switch (tipo) {
            case ERROR -> "modal-titulo-error";
            case EXITO -> "modal-titulo-exito";
            case INFO  -> "label-estado";
        };

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(estiloTitulo);
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(340);
        lblTitulo.setAlignment(Pos.CENTER);

        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("modal-mensaje");
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(340);
        lblMensaje.setAlignment(Pos.CENTER);

        Button btnOk = new Button("Aceptar");
        btnOk.getStyleClass().add("btn-modal-ok");
        if (tipo == TipoModal.ERROR) {
            btnOk.setStyle(
                "-fx-background-color: #E8A598; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10; -fx-padding: 12 32 12 32; -fx-cursor: hand;"
            );
        }
        btnOk.setOnAction(e -> modal.close());
        btnOk.setPrefWidth(160);

        VBox card;
        if (titulo == null || titulo.isBlank()) {
            card = new VBox(18, lblMensaje, btnOk);
        } else {
            card = new VBox(18, lblTitulo, lblMensaje, btnOk);
        }
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(420);

        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(30, 30, 30, 0.40);");
        overlay.setPrefSize(520, 420);

        // Cerrar al clicar fuera de la tarjeta
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) modal.close();
        });

        Scene scene = new Scene(overlay);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                modal.close();
            }
        });

        // Cargar CSS
        var css = ModalUtil.class.getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        modal.setScene(scene);
        modal.showAndWait();
    }

    public static void error(Window owner, String mensaje) {
        mostrar(owner, TipoModal.ERROR, "", mensaje);
    }

    public static void exito(Window owner, String mensaje) {
        mostrar(owner, TipoModal.EXITO, "", mensaje);
    }

    public static void info(Window owner, String titulo, String mensaje) {
        mostrar(owner, TipoModal.INFO, titulo, mensaje);
    }

    /**
     * Muestra un modal de confirmación bloqueante.
     *
     * @param owner   Ventana propietaria (para centrarlo)
     * @param mensaje Mensaje de confirmación
     * @return true si el usuario presiona "Confirmar", false si presiona "Cancelar"
     */
    public static boolean confirmar(Window owner, String mensaje) {
        Stage modal = new Stage();
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);

        // Contenedor para el resultado
        boolean[] resultado = { false };

        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("modal-mensaje");
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(340);
        lblMensaje.setAlignment(Pos.CENTER);

        Button btnConfirmar = new Button("Sí");
        btnConfirmar.getStyleClass().add("btn-modal-ok");
        btnConfirmar.setStyle(
            "-fx-background-color: #7B9EA6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 12 32 12 32; -fx-cursor: hand;"
        );
        btnConfirmar.setPrefWidth(140);
        btnConfirmar.setOnAction(e -> {
            resultado[0] = true;
            modal.close();
        });

        Button btnCancelar = new Button("No");
        btnCancelar.setStyle(
            "-fx-background-color: #7B9EA6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28; -fx-cursor: hand;"
        );
        btnCancelar.setPrefWidth(140);
        btnCancelar.setOnAction(e -> {
            resultado[0] = false;
            modal.close();
        });

        HBox botonesBox = new HBox(16, btnConfirmar, btnCancelar);
        botonesBox.setAlignment(Pos.CENTER);

        VBox card = new VBox(24, lblMensaje, botonesBox);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(420);

        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(30, 30, 30, 0.40);");
        overlay.setPrefSize(520, 420);

        // Cerrar al clicar fuera de la tarjeta
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) modal.close();
        });

        Scene scene = new Scene(overlay);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                modal.close();
            }
        });

        // Cargar CSS
        var css = ModalUtil.class.getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        modal.setScene(scene);
        modal.showAndWait();

        return resultado[0];
    }
}
