package hotel.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "paid_amount", nullable = false)
    private double paidAmount;
    
    @Column(name = "used_amount", nullable = false)
    private double usedAmount;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    // Für JPA erforderlicher Default-Konstruktor
    protected Payment() {
    }

    public Payment(String customerName, double paidAmount){
        this.customerName = customerName;
        this.paidAmount = paidAmount;
        this.paymentDate = LocalDate.now();
        this.usedAmount= 0.0;
    }

    public Long getId() {
        return id;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public double getUsedAmount() {
        return usedAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void reduceCreditBy(double amount) {
        usedAmount += amount;
    }
}