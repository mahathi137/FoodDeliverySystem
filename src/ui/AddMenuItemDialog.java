package ui;

import db.DBConnection;
import model.Restaurant; 
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddMenuItemDialog extends JDialog {
    private JComboBox<Restaurant> cbHotel;
    private JTextField txtName, txtCategory, txtPrice;
    private AdminDashboard adminDashboard;

    public AddMenuItemDialog(AdminDashboard owner) {
        super(owner, "Add New Menu Item", true);
        this.adminDashboard = owner;
        setSize(450, 350);
        setLocationRelativeTo(owner);
        initUI();
        loadHotels();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel title = new JLabel("Enter Menu Item Details", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        formPanel.add(new JLabel("Select Hotel:"));
        cbHotel = new JComboBox<>();
        formPanel.add(cbHotel);
        
        formPanel.add(new JLabel("Item Name:"));
        txtName = new JTextField();
        formPanel.add(txtName);
        
        formPanel.add(new JLabel("Category:"));
        txtCategory = new JTextField();
        formPanel.add(txtCategory);

        formPanel.add(new JLabel("Price (INR):"));
        txtPrice = new JTextField();
        formPanel.add(txtPrice);

        formPanel.add(new JLabel("")); 
        JButton btnAdd = new JButton("Add Menu Item");
        btnAdd.addActionListener(e -> addMenuItem());
        formPanel.add(btnAdd);

        add(formPanel, BorderLayout.CENTER);
    }
    
    private void loadHotels() {
        List<Restaurant> hotels = new ArrayList<>();
        // Fetch only the necessary fields for the Restaurant model
        String sql = "SELECT hotel_id, name, location, phone FROM hotels ORDER BY name";
        try (Connection cn = DBConnection.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                // Category field is omitted as it's not in your hotels table structure
                hotels.add(new Restaurant(
                    rs.getInt(1), rs.getString(2), "", rs.getString(3), rs.getString(4)
                ));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading hotels: " + ex.getMessage());
            return;
        }
        
        cbHotel.removeAllItems();
        if (hotels.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hotels found. Add a hotel first.");
        } else {
            for (Restaurant h : hotels) {
                cbHotel.addItem(h);
            }
        }
    }

    private void addMenuItem() {
        Restaurant selectedHotel = (Restaurant) cbHotel.getSelectedItem();
        if (selectedHotel == null) {
            JOptionPane.showMessageDialog(this, "Please select a hotel.");
            return;
        }
        
        String name = txtName.getText().trim();
        String category = txtCategory.getText().trim();
        String priceStr = txtPrice.getText().trim();
        double price;

        if (name.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Price are required.");
            return;
        }
        
        try { price = Double.parseDouble(priceStr); if (price <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this,"Invalid price. Must be a positive number."); return; }

        String sql = "INSERT INTO menu_items (hotel_id, item_name, category, price) VALUES (?, ?, ?, ?)";
        
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setInt(1, selectedHotel.getId());
            ps.setString(2, name);
            ps.setString(3, category);
            ps.setDouble(4, price);

            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Menu item added successfully!");
            adminDashboard.loadAllData();
            dispose();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Menu item addition failed: " + ex.getMessage());
        }
    }
}