package es.uco.pw.pw2526.controller.Empleados;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Empleados.Patron;
import es.uco.pw.pw2526.model.Repository.PatronRepository;

@Controller
public class ObtenerPatronesController {

    PatronRepository patronRepository;

    /**
     * Constructor del controlador para obtener patrones
     * 
     * @param patronRepository Repositorio de patrones
     */
    public ObtenerPatronesController(PatronRepository patronRepository) {
        this.patronRepository = patronRepository;
        String sqlQueriesFileName = "./src/main/resources/db/sql.properties";
        this.patronRepository.setSQLQueriesFileName(sqlQueriesFileName);
    }

    /**
     * Obtiene y muestra la lista de todos los patrones
     * 
     * @return ModelAndView con la lista de patrones
     */
    @GetMapping("/verEmpleados")
    public ModelAndView obtenerPatrones() {
        List<Patron> patrones = patronRepository.obtenerPatrones();
        ModelAndView model = new ModelAndView("obtenerEmpleadosView.html");
        model.addObject("Empleados", patrones);
        return model;
    }
}
