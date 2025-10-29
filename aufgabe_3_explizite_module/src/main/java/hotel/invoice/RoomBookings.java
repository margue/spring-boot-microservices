package hotel.invoice;

import hotel.hotel.BookingInterval;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class RoomBookings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomNumber;

    @ElementCollection
    private List<LineItem> lineItems = new ArrayList<>();

    public RoomBookings(String roomNumber, List<LineItem> lineItems) {
        this.roomNumber = roomNumber;
        this.lineItems = lineItems;
    }

    // Für JPA erforderlicher Default-Konstruktor
    public RoomBookings() {}

    public String getRoomNumber() {
        return roomNumber;
    }

    public List<LineItem> getLineItems() {
        return lineItems;
    }
}

