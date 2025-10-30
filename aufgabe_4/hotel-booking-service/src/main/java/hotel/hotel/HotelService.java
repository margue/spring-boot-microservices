package hotel.hotel;

import hotel.payment.PaymentServiceApi;
import hotel.shared.PriceCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
class HotelService {

    private final RoomRepository roomRepository;
    private final PaymentServiceApi paymentService;

    public HotelService(RoomRepository roomRepository, PaymentServiceApi paymentService) {
        this.roomRepository = roomRepository;
        this.paymentService = paymentService;
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
                return PriceCalculator.calcPrice(bookingInterval.dates().size());
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

    public Long produceInvoice(String customerName, LocalDate endDate, List<String> roomNumbers) {
        Map<String, List<BookingInterval>> bookingsForRooms = getBookingsForRooms(customerName, endDate, roomNumbers);
        double totalAmount = getTotalAmount(bookingsForRooms);

        Map<String, List<String>> lineItemsPerRoom = bookingsForRooms.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToLineItemDescription(entry.getValue())
                ));
        Long invoiceId = paymentService.produceInvoice(customerName, totalAmount, lineItemsPerRoom);

        roomRepository.markBookingsAsInvoiced(bookingsForRooms, invoiceId);

        return invoiceId;
    }


    private double getTotalAmount(Map<String, List<BookingInterval>> bookingsForRooms) {
        return bookingsForRooms.values().stream()
                .mapToDouble(bookingsForRoom -> bookingsForRoom.stream()
                        .mapToDouble(bookingInterval -> PriceCalculator.calcPrice(bookingInterval.dates().size()))
                        .sum())
                .sum();
    }

    private Map<String, List<BookingInterval>> getBookingsForRooms(String customerName, LocalDate endDate, List<String> roomNumbers) {
        List<Room> bookedRooms = roomRepository.findAllRoomsWithBookingIntervalsByCustomerName(customerName)
                .stream().filter(r -> roomNumbers.contains(r.getRoomNumber())).collect(Collectors.toList());
        Map<String, List<BookingInterval>> bookingsForRooms = new HashMap<>();

        bookedRooms.forEach(room -> {
            List<BookingInterval> applicableBookings = room.getBookings().stream()
                    .filter(booking -> Objects.equals(booking.getCustomerName(), customerName))
                    .filter(booking -> !booking.getEndDate().isAfter(endDate))
                    .filter(booking -> booking.getInvoiceId() == null)
                    .filter(BookingInterval::isCheckedIn).collect(Collectors.toList());
            if(applicableBookings.size() > 0 ){
                bookingsForRooms.put(room.getRoomNumber(), applicableBookings);
            } else {
                throw new IllegalArgumentException(String.format("No bookingIntervals to be invoiced for given customer " +
                        "'%s', endDate [%s] and roomNumbers %s", customerName, endDate, roomNumbers));
            }
        });
        return bookingsForRooms;
    }

    private List<String> convertToLineItemDescription(List<BookingInterval> bookingIntervals) {
        return bookingIntervals.stream()
                .map(b -> b.getCustomerName() + ": " +b.getStartDate() + " - " +b.getEndDate())
                .toList();
    }
}
