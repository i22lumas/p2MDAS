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

    private void verificarQueriesSQL() {
        if (this.sqlQueries == null) this.sqlQueries = cargarSqlProperties();
    }

    public List<Socio> obtenerTodosSocios() {
        try {
            return ejecutarObtenerTodosSocios();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todos los socios. " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private List<Socio> ejecutarObtenerTodosSocios() {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("socios.obtener.todos") != null
            ? sqlQueries.getProperty("socios.obtener.todos")
            : "SELECT * FROM Socios ORDER BY fecha_inscripcion DESC";
        return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
    }
    
    public int insertarSocioYRetornarId(Socio socio, int inscripcionId) { 
        try {
            return ejecutarInsertarSocioYRetornarId(socio, inscripcionId);
        } catch (DataAccessException excepcion) {
            System.err.println("Error añadiendo socio: " + excepcion.getMessage());
            excepcion.printStackTrace();
            throw excepcion;
        }
    }

    private int ejecutarInsertarSocioYRetornarId(Socio socio, int inscripcionId) {
        verificarQueriesSQL();
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
    }

    public Socio obtenerSocioPorDni(String dni) {
        try {
            return ejecutarObtenerSocioPorDni(dni);
        } catch (EmptyResultDataAccessException excepcion) {
            return null;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo socio por DNI: " + dni + ". " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private Socio ejecutarObtenerSocioPorDni(String dni) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("socios.buscar.dni") != null
            ? sqlQueries.getProperty("socios.buscar.dni")
            : "SELECT * FROM Socios WHERE dni = ?";
        return jdbcTemplate.queryForObject(query, new Object[] { dni }, (rs, rowNum) -> mapSocio(rs));
    }
    
    public Socio obtenerSocioPorId(int id) {
        try {
            return ejecutarObtenerSocioPorId(id);
        } catch (EmptyResultDataAccessException excepcion) {
            return null;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo socio por ID: " + id + ". " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private Socio ejecutarObtenerSocioPorId(int id) {
        String query = "SELECT * FROM Socios WHERE id_socio = ?";
        return jdbcTemplate.queryForObject(query, new Object[] { id }, (rs, rowNum) -> mapSocio(rs));
    }

    public boolean existeSocioPorDni(String dni) {
        try {
            return ejecutarExisteSocioPorDni(dni);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de DNI: " + dni + ". " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarExisteSocioPorDni(String dni) {
        String query = "SELECT count(*) FROM Socios WHERE dni = ?";
        Integer count = jdbcTemplate.queryForObject(query, new Object[] { dni }, Integer.class);
        return count != null && count > 0;
    }
    
    public boolean actualizarDatosPersonalesSocio(Socio socio) {
        try {
            return ejecutarActualizarDatosPersonales(socio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando datos personales del socio DNI: " + socio.getDni());
            throw excepcion;
        }
    }

    private boolean ejecutarActualizarDatosPersonales(Socio socio) {
        final String query = "UPDATE Socios SET nombre=?, apellidos=?, direccion=?, fecha_nacimiento=?, tiene_titulo_patron=? WHERE dni=?";
        int filasAfectadas = jdbcTemplate.update(query, 
            socio.getNombre(), 
            socio.getApellidos(), 
            socio.getDireccion(), 
            socio.getFechaNacimiento(), 
            socio.getTieneTituloPatron(), 
            socio.getDni());
        return filasAfectadas > 0;
    }
    
    public boolean actualizarInscripcionIdSocio(int idSocio, int idInscripcion) {
        try {
            return ejecutarActualizarInscripcionIdSocio(idSocio, idInscripcion);
        } catch (DataAccessException excepcion) {
            System.err.println("Error al actualizar inscripcion_id para Socio ID: " + idSocio);
            throw excepcion;
        }
    }

    private boolean ejecutarActualizarInscripcionIdSocio(int idSocio, int idInscripcion) {
        final String query = "UPDATE Socios SET inscripcion_id = ? WHERE id_socio = ?";
        int filasAfectadas = jdbcTemplate.update(query, idInscripcion, idSocio);
        return filasAfectadas > 0;
    }
    
    public boolean actualizarIdSocioTitular(int idSocio, int idSocioTitularFk) {
        try {
            return ejecutarActualizarIdSocioTitular(idSocio, idSocioTitularFk);
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando id_socio_titular_fk para Socio ID: " + idSocio + ". " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarActualizarIdSocioTitular(int idSocio, int idSocioTitularFk) {
        String query = "UPDATE Socios SET id_socio_titular_fk = ? WHERE id_socio = ?";
        int filasAfectadas = jdbcTemplate.update(query, idSocioTitularFk, idSocio);
        return filasAfectadas > 0;
    }

    public boolean vincularSocioAInscripcion(Socio socio) {
         try {
            return ejecutarVincularSocioAInscripcion(socio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error al vincular socio ID " + socio.getId());
            throw excepcion;
        }
    }

    private boolean ejecutarVincularSocioAInscripcion(Socio socio) {
        final String query = "UPDATE Socios SET inscripcion_id=?, id_socio_titular_fk=?, tipo_miembro=? WHERE id_socio=?";
        int filasAfectadas = jdbcTemplate.update(query, 
            socio.getInscripcionId(), 
            socio.getIdSocioTitularFk(), 
            socio.getTipoMiembro().toString(),
            socio.getId());
        return filasAfectadas > 0;
    }

    public boolean desvincularSocioDeInscripcion(int idSocio) {
        try {
            return ejecutarDesvincularSocioDeInscripcion(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error al desvincular socio ID " + idSocio);
            throw excepcion;
        }
    }

    private boolean ejecutarDesvincularSocioDeInscripcion(int idSocio) {
        final String query = "UPDATE Socios SET inscripcion_id=NULL, id_socio_titular_fk=NULL, tipo_miembro='TITULAR' WHERE id_socio=?";
        int filasAfectadas = jdbcTemplate.update(query, idSocio);
        return filasAfectadas > 0;
    }
    
    public boolean desvincularTodosSociosDeInscripcion(int idInscripcion) {
        try {
            return ejecutarDesvincularTodosSocios(idInscripcion);
        } catch (DataAccessException excepcion) {
            System.err.println("Error al desvincular todos los socios de la inscripción ID " + idInscripcion);
            throw excepcion;
        }
    }

    private boolean ejecutarDesvincularTodosSocios(int idInscripcion) {
        final String query = "UPDATE Socios SET inscripcion_id=NULL, id_socio_titular_fk=NULL, tipo_miembro='TITULAR' WHERE inscripcion_id=?";
        int filasAfectadas = jdbcTemplate.update(query, idInscripcion);
        return filasAfectadas > 0;
    }
    
    public boolean eliminarSocio(int idSocio) {
        try {
            return ejecutarEliminarSocio(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando socio ID: " + idSocio);
            throw excepcion;
        }
    }

    private boolean ejecutarEliminarSocio(int idSocio) {
        final String query = "DELETE FROM Socios WHERE id_socio = ?";
        int filasAfectadas = jdbcTemplate.update(query, idSocio);
        return filasAfectadas > 0;
    }
    
    public List<Socio> obtenerSociosTitulares() {
        try {
            return ejecutarObtenerSociosTitulares();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo solo los socios titulares.");
            excepcion.printStackTrace();
            throw excepcion;
        }
    }

    private List<Socio> ejecutarObtenerSociosTitulares() {
        String query = "SELECT * FROM Socios WHERE tipo_miembro = 'TITULAR' AND inscripcion_id IS NOT NULL ORDER BY apellidos, nombre";
        return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
    }
    
    public List<Socio> obtenerMiembrosSinTituloPatron() {
        try {
            return ejecutarObtenerMiembrosSinTituloPatron();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo miembros sin título de patrón.");
            excepcion.printStackTrace();
            throw excepcion;
        }
    }

    private List<Socio> ejecutarObtenerMiembrosSinTituloPatron() {
        verificarQueriesSQL();
        String query = "SELECT * FROM Socios WHERE tiene_titulo_patron = FALSE AND tipo_miembro IN ('TITULAR', 'CONYUGE') ORDER BY tipo_miembro, apellidos";
        return jdbcTemplate.query(query, (rs, rowNum) -> mapSocio(rs));
    }
    
    public boolean otorgarTituloPatron(String dni) {
        try {
            return actualizarTituloPatron(dni, true);
        } catch (DataAccessException excepcion) {
            System.err.println("Error otorgando título de patrón para DNI: " + dni);
            throw excepcion;
        }
    }

    public boolean revocarTituloPatron(String dni) {
        try {
            return actualizarTituloPatron(dni, false);
        } catch (DataAccessException excepcion) {
            System.err.println("Error revocando título de patrón para DNI: " + dni);
            throw excepcion;
        }
    }

    private boolean actualizarTituloPatron(String dni, boolean tieneTitulo) {
        String query = "UPDATE Socios SET tiene_titulo_patron = ? WHERE dni = ?";
        int filasAfectadas = jdbcTemplate.update(query, tieneTitulo, dni);
        return filasAfectadas > 0;
    }
}