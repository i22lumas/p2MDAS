package es.uco.pw.pw2526.controller.Alquiler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.Repository.AlquilerRepository;
import es.uco.pw.pw2526.model.Repository.EmbarcacionRepository;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.Repository.ReservaRepository;
import es.uco.pw.pw2526.model.domain.alquiler.Alquiler;
import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.Socio.Socio;

@Controller
public class AlquilerController {

    private AlquilerRepository alquilerRepository;
    private EmbarcacionRepository embarcacionRepository;
    private SocioRepository socioRepository;
    private ReservaRepository reservaRepository;

    public AlquilerController(AlquilerRepository alquilerRepository,
            EmbarcacionRepository embarcacionRepository,
            SocioRepository socioRepository,
            ReservaRepository reservaRepository) {
        this.alquilerRepository = alquilerRepository;
        this.embarcacionRepository = embarcacionRepository;
        this.socioRepository = socioRepository;
        this.reservaRepository = reservaRepository;
    }

    // ========== Endpoints públicos ==========

    @GetMapping("/buscarEmbarcacionesDisponibles")
    public ModelAndView mostrarBusquedaDisponibilidad() {
        return new ModelAndView("buscarEmbarcacionesDisponibles.html");
    }

    @PostMapping("/buscarEmbarcacionesDisponibles")
    public ModelAndView buscarEmbarcacionesDisponibles(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {
        ModelAndView model = new ModelAndView("buscarEmbarcacionesDisponibles.html");

        if (fechaInicio.isAfter(fechaFin)) {
            return agregarError(model, "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        if (fechaInicio.isBefore(LocalDate.now())) {
            return agregarError(model, "La fecha de inicio no puede ser en el pasado.");
        }

        return ejecutarBusquedaDisponibilidad(model, fechaInicio, fechaFin);
    }

    @GetMapping("/alquilarEmbarcacion")
    public ModelAndView mostrarFormularioAlquiler() {
        ModelAndView model = new ModelAndView("addAlquilerView.html");
        model.addObject("nuevoAlquiler", new Alquiler());
        model.addObject("embarcaciones", cargarEmbarcaciones());
        return model;
    }

    @PostMapping("/alquilarEmbarcacion")
    public ModelAndView procesarAlquiler(@ModelAttribute Alquiler nuevoAlquiler,
            @RequestParam(value = "dnisTripulantes", required = false) List<String> dnisTripulantes) {

        asignarTripulantes(nuevoAlquiler, dnisTripulantes);

        ModelAndView errorSocio = validarSocioTitular(nuevoAlquiler);
        if (errorSocio != null) {
            return errorSocio;
        }

        Embarcacion embarcacion = buscarEmbarcacionPorMatricula(nuevoAlquiler.getMatriculaEmbarcacion());
        if (embarcacion == null) {
            return construirVistaFallo("La embarcación seleccionada no existe.");
        }

        ModelAndView errorFechas = validarFechasAlquiler(nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin());
        if (errorFechas != null) {
            return errorFechas;
        }

        ModelAndView errorTemporada = validarDuracionSegunTemporada(nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin());
        if (errorTemporada != null) {
            return errorTemporada;
        }

        ModelAndView errorCapacidad = validarCapacidadTripulantes(nuevoAlquiler, embarcacion);
        if (errorCapacidad != null) {
            return errorCapacidad;
        }

        ModelAndView errorDisponibilidad = validarDisponibilidadEmbarcacion(
                embarcacion, nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin());
        if (errorDisponibilidad != null) {
            return errorDisponibilidad;
        }

        return ejecutarAlquiler(nuevoAlquiler, embarcacion);
    }

    @GetMapping("/listarAlquileresFuturos")
    public ModelAndView mostrarAlquileresFuturos() {
        return ejecutarConsultaAlquileres(
                "listarAlquileresView.html",
                alquilerRepository.obtenerAlquileresFuturos(),
                "No hay alquileres futuros registrados.",
                "Error al cargar los alquileres futuros.");
    }

    @GetMapping("/listarTodosAlquileres")
    public ModelAndView mostrarTodosAlquileres() {
        return ejecutarConsultaAlquileres(
                "listarTodosAlquileresView.html",
                alquilerRepository.obtenerTodosAlquileres(),
                "No hay alquileres registrados.",
                "Error al cargar los alquileres.");
    }

    @GetMapping("/buscarAlquileresPorSocio")
    public ModelAndView mostrarBusquedaAlquileresPorSocio() {
        return new ModelAndView("buscarAlquileresPorSocio.html");
    }

    @PostMapping("/buscarAlquileresPorSocio")
    public ModelAndView buscarAlquileresPorSocio(@RequestParam String dniSocio) {
        ModelAndView model = ejecutarConsultaAlquileres(
                "buscarAlquileresPorSocio.html",
                alquilerRepository.obtenerAlquileresPorSocio(dniSocio),
                "No se encontraron alquileres para el DNI: " + dniSocio,
                "Error al buscar alquileres para el DNI: " + dniSocio);
        model.addObject("dniSocio", dniSocio);
        return model;
    }

    @GetMapping("/buscarAlquileresPorEmbarcacion")
    public ModelAndView mostrarBusquedaAlquileresPorEmbarcacion() {
        ModelAndView model = new ModelAndView("buscarAlquileresPorEmbarcacion.html");
        model.addObject("embarcaciones", cargarEmbarcaciones());
        return model;
    }

    @PostMapping("/buscarAlquileresPorEmbarcacion")
    public ModelAndView buscarAlquileresPorEmbarcacion(@RequestParam String matricula) {
        ModelAndView model = ejecutarConsultaAlquileres(
                "buscarAlquileresPorEmbarcacion.html",
                alquilerRepository.obtenerAlquileresPorEmbarcacion(matricula),
                "No se encontraron alquileres para la matrícula: " + matricula,
                "Error al buscar alquileres para la matrícula: " + matricula);
        model.addObject("embarcaciones", cargarEmbarcaciones());
        model.addObject("matricula", matricula);
        return model;
    }

    // ========== Métodos privados: Validación ==========

    private ModelAndView validarSocioTitular(Alquiler nuevoAlquiler) {
        Socio titular = socioRepository.obtenerSocioPorDni(nuevoAlquiler.getDniSocioTitular());

        if (titular == null) {
            return construirVistaFallo(
                    "Socio titular con DNI " + nuevoAlquiler.getDniSocioTitular() + " no encontrado.");
        }

        if (!titular.esPatron()) {
            return construirVistaFallo("El socio titular no tiene título de patrón de embarcación.");
        }

        nuevoAlquiler.setIdSocioTitular(titular.getId());
        return null;
    }

    private ModelAndView validarFechasAlquiler(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            return construirVistaFallo("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        if (inicio.isBefore(LocalDate.now())) {
            return construirVistaFallo("La fecha de inicio no puede ser en el pasado.");
        }

        return null;
    }

    private ModelAndView validarDuracionSegunTemporada(LocalDate inicio, LocalDate fin) {
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        int mes = inicio.getMonthValue();

        if (esTemporadaBaja(mes) && dias > 3) {
            return construirVistaFallo(
                    "En temporada baja (octubre-abril) el máximo es 3 días. Seleccionaste: " + dias + " días.");
        }

        if (esTemporadaAlta(mes) && dias != 7 && dias != 14) {
            return construirVistaFallo(
                    "En temporada alta (mayo-septiembre) solo se permiten 7 días (1 semana) o 14 días (2 semanas). Seleccionaste: "
                            + dias + " días.");
        }

        return null;
    }

    private ModelAndView validarCapacidadTripulantes(Alquiler nuevoAlquiler, Embarcacion embarcacion) {
        int plazas = nuevoAlquiler.getPlazasSolicitadas();
        int totalTripulantes = calcularTotalTripulantes(nuevoAlquiler);

        if (totalTripulantes > plazas) {
            return construirVistaFallo(
                    "El número de tripulantes (" + totalTripulantes + ") excede las plazas solicitadas (" + plazas
                            + ").");
        }

        if (plazas > embarcacion.getNumeroPlazas()) {
            return construirVistaFallo(
                    "Plazas solicitadas (" + plazas + ") exceden la capacidad de la embarcación ("
                            + embarcacion.getNumeroPlazas() + " plazas).");
        }

        return null;
    }

    private ModelAndView validarDisponibilidadEmbarcacion(Embarcacion embarcacion, LocalDate inicio, LocalDate fin) {
        boolean disponibleAlquiler = alquilerRepository.estaDisponible(embarcacion.getMatricula(), inicio, fin);
        boolean disponibleReserva = reservaRepository.estaDisponible(embarcacion.getMatricula(), inicio, fin);

        if (!disponibleAlquiler || !disponibleReserva) {
            return construirVistaFallo(
                    "La embarcación '" + embarcacion.getNombre()
                            + "' no está disponible en las fechas seleccionadas ("
                            + inicio + " a " + fin
                            + "). Ya está reservada o alquilada. Por favor, seleccione otras fechas.");
        }

        return null;
    }

    // ========== Métodos privados: Lógica de negocio ==========

    private void asignarTripulantes(Alquiler nuevoAlquiler, List<String> dnisTripulantes) {
        if (dnisTripulantes == null) {
            nuevoAlquiler.setDnisTripulantes(new ArrayList<>());
            return;
        }

        List<String> dnisFiltrados = dnisTripulantes.stream()
                .filter(dni -> dni != null && !dni.trim().isEmpty())
                .collect(Collectors.toList());
        nuevoAlquiler.setDnisTripulantes(dnisFiltrados);
    }

    private ModelAndView ejecutarAlquiler(Alquiler nuevoAlquiler, Embarcacion embarcacion) {
        long dias = ChronoUnit.DAYS.between(nuevoAlquiler.getFechaInicio(), nuevoAlquiler.getFechaFin()) + 1;
        int plazas = nuevoAlquiler.getPlazasSolicitadas();

        nuevoAlquiler.setPrecioTotal(calcularPrecioAlquiler(plazas, dias));

        int idAlquiler = alquilerRepository.insertarAlquilerYRetornarId(nuevoAlquiler);

        if (idAlquiler <= 0) {
            return construirVistaFallo("Error al realizar el alquiler en la base de datos.");
        }

        return construirVistaExitoAlquiler(nuevoAlquiler, embarcacion, dias);
    }

    private double calcularPrecioAlquiler(int plazas, long dias) {
        return 20.0 * plazas * dias;
    }

    private int calcularTotalTripulantes(Alquiler alquiler) {
        int tripulantesExtra = (alquiler.getDnisTripulantes() != null) ? alquiler.getDnisTripulantes().size() : 0;
        return 1 + tripulantesExtra;
    }

    // ========== Métodos privados: Búsqueda y carga de datos ==========

    private Embarcacion buscarEmbarcacionPorMatricula(String matricula) {
        List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        if (embarcaciones == null) {
            return null;
        }

        return embarcaciones.stream()
                .filter(embarcacion -> embarcacion.getMatricula().equals(matricula))
                .findFirst()
                .orElse(null);
    }

    private List<Embarcacion> cargarEmbarcaciones() {
        try {
            List<Embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
            return embarcaciones != null ? embarcaciones : new ArrayList<>();
        } catch (Exception excepcion) {
            return new ArrayList<>();
        }
    }

    private ModelAndView ejecutarBusquedaDisponibilidad(ModelAndView model, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            List<Embarcacion> disponibles = obtenerEmbarcacionesDisponibles(fechaInicio, fechaFin);
            model.addObject("embarcacionesDisponibles", disponibles);
            model.addObject("fechaInicio", fechaInicio);
            model.addObject("fechaFin", fechaFin);
            model.addObject("mensaje", construirMensajeDisponibilidad(disponibles));
        } catch (Exception excepcion) {
            model.addObject("error", "Error al buscar disponibilidad: " + excepcion.getMessage());
        }
        return model;
    }

    private List<Embarcacion> obtenerEmbarcacionesDisponibles(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Embarcacion> todasEmbarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        if (todasEmbarcaciones == null || todasEmbarcaciones.isEmpty()) {
            return new ArrayList<>();
        }

        List<Embarcacion> disponibles = new ArrayList<>();
        for (Embarcacion embarcacion : todasEmbarcaciones) {
            if (estaDisponibleEnRango(embarcacion.getMatricula(), fechaInicio, fechaFin)) {
                disponibles.add(embarcacion);
            }
        }
        return disponibles;
    }

    private boolean estaDisponibleEnRango(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        return alquilerRepository.estaDisponible(matricula, fechaInicio, fechaFin)
                && reservaRepository.estaDisponible(matricula, fechaInicio, fechaFin);
    }

    // ========== Métodos privados: Consultas genéricas de alquileres ==========

    private ModelAndView ejecutarConsultaAlquileres(String vista, List<Alquiler> alquileres,
            String mensajeVacio, String mensajeError) {
        ModelAndView model = new ModelAndView(vista);
        try {
            List<Alquiler> resultado = alquileres != null ? alquileres : new ArrayList<>();
            model.addObject("alquileres", resultado);
            model.addObject("mensaje", resultado.isEmpty() ? mensajeVacio
                    : "Se encontraron " + resultado.size() + " alquileres.");
        } catch (Exception excepcion) {
            model.addObject("alquileres", new ArrayList<>());
            model.addObject("mensaje", mensajeError);
        }
        return model;
    }

    // ========== Métodos privados: Construcción de vistas ==========

    private ModelAndView construirVistaFallo(String mensaje) {
        ModelAndView model = new ModelAndView("addAlquilerViewFail.html");
        model.addObject("mensaje", mensaje);
        return model;
    }

    private ModelAndView construirVistaExitoAlquiler(Alquiler alquiler, Embarcacion embarcacion, long dias) {
        ModelAndView model = new ModelAndView("addAlquilerViewSuccess.html");
        model.addObject("mensaje", "Alquiler realizado con éxito para " + embarcacion.getNombre() + ".");
        model.addObject("alquiler", alquiler);
        model.addObject("dias", dias);
        model.addObject("precioPorPersonaPorDia", 20.0);
        return model;
    }

    private ModelAndView agregarError(ModelAndView model, String mensaje) {
        model.addObject("error", mensaje);
        return model;
    }

    private String construirMensajeDisponibilidad(List<Embarcacion> disponibles) {
        if (disponibles.isEmpty()) {
            return "No hay embarcaciones disponibles en las fechas seleccionadas.";
        }
        return "Se encontraron " + disponibles.size() + " embarcaciones disponibles.";
    }

    // ========== Métodos privados: Utilidades de temporada ==========

    private boolean esTemporadaBaja(int mes) {
        return mes >= 10 || mes <= 4;
    }

    private boolean esTemporadaAlta(int mes) {
        return mes >= 5 && mes <= 9;
    }
}