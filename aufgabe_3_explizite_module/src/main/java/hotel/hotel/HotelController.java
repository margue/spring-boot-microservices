package hotel.hotel;

import hotel.invoice.Invoice;
import hotel.payment.PaymentController;
import hotel.shared.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotel")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/rooms/availability")
    public ResponseEntity<?> checkRoomAvailability(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        Double price = hotelService.requestRoom(startDate, endDate);

        if (price != null) {
            return ResponseEntity.ok(new RoomAvailabilityResponse(true, price));
        } else {
            return ResponseEntity.ok(new RoomAvailabilityResponse(false, null));
        }
    }

    @PostMapping("/rooms/booking")
    public ResponseEntity<?> bookRoom(
            @RequestBody RoomBookingRequest request) {

        try {
            hotelService.bookRoom(request.getStartDate(), request.getEndDate(), request.getCustomerName());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new BookingResponse("Zimmer erfolgreich gebucht", request.getStartDate(),
                            request.getEndDate(), request.getCustomerName())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(
            @RequestBody CheckInRequest request) {

        try {
            List<String> roomNumbers = hotelService.checkIn(request.getCustomerName(), request.getStartDate());
            return ResponseEntity.ok(new CheckInResponse("Check-in erfolgreich", roomNumbers));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(
            @RequestBody CheckOutRequest request) {

        try {
            hotelService.checkOut(request.getCustomerName(), request.getRoomNumber(), request.getEndDate());
            return ResponseEntity.ok(new CheckOutResponse("Check-out erfolgreich"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }


    @PostMapping("/invoice")
    public ResponseEntity<?> createInvoice(@RequestBody PaymentController.InvoiceRequest request) {
        try {
            Invoice invoice = hotelService.produceInvoice(
                    request.getCustomerName(),
                    request.getEndDate(),
                    request.getRoomNumbers()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PaymentController.InvoiceResponse(
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

    // DTO-Klassen

    public static class RoomAvailabilityResponse {
        private boolean available;
        private Double price;

        public RoomAvailabilityResponse(boolean available, Double price) {
            this.available = available;
            this.price = price;
        }

        public boolean isAvailable() {
            return available;
        }

        public Double getPrice() {
            return price;
        }
    }

    public static class RoomBookingRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String customerName;

        // Standardkonstruktor für Jackson
        public RoomBookingRequest() {}

        public RoomBookingRequest(LocalDate startDate, LocalDate endDate, String customerName) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.customerName = customerName;
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
    }

    public static class BookingResponse {
        private String message;
        private LocalDate startDate;
        private LocalDate endDate;
        private String customerName;

        public BookingResponse(String message, LocalDate startDate, LocalDate endDate, String customerName) {
            this.message = message;
            this.startDate = startDate;
            this.endDate = endDate;
            this.customerName = customerName;
        }

        public String getMessage() {
            return message;
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
    }

    public static class CheckInRequest {
        private String customerName;
        private LocalDate startDate;

        // Standardkonstruktor für Jackson
        public CheckInRequest() {}

        public CheckInRequest(String customerName, LocalDate startDate) {
            this.customerName = customerName;
            this.startDate = startDate;
        }

        public String getCustomerName() {
            return customerName;
        }

        public LocalDate getStartDate() {
            return startDate;
        }
    }

    public static class CheckInResponse {
        private String message;
        private List<String> roomNumbers;

        public CheckInResponse(String message, List<String> roomNumbers) {
            this.message = message;
            this.roomNumbers = roomNumbers;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getRoomNumbers() {
            return roomNumbers;
        }
    }

    public static class CheckOutRequest {
        private String customerName;
        private String roomNumber;
        private LocalDate endDate;

        // Standardkonstruktor für Jackson
        public CheckOutRequest() {}

        public CheckOutRequest(String customerName, String roomNumber, LocalDate endDate) {
            this.customerName = customerName;
            this.roomNumber = roomNumber;
            this.endDate = endDate;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }

    public static class CheckOutResponse {
        private String message;

        public CheckOutResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
