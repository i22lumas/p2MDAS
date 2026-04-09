package es.uco.pw.pw2526.controller.Alquiler;

import java.time.LocalDate;
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
import es.uco.pw.pw2526.model.domain.alquiler.alquiler;
import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
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

    @GetMapping("/buscarEmbarcacionesDisponibles")
    public ModelAndView mostrarBusquedaDisponibilidad() {
        ModelAndView model = new ModelAndView("buscarEmbarcacionesDisponibles.html");
        return model;
    }

    @PostMapping("/buscarEmbarcacionesDisponibles")
    public ModelAndView buscarEmbarcacionesDisponibles(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {

        ModelAndView model = new ModelAndView("buscarEmbarcacionesDisponibles.html");

        System.out.println("=== INICIO BÚSQUEDA DISPONIBILIDAD ===");
        System.out.println("Fecha inicio: " + fechaInicio);
        System.out.println("Fecha fin: " + fechaFin);

        if (fechaInicio.isAfter(fechaFin)) {
            model.addObject("error", "La fecha de inicio no puede ser posterior a la fecha de fin.");
            return model;
        }

        if (fechaInicio.isBefore(LocalDate.now())) {
            model.addObject("error", "La fecha de inicio no puede ser en el pasado.");
            return model;
        }

        try {
            List<embarcacion> todasEmbarcaciones = embarcacionRepository.obtenerEmbarcaciones();
            System.out.println("Total embarcaciones encontradas: "
                    + (todasEmbarcaciones != null ? todasEmbarcaciones.size() : "NULL"));

            if (todasEmbarcaciones == null || todasEmbarcaciones.isEmpty()) {
                model.addObject("error", "No se pudieron cargar las embarcaciones.");
                return model;
            }

            List<embarcacion> embarcacionesDisponibles = new ArrayList<>();

            for (embarcacion embarcacion : todasEmbarcaciones) {
                System.out.println("Verificando disponibilidad de: " + embarcacion.getMatricula());

                boolean disponibleAlquiler = alquilerRepository.estaDisponible(embarcacion.getMatricula(), fechaInicio,
                        fechaFin);
                boolean disponibleReserva = reservaRepository.estaDisponible(embarcacion.getMatricula(), fechaInicio,
                        fechaFin);
                boolean disponible = disponibleAlquiler && disponibleReserva;

                System.out.println("Disponibilidad - Alquiler: " + disponibleAlquiler +
                        ", Reserva: " + disponibleReserva +
                        ", Total: " + disponible);

                if (disponible) {
                    embarcacionesDisponibles.add(embarcacion);
                }
            }

            System.out.println("Embarcaciones disponibles encontradas: " + embarcacionesDisponibles.size());
            System.out.println("=== FIN BÚSQUEDA DISPONIBILIDAD ===");

            model.addObject("embarcacionesDisponibles", embarcacionesDisponibles);
            model.addObject("fechaInicio", fechaInicio);
            model.addObject("fechaFin", fechaFin);
            model.addObject("mensaje",
                    embarcacionesDisponibles.isEmpty() ? "No hay embarcaciones disponibles en las fechas seleccionadas."
                            : "Se encontraron " + embarcacionesDisponibles.size() + " embarcaciones disponibles.");

        } catch (Exception e) {
            System.err.println("ERROR en búsqueda de disponibilidad: " + e.getMessage());
            e.printStackTrace();
            model.addObject("error", "Error al buscar disponibilidad: " + e.getMessage());
        }

        return model;
    }

    @GetMapping("/alquilarEmbarcacion")
    public ModelAndView mostrarFormularioAlquiler() {
        ModelAndView model = new ModelAndView("addAlquilerView.html");
        model.addObject("nuevoAlquiler", new alquiler());

        try {
            List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
            model.addObject("embarcaciones", embarcaciones != null ? embarcaciones : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error cargando embarcaciones: " + e.getMessage());
            model.addObject("embarcaciones", new ArrayList<>());
        }

        return model;
    }

    @PostMapping("/alquilarEmbarcacion")
    public ModelAndView procesarAlquiler(@ModelAttribute alquiler nuevoAlquiler,
            @RequestParam(value = "dnisTripulantes", required = false) List<String> dnisTripulantes) {
        ModelAndView model = new ModelAndView();

        System.out.println("=== INICIO PROCESAR ALQUILER ===");
        System.out.println("DNI Socio: " + nuevoAlquiler.getDniSocioTitular());
        System.out.println("Matrícula: " + nuevoAlquiler.getMatriculaEmbarcacion());
        System.out.println("Fecha inicio: " + nuevoAlquiler.getFechaInicio());
        System.out.println("Fecha fin: " + nuevoAlquiler.getFechaFin());
        System.out.println("Plazas: " + nuevoAlquiler.getPlazasSolicitadas());

        if (dnisTripulantes != null) {
            List<String> dnisFiltrados = dnisTripulantes.stream()
                    .filter(dni -> dni != null && !dni.trim().isEmpty())
                    .collect(Collectors.toList());
            nuevoAlquiler.setDnisTripulantes(dnisFiltrados);
            System.out.println("Tripulantes añadidos: " + dnisFiltrados.size());
        } else {
            nuevoAlquiler.setDnisTripulantes(new ArrayList<>());
        }

        Socio titular = socioRepository.obtenerSocioPorDni(nuevoAlquiler.getDniSocioTitular());
        if (titular == null) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje",
                    "Socio titular con DNI " + nuevoAlquiler.getDniSocioTitular() + " no encontrado.");
            return model;
        }

        if (!titular.isEsPatron()) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", "El socio titular no tiene título de patrón de embarcación.");
            return model;
        }

        nuevoAlquiler.setIdSocioTitular(titular.getId());

        List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
        embarcacion embarcacionSeleccionada = null;

        if (embarcaciones != null) {
            embarcacionSeleccionada = embarcaciones.stream()
                    .filter(e -> e.getMatricula().equals(nuevoAlquiler.getMatriculaEmbarcacion()))
                    .findFirst()
                    .orElse(null);
        }

        if (embarcacionSeleccionada == null) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", "La embarcación seleccionada no existe.");
            return model;
        }

        LocalDate inicio = nuevoAlquiler.getFechaInicio();
        LocalDate fin = nuevoAlquiler.getFechaFin();
        int plazas = nuevoAlquiler.getPlazasSolicitadas();

        if (inicio.isAfter(fin)) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", "La fecha de inicio no puede ser posterior a la fecha de fin.");
            return model;
        }

        if (inicio.isBefore(LocalDate.now())) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", "La fecha de inicio no puede ser en el pasado.");
            return model;
        }

        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) + 1;
        System.out.println("Días calculados: " + dias + " (desde " + inicio + " hasta " + fin + ")");

        int mes = inicio.getMonthValue();
        boolean fechasValidas = false;
        String mensajeErrorDuracion = "";

        if (mes >= 10 || mes <= 4) {
            fechasValidas = dias <= 3;
            mensajeErrorDuracion = "En temporada baja (octubre-abril) el máximo es 3 días. Seleccionaste: " + dias
                    + " días.";
            System.out.println("Temporada baja: " + dias + " días - Válido: " + fechasValidas);
        } else if (mes >= 5 && mes <= 9) {
            fechasValidas = (dias == 7 || dias == 14);
            mensajeErrorDuracion = "En temporada alta (mayo-septiembre) solo se permiten 7 días (1 semana) o 14 días (2 semanas). Seleccionaste: "
                    + dias + " días.";
            System.out.println("Temporada alta: " + dias + " días - Válido: " + fechasValidas);
        }

        if (!fechasValidas) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", mensajeErrorDuracion);
            return model;
        }

        int totalTripulantes = 1
                + (nuevoAlquiler.getDnisTripulantes() != null ? nuevoAlquiler.getDnisTripulantes().size() : 0);
        if (totalTripulantes > plazas) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje",
                    "El número de tripulantes (" + totalTripulantes + ") excede las plazas solicitadas (" + plazas
                            + ").");
            return model;
        }

        boolean capacidadOk = plazas <= embarcacionSeleccionada.getPlaza();
        if (!capacidadOk) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje",
                    "Plazas solicitadas (" + plazas + ") exceden la capacidad de la embarcación (" +
                            embarcacionSeleccionada.getPlaza() + " plazas).");
            return model;
        }

        System.out.println("Verificando disponibilidad para: " + embarcacionSeleccionada.getMatricula());
        boolean disponibleAlquiler = alquilerRepository.estaDisponible(embarcacionSeleccionada.getMatricula(), inicio,
                fin);
        boolean disponibleReserva = reservaRepository.estaDisponible(embarcacionSeleccionada.getMatricula(), inicio,
                fin);
        boolean disponible = disponibleAlquiler && disponibleReserva;

        System.out.println("Disponibilidad - Alquiler: " + disponibleAlquiler +
                ", Reserva: " + disponibleReserva +
                ", Total: " + disponible);

        if (!disponible) {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje",
                    "La embarcación '" + embarcacionSeleccionada.getNombre() +
                            "' no está disponible en las fechas seleccionadas (" +
                            inicio + " a " + fin
                            + "). Ya está reservada o alquilada. Por favor, seleccione otras fechas.");
            return model;
        }

        double precioTotal = 20.0 * plazas * dias;
        nuevoAlquiler.setPrecioTotal(precioTotal);

        System.out.println("Precio calculado: " + precioTotal + "€ (" + plazas + " plazas × " + dias + " días × 20€)");

        int idAlquiler = alquilerRepository.insertarAlquilerYRetornarId(nuevoAlquiler);

        System.out.println("Inserción exitosa: " + (idAlquiler > 0));
        System.out.println("=== FIN PROCESAR ALQUILER ===");

        if (idAlquiler > 0) {
            model.setViewName("addAlquilerViewSuccess.html");
            model.addObject("mensaje",
                    "Alquiler realizado con éxito para " + embarcacionSeleccionada.getNombre() + ".");
            model.addObject("alquiler", nuevoAlquiler);
            model.addObject("dias", dias);
            model.addObject("precioPorPersonaPorDia", 20.0);
        } else {
            model.setViewName("addAlquilerViewFail.html");
            model.addObject("mensaje", "Error al realizar el alquiler en la base de datos.");
        }

        return model;
    }

    @GetMapping("/listarAlquileresFuturos")
    public ModelAndView mostrarAlquileresFuturos() {
        ModelAndView model = new ModelAndView("listarAlquileresView.html");

        try {
            List<alquiler> alquileres = alquilerRepository.obtenerAlquileresFuturos();
            System.out.println("Alquileres futuros encontrados: " + (alquileres != null ? alquileres.size() : "NULL"));

            model.addObject("alquileres", alquileres != null ? alquileres : new ArrayList<>());
            model.addObject("mensaje",
                    alquileres == null || alquileres.isEmpty() ? "No hay alquileres futuros registrados."
                            : "Se encontraron " + alquileres.size() + " alquileres futuros.");
        } catch (Exception e) {
            System.err.println("Error obteniendo alquileres futuros: " + e.getMessage());
            model.addObject("alquileres", new ArrayList<>());
            model.addObject("mensaje", "Error al cargar los alquileres futuros.");
        }

        return model;
    }

    @GetMapping("/listarTodosAlquileres")
    public ModelAndView mostrarTodosAlquileres() {
        ModelAndView model = new ModelAndView("listarTodosAlquileresView.html");

        try {
            List<alquiler> alquileres = alquilerRepository.obtenerTodosAlquileres();
            System.out.println("Total alquileres encontrados: " + (alquileres != null ? alquileres.size() : "NULL"));

            model.addObject("alquileres", alquileres != null ? alquileres : new ArrayList<>());
            model.addObject("mensaje",
                    alquileres == null || alquileres.isEmpty() ? "No hay alquileres registrados."
                            : "Se encontraron " + alquileres.size() + " alquileres.");
        } catch (Exception e) {
            System.err.println("Error obteniendo todos los alquileres: " + e.getMessage());
            model.addObject("alquileres", new ArrayList<>());
            model.addObject("mensaje", "Error al cargar los alquileres.");
        }

        return model;
    }

    @GetMapping("/buscarAlquileresPorSocio")
    public ModelAndView mostrarBusquedaAlquileresPorSocio() {
        ModelAndView model = new ModelAndView("buscarAlquileresPorSocio.html");
        return model;
    }

    @PostMapping("/buscarAlquileresPorSocio")
    public ModelAndView buscarAlquileresPorSocio(@RequestParam String dniSocio) {
        ModelAndView model = new ModelAndView("buscarAlquileresPorSocio.html");

        try {
            List<alquiler> alquileres = alquilerRepository.obtenerAlquileresPorSocio(dniSocio);

            model.addObject("alquileres", alquileres != null ? alquileres : new ArrayList<>());
            model.addObject("dniSocio", dniSocio);
            model.addObject("mensaje",
                    alquileres == null || alquileres.isEmpty() ? "No se encontraron alquileres para el DNI: " + dniSocio
                            : "Se encontraron " + alquileres.size() + " alquileres para el DNI: " + dniSocio);
        } catch (Exception e) {
            System.err.println("Error obteniendo alquileres por socio: " + e.getMessage());
            model.addObject("alquileres", new ArrayList<>());
            model.addObject("mensaje", "Error al buscar alquileres para el DNI: " + dniSocio);
        }

        return model;
    }

    @GetMapping("/buscarAlquileresPorEmbarcacion")
    public ModelAndView mostrarBusquedaAlquileresPorEmbarcacion() {
        ModelAndView model = new ModelAndView("buscarAlquileresPorEmbarcacion.html");

        try {
            List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();
            model.addObject("embarcaciones", embarcaciones != null ? embarcaciones : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error cargando embarcaciones: " + e.getMessage());
            model.addObject("embarcaciones", new ArrayList<>());
        }

        return model;
    }

    @PostMapping("/buscarAlquileresPorEmbarcacion")
    public ModelAndView buscarAlquileresPorEmbarcacion(@RequestParam String matricula) {
        ModelAndView model = new ModelAndView("buscarAlquileresPorEmbarcacion.html");

        try {
            List<alquiler> alquileres = alquilerRepository.obtenerAlquileresPorEmbarcacion(matricula);
            List<embarcacion> embarcaciones = embarcacionRepository.obtenerEmbarcaciones();

            model.addObject("alquileres", alquileres != null ? alquileres : new ArrayList<>());
            model.addObject("embarcaciones", embarcaciones != null ? embarcaciones : new ArrayList<>());
            model.addObject("matricula", matricula);
            model.addObject("mensaje",
                    alquileres == null || alquileres.isEmpty()
                            ? "No se encontraron alquileres para la matrícula: " + matricula
                            : "Se encontraron " + alquileres.size() + " alquileres para la matrícula: " + matricula);
        } catch (Exception e) {
            System.err.println("Error obteniendo alquileres por embarcación: " + e.getMessage());
            model.addObject("alquileres", new ArrayList<>());
            model.addObject("mensaje", "Error al buscar alquileres para la matrícula: " + matricula);
        }

        return model;
    }
}