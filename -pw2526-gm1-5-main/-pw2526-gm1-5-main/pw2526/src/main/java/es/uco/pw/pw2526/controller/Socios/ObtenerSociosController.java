package es.uco.pw.pw2526.controller.socios;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.Repository.SocioRepository;

@Controller
public class ObtenerSociosController {

    private final SocioRepository socioRepository;

    public ObtenerSociosController(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    @GetMapping("/verSocios")
    public ModelAndView obtenerSocios() {
        List<Socio> socios = socioRepository.obtenerTodosSocios();

        ModelAndView model = new ModelAndView("obtenerSociosView.html");
        model.addObject("socios", socios);
        return model;
    }
}