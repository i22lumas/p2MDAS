package es.uco.pw.pw2526.model.domain.Asignacion;

import java.time.LocalDate;

public class Asignacion {

    private int idAsignacion;
    private String matriculaEmbarcacion;
    private int idEmpleado;
    private LocalDate fechaAsignacion;
    private LocalDate fechaFin;

    public Asignacion() {
    }

    public Asignacion(int idAsignacion, String matriculaEmbarcacion, int idEmpleado, LocalDate fechaAsignacion,
            LocalDate fechaFin) {
        this.idAsignacion = idAsignacion;
        this.matriculaEmbarcacion = matriculaEmbarcacion;
        this.idEmpleado = idEmpleado;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaFin = fechaFin;
    }

    public int getIdAsignacion() {
        return idAsignacion;
    }

    public void setIdAsignacion(int idAsignacion) {
        this.idAsignacion = idAsignacion;
    }

    public String getMatriculaEmbarcacion() {
        return matriculaEmbarcacion;
    }

    public void setMatriculaEmbarcacion(String matriculaEmbarcacion) {
        this.matriculaEmbarcacion = matriculaEmbarcacion;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDate fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
}
