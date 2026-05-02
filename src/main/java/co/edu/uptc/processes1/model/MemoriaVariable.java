package co.edu.uptc.processes1.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MemoriaVariable {

    public record ResultadoAsignacion(BigInteger direccion, boolean compactoAntes) {}

    private final BigInteger tamanioTotal;
    private final List<BloqueMemoria> bloquesOcupados;
    private final List<HuecoMemoria> huecos;

    public MemoriaVariable(BigInteger tamanioTotal) {
        this.tamanioTotal = tamanioTotal;
        this.bloquesOcupados = new ArrayList<>();
        this.huecos = new ArrayList<>();
        huecos.add(new HuecoMemoria(BigInteger.ZERO, tamanioTotal));
    }

    public ResultadoAsignacion asignar(int idProceso, String nombreProceso, BigInteger tamanio) {
        boolean compactoAntes = false;
        
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
                return new ResultadoAsignacion(direccionAsignada, compactoAntes);
            }
        }

        // Si llegamos aquí, no hay hueco individual suficiente.
        // Verificamos si hay espacio libre total suficiente para compactar.
        if (getEspacioLibreTotal().compareTo(tamanio) >= 0) {
            compactar();
            // Intentamos asignar nuevamente después de compactar
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
                    return new ResultadoAsignacion(direccionAsignada, true);
                }
            }
        }

        return new ResultadoAsignacion(null, false);
    }

    public void compactar() {
        // a) Ordena bloquesOcupados por direccionInicio ascendente.
        List<BloqueMemoria> ordenados = bloquesOcupados.stream()
            .sorted(Comparator.comparing(BloqueMemoria::getDireccionInicio))
            .toList();

        // b) Reasigna direcciones de forma contigua empezando desde BigInteger.ZERO
        BigInteger cursor = BigInteger.ZERO;
        List<BloqueMemoria> bloquesCompactados = new ArrayList<>();
        for (BloqueMemoria bloque : ordenados) {
            BloqueMemoria nuevoBloque = new BloqueMemoria(
                bloque.getIdProceso(),
                bloque.getNombreProceso(),
                cursor,
                bloque.getTamanio()
            );
            bloquesCompactados.add(nuevoBloque);
            cursor = cursor.add(bloque.getTamanio());
        }
        bloquesOcupados.clear();
        bloquesOcupados.addAll(bloquesCompactados);

        // c) Recalcula huecos
        huecos.clear();
        BigInteger espacioOcupado = cursor;
        if (espacioOcupado.compareTo(tamanioTotal) < 0) {
            huecos.add(new HuecoMemoria(espacioOcupado, tamanioTotal.subtract(espacioOcupado)));
        }
    }

    public boolean liberar(int idProceso) {
        boolean removido = bloquesOcupados.removeIf(b -> b.getIdProceso() == idProceso);
        if (!removido) {
            return false;
        }

        int huecoAntes = huecos.size();
        recalcularHuecos();
        condensar();
        return huecos.size() < huecoAntes + 1;
    }

    public boolean liberarSinCondensar(int idProceso) {
        boolean removido = bloquesOcupados.removeIf(b -> b.getIdProceso() == idProceso);
        if (!removido) {
            return false;
        }

        recalcularHuecos();
        return true;
    }

    private void recalcularHuecos() {
        huecos.clear();

        List<BloqueMemoria> ordenados = bloquesOcupados.stream()
            .sorted(Comparator.comparing(BloqueMemoria::getDireccionInicio))
            .toList();

        BigInteger cursor = BigInteger.ZERO;
        for (BloqueMemoria bloque : ordenados) {
            if (cursor.compareTo(bloque.getDireccionInicio()) < 0) {
                huecos.add(new HuecoMemoria(cursor,
                    bloque.getDireccionInicio().subtract(cursor)));
            }
            cursor = bloque.getDireccionFin();
        }

        if (cursor.compareTo(tamanioTotal) < 0) {
            huecos.add(new HuecoMemoria(cursor,
                tamanioTotal.subtract(cursor)));
        }
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
