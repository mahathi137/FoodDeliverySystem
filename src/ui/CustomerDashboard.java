package ui;

import db.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import javax.imageio.ImageIO;
import java.net.URL;

public class CustomerDashboard extends JFrame {
    private final int userId;
    private final String fullname;

    private JTable tblHotels, tblMenu;
    private DefaultTableModel hotelsModel, menuModel, cartModel;
    private JLabel lblTotal;

    private double totalAmount = 0.0;

    public CustomerDashboard(int userId, String fullname) {
        this.userId = userId;
        this.fullname = fullname;
        initUI();
        loadHotels();
    }

    private void showError(String s) {
        JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void initUI() {
        setTitle("Customer Dashboard - " + fullname);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Customer Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0,90,170));
        add(title, BorderLayout.NORTH);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplit.setDividerLocation(300);

        hotelsModel = new DefaultTableModel(new String[]{"ID","Name","Location","Phone"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        tblHotels = new JTable(hotelsModel);
        tblHotels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topSplit.setLeftComponent(new JScrollPane(tblHotels));

        menuModel = new DefaultTableModel(new String[]{"No","DB_ID","Item Name","Category","Price"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        tblMenu = new JTable(menuModel);
        tblMenu.getColumnModel().getColumn(1).setMinWidth(0);
        tblMenu.getColumnModel().getColumn(1).setMaxWidth(0);
        topSplit.setRightComponent(new JScrollPane(tblMenu));

        add(topSplit, BorderLayout.CENTER);

        cartModel = new DefaultTableModel(new String[]{"DB_ID","Item Name","Qty","Price","Line Total"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        JButton btnViewMenu = new JButton("View Menu");
        JButton btnAddToCart = new JButton("Add to Cart");
        JButton btnViewCart = new JButton("View Cart");
        JButton btnPlaceOrder = new JButton("Place Order");
        JButton btnTrackOrder = new JButton("Track Order");
        JButton btnLogout = new JButton("Logout");

        lblTotal = new JLabel("Total: ₹%.2f".formatted(totalAmount));
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));

        buttonPanel.add(btnViewMenu);
        buttonPanel.add(btnAddToCart);
        buttonPanel.add(btnViewCart);
        buttonPanel.add(btnPlaceOrder);
        buttonPanel.add(btnTrackOrder);
        buttonPanel.add(btnLogout);
        buttonPanel.add(lblTotal);
        add(buttonPanel, BorderLayout.SOUTH);

        btnViewMenu.addActionListener(e -> loadMenuForSelectedHotel());
        btnAddToCart.addActionListener(e -> addSelectedItemToCart());
        btnViewCart.addActionListener(e -> viewCartDialog());
        btnPlaceOrder.addActionListener(e -> placeOrderDialog());
        btnTrackOrder.addActionListener(e -> { 
            new TrackOrderDialog(this, userId).setVisible(true); 
        });
        btnLogout.addActionListener(e -> { dispose(); new LoginUI().setVisible(true); });
    }

    private void viewCartDialog() {
        if (cartModel.getRowCount()==0) {
            JOptionPane.showMessageDialog(this, "Your cart is empty."); return;
        }
        JDialog cartDialog = new JDialog(this, "Your Shopping Cart", true);
        cartDialog.setSize(600, 400);
        cartDialog.setLocationRelativeTo(this);
        cartDialog.setLayout(new BorderLayout());

        JTable displayCartTable = new JTable(cartModel);
        displayCartTable.getColumnModel().getColumn(0).setMinWidth(0);
        displayCartTable.getColumnModel().getColumn(0).setMaxWidth(0);

        cartDialog.add(new JScrollPane(displayCartTable), BorderLayout.CENTER);

        JLabel dialogTotal = new JLabel(String.format("Cart Total: ₹%.2f", totalAmount), SwingConstants.RIGHT);
        dialogTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRemove = new JButton("Remove Item");
        JButton btnClose = new JButton("Close");

        btnRemove.addActionListener(e -> {
            int row = displayCartTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(cartDialog, "Select an item to remove."); return; }
            double lineTotal = (double) cartModel.getValueAt(row, 4);
            totalAmount -= lineTotal;
            cartModel.removeRow(row);
            updateTotalLabel();
            dialogTotal.setText(String.format("Cart Total: ₹%.2f", totalAmount));
            if (cartModel.getRowCount()==0) { cartDialog.dispose(); JOptionPane.showMessageDialog(this, "Cart is now empty."); }
        });

        bottomPanel.add(btnRemove);
        bottomPanel.add(dialogTotal);
        bottomPanel.add(btnClose);
        cartDialog.add(bottomPanel, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> cartDialog.dispose());
        cartDialog.setVisible(true);
    }

    private void loadHotels() {
        hotelsModel.setRowCount(0);
        String sql = "SELECT hotel_id, name, location, phone FROM hotels ORDER BY name";
        try (Connection cn = DBConnection.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                hotelsModel.addRow(new Object[]{rs.getInt("hotel_id"), rs.getString("name"), rs.getString("location"), rs.getString("phone")});
            }
        } catch (SQLException ex) {
            showError("Error loading hotels: " + ex.getMessage());
        }
    }

    private void loadMenuForSelectedHotel() {
        int row = tblHotels.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a hotel first."); return; }
        int hotelId = (int) hotelsModel.getValueAt(row,0);
        menuModel.setRowCount(0);
        String sql = "SELECT item_id, item_name, category, price FROM menu_items WHERE hotel_id=? ORDER BY category, item_name";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                int localNo = 1;
                while (rs.next()) {
                    menuModel.addRow(new Object[]{localNo++, rs.getInt("item_id"), rs.getString("item_name"), rs.getString("category"), rs.getDouble("price")});
                }
            }
        } catch (SQLException ex) {
            showError("Error loading menu: " + ex.getMessage());
        }
    }

    private void addSelectedItemToCart() {
        int row = tblMenu.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Select a menu item first."); return; }

        int dbItemId = (int) menuModel.getValueAt(row,1);
        String itemName = (String) menuModel.getValueAt(row,2);
        double price = ((Number)menuModel.getValueAt(row,4)).doubleValue();

        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity for "+itemName, "1");
        if (qtyStr==null) return;
        int qty;
        try { qty = Integer.parseInt(qtyStr); if (qty<=0) throw new NumberFormatException(); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this,"Invalid quantity."); return; }

        double line = price * qty;
        
        boolean found = false;
        for (int i=0; i<cartModel.getRowCount(); i++) {
            if ((int) cartModel.getValueAt(i, 0) == dbItemId) {
                int currentQty = (int) cartModel.getValueAt(i, 2);
                int newQty = currentQty + qty;
                double newLineTotal = price * newQty;
                
                cartModel.setValueAt(newQty, i, 2);
                cartModel.setValueAt(newLineTotal, i, 4);
                totalAmount += line; 
                found = true;
                break;
            }
        }
        
        if (!found) {
            cartModel.addRow(new Object[]{dbItemId, itemName, qty, price, line});
            totalAmount += line;
        }
        
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        lblTotal.setText(String.format("Total: ₹%.2f", totalAmount));
    }

    private int assignDeliveryBoy(Connection cn) throws SQLException {
        int deliveryId = -1;
        String findSql = "SELECT delivery_id FROM delivery_boys WHERE status='available' ORDER BY RAND() LIMIT 1 FOR UPDATE";
        
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(findSql)) {
            if (rs.next()) {
                deliveryId = rs.getInt("delivery_id");
                
                String updateSql = "UPDATE delivery_boys SET status='busy' WHERE delivery_id=?";
                try (PreparedStatement psUpdate = cn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, deliveryId);
                    psUpdate.executeUpdate();
                }
            }
        }
        return deliveryId;
    }

    /**
     * ✅ FINAL FIX: Join users table (u) with delivery_boys table (d) 
     * using u.user_id = d.delivery_id, assuming the primary keys are linked.
     */
    private String getDeliveryBoyName(int deliveryId) {
        String name = "ID " + deliveryId;
        // The join condition is now on delivery_id, assuming it stores the user's ID
        String sql = "SELECT u.fullname FROM users u JOIN delivery_boys d ON u.user_id = d.delivery_id WHERE d.delivery_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) name = rs.getString("fullname");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get delivery boy name: " + e.getMessage());
        }
        return name;
    }
    
    private void resetDeliveryBoyStatus(int deliveryId) {
        String sql = "UPDATE delivery_boys SET status='available' WHERE delivery_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to reset delivery boy status: " + e.getMessage());
        }
    }
    
    private void displayQRCode(String paymentMode) {
        if (!paymentMode.equals("Online (UPI/Card)")) return;

        JDialog qrDialog = new JDialog(this, "Scan & Pay", true); 
        qrDialog.setSize(400, 450);
        qrDialog.setLocationRelativeTo(this);
        qrDialog.setLayout(new BorderLayout());

        JLabel qrLabel = new JLabel(
            "<html><center>Scan this QR code to pay ₹" + String.format("%.2f", totalAmount) + 
            "<br>(Please ensure the amount is correct on your UPI app)</center></html>", 
            SwingConstants.CENTER
        );
        qrLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        qrDialog.add(qrLabel, BorderLayout.NORTH);

        Icon icon = null; 
        URL imageURL = getClass().getResource("/resources/qr_code.jpg"); 
        
        try { 
            if (imageURL == null) {
                throw new IllegalArgumentException("Resource not found. Check path: /resources/qr_code.jpg");
            }
            Image img = ImageIO.read(imageURL); 
            Image scaledImg = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImg);
        } catch (Exception e) {
            System.err.println("Error loading QR code image. Using placeholder. Error: " + e.getMessage());
            icon = UIManager.getIcon("OptionPane.errorIcon"); 
        }
        
        JLabel imageLabel = new JLabel(icon, SwingConstants.CENTER); 
        qrDialog.add(imageLabel, BorderLayout.CENTER);

        JButton btnDone = new JButton("I Have Paid / Continue");
        btnDone.addActionListener(e -> qrDialog.dispose());
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.add(btnDone);
        qrDialog.add(southPanel, BorderLayout.SOUTH);

        qrDialog.setVisible(true);
    }

    private void placeOrderDialog() {
        if (cartModel.getRowCount()==0) { JOptionPane.showMessageDialog(this,"Cart empty."); return; }
        int hotelRow = tblHotels.getSelectedRow();
        if (hotelRow < 0) { JOptionPane.showMessageDialog(this,"Select hotel first."); return; }
        int hotelId = (int) hotelsModel.getValueAt(hotelRow,0);

        JTextField addr = new JTextField();
        JTextField specs = new JTextField();
        JComboBox<String> cbPayment = new JComboBox<>(new String[]{"Cash on Delivery","Online (UPI/Card)"});
        Object[] msg = {"Delivery Address:", addr, "Any specifications (spice/packing):", specs, "Payment Mode:", cbPayment};
        int ok = JOptionPane.showConfirmDialog(this, msg, "Place Order", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String address = addr.getText().trim();
        String specifications = specs.getText().trim();
        String paymentMode = (String) cbPayment.getSelectedItem();
        if (address.isEmpty()) { JOptionPane.showMessageDialog(this,"Address required."); return; }

        if (paymentMode.equals("Online (UPI/Card)")) {
            displayQRCode(paymentMode);
        }

        int deliveryId = -1;
        Connection cn = null;
        
        try {
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);
            
            deliveryId = assignDeliveryBoy(cn);
            if (deliveryId == -1) {
                JOptionPane.showMessageDialog(this, "No delivery personnel available. Try again.", "Order Failed", JOptionPane.WARNING_MESSAGE);
                cn.rollback();
                return;
            }
            
            String deliveryStatus = "Assigned to Delivery";
            String paymentStatus = paymentMode.equals("Online (UPI/Card)") ? "Confirmed" : "Pending";

            String insOrder = "INSERT INTO orders (user_id, hotel_id, total_amount, address, specifications, payment_mode, payment_status, delivery_status, eta, delivery_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = cn.prepareStatement(insOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId); ps.setInt(2, hotelId); ps.setDouble(3, totalAmount);
                ps.setString(4, address); ps.setString(5, specifications); ps.setString(6, paymentMode);
                ps.setString(7, paymentStatus); ps.setString(8, deliveryStatus); ps.setInt(9, 30); ps.setInt(10, deliveryId);
                ps.executeUpdate();

                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (!gk.next()) throw new SQLException("Failed to retrieve Order ID.");
                    int orderId = gk.getInt(1);

                    String insItem = "INSERT INTO order_items (order_id, item_id, quantity, price) VALUES (?,?,?,?)";
                    try (PreparedStatement ps2 = cn.prepareStatement(insItem)) {
                        for (int i=0;i<cartModel.getRowCount();i++){
                            ps2.setInt(1, orderId);
                            ps2.setInt(2, (int) cartModel.getValueAt(i,0));
                            ps2.setInt(3, (int) cartModel.getValueAt(i,2));
                            ps2.setDouble(4, ((Number)cartModel.getValueAt(i,3)).doubleValue());
                            ps2.addBatch();
                        }
                        ps2.executeBatch();
                    }
                    
                    cn.commit();
                    
                    String deliveryName = getDeliveryBoyName(deliveryId);
                    JOptionPane.showMessageDialog(this, "Order placed successfully! ID: " + orderId + "\nDelivery Person: " + deliveryName + "\nPayment Status: " + paymentStatus, "Order Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    cartModel.setRowCount(0); totalAmount = 0.0; updateTotalLabel();
                }
            }
        } catch (SQLException ex) {
            showError("Order failed: " + ex.getMessage());
            if (cn != null) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) { /* Ignore */ }
            }
        } finally {
            if (cn != null) { try { cn.close(); } catch (SQLException closeEx) { /* Ignore */ } }
        }
    }
}