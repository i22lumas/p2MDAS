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

    @Autowired
    private PatronRepository patronRepository;

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
            return procesarObtenerPorTipo(tipo);
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
        ResponseEntity<String> errorValidacion = validarDatosEmbarcacion(nuevaEmbarcacion);
        if (errorValidacion != null) {
            return errorValidacion;
        }

        boolean insertadoConExito = embarcacionRepository.insertarEmbarcacion(nuevaEmbarcacion);

        if (insertadoConExito) {
            return new ResponseEntity<>("Embarcación creada correctamente", HttpStatus.CREATED);
        }

        return new ResponseEntity<>("Error creando embarcación", HttpStatus.BAD_REQUEST);
    }

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

        aplicarCamposActualizacionEmbarcacion(embarcacionActual, embarcacionActualizaciones);

        boolean actualizadoConExito = embarcacionRepository.actualizarEmbarcacion(embarcacionActual);

        if (actualizadoConExito) {
            return new ResponseEntity<>(embarcacionActual, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
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

        ResponseEntity<String> errorValidacion = validarEmbarcacionYPatronExisten(matricula, idPatron);
        if (errorValidacion != null) {
            return errorValidacion;
        }

        boolean vinculadoConExito = embarcacionRepository.vincularPatron(matricula, idPatron);

        if (vinculadoConExito) {
            return new ResponseEntity<>("Patrón vinculado correctamente a la embarcación", HttpStatus.OK);
        }

        return new ResponseEntity<>("Error vinculando patrón a la embarcación", HttpStatus.BAD_REQUEST);
    }

    /**
     * 4. Desvincular un patrón de una embarcación (PATCH
     * /api/boats/{matricula}/unassign-patron) [cite: 77]
     */
    @PatchMapping("/{matricula}/unassign-patron")
    public ResponseEntity<String> desasignarPatronDeEmbarcacion(@PathVariable String matricula) {

        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        boolean desvinculadoConExito = embarcacionRepository.desvincularPatron(matricula);

        if (desvinculadoConExito) {
            return new ResponseEntity<>("Patrón desvinculado correctamente de la embarcación", HttpStatus.OK);
        }

        return new ResponseEntity<>("Error desvinculando patrón de la embarcación", HttpStatus.BAD_REQUEST);
    }

    /**
     * 5. Eliminar una embarcación si no está vinculada a ningún alquiler o reserva,
     * ya sea pasada o futura (DELETE /api/boats/{matricula}) [cite: 77]
     */
    @DeleteMapping("/{matricula}")
    public ResponseEntity<Void> eliminarEmbarcacion(@PathVariable String matricula) {

        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean eliminadoConExito = embarcacionRepository.eliminarEmbarcacion(matricula);

        if (eliminadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Error al eliminar (probablemente tiene alquileres/reservas)
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    // ==================== Métodos auxiliares ====================

    private ResponseEntity<List<Embarcacion>> procesarObtenerPorTipo(String tipo) {
        TipoEmbarcacion tipoEnum = TipoEmbarcacion.valueOf(tipo.toUpperCase());
        List<Embarcacion> embarcaciones = embarcacionRepository.buscarPorTipo(tipoEnum);
        return new ResponseEntity<>(embarcaciones, HttpStatus.OK);
    }

    private ResponseEntity<String> validarDatosEmbarcacion(Embarcacion embarcacion) {
        if (embarcacion.getNumeroPlazas() <= 0) {
            return new ResponseEntity<>("El número de plazas debe ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        if (embarcacion.getEsloraEnMetros() <= 0) {
            return new ResponseEntity<>("La eslora en metros debe ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    private void aplicarCamposActualizacionEmbarcacion(Embarcacion actual, Embarcacion actualizaciones) {
        // La matrícula no se puede actualizar
        aplicarNombreEmbarcacion(actual, actualizaciones);
        aplicarTipoEmbarcacion(actual, actualizaciones);
        aplicarNumeroPlazasEmbarcacion(actual, actualizaciones);
        aplicarEsloraEmbarcacion(actual, actualizaciones);
        // idPatronAsignado se gestiona con los endpoints assign/unassign-patron
    }

    private void aplicarNombreEmbarcacion(Embarcacion actual, Embarcacion actualizaciones) {
        if (actualizaciones.getNombre() != null && !actualizaciones.getNombre().isEmpty()) {
            actual.setNombre(actualizaciones.getNombre());
        }
    }

    private void aplicarTipoEmbarcacion(Embarcacion actual, Embarcacion actualizaciones) {
        if (actualizaciones.getTipo() != null) {
            actual.setTipo(actualizaciones.getTipo());
        }
    }

    private void aplicarNumeroPlazasEmbarcacion(Embarcacion actual, Embarcacion actualizaciones) {
        if (actualizaciones.getNumeroPlazas() > 0) {
            actual.setNumeroPlazas(actualizaciones.getNumeroPlazas());
        }
    }

    private void aplicarEsloraEmbarcacion(Embarcacion actual, Embarcacion actualizaciones) {
        if (actualizaciones.getEsloraEnMetros() > 0) {
            actual.setEsloraEnMetros(actualizaciones.getEsloraEnMetros());
        }
    }

    private ResponseEntity<String> validarEmbarcacionYPatronExisten(String matricula, Integer idPatron) {
        Embarcacion embarcacionExistente = embarcacionRepository.buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        if (!patronRepository.existePatron(idPatron)) {
            return new ResponseEntity<>("Patrón no encontrado", HttpStatus.BAD_REQUEST);
        }

        return null;
    }
}