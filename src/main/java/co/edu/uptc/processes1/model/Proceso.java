package co.edu.uptc.processes1.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Proceso {

    private final IntegerProperty id;
    private final StringProperty nombre;
    private final IntegerProperty tiempoRestante;
    private final IntegerProperty tamanioMemoria;
    private final BooleanProperty pasaPorBloqueado;
    private final StringProperty estadoActual;
    private Particion particion;

    public Proceso(
        int id,
        String nombre,
        int tiempoRestante,
        int tamanioMemoria,
        boolean pasaPorBloqueado
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.tiempoRestante = new SimpleIntegerProperty(tiempoRestante);
        this.tamanioMemoria = new SimpleIntegerProperty(tamanioMemoria);
        this.pasaPorBloqueado = new SimpleBooleanProperty(pasaPorBloqueado);
        this.estadoActual = new SimpleStringProperty("Inicio");
        this.particion = null;
    }

    public Proceso(int id, String nombre, int tiempoRestante, int tamanioMemoria) {
        this(id, nombre, tiempoRestante, tamanioMemoria, false);
    }

    public Proceso(String nombre, int tiempoRestante, int tamanioMemoria) {
        this(0, nombre, tiempoRestante, tamanioMemoria);
    }

    public Proceso(
        String nombre,
        int tiempoRestante,
        int tamanioMemoria,
        boolean pasaPorBloqueado
    ) {
        this(0, nombre, tiempoRestante, tamanioMemoria, pasaPorBloqueado);
    }

    public int getId() {
        return id.get();
    }

    public String getNombre() {
        return nombre.get();
    }

    public int getTiempoRestante() {
        return tiempoRestante.get();
    }

    public int getTiempo() {
        return getTiempoRestante();
    }

    public int getTamanioMemoria() {
        return tamanioMemoria.get();
    }

    public boolean isPasaPorBloqueado() {
        return pasaPorBloqueado.get();
    }

    public String getEstadoActual() {
        return estadoActual.get();
    }

    public Particion getParticion() {
        return particion;
    }

    public void setTiempoRestante(int tiempoRestante) {
        this.tiempoRestante.set(tiempoRestante);
    }

    public void setTiempo(int tiempo) {
        setTiempoRestante(tiempo);
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual.set(estadoActual);
    }

    public void setTamanioMemoria(int tamanioMemoria) {
        this.tamanioMemoria.set(tamanioMemoria);
    }

    public void setParticion(Particion particion) {
        this.particion = particion;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public IntegerProperty tiempoRestanteProperty() {
        return tiempoRestante;
    }

    public IntegerProperty tiempoProperty() {
        return tiempoRestante;
    }

    public IntegerProperty tamanioMemoriaProperty() {
        return tamanioMemoria;
    }

    public BooleanProperty pasaPorBloqueadoProperty() {
        return pasaPorBloqueado;
    }

    public StringProperty estadoActualProperty() {
        return estadoActual;
    }

    public Proceso copiar() {
        Proceso copia = new Proceso(
            getId(),
            getNombre(),
            getTiempoRestante(),
            getTamanioMemoria(),
            isPasaPorBloqueado()
        );
        copia.setEstadoActual(getEstadoActual());
        copia.setParticion(getParticion());
        return copia;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", getNombre(), getTiempoRestante() / 1000);
    }
}
