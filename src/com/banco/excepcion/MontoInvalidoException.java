package com.banco.excepcion;

import java.math.BigDecimal;

public class MontoInvalidoException extends OperacionBancariaException {
    public MontoInvalidoException(BigDecimal monto) {
        super("El monto " + (monto == null ? "indicado" : monto.toPlainString())
            + " no es válido: debe ser un número mayor que cero.");
    }
}