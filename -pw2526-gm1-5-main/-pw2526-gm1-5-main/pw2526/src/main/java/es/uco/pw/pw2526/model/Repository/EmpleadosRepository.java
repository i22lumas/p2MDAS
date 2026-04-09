package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.Empleados.Empleados;

@Repository
public class EmpleadosRepository extends AbstractRepository {

    /**
     * Constructor del repositorio de empleados
     * 
     * @param jdbcTemplate Plantilla JDBC para operaciones de base de datos
     */
    public EmpleadosRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlQueries = cargarSqlProperties();
    }

    /**
     * Añade un nuevo empleado a la base de datos
     * 
     * @param empleado Objeto Empleados a insertar
     * @return true si la inserción fue exitosa, false en caso contrario
     */
    public boolean addEmpleados(Empleados empleado) {
        try {
            String query = sqlQueries.getProperty("empleados.insertar");
            if (query == null) {
                query = "INSERT INTO Empleado (dni, nombre, apellidos, fecha_nacimiento, fecha_expedicion_titulo) VALUES (?, ?, ?, ?, ?)";
            }

            System.out.println("🔍 Insertando empleado: " + empleado.getNombre() + " " + empleado.getApellidos());

            int result = jdbcTemplate.update(query,
                    empleado.getDni(),
                    empleado.getNombre(),
                    empleado.getApellidos(),
                    empleado.getFech_nacimiento(),
                    empleado.getFech_expedicion_titulo());

            System.out.println("✅ Resultado de inserción: " + result + " filas afectadas");
            return result > 0;

        } catch (DataAccessException e) {
            System.err.println("❌ Error añadiendo empleado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene un empleado por su ID
     * 
     * @param id ID del empleado a buscar
     * @return Objeto Empleados encontrado, null si no existe
     */
    public Empleados obtenerEmpleadoPorId(int id) {
        try {
            String query = "SELECT * FROM Empleado WHERE id_empleado = ?";

            List<Empleados> empleados = jdbcTemplate.query(query,
                    new Object[] { id },
                    (rs, rowNum) -> mapEmpleado(rs));

            return empleados.isEmpty() ? null : empleados.get(0);

        } catch (DataAccessException e) {
            System.err.println("Error obteniendo empleado por ID: " + id);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene todos los empleados registrados
     * 
     * @return Lista de todos los empleados
     */
    public List<Empleados> obtenerEmpleados() {
        try {
            String query = sqlQueries.getProperty("empleados.obtener.todos");
            if (query == null) {
                query = "SELECT * FROM Empleado ORDER BY apellidos, nombre";
            }

            return jdbcTemplate.query(query, (rs, rowNum) -> mapEmpleado(rs));
        } catch (DataAccessException e) {
            System.err.println("Error obteniendo empleados");
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene los empleados disponibles (no asignados como patrones)
     * 
     * @return Lista de empleados disponibles
     */
    public List<Empleados> obtenerEmpleadosDisponibles() {
        try {
            String query = sqlQueries.getProperty("empleados.obtener.disponibles");
            if (query == null) {
                query = "SELECT e.* FROM Empleado e WHERE e.id_empleado NOT IN (SELECT emb.id_patron_asignado FROM Embarcaciones emb WHERE emb.id_patron_asignado IS NOT NULL)";
            }

            return jdbcTemplate.query(query, (rs, rowNum) -> mapEmpleado(rs));

        } catch (DataAccessException e) {
            System.err.println("❌ Error obteniendo empleados disponibles: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Actualiza los campos de información de un patrón/empleado, excepto el DNI
     * 
     * @param empleado Objeto Empleados con los datos actualizados
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    public boolean actualizarEmpleado(Empleados empleado) {
        try {
            // Primero obtenemos el empleado actual para verificar el DNI
            Empleados empleadoActual = obtenerEmpleadoPorId(empleado.getId());
            if (empleadoActual == null) {
                System.err.println("El empleado con ID " + empleado.getId() + " no existe");
                return false;
            }

            // Verificamos que el DNI no se haya modificado
            if (!empleadoActual.getDni().equals(empleado.getDni())) {
                System.err.println("No se puede modificar el DNI de un empleado");
                return false;
            }

            String query = "UPDATE Empleado SET nombre = ?, apellidos = ?, fecha_nacimiento = ?, fecha_expedicion_titulo = ? WHERE id_empleado = ?";

            int result = jdbcTemplate.update(query,
                    empleado.getNombre(),
                    empleado.getApellidos(),
                    empleado.getFech_nacimiento(),
                    empleado.getFech_expedicion_titulo(),
                    empleado.getId());

            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error actualizando empleado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un patrón/empleado si no está vinculado con ninguna embarcación
     * 
     * @param id ID del empleado a eliminar
     * @return true si la eliminación fue exitosa, false en caso contrario
     */
    public boolean eliminarEmpleado(int id) {
        try {
            // Verificar si el empleado está asignado como patrón a alguna embarcación
            String checkQuery = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, id);

            if (count != null && count > 0) {
                System.err.println("No se puede eliminar el empleado, está asignado a " + count + " embarcaciones");
                return false;
            }

            // Eliminar el empleado
            String deleteQuery = "DELETE FROM Empleado WHERE id_empleado = ?";
            int result = jdbcTemplate.update(deleteQuery, id);

            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error eliminando empleado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un empleado existe por su ID
     * 
     * @param id ID del empleado
     * @return true si existe, false si no
     */
    public boolean existeEmpleado(int id) {
        try {
            String query = "SELECT COUNT(*) FROM Empleado WHERE id_empleado = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            System.err.println("Error verificando existencia de empleado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un empleado existe por su DNI
     * 
     * @param dni DNI del empleado
     * @return true si existe, false si no
     */
    public boolean existeEmpleadoPorDNI(String dni) {
        try {
            String query = "SELECT COUNT(*) FROM Empleado WHERE dni = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, dni);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            System.err.println("Error verificando existencia de empleado por DNI: " + e.getMessage());
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

    /**
     * Verifica si un empleado tiene patrón asignado
     * 
     * @param id ID del empleado
     * @return true si está asignado a alguna embarcación, false si no
     */
    public boolean tienePatronAsignado(int id) {
        try {
            String query = "SELECT COUNT(*) FROM Embarcaciones WHERE id_patron_asignado = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            System.err.println("Error verificando si empleado tiene patrón asignado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}