package co.edu.uptc.processes1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.BigInteger;

public class Particion {

    private final int id;
    private String nombre;
    private BigInteger tamanoTotal;
    private final List<Proceso> procesosAlojados;
    private boolean ocupada = false;

    public Particion(int id, String nombre, BigInteger tamanoTotal) {
        this.id = id;
        this.nombre = nombre;
        this.tamanoTotal = tamanoTotal;
        this.procesosAlojados = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigInteger getTamanoTotal() {
        return tamanoTotal;
    }

    public void setTamanoTotal(BigInteger tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
    }

    public BigInteger getEspacioDisponible() {
        return ocupada ? BigInteger.ZERO : tamanoTotal;
    }

    public boolean estaDisponible(BigInteger tamanioRequerido) {
        return !ocupada && tamanioRequerido.compareTo(tamanoTotal) <= 0;
    }

    public void ocupar() {
        this.ocupada = true;
    }

    public void liberar() {
        this.ocupada = false;
    }

    public List<Proceso> getProcesosAlojados() {
        return Collections.unmodifiableList(procesosAlojados);
    }

    public boolean estaLibre() {
        return procesosAlojados.isEmpty();
    }

    public void agregarProceso(Proceso proceso) {
        if (proceso == null) {
            return;
        }
        if (proceso.getTamanioMemoria().compareTo(tamanoTotal) > 0) {
            return;
        }
        procesosAlojados.add(proceso);
        ocupar();
    }

    public void removerProceso(Proceso proceso) {
        if (proceso == null) {
            return;
        }
        procesosAlojados.remove(proceso);
        if (procesosAlojados.isEmpty()) {
            liberar();
        }
    }

    public void liberarProceso() {
        procesosAlojados.clear();
        liberar();
    }

    @Override
    public String toString() {
        return this.nombre;
    }
}
