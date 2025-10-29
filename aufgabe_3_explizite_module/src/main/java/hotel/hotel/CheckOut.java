package hotel.hotel;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class CheckOut {

    private LocalDate date;

    public CheckOut(LocalDate date) {
        this.date = date;
    }

    public CheckOut() {}
}
