/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.banco.util.vista;

import com.banco.dao.ClienteDAO;
import com.banco.modelo.Cliente;
import com.banco.modelo.EstadoCliente;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
 
/**
 *
 * @author Latitude E5470
 */
public class ClientesForm extends javax.swing.JFrame {

  
    private final ClienteDAO dao = new ClienteDAO();

    private static final String TODOS = "Todos";
    private static final String[] COLUMNAS =
            {"Código", "DUI", "Nombre completo", "Teléfono", "Correo", "Dirección", "Estado"};

    /** Clientes que muestra la tabla ahora mismo (misma posición que las filas del modelo). */
    private List<Cliente> clientesMostrados = new ArrayList<>();
    /** Cliente cargado en el formulario; null cuando se está en modo "nuevo". */
    private Cliente clienteSeleccionado;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; // la tabla es solo de consulta
        }
    };

    public ClientesForm() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/com/banco/img/logo.jfif")).getImage());
        configurarFormulario();
        recargarTabla();
        limpiarFormulario();
    }

    private void configurarFormulario() {
        setTitle("Banco Académico UDB - Gestión de Clientes");
        setLocationRelativeTo(null);

        txtCodigo.setEditable(false);
        cmbEstado.setModel(new DefaultComboBoxModel<>(new String[]{"Activo", "Inactivo", "Bloqueado"}));
        cmbFiltroEstado.setModel(new DefaultComboBoxModel<>(new String[]{TODOS, "Activo", "Inactivo", "Bloqueado"}));

        tblClientes.setModel(modeloTabla);
        tblClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblClientes.setRowHeight(24);
        tblClientes.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {80, 100, 200, 90, 190, 220, 80};
        for (int i = 0; i < anchos.length; i++) {
            tblClientes.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tblClientes.getColumnModel().getColumn(6).setCellRenderer(new RenderizadorEstado());

        // Eventos de los botones
        btnGuardar.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizarCliente());
        btnCambiarEstado.addActionListener(e -> cambiarEstado());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnBuscar.addActionListener(e -> recargarTabla());
        btnEliminar.addActionListener(e -> eliminarCliente());
        txtBuscar.addActionListener(e -> recargarTabla());
        btnMostrarTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cmbFiltroEstado.setSelectedIndex(0);
            recargarTabla();
        });

        // Al elegir una fila, sus datos pasan al formulario (modo edición)
        tblClientes.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int fila = tblClientes.getSelectedRow();
            if (fila >= 0) {
                int indice = tblClientes.convertRowIndexToModel(fila);
                cargarEnFormulario(clientesMostrados.get(indice));
            }
        });
    }


    /** Registra un cliente nuevo (INSERT). */
    private void guardarNuevo() {
        Cliente c = leerFormulario();
        if (!datosValidos(c)) {
            return;
        }
        try {
            if (dao.existeDui(c.getDui(), 0)) {
                avisar("Ya existe un cliente registrado con el DUI " + c.getDui() + ".");
                return;
            }
            dao.insertar(c);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cliente " + c.getCodigoCliente() + " registrado correctamente.");
        } catch (SQLException ex) {
            mostrarErrorBD("registrar el cliente", ex);
        }
    }

    /** Guarda los cambios del cliente seleccionado (UPDATE). */
    private void actualizarCliente() {
        if (clienteSeleccionado == null) {
            avisar("Seleccione un cliente de la tabla para actualizarlo.");
            return;
        }
        Cliente c = leerFormulario();
        c.setIdCliente(clienteSeleccionado.getIdCliente());
        c.setCodigoCliente(clienteSeleccionado.getCodigoCliente());
        if (!datosValidos(c)) {
            return;
        }
        try {
            if (dao.existeDui(c.getDui(), c.getIdCliente())) {
                avisar("El DUI " + c.getDui() + " ya pertenece a otro cliente.");
                return;
            }
            int opcion = JOptionPane.showConfirmDialog(this,
                    "¿Guardar los cambios del cliente " + c.getCodigoCliente() + "?",
                    "Confirmar actualización", JOptionPane.YES_NO_OPTION);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
            dao.actualizar(c);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cliente " + c.getCodigoCliente() + " actualizado correctamente.");
        } catch (SQLException ex) {
            mostrarErrorBD("actualizar el cliente", ex);
        }
    }

    /** Cambia solo el estado del cliente seleccionado (Activo / Inactivo / Bloqueado). */
    private void cambiarEstado() {
        if (clienteSeleccionado == null) {
            avisar("Seleccione un cliente de la tabla para cambiar su estado.");
            return;
        }
        EstadoCliente nuevo = estadoDelCombo();
        if (nuevo == clienteSeleccionado.getEstado()) {
            avisar("El cliente ya se encuentra en estado " + nuevo + ".\n"
                    + "Elija otro estado en la lista \"Estado\" y vuelva a intentarlo.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Cambiar el estado de " + clienteSeleccionado + "\nde " + clienteSeleccionado.getEstado()
                        + " a " + nuevo + "?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String codigo = clienteSeleccionado.getCodigoCliente();
            dao.cambiarEstado(clienteSeleccionado.getIdCliente(), nuevo);
            recargarTabla();
            limpiarFormulario();
            mostrarMensaje("Cliente " + codigo + " ahora está " + nuevo + ".");
        } catch (SQLException ex) {
            mostrarErrorBD("cambiar el estado del cliente", ex);
        }
    }

    /** Eliminar cliente */
    private void eliminarCliente() {
    if (clienteSeleccionado == null) {
        avisar("Seleccione un cliente de la tabla para eliminarlo.");
        return;
    }
    int opcion = JOptionPane.showConfirmDialog(this,
            "¿Eliminar definitivamente al cliente " + clienteSeleccionado + "?\n"
            + "Si tiene cuentas asociadas, el sistema no permitirá eliminarlo.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (opcion != JOptionPane.YES_OPTION) {
        return;
    }
    try {
        dao.eliminar(clienteSeleccionado.getIdCliente());
        recargarTabla();
        limpiarFormulario();
        mostrarMensaje("Cliente eliminado correctamente.");
    } catch (SQLException ex) {
        mostrarErrorBD("eliminar el cliente", ex);
    }
}

    private EstadoCliente estadoDelCombo() {
        return EstadoCliente.desdeEtiqueta((String) cmbEstado.getSelectedItem());
    }

   
    
    /** Lee los campos del formulario y arma un objeto Cliente. */
    private Cliente leerFormulario() {
        Cliente c = new Cliente();
        c.setCodigoCliente(txtCodigo.getText());
        c.setDui(txtDui.getText());
        c.setNombreCompleto(txtNombre.getText());
        c.setTelefono(txtTelefono.getText());
        c.setCorreo(txtCorreo.getText());
        c.setDireccion(txtDireccion.getText());
        c.setEstado(estadoDelCombo());
        c.normalizarTelefono();
        return c;
    }

    /** Valida con las reglas de Cliente y muestra todos los errores juntos. */
    private boolean datosValidos(Cliente c) {
        List<String> errores = c.validar();
        if (errores.isEmpty()) {
            return true;
        }
        StringBuilder sb = new StringBuilder("Corrija lo siguiente:\n");
        for (String error : errores) {
            sb.append("  • ").append(error).append('\n');
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Datos no válidos", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private void cargarEnFormulario(Cliente c) {
        clienteSeleccionado = c;
        txtCodigo.setText(c.getCodigoCliente());
        txtDui.setText(c.getDui());
        txtNombre.setText(c.getNombreCompleto());
        txtTelefono.setText(c.getTelefono());
        txtCorreo.setText(c.getCorreo());
        txtDireccion.setText(c.getDireccion());
        cmbEstado.setSelectedItem(c.getEstado().getEtiqueta());
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnCambiarEstado.setEnabled(true);
        mostrarMensaje("Editando " + c + ".");
    }

    /** Vuelve al modo "nuevo cliente" con el siguiente código disponible. */
    private void limpiarFormulario() {
        clienteSeleccionado = null;
        tblClientes.clearSelection();
        txtDui.setText("");
        txtNombre.setText("");
        txtTelefono.setText("");
        txtCorreo.setText("");
        txtDireccion.setText("");
        cmbEstado.setSelectedItem("Activo");
        try {
            txtCodigo.setText(dao.siguienteCodigo());
        } catch (SQLException ex) {
            txtCodigo.setText("");
            mostrarMensaje("Sin conexión con la base de datos.");
        }
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnCambiarEstado.setEnabled(false);
        txtDui.requestFocusInWindow();
    }

    /** Consulta la BD según los filtros de búsqueda y llena la tabla (DefaultTableModel). */
    private void recargarTabla() {
        String filtro = (String) cmbFiltroEstado.getSelectedItem();
        EstadoCliente estado = TODOS.equals(filtro) ? null : EstadoCliente.desdeEtiqueta(filtro);
        try {
            clientesMostrados = dao.buscar(txtBuscar.getText(), estado);
        } catch (SQLException ex) {
            clientesMostrados = new ArrayList<>();
            mostrarErrorBD("consultar los clientes", ex);
        }
        // Se quita el ordenador mientras se llena el modelo y se vuelve a poner al final
        tblClientes.setRowSorter(null);
        modeloTabla.setRowCount(0);
        for (Cliente c : clientesMostrados) {
            modeloTabla.addRow(new Object[]{
                c.getCodigoCliente(), c.getDui(), c.getNombreCompleto(), c.getTelefono(),
                c.getCorreo(), c.getDireccion(), c.getEstado()
            });
        }
        tblClientes.setRowSorter(new TableRowSorter<>(modeloTabla));
        mostrarMensaje(clientesMostrados.size() + " cliente(s) en la lista.");
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
    }

    private void avisar(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarErrorBD(String accion, SQLException ex) {
        String detalle;
        if (ex.getErrorCode() == 1062) {
            detalle = "Ya existe un cliente con ese código o DUI. Pulse \"Limpiar\" e intente de nuevo.";
        } else {
            detalle = "Verifique que MySQL esté encendido y que los datos de ConexionDB sean correctos.\n\n"
                    + "Detalle: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, "No se pudo " + accion + ".\n" + detalle,
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    /** Pinta la columna Estado: verde = Activo, gris = Inactivo, rojo = Bloqueado. */
    private static class RenderizadorEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (!seleccionada) {
                if (valor == EstadoCliente.ACTIVO) {
                    c.setForeground(new Color(27, 127, 59));
                } else if (valor == EstadoCliente.BLOQUEADO) {
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

        txtCodigo = new javax.swing.JTextField();
        txtDui = new javax.swing.JTextField();
        txtDireccion = new javax.swing.JTextField();
        txtBuscar = new javax.swing.JTextField();
        txtNombre = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        cmbEstado = new javax.swing.JComboBox<>();
        cmbFiltroEstado = new javax.swing.JComboBox<>();
        btnCambiarEstado = new javax.swing.JButton();
        txtCorreo = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblClientes = new javax.swing.JTable();
        lblMensaje = new javax.swing.JLabel();
        btnActualizar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        btnMostrarTodos = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        btnEliminar = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        btnIrACuentas = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        txtCodigo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCodigoActionPerformed(evt);
            }
        });

        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        cmbFiltroEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnCambiarEstado.setText("Cambiar estado");

        txtCorreo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCorreoActionPerformed(evt);
            }
        });

        tblClientes.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblClientes);

        btnActualizar.setText("Actualizar");

        btnGuardar.setText("Guardar");

        btnLimpiar.setText("Limpiar");

        btnBuscar.setText("Buscar");

        btnMostrarTodos.setText("Mostrar todos");

        jLabel1.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel1.setText("Buscar (código, DUI o nombre): ");

        jLabel2.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel2.setText("DUI:");

        jLabel3.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel3.setText("Correo:");

        jLabel4.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel4.setText("Nombre:");

        jLabel5.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel5.setText("Dirección:");

        jLabel6.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel6.setText("Teléfono:");

        jLabel7.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel7.setText("Estado:");

        jLabel8.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel8.setText("Código:");

        btnEliminar.setText("Eliminar");
        btnEliminar.setMaximumSize(new java.awt.Dimension(72, 23));
        btnEliminar.setMinimumSize(new java.awt.Dimension(72, 23));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setText("Datos del usuario");

        btnIrACuentas.setText("Ir a Cuentas");
        btnIrACuentas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIrACuentasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(lblMensaje, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel8))
                        .addGap(39, 39, 39)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtCodigo, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                                    .addComponent(txtDui))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 78, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(txtCorreo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
                                    .addComponent(txtNombre, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtDireccion))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addGap(423, 423, 423))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnCambiarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnIrACuentas, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(246, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cmbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnMostrarTodos, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(64, 64, 64))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtDui, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCambiarEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnIrACuentas, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(108, 108, 108)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtBuscar)
                    .addComponent(cmbFiltroEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMostrarTodos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1))
                .addGap(9, 9, 9)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel9.getAccessibleContext().setAccessibleName("lbl9");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtCodigoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCodigoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCodigoActionPerformed

    private void txtCorreoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCorreoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCorreoActionPerformed

    private void btnIrACuentasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIrACuentasActionPerformed

    this.dispose();                    
    CuentasForm destino = new CuentasForm();
    destino.setVisible(true); 
    }//GEN-LAST:event_btnIrACuentasActionPerformed


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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ClientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientesForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientesForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCambiarEstado;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIrACuentas;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMostrarTodos;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbFiltroEstado;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JTable tblClientes;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtCodigo;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtDui;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}
