package es.uco.pw.pw2526.controller.Empleados;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.Empleados.Empleados;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;

@Controller
public class ObtenerEmpleadosController {

    EmpleadosRepository EmpleadosRepository;

    /**
     * Constructor del controlador para obtener empleados
     * 
     * @param EmpleadosRepository Repositorio de empleados
     */
    public ObtenerEmpleadosController(EmpleadosRepository EmpleadosRepository) {
        this.EmpleadosRepository = EmpleadosRepository;
        String sqlQueriesFileName = "./src/main/resources/db/sql.properties";
        this.EmpleadosRepository.setSQLQueriesFileName(sqlQueriesFileName);
    }

    /**
     * Obtiene y muestra la lista de todos los empleados
     * 
     * @return ModelAndView con la lista de empleados
     */
    @GetMapping("/verEmpleados")
    public ModelAndView obtenerEmpleados() {
        List<Empleados> Empleados = EmpleadosRepository.obtenerEmpleados();
        ModelAndView model = new ModelAndView("obtenerEmpleadosView.html");
        model.addObject("Empleados", Empleados);
        return model;
    }
}