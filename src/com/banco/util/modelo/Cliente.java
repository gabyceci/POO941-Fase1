package com.banco.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class Cliente {

    private static final Pattern PATRON_DUI = Pattern.compile("^\\d{8}-\\d$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\d{4}-?\\d{4}$");
    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    private int idCliente;              
    private String codigoCliente;      
    private String dui;                 
    private String nombreCompleto;
    private String telefono;
    private String correo;
    private String direccion;
    private EstadoCliente estado;

    public Cliente() {
        this.estado = EstadoCliente.ACTIVO;
    }

    public Cliente(String codigoCliente, String dui, String nombreCompleto,
                   String telefono, String correo, String direccion, EstadoCliente estado) {
        this(0, codigoCliente, dui, nombreCompleto, telefono, correo, direccion, estado);
    }

    public Cliente(int idCliente, String codigoCliente, String dui, String nombreCompleto,
                   String telefono, String correo, String direccion, EstadoCliente estado) {
        this.idCliente = idCliente;
        setCodigoCliente(codigoCliente);
        setDui(dui);
        setNombreCompleto(nombreCompleto);
        setTelefono(telefono);
        setCorreo(correo);
        setDireccion(direccion);
        this.estado = estado;
    }


    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getCodigoCliente() { return codigoCliente; }
    public void setCodigoCliente(String codigoCliente) { this.codigoCliente = limpiar(codigoCliente); }

    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = limpiar(dui); }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = limpiar(nombreCompleto); }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = limpiar(telefono); }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = limpiar(correo); }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = limpiar(direccion); }

    public EstadoCliente getEstado() { return estado; }
    public void setEstado(EstadoCliente estado) { this.estado = estado; }

   
    public List<String> validar() {
        List<String> errores = new ArrayList<>();

        if (codigoCliente.isEmpty() || codigoCliente.length() > 10) {
            errores.add("El código de cliente es obligatorio (máximo 10 caracteres).");
        }
        if (!PATRON_DUI.matcher(dui).matches()) {
            errores.add("El DUI debe tener el formato 00000000-0 (8 dígitos, guion y 1 dígito).");
        }
        if (nombreCompleto.isEmpty() || nombreCompleto.length() > 100) {
            errores.add("El nombre completo es obligatorio (máximo 100 caracteres).");
        }
        if (!PATRON_TELEFONO.matcher(telefono).matches()) {
            errores.add("El teléfono debe tener 8 dígitos (ej.: 7000-0001).");
        }
        if (correo.length() > 100 || !PATRON_CORREO.matcher(correo).matches()) {
            errores.add("El correo electrónico no es válido.");
        }
        if (direccion.isEmpty() || direccion.length() > 200) {
            errores.add("La dirección es obligatoria (máximo 200 caracteres).");
        }
        if (estado == null) {
            errores.add("Debe seleccionar un estado.");
        }
        return errores;
    }

    public void normalizarTelefono() {
        if (telefono.matches("^\\d{8}$")) {
            telefono = telefono.substring(0, 4) + "-" + telefono.substring(4);
        }
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    @Override
    public String toString() {
        return codigoCliente + " - " + nombreCompleto;
    }
}
