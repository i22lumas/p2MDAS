
package es.uco.pw.pw2526.model.domain.embarcacion;

public class embarcacion {
    private String Matricula;
    private TiposBarcos tipo;
    private String Nombre;
    private int Plaza;
    private float dimensiones;
    private Integer idPatronAsignado;

    public embarcacion() {
        this.Matricula = "";
        this.tipo = TiposBarcos.NONE;
        this.Nombre = "";
        this.Plaza = -1;
        this.dimensiones = -1;
        this.idPatronAsignado = null;
    }

    public String getMatricula() {
        return Matricula;
    }

    public void setMatricula(String matricula) {
        Matricula = matricula;
    }

    public TiposBarcos getTipo() {
        return tipo;
    }

    public void setTipo(TiposBarcos tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public int getPlaza() {
        return Plaza;
    }

    public void setPlaza(int plaza) {
        Plaza = plaza;
    }

    public float getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(float dimensiones) {
        this.dimensiones = dimensiones;
    }

    public Integer getIdPatronAsignado() {
        return idPatronAsignado;
    }

    public void setIdPatronAsignado(Integer idPatronAsignado) {
        this.idPatronAsignado = idPatronAsignado;
    }
}
