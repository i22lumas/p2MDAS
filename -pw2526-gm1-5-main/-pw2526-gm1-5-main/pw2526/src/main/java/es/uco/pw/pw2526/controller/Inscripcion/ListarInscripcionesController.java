package es.uco.pw.pw2526.controller.Inscripcion;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import es.uco.pw.pw2526.model.Repository.InscripcionRepository;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/listarInscripciones")
public class ListarInscripcionesController {

    private final InscripcionRepository inscripcionRepository;

    public ListarInscripcionesController(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @GetMapping
    public String listarInscripciones(Model model) {
        List<Map<String, Object>> detalles = inscripcionRepository.obtenerDetallesInscripciones();

        model.addAttribute("detallesInscripcion", detalles);

        return "ListarInscripcionesView";
    }
}