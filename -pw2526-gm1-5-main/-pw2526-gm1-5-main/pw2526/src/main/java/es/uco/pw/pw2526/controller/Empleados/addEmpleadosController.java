package es.uco.pw.pw2526.controller.Empleados;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Empleados.Empleados;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;

@Controller
public class addEmpleadosController {

    private final EmpleadosRepository empleadosRepository;

    /**
     * Constructor del controlador para añadir empleados
     * 
     * @param empleadosRepository Repositorio de empleados
     */
    public addEmpleadosController(EmpleadosRepository empleadosRepository) {
        this.empleadosRepository = empleadosRepository;
        String sqlQueriesFileName = "./src/main/resources/db/sql.properties";
        this.empleadosRepository.setSQLQueriesFileName(sqlQueriesFileName);
    }

    /**
     * Muestra la vista para añadir un nuevo empleado
     * 
     * @return ModelAndView con el formulario de añadir empleado
     */
    @GetMapping("/addEmpleados")
    public ModelAndView getAddEmpleadosView() {
        ModelAndView modelAndView = new ModelAndView("addEmpleadosView.html");
        modelAndView.addObject("newEmpleados", new Empleados());
        return modelAndView;
    }

    /**
     * Procesa la adición de un nuevo empleado
     * 
     * @param newEmpleados Objeto Empleados con los datos del nuevo empleado
     * @return ModelAndView con el resultado de la operación (éxito o error)
     */
    @PostMapping("/addEmpleados")
    public ModelAndView addEmpleados(@ModelAttribute Empleados newEmpleados) {
        boolean success = empleadosRepository.addEmpleados(newEmpleados);
        ModelAndView modelAndView;

        if (success) {
            modelAndView = new ModelAndView("addEmpleadosViewSuccess.html");
        } else {
            modelAndView = new ModelAndView("addEmpleadosViewFail.html");
        }

        modelAndView.addObject("Empleados", newEmpleados);
        return modelAndView;
    }
}