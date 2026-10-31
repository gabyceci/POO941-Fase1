package com.banco.util.modelo;

public enum EstadoCuenta {
    ACTIVA("Activa"),
    INACTIVA("Inactiva"),
    BLOQUEADA("Bloqueada");

    private final String etiqueta;

    EstadoCuenta(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }

    public static EstadoCuenta desdeEtiqueta(String texto) {
        for (EstadoCuenta e : values()) {
            if (e.etiqueta.equalsIgnoreCase(texto)) return e;
        }
        throw new IllegalArgumentException("Estado no válido: " + texto);
    }
    @Override
    public String toString() { return etiqueta; }
}
