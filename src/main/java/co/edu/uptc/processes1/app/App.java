package co.edu.uptc.processes1.app;

import co.edu.uptc.processes1.presenter.SimuladorPresenter;
import co.edu.uptc.processes1.view.MainView;
import javafx.application.Application;
import javafx.stage.Stage;

/** Arma la aplicacion JavaFX bajo el patron MVP. */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView view = new MainView(primaryStage);
        SimuladorPresenter presenter = new SimuladorPresenter(view);
        view.setPresenter(presenter);
        view.mostrar();
        view.actualizarEstadoMemoria(presenter.getMemoriaVariable());
    }
}
