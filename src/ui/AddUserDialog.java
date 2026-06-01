package ui;

import db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddUserDialog extends JDialog {
    private JTextField txtUsername, txtFullname, txtPhone;
    private JPasswordField txtPassword;
    private JComboBox<String> cbRole;
    private AdminDashboard adminDashboard;

    public AddUserDialog(AdminDashboard owner) {
        super(owner, "Add New Admin or Delivery User", true);
        this.adminDashboard = owner;
        setSize(450, 400);
        setLocationRelativeTo(owner);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel title = new JLabel("Create Admin/Delivery Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        formPanel.add(new JLabel("Role:"));
        cbRole = new JComboBox<>(new String[]{"delivery", "admin", "customer"});
        formPanel.add(cbRole);

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
        JButton btnAdd = new JButton("Create Account");
        btnAdd.addActionListener(e -> addUser());
        formPanel.add(btnAdd);

        add(formPanel, BorderLayout.CENTER);
    }

    private void addUser() {
        String role = (String) cbRole.getSelectedItem();
        String fullname = txtFullname.getText().trim();
        String phone = txtPhone.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (fullname.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        Connection cn = null;
        try {
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);

            // 1. Insert into users table
            String sqlUser = "INSERT INTO users (username, password, fullname, role, phone) VALUES (?, ?, ?, ?, ?)";
            int userId = -1;
            try (PreparedStatement ps = cn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, fullname);
                ps.setString(4, role);
                ps.setString(5, phone);
                ps.executeUpdate();
                
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) userId = gk.getInt(1);
                }
            }

            // 2. If role is 'delivery', insert into delivery_boys table
            if (role.equals("delivery") && userId != -1) {
                String sqlDelivery = "INSERT INTO delivery_boys (user_id, name, phone, status) VALUES (?, ?, ?, 'available')";
                try (PreparedStatement ps = cn.prepareStatement(sqlDelivery)) {
                    ps.setInt(1, userId);
                    ps.setString(2, fullname);
                    ps.setString(3, phone);
                    ps.executeUpdate();
                }
            }
            
            cn.commit();
            
            JOptionPane.showMessageDialog(this, "Account created successfully for role: " + role);
            adminDashboard.loadAllData();
            dispose();
            
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "Username already taken or database integrity violation.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Creation failed: " + ex.getMessage());
            ex.printStackTrace();
            if (cn != null) { try { cn.rollback(); } catch (SQLException rollbackEx) { /* Ignore */ } }
        } finally {
            if (cn != null) { try { cn.close(); } catch (SQLException closeEx) { /* Ignore */ } }
        }
    }
}