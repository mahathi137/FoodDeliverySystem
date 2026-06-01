# 🍔 Food Delivery Management System

A desktop-based Food Delivery Management System developed using **Java, Java Swing, JDBC, and MySQL**. The application simulates the workflow of modern food delivery platforms by allowing customers to browse restaurants, place orders, track deliveries, and interact with a centralized database system.

## 🚀 Features

### 👤 Customer Module
- User Login & Authentication
- Browse Restaurants
- View Food Menus
- Add Items to Cart
- Place Orders
- Track Order Status
- Cancel Orders

### 🚚 Delivery Module
- View Assigned Orders
- Update Delivery Status
- Manage Availability

### 🛠️ Admin Module
- Monitor All Orders
- Manage Restaurants & Menus
- View Customer and Delivery Information

## 🗄️ Database Design

### Main Entities
- Users
- Hotels
- Menu Items
- Orders
- Order Items
- Delivery Boys

### DBMS Concepts Used
- Relational Database Design
- Primary Keys & Foreign Keys
- Referential Integrity
- Normalization
- CRUD Operations
- SQL Queries
- Transactions
- Authentication & Validation

## 💻 Technologies Used

| Technology | Purpose |
|------------|---------|
| Java | Application Logic |
| Java Swing | User Interface |
| JDBC | Database Connectivity |
| MySQL | Database Management |
| SQL | Data Manipulation |


## ⚙️ Setup Instructions

### Prerequisites
- Java JDK 8 or above
- MySQL Server
- Eclipse / IntelliJ IDEA
- MySQL Connector/J

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/FoodDeliverySystem.git
   ```

2. Create the database:
   ```sql
   CREATE DATABASE food_delivery;
   ```

3. Import the provided SQL script:
   ```sql
   food_delivery.sql
   ```

4. Add MySQL Connector/J to your project's build path.

5. Update database credentials in the connection file:

   ```java
   String url = "jdbc:mysql://localhost:3306/food_delivery";
   String username = "root";
   String password = "your_password";
   ```

6. Run the application:
   ```
   Main.java
   ```

## 🌟 Project Highlights

- Multi-user system (Admin, Customer, Delivery Personnel)
- Automatic delivery assignment
- Shopping cart functionality
- Complete order lifecycle management
- Secure login validation
- Real-time order tracking
- Persistent data storage using MySQL

## 👨‍💻 Author

**Mahathi Relangi**

Java • JDBC • MySQL • DBMS Project
