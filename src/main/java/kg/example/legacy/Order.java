package kg.example.legacy;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String customerName;
    public String productName;
    public double price;
    public int quantity;
    public double total;

    public Order() {}

    public Order(String customerName, String productName, double price, int quantity) {
        this.customerName = customerName;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.total = price * quantity;
    }
}
