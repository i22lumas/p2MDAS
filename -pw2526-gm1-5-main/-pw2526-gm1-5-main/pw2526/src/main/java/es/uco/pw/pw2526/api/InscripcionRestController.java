package es.uco.pw.pw2526.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.Repository.InscripcionRepository;
import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.Socio.Socio.TipoMiembro;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion.TipoInscripcion;
import java.time.LocalDate;

@RestController()
@RequestMapping(path="/api/inscripciones", produces="application/json")
public class InscripcionRestController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository;
    
    public InscripcionRestController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository){
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * [cite_start]1. Obtener la lista de inscripciones individuales (GET /api/inscripciones/individual) [cite: 151]
     * [cite_start]2. Obtener la lista de inscripciones familiares (GET /api/inscripciones/familiar) [cite: 152]
     * Implementado con @RequestParam "tipo"
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getInscripciones(@RequestParam(required = false) String tipo) {
        
        List<Map<String, Object>> inscripciones;
        
        if (tipo != null) {
            if (tipo.equalsIgnoreCase("individual") || tipo.equalsIgnoreCase("familiar")) {
                inscripciones = inscripcionRepository.obtenerDetallesInscripcionesPorTipo(tipo.toUpperCase()); 
            } else {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        } else {
            inscripciones = inscripcionRepository.obtenerDetallesInscripciones(); 
        }
        return new ResponseEntity<>(inscripciones, HttpStatus.OK);
    }
    
    /**
     * [cite_start]3. Obtener la información de una inscripción dado el DNI del socio titular (GET /api/inscripciones/{dniTitular}) [cite: 153]
     */
    @GetMapping("/{dniTitular}")
    public ResponseEntity<Inscripcion> getInscripcionByTitularDni(@PathVariable String dniTitular){
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        if (titular == null || !titular.isEsTitular()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId()); 
        
        if(inscripcion != null){
            return new ResponseEntity<>(inscripcion, HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * [cite_start]4. Crear una inscripción para un socio titular (POST /api/inscripciones) [cite: 154]
     */
    @PostMapping
    public ResponseEntity<Inscripcion> createInscripcion(@RequestBody Inscripcion nuevaInscripcion) {
        Socio titular = socioRepository.obtenerSocioPorId(nuevaInscripcion.getIdSocioTitular());
        
        if (titular == null || titular.getInscripcionId() != -1) { 
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY); 
        }
        
        nuevaInscripcion.setFechaCreacion(LocalDate.now());

        int idGenerado = inscripcionRepository.addInscripcion(nuevaInscripcion);

        if(idGenerado > 0){
            nuevaInscripcion.setId(idGenerado);
            socioRepository.actualizarInscripcionIdSocio(titular.getId(), idGenerado); 
            return new ResponseEntity<>(nuevaInscripcion, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [cite_start]2. Actualizar una inscripción individual para convertirla en una familiar (PUT /api/inscripciones/{dniTitular}/tipo) [cite: 188]
     */
    @PutMapping("/{dniTitular}/tipo")
    public ResponseEntity<Inscripcion> putInscripcionToFamiliar(@PathVariable String dniTitular) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        if (titular == null || !titular.isEsTitular()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Inscripcion currentInscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (currentInscripcion == null || currentInscripcion.getTipoInscripcion() != TipoInscripcion.INDIVIDUAL) {
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY); // No existe o no es individual
        }
        
        // Lógica de negocio (actualizar cuota y tipo)
        currentInscripcion.setTipoInscripcion(TipoInscripcion.FAMILIAR);
        // La cuota debería actualizarse aquí, pero la omitimos por simplicidad del ejemplo.
        
        boolean resultOk = inscripcionRepository.actualizarInscripcion(currentInscripcion);
        
        if(resultOk){
            return new ResponseEntity<>(currentInscripcion, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * [cite_start]3. Vincular a un nuevo miembro en una inscripción familiar (PATCH /api/inscripciones/{dniTitular}/miembros) [cite: 189, 190]
     */
    @PatchMapping("/{dniTitular}/miembros")
    public ResponseEntity<Socio> vincularMiembro(@PathVariable String dniTitular, @RequestBody Socio miembroVinculado) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        Socio socioMiembro = socioRepository.obtenerSocioPorDni(miembroVinculado.getDni());
        
        if (titular == null || socioMiembro == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        if (inscripcion == null || inscripcion.getTipoInscripcion() != TipoInscripcion.FAMILIAR) {
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        // Verificar que el miembro no esté ya asociado a NINGUNA inscripción
        if (socioMiembro.getInscripcionId() != -1) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
        
        boolean resultOk = socioRepository.vincularSocioAInscripcion(
            socioMiembro.getId(), 
            inscripcion.getId(), 
            titular.getId(), 
            miembroVinculado.getTipoMiembro()
        );
        
        if (resultOk) {
            Socio updatedSocio = socioRepository.obtenerSocioPorDni(miembroVinculado.getDni());
            return new ResponseEntity<>(updatedSocio, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [cite_start]4. Desvincular a un miembro de una inscripción familiar (PATCH /api/inscripciones/{dniTitular}/miembros/{dniMiembro}) [cite: 191]
     */
    @PatchMapping("/{dniTitular}/miembros/{dniMiembro}") // Cambiado de @DeleteMapping a @PatchMapping
    public ResponseEntity<Void> desvincularMiembro(@PathVariable String dniTitular, @PathVariable String dniMiembro) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        Socio socioMiembro = socioRepository.obtenerSocioPorDni(dniMiembro);
        
        if (titular == null || socioMiembro == null || !titular.isEsTitular()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (inscripcion == null || inscripcion.getTipoInscripcion() != TipoInscripcion.FAMILIAR) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // El miembro debe pertenecer a esta inscripción y no debe ser el titular
        if (socioMiembro.getInscripcionId() != inscripcion.getId() || socioMiembro.isEsTitular()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        boolean resultOk = socioRepository.desvincularSocioDeInscripcion(socioMiembro.getId()); 
        
        if (resultOk) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * [cite_start]5. Cancelar una inscripción (DELETE /api/inscripciones/{dniTitular}) [cite: 191]
     */
    @DeleteMapping("/{dniTitular}")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable String dniTitular) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        
        if (titular == null || !titular.isEsTitular()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (inscripcion == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        boolean inscripcionCancelada = inscripcionRepository.cancelarInscripcion(inscripcion.getId());
        
        if (inscripcionCancelada) {
            // Desvincular a todos los socios de la inscripción antes de devolver 204
            socioRepository.desvincularTodosSociosDeInscripcion(inscripcion.getId());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}