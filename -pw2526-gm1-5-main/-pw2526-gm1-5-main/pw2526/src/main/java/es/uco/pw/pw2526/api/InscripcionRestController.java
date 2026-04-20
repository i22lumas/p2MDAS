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
     * 1. Obtener la lista de inscripciones individuales (GET /api/inscripciones/individual)
     * 2. Obtener la lista de inscripciones familiares (GET /api/inscripciones/familiar)
     * Implementado con @RequestParam "tipo"
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> obtenerInscripciones(@RequestParam(required = false) String tipo) {
        
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
     * 3. Obtener la información de una inscripción dado el DNI del socio titular (GET /api/inscripciones/{dniTitular})
     */
    @GetMapping("/{dniTitular}")
    public ResponseEntity<Inscripcion> obtenerInscripcionPorDniTitular(@PathVariable String dniTitular){
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        if (titular == null || !titular.esTitular()) {
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
     * 4. Crear una inscripción para un socio titular (POST /api/inscripciones)
     */
    @PostMapping
    public ResponseEntity<Inscripcion> crearInscripcion(@RequestBody Inscripcion nuevaInscripcion) {
        Socio titular = socioRepository.obtenerSocioPorId(nuevaInscripcion.getIdSocioTitular());
        
        if (titular == null || titular.getInscripcionId() != -1) { 
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY); 
        }
        
        nuevaInscripcion.setFechaCreacion(LocalDate.now());

        int idGenerado = inscripcionRepository.insertarInscripcion(nuevaInscripcion);

        if(idGenerado > 0){
            nuevaInscripcion.setId(idGenerado);
            socioRepository.actualizarInscripcionIdSocio(titular.getId(), idGenerado); 
            return new ResponseEntity<>(nuevaInscripcion, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 2. Actualizar una inscripción individual para convertirla en una familiar (PUT /api/inscripciones/{dniTitular}/tipo)
     */
    @PutMapping("/{dniTitular}/tipo")
    public ResponseEntity<Inscripcion> convertirInscripcionAFamiliar(@PathVariable String dniTitular) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        if (titular == null || !titular.esTitular()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcionActual = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (inscripcionActual == null || inscripcionActual.getTipoInscripcion() != TipoInscripcion.INDIVIDUAL) {
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY); // No existe o no es individual
        }
        
        // Lógica de negocio (actualizar cuota y tipo)
        inscripcionActual.setTipoInscripcion(TipoInscripcion.FAMILIAR);
        // La cuota debería actualizarse aquí, pero la omitimos por simplicidad del ejemplo.
        
        boolean actualizadoConExito = inscripcionRepository.actualizarInscripcion(inscripcionActual);
        
        if(actualizadoConExito){
            return new ResponseEntity<>(inscripcionActual, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 3. Vincular a un nuevo miembro en una inscripción familiar (PATCH /api/inscripciones/{dniTitular}/miembros)
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
        
        boolean vinculadoConExito = socioRepository.vincularSocioAInscripcion(
            socioMiembro.getId(), 
            inscripcion.getId(), 
            titular.getId(), 
            miembroVinculado.getTipoMiembro()
        );
        
        if (vinculadoConExito) {
            Socio socioActualizado = socioRepository.obtenerSocioPorDni(miembroVinculado.getDni());
            return new ResponseEntity<>(socioActualizado, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4. Desvincular a un miembro de una inscripción familiar (PATCH /api/inscripciones/{dniTitular}/miembros/{dniMiembro})
     */
    @PatchMapping("/{dniTitular}/miembros/{dniMiembro}")
    public ResponseEntity<Void> desvincularMiembro(@PathVariable String dniTitular, @PathVariable String dniMiembro) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        Socio socioMiembro = socioRepository.obtenerSocioPorDni(dniMiembro);
        
        if (titular == null || socioMiembro == null || !titular.esTitular()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (inscripcion == null || inscripcion.getTipoInscripcion() != TipoInscripcion.FAMILIAR) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // El miembro debe pertenecer a esta inscripción y no debe ser el titular
        if (socioMiembro.getInscripcionId() != inscripcion.getId() || socioMiembro.esTitular()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        boolean desvinculadoConExito = socioRepository.desvincularSocioDeInscripcion(socioMiembro.getId()); 
        
        if (desvinculadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 5. Cancelar una inscripción (DELETE /api/inscripciones/{dniTitular})
     */
    @DeleteMapping("/{dniTitular}")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable String dniTitular) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        
        if (titular == null || !titular.esTitular()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        
        if (inscripcion == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        boolean canceladaConExito = inscripcionRepository.cancelarInscripcion(inscripcion.getId());
        
        if (canceladaConExito) {
            // Desvincular a todos los socios de la inscripción antes de devolver 204
            socioRepository.desvincularTodosSociosDeInscripcion(inscripcion.getId());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}