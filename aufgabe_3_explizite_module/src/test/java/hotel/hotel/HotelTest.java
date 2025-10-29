package hotel.hotel;

import hotel.hotel.HotelController.CheckInRequest;
import hotel.hotel.HotelController.CheckOutRequest;
import hotel.hotel.HotelController.RoomBookingRequest;
import hotel.invoice.InvoiceRepository;
import hotel.payment.PaymentController;
import hotel.payment.PaymentRepository;
import hotel.shared.ErrorResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class HotelTest {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private HotelController hotelController;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentController paymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    public void clear(){
        roomRepository.deleteAll();
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private void setupRooms(int numberOfRooms) {
        for (int i = 1; i <= numberOfRooms; i++) {
            roomRepository.save(new Room(Integer.toString(i), new ArrayList<>()));
        }
    }

    private void setupRoomWithBookings(BookingInterval... bookingIntervals) {
        roomRepository.save(new Room("1", new ArrayList<>(Arrays.asList(bookingIntervals))));
    }

    String customer1 = "Peter Meier";

    @Test
    void requestRoom_roomAvailable() {
        // GIVEN
        setupRooms(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);

        // WHEN
        ResponseEntity<?> response = hotelController.checkRoomAvailability(startDate, endDate);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.RoomAvailabilityResponse.class);
        HotelController.RoomAvailabilityResponse availabilityResponse = 
            (HotelController.RoomAvailabilityResponse) response.getBody();
        assertThat(availabilityResponse.isAvailable()).isTrue();
        assertThat(availabilityResponse.getPrice()).isEqualTo(100.0);
    }

    @Test
    void requestRoom_roomAvailableForMultipleNights() {
        // GIVEN
        setupRooms(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);

        // WHEN
        ResponseEntity<?> response = hotelController.checkRoomAvailability(startDate, endDate);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.RoomAvailabilityResponse.class);
        HotelController.RoomAvailabilityResponse availabilityResponse = 
            (HotelController.RoomAvailabilityResponse) response.getBody();
        assertThat(availabilityResponse.isAvailable()).isTrue();
        assertThat(availabilityResponse.getPrice()).isEqualTo(200.0);
    }

    @Test
    void requestRoom_roomNotAvailable() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate));

        // WHEN
        ResponseEntity<?> response = hotelController.checkRoomAvailability(startDate, endDate);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.RoomAvailabilityResponse.class);
        HotelController.RoomAvailabilityResponse availabilityResponse = 
            (HotelController.RoomAvailabilityResponse) response.getBody();
        assertThat(availabilityResponse.isAvailable()).isFalse();
        assertThat(availabilityResponse.getPrice()).isNull();
    }

    @Test
    void requestRoom_roomAvailableAlthoughBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        setupRoomWithBookings(new BookingInterval(startDate.plusDays(5), endDate.plusDays(7)));

        // WHEN
        ResponseEntity<?> response = hotelController.checkRoomAvailability(startDate, endDate);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.RoomAvailabilityResponse.class);
        HotelController.RoomAvailabilityResponse availabilityResponse = 
            (HotelController.RoomAvailabilityResponse) response.getBody();
        assertThat(availabilityResponse.isAvailable()).isTrue();
        assertThat(availabilityResponse.getPrice()).isEqualTo(100.0);
    }

    @Test
    void bookRoom_bookingRequiresCustomerName() {
        // GIVEN
        setupRooms(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        RoomBookingRequest request = new RoomBookingRequest(startDate, endDate, null);

        // WHEN
        ResponseEntity<?> response = hotelController.bookRoom(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void bookRoom_roomAvailable() {
        // GIVEN
        setupRoomWithBookings();
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        RoomBookingRequest request = new RoomBookingRequest(startDate, endDate, "Peter");

        // WHEN
        ResponseEntity<?> response = hotelController.bookRoom(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<BookingInterval> foundIntervals = roomRepository.findAllBookingIntervalsByCustomerName("Peter");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_bookTwoRoomsForSameNights() {
        // GIVEN
        setupRooms(2);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        RoomBookingRequest request1 = new RoomBookingRequest(startDate, endDate, "Peter");
        RoomBookingRequest request2 = new RoomBookingRequest(startDate, endDate, "Peter");

        // WHEN
        hotelController.bookRoom(request1);
        hotelController.bookRoom(request2);

        // THEN
        List<Room> foundRooms = roomRepository.findAllRoomsWithBookingIntervalsByCustomerName("Peter");
        assertThat(foundRooms).hasSize(2);
        assertThat(foundRooms).extracting(Room::getRoomNumber)
                        .containsExactly("1", "2");
        assertThat(foundRooms.get(0).getBookings().get(0).getStartDate()).isEqualTo(startDate);
        assertThat(foundRooms.get(0).getBookings().get(0).getEndDate()).isEqualTo(endDate);
        assertThat(foundRooms.get(1).getBookings().get(0).getStartDate()).isEqualTo(startDate);
        assertThat(foundRooms.get(1).getBookings().get(0).getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_roomAvailableForMultipleNights() {
        // GIVEN
        setupRoomWithBookings();
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomBookingRequest request = new RoomBookingRequest(startDate, endDate, "Fred");

        // WHEN
        ResponseEntity<?> response = hotelController.bookRoom(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<BookingInterval> foundIntervals = roomRepository.findAllBookingIntervalsByCustomerName("Fred");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_roomNotAvailable() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate));
        RoomBookingRequest request = new RoomBookingRequest(startDate, endDate, "Jack");

        // WHEN
        ResponseEntity<?> response = hotelController.bookRoom(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        List<BookingInterval> foundIntervals = roomRepository.findAllBookingIntervalsByCustomerName("Jack");
        assertThat(foundIntervals).hasSize(0);
    }

    @Test
    void bookRoom_roomAvailableAlthoughBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        setupRoomWithBookings(new BookingInterval(startDate.plusDays(5), endDate.plusDays(7)));
        RoomBookingRequest request = new RoomBookingRequest(startDate, endDate, "Jim");

        // WHEN
        ResponseEntity<?> response = hotelController.bookRoom(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<BookingInterval> foundIntervals = roomRepository.findAllBookingIntervalsByCustomerName("Jim");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void checkIn_roomWasBooked() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate, "Fritz"));
        CheckInRequest request = new CheckInRequest("Fritz", startDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkIn(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.CheckInResponse.class);
        HotelController.CheckInResponse checkInResponse = (HotelController.CheckInResponse) response.getBody();
        assertThat(checkInResponse.getRoomNumbers().size()).isEqualTo(1);
        assertThat(checkInResponse.getRoomNumbers().getFirst()).isEqualTo("1");
    }

    @Test
    void checkIn_roomWasNotBooked() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        setupRoomWithBookings();
        CheckInRequest request = new CheckInRequest("Fritz", startDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkIn(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        List<BookingInterval> foundIntervals = roomRepository.findAllBookingIntervalsByCustomerName("Fritz");
        assertThat(foundIntervals).hasSize(0);
    }

    @Test
    void checkIn_roomWasBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate, "Fritz"));
        LocalDate checkInDate = startDate.plusDays(17);
        CheckInRequest request = new CheckInRequest("Fritz", checkInDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkIn(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(HotelController.CheckInResponse.class);
        HotelController.CheckInResponse checkInResponse = (HotelController.CheckInResponse) response.getBody();
        assertThat(checkInResponse.getRoomNumbers().size()).isEqualTo(0);
    }

    @Test
    void checkOut_roomWasBooked_error() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate, "Fritz"));
        CheckOutRequest request = new CheckOutRequest("Fritz", "1", endDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkOut(request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkOut_roomWasCheckedIn_error() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate, "Fritz"));
        
        CheckInRequest checkInRequest = new CheckInRequest("Fritz", startDate);
        hotelController.checkIn(checkInRequest);
        
        CheckOutRequest checkOutRequest = new CheckOutRequest("Fritz", "1", endDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkOut(checkOutRequest);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkOut_roomWasInvoiced() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        setupRoomWithBookings(new BookingInterval(startDate, endDate, "Fritz"));
        
        CheckInRequest checkInRequest = new CheckInRequest("Fritz", startDate);
        hotelController.checkIn(checkInRequest);

        paymentController.makePayment(new PaymentController.PaymentRequest("Fritz", 200.0));
        hotelService.produceInvoice("Fritz", endDate, Collections.singletonList("1"));

        CheckOutRequest checkOutRequest = new CheckOutRequest("Fritz", "1", endDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkOut(checkOutRequest);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(roomRepository.findByRoomNumber("1").getBookings().getFirst().isCheckedOut()).isTrue();
        Assertions.assertThat(roomRepository.findByRoomNumber("1").getBookings().getFirst().getInvoiceId()).isNotNull();
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
        PaymentController.InvoiceRequest request = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = hotelController.createInvoice(request);

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

        PaymentController.PaymentRequest paymentRequest = new PaymentController.PaymentRequest(customer1, 50.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = hotelController.createInvoice(invoiceRequest);

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

        PaymentController.PaymentRequest paymentRequest = new PaymentController.PaymentRequest(customer1, 100.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = hotelController.createInvoice(invoiceRequest);

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

        PaymentController.PaymentRequest paymentRequest = new PaymentController.PaymentRequest(customer1, 500.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = hotelController.createInvoice(invoiceRequest);

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

        PaymentController.PaymentRequest paymentRequest = new PaymentController.PaymentRequest(customer1, 200.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        ResponseEntity<?> response = hotelController.createInvoice(invoiceRequest);

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

        PaymentController.PaymentRequest paymentRequest = new PaymentController.PaymentRequest(customer1, 100.0);
        paymentController.makePayment(paymentRequest);

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse =
                (ResponseEntity<PaymentController.CreditResponse>) paymentController.getRemainingCredit(customer1);
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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 30.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse =
                (ResponseEntity<PaymentController.CreditResponse>) paymentController.getRemainingCredit(customer1);
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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 170.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse =
                (ResponseEntity<PaymentController.CreditResponse>) paymentController.getRemainingCredit(customer1);
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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 100.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // THEN
        ResponseEntity<PaymentController.CreditResponse> creditResponse =
                (ResponseEntity<PaymentController.CreditResponse>) paymentController.getRemainingCredit(customer1);
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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 70.0));
        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 100.0));

        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // WHEN
        ResponseEntity<?> response = hotelController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        Assertions.assertThat(((ErrorResponse)response.getBody()).getError())
                .contains(String.format("No bookingIntervals to be invoiced for given customer '%s', endDate [%s] " +
                        "and roomNumbers %s", customer1, endDate, roomNumbers));

        ResponseEntity<PaymentController.CreditResponse> creditResponse =
                (ResponseEntity<PaymentController.CreditResponse>) paymentController.getRemainingCredit(customer1);
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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 100.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 200.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

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

        paymentController.makePayment(new PaymentController.PaymentRequest(customer1, 100.0));

        // WHEN
        PaymentController.InvoiceRequest invoiceRequest = new PaymentController.InvoiceRequest(customer1, endDate, roomNumbers);
        hotelController.createInvoice(invoiceRequest);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1).size()).isEqualTo(2);
        List<BookingInterval> bookingIntervals = roomRepository.findAllBookingIntervalsByCustomerName(customer1);
        Assertions.assertThat(bookingIntervals.get(0).getInvoiceId()).isNotNull();
        Assertions.assertThat(bookingIntervals.get(1).getInvoiceId()).isNull();
    }
}
