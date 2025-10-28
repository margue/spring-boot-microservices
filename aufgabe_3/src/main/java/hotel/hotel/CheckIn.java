package hotel.hotel;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class CheckIn {

    private LocalDate date;

    public CheckIn(LocalDate date) {
        this.date = date;
    }

    public CheckIn() {}
}
