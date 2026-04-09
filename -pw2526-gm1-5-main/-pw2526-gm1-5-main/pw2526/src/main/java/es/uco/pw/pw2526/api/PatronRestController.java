package es.uco.pw.pw2526.api;

import es.uco.pw.pw2526.model.domain.Empleados.Empleados;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patrones")
public class PatronRestController {

    @Autowired
    private EmpleadosRepository empleadosRepository;

    // ----------------------------------------------------------------------
    // SEMANA 1: GET y POST (Patrones)
    // ----------------------------------------------------------------------

    /**
     * 3. Obtener la lista completa de patrones (GET /api/patrones) [cite: 55]
     */
    @GetMapping
    public ResponseEntity<List<Empleados>> getAllPatrones() {
        List<Empleados> empleados = empleadosRepository.obtenerEmpleados();
        return new ResponseEntity<>(empleados, HttpStatus.OK);
    }

    /**
     * 5. Crear un nuevo patrón, sin asociarle embarcación (POST /api/patrones)
     * [cite: 55]
     */
    @PostMapping
    public ResponseEntity<String> createPatron(@RequestBody Empleados nuevoEmpleado) {
        boolean ok = empleadosRepository.addEmpleados(nuevoEmpleado);

        if (ok) {
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
    public ResponseEntity<Empleados> updatePatron(
            @PathVariable Integer id,
            @RequestBody Empleados empleadoUpdates) {

        Empleados currentEmpleado = empleadosRepository.obtenerEmpleadoPorId(id);
        if (currentEmpleado == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // El DNI no se puede actualizar - verificar que no se haya modificado
        if (empleadoUpdates.getDni() != null && !empleadoUpdates.getDni().equals(currentEmpleado.getDni())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // Aplicar solo los campos que se pueden modificar
        if (empleadoUpdates.getNombre() != null && !empleadoUpdates.getNombre().isEmpty()) {
            currentEmpleado.setNombre(empleadoUpdates.getNombre());
        }

        if (empleadoUpdates.getApellidos() != null && !empleadoUpdates.getApellidos().isEmpty()) {
            currentEmpleado.setApellidos(empleadoUpdates.getApellidos());
        }

        if (empleadoUpdates.getFech_nacimiento() != null) {
            currentEmpleado.setFech_nacimiento(empleadoUpdates.getFech_nacimiento());
        }

        if (empleadoUpdates.getFech_expedicion_titulo() != null) {
            currentEmpleado.setFech_expedicion_titulo(empleadoUpdates.getFech_expedicion_titulo());
        }

        boolean resultOk = empleadosRepository.actualizarEmpleado(currentEmpleado);

        if (resultOk) {
            return new ResponseEntity<>(currentEmpleado, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 6. Eliminar a un patrón si no está vinculado con ninguna embarcación (DELETE
     * /api/patrones/{id}) [cite: 77]
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatron(@PathVariable Integer id) {

        Empleados empleado = empleadosRepository.obtenerEmpleadoPorId(id);

        if (empleado == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean ok = empleadosRepository.eliminarEmpleado(id);

        if (ok) {
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
    public ResponseEntity<Empleados> getPatronById(@PathVariable Integer id) {
        Empleados empleado = empleadosRepository.obtenerEmpleadoPorId(id);
        if (empleado != null) {
            return new ResponseEntity<>(empleado, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /api/patrones/disponibles
     * Devuelve los patrones disponibles (no asignados a embarcaciones)
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Empleados>> getPatronesDisponibles() {
        List<Empleados> empleadosDisponibles = empleadosRepository.obtenerEmpleadosDisponibles();
        return new ResponseEntity<>(empleadosDisponibles, HttpStatus.OK);
    }
}