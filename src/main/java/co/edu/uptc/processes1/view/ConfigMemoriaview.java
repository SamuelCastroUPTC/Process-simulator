package co.edu.uptc.processes1.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ConfigMemoriaView — Ventana UNDECORATED que se muestra al arrancar el programa.
 * Bloquea el acceso al simulador hasta que el usuario configure las particiones.
 *
 * Flujo de tres pasos:
 *
 *   [Paso A] Usuario ingresa cantidad de particiones y pulsa "Generar Ranuras"
 *       │
 *       ▼
 *   [Paso B] Aparece dinamicamente un campo por particion (dentro de ScrollPane)
 *       │
 *       ▼
 *   [Paso C] Usuario pulsa "Guardar y Continuar"
 *            → valida → captura List<Integer> tamanios
 *            → cierra esta ventana
 *            → invoca el callback onConfigurado con la lista
 */
public class ConfigMemoriaview {

    private Stage stage;

    // Paso A
    private TextField txtCantidad;

    // Paso B — generado dinamicamente
    private VBox       contenedorRanuras;
    private ScrollPane scrollRanuras;
    private final List<TextField> camposRanura = new ArrayList<>();

    // Paso C — callback que recibe la lista de tamanios configurados
    private Consumer<List<Integer>> onConfigurado;

    // Para drag de la ventana sin bordes
    private double dragOffsetX, dragOffsetY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConfigMemoriaview() {
        buildUI();
    }

    // ── Construccion de la UI ─────────────────────────────────────────────────

    private void buildUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        // ── Encabezado ────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Configuracion de Memoria");
        lblTitulo.setStyle(
            "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;"
        );

        Label lblSub = new Label(
            "Configure las particiones de memoria fija antes de iniciar el simulador."
        );
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #7A7A7A;");
        lblSub.setWrapText(true);

        VBox encabezado = new VBox(6, lblTitulo, lblSub);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        // ── Paso A: cantidad de particiones ───────────────────────────────────
        Label lblPasoA = new Label("PASO 1 — Indique cuantas particiones necesita");
        lblPasoA.setStyle(
            "-fx-text-fill: #7B9EA6; -fx-font-size: 11px; -fx-font-weight: bold;"
        );

        txtCantidad = new TextField();
        txtCantidad.setPromptText("Ej: 4");
        txtCantidad.setMaxWidth(Double.MAX_VALUE);
        soloNumeros(txtCantidad);

        Button btnGenerar = new Button("Generar Ranuras");
        btnGenerar.setMaxWidth(Double.MAX_VALUE);
        estiloBotonPrimario(btnGenerar, false);
        btnGenerar.setOnMouseEntered(e -> estiloBotonPrimario(btnGenerar, true));
        btnGenerar.setOnMouseExited (e -> estiloBotonPrimario(btnGenerar, false));
        btnGenerar.setOnAction(e -> generarCamposRanura());

