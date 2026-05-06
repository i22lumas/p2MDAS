package es.uco.pw.pw2526.api;

import es.uco.pw.pw2526.model.Repository.AlquilerRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.ReservaRepository;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.alquiler.Alquiler;
import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.Socio.Socio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controlador REST para la gestión de alquileres según la Práctica 2
 * Implementa las especificaciones del guion:
 * - Temporada baja (octubre-abril): máximo 3 días
 * - Temporada alta (mayo-septiembre): solo 7 o 14 días
 */
@RestController
@RequestMapping("/api/alquileres")
@CrossOrigin(origins = "*")
public class AlquilerRestController {

    private final AlquilerRepository alquilerRepository;
    private final EmbarcacionRepository embarcacionRepository;
    private final ReservaRepository reservaRepository;
    private final SocioRepository socioRepository;

    public AlquilerRestController(AlquilerRepository alquilerRepository,
            EmbarcacionRepository embarcacionRepository,
            ReservaRepository reservaRepository,
            SocioRepository socioRepository) {
        this.alquilerRepository = alquilerRepository;
        this.embarcacionRepository = embarcacionRepository;
        this.reservaRepository = reservaRepository;
        this.socioRepository = socioRepository;
    }

    @GetMapping
    public ResponseEntity<List<Alquiler>> obtenerTodosAlquileres() {
        try {
            return procesarObtenerTodosAlquileres();
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/futuros")
    public ResponseEntity<?> obtenerAlquileresFuturos(@RequestParam(required = false) String fecha) {
        try {
            return procesarObtenerAlquileresFuturos(fecha);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener alquileres futuros"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAlquilerPorId(@PathVariable int id) {
        try {
            return procesarObtenerAlquilerPorId(id);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el alquiler"));
        }
    }

    @GetMapping("/disponibles")
    public ResponseEntity<?> obtenerEmbarcacionesDisponibles(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        try {
            return procesarObtenerEmbarcacionesDisponibles(fechaInicio, fechaFin);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener embarcaciones disponibles"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearAlquiler(@RequestBody Alquiler nuevoAlquiler) {
        try {
            return procesarCreacionAlquiler(nuevoAlquiler);
        } catch (Exception excepcion) {
            return respuestaErrorInterno(excepcion);
        }
    }

    @PatchMapping("/{id}/agregar-socio")
    public ResponseEntity<?> agregarSocio(@PathVariable int id, @RequestParam String dniSocio) {
        try {
            return procesarAgregarSocio(id, dniSocio);
        } catch (Exception excepcion) {
            return respuestaErrorInterno(excepcion);
        }
    }

    @PatchMapping("/{id}/quitar-socio")
    public ResponseEntity<?> quitarSocio(@PathVariable int id, @RequestParam String dniSocio) {
        try {
            return procesarQuitarSocio(id, dniSocio);
        } catch (Exception excepcion) {
            return respuestaErrorInterno(excepcion);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarAlquiler(@PathVariable int id) {
        try {
            return procesarCancelacionAlquiler(id);
        } catch (Exception excepcion) {
            return respuestaErrorInterno(excepcion);
        }
    }

    // ==================== Métodos de procesamiento ====================

    private ResponseEntity<List<Alquiler>> procesarObtenerTodosAlquileres() {
        List<Alquiler> alquileres = alquilerRepository.obtenerTodosAlquileres();
        return ResponseEntity.ok(alquileres);
    }

    private ResponseEntity<?> procesarObtenerAlquileresFuturos(String fecha) {
        List<Alquiler> alquileres = alquilerRepository.obtenerAlquileresFuturos();

        if (fecha != null && !fecha.isEmpty()) {
            alquileres = filtrarAlquileresPorFecha(alquileres, fecha);
        }

        return ResponseEntity.ok(alquileres);
    }

    private List<Alquiler> filtrarAlquileresPorFecha(List<Alquiler> alquileres, String fecha) {
        LocalDate fechaFiltro = LocalDate.parse(fecha);
        return alquileres.stream()
                .filter(alquiler -> !alquiler.getFechaInicio().isBefore(fechaFiltro))
                .toList();
    }

    private ResponseEntity<?> procesarObtenerAlquilerPorId(int id) {
        Alquiler alquilerEncontrado = encontrarAlquilerPorId(id);

        if (alquilerEncontrado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
        }

        return ResponseEntity.ok(alquilerEncontrado);
    }

    private ResponseEntity<?> procesarObtenerEmbarcacionesDisponibles(String fechaInicio, String fechaFin) {
        LocalDate inicio = parsearFecha(fechaInicio);
        LocalDate fin = parsearFecha(fechaFin);

        if (inicio == null || fin == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD"));
        }

        ResponseEntity<?> errorValidacion = validarRangoFechas(inicio, fin);
        if (errorValidacion != null) {
            return errorValidacion;
        }

        List<Embarcacion> disponibles = filtrarEmbarcacionesDisponibles(inicio, fin);
        return ResponseEntity.ok(disponibles);
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (Exception excepcion) {
            return null;
        }
    }

    private ResponseEntity<?> validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha de inicio no puede ser en el pasado"));
        }

        if (fin.isBefore(inicio)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha de fin no puede ser anterior a la fecha de inicio"));
        }

        validarDuracionSegunTemporada(inicio, fin);
        return null;
    }

    private List<Embarcacion> filtrarEmbarcacionesDisponibles(LocalDate inicio, LocalDate fin) {
        List<Embarcacion> todas = embarcacionRepository.obtenerEmbarcaciones();
        List<Embarcacion> disponibles = new ArrayList<>();

        for (Embarcacion embarcacion : todas) {
            if (estaEmbarcacionDisponible(embarcacion.getMatricula(), inicio, fin)) {
                disponibles.add(embarcacion);
            }
        }

        return disponibles;
    }

    private boolean estaEmbarcacionDisponible(String matricula, LocalDate inicio, LocalDate fin) {
        boolean libreAlquiler = alquilerRepository.estaDisponible(matricula, inicio, fin);
        boolean libreReserva = reservaRepository.estaDisponible(matricula, inicio, fin);
        return libreAlquiler && libreReserva;
    }

    // ==================== Procesamiento de creación ====================

    private ResponseEntity<?> procesarCreacionAlquiler(Alquiler nuevoAlquiler) {
        ResponseEntity<?> errorDatos = validarDatosObligatoriosAlquiler(nuevoAlquiler);
        if (errorDatos != null) {
            return errorDatos;
        }

        Socio titular = buscarYValidarTitular(nuevoAlquiler.getDniSocioTitular());
        if (titular == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error",
                            "Socio titular con DNI " + nuevoAlquiler.getDniSocioTitular() + " no encontrado"));
        }

        ResponseEntity<?> errorPatron = validarTituloPatron(titular);
        if (errorPatron != null) {
            return errorPatron;
        }

        nuevoAlquiler.setIdSocioTitular(titular.getId());

        Embarcacion embarcacion = buscarEmbarcacionPorMatricula(nuevoAlquiler.getMatriculaEmbarcacion());
        if (embarcacion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Embarcación con matrícula " + nuevoAlquiler.getMatriculaEmbarcacion()
                            + " no encontrada"));
        }

        ResponseEntity<?> errorFechas = validarFechasAlquiler(nuevoAlquiler);
        if (errorFechas != null) {
            return errorFechas;
        }

        ResponseEntity<?> errorPlazas = validarPlazasAlquiler(nuevoAlquiler, embarcacion);
        if (errorPlazas != null) {
            return errorPlazas;
        }

        ResponseEntity<?> errorDisponibilidad = verificarDisponibilidadAlquiler(nuevoAlquiler);
        if (errorDisponibilidad != null) {
            return errorDisponibilidad;
        }

        calcularYAsignarPrecio(nuevoAlquiler);
        return persistirAlquiler(nuevoAlquiler);
    }

