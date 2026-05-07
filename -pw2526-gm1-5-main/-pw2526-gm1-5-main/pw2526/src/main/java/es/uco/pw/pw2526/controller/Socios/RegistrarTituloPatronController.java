package es.uco.pw.pw2526.controller.Socios;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.Socio.Socio;

@Controller
public class RegistrarTituloPatronController {

    private final SocioRepository socioRepository;

    public RegistrarTituloPatronController(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    @GetMapping("/registrarTituloPatron")
    public ModelAndView mostrarSociosPendientesDeTitulo() {
        List<Socio> miembrosPendientes = socioRepository.obtenerMiembrosSinTituloPatron();
        
        ModelAndView model = new ModelAndView("registrarTituloPatronView.html");
        model.addObject("socios", miembrosPendientes);
        return model;
    }

    @PostMapping("/registrarTituloPatron/otorgar")
    public ModelAndView otorgarTitulo(@RequestParam String dni) {
        Socio miembro = socioRepository.obtenerSocioPorDni(dni);

        if (miembro == null) {
            return construirVistaFallo("Error: DNI no encontrado en la base de datos de socios.");
        }

        /*
         * Refactorización aplicada: Move Method + Replace String Comparison with Enum.
         * La responsabilidad de determinar elegibilidad se delega al modelo Socio,
         * que ya encapsula métodos de dominio como esTitular() y esPatron().
         */
        if (!miembro.esElegibleParaTituloPatron()) {
            return construirVistaFallo(
                    "Error: Solo los socios titulares y cónyuges son elegibles para obtener el título de patrón.");
        }

        return ejecutarOtorgamiento(miembro, dni);
    }

    private ModelAndView ejecutarOtorgamiento(Socio miembro, String dni) {
        boolean otorgadoConExito = socioRepository.otorgarTituloPatron(dni);

        if (!otorgadoConExito) {
            return construirVistaFallo(
                    "Error de BD al actualizar el título de patrón para el DNI " + dni + ".");
        }

        miembro.setTieneTituloPatron(true);
        return construirVistaExito(miembro);
    }

    private ModelAndView construirVistaFallo(String mensajeError) {
        ModelAndView modelAndView = new ModelAndView("registrarTituloPatronViewFail.html");
        modelAndView.addObject("error", mensajeError);
        return modelAndView;
    }

    private ModelAndView construirVistaExito(Socio miembro) {
        ModelAndView modelAndView = new ModelAndView("registrarTituloPatronViewSuccess.html");
        modelAndView.addObject("mensaje",
                "Título de patrón otorgado a " + miembro.getNombre() + " " + miembro.getApellidos() + ".");
        modelAndView.addObject("socio", miembro);
        return modelAndView;
    }
}