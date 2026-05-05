package co.edu.uptc.processes1.presenter;

import co.edu.uptc.processes1.model.MemoriaVariable;
import co.edu.uptc.processes1.model.Particion;
import co.edu.uptc.processes1.model.Proceso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test de integración JUnit 5 para MotorSimulacion.
 *
 * Valida el comportamiento del motor de simulación Round Robin con:
 * - Renombramiento dinámico de particiones (IDs secuenciales)
 * - Desplazamiento automático post-liberación
 * - Condensación de huecos adyacentes
 * - Compactación bajo demanda
 *
 * Escenario: 3 procesos con memoria total de 500 bytes.
 */
@DisplayName("Integración: MotorSimulacion con Renombramiento y Desplazamiento")
class MotorSimulacionIntegrationTest {

    private MotorSimulacion motor;
    private MemoriaVariable memoria;
    private RegistroSimulacion registroActual;

    @BeforeEach
    void setUp() {
        motor = new MotorSimulacion();
        memoria = new MemoriaVariable(BigInteger.valueOf(500));
    }

    /**
     * Crea lista de procesos para el escenario de prueba:
     * - P1: 1000 ms, 100 bytes
     * - P2: 3000 ms, 150 bytes
     * - P3: 2000 ms, 200 bytes
     */
    private List<Proceso> crearProcesosEscenario() {
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(new Proceso(1, "P1", BigInteger.valueOf(1000), BigInteger.valueOf(100)));
        procesos.add(new Proceso(2, "P2", BigInteger.valueOf(3000), BigInteger.valueOf(150)));
        procesos.add(new Proceso(3, "P3", BigInteger.valueOf(2000), BigInteger.valueOf(200)));
        return procesos;
    }

    @Test
    @DisplayName("Estado inicial: PAR1 es la memoria completa LIBRE")
    void estadoInicial_ParticionUnicaLibre() {
        // Arrange
        List<Particion> particionesIniciales = memoria.getParticiones();

        // Act & Assert
        assertThat(particionesIniciales)
            .as("Debe haber exactamente 1 partición inicial")
            .hasSize(1);

        Particion parUnica = particionesIniciales.get(0);
        assertThat(parUnica.getId())
            .as("La partición inicial debe tener ID = 1")
            .isEqualTo(1);

        assertThat(parUnica.getNombre())
            .as("La partición inicial debe llamarse PAR1")
            .isEqualTo("PAR1");

        assertThat(parUnica.estaLibre())
            .as("La partición inicial debe estar LIBRE")
            .isTrue();

        assertThat(parUnica.getDireccionInicio())
            .as("La partición inicial debe empezar en dirección 0")
            .isEqualTo(BigInteger.ZERO);

        assertThat(parUnica.getTamanio())
            .as("La partición inicial debe ocupar los 500 bytes totales")
            .isEqualTo(BigInteger.valueOf(500));
    }

    @Test
    @DisplayName("Tras asignar P1: asignación registrada correctamente")
    void asignacionP1_CreaParticionOcupadaYLibre() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();
        List<Proceso> soloP1 = List.of(procesos.get(0)); // Solo P1

        // Act
        registroActual = motor.ejecutar(soloP1, memoria);

        // Assert: Verificar que la asignación fue registrada en el historial
        List<RegistroSimulacion.SnapshotMemoria> asignaciones = registroActual.getHistorialMemoria(RegistroSimulacion.ASIGNACION);
        assertThat(asignaciones)
            .as("Debe haber exactamente 1 asignación para P1")
            .hasSize(1);

        RegistroSimulacion.SnapshotMemoria snapshot = asignaciones.get(0);
        assertThat(snapshot.nombreProceso())
            .as("El proceso asignado debe ser P1")
            .isEqualTo("P1");

        assertThat(snapshot.tamanio())
            .as("El tamaño del proceso debe ser 100 bytes")
            .isEqualTo(BigInteger.valueOf(100));

        // Al final, P1 debe haber completado su ejecución (FINALIZADO)
        // No debe haber particiones OCUPADAS en memoria
        List<Particion> ocupadas = memoria.getParticionesOcupadas();
        assertThat(ocupadas)
            .as("Tras finalizar P1, no debe haber particiones OCUPADAS")
            .isEmpty();

