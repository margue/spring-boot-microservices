package hotel.payment;

import hotel.invoice.InvoiceServiceApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class PaymentService implements PaymentServiceApi {

    private final PaymentRepository paymentRepository;
    private final InvoiceServiceApi invoiceService;

    public PaymentService(PaymentRepository paymentRepository, InvoiceServiceApi invoiceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
    }

    public void payAmount(String customerName, Double amount) {
        Payment payment = new Payment(customerName, amount);
        paymentRepository.save(payment);
    }

    public double remainingCredit(String customerName) {
        return paymentRepository.findByCustomerName(customerName).stream().mapToDouble(payment -> payment.getPaidAmount() - payment.getUsedAmount()).sum();
    }

    @Override
    public Long produceInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {

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

        Long invoiceId = invoiceService.createInvoice(customerName, totalAmount, lineItemsPerRoom);

        return invoiceId;
    }

    @Override
    public void deleteAll() {
        paymentRepository.deleteAll();
    }

}
