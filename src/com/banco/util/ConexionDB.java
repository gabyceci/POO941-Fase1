package com.banco.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ConexionDB {

    private static final String HOST     = "localhost";
    private static final String PUERTO   = "3306";
    private static final String BASE     = "banco_udb";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private ConexionDB() { }

    public static Connection getConexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de MySQL (falta el .jar)", e);
        }
    }

    public static void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try (Connection con = getConexion()) {
            System.out.println("¡Conexión exitosa a " + BASE + "!");
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}