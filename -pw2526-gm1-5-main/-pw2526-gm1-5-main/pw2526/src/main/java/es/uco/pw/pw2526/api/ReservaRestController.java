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
            LocalDate fechaConsulta;

            if (fecha != null && !fecha.isEmpty()) {
                fechaConsulta = LocalDate.parse(fecha);
            } else {
                fechaConsulta = LocalDate.now();
            }

            List<Reserva> reservasFuturas = reservaRepository.obtenerReservasFuturas(fechaConsulta);
            return new ResponseEntity<>(reservasFuturas, HttpStatus.OK);
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
            Reserva reservaEncontrada = reservaRepository.obtenerReservaPorId(id);

            if (reservaEncontrada != null) {
                return new ResponseEntity<>(reservaEncontrada, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
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

            if (nuevaReserva.getMatriculaEmbarcacion() == null || nuevaReserva.getMatriculaEmbarcacion().isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            if (nuevaReserva.getFechaActividad() == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            if (nuevaReserva.getIdSocioSolicitante() == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }


            boolean disponible = reservaRepository.estaDisponible(
                    nuevaReserva.getMatriculaEmbarcacion(),
                    nuevaReserva.getFechaActividad(),
                    nuevaReserva.getFechaActividad());

            if (!disponible) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }


            boolean esMayorDeEdad = reservaRepository.esSocioMayorDeEdad(nuevaReserva.getIdSocioSolicitante());
            if (!esMayorDeEdad) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }


            int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(nuevaReserva.getMatriculaEmbarcacion());
            if (nuevaReserva.getPlazasSolicitadas() > capacidad) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }


            boolean tienePatron = reservaRepository.tienePatronAsignado(nuevaReserva.getMatriculaEmbarcacion(),
                    nuevaReserva.getFechaActividad());
            if (!tienePatron) {

                boolean socioEsPatron = reservaRepository.socioTieneTituloPatron(nuevaReserva.getIdSocioSolicitante());
                if (!socioEsPatron) {
                    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
                }

                nuevaReserva.setIdPatron(nuevaReserva.getIdSocioSolicitante());
            } else {

                Integer idPatron = reservaRepository.obtenerPatronAsignado(nuevaReserva.getMatriculaEmbarcacion(),
                        nuevaReserva.getFechaActividad());
                nuevaReserva.setIdPatron(idPatron);
            }


            boolean insertadoConExito = reservaRepository.insertarReserva(nuevaReserva);

            if (insertadoConExito) {
                return new ResponseEntity<>(nuevaReserva, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }

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
                    reservaActual.getMatriculaEmbarcacion(),
                    nuevaFecha,
                    nuevaFecha);

            if (!disponible) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }


            reservaActual.setFechaActividad(nuevaFecha);

            return new ResponseEntity<>(reservaActual, HttpStatus.OK);

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

            Reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
            if (reservaActual == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }


            if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }


            boolean cambiosRealizados = false;


            if (reservaActualizaciones.getPropositoActividad() != null
                    && !reservaActualizaciones.getPropositoActividad().isEmpty()) {
                reservaActual.setPropositoActividad(reservaActualizaciones.getPropositoActividad());
                cambiosRealizados = true;
            }


            if (reservaActualizaciones.getPlazasSolicitadas() != null
                    && reservaActualizaciones.getPlazasSolicitadas() > 0) {


                int capacidad = reservaRepository
                        .obtenerCapacidadEmbarcacion(reservaActual.getMatriculaEmbarcacion());
                if (reservaActualizaciones.getPlazasSolicitadas() > capacidad) {
                    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
                }

                reservaActual.setPlazasSolicitadas(reservaActualizaciones.getPlazasSolicitadas());
                cambiosRealizados = true;
            }


            if (!cambiosRealizados) {
                return new ResponseEntity<>(reservaActual, HttpStatus.OK);
            }

            return new ResponseEntity<>(reservaActual, HttpStatus.OK);

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
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

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
}