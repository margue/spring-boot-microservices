package hotel.controller;

import hotel.persistence.Invoice;
import hotel.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(@RequestBody PaymentRequest request) {
        try {
            paymentService.payAmount(request.getCustomerName(), request.getAmount());
            double remainingCredit = paymentService.remainingCredit(request.getCustomerName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PaymentResponse("Zahlung erfolgreich durchgeführt",
                            request.getAmount(), remainingCredit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PaymentResponse("Fehler bei der Zahlung: " + e.getMessage(), null, null));
        }
    }

    @GetMapping("/credit/{customerName}")
    public ResponseEntity<CreditResponse> getRemainingCredit(@PathVariable String customerName) {
        try {
            double credit = paymentService.remainingCredit(customerName);
            return ResponseEntity.ok(new CreditResponse(customerName, credit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CreditResponse(customerName, null, "Fehler bei der Abfrage: " + e.getMessage()));
        }
    }

    @PostMapping("/invoice")
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceRequest request) {
        try {
            Invoice invoice = paymentService.produceInvoice(
                    request.getCustomerName(),
                    request.getEndDate(),
                    request.getRoomNumbers()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new InvoiceResponse(
                            "Rechnung erfolgreich erstellt",
                            invoice.getCustomerName(),
                            invoice.getTotalAmount()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage()));
        }
    }

    // DTOs

    public static class PaymentRequest {
        private String customerName;
        private double amount;

        // Standardkonstruktor für Jackson
        public PaymentRequest() {}

        public PaymentRequest(String customerName, double amount) {
            this.customerName = customerName;
            this.amount = amount;
        }

        public String getCustomerName() {
            return customerName;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static class PaymentResponse {
        private String message;
        private Double paidAmount;
        private Double remainingCredit;

        public PaymentResponse(String message, Double paidAmount, Double remainingCredit) {
            this.message = message;
            this.paidAmount = paidAmount;
            this.remainingCredit = remainingCredit;
        }

        public String getMessage() {
            return message;
        }

        public Double getPaidAmount() {
            return paidAmount;
        }

        public Double getRemainingCredit() {
            return remainingCredit;
        }
    }

    public static class CreditResponse {
        private String customerName;
        private Double credit;
        private String errorMessage;

        public CreditResponse(String customerName, Double credit) {
            this.customerName = customerName;
            this.credit = credit;
            this.errorMessage = null;
        }

        public CreditResponse(String customerName, Double credit, String errorMessage) {
            this.customerName = customerName;
            this.credit = credit;
            this.errorMessage = errorMessage;
        }

        public String getCustomerName() {
            return customerName;
        }

        public Double getCredit() {
            return credit;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class InvoiceRequest {
        private String customerName;
        private LocalDate endDate;
        private List<String> roomNumbers;

        // Standardkonstruktor für Jackson
        public InvoiceRequest() {}

        public InvoiceRequest(String customerName, LocalDate endDate, List<String> roomNumbers) {
            this.customerName = customerName;
            this.endDate = endDate;
            this.roomNumbers = roomNumbers;
        }

        public String getCustomerName() {
            return customerName;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public List<String> getRoomNumbers() {
            return roomNumbers;
        }
    }

    public static class InvoiceResponse {
        private String message;
        private String customerName;
        private double totalAmount;

        public InvoiceResponse(String message, String customerName, double totalAmount) {
            this.message = message;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
        }

        public String getMessage() {
            return message;
        }

        public String getCustomerName() {
            return customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }
}