package es.uco.pw.pw2526.controller.Asignacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import es.uco.pw.pw2526.model.Repository.AsignacionRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.PatronRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.Empleados.Patron;

@Controller
@RequestMapping("/asignarPatron")
public class AsignarPatronController {

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private EmbarcacionRepository embarcacionRepository;

    @Autowired
    private PatronRepository patronRepository;

    /**
     * Muestra el formulario para asignar un patrón a una embarcación
     * 
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista del formulario
     */
    @GetMapping
    public String mostrarFormulario(Model model) {
        List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        List<Patron> patronesDisponibles = patronRepository.obtenerPatronesDisponibles();

        model.addAttribute("embarcaciones", embarcaciones);
        model.addAttribute("empleados", patronesDisponibles);
        return "AsignarPatronView";
    }

    /**
     * Procesa la asignación de un patrón a una embarcación
     * 
     * @param matricula  Matrícula de la embarcación
     * @param idEmpleado ID del empleado a asignar como patrón
     * @param model      Modelo para pasar datos a la vista
     * @return String con el nombre de la vista de resultado
     */
    @PostMapping
    public String asignarPatron(@RequestParam String matricula,
            @RequestParam int idEmpleado,
            Model model) {

        Integer idPatronActual = asignacionRepository.obtenerPatronActual(matricula);

        if (idPatronActual != null) {
            return mostrarConfirmacionReemplazo(matricula, idPatronActual, idEmpleado, model);
        }

        return ejecutarAsignacionDirecta(matricula, idEmpleado, model);
    }

    /**
     * Confirma el reemplazo de un patrón existente por uno nuevo
     * 
     * @param matricula       Matrícula de la embarcación
     * @param idEmpleadoNuevo ID del nuevo empleado a asignar como patrón
     * @param model           Modelo para pasar datos a la vista
     * @return String con el nombre de la vista de resultado
     */
    @PostMapping("/confirmarReemplazo")
    public String confirmarReemplazo(@RequestParam String matricula,
            @RequestParam int idEmpleadoNuevo,
            Model model) {

        Patron patronActual = obtenerPatronActualDeEmbarcacion(matricula);
        Patron nuevoPatron = patronRepository.obtenerPatronPorId(idEmpleadoNuevo);
        Embarcacion embarcacion = embarcacionRepository.buscarPorMatricula(matricula);

        boolean asignacionExitosa = asignacionRepository.asignarPatron(matricula, idEmpleadoNuevo);

        if (!asignacionExitosa) {
            model.addAttribute("mensaje", "❌ Error al asignar nuevo patrón.");
            return "AsignarPatronFail";
        }

        String mensaje = construirMensajeReemplazo(nuevoPatron, embarcacion, patronActual);
        model.addAttribute("mensaje", mensaje);
        return "AsignarPatronSuccess";
    }



    private String mostrarConfirmacionReemplazo(String matricula, int idPatronActual,
            int idEmpleadoNuevo, Model model) {
        Patron patronActual = patronRepository.obtenerPatronPorId(idPatronActual);
        Patron nuevoPatron = patronRepository.obtenerPatronPorId(idEmpleadoNuevo);
        Embarcacion embarcacion = embarcacionRepository.buscarPorMatricula(matricula);

        model.addAttribute("matricula", matricula);
        model.addAttribute("idEmpleadoNuevo", idEmpleadoNuevo);
        model.addAttribute("patronActual", patronActual);
        model.addAttribute("nuevoPatron", nuevoPatron);
        model.addAttribute("embarcacion", embarcacion);

        return "ConfirmarReemplazoPatron";
    }

    private String ejecutarAsignacionDirecta(String matricula, int idEmpleado, Model model) {
        if (asignacionRepository.asignarPatron(matricula, idEmpleado)) {
            model.addAttribute("mensaje", "✅ Patrón asignado correctamente a la embarcación.");
            return "AsignarPatronSuccess";
        }

        model.addAttribute("mensaje", "❌ Error al asignar patrón.");
        return "AsignarPatronFail";
    }

    private Patron obtenerPatronActualDeEmbarcacion(String matricula) {
        Integer idPatronActual = asignacionRepository.obtenerPatronActual(matricula);
        if (idPatronActual == null) {
            return null;
        }
        return patronRepository.obtenerPatronPorId(idPatronActual);
    }

    private String construirMensajeReemplazo(Patron nuevoPatron, Embarcacion embarcacion, Patron patronActual) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("✅ Patrón ").append(nuevoPatron.getNombre()).append(" ").append(nuevoPatron.getApellidos())
                .append(" asignado correctamente a la embarcación ").append(embarcacion.getNombre()).append(".");

        if (patronActual != null) {
            mensaje.append("\n\n🔄 Se ha liberado al patrón anterior: ")
                    .append(patronActual.getNombre()).append(" ").append(patronActual.getApellidos())
                    .append("\n📝 Este patrón ahora está disponible para nuevas asignaciones.");
        }

        return mensaje.toString();
    }
}