package hotel.service;

import hotel.hotel.*;
import hotel.invoice.InvoiceRepository;
import hotel.payment.PaymentRepository;
import hotel.payment.PaymentService;
import hotel.shared.ErrorResponse;
import hotel.hotel.HotelController.CheckInRequest;
import hotel.hotel.HotelController.CheckOutRequest;
import hotel.hotel.HotelController.RoomBookingRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HotelTest {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private HotelController hotelController;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private void setupRooms(int numberOfRooms) {
        for (int i = 1; i <= numberOfRooms; i++) {
            roomRepository.save(new Room(Integer.toString(i), new ArrayList<>()));
        }
    }

    private void setupRoomWithBookings(BookingInterval... bookingIntervals) {
        roomRepository.save(new Room("1", new ArrayList<>(Arrays.asList(bookingIntervals))));
    }

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

        paymentService.payAmount("Fritz", 200.0);
        paymentService.produceInvoice("Fritz", endDate, Collections.singletonList("1"));

        CheckOutRequest checkOutRequest = new CheckOutRequest("Fritz", "1", endDate);

        // WHEN
        ResponseEntity<?> response = hotelController.checkOut(checkOutRequest);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(roomRepository.findByRoomNumber("1").getBookings().getFirst().isCheckedOut()).isTrue();
        Assertions.assertThat(roomRepository.findByRoomNumber("1").getBookings().getFirst().getInvoiceId()).isNotNull();
    }
}
