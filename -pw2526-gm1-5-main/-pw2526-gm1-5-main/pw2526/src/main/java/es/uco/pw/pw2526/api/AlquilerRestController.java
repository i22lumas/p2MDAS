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

    // ==================== GET ====================
    // C.1: Obtener la lista completa de alquileres
    @GetMapping
    public ResponseEntity<List<Alquiler>> obtenerTodosAlquileres() {
        try {
            List<Alquiler> alquileres = alquilerRepository.obtenerTodosAlquileres();
            return ResponseEntity.ok(alquileres);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // C.2: Obtener la lista de alquileres futuros dada una fecha
    @GetMapping("/futuros")
    public ResponseEntity<?> obtenerAlquileresFuturos(@RequestParam(required = false) String fecha) {
        try {
            List<Alquiler> alquileres = alquilerRepository.obtenerAlquileresFuturos();
            
            if (fecha != null && !fecha.isEmpty()) {
                try {
                    LocalDate fechaFiltro = LocalDate.parse(fecha);
                    alquileres = alquileres.stream()
                            .filter(alquiler -> !alquiler.getFechaInicio().isBefore(fechaFiltro))
                            .toList();
                } catch (Exception excepcion) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD"));
                }
            }
            return ResponseEntity.ok(alquileres);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener alquileres futuros"));
        }
    }

    // C.3: Obtener la información concreta de un alquiler dado su identificador
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAlquilerPorId(@PathVariable int id) {
        try {
            List<Alquiler> todos = alquilerRepository.obtenerTodosAlquileres();
            Alquiler alquilerEncontrado = todos.stream()
                    .filter(alquiler -> alquiler.getIdAlquiler() == id)
                    .findFirst()
                    .orElse(null);
                    
            if (alquilerEncontrado == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
            }
            
            return ResponseEntity.ok(alquilerEncontrado);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el alquiler"));
        }
    }

    // C.4: Obtener las embarcaciones disponibles dada una fecha de inicio y de fin
    @GetMapping("/disponibles")
    public ResponseEntity<?> obtenerEmbarcacionesDisponibles(
            @RequestParam String fechaInicio, 
            @RequestParam String fechaFin) {
        try {
            // Validar formato de fechas
            LocalDate inicio;
            LocalDate fin;
            try {
                inicio = LocalDate.parse(fechaInicio);
                fin = LocalDate.parse(fechaFin);
            } catch (Exception excepcion) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD"));
            }
            
            // Validar que fecha inicio no sea pasada
            if (inicio.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La fecha de inicio no puede ser en el pasado"));
            }
            
            // Validar que fecha fin no sea anterior a inicio
            if (fin.isBefore(inicio)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La fecha de fin no puede ser anterior a la fecha de inicio"));
            }
            
            // Validar duración según temporada (según guion)
            String errorDuracion = validarDuracionSegunTemporada(inicio, fin);
            if (errorDuracion != null) {
                return ResponseEntity.badRequest().body(Map.of("error", errorDuracion));
            }
            
            List<Embarcacion> todas = embarcacionRepository.obtenerEmbarcaciones();
            List<Embarcacion> disponibles = new ArrayList<>();
            
            for (Embarcacion embarcacion : todas) {
                boolean libreAlquiler = alquilerRepository.estaDisponible(embarcacion.getMatricula(), inicio, fin);
                boolean libreReserva = reservaRepository.estaDisponible(embarcacion.getMatricula(), inicio, fin);
                if (libreAlquiler && libreReserva) {
                    disponibles.add(embarcacion);
                }
            }
            
            return ResponseEntity.ok(disponibles);
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener embarcaciones disponibles"));
        }
    }

    // ==================== POST ====================
    // C.5: Crear un alquiler para una embarcación disponible, sin incluir la vinculación 
    //      de los socios que participan en ella
    @PostMapping
    public ResponseEntity<?> crearAlquiler(@RequestBody Alquiler nuevoAlquiler) {
        try {
            // Validaciones básicas
            if (nuevoAlquiler.getDniSocioTitular() == null || nuevoAlquiler.getDniSocioTitular().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El DNI del socio titular es obligatorio"));
            }
            
            if (nuevoAlquiler.getMatriculaEmbarcacion() == null || nuevoAlquiler.getMatriculaEmbarcacion().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La matrícula de la embarcación es obligatoria"));
            }
            
            if (nuevoAlquiler.getFechaInicio() == null || nuevoAlquiler.getFechaFin() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Las fechas de inicio y fin son obligatorias"));
            }
            
            // Validar socio
            Socio titular = socioRepository.obtenerSocioPorDni(nuevoAlquiler.getDniSocioTitular());
            if (titular == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Socio titular con DNI " + nuevoAlquiler.getDniSocioTitular() + " no encontrado"));
            }
            
            // Validar que el socio es patrón (según guion: solo patrones pueden alquilar)
            if (!titular.esPatron()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El socio titular no tiene título de patrón de embarcación"));
            }
            
            nuevoAlquiler.setIdSocioTitular(titular.getId());
            
            // Validar embarcación
            Embarcacion embarcacionSeleccionada = embarcacionRepository.obtenerEmbarcaciones().stream()
                    .filter(embarcacion -> embarcacion.getMatricula().equals(nuevoAlquiler.getMatriculaEmbarcacion()))
                    .findFirst()
                    .orElse(null);
                    
            if (embarcacionSeleccionada == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Embarcación con matrícula " + nuevoAlquiler.getMatriculaEmbarcacion() + " no encontrada"));
            }
            
            // Validar fechas
            if (nuevoAlquiler.getFechaInicio().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La fecha de inicio no puede ser en el pasado"));
            }
            
            if (nuevoAlquiler.getFechaFin().isBefore(nuevoAlquiler.getFechaInicio())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La fecha de fin no puede ser anterior a la fecha de inicio"));
            }
            
            // Validar duración según temporada (SEGÚN GUION)
            String errorDuracion = validarDuracionSegunTemporada(nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin());
            if (errorDuracion != null) {
                return ResponseEntity.badRequest().body(Map.of("error", errorDuracion));
            }
            
            // Validar capacidad
            if (nuevoAlquiler.getPlazasSolicitadas() <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El número de plazas solicitadas debe ser mayor a 0"));
            }
            
            if (nuevoAlquiler.getPlazasSolicitadas() > embarcacionSeleccionada.getNumeroPlazas()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", 
                                "Plazas solicitadas (" + nuevoAlquiler.getPlazasSolicitadas() + 
                                ") exceden la capacidad de la embarcación (" + embarcacionSeleccionada.getNumeroPlazas() + ")"));
            }
            
            // Validar disponibilidad
            boolean disponibleAlquiler = alquilerRepository.estaDisponible(
                    nuevoAlquiler.getMatriculaEmbarcacion(), 
                    nuevoAlquiler.getFechaInicio(), 
                    nuevoAlquiler.getFechaFin());
                    
            boolean disponibleReserva = reservaRepository.estaDisponible(
                    nuevoAlquiler.getMatriculaEmbarcacion(), 
                    nuevoAlquiler.getFechaInicio(), 
                    nuevoAlquiler.getFechaFin());
                    
            if (!disponibleAlquiler || !disponibleReserva) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", 
                                "La embarcación no está disponible en las fechas seleccionadas"));
            }
            
            // Calcular precio (20€ por persona por día según guion)
            long dias = ChronoUnit.DAYS.between(nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin()) + 1;
            double precio = 20.0 * nuevoAlquiler.getPlazasSolicitadas() * dias;
            nuevoAlquiler.setPrecioTotal(precio);
            
            // Crear alquiler (sin tripulantes - según especificación C.5)
            int idAlquiler = alquilerRepository.insertarAlquilerYRetornarId(nuevoAlquiler);
            if (idAlquiler > 0) {
                nuevoAlquiler.setIdAlquiler(idAlquiler);
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Alquiler creado exitosamente");
                respuesta.put("idAlquiler", idAlquiler);
                respuesta.put("alquiler", nuevoAlquiler);
                return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al crear el alquiler en la base de datos"));
            }
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + excepcion.getMessage()));
        }
    }

    // ==================== PATCH ====================
    // C.1: Vincular a un nuevo socio (no titular) a un alquiler futuro, 
    //      actualizando el coste y número de plazas
    @PatchMapping("/{id}/agregar-socio")
    public ResponseEntity<?> agregarSocio(@PathVariable int id, @RequestParam String dniSocio) {
        try {
            if (dniSocio == null || dniSocio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El parámetro 'dniSocio' es obligatorio"));
            }
            
            // Verificar que el alquiler existe y es futuro
            Alquiler alquilerExistente = encontrarAlquilerPorId(id);
            if (alquilerExistente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
            }
            
            if (alquilerExistente.getFechaInicio().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se puede modificar un alquiler pasado"));
            }
            
            // Verificar que el socio existe
            Socio socio = socioRepository.obtenerSocioPorDni(dniSocio);
            if (socio == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Socio con DNI " + dniSocio + " no encontrado"));
            }
            
            // Verificar que no es el titular
            if (dniSocio.equals(alquilerExistente.getDniSocioTitular())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El socio titular ya está incluido en el alquiler"));
            }
            
            // Verificar que no está ya en la lista de tripulantes
            if (alquilerExistente.getDnisTripulantes() != null && alquilerExistente.getDnisTripulantes().contains(dniSocio)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El socio ya está en la lista de tripulantes"));
            }
            
            // Verificar capacidad
            int totalTripulantes = 1 + (alquilerExistente.getDnisTripulantes() != null ? alquilerExistente.getDnisTripulantes().size() : 0);
            if (totalTripulantes >= alquilerExistente.getPlazasSolicitadas()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No hay plazas disponibles en este alquiler"));
            }
            
            // Agregar socio
            boolean agregadoConExito = alquilerRepository.agregarSocioNoTitular(id, dniSocio);
            if (agregadoConExito) {
                // Actualizar precio (una plaza más)
                long dias = ChronoUnit.DAYS.between(alquilerExistente.getFechaInicio(), alquilerExistente.getFechaFin()) + 1;
                double nuevoPrecio = 20.0 * (totalTripulantes + 1) * dias;
                
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Socio agregado correctamente al alquiler");
                respuesta.put("idAlquiler", id);
                respuesta.put("dniSocio", dniSocio);
                respuesta.put("nuevoPrecio", nuevoPrecio);
                respuesta.put("plazasOcupadas", totalTripulantes + 1);
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al agregar el socio al alquiler"));
            }
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + excepcion.getMessage()));
        }
    }

    // C.2: Desvincular a un socio (no titular) de un alquiler futuro, 
    //      actualizando el coste y el número de plazas
    @PatchMapping("/{id}/quitar-socio")
    public ResponseEntity<?> quitarSocio(@PathVariable int id, @RequestParam String dniSocio) {
        try {
            if (dniSocio == null || dniSocio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El parámetro 'dniSocio' es obligatorio"));
            }
            
            // Verificar que el alquiler existe y es futuro
            Alquiler alquilerExistente = encontrarAlquilerPorId(id);
            if (alquilerExistente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
            }
            
            if (alquilerExistente.getFechaInicio().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se puede modificar un alquiler pasado"));
            }
            
            // Verificar que no es el titular
            if (dniSocio.equals(alquilerExistente.getDniSocioTitular())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se puede quitar al socio titular del alquiler"));
            }
            
            // Verificar que está en la lista de tripulantes
            if (alquilerExistente.getDnisTripulantes() == null || !alquilerExistente.getDnisTripulantes().contains(dniSocio)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El socio no está en la lista de tripulantes de este alquiler"));
            }
            
            // Quitar socio
            boolean quitadoConExito = alquilerRepository.quitarSocioNoTitular(id, dniSocio);
            if (quitadoConExito) {
                // Actualizar precio (una plaza menos)
                long dias = ChronoUnit.DAYS.between(alquilerExistente.getFechaInicio(), alquilerExistente.getFechaFin()) + 1;
                int totalTripulantes = 1 + (alquilerExistente.getDnisTripulantes() != null ? alquilerExistente.getDnisTripulantes().size() : 0);
                double nuevoPrecio = 20.0 * (totalTripulantes - 1) * dias;
                
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Socio eliminado correctamente del alquiler");
                respuesta.put("idAlquiler", id);
                respuesta.put("dniSocio", dniSocio);
                respuesta.put("nuevoPrecio", nuevoPrecio);
                respuesta.put("plazasOcupadas", totalTripulantes - 1);
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al eliminar el socio del alquiler"));
            }
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + excepcion.getMessage()));
        }
    }

    // ==================== DELETE ====================
    // C.3: Cancelar un alquiler que aún no se haya realizado
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarAlquiler(@PathVariable int id) {
        try {
            // Verificar que el alquiler existe
            Alquiler alquilerExistente = encontrarAlquilerPorId(id);
            if (alquilerExistente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Alquiler con ID " + id + " no encontrado"));
            }
            
            // Verificar que es futuro (según guion: "que aún no se haya realizado")
            if (alquilerExistente.getFechaInicio().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se puede cancelar un alquiler pasado"));
            }
            
            boolean canceladoConExito = alquilerRepository.cancelarAlquiler(id);
            if (canceladoConExito) {
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Alquiler cancelado correctamente");
                respuesta.put("idAlquiler", id);
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al cancelar el alquiler"));
            }
        } catch (Exception excepcion) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + excepcion.getMessage()));
        }
    }

    // ==================== MÉTODOS PRIVADOS AUXILIARES ====================
    
    private Alquiler encontrarAlquilerPorId(int id) {
        List<Alquiler> todos = alquilerRepository.obtenerTodosAlquileres();
        return todos.stream()
                .filter(alquiler -> alquiler.getIdAlquiler() == id)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Valida la duración del alquiler según la temporada (SEGÚN GUION):
     * - Temporada baja (octubre a abril): máximo 3 días
     * - Temporada alta (mayo a septiembre): solo 7 o 14 días
     */
    private String validarDuracionSegunTemporada(LocalDate inicio, LocalDate fin) {
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        int mes = inicio.getMonthValue();
        
        if (mes >= 10 || mes <= 4) { // Temporada baja: octubre a abril
            if (dias > 3) {
                return "En temporada baja (octubre-abril) el máximo es 3 días. Seleccionaste: " + dias + " días.";
            }
        } else if (mes >= 5 && mes <= 9) { // Temporada alta: mayo a septiembre
            if (dias != 7 && dias != 14) {
                return "En temporada alta (mayo-septiembre) solo se permiten 7 días (1 semana) o 14 días (2 semanas). Seleccionaste: " + dias + " días.";
            }
        }
        return null;
    }
    
    /**
     * Determina si una fecha está en temporada alta
     */
    private boolean esTemporadaAlta(LocalDate fecha) {
        int mes = fecha.getMonthValue();
        return mes >= 5 && mes <= 9;
    }
    
    /**
     * Determina si una fecha está en temporada baja
     */
    private boolean esTemporadaBaja(LocalDate fecha) {
        int mes = fecha.getMonthValue();
        return mes >= 10 || mes <= 4;
    }
}