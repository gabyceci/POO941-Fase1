package com.banco.servicio;

import com.banco.util.dao.MovimientoDAO;
import com.banco.excepcion.CuentaNoEncontradaException;
import com.banco.excepcion.EstadoCuentaInvalidoException;
import com.banco.excepcion.FondosInsuficientesException;
import com.banco.excepcion.MontoInvalidoException;
import com.banco.excepcion.OperacionBancariaException;
import com.banco.util.modelo.Cuenta;
import com.banco.util.modelo.EstadoCuenta;
import com.banco.util.modelo.Movimiento;
import com.banco.util.modelo.TipoMovimiento;
import com.banco.util.ConexionDB;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TransaccionService {
    private final MovimientoDAO movimientoDao = new MovimientoDAO();

    private static final String SQL_CUENTA_BLOQUEO =
            "SELECT id_cuenta, numero_cuenta, saldo, estado FROM cuenta_bancaria "
          + "WHERE numero_cuenta = ? FOR UPDATE";
    private static final String SQL_ACTUALIZAR_SALDO =
            "UPDATE cuenta_bancaria SET saldo = ? WHERE id_cuenta = ?";
    private static final String SQL_CONSULTA_VISUAL =
            "SELECT c.id_cuenta, c.numero_cuenta, c.tipo_cuenta, c.saldo, c.estado, "
          + "cl.codigo_cliente, cl.nombre_completo "
          + "FROM cuenta_bancaria c INNER JOIN cliente cl ON cl.id_cliente = c.id_cliente "
          + "WHERE c.numero_cuenta = ?";

    /** DEPOSITO la cuenta existe y esta activa y su monto > 0 → saldo aumenta. */
    public Movimiento depositar(String numeroCuenta, BigDecimal monto, String descripcion)
            throws OperacionBancariaException, SQLException {
        validarMonto(monto);
        return ejecutar(numeroCuenta, monto, descripcion, TipoMovimiento.DEPOSITO, true);
    }

    /** RETIRO: cuenta existe + activa + monto > 0 + saldo suficiente → saldo disminuye. */
    public Movimiento retirar(String numeroCuenta, BigDecimal monto, String descripcion)
            throws OperacionBancariaException, SQLException {
        validarMonto(monto);
        return ejecutar(numeroCuenta, monto, descripcion, TipoMovimiento.RETIRO, false);
    }

    /** Historial de movimientos de una cuenta (consulta de movimientos). */
    public List<Movimiento> historial(int idCuenta) throws SQLException {
        return movimientoDao.listarPorCuenta(idCuenta);
    }

    /** Consulta sin bloqueo para pintar titular/saldo/estado en la pantalla. */
    public Cuenta consultarPorNumero(String numeroCuenta) throws SQLException {
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(SQL_CONSULTA_VISUAL)) {
            ps.setString(1, numeroCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Cuenta c = new Cuenta();
                c.setIdCuenta(rs.getInt("id_cuenta"));
                c.setNumeroCuenta(rs.getString("numero_cuenta"));
                c.setTipoCuenta(com.banco.util.modelo.TipoCuenta.desdeEtiqueta(rs.getString("tipo_cuenta")));
                c.setSaldo(rs.getBigDecimal("saldo"));
                c.setEstado(EstadoCuenta.desdeEtiqueta(rs.getString("estado")));
                c.setCodigoCliente(rs.getString("codigo_cliente"));
                c.setNombreCliente(rs.getString("nombre_completo"));
                return c;
            }
        }
    }

    // Núcleo transaccional:

    private Movimiento ejecutar(String numeroCuenta, BigDecimal monto, String descripcion,
                                TipoMovimiento tipo, boolean esDeposito)
            throws OperacionBancariaException, SQLException {

        Connection con = ConexionDB.getConexion();
        try {
            con.setAutoCommit(false);                                  

            Cuenta cuenta = buscarConBloqueo(con, numeroCuenta);       
            if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {         
                throw new EstadoCuentaInvalidoException(numeroCuenta, cuenta.getEstado());
            }

            BigDecimal nuevoSaldo;
            if (esDeposito) {
                nuevoSaldo = cuenta.getSaldo().add(monto);            
            } else {
                if (cuenta.getSaldo().compareTo(monto) < 0) {         
                    throw new FondosInsuficientesException(cuenta.getSaldo(), monto);
                }
                nuevoSaldo = cuenta.getSaldo().subtract(monto);      
            }

            actualizarSaldo(con, cuenta.getIdCuenta(), nuevoSaldo);    
            Movimiento mov = new Movimiento(
                    cuenta.getIdCuenta(), tipo, monto, descripcion, nuevoSaldo);
            movimientoDao.insertar(con, mov);                       

            con.commit();                                             
            return mov;
        } catch (SQLException | OperacionBancariaException ex) {
            try { con.rollback(); } catch (SQLException r) { /* se notifica el original */ }
            throw ex;                                                
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { /* ignorar */ }
            ConexionDB.cerrar(con);
        }
    }

    private Cuenta buscarConBloqueo(Connection con, String numeroCuenta)
            throws SQLException, CuentaNoEncontradaException {
        try (PreparedStatement ps = con.prepareStatement(SQL_CUENTA_BLOQUEO)) {
            ps.setString(1, numeroCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new CuentaNoEncontradaException(numeroCuenta);
                }
                Cuenta c = new Cuenta();
                c.setIdCuenta(rs.getInt("id_cuenta"));
                c.setNumeroCuenta(rs.getString("numero_cuenta"));
                c.setSaldo(rs.getBigDecimal("saldo"));
                c.setEstado(EstadoCuenta.desdeEtiqueta(rs.getString("estado")));
                return c;
            }
        }
    }

    private void actualizarSaldo(Connection con, int idCuenta, BigDecimal nuevoSaldo)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR_SALDO)) {
            ps.setBigDecimal(1, nuevoSaldo);
            ps.setInt(2, idCuenta);
            ps.executeUpdate();
        }
    }

    private void validarMonto(BigDecimal monto) throws MontoInvalidoException {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException(monto);
        }
    }
}