    private ResponseEntity<?> validarDatosObligatoriosAlquiler(Alquiler alquiler) {
        if (alquiler.getDniSocioTitular() == null || alquiler.getDniSocioTitular().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El DNI del socio titular es obligatorio"));
        }

        if (alquiler.getMatriculaEmbarcacion() == null || alquiler.getMatriculaEmbarcacion().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La matrícula de la embarcación es obligatoria"));
        }

        if (alquiler.getFechaInicio() == null || alquiler.getFechaFin() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las fechas de inicio y fin son obligatorias"));
        }

        return null;
    }

    private Socio buscarYValidarTitular(String dniTitular) {
        return socioRepository.obtenerSocioPorDni(dniTitular);
    }

    private ResponseEntity<?> validarTituloPatron(Socio titular) {
        if (!titular.esPatron()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El socio titular no tiene título de patrón de embarcación"));
        }
        return null;
    }

    private Embarcacion buscarEmbarcacionPorMatricula(String matricula) {
        return embarcacionRepository.obtenerEmbarcaciones().stream()
                .filter(embarcacion -> embarcacion.getMatricula().equals(matricula))
                .findFirst()
                .orElse(null);
    }

    private ResponseEntity<?> validarFechasAlquiler(Alquiler alquiler) {
        if (alquiler.getFechaInicio().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha de inicio no puede ser en el pasado"));
        }

        if (alquiler.getFechaFin().isBefore(alquiler.getFechaInicio())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha de fin no puede ser anterior a la fecha de inicio"));
        }

        validarDuracionSegunTemporada(alquiler.getFechaInicio(), alquiler.getFechaFin());
        return null;
    }

