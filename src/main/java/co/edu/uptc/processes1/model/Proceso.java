package co.edu.uptc.processes1.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigInteger;

public class Proceso {

    private final IntegerProperty id;
    private final StringProperty nombre;
    private final ObjectProperty<BigInteger> tiempoRestante;
    private final ObjectProperty<BigInteger> tamanioMemoria;
    private final BooleanProperty pasaPorBloqueado;
    private final StringProperty estadoActual;
    private final BooleanProperty excedeTamanoParticion;
    private Particion particion;

    public Proceso(
        int id,
        String nombre,
        BigInteger tiempoRestante,
        BigInteger tamanioMemoria,
        boolean pasaPorBloqueado
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.tiempoRestante = new SimpleObjectProperty<>(BigInteger.ZERO);
        this.tamanioMemoria = new SimpleObjectProperty<>(BigInteger.ZERO);
        this.tiempoRestante.set(tiempoRestante);
        this.tamanioMemoria.set(tamanioMemoria);
        this.pasaPorBloqueado = new SimpleBooleanProperty(pasaPorBloqueado);
        this.estadoActual = new SimpleStringProperty("Listo");
        this.excedeTamanoParticion = new SimpleBooleanProperty(false);
        this.particion = null;
    }

    public Proceso(int id, String nombre, BigInteger tiempoRestante, BigInteger tamanioMemoria) {
        this(id, nombre, tiempoRestante, tamanioMemoria, false);
    }

    public Proceso(String nombre, BigInteger tiempoRestante, BigInteger tamanioMemoria) {
        this(0, nombre, tiempoRestante, tamanioMemoria);
    }

    public Proceso(
        String nombre,
        BigInteger tiempoRestante,
        BigInteger tamanioMemoria,
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

    public BigInteger getTiempoRestante() {
        return tiempoRestante.get();
    }

    public BigInteger getTiempo() {
        return getTiempoRestante();
    }

    public BigInteger getTamanioMemoria() {
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

    public void setTiempoRestante(BigInteger tiempoRestante) {
        this.tiempoRestante.set(tiempoRestante);
    }

    public void setTiempo(BigInteger tiempo) {
        setTiempoRestante(tiempo);
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual.set(estadoActual);
    }

    public void setTamanioMemoria(BigInteger tamanioMemoria) {
        this.tamanioMemoria.set(tamanioMemoria);
    }

    public void setParticion(Particion particion) {
        this.particion = particion;
    }

    public boolean isExcedeTamanoParticion() {
        return excedeTamanoParticion.get();
    }

    public void setExcedeTamanoParticion(boolean excedeTamanoParticion) {
        this.excedeTamanoParticion.set(excedeTamanoParticion);
    }

    public BooleanProperty excedeTamanoParticionProperty() {
        return excedeTamanoParticion;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public ObjectProperty<BigInteger> tiempoRestanteProperty() {
        return tiempoRestante;
    }

    public ObjectProperty<BigInteger> tiempoProperty() {
        return tiempoRestante;
    }

    public ObjectProperty<BigInteger> tamanioMemoriaProperty() {
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
        copia.setExcedeTamanoParticion(isExcedeTamanoParticion());
        return copia;
    }

    @Override
    public String toString() {
        return getNombre() + " (" + getTiempoRestante().divide(BigInteger.valueOf(1000L)) + ")";
    }
}
