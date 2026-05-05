package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.Empleados.Patron;

@Repository
public class PatronRepository extends AbstractRepository {

    public PatronRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlQueries = cargarSqlProperties();
    }

    private void verificarQueriesSQL() {
        if (this.sqlQueries == null) {
            this.sqlQueries = cargarSqlProperties();
        }
    }

    public boolean insertarPatron(Patron patron) {
        try {
            return ejecutarInsertarPatron(patron);
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error añadiendo patrón: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarInsertarPatron(Patron patron) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("empleados.insertar");
        if (query == null) {
            query = "INSERT INTO Empleado (dni, nombre, apellidos, fecha_nacimiento, fecha_expedicion_titulo) VALUES (?, ?, ?, ?, ?)";
        }

        int filasAfectadas = jdbcTemplate.update(query,
                patron.getDni(),
                patron.getNombre(),
                patron.getApellidos(),
                patron.getFechaNacimiento(),
                patron.getFechaExpedicionTitulo());

        return filasAfectadas > 0;
    }

    public Patron obtenerPatronPorId(int id) {
        try {
            return ejecutarObtenerPatronPorId(id);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrón por ID: " + id);
            throw excepcion;
        }
    }

    private Patron ejecutarObtenerPatronPorId(int id) {
        String query = "SELECT * FROM Empleado WHERE id_empleado = ?";
        List<Patron> patrones = jdbcTemplate.query(query, new Object[] { id }, (rs, rowNum) -> mapPatron(rs));
        return patrones.isEmpty() ? null : patrones.get(0);
    }

    public List<Patron> obtenerPatrones() {
        try {
            return ejecutarObtenerPatrones();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrones");
            throw excepcion;
        }
    }

    private List<Patron> ejecutarObtenerPatrones() {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("empleados.obtener.todos");
        if (query == null) {
            query = "SELECT * FROM Empleado ORDER BY apellidos, nombre";
        }
        return jdbcTemplate.query(query, (rs, rowNum) -> mapPatron(rs));
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
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("empleados.obtener.disponibles");
        if (query == null) {
            query = "SELECT e.* FROM Empleado e WHERE e.id_empleado NOT IN (SELECT emb.id_patron_asignado FROM Embarcaciones emb WHERE emb.id_patron_asignado IS NOT NULL)";
        }
        return jdbcTemplate.query(query, (rs, rowNum) -> mapPatron(rs));
    }

    public boolean actualizarPatron(Patron patron) {
        try {
            return ejecutarActualizarPatron(patron);
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando patrón: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarActualizarPatron(Patron patron) {
        Patron patronActual = obtenerPatronPorId(patron.getId());
        if (patronActual == null) {
            throw new RuntimeException("El patrón con ID " + patron.getId() + " no existe");
        }
        if (!patronActual.getDni().equals(patron.getDni())) {
            throw new RuntimeException("No se puede modificar el DNI de un patrón");
        }

        String query = "UPDATE Empleado SET nombre = ?, apellidos = ?, fecha_nacimiento = ?, fecha_expedicion_titulo = ? WHERE id_empleado = ?";
        int filasAfectadas = jdbcTemplate.update(query,
                patron.getNombre(),
                patron.getApellidos(),
                patron.getFechaNacimiento(),
                patron.getFechaExpedicionTitulo(),
                patron.getId());

        return filasAfectadas > 0;
    }

    public boolean eliminarPatron(int id) {
        try {
            return ejecutarEliminarPatron(id);
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando patrón: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarEliminarPatron(int id) {
        String checkQuery = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
        Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, id);

        if (count != null && count > 0) {
            throw new RuntimeException("No se puede eliminar el patrón, está asignado a " + count + " embarcaciones");
        }

        String deleteQuery = "DELETE FROM Empleado WHERE id_empleado = ?";
        int filasAfectadas = jdbcTemplate.update(deleteQuery, id);
        return filasAfectadas > 0;
    }

    public boolean existePatron(int id) {
        try {
            return ejecutarExistePatron(id);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de patrón: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarExistePatron(int id) {
        String query = "SELECT COUNT(*) FROM Empleado WHERE id_empleado = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
        return count != null && count > 0;
    }

    public boolean existePatronPorDNI(String dni) {
        try {
            return ejecutarExistePatronPorDNI(dni);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de patrón por DNI: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarExistePatronPorDNI(String dni) {
        String query = "SELECT COUNT(*) FROM Empleado WHERE dni = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, dni);
        return count != null && count > 0;
    }

    public boolean tienePatronAsignado(int id) {
        try {
            return ejecutarTienePatronAsignado(id);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando si patrón tiene asignación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarTienePatronAsignado(int id) {
        String query = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
        return count != null && count > 0;
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
