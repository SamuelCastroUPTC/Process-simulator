package co.edu.uptc.processes1.app;

import javafx.application.Application;

/**
 * Punto de entrada recomendado para IDE/Maven.
 * Evita ejecutar directamente la subclase de Application.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
