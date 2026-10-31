
package com.banco.util.vista;
import com.banco.util.dao.CuentaDAO;  
import com.banco.modelo.Cliente;
import com.banco.util.modelo.Cuenta;
import com.banco.util.modelo.EstadoCuenta;
import com.banco.util.modelo.TipoCuenta;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CuentasForm extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CuentasForm.class.getName());

    private final CuentaDAO dao = new CuentaDAO();
    private static final String TODOS = "Todos";
    private static final String[] COLUMNAS =
            {"Número", "Cliente", "Tipo", "Saldo", "Apertura", "Estado"};
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");


    private List<Cuenta> cuentasMostradas = new ArrayList<>();
    private List<Cliente> clientesActivos = new ArrayList<>();
    private Cuenta cuentaSeleccionada;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public CuentasForm() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/com/banco/img/logo.jfif")).getImage());
        configurarFormulario();
        recargarClientes();
        recargarTabla();
        limpiarFormulario();
    }

        
    private void configurarFormulario() {
        setTitle("Banco Académico UDB - Gestión de Cuentas Bancarias");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        txtNumero.setEditable(false);
        txtSaldo.setEditable(true);
        txtFecha.setEditable(false);
        cmbTipo.setModel(new DefaultComboBoxModel<>(new String[]{"Ahorro", "Corriente"}));
        cmbEstado.setModel(new DefaultComboBoxModel<>(new String[]{"Activa", "Inactiva", "Bloqueada"}));
        cmbFiltroEstado.setModel(new DefaultComboBoxModel<>(
                new String[]{TODOS, "Activa", "Inactiva", "Bloqueada"}));
        tblCuentas.setModel(modeloTabla);
        tblCuentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCuentas.setRowHeight(24);
        tblCuentas.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {90, 260, 80, 100, 90, 80};
        for (int i = 0; i < anchos.length; i++) {
            tblCuentas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tblCuentas.getColumnModel().getColumn(3).setCellRenderer(derecha);
        tblCuentas.getColumnModel().getColumn(5).setCellRenderer(new RenderizadorEstadoCuenta());

        btnGuardar.addActionListener(e -> guardarNueva());
        btnActualizar.addActionListener(e -> actualizarCuenta());
        btnCambiarEstado.addActionListener(e -> cambiarEstado());
        btnEliminar.addActionListener(e -> eliminarCuenta());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnBuscar.addActionListener(e -> recargarTabla());
        txtBuscar.addActionListener(e -> recargarTabla());
        btnMostrarTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cmbFiltroEstado.setSelectedIndex(0);
            recargarTabla();
        });
        tblCuentas.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int fila = tblCuentas.getSelectedRow();
            if (fila >= 0) {
                int indice = tblCuentas.convertRowIndexToModel(fila);
                cargarEnFormulario(cuentasMostradas.get(indice));
            }
        });
    }

    /** CREATE */
    private void guardarNueva() {
           if (!saldoConFormatoValido()) {   
        return;
    }
        Cuenta c = leerFormulario();
        if (!datosValidos(c)) {
            return;
        }
        try {
            if (dao.existeNumero(c.getNumeroCuenta(), 0)) {
                avisar("Ya existe una cuenta con el número " + c.getNumeroCuenta() + ".");
                return;
            }
            dao.insertar(c);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cuenta " + c.getNumeroCuenta() + " creada correctamente.");
        } catch (SQLException ex) {
            mostrarErrorBD("crear la cuenta", ex);
        }
    }

    /** UPDATE */
    private void actualizarCuenta() {
        if (cuentaSeleccionada == null) {
            avisar("Seleccione una cuenta de la tabla para actualizarla.");
            return;
        }
            if (!saldoConFormatoValido()) {    
        return;
    }
        
        Cuenta c = leerFormulario();
        c.setIdCuenta(cuentaSeleccionada.getIdCuenta());
        c.setNumeroCuenta(cuentaSeleccionada.getNumeroCuenta());
        c.setFechaApertura(cuentaSeleccionada.getFechaApertura());
        if (!datosValidos(c)) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Guardar los cambios de la cuenta " + c.getNumeroCuenta() + "?",
                "Confirmar actualización", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.actualizar(c);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cuenta " + c.getNumeroCuenta() + " actualizada correctamente.");
        } catch (SQLException ex) {
            mostrarErrorBD("actualizar la cuenta", ex);
        }
    }

    private void cambiarEstado() {
        if (cuentaSeleccionada == null) {
            avisar("Seleccione una cuenta de la tabla para cambiar su estado.");
            return;
        }
        EstadoCuenta nuevo = EstadoCuenta.desdeEtiqueta((String) cmbEstado.getSelectedItem());
        if (nuevo == cuentaSeleccionada.getEstado()) {
            avisar("La cuenta ya se encuentra en estado " + nuevo + ".\n"
                 + "Elija otro estado en la lista \"Estado\" y vuelva a intentarlo.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Cambiar el estado de la cuenta " + cuentaSeleccionada.getNumeroCuenta()
              + "\nde " + cuentaSeleccionada.getEstado() + " a " + nuevo + "?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String numero = cuentaSeleccionada.getNumeroCuenta();
            dao.cambiarEstado(cuentaSeleccionada.getIdCuenta(), nuevo);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("La cuenta " + numero + " ahora está " + nuevo + ".");
        } catch (SQLException ex) {
            mostrarErrorBD("cambiar el estado de la cuenta", ex);
        }
    }

    /** DELETE */
    private void eliminarCuenta() {
        if (cuentaSeleccionada == null) {
            avisar("Seleccione una cuenta de la tabla para eliminarla.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar definitivamente la cuenta " + cuentaSeleccionada.getNumeroCuenta()
              + "?\nEsta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.eliminar(cuentaSeleccionada.getIdCuenta());
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cuenta eliminada correctamente.");
        } catch (SQLException ex) {
            mostrarErrorBD("eliminar la cuenta", ex);
        }
    }

    private Cuenta leerFormulario() {
        Cuenta c = new Cuenta();
        c.setNumeroCuenta(txtNumero.getText());
        int indice = cmbClientes.getSelectedIndex();
        if (indice >= 0 && indice < clientesActivos.size()) {
            Cliente cli = clientesActivos.get(indice);
            c.setIdCliente(cli.getIdCliente());
            c.setCodigoCliente(cli.getCodigoCliente());
            c.setNombreCliente(cli.getNombreCompleto());
        }
        c.setTipoCuenta(TipoCuenta.desdeEtiqueta((String) cmbTipo.getSelectedItem()));
        c.setEstado(EstadoCuenta.desdeEtiqueta((String) cmbEstado.getSelectedItem()));
        c.setSaldo(parsearSaldoDelCampo());
        return c;
    }

    private boolean datosValidos(Cuenta c) {
        List<String> errores = c.validar();
        if (errores.isEmpty()) {
            return true;
        }
        StringBuilder sb = new StringBuilder("Corrija lo siguiente:\n");
        for (String error : errores) {
            sb.append("  • ").append(error).append('\n');
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Datos no válidos",
                JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private void cargarEnFormulario(Cuenta c) {
        cuentaSeleccionada = c;
        txtNumero.setText(c.getNumeroCuenta());
        seleccionarCliente(c);
        cmbTipo.setSelectedItem(c.getTipoCuenta().getEtiqueta());
        cmbEstado.setSelectedItem(c.getEstado().getEtiqueta());
        txtSaldo.setText(formatoMoneda(c.getSaldo()));
        txtFecha.setText(c.getFechaApertura() == null ? ""
                : c.getFechaApertura().format(FORMATO_FECHA));
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnCambiarEstado.setEnabled(true);
        btnEliminar.setEnabled(true);
        mostrarMensaje("Editando cuenta " + c.getNumeroCuenta() + ".");
    }


    private void seleccionarCliente(Cuenta c) {
        for (int i = 0; i < clientesActivos.size(); i++) {
            if (clientesActivos.get(i).getIdCliente() == c.getIdCliente()) {
                cmbClientes.setSelectedIndex(i);
                return;
            }
        }
        Cliente temporal = new Cliente();
        temporal.setIdCliente(c.getIdCliente());
        temporal.setCodigoCliente(c.getCodigoCliente());
        temporal.setNombreCompleto(c.getNombreCliente());
        clientesActivos = new ArrayList<>(clientesActivos);
        clientesActivos.add(temporal);
        refrescarComboClientes();
        cmbClientes.setSelectedIndex(clientesActivos.size() - 1);
    }

    private void refrescarComboClientes() {
        String[] opciones = new String[clientesActivos.size()];
        for (int i = 0; i < clientesActivos.size(); i++) {
            opciones[i] = clientesActivos.get(i).toString(); 
        }
        cmbClientes.setModel(new DefaultComboBoxModel<>(opciones));
    }

    private void limpiarFormulario() {
        cuentaSeleccionada = null;
        tblCuentas.clearSelection();
        recargarClientes();
        if (cmbClientes.getItemCount() > 0) {
            cmbClientes.setSelectedIndex(0);
        }
        cmbTipo.setSelectedIndex(0);
        cmbEstado.setSelectedItem("Activa");
        txtSaldo.setText("0.00");
        txtFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        try {
            txtNumero.setText(dao.siguienteNumero());
        } catch (SQLException ex) {
            txtNumero.setText("");
            mostrarMensaje("Sin conexión con la base de datos.");
        }
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnCambiarEstado.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    private void recargarClientes() {
        try {
            clientesActivos = dao.listarClientesActivos();
        } catch (SQLException ex) {
            clientesActivos = new ArrayList<>();
            mostrarErrorBD("consultar los clientes activos", ex);
        }
        refrescarComboClientes();
    }

    private void recargarTabla() {
        String filtro = (String) cmbFiltroEstado.getSelectedItem();
        EstadoCuenta estado = TODOS.equals(filtro) ? null : EstadoCuenta.desdeEtiqueta(filtro);
        try {
            cuentasMostradas = dao.buscar(txtBuscar.getText(), estado);
        } catch (SQLException ex) {
            cuentasMostradas = new ArrayList<>();
            mostrarErrorBD("consultar las cuentas", ex);
        }
        tblCuentas.setRowSorter(null);
        modeloTabla.setRowCount(0);
        for (Cuenta c : cuentasMostradas) {
            modeloTabla.addRow(new Object[]{
                c.getNumeroCuenta(),
                c.getCodigoCliente() + " - " + c.getNombreCliente(),
                c.getTipoCuenta().getEtiqueta(),
                formatoMoneda(c.getSaldo()),
                c.getFechaApertura() == null ? "" : c.getFechaApertura().format(FORMATO_FECHA),
                c.getEstado()
            });
        }
        tblCuentas.setRowSorter(new TableRowSorter<>(modeloTabla));
        mostrarMensaje(cuentasMostradas.size() + " cuenta(s) en la lista.");
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
    }

    private BigDecimal parsearSaldoDelCampo() {
    String texto = txtSaldo.getText() == null ? "" : txtSaldo.getText().trim().replace(',', '.');
    if (texto.isEmpty()) {
        return BigDecimal.ZERO;
    }
    try {
        return new BigDecimal(texto);
    } catch (NumberFormatException ex) {
        return null; 
    }
}

private boolean saldoConFormatoValido() {
    String texto = txtSaldo.getText() == null ? "" : txtSaldo.getText().trim().replace(',', '.');
    if (texto.isEmpty()) {
        return true; // vacío se interpreta como 0.00
    }
    try {
        new BigDecimal(texto);
        return true;
    } catch (NumberFormatException ex) {
        avisar("El saldo \"" + txtSaldo.getText().trim() + "\" no es un número válido.\n"
             + "Use formato decimal con punto, por ejemplo: 1500.50");
        txtSaldo.requestFocusInWindow();
        txtSaldo.selectAll();
        return false;
    }
}

private static String formatoMoneda(BigDecimal saldo) {
    return saldo == null ? "0.00" : saldo.setScale(2, RoundingMode.HALF_UP).toPlainString();
}
    
    
    private void avisar(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarErrorBD(String accion, SQLException ex) {
        String detalle;
        if (ex.getErrorCode() == 1062) {
            detalle = "Ya existe una cuenta con ese número. Pulse \"Limpiar\" e intente de nuevo.";
        } else {
            detalle = "Verifique que MySQL esté encendido y que los datos de ConexionDB sean correctos.\n"
                    + "Detalle: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, "No se pudo " + accion + ".\n" + detalle,
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    private static class RenderizadorEstadoCuenta extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionada, boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(
                    tabla, valor, seleccionada, foco, fila, columna);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (!seleccionada) {
                if (valor == EstadoCuenta.ACTIVA) {
                    c.setForeground(new Color(27, 127, 59));
                } else if (valor == EstadoCuenta.BLOQUEADA) {
                    c.setForeground(new Color(179, 38, 30));
                } else {
                    c.setForeground(new Color(110, 110, 110));
                }
            }
            return c;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        borde_titled = new javax.swing.JLabel();
        lblNumeroCuenta = new javax.swing.JLabel();
        txtNumero = new javax.swing.JTextField();
        lblTipoCuenta = new javax.swing.JLabel();
        lblSaldo = new javax.swing.JLabel();
        txtSaldo = new javax.swing.JTextField();
        cmbTipo = new javax.swing.JComboBox<>();
        lblCliente = new javax.swing.JLabel();
        lblEstado = new javax.swing.JLabel();
        lblFechaApertura = new javax.swing.JLabel();
        cmbClientes = new javax.swing.JComboBox<>();
        cmbEstado = new javax.swing.JComboBox<>();
        txtFecha = new javax.swing.JTextField();
        btnGuardar = new javax.swing.JButton();
        btnActualizar = new javax.swing.JButton();
        btnCambiarEstado = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        lblBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        cmbFiltroEstado = new javax.swing.JComboBox<>();
        btnMostrarTodos = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblCuentas = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        lblMensaje = new javax.swing.JLabel();
        btnIrAClientes = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        borde_titled.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        borde_titled.setText("Datos de la cuenta");

        lblNumeroCuenta.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblNumeroCuenta.setText("Numero de Cuenta");

        lblTipoCuenta.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblTipoCuenta.setText("Tipo de Cuenta:");

        lblSaldo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblSaldo.setText("Saldo");

        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblCliente.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblCliente.setText("Cliente:");

        lblEstado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblEstado.setText("Estado:");

        lblFechaApertura.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblFechaApertura.setText("Fecha apertura: ");

        cmbClientes.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnGuardar.setText("Guardar");

        btnActualizar.setText("Actualizar");

        btnCambiarEstado.setText("Cambiar Estado");

        btnEliminar.setText("Eliminar");

        btnLimpiar.setText("Limpiar");

        lblBuscar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblBuscar.setText("Buscar Numero de cuenta");

        cmbFiltroEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnMostrarTodos.setText("Mostrar Todos");

        btnBuscar.setText("Buscar");

        tblCuentas.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblCuentas);

        lblMensaje.setText("jLabel2");

        btnIrAClientes.setText("Ir a Clientes");
        btnIrAClientes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIrAClientesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblSaldo)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblNumeroCuenta)
                                    .addComponent(lblTipoCuenta))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(txtSaldo, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cmbTipo, javax.swing.GroupLayout.Alignment.LEADING, 0, 123, Short.MAX_VALUE)
                                    .addComponent(txtNumero)))
                            .addComponent(borde_titled, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(86, 86, 86)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFechaApertura, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 478, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblCliente)
                                    .addComponent(lblEstado))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(61, 61, 61)
                                        .addComponent(cmbClientes, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(67, 67, 67)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(txtFecha)
                                            .addComponent(cmbEstado, 0, 187, Short.MAX_VALUE))))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(190, 190, 190)
                        .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnMostrarTodos)
                        .addGap(127, 127, 127))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(168, 168, 168)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnCambiarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 1050, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblBuscar)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnIrAClientes, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(borde_titled, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblCliente)
                            .addComponent(cmbClientes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblEstado)
                            .addComponent(cmbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(16, 16, 16)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblFechaApertura)
                            .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblNumeroCuenta)
                            .addComponent(txtNumero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTipoCuenta)
                            .addComponent(cmbTipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtSaldo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSaldo))))
                .addGap(51, 51, 51)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCambiarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnIrAClientes, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBuscar)
                    .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMostrarTodos, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(369, 369, 369)
                        .addComponent(jLabel1)
                        .addContainerGap(86, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnIrAClientesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIrAClientesActionPerformed
    this.dispose();                       
    ClientesForm destino = new ClientesForm();
    destino.setVisible(true);  
    }//GEN-LAST:event_btnIrAClientesActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new CuentasForm().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel borde_titled;
    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCambiarEstado;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIrAClientes;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMostrarTodos;
    private javax.swing.JComboBox<String> cmbClientes;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbFiltroEstado;
    private javax.swing.JComboBox<String> cmbTipo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblCliente;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblFechaApertura;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblNumeroCuenta;
    private javax.swing.JLabel lblSaldo;
    private javax.swing.JLabel lblTipoCuenta;
    private javax.swing.JTable tblCuentas;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtFecha;
    private javax.swing.JTextField txtNumero;
    private javax.swing.JTextField txtSaldo;
    // End of variables declaration//GEN-END:variables
}
