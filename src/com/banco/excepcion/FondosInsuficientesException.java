package com.banco.excepcion;

import java.math.BigDecimal;

public class FondosInsuficientesException extends OperacionBancariaException {
    private final BigDecimal saldoDisponible;
    private final BigDecimal montoSolicitado;
    public FondosInsuficientesException(BigDecimal saldoDisponible, BigDecimal montoSolicitado) {
        super("Fondos insuficientes: saldo disponible " + saldoDisponible.toPlainString()
            + ", monto solicitado " + montoSolicitado.toPlainString() + ".");
        this.saldoDisponible = saldoDisponible;
        this.montoSolicitado = montoSolicitado;
    }
    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
}