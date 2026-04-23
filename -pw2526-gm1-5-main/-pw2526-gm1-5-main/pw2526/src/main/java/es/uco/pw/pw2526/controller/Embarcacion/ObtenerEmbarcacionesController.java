package es.uco.pw.pw2526.controller.Embarcacion;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;

@Controller
public class ObtenerEmbarcacionesController {

    private EmbarcacionRepository embarcacionRepository;


    public ObtenerEmbarcacionesController(EmbarcacionRepository embarcacionRepository) {
        this.embarcacionRepository = embarcacionRepository;
        this.embarcacionRepository.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
    }


    @GetMapping("/verEmbarcaciones")
    public ModelAndView obtenerEmbarcaciones() {
        List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        ModelAndView model = new ModelAndView("obtenerEmbarcacionesView.html");
        model.addObject("Embarcaciones", embarcaciones);
        return model;
    }
}