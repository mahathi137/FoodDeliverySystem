package ui;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TrackOrderDialog extends JDialog {
    private final int userId;
    private JTable tblOrders;
    private DefaultTableModel ordersModel;
    private JTextArea txtDetails;
    private JButton btnRefresh, btnCancelOrder;

    public TrackOrderDialog(Frame owner, int userId) {
        super(owner, "Track My Orders", true);
        this.userId = userId;
        setSize(700,500);
        setLocationRelativeTo(owner);
        initUI();
        loadOrders();
    }

    private void initUI() {
        setLayout(new BorderLayout(8,8));
        ordersModel = new DefaultTableModel(new String[]{"Order ID","Total","Status","Payment","ETA(min)","Created At"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        tblOrders = new JTable(ordersModel);
        add(new JScrollPane(tblOrders), BorderLayout.WEST);
        tblOrders.setPreferredScrollableViewportSize(new Dimension(300,400));
        tblOrders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtDetails = new JTextArea();
        txtDetails.setEditable(false);
        add(new JScrollPane(txtDetails), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefresh = new JButton("Refresh");
        btnCancelOrder = new JButton("Cancel Selected Order");
        bottom.add(btnCancelOrder);
        bottom.add(btnRefresh);
        add(bottom, BorderLayout.SOUTH);

        tblOrders.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedOrderDetails();
        });

        btnRefresh.addActionListener(e -> loadOrders());
        btnCancelOrder.addActionListener(e -> cancelSelectedOrder());
    }

    private void loadOrders() {
        ordersModel.setRowCount(0);
        String sql = "SELECT order_id, total_amount, delivery_status, payment_status, eta, created_at FROM orders WHERE user_id=? ORDER BY created_at DESC";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ordersModel.addRow(new Object[]{
                            rs.getInt("order_id"),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("delivery_status"),
                            rs.getString("payment_status"),
                            rs.getInt("eta"),
                            rs.getTimestamp("created_at")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading orders: " + ex.getMessage());
        }
    }

    private void showSelectedOrderDetails() {
        int row = tblOrders.getSelectedRow();
        if (row < 0) { txtDetails.setText(""); return; }
        int orderId = (int) ordersModel.getValueAt(row,0);

        StringBuilder sb = new StringBuilder();
        try (Connection cn = DBConnection.getConnection()) {
            // order + hotel + delivery
        	
            String q = "SELECT o.order_id,o.total_amount,o.address,o.specifications,o.payment_mode,o.payment_status,o.delivery_status,o.eta,o.created_at," +
                       "h.name AS hotel_name, d.name AS delivery_name, d.phone AS delivery_phone " +
                       "FROM orders o LEFT JOIN hotels h ON o.hotel_id=h.hotel_id " +
                       "LEFT JOIN delivery_boys d ON o.delivery_id=d.delivery_id WHERE o.order_id=?";
            try (PreparedStatement ps = cn.prepareStatement(q)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sb.append("Order ID: ").append(rs.getInt("order_id")).append("\n");
                        sb.append("Hotel: ").append(rs.getString("hotel_name")).append("\n");
                        sb.append("Total: ₹").append(rs.getBigDecimal("total_amount")).append("\n");
                        sb.append("Payment Mode: ").append(rs.getString("payment_mode")).append("\n");
                        sb.append("Payment Status: ").append(rs.getString("payment_status")).append("\n");
                        sb.append("Delivery Status: ").append(rs.getString("delivery_status")).append("\n");
                        sb.append("ETA: ").append(rs.getInt("eta")).append(" minutes\n");
                        sb.append("Address: ").append(rs.getString("address")).append("\n");
                        sb.append("Specifications: ").append(rs.getString("specifications")).append("\n");
                        String dname = rs.getString("delivery_name");
                        String dphone = rs.getString("delivery_phone");
                        sb.append("Delivery Person: ").append(dname==null ? "Not assigned" : dname + " ("+dphone+")").append("\n\n");
                    }
                }
            }

            // items
            sb.append("Items:\n");
            String q2 = "SELECT m.item_name, oi.quantity, oi.price FROM order_items oi JOIN menu_items m ON oi.item_id=m.item_id WHERE oi.order_id=?";
            try (PreparedStatement ps2 = cn.prepareStatement(q2)) {
                ps2.setInt(1, orderId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        sb.append(" - ").append(rs2.getString("item_name")).append(" x").append(rs2.getInt("quantity"))
                          .append(" @₹").append(rs2.getBigDecimal("price")).append("\n");
                    }
                }
            }

        } catch (SQLException ex) {
            sb.append("Error reading details: ").append(ex.getMessage());
        }
        txtDetails.setText(sb.toString());
    }

    private void cancelSelectedOrder() {
        int row = tblOrders.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Select an order to cancel."); return; }
        int orderId = (int) ordersModel.getValueAt(row,0);
        String status = (String) ordersModel.getValueAt(row,2);
        
        // Allow cancellation if status is Preparing, Pending, or Assigned to Delivery
        if (!status.equalsIgnoreCase("Preparing") && !status.equalsIgnoreCase("Pending") && !status.equalsIgnoreCase("Assigned to Delivery")) {
            JOptionPane.showMessageDialog(this, "Cannot cancel order after it has been dispatched/picked up.");
            return;
        }
        
        int ok = JOptionPane.showConfirmDialog(this, "Cancel order #" + orderId + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        Integer deliveryId = null; // Use Integer to correctly handle null
        Connection cn = null;

        try { 
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);
            
            // 1. Get current delivery_id if assigned
            String getDeliveryIdSql = "SELECT delivery_id FROM orders WHERE order_id=?";
            try(PreparedStatement ps = cn.prepareStatement(getDeliveryIdSql)) {
                ps.setInt(1, orderId);
                try(ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Use getObject and check for null
                        Object dbIdObj = rs.getObject("delivery_id"); 
                        if (dbIdObj != null) deliveryId = (Integer) dbIdObj;
                    }
                }
            }

            // 2. Update order status and set delivery_id to NULL
            String updateOrderSql = "UPDATE orders SET delivery_status='Cancelled', delivery_id=NULL, eta=0 WHERE order_id=?";
            try (PreparedStatement ps = cn.prepareStatement(updateOrderSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            
            // 3. Reset delivery boy status if they were assigned
            if (deliveryId != null) {
                 String updateDeliveryBoySql = "UPDATE delivery_boys SET status='available' WHERE delivery_id=?";
                 try (PreparedStatement ps = cn.prepareStatement(updateDeliveryBoySql)) {
                    ps.setInt(1, deliveryId);
                    ps.executeUpdate();
                }
            }
            
            cn.commit();
            
            JOptionPane.showMessageDialog(this, "Order cancelled.");
            loadOrders();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cancel failed: " + ex.getMessage());
             if (cn != null) {
                try {
                    cn.rollback(); 
                } catch (SQLException rollbackEx) { /* Ignore */ }
            }
        } finally {
             if (cn != null) {
                try {
                    cn.close();
                } catch (SQLException closeEx) { /* Ignore */ }
            }
        }
    }
}