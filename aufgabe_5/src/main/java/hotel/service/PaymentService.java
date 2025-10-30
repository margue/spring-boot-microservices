package hotel.service;

import hotel.controller.HotelController;
import hotel.persistence.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, RoomRepository roomRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public void payAmount(String customerName, Double amount) {
        Payment payment = new Payment(customerName, amount);
        paymentRepository.save(payment);
    }

    public double remainingCredit(String customerName) {
        return paymentRepository.findByCustomerName(customerName).stream()
                .mapToDouble(payment -> payment.getPaidAmount() - payment.getUsedAmount())
                .sum();
    }

    public Invoice produceInvoice(String customerName, LocalDate endDate, List<String> roomNumbers) {
        List<Room> bookedRooms = roomRepository.findAllRoomsWithBookingIntervalsByCustomerName(customerName)
                .stream().filter(r -> roomNumbers.contains(r.getRoomNumber())).collect(Collectors.toList());
        Map<String, List<BookingInterval>> bookingsForRooms = new HashMap<>();
        bookedRooms.forEach(room -> {
            List<BookingInterval> applicableBookings = room.getBookings().stream()
                    .filter(booking -> Objects.equals(booking.getCustomerName(), customerName))
                    .filter(booking -> !booking.getEndDate().isAfter(endDate))
                    .filter(booking -> booking.getInvoice() == null)
                    .filter(BookingInterval::isCheckedIn).collect(Collectors.toList());
            if(applicableBookings.size() > 0 ){
                bookingsForRooms.put(room.getRoomNumber(), applicableBookings);
            } else {
                throw new IllegalArgumentException(String.format("No bookingIntervals to be invoiced for given customer " +
                        "'%s', endDate [%s] and roomNumbers %s", customerName, endDate, roomNumbers));
            }
        });
        double totalAmount =
                bookingsForRooms.values().stream()
                        .mapToDouble(bookingsForRoom -> bookingsForRoom.stream()
                                .mapToDouble(HotelController::calcPrice)
                                .sum())
                        .sum();
        double credit = remainingCredit(customerName);
        if(totalAmount > credit){
            throw new IllegalStateException("Payment insufficient. Necessary payment: " + (totalAmount - credit));
        }

        List<Payment> payments = paymentRepository.findByCustomerName(customerName);
        payments.sort((o1, o2) -> o1.getPaymentDate().isEqual(o2.getPaymentDate()) ? 0 :
                        o1.getPaymentDate().isBefore(o2.getPaymentDate()) ? -1 : 1);
        double remainingTotalAmount = totalAmount;
        for (Payment payment: payments){
            if(remainingTotalAmount > 0.0){
                double remainingCreditForPayment = payment.getPaidAmount() - payment.getUsedAmount();
                if(remainingCreditForPayment >= remainingTotalAmount){
                    payment.reduceCreditBy(remainingTotalAmount);
                    remainingTotalAmount = 0.0;
                    break;
                } else {
                    payment.reduceCreditBy(remainingCreditForPayment);
                    remainingTotalAmount -= remainingCreditForPayment;
                }
            } else {
                break;
            }
        }
        paymentRepository.saveAll(payments);

        Invoice invoice = new Invoice(customerName, bookingsForRooms, totalAmount);
        invoice = invoiceRepository.save(invoice);

        roomRepository.markBookingsAsInvoiced(bookingsForRooms, invoice);

        return invoice;
    }

}
