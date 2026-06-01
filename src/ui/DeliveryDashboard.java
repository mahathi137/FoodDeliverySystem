package ui;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DeliveryDashboard extends JFrame {
    private final int userId;
    private int deliveryId = -1;
    private String deliveryAgentName = ""; 
    private DefaultTableModel model;
    private JTable table;
    
    // Status constants for clarity
    private static final String STATUS_OUT_FOR_DELIVERY = "On the Way"; 
    private static final String STATUS_DELIVERED = "Delivered"; 

    public DeliveryDashboard(int userId) {
        this.userId = userId;
        setSize(900,600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // This method will now rely on the assumption that delivery_id = user_id
        mapDeliveryId(); 
        
        setTitle("Delivery Dashboard - " + (deliveryAgentName.isEmpty() ? "Agent" : deliveryAgentName));
        
        initUI();
        loadAssignedOrders();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        JLabel lbl = new JLabel("Delivery - Assigned Orders", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lbl, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"OrderID","Customer","Hotel","Total","Status", "ETA"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel pnl = new JPanel();
        JButton btnRefresh = new JButton("Refresh");
        JButton btnOut = new JButton("Set Out for Delivery");
        JButton btnDelivered = new JButton("Set Delivered");
        JButton btnLogout = new JButton("Logout");
        
        pnl.add(btnRefresh);
        pnl.add(btnOut);
        pnl.add(btnDelivered);
        pnl.add(btnLogout);
        add(pnl, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadAssignedOrders());
        btnOut.addActionListener(e -> updateSelectedStatus(STATUS_OUT_FOR_DELIVERY));
        btnDelivered.addActionListener(e -> updateSelectedStatus(STATUS_DELIVERED));
        btnLogout.addActionListener(e -> { dispose(); new LoginUI().setVisible(true); });
    }

    /**
     * ✅ FINAL FIX: Assumes delivery_boys.delivery_id is the same as users.user_id.
     * This avoids the "Unknown column" error by not using a foreign key join column.
     */
    private void mapDeliveryId() {
        try (Connection cn = DBConnection.getConnection();
             // 1. Get the agent's name from the USERS table
             PreparedStatement psUser = cn.prepareStatement("SELECT fullname FROM users WHERE user_id=?")) {
            psUser.setInt(1, userId);
            try (ResultSet rsUser = psUser.executeQuery()) {
                if (rsUser.next()) {
                    deliveryAgentName = rsUser.getString("fullname");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching user name: " + ex.getMessage());
        }
        
        // 2. Map user_id to delivery_id by checking if the user_id exists as a delivery_id
        // This is a direct check, assuming the IDs are the same.
        String sql = "SELECT delivery_id FROM delivery_boys WHERE delivery_id=?"; 
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setInt(1, userId); // Use the logged-in user ID as the delivery ID
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    deliveryId = rs.getInt("delivery_id");
                } else {
                    JOptionPane.showMessageDialog(this, "Delivery ID not mapped. Logged-in user ID ("+userId+") is not registered as a delivery agent in 'delivery_boys' table.", "Mapping Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error mapping delivery ID: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void loadAssignedOrders() {
        model.setRowCount(0);
        if (deliveryId < 0) {
            System.err.println("Delivery ID not mapped. Cannot load orders.");
            return;
        }
        
        // This SQL remains correct as it joins orders to users and hotels
        String sql = "SELECT o.order_id, u.fullname, h.name, o.total_amount, o.delivery_status, o.eta FROM orders o " +
                     "JOIN users u ON o.user_id=u.user_id " +
                     "JOIN hotels h ON o.hotel_id=h.hotel_id " +
                     "WHERE o.delivery_id=? AND o.delivery_status NOT IN ('Delivered', 'Cancelled') " +
                     "ORDER BY o.order_id DESC";
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt(1), 
                        rs.getString(2), 
                        rs.getString(3), 
                        rs.getBigDecimal(4), 
                        rs.getString(5), 
                        rs.getInt(6) + " min" 
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading orders: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // updateSelectedStatus method remains correct
    private void updateSelectedStatus(String status) {
        int r = table.getSelectedRow();
        if (r < 0) { 
            JOptionPane.showMessageDialog(this,"Select an order to update."); 
            return; 
        }
        
        int orderId = (int) model.getValueAt(r,0);
        String currentStatus = (String) model.getValueAt(r, 4);

        if (status.equals(STATUS_OUT_FOR_DELIVERY) && (currentStatus.equals(STATUS_OUT_FOR_DELIVERY) || currentStatus.equals(STATUS_DELIVERED))) {
            JOptionPane.showMessageDialog(this, "Order is already dispatched or delivered.");
            return;
        }
        if (status.equals(STATUS_DELIVERED) && currentStatus.equals(STATUS_DELIVERED)) {
            JOptionPane.showMessageDialog(this, "Order is already delivered.");
            return;
        }

        int eta = status.equals(STATUS_DELIVERED) ? 0 : 15;
        
        Connection cn = null;
        try { 
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);
            
            // 1. Update Order Status and ETA
            String updateOrderSql = "UPDATE orders SET delivery_status=?, eta=? WHERE order_id=?";
            try (PreparedStatement ps = cn.prepareStatement(updateOrderSql)) {
                ps.setString(1, status);
                ps.setInt(2, eta);
                ps.setInt(3, orderId);
                ps.executeUpdate();
            }
            
            // 2. Free up Delivery Boy if Delivered
            if (status.equals(STATUS_DELIVERED)) {
                String updateDeliveryBoySql = "UPDATE delivery_boys SET status='available' WHERE delivery_id=?";
                try (PreparedStatement psDb = cn.prepareStatement(updateDeliveryBoySql)) {
                    psDb.setInt(1, deliveryId);
                    psDb.executeUpdate();
                }
            }
            
            cn.commit();
            
            loadAssignedOrders();
            JOptionPane.showMessageDialog(this, "Status updated to: " + status);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,"Update failed: "+ex.getMessage());
            ex.printStackTrace();
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