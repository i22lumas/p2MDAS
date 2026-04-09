package es.uco.pw.pw2526.controller.Reserva;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.reserva.reserva;
import es.uco.pw.pw2526.model.Repository.ReservaRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.domain.Socio.Socio;

@Controller
public class ReservaController {

    private ReservaRepository reservaRepository;
    private EmbarcacionRepository embarcacionRepository;
    private SocioRepository socioRepository;

    /**
     * Constructor del controlador de reservas
     * 
     * @param reservaRepository     Repositorio de reservas
     * @param embarcacionRepository Repositorio de embarcaciones
     * @param socioRepository       Repositorio de socios
     */
    public ReservaController(ReservaRepository reservaRepository,
            EmbarcacionRepository embarcacionRepository,
            SocioRepository socioRepository) {
        this.reservaRepository = reservaRepository;
        this.embarcacionRepository = embarcacionRepository;
        this.socioRepository = socioRepository;
    }

    /**
     * Muestra el formulario para realizar una reserva
     * 
     * @return ModelAndView con el formulario de reserva
     */
    @GetMapping("/reservarEmbarcacion")
    public ModelAndView mostrarFormularioReserva() {
        ModelAndView model = new ModelAndView("reservaView.html");
        model.addObject("nuevaReserva", new reserva());

        List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        List<Socio> socios = socioRepository.obtenerTodosSocios();

        model.addObject("embarcaciones", embarcaciones);
        model.addObject("socios", socios);
        model.addObject("fechaHoy", LocalDate.now());

        return model;
    }

    /**
     * Procesa la solicitud de reserva de una embarcación
     * 
     * @param nuevaReserva Objeto reserva con los datos de la nueva reserva
     * @return ModelAndView con el resultado de la operación (éxito o error)
     */
    @PostMapping("/reservarEmbarcacion")
    public ModelAndView procesarReserva(@ModelAttribute reserva nuevaReserva) {
        ModelAndView model = new ModelAndView();

        System.out.println("=== INICIO PROCESAR RESERVA ===");
        System.out.println("Socio solicitante: " + nuevaReserva.getIdSocioSolicitante());
        System.out.println("Matrícula: " + nuevaReserva.getMatriculaEmbarcacion());
        System.out.println("Fecha actividad: " + nuevaReserva.getFechaActividad());
        System.out.println("Plazas solicitadas: " + nuevaReserva.getPlazasSolicitadas());

        if (nuevaReserva.getPlazasSolicitadas() == null || nuevaReserva.getPlazasSolicitadas() <= 0) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "El número de plazas debe ser mayor a 0.");
            return model;
        }

        if (nuevaReserva.getFechaActividad() == null || nuevaReserva.getFechaActividad().isBefore(LocalDate.now())) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "La fecha de la actividad no puede ser anterior a hoy.");
            return model;
        }

        if (nuevaReserva.getIdSocioSolicitante() == null) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "Debe seleccionar un socio solicitante.");
            return model;
        }

        if (nuevaReserva.getMatriculaEmbarcacion() == null || nuevaReserva.getMatriculaEmbarcacion().isEmpty()) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "Debe seleccionar una embarcación.");
            return model;
        }

        boolean tienePatron = reservaRepository.tienePatronAsignado(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad());

        if (!tienePatron) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "La embarcación seleccionada no tiene patrón asignado para la fecha elegida.");
            return model;
        }

        boolean estaDisponible = reservaRepository.estaDisponible(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad(),
                nuevaReserva.getFechaActividad());

        if (!estaDisponible) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje",
                    "La embarcación seleccionada no está disponible para la fecha elegida (ya está reservada o alquilada).");
            return model;
        }

        int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(nuevaReserva.getMatriculaEmbarcacion());
        int capacidadDisponible = capacidad - 1;

        if (nuevaReserva.getPlazasSolicitadas() > capacidadDisponible) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje",
                    "La embarcación seleccionada no tiene capacidad suficiente. Capacidad máxima para socios: "
                            + capacidadDisponible + " plazas (capacidad total: " + capacidad
                            + " incluyendo al patrón).");
            return model;
        }

        Integer idPatron = reservaRepository.obtenerPatronAsignado(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad());

        if (idPatron == null) {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "No se pudo determinar el patrón asignado a la embarcación.");
            return model;
        }

        nuevaReserva.setIdPatron(idPatron);

        int totalPersonas = nuevaReserva.getPlazasSolicitadas() + 1;
        double precioTotal = totalPersonas * 40.0;
        nuevaReserva.setPrecioTotal(precioTotal);

        System.out.println("Precio calculado: " + precioTotal + "€ (" + totalPersonas + " personas × 40€)");

        boolean exito = reservaRepository.insertarReserva(nuevaReserva);

        System.out.println("Inserción exitosa: " + exito);
        System.out.println("=== FIN PROCESAR RESERVA ===");

        if (exito) {
            model.setViewName("reservaViewSuccess.html");
            model.addObject("mensaje", "Reserva realizada con éxito.");
            model.addObject("reserva", nuevaReserva);
            model.addObject("precioPorPersona", 40.0);
            model.addObject("totalPersonas", totalPersonas); // Para mostrar en la vista
        } else {
            model.setViewName("reservaViewFail.html");
            model.addObject("mensaje", "Error al realizar la reserva. Por favor, inténtelo de nuevo.");
        }

        return model;
    }

    /**
     * Muestra todas las reservas existentes
     * 
     * @return ModelAndView con la lista de todas las reservas
     */
    @GetMapping("/verReservas")
    public ModelAndView mostrarReservas() {
        List<reserva> reservas = reservaRepository.obtenerTodasLasReservas();
        ModelAndView model = new ModelAndView("verReservasView.html");
        model.addObject("reservas", reservas);
        return model;
    }
}