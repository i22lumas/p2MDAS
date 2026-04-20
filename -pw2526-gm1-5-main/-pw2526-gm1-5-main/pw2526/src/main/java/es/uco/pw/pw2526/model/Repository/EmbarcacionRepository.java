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

    /**
     * Constructor del repositorio de embarcaciones
     * 
     * @param jdbcTemplate Plantilla JDBC para operaciones de base de datos
     */
    public EmbarcacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Obtiene todas las embarcaciones registradas en la base de datos
     * 
     * @return Lista de embarcaciones, null si hay error
     */
    public List<Embarcacion> obtenerEmbarcaciones() {
        try {
            String query = sqlQueries.getProperty("embarcaciones.obtener.todas");
            if (query != null) {
                return jdbcTemplate.query(query, new RowMapper<Embarcacion>() {
                    public Embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Embarcacion embarcacion = new Embarcacion();
                        embarcacion.setMatricula(rs.getString("matricula"));
                        embarcacion.setNombre(rs.getString("nombre"));
                        embarcacion.setTipo(TipoEmbarcacion.valueOf(rs.getString("tipo_embarcacion")));
                        embarcacion.setNumeroPlazas(rs.getInt("numero_plazas"));
                        embarcacion.setEsloraEnMetros(parseDimensiones(rs.getString("dimensiones")));

                        // Nuevo campo: patrón asignado
                        Integer idPatron = rs.getInt("id_patron_asignado");
                        if (!rs.wasNull()) {
                            embarcacion.setIdPatronAsignado(idPatron);
                        }

                        return embarcacion;
                    }
                });
            } else {
                System.err.println("ERROR: Propiedad 'embarcaciones.obtener.todas' no encontrada en sql.properties");
                return null;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones desde la base de datos");
            excepcion.printStackTrace();
            return null;
        }
    }

    /**
     * Inserta una nueva embarcación en la base de datos
     * 
     * @param nueva Objeto Embarcacion a insertar
     * @return true si la inserción fue exitosa, false en caso contrario
     */
    public boolean insertarEmbarcacion(Embarcacion nueva) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.insertar");
            if (query != null) {
                int filasAfectadas = jdbcTemplate.update(query,
                        nueva.getMatricula().trim().toUpperCase(),
                        nueva.getTipo().name(),
                        nueva.getNombre().trim(),
                        nueva.getNumeroPlazas(),
                        nueva.getEsloraEnMetros() + "m");
                return filasAfectadas > 0;
            } else {
                System.err.println("ERROR: Propiedad 'embarcaciones.insertar' no encontrada en sql.properties");
                return false;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("No se pudo insertar la embarcación (posiblemente matrícula duplicada o error SQL)");
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Busca una embarcación por matrícula
     * 
     * @param matricula Matrícula de la embarcación a buscar
     * @return Objeto Embarcacion encontrado, null si no existe
     */
    public Embarcacion buscarPorMatricula(String matricula) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.buscar.por_matricula");
            if (query != null) {
                List<Embarcacion> resultado = jdbcTemplate.query(query,
                        new Object[] { matricula.trim().toUpperCase() },
                        new RowMapper<Embarcacion>() {
                            public Embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                Embarcacion embarcacion = new Embarcacion();
                                embarcacion.setMatricula(rs.getString("matricula"));
                                embarcacion.setNombre(rs.getString("nombre"));
                                embarcacion.setTipo(TipoEmbarcacion.valueOf(rs.getString("tipo_embarcacion")));
                                embarcacion.setNumeroPlazas(rs.getInt("numero_plazas"));
                                embarcacion.setEsloraEnMetros(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    embarcacion.setIdPatronAsignado(idPatron);
                                }

                                return embarcacion;
                            }
                        });
                return resultado.isEmpty() ? null : resultado.get(0);
            } else {
                System.err.println(
                        "ERROR: Propiedad 'embarcaciones.buscar.por_matricula' no encontrada en sql.properties");
                return null;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("Error buscando embarcación por matrícula");
            excepcion.printStackTrace();
            return null;
        }
    }

    /**
     * Busca embarcaciones por tipo
     * 
     * @param tipo Tipo de embarcación a buscar
     * @return Lista de embarcaciones del tipo especificado
     */
    public List<Embarcacion> buscarPorTipo(TipoEmbarcacion tipo) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.buscar.por_tipo");
            System.out.println("🔍 Buscando embarcaciones por tipo: " + tipo);
            System.out.println("🔍 Consulta SQL: " + query);

            if (query != null) {
                List<Embarcacion> resultado = jdbcTemplate.query(query,
                        new Object[] { tipo.name() },
                        new RowMapper<Embarcacion>() {
                            public Embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                Embarcacion embarcacion = new Embarcacion();
                                embarcacion.setMatricula(rs.getString("matricula"));
                                embarcacion.setNombre(rs.getString("nombre"));
                                embarcacion.setTipo(TipoEmbarcacion.valueOf(rs.getString("tipo_embarcacion")));
                                embarcacion.setNumeroPlazas(rs.getInt("numero_plazas"));
                                embarcacion.setEsloraEnMetros(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    embarcacion.setIdPatronAsignado(idPatron);
                                }

                                System.out.println(
                                        "✅ Encontrada embarcación: " + embarcacion.getMatricula() + " - " + embarcacion.getNombre() +
                                                " - Patrón: " + (embarcacion.getIdPatronAsignado() != null ? "SÍ" : "NO"));
                                return embarcacion;
                            }
                        });
                System.out.println("📊 Total embarcaciones encontradas: " + resultado.size());
                return resultado;
            } else {
                System.err.println("⚠️ Usando consulta por defecto para buscar por tipo");
                String defaultQuery = "SELECT * FROM Embarcaciones WHERE tipo_embarcacion = ? ORDER BY nombre";
                List<Embarcacion> resultado = jdbcTemplate.query(defaultQuery,
                        new Object[] { tipo.name() },
                        new RowMapper<Embarcacion>() {

                            public Embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                Embarcacion embarcacion = new Embarcacion();
                                embarcacion.setMatricula(rs.getString("matricula"));
                                embarcacion.setNombre(rs.getString("nombre"));
                                embarcacion.setTipo(TipoEmbarcacion.valueOf(rs.getString("tipo_embarcacion")));
                                embarcacion.setNumeroPlazas(rs.getInt("numero_plazas"));
                                embarcacion.setEsloraEnMetros(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    embarcacion.setIdPatronAsignado(idPatron);
                                }

                                return embarcacion;
                            }
                        });
                System.out.println("📊 Total embarcaciones encontradas (consulta por defecto): " + resultado.size());
                return resultado;
            }
        } catch (DataAccessException excepcion) {
            System.err.println("❌ Error buscando embarcaciones por tipo: " + tipo);
            excepcion.printStackTrace();
            return List.of();
        } catch (Exception excepcion) {
            System.err.println("❌ Error inesperado buscando embarcaciones por tipo: " + tipo);
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene embarcaciones con información del patrón asignado
     * 
     * @return Lista de embarcaciones con información del patrón
     */
    public List<Embarcacion> obtenerEmbarcacionesConPatron() {
        try {
            String query = sqlQueries.getProperty("embarcaciones.obtener.con.patron");
            if (query == null) {
                query = "SELECT e.*, em.nombre AS nombre_patron, em.apellidos AS apellidos_patron " +
                        "FROM Embarcaciones e " +
                        "LEFT JOIN Empleado em ON e.id_patron_asignado = em.id_empleado";
            }

            return jdbcTemplate.query(query, new RowMapper<Embarcacion>() {
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
            });

        } catch (DataAccessException excepcion) {
            System.err.println("Error obteniendo embarcaciones con patrón");
            excepcion.printStackTrace();
            return List.of();
        }
    }

    /**
     * Actualiza los campos de información de una embarcación, excepto la matrícula
     * 
     * @param embarcacion Objeto Embarcacion con los datos actualizados
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    public boolean actualizarEmbarcacion(Embarcacion embarcacion) {
        try {
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
        } catch (DataAccessException excepcion) {
            System.err.println("Error actualizando embarcación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Vincula un patrón a una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @param idPatron  ID del patrón a vincular
     * @return true si la operación fue exitosa, false en caso contrario
     */
    public boolean vincularPatron(String matricula, Integer idPatron) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.asignar.patron");
            if (query == null) {
                query = "UPDATE Embarcaciones SET id_patron_asignado = ? WHERE matricula = ?";
            }

            int filasAfectadas = jdbcTemplate.update(query,
                    idPatron,
                    matricula.trim().toUpperCase());

            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error vinculando patrón a embarcación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Desvincula un patrón de una embarcación
     * 
     * @param matricula Matrícula de la embarcación
     * @return true si la operación fue exitosa, false en caso contrario
     */
    public boolean desvincularPatron(String matricula) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.liberar.patron");
            if (query == null) {
                query = "UPDATE Embarcaciones SET id_patron_asignado = NULL WHERE matricula = ?";
            }

            int filasAfectadas = jdbcTemplate.update(query,
                    matricula.trim().toUpperCase());

            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error desvinculando patrón de embarcación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una embarcación si no está vinculada a ningún alquiler o reserva
     * 
     * @param matricula Matrícula de la embarcación a eliminar
     * @return true si la eliminación fue exitosa, false en caso contrario
     */
    public boolean eliminarEmbarcacion(String matricula) {
        try {
            // Verificar si la embarcación existe primero
            Embarcacion embarcacionExistente = buscarPorMatricula(matricula);
            if (embarcacionExistente == null) {
                System.err.println("La embarcación con matrícula " + matricula + " no existe");
                return false;
            }

            // Verificar si tiene alquileres o reservas
            String checkAlquileresQuery = "SELECT COUNT(*) FROM Alquiler WHERE matricula_embarcacion = ?";
            String checkReservasQuery = "SELECT COUNT(*) FROM Reserva WHERE matricula_embarcacion = ?";

            Integer alquileresCount = jdbcTemplate.queryForObject(checkAlquileresQuery, Integer.class, matricula);
            Integer reservasCount = jdbcTemplate.queryForObject(checkReservasQuery, Integer.class, matricula);

            int totalAsociaciones = (alquileresCount != null ? alquileresCount : 0) +
                    (reservasCount != null ? reservasCount : 0);

            if (totalAsociaciones > 0) {
                System.err.println("No se puede eliminar la embarcación, tiene " + totalAsociaciones
                        + " alquileres/reservas asociados");
                return false;
            }

            String query = sqlQueries.getProperty("embarcaciones.eliminar");
            if (query == null) {
                query = "DELETE FROM Embarcaciones WHERE matricula = ?";
            }

            int filasAfectadas = jdbcTemplate.update(query, matricula.trim().toUpperCase());
            return filasAfectadas > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error eliminando embarcación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una embarcación existe por su matrícula
     * 
     * @param matricula Matrícula de la embarcación
     * @return true si existe, false si no
     */
    public boolean existeEmbarcacion(String matricula) {
        try {
            String query = "SELECT COUNT(*) FROM Embarcaciones WHERE matricula = ?";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, matricula.trim().toUpperCase());
            return count != null && count > 0;
        } catch (DataAccessException excepcion) {
            System.err.println("Error verificando existencia de embarcación: " + excepcion.getMessage());
            excepcion.printStackTrace();
            return false;
        }
    }

    /**
     * Método auxiliar para parsear dimensiones
     * 
     * @param dimensionesStr String con las dimensiones
     * @return Valor float de las dimensiones, 0.0f si hay error
     */
    private float parseDimensiones(String dimensionesStr) {
        if (dimensionesStr == null || dimensionesStr.trim().isEmpty()) {
            return 0.0f;
        }
        try {
            String cleanDimension = dimensionesStr.replaceAll("[^\\d.]", "");
            if (!cleanDimension.isEmpty()) {
                return Float.parseFloat(cleanDimension);
            } else {
                return 0.0f;
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Error parseando dimensiones: " + dimensionesStr);
            return 0.0f;
        }
    }
}