package es.uco.pw.pw2526.api;

import es.uco.pw.pw2526.model.domain.Empleados.Patron;
import es.uco.pw.pw2526.model.Repository.PatronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patrones")
public class PatronRestController {

    @Autowired
    private PatronRepository patronRepository;

    // ----------------------------------------------------------------------
    // SEMANA 1: GET y POST (Patrones)
    // ----------------------------------------------------------------------

    /**
     * 3. Obtener la lista completa de patrones (GET /api/patrones) [cite: 55]
     */
    @GetMapping
    public ResponseEntity<List<Patron>> obtenerTodosPatrones() {
        List<Patron> patrones = patronRepository.obtenerPatrones();
        return new ResponseEntity<>(patrones, HttpStatus.OK);
    }

    /**
     * 5. Crear un nuevo patrón, sin asociarle embarcación (POST /api/patrones)
     * [cite: 55]
     */
    @PostMapping
    public ResponseEntity<String> crearPatron(@RequestBody Patron nuevoPatron) {
        boolean creadoConExito = patronRepository.insertarPatron(nuevoPatron);

        if (creadoConExito) {
            return new ResponseEntity<>("Patrón creado correctamente", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Error creando patrón", HttpStatus.BAD_REQUEST);
        }
    }

    // ----------------------------------------------------------------------
    // SEMANA 2: PATCH y DELETE (Patrones)
    // ----------------------------------------------------------------------

    /**
     * 2. Actualizar los campos de información de un patrón, excepto el DNI (PATCH
     * /api/patrones/{id}) [cite: 76]
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Patron> actualizarPatron(
            @PathVariable Integer id,
            @RequestBody Patron patronActualizaciones) {

        Patron patronActual = patronRepository.obtenerPatronPorId(id);
        if (patronActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // El DNI no se puede actualizar - verificar que no se haya modificado
        if (patronActualizaciones.getDni() != null && !patronActualizaciones.getDni().equals(patronActual.getDni())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // Aplicar solo los campos que se pueden modificar
        if (patronActualizaciones.getNombre() != null && !patronActualizaciones.getNombre().isEmpty()) {
            patronActual.setNombre(patronActualizaciones.getNombre());
        }

        if (patronActualizaciones.getApellidos() != null && !patronActualizaciones.getApellidos().isEmpty()) {
            patronActual.setApellidos(patronActualizaciones.getApellidos());
        }

        if (patronActualizaciones.getFechaNacimiento() != null) {
            patronActual.setFechaNacimiento(patronActualizaciones.getFechaNacimiento());
        }

        if (patronActualizaciones.getFechaExpedicionTitulo() != null) {
            patronActual.setFechaExpedicionTitulo(patronActualizaciones.getFechaExpedicionTitulo());
        }

        boolean actualizadoConExito = patronRepository.actualizarPatron(patronActual);

        if (actualizadoConExito) {
            return new ResponseEntity<>(patronActual, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 6. Eliminar a un patrón si no está vinculado con ninguna embarcación (DELETE
     * /api/patrones/{id}) [cite: 77]
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPatron(@PathVariable Integer id) {

        Patron patron = patronRepository.obtenerPatronPorId(id);

        if (patron == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean eliminadoConExito = patronRepository.eliminarPatron(id);

        if (eliminadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // Error al eliminar (probablemente está asignado a una embarcación)
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    // ----------------------------------------------------------------------
    // ENDPOINTS ADICIONALES
    // ----------------------------------------------------------------------

    /**
     * GET /api/patrones/{id}
     * Devuelve un patrón por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patron> obtenerPatronPorId(@PathVariable Integer id) {
        Patron patron = patronRepository.obtenerPatronPorId(id);
        if (patron != null) {
            return new ResponseEntity<>(patron, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /api/patrones/disponibles
     * Devuelve los patrones disponibles (no asignados a embarcaciones)
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Patron>> obtenerPatronesDisponibles() {
        List<Patron> patronesDisponibles = patronRepository.obtenerPatronesDisponibles();
        return new ResponseEntity<>(patronesDisponibles, HttpStatus.OK);
    }
}