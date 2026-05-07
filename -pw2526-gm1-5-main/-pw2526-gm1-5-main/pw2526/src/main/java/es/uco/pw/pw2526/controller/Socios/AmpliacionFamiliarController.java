package es.uco.pw.pw2526.controller.Socios;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import es.uco.pw.pw2526.model.Repository.InscripcionRepository;
import es.uco.pw.pw2526.model.Repository.SocioRepository;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion.TipoInscripcion;
import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.Socio.Socio.TipoMiembro; 

@Controller
public class AmpliacionFamiliarController {

    private final SocioRepository socioRepository;
    private final InscripcionRepository inscripcionRepository;

    public static final double CUOTA_ADULTO_TITULAR = 300.0;
    public static final double CUOTA_SEGUNDO_ADULTO = 250.0; 
    public static final double CUOTA_HIJO = 100.0;

    public AmpliacionFamiliarController(SocioRepository socioRepository, InscripcionRepository inscripcionRepository) {
        this.socioRepository = socioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @GetMapping("/ampliarInscripcion")
    public ModelAndView mostrarSeleccionTitular() {
        ModelAndView modelAndView = new ModelAndView("listarTitularView.html");
        List<Socio> socios = socioRepository.obtenerSociosTitulares();
        modelAndView.addObject("socios", socios);
        return modelAndView;
    }

    @PostMapping("/ampliarInscripcion/buscar")
    public ModelAndView buscarTitularParaAmpliacion(@RequestParam int titularId) {
        Socio titular = socioRepository.obtenerSocioPorId(titularId);

        if (titular == null || titular.getTipoMiembro() != TipoMiembro.TITULAR) {
            return construirVistaFallo("Socio no encontrado o no es titular de una inscripción.");
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        if (inscripcion == null) {
            return construirVistaFallo("No se encontró una inscripción válida para el titular.");
        }

        return construirVistaFormularioAmpliacion(titular, inscripcion);
    }

    @PostMapping("/ampliarInscripcion/procesar")
    public ModelAndView procesarAmpliacion(
            @RequestParam int inscripcionId,
            @RequestParam int titularId,
            @RequestParam(required = false) String dniNuevoAdulto, 
            @ModelAttribute Socio nuevoAdulto,
            @RequestParam(name = "hijos[0].dni", required = false) List<String> hijosDniList,
            @RequestParam(name = "hijos[0].nombre", required = false) List<String> hijosNombreList,
            @RequestParam(name = "hijos[0].apellidos", required = false) List<String> hijosApellidosList,
            @RequestParam(name = "hijos[0].fechaNacimiento", required = false) List<String> hijosFechaNacimientoList) {

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(inscripcionId);
        Socio titular = socioRepository.obtenerSocioPorId(titularId);

        if (inscripcion == null || titular == null) {
            return construirVistaFallo("Error: No se encontró la inscripción o el titular.");
        }

        double cuotaAnterior = inscripcion.getCuotaAnual();
        double nuevaCuota = cuotaAnterior;
        int adultosAnadidos = 0;
        int totalHijosAnadidos = 0;

        ModelAndView errorConyuge = validarYRegistrarConyuge(dniNuevoAdulto, nuevoAdulto, titular, inscripcion);
        if (errorConyuge != null) {
            return errorConyuge;
        }

        if (existenDatosConyuge(dniNuevoAdulto)) {
            adultosAnadidos = 1;
            /*
             * Refactorización: Use existing domain method (Feature Envy).
             * Inscripcion ya tiene esIndividual(), no hace falta comparar
             * directamente con el enum desde el controlador.
             */
            if (inscripcion.esIndividual()) {
                nuevaCuota += CUOTA_SEGUNDO_ADULTO;
            }
        }

        ModelAndView errorHijo = validarYRegistrarHijo(
                hijosDniList, hijosNombreList, hijosApellidosList, hijosFechaNacimientoList,
                titular, inscripcionId);
        if (errorHijo != null) {
            return errorHijo;
        }

        if (existenDatosHijo(hijosDniList)) {
            totalHijosAnadidos = 1;
            nuevaCuota += CUOTA_HIJO;
        }

        if (adultosAnadidos == 0 && totalHijosAnadidos == 0) {
            return construirVistaFallo("No se añadió ningún miembro para justificar la ampliación a familiar.");
        }

        return finalizarAmpliacion(inscripcion, titular, cuotaAnterior, nuevaCuota,
                adultosAnadidos, totalHijosAnadidos);
    }


    private ModelAndView validarYRegistrarConyuge(String dniNuevoAdulto, Socio nuevoAdulto,
            Socio titular, Inscripcion inscripcion) {
        if (!existenDatosConyuge(dniNuevoAdulto)) {
            return null;
        }

        if (socioRepository.existeSocioPorDni(dniNuevoAdulto)) {
            return construirVistaFallo("Error: El DNI del cónyuge ya está registrado en el club.");
        }

        if (nuevoAdulto.getFechaNacimiento() == null || !nuevoAdulto.esMayorDeEdad()) {
            return construirVistaFallo(
                    "Error: El cónyuge debe ser mayor de edad y se requiere la fecha de nacimiento.");
        }

        configurarDatosConyuge(nuevoAdulto, dniNuevoAdulto, titular, inscripcion);

        if (socioRepository.insertarSocioYRetornarId(nuevoAdulto, inscripcion.getId()) <= 0) {
            return construirVistaFallo("Error CRÍTICO al guardar los datos del cónyuge en la BD.");
        }

        return null;
    }

    private void configurarDatosConyuge(Socio nuevoAdulto, String dni, Socio titular, Inscripcion inscripcion) {
        nuevoAdulto.setDni(dni);
        nuevoAdulto.setIdSocioTitularFk(titular.getId());
        nuevoAdulto.setInscripcionId(inscripcion.getId());
        nuevoAdulto.setFechaInscripcion(LocalDate.now());
        nuevoAdulto.setTipoMiembro(TipoMiembro.CONYUGE);
        nuevoAdulto.setTieneTituloPatron(false);
        nuevoAdulto.setDireccion(titular.getDireccion());
    }


    private ModelAndView validarYRegistrarHijo(List<String> hijosDniList, List<String> hijosNombreList,
            List<String> hijosApellidosList, List<String> hijosFechaNacimientoList,
            Socio titular, int inscripcionId) {
        String dniHijo = extraerPrimerElemento(hijosDniList);
        if (dniHijo.isEmpty()) {
            return null;
        }

        if (socioRepository.existeSocioPorDni(dniHijo)) {
            return construirVistaFallo("Error: El DNI del hijo ya está registrado en el club.");
        }

        if (!datosHijoCompletos(hijosNombreList, hijosApellidosList, hijosFechaNacimientoList)) {
            return construirVistaFallo(
                    "Error: Faltan datos obligatorios (Nombre/Apellidos/Fecha de Nacimiento) para el hijo.");
        }

        Socio hijo = construirSocioHijo(dniHijo, hijosNombreList, hijosApellidosList,
                hijosFechaNacimientoList, titular, inscripcionId);
        if (hijo == null) {
            return construirVistaFallo("Error: La Fecha de Nacimiento del hijo no es válida.");
        }

        if (socioRepository.insertarSocioYRetornarId(hijo, inscripcionId) <= 0) {
            return construirVistaFallo("Error al guardar los datos del hijo en la BD.");
        }

        return null;
    }

    private Socio construirSocioHijo(String dniHijo, List<String> hijosNombreList,
            List<String> hijosApellidosList, List<String> hijosFechaNacimientoList,
            Socio titular, int inscripcionId) {
        Socio hijo = new Socio();
        hijo.setDni(dniHijo);
        hijo.setNombre(hijosNombreList.get(0).trim());
        hijo.setApellidos(hijosApellidosList.get(0).trim());

        try {
            hijo.setFechaNacimiento(LocalDate.parse(hijosFechaNacimientoList.get(0)));
        } catch (Exception excepcion) {
            return null;
        }

        hijo.setIdSocioTitularFk(titular.getId());
        hijo.setInscripcionId(inscripcionId);
        hijo.setFechaInscripcion(LocalDate.now());
        hijo.setTipoMiembro(TipoMiembro.HIJO);
        hijo.setTieneTituloPatron(false);
        hijo.setDireccion(titular.getDireccion());
        return hijo;
    }


    private ModelAndView finalizarAmpliacion(Inscripcion inscripcion, Socio titular,
            double cuotaAnterior, double nuevaCuota, int adultosAnadidos, int hijosAnadidos) {
        actualizarTipoInscripcion(inscripcion);
        inscripcion.setCuotaAnual(nuevaCuota);

        if (!inscripcionRepository.actualizarInscripcion(inscripcion)) {
            return construirVistaFallo("Error al actualizar el tipo de inscripción y la cuota en la BD.");
        }

        return construirVistaExitoAmpliacion(titular, cuotaAnterior, nuevaCuota,
                adultosAnadidos + hijosAnadidos);
    }

    private void actualizarTipoInscripcion(Inscripcion inscripcion) {
        if (inscripcion.esIndividual()) {
            inscripcion.setTipoInscripcion(TipoInscripcion.FAMILIAR);
        }
    }


    private boolean existenDatosConyuge(String dniNuevoAdulto) {
        return dniNuevoAdulto != null && !dniNuevoAdulto.trim().isEmpty();
    }

    private boolean existenDatosHijo(List<String> hijosDniList) {
        return !extraerPrimerElemento(hijosDniList).isEmpty();
    }

    private String extraerPrimerElemento(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return "";
        }
        return lista.get(0).trim();
    }

    private boolean datosHijoCompletos(List<String> nombres, List<String> apellidos, List<String> fechas) {
        return tieneContenido(nombres) && tieneContenido(apellidos) && tieneContenido(fechas);
    }

    private boolean tieneContenido(List<String> lista) {
        return lista != null && !lista.isEmpty() && !lista.get(0).trim().isEmpty();
    }


    private ModelAndView construirVistaFallo(String mensajeError) {
        ModelAndView modelAndView = new ModelAndView("ampliacionFamiliarViewFail.html");
        modelAndView.addObject("error", mensajeError);
        return modelAndView;
    }

    private ModelAndView construirVistaFormularioAmpliacion(Socio titular, Inscripcion inscripcion) {
        ModelAndView modelAndView = new ModelAndView("ampliacionFamiliarView.html");
        modelAndView.addObject("titular", titular);
        modelAndView.addObject("inscripcion", inscripcion);
        modelAndView.addObject("nuevoAdulto", new Socio());
        return modelAndView;
    }

    private ModelAndView construirVistaExitoAmpliacion(Socio titular, double cuotaAnterior,
            double nuevaCuota, int totalMiembrosNuevos) {
        ModelAndView modelAndView = new ModelAndView("ampliacionFamiliarViewSuccess.html");
        modelAndView.addObject("titularNombre", titular.getNombre() + " " + titular.getApellidos());
        modelAndView.addObject("cuotaAnterior", cuotaAnterior);
        modelAndView.addObject("cuotaNueva", nuevaCuota);
        modelAndView.addObject("miembrosAnadidos", totalMiembrosNuevos);
        modelAndView.addObject("mensaje",
                "La inscripción familiar se ha completado. Miembros añadidos: " + totalMiembrosNuevos);
        return modelAndView;
    }
}