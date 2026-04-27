package co.edu.uptc.processes1.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MemoriaVariable {

    private final BigInteger tamanioTotal;
    private final List<BloqueMemoria> bloquesOcupados;
    private final List<HuecoMemoria> huecos;

    public MemoriaVariable(BigInteger tamanioTotal) {
        this.tamanioTotal = tamanioTotal;
        this.bloquesOcupados = new ArrayList<>();
        this.huecos = new ArrayList<>();
        huecos.add(new HuecoMemoria(BigInteger.ZERO, tamanioTotal));
    }

    public BigInteger asignar(int idProceso, String nombreProceso, BigInteger tamanio) {
        for (int i = 0; i < huecos.size(); i++) {
            HuecoMemoria hueco = huecos.get(i);
            if (hueco.getTamanio().compareTo(tamanio) >= 0) {
                BigInteger direccionAsignada = hueco.getDireccionInicio();
                bloquesOcupados.add(new BloqueMemoria(idProceso, nombreProceso, direccionAsignada, tamanio));

                if (hueco.getTamanio().compareTo(tamanio) == 0) {
                    huecos.remove(i);
                } else {
                    hueco.setDireccionInicio(hueco.getDireccionInicio().add(tamanio));
                    hueco.setTamanio(hueco.getTamanio().subtract(tamanio));
                }
                return direccionAsignada;
            }
        }
        return null;
    }

    // En MemoriaVariable.java — cambia el tipo de retorno de void a boolean
public boolean liberar(int idProceso) {
    // ... código existente ...
    int huecoAntes = huecos.size();
    condensar();
    return huecos.size() < huecoAntes; // true si se fusionó algo
}

    private void condensar() {
        huecos.sort(Comparator.comparing(HuecoMemoria::getDireccionInicio));
        int i = 0;
        while (i < huecos.size() - 1) {
            HuecoMemoria actual = huecos.get(i);
            HuecoMemoria siguiente = huecos.get(i + 1);
            if (actual.getDireccionFin().equals(siguiente.getDireccionInicio())) {
                actual.setTamanio(actual.getTamanio().add(siguiente.getTamanio()));
                huecos.remove(i + 1);
            } else {
                i++;
            }
        }
    }

    public BigInteger getEspacioLibreTotal() {
        return huecos.stream().map(HuecoMemoria::getTamanio)
            .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public BigInteger getEspacioOcupado() {
        return bloquesOcupados.stream().map(BloqueMemoria::getTamanio)
            .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public List<HuecoMemoria> getHuecos() {
        return List.copyOf(huecos);
    }

    public List<BloqueMemoria> getBloquesOcupados() {
        return List.copyOf(bloquesOcupados);
    }

    public BigInteger getTamanioTotal() {
        return tamanioTotal;
    }
}
