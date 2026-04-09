package es.uco.pw.pw2526.controller.Embarcacion;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;

@Controller
public class AddEmbarcacionController {

    private ModelAndView modelAndView = new ModelAndView();
    private EmbarcacionRepository embarcacionRepository;

    /**
     * Constructor del controlador para añadir embarcaciones
     * 
     * @param embarcacionRepository Repositorio de embarcaciones
     */
    public AddEmbarcacionController(EmbarcacionRepository embarcacionRepository) {
        this.embarcacionRepository = embarcacionRepository;
        this.embarcacionRepository.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
    }

    /**
     * Muestra la vista para añadir una nueva embarcación
     * 
     * @return ModelAndView con el formulario de añadir embarcación
     */
    @GetMapping("/addEmbarcacion")
    public ModelAndView getAddEmbarcacionView() {
        this.modelAndView.setViewName("addEmbarcacionView.html");
        this.modelAndView.addObject("newEmbarcacion", new embarcacion());
        return this.modelAndView;
    }

    /**
     * Procesa la adición de una nueva embarcación
     * 
     * @param newEmbarcacion Objeto embarcacion con los datos de la nueva
     *                       embarcación
     * @return ModelAndView con el resultado de la operación (éxito o error)
     */
    @PostMapping("/addEmbarcacion")
    public ModelAndView addEmbarcacion(@ModelAttribute embarcacion newEmbarcacion) {
        boolean success = embarcacionRepository.addEmbarcacion(newEmbarcacion);
        String nextPage = success ? "addEmbarcacionViewSuccess.html" : "addEmbarcacionViewFail.html";
        this.modelAndView.setViewName(nextPage);
        this.modelAndView.addObject("Embarcacion", newEmbarcacion);
        return this.modelAndView;
    }
}