    private ResponseEntity<?> validarPlazasAlquiler(Alquiler alquiler, Embarcacion embarcacion) {
        if (alquiler.getPlazasSolicitadas() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El número de plazas solicitadas debe ser mayor a 0"));
        }

        if (alquiler.getPlazasSolicitadas() > embarcacion.getNumeroPlazas()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Plazas solicitadas (" + alquiler.getPlazasSolicitadas() +
                                    ") exceden la capacidad de la embarcación ("
                                    + embarcacion.getNumeroPlazas() + ")"));
        }

        return null;
    }

    private ResponseEntity<?> verificarDisponibilidadAlquiler(Alquiler alquiler) {
        boolean disponible = estaEmbarcacionDisponible(
                alquiler.getMatriculaEmbarcacion(),
                alquiler.getFechaInicio(),
                alquiler.getFechaFin());

        if (!disponible) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "La embarcación no está disponible en las fechas seleccionadas"));
        }

        return null;
    }

    private void calcularYAsignarPrecio(Alquiler alquiler) {
        long dias = calcularDias(alquiler.getFechaInicio(), alquiler.getFechaFin());
        double precio = 20.0 * alquiler.getPlazasSolicitadas() * dias;
        alquiler.setPrecioTotal(precio);
    }

    private ResponseEntity<?> persistirAlquiler(Alquiler alquiler) {
        int idAlquiler = alquilerRepository.insertarAlquilerYRetornarId(alquiler);

        if (idAlquiler <= 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear el alquiler en la base de datos"));
        }

        alquiler.setIdAlquiler(idAlquiler);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(construirRespuestaCreacion(idAlquiler, alquiler));
    }

    private Map<String, Object> construirRespuestaCreacion(int idAlquiler, Alquiler alquiler) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Alquiler creado exitosamente");
        respuesta.put("idAlquiler", idAlquiler);
        respuesta.put("alquiler", alquiler);
        return respuesta;
    }

    // ==================== Procesamiento de agregar/quitar socio ====================

    private ResponseEntity<?> procesarAgregarSocio(int id, String dniSocio) {
        ResponseEntity<?> errorParametro = validarParametroDni(dniSocio);
        if (errorParametro != null) {
            return errorParametro;
        }

        Alquiler alquilerExistente = encontrarAlquilerPorId(id);
        ResponseEntity<?> errorAlquiler = validarAlquilerModificable(alquilerExistente, id);
        if (errorAlquiler != null) {
            return errorAlquiler;
        }

        Socio socio = socioRepository.obtenerSocioPorDni(dniSocio);
        if (socio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Socio con DNI " + dniSocio + " no encontrado"));
        }

        ResponseEntity<?> errorDuplicado = validarSocioNoEnAlquiler(dniSocio, alquilerExistente);
        if (errorDuplicado != null) {
            return errorDuplicado;
        }

        int totalTripulantes = calcularTotalTripulantes(alquilerExistente);
        if (totalTripulantes >= alquilerExistente.getPlazasSolicitadas()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay plazas disponibles en este alquiler"));
        }

        boolean agregadoConExito = alquilerRepository.agregarSocioNoTitular(id, dniSocio);
        if (!agregadoConExito) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al agregar el socio al alquiler"));
        }

        double nuevoPrecio = calcularNuevoPrecioAlquiler(alquilerExistente, totalTripulantes + 1);
        return ResponseEntity.ok(construirRespuestaModificacion(
                "Socio agregado correctamente al alquiler", id, dniSocio, nuevoPrecio, totalTripulantes + 1));
    }

    private ResponseEntity<?> procesarQuitarSocio(int id, String dniSocio) {
        ResponseEntity<?> errorParametro = validarParametroDni(dniSocio);
        if (errorParametro != null) {
            return errorParametro;
        }

        Alquiler alquilerExistente = encontrarAlquilerPorId(id);
        ResponseEntity<?> errorAlquiler = validarAlquilerModificable(alquilerExistente, id);
        if (errorAlquiler != null) {
            return errorAlquiler;
        }

        if (dniSocio.equals(alquilerExistente.getDniSocioTitular())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede quitar al socio titular del alquiler"));
        }

        if (alquilerExistente.getDnisTripulantes() == null
                || !alquilerExistente.getDnisTripulantes().contains(dniSocio)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El socio no está en la lista de tripulantes de este alquiler"));
        }

        boolean quitadoConExito = alquilerRepository.quitarSocioNoTitular(id, dniSocio);
        if (!quitadoConExito) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el socio del alquiler"));
        }

        int totalTripulantes = calcularTotalTripulantes(alquilerExistente);
        double nuevoPrecio = calcularNuevoPrecioAlquiler(alquilerExistente, totalTripulantes - 1);
        return ResponseEntity.ok(construirRespuestaModificacion(
                "Socio eliminado correctamente del alquiler", id, dniSocio, nuevoPrecio, totalTripulantes - 1));
    }

    private ResponseEntity<?> validarParametroDni(String dniSocio) {
        if (dniSocio == null || dniSocio.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El parámetro 'dniSocio' es obligatorio"));
        }
        return null;
    }

    private ResponseEntity<?> validarAlquilerModificable(Alquiler alquiler, int id) {
        if (alquiler == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
        }

        if (alquiler.getFechaInicio().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede modificar un alquiler pasado"));
        }

        return null;
    }

    private ResponseEntity<?> validarSocioNoEnAlquiler(String dniSocio, Alquiler alquiler) {
        if (dniSocio.equals(alquiler.getDniSocioTitular())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El socio titular ya está incluido en el alquiler"));
        }

        if (alquiler.getDnisTripulantes() != null && alquiler.getDnisTripulantes().contains(dniSocio)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El socio ya está en la lista de tripulantes"));
        }

        return null;
    }

    private int calcularTotalTripulantes(Alquiler alquiler) {
        return 1 + (alquiler.getDnisTripulantes() != null ? alquiler.getDnisTripulantes().size() : 0);
    }

    private double calcularNuevoPrecioAlquiler(Alquiler alquiler, int totalPersonas) {
        long dias = calcularDias(alquiler.getFechaInicio(), alquiler.getFechaFin());
        return 20.0 * totalPersonas * dias;
    }

    private Map<String, Object> construirRespuestaModificacion(String mensaje, int idAlquiler, String dniSocio,
            double nuevoPrecio, int plazasOcupadas) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", mensaje);
        respuesta.put("idAlquiler", idAlquiler);
        respuesta.put("dniSocio", dniSocio);
        respuesta.put("nuevoPrecio", nuevoPrecio);
        respuesta.put("plazasOcupadas", plazasOcupadas);
        return respuesta;
    }

    // ==================== Procesamiento de cancelación ====================

    private ResponseEntity<?> procesarCancelacionAlquiler(int id) {
        Alquiler alquilerExistente = encontrarAlquilerPorId(id);

        if (alquilerExistente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
        }

        if (alquilerExistente.getFechaInicio().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede cancelar un alquiler pasado"));
        }

        boolean canceladoConExito = alquilerRepository.cancelarAlquiler(id);
        if (!canceladoConExito) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cancelar el alquiler"));
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Alquiler cancelado correctamente");
        respuesta.put("idAlquiler", id);
        return ResponseEntity.ok(respuesta);
    }

    // ==================== Métodos utilitarios ====================

    private Alquiler encontrarAlquilerPorId(int id) {
        List<Alquiler> todos = alquilerRepository.obtenerTodosAlquileres();
        return todos.stream()
                .filter(alquiler -> alquiler.getIdAlquiler() == id)
                .findFirst()
                .orElse(null);
    }

    private long calcularDias(LocalDate inicio, LocalDate fin) {
        return ChronoUnit.DAYS.between(inicio, fin) + 1;
    }

    /**
     * Valida la duración del alquiler según la temporada (SEGÚN GUION):
     * - Temporada baja (octubre a abril): máximo 3 días
     * - Temporada alta (mayo a septiembre): solo 7 o 14 días
     *
     * @throws IllegalArgumentException si la duración no es válida para la
     *                                  temporada
     */
    private void validarDuracionSegunTemporada(LocalDate inicio, LocalDate fin) {
        long dias = calcularDias(inicio, fin);
        int mes = inicio.getMonthValue();

        if (esTemporadaBaja(mes) && dias > 3) {
            throw new IllegalArgumentException(
                    "En temporada baja (octubre-abril) el máximo es 3 días. Seleccionaste: " + dias + " días.");
        }

        if (esTemporadaAlta(mes) && dias != 7 && dias != 14) {
            throw new IllegalArgumentException(
                    "En temporada alta (mayo-septiembre) solo se permiten 7 días (1 semana) o 14 días (2 semanas). Seleccionaste: "
                            + dias + " días.");
        }
    }

    /**
     * Determina si un mes corresponde a temporada alta
     */
    private boolean esTemporadaAlta(int mes) {
        return mes >= 5 && mes <= 9;
    }

    /**
     * Determina si un mes corresponde a temporada baja
     */
    private boolean esTemporadaBaja(int mes) {
        return mes >= 10 || mes <= 4;
    }

    private ResponseEntity<?> respuestaErrorInterno(Exception excepcion) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno: " + excepcion.getMessage()));
    }
}