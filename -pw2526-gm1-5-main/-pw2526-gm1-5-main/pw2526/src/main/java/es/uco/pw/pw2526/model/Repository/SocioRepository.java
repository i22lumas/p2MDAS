package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.Socio.Socio;
import es.uco.pw.pw2526.model.domain.Socio.Socio.TipoMiembro;

@Repository
public class SocioRepository extends AbstractRepository {

    public SocioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        String sqlQueriesFileName = "./src/main/resources/db/sql.properties";
        this.setSQLQueriesFileName(sqlQueriesFileName);
        this.sqlQueries = cargarSqlProperties();
    }

    private Socio mapSocio(ResultSet rs) throws SQLException {
        Socio socio = new Socio();
        
        socio.setId(rs.getInt("id_socio")); 
        
        int inscripcionId = rs.getInt("inscripcion_id");
        if (rs.wasNull()) {
            socio.setInscripcionId(-1);
        } else {
            socio.setInscripcionId(inscripcionId); 
        }
        
        socio.setDni(rs.getString("dni"));
        socio.setNombre(rs.getString("nombre"));
        socio.setApellidos(rs.getString("apellidos"));
        socio.setDireccion(rs.getString("direccion"));
        
        socio.setTieneTituloPatron(rs.getBoolean("tiene_titulo_patron")); 
        
        socio.setTipoMiembro(TipoMiembro.valueOf(rs.getString("tipo_miembro"))); 
        
        socio.setIdSocioTitularFk(rs.getInt("id_socio_titular_fk"));
        if (rs.wasNull()) {
            socio.setIdSocioTitularFk(null);
        }

        if (rs.getDate("fecha_nacimiento") != null) {
            socio.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());
        }
        if (rs.getDate("fecha_inscripcion") != null) { 
            socio.setFechaInscripcion(rs.getDate("fecha_inscripcion").toLocalDate());
        }
        
        return socio;
    }

    public List<Socio> obtenerTodosSocios() {
        try {
            if (this.sqlQueries == null) this.sqlQueries = cargarSqlProperties();
            
            String query = sqlQueries.getProperty("socios.obtener.todos") != null
                ? sqlQueries.getProperty("socios.obtener.todos")
                : "SELECT * FROM Socios ORDER BY fecha_inscripcion DESC";
            
            return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todos los socios. " + excepcion.getMessage());
            return List.of();
        }
    }
    
    public int insertarSocioYRetornarId(Socio socio, int inscripcionId) { 
        try {
            if (this.sqlQueries == null) this.sqlQueries = cargarSqlProperties();

            final String query = "INSERT INTO Socios (dni, nombre, apellidos, fecha_nacimiento, direccion, fecha_inscripcion, tiene_titulo_patron, tipo_miembro, inscripcion_id, id_socio_titular_fk) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, socio.getDni());
                ps.setString(2, socio.getNombre());
                ps.setString(3, socio.getApellidos());
                ps.setObject(4, socio.getFechaNacimiento());
                ps.setString(5, socio.getDireccion());
                ps.setObject(6, socio.getFechaInscripcion());
                ps.setBoolean(7, socio.getTieneTituloPatron());
                ps.setString(8, socio.getTipoMiembro().toString()); 
                
                if (inscripcionId != -1) { 
                    ps.setInt(9, inscripcionId);
                } else {
                    ps.setNull(9, Types.INTEGER);
                }
                
                if (socio.getIdSocioTitularFk() != null) { 
                    ps.setInt(10, socio.getIdSocioTitularFk()); 
                } else {
                    ps.setNull(10, Types.INTEGER);
                }
                return ps;
            }, keyHolder);

            return keyHolder.getKey() != null ? keyHolder.getKey().intValue() : -1;

        } catch (DataAccessException excepcion) {
            System.err.println("Error añadiendo socio: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return -1;
        }
    }

    public Socio obtenerSocioPorDni(String dni) {
        try {
            if (this.sqlQueries == null) this.sqlQueries = cargarSqlProperties();
            
            String query = sqlQueries.getProperty("socios.buscar.dni") != null
                ? sqlQueries.getProperty("socios.buscar.dni")
                : "SELECT * FROM Socios WHERE dni = ?";

            return jdbcTemplate.queryForObject(query, 
                    new Object[] { dni }, 
                    (rs, rowNum) -> mapSocio(rs));

        } catch (EmptyResultDataAccessException excepcion) {
            return null;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo socio por DNI: " + dni + ". " + excepcion.getMessage());
            return null;
        }
    }
    
    public Socio obtenerSocioPorId(int id) {
        try {
            String query = "SELECT * FROM Socios WHERE id_socio = ?";
            
            return jdbcTemplate.queryForObject(query, 
                    new Object[] { id }, 
                    (rs, rowNum) -> mapSocio(rs));

        } catch (EmptyResultDataAccessException excepcion) {
            return null;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo socio por ID: " + id + ". " + excepcion.getMessage());
            return null;
        }
    }

    public boolean existeSocioPorDni(String dni) {
        try {
            String query = "SELECT count(*) FROM Socios WHERE dni = ?";
            Integer count = jdbcTemplate.queryForObject(query, new Object[] { dni }, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de DNI: " + dni + ". " + excepcion.getMessage());
            return false;
        }
    }
    
    public boolean actualizarDatosPersonalesSocio(Socio socio) {
        try {
            final String query = "UPDATE Socios SET nombre=?, apellidos=?, direccion=?, fecha_nacimiento=?, tiene_titulo_patron=? WHERE dni=?";
            int filasAfectadas = jdbcTemplate.update(query, 
                socio.getNombre(), 
                socio.getApellidos(), 
                socio.getDireccion(), 
                socio.getFechaNacimiento(), 
                socio.getTieneTituloPatron(), 
                socio.getDni());
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando datos personales del socio DNI: " + socio.getDni());
            return false;
        }
    }
    
    public boolean actualizarInscripcionIdSocio(int idSocio, int idInscripcion) {
        try {
            final String query = "UPDATE Socios SET inscripcion_id = ? WHERE id_socio = ?";
            int filasAfectadas = jdbcTemplate.update(query, idInscripcion, idSocio);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error al actualizar inscripcion_id para Socio ID: " + idSocio);
            return false;
        }
    }
    
    public boolean actualizarIdSocioTitular(int idSocio, int idSocioTitularFk) {
        try {
            String query = "UPDATE Socios SET id_socio_titular_fk = ? WHERE id_socio = ?";
            int filasAfectadas = jdbcTemplate.update(query, idSocioTitularFk, idSocio);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando id_socio_titular_fk para Socio ID: " + idSocio + ". " + excepcion.getMessage());
            return false;
        }
    }

    public boolean vincularSocioAInscripcion(int idSocio, int idInscripcion, int idSocioTitularFk, TipoMiembro tipoMiembro) {
         try {
            final String query = "UPDATE Socios SET inscripcion_id=?, id_socio_titular_fk=?, tipo_miembro=? WHERE id_socio=?";
            int filasAfectadas = jdbcTemplate.update(query, 
                idInscripcion, 
                idSocioTitularFk, 
                tipoMiembro.toString(),
                idSocio);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error al vincular socio ID " + idSocio + " a inscripción " + idInscripcion);
            return false;
        }
    }

    public boolean desvincularSocioDeInscripcion(int idSocio) {
        try {
            final String query = "UPDATE Socios SET inscripcion_id=NULL, id_socio_titular_fk=NULL, tipo_miembro='TITULAR' WHERE id_socio=?";
            int filasAfectadas = jdbcTemplate.update(query, idSocio);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error al desvincular socio ID " + idSocio);
            return false;
        }
    }
    
    public boolean desvincularTodosSociosDeInscripcion(int idInscripcion) {
        try {
            final String query = "UPDATE Socios SET inscripcion_id=NULL, id_socio_titular_fk=NULL, tipo_miembro='TITULAR' WHERE inscripcion_id=?";
            int filasAfectadas = jdbcTemplate.update(query, idInscripcion);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error al desvincular todos los socios de la inscripción ID " + idInscripcion);
            return false;
        }
    }
    
    public boolean eliminarSocio(int idSocio) {
        try {
            final String query = "DELETE FROM Socios WHERE id_socio = ?";
            int filasAfectadas = jdbcTemplate.update(query, idSocio);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando socio ID: " + idSocio);
            return false;
        }
    }
    
    public List<Socio> obtenerSociosTitulares() {
        try {
            String query = "SELECT * FROM Socios WHERE tipo_miembro = 'TITULAR' AND inscripcion_id IS NOT NULL ORDER BY apellidos, nombre";
            return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo solo los socios titulares.");
            excepcion.printStackTrace();
            return List.of();
        }
    }
    
    public List<Socio> obtenerMiembrosSinTituloPatron() {
        try {
            if (this.sqlQueries == null) this.sqlQueries = cargarSqlProperties();
            
            String query = "SELECT * FROM Socios WHERE tiene_titulo_patron = FALSE AND tipo_miembro IN ('TITULAR', 'CONYUGE') ORDER BY tipo_miembro, apellidos";
            return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo miembros sin título de patrón.");
            excepcion.printStackTrace();
            return List.of();
        }
    }
    
    public boolean actualizarTituloPatron(String dni, boolean tieneTitulo) {
        try {
            String query = "UPDATE Socios SET tiene_titulo_patron = ? WHERE dni = ?";
            int filasAfectadas = jdbcTemplate.update(query, tieneTitulo, dni);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando título de patrón para DNI: " + dni + ". " + excepcion.getMessage());
            return false;
        }
    }
}