package hotel.payment;

import hotel.hotel.BookingInterval;
import hotel.hotel.Room;
import hotel.hotel.RoomRepository;
import hotel.invoice.Invoice;
import hotel.invoice.InvoiceRepository;
import hotel.invoice.LineItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import static hotel.shared.PriceCalculator.calcPrice;

@Service

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public void payAmount(String customerName, Double amount) {
        Payment payment = new Payment(customerName, amount);
        paymentRepository.save(payment);
    }

    public double remainingCredit(String customerName) {
        return paymentRepository.findByCustomerName(customerName).stream().mapToDouble(payment -> payment.getPaidAmount() - payment.getUsedAmount()).sum();
    }

    public Invoice produceInvoice(String customerName, double totalAmount, Map<String, List<LineItem>> lineItemsPerRoom) {

        double credit = remainingCredit(customerName);
        if (totalAmount > credit) {
            throw new IllegalStateException("Payment insufficient. Necessary payment: " + (totalAmount - credit));
        }

        List<Payment> payments = paymentRepository.findByCustomerName(customerName);
        payments.sort((o1, o2) -> o1.getPaymentDate().isEqual(o2.getPaymentDate()) ? 0 : o1.getPaymentDate().isBefore(o2.getPaymentDate()) ? -1 : 1);
        double remainingTotalAmount = totalAmount;
        for (Payment payment : payments) {
            if (remainingTotalAmount > 0.0) {
                double remainingCreditForPayment = payment.getPaidAmount() - payment.getUsedAmount();
                if (remainingCreditForPayment >= remainingTotalAmount) {
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

        Invoice invoice = new Invoice(customerName, lineItemsPerRoom, totalAmount);
        invoice = invoiceRepository.save(invoice);

        return invoice;
    }

}
