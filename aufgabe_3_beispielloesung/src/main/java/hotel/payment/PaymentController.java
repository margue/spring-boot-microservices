package hotel.payment;

import hotel.shared.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> makePayment(@RequestBody PaymentRequest request) {
        try {
            paymentService.payAmount(request.getCustomerName(), request.getAmount());
            double remainingCredit = paymentService.remainingCredit(request.getCustomerName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PaymentResponse("Zahlung erfolgreich durchgeführt",
                            request.getAmount(), remainingCredit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Fehler bei der Zahlung: " + e.getMessage()));
        }
    }

    @GetMapping("/credit/{customerName}")
    public ResponseEntity<?> getRemainingCredit(@PathVariable String customerName) {
        try {
            double credit = paymentService.remainingCredit(customerName);
            return ResponseEntity.ok(new CreditResponse(customerName, credit));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Fehler bei der Abfrage: " + e.getMessage()));
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
}