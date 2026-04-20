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
        ModelAndView modelAndView;

        if (socioRepository.existeSocioPorDni(nuevoSocio.getDni())) {
            modelAndView = new ModelAndView("addSocioViewFail.html");
            modelAndView.addObject("error", "El DNI " + nuevoSocio.getDni() + " ya está registrado.");
        } else if (!nuevoSocio.esMayorDeEdad()) {
             modelAndView = new ModelAndView("addSocioViewFail.html");
             modelAndView.addObject("error", "El socio debe ser mayor de edad para la inscripción.");
        } else {
            
            nuevoSocio.setFechaInscripcion(LocalDate.now()); 
            nuevoSocio.setTipoMiembro(TipoMiembro.TITULAR); 
            
            Inscripcion nuevaInscripcion = new Inscripcion();
            nuevaInscripcion.setTipoInscripcion(TipoInscripcion.INDIVIDUAL);
            nuevaInscripcion.setCuotaAnual(CUOTA_ADULTO_INDIVIDUAL);
            
            int inscripcionId = inscripcionRepository.insertarInscripcion(nuevaInscripcion);
            
            if (inscripcionId > 0) {
                
                nuevoSocio.setInscripcionId(inscripcionId);
                nuevoSocio.setIdSocioTitularFk(null); 

                int idSocioCreado = socioRepository.insertarSocioYRetornarId(nuevoSocio, inscripcionId); 
                
                if (idSocioCreado > 0) {
                    
                    nuevoSocio.setIdSocioTitularFk(idSocioCreado); 
                    
                    boolean fkActualizada = socioRepository.actualizarIdSocioTitular(idSocioCreado, idSocioCreado);
                    
                    boolean titularActualizado = inscripcionRepository.actualizarSocioTitular(inscripcionId, idSocioCreado);
                    
                    if (titularActualizado && fkActualizada) {
                        modelAndView = new ModelAndView("addSocioViewSuccess.html");
                    } else {
                        modelAndView = new ModelAndView("addSocioViewFail.html");
                        modelAndView.addObject("error", "Error crítico: Socio creado, pero no se pudo actualizar el ID del titular en la inscripción o su propia FK.");
                    }
                } else {
                    modelAndView = new ModelAndView("addSocioViewFail.html");
                    modelAndView.addObject("error", "Error interno al guardar los datos del socio. Inscripción creada.");
                }
            } else {
                 modelAndView = new ModelAndView("addSocioViewFail.html");
                 modelAndView.addObject("error", "Error creando la inscripción inicial en la BD.");
            }
        }

        modelAndView.addObject("Socio", nuevoSocio);
        return modelAndView;
    }
}