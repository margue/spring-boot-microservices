package hotel.hotel;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static hotel.hotel.HotelController.calcPrice;

@Service
public class HotelService {

    private final RoomRepository roomRepository;

    public HotelService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Welcome to Hilberts Hotel!
     *
     * @return price as double or null in case of no availability
     */
    public Double requestRoom(LocalDate startDate, LocalDate endDate) {
        for (Room room : roomRepository.findAll()) {
            BookingInterval bookingInterval = new BookingInterval(startDate, endDate);
            if (room.roomIsFree(bookingInterval)) {
                return calcPrice(bookingInterval);
            }
        }
        return null;
    }

    public void bookRoom(LocalDate startDate, LocalDate endDate, String customerName) {
        if (customerName == null) {
            throw new IllegalArgumentException("Customer name must not be null");
        }
        for (Room room : roomRepository.findAll()) {
            BookingInterval bookingInterval = new BookingInterval(startDate, endDate, customerName);
            if (room.roomIsFree(bookingInterval)) {
                room.getBookings().add(bookingInterval); // no validation (race condition?)
                roomRepository.save(room); // not needed here, but generally required for persistence
                return;
            }
        }
        throw new IllegalStateException("No rooms available on the given date(s)");
    }

    public List<String> checkIn(String customerName, LocalDate startDate) {
        List<Room> roomsForCustomer = roomRepository.findAllRoomsWithBookingIntervalsByCustomerName(customerName);
        if (roomsForCustomer.size() == 0) {
            throw new IllegalStateException("Customer cannot check in because they did not book a room");
        }
        List<String> bookedRoomNumbers = new ArrayList<>();
        roomsForCustomer.forEach(room -> {
            List<BookingInterval> currentBookings = room.getBookings().stream()
                    .filter(interval -> interval.getCustomerName().equals(customerName))
                    .filter(interval -> interval.getStartDate().equals(startDate))
                    .toList();
            if (currentBookings.size() > 0) {
                currentBookings.forEach(interval -> interval.setCheckedIn(interval.getStartDate()));
                bookedRoomNumbers.add(room.getRoomNumber());
                roomRepository.save(room);
            }
        });
        return bookedRoomNumbers;
    }

    public void checkOut(String customerName, String roomNumber, LocalDate endDate) {
        Room room = roomRepository.findByRoomNumber(roomNumber);
        List<BookingInterval> bookingsToCheckOut = room.getBookings().stream()
                .filter(interval -> Objects.equals(interval.getCustomerName(), customerName))
                .filter(interval -> interval.getEndDate().equals(endDate)).toList();
        if(bookingsToCheckOut.size() == 0){
            throw new IllegalStateException("No booking to be checked out!");
        }
        if(bookingsToCheckOut.size() > 1){
            throw new IllegalStateException("More than one booking found!");
        }
        BookingInterval booking = bookingsToCheckOut.getFirst();
        if(booking.getInvoiceId() == null){
            throw new IllegalStateException("Checkout only possible for invoiced bookings.");
        }
        booking.setCheckOut(booking.getEndDate());
        roomRepository.save(room);
    }
}
