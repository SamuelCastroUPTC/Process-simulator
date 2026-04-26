package co.edu.uptc.processes1.model;

import java.math.BigInteger;

public class BloqueMemoria {

    private final int idProceso;
    private final String nombreProceso;
    private final BigInteger direccionInicio;
    private final BigInteger tamanio;

    public BloqueMemoria(int idProceso, String nombreProceso, BigInteger direccionInicio, BigInteger tamanio) {
        this.idProceso = idProceso;
        this.nombreProceso = nombreProceso;
        this.direccionInicio = direccionInicio;
        this.tamanio = tamanio;
    }

    public int getIdProceso() {
        return idProceso;
    }

    public String getNombreProceso() {
        return nombreProceso;
    }

    public BigInteger getDireccionInicio() {
        return direccionInicio;
    }

    public BigInteger getTamanio() {
        return tamanio;
    }

    public BigInteger getDireccionFin() {
        return direccionInicio.add(tamanio);
    }
}
