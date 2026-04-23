package es.uco.pw.pw2526.api;

import java.util.List;
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

import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.Repository.InscripcionRepository;
import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.Socio.Socio.TipoMiembro;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion.TipoInscripcion;
import java.time.LocalDate;

@RestController()
@RequestMapping(path="/api/socios", produces="application/json")
public class SocioRestController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository;
    

    public SocioRestController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository){
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }



    /**
     * 1. Obtener la lista completa de socios, sean titulares o no. (GET /api/socios) [cite: 39]
     */
    @GetMapping
    public ResponseEntity<List<Socio>> obtenerTodosSocios(){
        List<Socio> socios = socioRepository.obtenerTodosSocios();
        return new ResponseEntity<>(socios, HttpStatus.OK);
    }

    /**
     * 2. Obtener la información de un socio, sea titular o no, dado su DNI (GET /api/socios/{dni})
     */
    @GetMapping("/{dni}")
    public ResponseEntity<Socio> obtenerSocioPorDni(@PathVariable String dni){
        Socio socio = socioRepository.obtenerSocioPorDni(dni);            
        if(socio != null){
            return new ResponseEntity<>(socio, HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * 3. Crear un nuevo socio sin asociación a ninguna inscripción previa (POST /api/socios)
     * Se asume que este socio se crea con TipoMiembro.TITULAR pero sin inscripcionId.
     */
    @PostMapping
    public ResponseEntity<Socio> crearSocioSinInscripcion(@RequestBody Socio nuevoSocio) {
        if (socioRepository.existeSocioPorDni(nuevoSocio.getDni())) {

             return new ResponseEntity<>(null, HttpStatus.CONFLICT); 
        }


        nuevoSocio.setTipoMiembro(TipoMiembro.TITULAR);
        nuevoSocio.setInscripcionId(-1); // ID inválido
        nuevoSocio.setIdSocioTitularFk(null);
        nuevoSocio.setFechaInscripcion(LocalDate.now());


        int idGenerado = socioRepository.insertarSocioYRetornarId(nuevoSocio, -1); 
        
        if (idGenerado > 0) {
            nuevoSocio.setId(idGenerado);
            return new ResponseEntity<>(nuevoSocio, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4. Crear un nuevo socio asociándolo a una inscripción familiar ya existente (POST /api/socios?inscripcionId={id})
     * El cuerpo de la petición (Socio) debe indicar si es CONYUGE o HIJO.
     */
    @PostMapping(params = "inscripcionId")
    public ResponseEntity<Socio> crearSocioYVincularAInscripcionFamiliar(@RequestBody Socio nuevoSocio, 
                                                                       @RequestParam int inscripcionId) {
        if (socioRepository.existeSocioPorDni(nuevoSocio.getDni())) {
             return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
        
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(inscripcionId);
        

        if (inscripcion == null || inscripcion.getTipoInscripcion() != TipoInscripcion.FAMILIAR) { 
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY); 
        }

        Socio socioTitular = socioRepository.obtenerSocioPorId(inscripcion.getIdSocioTitular());
        

        nuevoSocio.setInscripcionId(inscripcionId);
        nuevoSocio.setIdSocioTitularFk(socioTitular.getId());
        nuevoSocio.setFechaInscripcion(LocalDate.now());


        if (nuevoSocio.getTipoMiembro() == TipoMiembro.TITULAR) {
             return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        int idGenerado = socioRepository.insertarSocioYRetornarId(nuevoSocio, inscripcionId);
        
        if (idGenerado > 0) {
            nuevoSocio.setId(idGenerado);
            return new ResponseEntity<>(nuevoSocio, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    


    /**
     * 1. Actualizar los campos de información de un socio, excepto el DNI (PATCH /api/socios/{dni}) [cite: 79]
     */
    @PatchMapping("/{dni}")
    public ResponseEntity<Socio> actualizarDatosSocio(@PathVariable String dni, @RequestBody Socio socioActualizaciones) {
        Socio socioActual = socioRepository.obtenerSocioPorDni(dni);
        if (socioActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        

        if (socioActualizaciones.getNombre() != null) socioActual.setNombre(socioActualizaciones.getNombre());
        if (socioActualizaciones.getApellidos() != null) socioActual.setApellidos(socioActualizaciones.getApellidos());
        if (socioActualizaciones.getDireccion() != null) socioActual.setDireccion(socioActualizaciones.getDireccion());
        if (socioActualizaciones.getFechaNacimiento() != null) socioActual.setFechaNacimiento(socioActualizaciones.getFechaNacimiento());
        

        socioActual.setTieneTituloPatron(socioActualizaciones.getTieneTituloPatron());
        

        boolean actualizadoConExito = socioRepository.actualizarDatosPersonalesSocio(socioActual);
        
        if(actualizadoConExito){
            return new ResponseEntity<>(socioActual, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 6. Eliminar a un socio si no está vinculado a ninguna inscripción (DELETE /api/socios/{dni}) [cite: 84]
     */
    @DeleteMapping("/{dni}")
    public ResponseEntity<Void> eliminarSocio(@PathVariable String dni) {
        Socio socio = socioRepository.obtenerSocioPorDni(dni);
        
        if (socio == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        

        if (socio.getInscripcionId() == -1 && socio.esTitular()) {
            boolean eliminadoConExito = socioRepository.eliminarSocio(socio.getId());
            if (eliminadoConExito) {
                 return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                 return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {

             return new ResponseEntity<>(HttpStatus.CONFLICT); 
        }
    }
}