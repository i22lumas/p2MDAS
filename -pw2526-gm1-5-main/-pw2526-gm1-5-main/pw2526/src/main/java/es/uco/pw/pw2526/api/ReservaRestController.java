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
import es.uco.pw.pw2526.model.domain.reserva.reserva;

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
    public ResponseEntity<List<reserva>> getAllReservas() {
        try {
            List<reserva> reservas = reservaRepository.obtenerTodasLasReservas();
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 2. Obtener la lista de reservas futuras dada una fecha (GET
     * /api/reservas/futuras)
     * Parámetro: fecha (opcional) - Si no se proporciona, se usa la fecha actual
     */
    @GetMapping("/futuras")
    public ResponseEntity<List<reserva>> getReservasFuturas(@RequestParam(required = false) String fecha) {
        try {
            LocalDate fechaConsulta;

            if (fecha != null && !fecha.isEmpty()) {
                fechaConsulta = LocalDate.parse(fecha);
            } else {
                fechaConsulta = LocalDate.now();
            }

            List<reserva> reservasFuturas = reservaRepository.obtenerReservasFuturas(fechaConsulta);
            return new ResponseEntity<>(reservasFuturas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. Obtener la información concreta de una reserva dado su identificador (GET
     * /api/reservas/{id})
     */
    @GetMapping("/{id}")
    public ResponseEntity<reserva> getReservaById(@PathVariable Integer id) {
        try {
            reserva reserva = reservaRepository.obtenerReservaPorId(id);

            if (reserva != null) {
                return new ResponseEntity<>(reserva, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4. Crear una reserva para una embarcación disponible (POST /api/reservas)
     */
    @PostMapping
    public ResponseEntity<reserva> createReserva(@RequestBody reserva nuevaReserva) {
        try {
            // Validaciones básicas
            if (nuevaReserva.getMatriculaEmbarcacion() == null || nuevaReserva.getMatriculaEmbarcacion().isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            if (nuevaReserva.getFechaActividad() == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            if (nuevaReserva.getIdSocioSolicitante() == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Verificar disponibilidad de la embarcación
            boolean disponible = reservaRepository.estaDisponible(
                    nuevaReserva.getMatriculaEmbarcacion(),
                    nuevaReserva.getFechaActividad(),
                    nuevaReserva.getFechaActividad());

            if (!disponible) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }

            // Verificar que el socio existe y es mayor de edad
            boolean esMayorDeEdad = reservaRepository.esSocioMayorDeEdad(nuevaReserva.getIdSocioSolicitante());
            if (!esMayorDeEdad) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Verificar capacidad de la embarcación
            int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(nuevaReserva.getMatriculaEmbarcacion());
            if (nuevaReserva.getPlazasSolicitadas() > capacidad) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Verificar si necesita patrón y asignarlo si es necesario
            boolean tienePatron = reservaRepository.tienePatronAsignado(nuevaReserva.getMatriculaEmbarcacion(),
                    nuevaReserva.getFechaActividad());
            if (!tienePatron) {
                // Si la embarcación no tiene patrón asignado, verificar si el socio tiene
                // título de patrón
                boolean socioEsPatron = reservaRepository.socioTieneTituloPatron(nuevaReserva.getIdSocioSolicitante());
                if (!socioEsPatron) {
                    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
                }
                // Si el socio es patrón, se asigna a sí mismo como patrón
                nuevaReserva.setIdPatron(nuevaReserva.getIdSocioSolicitante());
            } else {
                // Si la embarcación tiene patrón asignado, obtenerlo
                Integer idPatron = reservaRepository.obtenerPatronAsignado(nuevaReserva.getMatriculaEmbarcacion(),
                        nuevaReserva.getFechaActividad());
                nuevaReserva.setIdPatron(idPatron);
            }

            // Insertar la reserva en la base de datos
            boolean insertado = reservaRepository.insertarReserva(nuevaReserva);

            if (insertado) {
                return new ResponseEntity<>(nuevaReserva, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ----------------------------------------------------------------------
    // SEMANA 2: PATCH y DELETE (Reservas)
    // ----------------------------------------------------------------------

    /**
     * D.1. Modificar la fecha de una reserva futura a otra posterior, solo si la
     * embarcación ya asignada está disponible en la nueva fecha solicitada (PATCH
     * /api/reservas/{id}/fecha)
     */
    @PatchMapping("/{id}/fecha")
    public ResponseEntity<reserva> updateReservaFecha(
            @PathVariable Integer id,
            @RequestBody Map<String, String> requestBody) {

        try {
            // Obtener la reserva actual
            reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
            if (reservaActual == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            // Verificar que la reserva es futura
            if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Obtener la nueva fecha del cuerpo de la petición
            String nuevaFechaStr = requestBody.get("nuevaFecha");
            if (nuevaFechaStr == null || nuevaFechaStr.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            LocalDate nuevaFecha = LocalDate.parse(nuevaFechaStr);

            // Verificar que la nueva fecha es posterior a la fecha actual de la reserva
            if (!nuevaFecha.isAfter(reservaActual.getFechaActividad())) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Verificar disponibilidad de la embarcación en la nueva fecha
            boolean disponible = reservaRepository.estaDisponible(
                    reservaActual.getMatriculaEmbarcacion(),
                    nuevaFecha,
                    nuevaFecha);

            if (!disponible) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }

            // Actualizar la fecha de la reserva
            // NOTA: Necesitarás implementar un método en ReservaRepository para actualizar
            // la fecha
            // Por ejemplo: reservaRepository.actualizarFechaReserva(id, nuevaFecha);

            // Por ahora, actualizamos el objeto localmente
            reservaActual.setFechaActividad(nuevaFecha);

            // En una implementación real, aquí llamarías al repositorio para guardar el
            // cambio
            // boolean actualizado = reservaRepository.actualizarFechaReserva(id,
            // nuevaFecha);
            // if (!actualizado) {
            // return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            // }

            return new ResponseEntity<>(reservaActual, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * D.2. Modificar los datos de la reserva (descripción y número de plazas). En
     * el caso del número de plazas, debe comprobarse que no exceda a la capacidad
     * máxima de la embarcación asignada (PATCH /api/reservas/{id})
     */
    @PatchMapping("/{id}")
    public ResponseEntity<reserva> updateReserva(
            @PathVariable Integer id,
            @RequestBody reserva reservaUpdates) {

        try {
            // Obtener la reserva actual
            reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
            if (reservaActual == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            // Verificar que la reserva es futura
            if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }

            // Aplicar las actualizaciones solo a los campos permitidos
            boolean cambiosRealizados = false;

            // Actualizar descripción si se proporciona
            if (reservaUpdates.getPropositoActividad() != null
                    && !reservaUpdates.getPropositoActividad().isEmpty()) {
                reservaActual.setPropositoActividad(reservaUpdates.getPropositoActividad());
                cambiosRealizados = true;
            }

            // Actualizar número de plazas si se proporciona
            if (reservaUpdates.getPlazasSolicitadas() != null
                    && reservaUpdates.getPlazasSolicitadas() > 0) {

                // Verificar que no excede la capacidad máxima de la embarcación
                int capacidad = reservaRepository
                        .obtenerCapacidadEmbarcacion(reservaActual.getMatriculaEmbarcacion());
                if (reservaUpdates.getPlazasSolicitadas() > capacidad) {
                    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
                }

                reservaActual.setPlazasSolicitadas(reservaUpdates.getPlazasSolicitadas());
                cambiosRealizados = true;
            }

            // Si no hay cambios, devolver la reserva actual sin modificar
            if (!cambiosRealizados) {
                return new ResponseEntity<>(reservaActual, HttpStatus.OK);
            }

            // NOTA: Necesitarás implementar un método en ReservaRepository para actualizar
            // la reserva
            // Por ejemplo: reservaRepository.actualizarReserva(reservaActual);

            // En una implementación real, aquí llamarías al repositorio para guardar los
            // cambios
            // boolean actualizado = reservaRepository.actualizarReserva(reservaActual);
            // if (!actualizado) {
            // return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            // }

            return new ResponseEntity<>(reservaActual, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * D.3. Cancelar una reserva de actividad que aún no se haya realizado (DELETE
     * /api/reservas/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReserva(@PathVariable Integer id) {

        try {
            // Obtener la reserva actual
            reserva reservaActual = reservaRepository.obtenerReservaPorId(id);
            if (reservaActual == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Verificar que la reserva es futura
            if (reservaActual.getFechaActividad().isBefore(LocalDate.now())) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Eliminar la reserva (este método ya existe en tu ReservaRepository)
            boolean eliminada = reservaRepository.eliminarReserva(id);

            if (eliminada) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por socio
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<reserva>> getReservasPorSocio(@PathVariable Integer idSocio) {
        try {
            List<reserva> reservas = reservaRepository.obtenerReservasPorSocio(idSocio);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por embarcación
     */
    @GetMapping("/embarcacion/{matricula}")
    public ResponseEntity<List<reserva>> getReservasPorEmbarcacion(@PathVariable String matricula) {
        try {
            List<reserva> reservas = reservaRepository.obtenerReservasPorEmbarcacion(matricula);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint adicional: Obtener reservas por fecha específica
     */
    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<reserva>> getReservasPorFecha(@PathVariable String fecha) {
        try {
            LocalDate fechaConsulta = LocalDate.parse(fecha);
            List<reserva> reservas = reservaRepository.obtenerReservasPorFecha(fechaConsulta);
            return new ResponseEntity<>(reservas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}