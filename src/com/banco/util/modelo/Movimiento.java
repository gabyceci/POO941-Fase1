package com.banco.util.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {
    private int idMovimiento;
    private int idCuenta;
    private String numeroCuenta;       
    private TipoMovimiento tipoMovimiento;
    private BigDecimal monto;
    private LocalDateTime fechaHora;
    private String descripcion;
    private BigDecimal saldoResultante;

    public Movimiento() { }

    public Movimiento(int idCuenta, TipoMovimiento tipoMovimiento, BigDecimal monto,
                      String descripcion, BigDecimal saldoResultante) {
        this.idCuenta = idCuenta;
        this.tipoMovimiento = tipoMovimiento;
        this.monto = monto;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.saldoResultante = saldoResultante;
        this.fechaHora = LocalDateTime.now();
    }

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public int getIdCuenta() { return idCuenta; }
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String n) { this.numeroCuenta = n == null ? "" : n.trim(); }
    public TipoMovimiento getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(TipoMovimiento t) { this.tipoMovimiento = t; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime f) { this.fechaHora = f; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String d) { this.descripcion = d == null ? "" : d.trim(); }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(BigDecimal s) { this.saldoResultante = s; }
}