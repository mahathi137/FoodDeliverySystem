package ui;

import db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginUI() {
        setTitle("Food Delivery System - Login");
        setSize(400, 280); // Increased size slightly
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Welcome! Please Login", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        panel.add(new JLabel("Username:"));
        txtUsername = new JTextField(15);
        panel.add(txtUsername);

        panel.add(new JLabel("Password:"));
        txtPassword = new JPasswordField(15);
        panel.add(txtPassword);

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> attemptLogin());
        
        JButton btnCreateAccount = new JButton("New User? Create Account"); // New Button
        btnCreateAccount.addActionListener(e -> openCreateAccountDialog());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(btnCreateAccount);
        bottomPanel.add(btnLogin);

        add(panel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void openCreateAccountDialog() {
        // Assume you have a CreateAccountDialog class
        new CreateAccountDialog(this).setVisible(true);
    }

    private void attemptLogin() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            return;
        }

        String sql = "SELECT user_id, fullname, role FROM users WHERE username=? AND password=?";
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("user_id");
                    String fullname = rs.getString("fullname");
                    String role = rs.getString("role");
                    
                    switch (role) {
                        case "admin":
                            new AdminDashboard().setVisible(true);
                            break;
                        case "customer":
                            new CustomerDashboard(id, fullname).setVisible(true);
                            break;
                        case "delivery":
                            // Assuming delivery agent user_id mapping is handled in DeliveryDashboard constructor
                            new DeliveryDashboard(id).setVisible(true);
                            break;
                        default:
                            JOptionPane.showMessageDialog(this, "Unknown user role: " + role);
                            return;
                    }
                    dispose(); 
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password. Try creating an account.");
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }
}