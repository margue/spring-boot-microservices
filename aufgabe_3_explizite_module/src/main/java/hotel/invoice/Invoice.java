package hotel.invoice;

import hotel.hotel.BookingInterval;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "invoices")
public class Invoice {

    public Long getId() {
        return id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "invoice_id")
    @MapKey(name = "roomNumber")
    private Map<String, RoomBookings> roomBookings = new HashMap<>();

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    // Für JPA erforderlicher Default-Konstruktor
    protected Invoice() {
    }

    public Invoice(String customerName, Map<String, List<LineItem>> lineItemsPerRoom, double totalAmount) {
        this.customerName = customerName;
        this.roomBookings = toRoomBookings(lineItemsPerRoom);
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public static Map<String, RoomBookings> toRoomBookings(Map<String, List<LineItem>> lineItemsPerRoom) {
        if (lineItemsPerRoom == null) {
            return Collections.emptyMap();
        }

        return lineItemsPerRoom.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            RoomBookings rb = new RoomBookings(e.getKey(), new ArrayList<>(e.getValue()));
                            return rb;
                        }
                ));
    }
}
