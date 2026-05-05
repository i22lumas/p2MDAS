package es.uco.pw.pw2526.controller.Reserva;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.domain.reserva.Reserva;
import es.uco.pw.pw2526.model.Repository.ReservaRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
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
        model.addObject("nuevaReserva", new Reserva());

        List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        List<Socio> socios = socioRepository.obtenerTodosSocios();

        model.addObject("embarcaciones", embarcaciones);
        model.addObject("socios", socios);
        model.addObject("fechaHoy", LocalDate.now());

        return model;
    }

    /**
     * Procesa la solicitud de reserva de una embarcación
     * 
     * @param nuevaReserva Objeto Reserva con los datos de la nueva reserva
     * @return ModelAndView con el resultado de la operación (éxito o error)
     */
    @PostMapping("/reservarEmbarcacion")
    public ModelAndView procesarReserva(@ModelAttribute Reserva nuevaReserva) {
        ModelAndView errorValidacion = validarDatosReserva(nuevaReserva);
        if (errorValidacion != null) {
            return errorValidacion;
        }

        ModelAndView errorRequisitos = verificarRequisitosPreviosReserva(nuevaReserva);
        if (errorRequisitos != null) {
            return errorRequisitos;
        }

        return ejecutarReserva(nuevaReserva);
    }

    /**
     * Muestra todas las reservas existentes
     * 
     * @return ModelAndView con la lista de todas las reservas
     */
    @GetMapping("/verReservas")
    public ModelAndView mostrarReservas() {
        List<Reserva> reservas = reservaRepository.obtenerTodasLasReservas();
        ModelAndView model = new ModelAndView("verReservasView.html");
        model.addObject("reservas", reservas);
        return model;
    }

    // ========== Métodos privados: Validación de datos de entrada ==========

    private ModelAndView validarDatosReserva(Reserva nuevaReserva) {
        if (nuevaReserva.getPlazasSolicitadas() == null || nuevaReserva.getPlazasSolicitadas() <= 0) {
            return construirVistaFallo("El número de plazas debe ser mayor a 0.");
        }

        if (nuevaReserva.getFechaActividad() == null || nuevaReserva.getFechaActividad().isBefore(LocalDate.now())) {
            return construirVistaFallo("La fecha de la actividad no puede ser anterior a hoy.");
        }

        if (nuevaReserva.getIdSocioSolicitante() == null) {
            return construirVistaFallo("Debe seleccionar un socio solicitante.");
        }

        if (nuevaReserva.getMatriculaEmbarcacion() == null || nuevaReserva.getMatriculaEmbarcacion().isEmpty()) {
            return construirVistaFallo("Debe seleccionar una embarcación.");
        }

        return null;
    }

    // ========== Métodos privados: Verificación de requisitos ==========

    private ModelAndView verificarRequisitosPreviosReserva(Reserva nuevaReserva) {
        ModelAndView errorPatron = verificarPatronAsignado(nuevaReserva);
        if (errorPatron != null) {
            return errorPatron;
        }

        if (!estaDisponibleParaReserva(nuevaReserva)) {
            return construirVistaFallo(
                    "La embarcación seleccionada no está disponible para la fecha elegida (ya está reservada o alquilada).");
        }

        ModelAndView errorCapacidad = verificarCapacidadEmbarcacion(nuevaReserva);
        if (errorCapacidad != null) {
            return errorCapacidad;
        }

        return null;
    }

    private ModelAndView verificarPatronAsignado(Reserva nuevaReserva) {
        boolean tienePatron = reservaRepository.tienePatronAsignado(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad());

        if (!tienePatron) {
            return construirVistaFallo("La embarcación seleccionada no tiene patrón asignado para la fecha elegida.");
        }

        return null;
    }

    private boolean estaDisponibleParaReserva(Reserva nuevaReserva) {
        return reservaRepository.estaDisponible(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad(),
                nuevaReserva.getFechaActividad());
    }

    private ModelAndView verificarCapacidadEmbarcacion(Reserva nuevaReserva) {
        int capacidad = reservaRepository.obtenerCapacidadEmbarcacion(nuevaReserva.getMatriculaEmbarcacion());
        int capacidadDisponible = capacidad - 1;

        if (nuevaReserva.getPlazasSolicitadas() > capacidadDisponible) {
            return construirVistaFallo(
                    "La embarcación seleccionada no tiene capacidad suficiente. Capacidad máxima para socios: "
                            + capacidadDisponible + " plazas (capacidad total: " + capacidad
                            + " incluyendo al patrón).");
        }

        return null;
    }

    // ========== Métodos privados: Ejecución de la reserva ==========

    private ModelAndView ejecutarReserva(Reserva nuevaReserva) {
        Integer idPatron = obtenerPatronParaReserva(nuevaReserva);
        if (idPatron == null) {
            return construirVistaFallo("No se pudo determinar el patrón asignado a la embarcación.");
        }

        nuevaReserva.setIdPatron(idPatron);
        calcularPrecioReserva(nuevaReserva);

        boolean insertadoConExito = reservaRepository.insertarReserva(nuevaReserva);

        if (!insertadoConExito) {
            return construirVistaFallo("Error al realizar la reserva. Por favor, inténtelo de nuevo.");
        }

        return construirVistaExitoReserva(nuevaReserva);
    }

    private Integer obtenerPatronParaReserva(Reserva nuevaReserva) {
        return reservaRepository.obtenerPatronAsignado(
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad());
    }

    private void calcularPrecioReserva(Reserva nuevaReserva) {
        int totalPersonas = nuevaReserva.getPlazasSolicitadas() + 1;
        double precioTotal = totalPersonas * 40.0;
        nuevaReserva.setPrecioTotal(precioTotal);
    }

    // ========== Métodos privados: Construcción de vistas ==========

    private ModelAndView construirVistaFallo(String mensaje) {
        ModelAndView model = new ModelAndView("reservaViewFail.html");
        model.addObject("mensaje", mensaje);
        return model;
    }

    private ModelAndView construirVistaExitoReserva(Reserva nuevaReserva) {
        int totalPersonas = nuevaReserva.getPlazasSolicitadas() + 1;
        ModelAndView model = new ModelAndView("reservaViewSuccess.html");
        model.addObject("mensaje", "Reserva realizada con éxito.");
        model.addObject("reserva", nuevaReserva);
        model.addObject("precioPorPersona", 40.0);
        model.addObject("totalPersonas", totalPersonas);
        return model;
    }
}