package com.banco.dao;

import com.banco.modelo.Cliente;
import com.banco.modelo.EstadoCliente;
import com.banco.util.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class ClienteDAO {

    private static final String COLUMNAS =
            "id_cliente, codigo_cliente, dui, nombre_completo, telefono, correo, direccion, estado";

    private static final String SQL_INSERT =
            "INSERT INTO cliente (codigo_cliente, dui, nombre_completo, telefono, correo, direccion, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE cliente SET dui = ?, nombre_completo = ?, telefono = ?, correo = ?, "
            + "direccion = ?, estado = ? WHERE id_cliente = ?";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE cliente SET estado = ? WHERE id_cliente = ?";

    private static final String SQL_SELECT_TODOS =
            "SELECT " + COLUMNAS + " FROM cliente ORDER BY id_cliente";

    private static final String SQL_SELECT_POR_ID =
            "SELECT " + COLUMNAS + " FROM cliente WHERE id_cliente = ?";

    private static final String SQL_BUSCAR =
            "SELECT " + COLUMNAS + " FROM cliente "
            + "WHERE (codigo_cliente LIKE ? OR dui LIKE ? OR nombre_completo LIKE ?) ";

    private static final String SQL_ORDEN = "ORDER BY id_cliente";

    private static final String SQL_SIGUIENTE_CODIGO =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_cliente, 5) AS UNSIGNED)), 0) + 1 FROM cliente";

    private static final String SQL_EXISTE_DUI =
            "SELECT COUNT(*) FROM cliente WHERE dui = ? AND id_cliente <> ?";

    
    public int insertar(Cliente c) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setString(i++, c.getCodigoCliente());
            ps.setString(i++, c.getDui());
            ps.setString(i++, c.getNombreCompleto());
            ps.setString(i++, c.getTelefono());
            ps.setString(i++, c.getCorreo());
            ps.setString(i++, c.getDireccion());
            ps.setString(i, c.getEstado().getEtiqueta());

            int filas = ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    c.setIdCliente(claves.getInt(1));
                }
            }
            return filas;
        }
    }


    public List<Cliente> listar() throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {
            return leerClientes(rs);
        }
    }

   
    public List<Cliente> buscar(String texto, EstadoCliente estado) throws SQLException {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        String sql = SQL_BUSCAR + (estado != null ? "AND estado = ? " : "") + SQL_ORDEN;

        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            if (estado != null) {
                ps.setString(4, estado.getEtiqueta());
            }
            try (ResultSet rs = ps.executeQuery()) {
                return leerClientes(rs);
            }
        }
    }

    public List<Cliente> listarPorEstado(EstadoCliente estado) throws SQLException {
        return buscar("", estado);
    }

    public Cliente obtenerPorId(int idCliente) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public String siguienteCodigo() throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_SIGUIENTE_CODIGO);
             ResultSet rs = ps.executeQuery()) {
            int numero = rs.next() ? rs.getInt(1) : 1;
            return String.format("CLI-%04d", numero);
        }
    }

    
    public boolean existeDui(String dui, int idExcluir) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_EXISTE_DUI)) {
            ps.setString(1, dui);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    
    public int actualizar(Cliente c) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {

            int i = 1;
            ps.setString(i++, c.getDui());
            ps.setString(i++, c.getNombreCompleto());
            ps.setString(i++, c.getTelefono());
            ps.setString(i++, c.getCorreo());
            ps.setString(i++, c.getDireccion());
            ps.setString(i++, c.getEstado().getEtiqueta());
            ps.setInt(i, c.getIdCliente());
            return ps.executeUpdate();
        }
    }

   
    public int cambiarEstado(int idCliente, EstadoCliente nuevoEstado) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, nuevoEstado.getEtiqueta());
            ps.setInt(2, idCliente);
            return ps.executeUpdate();
        }
    }


    private List<Cliente> leerClientes(ResultSet rs) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("codigo_cliente"),
                rs.getString("dui"),
                rs.getString("nombre_completo"),
                rs.getString("telefono"),
                rs.getString("correo"),
                rs.getString("direccion"),
                EstadoCliente.desdeEtiqueta(rs.getString("estado")));
    }
    
    private static final String SQL_DELETE = "DELETE FROM cliente WHERE id_cliente = ?";

public int eliminar(int idCliente) throws SQLException {
    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
        ps.setInt(1, idCliente);
        return ps.executeUpdate();
    }
}
}
