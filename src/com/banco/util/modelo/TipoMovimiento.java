package com.banco.util.modelo;

public enum TipoMovimiento {
    DEPOSITO("Depósito"),
    RETIRO("Retiro"),
    TRANSFERENCIA("Transferencia"); 
    
    private final String etiqueta;
    TipoMovimiento(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    public static TipoMovimiento desdeEtiqueta(String texto) {
        for (TipoMovimiento t : values()) {
            if (t.etiqueta.equalsIgnoreCase(texto == null ? "" : texto.trim())) return t;
        }
        throw new IllegalArgumentException("Tipo de movimiento desconocido: " + texto);
    }
    @Override public String toString() { return etiqueta; }
}