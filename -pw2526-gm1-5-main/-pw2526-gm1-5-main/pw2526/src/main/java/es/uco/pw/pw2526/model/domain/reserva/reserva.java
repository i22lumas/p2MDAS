package es.uco.pw.pw2526.model.domain.reserva;

import java.time.LocalDate;

public class Reserva {
    private Integer idReserva;
    private String propositoActividad;
    private Integer idSocioSolicitante;
    private LocalDate fechaActividad;
    private Integer plazasSolicitadas;
    private Double precioTotal;
    private String matriculaEmbarcacion;
    private Integer idPatron;

    public Reserva() {
        this.idReserva = null;
        this.propositoActividad = "";
        this.idSocioSolicitante = null;
        this.fechaActividad = LocalDate.now();
        this.plazasSolicitadas = null;
        this.precioTotal = null;
        this.matriculaEmbarcacion = "";
        this.idPatron = null;
    }

    public Reserva(Integer idReserva, String propositoActividad, Integer idSocioSolicitante,
            LocalDate fechaActividad, Integer plazasSolicitadas, Double precioTotal,
            String matriculaEmbarcacion, Integer idPatron) {
        this.idReserva = idReserva;
        this.propositoActividad = propositoActividad;
        this.idSocioSolicitante = idSocioSolicitante;
        this.fechaActividad = fechaActividad;
        this.plazasSolicitadas = plazasSolicitadas;
        this.precioTotal = precioTotal;
        this.matriculaEmbarcacion = matriculaEmbarcacion;
        this.idPatron = idPatron;
    }

    // Getters y Setters
    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public String getPropositoActividad() {
        return propositoActividad;
    }

    public void setPropositoActividad(String propositoActividad) {
        this.propositoActividad = propositoActividad;
    }

    public Integer getIdSocioSolicitante() {
        return idSocioSolicitante;
    }

    public void setIdSocioSolicitante(Integer idSocioSolicitante) {
        this.idSocioSolicitante = idSocioSolicitante;
    }

    public LocalDate getFechaActividad() {
        return fechaActividad;
    }

    public void setFechaActividad(LocalDate fechaActividad) {
        this.fechaActividad = fechaActividad;
    }

    public Integer getPlazasSolicitadas() {
        return plazasSolicitadas;
    }

    public void setPlazasSolicitadas(Integer plazasSolicitadas) {
        this.plazasSolicitadas = plazasSolicitadas;
    }

    public Double getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(Double precioTotal) {
        this.precioTotal = precioTotal;
    }

    public String getMatriculaEmbarcacion() {
        return matriculaEmbarcacion;
    }

    public void setMatriculaEmbarcacion(String matriculaEmbarcacion) {
        this.matriculaEmbarcacion = matriculaEmbarcacion;
    }

    public Integer getIdPatron() {
        return idPatron;
    }

    public void setIdPatron(Integer idPatron) {
        this.idPatron = idPatron;
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "idReserva=" + idReserva +
                ", propositoActividad='" + propositoActividad + '\'' +
                ", idSocioSolicitante=" + idSocioSolicitante +
                ", fechaActividad=" + fechaActividad +
                ", plazasSolicitadas=" + plazasSolicitadas +
                ", precioTotal=" + precioTotal +
                ", matriculaEmbarcacion='" + matriculaEmbarcacion + '\'' +
                ", idPatron=" + idPatron +
                '}';
    }
}