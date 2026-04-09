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

import es.uco.pw.pw2526.model.domain.Empleados.Empleados;

@Repository
public class AsignacionRepository extends AbstractRepository {

    /**
     * Constructor del repositorio de asignaciones
     * 
     * @param jdbcTemplate Plantilla JDBC para operaciones de base de datos
     */
    public AsignacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlQueries = cargarSqlProperties();
    }

    /**
     * Carga las propiedades SQL desde el archivo de configuración
     * 
     * @return Properties con las consultas SQL cargadas
     */
    public Properties cargarSqlProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db/sql.properties")) {
            if (input == null) {
                System.err.println("⚠️ No se encontró sql.properties en resources/");
                return props;
            }
            props.load(input);
            System.out.println("✅ Archivo sql.properties cargado correctamente.");
        } catch (IOException e) {
            System.err.println("❌ Error cargando sql.properties:");
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Obtiene el patrón actual asignado a una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return ID del empleado patrón asignado, null si no hay asignación
     */
    public Integer obtenerPatronActual(String matricula) {
        return obtenerPatronAsignado(matricula);
    }

    /**
     * Asigna un patrón a una embarcación
     * 
     * @param matricula  Matrícula de la embarcación
     * @param idEmpleado ID del empleado a asignar como patrón
     * @return true si la asignación fue exitosa, false en caso contrario
     */
    public boolean asignarPatron(String matricula, int idEmpleado) {
        try {
            String finalizarQuery = "UPDATE Asignacion SET fecha_fin_asignacion = CURDATE() WHERE matricula_embarcacion = ? AND fecha_fin_asignacion IS NULL";
            jdbcTemplate.update(finalizarQuery, matricula);

            boolean historialExitoso = asignarPatron(matricula, idEmpleado, LocalDate.now(), null);

            String updateEmbarcacionQuery = "UPDATE Embarcaciones SET id_patron_asignado = ? WHERE matricula = ?";
            int estadoActualizado = jdbcTemplate.update(updateEmbarcacionQuery, idEmpleado, matricula);

            return historialExitoso && (estadoActualizado > 0);

        } catch (DataAccessException e) {
            System.err.println("Error durante la asignación simple de patrón: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si existe solapamiento de fechas para una embarcación
     * 
     * @param matricula   Matrícula de la embarcación
     * @param fechaInicio Fecha de inicio del período a verificar
     * @param fechaFin    Fecha de fin del período a verificar
     * @return true si existe solapamiento, false en caso contrario
     */
    public boolean existeSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
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
                    new Object[] {
                            matricula,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio
                    },
                    Integer.class);

            boolean haySolapamiento = count != null && count > 0;
            System.out.println("🔍 Solapamiento encontrado para " + matricula + ": " + haySolapamiento);
            return haySolapamiento;

        } catch (DataAccessException e) {
            System.err.println("Error verificando solapamiento de fechas");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene los patrones que tienen asignaciones solapadas con un período
     * 
     * @param matricula   Matrícula de la embarcación
     * @param fechaInicio Fecha de inicio del período
     * @param fechaFin    Fecha de fin del período
     * @return Lista de empleados con asignaciones solapadas
     */
    public List<Empleados> obtenerPatronesSolapados(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String query = "SELECT DISTINCT e.* FROM Empleado e JOIN Asignacion a ON e.id_empleado = a.id_empleado " +
                    "WHERE a.matricula_embarcacion = ? AND " +
                    "((a.fecha_asignacion <= ? AND a.fecha_fin_asignacion >= ?) OR " +
                    "(a.fecha_asignacion >= ? AND a.fecha_fin_asignacion <= ?) OR " +
                    "(a.fecha_asignacion BETWEEN ? AND ?) OR " +
                    "(a.fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                    "(? BETWEEN a.fecha_asignacion AND a.fecha_fin_asignacion))";

            List<Empleados> empleados = jdbcTemplate.query(query,
                    new Object[] {
                            matricula,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio
                    },
                    (rs, rowNum) -> mapEmpleado(rs));

            System.out.println("🔍 Encontrados " + empleados.size() + " patrones solapados");
            return empleados;

        } catch (DataAccessException e) {
            System.err.println("Error obteniendo patrones solapados");
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Determina el tipo de solapamiento entre períodos
     * 
     * @param matricula   Matrícula de la embarcación
     * @param fechaInicio Fecha de inicio del período nuevo
     * @param fechaFin    Fecha de fin del período nuevo
     * @return String que describe el tipo de solapamiento
     */
    public String obtenerTipoSolapamiento(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String queryDentro = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "fecha_asignacion <= ? AND fecha_fin_asignacion >= ?";
            Integer dentro = jdbcTemplate.queryForObject(queryDentro,
                    new Object[] { matricula, fechaInicio, fechaFin }, Integer.class);

            if (dentro != null && dentro > 0) {
                return "NUEVA_DENTRO_EXISTENTE";
            }

            String queryCubre = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "fecha_asignacion >= ? AND fecha_fin_asignacion <= ?";
            Integer cubre = jdbcTemplate.queryForObject(queryCubre,
                    new Object[] { matricula, fechaInicio, fechaFin }, Integer.class);

            if (cubre != null && cubre > 0) {
                return "NUEVA_CUBRE_EXISTENTE";
            }

            String queryInicio = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "fecha_asignacion BETWEEN ? AND ?";
            Integer inicioSolap = jdbcTemplate.queryForObject(queryInicio,
                    new Object[] { matricula, fechaInicio, fechaFin }, Integer.class);

            if (inicioSolap != null && inicioSolap > 0) {
                return "SOLAPAMIENTO_INICIO";
            }

            String queryFin = "SELECT COUNT(*) FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "fecha_fin_asignacion BETWEEN ? AND ?";
            Integer finSolap = jdbcTemplate.queryForObject(queryFin,
                    new Object[] { matricula, fechaInicio, fechaFin }, Integer.class);

            if (finSolap != null && finSolap > 0) {
                return "SOLAPAMIENTO_FIN";
            }

            return "SIN_SOLAPAMIENTO";

        } catch (DataAccessException e) {
            System.err.println("Error verificando tipo de solapamiento");
            return "ERROR";
        }
    }

    /**
     * Obtiene el patrón asignado a una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return ID del empleado patrón asignado
     */
    public Integer obtenerPatronAsignado(String matricula) {
        try {
            String query = sqlQueries.getProperty("asignacion.obtener.patron.embarcacion");
            if (query == null) {
                System.out.println("⚠️ Usando consulta directa para obtener patrón asignado");
                query = "SELECT id_patron_asignado FROM Embarcaciones WHERE matricula = ?";
            }

            System.out.println("🔍 Consulta para obtener patrón: " + query);
            System.out.println("🔍 Matrícula: " + matricula);

            List<Integer> ids = jdbcTemplate.query(query,
                    new Object[] { matricula },
                    (ResultSet rs, int rowNum) -> {
                        int id = rs.getInt("id_patron_asignado");
                        return rs.wasNull() ? null : id;
                    });

            Integer resultado = ids.isEmpty() ? null : ids.get(0);
            System.out.println("🔍 Resultado de obtenerPatronAsignado para " + matricula + ": " + resultado);
            return resultado;

        } catch (DataAccessException e) {
            System.err.println("Error al obtener patrón asignado para: " + matricula);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene todas las asignaciones de una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return Lista de mapas con información de las asignaciones
     */
    public List<Map<String, Object>> obtenerTodasAsignacionesEmbarcacion(String matricula) {
        try {
            String query = sqlQueries.getProperty("asignacion.obtener.todas.por.embarcacion");
            if (query == null) {
                query = "SELECT a.id_empleado, a.fecha_asignacion, a.fecha_fin_asignacion, " +
                        "e.nombre AS nombre_patron, e.apellidos AS apellidos_patron " +
                        "FROM Asignacion a " +
                        "JOIN Empleado e ON a.id_empleado = e.id_empleado " +
                        "WHERE a.matricula_embarcacion = ? " +
                        "ORDER BY a.fecha_asignacion DESC";
            }

            System.out.println("🔍 Buscando TODAS las asignaciones para matrícula: " + matricula);
            System.out.println("🔍 Consulta SQL: " + query);

            List<Map<String, Object>> resultados = jdbcTemplate.query(query,
                    new Object[] { matricula },
                    new RowMapper<Map<String, Object>>() {
                        public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Map<String, Object> info = new HashMap<>();
                            info.put("id_empleado", rs.getInt("id_empleado"));
                            info.put("nombre_patron", rs.getString("nombre_patron"));
                            info.put("apellidos_patron", rs.getString("apellidos_patron"));

                            java.sql.Date fechaAsignacionSql = rs.getDate("fecha_asignacion");
                            if (fechaAsignacionSql != null) {
                                info.put("fecha_asignacion", fechaAsignacionSql.toLocalDate());
                            } else {
                                info.put("fecha_asignacion", null);
                            }

                            java.sql.Date fechaFinSql = rs.getDate("fecha_fin_asignacion");
                            if (fechaFinSql != null) {
                                info.put("fecha_fin_asignacion", fechaFinSql.toLocalDate());
                            } else {
                                info.put("fecha_fin_asignacion", null);
                            }

                            System.out.println("✅ Encontrada asignación: Patrón " + info.get("nombre_patron") + " "
                                    + info.get("apellidos_patron") +
                                    " | Fecha inicio: " + info.get("fecha_asignacion") +
                                    " | Fecha fin: " + info.get("fecha_fin_asignacion"));
                            return info;
                        }
                    });

            System.out.println("📊 Total de asignaciones encontradas para " + matricula + ": " + resultados.size());
            return resultados;

        } catch (DataAccessException e) {
            System.err.println("❌ Error al obtener información del patrón asignado para: " + matricula);
            e.printStackTrace();
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error inesperado al obtener asignaciones para: " + matricula);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene la asignación activa actual de una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return Mapa con información de la asignación activa
     */
    public Map<String, Object> obtenerAsignacionActiva(String matricula) {
        try {
            String query = "SELECT a.id_empleado, a.fecha_asignacion, a.fecha_fin_asignacion, " +
                    "e.nombre AS nombre_patron, e.apellidos AS apellidos_patron " +
                    "FROM Asignacion a " +
                    "JOIN Empleado e ON a.id_empleado = e.id_empleado " +
                    "WHERE a.matricula_embarcacion = ? " +
                    "AND a.fecha_asignacion <= CURDATE() " +
                    "AND (a.fecha_fin_asignacion IS NULL OR a.fecha_fin_asignacion >= CURDATE()) " +
                    "ORDER BY a.fecha_asignacion DESC LIMIT 1";

            List<Map<String, Object>> resultados = jdbcTemplate.query(query,
                    new Object[] { matricula },
                    new RowMapper<Map<String, Object>>() {
                        public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Map<String, Object> info = new HashMap<>();
                            info.put("id_empleado", rs.getInt("id_empleado"));
                            info.put("nombre_patron", rs.getString("nombre_patron"));
                            info.put("apellidos_patron", rs.getString("apellidos_patron"));

                            java.sql.Date fechaAsignacionSql = rs.getDate("fecha_asignacion");
                            if (fechaAsignacionSql != null) {
                                info.put("fecha_asignacion", fechaAsignacionSql.toLocalDate());
                            } else {
                                info.put("fecha_asignacion", null);
                            }

                            java.sql.Date fechaFinSql = rs.getDate("fecha_fin_asignacion");
                            if (fechaFinSql != null) {
                                info.put("fecha_fin_asignacion", fechaFinSql.toLocalDate());
                            } else {
                                info.put("fecha_fin_asignacion", null);
                            }

                            return info;
                        }
                    });

            return resultados.isEmpty() ? null : resultados.get(0);

        } catch (DataAccessException e) {
            System.err.println("❌ Error al obtener asignación activa para: " + matricula);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifica si una embarcación tiene patrón asignado actualmente
     * 
     * @param matricula Matrícula de la embarcación
     * @return true si tiene patrón asignado, false en caso contrario
     */
    public boolean tienePatronAsignado(String matricula) {
        try {
            String query = "SELECT COUNT(*) FROM Asignacion " +
                    "WHERE matricula_embarcacion = ? " +
                    "AND fecha_asignacion <= CURDATE() " +
                    "AND (fecha_fin_asignacion IS NULL OR fecha_fin_asignacion >= CURDATE())";

            Integer count = jdbcTemplate.queryForObject(query, new Object[] { matricula }, Integer.class);
            boolean tienePatron = count != null && count > 0;
            System.out.println("🔍 Embarcación " + matricula + " tiene patrón asignado: " + tienePatron);
            return tienePatron;

        } catch (DataAccessException e) {
            System.err.println("Error verificando si la embarcación tiene patrón asignado: " + matricula);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un patrón ya está asignado actualmente
     * 
     * @param idEmpleado ID del empleado patrón
     * @return true si el patrón ya está asignado, false en caso contrario
     */
    public boolean patronYaAsignado(int idEmpleado) {
        try {
            String query = sqlQueries.getProperty("asignacion.patron.yaAsignado");
            if (query == null) {
                query = "SELECT COUNT(*) FROM Asignacion WHERE id_empleado = ? AND " +
                        "fecha_asignacion <= CURDATE() AND (fecha_fin_asignacion IS NULL OR fecha_fin_asignacion >= CURDATE())";
            }

            Integer count = jdbcTemplate.queryForObject(query, new Object[] { idEmpleado }, Integer.class);
            return count != null && count > 0;

        } catch (DataAccessException e) {
            System.err.println("Error verificando si el patrón ya está asignado");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un patrón tiene solapamiento en un período específico
     * 
     * @param idEmpleado  ID del empleado patrón
     * @param fechaInicio Fecha de inicio del período
     * @param fechaFin    Fecha de fin del período
     * @return true si hay solapamiento, false en caso contrario
     */
    public boolean patronTieneSolapamiento(int idEmpleado, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String query = "SELECT COUNT(*) FROM Asignacion WHERE id_empleado = ? AND " +
                    "((fecha_asignacion <= ? AND fecha_fin_asignacion >= ?) OR " +
                    "(fecha_asignacion >= ? AND fecha_fin_asignacion <= ?) OR " +
                    "(fecha_asignacion BETWEEN ? AND ?) OR " +
                    "(fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                    "(? BETWEEN fecha_asignacion AND fecha_fin_asignacion))";

            Integer count = jdbcTemplate.queryForObject(query,
                    new Object[] {
                            idEmpleado,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio, fechaFin,
                            fechaInicio
                    },
                    Integer.class);
            return count != null && count > 0;

        } catch (DataAccessException e) {
            System.err.println("Error verificando solapamiento del patrón");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene todos los patrones disponibles
     * 
     * @return Lista de empleados disponibles como patrones
     */
    public List<Empleados> obtenerPatronesDisponibles() {
        try {
            String query = sqlQueries.getProperty("empleados.obtener.todos");
            if (query == null) {
                query = "SELECT * FROM Empleado ORDER BY apellidos, nombre";
            }

            System.out.println("🔍 Ejecutando consulta para patrones disponibles: " + query);
            List<Empleados> empleados = jdbcTemplate.query(query, (rs, rowNum) -> mapEmpleado(rs));

            System.out.println("✅ Patrones disponibles encontrados: " + empleados.size());
            return empleados;

        } catch (DataAccessException e) {
            System.err.println("❌ Error obteniendo patrones disponibles: " + e.getMessage());
            try {
                String fallbackQuery = "SELECT * FROM Empleado";
                return jdbcTemplate.query(fallbackQuery, (rs, rowNum) -> mapEmpleado(rs));
            } catch (DataAccessException e2) {
                System.err.println("❌ Error incluso con consulta fallback");
                return List.of();
            }
        }
    }

    /**
     * Asigna un patrón a una embarcación con fechas específicas
     * 
     * @param matricula   Matrícula de la embarcación
     * @param idEmpleado  ID del empleado patrón
     * @param fechaInicio Fecha de inicio de la asignación
     * @param fechaFin    Fecha de fin de la asignación
     * @return true si la asignación fue exitosa, false en caso contrario
     */
    public boolean asignarPatron(String matricula, int idEmpleado, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String query = sqlQueries.getProperty("asignacion.insertar");
            if (query == null) {
                System.err.println("ERROR: Propiedad 'asignacion.insertar' no encontrada");
                return false;
            }

            int result = jdbcTemplate.update(query, matricula, idEmpleado, fechaInicio, fechaFin);
            System.out.println("✅ Asignación creada: " + result + " fila(s) afectada(s)");
            return result > 0;

        } catch (DataAccessException e) {
            System.err.println("Error asignando patrón a embarcación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina asignaciones solapadas para una embarcación
     * 
     * @param matricula   Matrícula de la embarcación
     * @param fechaInicio Fecha de inicio del período
     * @param fechaFin    Fecha de fin del período
     * @return true si la eliminación fue exitosa, false en caso contrario
     */
    public boolean eliminarAsignacionesSolapadas(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            String query = "DELETE FROM Asignacion WHERE matricula_embarcacion = ? AND " +
                    "((fecha_asignacion <= ? AND fecha_fin_asignacion >= ?) OR " +
                    "(fecha_asignacion >= ? AND fecha_fin_asignacion <= ?) OR " +
                    "(fecha_asignacion BETWEEN ? AND ?) OR " +
                    "(fecha_fin_asignacion BETWEEN ? AND ?) OR " +
                    "(? BETWEEN fecha_asignacion AND fecha_fin_asignacion))";

            int result = jdbcTemplate.update(query, matricula,
                    fechaInicio, fechaFin,
                    fechaInicio, fechaFin,
                    fechaInicio, fechaFin,
                    fechaInicio, fechaFin,
                    fechaInicio);

            System.out.println("🗑️ Eliminadas " + result + " asignaciones solapadas para " + matricula);
            return result >= 0;

        } catch (DataAccessException e) {
            System.err.println("Error eliminando asignaciones solapadas");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mapea un ResultSet a un objeto Empleados
     * 
     * @param rs ResultSet con los datos de la base de datos
     * @return Objeto Empleados mapeado
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private Empleados mapEmpleado(ResultSet rs) throws SQLException {
        Empleados emp = new Empleados();
        emp.setId(rs.getInt("id_empleado"));
        emp.setDni(rs.getString("dni"));
        emp.setNombre(rs.getString("nombre"));
        emp.setApellidos(rs.getString("apellidos"));

        if (rs.getDate("fecha_nacimiento") != null) {
            emp.setFech_nacimiento(rs.getDate("fecha_nacimiento").toLocalDate());
        }
        if (rs.getDate("fecha_expedicion_titulo") != null) {
            emp.setFech_expedicion_titulo(rs.getDate("fecha_expedicion_titulo").toLocalDate());
        }

        return emp;
    }
}