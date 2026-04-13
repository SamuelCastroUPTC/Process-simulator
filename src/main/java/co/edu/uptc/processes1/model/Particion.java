package co.edu.uptc.processes1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Particion {

    private final int id;
    private String nombre;
    private long tamanoTotal;
    private final List<Proceso> procesosAlojados;
    private boolean ocupada = false;

    public Particion(int id, String nombre, long tamanoTotal) {
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

    public long getTamanoTotal() {
        return tamanoTotal;
    }

    public void setTamanoTotal(long tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
    }

    public long getEspacioDisponible() {
        return ocupada ? 0 : tamanoTotal;
    }

    public boolean estaDisponible(long tamanioRequerido) {
        return !ocupada && tamanioRequerido <= tamanoTotal;
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
        if (proceso.getTamanioMemoria() > tamanoTotal) {
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
