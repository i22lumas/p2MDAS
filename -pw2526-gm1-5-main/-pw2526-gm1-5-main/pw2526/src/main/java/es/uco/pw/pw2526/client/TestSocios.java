package es.uco.pw.pw2526.client;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.util.List;
import java.util.Map;

public class TestSocios {

    private static final String BASE_URL = "http://localhost:8080";
    private static final RestTemplate restTemplate;

    private static final String DNI_TITULAR_NUEVO = "55555555N";
    private static final String DNI_TITULAR_VINCULADO = "12345678A"; // DNI de un socio ya existente

    static {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.createDefault());
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate = new RestTemplate(requestFactory);
    }

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE SOCIOS (/api/socios) ===\n");

        testPostSocioExitoso();
        testGetSocios();
        testPatchSocio(DNI_TITULAR_NUEVO);
        testPostSocioErrores();
        testDeleteSocios();

        System.out.println("\n=== FIN DE PRUEBAS DE SOCIOS ===\n");
    }

    // ==================== POST ====================

    private static void testPostSocioExitoso() {
        System.out.println("--- 1. POST /api/socios (Creación Exitosa) ---");
        String socioJson = String.format("""
                { "dni": "%s", "nombre": "Socio", "apellidos": "Nuevo", "fechaNacimiento": "1980-01-01", "direccion": "Calle Testeable" }
                """, DNI_TITULAR_NUEVO);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/api/socios",
                new HttpEntity<>(socioJson, getJsonHeaders()), String.class);
            System.out.println("Status: " + response.getStatusCode() + " (Socio creado: " + DNI_TITULAR_NUEVO + ")");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("Socio ya existía. Continuando con pruebas.");
        } catch (Exception e) {
            System.out.println("Error en POST: " + e.getMessage());
        }
    }

    private static void testPostSocioErrores() {
        System.out.println("\n--- 4. POST /api/socios (Pruebas de Errores) ---");
        testPostSocioDniDuplicado();
    }

    private static void testPostSocioDniDuplicado() {
        System.out.println(">> DNI Duplicado");
        String duplicateSocioJson = String.format("""
                { "dni": "%s", "nombre": "Duplicado", "apellidos": "Error", "fechaNacimiento": "1990-01-01", "direccion": "C/ Error" }
                """, DNI_TITULAR_NUEVO);
        try {
            restTemplate.postForEntity(BASE_URL + "/api/socios", new HttpEntity<>(duplicateSocioJson, getJsonHeaders()), String.class);
            System.out.println("ERROR: Debería fallar con 409 Conflict.");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("CORRECTO: Devuelve 409 Conflict.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== GET ====================

    private static void testGetSocios() {
        System.out.println("\n--- 2. GET /api/socios (Pruebas de Lectura) ---");
        testGetTodosSocios();
        testGetSocioPorDni();
        testGetSocioInexistente();
    }

    private static void testGetTodosSocios() {
        try {
            ResponseEntity<List<Map<String, Object>>> socios = restTemplate.exchange(
                    BASE_URL + "/api/socios", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            System.out.println("GET /api/socios: Status " + socios.getStatusCode() + ". Total socios: " + socios.getBody().size());
        } catch (Exception e) {
            System.out.println("Error GET /api/socios: " + e.getMessage());
        }
    }

    private static void testGetSocioPorDni() {
        try {
            ResponseEntity<Map<String, Object>> socio = restTemplate.exchange(
                    BASE_URL + "/api/socios/" + DNI_TITULAR_NUEVO, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            System.out.println("GET /api/socios/{dni}: Status " + socio.getStatusCode() + " (Socio encontrado).");
        } catch (Exception e) {
            System.out.println("Error GET /api/socios/{dni}: " + e.getMessage());
        }
    }

    private static void testGetSocioInexistente() {
        try {
            restTemplate.getForEntity(BASE_URL + "/api/socios/00000000Z", String.class);
            System.out.println("ERROR: Debería fallar con 404 Not Found.");
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("CORRECTO: Devuelve 404 Not Found (Socio inexistente).");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== PATCH ====================

    private static void testPatchSocio(String dni) {
        System.out.println("\n--- 3. PATCH /api/socios/{dni} (Actualización) ---");

        String patchJson = """
                { "nombre": "Socio Parcheado", "direccion": "Direccion Modificada", "tieneTituloPatron": true }
                """;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/api/socios/" + dni, HttpMethod.PATCH,
                    new HttpEntity<>(patchJson, getJsonHeaders()), String.class);

            System.out.println("PATCH /api/socios/{dni}: Status " + response.getStatusCode() + " (Actualización exitosa).");
        } catch (Exception e) {
            System.out.println("Error PATCH: " + e.getMessage());
        }
    }

    // ==================== DELETE ====================

    private static void testDeleteSocios() {
        System.out.println("\n--- 5. DELETE /api/socios/{dni} (Eliminación) ---");
        testDeleteSocioVinculado();
        testDeleteSocioNoVinculado();
    }

    private static void testDeleteSocioVinculado() {
        System.out.println(">> DELETE Socio Vinculado");
        try {
            restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_TITULAR_VINCULADO, HttpMethod.DELETE, null, Void.class);
            System.out.println("ERROR: Debería fallar con 409 Conflict.");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("CORRECTO: Devuelve 409 Conflict (Socio vinculado).");
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("Advertencia: El socio vinculado de prueba no existe. No se puede probar el 409.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void testDeleteSocioNoVinculado() {
        System.out.println(">> DELETE Socio No Vinculado");
        try {
            restTemplate.exchange(BASE_URL + "/api/socios/" + DNI_TITULAR_NUEVO, HttpMethod.DELETE, null, Void.class);
            System.out.println("DELETE /api/socios/{dni}: Status 204 No Content (Socio eliminado).");
        } catch (Exception e) {
            System.out.println("Error DELETE: " + e.getMessage());
        }
    }

    // ==================== Utilidades ====================

    private static HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}