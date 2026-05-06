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
@RequestMapping(path = "/api/socios", produces = "application/json")
public class SocioRestController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository;

    public SocioRestController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository) {
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * 1. Obtener la lista completa de socios, sean titulares o no. (GET
     * /api/socios) [cite: 39]
     */
    @GetMapping
    public ResponseEntity<List<Socio>> obtenerTodosSocios() {
        List<Socio> socios = socioRepository.obtenerTodosSocios();
        return new ResponseEntity<>(socios, HttpStatus.OK);
    }

    /**
     * 2. Obtener la información de un socio, sea titular o no, dado su DNI (GET
     * /api/socios/{dni})
     */
    @GetMapping("/{dni}")
    public ResponseEntity<Socio> obtenerSocioPorDni(@PathVariable String dni) {
        Socio socio = socioRepository.obtenerSocioPorDni(dni);
        if (socio != null) {
            return new ResponseEntity<>(socio, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    /**
     * 3. Crear un nuevo socio sin asociación a ninguna inscripción previa (POST
     * /api/socios)
     * Se asume que este socio se crea con TipoMiembro.TITULAR pero sin
     * inscripcionId.
     */
    @PostMapping
    public ResponseEntity<Socio> crearSocioSinInscripcion(@RequestBody Socio nuevoSocio) {
        if (socioRepository.existeSocioPorDni(nuevoSocio.getDni())) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        inicializarSocioSinInscripcion(nuevoSocio);
        return persistirSocio(nuevoSocio, -1);
    }

    /**
     * 4. Crear un nuevo socio asociándolo a una inscripción familiar ya existente
     * (POST /api/socios?inscripcionId={id})
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

        if (nuevoSocio.getTipoMiembro() == TipoMiembro.TITULAR) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        vincularSocioAInscripcionFamiliar(nuevoSocio, inscripcion);
        return persistirSocio(nuevoSocio, inscripcionId);
    }

    /**
     * 1. Actualizar los campos de información de un socio, excepto el DNI (PATCH
     * /api/socios/{dni}) [cite: 79]
     */
    @PatchMapping("/{dni}")
    public ResponseEntity<Socio> actualizarDatosSocio(@PathVariable String dni,
            @RequestBody Socio socioActualizaciones) {
        Socio socioActual = socioRepository.obtenerSocioPorDni(dni);
        if (socioActual == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        aplicarCamposActualizacionSocio(socioActual, socioActualizaciones);

        boolean actualizadoConExito = socioRepository.actualizarDatosPersonalesSocio(socioActual);

        if (actualizadoConExito) {
            return new ResponseEntity<>(socioActual, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 6. Eliminar a un socio si no está vinculado a ninguna inscripción (DELETE
     * /api/socios/{dni}) [cite: 84]
     */
    @DeleteMapping("/{dni}")
    public ResponseEntity<Void> eliminarSocio(@PathVariable String dni) {
        Socio socio = socioRepository.obtenerSocioPorDni(dni);

        if (socio == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!esElegibleParaEliminacion(socio)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        boolean eliminadoConExito = socioRepository.eliminarSocio(socio.getId());

        if (eliminadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Métodos auxiliares ====================

    private void inicializarSocioSinInscripcion(Socio socio) {
        socio.setTipoMiembro(TipoMiembro.TITULAR);
        socio.setInscripcionId(-1); // ID inválido
        socio.setIdSocioTitularFk(null);
        socio.setFechaInscripcion(LocalDate.now());
    }

    private void vincularSocioAInscripcionFamiliar(Socio socio, Inscripcion inscripcion) {
        Socio socioTitular = socioRepository.obtenerSocioPorId(inscripcion.getIdSocioTitular());
        socio.setInscripcionId(inscripcion.getId());
        socio.setIdSocioTitularFk(socioTitular.getId());
        socio.setFechaInscripcion(LocalDate.now());
    }

    private ResponseEntity<Socio> persistirSocio(Socio socio, int inscripcionId) {
        int idGenerado = socioRepository.insertarSocioYRetornarId(socio, inscripcionId);

        if (idGenerado > 0) {
            socio.setId(idGenerado);
            return new ResponseEntity<>(socio, HttpStatus.CREATED);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void aplicarCamposActualizacionSocio(Socio actual, Socio actualizaciones) {
        aplicarNombreSocio(actual, actualizaciones);
        aplicarApellidosSocio(actual, actualizaciones);
        aplicarDireccionSocio(actual, actualizaciones);
        aplicarFechaNacimientoSocio(actual, actualizaciones);
        actual.setTieneTituloPatron(actualizaciones.getTieneTituloPatron());
    }

    private void aplicarNombreSocio(Socio actual, Socio actualizaciones) {
        if (actualizaciones.getNombre() != null) {
            actual.setNombre(actualizaciones.getNombre());
        }
    }

    private void aplicarApellidosSocio(Socio actual, Socio actualizaciones) {
        if (actualizaciones.getApellidos() != null) {
            actual.setApellidos(actualizaciones.getApellidos());
        }
    }

    private void aplicarDireccionSocio(Socio actual, Socio actualizaciones) {
        if (actualizaciones.getDireccion() != null) {
            actual.setDireccion(actualizaciones.getDireccion());
        }
    }

    private void aplicarFechaNacimientoSocio(Socio actual, Socio actualizaciones) {
        if (actualizaciones.getFechaNacimiento() != null) {
            actual.setFechaNacimiento(actualizaciones.getFechaNacimiento());
        }
    }

    private boolean esElegibleParaEliminacion(Socio socio) {
        return socio.getInscripcionId() == -1 && socio.esTitular();
    }
}