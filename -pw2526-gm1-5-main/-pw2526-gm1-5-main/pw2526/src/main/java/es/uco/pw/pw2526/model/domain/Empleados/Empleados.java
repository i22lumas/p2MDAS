package es.uco.pw.pw2526.model.domain.Empleados;

import java.time.LocalDate;

public class Empleados {

    private int id;
    private String nombre;
    private String apellidos;
    private String dni;
    private LocalDate fech_nacimiento;
    private LocalDate fech_expedicion_titulo;

    public Empleados() {
        this.id = -1;
        this.nombre = "";
        this.apellidos = "";
        this.dni = "";
        this.fech_nacimiento = LocalDate.of(1899, 11, 19);
        this.fech_expedicion_titulo = LocalDate.of(1899, 11, 19);

    }

    public Empleados(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getDni() {
        return dni;
    }

    public LocalDate getFech_nacimiento() {
        return fech_nacimiento;
    }

    public LocalDate getFech_expedicion_titulo() {
        return fech_expedicion_titulo;
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
        return "Empleados [id=" + id + ", nombre=" + nombre + "]";
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setFech_nacimiento(LocalDate fech_nacimiento) {
        this.fech_nacimiento = fech_nacimiento;
    }

    public void setFech_expedicion_titulo(LocalDate fech_expedicion_titulo) {
        this.fech_expedicion_titulo = fech_expedicion_titulo;
    }

}
