package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.embarcacion.Embarcacion;
import es.uco.pw.pw2526.model.domain.embarcacion.TipoEmbarcacion;

@Repository
public class EmbarcacionRepository extends AbstractRepository {

    public EmbarcacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private RowMapper<Embarcacion> embarcacionRowMapper = new RowMapper<Embarcacion>() {
        public Embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Embarcacion embarcacion = new Embarcacion();
            embarcacion.setMatricula(rs.getString("matricula"));
            embarcacion.setNombre(rs.getString("nombre"));
            embarcacion.setTipo(TipoEmbarcacion.valueOf(rs.getString("tipo_embarcacion")));
            embarcacion.setNumeroPlazas(rs.getInt("numero_plazas"));
            embarcacion.setEsloraEnMetros(parseDimensiones(rs.getString("dimensiones")));

            Integer idPatron = rs.getInt("id_patron_asignado");
            if (!rs.wasNull()) {
                embarcacion.setIdPatronAsignado(idPatron);
            }

            return embarcacion;
        }
    };

    public List<Embarcacion> obtenerEmbarcaciones() {
        try {
            return ejecutarObtenerEmbarcaciones();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones desde la base de datos");
            throw excepcion;
        }
    }

    private List<Embarcacion> ejecutarObtenerEmbarcaciones() {
        String query = sqlQueries.getProperty("embarcaciones.obtener.todas");
        if (query == null) {
            throw new RuntimeException("ERROR: Propiedad 'embarcaciones.obtener.todas' no encontrada en sql.properties");
        }
        return jdbcTemplate.query(query, embarcacionRowMapper);
    }

    public boolean insertarEmbarcacion(Embarcacion nueva) {
        try {
            return ejecutarInsertarEmbarcacion(nueva);
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar la embarcación (posiblemente matrícula duplicada o error SQL)");
            throw excepcion;
        }
    }

    private boolean ejecutarInsertarEmbarcacion(Embarcacion nueva) {
        String query = sqlQueries.getProperty("embarcaciones.insertar");
        if (query == null) {
            throw new RuntimeException("ERROR: Propiedad 'embarcaciones.insertar' no encontrada en sql.properties");
        }
        int filasAfectadas = jdbcTemplate.update(query,
                nueva.getMatricula().trim().toUpperCase(),
                nueva.getTipo().name(),
                nueva.getNombre().trim(),
                nueva.getNumeroPlazas(),
                nueva.getEsloraEnMetros() + "m");
        return filasAfectadas > 0;
    }

    public Embarcacion buscarPorMatricula(String matricula) {
        try {
            return ejecutarBuscarPorMatricula(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error buscando embarcación por matrícula");
            throw excepcion;
        }
    }

    private Embarcacion ejecutarBuscarPorMatricula(String matricula) {
        String query = sqlQueries.getProperty("embarcaciones.buscar.por_matricula");
        if (query == null) {
            throw new RuntimeException("ERROR: Propiedad 'embarcaciones.buscar.por_matricula' no encontrada en sql.properties");
        }
        List<Embarcacion> resultado = jdbcTemplate.query(query, new Object[] { matricula.trim().toUpperCase() }, embarcacionRowMapper);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Embarcacion> buscarPorTipo(TipoEmbarcacion tipo) {
        try {
            return ejecutarBuscarPorTipo(tipo);
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error buscando embarcaciones por tipo: " + tipo);
            throw excepcion;
        }
    }

    private List<Embarcacion> ejecutarBuscarPorTipo(TipoEmbarcacion tipo) {
        String query = sqlQueries.getProperty("embarcaciones.buscar.por_tipo");
        if (query == null) {
            query = "SELECT * FROM Embarcaciones WHERE tipo_embarcacion = ? ORDER BY nombre";
        }
        return jdbcTemplate.query(query, new Object[] { tipo.name() }, embarcacionRowMapper);
    }

    public List<Embarcacion> obtenerEmbarcacionesConPatron() {
        try {
            return ejecutarObtenerEmbarcacionesConPatron();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones con patrón");
            throw excepcion;
        }
    }

    private List<Embarcacion> ejecutarObtenerEmbarcacionesConPatron() {
        String query = sqlQueries.getProperty("embarcaciones.obtener.con.patron");
        if (query == null) {
            query = "SELECT e.*, em.nombre AS nombre_patron, em.apellidos AS apellidos_patron " +
                    "FROM Embarcaciones e " +
                    "LEFT JOIN Empleado em ON e.id_patron_asignado = em.id_empleado";
        }
        return jdbcTemplate.query(query, embarcacionRowMapper);
    }

    public boolean actualizarEmbarcacion(Embarcacion embarcacion) {
        try {
            return ejecutarActualizarEmbarcacion(embarcacion);
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarActualizarEmbarcacion(Embarcacion embarcacion) {
        String query = sqlQueries.getProperty("embarcaciones.actualizar");
        if (query == null) {
            query = "UPDATE Embarcaciones SET tipo_embarcacion = ?, nombre = ?, numero_plazas = ?, dimensiones = ? WHERE matricula = ?";
        }
        int filasAfectadas = jdbcTemplate.update(query,
                embarcacion.getTipo().name(),
                embarcacion.getNombre().trim(),
                embarcacion.getNumeroPlazas(),
                embarcacion.getEsloraEnMetros() + "m",
                embarcacion.getMatricula().trim().toUpperCase());
        return filasAfectadas > 0;
    }

    public boolean vincularPatron(String matricula, Integer idPatron) {
        try {
            return ejecutarVincularPatron(matricula, idPatron);
        } catch (DataAccessException excepcion) {
            System.err.println("Error vinculando patrón a embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarVincularPatron(String matricula, Integer idPatron) {
        String query = sqlQueries.getProperty("embarcaciones.asignar.patron");
        if (query == null) {
            query = "UPDATE Embarcaciones SET id_patron_asignado = ? WHERE matricula = ?";
        }
        int filasAfectadas = jdbcTemplate.update(query, idPatron, matricula.trim().toUpperCase());
        return filasAfectadas > 0;
    }

    public boolean desvincularPatron(String matricula) {
        try {
            return ejecutarDesvincularPatron(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error desvinculando patrón de embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarDesvincularPatron(String matricula) {
        String query = sqlQueries.getProperty("embarcaciones.liberar.patron");
        if (query == null) {
            query = "UPDATE Embarcaciones SET id_patron_asignado = NULL WHERE matricula = ?";
        }
        int filasAfectadas = jdbcTemplate.update(query, matricula.trim().toUpperCase());
        return filasAfectadas > 0;
    }

    public boolean eliminarEmbarcacion(String matricula) {
        try {
            return ejecutarEliminarEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarEliminarEmbarcacion(String matricula) {
        Embarcacion embarcacionExistente = buscarPorMatricula(matricula);
        if (embarcacionExistente == null) {
            throw new RuntimeException("La embarcación con matrícula " + matricula + " no existe");
        }
        verificarAsociacionesAlquilerReserva(matricula);
        String query = sqlQueries.getProperty("embarcaciones.eliminar");
        if (query == null) {
            query = "DELETE FROM Embarcaciones WHERE matricula = ?";
        }
        int filasAfectadas = jdbcTemplate.update(query, matricula.trim().toUpperCase());
        return filasAfectadas > 0;
    }

    private void verificarAsociacionesAlquilerReserva(String matricula) {
        String checkAlquileresQuery = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ?";
        String checkReservasQuery = "SELECT COUNT(*) FROM Reserva WHERE matricula_embarcacion = ?";

        Integer alquileresCount = jdbcTemplate.queryForObject(checkAlquileresQuery, Integer.class, matricula);
        Integer reservasCount = jdbcTemplate.queryForObject(checkReservasQuery, Integer.class, matricula);

        int totalAsociaciones = (alquileresCount != null ? alquileresCount : 0) + (reservasCount != null ? reservasCount : 0);

        if (totalAsociaciones > 0) {
            throw new RuntimeException("No se puede eliminar la embarcación, tiene " + totalAsociaciones + " alquileres/reservas asociados");
        }
    }

    public boolean existeEmbarcacion(String matricula) {
        try {
            return ejecutarExisteEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de embarcación: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private boolean ejecutarExisteEmbarcacion(String matricula) {
        String query = "SELECT COUNT(*) FROM Embarcaciones WHERE matricula = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, matricula.trim().toUpperCase());
        return count != null && count > 0;
    }

    private float parseDimensiones(String dimensionesStr) {
        if (dimensionesStr == null || dimensionesStr.trim().isEmpty()) {
            return 0.0f;
        }
        try {
            String cleanDimension = dimensionesStr.replaceAll("[^\\d.]", "");
            return !cleanDimension.isEmpty() ? Float.parseFloat(cleanDimension) : 0.0f;
        } catch (NumberFormatException nfe) {
            return 0.0f;
        }
    }
}