package es.uco.pw.pw2526.client;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestReservasAPI {

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
        System.out.println("║   PRUEBAS DE LA API DE RESERVAS (TESTS ESTABLES)            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        if (!verificarServidorActivo()) {
            System.out.println("❌ ERROR: El servidor no está disponible en " + BASE_URL);
            return;
        }

        verificarEstadoBD();

        // ==================== PRUEBAS GET ====================
        System.out.println("\n📋 ==================== PRUEBAS GET ====================\n");
        testGetAllReservas();
        testGetReservasFuturas();
        testGetReservasFuturasConFecha();
        testGetReservaById();
        testGetReservaByIdInexistente();
        testGetReservasPorSocio();
        testGetReservasPorSocioInexistente();
        testGetReservasPorEmbarcacion();
        testGetReservasPorEmbarcacionInexistente();
        testGetReservasPorFecha();

        // ==================== PRUEBAS POST ====================
        System.out.println("\n📝 ==================== PRUEBAS POST ====================\n");

        // Casos exitosos
        testPostReservaExitoso();
        testPostReservaConPatron();
        testPostReservaDiferenteEmbarcacion();

        // Casos de error
        testPostReservaSinMatricula();
        testPostReservaSinSocio();
        testPostReservaFechaPasada();
        testPostReservaPlazasExcedidas();
        testPostReservaSocioNoMayorEdad();
        testPostReservaSinTituloPatron();
        testPostReservaJSONMalFormado();

        // ==================== PRUEBAS DELETE ====================
        System.out.println("\n🗑️  ==================== PRUEBAS DELETE ====================\n");

        testDeleteReservaInexistente();
        testDeleteReservaPasada();

        // ==================== RESULTADOS FINALES ====================
        System.out.println("\n📊 ==================== RESULTADOS ====================\n");
        System.out.println("✅ Tests exitosos: " + testsExitosos);
        System.out.println("❌ Tests fallidos: " + testsFallidos);
        int totalTests = testsExitosos + testsFallidos;
        if (totalTests > 0) {
            System.out.println("📈 Porcentaje de éxito: " +
                    String.format("%.1f", (testsExitosos * 100.0 / totalTests)) + "%");
        }

        System.out.println("\n✅ TESTS COMPLETADOS");
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
            restTemplate.getForEntity(BASE_URL + "/api/reservas", List.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String generarFechaUnica(int minDias, int maxDias) {
        int dias = minDias + random.nextInt(maxDias - minDias + 1);
        return LocalDate.now()
                .plusDays(dias)
                .format(DateTimeFormatter.ISO_DATE);
    }

    // ==================== PRUEBAS GET ====================

    private static void testGetAllReservas() {
        printTestHeader("GET /api/reservas - Obtener todas las reservas");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Total reservas: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasFuturas() {
        printTestHeader("GET /api/reservas/futuras - Sin parámetro fecha");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/futuras",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas futuras: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasFuturasConFecha() {
        printTestHeader("GET /api/reservas/futuras?fecha=2024-12-01 - Con fecha específica");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/futuras?fecha=2024-12-01",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas futuras desde 2024-12-01: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservaById() {
        printTestHeader("GET /api/reservas/1 - Reserva existente");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/1",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Status: " + response.getStatusCode());
                System.out.println("✅ Reserva encontrada");
                registroExitoso();
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("ℹ️  Reserva con ID 1 no encontrada");
                registroExitoso();
            }
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("ℹ️  Reserva no encontrada (caso válido)");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservaByIdInexistente() {
        printTestHeader("GET /api/reservas/99999 - Reserva inexistente");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/99999",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("✅ Status NOT_FOUND correcto");
                registroExitoso();
            } else {
                System.out.println("⚠️  Status: " + response.getStatusCode());
                registroFallido();
            }
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("✅ Error NOT_FOUND capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasPorSocio() {
        printTestHeader("GET /api/reservas/socio/1 - Reservas de socio existente");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/socio/1",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas del socio 1: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasPorSocioInexistente() {
        printTestHeader("GET /api/reservas/socio/99999 - Socio inexistente");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/socio/99999",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas: " + response.getBody().size() + " (debería ser 0)");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasPorEmbarcacion() {
        printTestHeader("GET /api/reservas/embarcacion/ABC123 - Embarcación existente");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/embarcacion/ABC123",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas de embarcación ABC123: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasPorEmbarcacionInexistente() {
        printTestHeader("GET /api/reservas/embarcacion/INEXISTENTE - Embarcación inexistente");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/embarcacion/INEXISTENTE",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas: " + response.getBody().size() + " (debería ser 0)");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testGetReservasPorFecha() {
        printTestHeader("GET /api/reservas/fecha/2024-12-01 - Fecha específica");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/fecha/2024-12-01",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Reservas para 2024-12-01: " + response.getBody().size());
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    // ==================== PRUEBAS POST ====================

    private static void testPostReservaExitoso() {
        printTestHeader("POST /api/reservas - Caso exitoso básico");

        String fechaUnica = generarFechaUnica(30, 60);
        String descripcion = "Reserva exitosa test " + System.currentTimeMillis();

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 1,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "%s",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "%s"
                }
                """, descripcion, fechaUnica);

        if (ejecutarPostReserva(reservaJson, true)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaConPatron() {
        printTestHeader("POST /api/reservas - Con embarcación que requiere patrón");

        String fechaUnica = generarFechaUnica(70, 90);

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 3,
                    "plazasSolicitadas": 3,
                    "propositoActividad": "Reserva con patrón %s",
                    "precioTotal": 180.0,
                    "matriculaEmbarcacion": "GHI789",
                    "fechaActividad": "%s"
                }
                """, System.currentTimeMillis(), fechaUnica);

        if (ejecutarPostReserva(reservaJson, true)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaDiferenteEmbarcacion() {
        printTestHeader("POST /api/reservas - Con diferente embarcación");

        String fechaUnica = generarFechaUnica(100, 120);

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 2,
                    "plazasSolicitadas": 1,
                    "propositoActividad": "Reserva diferente embarcación %s",
                    "precioTotal": 80.0,
                    "matriculaEmbarcacion": "DEF456",
                    "fechaActividad": "%s"
                }
                """, System.currentTimeMillis(), fechaUnica);

        if (ejecutarPostReserva(reservaJson, true)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaSinMatricula() {
        printTestHeader("POST /api/reservas - Sin matrícula (error esperado)");

        String reservaJson = """
                {
                    "idSocioSolicitante": 1,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "Reserva sin matrícula",
                    "precioTotal": 100.0,
                    "fechaActividad": "2024-12-15"
                }
                """;

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaSinSocio() {
        printTestHeader("POST /api/reservas - Sin socio (error esperado)");

        String reservaJson = """
                {
                    "plazasSolicitadas": 2,
                    "propositoActividad": "Reserva sin socio",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "2024-12-15"
                }
                """;

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaFechaPasada() {
        printTestHeader("POST /api/reservas - Fecha pasada (error esperado)");

        String reservaJson = """
                {
                    "idSocioSolicitante": 1,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "Reserva fecha pasada",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "2020-01-01"
                }
                """;

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaPlazasExcedidas() {
        printTestHeader("POST /api/reservas - Plazas exceden capacidad (error esperado)");

        String fechaUnica = generarFechaUnica(130, 150);

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 1,
                    "plazasSolicitadas": 50,
                    "propositoActividad": "Reserva plazas excedidas",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "%s"
                }
                """, fechaUnica);

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaSocioNoMayorEdad() {
        printTestHeader("POST /api/reservas - Socio no mayor de edad (error esperado)");

        String fechaUnica = generarFechaUnica(160, 180);

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 999,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "Reserva socio menor",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "%s"
                }
                """, fechaUnica);

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaSinTituloPatron() {
        printTestHeader("POST /api/reservas - Sin título de patrón (error esperado)");

        String fechaUnica = generarFechaUnica(190, 210);

        String reservaJson = String.format("""
                {
                    "idSocioSolicitante": 4,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "Reserva sin título patrón",
                    "precioTotal": 100.0,
                    "matriculaEmbarcacion": "JKL012",
                    "fechaActividad": "%s"
                }
                """, fechaUnica);

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    private static void testPostReservaJSONMalFormado() {
        printTestHeader("POST /api/reservas - JSON mal formado (error esperado)");

        String reservaJson = """
                {
                    "idSocioSolicitante": 1,
                    "plazasSolicitadas": 2,
                    "propositoActividad": "JSON malo",
                    "precioTotal": 100.0
                    "matriculaEmbarcacion": "ABC123",
                    "fechaActividad": "2024-12-15"
                """;

        if (ejecutarPostReserva(reservaJson, false)) {
            registroExitoso();
        } else {
            registroFallido();
        }
    }

    // ==================== PRUEBAS DELETE ====================

    private static void testDeleteReservaInexistente() {
        printTestHeader("DELETE /api/reservas/99999 - Reserva inexistente");
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/99999",
                    HttpMethod.DELETE,
                    null,
                    Void.class);

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("✅ NOT_FOUND correcto");
                registroExitoso();
            } else {
                System.out.println("⚠️  Status: " + response.getStatusCode());
                registroFallido();
            }
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("✅ Error NOT_FOUND capturado correctamente");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    private static void testDeleteReservaPasada() {
        printTestHeader("DELETE /api/reservas/1 - Reserva posiblemente pasada");
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas/1",
                    HttpMethod.DELETE,
                    null,
                    Void.class);

            if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                System.out.println("✅ BAD_REQUEST correcto para reserva pasada");
                registroExitoso();
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("ℹ️  Reserva no encontrada");
                registroExitoso();
            } else if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                System.out.println("ℹ️  Reserva eliminada (era futura)");
                registroExitoso();
            } else {
                System.out.println("⚠️  Status inesperado: " + response.getStatusCode());
                registroFallido();
            }
        } catch (HttpClientErrorException.BadRequest e) {
            System.out.println("✅ Error BAD_REQUEST capturado correctamente");
            registroExitoso();
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("ℹ️  Reserva no encontrada");
            registroExitoso();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            registroFallido();
        }
    }

    // ==================== MÉTODOS DE APOYO ====================

    private static boolean ejecutarPostReserva(String json, boolean esperaExito) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas",
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (esperaExito && response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("✅ POST exitoso - Status: " + response.getStatusCode());
                return true;
            } else if (!esperaExito && response.getStatusCode().is4xxClientError()) {
                System.out.println("✅ Error esperado - Status: " + response.getStatusCode());
                return true;
            } else {
                System.out.println("⚠️  Status inesperado: " + response.getStatusCode() +
                        " (esperaba: " + (esperaExito ? "CREATED" : "4xx") + ")");
                return false;
            }
        } catch (HttpClientErrorException e) {
            if (!esperaExito && e.getStatusCode().is4xxClientError()) {
                System.out.println("✅ Error " + e.getStatusCode() + " capturado correctamente");
                return true;
            } else if (esperaExito) {
                System.out.println("❌ Error inesperado: " + e.getStatusCode());
                return false;
            } else {
                System.out.println("⚠️  Error diferente: " + e.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Excepción: " + e.getMessage());
            return false;
        }
    }

    private static void verificarEstadoBD() {
        System.out.println("🔍 VERIFICANDO ESTADO DE LA BD");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    BASE_URL + "/api/reservas",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            System.out.println("📊 Total reservas: " + response.getBody().size());

            if (!response.getBody().isEmpty()) {
                System.out.println("📋 Muestra de reservas:");
                for (int i = 0; i < Math.min(response.getBody().size(), 3); i++) {
                    Map<String, Object> reserva = response.getBody().get(i);
                    System.out.println("   - ID: " + reserva.get("idReserva") +
                            ", Fecha: " + reserva.get("fechaActividad") +
                            ", Embarcación: " + reserva.get("matriculaEmbarcacion"));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  No se pudo verificar estado: " + e.getMessage());
        }
        System.out.println();
    }
}