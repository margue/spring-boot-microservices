package hotel.persistence;

import jakarta.persistence.Embeddable;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
@Embeddable
public class CheckOut {

    private LocalDate date;

    public CheckOut(LocalDate date) {
        this.date = date;
    }

    public CheckOut() {}
}
