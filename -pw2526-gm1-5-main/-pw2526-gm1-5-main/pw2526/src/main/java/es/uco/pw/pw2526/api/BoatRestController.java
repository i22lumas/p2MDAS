package es.uco.pw.pw2526.api;

import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.embarcacion.TipoEmbarcacion;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.PatronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boats")
public class BoatRestController {

    @Autowired
    private EmbarcacionRepository embarcacionRepository;

    @Autowired // AÑADIDO: Necesario para validar si el patrón existe
    private PatronRepository patronRepository;

    // ----------------------------------------------------------------------
    // SEMANA 1: GET y POST (Embarcaciones)
    // ----------------------------------------------------------------------

    /**
     * 1. Obtener la lista completa de embarcaciones (GET /api/boats) [cite: 55]
     */
    @GetMapping
    public ResponseEntity<List<Embarcacion>> obtenerTodasEmbarcaciones() {
        List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        if (embarcaciones == null) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(embarcaciones, HttpStatus.OK);
    }

    /**
     * 2. Obtener la lista de embarcaciones según el tipo de embarcación (GET
     * /api/boats/type/{tipo}) [cite: 55]
     */
    @GetMapping("/type/{tipo}")
    public ResponseEntity<List<Embarcacion>> obtenerEmbarcacionesPorTipo(@PathVariable String tipo) {
        try {
            TipoEmbarcacion tipoEnum = TipoEmbarcacion.valueOf(tipo.toUpperCase());
            List<Embarcacion> embarcaciones = embarcacionRepository.buscarPorTipo(tipoEnum);
            return new ResponseEntity<>(embarcaciones, HttpStatus.OK);
        } catch (IllegalArgumentException excepcion) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 4. Crear una nueva embarcación, sin asociarle patrón (POST /api/boats) [cite:
     * 55]
     */
    @PostMapping
    public ResponseEntity<String> crearEmbarcacion(@RequestBody Embarcacion nuevaEmbarcacion) {
        // ARREGLADO: Validación de plaza positiva
        if (nuevaEmbarcacion.getNumeroPlazas() <= 0) {
            return new ResponseEntity<>("El número de plazas debe ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        // ARREGLADO: Validación de dimensiones positiva
        if (nuevaEmbarcacion.getEsloraEnMetros() <= 0) {
            return new ResponseEntity<>("La eslora en metros debe ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        boolean insertadoConExito = embarcacionRepository.insertarEmbarcacion(nuevaEmbarcacion);

        if (insertadoConExito) {
            return new ResponseEntity<>("Embarcación creada correctamente", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Error creando embarcación", HttpStatus.BAD_REQUEST);
        }
    }

    // ----------------------------------------------------------------------
    // SEMANA 2: PATCH y DELETE (Embarcaciones)
    // ----------------------------------------------------------------------

    /**
     * 1. Actualizar los campos de información de una embarcación, excepto la
     * matrícula (PATCH /api/boats/{matricula}) [cite: 76]
     */
    @PatchMapping("/{matricula}")
    public ResponseEntity<Embarcacion> actualizarEmbarcacion(
            @PathVariable String matricula,
            @RequestBody Embarcacion embarcacionActualizaciones) {

        Embarcacion embarcacionActual = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // La matrícula no se puede actualizar
        // Aplicar solo los campos que se pueden modificar
        if (embarcacionActualizaciones.getNombre() != null && !embarcacionActualizaciones.getNombre().isEmpty()) {
            embarcacionActual.setNombre(embarcacionActualizaciones.getNombre());
        }

        if (embarcacionActualizaciones.getTipo() != null) {
            embarcacionActual.setTipo(embarcacionActualizaciones.getTipo());
        }

        if (embarcacionActualizaciones.getNumeroPlazas() > 0) {
            embarcacionActual.setNumeroPlazas(embarcacionActualizaciones.getNumeroPlazas());
        }

        if (embarcacionActualizaciones.getEsloraEnMetros() > 0) {
            embarcacionActual.setEsloraEnMetros(embarcacionActualizaciones.getEsloraEnMetros());
        }

        // idPatronAsignado no se actualiza aquí (hay endpoints específicos para eso)

        boolean actualizadoConExito = embarcacionRepository.actualizarEmbarcacion(embarcacionActual);

        if (actualizadoConExito) {
            return new ResponseEntity<>(embarcacionActual, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. Vincular un patrón a una embarcación (PATCH
     * /api/boats/{matricula}/assign-patron) [cite: 77]
     */
    @PatchMapping("/{matricula}/assign-patron")
    public ResponseEntity<String> asignarPatronAEmbarcacion(
            @PathVariable String matricula,
            @RequestBody Map<String, Integer> cuerpoSolicitud) {

        Integer idPatron = cuerpoSolicitud.get("idPatron");

        if (idPatron == null) {
            return new ResponseEntity<>("El campo 'idPatron' es requerido", HttpStatus.BAD_REQUEST);
        }

        // Verificar que la embarcación existe
        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        // ARREGLADO: Verificar que el patrón existe
        if (!patronRepository.existePatron(idPatron)) {
            return new ResponseEntity<>("Patrón no encontrado", HttpStatus.BAD_REQUEST);
        }

        boolean vinculadoConExito = embarcacionRepository.vincularPatron(matricula, idPatron);

        if (vinculadoConExito) {
            return new ResponseEntity<>("Patrón vinculado correctamente a la embarcación", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Error vinculando patrón a la embarcación", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 4. Desvincular un patrón de una embarcación (PATCH
     * /api/boats/{matricula}/unassign-patron) [cite: 77]
     */
    @PatchMapping("/{matricula}/unassign-patron")
    public ResponseEntity<String> desasignarPatronDeEmbarcacion(@PathVariable String matricula) {

        // Verificar que la embarcación existe
        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        boolean desvinculadoConExito = embarcacionRepository.desvincularPatron(matricula);

        if (desvinculadoConExito) {
            return new ResponseEntity<>("Patrón desvinculado correctamente de la embarcación", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Error desvinculando patrón de la embarcación", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 5. Eliminar una embarcación si no está vinculada a ningún alquiler o reserva,
     * ya sea pasada o futura (DELETE /api/boats/{matricula}) [cite: 77]
     */
    @DeleteMapping("/{matricula}")
    public ResponseEntity<Void> eliminarEmbarcacion(@PathVariable String matricula) {

        // Verificar que la embarcación existe
        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean eliminadoConExito = embarcacionRepository.eliminarEmbarcacion(matricula);

        if (eliminadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // Error al eliminar (probablemente tiene alquileres/reservas)
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}