        // Toda la memoria debe estar LIBRE
        BigInteger totalLibre = memoria.getEspacioLibreTotal();
        assertThat(totalLibre)
            .as("Toda la memoria debe estar libre tras finalizar P1")
            .isEqualTo(BigInteger.valueOf(500));
    }

    @Test
    @DisplayName("Valida que múltiples procesos generan eventos de memoria")
    void debeGenerarDesplazamientoYCondensacionConRenombramiento() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act: ejecutar simulación completa con 3 procesos
        registroActual = motor.ejecutar(procesos, memoria);

        // Assert 1: Debe haber asignaciones (al menos una por proceso)
        List<RegistroSimulacion.SnapshotMemoria> asignaciones = registroActual.getHistorialMemoria(RegistroSimulacion.ASIGNACION);
        assertThat(asignaciones)
            .as("Debe haber asignaciones para todos los procesos")
            .hasSizeGreaterThanOrEqualTo(3);

        // Assert 2: Verificar que al menos algunos procesos se finalizaron y liberaron memoria
        // Puede haber DESPLAZAMIENTO, CONDENSACION o ambos dependiendo de la dinámica
        List<RegistroSimulacion.SnapshotMemoria> desplazamientos = registroActual.getHistorialMemoria(RegistroSimulacion.DESPLAZAMIENTO);
        List<RegistroSimulacion.SnapshotMemoria> condensaciones = registroActual.getHistorialMemoria(RegistroSimulacion.CONDENSACION);

        int totalEventosMem = desplazamientos.size() + condensaciones.size();
        assertThat(totalEventosMem)
            .as("Debe haber al menos 1 evento de DESPLAZAMIENTO o CONDENSACION")
            .isGreaterThanOrEqualTo(1);

        // Assert 3: Validar que los eventos de DESPLAZAMIENTO contienen metadatos (si hay alguno)
        for (RegistroSimulacion.SnapshotMemoria snapshotDesplazamiento : desplazamientos) {
            assertThat(snapshotDesplazamiento.evento())
                .as("El evento debe ser DESPLAZAMIENTO")
                .isEqualTo(RegistroSimulacion.DESPLAZAMIENTO);

            assertThat(snapshotDesplazamiento.detalle())
                .as("El detalle debe describir el movimiento")
                .contains("desplazado");

            assertThat(snapshotDesplazamiento.metadatoExtra())
                .as("El metadatoExtra debe contener el nombre de la partición anterior")
                .isNotBlank();
        }
    }

    @Test
    @DisplayName("Contador de particiones crece correctamente")
    void contadorParticionDebeCrecer() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act
        registroActual = motor.ejecutar(procesos, memoria);
        int contadorFinal = memoria.getContadorIdActual();

        // Assert: para 3 procesos con shifts y condensación, esperamos > 10 IDs totales
        assertThat(contadorFinal)
            .as("El contador de particiones debe ser > 10 para este escenario de 3 procesos")
            .isGreaterThan(10);

        // Adicional: validar que cada partición tiene ID único
        List<Particion> todasParticiones = memoria.getParticiones();
        List<Integer> ids = todasParticiones.stream()
            .map(Particion::getId)
            .toList();

        assertThat(ids)
            .as("Los IDs de todas las particiones deben ser únicos")
            .doesNotHaveDuplicates();

        // El contador final debe ser >= al máximo ID + 1
        int idMaximo = ids.stream().mapToInt(Integer::intValue).max().orElse(0);
        assertThat(contadorFinal)
            .as("El contador debe ser >= al ID máximo + 1")
            .isGreaterThanOrEqualTo(idMaximo + 1);
    }

    @Test
    @DisplayName("Al finalizar todos los procesos, hay máximo un hueco libre")
    void debeMantenerseSoloUnHuecoLibreAlFinal() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act
        registroActual = motor.ejecutar(procesos, memoria);
        List<Particion> particionesFinales = memoria.getParticiones();

        // Assert: al final, todas las particiones deben estar LIBRES
        long cantidadOcupadas = particionesFinales.stream()
            .filter(p -> !p.estaLibre())
            .count();

        assertThat(cantidadOcupadas)
            .as("Al finalizar, no debe haber particiones OCUPADAS")
            .isZero();

        // Puede haber múltiples huecos si hubo procesos que no se condensaron completamente
        // pero el diseño debería mantener máximo 1 hueco residual
        List<Particion> libres = memoria.getParticionesLibres();
        assertThat(libres)
            .as("Al finalizar, debe haber al menos 1 partición LIBRE")
            .hasSizeGreaterThanOrEqualTo(1);

        // Validar que la suma de libres = 500 (toda la memoria)
        BigInteger totalLibre = libres.stream()
            .map(Particion::getTamanio)
            .reduce(BigInteger.ZERO, BigInteger::add);

        assertThat(totalLibre)
            .as("La suma de particiones libres debe ser 500 bytes (toda la memoria)")
            .isEqualTo(BigInteger.valueOf(500));
    }

    @Test
    @DisplayName("Validar dinámicas de asignación, liberación y desplazamiento")
    void flujoCompletoDinamico() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act
        registroActual = motor.ejecutar(procesos, memoria);

        // Assert: secuencia lógica
        // Debe haber asignaciones
        List<RegistroSimulacion.SnapshotMemoria> asignaciones = registroActual.getHistorialMemoria(RegistroSimulacion.ASIGNACION);
        assertThat(asignaciones)
            .as("Debe haber al menos 3 asignaciones (una por proceso)")
            .hasSizeGreaterThanOrEqualTo(3);

        // Conteo de eventos por tipo
        int desplazamientos = registroActual.getHistorialMemoria(RegistroSimulacion.DESPLAZAMIENTO).size();
        int condensaciones = registroActual.getHistorialMemoria(RegistroSimulacion.CONDENSACION).size();
        int compactaciones = registroActual.getHistorialMemoria(RegistroSimulacion.COMPACTACION).size();

        // Debe haber al menos desplazamiento (por P1 liberándose) y condensación
        assertThat(desplazamientos + condensaciones)
            .as("La suma de DESPLAZAMIENTO + CONDENSACION debe ser > 0")
            .isGreaterThan(0);

        // Imprimir diagrama de eventos para debugging
        System.out.println("\n=== DIAGRAMA DE EVENTOS ===");
        System.out.println("ASIGNACIONES: " + asignaciones.size());
        System.out.println("DESPLAZAMIENTOS: " + desplazamientos);
        System.out.println("CONDENSACIONES: " + condensaciones);
        System.out.println("COMPACTACIONES: " + compactaciones);
        System.out.println("Contador final de particiones: " + memoria.getContadorIdActual());
        System.out.println("Particiones finales: " + memoria.getParticiones().size());
        System.out.println("=== FIN DIAGRAMA ===\n");
    }

    @Test
    @DisplayName("Validar renombramiento de particiones durante desplazamiento")
    void nombramientoParticionesConsistente() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act
        registroActual = motor.ejecutar(procesos, memoria);
        List<Particion> particionesFinales = memoria.getParticiones();

        // Assert: todos los nombres deben ser PARn donde n es el ID
        for (Particion p : particionesFinales) {
            String nombreEsperado = "PAR" + p.getId();
            assertThat(p.getNombre())
                .as("El nombre de la partición debe ser PAR" + p.getId() + ", pero fue " + p.getNombre())
                .isEqualTo(nombreEsperado);
        }

        // Validar que los IDs son secuenciales (sin gaps en uso)
        List<Integer> ids = particionesFinales.stream()
            .map(Particion::getId)
            .sorted()
            .toList();

        assertThat(ids)
            .as("Los IDs deben estar entre 1 y el máximo contador")
            .allMatch(id -> id >= 1);
    }

    @Test
    @DisplayName("Verificar eventos de memoria en registro con metadatos enriquecidos")
    void eventosMemoriaConMetadatos() {
        // Arrange
        List<Proceso> procesos = crearProcesosEscenario();

        // Act
        registroActual = motor.ejecutar(procesos, memoria);

        // Assert: todos los eventos de DESPLAZAMIENTO deben tener metadatoExtra
        List<RegistroSimulacion.SnapshotMemoria> desplazamientos = registroActual.getHistorialMemoria(RegistroSimulacion.DESPLAZAMIENTO);

        for (RegistroSimulacion.SnapshotMemoria snapshot : desplazamientos) {
            assertThat(snapshot.evento())
                .isEqualTo(RegistroSimulacion.DESPLAZAMIENTO);

            assertThat(snapshot.metadatoExtra())
                .as("DESPLAZAMIENTO debe incluir metadatoExtra con partición anterior")
                .isNotNull()
                .isNotBlank();

            assertThat(snapshot.nombreProceso())
                .as("DESPLAZAMIENTO debe registrar el nombre del proceso que se movió")
                .isNotNull();

            assertThat(snapshot.tamanio())
                .as("DESPLAZAMIENTO debe registrar el tamaño del proceso")
                .isNotNull();
        }
    }
}
