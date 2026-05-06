package es.uco.pw.pw2526.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.uco.pw.pw2526.model.Repository.ReservaRepository;
import es.uco.pw.pw2526.model.domain.reserva.Reserva;

@RestController
@RequestMapping(path = "/api/reservas", produces = "application/json")
public class ReservaRestController {

    private final ReservaRepository reservaRepository;

    public ReservaRestController(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * 1. Obtener la lista completa de reservas (GET /api/reservas)
     */
    @GetMapping
    public ResponseEntity<List<Reserva>> obtenerTodasReservas() {
        try {
            List<Reserva> reservas = reservaRepository.obtenerTodasLasReservas();
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 2. Obtener la lista de reservas futuras dada una fecha (GET
     * /api/reservas/futuras)
     * Parámetro: fecha (opcional) - Si no se proporciona, se usa la fecha actual
     */
    @GetMapping("/futuras")
    public ResponseEntity<List<Reserva>> obtenerReservasFuturas(@RequestParam(required = false) String fecha) {
        try {
            return procesarObtenerReservasFuturas(fecha);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. Obtener la información concreta de una reserva dado su identificador (GET
     * /api/reservas/{id})
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtenerReservaPorId(@PathVariable Integer id) {
        try {
            return procesarObtenerReservaPorId(id);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4. Crear una reserva para una embarcación disponible (POST /api/reservas)
     */
    @PostMapping
    public ResponseEntity<Reserva> crearReserva(@RequestBody Reserva nuevaReserva) {
        try {
            return procesarCreacionReserva(nuevaReserva);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * D.1. Modificar la fecha de una reserva futura a otra posterior, solo si la
     * embarcación ya asignada está disponible en la nueva fecha solicitada (PATCH
     * /api/reservas/{id}/fecha)
     */
    @PatchMapping("/{id}/fecha")
    public ResponseEntity<Reserva> actualizarFechaReserva(
            @PathVariable Integer id,
            @RequestBody Map<String, String> cuerpoSolicitud) {
        try {
            return procesarActualizacionFechaReserva(id, cuerpoSolicitud);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * D.2. Modificar los datos de la reserva (descripción y número de plazas). En
     * el caso del número de plazas, debe comprobarse que no exceda a la capacidad
     * máxima de la embarcación asignada (PATCH /api/reservas/{id})
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Reserva> actualizarReserva(
            @PathVariable Integer id,
            @RequestBody Reserva reservaActualizaciones) {
        try {
            return procesarActualizacionReserva(id, reservaActualizaciones);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * D.3. Cancelar una reserva de actividad que aún no se haya realizado (DELETE
     * /api/reservas/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarReserva(@PathVariable Integer id) {
        try {
            return procesarCancelacionReserva(id);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por socio
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<Reserva>> obtenerReservasPorSocio(@PathVariable Integer idSocio) {
        try {
            List<Reserva> reservas = reservaRepository.obtenerReservasPorSocio(idSocio);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por embarcación
     */
    @GetMapping("/embarcacion/{matricula}")
    public ResponseEntity<List<Reserva>> obtenerReservasPorEmbarcacion(@PathVariable String matricula) {
        try {
            List<Reserva> reservas = reservaRepository.obtenerReservasPorEmbarcacion(matricula);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por fecha específica
     */
    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<Reserva>> obtenerReservasPorFecha(@PathVariable String fecha) {
        try {
            LocalDate fechaConsulta = LocalDate.parse(fecha);
            List<Reserva> reservas = reservaRepository.obtenerReservasPorFecha(fechaConsulta);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception excepcion) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Métodos de procesamiento ====================

    private ResponseEntity<List<Reserva>> procesarObtenerReservasFuturas(String fecha) {
        LocalDate fechaConsulta = determinarFechaConsulta(fecha);
        List<Reserva> reservasFuturas = reservaRepository.obtenerReservasFuturas(fechaConsulta);
        return new ResponseEntity<>(reservasFuturas, HttpStatus.OK);
    }

    private LocalDate determinarFechaConsulta(String fecha) {
        if (fecha != null && !fecha.isEmpty()) {
            return LocalDate.parse(fecha);
        }
        return LocalDate.now();
    }

    private ResponseEntity<Reserva> procesarObtenerReservaPorId(Integer id) {
        Reserva reservaEncontrada = reservaRepository.obtenerReservaPorId(id);

        if (reservaEncontrada != null) {
            return new ResponseEntity<>(reservaEncontrada, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    // ==================== Procesamiento de creación ====================

    private ResponseEntity<Reserva> procesarCreacionReserva(Reserva nuevaReserva) {
        ResponseEntity<Reserva> errorDatos = validarDatosObligatoriosReserva(nuevaReserva);
        if (errorDatos != null) {
            return errorDatos;
        }

        ResponseEntity<Reserva> errorDisponibilidad = validarDisponibilidadReserva(nuevaReserva);
        if (errorDisponibilidad != null) {
            return errorDisponibilidad;
        }

        ResponseEntity<Reserva> errorSocioYCapacidad = validarSocioYCapacidad(nuevaReserva);
        if (errorSocioYCapacidad != null) {
            return errorSocioYCapacidad;
        }

        ResponseEntity<Reserva> errorPatron = asignarPatronReserva(nuevaReserva);
        if (errorPatron != null) {
            return errorPatron;
        }

        return persistirReserva(nuevaReserva);
    }

    private ResponseEntity<Reserva> validarDatosObligatoriosReserva(Reserva reserva) {
        if (reserva.getMatriculaEmbarcacion() == null || reserva.getMatriculaEmbarcacion().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        if (reserva.getFechaActividad() == null) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        if (reserva.getIdSocioSolicitante() == null) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    private ResponseEntity<Reserva> validarDisponibilidadReserva(Reserva reserva) {
        boolean disponible = reservaRepository.estaDisponible(
                reserva.getMatriculaEmbarcacion(),
                reserva.getFechaActividad(),
                reserva.getFechaActividad());

        if (!disponible) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        return null;
    }

    private ResponseEntity<Reserva> validarSocioYCapacidad(Reserva reserva) {
        boolean esMayorDeEdad = reservaRepository.esSocioMayorDeEdad(reserva.getIdSocioSolicitante());
        if (!esMayorDeEdad) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(reserva.getMatriculaEmbarcacion());
        if (reserva.getPlazasSolicitadas() > capacidad) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    private ResponseEntity<Reserva> asignarPatronReserva(Reserva reserva) {
        boolean tienePatron = reservaRepository.tienePatronAsignado(
                reserva.getMatriculaEmbarcacion(), reserva.getFechaActividad());

        if (tienePatron) {
            Integer idPatron = reservaRepository.obtenerPatronAsignado(
                    reserva.getMatriculaEmbarcacion(), reserva.getFechaActividad());
            reserva.setIdPatron(idPatron);
            return null;
        }

        boolean socioEsPatron = reservaRepository.socioTieneTituloPatron(reserva.getIdSocioSolicitante());
        if (!socioEsPatron) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        reserva.setIdPatron(reserva.getIdSocioSolicitante());
        return null;
    }

    private ResponseEntity<Reserva> persistirReserva(Reserva reserva) {
        boolean insertadoConExito = reservaRepository.insertarReserva(reserva);

        if (insertadoConExito) {
            return new ResponseEntity<>(reserva, HttpStatus.CREATED);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Procesamiento de actualización ====================

    private ResponseEntity<Reserva> procesarActualizacionFechaReserva(Integer id, Map<String, String> cuerpoSolicitud) {
        Reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
        if (reservaActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        String nuevaFechaStr = cuerpoSolicitud.get("nuevaFecha");
        if (nuevaFechaStr == null || nuevaFechaStr.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        LocalDate nuevaFecha = LocalDate.parse(nuevaFechaStr);

        if (!nuevaFecha.isAfter(reservaActual.getFechaActividad())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        boolean disponible = reservaRepository.estaDisponible(
                reservaActual.getMatriculaEmbarcacion(), nuevaFecha, nuevaFecha);
        if (!disponible) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        reservaActual.setFechaActividad(nuevaFecha);
        return new ResponseEntity<>(reservaActual, HttpStatus.OK);
    }

    private ResponseEntity<Reserva> procesarActualizacionReserva(Integer id, Reserva reservaActualizaciones) {
        Reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
        if (reservaActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        aplicarCambiosReserva(reservaActual, reservaActualizaciones);
        return new ResponseEntity<>(reservaActual, HttpStatus.OK);
    }

    private void aplicarCambiosReserva(Reserva reservaActual, Reserva actualizaciones) {
        aplicarCambioPropositoActividad(reservaActual, actualizaciones);
        aplicarCambioPlazasSolicitadas(reservaActual, actualizaciones);
    }

    private void aplicarCambioPropositoActividad(Reserva reservaActual, Reserva actualizaciones) {
        if (actualizaciones.getPropositoActividad() != null
                && !actualizaciones.getPropositoActividad().isEmpty()) {
            reservaActual.setPropositoActividad(actualizaciones.getPropositoActividad());
        }
    }

    private void aplicarCambioPlazasSolicitadas(Reserva reservaActual, Reserva actualizaciones) {
        if (actualizaciones.getPlazasSolicitadas() == null || actualizaciones.getPlazasSolicitadas() <= 0) {
            return;
        }

        int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(reservaActual.getMatriculaEmbarcacion());
        if (actualizaciones.getPlazasSolicitadas() <= capacidad) {
            reservaActual.setPlazasSolicitadas(actualizaciones.getPlazasSolicitadas());
        }
    }

    // ==================== Procesamiento de cancelación ====================

    private ResponseEntity<Void> procesarCancelacionReserva(Integer id) {
        Reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
        if (reservaActual == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        boolean eliminadaConExito = reservaRepository.eliminarReserva(id);

        if (eliminadaConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}