        VBox pasoA = new VBox(8, lblPasoA, new Separator(), txtCantidad, btnGenerar);
        pasoA.setPadding(new Insets(14, 16, 14, 16));
        pasoA.setStyle(
            "-fx-background-color: #F7FBFC;" +
            "-fx-border-color: #C8DCE0;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        // ── Paso B: campos de tamanio (generados dinamicamente) ───────────────
        Label lblPasoB = new Label("PASO 2 — Ingrese el tamano (unidades) de cada particion");
        lblPasoB.setStyle(
            "-fx-text-fill: #7B9EA6; -fx-font-size: 11px; -fx-font-weight: bold;"
        );
        lblPasoB.setVisible(false);

        contenedorRanuras = new VBox(10);
        contenedorRanuras.setAlignment(Pos.TOP_LEFT);
        contenedorRanuras.setPadding(new Insets(4, 8, 4, 8));

        scrollRanuras = new ScrollPane(contenedorRanuras);
        scrollRanuras.setFitToWidth(true);
        scrollRanuras.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRanuras.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRanuras.setPrefHeight(220);
        scrollRanuras.setMaxHeight(280);
        scrollRanuras.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollRanuras.setVisible(false);

        VBox pasoB = new VBox(8, lblPasoB, scrollRanuras);
        pasoB.setPadding(new Insets(14, 16, 14, 16));
        pasoB.setStyle(
            "-fx-background-color: #F7FBFC;" +
            "-fx-border-color: #C8DCE0;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        pasoB.setVisible(false);

        // Guardamos referencias para mostrar/ocultar en el Paso B
        // Los hacemos accesibles desde generarCamposRanura()
        pasoB.setUserData(lblPasoB);   // truco para reusar la referencia

        // ── Paso C: boton Guardar y Continuar ─────────────────────────────────
        Button btnGuardar = new Button("Guardar y Continuar");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        estiloBotonGuardar(btnGuardar, false);
        btnGuardar.setOnMouseEntered(e -> estiloBotonGuardar(btnGuardar, true));
        btnGuardar.setOnMouseExited (e -> estiloBotonGuardar(btnGuardar, false));
        btnGuardar.setVisible(false);
        btnGuardar.setOnAction(e -> guardarYContinuar(pasoB, btnGuardar));

        // ── Layout principal ──────────────────────────────────────────────────
        VBox contenido = new VBox(16,
            encabezado,
            new Separator(),
            pasoA,
            pasoB,
            btnGuardar
        );
        contenido.setPadding(new Insets(32, 36, 32, 36));
        contenido.setStyle("-fx-background-color: #FFFFFF;");
        contenido.setPrefWidth(480);
        contenido.setMinWidth(440);

        // Accion de "Generar Ranuras" necesita mostrar pasoB y btnGuardar
        btnGenerar.setOnAction(e -> generarCamposRanura(pasoB, lblPasoB, scrollRanuras, btnGuardar));

        // Drag de la ventana sin bordes desde el encabezado
        encabezado.setOnMousePressed(e -> { dragOffsetX = e.getSceneX(); dragOffsetY = e.getSceneY(); });
        encabezado.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        Scene scene = new Scene(contenido);
        scene.setFill(Color.WHITE);

        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);

        // Centrar en pantalla
        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + (bounds.getWidth()  - 480) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - 500) / 2);
    }

    // ── Paso B: genera los campos de tamanio dinamicamente ───────────────────

    private void generarCamposRanura(VBox pasoB, Label lblPasoB,
                                     ScrollPane scroll, Button btnGuardar) {
        String txt = txtCantidad.getText().trim();

        if (txt.isEmpty()) {
            mostrarError("Ingrese la cantidad de particiones antes de continuar.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txt);
            if (cantidad <= 0 || cantidad > 64) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            mostrarError("Ingrese un numero valido entre 1 y 64.");
            return;
        }

        // Limpiar campos anteriores
        camposRanura.clear();
        contenedorRanuras.getChildren().clear();

        // Crear un campo por particion
        for (int i = 1; i <= cantidad; i++) {
            Label lbl = new Label("Particion " + i + ":");
            lbl.setStyle(
                "-fx-text-fill: #5A7A85; -fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-min-width: 110;"
            );

            TextField campo = new TextField();
            campo.setPromptText("Tamano en unidades");
            campo.setMaxWidth(Double.MAX_VALUE);
            soloNumeros(campo);
            camposRanura.add(campo);

            HBox fila = new HBox(12, lbl, campo);
            fila.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(campo, Priority.ALWAYS);

            contenedorRanuras.getChildren().add(fila);
        }

        // Mostrar Paso B y boton Guardar
        pasoB.setVisible(true);
        lblPasoB.setVisible(true);
        scroll.setVisible(true);
        btnGuardar.setVisible(true);

        // Pedir foco al primer campo
        if (!camposRanura.isEmpty()) {
            camposRanura.get(0).requestFocus();
        }
    }

    // Sobrecarga vacia para no romper la firma del setOnAction original
    private void generarCamposRanura() { /* reemplazada por la version con parametros */ }

    // ── Paso C: valida, captura y notifica ────────────────────────────────────

    private void guardarYContinuar(VBox pasoB, Button btnGuardar) {
        if (camposRanura.isEmpty()) {
            mostrarError("Primero genere las ranuras de particion.");
            return;
        }

        List<Integer> tamanios = new ArrayList<>();
        for (int i = 0; i < camposRanura.size(); i++) {
            String val = camposRanura.get(i).getText().trim();
            if (val.isEmpty()) {
                mostrarError("El tamano de la Particion " + (i + 1) + " no puede estar vacio.");
                camposRanura.get(i).requestFocus();
                return;
            }
            int tamano;
            try {
                tamano = Integer.parseInt(val);
                if (tamano <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarError("El tamano de la Particion " + (i + 1) + " debe ser un numero entero positivo.");
                camposRanura.get(i).requestFocus();
                return;
            }
            tamanios.add(tamano);
        }

        // Cerrar esta ventana
        stage.close();

        // Notificar al callback con la lista de tamanios
        if (onConfigurado != null) {
            onConfigurado.accept(tamanios);
        }
    }

    // ── API publica ───────────────────────────────────────────────────────────

    /**
     * Registra el callback que se invoca cuando el usuario finaliza la
     * configuracion. Recibe la List<Integer> con los tamanios de cada particion.
     *
     * @param callback lambda que recibe List<Integer>
     */
    public void setOnConfigurado(Consumer<List<Integer>> callback) {
        this.onConfigurado = callback;
    }

    /**
     * Muestra la ventana de forma bloqueante (showAndWait).
     * La llamada no retorna hasta que el usuario complete la configuracion
     * o cierre la ventana.
     */
    public void mostrarYEsperar() {
        stage.showAndWait();
    }

    public Stage getStage() { return stage; }

    // ── Utilidades ────────────────────────────────────────────────────────────

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
        err.setScene(s);
        err.showAndWait();
    }

    private void soloNumeros(TextField tf) {
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("\\D", ""));
        });
    }

    private void estiloBotonPrimario(Button btn, boolean hover) {
        String bg = hover ? "#6A8F98" : "#7B9EA6";
        btn.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 11 24 11 24; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(123,158,166,0.38), 8, 0, 0, 2);"
        );
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