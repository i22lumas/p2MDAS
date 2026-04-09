package es.uco.pw.pw2526.controller.socios;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.Socio.Socio;

@Controller
public class RegistrarTituloPatronController {

    private final SocioRepository socioRepository;

    public RegistrarTituloPatronController(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    @GetMapping("/registrarTituloPatron")
    public ModelAndView getSociosPendientesDeTitulo() {
        // Se llama al método actualizado que devuelve Socios con TipoMiembro TITULAR o CONYUGE y sin título.
        List<Socio> miembrosPendientes = socioRepository.obtenerMiembrosSinTituloPatron();
        
        ModelAndView model = new ModelAndView("registrarTituloPatronView.html");
        model.addObject("socios", miembrosPendientes);
        return model;
    }

    @PostMapping("/registrarTituloPatron/otorgar")
    public ModelAndView otorgarTitulo(@RequestParam String dni) {
        Socio miembro = socioRepository.obtenerSocioPorDni(dni);
        ModelAndView modelAndView;
        boolean success = false;

        if (miembro == null) {
            modelAndView = new ModelAndView("registrarTituloPatronViewFail.html");
            modelAndView.addObject("error", "Error: DNI no encontrado en la base de datos de socios.");
            return modelAndView;
        }

        String tipoMiembro = miembro.getTipoMiembro().toString();

        if (tipoMiembro.equals("TITULAR") || tipoMiembro.equals("CONYUGE")) {
            
            success = socioRepository.actualizarTituloPatron(dni, true);
            
            if (success) {
                miembro.setTieneTituloPatron(true);
                modelAndView = new ModelAndView("registrarTituloPatronViewSuccess.html");
                modelAndView.addObject("mensaje", "Título de patrón otorgado a " + miembro.getNombre() + " " + miembro.getApellidos() + ".");
                modelAndView.addObject("socio", miembro);
            } else {
                modelAndView = new ModelAndView("registrarTituloPatronViewFail.html");
                modelAndView.addObject("error", "Error de BD al actualizar el título de patrón para el DNI " + dni + ".");
            }
            
        } else {
            modelAndView = new ModelAndView("registrarTituloPatronViewFail.html");
            modelAndView.addObject("error", "Error: Solo los socios titulares y cónyuges son elegibles para obtener el título de patrón.");
        }

        return modelAndView;
    }
}