package co.edu.uptc.processes1.view;

import co.edu.uptc.processes1.model.Proceso;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * FormProcces — Modal UNDECORATED para crear un proceso.
 * Sin íconos. Botón de texto plano para cerrar.
 *
 * Layout de dos columnas:
 *
 *  ┌─────────────────────────────────────────────────────────────────────┐
 *  │  Crear Nuevo Proceso                              [ Cerrar ]        │
 *  ├──────────────────────────────┬──────────────────────────────────────┤
 *  │  DATOS BASICOS               │  PROCESOS CARGADOS                  │
 *  │  Nombre        [_________]   │  Lista de procesos en cola          │
 *  │  Tiempo (seg)  [_________]   │                                      │
 *  │  Tamano Mem.   [_________]   │                                      │
 *  ├──────────────────────────────┴──────────────────────────────────────┤
 *  │                  [ Cancelar ]   [ Cargar Proceso ]                  │
 *  └─────────────────────────────────────────────────────────────────────┘
 */
public class FormProcces {

    // ── Controles ─────────────────────────────────────────────────────────────
    private TextField        txtNombre;
    private TextField        txtTiempo;
    private TextField        txtTamanioMemoria;   // campo nuevo
    private ListView<String> listaProcesosCargados;

    private Stage    modalStage;
    private Runnable onCargar;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FormProcces(Window owner) {
        buildUI(owner);
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private void buildUI(Window owner) {
        modalStage = new Stage();
        modalStage.initOwner(owner);
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.UNDECORATED);   // sin bordes del SO
        modalStage.setResizable(false);

        // ══════════════ ENCABEZADO ════════════════════════════════════════════
        Label lblTitulo = new Label("Crear Nuevo Proceso");
        lblTitulo.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3D3D3D;"
        );

        Label lblSub = new Label("Complete todos los campos del proceso");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #9A9A9A;");

        // Boton de texto plano — sin iconos
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

        // ══════════════ PANEL IZQUIERDO — Datos basicos ═══════════════════════
        txtNombre         = campo(null);
        txtTiempo         = campo("Ej: 5");     soloNumeros(txtTiempo);
        txtTamanioMemoria = campo("Ej: 128");   formatearConPuntosMiles(txtTamanioMemoria);

        GridPane gridIzq = panelGrid(160);
        int fi = 0;
        agregarFila(gridIzq, fi++, "Nombre:",                 txtNombre);
        agregarFila(gridIzq, fi++, "Tiempo (seg):",           txtTiempo);
        agregarFila(gridIzq, fi++, "Tamano en Memoria:", txtTamanioMemoria);

        VBox colIzq = new VBox(10,
            seccion("DATOS BASICOS"),
            new Separator(),
            gridIzq
        );
        colIzq.setAlignment(Pos.TOP_LEFT);
        colIzq.setPadding(new Insets(0, 20, 0, 0));
        HBox.setHgrow(colIzq, Priority.ALWAYS);

        // ══════════════ PANEL DERECHO EXTRA — Procesos cargados ══════════════
        listaProcesosCargados = new ListView<>();
        listaProcesosCargados.setFocusTraversable(false);
        listaProcesosCargados.setMouseTransparent(true);
        listaProcesosCargados.setPlaceholder(new Label("No hay procesos cargados."));
        listaProcesosCargados.setPrefHeight(220);
        listaProcesosCargados.setMaxWidth(Double.MAX_VALUE);

        VBox panelProcesos = new VBox(10,
            seccion("PROCESOS CARGADOS"),
            new Separator(),
            listaProcesosCargados
        );
        panelProcesos.setAlignment(Pos.TOP_LEFT);
        panelProcesos.setPadding(new Insets(0, 0, 0, 20));
        panelProcesos.setPrefWidth(280);
        panelProcesos.setMaxWidth(320);
        VBox.setVgrow(listaProcesosCargados, Priority.ALWAYS);
        HBox.setHgrow(panelProcesos, Priority.ALWAYS);

        // ══════════════ FILA DE DOS COLUMNAS ═════════════════════════════════
        Separator sepV = new Separator();
        sepV.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sepV.setPrefHeight(50);

        HBox dosColumnas = new HBox(colIzq, sepV, panelProcesos);
        dosColumnas.setAlignment(Pos.TOP_LEFT);
        dosColumnas.setPadding(new Insets(8, 0, 8, 0));

