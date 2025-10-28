package hotel.service;

import hotel.hotel.BookingInterval;
import hotel.hotel.HotelService;
import hotel.hotel.Room;
import hotel.hotel.RoomRepository;
import hotel.invoice.Invoice;
import hotel.invoice.InvoiceRepository;
import hotel.payment.PaymentRepository;
import hotel.shared.ErrorResponse;
import hotel.payment.PaymentController;
import hotel.payment.PaymentController.InvoiceRequest;
import hotel.payment.PaymentController.PaymentRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class PaymentTest {
    String customer1 = "Peter Meier";
    String customer2 = "Lisa Müller";

    @Autowired
    private PaymentController paymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    void clearRepositories() {
        List<Room> allRooms = roomRepository.findAll();
        allRooms.forEach(room ->
                room.getBookings().forEach(bookingInterval ->
                        bookingInterval.setInvoiceId(null != null ? ((Invoice) null).getId() : null)));
        roomRepository.saveAll(allRooms);
        invoiceRepository.deleteAll();
        roomRepository.deleteAll();
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

    @Test
    public void produceInvoice_noPayment() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        // WHEN
        InvoiceRequest request = new InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = paymentController.createInvoice(request);

        // THEN
        Assertions.assertThat(response.getStatusCode()).isInstanceOf(HttpStatus.BAD_REQUEST.getClass());
        Assertions.assertThat((response.getBody())).isInstanceOf(ErrorResponse.class);
        Assertions.assertThat(((ErrorResponse)response.getBody()).getError()).contains("100.0");
    }

    @Test
    public void produceInvoice_paymentInsufficient() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentRequest paymentRequest = new PaymentRequest(customer1, 50.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = paymentController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(response.getStatusCode()).isInstanceOf(HttpStatus.class);
        Assertions.assertThat(((ErrorResponse)response.getBody()).getError()).contains("50.0");
    }

    @Test
    public void produceInvoice_oneRoomOneNight_withOldBooking() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        roomRepository.save(new Room("2", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentRequest paymentRequest = new PaymentRequest(customer1, 100.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = paymentController.createInvoice(invoiceRequest);
        
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isInstanceOf(PaymentController.InvoiceResponse.class);
        PaymentController.InvoiceResponse invoiceResponse = (PaymentController.InvoiceResponse) response.getBody();
        Assertions.assertThat(invoiceResponse.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoiceResponse.getTotalAmount()).isEqualTo(100.0);
    }

    @Test
    public void produceInvoice_manyBookingsDifferentStartDaysSameEndDay() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        roomRepository.save(new Room("2", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");
        roomNumbers.add("2");

        hotelService.bookRoom(startDate.minusDays(3), endDate, customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate.minusDays(3));
        hotelService.checkIn(customer1, startDate);

        PaymentRequest paymentRequest = new PaymentRequest(customer1, 500.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = paymentController.createInvoice(invoiceRequest);
        
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isInstanceOf(PaymentController.InvoiceResponse.class);
        PaymentController.InvoiceResponse invoiceResponse = (PaymentController.InvoiceResponse) response.getBody();
        Assertions.assertThat(invoiceResponse.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoiceResponse.getTotalAmount()).isEqualTo(500.0);
    }
    
    @Test
    public void produceInvoice_manyBookingsEndingOnInvoiceDayOrEarlier() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate.minusDays(1), endDate.minusDays(1), customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate.minusDays(1));
        hotelService.checkIn(customer1, startDate);

        PaymentRequest paymentRequest = new PaymentRequest(customer1, 200.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = paymentController.createInvoice(invoiceRequest);
        
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isInstanceOf(PaymentController.InvoiceResponse.class);
        PaymentController.InvoiceResponse invoiceResponse = (PaymentController.InvoiceResponse) response.getBody();
        Assertions.assertThat(invoiceResponse.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoiceResponse.getTotalAmount()).isEqualTo(200.0);
    }

    @Test
    public void produceInvoice_onePaymentIsMarkedAsUsed() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentRequest paymentRequest = new PaymentRequest(customer1, 100.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse = 
            paymentController.getRemainingCredit(customer1);
        Assertions.assertThat(creditResponse.getBody().getCredit()).isEqualTo(0.0);
    }

    @Test
    public void produceInvoice_twoPaymentsAreMarkedAsUsed() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        paymentController.makePayment(new PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentRequest(customer1, 30.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse = 
            paymentController.getRemainingCredit(customer1);
        Assertions.assertThat(creditResponse.getBody().getCredit()).isEqualTo(0.0);
    }

    @Test
    public void produceInvoice_onePaymentIsDeducted() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        paymentController.makePayment(new PaymentRequest(customer1, 170.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse = 
            paymentController.getRemainingCredit(customer1);
        Assertions.assertThat(creditResponse.getBody().getCredit()).isEqualTo(70.0);
    }

    @Test
    public void produceInvoice_twoPaymentsArePartiallyDeducted() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        paymentController.makePayment(new PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentRequest(customer1, 100.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse = 
            paymentController.getRemainingCredit(customer1);
        Assertions.assertThat(creditResponse.getBody().getCredit()).isEqualTo(70.0);
    }

    @Test
    public void produceInvoice_sameInvoiceTwiceLeadsToExcetionAlreadyPaid() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        paymentController.makePayment(new PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentRequest(customer1, 100.0));
        
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // WHEN
        ResponseEntity<?> response = paymentController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        Assertions.assertThat(((ErrorResponse)response.getBody()).getError())
                .contains(String.format("No bookingIntervals to be invoiced for given customer '%s', endDate [%s] " +
                        "and roomNumbers %s", customer1, endDate, roomNumbers));
                
        ResponseEntity<PaymentController.CreditResponse> creditResponse = 
            paymentController.getRemainingCredit(customer1);
        Assertions.assertThat(creditResponse.getBody().getCredit()).isEqualTo(70.0);
    }

    @Test
    public void markBookingsAsInvoiced_oneBooking() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        paymentController.makePayment(new PaymentRequest(customer1, 100.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1))
                .extracting(BookingInterval::getInvoiceId).doesNotContainNull();
    }

    @Transactional
    @Test
    public void markBookingsAsInvoiced_twoBookingsInPast() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate.minusDays(5), endDate.minusDays(5), customer1);
        hotelService.checkIn(customer1, startDate);
        hotelService.checkIn(customer1, startDate.minusDays(5));

        paymentController.makePayment(new PaymentRequest(customer1, 200.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(invoiceRepository.findAll()).isNotEmpty();
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1))
                .extracting(BookingInterval::getInvoiceId).doesNotContainNull();
    }

    @Test
    public void markBookingsAsInvoiced_twoBookingsOneInPast() {
        // GIVEN
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate.plusDays(5), endDate.plusDays(5), customer1);
        hotelService.checkIn(customer1, startDate);
        hotelService.checkIn(customer1, startDate.plusDays(5));

        paymentController.makePayment(new PaymentRequest(customer1, 100.0));

        // WHEN
        InvoiceRequest invoiceRequest = new InvoiceRequest(customer1, endDate, roomNumbers);
        paymentController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1).size()).isEqualTo(2);
        List<BookingInterval> bookingIntervals = roomRepository.findAllBookingIntervalsByCustomerName(customer1);
        Assertions.assertThat(bookingIntervals.get(0).getInvoiceId()).isNotNull();
        Assertions.assertThat(bookingIntervals.get(1).getInvoiceId()).isNull();
    }
}
