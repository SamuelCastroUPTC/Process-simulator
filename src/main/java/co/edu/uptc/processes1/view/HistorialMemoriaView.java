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
            case "Condensación" -> "Huecos libres adyacentes fusionados tras cada liberación";
            case "Compactación" -> "Procesos desplazados hacia arriba (renombrados) para agrupar el espacio libre";
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

        if (this.evento.equals(RegistroSimulacion.COMPACTACION)) {
            // Columnas especiales para compactación de procesos
            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colProceso = new TableColumn<>("Proceso");
            colProceso.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().nombreProceso() != null ? c.getValue().nombreProceso() : "-"
            ));

            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colPartAnterior = new TableColumn<>("Partición Anterior");
            colPartAnterior.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().metadatoExtra() != null ? c.getValue().metadatoExtra() : "-"
            ));

            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colPartNueva = new TableColumn<>("Partición Nueva");
            colPartNueva.setCellValueFactory(c -> {
                String det = c.getValue().detalle();
                if (det == null) return new SimpleStringProperty("-");
                int flechaIdx = det.indexOf("→");
                if (flechaIdx == -1) return new SimpleStringProperty("-");
                String posterior = det.substring(flechaIdx + 2).trim();
                int atIdx = posterior.indexOf("@");
                return new SimpleStringProperty(atIdx == -1 ? posterior : posterior.substring(0, atIdx));
            });

            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDirAnterior = new TableColumn<>("Dir. Anterior");
            colDirAnterior.setCellValueFactory(c -> {
                String det = c.getValue().detalle();
                if (det == null) return new SimpleStringProperty("-");
                int deIdx = det.indexOf("@");
                if (deIdx == -1) return new SimpleStringProperty("-");
                String desde = det.substring(deIdx + 1);
                int flechaIdx = desde.indexOf("→");
                return new SimpleStringProperty(
                    flechaIdx == -1 ? desde.trim() : desde.substring(0, flechaIdx).trim()
                );
            });

            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colDirNueva = new TableColumn<>("Dir. Nueva");
            colDirNueva.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().direccionInicio() != null ? c.getValue().direccionInicio().toString() : "-"
            ));

            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colTamanio = new TableColumn<>("Tamaño");
            colTamanio.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().tamanio() != null ? c.getValue().tamanio().toString() : "-"
            ));

            tablaEventos.getColumns().addAll(
                colProceso, colPartAnterior, colPartNueva, colDirAnterior, colDirNueva, colTamanio
            );
        } else {
            // Columnas estándar para otros eventos
            String encabezadoProceso = this.evento.equals(RegistroSimulacion.CONDENSACION)
                ? "Partición Resultante"
                : "Proceso";
            TableColumn<RegistroSimulacion.SnapshotMemoria, String> colProceso = new TableColumn<>(encabezadoProceso);
            colProceso.setCellValueFactory(c -> {
                String val = c.getValue().nombreProceso();
                if (val == null || val.isBlank()) {
                    String det = c.getValue().detalle();
                    if (det != null && det.contains("→")) {
                        val = det.substring(det.lastIndexOf("→") + 2).trim();
                    }
                }
                return new SimpleStringProperty(val != null ? val : "-");
            });

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
        }
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        // ── Layout central ────────────────────────────────────────────────────
        VBox contenido = new VBox(12, tablaEventos);
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
        int n = datos.size();
        lblContador.setText(n + (n == 1 ? " evento" : " eventos"));
        stage.show();
        stage.toFront();
    }

    /**
     * Extrae la partición nueva desde el detalle.
     * Formato esperado: "Proceso 'X' desplazado de PAR1@dir1 → PAR2@dir2"
     */
    private String extraerParticionNueva(String detalle) {
        if (detalle == null) return "-";
        // Buscar "→ " y extraer lo que viene después
        int idx = detalle.indexOf("→");
        if (idx == -1) return "-";
        String posterior = detalle.substring(idx + 2).trim();
        // Extraer hasta el "@"
        int at = posterior.indexOf("@");
        if (at == -1) return posterior;
        return posterior.substring(0, at);
    }

    /**
     * Extrae la dirección anterior desde el detalle.
     * Formato esperado: "Proceso 'X' desplazado de PAR1@dir1 → PAR2@dir2"
     */
    private String extraerDireccionAnterior(String detalle) {
        if (detalle == null) return "-";
        // Buscar " de " y extraer lo que viene después hasta "→"
        int deIdx = detalle.indexOf(" de ");
        if (deIdx == -1) return "-";
        String parte = detalle.substring(deIdx + 4).trim();
        // Buscar el "@" para obtener la dirección
        int atIdx = parte.indexOf("@");
        if (atIdx == -1) return "-";
        String dirAnterior = parte.substring(atIdx + 1);
        // Extraer hasta el espacio antes del "→"
        int flecha = dirAnterior.indexOf("→");
        if (flecha == -1) return dirAnterior.trim();
        return dirAnterior.substring(0, flecha).trim();
    }

    public void cerrar() { stage.close(); }
    public Stage getStage() { return stage; }
}