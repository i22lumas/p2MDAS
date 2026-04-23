package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Inserta un nuevo alquiler en la base de datos
     */
    public int insertarAlquilerYRetornarId(Alquiler nuevoAlquiler) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.insertar");
            if (query != null) {
                System.out.println("=== INSERTANDO NUEVO ALQUILER ===");
                System.out.println("ID Socio: " + nuevoAlquiler.getIdSocioTitular());
                System.out.println("Matrícula: " + nuevoAlquiler.getMatriculaEmbarcacion());
                System.out.println("Plazas: " + nuevoAlquiler.getPlazasSolicitadas());
                System.out.println("Tripulantes: "
                        + (nuevoAlquiler.getDnisTripulantes() != null ? nuevoAlquiler.getDnisTripulantes().size() : 0));


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

                if (filasAfectadas > 0 && keyHolder.getKey() != null) {
                    int idAlquiler = keyHolder.getKey().intValue();
                    System.out.println("Alquiler insertado con ID: " + idAlquiler);


                    if (nuevoAlquiler.getDnisTripulantes() != null && !nuevoAlquiler.getDnisTripulantes().isEmpty()) {
                        insertarTripulantesAlquiler(idAlquiler, nuevoAlquiler.getDnisTripulantes());
                    }

                    System.out.println("=== FIN INSERCIÓN ===");
                    return idAlquiler;
                }

                return -1;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.insertar' no encontrada");
                return -1;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar el alquiler: " + excepcion.getMessage());
            return -1;
        }
    }

    /**
     * Inserta los tripulantes de un alquiler
     */
    private void insertarTripulantesAlquiler(int idAlquiler, List<String> dnisTripulantes) {
        try {
            String query = "INSERT INTO Alquiler_Tripulantes (id_alquiler, dni_tripulante) VALUES (?, ?)";

            for (String dni : dnisTripulantes) {
                if (dni != null && !dni.trim().isEmpty()) {
                    jdbcTemplate.update(query, idAlquiler, dni.trim());
                    System.out.println("Tripulante insertado: " + dni + " para alquiler ID: " + idAlquiler);
                }
            }
        } catch (Exception excepcion) {
            System.err.println("Error insertando tripulantes: " + excepcion.getMessage());
        }
    }

    /**
     * Obtiene los DNI de los tripulantes de un alquiler
     */
    public List<String> obtenerTripulantesPorAlquiler(int idAlquiler) {
        try {
            String query = "SELECT dni_tripulante FROM Alquiler_Tripulantes WHERE id_alquiler = ?";
            return jdbcTemplate.query(query,
                    new Object[] { idAlquiler },
                    (rs, rowNum) -> rs.getString("dni_tripulante"));
        } catch (Exception excepcion) {
            System.err.println("Error obteniendo tripulantes para alquiler: " + idAlquiler);
            return new ArrayList<>();
        }
    }

    /**
     * Mapea un ResultSet a un objeto Alquiler (INCLUYENDO id_alquiler)
     */
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

    /**
     * Obtiene la lista de alquileres futuros con tripulantes
     */
    public List<Alquiler> obtenerAlquileresFuturos() {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.obtener.futuros");
            if (query != null) {
                List<Alquiler> alquileres = jdbcTemplate.query(query, new RowMapper<Alquiler>() {
                    public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return mapAlquiler(rs);
                    }
                });
                return alquileres;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.obtener.futuros' no encontrada");
                return new ArrayList<>();
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres futuros");
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los alquileres de una embarcación específica
     */
    public List<Alquiler> obtenerAlquileresPorEmbarcacion(String matricula) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.obtener.por.embarcacion");
            if (query != null) {
                List<Alquiler> alquileres = jdbcTemplate.query(query, new Object[] { matricula },
                        new RowMapper<Alquiler>() {
                            public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return mapAlquiler(rs);
                            }
                        });
                return alquileres;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.obtener.por.embarcacion' no encontrada");
                return new ArrayList<>();
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres por embarcación: " + matricula);
            return new ArrayList<>();
        }
    }

    public boolean estaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.verificar.disponibilidad");

            if (query == null) {
                query = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ? AND NOT (fecha_fin < ? OR fecha_inicio > ?)";
            }

            Integer count = jdbcTemplate.queryForObject(
                    query,
                    new Object[] { matricula, fechaInicio, fechaFin },
                    Integer.class);

            return count == 0;

        } catch (Exception excepcion) {
            System.err.println("ERROR en estaDisponible para " + matricula + ": " + excepcion.getMessage());
            return false;
        }
    }

    /**
     * Obtiene los alquileres de un socio específico
     */
    public List<Alquiler> obtenerAlquileresPorSocio(String dniSocio) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.obtener.por.socio");
            if (query != null) {
                List<Alquiler> alquileres = jdbcTemplate.query(query, new Object[] { dniSocio },
                        new RowMapper<Alquiler>() {
                            public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return mapAlquiler(rs);
                            }
                        });
                return alquileres;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.obtener.por.socio' no encontrada");
                return new ArrayList<>();
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo alquileres por socio: " + dniSocio);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene todos los alquileres del sistema
     */
    public List<Alquiler> obtenerTodosAlquileres() {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.obtener.todos");
            if (query != null) {
                List<Alquiler> alquileres = jdbcTemplate.query(query, new RowMapper<Alquiler>() {
                    public Alquiler mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return mapAlquiler(rs);
                    }
                });
                return alquileres;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.obtener.todos' no encontrada");
                return new ArrayList<>();
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todos los alquileres");
            return new ArrayList<>();
        }
    }

    public int contarAlquileresActivosPorSocio(int idSocio) {
        try {
            if (this.sqlQueries == null)
                this.sqlQueries = cargarSqlProperties();

            String query = sqlQueries.getProperty("alquileres.contar.activos.por.socio");
            if (query != null) {
                Integer count = jdbcTemplate.queryForObject(query, new Object[] { idSocio, LocalDate.now() },
                        Integer.class);
                return count != null ? count : 0;
            } else {
                System.err.println("ERROR: Consulta 'alquileres.contar.activos.por.socio' no encontrada");
                return 0;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error contando alquileres activos para socio: " + idSocio);
            return 0;
        }
    }





    /**
     *  PATCH: Agregar socio no titular 
     */
    public boolean agregarSocioNoTitular(int idAlquiler, String dniSocio) {
        try {
            String query = "INSERT INTO Alquiler_Tripulantes (id_alquiler, dni_tripulante) VALUES (?, ?)";
            int filasAfectadas = jdbcTemplate.update(query, idAlquiler, dniSocio);
            return filasAfectadas > 0;
        }
        catch (Exception excepcion) {
            System.err.println("Error agregando socio no titular: " + excepcion.getMessage());
            return false;
        }
    }

    /**
     *  PATCH: Quitar socio no titular 
     */
    public boolean quitarSocioNoTitular(int idAlquiler, String dniSocio) {
        try {
            String query = "DELETE FROM Alquiler_Tripulantes WHERE id_alquiler = ? AND dni_tripulante = ?";
            int filasAfectadas = jdbcTemplate.update(query, idAlquiler, dniSocio);
            return filasAfectadas > 0;
        }
        catch (Exception excepcion) {
            System.err.println("Error quitando socio no titular: " + excepcion.getMessage());
            return false;
        }
    }

    /**
     *  DELETE: Cancelar alquiler futuro 
     */
    public boolean cancelarAlquiler(int idAlquiler) {
        try {
            String queryTripulantes = "DELETE FROM Alquiler_Tripulantes WHERE id_alquiler = ?";
            jdbcTemplate.update(queryTripulantes, idAlquiler);
            String queryAlquiler = "DELETE FROM Alquiler WHERE id_alquiler = ?";
            int filasAfectadas = jdbcTemplate.update(queryAlquiler, idAlquiler);
            return filasAfectadas > 0;
        } 
        catch (Exception excepcion) {
            System.err.println("Error cancelando alquiler: " + excepcion.getMessage());
        return false;
        }
    }
}
    