        // ══════════════ BOTONES ═══════════════════════════════════════════════
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: #7B9EA6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 12 28 12 28; -fx-cursor: hand;"
        );
        btnCancelar.setOnAction(e -> modalStage.close());

        Button btnCargar = new Button("Cargar Proceso");
        estiloBtnCargar(btnCargar, false);
        btnCargar.setOnMouseEntered(e -> estiloBtnCargar(btnCargar, true));
        btnCargar.setOnMouseExited (e -> estiloBtnCargar(btnCargar, false));
        btnCargar.setOnAction(e -> { if (onCargar != null) onCargar.run(); });

        HBox filaBotones = new HBox(16, btnCancelar, btnCargar);
        filaBotones.setAlignment(Pos.CENTER);

        // ══════════════ TARJETA con BorderPane ═══════════════════════════════
        VBox topSection = new VBox(16, header, new Separator());
        topSection.setPadding(new Insets(0, 0, 4, 0));

        VBox bottomSection = new VBox(14, new Separator(), filaBotones);
        bottomSection.setPadding(new Insets(4, 0, 0, 0));

        BorderPane card = new BorderPane();
        card.setTop(topSection);
        card.setCenter(dosColumnas);
        card.setBottom(bottomSection);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #DDD8D3; -fx-border-width: 1;"
        );
        card.setPrefWidth(900);
        card.setMinWidth(660);
        card.setMaxWidth(960);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(18));
        overlay.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.25);");
        overlay.setOnMouseClicked(e -> {
            javafx.geometry.Bounds cardBounds = card.getBoundsInParent();
            if (!cardBounds.contains(e.getX(), e.getY())) {
                modalStage.close();
            }
        });

        Scene scene = new Scene(overlay, screenBounds.getWidth(), screenBounds.getHeight());
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                modalStage.close();
            }
        });

        var css = getClass().getResource("/css/Simulador.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        modalStage.setX(screenBounds.getMinX());
        modalStage.setY(screenBounds.getMinY());
        modalStage.setScene(scene);
    }

    // ── Helpers de construcción ───────────────────────────────────────────────

    private GridPane panelGrid(double labelWidth) {
        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(12);

        ColumnConstraints cLabel = new ColumnConstraints();
        cLabel.setMinWidth(labelWidth);
        cLabel.setPrefWidth(labelWidth);
        cLabel.setHgrow(Priority.NEVER);

        ColumnConstraints cControl = new ColumnConstraints();
        cControl.setHgrow(Priority.ALWAYS);
        cControl.setFillWidth(true);

        gp.getColumnConstraints().addAll(cLabel, cControl);
        return gp;
    }

    private void agregarFila(GridPane gp, int fila, String texto, javafx.scene.Node control) {
        gp.add(etiqueta(texto), 0, fila);
        gp.add(control,         1, fila);
    }

   // ── API pública ───────────────────────────────────────────────────────────

    public void mostrar() {
        limpiar();
        
        // Configuramos el centrado y el foco JUSTO cuando la ventana ya sabe su tamaño real
        modalStage.setOnShown(e -> {
            Window parent = modalStage.getOwner();
            if (parent != null) {
                // Centrar respecto a la ventana principal de forma segura
                modalStage.setX(parent.getX() + (parent.getWidth() - modalStage.getWidth()) / 2);
                modalStage.setY(parent.getY() + (parent.getHeight() - modalStage.getHeight()) / 2);
            }
            txtNombre.requestFocus();
        });

        modalStage.toFront();
        modalStage.showAndWait();
    }

    public void cerrar() {
        if (modalStage.isShowing()) modalStage.close();
    }

    public void setOnCargar(Runnable cb) {
        this.onCargar = cb;
    }

    public void limpiar() {
        txtNombre.clear();
        txtTiempo.clear();
        txtTamanioMemoria.clear();
        // Quitamos el requestFocus() de aquí, porque lo pasamos al evento setOnShown de arriba
    }

    public void setModoEdicion(boolean modoEdicion) {
        if (modoEdicion) {
            txtNombre.setDisable(true);
            txtTiempo.setDisable(false);
            txtTamanioMemoria.setDisable(false);
        } else {
            txtNombre.setDisable(false);
            txtTiempo.setDisable(false);
            txtTamanioMemoria.setDisable(false);
        }
    }

     // ── Getters ───────────────────────────────────────────────────────────────

    public String  getNombre()            { return txtNombre.getText().trim(); }
    public String  getTiempo()            { return txtTiempo.getText().trim(); }
    public String  getTamanioMemoria()    { return txtTamanioMemoria.getText().replaceAll("\\.", "").trim(); }

    public void setProcesosCargados(List<Proceso> procesos) {
        List<String> items = new ArrayList<>();
        if (procesos != null) {
            for (Proceso proceso : procesos) {
                if (proceso == null) {
                    continue;
                }
                BigInteger tiempoSegundos = proceso.getTiempoRestante().divide(BigInteger.valueOf(1000L));
                items.add(proceso.getNombre() + " | " + tiempoSegundos + " s | " + proceso.getTamanioMemoria() + "");
            }
        }
        listaProcesosCargados.setItems(FXCollections.observableArrayList(items));
    }

    public Stage getModalStage() { return modalStage; }

    // ── Utilidades privadas ───────────────────────────────────────────────────

    private Label seccion(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #7B9EA6; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #7A7A7A; -fx-font-size: 11px; -fx-font-weight: bold;");
        l.setWrapText(false);
        return l;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        if (prompt != null) tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private void soloNumeros(TextField tf) {
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("\\D", ""));
        });
    }

    private void formatearConPuntosMiles(TextField campo) {
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            String soloDigitos = newVal.replaceAll("[^\\d]", "");

            if (soloDigitos.isEmpty()) {
                if (!newVal.equals("")) {
                    campo.setText("");
                }
                return;
            }

            StringBuilder sb = new StringBuilder(soloDigitos);
            int insertarEn = sb.length() - 3;
            while (insertarEn > 0) {
                sb.insert(insertarEn, '.');
                insertarEn -= 3;
            }
            String formateado = sb.toString();

            if (!newVal.equals(formateado)) {
                campo.setText(formateado);
                campo.positionCaret(formateado.length());
            }
        });
    }

    private void estiloBtnCargar(Button btn, boolean hover) {
        String bg     = hover ? "#6A8F98" : "#7B9EA6";
        String shadow = hover
            ? "dropshadow(gaussian, rgba(123,158,166,0.55), 12, 0, 0, 4)"
            : "dropshadow(gaussian, rgba(123,158,166,0.38), 9, 0, 0, 3)";
        btn.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-padding: 12 32 12 32;" +
            "-fx-cursor: hand; -fx-effect: " + shadow + ";"
        );
    }
}