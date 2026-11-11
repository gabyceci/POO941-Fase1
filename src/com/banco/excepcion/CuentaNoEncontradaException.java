package com.banco.excepcion;

public class CuentaNoEncontradaException extends OperacionBancariaException {
    private final String numeroCuenta;
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No existe ninguna cuenta con el número " + numeroCuenta + ".");
        this.numeroCuenta = numeroCuenta;
    }
    public String getNumeroCuenta() { return numeroCuenta; }
}