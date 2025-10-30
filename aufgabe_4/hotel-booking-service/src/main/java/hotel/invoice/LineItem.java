package hotel.invoice;

import jakarta.persistence.Embeddable;

@Embeddable
record LineItem(String description) {
}
