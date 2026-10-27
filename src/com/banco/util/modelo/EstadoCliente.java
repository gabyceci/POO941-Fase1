package com.banco.modelo;


public enum EstadoCliente {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    BLOQUEADO("Bloqueado");

    private final String etiqueta;

    EstadoCliente(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoCliente desdeEtiqueta(String texto) {
        for (EstadoCliente e : values()) {
            if (e.etiqueta.equalsIgnoreCase(texto)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de cliente no válido: " + texto);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
