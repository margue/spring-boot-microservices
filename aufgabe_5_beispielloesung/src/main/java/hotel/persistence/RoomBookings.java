package hotel.persistence;

import jakarta.persistence.*;
import org.jmolecules.ddd.annotation.Identity;

import java.util.ArrayList;
import java.util.List;

@org.jmolecules.ddd.annotation.Entity
@Entity
public class RoomBookings {

    @Identity
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomNumber;

    @OneToMany(cascade = CascadeType.DETACH)
    private List<BookingInterval> bookingIntervals = new ArrayList<>();

    public RoomBookings(String roomNumber, List<BookingInterval> bookingIntervals) {
        this.roomNumber = roomNumber;
        this.bookingIntervals = bookingIntervals;
    }

    // Für JPA erforderlicher Default-Konstruktor
    public RoomBookings() {}

    public String getRoomNumber() {
        return roomNumber;
    }

    public List<BookingInterval> getBookingIntervals() {
        return bookingIntervals;
    }
}

