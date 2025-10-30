package hotel.persistence;

import jakarta.persistence.Embeddable;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
@Embeddable
public class CheckIn {

    private LocalDate date;

    public CheckIn(LocalDate date) {
        this.date = date;
    }

    public CheckIn() {}
}
