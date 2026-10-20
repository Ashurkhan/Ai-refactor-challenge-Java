package kg.example.legacy;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository repo;

    public OrderController(OrderRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Order add(@RequestBody Order x) {
        x.total = x.price * x.quantity;
        return repo.save(x);
    }

    @GetMapping
    public List<Order> all() {
        return repo.findAll();
    }

    // Legacy problem: loads every row into memory and searches in Java.
    // The search is case-sensitive and has no pagination.
    @GetMapping("/search")
    public List<Order> search(@RequestParam String q) {
        List<Order> a = repo.findAll();
        List<Order> b = new ArrayList<>();
        for (Order d : a) {
            if (d.customerName != null && d.customerName.contains(q)) {
                b.add(d);
            } else if (d.productName != null && d.productName.contains(q)) {
                b.add(d);
            }
        }
        return b;
    }

    @GetMapping("/{id}/total")
    public Map<String, Object> total(@PathVariable Long id) {
        Order x = repo.findById(id).get();
        double tmp = x.price * x.quantity;
        Map<String, Object> d = new HashMap<>();
        d.put("orderId", x.id);
        d.put("total", tmp);
        return d;
    }

    @GetMapping("/expensive")
    public List<Order> expensive(@RequestParam(defaultValue = "100") double price) {
        List<Order> a = repo.findAll();
        List<Order> b = new ArrayList<>();
        for (Order x : a) {
            if (x.price > price) {
                b.add(x);
            }
        }
        return b;
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @RequestBody Order x) {
        Order d = repo.findById(id).get();
        d.customerName = x.customerName;
        d.productName = x.productName;
        d.price = x.price;
        d.quantity = x.quantity;
        d.total = x.price * x.quantity;
        return repo.save(d);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        repo.deleteById(id);
        Map<String, String> x = new HashMap<>();
        x.put("message", "deleted");
        return x;
    }
}
