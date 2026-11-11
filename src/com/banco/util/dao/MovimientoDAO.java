package com.banco.util.dao;

import com.banco.util.modelo.Movimiento;
import com.banco.util.modelo.TipoMovimiento;
import com.banco.util.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {
    private static final String SQL_INSERT =
            "INSERT INTO movimiento (id_cuenta, tipo_movimiento, monto, fecha_hora, descripcion, saldo_resultante) "
          + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_POR_CUENTA =
            "SELECT m.id_movimiento, m.id_cuenta, c.numero_cuenta, m.tipo_movimiento, m.monto, "
          + "m.fecha_hora, m.descripcion, m.saldo_resultante "
          + "FROM movimiento m INNER JOIN cuenta_bancaria c ON c.id_cuenta = m.id_cuenta "
          + "WHERE m.id_cuenta = ? ORDER BY m.id_movimiento DESC";

    /** INSERT dentro de una transacción ya abierta. */
    public int insertar(Connection con, Movimiento m) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, m.getIdCuenta());
            ps.setString(i++, m.getTipoMovimiento().getEtiqueta());
            ps.setBigDecimal(i++, m.getMonto());
            ps.setTimestamp(i++, Timestamp.valueOf(m.getFechaHora()));
            ps.setString(i++, m.getDescripcion());
            ps.setBigDecimal(i, m.getSaldoResultante());
            int filas = ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) m.setIdMovimiento(claves.getInt(1));
            }
            return filas;
        }
    }

    /** Historial de una cuenta */
    public List<Movimiento> listarPorCuenta(int idCuenta) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_POR_CUENTA)) {
            ps.setInt(1, idCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                List<Movimiento> lista = new ArrayList<>();
                while (rs.next()) lista.add(mapear(rs));
                return lista;
            }
        }
    }

    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.setIdMovimiento(rs.getInt("id_movimiento"));
        m.setIdCuenta(rs.getInt("id_cuenta"));
        m.setNumeroCuenta(rs.getString("numero_cuenta"));
        m.setTipoMovimiento(TipoMovimiento.desdeEtiqueta(rs.getString("tipo_movimiento")));
        m.setMonto(rs.getBigDecimal("monto"));
        Timestamp ts = rs.getTimestamp("fecha_hora");
        m.setFechaHora(ts == null ? null : ts.toLocalDateTime());
        m.setDescripcion(rs.getString("descripcion"));
        m.setSaldoResultante(rs.getBigDecimal("saldo_resultante"));
        return m;
    }
}