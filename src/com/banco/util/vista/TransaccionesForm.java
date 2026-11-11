
package com.banco.util.vista;

import com.banco.excepcion.OperacionBancariaException;
import com.banco.util.modelo.Cuenta;
import com.banco.util.modelo.Movimiento;
import com.banco.util.modelo.TipoMovimiento;
import com.banco.servicio.TransaccionService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author MacBookPro M2
 */

public class TransaccionesForm extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TransaccionesForm.class.getName());

    /**
     * Creates new form TransaccionesForm
     */
    public TransaccionesForm() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/com/banco/img/logo.jfif")).getImage());
        configurarFormulario();
    }

    
        // Inicia fase de la logica: */
    private final TransaccionService servicio = new TransaccionService();
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLUMNAS =
            {"Fecha", "Tipo", "Monto", "Saldo resultante", "Descripción"};

    /** Cuenta cargada actualmente; null si aún no se ha buscado ninguna. */
    private Cuenta cuentaActual;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;   // el historial es solo consulta
        }
    };

    private void configurarFormulario() {
        setTitle("Banco Académico UDB - Depósitos y Retiros");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        cmbOperacion.setModel(new DefaultComboBoxModel<>(new String[]{"Depósito", "Retiro"}));
        tblMovimientos.setModel(modeloTabla);
        tblMovimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMovimientos.setRowHeight(24);
        tblMovimientos.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {120, 100, 110, 120, 300};
        for (int i = 0; i < anchos.length; i++) {
            tblMovimientos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tblMovimientos.getColumnModel().getColumn(2).setCellRenderer(new RenderizadorMonto());

        btnBuscar.addActionListener(e -> buscarCuenta());
        btnEjecutar.addActionListener(e -> ejecutarOperacion());

    }

    /** Busca la cuenta por número y pinta titular, estado, saldo e historial. */
    private void buscarCuenta() {
        String numero = txtNumeroCuenta.getText().trim();
        if (numero.isEmpty()) {
            avisar("Escriba el número de cuenta y pulse \"Buscar\".");
            return;
        }
        try {
            cuentaActual = servicio.consultarPorNumero(numero);
            if (cuentaActual == null) {
                lblTitular.setText("—");
                lblEstadoCuenta.setText("—");
                lblSaldoActual.setText("—");
                modeloTabla.setRowCount(0);
                avisar("No existe ninguna cuenta con el número " + numero + ".");
                return;
            }
            lblTitular.setText(cuentaActual.getCodigoCliente() + " - " + cuentaActual.getNombreCliente());
            lblEstadoCuenta.setText(cuentaActual.getEstado().getEtiqueta());
            lblSaldoActual.setText(cuentaActual.getSaldo().toPlainString());
            recargarHistorial();
            mostrarMensaje("Cuenta " + numero + " cargada. Lista para operar.");
        } catch (SQLException ex) {
            mostrarErrorBD("consultar la cuenta", ex);
        }
    }

    /** Ejecuta depósito o retiro según la opcion seleccionada. */
    private void ejecutarOperacion() {
        if (cuentaActual == null) {
            avisar("Primero busque y seleccione una cuenta.");
            return;
        }
        BigDecimal monto;
        try {
            monto = new BigDecimal(txtMonto.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            avisar("El monto debe ser un número decimal, por ejemplo: 250.00");
            return;
        }
        String descripcion = txtDescripcion.getText().trim();
        boolean esDeposito = cmbOperacion.getSelectedIndex() == 0;
        try {
            Movimiento mov = esDeposito
                    ? servicio.depositar(cuentaActual.getNumeroCuenta(), monto, descripcion)
                    : servicio.retirar(cuentaActual.getNumeroCuenta(), monto, descripcion);
            mostrarMensaje((esDeposito ? "Depósito" : "Retiro") + " registrado. Nuevo saldo: "
                    + mov.getSaldoResultante().toPlainString());
            txtMonto.setText("");
            txtDescripcion.setText("");
            buscarCuenta();   // refresca saldo, estado e historial
        } catch (OperacionBancariaException ex) {   // excepciones
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Operación rechazada", JOptionPane.WARNING_MESSAGE);
            mostrarMensaje("Operación rechazada: " + ex.getMessage());
        } catch (SQLException ex) {
            mostrarErrorBD("registrar la operación", ex);
        }
    }

    /** Se presenta el historial en la tabla desde el mas actual al mas antiguo. */
    private void recargarHistorial() {
        modeloTabla.setRowCount(0);
        if (cuentaActual == null) {
            return;
        }
        try {
            List<Movimiento> movimientos = servicio.historial(cuentaActual.getIdCuenta());
            for (Movimiento m : movimientos) {
                String signo = m.getTipoMovimiento() == TipoMovimiento.DEPOSITO ? "+"
                        : m.getTipoMovimiento() == TipoMovimiento.RETIRO ? "-" : "";
                modeloTabla.addRow(new Object[]{
                    m.getFechaHora() == null ? "" : m.getFechaHora().format(FORMATO_FECHA_HORA),
                    m.getTipoMovimiento(),
                    signo + m.getMonto().toPlainString(),
                    m.getSaldoResultante().toPlainString(),
                    m.getDescripcion()
                });
            }
            mostrarMensaje(movimientos.size() + " movimiento(s) en el historial.");
        } catch (SQLException ex) {
            mostrarErrorBD("consultar el historial", ex);
        }
    }

    private void limpiarFormulario() {
        cuentaActual = null;
        txtNumeroCuenta.setText("");
        txtMonto.setText("");
        txtDescripcion.setText("");
        cmbOperacion.setSelectedIndex(0);
        lblTitular.setText("—");
        lblEstadoCuenta.setText("—");
        lblSaldoActual.setText("—");
        modeloTabla.setRowCount(0);
        mostrarMensaje(" ");
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
    }

    private void avisar(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarErrorBD(String accion, SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "No se pudo " + accion + ".\nVerifique que MySQL esté encendido y que ConexionDB sea correcto.\n"
              + "Detalle: " + ex.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    /** Colores que puse: Verde = (depósito), rojo =  (retiro). */
    private static class RenderizadorMonto extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean sel, boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, sel, foco, fila, columna);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(getFont().deriveFont(Font.BOLD));
            if (!sel) {
                Object tipo = tabla.getValueAt(fila, 1);
                if (tipo == TipoMovimiento.DEPOSITO) {
                    setForeground(new Color(27, 127, 59));
                } else if (tipo == TipoMovimiento.RETIRO) {
                    setForeground(new Color(179, 38, 30));
                } else {
                    setForeground(new Color(0, 102, 204));
                }
            }
            return c;
        }
    }

    // Se han finalizado la logica de transacciones
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        btnIrACuentas = new javax.swing.JButton();
        lblnumerocuenta = new javax.swing.JLabel();
        cmbOperacion = new javax.swing.JComboBox<>();
        lblOperacion = new javax.swing.JLabel();
        txtNumeroCuenta = new javax.swing.JTextField();
        btnBuscar = new javax.swing.JButton();
        lbtitulartext = new javax.swing.JLabel();
        lblSaldotext = new javax.swing.JLabel();
        lbEstadotext = new javax.swing.JLabel();
        lblTitular = new javax.swing.JLabel();
        lblEstadoCuenta = new javax.swing.JLabel();
        lblSaldoActual = new javax.swing.JLabel();
        btnEjecutar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        texthistorialmovi = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMovimientos = new javax.swing.JTable();
        lblmontotext = new javax.swing.JLabel();
        lblmontotext1 = new javax.swing.JLabel();
        txtMonto = new javax.swing.JTextField();
        txtDescripcion = new javax.swing.JTextField();
        lblMensaje = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Arial Black", 1, 21)); // NOI18N
        jLabel1.setText("Banco Académico UDB - Depósitos y Retiros");

        btnIrACuentas.setText("Ir a Cuentas");
        btnIrACuentas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIrACuentasActionPerformed(evt);
            }
        });

        lblnumerocuenta.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblnumerocuenta.setText("Numero de Cuenta:");

        cmbOperacion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblOperacion.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblOperacion.setText("Operacion: ");

        txtNumeroCuenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNumeroCuentaActionPerformed(evt);
            }
        });

        btnBuscar.setText("Buscar");
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarActionPerformed(evt);
            }
        });

        lbtitulartext.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lbtitulartext.setText("Titular: ");

        lblSaldotext.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblSaldotext.setText("Saldo: ");

        lbEstadotext.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lbEstadotext.setText("Estado:");

        lblTitular.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N

        lblEstadoCuenta.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        lblSaldoActual.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        btnEjecutar.setText("Ejecutar");

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        texthistorialmovi.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        texthistorialmovi.setText("Historial de movimientos");

        tblMovimientos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblMovimientos);

        lblmontotext.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblmontotext.setText("Monto: ");

        lblmontotext1.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblmontotext1.setText("Descripcion: ");

        txtMonto.setMinimumSize(new java.awt.Dimension(64, 30));

        txtDescripcion.setMinimumSize(new java.awt.Dimension(64, 30));

        lblMensaje.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel2.setText("Ingrese informacion solicitada.");

        jLabel3.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel3.setText("Detalle de la cuenta:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(texthistorialmovi, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 913, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 475, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblnumerocuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtNumeroCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbtitulartext, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblTitular, javax.swing.GroupLayout.PREFERRED_SIZE, 306, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(lbEstadotext, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblEstadoCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSaldotext, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblSaldoActual, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(367, 367, 367)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblmontotext1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnEjecutar, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(51, 51, 51)
                                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblOperacion)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbOperacion, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(lblmontotext, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(129, 129, 129)
                        .addComponent(txtDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, 354, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(599, 599, 599))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 616, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(116, 116, 116)
                        .addComponent(btnIrACuentas)
                        .addGap(26, 26, 26))
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnIrACuentas, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblnumerocuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNumeroCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(2, 2, 2)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lbtitulartext, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lbEstadotext, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblTitular, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblEstadoCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblSaldoActual, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblOperacion, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbOperacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblmontotext, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtMonto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblmontotext1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnEjecutar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(texthistorialmovi, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblSaldotext, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNumeroCuentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNumeroCuentaActionPerformed
       buscarCuenta();
    }//GEN-LAST:event_txtNumeroCuentaActionPerformed

    private void btnIrACuentasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIrACuentasActionPerformed
        this.dispose();                     
        new CuentasForm().setVisible(true);
    }//GEN-LAST:event_btnIrACuentasActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
       limpiarFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnBuscarActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new TransaccionesForm().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnEjecutar;
    private javax.swing.JButton btnIrACuentas;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JComboBox<String> cmbOperacion;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbEstadotext;
    private javax.swing.JLabel lblEstadoCuenta;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblOperacion;
    private javax.swing.JLabel lblSaldoActual;
    private javax.swing.JLabel lblSaldotext;
    private javax.swing.JLabel lblTitular;
    private javax.swing.JLabel lblmontotext;
    private javax.swing.JLabel lblmontotext1;
    private javax.swing.JLabel lblnumerocuenta;
    private javax.swing.JLabel lbtitulartext;
    private javax.swing.JTable tblMovimientos;
    private javax.swing.JLabel texthistorialmovi;
    private javax.swing.JTextField txtDescripcion;
    private javax.swing.JTextField txtMonto;
    private javax.swing.JTextField txtNumeroCuenta;
    // End of variables declaration//GEN-END:variables
}
