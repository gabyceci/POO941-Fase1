/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.util.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Cuenta {
    private int idCuenta;
    private String numeroCuenta;
    private int idCliente;
    private String codigoCliente; 
    private String nombreCliente;
    private TipoCuenta tipoCuenta;
    private BigDecimal saldo;
    private LocalDate fechaApertura;
    private EstadoCuenta estado;

    public Cuenta() {
        this.tipoCuenta = TipoCuenta.AHORRO;
        this.estado = EstadoCuenta.ACTIVA;
        this.saldo = BigDecimal.ZERO;
        this.fechaApertura = LocalDate.now();
    }

    public Cuenta(int idCuenta, String numeroCuenta, int idCliente, String codigoCliente,
                  String nombreCliente, TipoCuenta tipoCuenta, BigDecimal saldo,
                  LocalDate fechaApertura, EstadoCuenta estado) {
        this.idCuenta = idCuenta;
        setNumeroCuenta(numeroCuenta);
        this.idCliente = idCliente;
        setCodigoCliente(codigoCliente);
        setNombreCliente(nombreCliente);
        this.tipoCuenta = tipoCuenta;
        this.saldo = saldo;
        this.fechaApertura = fechaApertura;
        this.estado = estado;
    }

    public int getIdCuenta() { return idCuenta; }
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String n) { this.numeroCuenta = limpiar(n); }
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getCodigoCliente() { return codigoCliente; }
    public void setCodigoCliente(String c) { this.codigoCliente = limpiar(c); }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String n) { this.nombreCliente = limpiar(n); }
    public TipoCuenta getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(TipoCuenta t) { this.tipoCuenta = t; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public LocalDate getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDate f) { this.fechaApertura = f; }
    public EstadoCuenta getEstado() { return estado; }
    public void setEstado(EstadoCuenta estado) { this.estado = estado; }

    public List<String> validar() {
        List<String> errores = new ArrayList<>();
        if (numeroCuenta == null || numeroCuenta.isEmpty() || numeroCuenta.length() > 20)
            errores.add("El número de cuenta es obligatorio (máximo 20 caracteres).");
        if (idCliente <= 0)
            errores.add("Debe seleccionar un cliente activo para la cuenta.");
        if (tipoCuenta == null)
            errores.add("Debe seleccionar el tipo de cuenta.");
        if (saldo == null || saldo.signum() < 0)
            errores.add("El saldo inicial no puede ser negativo.");
        if (estado == null)
            errores.add("Debe seleccionar un estado.");
        return errores;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    @Override
    public String toString() {
        return numeroCuenta + " - " + nombreCliente;
    }
}