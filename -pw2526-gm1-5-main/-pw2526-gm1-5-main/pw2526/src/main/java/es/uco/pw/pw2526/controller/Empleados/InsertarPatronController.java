package es.uco.pw.pw2526.controller.Empleados;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Empleados.Patron;
import es.uco.pw.pw2526.model.Repository.PatronRepository;

@Controller
public class InsertarPatronController {

    private final PatronRepository patronRepository;

    /**
     * Constructor del controlador para insertar patrones
     * 
     * @param patronRepository Repositorio de patrones
     */
    public InsertarPatronController(PatronRepository patronRepository) {
        this.patronRepository = patronRepository;
        String sqlQueriesFileName = "./src/main/resources/db/sql.properties";
        this.patronRepository.setSQLQueriesFileName(sqlQueriesFileName);
    }

    /**
     * Muestra la vista para añadir un nuevo patrón
     * 
     * @return ModelAndView con el formulario de añadir patrón
     */
    @GetMapping("/addEmpleados")
    public ModelAndView mostrarFormularioAnadirPatron() {
        ModelAndView modelAndView = new ModelAndView("addEmpleadosView.html");
        modelAndView.addObject("newEmpleados", new Patron());
        return modelAndView;
    }

    /**
     * Procesa la adición de un nuevo patrón
     * 
     * @param nuevoPatron Objeto Patron con los datos del nuevo patrón
     * @return ModelAndView con el resultado de la operación (éxito o error)
     */
    @PostMapping("/addEmpleados")
    public ModelAndView insertarPatron(@ModelAttribute Patron nuevoPatron) {
        boolean insertadoConExito = patronRepository.insertarPatron(nuevoPatron);
        ModelAndView modelAndView;

        if (insertadoConExito) {
            modelAndView = new ModelAndView("addEmpleadosViewSuccess.html");
        } else {
            modelAndView = new ModelAndView("addEmpleadosViewFail.html");
        }

        modelAndView.addObject("Empleados", nuevoPatron);
        return modelAndView;
    }
}
