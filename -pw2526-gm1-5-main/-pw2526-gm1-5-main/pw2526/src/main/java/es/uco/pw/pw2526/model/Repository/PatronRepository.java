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

    /**
     * Inserta un nuevo patrón en la base de datos
     * 
     * @param patron Objeto Patron a insertar
     * @return true si la inserción fue exitosa, false en caso contrario
     */
    public boolean insertarPatron(Patron patron) {
        try {
            String query = sqlQueries.getProperty("empleados.insertar");
            if (query == null) {
                query = "INSERT INTO Empleado (dni, nombre, apellidos, fecha_nacimiento, fecha_expedicion_titulo) VALUES (?, ?, ?, ?, ?)";
            }

            System.out.println("🔍 Insertando patrón: " + patron.getNombre() + " " + patron.getApellidos());

            int filasAfectadas = jdbcTemplate.update(query,
                    patron.getDni(),
                    patron.getNombre(),
                    patron.getApellidos(),
                    patron.getFechaNacimiento(),
                    patron.getFechaExpedicionTitulo());

            System.out.println("✅ Resultado de inserción: " + filasAfectadas + " filas afectadas");
            return filasAfectadas > 0;

        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error añadiendo patrón: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene un patrón por su ID
     * 
     * @param id ID del patrón a buscar
     * @return Objeto Patron encontrado, null si no existe
     */
    public Patron obtenerPatronPorId(int id) {
        try {
            String query = "SELECT * FROM Empleado WHERE id_empleado = ?";

            List<Patron> patrones = jdbcTemplate.query(query,
                    new Object[] { id },
                    (rs, rowNum) -> mapPatron(rs));

            return patrones.isEmpty() ? null : patrones.get(0);

        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrón por ID: " + id);
            excepcion.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene todos los patrones registrados
     * 
     * @return Lista de todos los patrones
     */
    public List<Patron> obtenerPatrones() {
        try {
            String query = sqlQueries.getProperty("empleados.obtener.todos");
            if (query == null) {
                query = "SELECT * FROM Empleado ORDER BY apellidos, nombre";
            }

            return jdbcTemplate.query(query, (rs, rowNum) -> mapPatron(rs));
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrones");
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene los patrones disponibles (no asignados como patrones)
     * 
     * @return Lista de patrones disponibles
     */
    public List<Patron> obtenerPatronesDisponibles() {
        try {
            String query = sqlQueries.getProperty("empleados.obtener.disponibles");
            if (query == null) {
                query = "SELECT e.* FROM Empleado e WHERE e.id_empleado NOT IN (SELECT emb.id_patron_asignado FROM Embarcaciones emb WHERE emb.id_patron_asignado IS NOT NULL)";
            }

            return jdbcTemplate.query(query, (rs, rowNum) -> mapPatron(rs));

        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error obteniendo patrones disponibles: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Actualiza los campos de información de un patrón, excepto el DNI
     * 
     * @param patron Objeto Patron con los datos actualizados
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    public boolean actualizarPatron(Patron patron) {
        try {
            Patron patronActual = obtenerPatronPorId(patron.getId());
            if (patronActual == null) {
                System.err.println("El patrón con ID " + patron.getId() + " no existe");
                return false;
            }

            if (!patronActual.getDni().equals(patron.getDni())) {
                System.err.println("No se puede modificar el DNI de un patrón");
                return false;
            }

            String query = "UPDATE Empleado SET nombre = ?, apellidos = ?, fecha_nacimiento = ?, fecha_expedicion_titulo = ? WHERE id_empleado = ?";

            int filasAfectadas = jdbcTemplate.update(query,
                    patron.getNombre(),
                    patron.getApellidos(),
                    patron.getFechaNacimiento(),
                    patron.getFechaExpedicionTitulo(),
                    patron.getId());

            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando patrón: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un patrón si no está vinculado con ninguna embarcación
     * 
     * @param id ID del patrón a eliminar
     * @return true si la eliminación fue exitosa, false en caso contrario
     */
    public boolean eliminarPatron(int id) {
        try {
            String checkQuery = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, id);

            if (count != null && count > 0) {
                System.err.println("No se puede eliminar el patrón, está asignado a " + count + " embarcaciones");
                return false;
            }

            String deleteQuery = "DELETE FROM Empleado WHERE id_empleado = ?";
            int filasAfectadas = jdbcTemplate.update(deleteQuery, id);

            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando patrón: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un patrón existe por su ID
     * 
     * @param id ID del patrón
     * @return true si existe, false si no
     */
    public boolean existePatron(int id) {
        try {
            String query = "SELECT COUNT(*) FROM Empleado WHERE id_empleado = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de patrón: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un patrón existe por su DNI
     * 
     * @param dni DNI del patrón
     * @return true si existe, false si no
     */
    public boolean existePatronPorDNI(String dni) {
        try {
            String query = "SELECT COUNT(*) FROM Empleado WHERE dni = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, dni);
            return count != null && count > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de patrón por DNI: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Mapea un ResultSet a un objeto Patron
     * 
     * @param rs ResultSet con los datos de la base de datos
     * @return Objeto Patron mapeado
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
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

    /**
     * Verifica si un patrón tiene asignación a embarcación
     * 
     * @param id ID del patrón
     * @return true si está asignado a alguna embarcación, false si no
     */
    public boolean tienePatronAsignado(int id) {
        try {
            String query = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando si patrón tiene asignación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

}
