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

    /**
     * Constructor del repositorio de reservas
     * 
     * @param jdbcTemplate Plantilla JDBC para operaciones de base de datos
     */
    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.setSQLQueriesFileName("./src/main/resources/db/sql.properties");
        this.sqlQueries = cargarSqlProperties();
    }

    /**
     * Inserta una nueva reserva en la base de datos
     * 
     * @param nuevaReserva Objeto Reserva a insertar
     * @return true si la inserción fue exitosa, false en caso contrario
     */
    public boolean insertarReserva(Reserva nuevaReserva) {
        try {
            String query = sqlQueries.getProperty("reservas.insertar");
            if (query != null) {
                int filasAfectadas = jdbcTemplate.update(query,
                        nuevaReserva.getIdSocioSolicitante(),
                        nuevaReserva.getPlazasSolicitadas(),
                        nuevaReserva.getPropositoActividad(),
                        nuevaReserva.getPrecioTotal(),
                        nuevaReserva.getMatriculaEmbarcacion(),
                        nuevaReserva.getFechaActividad(),
                        nuevaReserva.getIdPatron());
                return filasAfectadas > 0;
            } else {
                System.err.println("ERROR: Propiedad 'reservas.insertar' no encontrada en sql.properties");
                return false;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar la reserva");
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una embarcación está disponible en un rango de fechas
     * CORREGIDO: Ahora verifica tanto reservas como alquileres para evitar
     * solapamientos
     * 
     * @param matricula   Matrícula de la embarcación
     * @param fechaInicio Fecha de inicio del período
     * @param fechaFin    Fecha de fin del período
     * @return true si está disponible, false si ya está reservada o alquilada
     */
    public boolean estaDisponible(String matricula, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            System.out.println("=== VERIFICANDO DISPONIBILIDAD COMPLETA ===");
            System.out.println("Matrícula: " + matricula);
            System.out.println("Fecha inicio: " + fechaInicio);
            System.out.println("Fecha fin: " + fechaFin);

            String queryReservas = sqlQueries.getProperty("reservas.verificar.disponibilidad.rango");
            if (queryReservas == null) {
                queryReservas = "SELECT COUNT(*) FROM Reserva WHERE matricula_embarcacion = ? " +
                        "AND fecha_reserva BETWEEN ? AND ?";
            }

            Integer countReservas = jdbcTemplate.queryForObject(queryReservas, Integer.class,
                    matricula, fechaInicio, fechaFin);

            System.out.println("Reservas encontradas en el rango: " + countReservas);

            if (countReservas != null && countReservas > 0) {
                System.out.println("❌ NO DISPONIBLE - Ya hay reservas en este rango");
                return false;
            }

            String queryAlquileres = sqlQueries.getProperty("alquileres.verificar.disponibilidad");
            if (queryAlquileres == null) {
                queryAlquileres = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ? " +
                        "AND NOT (fecha_fin < ? OR fecha_inicio > ?)";
            }

            Integer countAlquileres = jdbcTemplate.queryForObject(queryAlquileres, Integer.class,
                    matricula, fechaInicio, fechaFin);

            System.out.println("Alquileres que se solapan: " + countAlquileres);

            boolean disponible = countAlquileres == null || countAlquileres == 0;
            System.out.println("¿Disponible? " + disponible);
            System.out.println("=== FIN VERIFICACIÓN DISPONIBILIDAD ===");

            return disponible;

        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando disponibilidad para embarcación: " + matricula);
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una embarcación tiene patrón asignado
     * 
     * @param matricula Matrícula de la embarcación
     * @param fecha     Fecha para la que se verifica
     * @return true si tiene patrón asignado, false en caso contrario
     */
    public boolean tienePatronAsignado(String matricula, LocalDate fecha) {
        try {
            String query = sqlQueries.getProperty("reservas.verificar.patron.asignado");
            if (query == null) {
                query = "SELECT COUNT(*) FROM Embarcaciones WHERE matricula = ? AND id_patron_asignado IS NOT NULL";
            }

            Integer count = jdbcTemplate.queryForObject(query, Integer.class, matricula);
            boolean tienePatron = count != null && count > 0;
            System.out.println("¿Tiene patrón asignado " + matricula + "? " + tienePatron);
            return tienePatron;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando patrón asignado para embarcación: " + matricula);
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el patrón asignado a una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @param fecha     Fecha para la que se consulta
     * @return ID del patrón asignado, null si no hay patrón
     */
    public Integer obtenerPatronAsignado(String matricula, LocalDate fecha) {
        try {
            String query = sqlQueries.getProperty("reservas.obtener.patron.asignado");
            if (query == null) {
                query = "SELECT id_patron_asignado FROM Embarcaciones WHERE matricula = ?";
            }

            List<Integer> resultados = jdbcTemplate.query(query,
                    new Object[] { matricula },
                    (rs, rowNum) -> {
                        Integer idPatron = rs.getInt("id_patron_asignado");
                        return rs.wasNull() ? null : idPatron;
                    });

            Integer patron = resultados.isEmpty() ? null : resultados.get(0);
            System.out.println("Patrón asignado a " + matricula + ": " + patron);
            return patron;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo patrón asignado para embarcación: " + matricula);
            excepcion.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene las embarcaciones disponibles con patrón en una fecha específica
     * 
     * @param fecha Fecha para la que se consulta disponibilidad
     * @return Lista de matrículas de embarcaciones disponibles
     */
    public List<String> obtenerEmbarcacionesDisponibles(LocalDate fecha) {
        try {
            String query = sqlQueries.getProperty("reservas.obtener.embarcaciones.disponibles");
            if (query == null) {
                query = "SELECT e.matricula FROM Embarcaciones e " +
                        "WHERE e.id_patron_asignado IS NOT NULL " +
                        "AND e.matricula NOT IN (" +
                        "   SELECT r.matricula_embarcacion FROM Reserva r " +
                        "   WHERE r.fecha_reserva = ?" +
                        ")";
            }

            return jdbcTemplate.query(query,
                    new Object[] { fecha },
                    (rs, rowNum) -> rs.getString("matricula"));
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones disponibles para fecha: " + fecha);
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene la capacidad de una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return Número de plazas de la embarcación, 0 si hay error
     */
    public int obtenerCapacidadEmbarcacion(String matricula) {
        try {
            String query = "SELECT numero_plazas FROM Embarcaciones WHERE matricula = ?";
            Integer capacidad = jdbcTemplate.queryForObject(query, Integer.class, matricula);
            int capacidadFinal = capacidad != null ? capacidad : 0;
            System.out.println("Capacidad de " + matricula + ": " + capacidadFinal + " plazas");
            return capacidadFinal;
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo capacidad de embarcación: " + matricula);
            excepcion.printStackTrace();
            return 0;
        }
    }

    /**
     * Obtiene todas las reservas
     * 
     * @return Lista de todas las reservas
     */
    public List<Reserva> obtenerTodasLasReservas() {
        try {
            String query = sqlQueries.getProperty("reservas.obtener.todas");
            if (query == null) {
                query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                        "proposito_actividad, precio_total, matricula_embarcacion, " +
                        "fecha_reserva, id_empleado FROM Reserva ORDER BY fecha_reserva DESC";
            }

            return jdbcTemplate.query(query, new ReservaRowMapper());
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo todas las reservas");
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene reservas por socio
     * 
     * @param idSocio ID del socio
     * @return Lista de reservas del socio
     */
    public List<Reserva> obtenerReservasPorSocio(Integer idSocio) {
        try {
            String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva WHERE id_socio_solicitante = ? ORDER BY fecha_reserva DESC";
            return jdbcTemplate.query(query, new Object[] { idSocio }, new ReservaRowMapper());
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para socio: " + idSocio);
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Verifica si un socio tiene título de patrón
     * 
     * @param idSocio ID del socio
     * @return true si el socio tiene título de patrón, false en caso contrario
     */
    public boolean socioTieneTituloPatron(Integer idSocio) {
        try {
            String query = "SELECT tiene_titulo_patron FROM Socios WHERE id_socio = ?";
            Boolean tieneTitulo = jdbcTemplate.queryForObject(query, Boolean.class, idSocio);
            return tieneTitulo != null && tieneTitulo;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando título de patrón para socio: " + idSocio);
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene las reservas futuras a partir de una fecha dada
     * 
     * @param fecha Fecha a partir de la cual se buscan reservas futuras
     * @return Lista de reservas futuras ordenadas por fecha
     */
    public List<Reserva> obtenerReservasFuturas(LocalDate fecha) {
        try {
            String query = sqlQueries.getProperty("reservas.obtener.futuras");
            if (query == null) {
                query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                        "proposito_actividad, precio_total, matricula_embarcacion, " +
                        "fecha_reserva, id_empleado FROM Reserva WHERE fecha_reserva >= ? ORDER BY fecha_reserva ASC";
            }

            return jdbcTemplate.query(query, new Object[] { fecha }, new ReservaRowMapper());
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas futuras desde fecha: " + fecha);
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene una reserva por ID
     * 
     * @param idReserva ID de la reserva a buscar
     * @return Objeto Reserva encontrado, null si no existe
     */
    public Reserva obtenerReservaPorId(Integer idReserva) {
        try {
            String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva WHERE id_reserva = ?";
            List<Reserva> resultados = jdbcTemplate.query(query, new Object[] { idReserva }, new ReservaRowMapper());
            return resultados.isEmpty() ? null : resultados.get(0);
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reserva por ID: " + idReserva);
            excepcion.printStackTrace();
            return null;
        }
    }

    /**
     * RowMapper para la entidad Reserva - CORREGIDO (usa fecha_reserva)
     */
    private static class ReservaRowMapper implements RowMapper<Reserva> {
        /**
         * Mapea un ResultSet a un objeto Reserva
         * 
         * @param rs     ResultSet con los datos de la base de datos
         * @param rowNum Número de fila
         * @return Objeto Reserva mapeado
         * @throws SQLException Si ocurre un error al acceder a los datos
         */
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

    /**
     * Elimina una reserva por ID
     * 
     * @param idReserva ID de la reserva a eliminar
     * @return true si la eliminación fue exitosa, false en caso contrario
     */
    public boolean eliminarReserva(Integer idReserva) {
        try {
            String query = "DELETE FROM Reserva WHERE id_reserva = ?";
            int filasAfectadas = jdbcTemplate.update(query, idReserva);
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando reserva: " + idReserva);
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Método adicional: Verifica si un socio es mayor de edad
     * 
     * @param idSocio ID del socio
     * @return true si el socio es mayor de edad, false en caso contrario
     */
    public boolean esSocioMayorDeEdad(Integer idSocio) {
        try {
            String query = "SELECT fecha_nacimiento FROM Socios WHERE id_socio = ?";
            LocalDate fechaNacimiento = jdbcTemplate.queryForObject(query, LocalDate.class, idSocio);
            if (fechaNacimiento != null) {
                LocalDate hoy = LocalDate.now();
                return fechaNacimiento.plusYears(18).isBefore(hoy) || fechaNacimiento.plusYears(18).isEqual(hoy);
            }
            return false;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando edad del socio: " + idSocio);
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene las reservas por embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return Lista de reservas de la embarcación
     */
    public List<Reserva> obtenerReservasPorEmbarcacion(String matricula) {
        try {
            String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva WHERE matricula_embarcacion = ? ORDER BY fecha_reserva DESC";
            return jdbcTemplate.query(query, new Object[] { matricula }, new ReservaRowMapper());
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para embarcación: " + matricula);
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene las reservas por fecha
     * 
     * @param fecha Fecha para la que se consultan las reservas
     * @return Lista de reservas para la fecha especificada
     */
    public List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        try {
            String query = "SELECT id_reserva, id_socio_solicitante, plazas_solicitadas, " +
                    "proposito_actividad, precio_total, matricula_embarcacion, " +
                    "fecha_reserva, id_empleado FROM Reserva WHERE fecha_reserva = ? ORDER BY id_reserva DESC";
            return jdbcTemplate.query(query, new Object[] { fecha }, new ReservaRowMapper());
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo reservas para fecha: " + fecha);
            excepcion.printStackTrace();
            return List.of();
        }
    }
}