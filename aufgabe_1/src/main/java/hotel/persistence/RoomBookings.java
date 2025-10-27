package hotel.persistence;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class RoomBookings {

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

