package es.uco.pw.pw2526.model.domain.Socio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Socio {

    public enum TipoMiembro {
        TITULAR, CONYUGE, HIJO
    }

    private int id;
    private String dni;
    private String nombre;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String direccion;
    private LocalDate fechaInscripcion;
    private boolean tieneTituloPatron;
    private TipoMiembro tipoMiembro; 
    private int inscripcionId;
    private Integer idSocioTitularFk; 

    public Socio() {
        this.id = -1;
        this.dni = "";
        this.nombre = "";
        this.apellidos = "";
        this.fechaNacimiento = null;
        this.direccion = "";
        this.fechaInscripcion = null;
        this.tieneTituloPatron = false;
        this.tipoMiembro = TipoMiembro.TITULAR; 
        this.inscripcionId = -1;
        this.idSocioTitularFk = null;
    }



    public boolean esMayorDeEdad() {
        if (this.fechaNacimiento == null) {
            return false;
        }
        long years = ChronoUnit.YEARS.between(this.fechaNacimiento, LocalDate.now());
        return years >= 18;
    }

    public boolean esTitular() {
        return this.tipoMiembro == TipoMiembro.TITULAR;
    }
    
    public boolean esPatron() {
        return tieneTituloPatron;
    }
    
    public boolean estaVinculadoAInscripcion() {
        return this.inscripcionId != -1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public void setFechaInscripcion(LocalDate fechaInscripcion) { this.fechaInscripcion = fechaInscripcion; }
    public boolean getTieneTituloPatron() { return tieneTituloPatron; }
    public void setTieneTituloPatron(boolean tieneTituloPatron) { this.tieneTituloPatron = tieneTituloPatron; }
    public TipoMiembro getTipoMiembro() { return tipoMiembro; }
    public void setTipoMiembro(TipoMiembro tipoMiembro) { this.tipoMiembro = tipoMiembro; }
    public int getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(int inscripcionId) { this.inscripcionId = inscripcionId; }
    public Integer getIdSocioTitularFk() { return idSocioTitularFk; }
    public void setIdSocioTitularFk(Integer idSocioTitularFk) { this.idSocioTitularFk = idSocioTitularFk; }

    @Override
    public String toString() {
        return "Socio [id=" + id + ", dni=" + dni + ", nombre=" + nombre + ", tipo=" + tipoMiembro + ", inscripcion=" + inscripcionId + "]";
    }
}