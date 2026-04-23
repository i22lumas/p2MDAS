
package es.uco.pw.pw2526.model.domain.embarcacion;

public class Embarcacion {
    private String matricula;
    private TipoEmbarcacion tipo;
    private String nombre;
    private int numeroPlazas;
    private float esloraEnMetros;
    private Integer idPatronAsignado;

    public Embarcacion() {
        this.matricula = "";
        this.tipo = TipoEmbarcacion.NONE;
        this.nombre = "";
        this.numeroPlazas = -1;
        this.esloraEnMetros = -1;
        this.idPatronAsignado = null;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public TipoEmbarcacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoEmbarcacion tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getNumeroPlazas() {
        return numeroPlazas;
    }

    public void setNumeroPlazas(int numeroPlazas) {
        this.numeroPlazas = numeroPlazas;
    }

    public float getEsloraEnMetros() {
        return esloraEnMetros;
    }

    public void setEsloraEnMetros(float esloraEnMetros) {
        this.esloraEnMetros = esloraEnMetros;
    }

    public Integer getIdPatronAsignado() {
        return idPatronAsignado;
    }

    public void setIdPatronAsignado(Integer idPatronAsignado) {
        this.idPatronAsignado = idPatronAsignado;
    }
}
