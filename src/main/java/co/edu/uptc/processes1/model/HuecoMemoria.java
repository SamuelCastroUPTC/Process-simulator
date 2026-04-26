package co.edu.uptc.processes1.model;

import java.math.BigInteger;

public class HuecoMemoria {

    private BigInteger direccionInicio;
    private BigInteger tamanio;

    public HuecoMemoria(BigInteger direccionInicio, BigInteger tamanio) {
        this.direccionInicio = direccionInicio;
        this.tamanio = tamanio;
    }

    public BigInteger getDireccionInicio() {
        return direccionInicio;
    }

    public void setDireccionInicio(BigInteger direccionInicio) {
        this.direccionInicio = direccionInicio;
    }

    public BigInteger getTamanio() {
        return tamanio;
    }

    public void setTamanio(BigInteger tamanio) {
        this.tamanio = tamanio;
    }

    public BigInteger getDireccionFin() {
        return direccionInicio.add(tamanio);
    }

    @Override
    public String toString() {
        return "Hueco[" + direccionInicio + " - " + getDireccionFin() + ", tam=" + tamanio + "]";
    }
}
