package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.reserva.Reserva;

@Repository
public class ReservaRepository extends AbstractRepository {

    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
        this.sqlQueries = cargarSqlProperties();
    }

    private void verificarQueriesSQL() {
        if (this.sqlQueries == null) {
            this.sqlQueries = cargarSqlProperties();
        }
    }

    public boolean insertarReserva(Reserva nuevaReserva) {
        try {
            return ejecutarInsertarReserva(nuevaReserva);
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar la reserva");
            throw excepcion;
        }
    }

    private boolean ejecutarInsertarReserva(Reserva nuevaReserva) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.insertar");
        if (query == null) {
            throw new RuntimeException("ERROR: Propiedad 'reservas.insertar' no encontrada en sql.properties");
        }
        int filasAfectadas = jdbcTemplate.update(query,
                nuevaReserva.getIdSocioSolicitante(),
                nuevaReserva.getPlazasSolicitadas(),
                nuevaReserva.getPropositoActividad(),
                nuevaReserva.getPrecioTotal(),
                nuevaReserva.getMatriculaEmbarcacion(),
                nuevaReserva.getFechaActividad(),
                nuevaReserva.getIdPatron());
        return filasAfectadas > 0;
    }

    public boolean estaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            return ejecutarEstaDisponible(matricula, fechaInicio, fechaFin);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando disponibilidad para embarcación: " + matricula);
            throw excepcion;
        }
    }

    private boolean ejecutarEstaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        verificarQueriesSQL();
        if (tieneReservaEnRango(matricula, fechaInicio, fechaFin)) {
            System.out.println("❌ NO DISPONIBLE - Ya hay reservas en este rango");
            return false;
        }
        return !tieneAlquilerSolapado(matricula, fechaInicio, fechaFin);
    }

    private boolean tieneReservaEnRango(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        String queryReservas = sqlQueries.getProperty("reservas.verificar.disponibilidad.rango");
        if (queryReservas == null) {
            queryReservas = "SELECT COUNT(*) FROM Reserva WHERE matricula_embarcacion = ? " +
                    "AND fecha_reserva BETWEEN ? AND ?";
        }
        Integer countReservas = jdbcTemplate.queryForObject(queryReservas, Integer.class, matricula, fechaInicio, fechaFin);
        return countReservas != null && countReservas > 0;
    }

    private boolean tieneAlquilerSolapado(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        String queryAlquileres = sqlQueries.getProperty("alquileres.verificar.disponibilidad");
        if (queryAlquileres == null) {
            queryAlquileres = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ? " +
                    "AND NOT (fecha_fin < ? OR fecha_inicio > ?)";
        }
        Integer countAlquileres = jdbcTemplate.queryForObject(queryAlquileres, Integer.class, matricula, fechaInicio, fechaFin);
        return countAlquileres != null && countAlquileres > 0;
    }

    public boolean tienePatronAsignado(String matricula, LocalDate fecha) {
        try {
            return ejecutarTienePatronAsignado(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando patrón asignado para embarcación: " + matricula);
            throw excepcion;
        }
    }

    private boolean ejecutarTienePatronAsignado(String matricula) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.verificar.patron.asignado");
        if (query == null) {
            query = "SELECT COUNT(*) FROM Embarcaciones WHERE matricula = ? AND id_patron_asignado IS NOT NULL";
        }
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, matricula);
        return count != null && count > 0;
    }

    public Integer obtenerPatronAsignado(String matricula, LocalDate fecha) {
        try {
            return ejecutarObtenerPatronAsignado(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrón asignado para embarcación: " + matricula);
            throw excepcion;
        }
    }

    private Integer ejecutarObtenerPatronAsignado(String matricula) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.obtener.patron.asignado");
        if (query == null) {
            query = "SELECT id_patron_asignado FROM Embarcaciones WHERE matricula = ?";
        }
        List<Integer> resultados = jdbcTemplate.query(query, new Object[] { matricula }, (rs, rowNum) -> {
            Integer idPatron = rs.getInt("id_patron_asignado");
            return rs.wasNull() ? null : idPatron;
        });
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public List<String> obtenerEmbarcacionesDisponibles(LocalDate fecha) {
        try {
            return ejecutarObtenerEmbarcacionesDisponibles(fecha);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones disponibles para fecha: " + fecha);
            throw excepcion;
        }
    }

    private List<String> ejecutarObtenerEmbarcacionesDisponibles(LocalDate fecha) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.obtener.embarcaciones.disponibles");
        if (query == null) {
            query = "SELECT e.matricula FROM Embarcaciones e " +
                    "WHERE e.id_patron_asignado IS NOT NULL " +
                    "AND e.matricula NOT IN (" +
                    "   SELECT r.matricula_embarcacion FROM Reserva r " +
                    "   WHERE r.fecha_reserva = ?" +
                    ")";
        }
        return jdbcTemplate.query(query, new Object[] { fecha }, (rs, rowNum) -> rs.getString("matricula"));
    }

    public int obtenerCapacidadEmbarcacion(String matricula) {
        try {
            return ejecutarObtenerCapacidadEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo capacidad de embarcación: " + matricula);
            throw excepcion;
        }
    }

    private int ejecutarObtenerCapacidadEmbarcacion(String matricula) {
        String query = "SELECT numero_plazas FROM Embarcaciones WHERE matricula = ?";
        Integer capacidad = jdbcTemplate.queryForObject(query, Integer.class, matricula);
        return capacidad != null ? capacidad : 0;
    }

    public List<Reserva> obtenerTodasLasReservas() {
        try {
            return ejecutarObtenerTodasLasReservas();
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todas las reservas");
            throw excepcion;
        }
    }

    private List<Reserva> ejecutarObtenerTodasLasReservas() {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.obtener.todas");
        if (query == null) {
            query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva ORDER BY fecha_reserva DESC";
        }
        return jdbcTemplate.query(query, new ReservaRowMapper());
    }

    public List<Reserva> obtenerReservasPorSocio(Integer idSocio) {
        try {
            return ejecutarObtenerReservasPorSocio(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para socio: " + idSocio);
            throw excepcion;
        }
    }

    private List<Reserva> ejecutarObtenerReservasPorSocio(Integer idSocio) {
        String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                "proposito_actividad, precio_total, matricula_embarcacion, " +
                "fecha_reserva, id_empleado FROM Reserva WHERE id_socio_solicitante = ? ORDER BY fecha_reserva DESC";
        return jdbcTemplate.query(query, new Object[] { idSocio }, new ReservaRowMapper());
    }

    public boolean socioTieneTituloPatron(Integer idSocio) {
        try {
            return ejecutarSocioTieneTituloPatron(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando título de patrón para socio: " + idSocio);
            throw excepcion;
        }
    }

    private boolean ejecutarSocioTieneTituloPatron(Integer idSocio) {
        String query = "SELECT tiene_titulo_patron FROM Socios WHERE id_socio = ?";
        Boolean tieneTitulo = jdbcTemplate.queryForObject(query, Boolean.class, idSocio);
        return tieneTitulo != null && tieneTitulo;
    }

    public List<Reserva> obtenerReservasFuturas(LocalDate fecha) {
        try {
            return ejecutarObtenerReservasFuturas(fecha);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas futuras desde fecha: " + fecha);
            throw excepcion;
        }
    }

    private List<Reserva> ejecutarObtenerReservasFuturas(LocalDate fecha) {
        verificarQueriesSQL();
        String query = sqlQueries.getProperty("reservas.obtener.futuras");
        if (query == null) {
            query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva WHERE fecha_reserva >= ? ORDER BY fecha_reserva ASC";
        }
        return jdbcTemplate.query(query, new Object[] { fecha }, new ReservaRowMapper());
    }

    public Reserva obtenerReservaPorId(Integer idReserva) {
        try {
            return ejecutarObtenerReservaPorId(idReserva);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reserva por ID: " + idReserva);
            throw excepcion;
        }
    }

    private Reserva ejecutarObtenerReservaPorId(Integer idReserva) {
        String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                "proposito_actividad, precio_total, matricula_embarcacion, " +
                "fecha_reserva, id_empleado FROM Reserva WHERE id_reserva = ?";
        List<Reserva> resultados = jdbcTemplate.query(query, new Object[] { idReserva }, new ReservaRowMapper());
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    private static class ReservaRowMapper implements RowMapper<Reserva> {
        @Override
        public Reserva mapRow(ResultSet rs, int rowNum) throws SQLException {
            Reserva reserva = new Reserva();
            reserva.setIdReserva(rs.getInt("id_reserva"));
            reserva.setIdSocioSolicitante(rs.getInt("id_socio_solicitante"));
            reserva.setPlazasSolicitadas(rs.getInt("plazas_solicitadas"));
            reserva.setPropositoActividad(rs.getString("proposito_actividad"));
            reserva.setPrecioTotal(rs.getDouble("precio_total"));
            reserva.setMatriculaEmbarcacion(rs.getString("matricula_embarcacion"));

            if (rs.getDate("fecha_reserva") != null) {
                reserva.setFechaActividad(rs.getDate("fecha_reserva").toLocalDate());
            } else {
                reserva.setFechaActividad(LocalDate.now());
            }

            try {
                if (rs.getObject("id_empleado") != null) {
                    reserva.setIdPatron(rs.getInt("id_empleado"));
                } else {
                    reserva.setIdPatron(null);
                }
            } catch (SQLException excepcion) {
                reserva.setIdPatron(null);
            }

            return reserva;
        }
    }

    public boolean eliminarReserva(Integer idReserva) {
        try {
            return ejecutarEliminarReserva(idReserva);
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando reserva: " + idReserva);
            throw excepcion;
        }
    }

    private boolean ejecutarEliminarReserva(Integer idReserva) {
        String query = "DELETE FROM Reserva WHERE id_reserva = ?";
        int filasAfectadas = jdbcTemplate.update(query, idReserva);
        return filasAfectadas > 0;
    }

    public boolean esSocioMayorDeEdad(Integer idSocio) {
        try {
            return ejecutarEsSocioMayorDeEdad(idSocio);
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando edad del socio: " + idSocio);
            throw excepcion;
        }
    }

    private boolean ejecutarEsSocioMayorDeEdad(Integer idSocio) {
        String query = "SELECT fecha_nacimiento FROM Socios WHERE id_socio = ?";
        LocalDate fechaNacimiento = jdbcTemplate.queryForObject(query, LocalDate.class, idSocio);
        if (fechaNacimiento != null) {
            LocalDate hoy = LocalDate.now();
            return fechaNacimiento.plusYears(18).isBefore(hoy) || fechaNacimiento.plusYears(18).isEqual(hoy);
        }
        return false;
    }

    public List<Reserva> obtenerReservasPorEmbarcacion(String matricula) {
        try {
            return ejecutarObtenerReservasPorEmbarcacion(matricula);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para embarcación: " + matricula);
            throw excepcion;
        }
    }

    private List<Reserva> ejecutarObtenerReservasPorEmbarcacion(String matricula) {
        String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                "proposito_actividad, precio_total, matricula_embarcacion, " +
                "fecha_reserva, id_empleado FROM Reserva WHERE matricula_embarcacion = ? ORDER BY fecha_reserva DESC";
        return jdbcTemplate.query(query, new Object[] { matricula }, new ReservaRowMapper());
    }

    public List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        try {
            return ejecutarObtenerReservasPorFecha(fecha);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para fecha: " + fecha);
            throw excepcion;
        }
    }

    private List<Reserva> ejecutarObtenerReservasPorFecha(LocalDate fecha) {
        String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                "proposito_actividad, precio_total, matricula_embarcacion, " +
                "fecha_reserva, id_empleado FROM Reserva WHERE fecha_reserva = ? ORDER BY id_reserva DESC";
        return jdbcTemplate.query(query, new Object[] { fecha }, new ReservaRowMapper());
    }
}