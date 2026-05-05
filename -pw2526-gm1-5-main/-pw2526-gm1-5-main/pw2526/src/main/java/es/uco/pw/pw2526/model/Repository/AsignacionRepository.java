package es.uco.pw.pw2526.model.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.Asignacion.Asignacion;
import es.uco.pw.pw2526.model.domain.Empleados.Patron;

@Repository
public class AsignacionRepository extends AbstractRepository {

    public AsignacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlQueries = cargarSqlProperties();
    }

    @Override
    public Properties cargarSqlProperties() {
        Properties props = new Properties();
        try {
            cargarInputStream(props);
        } catch (IOException excepcion) {
            System.err.println("❌ Error cargando sql.properties:");
            excepcion.printStackTrace();
            throw new RuntimeException(excepcion);
        }
        return props;
    }

    private void cargarInputStream(Properties props) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db/sql.properties")) {
            if (input == null) {
                System.err.println("⚠️ No se encontró sql.properties en resources/");
                return;
            }
            props.load(input);
            System.out.println("✅ Archivo sql.properties cargado correctamente.");
        }
    }

    public Integer obtenerPatronActual(String matricula) {
        return obtenerPatronAsignado(matricula);
    }

    public boolean asignarPatron(String matricula, int idEmpleado) {
        try {
            return ejecutarAsignarPatronSimple(matricula, idEmpleado);
        } catch (DataAccessException excepcion) {
            System.err.println("Error durante la asignación simple de patrón: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarAsignarPatronSimple(String matricula, int idEmpleado) {
        String finalizarQuery = "UPDATE Asignacion SET fecha_fin_asignacion = CURDATE() WHERE matricula_embarcacion = ? AND fecha_fin_asignacion IS NULL";
        jdbcTemplate.update(finalizarQuery, matricula);

        Asignacion nuevaAsignacion = new Asignacion();
        nuevaAsignacion.setMatriculaEmbarcacion(matricula);
        nuevaAsignacion.setIdEmpleado(idEmpleado);
        nuevaAsignacion.setFechaAsignacion(LocalDate.now());
        nuevaAsignacion.setFechaFin(null);

        boolean historialExitoso = asignarPatron(nuevaAsignacion);

        String updateEmbarcacionQuery = "UPDATE Embarcaciones SET id_patron_asignado = ? WHERE matricula = ?";
        int estadoActualizado = jdbcTemplate.update(updateEmbarcacionQuery, idEmpleado, matricula);

        return historialExitoso && (estadoActualizado > 0);
    }

    public boolean existeSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarExisteSolapamiento(matricula, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando solapamiento de fechas");
            throw excepcion;
        }
    }

    private boolean ejecutarExisteSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        String query = sqlQueries.getProperty("asignacion.verificar.solapamiento");
        if (query == null) {
            query = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "((fecha_asignacion <= ? AND fecha_fin_asignacion >= ?) OR " +
                    "(fecha_asignacion >= ? AND fecha_fin_asignacion <= ?) OR " +
                    "(fecha_asignacion BETWEEN ? AND ?) OR " +
                    "(fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                    "(? BETWEEN fecha_asignacion AND fecha_fin_asignacion))";
        }
        Integer count = jdbcTemplate.queryForObject(query,
                new Object[] { matricula, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio },
                Integer.class);
        return count != null && count > 0;
    }

    public List<Patron> obtenerPatronesSolapados(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarObtenerPatronesSolapados(matricula, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrones solapados");
            throw excepcion;
        }
    }

    private List<Patron> ejecutarObtenerPatronesSolapados(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        String query = "SELECT DISTINCT e.* FROM Empleado e JOIN Asignacion a ON e.id_empleado = a.id_empleado " +
                "WHERE a.matricula_embarcacion = ? AND " +
                "((a.fecha_asignacion <= ? AND a.fecha_fin_asignacion >= ?) OR " +
                "(a.fecha_asignacion >= ? AND a.fecha_fin_asignacion <= ?) OR " +
                "(a.fecha_asignacion BETWEEN ? AND ?) OR " +
                "(a.fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                "(? BETWEEN a.fecha_asignacion AND a.fecha_fin_asignacion))";
        return jdbcTemplate.query(query,
                new Object[] { matricula, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio },
                (rs, rowNum) -> mapPatron(rs));
    }

    public String obtenerTipoSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return evaluarTipoSolapamiento(matricula, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando tipo de solapamiento");
            throw excepcion;
        }
    }

    private String evaluarTipoSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        if (solapaDentro(matricula, fechaInicio, fechaFin)) return "NUEVA_DENTRO_EXISTENTE";
        if (solapaCubre(matricula, fechaInicio, fechaFin)) return "NUEVA_CUBRE_EXISTENTE";
        if (solapaInicio(matricula, fechaInicio, fechaFin)) return "SOLAPAMIENTO_INICIO";
        if (solapaFin(matricula, fechaInicio, fechaFin)) return "SOLAPAMIENTO_FIN";
        return "SIN_SOLAPAMIENTO";
    }

    private boolean solapaDentro(String matricula, LocalDate fInicio, LocalDate fFin) {
        String q = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND fecha_asignacion <= ? AND fecha_fin_asignacion >= ?";
        Integer count = jdbcTemplate.queryForObject(q, new Object[] { matricula, fInicio, fFin }, Integer.class);
        return count != null && count > 0;
    }

    private boolean solapaCubre(String matricula, LocalDate fInicio, LocalDate fFin) {
        String q = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND fecha_asignacion >= ? AND fecha_fin_asignacion <= ?";
        Integer count = jdbcTemplate.queryForObject(q, new Object[] { matricula, fInicio, fFin }, Integer.class);
        return count != null && count > 0;
    }

    private boolean solapaInicio(String matricula, LocalDate fInicio, LocalDate fFin) {
        String q = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND fecha_asignacion BETWEEN ? AND ?";
        Integer count = jdbcTemplate.queryForObject(q, new Object[] { matricula, fInicio, fFin }, Integer.class);
        return count != null && count > 0;
    }

    private boolean solapaFin(String matricula, LocalDate fInicio, LocalDate fFin) {
        String q = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND fecha_fin_asignacion BETWEEN ? AND ?";
        Integer count = jdbcTemplate.queryForObject(q, new Object[] { matricula, fInicio, fFin }, Integer.class);
        return count != null && count > 0;
    }

    public Integer obtenerPatronAsignado(String matricula) {
        try {
            return ejecutarObtenerPatronAsignado(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error al obtener patrón asignado para: " + matricula);
            throw excepcion;
        }
    }

    private Integer ejecutarObtenerPatronAsignado(String matricula) {
        String query = sqlQueries.getProperty("asignacion.obtener.patron.embarcacion");
        if (query == null) {
            query = "SELECT id_patron_asignado FROM Embarcaciones WHERE matricula = ?";
        }
        List<Integer> ids = jdbcTemplate.query(query, new Object[] { matricula }, (ResultSet rs, int rowNum) -> {
            int id = rs.getInt("id_patron_asignado");
            return rs.wasNull() ? null : id;
        });
        return ids.isEmpty() ? null : ids.get(0);
    }

    public List<Map<String, Object>> obtenerTodasAsignacionesEmbarcacion(String matricula) {
        try {
            return ejecutarObtenerTodasAsignacionesEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error al obtener información del patrón asignado para: " + matricula);
            throw excepcion;
        }
    }

    private List<Map<String, Object>> ejecutarObtenerTodasAsignacionesEmbarcacion(String matricula) {
        String query = sqlQueries.getProperty("asignacion.obtener.todas.por.embarcacion");
        if (query == null) {
            query = "SELECT a.id_empleado, a.fecha_asignacion, a.fecha_fin_asignacion, " +
                    "e.nombre AS nombre_patron, e.apellidos AS apellidos_patron " +
                    "FROM Asignacion a " +
                    "JOIN Empleado e ON a.id_empleado = e.id_empleado " +
                    "WHERE a.matricula_embarcacion = ? " +
                    "ORDER BY a.fecha_asignacion DESC";
        }
        return jdbcTemplate.query(query, new Object[] { matricula }, new RowMapper<Map<String, Object>>() {
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> info = new HashMap<>();
                info.put("id_empleado", rs.getInt("id_empleado"));
                info.put("nombre_patron", rs.getString("nombre_patron"));
                info.put("apellidos_patron", rs.getString("apellidos_patron"));
                java.sql.Date fechaAsignacionSql = rs.getDate("fecha_asignacion");
                info.put("fecha_asignacion", fechaAsignacionSql != null ? fechaAsignacionSql.toLocalDate() : null);
                java.sql.Date fechaFinSql = rs.getDate("fecha_fin_asignacion");
                info.put("fecha_fin_asignacion", fechaFinSql != null ? fechaFinSql.toLocalDate() : null);
                return info;
            }
        });
    }

    public Map<String, Object> obtenerAsignacionActiva(String matricula) {
        try {
            return ejecutarObtenerAsignacionActiva(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error al obtener asignación activa para: " + matricula);
            throw excepcion;
        }
    }

    private Map<String, Object> ejecutarObtenerAsignacionActiva(String matricula) {
        String query = "SELECT a.id_empleado, a.fecha_asignacion, a.fecha_fin_asignacion, " +
                "e.nombre AS nombre_patron, e.apellidos AS apellidos_patron " +
                "FROM Asignacion a " +
                "JOIN Empleado e ON a.id_empleado = e.id_empleado " +
                "WHERE a.matricula_embarcacion = ? " +
                "AND a.fecha_asignacion <= CURDATE() " +
                "AND (a.fecha_fin_asignacion IS NULL OR a.fecha_fin_asignacion >= CURDATE()) " +
                "ORDER BY a.fecha_asignacion DESC LIMIT 1";

        List<Map<String, Object>> resultados = jdbcTemplate.query(query, new Object[] { matricula }, new RowMapper<Map<String, Object>>() {
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> info = new HashMap<>();
                info.put("id_empleado", rs.getInt("id_empleado"));
                info.put("nombre_patron", rs.getString("nombre_patron"));
                info.put("apellidos_patron", rs.getString("apellidos_patron"));
                java.sql.Date fechaAsignacionSql = rs.getDate("fecha_asignacion");
                info.put("fecha_asignacion", fechaAsignacionSql != null ? fechaAsignacionSql.toLocalDate() : null);
                java.sql.Date fechaFinSql = rs.getDate("fecha_fin_asignacion");
                info.put("fecha_fin_asignacion", fechaFinSql != null ? fechaFinSql.toLocalDate() : null);
                return info;
            }
        });
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public boolean tienePatronAsignado(String matricula) {
        try {
            return ejecutarTienePatronAsignado(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando si la embarcación tiene patrón asignado: " + matricula);
            throw excepcion;
        }
    }

    private boolean ejecutarTienePatronAsignado(String matricula) {
        String query = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? " +
                "AND fecha_asignacion <= CURDATE() AND (fecha_fin_asignacion IS NULL OR fecha_fin_asignacion >= CURDATE())";
        Integer count = jdbcTemplate.queryForObject(query, new Object[] { matricula }, Integer.class);
        return count != null && count > 0;
    }

    public boolean patronYaAsignado(int idEmpleado) {
        try {
            return ejecutarPatronYaAsignado(idEmpleado);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando si el patrón ya está asignado");
            throw excepcion;
        }
    }

    private boolean ejecutarPatronYaAsignado(int idEmpleado) {
        String query = sqlQueries.getProperty("asignacion.patron.yaAsignado");
        if (query == null) {
            query = "SELECT COUNT(*) FROM Asignacion WHERE id_empleado = ? AND " +
                    "fecha_asignacion <= CURDATE() AND (fecha_fin_asignacion IS NULL OR fecha_fin_asignacion >= CURDATE())";
        }
        Integer count = jdbcTemplate.queryForObject(query, new Object[] { idEmpleado }, Integer.class);
        return count != null && count > 0;
    }

    public boolean patronTieneSolapamiento(int idEmpleado, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarPatronTieneSolapamiento(idEmpleado, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando solapamiento del patrón");
            throw excepcion;
        }
    }

    private boolean ejecutarPatronTieneSolapamiento(int idEmpleado, LocalDate fechaInicio, LocalDate fechaFin) {
        String query = "SELECT COUNT(*) FROM Asignacion WHERE id_empleado = ? AND " +
                "((fecha_asignacion <= ? AND fecha_fin_asignacion >= ?) OR " +
                "(fecha_asignacion >= ? AND fecha_fin_asignacion <= ?) OR " +
                "(fecha_asignacion BETWEEN ? AND ?) OR " +
                "(fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                "(? BETWEEN fecha_asignacion AND fecha_fin_asignacion))";
        Integer count = jdbcTemplate.queryForObject(query,
                new Object[] { idEmpleado, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio },
                Integer.class);
        return count != null && count > 0;
    }

    public List<Patron> obtenerPatronesDisponibles() {
        try {
            return ejecutarObtenerPatronesDisponibles();
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error obteniendo patrones disponibles: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private List<Patron> ejecutarObtenerPatronesDisponibles() {
        String query = sqlQueries.getProperty("empleados.obtener.todos");
        if (query == null) {
            query = "SELECT * FROM Empleado ORDER BY apellidos, nombre";
        }
        return jdbcTemplate.query(query, (rs, rowNum) -> mapPatron(rs));
    }

    public boolean asignarPatron(Asignacion asignacion) {
        try {
            return ejecutarAsignarPatron(asignacion);
        } catch (DataAccessException excepcion) {
            System.err.println("Error asignando patrón a embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarAsignarPatron(Asignacion asignacion) {
        String query = sqlQueries.getProperty("asignacion.insertar");
        if (query == null) {
            throw new RuntimeException("ERROR: Propiedad 'asignacion.insertar' no encontrada");
        }
        int filasAfectadas = jdbcTemplate.update(query, asignacion.getMatriculaEmbarcacion(), asignacion.getIdEmpleado(), asignacion.getFechaAsignacion(), asignacion.getFechaFin());
        return filasAfectadas > 0;
    }

    public boolean eliminarAsignacionesSolapadas(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarEliminarAsignacionesSolapadas(matricula, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando asignaciones solapadas");
            throw excepcion;
        }
    }

    private boolean ejecutarEliminarAsignacionesSolapadas(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        String query = "DELETE FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                "((fecha_asignacion <= ? AND fecha_fin_asignacion >= ?) OR " +
                "(fecha_asignacion >= ? AND fecha_fin_asignacion <= ?) OR " +
                "(fecha_asignacion BETWEEN ? AND ?) OR " +
                "(fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                "(? BETWEEN fecha_asignacion AND fecha_fin_asignacion))";
        int filasAfectadas = jdbcTemplate.update(query, matricula, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio, fechaFin, fechaInicio);
        return filasAfectadas >= 0;
    }

    private Patron mapPatron(ResultSet rs) throws SQLException {
        Patron patron = new Patron();
        patron.setId(rs.getInt("id_empleado"));
        patron.setDni(rs.getString("dni"));
        patron.setNombre(rs.getString("nombre"));
        patron.setApellidos(rs.getString("apellidos"));
        if (rs.getDate("fecha_nacimiento") != null) {
            patron.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());
        }
        if (rs.getDate("fecha_expedicion_titulo") != null) {
            patron.setFechaExpedicionTitulo(rs.getDate("fecha_expedicion_titulo").toLocalDate());
        }
        return patron;
    }
}