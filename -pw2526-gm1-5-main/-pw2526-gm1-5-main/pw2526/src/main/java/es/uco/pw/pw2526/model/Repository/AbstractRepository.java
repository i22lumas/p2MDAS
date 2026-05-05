package es.uco.pw.pw2526.model.Repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractRepository {

    protected JdbcTemplate jdbcTemplate;
    protected Properties sqlQueries;
    protected String sqlQueriesFileName;

    private void createProperties(String fileName) {
        sqlQueries = new Properties();
        try {
            cargarArchivoProperties(fileName);
        } catch (IOException excepcion) {
            manejarErrorProperties(excepcion);
        }
    }

    private void cargarArchivoProperties(String fileName) throws IOException {
        File archivoSql = new File(fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoSql))) {
            sqlQueries.load(reader);
            System.out.println("✅ Archivo " + fileName + " cargado correctamente.");
        }
    }

    private void manejarErrorProperties(IOException excepcion) {
        System.err.println("❌ Error creando objeto Properties para SQL queries: " + excepcion.getMessage());
        excepcion.printStackTrace();
        throw new RuntimeException("Error cargando propiedades SQL", excepcion);
    }

    /**
     * Establece el archivo de propiedades SQL y lo carga
     * 
     * @param sqlQueriesFileName Ruta del archivo de propiedades SQL
     */
    public void setSQLQueriesFileName(String sqlQueriesFileName) {
        this.sqlQueriesFileName = sqlQueriesFileName;
        createProperties(sqlQueriesFileName);
    }

    /**
     * Carga y devuelve las propiedades SQL
     * 
     * @return Properties con las consultas SQL cargadas
     */
    public Properties cargarSqlProperties() {
        inicializarPropertiesSiEsNecesario();
        return devolverPropertiesOGenerarVacio();
    }

    private void inicializarPropertiesSiEsNecesario() {
        if (this.sqlQueries == null && this.sqlQueriesFileName != null) {
            createProperties(this.sqlQueriesFileName);
        }
    }

    private Properties devolverPropertiesOGenerarVacio() {
        if (this.sqlQueries == null) {
            System.err.println("Advertencia: Propiedades SQL no inicializadas. Se devolverá un objeto vacío.");
            return new Properties();
        }
        return this.sqlQueries;
    }
}