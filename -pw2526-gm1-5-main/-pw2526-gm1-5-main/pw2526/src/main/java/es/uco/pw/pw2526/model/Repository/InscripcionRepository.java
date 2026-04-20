package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion;
import es.uco.pw.pw2526.model.domain.inscripcion.Inscripcion.TipoInscripcion;

@Repository
public class InscripcionRepository extends AbstractRepository {
    
    private final SocioRepository socioRepository; 

    public InscripcionRepository(JdbcTemplate jdbcTemplate, SocioRepository socioRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.socioRepository = socioRepository;
    }

    private Inscripcion mapInscripcion(ResultSet rs, int rowNum) throws SQLException {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(rs.getInt("id_inscripcion"));
        inscripcion.setIdSocioTitular(rs.getInt("id_socio_titular"));
        inscripcion.setCuotaAnual(rs.getDouble("cuota_anual"));

        String tipo = rs.getString("tipo_inscripcion");
        inscripcion.setTipoInscripcion(TipoInscripcion.valueOf(tipo));

        if (rs.getDate("fecha_creacion") != null) {
            inscripcion.setFechaCreacion(rs.getDate("fecha_creacion").toLocalDate());
        }
        return inscripcion;
    }

    public int insertarInscripcion(Inscripcion inscripcion) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            final String query = sqlQueries.getProperty("inscripciones.insertar") != null
                    ? sqlQueries.getProperty("inscripciones.insertar")
                    : "INSERT INTO Inscripciones (id_socio_titular, tipo_inscripcion, cuota_anual, fecha_creacion) VALUES (?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, inscripcion.getIdSocioTitular());
                ps.setString(2, inscripcion.getTipoInscripcion().toString());
                ps.setDouble(3, inscripcion.getCuotaAnual());
                ps.setObject(4, inscripcion.getFechaCreacion());
                return ps;
            }, keyHolder);

            return keyHolder.getKey() != null ? keyHolder.getKey().intValue() : -1;

        } catch (DataAccessException excepcion) {
            System.err.println("Error añadiendo inscripción: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return -1;
        }
    }
    
    public boolean actualizarSocioTitular(int inscripcionId, int idSocioTitular) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();
                
            final String query = "UPDATE Inscripciones SET id_socio_titular = ? WHERE id_inscripcion = ?";

            int filasAfectadas = jdbcTemplate.update(query, idSocioTitular, inscripcionId);

            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error al actualizar el socio titular de la inscripción: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }


    public boolean actualizarInscripcion(Inscripcion inscripcion) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();
            
            final String query = "UPDATE Inscripciones SET cuota_anual = ?, tipo_inscripcion = ? WHERE id_inscripcion = ?";

            int filasAfectadas = jdbcTemplate.update(query,
                    inscripcion.getCuotaAnual(),
                    inscripcion.getTipoInscripcion().toString(),
                    inscripcion.getId());

            return filasAfectadas > 0;

        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando inscripción: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    public Inscripcion obtenerInscripcionPorId(int idInscripcion) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            final String query = sqlQueries.getProperty("inscripciones.buscar.id") != null
                    ? sqlQueries.getProperty("inscripciones.buscar.id")
                    : "SELECT * FROM Inscripciones WHERE id_inscripcion = ?";

            return jdbcTemplate.queryForObject(query,
                    new Object[] { idInscripcion },
                    this::mapInscripcion);

        } catch (EmptyResultDataAccessException excepcion) {
            return null;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo inscripción por ID: " + idInscripcion);
            return null;
        }
    }
    
    public List<Map<String, Object>> obtenerDetallesInscripcionesPorTipo(String tipoInscripcion) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();
                
            String query = "SELECT I.id_inscripcion, I.cuota_anual, I.tipo_inscripcion, " +
                    "S.dni AS dni_titular, " +
                    "CONCAT(S.nombre, ' ', S.apellidos) AS nombre_titular, " +
                    "COALESCE(COUNT(F.id_socio), 0) AS numero_familiares " + 
                    "FROM Inscripciones I " +
                    "JOIN Socios S ON I.id_socio_titular = S.id_socio " +
                    "LEFT JOIN Socios F ON I.id_inscripcion = F.inscripcion_id AND F.tipo_miembro <> 'TITULAR' " + 
                    "WHERE I.tipo_inscripcion = ? " +
                    "GROUP BY I.id_inscripcion, I.cuota_anual, I.tipo_inscripcion, S.dni, S.nombre, S.apellidos " + 
                    "ORDER BY I.id_inscripcion";

            return jdbcTemplate.query(query, new Object[]{tipoInscripcion}, (rs, rowNum) -> {
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("id_inscripcion", rs.getInt("id_inscripcion"));
                detalle.put("cuota_anual", rs.getDouble("cuota_anual"));
                detalle.put("tipo_inscripcion", rs.getString("tipo_inscripcion")); 
                detalle.put("dni_titular", rs.getString("dni_titular"));
                detalle.put("nombre_titular", rs.getString("nombre_titular"));
                detalle.put("numero_familiares", rs.getInt("numero_familiares"));
                return detalle;
            });

        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo el listado detallado de inscripciones por tipo: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return List.of();
        }
    }
    
    public List<Map<String, Object>> obtenerDetallesInscripciones() {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = "SELECT I.id_inscripcion, I.cuota_anual, I.tipo_inscripcion, " +
                    "S.dni AS dni_titular, " +
                    "CONCAT(S.nombre, ' ', S.apellidos) AS nombre_titular, " +
                    "COALESCE(COUNT(F.id_socio), 0) AS numero_familiares " + 
                    "FROM Inscripciones I " +
                    "JOIN Socios S ON I.id_socio_titular = S.id_socio " +
                    "LEFT JOIN Socios F ON I.id_inscripcion = F.inscripcion_id AND F.tipo_miembro <> 'TITULAR' " + 
                    "GROUP BY I.id_inscripcion, I.cuota_anual, I.tipo_inscripcion, S.dni, S.nombre, S.apellidos " + 
                    "ORDER BY I.id_inscripcion";

            return jdbcTemplate.query(query, (rs, rowNum) -> {
                Map<String, Object> detalle = new HashMap<>();

                detalle.put("id_inscripcion", rs.getInt("id_inscripcion"));
                detalle.put("cuota_anual", rs.getDouble("cuota_anual"));
                detalle.put("tipo_inscripcion", rs.getString("tipo_inscripcion")); 
                detalle.put("dni_titular", rs.getString("dni_titular"));
                detalle.put("nombre_titular", rs.getString("nombre_titular"));
                detalle.put("numero_familiares", rs.getInt("numero_familiares"));

                return detalle;
            });

        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo el listado detallado de inscripciones: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return List.of();
        }
    }

    public Inscripcion obtenerInscripcionPorDniTitular(String dniTitular) {
        try {
            Socio socio = socioRepository.obtenerSocioPorDni(dniTitular); 
            if (socio == null || !socio.esTitular() || socio.getInscripcionId() == -1) {
                return null;
            }
            return obtenerInscripcionPorId(socio.getInscripcionId());

        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo inscripción por DNI del titular: " + dniTitular);
            return null;
        }
    }
    
    public boolean cancelarInscripcion(int idInscripcion) {
        try {
            final String query = "DELETE FROM Inscripciones WHERE id_inscripcion = ?";
            int filasAfectadas = jdbcTemplate.update(query, idInscripcion);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error cancelando/eliminando inscripción ID: " + idInscripcion);
            return false;
        }
    }
}