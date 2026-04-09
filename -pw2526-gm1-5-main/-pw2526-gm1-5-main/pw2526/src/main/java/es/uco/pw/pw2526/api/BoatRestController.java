package es.uco.pw.pw2526.api;

import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;
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
    private EmpleadosRepository empleadosRepository;

    // ----------------------------------------------------------------------
    // SEMANA 1: GET y POST (Embarcaciones)
    // ----------------------------------------------------------------------

    /**
     * 1. Obtener la lista completa de embarcaciones (GET /api/boats) [cite: 55]
     */
    @GetMapping
    public ResponseEntity<List<embarcacion>> getAllBoats() {
        List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
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
    public ResponseEntity<List<embarcacion>> getBoatsByType(@PathVariable String tipo) {
        try {
            es.uco.pw.pw2526.model.domain.embarcacion.TiposBarcos tipoEnum = es.uco.pw.pw2526.model.domain.embarcacion.TiposBarcos
                    .valueOf(tipo.toUpperCase());
            List<embarcacion> embarcaciones = embarcacionRepository.buscarPorTipo(tipoEnum);
            return new ResponseEntity<>(embarcaciones, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 4. Crear una nueva embarcación, sin asociarle patrón (POST /api/boats) [cite:
     * 55]
     */
    @PostMapping
    public ResponseEntity<String> createBoat(@RequestBody embarcacion nuevaEmbarcacion) {
        // ARREGLADO: Validación de plaza positiva
        if (nuevaEmbarcacion.getPlaza() <= 0) {
            return new ResponseEntity<>("La plaza debe ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        // ARREGLADO: Validación de dimensiones positiva
        if (nuevaEmbarcacion.getDimensiones() <= 0) {
            return new ResponseEntity<>("Las dimensiones deben ser un número positivo", HttpStatus.BAD_REQUEST);
        }

        boolean ok = embarcacionRepository.addEmbarcacion(nuevaEmbarcacion);

        if (ok) {
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
    public ResponseEntity<embarcacion> updateBoat(
            @PathVariable String matricula,
            @RequestBody embarcacion embarcacionUpdates) {

        embarcacion currentEmbarcacion = embarcacionRepository.buscarPorMatricula(matricula);
        if (currentEmbarcacion == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // La matrícula no se puede actualizar
        // Aplicar solo los campos que se pueden modificar
        if (embarcacionUpdates.getNombre() != null && !embarcacionUpdates.getNombre().isEmpty()) {
            currentEmbarcacion.setNombre(embarcacionUpdates.getNombre());
        }

        if (embarcacionUpdates.getTipo() != null) {
            currentEmbarcacion.setTipo(embarcacionUpdates.getTipo());
        }

        if (embarcacionUpdates.getPlaza() > 0) {
            currentEmbarcacion.setPlaza(embarcacionUpdates.getPlaza());
        }

        if (embarcacionUpdates.getDimensiones() > 0) {
            currentEmbarcacion.setDimensiones(embarcacionUpdates.getDimensiones());
        }

        // idPatronAsignado no se actualiza aquí (hay endpoints específicos para eso)

        boolean resultOk = embarcacionRepository.actualizarEmbarcacion(currentEmbarcacion);

        if (resultOk) {
            return new ResponseEntity<>(currentEmbarcacion, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. Vincular un patrón a una embarcación (PATCH
     * /api/boats/{matricula}/assign-patron) [cite: 77]
     */
    @PatchMapping("/{matricula}/assign-patron")
    public ResponseEntity<String> assignPatronToBoat(
            @PathVariable String matricula,
            @RequestBody Map<String, Integer> requestBody) {

        Integer idPatron = requestBody.get("idPatron");

        if (idPatron == null) {
            return new ResponseEntity<>("El campo 'idPatron' es requerido", HttpStatus.BAD_REQUEST);
        }

        // Verificar que la embarcación existe
        embarcacion emb = embarcacionRepository.buscarPorMatricula(matricula);
        if (emb == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        // ARREGLADO: Verificar que el patrón existe
        if (!empleadosRepository.existeEmpleado(idPatron)) {
            return new ResponseEntity<>("Patrón no encontrado", HttpStatus.BAD_REQUEST);
        }

        boolean ok = embarcacionRepository.vincularPatron(matricula, idPatron);

        if (ok) {
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
    public ResponseEntity<String> unassignPatronFromBoat(@PathVariable String matricula) {

        // Verificar que la embarcación existe
        embarcacion emb = embarcacionRepository.buscarPorMatricula(matricula);
        if (emb == null) {
            return new ResponseEntity<>("Embarcación no encontrada", HttpStatus.NOT_FOUND);
        }

        boolean ok = embarcacionRepository.desvincularPatron(matricula);

        if (ok) {
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
    public ResponseEntity<Void> deleteBoat(@PathVariable String matricula) {

        // Verificar que la embarcación existe
        embarcacion emb = embarcacionRepository.buscarPorMatricula(matricula);
        if (emb == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean ok = embarcacionRepository.eliminarEmbarcacion(matricula);

        if (ok) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // Error al eliminar (probablemente tiene alquileres/reservas)
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}