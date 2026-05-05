package es.uco.pw.pw2526.controller.Embarcacion;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;

@Controller
public class AddEmbarcacionController {

    private ModelAndView modelAndView = new ModelAndView();
    private EmbarcacionRepository embarcacionRepository;

    public AddEmbarcacionController(EmbarcacionRepository embarcacionRepository) {
        this.embarcacionRepository = embarcacionRepository;
        this.embarcacionRepository.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
    }

    @GetMapping("/addEmbarcacion")
    public ModelAndView mostrarFormularioAnadirEmbarcacion() {
        this.modelAndView.setViewName("addEmbarcacionView.html");
        this.modelAndView.addObject("nuevaEmbarcacion", new Embarcacion());
        return this.modelAndView;
    }

    @PostMapping("/addEmbarcacion")
    public ModelAndView insertarEmbarcacion(@ModelAttribute Embarcacion nuevaEmbarcacion) {
        boolean insertadoConExito = embarcacionRepository.insertarEmbarcacion(nuevaEmbarcacion);
        String paginaSiguiente = insertadoConExito ? "addEmbarcacionViewSuccess.html" : "addEmbarcacionViewFail.html";
        this.modelAndView.setViewName(paginaSiguiente);
        this.modelAndView.addObject("Embarcacion", nuevaEmbarcacion);
        return this.modelAndView;
    }
}