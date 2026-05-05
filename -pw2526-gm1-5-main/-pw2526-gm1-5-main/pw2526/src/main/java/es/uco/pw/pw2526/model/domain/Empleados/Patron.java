package es.uco.pw.pw2526.model.domain.Empleados;

import java.time.LocalDate;

public class Patron {

    private int id;
    private String nombre;
    private String apellidos;
    private String dni;
    private LocalDate fechaNacimiento;
    private LocalDate fechaExpedicionTitulo;

    public Patron() {
        this.id = -1;
        this.nombre = "";
        this.apellidos = "";
        this.dni = "";
        this.fechaNacimiento = LocalDate.of(1899, 11, 19);
        this.fechaExpedicionTitulo = LocalDate.of(1899, 11, 19);
    }

    public Patron(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getDni() {
        return dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public LocalDate getFechaExpedicionTitulo() {
        return fechaExpedicionTitulo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "Patron [id=" + id + ", nombre=" + nombre + "]";
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setFechaExpedicionTitulo(LocalDate fechaExpedicionTitulo) {
        this.fechaExpedicionTitulo = fechaExpedicionTitulo;
    }

}
