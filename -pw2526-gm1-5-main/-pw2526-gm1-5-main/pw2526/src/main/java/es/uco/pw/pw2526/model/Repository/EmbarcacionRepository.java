package es.uco.pw.pw2526.model.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import es.uco.pw.pw2526.model.domain.embarcacion.embarcacion;
import es.uco.pw.pw2526.model.domain.embarcacion.TiposBarcos;

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
    public List<embarcacion> obtenerEmbarcaciones() {
        try {
            String query = sqlQueries.getProperty("embarcaciones.obtener.todas");
            if (query != null) {
                return jdbcTemplate.query(query, new RowMapper<embarcacion>() {
                    public embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                        embarcacion e = new embarcacion();
                        e.setMatricula(rs.getString("matricula"));
                        e.setNombre(rs.getString("nombre"));
                        e.setTipo(TiposBarcos.valueOf(rs.getString("tipo_embarcacion")));
                        e.setPlaza(rs.getInt("numero_plazas"));
                        e.setDimensiones(parseDimensiones(rs.getString("dimensiones")));

                        // Nuevo campo: patrón asignado
                        Integer idPatron = rs.getInt("id_patron_asignado");
                        if (!rs.wasNull()) {
                            e.setIdPatronAsignado(idPatron);
                        }

                        return e;
                    }
                });
            } else {
                System.err.println("ERROR: Propiedad 'embarcaciones.obtener.todas' no encontrada en sql.properties");
                return null;
            }
        } catch (DataAccessException e) {
            System.err.println("Error obteniendo embarcaciones desde la base de datos");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inserta una nueva embarcación en la base de datos
     * 
     * @param nueva Objeto embarcacion a insertar
     * @return true si la inserción fue exitosa, false en caso contrario
     */
    public boolean addEmbarcacion(embarcacion nueva) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.insertar");
            if (query != null) {
                int result = jdbcTemplate.update(query,
                        nueva.getMatricula().trim().toUpperCase(),
                        nueva.getTipo().name(),
                        nueva.getNombre().trim(),
                        nueva.getPlaza(),
                        nueva.getDimensiones() + "m");
                return result > 0;
            } else {
                System.err.println("ERROR: Propiedad 'embarcaciones.insertar' no encontrada en sql.properties");
                return false;
            }
        } catch (DataAccessException e) {
            System.err.println("No se pudo insertar la embarcación (posiblemente matrícula duplicada o error SQL)");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca una embarcación por matrícula
     * 
     * @param matricula Matrícula de la embarcación a buscar
     * @return Objeto embarcacion encontrado, null si no existe
     */
    public embarcacion buscarPorMatricula(String matricula) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.buscar.por_matricula");
            if (query != null) {
                List<embarcacion> resultado = jdbcTemplate.query(query,
                        new Object[] { matricula.trim().toUpperCase() },
                        new RowMapper<embarcacion>() {
                            public embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                embarcacion e = new embarcacion();
                                e.setMatricula(rs.getString("matricula"));
                                e.setNombre(rs.getString("nombre"));
                                e.setTipo(TiposBarcos.valueOf(rs.getString("tipo_embarcacion")));
                                e.setPlaza(rs.getInt("numero_plazas"));
                                e.setDimensiones(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    e.setIdPatronAsignado(idPatron);
                                }

                                return e;
                            }
                        });
                return resultado.isEmpty() ? null : resultado.get(0);
            } else {
                System.err.println(
                        "ERROR: Propiedad 'embarcaciones.buscar.por_matricula' no encontrada en sql.properties");
                return null;
            }
        } catch (DataAccessException e) {
            System.err.println("Error buscando embarcación por matrícula");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Busca embarcaciones por tipo
     * 
     * @param tipo Tipo de embarcación a buscar
     * @return Lista de embarcaciones del tipo especificado
     */
    public List<embarcacion> buscarPorTipo(TiposBarcos tipo) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.buscar.por_tipo");
            System.out.println("🔍 Buscando embarcaciones por tipo: " + tipo);
            System.out.println("🔍 Consulta SQL: " + query);

            if (query != null) {
                List<embarcacion> resultado = jdbcTemplate.query(query,
                        new Object[] { tipo.name() },
                        new RowMapper<embarcacion>() {
                            public embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                embarcacion e = new embarcacion();
                                e.setMatricula(rs.getString("matricula"));
                                e.setNombre(rs.getString("nombre"));
                                e.setTipo(TiposBarcos.valueOf(rs.getString("tipo_embarcacion")));
                                e.setPlaza(rs.getInt("numero_plazas"));
                                e.setDimensiones(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    e.setIdPatronAsignado(idPatron);
                                }

                                System.out.println(
                                        "✅ Encontrada embarcación: " + e.getMatricula() + " - " + e.getNombre() +
                                                " - Patrón: " + (e.getIdPatronAsignado() != null ? "SÍ" : "NO"));
                                return e;
                            }
                        });
                System.out.println("📊 Total embarcaciones encontradas: " + resultado.size());
                return resultado;
            } else {
                System.err.println("⚠️ Usando consulta por defecto para buscar por tipo");
                String defaultQuery = "SELECT * FROM Embarcaciones WHERE tipo_embarcacion = ? ORDER BY nombre";
                List<embarcacion> resultado = jdbcTemplate.query(defaultQuery,
                        new Object[] { tipo.name() },
                        new RowMapper<embarcacion>() {

                            public embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                                embarcacion e = new embarcacion();
                                e.setMatricula(rs.getString("matricula"));
                                e.setNombre(rs.getString("nombre"));
                                e.setTipo(TiposBarcos.valueOf(rs.getString("tipo_embarcacion")));
                                e.setPlaza(rs.getInt("numero_plazas"));
                                e.setDimensiones(parseDimensiones(rs.getString("dimensiones")));

                                // Nuevo campo: patrón asignado
                                Integer idPatron = rs.getInt("id_patron_asignado");
                                if (!rs.wasNull()) {
                                    e.setIdPatronAsignado(idPatron);
                                }

                                return e;
                            }
                        });
                System.out.println("📊 Total embarcaciones encontradas (consulta por defecto): " + resultado.size());
                return resultado;
            }
        } catch (DataAccessException e) {
            System.err.println("❌ Error buscando embarcaciones por tipo: " + tipo);
            e.printStackTrace();
            return List.of();
        } catch (Exception e) {
            System.err.println("❌ Error inesperado buscando embarcaciones por tipo: " + tipo);
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Obtiene embarcaciones con información del patrón asignado
     * 
     * @return Lista de embarcaciones con información del patrón
     */
    public List<embarcacion> obtenerEmbarcacionesConPatron() {
        try {
            String query = sqlQueries.getProperty("embarcaciones.obtener.con.patron");
            if (query == null) {
                query = "SELECT e.*, em.nombre AS nombre_patron, em.apellidos AS apellidos_patron " +
                        "FROM Embarcaciones e " +
                        "LEFT JOIN Empleado em ON e.id_patron_asignado = em.id_empleado";
            }

            return jdbcTemplate.query(query, new RowMapper<embarcacion>() {
                public embarcacion mapRow(ResultSet rs, int rowNum) throws SQLException {
                    embarcacion e = new embarcacion();
                    e.setMatricula(rs.getString("matricula"));
                    e.setNombre(rs.getString("nombre"));
                    e.setTipo(TiposBarcos.valueOf(rs.getString("tipo_embarcacion")));
                    e.setPlaza(rs.getInt("numero_plazas"));
                    e.setDimensiones(parseDimensiones(rs.getString("dimensiones")));

                    Integer idPatron = rs.getInt("id_patron_asignado");
                    if (!rs.wasNull()) {
                        e.setIdPatronAsignado(idPatron);
                    }

                    return e;
                }
            });

        } catch (DataAccessException e) {
            System.err.println("Error obteniendo embarcaciones con patrón");
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Actualiza los campos de información de una embarcación, excepto la matrícula
     * 
     * @param embarcacion Objeto embarcacion con los datos actualizados
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    public boolean actualizarEmbarcacion(embarcacion embarcacion) {
        try {
            String query = sqlQueries.getProperty("embarcaciones.actualizar");
            if (query == null) {
                query = "UPDATE Embarcaciones SET tipo_embarcacion = ?, nombre = ?, numero_plazas = ?, dimensiones = ? WHERE matricula = ?";
            }

            int result = jdbcTemplate.update(query,
                    embarcacion.getTipo().name(),
                    embarcacion.getNombre().trim(),
                    embarcacion.getPlaza(),
                    embarcacion.getDimensiones() + "m",
                    embarcacion.getMatricula().trim().toUpperCase());

            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error actualizando embarcación: " + e.getMessage());
            e.printStackTrace();
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

            int result = jdbcTemplate.update(query,
                    idPatron,
                    matricula.trim().toUpperCase());

            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error vinculando patrón a embarcación: " + e.getMessage());
            e.printStackTrace();
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

            int result = jdbcTemplate.update(query,
                    matricula.trim().toUpperCase());

            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error desvinculando patrón de embarcación: " + e.getMessage());
            e.printStackTrace();
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
            embarcacion emb = buscarPorMatricula(matricula);
            if (emb == null) {
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

            int result = jdbcTemplate.update(query, matricula.trim().toUpperCase());
            return result > 0;
        } catch (DataAccessException e) {
            System.err.println("Error eliminando embarcación: " + e.getMessage());
            e.printStackTrace();
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
        } catch (DataAccessException e) {
            System.err.println("Error verificando existencia de embarcación: " + e.getMessage());
            e.printStackTrace();
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