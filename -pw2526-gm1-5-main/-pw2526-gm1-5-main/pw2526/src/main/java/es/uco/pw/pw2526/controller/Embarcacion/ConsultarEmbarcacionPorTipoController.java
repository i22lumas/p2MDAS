package es.uco.pw.pw2526.controller.Embarcacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.TiposBarcos;
import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.domain.Empleados.Empleados;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/consultarEmbarcacionPorTipo")
public class ConsultarEmbarcacionPorTipoController {

    @Autowired
    private EmbarcacionRepository embarcacionRepository;

    @Autowired // INYECTAR EMPLEADOS REPOSITORY
    private EmpleadosRepository empleadosRepository;

    /**
     * Muestra el formulario para consultar embarcaciones por tipo
     * 
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista del formulario
     */
    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("tiposBarcos", TiposBarcos.values());
        return "ConsultarEmbarcacionPorTipoView";
    }

    /**
     * Consulta las embarcaciones por tipo y muestra los resultados con información
     * de patrones asignados
     * 
     * @param tipo  Tipo de barco a consultar
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista de resultados
     */
    @PostMapping
    public String consultarPorTipo(@RequestParam TiposBarcos tipo, Model model) {
        System.out.println("🔍 INICIANDO CONSULTA POR TIPO: " + tipo);

        List<embarcacion> embarcaciones = embarcacionRepository.buscarPorTipo(tipo);
        System.out.println("✅ EMBARCACIONES ENCONTRADAS: " + embarcaciones.size());

        Map<String, String> patronesAsignados = new HashMap<>();
        Map<String, List<String>> detallesPatron = new HashMap<>();
        Map<String, List<String>> fechasAsignacion = new HashMap<>();
        Map<String, List<String>> estadosAsignacion = new HashMap<>();

        for (embarcacion emb : embarcaciones) {
            String matricula = emb.getMatricula();
            System.out.println("🔍 Procesando embarcación: " + matricula + " - " + emb.getNombre());

            detallesPatron.put(matricula, new ArrayList<>());
            fechasAsignacion.put(matricula, new ArrayList<>());
            estadosAsignacion.put(matricula, new ArrayList<>());

            Integer idPatron = emb.getIdPatronAsignado();

            if (idPatron != null) {
                Empleados patronActual = empleadosRepository.obtenerEmpleadoPorId(idPatron);

                if (patronActual != null) {
                    patronesAsignados.put(matricula, "SÍ");
                    detallesPatron.get(matricula).add(patronActual.getNombre() + " " + patronActual.getApellidos()
                            + " (ID: " + patronActual.getId() + ")");

                    fechasAsignacion.get(matricula).add("Asignación actual");
                    estadosAsignacion.get(matricula).add("ACTIVA");
                    System.out.println("✅ Patrón asignado: " + detallesPatron.get(matricula).get(0));
                } else {

                    patronesAsignados.put(matricula, "NO");
                    detallesPatron.get(matricula).add("ERROR: Empleado ID " + idPatron + " no encontrado.");
                    fechasAsignacion.get(matricula).add("-");
                    estadosAsignacion.get(matricula).add("INACTIVA/ERROR");
                    System.out.println("❌ Patrón asignado con ID inválido.");
                }
            } else {
                patronesAsignados.put(matricula, "NO");
                detallesPatron.get(matricula).add("Sin patrón asignado");
                fechasAsignacion.get(matricula).add("-");
                estadosAsignacion.get(matricula).add("-");
                System.out.println("❌ Sin patrón asignado");
            }
        }

        model.addAttribute("embarcaciones", embarcaciones);
        model.addAttribute("patronesAsignados", patronesAsignados);
        model.addAttribute("detallesPatron", detallesPatron);
        model.addAttribute("fechasAsignacion", fechasAsignacion);
        model.addAttribute("estadosAsignacion", estadosAsignacion);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("tiposBarcos", TiposBarcos.values());

        System.out.println("🏁 FINALIZANDO CONSULTA - Enviando " + embarcaciones.size() + " embarcaciones a la vista");
        return "ConsultarEmbarcacionPorTipoView";
    }
}