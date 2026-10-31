package com.banco.util.modelo;

public enum TipoCuenta {
    AHORRO("Ahorro"),
    CORRIENTE("Corriente");

    private final String etiqueta;

    TipoCuenta(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }

    public static TipoCuenta desdeEtiqueta(String texto) {
        for (TipoCuenta t : values()) {
            if (t.etiqueta.equalsIgnoreCase(texto)) return t;
        }
        throw new IllegalArgumentException("Tipo no válido: " + texto);
    }
    @Override
    public String toString() { return etiqueta; }
}