package es.uco.pw.pw2526.model.domain.inscripcion;

import java.time.LocalDate;

public class Inscripcion {

    public enum TipoInscripcion {
        INDIVIDUAL, FAMILIAR
    }

    private int id;
    private int idSocioTitular;
    private TipoInscripcion tipoInscripcion;
    private double cuotaAnual;
    private LocalDate fechaCreacion;

    public Inscripcion() {
        this.id = -1;
        this.idSocioTitular = -1;
        this.tipoInscripcion = TipoInscripcion.INDIVIDUAL;
        this.cuotaAnual = 0.0;
        this.fechaCreacion = LocalDate.now();
    }

    public Inscripcion(int id, int idSocioTitular, TipoInscripcion tipoInscripcion, double cuotaAnual, LocalDate fechaCreacion) {
        this.id = id;
        this.idSocioTitular = idSocioTitular;
        this.tipoInscripcion = tipoInscripcion;
        this.cuotaAnual = cuotaAnual;
        this.fechaCreacion = fechaCreacion;
    }

  
    public boolean esIndividual() {
        return this.tipoInscripcion == TipoInscripcion.INDIVIDUAL;
    }

    public boolean esFamiliar() {
        return this.tipoInscripcion == TipoInscripcion.FAMILIAR;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdSocioTitular() { return idSocioTitular; }
    public void setIdSocioTitular(int idSocioTitular) { this.idSocioTitular = idSocioTitular; }
    public TipoInscripcion getTipoInscripcion() { return tipoInscripcion; }
    public void setTipoInscripcion(TipoInscripcion tipoInscripcion) { this.tipoInscripcion = tipoInscripcion; }
    public double getCuotaAnual() { return cuotaAnual; }
    public void setCuotaAnual(double cuotaAnual) { this.cuotaAnual = cuotaAnual; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    @Override
    public String toString() {
        return "Inscripcion [id=" + id + ", titular=" + idSocioTitular + ", tipo=" + tipoInscripcion + ", cuota=" + cuotaAnual + "]";
    }
}