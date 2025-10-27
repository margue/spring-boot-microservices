package hotel.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Room {

    @Id
    private String roomNumber;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_number")
    private List<BookingInterval> bookings = new ArrayList<>();

    public Room(String roomNumber, List<BookingInterval> bookings) {
        this.roomNumber = roomNumber;
        this.bookings = bookings;
    }

    // Für JPA erforderlicher Default-Konstruktor
    protected Room() {
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public List<BookingInterval> getBookings() {
        return bookings;
    }

    private boolean dateIsFree(LocalDate date) {
        for (BookingInterval booking : bookings) {
            if (booking.contains(date)) {
                return false;
            }
        }
        return true;
    }

    public boolean roomIsFree(BookingInterval interval) {
        for (LocalDate date : interval.dates()) {
            if (!dateIsFree(date)) {
                return false;
            }
        }
        return true;
    }
}
