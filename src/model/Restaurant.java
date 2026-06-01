package model;

// Used primarily for displaying hotel names correctly in JComboBoxes.
public class Restaurant {
    private int id;
    private String name;
    private String category;
    private String location;
    private String phone;

    public Restaurant(int id, String name, String category, String location, String phone) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }
    
    // This is crucial: it determines what is displayed in the JComboBox
    @Override
    public String toString() {
        return name + " (" + location + ")";
    }
}