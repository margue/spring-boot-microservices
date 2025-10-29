package hotel.invoice;

import jakarta.persistence.Embeddable;

@Embeddable
public record LineItem(String description) {
}
