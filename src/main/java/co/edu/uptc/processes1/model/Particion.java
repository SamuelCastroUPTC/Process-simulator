package co.edu.uptc.processes1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Particion {

    private final int id;
    private String nombre;
    private final int tamanoTotal;
    private final List<Proceso> procesosAlojados;

    public Particion(int id, String nombre, int tamanoTotal) {
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

    public int getTamanoTotal() {
        return tamanoTotal;
    }

    public int getEspacioDisponible() {
        return tamanoTotal;
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
    }

    public void removerProceso(Proceso proceso) {
        if (proceso == null) {
            return;
        }
        procesosAlojados.remove(proceso);
    }

    public void liberarProceso() {
        procesosAlojados.clear();
    }

    @Override
    public String toString() {
        return this.nombre;
    }
}
