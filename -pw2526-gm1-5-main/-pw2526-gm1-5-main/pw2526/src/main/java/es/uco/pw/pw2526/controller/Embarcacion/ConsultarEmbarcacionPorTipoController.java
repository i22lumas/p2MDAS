package es.uco.pw.pw2526.controller.Embarcacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.PatronRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.TipoEmbarcacion;
import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.Empleados.Patron;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/consultarEmbarcacionPorTipo")
public class ConsultarEmbarcacionPorTipoController {

    @Autowired
    private EmbarcacionRepository embarcacionRepository;

    @Autowired
    private PatronRepository patronRepository;

    /**
     * Muestra el formulario para consultar embarcaciones por tipo
     * 
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista del formulario
     */
    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("tiposEmbarcacion", TipoEmbarcacion.values());
        return "ConsultarEmbarcacionPorTipoView";
    }

    /**
     * Consulta las embarcaciones por tipo y muestra los resultados con información
     * de patrones asignados
     * 
     * @param tipo  Tipo de embarcación a consultar
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista de resultados
     */
    @PostMapping
    public String consultarPorTipo(@RequestParam TipoEmbarcacion tipo, Model model) {
        List<Embarcacion> embarcaciones = embarcacionRepository.buscarPorTipo(tipo);

        Map<String, String> patronesAsignados = new HashMap<>();
        Map<String, List<String>> detallesPatron = new HashMap<>();
        Map<String, List<String>> fechasAsignacion = new HashMap<>();
        Map<String, List<String>> estadosAsignacion = new HashMap<>();

        for (Embarcacion embarcacion : embarcaciones) {
            cargarInformacionPatron(embarcacion, patronesAsignados, detallesPatron,
                    fechasAsignacion, estadosAsignacion);
        }

        agregarAtributosAlModelo(model, embarcaciones, tipo, patronesAsignados,
                detallesPatron, fechasAsignacion, estadosAsignacion);

        return "ConsultarEmbarcacionPorTipoView";
    }

    // ========== Métodos privados ==========

    private void cargarInformacionPatron(Embarcacion embarcacion,
            Map<String, String> patronesAsignados, Map<String, List<String>> detallesPatron,
            Map<String, List<String>> fechasAsignacion, Map<String, List<String>> estadosAsignacion) {

        String matricula = embarcacion.getMatricula();
        inicializarMapasParaMatricula(matricula, detallesPatron, fechasAsignacion, estadosAsignacion);

        Integer idPatron = embarcacion.getIdPatronAsignado();

        if (idPatron == null) {
            registrarSinPatron(matricula, patronesAsignados, detallesPatron, fechasAsignacion, estadosAsignacion);
            return;
        }

        Patron patronActual = patronRepository.obtenerPatronPorId(idPatron);

        if (patronActual == null) {
            registrarPatronInvalido(matricula, idPatron, patronesAsignados, detallesPatron,
                    fechasAsignacion, estadosAsignacion);
            return;
        }

        registrarPatronActivo(matricula, patronActual, patronesAsignados, detallesPatron,
                fechasAsignacion, estadosAsignacion);
    }

    private void inicializarMapasParaMatricula(String matricula, Map<String, List<String>> detallesPatron,
            Map<String, List<String>> fechasAsignacion, Map<String, List<String>> estadosAsignacion) {
        detallesPatron.put(matricula, new ArrayList<>());
        fechasAsignacion.put(matricula, new ArrayList<>());
        estadosAsignacion.put(matricula, new ArrayList<>());
    }

    private void registrarSinPatron(String matricula, Map<String, String> patronesAsignados,
            Map<String, List<String>> detallesPatron, Map<String, List<String>> fechasAsignacion,
            Map<String, List<String>> estadosAsignacion) {
        patronesAsignados.put(matricula, "NO");
        detallesPatron.get(matricula).add("Sin patrón asignado");
        fechasAsignacion.get(matricula).add("-");
        estadosAsignacion.get(matricula).add("-");
    }

    private void registrarPatronInvalido(String matricula, int idPatron,
            Map<String, String> patronesAsignados, Map<String, List<String>> detallesPatron,
            Map<String, List<String>> fechasAsignacion, Map<String, List<String>> estadosAsignacion) {
        patronesAsignados.put(matricula, "NO");
        detallesPatron.get(matricula).add("ERROR: Patrón ID " + idPatron + " no encontrado.");
        fechasAsignacion.get(matricula).add("-");
        estadosAsignacion.get(matricula).add("INACTIVA/ERROR");
    }

    private void registrarPatronActivo(String matricula, Patron patron,
            Map<String, String> patronesAsignados, Map<String, List<String>> detallesPatron,
            Map<String, List<String>> fechasAsignacion, Map<String, List<String>> estadosAsignacion) {
        patronesAsignados.put(matricula, "SÍ");
        detallesPatron.get(matricula).add(
                patron.getNombre() + " " + patron.getApellidos() + " (ID: " + patron.getId() + ")");
        fechasAsignacion.get(matricula).add("Asignación actual");
        estadosAsignacion.get(matricula).add("ACTIVA");
    }

    private void agregarAtributosAlModelo(Model model, List<Embarcacion> embarcaciones,
            TipoEmbarcacion tipo, Map<String, String> patronesAsignados,
            Map<String, List<String>> detallesPatron, Map<String, List<String>> fechasAsignacion,
            Map<String, List<String>> estadosAsignacion) {
        model.addAttribute("embarcaciones", embarcaciones);
        model.addAttribute("patronesAsignados", patronesAsignados);
        model.addAttribute("detallesPatron", detallesPatron);
        model.addAttribute("fechasAsignacion", fechasAsignacion);
        model.addAttribute("estadosAsignacion", estadosAsignacion);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("tiposEmbarcacion", TipoEmbarcacion.values());
    }
}