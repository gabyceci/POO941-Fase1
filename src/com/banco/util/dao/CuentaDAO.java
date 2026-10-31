/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.util.dao;

import com.banco.modelo.Cliente;
import com.banco.util.modelo.Cuenta;
import com.banco.util.modelo.EstadoCuenta;
import com.banco.util.modelo.TipoCuenta;
import com.banco.util.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {
    private static final String COLUMNAS =
            "c.id_cuenta, c.numero_cuenta, c.id_cliente, cl.codigo_cliente, cl.nombre_completo, "
          + "c.tipo_cuenta, c.saldo, c.fecha_apertura, c.estado";
    private static final String DESDE =
            " FROM cuenta_bancaria c INNER JOIN cliente cl ON cl.id_cliente = c.id_cliente ";
    private static final String SQL_INSERT =
            "INSERT INTO cuenta_bancaria (numero_cuenta, id_cliente, tipo_cuenta, saldo, fecha_apertura, estado) "
          + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
        "UPDATE cuenta_bancaria SET id_cliente = ?, tipo_cuenta = ?, saldo = ?, estado = ? "
      + "WHERE id_cuenta = ?"; 
    
    private static final String SQL_UPDATE_ESTADO =
            "UPDATE cuenta_bancaria SET estado = ? WHERE id_cuenta = ?";
    private static final String SQL_DELETE = "DELETE FROM cuenta_bancaria WHERE id_cuenta = ?";
    private static final String SQL_BUSCAR =
            "SELECT " + COLUMNAS + DESDE
          + "WHERE (c.numero_cuenta LIKE ? OR cl.codigo_cliente LIKE ? OR cl.nombre_completo LIKE ?) ";
    private static final String SQL_ORDEN = "ORDER BY c.id_cuenta";
    
    
private static final String SQL_SIGUIENTE_NUMERO =
        "SELECT COALESCE(MAX(CAST(numero_cuenta AS UNSIGNED)), 100000000000) + 1 "
      + "FROM cuenta_bancaria "
      + "WHERE numero_cuenta REGEXP '^[0-9]+$'";
    
    
    private static final String SQL_EXISTE_NUMERO =
            "SELECT COUNT(*) FROM cuenta_bancaria WHERE numero_cuenta = ? AND id_cuenta <> ?";
    private static final String SQL_CLIENTES_ACTIVOS =
            "SELECT id_cliente, codigo_cliente, nombre_completo FROM cliente "
          + "WHERE estado = 'Activo' ORDER BY nombre_completo";

    public int insertar(Cuenta c) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, c.getNumeroCuenta());
            ps.setInt(i++, c.getIdCliente());
            ps.setString(i++, c.getTipoCuenta().getEtiqueta());
            ps.setBigDecimal(i++, c.getSaldo());
            ps.setDate(i++, java.sql.Date.valueOf(
                    c.getFechaApertura() != null ? c.getFechaApertura() : LocalDate.now()));
            ps.setString(i, c.getEstado().getEtiqueta());
            int filas = ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) c.setIdCuenta(claves.getInt(1));
            }
            return filas;
        }
    }

    public List<Cuenta> buscar(String texto, EstadoCuenta estado) throws SQLException {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        String sql = SQL_BUSCAR + (estado != null ? "AND c.estado = ? " : "") + SQL_ORDEN;
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            if (estado != null) ps.setString(4, estado.getEtiqueta());
            try (ResultSet rs = ps.executeQuery()) {
                List<Cuenta> lista = new ArrayList<>();
                while (rs.next()) lista.add(mapear(rs));
                return lista;
            }
        }
    }

    public List<Cuenta> listar() throws SQLException {
        return buscar("", null);
    }

public String siguienteNumero() throws SQLException {
    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(SQL_SIGUIENTE_NUMERO);
         ResultSet rs = ps.executeQuery()) {
        long numero = rs.next() ? rs.getLong(1) : 100000000001L;
        return String.format("%012d", numero);
    }
}

    public boolean existeNumero(String numero, int idExcluir) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_EXISTE_NUMERO)) {
            ps.setString(1, numero);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<Cliente> listarClientesActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_CLIENTES_ACTIVOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getInt("id_cliente"));
                c.setCodigoCliente(rs.getString("codigo_cliente"));
                c.setNombreCompleto(rs.getString("nombre_completo"));
                lista.add(c);
            }
        }
        return lista;
    }

    public int actualizar(Cuenta c) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            int i = 1;
            ps.setInt(i++, c.getIdCliente());
            ps.setString(i++, c.getTipoCuenta().getEtiqueta());
            ps.setBigDecimal(i++, c.getSaldo()); 
            ps.setString(i++, c.getEstado().getEtiqueta());
            ps.setInt(i, c.getIdCuenta());
            return ps.executeUpdate();
        }
    }

    public int cambiarEstado(int idCuenta, EstadoCuenta nuevo) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, nuevo.getEtiqueta());
            ps.setInt(2, idCuenta);
            return ps.executeUpdate();
        }
    }

    public int eliminar(int idCuenta) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idCuenta);
            return ps.executeUpdate();
        }
    }

    private Cuenta mapear(ResultSet rs) throws SQLException {
        Cuenta c = new Cuenta();
        c.setIdCuenta(rs.getInt("id_cuenta"));
        c.setNumeroCuenta(rs.getString("numero_cuenta"));
        c.setIdCliente(rs.getInt("id_cliente"));
        c.setCodigoCliente(rs.getString("codigo_cliente"));
        c.setNombreCliente(rs.getString("nombre_completo"));
        c.setTipoCuenta(TipoCuenta.desdeEtiqueta(rs.getString("tipo_cuenta")));
        c.setSaldo(rs.getBigDecimal("saldo"));
        java.sql.Date f = rs.getDate("fecha_apertura");
        c.setFechaApertura(f == null ? null : f.toLocalDate());
        c.setEstado(EstadoCuenta.desdeEtiqueta(rs.getString("estado")));
        return c;
    }
}