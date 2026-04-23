package es.uco.pw.pw2526.client;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.util.List;
import java.util.Map;

public class TestInscripciones {

    private static final String BASE_URL = "http://localhost:8080";
    private static final RestTemplate restTemplate;


    private static final String DNI_TITULAR_I = "99999999I";
    private static final String DNI_MIEMBRO_VINC = "88888888M";
    private static int ID_TITULAR_I = -1;
    private static int ID_MIEMBRO_VINC = -1;
    private static Integer ID_INSCRIPCION = -1;
    
    static {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.createDefault());
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate = new RestTemplate(requestFactory);
    }

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE INSCRIPCIONES (/api/inscripciones) ===\n");


        setupSocios();


        testPostInscripcion();


        testGetInscripciones();


        if (ID_INSCRIPCION > 0) {
            testPutConvertirFamiliar();
            testPatchVincularDesvincular();
        }


        testDeleteInscripcion();


        limpiezaFinal();
        
        System.out.println("\n=== FIN DE PRUEBAS DE INSCRIPCIONES ===\n");
    }



    private static void setupSocios() {
        System.out.println("--- 1. SETUP: Creando Socios Requeridos ---");
        

        try {
            String titularJson = String.format("""
                    { "dni": "%s", "nombre": "Inscripcion", "apellidos": "Titular", "fechaNacimiento": "1975-01-01", "direccion": "C/ I-Titular" }
                    """, DNI_TITULAR_I);

            Map<String, Object> response = restTemplate.postForObject(BASE_URL + "/api/socios", new HttpEntity<>(titularJson, getJsonHeaders()), Map.class);
            
            System.out.println("Socio Titular (" + DNI_TITULAR_I + ") creado.");
            

            ID_TITULAR_I = (Integer) restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_TITULAR_I, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody().get("id");
            
        } catch (HttpClientErrorException.Conflict e) {
             ID_TITULAR_I = buscarIdSocio(DNI_TITULAR_I);
             System.out.println("Socio Titular ya existe (ID: " + ID_TITULAR_I + ").");
        } catch (Exception e) {
            System.out.println("ERROR SETUP Titular: " + e.getMessage());
        }


        try {
            String miembroJson = String.format("""
                    { "dni": "%s", "nombre": "Inscripcion", "apellidos": "Miembro", "fechaNacimiento": "1990-01-01", "direccion": "C/ I-Miembro" }
                    """, DNI_MIEMBRO_VINC);
            restTemplate.postForEntity(BASE_URL + "/api/socios", new HttpEntity<>(miembroJson, getJsonHeaders()), String.class);
            System.out.println("Socio Miembro (" + DNI_MIEMBRO_VINC + ") creado.");
            ID_MIEMBRO_VINC = (Integer) restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_MIEMBRO_VINC, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody().get("id");

        } catch (HttpClientErrorException.Conflict e) {
             ID_MIEMBRO_VINC = buscarIdSocio(DNI_MIEMBRO_VINC);
             System.out.println("Socio Miembro ya existe (ID: " + ID_MIEMBRO_VINC + ").");
        } catch (Exception e) {
            System.out.println("ERROR SETUP Miembro: " + e.getMessage());
        }
    }



    private static void testPostInscripcion() {
        if (ID_TITULAR_I < 0) return;
        System.out.println("\n--- 2. POST /api/inscripciones (Creación Inicial) ---");


        try {
            String inscripcionJson = String.format("""
                    { "idSocioTitular": %d, "tipoInscripcion": "INDIVIDUAL", "cuotaAnual": 300.0 }
                    """, ID_TITULAR_I);

            ResponseEntity<Map> response = restTemplate.postForEntity(BASE_URL + "/api/inscripciones", 
                new HttpEntity<>(inscripcionJson, getJsonHeaders()), Map.class);
            
            ID_INSCRIPCION = (Integer) response.getBody().get("id");
            System.out.println("Status: " + response.getStatusCode() + " (Inscripción ID: " + ID_INSCRIPCION + " creada).");
        } catch (Exception e) {
            System.out.println("Error POST Inscripción: " + e.getMessage());
        }
    }



    private static void testGetInscripciones() {
        System.out.println("\n--- 3. GET /api/inscripciones (Pruebas de Lectura) ---");


        try {
            ResponseEntity<List<Map<String, Object>>> inscripciones = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones?tipo=individual", HttpMethod.GET, null, 
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            System.out.println("GET ?tipo=individual: Status " + inscripciones.getStatusCode() + ". Total: " + inscripciones.getBody().size());
        } catch (Exception e) {
            System.out.println("Error GET ?tipo=individual: " + e.getMessage());
        }
        

        try {
            ResponseEntity<List<Map<String, Object>>> inscripciones = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones", HttpMethod.GET, null, 
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            System.out.println("GET /api/inscripciones: Status " + inscripciones.getStatusCode() + ". Total: " + inscripciones.getBody().size());
        } catch (Exception e) {
            System.out.println("Error GET /api/inscripciones: " + e.getMessage());
        }



        try {
            ResponseEntity<Map<String, Object>> inscripcion = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones/" + DNI_TITULAR_I, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            System.out.println("GET /api/inscripciones/{dniTitular}: Status " + inscripcion.getStatusCode() + " (Inscripción ID: " + inscripcion.getBody().get("id") + ").");
        } catch (Exception e) {
            System.out.println("Error GET /api/inscripciones/{dniTitular}: " + e.getMessage());
        }
    }



    private static void testPutConvertirFamiliar() {
        if (ID_TITULAR_I < 0) return;
        System.out.println("\n--- 4. PUT /api/inscripciones/{dniTitular}/tipo (Convertir a Familiar) ---");
        

        String putJson = """
                { "tipoInscripcion": "FAMILIAR", "cuotaAnual": 550.0 }
                """;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones/" + DNI_TITULAR_I + "/tipo", HttpMethod.PUT, 
                    new HttpEntity<>(putJson, getJsonHeaders()), String.class);
            
            System.out.println("PUT /tipo: Status " + response.getStatusCode() + " (Convertido a Familiar).");
        } catch (Exception e) {
            System.out.println("Error PUT /tipo: " + e.getMessage());
        }
    }

    private static void testPatchVincularDesvincular() {
        if (ID_TITULAR_I < 0 || ID_MIEMBRO_VINC < 0) return;


        System.out.println("\n--- 5. PATCH Vincular y Desvincular Miembros ---");
        String patchVincularJson = String.format("""
                { "dni": "%s", "tipoMiembro": "CONYUGE" }
                """, DNI_MIEMBRO_VINC);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones/" + DNI_TITULAR_I + "/miembros", HttpMethod.PATCH, 
                    new HttpEntity<>(patchVincularJson, getJsonHeaders()), String.class);
            
            System.out.println("PATCH Vincular Miembro: Status " + response.getStatusCode() + " (Vínculo exitoso).");
        } catch (Exception e) {
            System.out.println("Error PATCH Vincular: " + e.getMessage());
            return; // Si falla la vinculación, no podemos desvincular
        }


        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    BASE_URL + "/api/inscripciones/" + DNI_TITULAR_I + "/miembros/" + DNI_MIEMBRO_VINC, HttpMethod.PATCH, 
                    null, Void.class);
            
            System.out.println("PATCH Desvincular Miembro: Status " + response.getStatusCode() + " (Desvínculo exitoso).");
        } catch (Exception e) {
            System.out.println("Error PATCH Desvincular: " + e.getMessage());
        }
    }



    private static void testDeleteInscripcion() {
        System.out.println("\n--- 6. DELETE /api/inscripciones/{dniTitular} (Cancelar) ---");


        try {
            restTemplate.exchange(BASE_URL + "/api/inscripciones/" + DNI_TITULAR_I, HttpMethod.DELETE, null, Void.class);
            System.out.println("DELETE /api/inscripciones/{dniTitular}: Status 204 No Content (Inscripción cancelada).");
        } catch (Exception e) {
            System.out.println("Error DELETE Inscripción: " + e.getMessage());
        }
    }
    

    
    private static void limpiezaFinal() {
        System.out.println("\n--- 7. LIMPIEZA: Eliminando Socios de Prueba ---");
        

        try {
            restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_TITULAR_I, HttpMethod.DELETE, null, Void.class);
            System.out.println("Socio Titular (" + DNI_TITULAR_I + ") eliminado.");
        } catch (Exception e) {
            System.out.println("Error al eliminar Socio Titular: " + e.getMessage());
        }
        

        try {
            restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_MIEMBRO_VINC, HttpMethod.DELETE, null, Void.class);
            System.out.println("Socio Miembro (" + DNI_MIEMBRO_VINC + ") eliminado.");
        } catch (Exception e) {
            System.out.println("Error al eliminar Socio Miembro: " + e.getMessage());
        }
    }
    
    private static int buscarIdSocio(String dni) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    BASE_URL + "/api/socios/" + dni, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
            return (Integer) response.getBody().get("id");
        } catch (Exception e) {
            return -1;
        }
    }

    private static HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}