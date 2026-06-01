package ui;

import db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddHotelDialog extends JDialog {
    private JTextField txtName, txtLocation, txtPhone; 
    private AdminDashboard adminDashboard;

    public AddHotelDialog(AdminDashboard owner) {
        super(owner, "Add New Hotel", true);
        this.adminDashboard = owner;
        setSize(450, 300);
        setLocationRelativeTo(owner);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel title = new JLabel("Enter Hotel Details", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        formPanel.add(new JLabel("Name:"));
        txtName = new JTextField();
        formPanel.add(txtName);
        
        formPanel.add(new JLabel("Location:"));
        txtLocation = new JTextField();
        formPanel.add(txtLocation);

        formPanel.add(new JLabel("Phone:"));
        txtPhone = new JTextField();
        formPanel.add(txtPhone);

        add(formPanel, BorderLayout.CENTER);
        
        JButton btnAdd = new JButton("Add Hotel");
        btnAdd.addActionListener(e -> addHotel());
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.add(btnAdd);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void addHotel() {
        String name = txtName.getText().trim();
        String location = txtLocation.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Location are required.");
            return;
        }

        String sql = "INSERT INTO hotels (name, location, phone) VALUES (?, ?, ?)";
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setString(1, name);
            ps.setString(2, location);
            ps.setString(3, phone);

            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Hotel added successfully!");
            adminDashboard.loadAllData();
            dispose();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Hotel addition failed: " + ex.getMessage());
        }
    }
}