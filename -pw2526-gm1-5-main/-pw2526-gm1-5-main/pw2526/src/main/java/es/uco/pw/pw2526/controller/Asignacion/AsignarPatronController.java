package es.uco.pw.pw2526.controller.Asignacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import es.uco.pw.pw2526.model.Repository.AsignacionRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.EmpleadosRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.domain.Empleados.Empleados;

@Controller
@RequestMapping("/asignarPatron")
public class AsignarPatronController {

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private EmbarcacionRepository embarcacionRepository;

    @Autowired
    private EmpleadosRepository empleadosRepository;

    /**
     * Muestra el formulario para asignar un patrón a una embarcación
     * 
     * @param model Modelo para pasar datos a la vista
     * @return String con el nombre de la vista del formulario
     */
    @GetMapping
    public String mostrarFormulario(Model model) {
        List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        List<Empleados> empleadosDisponibles = empleadosRepository.obtenerEmpleadosDisponibles();

        model.addAttribute("embarcaciones", embarcaciones);
        model.addAttribute("empleados", empleadosDisponibles);
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

        System.out.println("🔍 Iniciando asignación para matrícula: " + matricula + ", empleado: " + idEmpleado);

        // Verificar si la embarcación ya tiene un patrón asignado
        Integer idPatronActual = asignacionRepository.obtenerPatronActual(matricula);
        System.out.println("🔍 ID Patrón Actual encontrado: " + idPatronActual);

        if (idPatronActual != null) {
            System.out.println("✅ Embarcación tiene patrón actual, mostrando confirmación...");

            Empleados patronActual = empleadosRepository.obtenerEmpleadoPorId(idPatronActual);
            Empleados nuevoPatron = empleadosRepository.obtenerEmpleadoPorId(idEmpleado);
            embarcacion embarcacion = embarcacionRepository.buscarPorMatricula(matricula);

            System.out.println("🔍 Patrón Actual: " + (patronActual != null ? patronActual.getNombre() : "null"));
            System.out.println("🔍 Nuevo Patrón: " + (nuevoPatron != null ? nuevoPatron.getNombre() : "null"));
            System.out.println("🔍 Embarcación: " + (embarcacion != null ? embarcacion.getNombre() : "null"));

            model.addAttribute("matricula", matricula);
            model.addAttribute("idEmpleadoNuevo", idEmpleado);
            model.addAttribute("patronActual", patronActual);
            model.addAttribute("nuevoPatron", nuevoPatron);
            model.addAttribute("embarcacion", embarcacion);

            return "ConfirmarReemplazoPatron";
        }

        System.out.println("🔍 Embarcación NO tiene patrón actual, asignando directamente...");

        // Si no hay patrón actual, asignar directamente
        if (asignacionRepository.asignarPatron(matricula, idEmpleado)) {
            model.addAttribute("mensaje", "✅ Patrón asignado correctamente a la embarcación.");
            return "AsignarPatronSuccess";
        } else {
            model.addAttribute("mensaje", "❌ Error al asignar patrón.");
            return "AsignarPatronFail";
        }
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

        System.out.println(
                "🔍 Confirmando reemplazo para matrícula: " + matricula + ", nuevo empleado: " + idEmpleadoNuevo);

        Integer idPatronActual = asignacionRepository.obtenerPatronActual(matricula);
        Empleados patronActual = null;
        if (idPatronActual != null) {
            patronActual = empleadosRepository.obtenerEmpleadoPorId(idPatronActual);
        }

        Empleados nuevoPatron = empleadosRepository.obtenerEmpleadoPorId(idEmpleadoNuevo);
        embarcacion embarcacion = embarcacionRepository.buscarPorMatricula(matricula);

        boolean asignacionExitosa = asignacionRepository.asignarPatron(matricula, idEmpleadoNuevo);

        if (asignacionExitosa) {
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("✅ Patrón ").append(nuevoPatron.getNombre()).append(" ").append(nuevoPatron.getApellidos())
                    .append(" asignado correctamente a la embarcación ").append(embarcacion.getNombre()).append(".");

            if (patronActual != null) {
                mensaje.append("\n\n🔄 Se ha liberado al patrón anterior: ")
                        .append(patronActual.getNombre()).append(" ").append(patronActual.getApellidos())
                        .append("\n📝 Este patrón ahora está disponible para nuevas asignaciones.");
            }

            model.addAttribute("mensaje", mensaje.toString());
            return "AsignarPatronSuccess";
        } else {
            model.addAttribute("mensaje", "❌ Error al asignar nuevo patrón.");
            return "AsignarPatronFail";
        }
    }
}