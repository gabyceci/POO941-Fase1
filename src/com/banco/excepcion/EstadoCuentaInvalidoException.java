package com.banco.excepcion;

import com.banco.util.modelo.EstadoCuenta;

public class EstadoCuentaInvalidoException extends OperacionBancariaException {
    public EstadoCuentaInvalidoException(String numeroCuenta, EstadoCuenta estado) {
        super("La cuenta " + numeroCuenta + " se encuentra " + estado
            + ". Solo las cuentas Activas pueden realizar operaciones.");
    }
}