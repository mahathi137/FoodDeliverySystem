package ui;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    
    // DefaultTableModel references for easy refresh
    private DefaultTableModel ht, mt, ot;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(1000, 700); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel lbl = new JLabel("Admin Control Panel", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lbl.setForeground(new Color(150, 0, 0));
        add(lbl, BorderLayout.NORTH);

        // --- Action Panel (EAST) ---
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Admin Actions"));
        actionPanel.setPreferredSize(new Dimension(200, getHeight()));

        JButton btnAddHotel = new JButton("Add New Hotel");
        JButton btnAddMenu = new JButton("Add Menu Item");
        JButton btnAddUser = new JButton("Add User/Delivery");
        JButton btnRefresh = new JButton("Refresh Data");
        JButton btnLogout = new JButton("Logout");

        // Align buttons
        Dimension buttonSize = new Dimension(180, 30);
        btnAddHotel.setMaximumSize(buttonSize);
        btnAddMenu.setMaximumSize(buttonSize);
        btnAddUser.setMaximumSize(buttonSize);
        btnRefresh.setMaximumSize(buttonSize);
        btnLogout.setMaximumSize(buttonSize);
        
        actionPanel.add(Box.createVerticalStrut(20));
        actionPanel.add(btnAddHotel);
        actionPanel.add(Box.createVerticalStrut(10));
        actionPanel.add(btnAddMenu);
        actionPanel.add(Box.createVerticalStrut(10));
        actionPanel.add(btnAddUser);
        actionPanel.add(Box.createVerticalStrut(20));
        actionPanel.add(btnRefresh);
        actionPanel.add(Box.createVerticalStrut(150)); // Spacer
        actionPanel.add(btnLogout);
        actionPanel.add(Box.createVerticalStrut(20));

        // --- Action Listeners ---
        btnAddHotel.addActionListener(e -> new AddHotelDialog(this).setVisible(true));
        btnAddMenu.addActionListener(e -> new AddMenuItemDialog(this).setVisible(true));
        btnAddUser.addActionListener(e -> new AddUserDialog(this).setVisible(true));
        btnRefresh.addActionListener(e -> loadAllData());
        btnLogout.addActionListener(e -> { dispose(); new LoginUI().setVisible(true); });

        add(actionPanel, BorderLayout.EAST);

        // --- Data Tabs Setup (CENTER) ---
        JTabbedPane tabs = new JTabbedPane();

        // 1. Hotels
        ht = new DefaultTableModel(new String[]{"ID","Name","Location","Phone"},0);
        JTable tHot = new JTable(ht);
        tabs.add("Hotels", new JScrollPane(tHot));

        // 2. Menu Items
        mt = new DefaultTableModel(new String[]{"ItemID","HotelID","Item","Category","Price"},0);
        JTable tMenu = new JTable(mt);
        tabs.add("Menu Items", new JScrollPane(tMenu));

        // 3. Orders
        ot = new DefaultTableModel(new String[]{"OrderID","UserID","HotelID","Total","Status","Payment"},0);
        JTable tOrders = new JTable(ot);
        tabs.add("Orders", new JScrollPane(tOrders));
        
        // 4. New: User/Delivery Tab
        DefaultTableModel ut = new DefaultTableModel(new String[]{"UserID","Username","Full Name","Role","Phone"},0);
        JTable tUsers = new JTable(ut);
        tabs.add("Users/Delivery", new JScrollPane(tUsers));


        add(tabs, BorderLayout.CENTER);
        
        // Initial data load
        loadAllData();
    }

    // New method to encapsulate all data loading and refreshing
    public void loadAllData() {
        // Clear all tables
        ht.setRowCount(0); mt.setRowCount(0); ot.setRowCount(0);
        
        // Safely get the Users table model
        JTabbedPane tabs = (JTabbedPane) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JScrollPane scrollPane = (JScrollPane) tabs.getComponentAt(3);
        JTable tUsers = (JTable) scrollPane.getViewport().getView();
        DefaultTableModel ut = (DefaultTableModel) tUsers.getModel();
        ut.setRowCount(0);

        try (Connection cn = DBConnection.getConnection()) {
            // Load Hotels (4 columns based on your schema)
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT hotel_id,name,location,phone FROM hotels")) {
                while (rs.next()) ht.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
            // Load Menu Items
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT item_id,hotel_id,item_name,category,price FROM menu_items")) {
                while (rs.next()) mt.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5)});
            }
            // Load Orders
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT order_id,user_id,hotel_id,total_amount,delivery_status,payment_status FROM orders")) {
                while (rs.next()) ot.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4), rs.getString(5), rs.getString(6)});
            }
           
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT user_id,username,fullname,role,phone FROM users ORDER BY role, fullname")) {
                while (rs.next()) ut.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}