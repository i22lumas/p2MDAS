package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.alquiler.Alquiler;

@Repository
public class AlquilerRepository extends AbstractRepository {

    public AlquilerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
        this.sqlQueries = cargarSqlProperties();
    }

    public int insertarAlquilerYRetornarId(Alquiler nuevoAlquiler) {
        try {
            return ejecutarInsertarAlquiler(nuevoAlquiler);
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar el alquiler: " + excepcion.getMessage());
            throw excepcion;
        }
    }

    private int ejecutarInsertarAlquiler(Alquiler nuevoAlquiler) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.insertar");
        validarQuery(query, "alquileres.insertar");

        imprimirDatosAlquiler(nuevoAlquiler);

        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        int filasAfectadas = jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, nuevoAlquiler.getIdSocioTitular());
            ps.setString(2, nuevoAlquiler.getMatriculaEmbarcacion());
            ps.setDate(3, java.sql.Date.valueOf(nuevoAlquiler.getFechaInicio()));
            ps.setDate(4, java.sql.Date.valueOf(nuevoAlquiler.getFechaFin()));
            ps.setInt(5, nuevoAlquiler.getPlazasSolicitadas());
            ps.setDouble(6, nuevoAlquiler.getPrecioTotal());
            return ps;
        }, keyHolder);

        return procesarResultadoInsercion(nuevoAlquiler, filasAfectadas, keyHolder);
    }

    private void verificarQueriesSQL() {
        if (this.sqlQueries == null) {
            this.sqlQueries = cargarSqlProperties();
        }
    }

    private void validarQuery(String query, String nombreQuery) {
        if (query == null) {
            String errorMsg = "ERROR: Consulta '" + nombreQuery + "' no encontrada";
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    private void imprimirDatosAlquiler(Alquiler nuevoAlquiler) {
        System.out.println("=== INSERTANDO NUEVO ALQUILER ===");
        System.out.println("ID Socio: " + nuevoAlquiler.getIdSocioTitular());
        System.out.println("Matrícula: " + nuevoAlquiler.getMatriculaEmbarcacion());
        System.out.println("Plazas: " + nuevoAlquiler.getPlazasSolicitadas());
        System.out.println("Tripulantes: " + (nuevoAlquiler.getDnisTripulantes() != null ? nuevoAlquiler.getDnisTripulantes().size() : 0));
    }

    private int procesarResultadoInsercion(Alquiler nuevoAlquiler, int filasAfectadas, org.springframework.jdbc.support.KeyHolder keyHolder) {
        if (filasAfectadas > 0 && keyHolder.getKey() != null) {
            int idAlquiler = keyHolder.getKey().intValue();
            System.out.println("Alquiler insertado con ID: " + idAlquiler);
            insertarTripulantesSiExisten(idAlquiler, nuevoAlquiler.getDnisTripulantes());
            System.out.println("=== FIN INSERCIÓN ===");
            return idAlquiler;
        }
        throw new RuntimeException("Fallo al insertar el alquiler, no se generó ID");
    }

    private void insertarTripulantesSiExisten(int idAlquiler, List<String> dnisTripulantes) {
        if (dnisTripulantes != null && !dnisTripulantes.isEmpty()) {
            insertarTripulantesAlquiler(idAlquiler, dnisTripulantes);
        }
    }

    private void insertarTripulantesAlquiler(int idAlquiler, List<String> dnisTripulantes) {
        try {
            ejecutarInsertarTripulantes(idAlquiler, dnisTripulantes);
        } catch (Exception excepcion) {
            System.err.println("Error insertando tripulantes: " + excepcion.getMessage());
            throw new RuntimeException(excepcion);
        }
    }

    private void ejecutarInsertarTripulantes(int idAlquiler, List<String> dnisTripulantes) {
        String query = "INSERT INTO Alquiler_Tripulantes (id_alquiler, dni_tripulante) VALUES (?, ?)";
        for (String dni : dnisTripulantes) {
            if (dni != null && !dni.trim().isEmpty()) {
                jdbcTemplate.update(query, idAlquiler, dni.trim());
                System.out.println("Tripulante insertado: " + dni + " para alquiler ID: " + idAlquiler);
            }
        }
    }

    public List<String> obtenerTripulantesPorAlquiler(int idAlquiler) {
        try {
            return ejecutarObtenerTripulantes(idAlquiler);
        } catch (Exception excepcion) {
            System.err.println("Error obteniendo tripulantes para alquiler: " + idAlquiler);
            throw new RuntimeException(excepcion);
        }
    }

    private List<String> ejecutarObtenerTripulantes(int idAlquiler) {
        String query = "SELECT dni_tripulante FROM Alquiler_Tripulantes WHERE id_alquiler = ?";
        return jdbcTemplate.query(query,
                new Object[] { idAlquiler },
                (rs, rowNum) -> rs.getString("dni_tripulante"));
    }

    private Alquiler mapAlquiler(ResultSet rs) throws SQLException {
        Alquiler alquiler = new Alquiler();
        alquiler.setIdAlquiler(rs.getInt("id_alquiler"));
        alquiler.setIdSocioTitular(rs.getInt("id_socio_titular"));
        alquiler.setDniSocioTitular(rs.getString("dni"));
        alquiler.setMatriculaEmbarcacion(rs.getString("matricula_embarcacion"));
        alquiler.setFechaInicio(rs.getDate("fecha_inicio").toLocalDate());
        alquiler.setFechaFin(rs.getDate("fecha_fin").toLocalDate());
        alquiler.setPlazasSolicitadas(rs.getInt("plazas_solicitadas"));
        alquiler.setPrecioTotal(rs.getDouble("precio_total"));

        List<String> tripulantes = obtenerTripulantesPorAlquiler(rs.getInt("id_alquiler"));
        alquiler.setDnisTripulantes(tripulantes);

        return alquiler;
    }

    public List<Alquiler> obtenerAlquileresFuturos() {
        try {
            return ejecutarObtenerAlquileresFuturos();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres futuros");
            throw excepcion;
        }
    }

    private List<Alquiler> ejecutarObtenerAlquileresFuturos() {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.obtener.futuros");
        validarQuery(query, "alquileres.obtener.futuros");
        return jdbcTemplate.query(query, new RowMapper<Alquiler>() {
            public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapAlquiler(rs);
            }
        });
    }

    public List<Alquiler> obtenerAlquileresPorEmbarcacion(String matricula) {
        try {
            return ejecutarObtenerAlquileresPorEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres por embarcación: " + matricula);
            throw excepcion;
        }
    }

    private List<Alquiler> ejecutarObtenerAlquileresPorEmbarcacion(String matricula) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.obtener.por.embarcacion");
        validarQuery(query, "alquileres.obtener.por.embarcacion");
        return jdbcTemplate.query(query, new Object[] { matricula },
                new RowMapper<Alquiler>() {
                    public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return mapAlquiler(rs);
                    }
                });
    }

    public boolean estaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarEstaDisponible(matricula, fechaInicio, fechaFin);
        } catch (Exception excepcion) {
            System.err.println("ERROR en estaDisponible para " + matricula + ": " + excepcion.getMessage());
            throw new RuntimeException(excepcion);
        }
    }

    private boolean ejecutarEstaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.verificar.disponibilidad");
        if (query == null) {
            query = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ? AND NOT (fecha_fin < ? OR fecha_inicio > ?)";
        }
        Integer count = jdbcTemplate.queryForObject(
                query,
                new Object[] { matricula, fechaInicio, fechaFin },
                Integer.class);
        return count == 0;
    }

    public List<Alquiler> obtenerAlquileresPorSocio(String dniSocio) {
        try {
            return ejecutarObtenerAlquileresPorSocio(dniSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres por socio: " + dniSocio);
            throw excepcion;
        }
    }

    private List<Alquiler> ejecutarObtenerAlquileresPorSocio(String dniSocio) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.obtener.por.socio");
        validarQuery(query, "alquileres.obtener.por.socio");
        return jdbcTemplate.query(query, new Object[] { dniSocio },
                new RowMapper<Alquiler>() {
                    public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return mapAlquiler(rs);
                    }
                });
    }

    public List<Alquiler> obtenerTodosAlquileres() {
        try {
            return ejecutarObtenerTodosAlquileres();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todos los alquileres");
            throw excepcion;
        }
    }

    private List<Alquiler> ejecutarObtenerTodosAlquileres() {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.obtener.todos");
        validarQuery(query, "alquileres.obtener.todos");
        return jdbcTemplate.query(query, new RowMapper<Alquiler>() {
            public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapAlquiler(rs);
            }
        });
    }

    public int contarAlquileresActivosPorSocio(int idSocio) {
        try {
            return ejecutarContarAlquileresActivos(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error contando alquileres activos para socio: " + idSocio);
            throw excepcion;
        }
    }

    private int ejecutarContarAlquileresActivos(int idSocio) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("alquileres.contar.activos.por.socio");
        validarQuery(query, "alquileres.contar.activos.por.socio");
        Integer count = jdbcTemplate.queryForObject(query, new Object[] { idSocio, LocalDate.now() }, Integer.class);
        return count != null ? count : 0;
    }

    public boolean agregarSocioNoTitular(int idAlquiler, String dniSocio) {
        try {
            return ejecutarAgregarSocioNoTitular(idAlquiler, dniSocio);
        } catch (Exception excepcion) {
            System.err.println("Error agregando socio no titular: " + excepcion.getMessage());
            throw new RuntimeException(excepcion);
        }
    }

    private boolean ejecutarAgregarSocioNoTitular(int idAlquiler, String dniSocio) {
        String query = "INSERT INTO Alquiler_Tripulantes (id_alquiler, dni_tripulante) VALUES (?, ?)";
        int filasAfectadas = jdbcTemplate.update(query, idAlquiler, dniSocio);
        return filasAfectadas > 0;
    }

    public boolean quitarSocioNoTitular(int idAlquiler, String dniSocio) {
        try {
            return ejecutarQuitarSocioNoTitular(idAlquiler, dniSocio);
        } catch (Exception excepcion) {
            System.err.println("Error quitando socio no titular: " + excepcion.getMessage());
            throw new RuntimeException(excepcion);
        }
    }

    private boolean ejecutarQuitarSocioNoTitular(int idAlquiler, String dniSocio) {
        String query = "DELETE FROM Alquiler_Tripulantes WHERE id_alquiler = ? AND dni_tripulante = ?";
        int filasAfectadas = jdbcTemplate.update(query, idAlquiler, dniSocio);
        return filasAfectadas > 0;
    }

    public boolean cancelarAlquiler(int idAlquiler) {
        try {
            return ejecutarCancelarAlquiler(idAlquiler);
        } catch (Exception excepcion) {
            System.err.println("Error cancelando alquiler: " + excepcion.getMessage());
            throw new RuntimeException(excepcion);
        }
    }

    private boolean ejecutarCancelarAlquiler(int idAlquiler) {
        String queryTripulantes = "DELETE FROM Alquiler_Tripulantes WHERE id_alquiler = ?";
        jdbcTemplate.update(queryTripulantes, idAlquiler);
        String queryAlquiler = "DELETE FROM Alquiler WHERE id_alquiler = ?";
        int filasAfectadas = jdbcTemplate.update(queryAlquiler, idAlquiler);
        return filasAfectadas > 0;
    }
}