package es.uco.pw.pw2526.controller.Socios;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.Socio.Socio.TipoMiembro; 
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion.TipoInscripcion;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.Repository.InscripcionRepository;

@Controller
public class AddSocioController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository; 

    private static final double CUOTA_ADULTO_INDIVIDUAL = 300.0;

    public AddSocioController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository) {
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @GetMapping("/addSocio")
    public ModelAndView mostrarFormularioAnadirSocio() {
        ModelAndView modelAndView = new ModelAndView("addSocioView.html");
        modelAndView.addObject("newSocio", new Socio());
        return modelAndView;
    }

    @PostMapping("/addSocio")
    public ModelAndView insertarSocio(@ModelAttribute Socio nuevoSocio) {
        if (socioRepository.existeSocioPorDni(nuevoSocio.getDni())) {
            return construirVistaFallo("El DNI " + nuevoSocio.getDni() + " ya está registrado.");
        }

        if (!nuevoSocio.esMayorDeEdad()) {
            return construirVistaFallo("El socio debe ser mayor de edad para la inscripción.");
        }

        configurarSocioTitular(nuevoSocio);

        int inscripcionId = crearInscripcionInicial();
        if (inscripcionId <= 0) {
            return construirVistaFallo("Error creando la inscripción inicial en la BD.");
        }

        nuevoSocio.setInscripcionId(inscripcionId);
        nuevoSocio.setIdSocioTitularFk(null);

        int idSocioCreado = socioRepository.insertarSocioYRetornarId(nuevoSocio, inscripcionId);
        if (idSocioCreado <= 0) {
            return construirVistaFallo("Error interno al guardar los datos del socio. Inscripción creada.");
        }

        ModelAndView resultado = vincularSocioConInscripcion(idSocioCreado, inscripcionId);
        resultado.addObject("Socio", nuevoSocio);
        return resultado;
    }

    // ========== Métodos privados ==========

    private void configurarSocioTitular(Socio nuevoSocio) {
        nuevoSocio.setFechaInscripcion(LocalDate.now());
        nuevoSocio.setTipoMiembro(TipoMiembro.TITULAR);
    }

    private int crearInscripcionInicial() {
        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setTipoInscripcion(TipoInscripcion.INDIVIDUAL);
        nuevaInscripcion.setCuotaAnual(CUOTA_ADULTO_INDIVIDUAL);
        return inscripcionRepository.insertarInscripcion(nuevaInscripcion);
    }

    private ModelAndView vincularSocioConInscripcion(int idSocioCreado, int inscripcionId) {
        boolean fkActualizada = socioRepository.actualizarIdSocioTitular(idSocioCreado, idSocioCreado);
        boolean titularActualizado = inscripcionRepository.actualizarSocioTitular(inscripcionId, idSocioCreado);

        if (titularActualizado && fkActualizada) {
            return new ModelAndView("addSocioViewSuccess.html");
        }

        return construirVistaFallo(
                "Error crítico: Socio creado, pero no se pudo actualizar el ID del titular en la inscripción o su propia FK.");
    }

    private ModelAndView construirVistaFallo(String mensajeError) {
        ModelAndView modelAndView = new ModelAndView("addSocioViewFail.html");
        modelAndView.addObject("error", mensajeError);
        return modelAndView;
    }
}