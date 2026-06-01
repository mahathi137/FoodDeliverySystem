package ui;

import db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class CreateAccountDialog extends JDialog {
    private JTextField txtUsername, txtFullname, txtPhone;
    private JPasswordField txtPassword;

    public CreateAccountDialog(Frame owner) {
        super(owner, "Create New Account", true);
        setSize(400, 350);
        setLocationRelativeTo(owner);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        JLabel title = new JLabel("Customer Registration", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        formPanel.add(new JLabel("Full Name:"));
        txtFullname = new JTextField();
        formPanel.add(txtFullname);
        
        formPanel.add(new JLabel("Phone:"));
        txtPhone = new JTextField();
        formPanel.add(txtPhone);

        formPanel.add(new JLabel("Username (Login):"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);
        
        formPanel.add(new JLabel("")); // Spacer
        JButton btnRegister = new JButton("Register");
        btnRegister.addActionListener(e -> registerUser());
        formPanel.add(btnRegister);

        add(formPanel, BorderLayout.CENTER);
        getRootPane().setDefaultButton(btnRegister);
    }

    private void registerUser() {
        String fullname = txtFullname.getText().trim();
        String phone = txtPhone.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (fullname.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        // Simple validation checks (can be expanded)
        if (password.length() < 4) {
             JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.");
             return;
        }
        
        String sql = "INSERT INTO users (username, password, fullname, role, phone) VALUES (?, ?, ?, 'customer', ?)";
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullname);
            ps.setString(4, phone);

            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Account created successfully! You can now log in.");
            dispose();
            
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "Username already taken. Please choose a different one.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}