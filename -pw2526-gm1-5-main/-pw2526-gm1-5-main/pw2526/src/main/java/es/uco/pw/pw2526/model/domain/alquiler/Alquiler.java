package es.uco.pw.pw2526.model.domain.alquiler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Alquiler {

    private int idAlquiler;
    private int idSocioTitular;
    private String dniSocioTitular; 
    private String matriculaEmbarcacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int plazasSolicitadas;
    private double precioTotal;
    private List<String> dnisTripulantes;

    public Alquiler() {
        this.idAlquiler = -1;
        this.idSocioTitular = -1;
        this.dniSocioTitular = "";
        this.matriculaEmbarcacion = "";
        this.fechaInicio = null;
        this.fechaFin = null;
        this.plazasSolicitadas = -1;
        this.precioTotal = 0.0;
        this.dnisTripulantes = new ArrayList<>();
    }

    public int getIdAlquiler() {
        return idAlquiler;
    }

    public void setIdAlquiler(int idAlquiler) {
        this.idAlquiler = idAlquiler;
    }

    public int getIdSocioTitular() {
        return idSocioTitular;
    }

    public String getDniSocioTitular() {
        return dniSocioTitular;
    }

    public String getMatriculaEmbarcacion() {
        return matriculaEmbarcacion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getPlazasSolicitadas() {
        return plazasSolicitadas;
    }

    public double getPrecioTotal() {
        return precioTotal;
    }

    public List<String> getDnisTripulantes() {
        return dnisTripulantes;
    }

    public void setIdSocioTitular(int idSocioTitular) {
        this.idSocioTitular = idSocioTitular;
    }

    public void setDniSocioTitular(String dniSocioTitular) {
        this.dniSocioTitular = dniSocioTitular;
    }

    public void setMatriculaEmbarcacion(String matriculaEmbarcacion) {
        this.matriculaEmbarcacion = matriculaEmbarcacion;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setPlazasSolicitadas(int plazasSolicitadas) {
        this.plazasSolicitadas = plazasSolicitadas;
    }

    public void setPrecioTotal(double precioTotal) {
        this.precioTotal = precioTotal;
    }

    public void setDnisTripulantes(List<String> dnisTripulantes) {
        this.dnisTripulantes = dnisTripulantes;
    }

    public void agregarDniTripulante(String dni) {
        if (this.dnisTripulantes == null) {
            this.dnisTripulantes = new ArrayList<>();
        }
        this.dnisTripulantes.add(dni);
    }


    public String obtenerTripulantesComoString() {
        if (dnisTripulantes == null || dnisTripulantes.isEmpty()) {
            return dniSocioTitular;
        }

        List<String> todos = new ArrayList<>();
        todos.add(dniSocioTitular);
        todos.addAll(dnisTripulantes);

        return String.join(", ", todos);
    }
}