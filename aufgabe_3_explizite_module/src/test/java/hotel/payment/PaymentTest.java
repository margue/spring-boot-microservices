package hotel.payment;

import hotel.invoice.InvoiceRepository;
import hotel.payment.PaymentController.PaymentRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class PaymentTest {
    String customer1 = "Peter Meier";
    String customer2 = "Lisa Müller";

    @Autowired
    private PaymentController paymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    void clearRepositories() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    public void payAmount_customerPaidForTheFirstTime() {
        // GIVEN
        PaymentRequest request = new PaymentRequest(customer1, 42.0);

        // WHEN
        paymentController.makePayment(request);

        // THEN
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1)).hasSize(1);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
    }

    @Test
    public void payAmount_customerPaidForTheSecondTime() {
        // GIVEN
        PaymentRequest request1 = new PaymentRequest(customer1, 42.0);
        paymentController.makePayment(request1);

        // WHEN
        PaymentRequest request2 = new PaymentRequest(customer1, 120.0);
        paymentController.makePayment(request2);

        // THEN
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1)).hasSize(2);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1).get(1).getPaidAmount()).isEqualTo(120.0);
    }

    @Test
    public void payAmount_secondCustomerPaidForTheFirstTime() {
        // GIVEN
        PaymentRequest request1 = new PaymentRequest(customer1, 42.0);
        paymentController.makePayment(request1);

        // WHEN
        PaymentRequest request2 = new PaymentRequest(customer2, 120.0);
        paymentController.makePayment(request2);

        // THEN
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1)).hasSize(1);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer2)).hasSize(1);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
        Assertions.assertThat(paymentRepository.findByCustomerName(customer2).getFirst().getPaidAmount()).isEqualTo(120.0);
    }
}
