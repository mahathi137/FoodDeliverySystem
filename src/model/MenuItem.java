package model;

public class MenuItem {
    private int id, restaurantId; 
    private String name, category; 
    private double price;
    
    public MenuItem(int id, int restaurantId, String name, String category, double price){
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.category = category;
        this.price = price;
    }
    
    public int getId(){return id;}
    public int getRestaurantId(){return restaurantId;}
    public String getName(){return name;}
    public String getCategory(){return category;}
    public double getPrice(){return price;}
    public String toString(){ return name + " (" + category + ") - ₹" + price; }
}