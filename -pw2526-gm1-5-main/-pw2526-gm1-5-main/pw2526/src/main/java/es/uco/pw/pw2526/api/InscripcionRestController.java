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
@RequestMapping(path = "/api/inscripciones", produces = "application/json")
public class InscripcionRestController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository;

    public InscripcionRestController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository) {
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * 1. Obtener la lista de inscripciones individuales (GET
     * /api/inscripciones/individual)
     * 2. Obtener la lista de inscripciones familiares (GET
     * /api/inscripciones/familiar)
     * Implementado con @RequestParam "tipo"
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> obtenerInscripciones(@RequestParam(required = false) String tipo) {
        if (tipo != null) {
            return obtenerInscripcionesPorTipo(tipo);
        }

        List<Map<String, Object>> inscripciones = inscripcionRepository.obtenerDetallesInscripciones();
        return new ResponseEntity<>(inscripciones, HttpStatus.OK);
    }

    /**
     * 3. Obtener la información de una inscripción dado el DNI del socio titular
     * (GET /api/inscripciones/{dniTitular})
     */
    @GetMapping("/{dniTitular}")
    public ResponseEntity<Inscripcion> obtenerInscripcionPorDniTitular(@PathVariable String dniTitular) {
        Socio titular = buscarTitularPorDni(dniTitular);
        if (titular == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        if (inscripcion != null) {
            return new ResponseEntity<>(inscripcion, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
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
        return persistirInscripcion(nuevaInscripcion, titular);
    }

    /**
     * 2. Actualizar una inscripción individual para convertirla en una familiar
     * (PUT /api/inscripciones/{dniTitular}/tipo)
     */
    @PutMapping("/{dniTitular}/tipo")
    public ResponseEntity<Inscripcion> convertirInscripcionAFamiliar(@PathVariable String dniTitular) {
        Socio titular = buscarTitularPorDni(dniTitular);
        if (titular == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcionActual = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        if (inscripcionActual == null || inscripcionActual.getTipoInscripcion() != TipoInscripcion.INDIVIDUAL) {
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        inscripcionActual.setTipoInscripcion(TipoInscripcion.FAMILIAR);

        boolean actualizadoConExito = inscripcionRepository.actualizarInscripcion(inscripcionActual);

        if (actualizadoConExito) {
            return new ResponseEntity<>(inscripcionActual, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 3. Vincular a un nuevo miembro en una inscripción familiar (PATCH
     * /api/inscripciones/{dniTitular}/miembros)
     */
    @PatchMapping("/{dniTitular}/miembros")
    public ResponseEntity<Socio> vincularMiembro(@PathVariable String dniTitular, @RequestBody Socio miembroVinculado) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        Socio socioMiembro = socioRepository.obtenerSocioPorDni(miembroVinculado.getDni());

        ResponseEntity<Socio> errorSocios = validarSociosParaVinculacion(titular, socioMiembro);
        if (errorSocios != null) {
            return errorSocios;
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        ResponseEntity<Socio> errorInscripcion = validarInscripcionFamiliar(inscripcion);
        if (errorInscripcion != null) {
            return errorInscripcion;
        }

        if (socioMiembro.getInscripcionId() != -1) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        return ejecutarVinculacion(socioMiembro, inscripcion, titular, miembroVinculado);
    }

    /**
     * 4. Desvincular a un miembro de una inscripción familiar (PATCH
     * /api/inscripciones/{dniTitular}/miembros/{dniMiembro})
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

        if (socioMiembro.getInscripcionId() != inscripcion.getId() || socioMiembro.esTitular()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        boolean desvinculadoConExito = socioRepository.desvincularSocioDeInscripcion(socioMiembro.getId());

        if (desvinculadoConExito) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 5. Cancelar una inscripción (DELETE /api/inscripciones/{dniTitular})
     */
    @DeleteMapping("/{dniTitular}")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable String dniTitular) {
        Socio titular = buscarTitularPorDni(dniTitular);
        if (titular == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());
        if (inscripcion == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ejecutarCancelacionInscripcion(inscripcion);
    }

    // ==================== Métodos auxiliares ====================

    private ResponseEntity<List<Map<String, Object>>> obtenerInscripcionesPorTipo(String tipo) {
        if (tipo.equalsIgnoreCase("individual") || tipo.equalsIgnoreCase("familiar")) {
            List<Map<String, Object>> inscripciones = inscripcionRepository
                    .obtenerDetallesInscripcionesPorTipo(tipo.toUpperCase());
            return new ResponseEntity<>(inscripciones, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    private Socio buscarTitularPorDni(String dniTitular) {
        Socio titular = socioRepository.obtenerSocioPorDni(dniTitular);
        if (titular == null || !titular.esTitular()) {
            return null;
        }
        return titular;
    }

    private ResponseEntity<Inscripcion> persistirInscripcion(Inscripcion inscripcion, Socio titular) {
        int idGenerado = inscripcionRepository.insertarInscripcion(inscripcion);

        if (idGenerado <= 0) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        inscripcion.setId(idGenerado);
        socioRepository.actualizarInscripcionIdSocio(titular.getId(), idGenerado);
        return new ResponseEntity<>(inscripcion, HttpStatus.CREATED);
    }

    private ResponseEntity<Socio> validarSociosParaVinculacion(Socio titular, Socio miembro) {
        if (titular == null || miembro == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return null;
    }

    private ResponseEntity<Socio> validarInscripcionFamiliar(Inscripcion inscripcion) {
        if (inscripcion == null || inscripcion.getTipoInscripcion() != TipoInscripcion.FAMILIAR) {
            return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return null;
    }

    private ResponseEntity<Socio> ejecutarVinculacion(Socio socioMiembro, Inscripcion inscripcion,
            Socio titular, Socio miembroVinculado) {
        boolean vinculadoConExito = socioRepository.vincularSocioAInscripcion(
                socioMiembro.getId(),
                inscripcion.getId(),
                titular.getId(),
                miembroVinculado.getTipoMiembro());

        if (vinculadoConExito) {
            Socio socioActualizado = socioRepository.obtenerSocioPorDni(miembroVinculado.getDni());
            return new ResponseEntity<>(socioActualizado, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Void> ejecutarCancelacionInscripcion(Inscripcion inscripcion) {
        boolean canceladaConExito = inscripcionRepository.cancelarInscripcion(inscripcion.getId());

        if (canceladaConExito) {
            // Desvincular a todos los socios de la inscripción antes de devolver 204
            socioRepository.desvincularTodosSociosDeInscripcion(inscripcion.getId());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}