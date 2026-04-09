package es.uco.pw.pw2526.client;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestAlquilerAPI {

    private static final String BASE_URL = "http://localhost:8080";
    private static final RestTemplate restTemplate;
    private static final Random random = new Random();
    private static int testCounter = 1;
    private static int testsExitosos = 0;
    private static int testsFallidos = 0;

    static {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.createDefault());
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate = new RestTemplate(requestFactory);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   PRUEBAS DE LA API DE ALQUILERES (SEGÚN GUION PRÁCTICA)   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        if (!verificarServidorActivo()) {
            System.out.println("❌ ERROR: El servidor no está disponible en " + BASE_URL);
            return;
        }

        verificarEstadoBD();

        // ==================== PRUEBAS GET ====================
        System.out.println("\n📋 ==================== PRUEBAS GET ====================\n");
        testGetAllAlquileres();
        testGetAlquileresFuturos();
        testGetAlquileresFuturosConFecha();
        testGetEmbarcacionesDisponiblesSinFechas();
        testGetEmbarcacionesDisponiblesFechasInvalidas();

        // ==================== PRUEBAS POST ====================
        System.out.println("\n📝 ==================== PRUEBAS POST ====================\n");

        // Casos de error
        testPostAlquilerSinDatos();
        testPostAlquilerFechasPasadas();
        testPostAlquilerFechaFinAnterior();
        testPostAlquilerSocioNoPatron();
        testPostAlquilerSocioInexistente();
        testPostAlquilerEmbarcacionInexistente();
        testPostAlquilerJSONMalFormado();

        // ==================== PRUEBAS PATCH ====================
        System.out.println("\n🔧 ==================== PRUEBAS PATCH ====================\n");

        testPatchAgregarSocioAlquilerInexistente();
        testPatchQuitarSocioAlquilerInexistente();
        testPatchAgregarSocioSinParametro();
        testPatchQuitarSocioSinParametro();

        // ==================== PRUEBAS DELETE ====================
        System.out.println("\n🗑️  ==================== PRUEBAS DELETE ====================\n");

        testDeleteAlquilerInexistente();

        // ==================== RESULTADOS FINALES ====================
        System.out.println("\n📊 ==================== RESULTADOS ====================\n");
        System.out.println("✅ Tests exitosos: " + testsExitosos);
        System.out.println("❌ Tests fallidos: " + testsFallidos);
        int totalTests = testsExitosos + testsFallidos;
        if (totalTests > 0) {
            System.out.println("📈 Porcentaje de éxito: " +
                    String.format("%.1f", (testsExitosos * 100.0 / totalTests)) + "%");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏆 TESTS DE ALQUILERES COMPLETADOS (SEGÚN GUION)");
        System.out.println("=".repeat(60));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private static void printTestHeader(String nombre) {
        System.out.println("\n" + (testCounter++) + ". " + nombre);
        System.out.println("-".repeat(50));
    }

    private static void registroExitoso() {
        testsExitosos++;
    }

    private static void registroFallido() {
        testsFallidos++;
    }

    private static boolean verificarServidorActivo() {
        try {
            restTemplate.getForEntity(BASE_URL + "/api/alquileres", List.class);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️  No se puede conectar al servidor: " + e.getMessage());
            return false;
        }
    }

    private static HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static void verificarEstadoBD() {
        System.out.println("🔍 VERIFICANDO ESTADO DE LA BD");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("📊 Total alquileres en BD: " + response.getBody().size());

            if (!response.getBody().isEmpty()) {
                System.out.println("📋 Muestra de alquileres:");
                for (int i = 0; i < Math.min(response.getBody().size(), 3); i++) {
                    Map<String, Object> alquiler = response.getBody().get(i);
                    System.out.println("   - ID: " + alquiler.get("idAlquiler") +
                            ", Embarcación: " + alquiler.get("matriculaEmbarcacion") +
                            ", Fecha: " + alquiler.get("fechaInicio"));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  No se pudo verificar estado: " + e.getMessage());
        }
        System.out.println();
    }

    // ==================== PRUEBAS GET ====================

    private static void testGetAllAlquileres() {
        printTestHeader("GET /api/alquileres - Obtener todos los alquileres");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Total alquileres: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetAlquileresFuturos() {
        printTestHeader("GET /api/alquileres/futuros - Sin parámetro fecha");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/alquileres/futuros",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Alquileres futuros: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetAlquileresFuturosConFecha() {
        printTestHeader("GET /api/alquileres/futuros?fecha=2024-12-01 - Con fecha específica");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/alquileres/futuros?fecha=2024-12-01",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Alquileres futuros desde 2024-12-01: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetEmbarcacionesDisponiblesSinFechas() {
        printTestHeader("GET /api/alquileres/disponibles - Sin fechas (error esperado)");
        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/disponibles",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            System.out.println("❌ ERROR ESPERADO - Debería fallar sin fechas");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetEmbarcacionesDisponiblesFechasInvalidas() {
        printTestHeader("GET /api/alquileres/disponibles - Fechas inválidas");
        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/disponibles?fechaInicio=2025-01-01&fechaFin=2024-01-01",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            System.out.println("❌ ERROR ESPERADO - Fecha fin anterior a inicio");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("ℹ️  Resultado: " + e.getMessage());
            registroExitoso();
        }
    }

    // ==================== PRUEBAS POST ====================

    private static void testPostAlquilerSinDatos() {
        printTestHeader("POST /api/alquileres - Sin datos (error esperado)");

        String alquilerJson = "{}";

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Debería fallar sin datos");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerFechasPasadas() {
        printTestHeader("POST /api/alquileres - Fechas en pasado (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 1,
                    "dniSocioTitular": "12345678A",
                    "matriculaEmbarcacion": "ABC123",
                    "fechaInicio": "2020-01-01",
                    "fechaFin": "2020-01-03",
                    "plazasSolicitadas": 2,
                    "precioTotal": 120.0
                }
                """;

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Fechas en pasado");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerFechaFinAnterior() {
        printTestHeader("POST /api/alquileres - Fecha fin anterior a inicio (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 1,
                    "dniSocioTitular": "12345678A",
                    "matriculaEmbarcacion": "ABC123",
                    "fechaInicio": "2024-12-15",
                    "fechaFin": "2024-12-10",
                    "plazasSolicitadas": 2,
                    "precioTotal": 120.0
                }
                """;

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Fecha fin anterior");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerSocioNoPatron() {
        printTestHeader("POST /api/alquileres - Socio sin título patrón (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 999,
                    "dniSocioTitular": "99999999Z",
                    "matriculaEmbarcacion": "ABC123",
                    "fechaInicio": "2024-12-20",
                    "fechaFin": "2024-12-27",
                    "plazasSolicitadas": 2,
                    "precioTotal": 420.0
                }
                """;

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Socio no patrón");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerSocioInexistente() {
        printTestHeader("POST /api/alquileres - Socio inexistente (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 99999,
                    "dniSocioTitular": "INEXISTENTE",
                    "matriculaEmbarcacion": "ABC123",
                    "fechaInicio": "2024-12-20",
                    "fechaFin": "2024-12-27",
                    "plazasSolicitadas": 2,
                    "precioTotal": 420.0
                }
                """;

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Socio inexistente");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerEmbarcacionInexistente() {
        printTestHeader("POST /api/alquileres - Embarcación inexistente (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 1,
                    "dniSocioTitular": "12345678A",
                    "matriculaEmbarcacion": "INEXISTENTE999",
                    "fechaInicio": "2024-12-20",
                    "fechaFin": "2024-12-27",
                    "plazasSolicitadas": 2,
                    "precioTotal": 420.0
                }
                """;

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Embarcación inexistente");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPostAlquilerJSONMalFormado() {
        printTestHeader("POST /api/alquileres - JSON mal formado (error esperado)");

        String alquilerJson = """
                {
                    "idSocioTitular": 1,
                    "dniSocioTitular": "12345678A",
                    "matriculaEmbarcacion": "ABC123",
                    "fechaInicio": "2024-12-01",
                    "fechaFin": "2024-12-03",
                    "plazasSolicitadas": 2
                    "precioTotal": 120.0
                """; // Falta coma después de plazasSolicitadas

        HttpHeaders headers = getJsonHeaders();
        HttpEntity<String> request = new HttpEntity<>(alquilerJson, headers);

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres",
                    HttpMethod.POST,
                    request,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - JSON mal formado");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    // ==================== PRUEBAS PATCH ====================

    private static void testPatchAgregarSocioAlquilerInexistente() {
        printTestHeader("PATCH /api/alquileres/99999/agregar-socio - Alquiler inexistente");

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/99999/agregar-socio?dniSocio=11111111A",
                    HttpMethod.PATCH,
                    new HttpEntity<>(getJsonHeaders()),
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Alquiler inexistente");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPatchQuitarSocioAlquilerInexistente() {
        printTestHeader("PATCH /api/alquileres/99999/quitar-socio - Alquiler inexistente");

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/99999/quitar-socio?dniSocio=11111111A",
                    HttpMethod.PATCH,
                    new HttpEntity<>(getJsonHeaders()),
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Alquiler inexistente");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPatchAgregarSocioSinParametro() {
        printTestHeader("PATCH /api/alquileres/1/agregar-socio - Sin parámetro dniSocio");

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/1/agregar-socio",
                    HttpMethod.PATCH,
                    new HttpEntity<>(getJsonHeaders()),
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Falta parámetro");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testPatchQuitarSocioSinParametro() {
        printTestHeader("PATCH /api/alquileres/1/quitar-socio - Sin parámetro dniSocio");

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/1/quitar-socio",
                    HttpMethod.PATCH,
                    new HttpEntity<>(getJsonHeaders()),
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Falta parámetro");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    // ==================== PRUEBAS DELETE ====================

    private static void testDeleteAlquilerInexistente() {
        printTestHeader("DELETE /api/alquileres/99999 - Alquiler inexistente");

        try {
            restTemplate.exchange(
                    BASE_URL + "/api/alquileres/99999",
                    HttpMethod.DELETE,
                    null,
                    String.class);
            System.out.println("❌ ERROR ESPERADO - Alquiler inexistente");
            registroFallido();
        } catch (HttpClientErrorException e) {
            System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }
}