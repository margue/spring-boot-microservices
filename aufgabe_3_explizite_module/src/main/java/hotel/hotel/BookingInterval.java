package hotel.hotel;

import hotel.invoice.Invoice;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Entity
public class BookingInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String customerName;

    private Long invoiceId;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "date", column = @Column(name = "check_in_date"))})
    private CheckIn checkIn = null;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "date", column = @Column(name = "check_out_date"))})
    private CheckOut checkOut = null;

    public BookingInterval(LocalDate startDate, LocalDate endDate) {
        this(startDate, endDate, null);
    }

    public BookingInterval(LocalDate startDate, LocalDate endDate, String customerName) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.customerName = customerName;
    }

    // Für JPA erforderlicher Default-Konstruktor
    protected BookingInterval() {
    }

    public boolean contains(LocalDate date) {
        return (date.equals(startDate) || date.isAfter(startDate)) && date.isBefore(endDate);
    }

    // method courtesy of Java 9 :)
    private Stream<LocalDate> datesFromTo(LocalDate startInclusive, LocalDate endExclusive) {
        long end = endExclusive.toEpochDay();
        long start = startInclusive.toEpochDay();
        if (end < start) {
            throw new IllegalArgumentException(endExclusive + " < " + this);
        }
        return LongStream.range(start, end).mapToObj(LocalDate::ofEpochDay);
    }


    public List<LocalDate> dates() {
        return datesFromTo(startDate, endDate).collect(Collectors.toList());
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public boolean isCheckedIn() {
        return this.checkIn != null;
    }

    public void setCheckedIn(LocalDate date) {
        this.checkIn = new CheckIn(date);
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setCheckOut(LocalDate date) {
        this.checkOut = new CheckOut(date);
    }

    public boolean isCheckedOut() {
        return this.checkOut != null;
    }
}
