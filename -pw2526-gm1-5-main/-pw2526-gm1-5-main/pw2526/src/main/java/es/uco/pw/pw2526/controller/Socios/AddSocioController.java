package es.uco.pw.pw2526.controller.socios;

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
    public ModelAndView getAddSocioView() {
        ModelAndView modelAndView = new ModelAndView("addSocioView.html");
        modelAndView.addObject("newSocio", new Socio());
        return modelAndView;
    }

    @PostMapping("/addSocio")
    public ModelAndView addSocio(@ModelAttribute Socio newSocio) {
        ModelAndView modelAndView;

        if (socioRepository.existeSocioPorDni(newSocio.getDni())) {
            modelAndView = new ModelAndView("addSocioViewFail.html");
            modelAndView.addObject("error", "El DNI " + newSocio.getDni() + " ya está registrado.");
        } else if (!newSocio.esMayorDeEdad()) {
             modelAndView = new ModelAndView("addSocioViewFail.html");
             modelAndView.addObject("error", "El socio debe ser mayor de edad para la inscripción.");
        } else {
            
            newSocio.setFechaInscripcion(LocalDate.now()); 
            newSocio.setTipoMiembro(TipoMiembro.TITULAR); 
            newSocio.setTieneTituloPatron(false);
            
            Inscripcion nuevaInscripcion = new Inscripcion();
            nuevaInscripcion.setTipoInscripcion(TipoInscripcion.INDIVIDUAL);
            nuevaInscripcion.setCuotaAnual(CUOTA_ADULTO_INDIVIDUAL);
            
            int inscripcionId = inscripcionRepository.addInscripcion(nuevaInscripcion);
            
            if (inscripcionId > 0) {
                
                newSocio.setInscripcionId(inscripcionId);
                newSocio.setIdSocioTitularFk(null); 

                int idSocioCreado = socioRepository.addSocioAndReturnId(newSocio, inscripcionId); 
                
                if (idSocioCreado > 0) {
                    
                    newSocio.setIdSocioTitularFk(idSocioCreado); 
                    
                    boolean actualizacionFk = socioRepository.actualizarIdSocioTitular(idSocioCreado, idSocioCreado);
                    
                    boolean actualizacionTitular = inscripcionRepository.actualizarSocioTitular(inscripcionId, idSocioCreado);
                    
                    if (actualizacionTitular && actualizacionFk) {
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

        modelAndView.addObject("Socio", newSocio);
        return modelAndView;
    }
}