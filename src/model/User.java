package model;

public class User {
    private int id;
    private String username, fullname, role, phone;
    
    public User(int id, String username, String fullname, String role, String phone) {
        this.id = id; 
        this.username = username; 
        this.fullname = fullname; 
        this.role = role; 
        this.phone = phone;
    }
    
    public int getId(){return id;}
    public String getUsername(){return username;}
    public String getFullname(){return fullname;}
    public String getRole(){return role;}
    public String getPhone(){return phone;}
}