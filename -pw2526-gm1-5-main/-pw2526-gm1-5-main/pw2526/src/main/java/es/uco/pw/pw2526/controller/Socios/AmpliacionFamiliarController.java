package es.uco.pw.pw2526.controller.socios;

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
    public ModelAndView getBuscarTitularView() {
        ModelAndView modelAndView = new ModelAndView("listarTitularView.html");
        // CAMBIO CLAVE: Obtener solo socios titulares.
        List<Socio> socios = socioRepository.obtenerSociosTitulares();
        modelAndView.addObject("socios", socios);
        return modelAndView;
    }

    @PostMapping("/ampliarInscripcion/buscar")
    public ModelAndView buscarTitularParaAmpliacion(@RequestParam int titularId) {
        Socio titular = socioRepository.obtenerSocioPorId(titularId);
        ModelAndView modelAndView = new ModelAndView();

        // La validación en el controlador sigue siendo necesaria por si el ID se manipula, 
        // pero la lista ya solo mostrará titulares.
        if (titular == null || titular.getTipoMiembro() != TipoMiembro.TITULAR) {
            modelAndView.setViewName("ampliacionFamiliarViewFail.html");
            modelAndView.addObject("error", "Socio no encontrado o no es titular de una inscripción.");
            return modelAndView;
        }

        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(titular.getInscripcionId());

        if (inscripcion == null) {
            modelAndView.setViewName("ampliacionFamiliarViewFail.html");
            modelAndView.addObject("error", "No se encontró una inscripción válida para el titular.");
            return modelAndView;
        }

        modelAndView.setViewName("ampliacionFamiliarView.html");
        modelAndView.addObject("titular", titular);
        modelAndView.addObject("inscripcion", inscripcion);
        modelAndView.addObject("nuevoAdulto", new Socio()); 

        return modelAndView;
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

        ModelAndView modelAndView = new ModelAndView("ampliacionFamiliarViewSuccess.html");
        Inscripcion inscripcion = inscripcionRepository.obtenerInscripcionPorId(inscripcionId);
        Socio titular = socioRepository.obtenerSocioPorId(titularId);

        if (inscripcion == null || titular == null) {
            modelAndView.setViewName("ampliacionFamiliarViewFail.html");
            modelAndView.addObject("error", "Error: No se encontró la inscripción o el titular.");
            return modelAndView;
        }

        double cuotaAnterior = inscripcion.getCuotaAnual();
        double nuevaCuota = cuotaAnterior;
        int adultosAnadidos = 0;
        int totalHijosAnadidos = 0;
        
        // --- 1. PROCESAMIENTO DEL CÓNYUGE (Adulto adicional)
        boolean existeConyugeData = dniNuevoAdulto != null && !dniNuevoAdulto.trim().isEmpty();

        if (existeConyugeData) {
            if (socioRepository.existeSocioPorDni(dniNuevoAdulto)) {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error: El DNI del cónyuge ya está registrado en el club.");
                return modelAndView;
            }
            if (nuevoAdulto.getFechaNacimiento() == null || !nuevoAdulto.esMayorDeEdad()) {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error: El cónyuge debe ser mayor de edad y se requiere la fecha de nacimiento.");
                return modelAndView;
            }

            // Inserción del cónyuge
            nuevoAdulto.setDni(dniNuevoAdulto); 
            nuevoAdulto.setIdSocioTitularFk(titularId);
            nuevoAdulto.setInscripcionId(inscripcionId);
            nuevoAdulto.setFechaInscripcion(LocalDate.now());
            nuevoAdulto.setTipoMiembro(TipoMiembro.CONYUGE); 
            nuevoAdulto.setTieneTituloPatron(false); 
            nuevoAdulto.setDireccion(titular.getDireccion()); 

            if (socioRepository.addSocioAndReturnId(nuevoAdulto, inscripcionId) > 0) {
                adultosAnadidos = 1;
                if (inscripcion.getTipoInscripcion() == TipoInscripcion.INDIVIDUAL) {
                   nuevaCuota += CUOTA_SEGUNDO_ADULTO;
                }
            } else {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error CRÍTICO al guardar los datos del cónyuge en la BD.");
                return modelAndView;
            }
        }

        // 2. PROCESAMIENTO DE HIJOS 
        
        String dniHijo = (hijosDniList != null && !hijosDniList.isEmpty()) ? hijosDniList.get(0).trim() : "";
        
        if (!dniHijo.isEmpty()) {
            
            if (socioRepository.existeSocioPorDni(dniHijo)) {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error: El DNI del hijo ya está registrado en el club.");
                return modelAndView;
            }
            
            if (hijosNombreList == null || hijosNombreList.isEmpty() || hijosNombreList.get(0).trim().isEmpty() ||
                hijosApellidosList == null || hijosApellidosList.isEmpty() || hijosApellidosList.get(0).trim().isEmpty() ||
                hijosFechaNacimientoList == null || hijosFechaNacimientoList.isEmpty() || hijosFechaNacimientoList.get(0).trim().isEmpty()) {
                 modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                 modelAndView.addObject("error", "Error: Faltan datos obligatorios (Nombre/Apellidos/Fecha de Nacimiento) para el hijo.");
                 return modelAndView;
            }

            Socio hijo = new Socio();
            hijo.setDni(dniHijo);
            hijo.setNombre(hijosNombreList.get(0).trim());
            hijo.setApellidos(hijosApellidosList.get(0).trim());
            
            try {
                hijo.setFechaNacimiento(LocalDate.parse(hijosFechaNacimientoList.get(0)));
            } catch (Exception e) {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error: La Fecha de Nacimiento del hijo no es válida.");
                return modelAndView;
            }
            
            hijo.setIdSocioTitularFk(titularId);
            hijo.setInscripcionId(inscripcionId);
            hijo.setFechaInscripcion(LocalDate.now());
            hijo.setTipoMiembro(TipoMiembro.HIJO); 
            hijo.setTieneTituloPatron(false); 
            hijo.setDireccion(titular.getDireccion());

            if (socioRepository.addSocioAndReturnId(hijo, inscripcionId) > 0) {
                totalHijosAnadidos = 1;
                nuevaCuota += CUOTA_HIJO;
            } else {
                modelAndView.setViewName("ampliacionFamiliarViewFail.html");
                modelAndView.addObject("error", "Error al guardar los datos del hijo en la BD.");
                return modelAndView;
            }
        }

        // 3. ACTUALIZACIÓN FINAL DE LA INSCRIPCIÓN

        if (adultosAnadidos > 0 || totalHijosAnadidos > 0) {
            if (inscripcion.getTipoInscripcion() == TipoInscripcion.INDIVIDUAL) {
                inscripcion.setTipoInscripcion(TipoInscripcion.FAMILIAR);
            }
            
            inscripcion.setCuotaAnual(nuevaCuota);
        } else {
            modelAndView.setViewName("ampliacionFamiliarViewFail.html");
            modelAndView.addObject("error", "No se añadió ningún miembro para justificar la ampliación a familiar.");
            return modelAndView;
        }

        boolean success = inscripcionRepository.actualizarInscripcion(inscripcion);

        if (success) {
            int totalMiembrosNuevos = adultosAnadidos + totalHijosAnadidos;

            modelAndView.addObject("titularNombre", titular.getNombre() + " " + titular.getApellidos());
            modelAndView.addObject("cuotaAnterior", cuotaAnterior);
            modelAndView.addObject("cuotaNueva", nuevaCuota);
            modelAndView.addObject("miembrosAnadidos", totalMiembrosNuevos);
            modelAndView.addObject("mensaje",
                    "La inscripción familiar se ha completado. Miembros añadidos: " + totalMiembrosNuevos);
        } else {
            modelAndView.setViewName("ampliacionFamiliarViewFail.html");
            modelAndView.addObject("error", "Error al actualizar el tipo de inscripción y la cuota en la BD.");
        }

        return modelAndView;
    }
}